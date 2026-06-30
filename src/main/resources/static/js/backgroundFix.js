// 双主题视差背景
(function() {
    var DARK_URL = '/pics/background-2.jpg';
    var LIGHT_URL = '/pics/background-1.jpg';
    var loadedDark = null;
    var loadedLight = null;

    function getTheme() {
        return document.documentElement.getAttribute('data-theme') || 'dark';
    }

    function preload(url, callback) {
        var img = new Image();
        img.onload = function() { callback(img); };
        img.onerror = function() { callback(null); };
        img.src = url;
    }

    function applyBg(img) {
        var vw = window.innerWidth;
        var imgW = img.naturalWidth;
        var imgH = img.naturalHeight;
        var renderedHeight = vw * (imgH / imgW);

        function updateBg() {
            var vh = window.innerHeight;
            var scrollY = window.pageYOffset;
            var bgY = -scrollY;
            if (bgY + renderedHeight < vh) {
                bgY = vh - renderedHeight;
            }
            document.body.style.backgroundPositionY = bgY + 'px';
        }

        document.body.style.backgroundImage = 'url(' + img.src + ')';
        window.addEventListener('scroll', updateBg, {passive: true});
        window.addEventListener('resize', function() {
            vw = window.innerWidth;
            renderedHeight = vw * (imgH / imgW);
            updateBg();
        }, {passive: true});
        updateBg();
    }

    function switchBackground() {
        var theme = getTheme();
        if (theme === 'light' && loadedLight) {
            applyBg(loadedLight);
        } else if (loadedDark) {
            applyBg(loadedDark);
        }
    }

    function init() {
        preload(DARK_URL, function(img) {
            loadedDark = img;
            if (getTheme() !== 'light' || !loadedLight) applyBg(img);
        });
        preload(LIGHT_URL, function(img) {
            loadedLight = img;
            if (getTheme() === 'light') applyBg(img);
        });

        // 监听主题切换，实时更新背景
        var observer = new MutationObserver(function() {
            switchBackground();
        });
        observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
    }

    if (window.requestIdleCallback) {
        requestIdleCallback(init, { timeout: 3000 });
    } else {
        setTimeout(init, 500);
    }
})();
