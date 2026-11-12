-- Module paiement : un paiement rapproché règle soit l'apport du dossier (référence AP…), soit l'une
-- de ses échéances (référence EC…, avec le rang de l'échéance). Le montant attendu est alors celui de
-- l'échéance. Les paiements déjà rapprochés sont des apports.

ALTER TABLE paiement ADD COLUMN objet VARCHAR(10);
ALTER TABLE paiement ADD COLUMN rang_echeance INTEGER;

-- Reprise des paiements existants : le trigger d'immuabilité est suspendu le temps de la migration.
ALTER TABLE paiement DISABLE TRIGGER paiement_immuable_trg;
UPDATE paiement SET objet = 'APPORT' WHERE statut = 'RAPPROCHE';
ALTER TABLE paiement ENABLE TRIGGER paiement_immuable_trg;

ALTER TABLE paiement DROP CONSTRAINT paiement_statut_ck;
ALTER TABLE paiement ADD CONSTRAINT paiement_statut_ck CHECK (
    (statut = 'RAPPROCHE' AND contrat_id IS NOT NULL AND numero_contrat IS NOT NULL AND demande_id IS NOT NULL
        AND client_id IS NOT NULL AND montant_attendu IS NOT NULL AND objet IS NOT NULL)
    OR (statut = 'NON_RAPPROCHE' AND contrat_id IS NULL AND numero_contrat IS NULL AND demande_id IS NULL
        AND client_id IS NULL AND montant_attendu IS NULL AND objet IS NULL AND rang_echeance IS NULL));
ALTER TABLE paiement ADD CONSTRAINT paiement_objet_ck CHECK (
    objet IS NULL
    OR (objet = 'APPORT' AND rang_echeance IS NULL)
    OR (objet = 'ECHEANCE' AND rang_echeance > 0));
