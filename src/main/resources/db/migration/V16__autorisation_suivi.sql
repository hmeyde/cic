-- Suivi de l'autorisation de prélèvement au back-office : date du dernier changement de statut et
-- document signé par le client, numérisé et déposé par le back-office (contenu rangé hors de la base,
-- sous document_signe_id). Une autorisation a un document signé si et seulement si elle n'est plus A_SIGNER.

ALTER TABLE autorisation_prelevement
    ADD COLUMN statut_change_le        TIMESTAMPTZ,
    ADD COLUMN document_signe_id       UUID,
    ADD COLUMN document_signe_format   VARCHAR(10),
    ADD COLUMN document_signe_taille   BIGINT,
    ADD COLUMN document_signe_depose_le TIMESTAMPTZ,
    ADD COLUMN document_signe_depose_par VARCHAR(100);

UPDATE autorisation_prelevement SET statut_change_le = emise_le;

ALTER TABLE autorisation_prelevement
    ALTER COLUMN statut_change_le SET NOT NULL,
    ADD CONSTRAINT autorisation_prelevement_document_ck CHECK (
        (document_signe_id IS NULL AND document_signe_format IS NULL AND document_signe_taille IS NULL
            AND document_signe_depose_le IS NULL AND document_signe_depose_par IS NULL)
        OR (document_signe_id IS NOT NULL AND document_signe_format IN ('PDF', 'JPEG', 'PNG')
            AND document_signe_taille > 0 AND document_signe_depose_le IS NOT NULL
            AND document_signe_depose_par IS NOT NULL)),
    ADD CONSTRAINT autorisation_prelevement_statut_document_ck CHECK (
        (statut = 'A_SIGNER') = (document_signe_id IS NULL));

CREATE INDEX autorisation_prelevement_emise_le_idx ON autorisation_prelevement (emise_le DESC);
