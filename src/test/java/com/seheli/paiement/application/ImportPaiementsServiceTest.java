package com.seheli.paiement.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.seheli.audit.application.JournalAuditService;
import com.seheli.audit.application.JournalEnMemoire;
import com.seheli.commun.domain.Montant;
import com.seheli.contrat.application.AutorisationsEnMemoire;
import com.seheli.contrat.application.ContratAEtablir;
import com.seheli.contrat.application.ContratService;
import com.seheli.contrat.application.ContratsEnMemoire;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.Prix;
import com.seheli.paiement.domain.Paiement;
import com.seheli.paiement.domain.ReferenceApport;
import com.seheli.paiement.domain.TypeEcart;
import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.application.ParametreService;

class ImportPaiementsServiceTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");
    private static final Instant MAINTENANT = Instant.parse("2026-11-14T10:00:00Z");

    private final Clock clock = Clock.fixed(MAINTENANT, NOUAKCHOTT);
    private final PaiementsEnMemoire paiements = new PaiementsEnMemoire();
    private final JournalAuditService journal = new JournalAuditService(new JournalEnMemoire());
    private final ContratService contrats;
    private final PaiementService paiementService;
    private final ImportPaiementsService service;

    ImportPaiementsServiceTest() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        contrats = new ContratService(new ContratsEnMemoire(), new AutorisationsEnMemoire(), parametres, clock);
        paiementService = new PaiementService(paiements, contrats, journal, evenement -> { }, clock);
        service = new ImportPaiementsService(paiementService);
    }

    /** Contrat de l'exemple chiffré : apport 8 320 MRU. */
    private String referenceDUnContrat() {
        Contrat c = contrats.etablir(new ContratAEtablir(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Galaxy A35",
                new Prix(Montant.de(20_000), Montant.de(18_400), Montant.de(20_800), Montant.de(8_320)),
                Montant.de(8_320), 6, 25, Optional.empty(), MAINTENANT.minusSeconds(86_400)));
        return ReferenceApport.pourContrat(c.numero()).valeur();
    }

    private ResultatImport importer(String csv) {
        return service.importer(csv.getBytes(StandardCharsets.UTF_8), "recouvrement");
    }

    @Test
    void importe_les_paiements_et_liste_les_ecarts() {
        String conforme = referenceDUnContrat();
        String autreMontant = referenceDUnContrat();

        ResultatImport r = importer("""
                reference;montant;date
                %s;8320;2026-11-13
                %s;8 000;13/11/2026
                INCONNUE123;5000;2026-11-12
                ;100;2026-11-12
                %s;8320;2026-11-14
                """.formatted(conforme, autreMontant, conforme));

        assertThat(r.lignes()).extracting(LigneImportee::issue).containsExactly(IssueLigne.CONFORME,
                IssueLigne.MONTANT_DIFFERENT, IssueLigne.REFERENCE_INCONNUE, IssueLigne.INVALIDE,
                IssueLigne.DEJA_ENREGISTRE);
        assertThat(r.enregistres()).isEqualTo(3);
        assertThat(paiements.tous()).hasSize(3);
        assertThat(r.ecarts()).extracting(LigneImportee::numero).containsExactly(3, 4);
        assertThat(r.lignes().get(3).erreur()).contains(SaisiePaiementInvalideException.REFERENCE_INVALIDE);
        assertThat(r.lignes().get(4).paiement()).map(Paiement::reference).map(x -> x.valeur()).contains(conforme);

        assertThat(paiementService.ecarts(10)).extracting(p -> p.ecart().orElseThrow())
                .containsExactlyInAnyOrder(TypeEcart.MONTANT_DIFFERENT, TypeEcart.REFERENCE_INCONNUE);
        Paiement p = r.lignes().getFirst().paiement().orElseThrow();
        assertThat(journal.historique(PaiementService.objet(p.id()))).singleElement()
                .satisfies(e -> assertThat(e.action()).isEqualTo(PaiementService.ACTION_IMPORT));
    }

    @Test
    void reimporter_le_meme_fichier_n_enregistre_rien_de_plus() {
        String csv = referenceDUnContrat() + ";8320;2026-11-13\nINCONNUE123;5000;2026-11-12\n";
        importer(csv);

        ResultatImport second = importer(csv);

        assertThat(second.lignes()).extracting(LigneImportee::issue).containsOnly(IssueLigne.DEJA_ENREGISTRE);
        assertThat(second.ecarts()).isEmpty();
        assertThat(paiements.tous()).hasSize(2);
    }
}
