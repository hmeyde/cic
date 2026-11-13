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
