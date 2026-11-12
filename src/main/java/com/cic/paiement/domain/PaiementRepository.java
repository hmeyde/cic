package com.cic.paiement.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Stockage des paiements reçus (port de sortie). */
public interface PaiementRepository {

    /**
     * Insère un nouveau paiement.
     *
     * @throws PaiementEnDoubleException un paiement porte déjà cette référence
     */
    void enregistrer(Paiement paiement);

    Optional<Paiement> parId(UUID id);

    Optional<Paiement> parReference(ReferencePaiement reference);

    /** Paiements les plus récemment saisis d'abord. */
    List<Paiement> derniers(int limite);

    /**
     * Paiements présentant un écart ({@link Paiement#ecart()}) : non rapprochés, ou rapprochés d'un
     * montant différent de l'apport attendu. Les plus récemment reçus d'abord.
     */
    List<Paiement> ecarts(int limite);
}
