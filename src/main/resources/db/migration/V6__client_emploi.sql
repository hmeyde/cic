-- Module identite : clients (NNI unique) et leur emploi actuel.
-- NNI : 10 chiffres ; téléphone : 8 chiffres sans indicatif, unique car il reçoit le code de connexion.
-- Le consentement à l'usage des données est obligatoire et horodaté.
-- Un emploi par client (l'emploi actuel) ; date de fin obligatoire pour un CDD et seulement pour lui.
-- Salaire en ouguiyas entières.

CREATE TABLE client (
    id                      UUID         PRIMARY KEY,
    nni                     VARCHAR(10)  NOT NULL,
    nom                     VARCHAR(100) NOT NULL,
    prenom                  VARCHAR(100) NOT NULL,
    date_naissance          DATE         NOT NULL,
    telephone               VARCHAR(8)   NOT NULL,
    consentement_donnees_le TIMESTAMPTZ  NOT NULL,
    nni_verifie             BOOLEAN      NOT NULL DEFAULT FALSE,
    piece_identite_verifiee BOOLEAN      NOT NULL DEFAULT FALSE,
    cree_le                 TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT client_nni_uk UNIQUE (nni),
    CONSTRAINT client_telephone_uk UNIQUE (telephone),
    CONSTRAINT client_nni_ck CHECK (nni ~ '^[0-9]{10}$'),
    CONSTRAINT client_telephone_ck CHECK (telephone ~ '^[0-9]{8}$')
);

CREATE TABLE emploi (
    id                       UUID           PRIMARY KEY,
    client_id                UUID           NOT NULL REFERENCES client (id),
    employeur                VARCHAR(150)   NOT NULL,
    type_contrat             VARCHAR(20)    NOT NULL,
    date_debut               DATE           NOT NULL,
    date_fin                 DATE,
    salaire_net              NUMERIC(12, 0) NOT NULL,
    jour_paie                SMALLINT       NOT NULL,
    salaire_verse_sur_compte BOOLEAN        NOT NULL,
    salaire_domicilie_bmci   BOOLEAN        NOT NULL DEFAULT FALSE,
    cree_le                  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT emploi_client_uk UNIQUE (client_id),
    CONSTRAINT emploi_type_contrat_ck CHECK (type_contrat IN ('FONCTIONNAIRE', 'CDI', 'CDD')),
    CONSTRAINT emploi_date_fin_ck CHECK (
        (type_contrat = 'CDD' AND date_fin IS NOT NULL AND date_fin > date_debut)
        OR (type_contrat <> 'CDD' AND date_fin IS NULL)),
    CONSTRAINT emploi_salaire_net_ck CHECK (salaire_net > 0),
    CONSTRAINT emploi_jour_paie_ck CHECK (jour_paie BETWEEN 1 AND 31),
    CONSTRAINT emploi_domiciliation_ck CHECK (NOT salaire_domicilie_bmci OR salaire_verse_sur_compte)
);
-- Concentration du risque par employeur (au plus 20 % de l'encours).
CREATE INDEX emploi_employeur_idx ON emploi (lower(employeur));
