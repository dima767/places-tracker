/**
 * Settings page customization for Google Maps Auto-Fill
 * Overrides field population to map coordinates to homeLatitude/homeLongitude
 */

// Override the populateFormFields function for settings page
const originalPopulateFormFields = window.populateFormFields;

window.populateFormFields = function(data) {
    // Only populate coordinates for settings page
    if (data.latitude !== null && data.latitude !== undefined) {
        const latField = document.getElementById('homeLatitude');
        if (latField) {
            latField.value = data.latitude;
            // Remove readonly temporarily to allow value change, then reapply
            latField.removeAttribute('readonly');
            latField.value = data.latitude;
            latField.setAttribute('readonly', 'readonly');
        }
    }
    if (data.longitude !== null && data.longitude !== undefined) {
        const lonField = document.getElementById('homeLongitude');
        if (lonField) {
            lonField.value = data.longitude;
            // Remove readonly temporarily to allow value change, then reapply
            lonField.removeAttribute('readonly');
            lonField.value = data.longitude;
            lonField.setAttribute('readonly', 'readonly');
        }
    }
};
