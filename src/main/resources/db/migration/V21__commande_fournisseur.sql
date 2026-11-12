-- Module demande : commandes fournisseur. Une commande est émise quand le paiement fournisseur d'un
-- dossier APPORT_ET_AUTORISATION est validé ; elle fait passer le dossier à COMMANDEE.
-- Au plus une commande par dossier. Le contrat, le fournisseur et le produit (autres modules) sont
-- référencés par identifiant, sans clé étrangère. Le montant est le prix d'achat payé au fournisseur.
-- Une commande est immuable.

CREATE SEQUENCE commande_fournisseur_numero_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE commande_fournisseur (
    id                  UUID          PRIMARY KEY,
    numero              VARCHAR(30)   NOT NULL,
    demande_id          UUID          NOT NULL REFERENCES demande (id),
    contrat_id          UUID          NOT NULL,
    numero_contrat      VARCHAR(30)   NOT NULL,
    fournisseur_id      UUID          NOT NULL,
    produit_id          UUID          NOT NULL,
    designation_produit VARCHAR(200)  NOT NULL,
    montant             NUMERIC(12,0) NOT NULL,
    valide_par          VARCHAR(100)  NOT NULL,
    valide_le           TIMESTAMPTZ   NOT NULL,
    CONSTRAINT commande_fournisseur_numero_uk UNIQUE (numero),
    CONSTRAINT commande_fournisseur_demande_uk UNIQUE (demande_id),
    CONSTRAINT commande_fournisseur_montant_ck CHECK (montant > 0)
);
CREATE INDEX commande_fournisseur_valide_le_idx ON commande_fournisseur (valide_le DESC);

CREATE FUNCTION commande_fournisseur_immuable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'commande_fournisseur est immuable : % interdit', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER commande_fournisseur_immuable_trg
    BEFORE UPDATE OR DELETE ON commande_fournisseur
    FOR EACH ROW EXECUTE FUNCTION commande_fournisseur_immuable();
