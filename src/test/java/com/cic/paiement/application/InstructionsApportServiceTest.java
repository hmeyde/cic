package com.cic.paiement.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.cic.commun.domain.Montant;
import com.cic.paiement.domain.InstructionsApport;
import com.cic.parametrage.ParametresDeDepart;
import com.cic.parametrage.application.ParametreService;
import com.cic.parametrage.domain.CleParametre;

class InstructionsApportServiceTest {

    private static InstructionsApportService service(String codeMarchand) {
        ParametreService parametres = new ParametreService(
                () -> ParametresDeDepart.avec(CleParametre.MASRVI_CODE_MARCHAND, codeMarchand));
        parametres.afterSingletonsInstantiated();
        return new InstructionsApportService(parametres);
    }

    @Test
    void montant_code_marchand_parametre_et_reference_du_dossier() {
        InstructionsApport i = service("012345").pour("CIC-2026-000001", Montant.de(8_320));

        assertThat(i.montant()).isEqualTo(Montant.de(8_320));
        assertThat(i.codeMarchand()).isEqualTo("012345");
        assertThat(i.reference().valeur()).isEqualTo("AP20260000011");
    }

    @Test
    void le_code_marchand_suit_la_table_parametre() {
        assertThat(service(" 778899 ").pour("CIC-2026-000001", Montant.de(8_320)).codeMarchand()).isEqualTo("778899");
    }
}
