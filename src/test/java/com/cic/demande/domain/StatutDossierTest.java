package com.cic.demande.domain;

import static com.cic.demande.domain.StatutDossier.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.cic.commun.domain.ErreurMetier;

class StatutDossierTest {

    /** Transitions permises, recopiées de CLAUDE.md. */
    private static final List<ChangementAttendu> PERMISES = List.of(
            new ChangementAttendu(DEMANDE_SOUMISE, EN_ANALYSE),
            new ChangementAttendu(EN_ANALYSE, ACCEPTEE),
            new ChangementAttendu(EN_ANALYSE, REFUSEE),
            new ChangementAttendu(ACCEPTEE, APPORT_ET_AUTORISATION),
            new ChangementAttendu(ACCEPTEE, ANNULEE),
            new ChangementAttendu(APPORT_ET_AUTORISATION, COMMANDEE),
            new ChangementAttendu(COMMANDEE, LIVREE),
            new ChangementAttendu(LIVREE, EN_REMBOURSEMENT),
            new ChangementAttendu(EN_REMBOURSEMENT, SOLDEE),
            new ChangementAttendu(EN_REMBOURSEMENT, EN_RETARD),
            new ChangementAttendu(EN_RETARD, EN_REMBOURSEMENT),
            new ChangementAttendu(EN_RETARD, RECOUVREMENT));

    private record ChangementAttendu(StatutDossier depuis, StatutDossier vers) {
    }

    static Stream<Arguments> transitionsPermises() {
        return PERMISES.stream().map(c -> Arguments.of(c.depuis(), c.vers()));
    }

    /** Toutes les paires (y compris vers soi-même) absentes de la liste des transitions permises. */
    static Stream<Arguments> transitionsInterdites() {
        List<Arguments> interdites = new ArrayList<>();
        for (StatutDossier depuis : values()) {
            for (StatutDossier vers : values()) {
                if (!PERMISES.contains(new ChangementAttendu(depuis, vers))) {
                    interdites.add(Arguments.of(depuis, vers));
                }
            }
        }
        return interdites.stream();
    }

    @ParameterizedTest(name = "{0} → {1} permise")
    @MethodSource("transitionsPermises")
    void transition_permise(StatutDossier depuis, StatutDossier vers) {
        assertThat(depuis.peutPasserA(vers)).isTrue();

        ChangementStatut changement = depuis.passerA(vers);

        assertThat(changement.depuis()).isEqualTo(depuis);
        assertThat(changement.vers()).isEqualTo(vers);
    }

    @ParameterizedTest(name = "{0} → {1} interdite")
    @MethodSource("transitionsInterdites")
    void transition_interdite(StatutDossier depuis, StatutDossier vers) {
        assertThat(depuis.peutPasserA(vers)).isFalse();

        assertThatThrownBy(() -> depuis.passerA(vers))
                .isInstanceOf(TransitionInterditeException.class)
                .isInstanceOf(ErreurMetier.class)
                .satisfies(e -> {
                    TransitionInterditeException t = (TransitionInterditeException) e;
                    assertThat(t.code()).isEqualTo("dossier.transition.interdite");
                    assertThat(t.depuis()).isEqualTo(depuis);
                    assertThat(t.vers()).isEqualTo(vers);
                });
    }

    @Test
    void les_transitions_interdites_couvrent_toutes_les_autres_paires() {
        int paires = values().length * values().length;
        assertThat(transitionsInterdites().count()).isEqualTo(paires - PERMISES.size());
    }

    @Test
    void les_statuts_finaux_sont_refusee_annulee_soldee_et_recouvrement() {
        Set<StatutDossier> finaux = EnumSet.noneOf(StatutDossier.class);
        for (StatutDossier s : values()) {
            if (s.estFinal()) {
                finaux.add(s);
            }
        }
        assertThat(finaux).containsExactlyInAnyOrder(REFUSEE, ANNULEE, SOLDEE, RECOUVREMENT);
    }

    @Test
    void un_dossier_suit_le_parcours_complet_avec_un_retard_regularise() {
        StatutDossier statut = DEMANDE_SOUMISE;
        for (StatutDossier suivant : List.of(EN_ANALYSE, ACCEPTEE, APPORT_ET_AUTORISATION, COMMANDEE,
                LIVREE, EN_REMBOURSEMENT, EN_RETARD, EN_REMBOURSEMENT, SOLDEE)) {
            statut = statut.passerA(suivant).vers();
        }
        assertThat(statut).isEqualTo(SOLDEE);
    }

    @Test
    void on_ne_peut_pas_sauter_l_analyse_ni_revenir_en_arriere() {
        assertThatThrownBy(() -> DEMANDE_SOUMISE.passerA(ACCEPTEE))
                .isInstanceOf(TransitionInterditeException.class)
                .hasMessageContaining("DEMANDE_SOUMISE").hasMessageContaining("ACCEPTEE");
        assertThatThrownBy(() -> LIVREE.passerA(COMMANDEE)).isInstanceOf(TransitionInterditeException.class);
        assertThatThrownBy(() -> SOLDEE.passerA(EN_REMBOURSEMENT)).isInstanceOf(TransitionInterditeException.class);
    }

    @Test
    void un_changement_de_statut_ne_peut_pas_etre_construit_pour_une_transition_interdite() {
        assertThatThrownBy(() -> new ChangementStatut(EN_ANALYSE, COMMANDEE))
                .isInstanceOf(TransitionInterditeException.class);
    }

    @Test
    void la_cible_est_obligatoire() {
        assertThatThrownBy(() -> EN_ANALYSE.passerA(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void les_suivants_ne_sont_pas_modifiables() {
        assertThatThrownBy(() -> EN_ANALYSE.suivants().add(SOLDEE))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
