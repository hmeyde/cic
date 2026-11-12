-- Module paiement : paiements reçus (Masrvi), saisis au back-office et rapprochés par référence.
-- La référence est unique : un même paiement ne peut pas être enregistré deux fois.
-- Rapproché : le contrat, le dossier et le client (autres modules) sont référencés par identifiant,
-- sans clé étrangère, avec l'apport attendu au contrat. Non rapproché : aucun d'eux.
-- Un paiement est immuable : une erreur se corrige par une opération inverse.

CREATE TABLE paiement (
    id               UUID          PRIMARY KEY,
    reference        VARCHAR(30)   NOT NULL,
    montant          NUMERIC(14,0) NOT NULL,
    recu_le          DATE          NOT NULL,
    statut           VARCHAR(20)   NOT NULL,
    contrat_id       UUID,
    numero_contrat   VARCHAR(30),
    demande_id       UUID,
    client_id        UUID,
    montant_attendu  NUMERIC(14,0),
    saisi_par        VARCHAR(100)  NOT NULL,
    saisi_le         TIMESTAMPTZ   NOT NULL,
    CONSTRAINT paiement_reference_uk UNIQUE (reference),
    CONSTRAINT paiement_reference_ck CHECK (reference ~ '^[A-Z0-9]{1,30}$'),
    CONSTRAINT paiement_montant_ck CHECK (montant > 0),
    CONSTRAINT paiement_statut_ck CHECK (
        (statut = 'RAPPROCHE' AND contrat_id IS NOT NULL AND numero_contrat IS NOT NULL AND demande_id IS NOT NULL
            AND client_id IS NOT NULL AND montant_attendu IS NOT NULL)
        OR (statut = 'NON_RAPPROCHE' AND contrat_id IS NULL AND numero_contrat IS NULL AND demande_id IS NULL
            AND client_id IS NULL AND montant_attendu IS NULL))
);
CREATE INDEX paiement_saisi_le_idx ON paiement (saisi_le DESC);
CREATE INDEX paiement_demande_idx ON paiement (demande_id) WHERE demande_id IS NOT NULL;

CREATE FUNCTION paiement_immuable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'paiement est immuable : % interdit', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER paiement_immuable_trg
    BEFORE UPDATE OR DELETE ON paiement
    FOR EACH ROW EXECUTE FUNCTION paiement_immuable();
