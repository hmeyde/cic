package com.cic.contrat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.cic.commun.domain.Montant;
import com.cic.contrat.domain.DureeAutorisee;
import com.cic.contrat.domain.EcheancePrevue;
import com.cic.contrat.domain.Echeancier;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;

class EcheancierServiceTest {

    private final EcheancierService service = service();

    private static EcheancierService service() {
        ParametreService parametres = new ParametreService(ParametresDeDepart::lignes);
        parametres.afterSingletonsInstantiated();
        return new EcheancierService(parametres);
    }

    @Test
    void lit_les_bornes_de_duree_dans_les_parametres() {
        assertThat(service.dureeAutorisee()).isEqualTo(new DureeAutorisee(3, 6));
    }

    @Test
    void etablit_l_exemple_chiffre() {
        Echeancier e = service.etablir(Montant.de(12_480), 6, LocalDate.of(2026, 11, 11), 25, Optional.empty());

        assertThat(e.echeances()).extracting(EcheancePrevue::montant).containsOnly(Montant.de(2_080));
    }

    @Test
    void refuse_une_duree_hors_parametres() {
        assertThatThrownBy(() -> service.etablir(Montant.de(12_480), 7, LocalDate.of(2026, 11, 11), 25,
                Optional.empty())).isInstanceOf(IllegalArgumentException.class);
    }
}
