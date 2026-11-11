-- Module catalogue : fournisseurs (remise négociée) et produits (catégorie, prix public, disponibilité).
-- Identifiants UUID générés par l'application. Montants en ouguiyas entières (NUMERIC sans décimale).
-- Les bornes métier de la remise (5 à 10 %) sont dans la table parametre et contrôlées par
-- l'application ; la base ne garantit que la validité du taux.

CREATE TABLE fournisseur (
    id                  UUID          PRIMARY KEY,
    nom                 VARCHAR(150)  NOT NULL,
    remise_negociee_pct NUMERIC(5, 2) NOT NULL,
    actif               BOOLEAN       NOT NULL DEFAULT TRUE,
    cree_le             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fournisseur_remise_ck CHECK (remise_negociee_pct >= 0 AND remise_negociee_pct < 100)
);
CREATE UNIQUE INDEX fournisseur_nom_uk ON fournisseur (lower(nom));

CREATE TABLE produit (
    id             UUID           PRIMARY KEY,
    reference      VARCHAR(50)    NOT NULL,
    designation    VARCHAR(200)   NOT NULL,
    marque         VARCHAR(100)   NOT NULL,
    categorie      VARCHAR(30)    NOT NULL,
    fournisseur_id UUID           NOT NULL REFERENCES fournisseur (id),
    prix_public    NUMERIC(12, 0) NOT NULL,
    disponible     BOOLEAN        NOT NULL DEFAULT TRUE,
    cree_le        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT produit_reference_uk UNIQUE (reference),
    CONSTRAINT produit_categorie_ck CHECK (categorie IN ('TELEPHONE', 'ELECTROMENAGER')),
    CONSTRAINT produit_prix_public_ck CHECK (prix_public > 0)
);
CREATE INDEX produit_fournisseur_idx ON produit (fournisseur_id);
CREATE INDEX produit_disponible_categorie_idx ON produit (categorie) WHERE disponible;
