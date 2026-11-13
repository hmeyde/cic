import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Génère les icônes PNG de la PWA à partir du même dessin que static/img/icone.svg.
 * Usage : java -Djava.awt.headless=true scripts/icones/GenererIcones.java src/main/resources/static/img
 */
public class GenererIcones {

    private static final Color VERT = new Color(0x0b, 0x6e, 0x4f);

    public static void main(String[] args) throws Exception {
        File dossier = new File(args.length > 0 ? args[0] : "src/main/resources/static/img");
        dossier.mkdirs();
        ecrire(dessiner(192, false), new File(dossier, "icone-192.png"));
        ecrire(dessiner(512, false), new File(dossier, "icone-512.png"));
        ecrire(dessiner(192, true), new File(dossier, "icone-maskable-192.png"));
        ecrire(dessiner(512, true), new File(dossier, "icone-maskable-512.png"));
        ecrire(dessiner(180, true), new File(dossier, "apple-touch-icon.png"));
        ecrire(dessiner(32, false), new File(dossier, "favicon-32.png"));
    }

    /** pleineSurface : fond carré sans transparence et texte dans la zone sûre (80 %) pour les icônes « maskable ». */
    private static BufferedImage dessiner(int taille, boolean pleineSurface) {
        BufferedImage image = new BufferedImage(taille, taille, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(VERT);
        if (pleineSurface) {
            g.fillRect(0, 0, taille, taille);
        } else {
            double rayon = taille * 96.0 / 512 * 2;
            g.fill(new RoundRectangle2D.Double(0, 0, taille, taille, rayon, rayon));
        }
        float corps = (float) (taille * (pleineSurface ? 150.0 : 180.0) / 512);
        g.setFont(new Font("DejaVu Sans", Font.BOLD, 1).deriveFont(corps));
        FontMetrics m = g.getFontMetrics();
        String texte = "CIC";
        int x = (taille - m.stringWidth(texte)) / 2;
        int y = (taille - m.getAscent() - m.getDescent()) / 2 + m.getAscent();
        g.setColor(Color.WHITE);
        g.drawString(texte, x, y);
        g.dispose();
        return image;
    }

    private static void ecrire(BufferedImage image, File fichier) throws Exception {
        ImageIO.write(image, "png", fichier);
        System.out.println(fichier + " (" + image.getWidth() + "px)");
    }
}
