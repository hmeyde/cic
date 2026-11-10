-- Migration initiale : tables créées au fur et à mesure des modules.
-- Ce fichier sera complété à chaque sprint.

CREATE TABLE parametre (
    cle     VARCHAR(100) PRIMARY KEY,
    valeur  VARCHAR(500) NOT NULL,
    description VARCHAR(255)
);

INSERT INTO parametre (cle, valeur, description) VALUES
    ('apport.minimum.pct',          '40',   'Apport minimum en % du prix de vente'),
    ('marge.cic.pct',               '4',    'Marge CIC en % du prix public par catégorie'),
    ('remise.fournisseur.min.pct',  '5',    'Remise fournisseur minimale en %'),
    ('remise.fournisseur.max.pct',  '10',   'Remise fournisseur maximale en %'),
    ('duree.min.mois',              '3',    'Durée minimale du crédit en mois'),
    ('duree.max.mois',              '6',    'Durée maximale du crédit en mois'),
    ('mensualite.salaire.max.pct',  '33',   'Part maximale du salaire pour les mensualités en %'),
    ('delai.apport.jours',          '7',    'Délai pour verser l apport après acceptation en jours'),
    ('provision.30j.pct',           '50',   'Provision à 30 jours de retard en %'),
    ('provision.90j.pct',           '100',  'Provision à 90 jours de retard en %'),
    ('perte.180j.jours',            '180',  'Passage en perte après N jours de retard'),
    ('risque.employeur.max.pct',    '20',   'Encours max sur un même employeur en % du total'),
    ('risque.fournisseur.max.pct',  '20',   'Encours max sur un même fournisseur en % du total'),
    ('encours.capital.max.pct',     '90',   'Encours max en % du capital disponible');
