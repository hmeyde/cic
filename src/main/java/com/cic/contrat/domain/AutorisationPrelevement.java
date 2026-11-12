package com.cic.contrat.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Autorisation de prélèvement des mensualités d'un contrat : une par contrat, émise avec lui à
 * l'acceptation du dossier. Le dossier et le client sont référencés par identifiant (autres modules).
 * Les montants et les dates prélevés sont ceux des échéances du contrat : l'autorisation n'en porte
 * pas de copie.
 */
public final class AutorisationPrelevement {

    /** Type proposé par défaut : tout client accepté a son salaire versé sur un compte. */
    public static final TypeAutorisation TYPE_PAR_DEFAUT = TypeAutorisation.VIREMENT_PERMANENT;

    private final UUID id;
    private final UUID contratId;
    private final UUID demandeId;
    private final UUID clientId;
    private final TypeAutorisation type;
    private final StatutAutorisation statut;
    private final Instant emiseLe;

    private AutorisationPrelevement(UUID id, UUID contratId, UUID demandeId, UUID clientId, TypeAutorisation type,
            StatutAutorisation statut, Instant emiseLe) {
        this.id = Objects.requireNonNull(id, "id");
        this.contratId = Objects.requireNonNull(contratId, "contratId");
        this.demandeId = Objects.requireNonNull(demandeId, "demandeId");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.type = Objects.requireNonNull(type, "type");
        this.statut = Objects.requireNonNull(statut, "statut");
        this.emiseLe = Objects.requireNonNull(emiseLe, "emiseLe");
    }

    /** Autorisation du contrat, de type {@link #TYPE_PAR_DEFAUT}, à signer par le client. */
    public static AutorisationPrelevement emettre(Contrat contrat, Instant le) {
        Objects.requireNonNull(contrat, "contrat");
        return new AutorisationPrelevement(UUID.randomUUID(), contrat.id(), contrat.demandeId(), contrat.clientId(),
                TYPE_PAR_DEFAUT, StatutAutorisation.A_SIGNER, le);
    }

    public static AutorisationPrelevement reconstituer(UUID id, UUID contratId, UUID demandeId, UUID clientId,
            TypeAutorisation type, StatutAutorisation statut, Instant emiseLe) {
        return new AutorisationPrelevement(id, contratId, demandeId, clientId, type, statut, emiseLe);
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
