package com.cic.contrat.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Stockage des autorisations de prélèvement (port de sortie). */
public interface AutorisationPrelevementRepository {

    /** Insère une nouvelle autorisation. */
    void enregistrer(AutorisationPrelevement autorisation);

    /** Enregistre le statut et le document signé d'une autorisation existante. */
    void mettreAJour(AutorisationPrelevement autorisation);

    Optional<AutorisationPrelevement> parId(UUID id);

    /** Autorisation verrouillée jusqu'à la fin de la transaction : deux suivis ne la modifient pas en même temps. */
    Optional<AutorisationPrelevement> parIdVerrouillee(UUID id);

    /** Autorisation du contrat ; au plus une par contrat. */
    Optional<AutorisationPrelevement> parContrat(UUID contratId);

    /**
     * Autorisations les plus récemment émises d'abord.
     *
     * @param statut filtre ; vide pour tous les statuts
     */
    List<AutorisationPrelevement> dernieres(Optional<StatutAutorisation> statut, int limite);
}
