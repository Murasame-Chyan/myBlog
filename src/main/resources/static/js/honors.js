/* Honors page — click/keyboard handlers for owned honor cards.
 * Delegates to window.openBadgeModal exposed by badge-card.js. */
(function () {
    "use strict";

    document.querySelectorAll('.honor-card--owned').forEach(function (card) {
        card.addEventListener('click', function () {
            var achId = card.dataset.achievementId;
            if (achId && window.openBadgeModal) {
                window.openBadgeModal(achId, card);
            }
        });

        // Keyboard: Enter/Space to open
        card.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                card.click();
            }
        });
    });
})();
