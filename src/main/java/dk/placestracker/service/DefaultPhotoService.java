package dk.placestracker.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.coobird.thumbnailator.Thumbnails;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Default implementation of PhotoService using MongoDB GridFS.
 * Photos are stored with visit-level metadata for proper organization.
 *
 * @author Dmitriy Kopylenko
 */
@Service
public class DefaultPhotoService implements PhotoService {

    private static final Logger log = LoggerFactory.getLogger(DefaultPhotoService.class);

    // Allowed file types for photos
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    );

    private final GridFsTemplate gridFsTemplate;
    private final long maxFileSizeBytes;

    public DefaultPhotoService(GridFsTemplate gridFsTemplate,
                              @Value("${app.photo.max-size-mb}") int maxSizeMB) {
        this.gridFsTemplate = gridFsTemplate;
        this.maxFileSizeBytes = maxSizeMB * 1024L * 1024L;
    }

    @Override
    public String savePhotoForVisit(MultipartFile file, String placeId, String visitId) throws IOException {
        // Validate file
        validatePhoto(file);

        // Create metadata document with place and visit IDs
        Document metadata = new Document()
            .append("placeId", placeId)
            .append("visitId", visitId)
            .append("uploadDate", LocalDateTime.now().toString());

        // Store in GridFS with metadata
        ObjectId fileId = gridFsTemplate.store(
            file.getInputStream(),
            file.getOriginalFilename(),
            file.getContentType(),
            metadata
        );

        log.info("Stored photo {} for place {} visit {} with GridFS ID: {}",
                 file.getOriginalFilename(), placeId, visitId, fileId.toString());

        return fileId.toString();
    }

    @Override
    public List<String> savePhotosForVisit(List<MultipartFile> files, String placeId, String visitId) throws IOException {
        if (files.isEmpty()) {
            return new ArrayList<>();
        }

        // For small number of files, use sequential upload (overhead not worth it)
        if (files.size() <= 2) {
            return savePhotosSequentially(files, placeId, visitId);
        }

        // Parallel upload for 3+ files
        ConcurrentLinkedQueue<String> uploadedIds = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Exception> errors = new ConcurrentLinkedQueue<>();

        files.parallelStream().forEach(file -> {
            try {
                String photoId = savePhotoForVisit(file, placeId, visitId);
                uploadedIds.add(photoId);
            } catch (Exception e) {
                log.error("Failed to upload photo {}: {}", file.getOriginalFilename(), e.getMessage());
                errors.add(e);
            }
        });

        // If any errors occurred, clean up all uploaded photos and throw exception
        if (!errors.isEmpty()) {
            log.warn("Cleaning up {} uploaded photos due to {} errors", uploadedIds.size(), errors.size());
            for (String photoId : uploadedIds) {
                try {
                    deletePhoto(photoId);
                } catch (Exception cleanupException) {
                    log.error("Failed to cleanup photo {}", photoId, cleanupException);
                }
            }
            Exception firstError = errors.peek();
            throw new IOException("Failed to upload " + errors.size() + " photo(s): " + firstError.getMessage(), firstError);
        }

        List<String> photoIds = new ArrayList<>(uploadedIds);
        log.info("Successfully uploaded {} photos in parallel for place {} visit {}", photoIds.size(), placeId, visitId);
        return photoIds;
    }

    private List<String> savePhotosSequentially(List<MultipartFile> files, String placeId, String visitId) throws IOException {
        List<String> photoIds = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                String photoId = savePhotoForVisit(file, placeId, visitId);
                photoIds.add(photoId);
            } catch (Exception e) {
                // If any file fails, clean up already uploaded photos
                log.error("Failed to upload photo {}, cleaning up already uploaded photos", file.getOriginalFilename(), e);
                for (String photoId : photoIds) {
                    try {
                        deletePhoto(photoId);
                    } catch (Exception cleanupException) {
                        log.error("Failed to cleanup photo {}", photoId, cleanupException);
                    }
                }
                throw new IOException("Failed to upload photos: " + e.getMessage(), e);
            }
        }

        log.info("Successfully uploaded {} photos for place {} visit {}", photoIds.size(), placeId, visitId);
        return photoIds;
    }

    @Override
    public PhotoData getPhoto(String photoId) {
        try {
            GridFSFile gridFsFile = gridFsTemplate.findOne(
                new Query(Criteria.where("_id").is(new ObjectId(photoId)))
            );

            if (gridFsFile == null) {
                throw new RuntimeException("Photo not found: " + photoId);
            }

            GridFsResource resource = gridFsTemplate.getResource(gridFsFile);

            return new PhotoData(
                resource.getInputStream(),
                gridFsFile.getMetadata() != null ?
                    gridFsFile.getMetadata().getString("_contentType") : "image/jpeg",
                gridFsFile.getFilename(),
                gridFsFile.getLength()
            );

        } catch (IOException e) {
            log.error("Error retrieving photo: {}", photoId, e);
            throw new RuntimeException("Failed to retrieve photo", e);
        }
    }

    @Override
    public void deletePhoto(String photoId) {
        // Delete the original photo
        gridFsTemplate.delete(new Query(Criteria.where("_id").is(new ObjectId(photoId))));

        // Also delete any cached thumbnails for this photo
        Query thumbnailQuery = new Query(Criteria.where("metadata.originalId").is(photoId));
        gridFsTemplate.delete(thumbnailQuery);

        log.info("Deleted photo and thumbnails with GridFS ID: {}", photoId);
    }

    @Override
    public void deletePhotosByVisitId(String visitId) {
        Query query = new Query(Criteria.where("metadata.visitId").is(visitId));
        gridFsTemplate.delete(query);
        log.info("Deleted all photos for visit: {}", visitId);
    }

    @Override
    public List<String> findPhotoIdsByVisitId(String visitId) {
        Query query = new Query(Criteria.where("metadata.visitId").is(visitId));
        return gridFsTemplate.find(query)
            .into(new ArrayList<>())
            .stream()
            .map(gridFSFile -> gridFSFile.getObjectId().toString())
            .collect(Collectors.toList());
    }

    @Override
    public boolean photoExists(String photoId) {
        try {
            GridFSFile gridFsFile = gridFsTemplate.findOne(
                new Query(Criteria.where("_id").is(new ObjectId(photoId)))
            );
            return gridFsFile != null;
        } catch (IllegalArgumentException e) {
            // Invalid ObjectId format
            log.warn("Invalid photo ID format: {}", photoId);
            return false;
        } catch (Exception e) {
            log.error("Error checking if photo exists: {}", photoId, e);
            return false;
        }
    }

    @Override
    public Set<String> photosExist(Set<String> photoIds) {
        if (photoIds == null || photoIds.isEmpty()) {
            return Collections.emptySet();
        }

        try {
            // Convert string IDs to ObjectIds, filtering out invalid ones
            List<ObjectId> objectIds = photoIds.stream()
                .filter(id -> {
                    try {
                        new ObjectId(id);
                        return true;
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid photo ID format: {}", id);
                        return false;
                    }
                })
                .map(ObjectId::new)
                .toList();

            if (objectIds.isEmpty()) {
                return Collections.emptySet();
            }

            // Single query to find all existing photos
            Query query = new Query(Criteria.where("_id").in(objectIds));
            return gridFsTemplate.find(query)
                .into(new ArrayList<>())
                .stream()
                .map(file -> file.getObjectId().toString())
                .collect(Collectors.toSet());

        } catch (Exception e) {
            log.error("Error checking if photos exist: {}", photoIds, e);
            return Collections.emptySet();
        }
    }

    @Override
    public PhotoData getThumbnail(String photoId, int size) {
        // Look for cached thumbnail first
        String thumbnailFilename = "thumb_" + photoId + "_" + size + ".jpg";
        Query thumbnailQuery = new Query(Criteria.where("metadata.originalId").is(photoId)
                .and("metadata.thumbnailSize").is(size));

        GridFSFile cachedThumb = gridFsTemplate.findOne(thumbnailQuery);
        if (cachedThumb != null) {
            try {
                GridFsResource resource = gridFsTemplate.getResource(cachedThumb);
                return new PhotoData(
                    resource.getInputStream(),
                    "image/jpeg",
                    thumbnailFilename,
                    cachedThumb.getLength()
                );
            } catch (IOException e) {
                log.warn("Failed to read cached thumbnail, regenerating: {}", photoId, e);
                // Fall through to regenerate
            }
        }

        // Generate thumbnail from original
        try {
            PhotoData original = getPhoto(photoId);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Thumbnails.of(original.inputStream())
                .size(size, size)
                .outputFormat("jpg")
                .outputQuality(0.8)
                .toOutputStream(outputStream);

            byte[] thumbnailBytes = outputStream.toByteArray();

            // Cache the thumbnail in GridFS
            Document metadata = new Document()
                .append("originalId", photoId)
                .append("thumbnailSize", size)
                .append("type", "thumbnail")
                .append("generatedAt", LocalDateTime.now().toString());

            gridFsTemplate.store(
                new ByteArrayInputStream(thumbnailBytes),
                thumbnailFilename,
                "image/jpeg",
                metadata
            );

            log.info("Generated and cached {}px thumbnail for photo {}", size, photoId);

            return new PhotoData(
                new ByteArrayInputStream(thumbnailBytes),
                "image/jpeg",
                thumbnailFilename,
                thumbnailBytes.length
            );

        } catch (Exception e) {
            log.error("Failed to generate thumbnail for photo {}: {}", photoId, e.getMessage());
            // Fall back to original photo
            return getPhoto(photoId);
        }
    }

    private void validatePhoto(MultipartFile file) throws IOException {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }

        // Check file size
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException(
                String.format("File size exceeds maximum allowed size of %d MB",
                             maxFileSizeBytes / (1024 * 1024))
            );
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                "Invalid file type. Allowed types: " + String.join(", ", ALLOWED_CONTENT_TYPES)
            );
        }

        // Additional security: Check file extension
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            if (!Arrays.asList("jpg", "jpeg", "png", "webp").contains(extension)) {
                throw new IllegalArgumentException("Invalid file extension: " + extension);
            }
        }
    }
}
