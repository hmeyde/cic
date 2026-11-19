-- Simulation client : borne haute du curseur d'apport. CLAUDE.md ne fixe que le minimum (40 %) ;
-- 80 % est la valeur de départ retenue (au-delà, l'achat à crédit perd son intérêt), modifiable sans redéploiement.
INSERT INTO parametre (cle, valeur, type, unite, description) VALUES
    ('apport.simulation.max.pct', '80', 'POURCENTAGE', '%',
     'Apport maximum proposé par le curseur de la simulation client, en % du prix de vente');
