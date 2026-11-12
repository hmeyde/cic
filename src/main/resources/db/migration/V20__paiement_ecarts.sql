-- Liste des écarts de paiement : paiements non rapprochés (référence inconnue) ou d'un montant
-- différent de l'apport attendu. Index partiel : seuls les paiements en écart y figurent.
CREATE INDEX paiement_ecart_idx ON paiement (recu_le DESC, saisi_le DESC)
    WHERE contrat_id IS NULL OR montant <> montant_attendu;
