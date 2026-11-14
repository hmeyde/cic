package com.seheli.demande.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Stockage des commandes fournisseur (port de sortie). */
public interface CommandeFournisseurRepository {

    /** Insère une nouvelle commande ; au plus une par dossier. */
    void enregistrer(CommandeFournisseur commande);

    Optional<CommandeFournisseur> parDemande(UUID demandeId);

    /** Commandes les plus récemment validées d'abord. */
    List<CommandeFournisseur> dernieres(int limite);

    /** Numéro d'ordre suivant, unique, pour numéroter les bons de commande. */
    long prochainNumeroOrdre();
}
