import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Génère les images bitmap de la marque à partir des SVG de static/img/logo (source unique).
 *
 * <p>Icônes PWA, favicon PNG, apple-touch-icon, image Open Graph et logo des PDF (OpenPDF ne lit pas le SVG).
 * Le rendu SVG est volontairement minimal : il couvre ce qu'utilisent nos fichiers (svg viewBox, g translate,
 * path M/L/H/V/A/C/Z absolus, circle, rect, fill, stroke, stroke-width, stroke-linecap, stroke-linejoin).
 *
 * <p>Usage, depuis la racine du dépôt :
 * {@code java -Djava.awt.headless=true scripts/icones/GenererIcones.java}
 */
public class GenererIcones {

    private static final File LOGOS = new File("src/main/resources/static/img/logo");
    private static final File IMG = new File("src/main/resources/static/img");
    private static final File PDF = new File("src/main/resources/pdf");

    public static void main(String[] args) throws Exception {
        Element symbole = lire(new File(LOGOS, "seheli-symbole.svg"));
        Element logo = lire(new File(LOGOS, "seheli-logo.svg"));
        Element logoArabe = lire(new File(LOGOS, "seheli-logo-arabe.svg"));

        // « any » : carré blanc arrondi, symbole à 76 % ; lisible sur un lanceur clair comme sombre.
        ecrire(icone(symbole, 192, false, 0.76), new File(IMG, "icone-192.png"));
        ecrire(icone(symbole, 512, false, 0.76), new File(IMG, "icone-512.png"));
        // « maskable » et Apple : fond plein, symbole dans la zone sûre (cercle de 80 %).
        ecrire(icone(symbole, 192, true, 0.62), new File(IMG, "icone-maskable-192.png"));
        ecrire(icone(symbole, 512, true, 0.62), new File(IMG, "icone-maskable-512.png"));
        ecrire(icone(symbole, 180, true, 0.70), new File(IMG, "apple-touch-icon.png"));
        // Favicon : symbole seul sur fond transparent.
        ecrire(icone(symbole, 32, false, 1.0, false), new File(IMG, "favicon-32.png"));
        ecrire(icone(symbole, 16, false, 1.0, false), new File(IMG, "favicon-16.png"));

        ecrire(openGraph(logo, logoArabe), new File(IMG, "og-seheli.png"));
        // Logo des PDF : 6 fois la taille nominale (1248 × 384 px), assez net à l'impression.
        PDF.mkdirs();
        ecrire(rendre(logo, 6.0), new File(PDF, "seheli-logo.png"));
    }

    // ---------- Compositions ----------

    private static BufferedImage icone(Element symbole, int taille, boolean pleineSurface, double part) {
        return icone(symbole, taille, pleineSurface, part, true);
    }

    private static BufferedImage icone(Element symbole, int taille, boolean pleineSurface, double part, boolean fond) {
        BufferedImage image = new BufferedImage(taille, taille, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphique(image);
        if (fond) {
            g.setColor(Color.WHITE);
            if (pleineSurface) {
                g.fillRect(0, 0, taille, taille);
            } else {
                double rayon = taille * 0.44;
                g.fill(new RoundRectangle2D.Double(0, 0, taille, taille, rayon, rayon));
            }
        }
        double[] vb = viewBox(symbole);
        double echelle = taille * part / vb[3];
        g.translate((taille - vb[2] * echelle) / 2, (taille - vb[3] * echelle) / 2);
        g.scale(echelle, echelle);
        dessiner(g, symbole, new Style());
        g.dispose();
        return image;
    }

    /** 1200 × 630 : logo latin au-dessus, logo arabe en dessous, sur fond blanc, liseré en bas. */
    private static BufferedImage openGraph(Element logo, Element logoArabe) {
        int l = 1200;
        int h = 630;
        BufferedImage image = new BufferedImage(l, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphique(image);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, l, h);
        placer(g, logo, l, 100, 3.0);
        placer(g, logoArabe, l, 330, 2.0);
        // Liseré aux couleurs du symbole : 40 % or (l'apport), 60 % vert (les mensualités).
        g.setColor(new Color(0xf2, 0xb7, 0x05));
        g.fillRect(0, h - 20, l * 2 / 5, 20);
        g.setColor(new Color(0x0b, 0x6e, 0x4f));
        g.fillRect(l * 2 / 5, h - 20, l - l * 2 / 5, 20);
        g.dispose();
        return image;
    }

    private static void placer(Graphics2D g, Element svg, int largeur, double y, double echelle) {
        double[] vb = viewBox(svg);
        AffineTransform avant = g.getTransform();
        g.translate((largeur - vb[2] * echelle) / 2, y);
        g.scale(echelle, echelle);
        dessiner(g, svg, new Style());
        g.setTransform(avant);
    }

    private static BufferedImage rendre(Element svg, double echelle) {
        double[] vb = viewBox(svg);
        BufferedImage image = new BufferedImage((int) Math.ceil(vb[2] * echelle), (int) Math.ceil(vb[3] * echelle),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphique(image);
        g.scale(echelle, echelle);
        dessiner(g, svg, new Style());
        g.dispose();
        return image;
    }

    private static Graphics2D graphique(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    // ---------- Rendu SVG minimal ----------

    private record Style(String fill, String stroke, float largeur, int bout, int jointure) {
        Style() {
            this("#000000", "none", 1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        }

        Style deriver(Element e) {
            String f = e.hasAttribute("fill") ? e.getAttribute("fill") : fill;
            String s = e.hasAttribute("stroke") ? e.getAttribute("stroke") : stroke;
            float w = e.hasAttribute("stroke-width") ? Float.parseFloat(e.getAttribute("stroke-width")) : largeur;
            int cap = switch (e.getAttribute("stroke-linecap")) {
                case "round" -> BasicStroke.CAP_ROUND;
                case "square" -> BasicStroke.CAP_SQUARE;
                case "butt" -> BasicStroke.CAP_BUTT;
                default -> bout;
            };
            int join = switch (e.getAttribute("stroke-linejoin")) {
                case "round" -> BasicStroke.JOIN_ROUND;
                case "bevel" -> BasicStroke.JOIN_BEVEL;
                case "miter" -> BasicStroke.JOIN_MITER;
                default -> jointure;
            };
            return new Style(f, s, w, cap, join);
        }
    }

    private static Element lire(File fichier) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return f.newDocumentBuilder().parse(fichier).getDocumentElement();
    }

    private static double[] viewBox(Element svg) {
        String[] v = svg.getAttribute("viewBox").trim().split("[\\s,]+");
        return new double[] {Double.parseDouble(v[0]), Double.parseDouble(v[1]), Double.parseDouble(v[2]),
                Double.parseDouble(v[3])};
    }

    private static void dessiner(Graphics2D g, Element e, Style parent) {
        Style style = parent.deriver(e);
        AffineTransform avant = g.getTransform();
        Matcher t = Pattern.compile("translate\\(\\s*([-\\d.]+)[\\s,]+([-\\d.]+)\\s*\\)").matcher(e.getAttribute("transform"));
        if (t.find()) {
            g.translate(Double.parseDouble(t.group(1)), Double.parseDouble(t.group(2)));
        }
        Shape forme = switch (e.getTagName()) {
            case "path" -> chemin(e.getAttribute("d"));
            case "circle" -> {
                double r = nombre(e, "r");
                yield new Ellipse2D.Double(nombre(e, "cx") - r, nombre(e, "cy") - r, 2 * r, 2 * r);
            }
            case "rect" -> new RoundRectangle2D.Double(nombre(e, "x"), nombre(e, "y"), nombre(e, "width"),
                    nombre(e, "height"), 2 * nombre(e, "rx"), 2 * nombre(e, "rx"));
            default -> null;
        };
        if (forme != null) {
            if (!"none".equals(style.fill())) {
                g.setColor(Color.decode(style.fill()));
                g.fill(forme);
            }
            if (!"none".equals(style.stroke())) {
                g.setColor(Color.decode(style.stroke()));
                g.setStroke(new BasicStroke(style.largeur(), style.bout(), style.jointure()));
                g.draw(forme);
            }
        }
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element enfant && !"title".equals(enfant.getTagName())) {
                dessiner(g, enfant, style);
            }
        }
        g.setTransform(avant);
    }

    private static double nombre(Element e, String attribut) {
        String v = e.getAttribute(attribut);
        return v.isEmpty() ? 0 : Double.parseDouble(v);
    }

    private static Path2D chemin(String d) {
        Matcher m = Pattern.compile("[MLHVACZmlhvacz]|-?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?").matcher(d);
        List<String> jetons = new ArrayList<>();
        while (m.find()) {
            jetons.add(m.group());
        }
        Path2D.Double p = new Path2D.Double();
        char commande = 'M';
        int i = 0;
        while (i < jetons.size()) {
            String j = jetons.get(i);
            if (Character.isLetter(j.charAt(0))) {
                commande = j.charAt(0);
                i++;
                if (commande == 'Z' || commande == 'z') {
                    p.closePath();
                    continue;
                }
            }
            if (Character.isLowerCase(commande)) {
                throw new IllegalArgumentException("Commande relative non gérée : " + commande);
            }
            Point2D c = p.getCurrentPoint();
            switch (commande) {
                case 'M' -> {
                    p.moveTo(d(jetons, i), d(jetons, i + 1));
                    i += 2;
                    commande = 'L';
                }
                case 'L' -> {
                    p.lineTo(d(jetons, i), d(jetons, i + 1));
                    i += 2;
                }
                case 'H' -> p.lineTo(d(jetons, i++), c.getY());
                case 'V' -> p.lineTo(c.getX(), d(jetons, i++));
                case 'C' -> {
                    p.curveTo(d(jetons, i), d(jetons, i + 1), d(jetons, i + 2), d(jetons, i + 3), d(jetons, i + 4),
                            d(jetons, i + 5));
                    i += 6;
                }
                case 'A' -> {
                    arc(p, c.getX(), c.getY(), d(jetons, i), d(jetons, i + 1), d(jetons, i + 2),
                            d(jetons, i + 3) != 0, d(jetons, i + 4) != 0, d(jetons, i + 5), d(jetons, i + 6));
                    i += 7;
                }
                default -> throw new IllegalArgumentException("Commande non gérée : " + commande);
            }
        }
        return p;
    }

    private static double d(List<String> jetons, int i) {
        return Double.parseDouble(jetons.get(i));
    }

    /** Arc SVG (paramétrage par les extrémités, SVG 1.1 annexe F.6) converti en Arc2D. */
    private static void arc(Path2D p, double x1, double y1, double rx, double ry, double rotation, boolean grand,
            boolean horaire, double x2, double y2) {
        if (rx == 0 || ry == 0) {
            p.lineTo(x2, y2);
            return;
        }
        rx = Math.abs(rx);
        ry = Math.abs(ry);
        double phi = Math.toRadians(rotation);
        double cos = Math.cos(phi);
        double sin = Math.sin(phi);
        double dx = (x1 - x2) / 2;
        double dy = (y1 - y2) / 2;
        double xp = cos * dx + sin * dy;
        double yp = -sin * dx + cos * dy;
        double lambda = xp * xp / (rx * rx) + yp * yp / (ry * ry);
        if (lambda > 1) {
            rx *= Math.sqrt(lambda);
            ry *= Math.sqrt(lambda);
        }
        double num = rx * rx * ry * ry - rx * rx * yp * yp - ry * ry * xp * xp;
        double den = rx * rx * yp * yp + ry * ry * xp * xp;
        double coef = Math.sqrt(Math.max(0, num / den)) * (grand == horaire ? -1 : 1);
        double cxp = coef * rx * yp / ry;
        double cyp = -coef * ry * xp / rx;
        double cx = cos * cxp - sin * cyp + (x1 + x2) / 2;
        double cy = sin * cxp + cos * cyp + (y1 + y2) / 2;
        double theta1 = angle(1, 0, (xp - cxp) / rx, (yp - cyp) / ry);
        double delta = angle((xp - cxp) / rx, (yp - cyp) / ry, (-xp - cxp) / rx, (-yp - cyp) / ry);
        if (!horaire && delta > 0) {
            delta -= 2 * Math.PI;
        } else if (horaire && delta < 0) {
            delta += 2 * Math.PI;
        }
        // Arc2D : angles paramétriques, sens trigonométrique à l'écran (y vers le bas) → signes inversés.
        Arc2D.Double a = new Arc2D.Double(cx - rx, cy - ry, 2 * rx, 2 * ry, -Math.toDegrees(theta1),
                -Math.toDegrees(delta), Arc2D.OPEN);
        Shape s = AffineTransform.getRotateInstance(phi, cx, cy).createTransformedShape(a);
        p.append(s, true);
    }

    private static double angle(double ux, double uy, double vx, double vy) {
        return Math.atan2(ux * vy - uy * vx, ux * vx + uy * vy);
    }

    private static void ecrire(BufferedImage image, File fichier) throws Exception {
        ImageIO.write(image, "png", fichier);
        System.out.println(fichier + " (" + image.getWidth() + "×" + image.getHeight() + " px)");
    }
}
