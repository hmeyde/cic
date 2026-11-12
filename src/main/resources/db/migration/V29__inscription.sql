-- Module inscription : pré-inscriptions de la page publique (preuve de concept).
-- Téléphone : 8 chiffres sans indicatif, unique (anti-doublon). Consentement obligatoire et horodaté.
-- Une tranche de salaire seulement : le salaire exact est demandé à la création du compte client.
CREATE TABLE inscription (
    id                      UUID         PRIMARY KEY,
    nom                     VARCHAR(100) NOT NULL,
    telephone               VARCHAR(8)   NOT NULL,
    employeur               VARCHAR(150) NOT NULL,
    type_contrat            VARCHAR(20)  NOT NULL,
    tranche_salaire         VARCHAR(30)  NOT NULL,
    produit_souhaite        VARCHAR(150) NOT NULL,
    consentement_donnees_le TIMESTAMPTZ  NOT NULL,
    cree_le                 TIMESTAMPTZ  NOT NULL,
    CONSTRAINT inscription_telephone_uk UNIQUE (telephone),
    CONSTRAINT inscription_telephone_ck CHECK (telephone ~ '^[0-9]{8}$'),
    CONSTRAINT inscription_type_contrat_ck CHECK (type_contrat IN ('FONCTIONNAIRE', 'CDI', 'CDD')),
    CONSTRAINT inscription_tranche_salaire_ck CHECK (tranche_salaire IN
        ('MOINS_DE_10000', 'DE_10000_A_20000', 'DE_20000_A_40000', 'PLUS_DE_40000')),
    CONSTRAINT inscription_nom_ck CHECK (length(btrim(nom)) > 0),
    CONSTRAINT inscription_employeur_ck CHECK (length(btrim(employeur)) > 0),
    CONSTRAINT inscription_produit_ck CHECK (length(btrim(produit_souhaite)) > 0)
);
CREATE INDEX inscription_cree_le_idx ON inscription (cree_le DESC);
