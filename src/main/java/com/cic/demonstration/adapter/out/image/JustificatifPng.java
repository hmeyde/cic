package com.cic.demonstration.adapter.out.image;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.imageio.ImageIO;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.cic.demonstration.domain.GenerateurJustificatif;

/** Justificatif fictif dessiné en PNG (Java2D, sans écran), barré de la mention « document fictif ». */
@Component
@Profile("demo")
class JustificatifPng implements GenerateurJustificatif {

    private static final int LARGEUR = 640;
    private static final int HAUTEUR = 400;
    private static final Color VERT = new Color(0x0B, 0x6E, 0x4F);

    @Override
    public byte[] png(String titre, String... lignes) {
        BufferedImage image = new BufferedImage(LARGEUR, HAUTEUR, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0xF7, 0xF4, 0xEC));
            g.fillRect(0, 0, LARGEUR, HAUTEUR);
            g.setColor(VERT);
            g.setStroke(new BasicStroke(4));
            g.drawRect(10, 10, LARGEUR - 20, HAUTEUR - 20);
            g.fillRect(10, 10, LARGEUR - 20, 56);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
            g.drawString(titre, 30, 47);
            g.setColor(Color.DARK_GRAY);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
            int y = 110;
            for (String ligne : lignes) {
                g.drawString(ligne, 30, y);
                y += 34;
            }
            g.setColor(new Color(0xC0, 0x39, 0x2B));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            g.drawString("DOCUMENT FICTIF — DÉMONSTRATION CIC", 30, HAUTEUR - 34);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", sortie);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sortie.toByteArray();
    }
}
