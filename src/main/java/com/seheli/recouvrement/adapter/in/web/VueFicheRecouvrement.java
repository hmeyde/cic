package com.seheli.recouvrement.adapter.in.web;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.seheli.commun.domain.Montant;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.Echeance;
import com.seheli.contrat.domain.StatutEcheance;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.recouvrement.application.FicheRecouvrement;
import com.seheli.recouvrement.domain.TypeRelance;

/** Fiche d'un dossier pour le recouvrement, dates à l'heure locale. */
record VueFicheRecouvrement(UUID demandeId, StatutDossier statut, String numeroContrat, String designation,
        Optional<String> client, Optional<String> telephone, Montant prixVente, Montant restantDu,
        int echeancesEnRetard, List<Ligne> echeances, List<VueRelance> relances, List<VueNote> notes,
        boolean peutRegulariser, boolean peutPasserEnRecouvrement, boolean peutSolder) {

    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    record Ligne(int rang, String date, Montant montant, StatutEcheance statut) {
    }

    record VueRelance(TypeRelance type, int rang, String dateEcheance, String envoyeeLe) {
    }

    record VueNote(String ecriteLe, String auteur, String texte) {
    }

    static VueFicheRecouvrement depuis(UUID demandeId, FicheRecouvrement f, Clock clock) {
        Contrat c = f.contrat();
        Montant restant = c.echeances().stream().filter(e -> !e.estPayee()).map(Echeance::montant)
                .reduce(Montant.ZERO, Montant::plus);
        List<Ligne> lignes = c.echeances().stream()
                .map(e -> new Ligne(e.rang(), VuePrelevement.DATE.format(e.date()), e.montant(), e.statut())).toList();
        List<VueRelance> relances = f.relances().stream()
                .map(r -> new VueRelance(r.type(), rang(c, r.echeanceId()),
                        VuePrelevement.DATE.format(r.dateEcheance()), dateHeure(r.envoyeeLe(), clock)))
                .toList();
        List<VueNote> notes = f.notes().stream()
                .map(n -> new VueNote(dateHeure(n.ecriteLe(), clock), n.auteur(), n.texte())).toList();
        return new VueFicheRecouvrement(demandeId, f.statut(), c.numero(), c.designationProduit(),
                f.client().map(cl -> cl.prenom() + " " + cl.nom()), f.client().map(cl -> cl.telephone()),
                c.prix().prixVente(), restant, f.situation().enRetard(), lignes, relances, notes,
                f.peutRegulariser(), f.peutPasserEnRecouvrement(), f.peutSolder());
    }

    private static int rang(Contrat c, UUID echeanceId) {
        return c.echeances().stream().filter(e -> e.id().equals(echeanceId)).mapToInt(Echeance::rang).findFirst()
                .orElse(0);
    }

    private static String dateHeure(Instant instant, Clock clock) {
        return DATE_HEURE.format(instant.atZone(clock.getZone()));
    }
}
