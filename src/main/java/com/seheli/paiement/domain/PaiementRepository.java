package com.seheli.paiement.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.seheli.commun.domain.Montant;

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

    /** Somme des paiements de l'apport rattachés au dossier (hors échéances) ; zéro s'il n'en a aucun. */
    Montant totalApportRecuPourDossier(UUID demandeId);
}
