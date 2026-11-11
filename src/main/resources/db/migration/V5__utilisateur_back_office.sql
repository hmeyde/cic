-- Module identite : comptes du back-office (équipe CIC), connexion par identifiant et mot de passe.
-- Le mot de passe n'est jamais stocké en clair : seul son hachage BCrypt l'est.
-- Un rôle par compte : la séparation des tâches (décision / paiement fournisseur) s'appuie sur
-- des personnes distinctes, pas sur le cumul de rôles.

CREATE TABLE utilisateur_back_office (
    id                UUID         PRIMARY KEY,
    identifiant       VARCHAR(50)  NOT NULL,
    mot_de_passe_hash VARCHAR(100) NOT NULL,
    role              VARCHAR(20)  NOT NULL,
    actif             BOOLEAN      NOT NULL DEFAULT TRUE,
    cree_le           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT utilisateur_back_office_role_ck CHECK (role IN ('ANALYSTE', 'RECOUVREMENT', 'ADMIN', 'ASSOCIE'))
);
CREATE UNIQUE INDEX utilisateur_back_office_identifiant_uk ON utilisateur_back_office (lower(identifiant));
