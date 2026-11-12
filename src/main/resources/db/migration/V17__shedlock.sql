-- Verrous des jobs planifiés (ShedLock, fournisseur JDBC) : un job ne tourne que sur un nœud à la fois.
-- Heures en UTC, fixées par la base (JdbcTemplateLockProvider.usingDbTime()).
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    CONSTRAINT shedlock_pk PRIMARY KEY (name)
);

-- Recherche des dossiers ACCEPTEE dont le délai d'apport et d'autorisation est dépassé.
CREATE INDEX demande_statut_change_idx ON demande (statut, statut_change_le);
