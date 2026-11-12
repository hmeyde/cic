-- Relances envoyées au client (recouvrement). Une relance d'un type donné n'est envoyée qu'une fois
-- par échéance : la contrainte d'unicité rend le job de rappel idempotent, même relancé ou exécuté
-- sur plusieurs nœuds. Échéance et contrat référencés par identifiant (module contrat, sans clé étrangère).
CREATE TABLE relance (
    id             UUID         PRIMARY KEY,
    echeance_id    UUID         NOT NULL,
    contrat_id     UUID         NOT NULL,
    type           VARCHAR(30)  NOT NULL,
    date_echeance  DATE         NOT NULL,
    envoyee_le     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT relance_echeance_type_uk UNIQUE (echeance_id, type),
    CONSTRAINT relance_type_ck CHECK (type IN ('AVANT_PRELEVEMENT'))
);

-- Le rappel part la veille du prélèvement.
UPDATE parametre SET valeur = '1', description = 'Envoi du message de rappel N jours avant chaque prélèvement (1 = la veille)'
 WHERE cle = 'relance.avant.prelevement.jours';

-- Numéro WhatsApp de CIC (8 chiffres, sans +222), cible du lien wa.me prérempli du rappel (valeur de démonstration).
INSERT INTO parametre (cle, valeur, type, unite, description) VALUES
    ('cic.whatsapp.numero', '20000000', 'TEXTE', NULL, 'Numéro WhatsApp de CIC (8 chiffres, sans +222) proposé au client dans les rappels');
