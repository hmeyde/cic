-- Changement de marque : le critère « historique chez CIC » du score s'appelle désormais HISTORIQUE_SEHELI.
-- Les scores déjà calculés gardent leurs points et leur raison ; seul le code du critère change.
UPDATE score_critere SET critere = 'HISTORIQUE_SEHELI' WHERE critere = 'HISTORIQUE_CIC';
