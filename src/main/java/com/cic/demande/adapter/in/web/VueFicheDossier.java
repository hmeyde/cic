package com.cic.demande.adapter.in.web;

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

import com.cic.commun.domain.Montant;
import com.cic.demande.application.FicheDossier;
import com.cic.demande.domain.Demande;
import com.cic.demande.domain.ResultatCondition;
import com.cic.demande.domain.ResultatCritere;
import com.cic.demande.domain.Score;
import com.cic.demande.domain.StatutDossier;
import com.cic.demande.domain.TermesDemande;
import com.cic.identite.domain.Client;
import com.cic.identite.domain.Document;
import com.cic.identite.domain.Emploi;
import com.cic.identite.domain.TypeDocument;

/**
 * Fiche dossier telle qu'affichée à l'analyste : dates à l'heure locale, âge et ancienneté à la date
 * du jour, part du salaire consacrée aux mensualités, score critère par critère.
 *
 * @param client        vide si le client est introuvable
 * @param emploi        vide si le client n'a plus d'emploi déclaré
 * @param typesManquants types de justificatifs attendus que le client n'a pas encore envoyés
 */
record VueFicheDossier(UUID id, StatutDossier statut, String soumiseLe, String statutChangeLe, Conditions conditions,
        Optional<VueClient> client, Optional<VueEmploi> emploi, List<VueJustificatif> justificatifs,
        List<TypeDocument> typesManquants, VueScore score) {

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
                justificatifs, manquants, score);
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

    private static VueJustificatif justificatif(Document doc, Clock clock) {
        return new VueJustificatif(doc.id(), doc.type(), doc.format().name(),
                DATE_HEURE.format(doc.recuLe().atZone(clock.getZone())),
                Math.max(1, (doc.tailleOctets() + 1023) / 1024));
    }
}
