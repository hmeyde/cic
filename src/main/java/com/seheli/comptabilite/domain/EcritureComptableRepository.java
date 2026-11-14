package com.seheli.comptabilite.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Stockage des écritures comptables (port de sortie) : insertion seulement, jamais de modification. */
public interface EcritureComptableRepository {

    /**
     * Insère l'écriture si aucune n'existe pour sa nature et sa source.
     *
     * @return faux si le fait était déjà comptabilisé (rien n'est inséré)
     */
    boolean enregistrerSiNouvelle(EcritureComptable ecriture);

    Optional<EcritureComptable> parId(UUID id);

    Optional<EcritureComptable> parSource(NatureEcriture nature, UUID sourceId);

    /** Écritures d'un dossier, dans l'ordre où elles ont été passées. */
    List<EcritureComptable> parDemande(UUID demandeId);

    /** Soldes banque et achats de chaque dossier ayant au moins une écriture. */
    List<SoldesDossier> soldesParDossier();

    /** Écritures les plus récentes d'abord. */
    List<EcritureComptable> dernieres(int limite);
}
