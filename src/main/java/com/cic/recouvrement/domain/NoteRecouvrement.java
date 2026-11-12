package com.cic.recouvrement.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Note datée du recouvrement sur un dossier : appel au client, promesse de paiement, échange avec
 * l'employeur. Une note n'est jamais modifiée ni supprimée ; une correction se fait par une nouvelle
 * note. Le dossier est référencé par identifiant (module demande).
 *
 * @param auteur identifiant de connexion du back-office
 */
public record NoteRecouvrement(UUID id, UUID demandeId, String auteur, String texte, Instant ecriteLe) {

    /** Longueur maximale du texte d'une note. */
    public static final int LONGUEUR_MAX = 1000;

    public NoteRecouvrement {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(demandeId, "demandeId");
        Objects.requireNonNull(auteur, "auteur");
        Objects.requireNonNull(texte, "texte");
        Objects.requireNonNull(ecriteLe, "ecriteLe");
    }

    /**
     * Nouvelle note, texte débarrassé de ses espaces superflus.
     *
     * @throws NoteInvalideException texte vide ou de plus de {@link #LONGUEUR_MAX} caractères
     */
    public static NoteRecouvrement ecrire(UUID demandeId, String auteur, String texte, Instant le) {
        if (texte == null || texte.isBlank() || texte.strip().length() > LONGUEUR_MAX) {
            throw new NoteInvalideException();
        }
        return new NoteRecouvrement(UUID.randomUUID(), demandeId, auteur, texte.strip(), le);
    }

    /** Pas de texte : il peut contenir des données personnelles et ce résumé peut finir dans les journaux. */
    @Override
    public String toString() {
        return "NoteRecouvrement[" + id + ", dossier " + demandeId + ", " + ecriteLe + "]";
    }
}
