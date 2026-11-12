package com.cic.comptabilite.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.cic.commun.domain.Montant;

/**
 * Écriture comptable en partie double : au moins deux lignes, total des débits = total des crédits.
 * Immuable : une écriture erronée n'est ni modifiée ni supprimée, elle est annulée par une écriture
 * inverse ({@link #contrepasser}).
 *
 * <p>Chaque écriture comptabilise un fait identifié par sa nature et sa source (paiement, commande,
 * dossier) : un fait n'est comptabilisé qu'une fois. Les montants des lignes sont positifs, le sens
 * est porté par la ligne. Le dossier et le contrat (autres modules) sont référencés par identifiant ;
 * le libellé ne porte aucune donnée personnelle.
 */
public final class EcritureComptable {

    public static final int LIBELLE_LONGUEUR_MAX = 200;

    private final UUID id;
    private final NatureEcriture nature;
    private final UUID sourceId;
    private final LocalDate date;
    private final String libelle;
    private final UUID demandeId;
    private final UUID contratId;
    private final List<LigneEcriture> lignes;
    private final Instant passeeLe;

    private EcritureComptable(UUID id, NatureEcriture nature, UUID sourceId, LocalDate date, String libelle,
            UUID demandeId, UUID contratId, List<LigneEcriture> lignes, Instant passeeLe) {
        this.id = Objects.requireNonNull(id, "id");
        this.nature = Objects.requireNonNull(nature, "nature");
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
        this.date = Objects.requireNonNull(date, "date");
        this.libelle = Objects.requireNonNull(libelle, "libelle");
        this.demandeId = demandeId;
        this.contratId = contratId;
        this.lignes = List.copyOf(Objects.requireNonNull(lignes, "lignes"));
        this.passeeLe = Objects.requireNonNull(passeeLe, "passeeLe");
        if (libelle.isBlank() || libelle.length() > LIBELLE_LONGUEUR_MAX) {
            throw new IllegalArgumentException("Libellé d'écriture vide ou trop long");
        }
        if (this.lignes.size() < 2) {
            throw new IllegalArgumentException("Une écriture a au moins deux lignes");
        }
        if (total(Sens.DEBIT).compareTo(total(Sens.CREDIT)) != 0) {
            throw new IllegalArgumentException("Écriture déséquilibrée : débit " + total(Sens.DEBIT) + ", crédit "
                    + total(Sens.CREDIT));
        }
    }

    /** Apport reçu : la banque encaisse, l'avance du client est due jusqu'à la remise du produit. */
    public static EcritureComptable apportEncaisse(UUID paiementId, UUID demandeId, UUID contratId,
            String numeroContrat, Montant recu, LocalDate recuLe, Instant le) {
        return new EcritureComptable(UUID.randomUUID(), NatureEcriture.APPORT_ENCAISSE, paiementId, recuLe,
                "Apport du contrat " + numeroContrat, demandeId, contratId,
                List.of(LigneEcriture.debit(Compte.BANQUE, recu), LigneEcriture.credit(Compte.CLIENTS_APPORTS, recu)),
                le);
    }

    /** Prix d'achat payé au fournisseur à la validation de la commande. */
    public static EcritureComptable paiementFournisseur(UUID commandeId, String numeroCommande, UUID demandeId,
            UUID contratId, Montant prixAchat, LocalDate le, Instant passeeLe) {
        return new EcritureComptable(UUID.randomUUID(), NatureEcriture.PAIEMENT_FOURNISSEUR, commandeId, le,
                "Paiement fournisseur, bon de commande " + numeroCommande, demandeId, contratId,
                List.of(LigneEcriture.debit(Compte.ACHATS, prixAchat), LigneEcriture.credit(Compte.BANQUE, prixAchat)),
                passeeLe);
    }

    /**
     * Vente constatée à la remise du produit : le prix de vente fixé à la signature est réglé par
     * l'apport (avance soldée) et par le reste dû en mensualités.
     */
    public static EcritureComptable venteLivree(UUID demandeId, UUID contratId, String numeroContrat,
            Montant prixVente, Montant apport, LocalDate le, Instant passeeLe) {
        Objects.requireNonNull(prixVente, "prixVente");
        Objects.requireNonNull(apport, "apport");
        if (!prixVente.estSuperieurA(apport)) {
            throw new IllegalArgumentException("L'apport doit être inférieur au prix de vente");
        }
        List<LigneEcriture> lignes = apport.estZero()
                ? List.of(LigneEcriture.debit(Compte.CLIENTS, prixVente),
                        LigneEcriture.credit(Compte.VENTES, prixVente))
                : List.of(LigneEcriture.debit(Compte.CLIENTS_APPORTS, apport),
                        LigneEcriture.debit(Compte.CLIENTS, prixVente.moins(apport)),
                        LigneEcriture.credit(Compte.VENTES, prixVente));
        return new EcritureComptable(UUID.randomUUID(), NatureEcriture.VENTE_LIVREE, demandeId, le,
                "Vente du contrat " + numeroContrat, demandeId, contratId, lignes, passeeLe);
    }

    /** Échéance reçue : la banque encaisse, le reste dû du client diminue. */
    public static EcritureComptable echeanceEncaissee(UUID paiementId, UUID demandeId, UUID contratId,
            String numeroContrat, int rang, Montant recu, LocalDate recuLe, Instant le) {
        return new EcritureComptable(UUID.randomUUID(), NatureEcriture.ECHEANCE_ENCAISSEE, paiementId, recuLe,
                "Échéance " + rang + " du contrat " + numeroContrat, demandeId, contratId,
                List.of(LigneEcriture.debit(Compte.BANQUE, recu), LigneEcriture.credit(Compte.CLIENTS, recu)), le);
    }

    /** Paiement reçu sans dossier identifié : encaissé, en attente d'identification. */
    public static EcritureComptable paiementAIdentifier(UUID paiementId, Montant recu, LocalDate recuLe, Instant le) {
        return new EcritureComptable(UUID.randomUUID(), NatureEcriture.PAIEMENT_A_IDENTIFIER, paiementId, recuLe,
                "Paiement reçu à identifier", null, null,
                List.of(LigneEcriture.debit(Compte.BANQUE, recu),
                        LigneEcriture.credit(Compte.PAIEMENTS_A_IDENTIFIER, recu)),
                le);
    }

    /** Apport en capital d'un associé : la banque encaisse, le capital augmente. Libellé sans nom d'associé. */
    public static EcritureComptable apportCapital(UUID apportId, Montant montant, LocalDate verseLe, Instant le) {
        return new EcritureComptable(UUID.randomUUID(), NatureEcriture.APPORT_CAPITAL, apportId, verseLe,
                "Apport en capital d'un associé", null, null,
                List.of(LigneEcriture.debit(Compte.BANQUE, montant), LigneEcriture.credit(Compte.CAPITAL, montant)),
                le);
    }

    public static EcritureComptable reconstituer(UUID id, NatureEcriture nature, UUID sourceId, LocalDate date,
            String libelle, UUID demandeId, UUID contratId, List<LigneEcriture> lignes, Instant passeeLe) {
        return new EcritureComptable(id, nature, sourceId, date, libelle, demandeId, contratId, lignes, passeeLe);
    }

    /**
     * Écriture inverse qui annule celle-ci : mêmes comptes et montants, sens inversés. L'écriture
     * d'origine reste telle quelle.
     *
     * @throws EcritureDejaContrepasseeException celle-ci est elle-même une contrepassation
     */
    public EcritureComptable contrepasser(String motif, LocalDate le, Instant passeeLe) {
        Objects.requireNonNull(motif, "motif");
        if (estContrepassation()) {
            throw new EcritureDejaContrepasseeException();
        }
        String texte = "Contrepassation : " + libelle + (motif.isBlank() ? "" : " — " + motif.strip());
        return new EcritureComptable(UUID.randomUUID(), NatureEcriture.CONTREPASSATION, id, le,
                texte.length() > LIBELLE_LONGUEUR_MAX ? texte.substring(0, LIBELLE_LONGUEUR_MAX) : texte, demandeId,
                contratId, lignes.stream().map(LigneEcriture::inverse).toList(), passeeLe);
    }

    public boolean estContrepassation() {
        return nature == NatureEcriture.CONTREPASSATION;
    }

    /** Écriture annulée par celle-ci ; vide si ce n'est pas une contrepassation. */
    public Optional<UUID> contrepasse() {
        return estContrepassation() ? Optional.of(sourceId) : Optional.empty();
    }

    /** Total des lignes de ce sens. */
    public Montant total(Sens sens) {
        return lignes.stream().filter(l -> l.sens() == sens).map(LigneEcriture::montant).reduce(Montant.ZERO,
                Montant::plus);
    }

    /** Montant de l'écriture : total des débits, égal au total des crédits. */
    public Montant montant() {
        return total(Sens.DEBIT);
    }

    public UUID id() {
        return id;
    }

    public NatureEcriture nature() {
        return nature;
    }

    /** Paiement, commande, dossier ou écriture contrepassée, selon la nature. */
    public UUID sourceId() {
        return sourceId;
    }

    /** Date comptable : date du paiement reçu ou du fait comptabilisé. */
    public LocalDate date() {
        return date;
    }

    public String libelle() {
        return libelle;
    }

    public Optional<UUID> demandeId() {
        return Optional.ofNullable(demandeId);
    }

    public Optional<UUID> contratId() {
        return Optional.ofNullable(contratId);
    }

    public List<LigneEcriture> lignes() {
        return lignes;
    }

    public Instant passeeLe() {
        return passeeLe;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EcritureComptable e && id.equals(e.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /** Pas de donnée personnelle : ce texte peut finir dans les journaux. */
    @Override
    public String toString() {
        return "EcritureComptable[" + id + ", " + nature + ", " + montant() + "]";
    }
}
