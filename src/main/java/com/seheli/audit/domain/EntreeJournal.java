package com.seheli.audit.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Entrée du journal d'audit, immuable : qui a fait quoi, sur quel objet, quand ; pour une
 * transition, le statut avant et après ; un détail libre (motif d'un refus, pièce demandée).
 *
 * <p>Une entrée n'est jamais modifiée ni supprimée : une correction se fait par une nouvelle entrée.
 *
 * @param action code stable de l'action, par exemple {@code ACCEPTATION}
 * @param avant  statut avant l'action ; vide hors transition ou à la création de l'objet
 * @param apres  statut après l'action ; vide hors transition
 * @param detail précision libre ; vide si aucune
 */
public record EntreeJournal(UUID id, Instant le, Acteur acteur, String action, ObjetAudite objet,
        Optional<String> avant, Optional<String> apres, Optional<String> detail) {

    public static final int LONGUEUR_ACTION_MAX = 50;
    public static final int LONGUEUR_STATUT_MAX = 50;
    public static final int LONGUEUR_DETAIL_MAX = 1000;

    public EntreeJournal {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(le, "le");
        Objects.requireNonNull(acteur, "acteur");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(objet, "objet");
        Objects.requireNonNull(avant, "avant");
        Objects.requireNonNull(apres, "apres");
        Objects.requireNonNull(detail, "detail");
        verifier(action, LONGUEUR_ACTION_MAX, "action");
        avant.ifPresent(a -> verifier(a, LONGUEUR_STATUT_MAX, "avant"));
        apres.ifPresent(a -> verifier(a, LONGUEUR_STATUT_MAX, "apres"));
        detail.ifPresent(d -> verifier(d, LONGUEUR_DETAIL_MAX, "detail"));
    }

    /** Action sans changement de statut. */
    public static EntreeJournal action(Instant le, Acteur acteur, String action, ObjetAudite objet, String detail) {
        return new EntreeJournal(UUID.randomUUID(), le, acteur, action, objet, Optional.empty(), Optional.empty(),
                texte(detail));
    }

    /**
     * Transition de statut.
     *
     * @param avant {@code null} à la création de l'objet
     */
    public static EntreeJournal transition(Instant le, Acteur acteur, String action, ObjetAudite objet, String avant,
            String apres, String detail) {
        Objects.requireNonNull(apres, "apres");
        return new EntreeJournal(UUID.randomUUID(), le, acteur, action, objet, texte(avant), Optional.of(apres),
                texte(detail));
    }

    private static Optional<String> texte(String valeur) {
        return valeur == null || valeur.isBlank() ? Optional.empty() : Optional.of(valeur.strip());
    }

    private static void verifier(String valeur, int max, String champ) {
        if (valeur.isBlank() || valeur.length() > max) {
            throw new IllegalArgumentException("Champ " + champ + " du journal vide ou trop long");
        }
    }

    /** Sans le détail, qui peut contenir du texte libre : seule forme autorisée dans les journaux. */
    @Override
    public String toString() {
        return "EntreeJournal[" + action + ", " + objet.type() + " " + objet.id() + ", " + acteur.type() + ", "
                + avant.orElse("-") + " → " + apres.orElse("-") + "]";
    }
}
