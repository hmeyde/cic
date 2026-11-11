-- Table parametre : règles de gestion de CLAUDE.md, modifiables sans redéploiement.
-- V1 a créé une première version de la table ; on la complète (type, unité, date de
-- modification) et on la remplit avec l'ensemble des règles, sous des clés homogènes.

DELETE FROM parametre;

ALTER TABLE parametre
    ADD COLUMN type       VARCHAR(20)  NOT NULL,
    ADD COLUMN unite      VARCHAR(20),
    ADD COLUMN modifie_le TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ALTER COLUMN description SET NOT NULL,
    ADD CONSTRAINT parametre_type_ck CHECK (type IN ('ENTIER', 'POURCENTAGE', 'FRACTION', 'TEXTE'));

INSERT INTO parametre (cle, valeur, type, unite, description) VALUES
    -- Prix et marge
    ('apport.minimum.pct',              '40',  'POURCENTAGE', '%',    'Apport minimum en % du prix de vente'),
    ('marge.cic.defaut.pct',            '4',   'POURCENTAGE', '%',    'Marge CIC en % du prix public, valeur par défaut d''une catégorie de produit'),
    ('remise.fournisseur.min.pct',      '5',   'POURCENTAGE', '%',    'Remise fournisseur minimale en % du prix public'),
    ('remise.fournisseur.max.pct',      '10',  'POURCENTAGE', '%',    'Remise fournisseur maximale en % du prix public'),
    -- Durée et capacité de remboursement
    ('duree.min.mois',                  '3',   'ENTIER',      'mois', 'Durée minimale du crédit'),
    ('duree.max.mois',                  '6',   'ENTIER',      'mois', 'Durée maximale du crédit'),
    ('endettement.max.fraction',        '1/3', 'FRACTION',    NULL,   'Part maximale du salaire net consacrée au total des mensualités CIC du client'),
    ('dossier.actif.max',               '1',   'ENTIER',      NULL,   'Nombre maximal de dossiers actifs par client'),
    ('age.minimum.ans',                 '18',  'ENTIER',      'ans',  'Âge minimum du client'),
    -- Délais
    ('delai.apport.autorisation.jours', '7',   'ENTIER',      'jours','Délai après acceptation pour l''apport et l''autorisation de prélèvement, sinon ANNULEE'),
    -- Relances
    ('relance.avant.prelevement.jours', '2',   'ENTIER',      'jours','Envoi du message de rappel N jours avant chaque prélèvement'),
    ('relance.echec.premiere.jours',    '3',   'ENTIER',      'jours','Première relance N jours après un échec de prélèvement'),
    ('relance.echec.seconde.jours',     '7',   'ENTIER',      'jours','Seconde relance N jours après un échec de prélèvement'),
    -- Risque
    ('risque.employeur.max.pct',        '20',  'POURCENTAGE', '%',    'Encours maximal sur un même employeur, en % de l''encours total'),
    ('risque.fournisseur.max.pct',      '20',  'POURCENTAGE', '%',    'Encours maximal sur un même fournisseur, en % de l''encours total'),
    ('encours.capital.max.pct',         '90',  'POURCENTAGE', '%',    'Aucun nouveau dossier si l''encours dépasse ce % du capital disponible'),
    ('provision.palier1.jours',         '30',  'ENTIER',      'jours','Retard à partir duquel s''applique la première provision'),
    ('provision.palier1.pct',           '50',  'POURCENTAGE', '%',    'Première provision en % du restant dû'),
    ('provision.palier2.jours',         '90',  'ENTIER',      'jours','Retard à partir duquel s''applique la seconde provision'),
    ('provision.palier2.pct',           '100', 'POURCENTAGE', '%',    'Seconde provision en % du restant dû'),
    ('perte.retard.jours',              '180', 'ENTIER',      'jours','Retard à partir duquel le restant dû passe en perte'),
    -- Score
    ('score.grille.version',            'v1',  'TEXTE',       NULL,   'Version de la grille de score appliquée aux nouvelles demandes'),
    ('score.seuil.favorable',           '70',  'ENTIER',      'points','Note minimale pour une recommandation FAVORABLE'),
    ('score.seuil.a.examiner',          '50',  'ENTIER',      'points','Note minimale pour une recommandation A_EXAMINER (en dessous : DEFAVORABLE)');

