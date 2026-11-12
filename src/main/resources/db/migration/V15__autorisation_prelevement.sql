-- Module contrat : autorisation de prélèvement des mensualités, une par contrat, émise avec lui à
-- l'acceptation du dossier et à signer par le client. Type VIREMENT_PERMANENT par défaut.
-- Le dossier et le client appartiennent à d'autres modules : référencés par identifiant, sans clé étrangère.
-- A_SIGNER : émise, en attente de la signature du client (état initial, avant SIGNEE).

CREATE TABLE autorisation_prelevement (
    id          UUID        PRIMARY KEY,
    contrat_id  UUID        NOT NULL REFERENCES contrat (id),
    demande_id  UUID        NOT NULL,
    client_id   UUID        NOT NULL,
    type        VARCHAR(20) NOT NULL,
    statut      VARCHAR(20) NOT NULL,
    emise_le    TIMESTAMPTZ NOT NULL,
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT autorisation_prelevement_contrat_uk UNIQUE (contrat_id),
    CONSTRAINT autorisation_prelevement_type_ck CHECK (type IN ('VIREMENT_PERMANENT', 'DOMICILIATION_BMCI',
        'RETENUE_EMPLOYEUR')),
    CONSTRAINT autorisation_prelevement_statut_ck CHECK (statut IN ('A_SIGNER', 'SIGNEE', 'TRANSMISE', 'ACCEPTEE',
        'ACTIVE', 'REVOQUEE'))
);
-- Suivi des autorisations par statut au back-office.
CREATE INDEX autorisation_prelevement_statut_idx ON autorisation_prelevement (statut);

-- Contrats déjà établis : leur autorisation est émise à la date du contrat.
INSERT INTO autorisation_prelevement (id, contrat_id, demande_id, client_id, type, statut, emise_le)
SELECT gen_random_uuid(), c.id, c.demande_id, c.client_id, 'VIREMENT_PERMANENT', 'A_SIGNER', c.etabli_le
FROM contrat c;

-- Compte de CIC qui reçoit les virements permanents des mensualités (valeurs de démonstration à remplacer).
INSERT INTO parametre (cle, valeur, type, unite, description) VALUES
    ('cic.compte.titulaire', 'CIC SARL',                   'TEXTE', NULL, 'Titulaire du compte de CIC qui reçoit les mensualités'),
    ('cic.compte.banque',    'BMCI',                       'TEXTE', NULL, 'Banque du compte de CIC qui reçoit les mensualités'),
    ('cic.compte.numero',    '00020 00101 00001234567 53', 'TEXTE', NULL, 'Numéro de compte (RIB) de CIC qui reçoit les mensualités');
