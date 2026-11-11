-- Module identite : justificatifs envoyés par les clients (métadonnées seulement).
-- Le contenu est rangé hors de la base et du dossier public, dans un fichier nommé par l'identifiant
-- du document. Le nom d'origine du fichier n'est pas conservé. PDF, JPEG ou PNG, 5 Mo au plus.
CREATE TABLE document (
    id            UUID        PRIMARY KEY,
    client_id     UUID        NOT NULL REFERENCES client (id),
    type_document VARCHAR(30) NOT NULL,
    format        VARCHAR(10) NOT NULL,
    taille_octets BIGINT      NOT NULL,
    recu_le       TIMESTAMPTZ NOT NULL,
    CONSTRAINT document_type_ck CHECK (type_document IN
        ('PIECE_IDENTITE', 'CONTRAT_OU_ATTESTATION', 'BULLETIN_PAIE', 'RIB')),
    CONSTRAINT document_format_ck CHECK (format IN ('PDF', 'JPEG', 'PNG')),
    CONSTRAINT document_taille_ck CHECK (taille_octets BETWEEN 1 AND 5242880)
);
CREATE INDEX document_client_idx ON document (client_id, recu_le DESC);
CREATE INDEX document_recu_le_idx ON document (recu_le DESC);
