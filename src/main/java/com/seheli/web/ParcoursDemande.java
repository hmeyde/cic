package com.seheli.web;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Barre de progression du parcours de demande : l'étape en cours et l'état de chacune des quatre
 * étapes. Une étape est faite quand ses informations sont enregistrées (compte créé, emploi déclaré,
 * tous les justificatifs attendus envoyés) ; la demande n'est jamais « faite » ici, puisqu'une fois
 * soumise le client n'est plus dans le parcours.
 *
 * @param courante étape de la page affichée
 * @param etapes   les quatre étapes, dans l'ordre
 */
public record ParcoursDemande(EtapeParcours courante, List<Etape> etapes) {

    public enum Statut { FAITE, COURANTE, A_FAIRE }

    /**
     * Une étape de la barre ; {@code nom} sert de suffixe aux clés de messages.
     *
     * @param lien page où revenir sur cette étape, {@code null} si elle n'est pas un lien
     */
    public record Etape(EtapeParcours etape, Statut statut, String lien) {
        public String nom() {
            return etape.name();
        }

        public int numero() {
            return etape.numero();
        }
    }

    public ParcoursDemande {
        Objects.requireNonNull(courante, "courante");
        etapes = List.copyOf(etapes);
    }

    /**
     * Avant la création du compte, aucune étape n'est un lien (les autres pages demandent d'être
     * connecté) ; ensuite, toute étape autre que la courante en est un, sauf le compte, qui ne se
     * rouvre pas.
     */
    public static ParcoursDemande pour(EtapeParcours courante, boolean compteCree, boolean emploiDeclare,
            boolean justificatifsComplets) {
        boolean[] faites = {compteCree, emploiDeclare, justificatifsComplets, false};
        List<Etape> etapes = Arrays.stream(EtapeParcours.values())
                .map(e -> new Etape(e,
                        e == courante ? Statut.COURANTE : faites[e.ordinal()] ? Statut.FAITE : Statut.A_FAIRE,
                        e == courante || !compteCree ? null : e.chemin()))
                .toList();
        return new ParcoursDemande(courante, etapes);
    }

    /** Vrai si l'étape nommée ({@code EMPLOI}…) est faite. */
    public boolean faite(String nom) {
        return etapes.stream().anyMatch(e -> e.nom().equals(nom) && e.statut() == Statut.FAITE);
    }

    public int numero() {
        return courante.numero();
    }

    public int total() {
        return etapes.size();
    }

    public String nom() {
        return courante.name();
    }
}
