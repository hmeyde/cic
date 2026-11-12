-- Relances après une échéance impayée : J+3 (relance.echec.premiere.jours) et J+7 (relance.echec.seconde.jours).
ALTER TABLE relance DROP CONSTRAINT relance_type_ck;
ALTER TABLE relance ADD CONSTRAINT relance_type_ck
    CHECK (type IN ('AVANT_PRELEVEMENT', 'ECHEC_PREMIERE', 'ECHEC_SECONDE'));
