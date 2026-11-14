package com.seheli.contrat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.seheli.commun.domain.Fraction;
import com.seheli.commun.domain.Montant;
import com.seheli.contrat.domain.DureeAutorisee;
import com.seheli.contrat.domain.EcheancePrevue;
import com.seheli.contrat.domain.Echeancier;
import com.seheli.contrat.domain.EndettementDepasseException;
import com.seheli.contrat.domain.FinContratDepasseeException;
import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.CleParametre;
import com.seheli.parametrage.domain.Parametre;

class EcheancierServiceTest {

    private static final LocalDate DEPART = LocalDate.of(2026, 11, 11);
    private static final Montant SALAIRE = Montant.de(30_000);

    private final EcheancierService service = service(ParametresDeDepart.lignes());

    private static EcheancierService service(List<Parametre> lignes) {
        ParametreService parametres = new ParametreService(() -> lignes);
        parametres.afterSingletonsInstantiated();
        return new EcheancierService(parametres);
    }

    @Test
    void lit_les_bornes_de_duree_dans_les_parametres() {
        assertThat(service.dureeAutorisee()).isEqualTo(new DureeAutorisee(3, 6));
    }

    @Test
    void etablit_l_exemple_chiffre() {
        Echeancier e = service.etablir(Montant.de(12_480), 6, DEPART, 25, Optional.empty(), SALAIRE, Montant.ZERO);

        assertThat(e.echeances()).extracting(EcheancePrevue::montant).containsOnly(Montant.de(2_080));
    }

    @Test
    void refuse_une_duree_hors_parametres() {
        assertThatThrownBy(() -> service.etablir(Montant.de(12_480), 7, DEPART, 25,
                Optional.empty(), SALAIRE, Montant.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applique_le_tiers_du_salaire_net_lu_dans_les_parametres() {
        assertThat(service.etablir(Montant.de(12_480), 6, DEPART, 25, Optional.empty(),
                Montant.de(6_240), Montant.ZERO).nombre()).isEqualTo(6);

        assertThatThrownBy(() -> service.etablir(Montant.de(12_480), 6, DEPART, 25, Optional.empty(),
                Montant.de(6_239), Montant.ZERO))
                .isInstanceOfSatisfying(EndettementDepasseException.class,
                        ex -> assertThat(ex.partMax()).isEqualTo(new Fraction(1, 3)));
    }

    @Test
    void tient_compte_des_mensualites_cic_en_cours() {
        assertThatThrownBy(() -> service.etablir(Montant.de(12_480), 6, DEPART, 25, Optional.empty(),
                Montant.de(9_000), Montant.de(1_000)))
                .isInstanceOf(EndettementDepasseException.class);
    }

    @Test
    void la_part_maximale_du_salaire_est_modifiable_sans_redeploiement() {
        EcheancierService unQuart = service(ParametresDeDepart.avec(CleParametre.ENDETTEMENT_MAX_FRACTION, "1/4"));

        // 2 080 ≤ 6 240 / 3 mais 2 080 > 6 240 / 4
        assertThatThrownBy(() -> unQuart.etablir(Montant.de(12_480), 6, DEPART, 25, Optional.empty(),
                Montant.de(6_240), Montant.ZERO))
                .isInstanceOf(EndettementDepasseException.class);
    }

    @Test
    void refuse_un_cdd_qui_finit_avant_la_derniere_echeance() {
        assertThatThrownBy(() -> service.etablir(Montant.de(12_480), 6, DEPART, 25,
                Optional.of(LocalDate.of(2027, 4, 25)), SALAIRE, Montant.ZERO))
                .isInstanceOf(FinContratDepasseeException.class);
    }
}
