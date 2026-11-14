package com.seheli.demande.adapter.in.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.seheli.audit.domain.EntreeJournal;
import com.seheli.audit.domain.TypeActeur;
import com.seheli.commun.domain.Montant;
import com.seheli.contrat.domain.StatutAutorisation;
import com.seheli.contrat.domain.TypeAutorisation;
import com.seheli.demande.application.FicheDossier;
import com.seheli.demande.domain.Demande;
import com.seheli.demande.domain.ResultatCondition;
import com.seheli.demande.domain.ResultatCritere;
import com.seheli.demande.domain.Score;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.demande.domain.TermesDemande;
import com.seheli.identite.domain.Client;
import com.seheli.identite.domain.Document;
import com.seheli.identite.domain.Emploi;
import com.seheli.identite.domain.TypeDocument;

/**
 * Fiche dossier telle qu'affichée à l'analyste : dates à l'heure locale, âge et ancienneté à la date
 * du jour, part du salaire consacrée aux mensualités, score critère par critère.
 *
 * @param client        vide si le client est introuvable
 * @param emploi        vide si le client n'a plus d'emploi déclaré
 * @param typesManquants   types de justificatifs attendus que le client n'a pas encore envoyés
 * @param decisionPossible le dossier attend la décision de l'analyste (EN_ANALYSE)
 * @param decision         décision déjà prise ; vide sinon
 * @param journal          journal d'audit du dossier, du plus ancien au plus récent
 * @param autorisation     autorisation de prélèvement ; vide tant que le dossier n'a pas de contrat
 */
record VueFicheDossier(UUID id, StatutDossier statut, String soumiseLe, String statutChangeLe, Conditions conditions,
        Optional<VueClient> client, Optional<VueEmploi> emploi, List<VueJustificatif> justificatifs,
        List<TypeDocument> typesManquants, VueScore score, boolean decisionPossible,
        Optional<VueDecision> decision, List<VueEntreeJournal> journal,
        Optional<VueAutorisation> autorisation) {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Conditions de la demande (le prix d'achat et la remise ne sont pas montrés au client, mais à l'analyste oui). */
    record Conditions(String designation, Montant prixPublic, Montant prixAchat, Montant prixVente,
            Montant apportMinimum, Montant apport, Montant resteAFinancer, int nombreMois, Montant mensualite,
            Montant derniereMensualite, boolean derniereDiffere) {
    }

    record VueClient(String nomComplet, String nni, String dateNaissance, int age, String telephone,
            boolean nniVerifie, boolean pieceIdentiteVerifiee, String consentementLe) {
    }

    /** @param partSalairePourcent part du salaire net prise par la mensualité la plus haute de ce dossier, en % arrondi */
    record VueEmploi(String employeur, String typeContrat, String dateDebut, int ancienneteAnnees,
            int ancienneteMois, Optional<String> dateFin, Montant salaireNet, int jourPaie,
            boolean salaireVerseSurCompte, boolean salaireDomicilieBmci, String modeRemboursement,
            int partSalairePourcent) {
    }

    record VueJustificatif(UUID id, TypeDocument type, String format, String recuLe, long tailleKo) {
    }

    /** @param motif motif du refus ; vide pour une acceptation */
    record VueDecision(String analyste, String le, Optional<String> motif) {
    }

    /**
     * @param acteur identifiant du membre du back-office ; vide pour le client (son identifiant
     *               technique n'apprend rien à l'analyste) et pour un traitement automatique
     */
    record VueEntreeJournal(String le, TypeActeur typeActeur, Optional<String> acteur, String action,
            Optional<String> avant, Optional<String> apres, Optional<String> detail) {
    }

    record VueAutorisation(UUID id, TypeAutorisation type, StatutAutorisation statut) {
    }

    record VueScore(String versionGrille, int note, boolean eligible, String recommandation,
            String recommandationNote, List<ResultatCondition> conditions, List<ResultatCritere> criteres) {
    }

    static VueFicheDossier depuis(FicheDossier fiche, Clock clock) {
        Demande d = fiche.demande();
        TermesDemande t = d.termes();
        LocalDate aujourdhui = LocalDate.now(clock);
        Conditions conditions = new Conditions(t.designationProduit(), t.prixPublic(), t.prixAchat(), t.prixVente(),
                t.apportMinimum(), t.apport(), t.resteAFinancer(), t.nombreMois(), t.mensualite(),
                t.derniereMensualite(), t.derniereMensualite().compareTo(t.mensualite()) != 0);

        Optional<VueClient> client = fiche.client().map(c -> client(c, aujourdhui, clock));
        Optional<VueEmploi> emploi = fiche.emploi().map(e -> emploi(e, t.mensualiteMaximale(), aujourdhui));

        List<VueJustificatif> justificatifs = fiche.justificatifs().stream()
                .map(doc -> justificatif(doc, clock)).toList();
        Set<TypeDocument> recus = fiche.justificatifs().stream().map(Document::type)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TypeDocument.class)));
        List<TypeDocument> manquants = Arrays.stream(TypeDocument.values()).filter(ty -> !recus.contains(ty))
                .toList();

        Score s = d.score();
        VueScore score = new VueScore(s.versionGrille(), s.note().note(), s.eligibilite().estEligible(),
                s.recommandation().name(), s.note().recommandation().name(), s.eligibilite().resultats(),
                s.note().criteres());

        return new VueFicheDossier(d.id(), d.statut(), DATE_HEURE.format(d.soumiseLe().atZone(clock.getZone())),
                DATE_HEURE.format(d.statutChangeLe().atZone(clock.getZone())), conditions, client, emploi,
                justificatifs, manquants, score, d.statut().attendDecision(),
                d.decision().map(dec -> new VueDecision(dec.analyste(),
                        DATE_HEURE.format(dec.le().atZone(clock.getZone())), dec.motif())),
                fiche.journal().stream().map(e -> entree(e, clock)).toList(),
                fiche.autorisation().map(a -> new VueAutorisation(a.id(), a.type(), a.statut())));
    }

    private static VueClient client(Client c, LocalDate aujourdhui, Clock clock) {
        return new VueClient((c.prenom() + " " + c.nom()).strip(), c.nni(), DATE.format(c.dateNaissance()),
                c.ageAu(aujourdhui), c.telephone(), c.estNniVerifie(), c.estPieceIdentiteVerifiee(),
                DATE_HEURE.format(c.consentementDonneesLe().atZone(clock.getZone())));
    }

    private static VueEmploi emploi(Emploi e, Montant mensualiteMaximale, LocalDate aujourdhui) {
        Period anciennete = e.dateDebut().isAfter(aujourdhui) ? Period.ZERO
                : Period.between(e.dateDebut(), aujourdhui);
        int part = mensualiteMaximale.valeur().multiply(BigDecimal.valueOf(100))
                .divide(e.salaireNet().valeur(), 0, RoundingMode.HALF_UP).intValueExact();
        return new VueEmploi(e.employeur(), e.typeContrat().name(), DATE.format(e.dateDebut()),
                anciennete.getYears(), anciennete.getMonths(), e.dateFin().map(DATE::format), e.salaireNet(),
                e.jourPaie(), e.estSalaireVerseSurCompte(), e.estSalaireDomicilieBmci(),
                e.modeRemboursement().name(), part);
    }

    private static VueEntreeJournal entree(EntreeJournal e, Clock clock) {
        Optional<String> acteur = e.acteur().type() == TypeActeur.BACK_OFFICE
                ? Optional.of(e.acteur().identifiant()) : Optional.empty();
        return new VueEntreeJournal(DATE_HEURE.format(e.le().atZone(clock.getZone())), e.acteur().type(), acteur,
                e.action(), e.avant(), e.apres(), e.detail());
    }

    private static VueJustificatif justificatif(Document doc, Clock clock) {
        return new VueJustificatif(doc.id(), doc.type(), doc.format().name(),
                DATE_HEURE.format(doc.recuLe().atZone(clock.getZone())),
                Math.max(1, (doc.tailleOctets() + 1023) / 1024));
    }
}
