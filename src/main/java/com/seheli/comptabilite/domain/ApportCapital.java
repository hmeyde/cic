package com.seheli.comptabilite.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.seheli.commun.domain.Montant;

/**
 * Apport en capital d'un associé : argent versé à Seheli pour financer les achats aux fournisseurs.
 * Immuable : un apport saisi n'est ni modifié ni supprimé ; une erreur se corrige par contrepassation
 * de son écriture comptable. Chaque apport est comptabilisé (banque 512 / capital 101).
 *
 * @param associe  nom de l'associé qui a versé l'apport
 * @param verseLe  date du versement, au plus tard le jour de la saisie
 * @param saisiPar identifiant de connexion du back-office
 */
public record ApportCapital(UUID id, String associe, Montant montant, LocalDate verseLe, String saisiPar,
        Instant saisiLe) {

    /** Longueur maximale du nom de l'associé. */
    public static final int ASSOCIE_LONGUEUR_MAX = 100;

    public ApportCapital {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(associe, "associe");
        Objects.requireNonNull(montant, "montant");
        Objects.requireNonNull(verseLe, "verseLe");
        Objects.requireNonNull(saisiPar, "saisiPar");
        Objects.requireNonNull(saisiLe, "saisiLe");
    }

    /**
     * Nouvel apport, nom de l'associé débarrassé de ses espaces superflus.
     *
     * @param aujourdhui date du jour : un versement ne peut pas être daté dans le futur
     * @throws ApportCapitalInvalideException associé vide ou trop long, montant nul, date future
     */
    public static ApportCapital saisir(String associe, Montant montant, LocalDate verseLe, LocalDate aujourdhui,
            String saisiPar, Instant le) {
        String nom = associe == null ? "" : associe.strip().replaceAll("\\s+", " ");
        if (nom.isEmpty() || nom.length() > ASSOCIE_LONGUEUR_MAX) {
            throw new ApportCapitalInvalideException(ApportCapitalInvalideException.ASSOCIE_INVALIDE);
        }
        if (montant == null || montant.estZero()) {
            throw new ApportCapitalInvalideException(ApportCapitalInvalideException.MONTANT_INVALIDE);
        }
        if (verseLe == null || verseLe.isAfter(Objects.requireNonNull(aujourdhui, "aujourdhui"))) {
            throw new ApportCapitalInvalideException(ApportCapitalInvalideException.DATE_INVALIDE);
        }
        return new ApportCapital(UUID.randomUUID(), nom, montant, verseLe, saisiPar, le);
    }

    /** Pas de nom d'associé : ce résumé peut finir dans les journaux. */
    @Override
    public String toString() {
        return "ApportCapital[" + id + ", " + montant + ", " + verseLe + "]";
    }
}
