package com.seheli.contrat.adapter.out.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.openpdf.text.Element;
import org.openpdf.text.Image;

/**
 * Logo Seheli en tête des PDF. OpenPDF ne lit pas le SVG : on utilise {@code pdf/seheli-logo.png}, rendu
 * depuis {@code static/img/logo/seheli-logo.svg} par {@code scripts/icones/GenererIcones.java}
 * (1248 × 384 px, net à l'impression). Les PDF sont en français : version latine du logo.
 */
final class LogoPdf {

    private static final String RESSOURCE = "/pdf/seheli-logo.png";
    private static final byte[] OCTETS = lire();

    private LogoPdf() {
    }

    /** Logo de {@code hauteur} points (8 mm minimum à l'impression, soit environ 23 pt), aligné à gauche. */
    static Image image(float hauteur) {
        try {
            Image logo = Image.getInstance(OCTETS);
            logo.scaleToFit(hauteur * logo.getWidth() / logo.getHeight(), hauteur);
            logo.setAlignment(Element.ALIGN_LEFT);
            logo.setAlt("Seheli");
            return logo;
        } catch (IOException e) {
            throw new UncheckedIOException("Logo PDF illisible", e);
        }
    }

    private static byte[] lire() {
        try (InputStream in = LogoPdf.class.getResourceAsStream(RESSOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Logo PDF introuvable : " + RESSOURCE);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Logo PDF illisible", e);
        }
    }
}
