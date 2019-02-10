importScripts('https://oduwsdl.github.io/Reconstructive/reconstructive.js');

const rc = new Reconstructive({
    debug: false,
    showBanner: true,
    urimPattern: `${self.location.origin}/replay/<datetime>/<urir>`,
    bannerElementLocation: 'https://oduwsdl.github.io/Reconstructive/reconstructive-banner.js',
});
rc.exclusions.specialEndpint = function (event, config) {
    return event.request.url.startsWith(self.location.origin + '/__jwarc__/');
};
self.addEventListener('fetch', function (event) {
    rc.reroute(event);
});
