package com.seheli.demande.domain;

import java.time.Duration;
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

    /**
     * Le dossier est ACCEPTEE depuis au moins {@code delai} : l'apport ou l'autorisation de
     * prélèvement manque encore, sinon il serait passé à APPORT_ET_AUTORISATION.
     */
    public boolean delaiApportEtAutorisationDepasse(Duration delai, Instant maintenant) {
        Objects.requireNonNull(delai, "delai");
        Objects.requireNonNull(maintenant, "maintenant");
        return statut == StatutDossier.ACCEPTEE && !maintenant.isBefore(statutChangeLe.plus(delai));
    }

    /**
     * ACCEPTEE → ANNULEE : l'apport ou l'autorisation de prélèvement n'est pas arrivé dans le délai
     * suivant l'acceptation.
     *
     * @throws TransitionInterditeException si la demande n'est pas ACCEPTEE
     * @throws DelaiNonDepasseException     si le délai n'est pas encore écoulé
     */
    public ChangementStatut annulerFauteApportOuAutorisation(Duration delai, Instant le) {
        ChangementStatut changement = statut.passerA(StatutDossier.ANNULEE);
        if (!delaiApportEtAutorisationDepasse(delai, le)) {
            throw new DelaiNonDepasseException();
        }
        appliquer(changement, le);
        return changement;
    }

    /**
     * ACCEPTEE → APPORT_ET_AUTORISATION : l'apport est encaissé et l'autorisation de prélèvement
     * ACCEPTEE par la banque. Le produit peut être commandé.
     *
     * @throws TransitionInterditeException si la demande n'est pas ACCEPTEE
     * @throws PrealablesManquantsException si l'apport ou l'autorisation manque
     */
    public ChangementStatut constaterApportEtAutorisation(PrealablesCommande prealables, Instant le) {
        Objects.requireNonNull(prealables, "prealables");
        ChangementStatut changement = statut.passerA(StatutDossier.APPORT_ET_AUTORISATION);
        if (!prealables.sontReunis()) {
            throw new PrealablesManquantsException(prealables);
        }
        appliquer(changement, le);
        return changement;
    }

    /**
     * APPORT_ET_AUTORISATION → COMMANDEE : le paiement fournisseur est validé et le bon de commande
     * émis. Les préalables sont vérifiés de nouveau (une autorisation peut avoir été révoquée entre
     * temps) ; la personne qui a décidé du dossier ne peut pas valider son paiement fournisseur.
     *
     * @param validateur identifiant de connexion du back-office
     * @throws TransitionInterditeException si la demande n'est pas APPORT_ET_AUTORISATION
     * @throws PrealablesManquantsException si l'apport ou l'autorisation manque
     * @throws SeparationDesTachesException si le validateur est l'analyste qui a décidé du dossier
     */
    public ChangementStatut commander(String validateur, PrealablesCommande prealables, Instant le) {
        Objects.requireNonNull(validateur, "validateur");
        Objects.requireNonNull(prealables, "prealables");
        ChangementStatut changement = statut.passerA(StatutDossier.COMMANDEE);
        if (!prealables.sontReunis()) {
            throw new PrealablesManquantsException(prealables);
        }
        if (decision == null || decision.prisePar(validateur)) {
            throw new SeparationDesTachesException();
        }
        appliquer(changement, le);
        return changement;
    }

    /**
     * COMMANDEE → LIVREE : le client confirme avoir reçu le produit.
     *
     * @throws TransitionInterditeException si la demande n'est pas COMMANDEE
     */
    public ChangementStatut confirmerRemise(Instant le) {
        return passerA(StatutDossier.LIVREE, le);
    }

    /**
     * LIVREE → EN_REMBOURSEMENT : le produit est remis, les mensualités fixées au contrat sont
     * désormais prélevées à leurs dates.
     *
     * @throws TransitionInterditeException si la demande n'est pas LIVREE
     */
    public ChangementStatut demarrerRemboursement(Instant le) {
        return passerA(StatutDossier.EN_REMBOURSEMENT, le);
    }

    /**
     * EN_REMBOURSEMENT → EN_RETARD : une échéance n'a pas été payée à sa date. Le prix ne change
     * pas : aucune pénalité de retard.
     *
     * @throws TransitionInterditeException si la demande n'est pas EN_REMBOURSEMENT
     */
    public ChangementStatut constaterRetard(Instant le) {
        return passerA(StatutDossier.EN_RETARD, le);
    }

    /**
     * EN_RETARD → EN_REMBOURSEMENT : les échéances en retard sont payées, les prélèvements suivants
     * reprennent à leurs dates. Le prix ne change pas.
     *
     * @throws TransitionInterditeException      si la demande n'est pas EN_RETARD
     * @throws RegularisationImpossibleException si une échéance est encore EN_RETARD
     */
    public ChangementStatut regulariser(SituationEcheances situation, Instant le) {
        Objects.requireNonNull(situation, "situation");
        if (statut != StatutDossier.EN_RETARD) {
            // LIVREE → EN_REMBOURSEMENT est permis, mais c'est le démarrage du remboursement, pas une régularisation.
            throw new TransitionInterditeException(statut, StatutDossier.EN_REMBOURSEMENT);
        }
        ChangementStatut changement = statut.passerA(StatutDossier.EN_REMBOURSEMENT);
        if (!situation.aucunRetard()) {
            throw new RegularisationImpossibleException();
        }
        appliquer(changement, le);
        return changement;
    }

    /**
     * EN_RETARD → RECOUVREMENT : le retard n'est pas rattrapé à l'amiable, le dossier passe au
     * recouvrement. Le client reste redevable du prix fixé au contrat, sans pénalité.
     *
     * @throws TransitionInterditeException       si la demande n'est pas EN_RETARD
     * @throws MotifRecouvrementInvalideException motif vide ou de plus de {@link #LONGUEUR_MOTIF_MAX} caractères
     */
    public ChangementStatut passerEnRecouvrement(String motif, Instant le) {
        ChangementStatut changement = statut.passerA(StatutDossier.RECOUVREMENT);
        if (motif == null || motif.isBlank() || motif.strip().length() > LONGUEUR_MOTIF_MAX) {
            throw new MotifRecouvrementInvalideException();
        }
        appliquer(changement, le);
        return changement;
    }

    /**
     * EN_REMBOURSEMENT → SOLDEE : toutes les échéances sont payées. Le dossier n'est plus actif, le
     * client peut faire une nouvelle demande.
     *
     * @throws TransitionInterditeException si la demande n'est pas EN_REMBOURSEMENT
     * @throws SoldeImpossibleException     si une échéance reste à payer
     */
    public ChangementStatut solder(SituationEcheances situation, Instant le) {
        Objects.requireNonNull(situation, "situation");
        ChangementStatut changement = statut.passerA(StatutDossier.SOLDEE);
        if (!situation.toutPaye()) {
            throw new SoldeImpossibleException();
        }
        appliquer(changement, le);
        return changement;
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
