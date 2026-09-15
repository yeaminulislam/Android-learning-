/* Shield Browser — media event bridge.
 * Reports HTML5 media play/pause/ended events back to the app so the
 * foreground service can keep audio alive in the background. */
(function () {
    if (window.__shieldMediaBridge) return;
    window.__shieldMediaBridge = true;

    function hook(el) {
        if (el.__shieldHooked) return;
        el.__shieldHooked = true;
        el.addEventListener('play', function () {
            try { ShieldMedia.onMediaPlay(); } catch (e) {}
        }, true);
        el.addEventListener('pause', function () {
            try { ShieldMedia.onMediaPause(); } catch (e) {}
        }, true);
        el.addEventListener('ended', function () {
            try { ShieldMedia.onMediaEnded(); } catch (e) {}
        }, true);
    }

    function scan() {
        var els = document.querySelectorAll('video,audio');
        for (var i = 0; i < els.length; i++) hook(els[i]);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', scan);
    } else {
        scan();
    }
    // catch late/SPA inserted media elements too
    setInterval(scan, 2500);
})();
