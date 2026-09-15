/* Shield Browser — YouTube ad skipper & cleaner.
 * - removes ad slots / promo cards from feed and search
 * - mutes + fast-forwards in-stream video ads and auto-clicks "Skip"
 * - strips ad payloads from YouTube's own player configs
 * - fights the "ad blockers violate YouTube's terms" enforcement popup */
(function () {
    if (window.__shieldYt) return;
    window.__shieldYt = true;

    var AD_CSS =
        'ytd-ad-slot-renderer,ytd-display-ad-renderer,ytd-promoted-video-renderer,' +
        'ytd-compact-promoted-video-renderer,ytd-banner-promo-renderer,' +
        'ytd-statement-banner-renderer,ytd-in-feed-ad-layout-renderer,' +
        'ytd-promoted-sparkles-web-renderer,ytd-promoted-sparkles-text-search-renderer,' +
        'yt-mealbar-promo-renderer,#masthead-ad,#player-ads,.ytp-ad-overlay-container,' +
        '.sparkles-light-cta,.ytd-compact-promoted-item-renderer' +
        '{display:none!important;}';

    var KILL_SELECTORS =
        'ytd-display-ad-renderer, ytd-promoted-video-renderer, ' +
        'ytd-compact-promoted-video-renderer, ytd-banner-promo-renderer, ' +
        'ytd-statement-banner-renderer, ytd-in-feed-ad-layout-renderer, ' +
        'ytd-promoted-sparkles-web-renderer, ytd-ad-slot-renderer, ' +
        'yt-mealbar-promo-renderer, ytd-compact-promoted-item-renderer';

    function addCss() {
        if (document.getElementById('__shield_yt_css')) return;
        var s = document.createElement('style');
        s.id = '__shield_yt_css';
        s.textContent = AD_CSS;
        var parent = document.head || document.documentElement;
        if (parent) parent.appendChild(s);
    }

    /* Remove ad payloads from parsed player responses. */
    function clean(obj) {
        try {
            if (obj && typeof obj === 'object') {
                if ('adPlacements' in obj) delete obj.adPlacements;
                if ('adSlots' in obj) delete obj.adSlots;
                if ('playerAds' in obj) delete obj.playerAds;
                if ('adBreakHeartbeatParams' in obj) delete obj.adBreakHeartbeatParams;
                if ('adBreakParams' in obj) delete obj.adBreakParams;
            }
        } catch (e) {}
        return obj;
    }

    try {
        var origParse = JSON.parse;
        JSON.parse = function () {
            var r = origParse.apply(this, arguments);
            return clean(r);
        };
    } catch (e) {}

    function fastForwardAds() {
        var adShowing = document.querySelector(
            '.ad-showing, .ytp-ad-player-overlay, .ytp-ad-player-overlay-instream-info'
        );
        if (!adShowing) return;
        var vids = document.querySelectorAll('video');
        for (var i = 0; i < vids.length; i++) {
            var v = vids[i];
            try {
                if (isFinite(v.duration) && v.duration > 0) {
                    v.muted = true;
                    v.playbackRate = 16;
                    if (v.currentTime < v.duration - 0.1) {
                        v.currentTime = v.duration;
                    }
                }
            } catch (e) {}
        }
    }

    function clickSkips() {
        var skip = document.querySelector(
            '.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, ' +
            '.ytp-ad-skip-button-slot button, button.ytp-ad-skip-button'
        );
        if (skip) { try { skip.click(); } catch (e) {} }
        var close = document.querySelector('.ytp-ad-overlay-close-button');
        if (close) { try { close.click(); } catch (e) {} }
    }

    function removeAdNodes() {
        var nodes = document.querySelectorAll(KILL_SELECTORS);
        for (var j = 0; j < nodes.length; j++) {
            try { nodes[j].remove(); } catch (e) {}
        }
    }

    /* Remove YouTube's anti-adblock enforcement popup and resume playback. */
    function fightEnforcement() {
        var dialogs = document.querySelectorAll(
            'ytd-enforcement-message-view-model, tp-yt-paper-dialog, ytd-popup-container'
        );
        for (var d = 0; d < dialogs.length; d++) {
            var t = dialogs[d].textContent || '';
            if (/ad blocker|adblock|ads violate|বিজ্ঞাপন ব্লকার/i.test(t)) {
                try { dialogs[d].remove(); } catch (e) {}
            }
        }
        var backdrop = document.querySelector('tp-yt-iron-overlay-backdrop.opened');
        if (backdrop) { try { backdrop.className = ''; backdrop.remove(); } catch (e) {} }

        var vids = document.querySelectorAll('video');
        for (var k = 0; k < vids.length; k++) {
            try {
                if (vids[k].paused && vids[k].currentTime > 0) {
                    var p = vids[k].play();
                    if (p && p.catch) p.catch(function () {});
                }
            } catch (e) {}
        }
    }

    function tick() {
        addCss();
        fastForwardAds();
        clickSkips();
        removeAdNodes();
        fightEnforcement();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', addCss);
    } else {
        addCss();
    }
    setInterval(tick, 800);
})();
