package com.seheli.paiement.adapter.in.web;

import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import com.seheli.commun.domain.Montant;
import com.seheli.paiement.domain.Rattachement;
import com.seheli.paiement.domain.Paiement;
import com.seheli.paiement.domain.StatutRapprochement;
import com.seheli.paiement.domain.TypeEcart;

/**
 * Paiement tel qu'affiché au back-office : dates à l'heure locale.
 *
 * @param numeroContrat  contrat du dossier rattaché ; vide si non rapproché
 * @param montantAttendu apport fixé au contrat ; vide si non rapproché
 * @param ecart          écart à traiter ; vide si le paiement est conforme
 * @param difference     écart de montant en valeur absolue ; vide sans écart de montant
 * @param tropPercu      le montant reçu dépasse l'apport attendu (sinon il manque {@code difference})
 */
record VuePaiement(UUID id, String reference, Montant montant, String recuLe, StatutRapprochement statut,
        Optional<String> numeroContrat, Optional<Montant> montantAttendu, boolean ecartDeMontant, String saisiLe,
        String saisiPar, Optional<TypeEcart> ecart, Optional<Montant> difference, boolean tropPercu) {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static VuePaiement depuis(Paiement p, Clock clock) {
        Optional<Montant> attendu = p.rattachement().map(Rattachement::montant);
        boolean tropPercu = attendu.map(a -> p.montant().estSuperieurA(a)).orElse(false);
        Optional<Montant> difference = p.ecartDeMontant()
                ? attendu.map(a -> tropPercu ? p.montant().moins(a) : a.moins(p.montant()))
                : Optional.empty();
        return new VuePaiement(p.id(), p.reference().valeur(), p.montant(), DATE.format(p.recuLe()), p.statut(),
                p.rattachement().map(Rattachement::numeroContrat), attendu, p.ecartDeMontant(),
                DATE_HEURE.format(p.saisiLe().atZone(clock.getZone())), p.saisiPar(), p.ecart(), difference,
                tropPercu);
    }
}
