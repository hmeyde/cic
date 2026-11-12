-- Module comptabilité : écritures en partie double, immuables. Chaque mouvement d'argent (apport
-- encaissé, paiement fournisseur, échéance encaissée, paiement à identifier) et la vente constatée à
-- la remise du produit produisent une écriture ; une correction se fait par une écriture inverse
-- (contrepassation), jamais par modification ni suppression.
--
-- Une écriture est équilibrée : total des débits = total des crédits, contrôlé à la validation de la
-- transaction (trigger différé). Le couple (nature, source) est unique : un même fait n'est
-- comptabilisé qu'une fois, même si son événement est traité deux fois ; une écriture n'est
-- contrepassée qu'une fois. Dossier et contrat (autres modules) référencés par identifiant, sans clé
-- étrangère.

CREATE TABLE ecriture_comptable (
    id             UUID          PRIMARY KEY,
    nature         VARCHAR(30)   NOT NULL,
    source_id      UUID          NOT NULL,
    date_ecriture  DATE          NOT NULL,
    libelle        VARCHAR(200)  NOT NULL,
    demande_id     UUID,
    contrat_id     UUID,
    contrepasse_id UUID          REFERENCES ecriture_comptable (id),
    passee_le      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ecriture_source_uk UNIQUE (nature, source_id),
    CONSTRAINT ecriture_nature_ck CHECK (nature IN ('APPORT_ENCAISSE', 'PAIEMENT_FOURNISSEUR', 'VENTE_LIVREE',
        'ECHEANCE_ENCAISSEE', 'PAIEMENT_A_IDENTIFIER', 'CONTREPASSATION')),
    CONSTRAINT ecriture_contrepassation_ck CHECK (
        (nature = 'CONTREPASSATION') = (contrepasse_id IS NOT NULL)
        AND (contrepasse_id IS NULL OR contrepasse_id = source_id))
);
CREATE INDEX ecriture_date_idx ON ecriture_comptable (date_ecriture DESC, passee_le DESC);
CREATE INDEX ecriture_demande_idx ON ecriture_comptable (demande_id) WHERE demande_id IS NOT NULL;

CREATE TABLE ligne_ecriture (
    ecriture_id UUID          NOT NULL REFERENCES ecriture_comptable (id),
    position    INTEGER       NOT NULL,
    compte      VARCHAR(10)   NOT NULL,
    sens        VARCHAR(6)    NOT NULL,
    montant     NUMERIC(14,0) NOT NULL,
    PRIMARY KEY (ecriture_id, position),
    CONSTRAINT ligne_ecriture_sens_ck CHECK (sens IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ligne_ecriture_montant_ck CHECK (montant > 0)
);
CREATE INDEX ligne_ecriture_compte_idx ON ligne_ecriture (compte);

CREATE FUNCTION ecriture_comptable_immuable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION '% est immuable : % interdit', TG_TABLE_NAME, TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ecriture_comptable_immuable_trg
    BEFORE UPDATE OR DELETE ON ecriture_comptable
    FOR EACH ROW EXECUTE FUNCTION ecriture_comptable_immuable();

CREATE TRIGGER ligne_ecriture_immuable_trg
    BEFORE UPDATE OR DELETE ON ligne_ecriture
    FOR EACH ROW EXECUTE FUNCTION ecriture_comptable_immuable();

-- Partie double : contrôle à la fin de la transaction, quand toutes les lignes sont insérées.
CREATE FUNCTION ecriture_comptable_equilibree() RETURNS trigger AS $$
DECLARE
    debit  NUMERIC;
    credit NUMERIC;
    lignes INTEGER;
BEGIN
    SELECT coalesce(sum(montant) FILTER (WHERE sens = 'DEBIT'), 0),
           coalesce(sum(montant) FILTER (WHERE sens = 'CREDIT'), 0),
           count(*)
      INTO debit, credit, lignes
      FROM ligne_ecriture WHERE ecriture_id = NEW.id;
    IF lignes < 2 OR debit <> credit THEN
        RAISE EXCEPTION 'écriture % déséquilibrée : débit %, crédit %, % lignes', NEW.id, debit, credit, lignes;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER ecriture_comptable_equilibree_trg
    AFTER INSERT ON ecriture_comptable
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION ecriture_comptable_equilibree();
