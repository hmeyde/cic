package com.seheli.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class NombresDialectTest {

    private final NombresDialect.Nombres nombres = new NombresDialect.Nombres();

    @Test
    void entierSepareLesMilliersParUneEspaceInsecable() {
        // Espace ordinaire : en arabe (dir="rtl"), « 20 800 » s'affichait « 800 20 ».
        assertThat(nombres.entier(new BigDecimal("20800"))).isEqualTo("20 800");
        assertThat(nombres.entier(1234567L)).isEqualTo("1 234 567");
        assertThat(nombres.entier(800)).isEqualTo("800");
        assertThat(nombres.entier(0)).isEqualTo("0");
    }

    @Test
    void entierArrondiALUnite() {
        assertThat(nombres.entier(new BigDecimal("2079.5"))).isEqualTo("2 080");
    }

    @Test
    void decimalAvecVirgule() {
        assertThat(nombres.decimal(new BigDecimal("35"), 1)).isEqualTo("35,0");
        assertThat(nombres.decimal(new BigDecimal("1234.56"), 1)).isEqualTo("1 234,6");
    }

    @Test
    void valeurAbsenteDonneUneChaineVide() {
        assertThat(nombres.entier(null)).isEmpty();
        assertThat(nombres.decimal(null, 1)).isEmpty();
    }
}
