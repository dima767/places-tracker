/**
 * Google Maps Auto-Fill functionality
 * Handles extracting place information from Google Maps URLs
 */

async function fillFromGoogleMapsUrl() {
    const urlInput = document.getElementById('googleMapsUrl');
    const mapsUrl = urlInput.value.trim();
    const btn = document.getElementById('fillFromUrlBtn');
    const spinner = document.getElementById('urlLoadingSpinner');
    const errorDiv = document.getElementById('urlErrorMessage');
    const successDiv = document.getElementById('urlSuccessMessage');

    // Clear previous messages
    errorDiv.classList.add('d-none');
    successDiv.classList.add('d-none');

    if (!mapsUrl) {
        showError('Please enter a Google Maps URL');
        return;
    }

    // Show loading state
    btn.disabled = true;
    spinner.classList.remove('d-none');

    try {
        // Use the API endpoint from config (set by template with context path)
        const url = window.placesTrackerConfig?.extractFromUrlEndpoint || '/placestracker/places/extract-from-url';
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: 'mapsUrl=' + encodeURIComponent(mapsUrl)
        });

        const result = await response.json();

        if (result.success && result.data) {
            populateFormFields(result.data);
            successDiv.textContent = result.message || 'Successfully filled place information!';
            successDiv.classList.remove('d-none');
        } else {
            showError(result.message || 'Failed to extract place information');
        }
    } catch (error) {
        console.error('Error extracting place:', error);
        showError('Network error: ' + error.message);
    } finally {
        btn.disabled = false;
        spinner.classList.add('d-none');
    }
}

function populateFormFields(data) {
    // Fill name
    if (data.name) {
        document.getElementById('name').value = data.name;
    }

    // Fill location (city)
    if (data.location) {
        document.getElementById('location').value = data.location;
    }

    // Fill country (triggers htmx state reload)
    if (data.country) {
        const countrySelect = document.getElementById('country');
        countrySelect.value = data.country;
        // Trigger change event to load states via htmx
        countrySelect.dispatchEvent(new Event('change'));

        // Wait for htmx to populate states, then set state value
        if (data.state) {
            setTimeout(() => {
                const stateSelect = document.getElementById('state');
                if (stateSelect) {
                    stateSelect.value = data.state;
                }
            }, 500); // Adjust timing based on htmx response speed
        }
    }

    // Fill coordinates
    if (data.latitude !== null && data.latitude !== undefined) {
        document.getElementById('latitude').value = data.latitude;
    }
    if (data.longitude !== null && data.longitude !== undefined) {
        document.getElementById('longitude').value = data.longitude;
    }

    // Fill Google-extracted data (hidden fields)
    if (data.formattedAddress) {
        document.getElementById('formattedAddress').value = data.formattedAddress;
    }
    if (data.googlePlaceId) {
        document.getElementById('googlePlaceId').value = data.googlePlaceId;
    }
    if (data.website) {
        document.getElementById('website').value = data.website;
    }
    if (data.phoneNumber) {
        document.getElementById('phoneNumber').value = data.phoneNumber;
    }
    if (data.googleRating !== null && data.googleRating !== undefined) {
        document.getElementById('googleRating').value = data.googleRating;
    }
    if (data.googleReviewCount !== null && data.googleReviewCount !== undefined) {
        document.getElementById('googleReviewCount').value = data.googleReviewCount;
    }

    // Serialize Google reviews as JSON
    if (data.googleReviews && Array.isArray(data.googleReviews)) {
        document.getElementById('googleReviewsJson').value = JSON.stringify(data.googleReviews);
    }
}

function showError(message) {
    const errorDiv = document.getElementById('urlErrorMessage');
    errorDiv.textContent = message;
    errorDiv.classList.remove('d-none');
}
