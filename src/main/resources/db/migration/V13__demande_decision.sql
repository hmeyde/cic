-- Décision de l'analyste sur un dossier : auteur (identifiant du back-office), date et motif d'un refus.
-- L'auteur est conservé pour la séparation des tâches : qui décide d'un dossier ne valide pas son
-- paiement fournisseur.

ALTER TABLE demande
    ADD COLUMN decide_par  VARCHAR(100),
    ADD COLUMN decide_le   TIMESTAMPTZ,
    ADD COLUMN motif_refus VARCHAR(500),
    ADD CONSTRAINT demande_decision_ck CHECK ((decide_par IS NULL) = (decide_le IS NULL)),
    ADD CONSTRAINT demande_decision_statut_ck CHECK (
        decide_par IS NOT NULL OR statut IN ('DEMANDE_SOUMISE', 'EN_ANALYSE')),
    ADD CONSTRAINT demande_motif_refus_ck CHECK ((statut = 'REFUSEE') = (motif_refus IS NOT NULL));
