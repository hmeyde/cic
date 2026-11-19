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
            // Chiffres latins en arabe aussi, comme les montants rendus par le serveur.
            var nf = new Intl.NumberFormat(langue === "ar" ? "ar-u-nu-latn" : langue, { maximumFractionDigits: 1 });
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
            fichier.focus();
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

        // Clavier : le champ fichier natif (visuellement caché) reçoit le focus et la zone en dessine l'anneau
        // (CSS :has). Pas de tabindex ni de role sur la zone : ce serait un second arrêt de tabulation
        // pour la même action, et role="button" est interdit sur un label.

        form.addEventListener("submit", function (event) {
            var f = choisi();
            if (!f) {
                event.preventDefault();
                afficherErreur(depot.getAttribute("data-texte-requis"));
                fichier.focus();
                return;
            }
            if (!formatAccepte(f) || f.size > tailleMax) {
                event.preventDefault();
                examiner(f === (fichier.files && fichier.files[0]) ? fichier : photo);
                fichier.focus();
            }
        });
        depot.classList.add("depot--actif");
    }

    // Code à usage unique : le champ unique (envoyé sous le nom « code ») devient 6 cases. Une case
    // accepte un chiffre puis passe à la suivante ; un collage ou le remplissage automatique du SMS
    // (autocomplete="one-time-code", qui peut verser les 6 chiffres dans une seule case) est réparti
    // sur les cases. Les chiffres arabes (٠-٩, ۰-۹) sont convertis. Le code complet est envoyé seul.
    var CHIFFRES_ORIENTAUX = /[٠-٩۰-۹]/g;

    function chiffres(texte) {
        return (texte || "").replace(CHIFFRES_ORIENTAUX, function (c) {
            var n = c.charCodeAt(0);
            return String(n >= 0x06F0 ? n - 0x06F0 : n - 0x0660);
        }).replace(/[^0-9]/g, "");
    }

    function initialiserCodeOtp(form) {
        var source = form.querySelector('input[name="code"]');
        if (!source) {
            return;
        }
        var longueur = parseInt(source.getAttribute("maxlength"), 10) || 6;
        var champ = source.closest(".champ");
        var invalide = source.getAttribute("aria-invalid") === "true";
        var decrit = source.getAttribute("aria-describedby") || "";
        var groupe = document.createElement("div");
        groupe.className = "code-otp";
        groupe.setAttribute("role", "group");
        groupe.setAttribute("aria-labelledby", "code-libelle");
        groupe.setAttribute("dir", "ltr");
        if (decrit) {
            groupe.setAttribute("aria-describedby", decrit);
        }
        var cases = [];
        for (var i = 0; i < longueur; i++) {
            var c = document.createElement("input");
            c.type = "text";
            c.className = "code-otp__case";
            c.setAttribute("inputmode", "numeric");
            c.setAttribute("pattern", "[0-9]*");
            c.setAttribute("autocomplete", i === 0 ? "one-time-code" : "off");
            c.setAttribute("spellcheck", "false");
            c.setAttribute("aria-label", format(form.getAttribute("data-texte-case"), i + 1));
            if (invalide) {
                c.setAttribute("aria-invalid", "true");
            }
            cases.push(c);
            groupe.appendChild(c);
        }
        // Le libellé « Code reçu par SMS » désigne la première case ; le champ d'origine devient caché.
        cases[0].id = source.id;
        source.removeAttribute("id");
        source.type = "hidden";
        source.required = false;
        source.removeAttribute("autofocus");
        source.value = "";
        champ.appendChild(groupe);

        var erreur = null;
        var envoye = false;

        function code() {
            return cases.map(function (c) { return c.value; }).join("");
        }

        function afficherErreur(message) {
            if (!erreur) {
                erreur = document.createElement("p");
                erreur.id = "code-erreur-saisie";
                erreur.className = "champ__erreur";
                erreur.setAttribute("role", "alert");
                champ.appendChild(erreur);
            }
            erreur.hidden = !message;
            erreur.textContent = message || "";
            cases.forEach(function (c) {
                if (message) {
                    c.setAttribute("aria-invalid", "true");
                } else {
                    c.removeAttribute("aria-invalid");
                }
            });
            groupe.setAttribute("aria-describedby", message ? decrit + " code-erreur-saisie" : decrit);
        }

        // Écrit les chiffres à partir de la case « depart » ; renvoie l'index de la case suivante.
        function repartir(texte, depart) {
            var n = chiffres(texte).split("");
            var i = depart;
            for (var k = 0; k < n.length && i < longueur; k++, i++) {
                cases[i].value = n[k];
            }
            return i;
        }

        function apresSaisie(suivante) {
            source.value = code();
            if (erreur && !erreur.hidden) {
                afficherErreur("");
            }
            if (source.value.length === longueur) {
                cases[longueur - 1].focus();
                envoyer();
            } else {
                cases[Math.min(suivante, longueur - 1)].focus();
            }
        }

        // Code complet : envoi automatique, une seule fois (chaque envoi compte comme un essai).
        function envoyer() {
            if (envoye) {
                return;
            }
            envoye = true;
            var bouton = form.querySelector('button[type="submit"]');
            if (bouton) {
                bouton.classList.add("bouton--chargement");
            }
            form.submit();
        }

        cases.forEach(function (c, i) {
            c.addEventListener("input", function (event) {
                var saisi = chiffres(c.value);
                // Frappe d'un chiffre dans une case déjà remplie : le nouveau chiffre remplace l'ancien.
                if (event.inputType === "insertText" && chiffres(event.data).length === 1) {
                    saisi = chiffres(event.data);
                }
                if (!saisi) {
                    c.value = "";
                    source.value = code();
                    return;
                }
                c.value = "";
                // Plusieurs chiffres d'un coup (remplissage automatique, saisie rapide) : depuis cette
                // case, ou depuis la première si le code entier arrive.
                apresSaisie(repartir(saisi, saisi.length >= longueur ? 0 : i));
            });
            c.addEventListener("paste", function (event) {
                var presse = event.clipboardData || window.clipboardData;
                if (!presse) {
                    return; // collage géré par l'événement « input »
                }
                event.preventDefault();
                var n = chiffres(presse.getData("text"));
                if (!n) {
                    return;
                }
                apresSaisie(repartir(n, n.length >= longueur ? 0 : i));
            });
            c.addEventListener("keydown", function (event) {
                if (event.key === "Backspace" && !c.value && i > 0) {
                    event.preventDefault();
                    cases[i - 1].value = "";
                    cases[i - 1].focus();
                    source.value = code();
                } else if (event.key === "ArrowLeft" && i > 0) {
                    event.preventDefault();
                    cases[i - 1].focus();
                } else if (event.key === "ArrowRight" && i < longueur - 1) {
                    event.preventDefault();
                    cases[i + 1].focus();
                } else if (event.key === "Home") {
                    event.preventDefault();
                    cases[0].focus();
                } else if (event.key === "End") {
                    event.preventDefault();
                    cases[longueur - 1].focus();
                }
            });
            c.addEventListener("focus", function () { c.select(); });
        });

        form.addEventListener("submit", function (event) {
            source.value = code();
            if (source.value.length !== longueur) {
                event.preventDefault();
                afficherErreur(form.getAttribute("data-texte-incomplet"));
                var vide = cases.findIndex(function (c) { return !c.value; });
                cases[vide < 0 ? 0 : vide].focus();
                return;
            }
            if (envoye) {
                event.preventDefault();
                return;
            }
            envoye = true;
        });
        // Retour arrière du navigateur vers une page restaurée depuis le cache : on peut renvoyer.
        window.addEventListener("pageshow", function () {
            envoye = false;
            var bouton = form.querySelector('button[type="submit"]');
            if (bouton) {
                bouton.classList.remove("bouton--chargement");
            }
        });
        cases[0].focus();
    }

    // Compte à rebours avant de pouvoir redemander un code : bouton désactivé, temps restant affiché.
    // Calculé sur l'heure de fin pour rester juste si l'onglet passe en arrière-plan.
    function initialiserCompteARebours(form) {
        var restant = parseInt(form.getAttribute("data-compte-a-rebours"), 10) || 0;
        var bouton = form.querySelector('button[type="submit"]');
        var texte = form.querySelector("[data-compte-a-rebours-texte]");
        if (restant <= 0 || !bouton) {
            return;
        }
        var fin = Date.now() + restant * 1000;
        bouton.disabled = true;
        function maj() {
            var s = Math.max(0, Math.ceil((fin - Date.now()) / 1000));
            if (s <= 0) {
                bouton.disabled = false;
                if (texte) {
                    texte.hidden = true;
                }
                return;
            }
            if (texte) {
                texte.textContent = format(form.getAttribute("data-texte-attente"), s);
            }
            setTimeout(maj, 250);
        }
        maj();
    }

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("form[data-code-otp]").forEach(initialiserCodeOtp);
        document.querySelectorAll("form[data-compte-a-rebours]").forEach(initialiserCompteARebours);
        document.querySelectorAll("form[data-sous-etapes]").forEach(initialiserSousEtapes);
        document.querySelectorAll("form[data-brouillon]").forEach(initialiserBrouillon);
        document.querySelectorAll("[data-depot]").forEach(initialiserDepot);
    });
})();

// ---------------------------------------------------------------------------------------------
// Bouton « Copier » ([data-copier]) : copie la valeur (référence Masrvi, code marchand) dans le
// presse-papiers et affiche « Copié » quelques secondes. Caché sans JavaScript ou sans presse-papiers :
// la valeur reste sélectionnable d'un geste (user-select: all).
// ---------------------------------------------------------------------------------------------
(function () {
    "use strict";

    // Presse-papiers moderne en contexte sécurisé ; sinon (démo servie en http sur le réseau local),
    // copie par une zone de texte temporaire.
    function copier(valeur) {
        if (navigator.clipboard && window.isSecureContext) {
            return navigator.clipboard.writeText(valeur);
        }
        return new Promise(function (ok, echec) {
            var zone = document.createElement("textarea");
            zone.value = valeur;
            zone.setAttribute("readonly", "");
            zone.className = "visuellement-cache";
            document.body.appendChild(zone);
            zone.select();
            var reussi = false;
            try {
                reussi = document.execCommand("copy");
            } catch (e) {
                reussi = false;
            }
            zone.remove();
            if (reussi) {
                ok();
            } else {
                echec();
            }
        });
    }

    function initialiserCopier(bouton) {
        var texte = bouton.textContent;
        var libelle = bouton.getAttribute("aria-label");
        var minuterie;
        bouton.hidden = false;
        bouton.addEventListener("click", function () {
            copier(bouton.getAttribute("data-copier")).then(function () {
                bouton.textContent = bouton.getAttribute("data-texte-copie") || texte;
                bouton.setAttribute("data-copie", "");
                bouton.setAttribute("aria-label", bouton.textContent);
                clearTimeout(minuterie);
                minuterie = setTimeout(function () {
                    bouton.textContent = texte;
                    bouton.removeAttribute("data-copie");
                    if (libelle) {
                        bouton.setAttribute("aria-label", libelle);
                    }
                }, 2500);
            }, function () {
                // Copie refusée : la valeur reste sélectionnable à la main.
            });
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("button[data-copier]").forEach(initialiserCopier);
    });
})();

// ---------------------------------------------------------------------------------------------
// États transverses.
// - Chargement : tout bouton qui envoie un formulaire ordinaire, et tout lien-bouton vers une autre
//   page, passe en « chargement » (roue, aria-busy) dès le clic ; un second envoi du même formulaire
//   est ignoré. Les requêtes HTMX marquent de même leur bouton, et leur zone cible passe en
//   aria-busy (squelette ou zone atténuée, voir app.css). Un formulaire GET [data-liste="#id"] met
//   sa liste en aria-busy pendant le rechargement. Tout est relâché au retour arrière (pageshow) et,
//   par sécurité, après 20 s (téléchargement, réponse sans nouvelle page).
// - Notifications brèves (toasts) : les messages [data-toast] posés par le serveur (succès d'une
//   action) sont déplacés dans la région #toasts et disparaissent seuls (pause au survol ou au
//   focus, bouton « Fermer »). Une réponse HTMX peut en demander une par l'en-tête
//   HX-Trigger: {"toast": {"message": "…", "type": "succes|erreur"}} ; un échec HTMX (réseau ou
//   serveur) en affiche une en erreur. Sans JavaScript, les messages restent dans la page.
// ---------------------------------------------------------------------------------------------
(function () {
    "use strict";

    var DUREE_TOAST = 6000;
    var SECURITE_CHARGEMENT = 20000;

    function texte(nom) {
        return document.body ? document.body.getAttribute("data-texte-" + nom) || "" : "";
    }

    // --- Notifications brèves ---------------------------------------------------------------

    function retirerToast(t) {
        if (t.classList.contains("toast--sortie")) {
            return;
        }
        t.classList.add("toast--sortie");
        setTimeout(function () { t.remove(); }, 250);
    }

    function minuterToast(t) {
        var minuterie;
        function lancer() {
            clearTimeout(minuterie);
            minuterie = setTimeout(function () { retirerToast(t); }, DUREE_TOAST);
        }
        function suspendre() {
            clearTimeout(minuterie);
        }
        t.addEventListener("mouseenter", suspendre);
        t.addEventListener("mouseleave", lancer);
        t.addEventListener("focusin", suspendre);
        t.addEventListener("focusout", lancer);
        lancer();
    }

    function toast(message, type) {
        var zone = document.getElementById("toasts");
        if (!zone || !message) {
            return;
        }
        var t = document.createElement("div");
        t.className = "toast toast--" + (type === "erreur" ? "erreur" : "succes");
        var p = document.createElement("p");
        p.className = "toast__texte";
        p.textContent = message;
        var fermer = document.createElement("button");
        fermer.type = "button";
        fermer.className = "toast__fermer";
        fermer.setAttribute("aria-label", texte("fermer"));
        fermer.textContent = "×";
        fermer.addEventListener("click", function () { retirerToast(t); });
        t.appendChild(p);
        t.appendChild(fermer);
        zone.appendChild(t);
        minuterToast(t);
    }

    // --- Indicateurs de chargement -------------------------------------------------------------

    function occuper(element) {
        if (!element || element.hasAttribute("data-chargement")) {
            return;
        }
        element.setAttribute("data-chargement", "");
        element.classList.add("bouton--chargement");
        element.setAttribute("aria-busy", "true");
        element.setAttribute("aria-disabled", "true");
    }

    function liberer(element) {
        if (!element || !element.hasAttribute("data-chargement")) {
            return;
        }
        element.removeAttribute("data-chargement");
        element.classList.remove("bouton--chargement");
        element.removeAttribute("aria-busy");
        element.removeAttribute("aria-disabled");
    }

    function toutLiberer() {
        document.querySelectorAll("[data-chargement]").forEach(liberer);
        document.querySelectorAll("form[data-envoi-en-cours]").forEach(function (f) {
            f.removeAttribute("data-envoi-en-cours");
        });
        document.querySelectorAll(".zone-chargeable[aria-busy]").forEach(function (z) {
            z.removeAttribute("aria-busy");
        });
    }

    // Formulaires ordinaires (les formulaires HTMX sont annulés par htmx : defaultPrevented).
    document.addEventListener("submit", function (event) {
        var form = event.target;
        if (event.defaultPrevented || !(form instanceof HTMLFormElement)) {
            return;
        }
        if (form.hasAttribute("data-envoi-en-cours")) {
            event.preventDefault(); // double clic : un seul envoi
            return;
        }
        if (form.target && form.target !== "_self") {
            return;
        }
        form.setAttribute("data-envoi-en-cours", "");
        occuper(event.submitter || form.querySelector('button[type="submit"], button:not([type])'));
        var liste = form.getAttribute("data-liste");
        var zone = liste ? document.querySelector(liste) : null;
        if (zone) {
            zone.setAttribute("aria-busy", "true");
        }
        setTimeout(toutLiberer, SECURITE_CHARGEMENT);
    });

    // Liens présentés en bouton qui ouvrent une autre page (pas les téléchargements ni les ancres).
    document.addEventListener("click", function (event) {
        if (event.defaultPrevented || event.button !== 0
                || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
            return;
        }
        var lien = event.target.closest ? event.target.closest("a.bouton[href]") : null;
        if (!lien || lien.hasAttribute("download") || lien.hasAttribute("hx-get")
                || (lien.target && lien.target !== "_self")) {
            return;
        }
        var url = new URL(lien.href, window.location.href);
        if (url.origin !== window.location.origin || /\.(pdf|csv)$/i.test(url.pathname)
                || (url.hash && url.pathname === window.location.pathname && url.search === window.location.search)) {
            return;
        }
        occuper(lien);
        setTimeout(toutLiberer, SECURITE_CHARGEMENT);
    });

    // Retour arrière vers une page restaurée depuis le cache : plus rien n'est en cours.
    window.addEventListener("pageshow", toutLiberer);

    // --- HTMX ------------------------------------------------------------------------------------

    document.addEventListener("htmx:beforeRequest", function (event) {
        var d = event.detail;
        var declencheur = d.requestConfig && d.requestConfig.triggeringEvent;
        var bouton = null;
        if (declencheur && declencheur.type === "submit") {
            bouton = declencheur.submitter || d.elt.querySelector('button[type="submit"]');
        } else if (declencheur && declencheur.type === "click" && d.elt.matches("a, button")) {
            bouton = d.elt;
        }
        if (bouton) {
            occuper(bouton);
            d.elt.seheliBouton = bouton;
        }
        if (d.target) {
            d.target.setAttribute("aria-busy", "true");
        }
    });

    document.addEventListener("htmx:afterRequest", function (event) {
        var d = event.detail;
        if (d.target) {
            d.target.removeAttribute("aria-busy");
        }
        if (d.elt.seheliBouton) {
            liberer(d.elt.seheliBouton);
            d.elt.seheliBouton = null;
        }
    });

    document.addEventListener("htmx:responseError", function () {
        toast(texte("erreur-serveur"), "erreur");
    });
    document.addEventListener("htmx:sendError", function () {
        toast(texte("erreur-reseau"), "erreur");
    });
    // En-tête HX-Trigger: {"toast": {...}} (htmx déclenche l'événement « toast », qui remonte au document).
    document.addEventListener("toast", function (event) {
        var d = event.detail || {};
        toast(d.message || d.value, d.type);
    });

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("[data-toast]").forEach(function (message) {
            toast(message.textContent.trim(), message.classList.contains("alerte--erreur") ? "erreur" : "succes");
            message.remove();
        });
    });
})();

// Back-office : tableaux interactifs, en amélioration progressive (sans JavaScript, le tableau
// complet reste lisible, dans l'ordre du serveur).
//   <table data-tableau [data-par-page="10"]> : tri par colonne (bouton dans chaque <th scope="col">
//   qui a un libellé visible ; [data-sans-tri] pour l'exclure), recherche plein texte, pagination.
//   <th data-filtre> : liste déroulante des valeurs distinctes de la colonne.
//   Clé de tri ou de filtre : [data-valeur] de la cellule (ou d'un élément qu'elle contient), sinon
//   son texte (dates jj/mm/aaaa [hh:mm] et nombres « 20 800 MRU » reconnus).
//   <tr data-lien="/url"> : toute la ligne ouvre l'URL (le lien de la ligne reste pour le clavier).
//   Libellés traduits : attributs data-texte-* de l'élément .bo (layout).
(function () {
    "use strict";

    var DATE = /^(\d{2})\/(\d{2})\/(\d{4})(?:\s+(\d{2}):(\d{2}))?/;
    var TAILLES = [10, 25, 50];

    function libelle(nom, defaut) {
        var racine = document.querySelector(".bo");
        var v = racine && racine.getAttribute("data-texte-" + nom);
        return v || defaut;
    }

    function format(modele, valeurs) {
        return modele.replace(/\{(\d+)\}/g, function (m, i) { return valeurs[Number(i)]; });
    }

    function normaliser(texte) {
        return (texte || "").normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().replace(/\s+/g, " ").trim();
    }

    function valeurBrute(cellule) {
        if (!cellule) { return ""; }
        var v = cellule.getAttribute("data-valeur");
        if (v === null) {
            var porteur = cellule.querySelector("[data-valeur]");
            if (porteur) { v = porteur.getAttribute("data-valeur"); }
        }
        return (v !== null ? v : cellule.textContent).replace(/\s+/g, " ").trim();
    }

    function cleTri(cellule) {
        var t = valeurBrute(cellule);
        var d = DATE.exec(t);
        if (d) { return Number(d[3] + d[2] + d[1] + (d[4] || "00") + (d[5] || "00")); }
        var n = t.replace(/[\s  ]/g, "");
        if (/^-?\d/.test(n)) { return parseFloat(n.replace(",", ".")); }
        return t;
    }

    function comparer(a, b) {
        if (typeof a === "number" && typeof b === "number") { return a - b; }
        return String(a).localeCompare(String(b), document.documentElement.lang || "fr", { numeric: true, sensitivity: "base" });
    }

    function initialiser(table) {
        var corps = table.tBodies[0];
        if (!corps || !table.tHead || table.hasAttribute("data-tableau-pret")) { return; }
        table.setAttribute("data-tableau-pret", "");
        var entetes = Array.prototype.slice.call(table.tHead.rows[0].cells);
        var lignes = Array.prototype.slice.call(corps.rows);
        var parPage = Number(table.getAttribute("data-par-page")) || TAILLES[0];
        var page = 0;
        var recherche = "";
        var filtres = {};

        var conteneur = table.closest(".tableau-defilant") || table;
        var outils = document.createElement("div");
        outils.className = "tableau-outils";
        outils.setAttribute("role", "group");
        outils.setAttribute("aria-label", libelle("filtres", "Filtres"));
        conteneur.parentNode.insertBefore(outils, conteneur);

        var champ = document.createElement("label");
        champ.className = "tableau-outils__champ";
        champ.appendChild(document.createTextNode(libelle("rechercher", "Rechercher")));
        var saisie = document.createElement("input");
        saisie.type = "search";
        saisie.autocomplete = "off";
        champ.appendChild(saisie);
        outils.appendChild(champ);
        saisie.addEventListener("input", function () { recherche = normaliser(saisie.value); page = 0; afficher(); });

        entetes.forEach(function (th, index) {
            if (!th.hasAttribute("data-filtre")) { return; }
            var valeurs = {};
            lignes.forEach(function (tr) {
                var c = tr.cells[index];
                var v = valeurBrute(c);
                if (v !== "" && !(v in valeurs)) { valeurs[v] = c.textContent.replace(/\s+/g, " ").trim(); }
            });
            var l = document.createElement("label");
            l.className = "tableau-outils__champ";
            l.appendChild(document.createTextNode(th.textContent.replace(/\s+/g, " ").trim()));
            var select = document.createElement("select");
            var tous = document.createElement("option");
            tous.value = "";
            tous.textContent = libelle("tous", "Tous");
            select.appendChild(tous);
            Object.keys(valeurs).sort(function (a, b) { return comparer(valeurs[a], valeurs[b]); }).forEach(function (v) {
                var o = document.createElement("option");
                o.value = v;
                o.textContent = valeurs[v];
                select.appendChild(o);
            });
            l.appendChild(select);
            outils.appendChild(l);
            select.addEventListener("change", function () {
                if (select.value) { filtres[index] = select.value; } else { delete filtres[index]; }
                page = 0;
                afficher();
            });
        });

        var compte = document.createElement("p");
        compte.className = "tableau-outils__compte";
        compte.setAttribute("aria-live", "polite");
        outils.appendChild(compte);

        var aucun = document.createElement("p");
        aucun.className = "tableau-aucun";
        aucun.hidden = true;
        aucun.textContent = libelle("aucun", "Aucun résultat.");
        var pagination = document.createElement("nav");
        pagination.className = "tableau-pagination";
        pagination.setAttribute("aria-label", libelle("pagination", "Pagination"));
        conteneur.parentNode.insertBefore(aucun, conteneur.nextSibling);
        conteneur.parentNode.insertBefore(pagination, aucun.nextSibling);

        entetes.forEach(function (th, index) {
            if (th.getAttribute("scope") !== "col" || th.hasAttribute("data-sans-tri")
                    || th.querySelector(".visuellement-cache") || th.textContent.trim() === "") { return; }
            var bouton = document.createElement("button");
            bouton.type = "button";
            bouton.className = "tableau__tri";
            while (th.firstChild) { bouton.appendChild(th.firstChild); }
            th.appendChild(bouton);
            bouton.addEventListener("click", function () {
                var sens = th.getAttribute("aria-sort") === "ascending" ? "descending" : "ascending";
                entetes.forEach(function (autre) { autre.removeAttribute("aria-sort"); });
                th.setAttribute("aria-sort", sens);
                var facteur = sens === "ascending" ? 1 : -1;
                lignes.sort(function (a, b) {
                    return facteur * comparer(cleTri(a.cells[index]), cleTri(b.cells[index]));
                });
                lignes.forEach(function (tr) { corps.appendChild(tr); });
                page = 0;
                afficher();
            });
        });

        function retenue(tr) {
            for (var i in filtres) {
                if (valeurBrute(tr.cells[i]) !== filtres[i]) { return false; }
            }
            return recherche === "" || normaliser(tr.textContent).indexOf(recherche) !== -1;
        }

        function bouton(texte, cible, actuel, desactive) {
            var b = document.createElement("button");
            b.type = "button";
            b.textContent = texte;
            if (actuel) { b.setAttribute("aria-current", "page"); }
            b.disabled = !!desactive;
            b.addEventListener("click", function () { page = cible; afficher(); });
            return b;
        }

        function afficher() {
            var retenues = lignes.filter(retenue);
            var pages = Math.max(1, Math.ceil(retenues.length / parPage));
            page = Math.min(page, pages - 1);
            var debut = page * parPage;
            var fin = Math.min(debut + parPage, retenues.length);
            lignes.forEach(function (tr) { tr.hidden = true; });
            retenues.slice(debut, fin).forEach(function (tr) { tr.hidden = false; });
            compte.textContent = retenues.length === 0 ? ""
                : format(libelle("resultats", "{0}–{1} sur {2}"), [debut + 1, fin, retenues.length]);
            aucun.hidden = retenues.length !== 0;

            pagination.textContent = "";
            if (retenues.length > TAILLES[0]) {
                var l = document.createElement("label");
                l.className = "tableau-outils__champ";
                l.appendChild(document.createTextNode(libelle("par-page", "Lignes par page")));
                var s = document.createElement("select");
                TAILLES.forEach(function (t) {
                    var o = document.createElement("option");
                    o.value = String(t);
                    o.textContent = String(t);
                    o.selected = t === parPage;
                    s.appendChild(o);
                });
                s.addEventListener("change", function () { parPage = Number(s.value); page = 0; afficher(); });
                l.appendChild(s);
                pagination.appendChild(l);
            }
            if (pages > 1) {
                pagination.appendChild(bouton(libelle("precedente", "Précédente"), page - 1, false, page === 0));
                for (var p = 0; p < pages; p++) {
                    pagination.appendChild(bouton(String(p + 1), p, p === page, false));
                }
                pagination.appendChild(bouton(libelle("suivante", "Suivante"), page + 1, false, page === pages - 1));
            }
        }

        corps.addEventListener("click", function (event) {
            var tr = event.target.closest("tr[data-lien]");
            if (!tr || event.target.closest("a, button, input, select, textarea, label, form")) { return; }
            if (window.getSelection && String(window.getSelection()) !== "") { return; }
            window.location.href = tr.getAttribute("data-lien");
        });

        afficher();
    }

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll("table[data-tableau]").forEach(initialiser);
        // Menu du back-office : replié par défaut sur petit écran (toujours ouvert sur ordinateur).
        var menu = document.querySelector("[data-menu-bo]");
        if (menu && window.matchMedia && window.matchMedia("(max-width: 63.99rem)").matches) { menu.open = false; }
    });
    // Liste rechargée par HTMX : ses nouveaux tableaux reçoivent aussi tri, filtres et pagination.
    document.addEventListener("htmx:afterSettle", function (event) {
        event.target.querySelectorAll("table[data-tableau]").forEach(initialiser);
    });
})();
