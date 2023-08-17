self.addEventListener('install', function (event) {
    console.log('[recorder-sw.js] Installed');
    event.waitUntil(self.skipWaiting());
});

self.addEventListener('activate', function (event) {
    console.log('[recorder-sw.js] Activated');
    event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', function (event) {
        const request = event.request;
        console.log('[recorder-sw.js] Fetch intercepted for:', request.url);

        let target = request.url;
        const url = new URL(target);
        if (url.origin === self.location.origin) {
            if (url.pathname === "/" || url.pathname.startsWith("/__jwarc__/")) {
                return;
            }
            const referrerLiveUrl = request.referrer.replace(/.*\/jwarcrecorder\/record\//, "");
            target = new URL(url.pathname + url.search, referrerLiveUrl).toString();
        }

        console.log("[recorder-sw.js] Fetching " + target);
        const newUrl = "/__jwarc__/record/" + target;
        const newRequest = new Request(newUrl, {
            method: request.method,
            headers: request.headers,
            mode: request.mode,
            credentials: request.credentials,
            redirect: request.redirect,
            referrer: request.referrer,
            body: request.body,
        });
        event.respondWith(fetch(newRequest));
    }
);