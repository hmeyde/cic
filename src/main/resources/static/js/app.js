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

// ---------------------------------------------------------------------------------------------
// Parcours de demande : formulaires découpés en questions ([data-sous-etapes] > fieldset[data-sous-etape]).
// Une seule question visible ; « Continuer » vérifie les champs de la question et affiche l'erreur à
// côté du champ ; « Retour » (ou le retour arrière du navigateur) revient à la précédente sans rien
// perdre : tout reste dans le même formulaire. Sans JavaScript, toutes les questions sont affichées.
// ---------------------------------------------------------------------------------------------
(function () {
    "use strict";

    function format(modele) {
        var args = Array.prototype.slice.call(arguments, 1);
        return (modele || "").replace(/\{(\d+)\}/g, function (tout, i) {
            return args[i] !== undefined ? args[i] : tout;
        });
    }

    // Affiche (ou retire, si message est vide) l'erreur posée par le navigateur à côté d'un champ.
    function erreurChamp(champ, message) {
        var id = champ.id + "-erreur";
        var p = document.getElementById(id);
        if (!message) {
            if (p && p.hasAttribute("data-erreur-js")) {
                p.remove();
                champ.removeAttribute("aria-invalid");
                retirerDescription(champ, id);
            }
            return;
        }
        if (!p) {
            p = document.createElement("p");
            p.id = id;
            p.className = "champ__erreur";
            p.setAttribute("data-erreur-js", "");
            var conteneur = champ.closest(".champ") || champ.parentNode;
            if (champ.type === "checkbox") {
                p.classList.add("champ__erreur--case");
            }
            conteneur.appendChild(p);
        }
        p.hidden = false;
        p.textContent = message;
        champ.setAttribute("aria-invalid", "true");
        var decrit = (champ.getAttribute("aria-describedby") || "").split(" ").filter(Boolean);
        if (decrit.indexOf(id) < 0) {
            decrit.push(id);
            champ.setAttribute("aria-describedby", decrit.join(" "));
        }
    }

    function retirerDescription(champ, id) {
        var decrit = (champ.getAttribute("aria-describedby") || "").split(" ").filter(function (x) {
            return x && x !== id;
        });
        if (decrit.length) {
            champ.setAttribute("aria-describedby", decrit.join(" "));
        } else {
            champ.removeAttribute("aria-describedby");
        }
    }

    function messagePour(champ, form) {
        var v = champ.validity;
        if (v.valueMissing) {
            return champ.getAttribute("data-texte-requis") || form.getAttribute("data-texte-requis")
                || champ.validationMessage;
        }
        return champ.getAttribute("data-texte-format") || champ.validationMessage;
    }

    // Vérifie les champs d'une question ; renvoie le premier champ invalide, ou null.
    function verifier(bloc, form) {
        var premier = null;
        bloc.querySelectorAll("input, select, textarea").forEach(function (champ) {
            if (champ.type === "hidden" || champ.disabled || !champ.willValidate) {
                return;
            }
            if (champ.checkValidity()) {
                erreurChamp(champ, "");
            } else {
                erreurChamp(champ, messagePour(champ, form));
                premier = premier || champ;
            }
        });
        return premier;
    }

    function initialiserSousEtapes(form) {
        var blocs = Array.prototype.slice.call(form.querySelectorAll("[data-sous-etape]"));
        if (blocs.length < 2) {
            return;
        }
        var compteur = form.querySelector("[data-sous-etapes-compteur]");
        var courant = 0;
        var cle = "sousEtape:" + location.pathname;

        blocs.forEach(function (bloc, i) {
            var question = bloc.querySelector(".sous-etape__question");
            if (question) {
                question.setAttribute("tabindex", "-1");
            }
            if (i === blocs.length - 1) {
                return;
            }
            var actions = document.createElement("div");
            actions.className = "actions-formulaire sous-etape__actions";
            var continuer = document.createElement("button");
            continuer.type = "button";
            continuer.className = "bouton bouton--principal";
            continuer.textContent = form.getAttribute("data-texte-continuer");
            continuer.addEventListener("click", function () { avancer(); });
            actions.appendChild(continuer);
            bloc.appendChild(actions);
        });
        blocs.forEach(function (bloc, i) {
            if (i === 0) {
                return;
            }
            var retour = document.createElement("button");
            retour.type = "button";
            retour.className = "bouton bouton--contour sous-etape__retour";
            retour.textContent = form.getAttribute("data-texte-retour");
            retour.addEventListener("click", reculer);
            var actions = bloc.querySelector(".sous-etape__actions") || bloc.querySelector("[data-sous-etapes-fin]");
            (actions || bloc).appendChild(retour);
        });

        function afficher(i, focaliser) {
            courant = Math.max(0, Math.min(i, blocs.length - 1));
            blocs.forEach(function (bloc, j) { bloc.hidden = j !== courant; });
            if (compteur) {
                compteur.hidden = false;
                compteur.textContent = format(form.getAttribute("data-texte-compteur"), courant + 1, blocs.length);
            }
            if (focaliser) {
                var question = blocs[courant].querySelector(".sous-etape__question");
                (question || blocs[courant]).focus({ preventScroll: true });
                form.scrollIntoView({ block: "start" });
            }
        }

        function avancer() {
            var invalide = verifier(blocs[courant], form);
            if (invalide) {
                invalide.focus();
                return;
            }
            afficher(courant + 1, true);
            memoriser(true, true);
        }

        // Retour : on dépile l'historique si la question courante y a été empilée par « Continuer »,
        // sinon (question rouverte après une erreur du serveur) on recule sans quitter la page.
        function reculer() {
            var etat = history.state && history.state[cle];
            if (etat && etat.empilee && etat.i === courant) {
                history.back();
                return;
            }
            afficher(courant - 1, true);
            memoriser(false, false);
        }

        function memoriser(empilee, nouvelleEntree) {
            var etat = history.state && typeof history.state === "object" ? Object.assign({}, history.state) : {};
            etat[cle] = { i: courant, empilee: empilee };
            if (nouvelleEntree) {
                history.pushState(etat, "");
            } else {
                history.replaceState(etat, "");
            }
        }

        window.addEventListener("popstate", function (event) {
            var etat = event.state && event.state[cle];
            afficher(etat ? etat.i : 0, true);
        });

        // Entrée dans un champ : passe à la question suivante au lieu d'envoyer le formulaire.
        form.addEventListener("submit", function (event) {
            if (courant < blocs.length - 1) {
                event.preventDefault();
                avancer();
                return;
            }
            var invalide = null;
            for (var i = 0; i < blocs.length && !invalide; i++) {
                invalide = verifier(blocs[i], form);
                if (invalide) {
                    event.preventDefault();
                    if (i !== courant) {
                        afficher(i, false);
                    }
                    invalide.focus();
                }
            }
        });

        // Erreur renvoyée par le serveur : on rouvre la première question concernée.
        var depart = blocs.findIndex(function (bloc) {
            return bloc.querySelector('[aria-invalid="true"]');
        });
        if (depart < 0) {
            var memorise = history.state && history.state[cle];
            depart = memorise ? memorise.i : 0;
        }
        afficher(depart, false);
        memoriser(Boolean(memorise && memorise.empilee && memorise.i === courant), false);
        // Un champ corrigé perd son message d'erreur.
        form.addEventListener("input", function (event) {
            if (event.target.checkValidity && event.target.checkValidity()) {
                erreurChamp(event.target, "");
            }
        });
    }

    // Brouillon gardé dans l'onglet (sessionStorage) : quitter la page puis y revenir ne perd pas la
    // saisie. Restauré seulement sur un formulaire vierge (pas après une erreur du serveur ni sur des
    // données déjà enregistrées) ; effacé à l'envoi.
    function initialiserBrouillon(form) {
        var cle = "brouillon:" + form.getAttribute("data-brouillon");
        var champs = Array.prototype.slice.call(form.elements).filter(function (c) {
            return c.name && c.name !== "_csrf" && c.type !== "hidden" && c.type !== "file"
                && c.type !== "submit" && c.type !== "button";
        });
        var stockage;
        try {
            stockage = window.sessionStorage;
        } catch (e) {
            return;
        }
        var vierge = champs.every(function (c) {
            return c.type === "checkbox" ? true : !c.value;
        }) && !form.querySelector('[aria-invalid="true"]');
        var brouillon = null;
        try {
            brouillon = JSON.parse(stockage.getItem(cle) || "null");
        } catch (e) {
            brouillon = null;
        }
        if (vierge && brouillon) {
            champs.forEach(function (c) {
                if (!(c.name in brouillon)) {
                    return;
                }
                if (c.type === "checkbox") {
                    c.checked = brouillon[c.name] === true;
                } else {
                    c.value = brouillon[c.name];
                }
            });
        }
        function enregistrer() {
            var donnees = {};
            champs.forEach(function (c) {
                donnees[c.name] = c.type === "checkbox" ? c.checked : c.value;
            });
            try {
                stockage.setItem(cle, JSON.stringify(donnees));
            } catch (e) {
                // stockage plein ou interdit : sans brouillon.
            }
        }
        form.addEventListener("input", enregistrer);
        form.addEventListener("change", enregistrer);
        form.addEventListener("submit", function (event) {
            // Après les vérifications des sous-étapes (écouteur ajouté avant) : envoi réel seulement.
            setTimeout(function () {
                if (!event.defaultPrevented) {
                    stockage.removeItem(cle);
                }
            }, 0);
        });
    }

    // Zone de dépôt d'un justificatif : choix, glisser-déposer, photo, aperçu et vérifications.
    function initialiserDepot(depot) {
        var form = depot.closest("form");
        var fichier = depot.querySelector("#fichier");
        var photo = depot.querySelector("#photo");
        var zone = depot.querySelector("[data-depot-zone]");
        var apercu = depot.querySelector("[data-depot-apercu]");
        var image = depot.querySelector("[data-depot-image]");
        var pdf = depot.querySelector("[data-depot-pdf]");
        var nom = depot.querySelector("[data-depot-nom]");
        var taille = depot.querySelector("[data-depot-taille]");
        var erreur = depot.querySelector("[data-depot-erreur]");
        var retirer = depot.querySelector("[data-depot-retirer]");
        var tailleMax = parseInt(depot.getAttribute("data-taille-max"), 10);
        var langue = document.documentElement.lang || "fr";
        var formats = ["application/pdf", "image/jpeg", "image/png"];

        function tailleLisible(octets) {
            var nf = new Intl.NumberFormat(langue, { maximumFractionDigits: 1 });
            return octets >= 1024 * 1024 ? nf.format(octets / (1024 * 1024)) + " Mo"
                : nf.format(Math.max(1, Math.round(octets / 1024))) + " Ko";
        }

        function formatAccepte(f) {
            if (formats.indexOf(f.type) >= 0) {
                return true;
            }
            return !f.type && /\.(pdf|jpe?g|png)$/i.test(f.name);
        }

        function afficherErreur(message) {
            erreur.textContent = message || "";
            erreur.hidden = !message;
            [fichier, photo].forEach(function (champ) {
                if (message) {
                    champ.setAttribute("aria-invalid", "true");
                } else {
                    champ.removeAttribute("aria-invalid");
                }
            });
            fichier.setAttribute("aria-describedby", message ? "fichier-aide fichier-erreur" : "fichier-aide");
            photo.setAttribute("aria-describedby", message ? "fichier-aide fichier-erreur" : "fichier-aide");
        }

        function vider() {
            fichier.value = "";
            photo.value = "";
            apercu.hidden = true;
            image.hidden = true;
            image.removeAttribute("src");
            pdf.hidden = true;
        }

        function choisi() {
            return (fichier.files && fichier.files[0]) || (photo.files && photo.files[0]) || null;
        }

        // Vérifie le fichier choisi ; s'il est refusé, il est retiré du formulaire.
        function examiner(source) {
            var autre = source === fichier ? photo : fichier;
            autre.value = "";
            var f = source.files && source.files[0];
            if (!f) {
                vider();
                return;
            }
            if (!formatAccepte(f)) {
                vider();
                afficherErreur(depot.getAttribute("data-texte-format"));
                return;
            }
            if (f.size > tailleMax) {
                vider();
                afficherErreur(format(depot.getAttribute("data-texte-trop-gros"), tailleLisible(f.size)));
                return;
            }
            afficherErreur("");
            nom.textContent = f.name;
            taille.textContent = tailleLisible(f.size);
            apercu.hidden = false;
            if (f.type.indexOf("image/") === 0) {
                pdf.hidden = true;
                // data: (et non blob:) : seul autorisé par la politique de sécurité du contenu.
                var lecteur = new FileReader();
                lecteur.onload = function () {
                    image.src = lecteur.result;
                    image.hidden = false;
                };
                lecteur.readAsDataURL(f);
            } else {
                image.hidden = true;
                pdf.hidden = false;
            }
        }

        fichier.addEventListener("change", function () { examiner(fichier); });
        photo.addEventListener("change", function () { examiner(photo); });
        retirer.addEventListener("click", function () {
            vider();
            afficherErreur("");
            zone.focus();
        });

        ["dragenter", "dragover"].forEach(function (type) {
            zone.addEventListener(type, function (event) {
                event.preventDefault();
                zone.classList.add("depot__zone--survol");
            });
        });
        ["dragleave", "drop"].forEach(function (type) {
            zone.addEventListener(type, function () { zone.classList.remove("depot__zone--survol"); });
        });
        zone.addEventListener("drop", function (event) {
            event.preventDefault();
            var fichiers = event.dataTransfer && event.dataTransfer.files;
            if (!fichiers || !fichiers.length) {
                return;
            }
            try {
                var transfert = new DataTransfer();
                transfert.items.add(fichiers[0]);
                fichier.files = transfert.files;
            } catch (e) {
                fichier.files = fichiers;
            }
            examiner(fichier);
        });
        // Un fichier lâché à côté de la zone ne doit pas ouvrir le fichier dans l'onglet.
        window.addEventListener("dragover", function (event) { event.preventDefault(); });
        window.addEventListener("drop", function (event) { event.preventDefault(); });

        // La zone (un label) s'active aussi au clavier.
        zone.setAttribute("tabindex", "0");
        zone.setAttribute("role", "button");
        zone.addEventListener("keydown", function (event) {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                fichier.click();
            }
        });

        form.addEventListener("submit", function (event) {
            var f = choisi();
            if (!f) {
                event.preventDefault();
                afficherErreur(depot.getAttribute("data-texte-requis"));
                zone.focus();
                return;
            }
            if (!formatAccepte(f) || f.size > tailleMax) {
                event.preventDefault();
                examiner(f === (fichier.files && fichier.files[0]) ? fichier : photo);
                zone.focus();
            }
        });
        depot.classList.add("depot--actif");
    }

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("form[data-sous-etapes]").forEach(initialiserSousEtapes);
        document.querySelectorAll("form[data-brouillon]").forEach(initialiserBrouillon);
        document.querySelectorAll("[data-depot]").forEach(initialiserDepot);
    });
})();
