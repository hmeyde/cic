package com.cic.demande.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
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
 * l'analyste décide ({@link #accepter}, {@link #refuser}, {@link #demanderJustificatif}). La
 * décision est conservée avec son auteur.
 */
public final class Demande {

    /** Longueur maximale du motif d'un refus. */
    public static final int LONGUEUR_MOTIF_MAX = 500;

    private final UUID id;
    private final UUID clientId;
    private final TermesDemande termes;
    private final Score score;
    private final Instant soumiseLe;
    private StatutDossier statut;
    private Instant statutChangeLe;
    private Decision decision;

    private Demande(UUID id, UUID clientId, TermesDemande termes, Score score, Instant soumiseLe,
            StatutDossier statut, Instant statutChangeLe, Decision decision) {
        this.id = Objects.requireNonNull(id, "id");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.termes = Objects.requireNonNull(termes, "termes");
        this.score = Objects.requireNonNull(score, "score");
        this.soumiseLe = Objects.requireNonNull(soumiseLe, "soumiseLe");
        this.statut = Objects.requireNonNull(statut, "statut");
        this.statutChangeLe = Objects.requireNonNull(statutChangeLe, "statutChangeLe");
        this.decision = decision;
    }

    /** Nouvelle demande au statut DEMANDE_SOUMISE, avec le score calculé au même instant. */
    public static Demande soumettre(UUID clientId, TermesDemande termes, Score score, Instant le) {
        return new Demande(UUID.randomUUID(), clientId, termes, score, le, StatutDossier.DEMANDE_SOUMISE, le, null);
    }

    /** Reconstitution depuis le stockage, sans décision. */
    public static Demande reconstituer(UUID id, UUID clientId, TermesDemande termes, Score score,
            Instant soumiseLe, StatutDossier statut, Instant statutChangeLe) {
        return reconstituer(id, clientId, termes, score, soumiseLe, statut, statutChangeLe, Optional.empty());
    }

    /** Reconstitution depuis le stockage. */
    public static Demande reconstituer(UUID id, UUID clientId, TermesDemande termes, Score score,
            Instant soumiseLe, StatutDossier statut, Instant statutChangeLe, Optional<Decision> decision) {
        return new Demande(id, clientId, termes, score, soumiseLe, statut, statutChangeLe, decision.orElse(null));
    }

    /**
     * DEMANDE_SOUMISE → EN_ANALYSE : le dossier entre dans la file des analystes.
     *
     * @throws TransitionInterditeException si la demande n'est pas au statut DEMANDE_SOUMISE
     */
    public ChangementStatut mettreEnAnalyse(Instant le) {
        return passerA(StatutDossier.EN_ANALYSE, le);
    }

    /**
     * EN_ANALYSE → ACCEPTEE, décidé par l'analyste. Le délai de l'apport et de l'autorisation de
     * prélèvement court à partir de cet instant.
     *
     * @throws TransitionInterditeException si la demande n'est pas EN_ANALYSE
     */
    public ChangementStatut accepter(String analyste, Instant le) {
        ChangementStatut changement = statut.passerA(StatutDossier.ACCEPTEE);
        Decision prise = new Decision(analyste, le, Optional.empty());
        appliquer(changement, le);
        decision = prise;
        return changement;
    }

    /**
     * EN_ANALYSE → REFUSEE, décidé par l'analyste avec un motif.
     *
     * @throws TransitionInterditeException si la demande n'est pas EN_ANALYSE
     * @throws MotifRefusInvalideException  motif vide ou de plus de {@link #LONGUEUR_MOTIF_MAX} caractères
     */
    public ChangementStatut refuser(String analyste, String motif, Instant le) {
        ChangementStatut changement = statut.passerA(StatutDossier.REFUSEE);
        if (motif == null || motif.isBlank() || motif.strip().length() > LONGUEUR_MOTIF_MAX) {
            throw new MotifRefusInvalideException();
        }
        Decision prise = new Decision(analyste, le, Optional.of(motif.strip()));
        appliquer(changement, le);
        decision = prise;
        return changement;
    }

    /**
     * L'analyste demande un justificatif avant de décider : le dossier reste EN_ANALYSE, seule la
     * demande est journalisée.
     *
     * @throws DecisionImpossibleException si le dossier n'attend pas de décision
     */
    public void demanderJustificatif() {
        if (!statut.attendDecision()) {
            throw new DecisionImpossibleException(statut);
        }
    }

    private ChangementStatut passerA(StatutDossier cible, Instant le) {
        ChangementStatut changement = statut.passerA(cible);
        appliquer(changement, le);
        return changement;
    }

    private void appliquer(ChangementStatut changement, Instant le) {
        Objects.requireNonNull(le, "le");
        statut = changement.vers();
        statutChangeLe = le;
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

    /** Décision de l'analyste ; vide tant que le dossier n'est ni accepté ni refusé. */
    public Optional<Decision> decision() {
        return Optional.ofNullable(decision);
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
