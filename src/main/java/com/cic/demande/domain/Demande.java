package com.cic.demande.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Dossier de crédit d'un client, de la demande au solde. Le client est référencé par identifiant
 * (module identite).
 *
 * <p>Le statut ne change que par la machine à états {@link StatutDossier} : toute transition non
 * prévue lève {@link TransitionInterditeException}. Chaque transition est rendue à l'appelant
 * ({@link ChangementStatut}) pour le journal d'audit.
 *
 * <p>Le score est calculé à la soumission et stocké avec la version de sa grille ; il recommande,
 * l'analyste décide.
 */
public final class Demande {

    private final UUID id;
    private final UUID clientId;
    private final TermesDemande termes;
    private final Score score;
    private final Instant soumiseLe;
    private StatutDossier statut;
    private Instant statutChangeLe;

    private Demande(UUID id, UUID clientId, TermesDemande termes, Score score, Instant soumiseLe,
            StatutDossier statut, Instant statutChangeLe) {
        this.id = Objects.requireNonNull(id, "id");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.termes = Objects.requireNonNull(termes, "termes");
        this.score = Objects.requireNonNull(score, "score");
        this.soumiseLe = Objects.requireNonNull(soumiseLe, "soumiseLe");
        this.statut = Objects.requireNonNull(statut, "statut");
        this.statutChangeLe = Objects.requireNonNull(statutChangeLe, "statutChangeLe");
    }

    /** Nouvelle demande au statut DEMANDE_SOUMISE, avec le score calculé au même instant. */
    public static Demande soumettre(UUID clientId, TermesDemande termes, Score score, Instant le) {
        return new Demande(UUID.randomUUID(), clientId, termes, score, le, StatutDossier.DEMANDE_SOUMISE, le);
    }

    /** Reconstitution depuis le stockage. */
    public static Demande reconstituer(UUID id, UUID clientId, TermesDemande termes, Score score,
            Instant soumiseLe, StatutDossier statut, Instant statutChangeLe) {
        return new Demande(id, clientId, termes, score, soumiseLe, statut, statutChangeLe);
    }

    /**
     * DEMANDE_SOUMISE → EN_ANALYSE : le dossier entre dans la file des analystes.
     *
     * @throws TransitionInterditeException si la demande n'est pas au statut DEMANDE_SOUMISE
     */
    public ChangementStatut mettreEnAnalyse(Instant le) {
        return passerA(StatutDossier.EN_ANALYSE, le);
    }

    private ChangementStatut passerA(StatutDossier cible, Instant le) {
        Objects.requireNonNull(le, "le");
        ChangementStatut changement = statut.passerA(cible);
        statut = cible;
        statutChangeLe = le;
        return changement;
    }

    public boolean estActive() {
        return statut.estActif();
    }

    public UUID id() {
        return id;
    }

    public UUID clientId() {
        return clientId;
    }

    public TermesDemande termes() {
        return termes;
    }

    public Score score() {
        return score;
    }

    public Instant soumiseLe() {
        return soumiseLe;
    }

    public StatutDossier statut() {
        return statut;
    }

    public Instant statutChangeLe() {
        return statutChangeLe;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Demande d && id.equals(d.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /** Pas de donnée personnelle : ce texte peut finir dans les journaux. */
    @Override
    public String toString() {
        return "Demande[" + id + ", client " + clientId + ", " + statut + "]";
    }
}
