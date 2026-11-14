package com.seheli.comptabilite.adapter.in.evenement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.seheli.audit.application.JournalAuditService;
import com.seheli.audit.application.JournalEnMemoire;
import com.seheli.commun.domain.Montant;
import com.seheli.comptabilite.application.ComptabiliteService;
import com.seheli.comptabilite.application.EcrituresEnMemoire;
import com.seheli.comptabilite.domain.Compte;
import com.seheli.comptabilite.domain.EcritureComptable;
import com.seheli.comptabilite.domain.EcritureDejaContrepasseeException;
import com.seheli.comptabilite.domain.LigneEcriture;
import com.seheli.comptabilite.domain.NatureEcriture;
import com.seheli.comptabilite.domain.Sens;
import com.seheli.contrat.application.AutorisationsEnMemoire;
import com.seheli.contrat.application.ContratAEtablir;
import com.seheli.contrat.application.ContratService;
import com.seheli.contrat.application.ContratsEnMemoire;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.Echeance;
import com.seheli.contrat.domain.Prix;
import com.seheli.demande.CommandeFournisseurValidee;
import com.seheli.demande.StatutDossierChange;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.paiement.ObjetPaiement;
import com.seheli.paiement.PaiementNonRapproche;
import com.seheli.paiement.PaiementRapproche;
import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.application.ParametreService;

class EcrituresListenerTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");
    private static final Instant MAINTENANT = Instant.parse("2026-11-14T10:00:00Z");
    private static final LocalDate AUJOURD_HUI = LocalDate.of(2026, 11, 14);

    private final Clock clock = Clock.fixed(MAINTENANT, NOUAKCHOTT);
    private final EcrituresEnMemoire ecritures = new EcrituresEnMemoire();
    private final ContratService contrats;
    private final ComptabiliteService comptabilite;
    private final EcrituresListener listener;

    EcrituresListenerTest() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        contrats = new ContratService(new ContratsEnMemoire(), new AutorisationsEnMemoire(), parametres, clock);
        comptabilite = new ComptabiliteService(ecritures, contrats, new JournalAuditService(new JournalEnMemoire()),
                clock);
        listener = new EcrituresListener(comptabilite);
    }

    /** Contrat de l'exemple chiffré : prix de vente 20 800, apport 8 320, 6 × 2 080. */
    private Contrat contrat() {
        return contrats.etablir(new ContratAEtablir(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Galaxy A35",
                new Prix(Montant.de(20_000), Montant.de(18_400), Montant.de(20_800), Montant.de(8_320)),
                Montant.de(8_320), 6, 25, Optional.empty(), MAINTENANT.minusSeconds(86_400)));
    }

    private static PaiementRapproche apport(Contrat c, Montant montant) {
        return new PaiementRapproche(UUID.randomUUID(), c.demandeId(), c.id(), c.numero(), c.clientId(),
                ObjetPaiement.APPORT, null, montant, c.apport(), AUJOURD_HUI, MAINTENANT);
    }

    private static PaiementRapproche echeance(Contrat c, Echeance e) {
        return new PaiementRapproche(UUID.randomUUID(), c.demandeId(), c.id(), c.numero(), c.clientId(),
                ObjetPaiement.ECHEANCE, e.rang(), e.montant(), e.montant(), e.date(), MAINTENANT);
    }

    private static CommandeFournisseurValidee commande(Contrat c) {
        return new CommandeFournisseurValidee(UUID.randomUUID(), "BC-2026-000001", c.demandeId(), c.id(),
                c.fournisseurId(), c.prix().prixAchat(), MAINTENANT);
    }

    private static StatutDossierChange statut(Contrat c, StatutDossier depuis, StatutDossier vers) {
        return new StatutDossierChange(c.demandeId(), c.clientId(), depuis, vers, MAINTENANT);
    }

    private Map<Compte, Long> soldes() {
        Map<Compte, Long> soldes = new EnumMap<>(Compte.class);
        for (EcritureComptable e : ecritures.toutes()) {
            for (LigneEcriture l : e.lignes()) {
                long m = l.montant().valeur().longValueExact();
                soldes.merge(l.compte(), l.sens() == Sens.DEBIT ? m : -m, Long::sum);
            }
        }
        return soldes;
    }

    @Test
    void chaque_mouvement_d_argent_d_un_dossier_produit_ses_ecritures() {
        Contrat c = contrat();

        listener.paiementRapproche(apport(c, c.apport()));
        listener.commandeFournisseurValidee(commande(c));
        listener.statutDossierChange(statut(c, StatutDossier.COMMANDEE, StatutDossier.LIVREE));
        listener.statutDossierChange(statut(c, StatutDossier.LIVREE, StatutDossier.EN_REMBOURSEMENT));
        c.echeances().forEach(e -> listener.paiementRapproche(echeance(c, e)));

        assertThat(comptabilite.duDossier(c.demandeId())).extracting(EcritureComptable::nature).containsExactly(
                NatureEcriture.APPORT_ENCAISSE, NatureEcriture.PAIEMENT_FOURNISSEUR, NatureEcriture.VENTE_LIVREE,
                NatureEcriture.ECHEANCE_ENCAISSEE, NatureEcriture.ECHEANCE_ENCAISSEE,
                NatureEcriture.ECHEANCE_ENCAISSEE, NatureEcriture.ECHEANCE_ENCAISSEE,
                NatureEcriture.ECHEANCE_ENCAISSEE, NatureEcriture.ECHEANCE_ENCAISSEE);
        assertThat(comptabilite.duDossier(c.demandeId())).allSatisfy(e -> {
            assertThat(e.total(Sens.DEBIT)).isEqualTo(e.total(Sens.CREDIT));
            assertThat(e.contratId()).contains(c.id());
        });
        assertThat(soldes()).containsEntry(Compte.BANQUE, 2_400L).containsEntry(Compte.CLIENTS, 0L)
                .containsEntry(Compte.CLIENTS_APPORTS, 0L).containsEntry(Compte.ACHATS, 18_400L)
                .containsEntry(Compte.VENTES, -20_800L);
        assertThat(comptabilite.duDossier(c.demandeId()).get(3).libelle())
                .isEqualTo("Échéance 1 du contrat " + c.numero());
        assertThat(comptabilite.duDossier(c.demandeId()).get(1).date()).isEqualTo(AUJOURD_HUI);
    }

    @Test
    void l_apport_est_comptabilise_pour_le_montant_recu_meme_en_ecart() {
        Contrat c = contrat();

        listener.paiementRapproche(apport(c, Montant.de(8_000)));

        assertThat(soldes()).containsEntry(Compte.BANQUE, 8_000L).containsEntry(Compte.CLIENTS_APPORTS, -8_000L);
    }

    @Test
    void un_paiement_sans_dossier_est_encaisse_en_attente_d_identification() {
        UUID paiement = UUID.randomUUID();

        listener.paiementNonRapproche(new PaiementNonRapproche(paiement, Montant.de(500), AUJOURD_HUI, MAINTENANT));

        assertThat(ecritures.toutes()).singleElement().satisfies(e -> {
            assertThat(e.nature()).isEqualTo(NatureEcriture.PAIEMENT_A_IDENTIFIER);
            assertThat(e.sourceId()).isEqualTo(paiement);
            assertThat(e.demandeId()).isEmpty();
        });
        assertThat(soldes()).containsEntry(Compte.BANQUE, 500L).containsEntry(Compte.PAIEMENTS_A_IDENTIFIER, -500L);
    }

    @Test
    void un_evenement_rejoue_ne_comptabilise_pas_deux_fois() {
        Contrat c = contrat();
        PaiementRapproche apport = apport(c, c.apport());
        CommandeFournisseurValidee commande = commande(c);
        StatutDossierChange livree = statut(c, StatutDossier.COMMANDEE, StatutDossier.LIVREE);

        for (int i = 0; i < 2; i++) {
            listener.paiementRapproche(apport);
            listener.commandeFournisseurValidee(commande);
            listener.statutDossierChange(livree);
        }

        assertThat(ecritures.toutes()).hasSize(3);
    }

    @Test
    void les_autres_changements_de_statut_ne_produisent_pas_d_ecriture() {
        Contrat c = contrat();

        listener.statutDossierChange(statut(c, StatutDossier.APPORT_ET_AUTORISATION, StatutDossier.COMMANDEE));
        listener.statutDossierChange(statut(c, StatutDossier.EN_REMBOURSEMENT, StatutDossier.SOLDEE));

        assertThat(ecritures.toutes()).isEmpty();
    }

    @Test
    void une_ecriture_se_corrige_par_contrepassation_une_seule_fois() {
        Contrat c = contrat();
        listener.paiementRapproche(apport(c, c.apport()));
        EcritureComptable origine = ecritures.toutes().getFirst();

        EcritureComptable inverse = comptabilite.contrepasser(origine.id(), "Saisie en double", "admin");

        assertThat(inverse.contrepasse()).contains(origine.id());
        assertThat(ecritures.toutes()).containsExactly(origine, inverse);
        assertThat(soldes().values()).containsOnly(0L);
        assertThatThrownBy(() -> comptabilite.contrepasser(origine.id(), "", "admin"))
                .isInstanceOf(EcritureDejaContrepasseeException.class);
        assertThatThrownBy(() -> comptabilite.contrepasser(inverse.id(), "", "admin"))
                .isInstanceOf(EcritureDejaContrepasseeException.class);
    }
}
