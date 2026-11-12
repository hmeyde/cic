package com.cic.recouvrement.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Relance due pour une échéance impayée, selon le nombre de jours écoulés depuis sa date :
 * la première à J+{@code premiere} (3), la seconde à J+{@code seconde} (7).
 *
 * <p>Une relance manquée (job arrêté, panne) est rattrapée : la première tant que la seconde n'est
 * pas due, la seconde pendant {@link #RATTRAPAGE_SECONDE_JOURS} jours. Au-delà, le dossier relève
 * du recouvrement et n'est plus relancé automatiquement.
 */
public record CalendrierRelances(int premiere, int seconde) {

    /** Durée pendant laquelle une seconde relance manquée est encore envoyée. */
    public static final int RATTRAPAGE_SECONDE_JOURS = 7;

    public CalendrierRelances {
        if (premiere < 1 || seconde <= premiere) {
            throw new IllegalArgumentException(
                    "Relances après échec invalides : 1 ≤ première (" + premiere + ") < seconde (" + seconde + ")");
        }
    }

    /** Relance à envoyer aujourd'hui pour une échéance impayée de cette date ; vide si aucune n'est due. */
    public Optional<TypeRelance> relanceDue(LocalDate dateEcheance, LocalDate aujourdhui) {
        long retard = ChronoUnit.DAYS.between(dateEcheance, aujourdhui);
        if (retard >= premiere && retard < seconde) {
            return Optional.of(TypeRelance.ECHEC_PREMIERE);
        }
        if (retard >= seconde && retard < seconde + RATTRAPAGE_SECONDE_JOURS) {
            return Optional.of(TypeRelance.ECHEC_SECONDE);
        }
        return Optional.empty();
    }

    /** Plus ancienne date d'échéance pouvant encore recevoir une relance aujourd'hui. */
    public LocalDate plusAncienneEcheance(LocalDate aujourdhui) {
        return aujourdhui.minusDays(seconde + RATTRAPAGE_SECONDE_JOURS - 1L);
    }

    /** Plus récente date d'échéance pouvant déjà recevoir une relance aujourd'hui. */
    public LocalDate plusRecenteEcheance(LocalDate aujourdhui) {
        return aujourdhui.minusDays(premiere);
    }
}
