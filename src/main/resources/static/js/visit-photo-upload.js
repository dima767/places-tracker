/**
 * Visit Photo Upload functionality
 * Handles multiple photo uploads per visit
 */

async function uploadVisitPhotos(visitId) {
    const fileInput = document.getElementById(`photo-input-${visitId}`);
    const statusDiv = document.getElementById(`upload-status-${visitId}`);
    const files = fileInput.files;

    if (!files || files.length === 0) {
        showUploadStatus(statusDiv, 'Please select at least one photo', 'danger');
        return;
    }

    const placeId = fileInput.dataset.placeId;
    const maxSizeMB = parseInt(document.querySelector('[data-max-photo-size-mb]')?.dataset.maxPhotoSizeMb) || 15;
    const maxSize = maxSizeMB * 1024 * 1024;

    // Validate all files
    for (let file of files) {
        if (file.size > maxSize) {
            showUploadStatus(statusDiv, `File ${file.name} exceeds ${maxSizeMB}MB`, 'danger');
            return;
        }
        const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
        if (!allowedTypes.includes(file.type.toLowerCase())) {
            showUploadStatus(statusDiv, `File ${file.name} is not a valid image type`, 'danger');
            return;
        }
    }

    // Show progress
    showUploadStatus(statusDiv, `Uploading ${files.length} photo(s)...`, 'info');

    try {
        const formData = new FormData();
        for (let file of files) {
            formData.append('photos', file);
        }

        const response = await fetch(`/placestracker/places/${placeId}/visits/${visitId}/photos/bulk`, {
            method: 'POST',
            body: formData
        });

        if (response.ok) {
            const message = await response.text();
            showUploadStatus(statusDiv, message, 'success');
            fileInput.value = ''; // Clear input

            // Reload the page to show new photos
            setTimeout(() => {
                window.location.reload();
            }, 1000);
        } else {
            const error = await response.text();
            showUploadStatus(statusDiv, `Upload failed: ${error}`, 'danger');
        }
    } catch (error) {
        console.error('Upload error:', error);
        showUploadStatus(statusDiv, `Upload error: ${error.message}`, 'danger');
    }
}

function showUploadStatus(statusDiv, message, type) {
    statusDiv.innerHTML = `<div class="alert alert-${type} alert-dismissible fade show py-1" role="alert">
        <small>${message}</small>
        <button type="button" class="btn-close btn-close-sm" data-bs-dismiss="alert" aria-label="Close"></button>
    </div>`;
}
