-- Module audit : journal immuable des transitions et actions sensibles.
-- L'objet audité appartient à un autre module : référencé par type et identifiant, sans clé étrangère.
-- Aucune donnée personnelle hors du détail libre (motif saisi par l'analyste).

CREATE TABLE journal_audit (
    id          UUID          PRIMARY KEY,
    numero      BIGINT        GENERATED ALWAYS AS IDENTITY UNIQUE,
    le          TIMESTAMPTZ   NOT NULL,
    type_acteur VARCHAR(20)   NOT NULL,
    acteur      VARCHAR(100)  NOT NULL,
    action      VARCHAR(50)   NOT NULL,
    type_objet  VARCHAR(50)   NOT NULL,
    objet_id    UUID          NOT NULL,
    avant       VARCHAR(50),
    apres       VARCHAR(50),
    detail      VARCHAR(1000),
    CONSTRAINT journal_audit_type_acteur_ck CHECK (type_acteur IN ('CLIENT', 'BACK_OFFICE', 'SYSTEME')),
    CONSTRAINT journal_audit_transition_ck CHECK (avant IS NULL OR apres IS NOT NULL)
);
CREATE INDEX journal_audit_objet_idx ON journal_audit (type_objet, objet_id, le, numero);

-- Immuable : une correction se fait par une nouvelle entrée, jamais par modification ou suppression.
CREATE FUNCTION journal_audit_immuable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'journal_audit est immuable : % interdit', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER journal_audit_immuable_trg
    BEFORE UPDATE OR DELETE ON journal_audit
    FOR EACH ROW EXECUTE FUNCTION journal_audit_immuable();

CREATE TRIGGER journal_audit_immuable_truncate_trg
    BEFORE TRUNCATE ON journal_audit
    FOR EACH STATEMENT EXECUTE FUNCTION journal_audit_immuable();
