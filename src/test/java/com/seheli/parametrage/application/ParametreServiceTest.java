package com.seheli.parametrage.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.domain.CleParametre;
import com.seheli.parametrage.domain.Parametre;
import com.seheli.parametrage.domain.ParametreInvalideException;

class ParametreServiceTest {

    private List<Parametre> enBase = ParametresDeDepart.lignes();
    private final ParametreService service = new ParametreService(() -> enBase);

    @Test
    void charge_les_parametres_au_demarrage() {
        service.afterSingletonsInstantiated();

        assertThat(service.pourcentage(CleParametre.APPORT_MINIMUM_PCT)).isEqualByComparingTo("40");
        assertThat(service.entier(CleParametre.DUREE_MIN_MOIS)).isEqualTo(3);
    }

    @Test
    void empeche_le_demarrage_si_un_parametre_manque() {
        enBase = List.of();

        assertThatThrownBy(service::afterSingletonsInstantiated).isInstanceOf(ParametreInvalideException.class);
    }

    @Test
    void refuse_la_lecture_avant_chargement() {
        assertThatThrownBy(() -> service.entier(CleParametre.DUREE_MIN_MOIS))
                .isInstanceOf(ParametreInvalideException.class);
    }

    @Test
    void prend_en_compte_une_modification_au_rechargement() {
        service.afterSingletonsInstantiated();
        enBase = ParametresDeDepart.avec(CleParametre.APPORT_MINIMUM_PCT, "50");

        service.rechargerPeriodiquement();

        assertThat(service.pourcentage(CleParametre.APPORT_MINIMUM_PCT)).isEqualByComparingTo("50");
    }

    @Test
    void conserve_les_anciennes_valeurs_si_le_rechargement_est_invalide() {
        service.afterSingletonsInstantiated();
        enBase = ParametresDeDepart.avec(CleParametre.DUREE_MIN_MOIS, "9");

        service.rechargerPeriodiquement();

        assertThat(service.entier(CleParametre.DUREE_MIN_MOIS)).isEqualTo(3);
        assertThatThrownBy(service::recharger).isInstanceOf(ParametreInvalideException.class);
        assertThat(service.entier(CleParametre.DUREE_MIN_MOIS)).isEqualTo(3);
    }
}
