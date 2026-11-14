package com.seheli.contrat.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Autorisation de prélèvement des mensualités d'un contrat : une par contrat, émise avec lui à
 * l'acceptation du dossier. Le dossier et le client sont référencés par identifiant (autres modules).
 * Les montants et les dates prélevés sont ceux des échéances du contrat : l'autorisation n'en porte
 * pas de copie.
 *
 * <p>Le suivi au back-office fait avancer le statut selon les règles de {@link StatutAutorisation} :
 * dépôt du document signé par le client (SIGNEE), transmission à la banque, acceptation par la
 * banque, activation au premier prélèvement, révocation. Le type est fixé à l'émission.
 */
public final class AutorisationPrelevement {

    /** Type proposé par défaut : tout client accepté a son salaire versé sur un compte. */
    public static final TypeAutorisation TYPE_PAR_DEFAUT = TypeAutorisation.VIREMENT_PERMANENT;

    private final UUID id;
    private final UUID contratId;
    private final UUID demandeId;
    private final UUID clientId;
    private final TypeAutorisation type;
    private final Instant emiseLe;
    private StatutAutorisation statut;
    private Instant statutChangeLe;
    private DocumentSigne documentSigne;

    private AutorisationPrelevement(UUID id, UUID contratId, UUID demandeId, UUID clientId, TypeAutorisation type,
            StatutAutorisation statut, Instant emiseLe, Instant statutChangeLe, DocumentSigne documentSigne) {
        this.id = Objects.requireNonNull(id, "id");
        this.contratId = Objects.requireNonNull(contratId, "contratId");
        this.demandeId = Objects.requireNonNull(demandeId, "demandeId");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.type = Objects.requireNonNull(type, "type");
        this.statut = Objects.requireNonNull(statut, "statut");
        this.emiseLe = Objects.requireNonNull(emiseLe, "emiseLe");
        this.statutChangeLe = Objects.requireNonNull(statutChangeLe, "statutChangeLe");
        if ((statut == StatutAutorisation.A_SIGNER) != (documentSigne == null)) {
            throw new IllegalArgumentException("Document signé incohérent avec le statut " + statut);
        }
        this.documentSigne = documentSigne;
    }

    /** Autorisation du contrat, de type {@link #TYPE_PAR_DEFAUT}, à signer par le client. */
    public static AutorisationPrelevement emettre(Contrat contrat, Instant le) {
        Objects.requireNonNull(contrat, "contrat");
        return new AutorisationPrelevement(UUID.randomUUID(), contrat.id(), contrat.demandeId(), contrat.clientId(),
                TYPE_PAR_DEFAUT, StatutAutorisation.A_SIGNER, le, le, null);
    }

    /** @param documentSigne {@code null} si et seulement si l'autorisation est A_SIGNER */
    public static AutorisationPrelevement reconstituer(UUID id, UUID contratId, UUID demandeId, UUID clientId,
            TypeAutorisation type, StatutAutorisation statut, Instant emiseLe, Instant statutChangeLe,
            DocumentSigne documentSigne) {
        return new AutorisationPrelevement(id, contratId, demandeId, clientId, type, statut, emiseLe, statutChangeLe,
                documentSigne);
    }

    /**
     * Dépose le document signé par le client : A_SIGNER → SIGNEE, ou remplacement du document d'une
     * autorisation SIGNEE (scan illisible) sans changement de statut.
     *
     * @return la transition ; vide pour un remplacement
     * @throws DepotDocumentSigneImpossibleException autorisation déjà transmise, acceptée, active ou révoquée
     */
    public Optional<ChangementStatutAutorisation> deposerDocumentSigne(DocumentSigne document) {
        Objects.requireNonNull(document, "document");
        if (!statut.accepteDocumentSigne()) {
            throw new DepotDocumentSigneImpossibleException(statut);
        }
        this.documentSigne = document;
        if (statut == StatutAutorisation.SIGNEE) {
            return Optional.empty();
        }
        return Optional.of(passerA(StatutAutorisation.SIGNEE, document.deposeLe()));
    }

    /** SIGNEE → TRANSMISE : l'ordre signé est remis à la banque. */
    public ChangementStatutAutorisation transmettre(Instant le) {
        return passerA(StatutAutorisation.TRANSMISE, le);
    }

    /** TRANSMISE → ACCEPTEE : la banque a visé l'ordre. */
    public ChangementStatutAutorisation accepter(Instant le) {
        return passerA(StatutAutorisation.ACCEPTEE, le);
    }

    /** ACCEPTEE → ACTIVE : premier prélèvement fait. */
    public ChangementStatutAutorisation activer(Instant le) {
        return passerA(StatutAutorisation.ACTIVE, le);
    }

    /** SIGNEE, TRANSMISE, ACCEPTEE ou ACTIVE → REVOQUEE (état final). */
    public ChangementStatutAutorisation revoquer(Instant le) {
        return passerA(StatutAutorisation.REVOQUEE, le);
    }

    private ChangementStatutAutorisation passerA(StatutAutorisation vers, Instant le) {
        Objects.requireNonNull(le, "le");
        if (!statut.peutPasserA(vers)) {
            throw new TransitionAutorisationInterditeException(statut, vers);
        }
        ChangementStatutAutorisation changement = new ChangementStatutAutorisation(statut, vers);
        this.statut = vers;
        this.statutChangeLe = le;
        return changement;
    }

    public UUID id() {
        return id;
    }

    public UUID contratId() {
        return contratId;
    }

    public UUID demandeId() {
        return demandeId;
    }

    public UUID clientId() {
        return clientId;
    }

    public TypeAutorisation type() {
        return type;
    }

    public StatutAutorisation statut() {
        return statut;
    }

    public Instant emiseLe() {
        return emiseLe;
    }

    public Instant statutChangeLe() {
        return statutChangeLe;
    }

    /** Document signé déposé ; vide tant que l'autorisation est A_SIGNER. */
    public Optional<DocumentSigne> documentSigne() {
        return Optional.ofNullable(documentSigne);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AutorisationPrelevement a && id.equals(a.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /** Pas de donnée personnelle : ce texte peut finir dans les journaux. */
    @Override
    public String toString() {
        return "AutorisationPrelevement[" + type + ", " + statut + ", contrat " + contratId + "]";
    }
}
