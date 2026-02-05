package dk.placestracker.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * Service for managing visit photos using MongoDB GridFS.
 * Photos are now associated with specific visits rather than places.
 *
 * @author Dmitriy Kopylenko
 */
public interface PhotoService {

    /**
     * Store a single photo in GridFS for a specific visit.
     *
     * @param file the photo file to store
     * @param placeId the ID of the place
     * @param visitId the ID of the visit this photo belongs to
     * @return the GridFS file ID
     * @throws IOException if there's an error reading the file
     */
    String savePhotoForVisit(MultipartFile file, String placeId, String visitId) throws IOException;

    /**
     * Store multiple photos in GridFS for a specific visit (bulk upload).
     *
     * @param files the photo files to store
     * @param placeId the ID of the place
     * @param visitId the ID of the visit these photos belong to
     * @return list of GridFS file IDs
     * @throws IOException if there's an error reading any file
     */
    List<String> savePhotosForVisit(List<MultipartFile> files, String placeId, String visitId) throws IOException;

    /**
     * Retrieve a photo from GridFS.
     *
     * @param photoId the GridFS file ID
     * @return PhotoData containing the photo stream and metadata
     */
    PhotoData getPhoto(String photoId);

    /**
     * Delete a photo from GridFS.
     *
     * @param photoId the GridFS file ID to delete
     */
    void deletePhoto(String photoId);

    /**
     * Delete all photos for a specific visit (cascade delete).
     *
     * @param visitId the visit ID
     */
    void deletePhotosByVisitId(String visitId);

    /**
     * Find all photo IDs for a specific visit.
     *
     * @param visitId the visit ID
     * @return list of GridFS file IDs
     */
    List<String> findPhotoIdsByVisitId(String visitId);

    /**
     * Check if a photo exists in GridFS.
     *
     * @param photoId the GridFS file ID
     * @return true if the photo exists, false otherwise
     */
    boolean photoExists(String photoId);

    /**
     * Check which photos exist in GridFS (batch operation).
     * This is more efficient than calling photoExists() for each photo.
     *
     * @param photoIds the set of GridFS file IDs to check
     * @return set of photo IDs that exist in GridFS
     */
    Set<String> photosExist(Set<String> photoIds);

    /**
     * Get or generate a thumbnail for a photo.
     * Thumbnails are generated on-demand and cached in GridFS.
     *
     * @param photoId the original photo's GridFS file ID
     * @param size the thumbnail size (width and height in pixels)
     * @return PhotoData containing the thumbnail stream and metadata
     */
    PhotoData getThumbnail(String photoId, int size);

    /**
     * Data class for photo retrieval.
     */
    record PhotoData(
        InputStream inputStream,
        String contentType,
        String filename,
        long length
    ) {}
}
