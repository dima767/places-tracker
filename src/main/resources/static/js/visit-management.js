/**
 * Visit Management functionality
 * Handles adding, removing, and managing visit entries in place forms
 */

let visitIndex;
const today = new Date().toISOString().split('T')[0];

/**
 * Initialize visit management
 * @param {Object} config - Configuration object
 * @param {number} config.initialIndex - Starting visit index (default: 1)
 * @param {Function} config.initMaxDate - Function to set max date on initial visit inputs
 */
function initVisitManagement(config = {}) {
    visitIndex = config.initialIndex !== undefined ? config.initialIndex : 1;

    document.addEventListener('DOMContentLoaded', function() {
        if (config.initMaxDate && typeof config.initMaxDate === 'function') {
            config.initMaxDate(today);
        }
        updateRemoveButtons();
        bindRemoveVisitButtons();
    });
}

/**
 * Bind click handlers to remove visit buttons for confirmation modal integration
 */
function bindRemoveVisitButtons() {
    document.addEventListener('click', function(e) {
        const removeBtn = e.target.closest('.remove-visit-btn');
        if (removeBtn && !removeBtn.disabled) {
            e.preventDefault();
            e.stopPropagation();

            // Use ConfirmModal if available, otherwise fall back to native confirm
            if (typeof ConfirmModal !== 'undefined') {
                ConfirmModal.show('delete-visit', function() {
                    executeRemoveVisit(removeBtn);
                });
            } else if (confirm('Are you sure you want to remove this visit?')) {
                executeRemoveVisit(removeBtn);
            }
        }
    });
}

/**
 * Execute the actual visit removal
 */
function executeRemoveVisit(button) {
    const row = button.closest('.visit-row');
    if (row) {
        row.remove();
        reindexVisits();
        updateRemoveButtons();
        updateVisitNumbers();
    }
}

function addVisit() {
    const container = document.getElementById('visits-container');
    const newRow = document.createElement('div');
    newRow.className = 'visit-row card mb-3';
    newRow.setAttribute('data-visit-index', visitIndex);
    // Visit card styling is handled by CSS (.visit-row.card)

    const visitId = generateVisitUUID();
    const placeIdField = document.getElementById('place-id-field');
    const isEditPage = placeIdField && placeIdField.value && placeIdField.readOnly;
    const placeId = placeIdField ? placeIdField.value : null;

    let photoSectionHtml;
    if (isEditPage && placeId) {
        photoSectionHtml = `
        <div class="mt-3 pt-3" style="border-top: 1px solid var(--border-light);">
            <div class="mb-2">
                <label for="photo-input-${visitId}" class="form-label small">
                    <i class="bi bi-images me-1"></i>Photos
                </label>
                <input type="file"
                       class="form-control form-control-sm"
                       id="photo-input-${visitId}"
                       multiple
                       accept="image/jpeg,image/png,image/webp"
                       data-place-id="${placeId}"
                       data-visit-id="${visitId}">
                <div class="form-text">Max 15MB each. JPG, PNG, WebP.</div>
                <button type="button"
                        class="btn btn-sm btn-primary mt-1"
                        data-visit-id="${visitId}"
                        onclick="uploadVisitPhotos(this.getAttribute('data-visit-id'))">
                    <i class="bi bi-upload me-1"></i>Upload
                </button>
                <div id="upload-status-${visitId}" class="mt-1"></div>
            </div>
            <div id="photo-gallery-${visitId}" class="small mt-2" style="color: var(--text-tertiary);">
                No photos yet
            </div>
        </div>`;
    } else {
        photoSectionHtml = `
        <div class="mt-3 pt-3" style="border-top: 1px solid var(--border-light);">
            <label class="form-label small">
                <i class="bi bi-images me-1"></i>Photos
            </label>
            <input type="file"
                   class="form-control form-control-sm visit-photos"
                   name="visitPhotos[${visitIndex}]"
                   data-visit-index="${visitIndex}"
                   multiple
                   accept="image/jpeg,image/png,image/webp"
                   onchange="updatePhotoCount(this)">
            <div class="form-text">Max 15MB each. JPG, PNG, WebP.</div>
            <div class="photo-count-display small mt-1" style="color: var(--primary-600);"></div>
        </div>`;
    }

    newRow.innerHTML = `
        <div class="card-header d-flex justify-content-between align-items-center py-2">
            <span class="fw-medium" style="color: var(--primary-700);">
                <i class="bi bi-calendar-event me-1"></i>Visit #<span class="visit-number">${visitIndex + 1}</span>
            </span>
            <button type="button" class="btn btn-outline-danger btn-sm remove-visit-btn">
                <i class="bi bi-trash me-1"></i>Remove
            </button>
        </div>
        <div class="card-body">
            <input type="hidden" class="visit-id-field" name="visits[${visitIndex}].id" value="${visitId}">
            <div class="row g-3">
                <div class="col-md-3">
                    <label class="form-label">Date <span class="text-danger">*</span></label>
                    <input type="date"
                           class="form-control visit-date-input"
                           name="visits[${visitIndex}].date"
                           max="${today}"
                           required>
                </div>
                <div class="col-md-2">
                    <label class="form-label">Temp (°F)</label>
                    <input type="number"
                           class="form-control"
                           name="visits[${visitIndex}].temperatureF"
                           step="0.1"
                           min="-50"
                           max="150"
                           placeholder="72">
                </div>
                <div class="col-md-2">
                    <label class="form-label">Duration</label>
                    <input type="text"
                           class="form-control"
                           name="visitDurations[${visitIndex}]"
                           placeholder="2h30min"
                           title="Format: 10min, 1h25min">
                </div>
                <div class="col-12 mt-2">
                    <label class="form-label">Notes</label>
                    <textarea class="form-control visit-notes-textarea"
                              name="visits[${visitIndex}].notes"
                              rows="6"
                              maxlength="2000"
                              placeholder="What did you experience? Describe the trails, wildlife, weather, memorable moments..."></textarea>
                </div>
            </div>
            ${photoSectionHtml}
        </div>
    `;
    container.appendChild(newRow);
    visitIndex++;
    updateRemoveButtons();
    updateVisitNumbers();
}

function generateVisitUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

// Legacy function kept for backwards compatibility
function removeVisit(button) {
    // Redirect to the new confirmation-based approach
    if (typeof ConfirmModal !== 'undefined') {
        ConfirmModal.show('delete-visit', function() {
            executeRemoveVisit(button);
        });
    } else if (confirm('Are you sure you want to remove this visit?')) {
        executeRemoveVisit(button);
    }
}

function reindexVisits() {
    const rows = document.querySelectorAll('.visit-row');
    rows.forEach((row, index) => {
        row.setAttribute('data-visit-index', index);
        row.querySelectorAll('input, textarea').forEach(input => {
            const name = input.getAttribute('name');
            if (name && name.startsWith('visits[')) {
                const field = name.substring(name.indexOf('].') + 2);
                input.setAttribute('name', `visits[${index}].${field}`);
            } else if (name && name.startsWith('visitPhotos[')) {
                input.setAttribute('name', `visitPhotos[${index}]`);
            } else if (name && name.startsWith('visitDurations[')) {
                input.setAttribute('name', `visitDurations[${index}]`);
            }
        });

        const fileInput = row.querySelector('.visit-photos');
        if (fileInput) {
            fileInput.setAttribute('data-visit-index', index);
        }
    });
    visitIndex = rows.length;
}

function updateRemoveButtons() {
    const rows = document.querySelectorAll('.visit-row');
    const removeButtons = document.querySelectorAll('.visit-row .remove-visit-btn');

    removeButtons.forEach(btn => {
        btn.disabled = rows.length === 1;
    });
}

function updateVisitNumbers() {
    const rows = document.querySelectorAll('.visit-row');
    rows.forEach((row, index) => {
        const visitNumberSpan = row.querySelector('.visit-number');
        if (visitNumberSpan) {
            visitNumberSpan.textContent = index + 1;
        }
    });
}

function updatePhotoCount(input) {
    const visitRow = input.closest('.visit-row');
    const displayDiv = visitRow.querySelector('.photo-count-display');
    const fileCount = input.files.length;

    if (fileCount === 0) {
        displayDiv.textContent = '';
    } else if (fileCount === 1) {
        displayDiv.textContent = `${fileCount} photo selected`;
    } else {
        displayDiv.textContent = `${fileCount} photos selected`;
    }
}
