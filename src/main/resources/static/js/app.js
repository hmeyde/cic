// Ajoute le jeton CSRF à toutes les requêtes HTMX (lu dans les balises <meta> du layout).
document.addEventListener("htmx:configRequest", function (event) {
    var token = document.querySelector('meta[name="_csrf"]');
    var header = document.querySelector('meta[name="_csrf_header"]');
    if (token && header) {
        event.detail.headers[header.content] = token.content;
    }
});

// Service worker : nécessaire pour l'installation de l'application (PWA) et la page hors ligne.
if ("serviceWorker" in navigator) {
    window.addEventListener("load", function () {
        navigator.serviceWorker.register("/sw.js", { scope: "/" });
    });
}
