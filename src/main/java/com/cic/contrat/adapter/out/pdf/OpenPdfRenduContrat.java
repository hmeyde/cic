package com.cic.contrat.adapter.out.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.HeaderFooter;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import com.cic.commun.domain.Montant;
import com.cic.contrat.domain.Acheteur;
import com.cic.contrat.domain.ContenuContrat;
import com.cic.contrat.domain.Contrat;
import com.cic.contrat.domain.Echeance;
import com.cic.contrat.domain.RenduContratPdf;

/**
 * Contrat de vente à tempérament mis en page avec OpenPDF (A4, Helvetica, en français).
 *
 * <p>Le client ne voit que ce qui le concerne : prix de vente total, apport, reste à payer et
 * échéancier. Ni le prix d'achat, ni la remise fournisseur, ni le capital engagé ne figurent sur le
 * contrat. Les montants sont écrits en ouguiyas entières, les milliers séparés par une espace
 * insécable.
 */
@Component
class OpenPdfRenduContrat implements RenduContratPdf {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final char ESPACE_INSECABLE = ' ';
    private static final Color GRIS_CLAIR = new Color(0xEE, 0xEE, 0xEE);

    private static final Font TITRE = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLD);
    private static final Font SOUS_TITRE = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
    private static final Font ARTICLE = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.BOLD);
    private static final Font TEXTE = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);
    private static final Font GRAS = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD);
    private static final Font PETIT = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.GRAY);

    @Override
    public byte[] rendre(ContenuContrat contenu) {
        Contrat c = contenu.contrat();
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            PdfWriter.getInstance(doc, sortie);
            doc.addTitle("Contrat de vente à tempérament " + c.numero());
            doc.addAuthor("CIC");
            doc.addCreator("CIC");
            HeaderFooter pied = new HeaderFooter(new Phrase("Contrat n° " + c.numero() + " — page ", PETIT), true);
            pied.setAlignment(Element.ALIGN_CENTER);
            pied.setBorder(Rectangle.NO_BORDER);
            doc.setFooter(pied);
            doc.open();

            entete(doc, contenu);
            parties(doc, contenu.acheteur());
            article(doc, "Article 1 — Objet",
                    "CIC vend à l'acheteur, qui l'accepte, le bien suivant : ", c.designationProduit(),
                    ". CIC achète ce bien auprès de son fournisseur, puis le revend à l'acheteur au prix fixé "
                            + "à l'article 2, payable en partie à la commande et le reste à tempérament.");
            prix(doc, c);
            article(doc, "Article 3 — Apport et autorisation de prélèvement",
                    "Au plus tard le ", date(contenu.dateLimiteApport()),
                    ", l'acheteur paie l'apport de " + montant(c.apport())
                            + " par Masrvi, avec la référence de paiement indiquée dans son espace client, "
                            + "et signe l'autorisation de prélèvement des mensualités. Si l'apport n'est pas "
                            + "encaissé ou si l'autorisation n'est pas acceptée à cette date, la vente est annulée "
                            + "de plein droit, sans frais pour l'acheteur.");
            article(doc, "Article 4 — Commande et remise du bien",
                    "CIC commande le bien au fournisseur dès que l'apport est encaissé et que l'autorisation "
                            + "de prélèvement est acceptée. Le bien est remis à l'acheteur, qui en confirme la "
                            + "réception. La propriété du bien passe à l'acheteur à sa remise.", "", "");
            echeancier(doc, c);
            article(doc, "Article 6 — Retard de paiement",
                    "Aucun intérêt ni aucune pénalité de retard ne sont dus : le prix ne change jamais après "
                            + "la signature. En cas d'échéance impayée, CIC prévient l'acheteur et présente de "
                            + "nouveau le prélèvement. Tant qu'un retard est en cours, l'acheteur ne peut pas faire "
                            + "de nouvel achat à crédit auprès de CIC. Un retard persistant ouvre une procédure de "
                            + "recouvrement amiable.", "", "");
            article(doc, "Article 7 — Données personnelles",
                    "L'acheteur a consenti à l'usage de ses données personnelles par CIC pour l'étude de sa "
                            + "demande, la gestion de ce contrat et le recouvrement des sommes dues. Ses "
                            + "justificatifs sont conservés de façon confidentielle.", "", "");
            article(doc, "Article 8 — Droit applicable",
                    "Le présent contrat est soumis au droit mauritanien.", "", "");
            signatures(doc, contenu.etabliLe());
            doc.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Mise en page du contrat " + c.numero() + " impossible", e);
        }
        return sortie.toByteArray();
    }

    private static void entete(Document doc, ContenuContrat contenu) {
        Paragraph titre = new Paragraph("CONTRAT DE VENTE À TEMPÉRAMENT", TITRE);
        titre.setAlignment(Element.ALIGN_CENTER);
        doc.add(titre);
        Paragraph sous = new Paragraph("Vente à crédit à marge fixe (mourabaha), sans intérêt ni pénalité", SOUS_TITRE);
        sous.setAlignment(Element.ALIGN_CENTER);
        doc.add(sous);
        Paragraph ref = new Paragraph("Contrat n° " + contenu.contrat().numero() + " du "
                + date(contenu.etabliLe()), GRAS);
        ref.setAlignment(Element.ALIGN_CENTER);
        ref.setSpacingBefore(6);
        ref.setSpacingAfter(14);
        doc.add(ref);
    }

    private static void parties(Document doc, Acheteur a) {
        doc.add(new Paragraph("Entre les soussignés :", TEXTE));
        Paragraph vendeur = new Paragraph();
        vendeur.setIndentationLeft(15);
        vendeur.setSpacingBefore(4);
        vendeur.add(new Chunk("Le vendeur : ", GRAS));
        vendeur.add(new Chunk("CIC, vente à crédit aux salariés, Nouakchott (Mauritanie), ci-après « CIC » ;", TEXTE));
        doc.add(vendeur);
        Paragraph acheteur = new Paragraph();
        acheteur.setIndentationLeft(15);
        acheteur.setSpacingBefore(4);
        acheteur.setSpacingAfter(6);
        acheteur.add(new Chunk("L'acheteur : ", GRAS));
        acheteur.add(new Chunk(a.prenom() + " " + a.nom() + ", né(e) le " + date(a.dateNaissance()) + ", NNI "
                + a.nni() + ", téléphone " + a.telephone() + ", ci-après « l'acheteur ».", TEXTE));
        doc.add(acheteur);
        doc.add(new Paragraph("Il a été convenu ce qui suit.", TEXTE));
    }

    private static void prix(Document doc, Contrat c) {
        titreArticle(doc, "Article 2 — Prix de vente");
        PdfPTable t = tableau(new float[] {3, 1});
        ligne(t, "Prix de vente total", montant(c.prix().prixVente()), true);
        ligne(t, "Apport payé à la commande", montant(c.apport()), false);
        ligne(t, "Reste à payer en " + c.nombreMois() + " mensualités", montant(c.resteAFinancer()), false);
        doc.add(t);
        paragraphe(doc, "Le prix de vente comprend la marge de CIC, fixée à la signature. Il est ferme et "
                + "définitif : il ne porte aucun intérêt et ne sera majoré d'aucune pénalité.");
    }

    private static void echeancier(Document doc, Contrat c) {
        titreArticle(doc, "Article 5 — Échéancier");
        paragraphe(doc, "Le reste à payer est réglé en " + c.nombreMois() + " mensualités, prélevées le jour du "
                + "versement du salaire de l'acheteur selon l'autorisation de prélèvement :");
        PdfPTable t = tableau(new float[] {1, 2, 2});
        t.setHeaderRows(1);
        entete(t, "N°", Element.ALIGN_LEFT);
        entete(t, "Date de prélèvement", Element.ALIGN_LEFT);
        entete(t, "Montant", Element.ALIGN_RIGHT);
        for (Echeance e : c.echeances()) {
            t.addCell(cellule(String.valueOf(e.rang()), TEXTE, Element.ALIGN_LEFT));
            t.addCell(cellule(date(e.date()), TEXTE, Element.ALIGN_LEFT));
            t.addCell(cellule(montant(e.montant()), TEXTE, Element.ALIGN_RIGHT));
        }
        PdfPCell total = cellule("Total des mensualités", GRAS, Element.ALIGN_LEFT);
        total.setColspan(2);
        t.addCell(total);
        t.addCell(cellule(montant(c.resteAFinancer()), GRAS, Element.ALIGN_RIGHT));
        doc.add(t);
        if (c.echeances().size() > 1
                && !c.echeances().getLast().montant().equals(c.echeances().getFirst().montant())) {
            paragraphe(doc, "Les montants sont arrondis à l'ouguiya ; la dernière mensualité absorbe l'écart.");
        }
    }

    private static void signatures(Document doc, LocalDate le) {
        Paragraph fait = new Paragraph("Fait à Nouakchott, le " + date(le) + ", en deux exemplaires originaux.", TEXTE);
        fait.setSpacingBefore(14);
        fait.setSpacingAfter(10);
        doc.add(fait);
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.addCell(signature("Pour CIC"));
        t.addCell(signature("L'acheteur, précédé de la mention « Lu et approuvé »"));
        doc.add(t);
    }

    private static PdfPCell signature(String libelle) {
        PdfPCell cell = new PdfPCell(new Phrase(libelle, GRAS));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(70);
        return cell;
    }

    private static void article(Document doc, String titre, String avant, String enGras, String apres) {
        titreArticle(doc, titre);
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        p.add(new Chunk(avant, TEXTE));
        if (!enGras.isEmpty()) {
            p.add(new Chunk(enGras, GRAS));
        }
        p.add(new Chunk(apres, TEXTE));
        doc.add(p);
    }

    private static void titreArticle(Document doc, String titre) {
        Paragraph p = new Paragraph(titre, ARTICLE);
        p.setSpacingBefore(10);
        p.setSpacingAfter(4);
        doc.add(p);
    }

    private static void paragraphe(Document doc, String texte) {
        Paragraph p = new Paragraph(texte, TEXTE);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        p.setSpacingBefore(4);
        doc.add(p);
    }

    private static PdfPTable tableau(float[] largeurs) {
        PdfPTable t = new PdfPTable(largeurs);
        t.setWidthPercentage(100);
        t.setSpacingBefore(4);
        t.setSpacingAfter(4);
        t.setKeepTogether(true);
        return t;
    }

    private static void ligne(PdfPTable t, String libelle, String valeur, boolean principale) {
        Font f = principale ? GRAS : TEXTE;
        t.addCell(cellule(libelle, f, Element.ALIGN_LEFT));
        t.addCell(cellule(valeur, f, Element.ALIGN_RIGHT));
    }

    private static void entete(PdfPTable t, String libelle, int alignement) {
        PdfPCell cell = cellule(libelle, GRAS, alignement);
        cell.setBackgroundColor(GRIS_CLAIR);
        t.addCell(cell);
    }

    private static PdfPCell cellule(String texte, Font font, int alignement) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setHorizontalAlignment(alignement);
        cell.setPadding(5);
        cell.setBorderColor(Color.LIGHT_GRAY);
        return cell;
    }

    private static String date(LocalDate d) {
        return d.format(DATE);
    }

    /** {@code 20800} → « 20 800 MRU » (espace insécable). */
    static String montant(Montant m) {
        String chiffres = m.valeur().toBigIntegerExact().toString();
        StringBuilder sb = new StringBuilder();
        int premier = chiffres.length() % 3 == 0 ? 3 : chiffres.length() % 3;
        sb.append(chiffres, 0, premier);
        for (int i = premier; i < chiffres.length(); i += 3) {
            sb.append(ESPACE_INSECABLE).append(chiffres, i, i + 3);
        }
        return sb.append(ESPACE_INSECABLE).append(Montant.DEVISE).toString();
    }
}
