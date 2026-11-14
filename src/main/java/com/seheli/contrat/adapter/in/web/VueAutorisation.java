package com.seheli.contrat.adapter.in.web;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.seheli.audit.domain.EntreeJournal;
import com.seheli.audit.domain.TypeActeur;
import com.seheli.contrat.application.FicheAutorisation;
import com.seheli.contrat.application.LigneAutorisation;
import com.seheli.contrat.domain.AutorisationPrelevement;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.DocumentSigne;
import com.seheli.contrat.domain.StatutAutorisation;
import com.seheli.contrat.domain.TypeAutorisation;

/**
 * Autorisation de prélèvement telle qu'affichée au back-office : dates à l'heure locale, actions
 * possibles au statut actuel.
 *
 * @param client vide si le client est introuvable
 */
record VueAutorisation(UUID id, UUID demandeId, String numeroContrat, String designation, Optional<String> client,
        TypeAutorisation type, StatutAutorisation statut, String emiseLe, String statutChangeLe,
        Optional<VueDocumentSigne> documentSigne, boolean depotPossible, boolean transmissionPossible,
        boolean acceptationPossible, boolean activationPossible, boolean revocationPossible,
        List<VueEntreeJournal> journal) {

    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    record VueDocumentSigne(String format, long tailleKo, String deposeLe, String deposePar) {
    }

    /** @param acteur identifiant du membre du back-office ; vide pour le client et un traitement automatique */
    record VueEntreeJournal(String le, TypeActeur typeActeur, Optional<String> acteur, String action,
            Optional<String> avant, Optional<String> apres, Optional<String> detail) {
    }

    static VueAutorisation depuis(LigneAutorisation l, Clock clock) {
        return vue(l.autorisation(), l.numeroContrat(), l.designation(),
                l.client().isEmpty() ? Optional.empty() : Optional.of(l.client()), List.of(), clock);
    }

    static VueAutorisation depuis(FicheAutorisation f, Clock clock) {
        Contrat c = f.contrat();
        return vue(f.autorisation(), c.numero(), c.designationProduit(),
                f.client().map(cl -> (cl.prenom() + " " + cl.nom()).strip()),
                f.journal().stream().map(e -> entree(e, clock)).toList(), clock);
    }

    private static VueAutorisation vue(AutorisationPrelevement a, String numero, String designation,
            Optional<String> client, List<VueEntreeJournal> journal, Clock clock) {
        StatutAutorisation s = a.statut();
        return new VueAutorisation(a.id(), a.demandeId(), numero, designation, client, a.type(), s,
                date(a.emiseLe(), clock), date(a.statutChangeLe(), clock),
                a.documentSigne().map(d -> document(d, clock)), s.accepteDocumentSigne(),
                s.peutPasserA(StatutAutorisation.TRANSMISE), s.peutPasserA(StatutAutorisation.ACCEPTEE),
                s.peutPasserA(StatutAutorisation.ACTIVE), s.peutPasserA(StatutAutorisation.REVOQUEE), journal);
    }

    private static VueDocumentSigne document(DocumentSigne d, Clock clock) {
        return new VueDocumentSigne(d.format().name(), Math.max(1, (d.tailleOctets() + 1023) / 1024),
                date(d.deposeLe(), clock), d.deposePar());
    }

    private static VueEntreeJournal entree(EntreeJournal e, Clock clock) {
        Optional<String> acteur = e.acteur().type() == TypeActeur.BACK_OFFICE
                ? Optional.of(e.acteur().identifiant()) : Optional.empty();
        return new VueEntreeJournal(date(e.le(), clock), e.acteur().type(), acteur, e.action(), e.avant(),
                e.apres(), e.detail());
    }

    private static String date(Instant instant, Clock clock) {
        return DATE_HEURE.format(instant.atZone(clock.getZone()));
    }
}
