-- Module contrat : contrat établi à l'acceptation d'un dossier (l'offre faite au client) et ses échéances.
-- Le dossier, le client, le produit et le fournisseur appartiennent à d'autres modules : référencés
-- par identifiant, sans clé étrangère. Montants en ouguiyas entières.
-- Prix fixé à la signature : apport + somme des échéances = prix de vente, sans intérêt ni pénalité.

CREATE SEQUENCE contrat_numero_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE contrat (
    id                  UUID           PRIMARY KEY,
    numero              VARCHAR(30)    NOT NULL,
    demande_id          UUID           NOT NULL,
    client_id           UUID           NOT NULL,
    produit_id          UUID           NOT NULL,
    fournisseur_id      UUID           NOT NULL,
    designation_produit VARCHAR(200)   NOT NULL,
    prix_public         NUMERIC(12, 0) NOT NULL,
    prix_achat          NUMERIC(12, 0) NOT NULL,
    prix_vente          NUMERIC(12, 0) NOT NULL,
    apport_minimum      NUMERIC(12, 0) NOT NULL,
    apport              NUMERIC(12, 0) NOT NULL,
    nombre_mois         INTEGER        NOT NULL,
    etabli_le           TIMESTAMPTZ    NOT NULL,
    version             BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT contrat_numero_uk UNIQUE (numero),
    -- Un seul contrat par dossier.
    CONSTRAINT contrat_demande_uk UNIQUE (demande_id),
    CONSTRAINT contrat_nombre_mois_ck CHECK (nombre_mois > 0),
    CONSTRAINT contrat_montants_ck CHECK (prix_public > 0 AND prix_achat > 0 AND prix_vente > 0
        AND apport_minimum >= 0),
    CONSTRAINT contrat_apport_ck CHECK (apport >= apport_minimum AND apport < prix_vente)
);
CREATE INDEX contrat_client_idx ON contrat (client_id);

CREATE TABLE echeance (
    id            UUID           PRIMARY KEY,
    contrat_id    UUID           NOT NULL REFERENCES contrat (id),
    rang          INTEGER        NOT NULL,
    date_echeance DATE           NOT NULL,
    montant       NUMERIC(12, 0) NOT NULL,
    statut        VARCHAR(20)    NOT NULL,
    version       BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT echeance_rang_uk UNIQUE (contrat_id, rang),
    CONSTRAINT echeance_rang_ck CHECK (rang > 0),
    CONSTRAINT echeance_montant_ck CHECK (montant > 0),
    CONSTRAINT echeance_statut_ck CHECK (statut IN ('A_VENIR', 'PAYEE', 'EN_RETARD'))
);
-- Prélèvements et relances du jour : échéances par statut et date.
CREATE INDEX echeance_statut_date_idx ON echeance (statut, date_echeance);
