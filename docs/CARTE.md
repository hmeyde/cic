# Carte du projet Seheli

Générée automatiquement depuis le code (`ccab505`). Sert à se repérer vite ; le code fait foi.

## Modules (`com.seheli.*`)

| Module | Couches | Classes principales |
|---|---|---|
| `audit` (12) | adapter.out adapter.out.persistence application domain | Acteur, JournalAuditJpaRepository, JournalAuditPersistenceAdapter, JournalAuditRepository, JournalAuditService |
| `catalogue` (32) | adapter.in adapter.in.web adapter.out adapter.out.persistence application domain | CatalogueClientController, CatalogueClientService, Fournisseur, FournisseurBackOfficeController, FournisseurJpaRepository, FournisseurPersistenceAdapter, FournisseurRepository, FournisseurService, Produit, ProduitBackOfficeController, ProduitJpaRepository, ProduitPersistenceAdapter |
| `commun` (4) | domain | Fraction, Montant |
| `comptabilite` (31) | adapter.in adapter.in.evenement adapter.in.web adapter.out adapter.out.persistence application domain | ApportCapitalJpaRepository, ApportCapitalPersistenceAdapter, ApportCapitalRepository, ApportCapitalService, CapitalBackOfficeController, ComptabiliteService, Compte, EcritureComptableJpaRepository, EcritureComptablePersistenceAdapter, EcritureComptableRepository, IndicateursFinanciersService, Sens |
| `config` (5) | - | - |
| `contrat` (61) | adapter.in adapter.in.web adapter.out adapter.out.pdf adapter.out.persistence adapter.out.stockage application domain | Acheteur, AutorisationPdfService, AutorisationPrelevementJpaRepository, AutorisationPrelevementPersistenceAdapter, AutorisationPrelevementRepository, AutorisationsBackOfficeController, CalculPrixService, Contrat, ContratJpaRepository, ContratPdfService, ContratPersistenceAdapter, ContratRepository |
| `demande` (101) | adapter.in adapter.in.evenement adapter.in.scheduling adapter.in.web adapter.out adapter.out.pdf adapter.out.persistence application domain | ApportEtAutorisationService, CommandeFournisseurJpaRepository, CommandeFournisseurPersistenceAdapter, CommandeFournisseurRepository, CommandeFournisseurService, CommandesFournisseurBackOfficeController, Decision, DecisionBackOfficeController, DecisionService, Demande, DemandeClientController, DemandeJpaRepository |
| `demonstration` (21) | adapter.in adapter.in.demarrage adapter.in.web adapter.out adapter.out.base adapter.out.demarrage adapter.out.horloge adapter.out.image application domain | DonneesDemonstrationService, ReinitialisationDemonstrationController, ReinitialisationDemonstrationService, Simulation |
| `identite` (77) | adapter.in adapter.in.web adapter.out adapter.out.code adapter.out.persistence adapter.out.stockage application domain | Client, ClientJpaRepository, ClientPersistenceAdapter, ClientRepository, ClientService, CodeConnexionJpaRepository, CodeConnexionPersistenceAdapter, CodeConnexionRepository, CompteBackOfficeService, ConnexionBackOfficeController, ConnexionClientController, ConnexionClientService |
| `inscription` (17) | adapter.in adapter.in.web adapter.out adapter.out.persistence application domain | Inscription, InscriptionController, InscriptionJpaRepository, InscriptionPersistenceAdapter, InscriptionRepository, InscriptionsBackOfficeController, InscriptionService |
| `notification` (6) | adapter.out adapter.out.journal domain | Canal, Message, Notification |
| `paiement` (36) | adapter.in adapter.in.web adapter.out adapter.out.persistence application domain | ImportPaiementsBackOfficeController, ImportPaiementsService, InstructionsApportService, Paiement, PaiementJpaRepository, PaiementPersistenceAdapter, PaiementRepository, PaiementsBackOfficeController, PaiementService, Rattachement |
| `parametrage` (13) | adapter.out adapter.out.persistence application domain | Parametre, ParametreJpaRepository, ParametrePersistenceAdapter, Parametres, ParametreService |
| `pilotage` (11) | adapter.in adapter.in.web application domain | Concentration, TableauDeBordController, TableauDeBordService |
| `recouvrement` (29) | adapter.in adapter.in.scheduling adapter.in.web adapter.out adapter.out.persistence application domain | NoteRecouvrementJpaRepository, NoteRecouvrementPersistenceAdapter, NoteRecouvrementRepository, RappelPrelevementJob, RappelPrelevementService, RecouvrementBackOfficeController, Relance, RelanceJpaRepository, RelancePersistenceAdapter, RelanceRepository, RetardEcheanceService, RetardsEtRelancesJob |
| `web` (6) | - | AccueilController, BackOfficeAccueilController, ComposantsDevController |

## Routes web

| Contrôleur (module) | Routes |
|---|---|
| `AccueilController` (web) | `GET /` |
| `AutorisationsBackOfficeController` (contrat) | `GET /bo/autorisations`, `GET /bo/autorisations/{id}`, `POST /bo/autorisations/{id}/document-signe`, `GET /bo/autorisations/{id}/document-signe`, `GET /bo/autorisations/{id}/autorisation.pdf`, `POST /bo/autorisations/{id}/transmission`, `POST /bo/autorisations/{id}/acceptation`, `POST /bo/autorisations/{id}/activation`, `POST /bo/autorisations/{id}/revocation` |
| `BackOfficeAccueilController` (web) | `GET /bo` |
| `CapitalBackOfficeController` (comptabilite) | `GET /bo/capital`, `POST /bo/capital` |
| `CatalogueClientController` (catalogue) | `GET /client/catalogue`, `GET /client/catalogue/{id}`, `GET /client/catalogue/{id}/simulation` |
| `CommandesFournisseurBackOfficeController` (demande) | `GET /bo/commandes`, `POST /bo/commandes/{id}/validation`, `GET /bo/commandes/{id}/bon-de-commande.pdf` |
| `ComposantsDevController` (web) | `GET /dev/composants` |
| `ConnexionBackOfficeController` (identite) | `GET /bo/login` |
| `ConnexionClientController` (identite) | `GET /client/connexion`, `POST /client/connexion`, `GET /client/connexion/code`, `POST /client/connexion/code` |
| `CreationCompteClientController` (identite) | `GET /client/connexion/compte`, `POST /client/connexion/compte` |
| `DecisionBackOfficeController` (demande) | `POST CHEMIN_ACCEPTATION`, `POST CHEMIN_REFUS`, `POST CHEMIN_JUSTIFICATIF` |
| `DemandeClientController` (demande) | `GET /client/demandes`, `GET /client/dossier`, `GET /client/dossier/contrat.pdf`, `GET /client/dossier/autorisation-prelevement.pdf`, `POST /client/dossier/remise`, `POST /client/demandes` |
| `EmploiClientController` (identite) | `GET /client/emploi`, `POST /client/emploi` |
| `EspaceClientController` (identite) | `GET /client`, `GET /client/profil` |
| `FicheDossierBackOfficeController` (demande) | `GET CHEMIN` |
| `FileAnalyseBackOfficeController` (demande) | `GET /bo/dossiers` |
| `FournisseurBackOfficeController` (catalogue) | `GET /bo/fournisseurs`, `GET /bo/fournisseurs/nouveau`, `POST /bo/fournisseurs`, `GET /bo/fournisseurs/{id}`, `POST /bo/fournisseurs/{id}` |
| `ImportPaiementsBackOfficeController` (paiement) | `GET /bo/paiements/import`, `POST /bo/paiements/import`, `GET /bo/paiements/ecarts` |
| `InscriptionController` (inscription) | `GET /inscription`, `POST /inscription`, `GET /inscription/merci` |
| `InscriptionsBackOfficeController` (inscription) | `GET /bo/inscriptions`, `GET /bo/inscriptions/export.csv` |
| `JustificatifsBackOfficeController` (identite) | `GET /bo/justificatifs`, `GET /bo/justificatifs/{id}` |
| `JustificatifsClientController` (identite) | `GET /client/justificatifs`, `POST /client/justificatifs`, `GET /client/justificatifs/{id}` |
| `PaiementsBackOfficeController` (paiement) | `GET /bo/paiements`, `POST /bo/paiements` |
| `ProduitBackOfficeController` (catalogue) | `GET /bo/produits`, `GET /bo/produits/nouveau`, `POST /bo/produits`, `GET /bo/produits/{id}`, `POST /bo/produits/{id}`, `POST /bo/produits/{id}/disponibilite` |
| `RecouvrementBackOfficeController` (recouvrement) | `GET /bo/recouvrement`, `GET /bo/recouvrement/dossiers/{id}`, `POST /bo/recouvrement/dossiers/{id}/notes`, `POST /bo/recouvrement/dossiers/{id}/regularisation`, `POST /bo/recouvrement/dossiers/{id}/recouvrement`, `POST /bo/recouvrement/dossiers/{id}/solde` |
| `ReinitialisationDemonstrationController` (demonstration) | `GET /bo/demonstration/reinitialisation`, `POST /bo/demonstration/reinitialisation` |
| `TableauDeBordController` (pilotage) | `GET /bo/pilotage` |

## Migrations Flyway

- V1__schema_initial.sql
- V2__parametre_regles_de_gestion.sql
- V3__event_publication.sql
- V4__catalogue_fournisseur_produit.sql
- V5__utilisateur_back_office.sql
- V6__client_emploi.sql
- V7__code_connexion_client.sql
- V8__code_connexion_par_telephone.sql
- V9__emploi_mode_remboursement.sql
- V10__document.sql
- V11__demande_score.sql
- V12__journal_audit.sql
- V13__demande_decision.sql
- V14__contrat_echeance.sql
- V15__autorisation_prelevement.sql
- V16__autorisation_suivi.sql
- V17__shedlock.sql
- V18__masrvi_code_marchand.sql
- V19__paiement.sql
- V20__paiement_ecarts.sql
- V21__commande_fournisseur.sql
- V22__paiement_echeance.sql
- V23__ecriture_comptable.sql
- V24__relance.sql
- V25__relance_retard.sql
- V26__note_recouvrement.sql
- V27__apport_capital.sql
- V28__parametre_alerte_prelevements.sql
- V29__inscription.sql
- V30__marque_seheli.sql

## Profils et configuration

- `application-demo.yml`
- `application-dev.yml`
- `application-prod.yml`
- `application.yml`
- Variables d'environnement utilisées : DB_PASSWORD, DB_URL, DB_USERNAME, SEHELI_AUTORISATIONS_REPERTOIRE, SEHELI_BO_ADMIN_IDENTIFIANT, SEHELI_BO_ADMIN_MOT_DE_PASSE, SEHELI_BO_ANALYSTE_IDENTIFIANT, SEHELI_BO_ANALYSTE_MOT_DE_PASSE, SEHELI_BO_ASSOCIE_IDENTIFIANT, SEHELI_BO_ASSOCIE_MOT_DE_PASSE, SEHELI_BO_RECOUVREMENT_IDENTIFIANT, SEHELI_BO_RECOUVREMENT_MOT_DE_PASSE, SEHELI_CLIENT_DEMO_NNI, SEHELI_CLIENT_DEMO_TELEPHONE, SEHELI_DOCUMENTS_REPERTOIRE

## Jobs planifiés

- `ShedLockConfig` (config)
- `ExpirationDossiersJob` (demande)
- `ParametreService` (parametrage)
- `RappelPrelevementJob` (recouvrement)
- `RetardsEtRelancesJob` (recouvrement)

## Tests

- 123 classes de test. Lancer une seule classe : `./mvnw -q test -Dtest=NomDuTest`.
