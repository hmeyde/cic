package com.cic.demande.adapter.out.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import com.cic.commun.domain.Montant;
import com.cic.demande.domain.CommandeFournisseur;
import com.cic.demande.domain.ContenuBonCommande;
import com.cic.demande.domain.RenduBonCommandePdf;

/**
 * Bon de commande fournisseur mis en page avec OpenPDF (A4, Helvetica, en français) : fournisseur,
 * produit, prix public, remise négociée et net à payer. Aucune donnée personnelle du client : le
 * contrat n'est cité que par son numéro.
 */
@Component
class OpenPdfRenduBonCommande implements RenduBonCommandePdf {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final char ESPACE_INSECABLE = ' ';
    private static final Color GRIS_CLAIR = new Color(0xEE, 0xEE, 0xEE);

    private static final Font TITRE = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLD);
    private static final Font TEXTE = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);
    private static final Font GRAS = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD);
    private static final Font PETIT = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.GRAY);

    @Override
    public byte[] rendre(ContenuBonCommande contenu) {
        CommandeFournisseur c = contenu.commande();
        String acheteur = contenu.raisonSociale();
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            PdfWriter.getInstance(doc, sortie);
            doc.addTitle("Bon de commande " + c.numero());
            doc.addAuthor(acheteur);
            doc.addCreator(acheteur);
            doc.open();

            Paragraph titre = new Paragraph("BON DE COMMANDE", TITRE);
            titre.setAlignment(Element.ALIGN_CENTER);
            doc.add(titre);
            Paragraph ref = new Paragraph("N° " + c.numero() + " du " + contenu.le().format(DATE), GRAS);
            ref.setAlignment(Element.ALIGN_CENTER);
            ref.setSpacingAfter(16);
            doc.add(ref);

            PdfPTable parties = new PdfPTable(2);
            parties.setWidthPercentage(100);
            parties.addCell(bloc("Acheteur", acheteur + ", vente à crédit aux salariés\nNouakchott (Mauritanie)"));
            parties.addCell(bloc("Fournisseur", contenu.nomFournisseur()));
            doc.add(parties);

            Paragraph objet = new Paragraph("Référence " + acheteur + " : contrat n° " + c.numeroContrat(), TEXTE);
            objet.setSpacingBefore(12);
            objet.setSpacingAfter(6);
            doc.add(objet);

            PdfPTable t = new PdfPTable(new float[] {4, 1, 2});
            t.setWidthPercentage(100);
            t.addCell(entete("Désignation", Element.ALIGN_LEFT));
            t.addCell(entete("Quantité", Element.ALIGN_RIGHT));
            t.addCell(entete("Montant", Element.ALIGN_RIGHT));
            t.addCell(cellule(c.designationProduit(), TEXTE, Element.ALIGN_LEFT));
            t.addCell(cellule("1", TEXTE, Element.ALIGN_RIGHT));
            t.addCell(cellule(montant(contenu.prixPublic()), TEXTE, Element.ALIGN_RIGHT));
            doc.add(t);
            doc.add(totaux(contenu));

            Paragraph conditions = new Paragraph("Le paiement de ce bon de commande a été validé le "
                    + contenu.le().format(DATE) + " par " + c.validePar() + ". Le produit est remis à " + acheteur + ", qui le "
                    + "remet à son client contre confirmation de réception.", TEXTE);
            conditions.setAlignment(Element.ALIGN_JUSTIFIED);
            conditions.setSpacingBefore(14);
            doc.add(conditions);

            PdfPTable signatures = new PdfPTable(2);
            signatures.setWidthPercentage(100);
            signatures.setSpacingBefore(20);
            signatures.addCell(signature("Pour " + acheteur));
            signatures.addCell(signature("Le fournisseur (cachet et signature)"));
            doc.add(signatures);

            Paragraph pied = new Paragraph("Bon de commande n° " + c.numero(), PETIT);
            pied.setAlignment(Element.ALIGN_CENTER);
            pied.setSpacingBefore(20);
            doc.add(pied);
            doc.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Mise en page du bon de commande " + c.numero() + " impossible", e);
        }
        return sortie.toByteArray();
    }

    private static PdfPTable totaux(ContenuBonCommande contenu) {
        PdfPTable t = new PdfPTable(new float[] {5, 2});
        t.setWidthPercentage(100);
        t.addCell(cellule("Prix public", TEXTE, Element.ALIGN_RIGHT));
        t.addCell(cellule(montant(contenu.prixPublic()), TEXTE, Element.ALIGN_RIGHT));
        t.addCell(cellule("Remise fournisseur", TEXTE, Element.ALIGN_RIGHT));
        t.addCell(cellule("− " + montant(contenu.remise()), TEXTE, Element.ALIGN_RIGHT));
        t.addCell(cellule("Net à payer au fournisseur", GRAS, Element.ALIGN_RIGHT));
        t.addCell(cellule(montant(contenu.commande().montant()), GRAS, Element.ALIGN_RIGHT));
        return t;
    }


    private static PdfPCell bloc(String titre, String texte) {
        Phrase p = new Phrase();
        p.add(new Phrase(titre + "\n", GRAS));
        p.add(new Phrase(texte, TEXTE));
        PdfPCell cell = new PdfPCell(p);
        cell.setPadding(8);
        cell.setBorderColor(Color.LIGHT_GRAY);
        return cell;
    }

    private static PdfPCell signature(String libelle) {
        PdfPCell cell = new PdfPCell(new Phrase(libelle, GRAS));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(70);
        return cell;
    }

    private static PdfPCell entete(String libelle, int alignement) {
        PdfPCell cell = cellule(libelle, GRAS, alignement);
        cell.setBackgroundColor(GRIS_CLAIR);
        return cell;
    }

    private static PdfPCell cellule(String texte, Font font, int alignement) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setHorizontalAlignment(alignement);
        cell.setPadding(5);
        cell.setBorderColor(Color.LIGHT_GRAY);
        return cell;
    }

    /** {@code 18400} → « 18 400 MRU » (espace insécable). */
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
