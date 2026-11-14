package com.seheli.demonstration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.seheli.demonstration.domain.ReinitialisationEnCoursException;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.Parametres;

class ReinitialisationDemonstrationServiceTest {

    private final List<String> etapes = new ArrayList<>();
    private Runnable pendantLaRemiseANeuf = () -> { };
    private boolean chargementReussi = true;

    private final ParametreService parametres = new ParametreService(List::of) {
        @Override
        public Parametres recharger() {
            etapes.add("parametres");
            return null;
        }
    };

    private final DonneesDemonstrationService donnees = new DonneesDemonstrationService(null, null, null) {
        @Override
        public boolean chargerSiAbsentes() {
            etapes.add("donnees");
            return chargementReussi;
        }
    };

    private final ReinitialisationDemonstrationService service = new ReinitialisationDemonstrationService(() -> {
        etapes.add("base");
        pendantLaRemiseANeuf.run();
    }, parametres, () -> etapes.add("initialisations"), donnees);

    @Test
    void remet_la_base_a_neuf_puis_relit_les_parametres_rejoue_le_demarrage_et_recharge_les_donnees() {
        assertThat(service.reinitialiser()).isTrue();

        assertThat(etapes).containsExactly("base", "parametres", "initialisations", "donnees");
    }

    @Test
    void signale_un_chargement_des_donnees_interrompu() {
        chargementReussi = false;

        assertThat(service.reinitialiser()).isFalse();
    }

    @Test
    void refuse_une_seconde_reinitialisation_pendant_la_premiere() {
        pendantLaRemiseANeuf = () -> assertThatThrownBy(service::reinitialiser)
                .isInstanceOf(ReinitialisationEnCoursException.class);

        service.reinitialiser();

        assertThat(etapes).containsExactly("base", "parametres", "initialisations", "donnees");
    }

    @Test
    void accepte_une_nouvelle_reinitialisation_apres_un_echec() {
        pendantLaRemiseANeuf = () -> {
            throw new IllegalStateException("base indisponible");
        };
        assertThatThrownBy(service::reinitialiser).isInstanceOf(IllegalStateException.class);

        pendantLaRemiseANeuf = () -> { };
        assertThat(service.reinitialiser()).isTrue();
    }
}
