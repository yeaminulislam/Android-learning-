/* Shield Browser — cosmetic filter keeper.
 * Expects window.__shieldCss to be set by the app (see JsInjector).
 * Applies the element-hiding CSS and re-applies it if the site
 * removes our <style> tag (SPA navigations, anti-adblock scripts). */
try {
    var __shieldCssText = window.__shieldCss || "";

    function __shieldApplyCosmetic() {
        var el = document.getElementById('__shield_cosmetic');
        if (!el) {
            el = document.createElement('style');
            el.id = '__shield_cosmetic';
            var parent = document.head || document.documentElement;
            if (parent) parent.appendChild(el);
        }
        if (el && el.textContent !== __shieldCssText) {
            el.textContent = __shieldCssText;
        }
    }

    __shieldApplyCosmetic();

    if (!window.__shieldCosmoKeeper) {
        window.__shieldCosmoKeeper = true;
        setInterval(__shieldApplyCosmetic, 3000);
    }
} catch (e) {}
