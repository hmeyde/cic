package com.cic.inscription.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.cic.inscription.domain.Inscription;
import com.cic.inscription.domain.InscriptionInvalideException;
import com.cic.inscription.domain.TelephoneDejaInscritException;
import com.cic.inscription.domain.TrancheSalaire;
import com.cic.inscription.domain.TypeContrat;

class InscriptionServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneId.of("Africa/Nouakchott"));
    private final InscriptionsEnMemoire depot = new InscriptionsEnMemoire();
    private final InscriptionService service = new InscriptionService(depot, clock);

    private static SaisieInscription saisie(String telephone) {
        return new SaisieInscription("Mariem Sidi", telephone, "SNIM", "CDI", "DE_10000_A_20000", "Smartphone", true);
    }

    @Test
    void une_inscription_est_enregistree() {
        Inscription i = service.inscrire(saisie("22 12 34 56"));

        assertThat(depot.toutes()).containsExactly(i);
        assertThat(i.typeContrat()).isEqualTo(TypeContrat.CDI);
        assertThat(i.trancheSalaire()).isEqualTo(TrancheSalaire.DE_10000_A_20000);
        assertThat(i.creeLe()).isEqualTo(clock.instant());
    }

    @Test
    void un_doublon_par_telephone_est_refuse_meme_ecrit_autrement() {
        service.inscrire(saisie("22123456"));

        assertThatThrownBy(() -> service.inscrire(saisie("+222 22 12 34 56")))
                .isInstanceOf(TelephoneDejaInscritException.class);
        assertThat(depot.toutes()).hasSize(1);
    }

    @Test
    void un_type_de_contrat_inconnu_est_refuse() {
        assertThatThrownBy(() -> service.inscrire(new SaisieInscription("Mariem", "22123456", "SNIM", "STAGE",
                "DE_10000_A_20000", "Smartphone", true)))
                .hasFieldOrPropertyWithValue("code", InscriptionInvalideException.TYPE_CONTRAT_INVALIDE);
    }
}
