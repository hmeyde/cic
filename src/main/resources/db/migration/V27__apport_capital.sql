-- Module comptabilité : apports en capital des associés. Un apport n'est jamais modifié ni supprimé ;
-- il est comptabilisé (banque 512 / capital 101) par une écriture de nature APPORT_CAPITAL dont la
-- source est l'apport ; une erreur se corrige par contrepassation de cette écriture.
CREATE TABLE apport_capital (
    id         UUID           PRIMARY KEY,
    associe    VARCHAR(100)   NOT NULL,
    montant    NUMERIC(14,0)  NOT NULL,
    verse_le   DATE           NOT NULL,
    saisi_par  VARCHAR(100)   NOT NULL,
    saisi_le   TIMESTAMPTZ    NOT NULL,
    CONSTRAINT apport_capital_montant_ck CHECK (montant > 0),
    CONSTRAINT apport_capital_associe_ck CHECK (length(btrim(associe)) > 0)
);
CREATE INDEX apport_capital_verse_idx ON apport_capital (verse_le DESC, saisi_le DESC);

CREATE TRIGGER apport_capital_immuable_trg
    BEFORE UPDATE OR DELETE ON apport_capital
    FOR EACH ROW EXECUTE FUNCTION ecriture_comptable_immuable();

ALTER TABLE ecriture_comptable DROP CONSTRAINT ecriture_nature_ck;
ALTER TABLE ecriture_comptable ADD CONSTRAINT ecriture_nature_ck CHECK (nature IN ('APPORT_ENCAISSE',
    'PAIEMENT_FOURNISSEUR', 'VENTE_LIVREE', 'ECHEANCE_ENCAISSEE', 'PAIEMENT_A_IDENTIFIER', 'APPORT_CAPITAL',
    'CONTREPASSATION'));
