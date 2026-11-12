-- Module demande : dossiers de crédit et leur score.
-- Le client, le produit et le fournisseur appartiennent à d'autres modules : référencés par
-- identifiant, sans clé étrangère. Montants en ouguiyas entières.
-- Conditions figées à la soumission : apport + (n - 1) × mensualité + dernière mensualité = prix de vente.

CREATE TABLE demande (
    id                  UUID           PRIMARY KEY,
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
    mensualite          NUMERIC(12, 0) NOT NULL,
    derniere_mensualite NUMERIC(12, 0) NOT NULL,
    statut              VARCHAR(30)    NOT NULL,
    soumise_le          TIMESTAMPTZ    NOT NULL,
    statut_change_le    TIMESTAMPTZ    NOT NULL,
    version             BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT demande_statut_ck CHECK (statut IN ('DEMANDE_SOUMISE', 'EN_ANALYSE', 'ACCEPTEE', 'REFUSEE',
        'ANNULEE', 'APPORT_ET_AUTORISATION', 'COMMANDEE', 'LIVREE', 'EN_REMBOURSEMENT', 'EN_RETARD', 'SOLDEE',
        'RECOUVREMENT')),
    CONSTRAINT demande_nombre_mois_ck CHECK (nombre_mois > 0),
    CONSTRAINT demande_montants_ck CHECK (prix_public > 0 AND prix_achat > 0 AND prix_vente > 0
        AND apport_minimum >= 0 AND mensualite > 0 AND derniere_mensualite > 0),
    CONSTRAINT demande_apport_ck CHECK (apport >= apport_minimum AND apport < prix_vente),
    CONSTRAINT demande_equilibre_ck CHECK (
        apport + mensualite * (nombre_mois - 1) + derniere_mensualite = prix_vente)
);
CREATE INDEX demande_client_idx ON demande (client_id, soumise_le DESC);
-- File des dossiers du back-office, par statut.
CREATE INDEX demande_statut_idx ON demande (statut, soumise_le);

-- Score calculé à la soumission (un par demande) : la version de la grille et la raison de chaque
-- critère sont conservées. Le score recommande, l'analyste décide.
CREATE TABLE score (
    demande_id          UUID        PRIMARY KEY REFERENCES demande (id),
    version_grille      VARCHAR(10) NOT NULL,
    eligible            BOOLEAN     NOT NULL,
    note                INTEGER     NOT NULL,
    recommandation_note VARCHAR(20) NOT NULL,
    recommandation      VARCHAR(20) NOT NULL,
    calcule_le          TIMESTAMPTZ NOT NULL,
    CONSTRAINT score_note_ck CHECK (note BETWEEN 0 AND 100),
    CONSTRAINT score_recommandation_note_ck CHECK (recommandation_note IN ('FAVORABLE', 'A_EXAMINER', 'DEFAVORABLE')),
    CONSTRAINT score_recommandation_ck CHECK (recommandation IN ('FAVORABLE', 'A_EXAMINER', 'DEFAVORABLE')),
    CONSTRAINT score_ineligible_defavorable_ck CHECK (eligible OR recommandation = 'DEFAVORABLE')
);

-- Conditions éliminatoires, dans l'ordre de la grille.
CREATE TABLE score_condition (
    demande_id  UUID         NOT NULL REFERENCES score (demande_id),
    rang        INTEGER      NOT NULL,
    condition_eliminatoire VARCHAR(30) NOT NULL,
    satisfaite  BOOLEAN      NOT NULL,
    raison      VARCHAR(500) NOT NULL,
    PRIMARY KEY (demande_id, rang)
);

-- Critères de la note sur 100, dans l'ordre de la grille.
CREATE TABLE score_critere (
    demande_id  UUID         NOT NULL REFERENCES score (demande_id),
    rang        INTEGER      NOT NULL,
    critere     VARCHAR(30)  NOT NULL,
    points      INTEGER      NOT NULL,
    points_max  INTEGER      NOT NULL,
    raison      VARCHAR(500) NOT NULL,
    PRIMARY KEY (demande_id, rang),
    CONSTRAINT score_critere_points_ck CHECK (points BETWEEN 0 AND points_max)
);
