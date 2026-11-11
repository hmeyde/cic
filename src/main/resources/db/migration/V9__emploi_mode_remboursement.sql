-- Mode de remboursement choisi par le client avec son emploi (mêmes valeurs que le type d'autorisation
-- de prélèvement). Les emplois déjà enregistrés reçoivent le mode que leur versement de salaire permet.
ALTER TABLE emploi ADD COLUMN mode_remboursement VARCHAR(20);

UPDATE emploi SET mode_remboursement = CASE
    WHEN salaire_domicilie_bmci THEN 'DOMICILIATION_BMCI'
    WHEN salaire_verse_sur_compte THEN 'VIREMENT_PERMANENT'
    ELSE 'RETENUE_EMPLOYEUR'
END;

ALTER TABLE emploi ALTER COLUMN mode_remboursement SET NOT NULL;
ALTER TABLE emploi ADD CONSTRAINT emploi_mode_remboursement_ck CHECK (
    (mode_remboursement = 'RETENUE_EMPLOYEUR')
    OR (mode_remboursement = 'DOMICILIATION_BMCI' AND salaire_domicilie_bmci)
    OR (mode_remboursement = 'VIREMENT_PERMANENT' AND salaire_verse_sur_compte));
