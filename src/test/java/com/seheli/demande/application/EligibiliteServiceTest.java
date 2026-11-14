package com.seheli.demande.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.seheli.commun.domain.Fraction;
import com.seheli.commun.domain.Montant;
import com.seheli.demande.domain.ConditionEliminatoire;
import com.seheli.demande.domain.DossierEligibilite;
import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.CleParametre;
import com.seheli.parametrage.domain.Parametre;

class EligibiliteServiceTest {

    private static final ZoneId NOUAKCHOTT = ZoneId.of("Africa/Nouakchott");

    private static EligibiliteService service(List<Parametre> lignes, LocalDate aujourdHui) {
        ParametreService parametres = new ParametreService(() -> lignes);
        parametres.afterSingletonsInstantiated();
        Clock clock = Clock.fixed(aujourdHui.atStartOfDay(NOUAKCHOTT).toInstant(), NOUAKCHOTT);
        return new EligibiliteService(parametres, clock);
    }

    private static DossierEligibilite neLe(LocalDate naissance) {
        return new DossierEligibilite(true, true, naissance, LocalDate.of(2020, 1, 1), Optional.empty(), true,
                Montant.de(30_000), Montant.ZERO, Montant.de(2_080), false);
    }

    @Test
    void regles_lues_dans_les_parametres_de_depart() {
        EligibiliteService s = service(ParametresDeDepart.lignes(), LocalDate.of(2026, 11, 11));

        assertThat(s.regles().ageMinimumAns()).isEqualTo(18);
        assertThat(s.regles().endettementMax()).isEqualTo(Fraction.de("1/3"));
    }

    @Test
    void la_date_d_evaluation_vient_de_l_horloge() {
        DossierEligibilite d = neLe(LocalDate.of(2008, 11, 11));

        assertThat(service(ParametresDeDepart.lignes(), LocalDate.of(2026, 11, 10)).evaluer(d).estEligible()).isFalse();
        assertThat(service(ParametresDeDepart.lignes(), LocalDate.of(2026, 11, 11)).evaluer(d).estEligible()).isTrue();
    }

    @Test
    void age_minimum_modifie_en_parametre() {
        EligibiliteService s = service(ParametresDeDepart.avec(CleParametre.AGE_MINIMUM_ANS, "21"),
                LocalDate.of(2026, 11, 11));

        assertThat(s.evaluer(neLe(LocalDate.of(2006, 1, 1))).nonSatisfaites())
                .extracting(r -> r.condition()).containsExactly(ConditionEliminatoire.AGE_MINIMUM);
    }
}
