/* Badge holographic card preview modal.
 *
 * Ported from SparkingCard demo (spring-physics pointer interaction).
 * Adapted to (a) live inside a modal that opens/closes, (b) drive multiple
 * badges off a single registry, (c) attach pointer listeners only while
 * the modal is open so a hidden card costs nothing.
 *
 * Asset convention:
 *   icon:      /images/badge/icon/{badgeId}-icon.jpg
 *   full card: /images/badge/fullbadge/{badgeId}-full-{n}.jpg  (n = 1..count) */

(function () {
    "use strict";

    // Badge registry. Loaded from /sign-in/achievements API on page load.
    // Fallback to static data if API is unavailable.
    const BADGES = {
        "re-zero": {
            name: "Re:0 第四季 纪念徽章",
            count: 1
        }
    };
    window.BADGES = BADGES;

    // Try to load from backend API
    fetch("/sign-in/achievements")
        .then(function (r) { return r.json(); })
        .then(function (d) {
            if (d.code === 200 && d.data && d.data.achievements) {
                window.BADGES_BY_ID = {};
                d.data.achievements.forEach(function (a) {
                    // Use achievement name as badgeId (kebab-cased)
                    var badgeId = a.name.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
                    if (!BADGES[badgeId]) {
                        BADGES[badgeId] = {
                            id: a.id,
                            name: a.name,
                            count: 1
                        };
                    }
                    // Also index by numeric ID for Thymeleaf data-badge-id lookups
                    window.BADGES_BY_ID[a.id] = BADGES[badgeId];
                });
                window.BADGES = BADGES;
            }
        })
        .catch(function () {});

    // Path helpers — encode the naming convention in one place.
    function getIconPath(badgeId) {
        return "/images/badge/icon/" + badgeId + "-icon.jpg";
    }
    function getFullPath(badgeId, n) {
        return "/images/badge/fullbadge/" + badgeId + "-full-" + n + ".jpg";
    }

    const modal = document.getElementById("badgeModal");
    if (!modal) return;

    const cardImg = document.getElementById("badgeCardImg");
    const nameEl = document.getElementById("badgeModalName");
    const closeBtn = modal.querySelector(".badge-modal__close");
    const backdrop = modal.querySelector(".badge-modal__backdrop");
    const card = modal.querySelector(".card");
    const rotator = modal.querySelector(".card__rotator");

    // ---------- Spring physics state ----------
    // Three independent springs: 3D rotation, shine background pan, pointer
    // position + effect intensity. Each animates toward a target the pointer
    // sets, with different stiffness/damping depending on "interacting" vs
    // "returning to rest".
    const rotationSpring = createSpring({ x: 0, y: 0 });
    const backgroundSpring = createSpring({ x: 50, y: 50 });
    const pointerSpring = createSpring({ x: 50, y: 50, effectIntensity: 0 });
    const springs = [rotationSpring, backgroundSpring, pointerSpring];

    const springInteractSettings = { stiffness: 0.066, damping: 0.25 };
    const springReturnSettings = { stiffness: 0.01, damping: 0.06 };
    let springSettings = springInteractSettings;

    let frameId = null;
    let lastTimestamp = 0;
    let resetTimer = null;
    let lastTrigger = null;

    // ---------- Wire up thumbs ----------
    document.querySelectorAll(".profile-badge-thumb").forEach(function (thumb) {
        thumb.addEventListener("click", function () {
            openBadgeModal(thumb.dataset.badgeId, thumb);
        });
    });

    // ---------- Modal open/close ----------
    function openBadgeModal(badgeId, triggerEl) {
        var badge = BADGES[badgeId] || (window.BADGES_BY_ID && window.BADGES_BY_ID[badgeId]);
        if (!badge) return;

        lastTrigger = triggerEl || null;
        // Full card image derived from convention: {badgeId}-full-1.jpg
        cardImg.src = getFullPath(badgeId, 1);
        cardImg.alt = badge.name;
        if (nameEl) nameEl.textContent = badge.name;

        modal.hidden = false;
        modal.setAttribute("aria-hidden", "false");
        document.body.classList.add("badge-modal-open");

        card.addEventListener("pointermove", handlePointerMove);
        card.addEventListener("pointerleave", handlePointerLeave);
        card.addEventListener("pointercancel", handlePointerLeave);
        document.addEventListener("keydown", handleKey);

        if (closeBtn) closeBtn.focus();
    }

    function closeBadgeModal() {
        modal.hidden = true;
        modal.setAttribute("aria-hidden", "true");
        document.body.classList.remove("badge-modal-open");

        card.removeEventListener("pointermove", handlePointerMove);
        card.removeEventListener("pointerleave", handlePointerLeave);
        card.removeEventListener("pointercancel", handlePointerLeave);
        document.removeEventListener("keydown", handleKey);

        // Reset spring state so the next open starts cleanly at neutral.
        clearTimeout(resetTimer);
        resetTimer = null;
        if (frameId !== null) {
            cancelAnimationFrame(frameId);
            frameId = null;
        }
        lastTimestamp = 0;
        rotationSpring.current = { x: 0, y: 0 };
        rotationSpring.target = { x: 0, y: 0 };
        backgroundSpring.current = { x: 50, y: 50 };
        backgroundSpring.target = { x: 50, y: 50 };
        pointerSpring.current = { x: 50, y: 50, effectIntensity: 0 };
        pointerSpring.target = { x: 50, y: 50, effectIntensity: 0 };
        springs.forEach(resetSpringVelocity);
        applyVisualState();

        if (lastTrigger) lastTrigger.focus();
        lastTrigger = null;
    }

    function handleKey(e) {
        if (e.key === "Escape") closeBadgeModal();
    }

    if (backdrop) backdrop.addEventListener("click", closeBadgeModal);
    if (closeBtn) closeBtn.addEventListener("click", closeBadgeModal);

    // ---------- Visual writes ----------
    // Each setter writes the spring's current value into a CSS custom property
    // on the rotator. CSS does the rest (transform, gradient positions, opacity).
    function setRotation(v) {
        rotator.style.setProperty("--tilt-left-right", v.x + "deg");
        rotator.style.setProperty("--tilt-up-down", v.y + "deg");
    }
    function setShineBackground(v) {
        rotator.style.setProperty("--background-x", v.x + "%");
        rotator.style.setProperty("--background-y", v.y + "%");
    }
    function setPointer(v) {
        rotator.style.setProperty("--pointer-x", v.x + "%");
        rotator.style.setProperty("--pointer-y", v.y + "%");
        rotator.style.setProperty("--pointer-from-center", distanceFromCenter(v.x, v.y));
        rotator.style.setProperty("--effect-intensity", v.effectIntensity);
    }
    function distanceFromCenter(x, y) {
        return round(clamp(Math.hypot(x - 50, y - 50) / 50, 0, 1));
    }
    function applyVisualState() {
        setRotation({
            x: round(rotationSpring.current.x),
            y: round(rotationSpring.current.y)
        });
        setShineBackground({
            x: round(backgroundSpring.current.x),
            y: round(backgroundSpring.current.y)
        });
        setPointer({
            x: round(pointerSpring.current.x),
            y: round(pointerSpring.current.y),
            effectIntensity: round(pointerSpring.current.effectIntensity)
        });
    }

    // ---------- Animation loop ----------
    // requestAnimationFrame drives all three springs together. We stop the
    // loop the moment every spring is at rest to avoid burning a frame per
    // tick forever.
    function animateCard(timestamp) {
        if (!lastTimestamp) lastTimestamp = timestamp;
        const deltaTime = Math.min((timestamp - lastTimestamp) / 16.666, 4);
        lastTimestamp = timestamp;

        springs.forEach(function (s) { updateSpring(s, deltaTime); });

        if (springs.every(isCloseToTarget)) {
            springs.forEach(finishSpringAtTarget);
            applyVisualState();
            frameId = null;
            lastTimestamp = 0;
            return;
        }
        applyVisualState();
        frameId = requestAnimationFrame(animateCard);
    }
    function startAnimation() {
        if (frameId === null) frameId = requestAnimationFrame(animateCard);
    }

    // ---------- Pointer handlers ----------
    function handlePointerMove(event) {
        clearTimeout(resetTimer);
        resetTimer = null;
        springSettings = springInteractSettings;

        const rect = card.getBoundingClientRect();
        const px = round(clamp(((event.clientX - rect.left) / rect.width) * 100));
        const py = round(clamp(((event.clientY - rect.top) / rect.height) * 100));
        const cx = px - 50;
        const cy = py - 50;

        // 50% offset → ~14deg tilt, matching the demo's feel.
        setSpringTarget(rotationSpring, { x: round(-(cx / 3.5)), y: round(cy / 3.5) });
        setSpringTarget(backgroundSpring, {
            x: mapRange(px, 0, 100, 37, 63),
            y: mapRange(py, 0, 100, 33, 67)
        });
        setSpringTarget(pointerSpring, { x: px, y: py, effectIntensity: 1 });

        startAnimation();
    }

    function handlePointerLeave() {
        // 500ms delay before returning to rest — feels less twitchy if the
        // pointer briefly leaves the card while the user is moving.
        clearTimeout(resetTimer);
        resetTimer = setTimeout(function () {
            springSettings = springReturnSettings;
            setSpringTarget(rotationSpring, { x: 0, y: 0 });
            setSpringTarget(backgroundSpring, { x: 50, y: 50 });
            setSpringTarget(pointerSpring, { x: 50, y: 50, effectIntensity: 0 });
            resetTimer = null;
            startAnimation();
        }, 500);
    }

    // ---------- Utilities ----------
    function mapRange(v, fromMin, fromMax, toMin, toMax) {
        return round(toMin + ((v - fromMin) / (fromMax - fromMin)) * (toMax - toMin));
    }
    function clamp(v, min, max) {
        if (min === undefined) min = 0;
        if (max === undefined) max = 100;
        return Math.min(Math.max(v, min), max);
    }
    function round(v, p) {
        if (p === undefined) p = 3;
        return Number(v.toFixed(p));
    }

    // Custom spring solver: explicit Euler integration of a damped spring
    // with per-axis state. Matches the demo's tuning.
    function createSpring(initial) {
        const axes = Object.keys(initial);
        return {
            axes: axes,
            current: Object.assign({}, initial),
            target: Object.assign({}, initial),
            velocity: Object.fromEntries(axes.map(function (a) { return [a, 0]; }))
        };
    }
    function setSpringTarget(s, v) { Object.assign(s.target, v); }
    function resetSpringVelocity(s) {
        s.axes.forEach(function (a) { s.velocity[a] = 0; });
    }
    function updateSpring(s, dt) {
        s.axes.forEach(function (a) {
            const d = s.target[a] - s.current[a];
            s.velocity[a] += d * springSettings.stiffness * dt;
            s.velocity[a] *= Math.pow(1 - springSettings.damping, dt);
            s.current[a] += s.velocity[a] * dt;
        });
    }
    const STOP_THRESHOLD = 0.001;
    function isCloseToTarget(s) {
        return s.axes.every(function (a) {
            return Math.abs(s.target[a] - s.current[a]) < STOP_THRESHOLD
                && Math.abs(s.velocity[a]) < STOP_THRESHOLD;
        });
    }
    function finishSpringAtTarget(s) {
        s.current = Object.assign({}, s.target);
        resetSpringVelocity(s);
    }

    // Expose for external pages (e.g., honors page)
    window.openBadgeModal = openBadgeModal;
    window.closeBadgeModal = closeBadgeModal;
})();
