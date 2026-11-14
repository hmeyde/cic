-- Changement de marque : CIC devient Seheli (سهلي).
-- Raison sociale imprimée sur les PDF (contrat, autorisation de prélèvement, bon de commande).
INSERT INTO parametre (cle, valeur, type, unite, description) VALUES
    ('societe.raison-sociale', 'Seheli', 'TEXTE', NULL,
     'Raison sociale de la société, imprimée sur le contrat, l''autorisation de prélèvement et le bon de commande');

-- Titulaire du compte qui reçoit les mensualités : seulement s'il porte encore la valeur de démonstration.
UPDATE parametre SET valeur = 'Seheli' WHERE cle = 'cic.compte.titulaire' AND valeur = 'CIC SARL';

-- Descriptions des paramètres : le nom de la marque n'y figure plus.
UPDATE parametre SET description = replace(description, 'CIC', 'Seheli') WHERE description LIKE '%CIC%';
