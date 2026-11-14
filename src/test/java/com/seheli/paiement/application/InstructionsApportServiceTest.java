package com.seheli.paiement.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.seheli.commun.domain.Montant;
import com.seheli.paiement.domain.InstructionsApport;
import com.seheli.parametrage.ParametresDeDepart;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.CleParametre;

class InstructionsApportServiceTest {

    private static InstructionsApportService service(String codeMarchand) {
        ParametreService parametres = new ParametreService(
                () -> ParametresDeDepart.avec(CleParametre.MASRVI_CODE_MARCHAND, codeMarchand));
        parametres.afterSingletonsInstantiated();
        return new InstructionsApportService(parametres);
    }

    @Test
    void montant_code_marchand_parametre_et_reference_du_dossier() {
        InstructionsApport i = service("012345").pour("SEH-2026-000001", Montant.de(8_320));

        assertThat(i.montant()).isEqualTo(Montant.de(8_320));
        assertThat(i.codeMarchand()).isEqualTo("012345");
        assertThat(i.reference().valeur()).isEqualTo("AP20260000011");
    }

    @Test
    void le_code_marchand_suit_la_table_parametre() {
        assertThat(service(" 778899 ").pour("SEH-2026-000001", Montant.de(8_320)).codeMarchand()).isEqualTo("778899");
    }
}
