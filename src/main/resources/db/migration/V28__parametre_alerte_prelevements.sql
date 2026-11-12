-- Tableau de bord des associés : seuil d'alerte sur le taux de prélèvements réussis.
-- CLAUDE.md ne fixe pas de valeur ; 90 % est la valeur de départ retenue, modifiable sans redéploiement.
INSERT INTO parametre (cle, valeur, type, unite, description) VALUES
    ('alerte.prelevements.reussis.min.pct', '90', 'POURCENTAGE', '%',
     'Alerte au tableau de bord si la part des échéances échues payées descend sous ce %');
