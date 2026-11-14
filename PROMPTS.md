# CIC — Prompts Claude Code pour le MVP

Mode d'emploi :
- CLAUDE.md à la racine du dépôt : Claude Code le lit tout seul.
- Un prompt = une petite tâche = un commit. Dans l'ordre.
- Les tests Testcontainers, les tests d'intégration et GitHub Actions viennent à la fin.

---

## Socle

### 1
```
Crée le projet Maven : Java 25, dernière version stable de Spring Boot 4.x, package racine com.cic, classe CicApplication.
Dépendances : Web, Thymeleaf, Validation, Data JPA, Security, Flyway, PostgreSQL, Actuator, Spring Modulith (core).
Terminé quand : ./mvnw compile passe.
```

### 2
```
Ajoute un docker-compose.yml avec PostgreSQL, et les fichiers application.yml, application-dev.yml, application-demo.yml et application-prod.yml (en prod, base et secrets par variables d'environnement).
Terminé quand : l'application démarre en profil dev, connectée à PostgreSQL.
```

### 3
```
Crée un package par module listé dans CLAUDE.md, chacun avec son package-info.java.
Ajoute une configuration de sécurité temporaire : tout est public sauf /bo/**.
Terminé quand : l'application démarre et les packages existent.
```

### 4
```
Crée le layout Thymeleaf commun (en-tête, contenu, pied de page), mobile d'abord, et une page d'accueil.
Terminé quand : la page d'accueil s'affiche correctement sur mobile.
```

### 5
```
Ajoute l'i18n français et arabe : fichiers de messages, bascule de langue dans l'en-tête, dir="rtl" en arabe.
Terminé quand : la page d'accueil bascule entre français et arabe.
```

### 6
```
Crée le value object Montant (BigDecimal, MRU, arrondi à l'ouguiya HALF_UP) et ses tests unitaires.
Terminé quand : les tests passent.
```

### 7
```
Crée la migration Flyway de la table parametre, remplie avec les règles de gestion de CLAUDE.md, et un service qui lit ces paramètres.
Terminé quand : les paramètres sont lus au démarrage.
```

## Calculs du domaine (Java pur, sans Spring)

### 8
```
Dans le module contrat, implémente le calcul du prix : prix d'achat, prix de vente, apport minimum de 40 %.
Tests unitaires avec l'exemple chiffré de CLAUDE.md.
Terminé quand : les tests passent.
```

### 9
```
Dans le module contrat, implémente l'échéancier : 3 à 6 échéances mensuelles arrondies à l'ouguiya, la dernière absorbe l'écart, dates au jour de paie du client.
Tests unitaires : exemple chiffré (6 × 2 080) et cas d'arrondi.
Terminé quand : les tests passent.
```

### 10
```
Ajoute les contrôles de l'échéancier : total des mensualités CIC ≤ 1/3 du salaire net ; pour un CDD, dernière échéance avant la fin du contrat, sinon erreur métier.
Tests unitaires pour chaque contrôle.
Terminé quand : les tests passent.
```

### 11
```
Dans le module demande, implémente les conditions éliminatoires du score v1 de CLAUDE.md.
Tests unitaires pour chaque condition.
Terminé quand : les tests passent.
```

### 12
```
Implémente la note sur 100 du score v1 : chaque critère avec ses points et sa raison, la version de la grille, la recommandation (FAVORABLE, A_EXAMINER, DEFAVORABLE).
Tests unitaires pour chaque critère.
Terminé quand : les tests passent.
```

### 13
```
Implémente la machine à états du cycle de vie d'un dossier décrit dans CLAUDE.md : transitions permises, erreur métier sinon.
Tests unitaires des transitions valides et interdites.
Terminé quand : les tests passent.
```

## Catalogue et back-office

### 14
```
Crée les entités Fournisseur (remise négociée) et Produit (catégorie, prix public, disponibilité), leur migration Flyway et leurs repositories.
Terminé quand : l'application démarre et les tables existent.
```

### 15
```
Sécurise le back-office /bo : table des utilisateurs, mots de passe BCrypt, page de connexion, rôles ANALYSTE, RECOUVREMENT, ADMIN, ASSOCIE. Crée un compte ADMIN en profil dev.
Terminé quand : /bo demande une connexion et le compte ADMIN fonctionne.
```

### 16
```
Ajoute au back-office la liste et le formulaire des fournisseurs (créer, modifier), réservés au rôle ADMIN.
Terminé quand : on peut créer et modifier un fournisseur.
```

### 17
```
Ajoute au back-office la liste et le formulaire des produits (créer, modifier, rendre indisponible), réservés au rôle ADMIN.
Terminé quand : on peut créer et modifier un produit.
```

## Espace client

### 18
```
Crée les entités Client (NNI unique) et Emploi, leur migration Flyway et leurs repositories.
Terminé quand : l'application démarre et les tables existent.
```

### 19
```
Crée le port Notification et un adaptateur qui journalise les messages.
Terminé quand : un message envoyé apparaît dans les journaux.
```

### 20
```
Implémente la connexion client par téléphone et code à usage unique : 6 chiffres, valable 5 minutes, 5 essais maximum, envoyé par le port Notification ; code fixe 000000 en profil demo. Protège /client/**.
Terminé quand : on se connecte avec le code lu dans les journaux.
```

### 21
```
Ajoute l'écran de création du compte client : NNI, nom, date de naissance, consentement explicite.
Terminé quand : un nouveau client crée son compte après la connexion.
```

### 22
```
Ajoute l'écran emploi du client : employeur, type de contrat, dates, salaire net, jour de paie, mode de remboursement.
Terminé quand : les données d'emploi sont enregistrées et modifiables.
```

### 23
```
Ajoute l'envoi des justificatifs : entité Document, stockage hors du dossier public, PDF, JPEG ou PNG, 5 Mo maximum (pièce d'identité, contrat ou attestation, bulletin de paie, RIB). Seuls le propriétaire et le back-office y accèdent.
Terminé quand : un client envoie et revoit ses justificatifs.
```

### 24
```
Ajoute le catalogue côté client (produits disponibles) et la page produit.
Terminé quand : le client parcourt le catalogue.
```

### 25
```
Ajoute la simulation sur la page produit avec HTMX : durée de 3 à 6 mois → prix total, apport, mensualité, via le calcul du module contrat.
Terminé quand : la simulation se met à jour sans recharger la page.
```

### 26
```
Crée les entités Demande et Score, et leur migration. Ajoute la soumission d'une demande : un seul dossier actif par client, règle du tiers du salaire, score calculé et stocké, statut EN_ANALYSE via la machine à états.
Terminé quand : un client soumet une demande et la voit enregistrée.
```

### 27
```
Ajoute le suivi du dossier côté client : statut et prochaine étape à faire.
Terminé quand : le client voit où en est son dossier.
```

## Décision

### 28
```
Ajoute au back-office la file des dossiers EN_ANALYSE, du plus ancien au plus récent, pour le rôle ANALYSTE.
Terminé quand : l'analyste voit la file.
```

### 29
```
Ajoute la fiche dossier : client, emploi, justificatifs, score détaillé critère par critère.
Terminé quand : l'analyste ouvre un dossier et voit son score détaillé.
```

### 30
```
Crée l'entité JournalAudit et un service d'audit. Ajoute la décision : accepter, refuser avec motif, demander un justificatif ; chaque décision passe par la machine à états et est journalisée.
Terminé quand : une décision change le statut et apparaît dans le journal.
```

## Offre et autorisation de prélèvement

### 31
```
Crée les entités Contrat et Echeance, et leur migration. À l'acceptation, crée l'offre et le contrat (prix fixé, apport, échéances générées) et affiche-les au client.
Terminé quand : le client voit son offre et son échéancier.
```

### 32
```
Génère le PDF du contrat de vente à tempérament, en français.
Terminé quand : le client télécharge un PDF avec les bons montants.
```

### 33
```
Crée l'entité AutorisationPrelevement (type VIREMENT_PERMANENT par défaut) et génère son PDF : virement permanent vers le compte de CIC, à signer pour la banque du client.
Terminé quand : le client télécharge l'autorisation remplie.
```

### 34
```
Ajoute au back-office le suivi de l'autorisation : type, statut (SIGNEE, TRANSMISE, ACCEPTEE, ACTIVE, REVOQUEE) et dépôt du document signé.
Terminé quand : l'analyste fait passer une autorisation à ACCEPTEE.
```

### 35
```
Ajoute ShedLock et un job planifié, avec Clock injectée : dossier ANNULEE si l'apport ou l'autorisation manquent 7 jours après l'acceptation.
Terminé quand : un dossier expiré passe en ANNULEE.
```

## Paiements

### 36
```
Affiche au client les instructions de paiement de l'apport : code marchand Masrvi de CIC (configurable) et référence unique du dossier.
Terminé quand : le client voit le montant, le code et la référence.
```

### 37
```
Crée l'entité Paiement (référence unique). Ajoute au back-office la saisie manuelle d'un paiement et le rapprochement par référence, idempotent.
Terminé quand : un paiement saisi est rattaché au bon dossier et un doublon est refusé.
```

### 38
```
Ajoute l'import CSV des paiements (référence, montant, date) et la liste des écarts : montant différent, référence inconnue.
Terminé quand : un fichier CSV est importé et les écarts sont listés.
```

### 39
```
Quand l'apport est encaissé et l'autorisation ACCEPTEE, passe le dossier en APPORT_ET_AUTORISATION. Ajoute le bon de commande fournisseur et la validation du paiement fournisseur par une autre personne que l'analyste → COMMANDEE.
Terminé quand : le dossier passe en COMMANDEE, et jamais sans apport ni autorisation.
```

### 40
```
Ajoute la confirmation de remise du produit par le client → LIVREE, puis EN_REMBOURSEMENT.
Terminé quand : la confirmation fait démarrer le remboursement.
```

### 41
```
Crée l'entité EcritureComptable : écritures en partie double, immuables, pour l'apport, le paiement fournisseur et chaque échéance encaissée.
Terminé quand : chaque mouvement d'argent produit ses écritures.
```

## Recouvrement

### 42
```
Ajoute un job planifié : message au client avant chaque prélèvement, avec un lien wa.me prérempli.
Terminé quand : le message apparaît dans les journaux la veille de l'échéance.
```

### 43
```
Crée l'entité Relance. Ajoute un job planifié : échéance EN_RETARD si elle n'est pas payée à la date prévue, relances à J+3 et J+7, blocage des nouveaux achats du client.
Terminé quand : un retard est détecté et le client ne peut plus faire de demande.
```

### 44
```
Ajoute l'écran RECOUVREMENT : prélèvements à venir, réussis et en échec ; notes datées ; transitions EN_REMBOURSEMENT, RECOUVREMENT et SOLDEE.
Terminé quand : un dossier en retard est régularisé ou passé en recouvrement depuis l'écran.
```

## Pilotage et inscription

### 45
```
Crée l'entité ApportCapital. Ajoute la saisie des apports en capital et le tableau de bord ASSOCIE : capital apporté, capital engagé, encours, bénéfice réalisé.
Terminé quand : l'associé voit ces quatre chiffres.
```

### 46
```
Ajoute au tableau de bord les indicateurs de risque et les alertes de CLAUDE.md : retard à 30 jours, perte, prélèvements réussis, utilisation du capital, concentration.
Terminé quand : une alerte s'affiche quand un seuil est dépassé.
```

### 47
```
Crée l'entité Inscription et la page publique d'inscription, en français et en arabe : nom, téléphone, employeur, type de contrat, tranche de salaire, produit souhaité, consentement ; anti-doublon par téléphone et champ piège anti-robot.
Terminé quand : une inscription est enregistrée et un doublon est refusé.
```

### 48
```
Ajoute au back-office le nombre d'inscrits, le nombre de demandes complètes et l'export CSV des inscriptions.
Terminé quand : l'export CSV se télécharge.
```

## Démo et déploiement

### 49
```
En profil demo, charge 3 fournisseurs et 15 produits réalistes (téléphones, électroménager) en MRU.
Terminé quand : le catalogue de démo s'affiche.
```

### 50
```
En profil demo, charge 10 clients fictifs avec des dossiers dans tous les états du cycle de vie, des paiements, un retard et un dossier soldé, plus un compte par rôle du back-office.
Terminé quand : chaque écran affiche des données parlantes.
```

### 51
```
Ajoute un bouton « Réinitialiser la démo », réservé au rôle ADMIN.
Terminé quand : la démo revient à son état initial.
```

### 52
```
Ajoute le manifest PWA et les icônes pour installer l'application sur mobile.
Terminé quand : le navigateur propose d'installer l'application.
```

### 53
```
Ajoute un Dockerfile multi-étapes et un docker-compose de production : application, PostgreSQL, Caddy en HTTPS, volume pour les justificatifs, aucun secret dans le dépôt.
Terminé quand : docker compose up lance l'application en profil demo.
```

### 54
```
Ajoute une sauvegarde quotidienne de la base et le health check Actuator.
Terminé quand : une sauvegarde est créée et /actuator/health répond UP.
```

### 55
```
Écris le README : lancement local, déploiement sur un VPS, accès de démonstration pour le jury.
Terminé quand : le README permet de tout relancer sans aide.
```

### 56
```
Écris un script Playwright qui capture en PNG, en français et en arabe, les écrans clés en profil demo, dans docs/captures.
Terminé quand : les captures sont générées.
```

## Tests et CI (à la fin)

### 57
```
Ajoute le test ModularityTests avec ApplicationModules.of(CicApplication.class).verify() et corrige les dépendances interdites entre modules.
Terminé quand : le test passe.
```

### 58
```
Ajoute Testcontainers (PostgreSQL) et une classe de base pour les tests d'intégration.
Terminé quand : un test d'intégration simple démarre la base et passe.
```

### 59
```
Écris les tests d'intégration du parcours de demande : compte, justificatifs, demande, score, décision.
Terminé quand : les tests passent.
```

### 60
```
Écris le test de bout en bout, de l'inscription jusqu'à SOLDEE, en vérifiant les montants et les écritures comptables.
Terminé quand : le test passe.
```

### 61
```
Écris les tests de sécurité MockMvc : accès par rôle, un client ne voit jamais les données d'un autre, CSRF, upload. Corrige ce que tu trouves.
Terminé quand : les tests passent.
```

### 62
```
Ajoute le workflow GitHub Actions qui lance ./mvnw verify à chaque push.
Terminé quand : le workflow passe sur GitHub.
```

## Nouveau nom : Seheli

Nom latin : Seheli. Nom arabe : سهلي. Tout nom ou logo d'une autre marque est interdit : on s'inspire des usages, jamais d'une identité existante.

### 63
```
Renomme la marque visible « CIC » en « Seheli » (en arabe : سهلي) : messages.properties et messages_ar.properties, titres de pages, layout, SMS et messages journalisés, PDF (contrat, autorisation de prélèvement, bon de commande), manifest PWA, page hors ligne, page d'inscription.
La raison sociale qui apparaît dans les PDF devient le paramètre societe.raison-sociale (valeur Seheli) dans la table parametre, ajouté par une nouvelle migration Flyway.
Ne modifie jamais une migration Flyway déjà existante (la base de production les a déjà appliquées) : toute correction de données passe par une nouvelle migration.
Terminé quand : plus aucune page, aucun PDF ni aucun message n'affiche « CIC » (grep sur templates, messages et PDF générés), et ./mvnw test passe.
```

### 64
```
Renomme le code technique : package com.cic en com.seheli, CicApplication en SeheliApplication, groupId et artifactId Maven en seheli, préfixe de configuration cic.* en seheli.*, nom du cache du service worker, commentaires d'en-tête. Mets à jour CLAUDE.md (package, titre, contexte) et régénère docs/CARTE.md avec bash scripts/carte.sh.
Garde le nom de la base de données et les migrations Flyway existantes tels quels.
Terminé quand : grep -ri "com.cic\|CicApplication" src pom.xml ne renvoie rien, ModularityTests et ./mvnw test passent, l'application démarre en profil dev.
```

### 65
```
Renomme les variables d'environnement CIC_* en SEHELI_* (docker-compose.prod.yml, .env.example, Caddyfile, application-prod.yml), l'image Docker cic:latest en seheli:latest, et mets à jour README.md et docs/DEPLOIEMENT.md.
Le serveur déjà en ligne doit continuer à fonctionner sans perte de données : fixe le nom du projet Compose (clé name: à la valeur actuelle « cic ») pour que les volumes pgdata, justificatifs, sauvegardes et caddy_data restent les mêmes, et ajoute dans DEPLOIEMENT.md une section « Passer de CIC à Seheli sur un serveur existant » avec la commande sed qui renomme les variables dans .env, puis git pull et docker compose up -d --build.
Ajoute aussi dans DEPLOIEMENT.md la mise en ligne sans nom de domaine : SEHELI_DOMAINE=VOTRE_IP.sslip.io, ports 80 et 443 ouverts, docker compose up -d --force-recreate caddy.
Terminé quand : docker compose -f docker-compose.prod.yml config passe avec un .env construit depuis .env.example, et grep -rn "CIC_" ne renvoie plus rien hors de la section de migration.
```

## Logo

### 66
```
Crée le logo Seheli en SVG, dessiné à la main (formes simples, pas d'image bitmap) dans src/main/resources/static/img/logo/ : symbole seul, logo horizontal en lettres latines, logo horizontal en arabe, version monochrome, version claire sur fond sombre.
Idée directrice : « seheli » évoque ce qui est facile ; le symbole peut suggérer un paiement découpé en parts régulières (segments d'un cercle, marches), sans croissant, sans pièce de monnaie cliché, sans ressemblance avec un logo existant.
Contraintes : lisible à 16 px, couleurs reprises des variables CSS (ajuste la palette si nécessaire et note-le), texte vectorisé en tracés pour ne dépendre d'aucune police.
Écris docs/MARQUE.md : sens du logo, palette (codes hexadécimaux et contrastes WCAG), typographies, tailles minimales, zone de protection, usages interdits.
Terminé quand : les cinq fichiers SVG sont valides et s'affichent dans un navigateur, et docs/MARQUE.md existe.
```

### 67
```
Intègre le logo partout : en-tête du layout client et du back-office, page de connexion, page publique d'inscription, page hors ligne, en-tête des trois PDF, favicon SVG et PNG. Régénère les icônes PWA (192, 512, maskable, apple-touch-icon) depuis le symbole avec scripts/icones/GenererIcones.java, et ajoute les balises Open Graph (titre, description, image) sur l'accueil et l'inscription.
En arabe, affiche la version arabe du logo.
Terminé quand : le logo s'affiche en français et en arabe sur ces écrans et dans un PDF généré, et le navigateur montre la nouvelle icône.
```

## UX/UI inspirée des applications de paiement échelonné

Références d'usages (pas d'identité) : applications de paiement en plusieurs fois, en particulier celles du Golfe qui fonctionnent en arabe et sans intérêt. Ce qui compte : prix mensuel visible partout, zéro frais affiché clairement, échéancier en frise, prochaine action toujours en évidence, parcours court sur mobile.

### 68
```
Crée le système de design dans app.css : variables de couleur issues de docs/MARQUE.md (avec mode sombre via prefers-color-scheme), échelle typographique, espacements, rayons, ombres. Héberge localement (dans static/fonts, licence OFL) une police latine et une police arabe assorties ; aucune police chargée depuis un CDN.
Composants CSS réutilisables : boutons (principal, secondaire, danger, chargement), champs et messages d'erreur, cartes, badges de statut d'un dossier et d'une échéance, alertes, tableaux. Utilise uniquement des propriétés logiques pour que tout s'inverse en RTL.
Ajoute une page de démonstration des composants, visible en profil dev seulement.
Terminé quand : la page des composants s'affiche correctement en français et en arabe, en clair et en sombre.
```

### 69
```
Refais la navigation de l'espace client, mobile d'abord : barre d'onglets fixe en bas de l'écran (Accueil, Catalogue, Mon dossier, Profil) avec icônes SVG inline et état actif, en-tête compact avec le logo et le choix de langue FR / ع. Sur grand écran, la barre devient un menu latéral ou supérieur.
Zones tactiles d'au moins 44 px, respect des zones sûres de l'écran (env(safe-area-inset-bottom)).
Terminé quand : la navigation fonctionne sur un écran de 375 px en français et en arabe.
```

### 70
```
Refais la page d'accueil publique : promesse en une phrase (« Équipez-vous aujourd'hui, payez avec votre salaire »), trois points clés (40 % d'apport, 3 à 6 mensualités, aucun intérêt et aucun frais de retard), « Comment ça marche » en 4 étapes illustrées, exemple chiffré réel (20 800 MRU = 8 320 puis 6 × 2 080), bloc confiance (mourabaha, prix fixé à la signature), FAQ repliable, bouton principal vers l'inscription.
Terminé quand : la page s'affiche en français et en arabe, et l'exemple chiffré vient du calcul du domaine, pas d'un texte en dur.
```

### 71
```
Refais le catalogue client : grille de cartes produit avec photo, nom, prix comptant et « à partir de X MRU / mois » en évidence (calculé sur 6 mois avec l'apport minimum), filtres par catégorie en puces défilantes, recherche HTMX, état vide soigné. La fiche produit montre le prix, la mensualité, la disponibilité et un bouton « Simuler mon paiement ».
Terminé quand : le catalogue de démo s'affiche ainsi, la mensualité affichée est juste au regard du domaine.
```

### 72
```
Refais la simulation : choix de la durée (3 à 6 mois) en boutons segmentés et de l'apport par curseur (40 % minimum), recalcul instantané en HTMX. Affiche l'échéancier en frise verticale avec les dates réelles au jour de paie du client, le total payé décomposé (prix, marge Seheli, 0 frais) et l'avertissement si les mensualités dépassent le tiers du salaire.
Terminé quand : changer la durée ou l'apport met à jour la frise sans recharger la page, en français et en arabe.
```

### 73
```
Transforme le parcours de demande en étapes avec barre de progression (Compte, Emploi, Justificatifs, Demande) : une seule question principale par écran, retour possible sans perte de saisie, erreurs affichées à côté du champ.
Pour les justificatifs : aperçu du fichier, glisser-déposer sur ordinateur, prise de photo directe sur mobile (accept et capture), taille et format vérifiés avant l'envoi avec un message clair.
Terminé quand : un client de démo va du compte à la demande soumise sur mobile, avec la progression visible à chaque étape.
```

### 74
```
Améliore l'écran du code à usage unique : 6 cases avec passage automatique à la suivante, collage du code entier, autocomplete="one-time-code" et inputmode="numeric", compte à rebours avant de pouvoir redemander un code, message d'erreur clair.
Terminé quand : la connexion fonctionne au clavier, par collage et sur mobile, en français et en arabe.
```

### 75
```
Refais le suivi du dossier : frise de statut façon suivi de livraison (Soumise, En analyse, Acceptée, Apport et autorisation, Commandée, Livrée, En remboursement, Soldée), étape courante mise en avant, et une carte « Prochaine action » en haut : référence Masrvi avec bouton Copier et montant de l'apport, compte à rebours des 7 jours, documents à télécharger, confirmation de remise.
Terminé quand : chaque état du cycle de vie des clients de démo affiche la bonne étape et la bonne prochaine action.
```

### 76
```
Ajoute l'écran de remboursement : anneau de progression (« 2 sur 6 payées »), montant restant, carte de la prochaine échéance avec sa date, liste des échéances avec badge PAYEE, A_VENIR ou EN_RETARD, et message rassurant sans pénalité en cas de retard. Graphiques en SVG généré côté serveur, sans bibliothèque JavaScript.
Terminé quand : les clients de démo en remboursement, en retard et soldé affichent chacun un écran juste.
```

### 77
```
Ajoute les états transverses : indicateur de chargement HTMX sur chaque bouton et formulaire, notifications brèves (toasts) après une action, états vides illustrés, pages d'erreur 403, 404 et 500 à la marque Seheli en français et en arabe, squelettes de chargement sur les listes.
Terminé quand : chaque page d'erreur s'affiche et une action client montre un retour visuel immédiat.
```

### 78
```
Refais l'interface du back-office pour un usage sur ordinateur : menu latéral par rôle, tableaux denses avec tri, filtres et pagination, badges de statut, fiche dossier en deux colonnes (identité et documents d'un côté, score et décision de l'autre). Le score montre chaque critère en barre avec ses points et sa raison, et la lecture FAVORABLE, A_EXAMINER ou DEFAVORABLE en couleur.
Terminé quand : un analyste de démo traite un dossier de la file à la décision avec la nouvelle interface.
```

### 79
```
Refais le tableau de bord des associés : tuiles de chiffres clés en haut (capital apporté, engagé, encours, bénéfice), alertes de risque en bandeau coloré, courbes d'encours et de recouvrement et barres de concentration par employeur et fournisseur, en SVG généré côté serveur avec valeurs lisibles et version texte accessible.
Terminé quand : le tableau de bord de démo affiche tuiles, alertes et graphiques en français et en arabe.
```

### 80
```
Passe accessibilité et arabe sur toutes les pages : contrastes WCAG AA, focus visible, libellés et attributs aria, ordre de tabulation, textes alternatifs, chiffres et montants lisibles en RTL (MRU bien placé), icônes directionnelles inversées en arabe, aucun texte coupé sur 320 px. Corrige ce que tu trouves.
Terminé quand : un audit Lighthouse d'accessibilité donne au moins 95 sur l'accueil, le catalogue, la simulation et le suivi du dossier, en français et en arabe.
```

### 81
```
Régénère les captures d'écran FR et AR avec scripts/captures.sh, mets à jour les captures et le texte du README avec le nom Seheli et la nouvelle interface, et vérifie que grep -rni "cic" ne renvoie plus que des noms techniques volontairement conservés (base de données, projet Compose, section de migration).
Terminé quand : les captures sont à jour et le README présente Seheli.
```
