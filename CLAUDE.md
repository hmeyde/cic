# CIC — MVP

## Contexte
CIC vend à crédit des téléphones et de l'électroménager à des salariés en Mauritanie.
Le client paie 40 % d'apport, reçoit le produit, puis rembourse le reste en 3 à 6 mensualités prélevées dès le versement de son salaire.
CIC achète au fournisseur avec une remise et revend presque au prix public, plus une marge fixée à la signature (logique de mourabaha : pas d'intérêt, pas de pénalité).
Le MVP est le prototype présenté au BMCI Business Challenge : il doit fonctionner de bout en bout en démonstration.

## Périmètre du MVP
Inclus :
- Parcours client : compte par code à usage unique, justificatifs, catalogue, simulation, demande, offre, contrat PDF, autorisation de prélèvement PDF, paiement de l'apport par référence Masrvi, confirmation de remise, suivi du dossier.
- Back-office : catalogue, file des dossiers, score détaillé, décision, suivi de l'autorisation, rapprochement des paiements, commande fournisseur, échéances, recouvrement.
- Tableau de bord des associés.
- Page publique d'inscription (preuve de concept).
- Données de démonstration.
- Interface en français et en arabe.

Exclus : Bankily, API Masrvi, vrai fournisseur SMS ou WhatsApp (adaptateur qui journalise), signature électronique, espace fournisseur, comptabilité complète (écritures de base seulement), modèle statistique de score.

## Stack
- Java 25 (LTS), dernière version stable de Spring Boot 4.x, Maven.
- Spring Modulith : modules vérifiés, registre d'événements JPA utilisé comme outbox.
- PostgreSQL, Flyway, Spring Data JPA.
- Spring Security : code à usage unique pour les clients ; identifiant et mot de passe (BCrypt) pour le back-office.
- Interface : Thymeleaf + HTMX, mobile d'abord, i18n FR/AR avec `dir="rtl"` en arabe, manifest PWA.
- PDF : bibliothèque open source compatible Spring Boot 4 ; PDF en français pour le MVP.
- Jobs : `@Scheduled` + ShedLock (JDBC). Une `Clock` injectée partout où l'heure compte.
- Tests : JUnit 5 et AssertJ pour le domaine pendant le développement. Testcontainers, MockMvc, ArchUnit, `ApplicationModules.of(...).verify()` et GitHub Actions sont ajoutés à la fin du projet.
- Actuator, Micrometer, journaux JSON en production.

## Architecture
- Monolithe modulaire. Un package par module sous `com.cic` : `identite`, `catalogue`, `demande`, `contrat`, `paiement`, `recouvrement`, `comptabilite`, `notification`, `pilotage`, `audit`, `inscription`.
- Architecture hexagonale dans chaque module : `domain` (Java pur, sans Spring), `application` (cas d'usage), `adapter.in.web`, `adapter.out.persistence`, `adapter.out.*` pour les services externes.
- Jamais d'accès direct au repository d'un autre module : passer par l'API publique du module ou par des événements de domaine.
- Pas de relation JPA entre modules : on référence un autre module par identifiant.
- Montants : value object `Montant` (BigDecimal, MRU), arrondi à l'ouguiya (HALF_UP). Jamais de `double` ni de `float`.
- Paiements idempotents par référence unique. Écritures comptables immuables : une correction se fait par écriture inverse.
- Tout paramètre métier est lu dans la table `parametre`, modifiable sans redéploiement.
- Langage du domaine en français sans accents (`Demande`, `Echeance`) ; code technique en anglais.

## Règles de gestion (valeurs de départ, table `parametre`)
- Apport minimum : 40 % du prix de vente.
- Marge CIC : 4 % du prix public, par catégorie de produit.
- Remise fournisseur : 5 à 10 %, par fournisseur.
- Durée : 3 à 6 mois.
- Total des mensualités CIC d'un client ≤ 1/3 de son salaire net.
- Un seul dossier actif par client.
- Apport et autorisation de prélèvement sous 7 jours après acceptation, sinon dossier ANNULEE.
- Date d'échéance = jour de paie du client. Pour un CDD, la dernière échéance tombe avant la fin du contrat.
- Relances : message avant chaque prélèvement, puis à J+3 et J+7 après un échec.
- Client en retard : nouveaux achats bloqués.
- Aucune pénalité de retard : le prix ne change jamais après la signature.
- Arrondi à l'ouguiya ; la dernière échéance absorbe l'écart.
- Paiement du fournisseur seulement après apport encaissé ET autorisation ACCEPTEE.
- Risque : au plus 20 % de l'encours sur un même employeur ou fournisseur ; provisions de 50 % du restant dû à 30 jours de retard, 100 % à 90 jours, passage en perte à 180 jours ; aucun nouveau dossier si l'encours dépasse 90 % du capital disponible.

## Exemple chiffré (fixture de test)
Prix public 20 000 MRU ; remise 8 % → prix d'achat 18 400 ; marge 4 % → prix de vente 20 800 ; apport 40 % = 8 320 ; reste 12 480 = 6 × 2 080 ; capital engagé 10 080 ; bénéfice brut 2 400.

## Score v1
Conditions éliminatoires : NNI et pièce d'identité vérifiés ; 18 ans ou plus ; contrat de travail en cours ; salaire versé sur un compte ; mensualités ≤ 1/3 du salaire net ; aucun retard en cours chez CIC.

Note sur 100 :
- Type de contrat (25) : fonctionnaire ou CDI 25 ; CDD plus long que le crédit 15.
- Ancienneté chez l'employeur (15) : plus de 2 ans 15 ; de 1 à 2 ans 10 ; moins d'un an 5.
- Mode de remboursement (15) : retenue par l'employeur ou salaire domicilié à la BMCI 15 ; virement permanent seul 8.
- Part du salaire consacrée aux échéances (15) : moins de 20 % 15 ; de 20 à 33 % 8.
- Apport au-delà de 40 % (10) : proportionnel à l'apport supplémentaire.
- Garant (10).
- Historique chez CIC (10) : plans déjà remboursés à temps.

Lecture : ≥ 70 FAVORABLE ; 50 à 69 A_EXAMINER ; < 50 DEFAVORABLE. Le score recommande, l'analyste décide toujours. Chaque critère garde ses points et sa raison ; la version de la grille est stockée.

## Cycle de vie d'un dossier
- DEMANDE_SOUMISE → EN_ANALYSE → ACCEPTEE | REFUSEE
- ACCEPTEE → APPORT_ET_AUTORISATION (apport encaissé + autorisation ACCEPTEE) | ANNULEE (délai de 7 jours dépassé)
- APPORT_ET_AUTORISATION → COMMANDEE → LIVREE → EN_REMBOURSEMENT
- EN_REMBOURSEMENT → SOLDEE | EN_RETARD
- EN_RETARD → EN_REMBOURSEMENT (régularisé) | RECOUVREMENT

Toute autre transition est interdite et lève une erreur métier. Chaque transition est enregistrée dans le journal d'audit.

## Modèle de données
Client, Emploi, Document, Fournisseur, Produit, Demande, Score, Contrat, AutorisationPrelevement (type : VIREMENT_PERMANENT, DOMICILIATION_BMCI, RETENUE_EMPLOYEUR ; statut : SIGNEE, TRANSMISE, ACCEPTEE, ACTIVE, REVOQUEE), Echeance (A_VENIR, PAYEE, EN_RETARD), Paiement (référence unique), EcritureComptable, Relance, ApportCapital, JournalAudit, Inscription, Parametre.

## Rôles
CLIENT, ANALYSTE, RECOUVREMENT, ADMIN, ASSOCIE.
Séparation des tâches : la personne qui décide d'un dossier ne valide pas le paiement fournisseur du même dossier.

## Sécurité
- Consentement explicite du client à l'usage de ses données.
- Justificatifs stockés hors du dossier public ; PDF, JPEG ou PNG, 5 Mo maximum ; accès réservé au propriétaire et au back-office.
- CSRF actif ; contrôle d'accès sur chaque endpoint ; en-têtes HTTP de sécurité.
- Aucun secret dans le dépôt ; configuration par variables d'environnement.
- Aucune donnée personnelle dans les URL ou les journaux.

## Définition de terminé
- L'application compile et démarre en profil dev.
- Les tests unitaires existants passent (`./mvnw test`).
- Le code respecte ce fichier.
- Une petite tâche = un commit avec un message clair.

## Commandes
- `docker compose up -d` : PostgreSQL local
- `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
- `./mvnw test`