-- Module identite : connexion du client par téléphone et code à usage unique (6 chiffres).
-- Un code en cours au plus par client (clé = client) : un nouveau code remplace le précédent.
-- Le code n'est jamais stocké en clair : seul son hachage BCrypt l'est.

CREATE TABLE code_connexion (
    client_id  UUID         PRIMARY KEY REFERENCES client (id),
    code_hash  VARCHAR(100) NOT NULL,
    emis_le    TIMESTAMPTZ  NOT NULL,
    expire_le  TIMESTAMPTZ  NOT NULL,
    essais_max SMALLINT     NOT NULL,
    essais     SMALLINT     NOT NULL DEFAULT 0,
    utilise    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT code_connexion_expiration_ck CHECK (expire_le > emis_le),
    CONSTRAINT code_connexion_essais_ck CHECK (essais_max >= 1 AND essais BETWEEN 0 AND essais_max)
);

INSERT INTO parametre (cle, valeur, type, unite, description) VALUES
    ('connexion.code.validite.minutes', '5',  'ENTIER', 'minutes',  'Durée de validité du code de connexion envoyé au client'),
    ('connexion.code.essais.max',       '5',  'ENTIER', NULL,       'Nombre maximal d''essais pour un même code de connexion'),
    ('connexion.code.renvoi.secondes',  '60', 'ENTIER', 'secondes', 'Délai minimal avant l''envoi d''un nouveau code au même client');
