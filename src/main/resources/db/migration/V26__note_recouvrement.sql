-- Notes datées du recouvrement sur un dossier (appel au client, promesse de paiement...). Une note
-- n'est jamais modifiée : une correction se fait par une nouvelle note. Dossier référencé par
-- identifiant (module demande, sans clé étrangère).
CREATE TABLE note_recouvrement (
    id          UUID           PRIMARY KEY,
    demande_id  UUID           NOT NULL,
    auteur      VARCHAR(100)   NOT NULL,
    texte       VARCHAR(1000)  NOT NULL,
    ecrite_le   TIMESTAMPTZ    NOT NULL,
    CONSTRAINT note_recouvrement_texte_ck CHECK (length(btrim(texte)) > 0)
);

CREATE INDEX note_recouvrement_demande_idx ON note_recouvrement (demande_id, ecrite_le DESC);
CREATE INDEX relance_contrat_idx ON relance (contrat_id);
