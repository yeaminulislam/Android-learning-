/* Shield Browser — background playback unlocker.
 * Makes pages believe they are always in the foreground, so sites like
 * YouTube keep playing audio/video when the app is minimized or the
 * screen is turned off. */
(function () {
    if (window.__shieldVisibility) return;
    window.__shieldVisibility = true;

    try {
        Object.defineProperty(document, 'hidden', {
            get: function () { return false; }
        });
        Object.defineProperty(document, 'webkitHidden', {
            get: function () { return false; }
        });
        Object.defineProperty(document, 'visibilityState', {
            get: function () { return 'visible'; }
        });
        Object.defineProperty(document, 'webkitVisibilityState', {
            get: function () { return 'visible'; }
        });
    } catch (e) {}

    try {
        document.hasFocus = function () { return true; };
    } catch (e) {}

    // Swallow visibilitychange listeners registered after this script runs.
    if (!document.__shieldPatchedAdd) {
        document.__shieldPatchedAdd = true;
        var origAdd = document.addEventListener;
        document.addEventListener = function (type, listener, options) {
            if (type === 'visibilitychange' || type === 'webkitvisibilitychange') {
                return; // silently drop: the page never learns it was hidden
            }
            return origAdd.call(this, type, listener, options);
        };
    }
    try {
        Object.defineProperty(document, 'onvisibilitychange', {
            get: function () { return null; },
            set: function () { /* dropped */ }
        });
    } catch (e) {}

    // Pause-on-window-blur tricks: block blur delivery.
    window.addEventListener('blur', function (e) {
        e.stopImmediatePropagation();
    }, true);

    // Nudge players that already paused when the tab went to background.
    setTimeout(function () {
        try {
            document.dispatchEvent(new Event('visibilitychange'));
            window.dispatchEvent(new Event('focus'));
        } catch (e) {}
    }, 400);
})();
