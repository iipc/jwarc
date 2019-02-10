let reload = !navigator.serviceWorker.controller;
navigator.serviceWorker.register('/__jwarc__/sw.js', {scope: '/replay/'}).then(function (registration) {
    registration.update();
    if (reload) {
        window.location.reload();
    }
});

window.addEventListener("load", function() {
    var anchors = document.getElementsByTagName("a");
    var prefix = /(\/replay\/[0-9]+\/https?:\/\/[^\/]*)\/.*/.exec(location.pathname)[1];
    for (var i = 0; i < anchors.length; i++) {
        var a = anchors[i];
        if (a.href.startsWith(location.origin + '/') && !a.href.startsWith(location.origin + '/replay/')) {
            var url = new URL(a.href);
            url.pathname = prefix + url.pathname;
            a.href = url.toString();
        }
    }
});