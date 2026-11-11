-- Module identite : le code de connexion est rattaché au téléphone, et non plus au client, pour qu'un
-- nouveau client puisse prouver que le numéro est à lui avant de créer son compte.
-- Les codes sont éphémères (5 minutes) : la table est recréée sans reprise des codes en cours.

DROP TABLE code_connexion;

CREATE TABLE code_connexion (
    telephone  VARCHAR(8)   PRIMARY KEY,
    code_hash  VARCHAR(100) NOT NULL,
    emis_le    TIMESTAMPTZ  NOT NULL,
    expire_le  TIMESTAMPTZ  NOT NULL,
    essais_max SMALLINT     NOT NULL,
    essais     SMALLINT     NOT NULL DEFAULT 0,
    utilise    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT code_connexion_telephone_ck CHECK (telephone ~ '^[0-9]{8}$'),
    CONSTRAINT code_connexion_expiration_ck CHECK (expire_le > emis_le),
    CONSTRAINT code_connexion_essais_ck CHECK (essais_max >= 1 AND essais BETWEEN 0 AND essais_max)
);

UPDATE parametre
   SET description = 'Délai minimal avant l''envoi d''un nouveau code au même téléphone'
 WHERE cle = 'connexion.code.renvoi.secondes';
