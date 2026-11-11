package com.cic.catalogue.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cic.catalogue.domain.Fournisseur;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;
import com.cic.parametrage.domain.CleParametre;
import com.cic.parametrage.domain.Parametre;

class FournisseurServiceTest {

    private final FournisseursEnMemoire repository = new FournisseursEnMemoire();
    private final FournisseurService service = service(ParametresDeDepart.lignes());

    private FournisseurService service(List<Parametre> lignes) {
        ParametreService parametres = new ParametreService(() -> lignes);
        parametres.afterSingletonsInstantiated();
        return new FournisseurService(repository, parametres);
    }

    @Test
    void cree_un_fournisseur_actif_et_l_enregistre() {
        Fournisseur cree = service.creer(" Mauritel Distribution ", new BigDecimal("8"));

        Fournisseur relu = service.parId(cree.id());
        assertThat(relu.nom()).isEqualTo("Mauritel Distribution");
        assertThat(relu.remiseNegocieePct()).isEqualByComparingTo("8");
        assertThat(relu.estActif()).isTrue();
        assertThat(service.lister()).extracting(Fournisseur::id).containsExactly(cree.id());
    }

    @Test
    void la_remise_doit_rester_dans_les_bornes_des_parametres() {
        assertThatThrownBy(() -> service.creer("A", new BigDecimal("4.99")))
                .isInstanceOf(RemiseHorsBornesException.class)
                .satisfies(e -> {
                    RemiseHorsBornesException hors = (RemiseHorsBornesException) e;
                    assertThat(hors.minPct()).isEqualByComparingTo("5");
                    assertThat(hors.maxPct()).isEqualByComparingTo("10");
                });
        assertThatThrownBy(() -> service.creer("A", new BigDecimal("10.01")))
                .isInstanceOf(RemiseHorsBornesException.class);
        assertThat(service.creer("A", new BigDecimal("5")).remiseNegocieePct()).isEqualByComparingTo("5");
        assertThat(service.creer("B", new BigDecimal("10")).remiseNegocieePct()).isEqualByComparingTo("10");
        assertThat(repository.tous()).hasSize(2);
    }

    @Test
    void les_bornes_suivent_la_table_parametre() {
        FournisseurService elargi = service(ParametresDeDepart.avec(CleParametre.REMISE_FOURNISSEUR_MAX_PCT, "12"));

        assertThat(elargi.creer("A", new BigDecimal("12")).remiseNegocieePct()).isEqualByComparingTo("12");
        assertThat(elargi.bornesRemise()).isEqualTo(new BornesRemise(new BigDecimal("5"), new BigDecimal("12")));
    }

    @Test
    void le_nom_est_unique_sans_tenir_compte_de_la_casse() {
        service.creer("Mauritel Distribution", new BigDecimal("8"));

        assertThatThrownBy(() -> service.creer("  MAURITEL distribution", new BigDecimal("6")))
                .isInstanceOf(NomFournisseurDejaUtiliseException.class)
                .extracting(e -> ((NomFournisseurDejaUtiliseException) e).code())
                .isEqualTo("bo.fournisseur.nom.deja.utilise");
        assertThat(repository.tous()).hasSize(1);
    }

    @Test
    void modifie_le_nom_la_remise_et_l_etat() {
        UUID id = service.creer("Mauritel", new BigDecimal("8")).id();

        service.modifier(id, "Mauritel Distribution", new BigDecimal("7.5"), false);

        Fournisseur relu = service.parId(id);
        assertThat(relu.nom()).isEqualTo("Mauritel Distribution");
        assertThat(relu.remiseNegocieePct()).isEqualByComparingTo("7.5");
        assertThat(relu.estActif()).isFalse();

        service.modifier(id, "Mauritel Distribution", new BigDecimal("7.5"), true);
        assertThat(service.parId(id).estActif()).isTrue();
    }

    @Test
    void un_fournisseur_peut_garder_son_nom_en_changeant_de_casse() {
        UUID id = service.creer("mauritel", new BigDecimal("8")).id();

        service.modifier(id, "Mauritel", new BigDecimal("8"), true);

        assertThat(service.parId(id).nom()).isEqualTo("Mauritel");
    }

    @Test
    void la_modification_refuse_le_nom_d_un_autre_fournisseur() {
        service.creer("Mauritel", new BigDecimal("8"));
        UUID id = service.creer("Chinguitel", new BigDecimal("6")).id();

        assertThatThrownBy(() -> service.modifier(id, "mauritel", new BigDecimal("6"), true))
                .isInstanceOf(NomFournisseurDejaUtiliseException.class);
        assertThat(service.parId(id).nom()).isEqualTo("Chinguitel");
    }

    @Test
    void la_modification_refuse_une_nouvelle_remise_hors_bornes() {
        UUID id = service.creer("Mauritel", new BigDecimal("8")).id();

        assertThatThrownBy(() -> service.modifier(id, "Mauritel", new BigDecimal("11"), true))
                .isInstanceOf(RemiseHorsBornesException.class);
        assertThat(service.parId(id).remiseNegocieePct()).isEqualByComparingTo("8");
    }

    @Test
    void une_remise_inchangee_sortie_des_bornes_n_empeche_pas_de_renommer_ou_desactiver() {
        Fournisseur ancien = Fournisseur.reconstituer(UUID.randomUUID(), "Ancien", new BigDecimal("12"), true);
        repository.enregistrer(ancien);

        service.modifier(ancien.id(), "Ancien fournisseur", new BigDecimal("12.00"), false);

        Fournisseur relu = service.parId(ancien.id());
        assertThat(relu.nom()).isEqualTo("Ancien fournisseur");
        assertThat(relu.estActif()).isFalse();
    }

    @Test
    void un_identifiant_inconnu_leve_une_erreur_metier() {
        assertThatThrownBy(() -> service.parId(UUID.randomUUID()))
                .isInstanceOf(FournisseurIntrouvableException.class);
        assertThatThrownBy(() -> service.modifier(UUID.randomUUID(), "A", new BigDecimal("8"), true))
                .isInstanceOf(FournisseurIntrouvableException.class);
    }
}
