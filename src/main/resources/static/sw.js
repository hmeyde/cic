// Service worker minimal : rend l'application installable et affiche une page hors ligne.
// Volontairement, aucune page HTML n'est mise en cache : elles contiennent des données personnelles.
var CACHE = "seheli-statique-v3";
var HORS_LIGNE = "/hors-ligne.html";
var STATIQUES = [HORS_LIGNE, "/css/app.css", "/img/icone.svg", "/img/icone-192.png",
    "/img/logo/seheli-logo.svg", "/img/logo/seheli-logo-arabe.svg", "/img/logo/seheli-symbole.svg"];

self.addEventListener("install", function (event) {
    event.waitUntil(caches.open(CACHE).then(function (cache) { return cache.addAll(STATIQUES); }));
    self.skipWaiting();
});

self.addEventListener("activate", function (event) {
    event.waitUntil(caches.keys().then(function (cles) {
        return Promise.all(cles.filter(function (cle) { return cle !== CACHE; })
            .map(function (cle) { return caches.delete(cle); }));
    }).then(function () { return self.clients.claim(); }));
});

self.addEventListener("fetch", function (event) {
    var requete = event.request;
    if (requete.method !== "GET") {
        return;
    }
    if (requete.mode === "navigate") {
        event.respondWith(fetch(requete).catch(function () { return caches.match(HORS_LIGNE); }));
        return;
    }
    var url = new URL(requete.url);
    if (url.origin === self.location.origin && STATIQUES.indexOf(url.pathname) !== -1) {
        event.respondWith(fetch(requete).catch(function () { return caches.match(requete); }));
    }
});
