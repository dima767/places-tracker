/**
 * Places Tracker - Confirmation Modal
 * Replaces browser's default confirm() dialogs with a polished custom modal.
 *
 * Usage:
 * 1. For forms: Add data-confirm="messageKey" or data-confirm="Custom message" to the form
 * 2. For buttons/links: Add data-confirm="messageKey" and optionally data-confirm-action="url"
 * 3. For htmx: The modal intercepts hx-confirm automatically
 *
 * Message keys are defined in ConfirmModal.messages for centralized maintenance.
 */

const ConfirmModal = {
    // Centralized confirmation messages for easy maintenance
    messages: {
        // Place-related
        'delete-place': {
            title: 'Delete Place',
            message: 'Are you sure you want to delete this place? All visits and photos will be permanently removed.',
            confirmText: 'Delete Place',
            cancelText: 'Cancel',
            type: 'danger'
        },

        // Photo-related
        'delete-photo': {
            title: 'Delete Photo',
            message: 'Are you sure you want to delete this photo? This action cannot be undone.',
            confirmText: 'Delete',
            cancelText: 'Cancel',
            type: 'danger'
        },

        // Visit-related
        'delete-visit': {
            title: 'Remove Visit',
            message: 'Are you sure you want to remove this visit? Any photos attached to this visit will also be deleted.',
            confirmText: 'Remove',
            cancelText: 'Cancel',
            type: 'danger'
        },

        // Settings-related
        'clear-home-location': {
            title: 'Clear Home Location',
            message: 'Are you sure you want to clear your home location? Distance calculations will show N/A until you set a new location.',
            confirmText: 'Clear Location',
            cancelText: 'Cancel',
            type: 'warning'
        },

        // Generic confirmations
        'unsaved-changes': {
            title: 'Unsaved Changes',
            message: 'You have unsaved changes. Are you sure you want to leave this page?',
            confirmText: 'Leave Page',
            cancelText: 'Stay',
            type: 'warning'
        }
    },

    // Modal element reference
    modalElement: null,

    // Current confirmation callback
    currentCallback: null,
    currentForm: null,

    /**
     * Initialize the confirmation modal system
     */
    init: function() {
        this.createModal();
        this.bindEvents();
        this.setupHtmxIntegration();
    },

    /**
     * Create the modal HTML and inject into DOM
     */
    createModal: function() {
        const modalHTML = `
            <div class="confirm-modal-backdrop" id="confirmModalBackdrop">
                <div class="confirm-modal" role="dialog" aria-modal="true" aria-labelledby="confirmModalTitle">
                    <div class="confirm-modal-icon" id="confirmModalIcon">
                        <i class="bi bi-exclamation-triangle"></i>
                    </div>
                    <h3 class="confirm-modal-title" id="confirmModalTitle">Confirm Action</h3>
                    <p class="confirm-modal-message" id="confirmModalMessage">Are you sure you want to proceed?</p>
                    <div class="confirm-modal-actions">
                        <button type="button" class="confirm-modal-btn confirm-modal-btn-cancel" id="confirmModalCancel">
                            Cancel
                        </button>
                        <button type="button" class="confirm-modal-btn confirm-modal-btn-confirm" id="confirmModalConfirm">
                            Confirm
                        </button>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', modalHTML);
        this.modalElement = document.getElementById('confirmModalBackdrop');
    },

    /**
     * Bind event listeners
     */
    bindEvents: function() {
        const self = this;

        // Cancel button
        document.getElementById('confirmModalCancel').addEventListener('click', function() {
            self.hide();
            self.currentCallback = null;
            self.currentForm = null;
        });

        // Confirm button
        document.getElementById('confirmModalConfirm').addEventListener('click', function() {
            self.hide();
            if (self.currentCallback) {
                self.currentCallback();
            } else if (self.currentForm) {
                // Remove the data-confirm to prevent loop, then submit
                self.currentForm.removeAttribute('data-confirm');
                self.currentForm.submit();
            }
            self.currentCallback = null;
            self.currentForm = null;
        });

        // Close on backdrop click
        this.modalElement.addEventListener('click', function(e) {
            if (e.target === self.modalElement) {
                self.hide();
                self.currentCallback = null;
                self.currentForm = null;
            }
        });

        // Close on Escape key
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && self.modalElement.classList.contains('is-visible')) {
                self.hide();
                self.currentCallback = null;
                self.currentForm = null;
            }
        });

        // Intercept form submissions with data-confirm
        document.addEventListener('submit', function(e) {
            const form = e.target;
            const confirmKey = form.getAttribute('data-confirm');

            if (confirmKey) {
                e.preventDefault();
                self.currentForm = form;
                self.show(confirmKey);
            }
        });

        // Handle click events for non-form elements with data-confirm
        document.addEventListener('click', function(e) {
            const trigger = e.target.closest('[data-confirm]:not(form)');
            if (trigger && !trigger.closest('form[data-confirm]')) {
                e.preventDefault();
                const confirmKey = trigger.getAttribute('data-confirm');
                const action = trigger.getAttribute('data-confirm-action') || trigger.getAttribute('href');

                self.show(confirmKey, function() {
                    if (action) {
                        window.location.href = action;
                    }
                });
            }
        });
    },

    /**
     * Setup htmx integration to intercept hx-confirm
     */
    setupHtmxIntegration: function() {
        const self = this;

        document.body.addEventListener('htmx:confirm', function(e) {
            const confirmValue = e.detail.question;
            const issueRequest = e.detail.issueRequest;

            // Skip if no confirm question (not a confirmation request)
            if (!confirmValue) {
                return;  // Let htmx proceed normally
            }

            e.preventDefault();

            // Map common htmx confirm messages to our keys
            let messageKey = confirmValue;
            if (confirmValue.toLowerCase().includes('delete this photo')) {
                messageKey = 'delete-photo';
            } else if (confirmValue.toLowerCase().includes('delete this place')) {
                messageKey = 'delete-place';
            } else if (confirmValue.toLowerCase().includes('clear') && confirmValue.toLowerCase().includes('home')) {
                messageKey = 'clear-home-location';
            }

            self.show(messageKey, function() {
                issueRequest(true);
            });
        });
    },

    /**
     * Show the confirmation modal
     * @param {string} messageKeyOrText - Message key from messages object or custom text
     * @param {function} callback - Optional callback to execute on confirm
     */
    show: function(messageKeyOrText, callback) {
        const config = this.messages[messageKeyOrText] || {
            title: 'Confirm',
            message: messageKeyOrText,
            confirmText: 'Confirm',
            cancelText: 'Cancel',
            type: 'warning'
        };

        this.currentCallback = callback || null;

        // Update modal content
        document.getElementById('confirmModalTitle').textContent = config.title;
        document.getElementById('confirmModalMessage').textContent = config.message;
        document.getElementById('confirmModalConfirm').textContent = config.confirmText;
        document.getElementById('confirmModalCancel').textContent = config.cancelText;

        // Update icon and type
        const iconEl = document.getElementById('confirmModalIcon');
        const confirmBtn = document.getElementById('confirmModalConfirm');

        iconEl.className = 'confirm-modal-icon confirm-modal-icon-' + config.type;
        confirmBtn.className = 'confirm-modal-btn confirm-modal-btn-confirm confirm-modal-btn-' + config.type;

        if (config.type === 'danger') {
            iconEl.innerHTML = '<i class="bi bi-exclamation-triangle"></i>';
        } else if (config.type === 'warning') {
            iconEl.innerHTML = '<i class="bi bi-question-circle"></i>';
        } else {
            iconEl.innerHTML = '<i class="bi bi-info-circle"></i>';
        }

        // Show modal with animation
        this.modalElement.classList.add('is-visible');
        document.body.style.overflow = 'hidden';

        // Focus the cancel button for accessibility
        setTimeout(() => {
            document.getElementById('confirmModalCancel').focus();
        }, 100);
    },

    /**
     * Hide the confirmation modal
     */
    hide: function() {
        this.modalElement.classList.remove('is-visible');
        document.body.style.overflow = '';
    }
};

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', function() {
    ConfirmModal.init();
});
