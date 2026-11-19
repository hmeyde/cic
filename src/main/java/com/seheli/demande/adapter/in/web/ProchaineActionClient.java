package com.seheli.demande.adapter.in.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.seheli.commun.domain.Montant;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.Echeance;
import com.seheli.contrat.domain.StatutAutorisation;
import com.seheli.contrat.domain.StatutEcheance;
import com.seheli.demande.domain.PrealablesCommande;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.demande.domain.SuiviDossier;
import com.seheli.paiement.domain.InstructionsApport;
import com.seheli.paiement.domain.ReferenceEcheance;

/**
 * Carte « Prochaine action » du suivi : ce que le client doit faire maintenant, avec tout ce qu'il lui
 * faut sous la main. Vue destinée au gabarit, d'où les champs absents à {@code null} plutôt qu'en
 * {@code Optional}.
 *
 * <ul>
 *   <li>ACCEPTEE : instructions Masrvi de l'apport ({@code apport}), état de l'apport et de
 *       l'autorisation, compte à rebours du délai, documents à télécharger.</li>
 *   <li>APPORT_ET_AUTORISATION : les deux préalables cochés, Seheli commande.</li>
 *   <li>COMMANDEE : confirmation de la remise (formulaire dans le gabarit), contrat téléchargeable.</li>
 *   <li>LIVREE, EN_REMBOURSEMENT : prochaine échéance à provisionner.</li>
 *   <li>EN_RETARD, RECOUVREMENT : échéance à régulariser, avec sa référence Masrvi.</li>
 * </ul>
 *
 * @param apport instructions de paiement de l'apport, seulement en ACCEPTEE
 * @param apportEncaisse total reçu par la référence d'apport au moins égal à l'apport
 * @param autorisation statut de l'autorisation de prélèvement, {@code null} sans contrat
 * @param joursRestants jours restant avant l'annulation, seulement en ACCEPTEE
 * @param delaiEcoule part du délai écoulée (0 à 100), seulement en ACCEPTEE
 * @param documents contrat et autorisation téléchargeables (offre établie, dossier non annulé)
 * @param prochaineEcheance première échéance à venir, en LIVREE et EN_REMBOURSEMENT
 * @param aRegulariser première échéance en retard, en EN_RETARD et RECOUVREMENT
 * @param referenceRegularisation référence Masrvi de l'échéance à régulariser
 * @param totalEnRetard somme des échéances en retard
 * @param codeMarchand code marchand Masrvi de Seheli, pour l'apport ou la régularisation
 */
public record ProchaineActionClient(InstructionsApport apport, boolean apportEncaisse,
        StatutAutorisation autorisation, Long joursRestants, Integer delaiEcoule, boolean documents,
        Echeance prochaineEcheance, Echeance aRegulariser, String referenceRegularisation, Montant totalEnRetard,
        String codeMarchand) {

    /** Sans contrat (demande pas encore acceptée, ou refusée) : aucune donnée d'offre. */
    static ProchaineActionClient sansContrat() {
        return new ProchaineActionClient(null, false, null, null, null, false, null, null, null, null, null);
    }

    /**
     * @param instructions instructions Masrvi de l'apport du contrat
     * @param prealables apport encaissé et autorisation acceptée, tels qu'établis par les modules paiement et contrat
     * @param autorisation statut de l'autorisation, {@code null} si elle n'existe pas
     */
    static ProchaineActionClient depuis(SuiviDossier suivi, Contrat contrat, InstructionsApport instructions,
            PrealablesCommande prealables, StatutAutorisation autorisation, LocalDate aujourdhui) {
        Objects.requireNonNull(suivi, "suivi");
        Objects.requireNonNull(contrat, "contrat");
        StatutDossier statut = suivi.statut();
        boolean annulee = statut == StatutDossier.ANNULEE;
        List<Echeance> echeances = contrat.echeances();
        Echeance prochaine = statut == StatutDossier.LIVREE || statut == StatutDossier.EN_REMBOURSEMENT
                ? echeances.stream().filter(e -> e.statut() == StatutEcheance.A_VENIR).findFirst().orElse(null)
                : null;
        Echeance enRetard = null;
        String reference = null;
        Montant total = null;
        if (statut.estEnRetard()) {
            List<Echeance> retards = echeances.stream().filter(e -> e.statut() == StatutEcheance.EN_RETARD).toList();
            if (!retards.isEmpty()) {
                enRetard = retards.getFirst();
                reference = ReferenceEcheance.pourEcheance(contrat.numero(), enRetard.rang()).valeur();
                total = retards.stream().map(Echeance::montant).reduce(Montant.ZERO, Montant::plus);
            }
        }
        return new ProchaineActionClient(statut == StatutDossier.ACCEPTEE ? instructions : null,
                prealables.apportEncaisse(), autorisation,
                suivi.joursRestants(aujourdhui).orElse(null), suivi.pourcentageDelaiEcoule(aujourdhui).orElse(null),
                !annulee, prochaine, enRetard, reference, total, instructions.codeMarchand());
    }
}
