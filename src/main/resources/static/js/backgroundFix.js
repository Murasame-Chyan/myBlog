// 视差背景：延迟加载高清 WebP，避免阻塞首屏渲染
(function() {
    var HD_URL = '/pics/background.webp';

    function applyBackground() {
        var img = new Image();
        img.onload = function() {
            var vw = window.innerWidth;
            var imgW = img.naturalWidth;
            var imgH = img.naturalHeight;
            var renderedHeight = vw * (imgH / imgW);
            document.body.style.backgroundImage = 'url(' + HD_URL + ')';

            function updateBg() {
                var vh = window.innerHeight;
                var scrollY = window.pageYOffset;
                var bgY = -scrollY;
                if (bgY + renderedHeight < vh) {
                    bgY = vh - renderedHeight;
                }
                document.body.style.backgroundPositionY = bgY + 'px';
            }
            window.addEventListener('scroll', updateBg, {passive: true});
            window.addEventListener('resize', function() {
                vw = window.innerWidth;
                renderedHeight = vw * (imgH / imgW);
                updateBg();
            }, {passive: true});
            updateBg();
        };
        img.onerror = function() {
            document.body.style.backgroundImage = 'url(' + HD_URL + ')';
        };
        img.src = HD_URL;
    }

    // 延迟加载背景图：等页面首屏渲染完毕后再开始预加载高清图
    if (window.requestIdleCallback) {
        requestIdleCallback(applyBackground, { timeout: 3000 });
    } else {
        setTimeout(applyBackground, 500);
    }
})();
