package com.seheli.contrat.adapter.out.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

import com.seheli.contrat.domain.Acheteur;
import com.seheli.contrat.domain.CompteBeneficiaire;
import com.seheli.contrat.domain.ContenuAutorisation;
import com.seheli.contrat.domain.Contrat;
import com.seheli.contrat.domain.Echeance;
import com.seheli.contrat.domain.RenduAutorisationPdf;
import com.seheli.contrat.domain.TypeAutorisation;

/**
 * Autorisation de prélèvement mise en page avec OpenPDF (A4, Helvetica, en français). Pour le MVP,
 * seul l'ordre de virement permanent est mis en page : le client le signe et le remet à sa banque,
 * qui vire chaque mensualité de son compte vers celui du vendeur aux dates de l'échéancier du contrat.
 *
 * <p>Tout ce que le vendeur connaît est rempli : titulaire, compte du vendeur, montants, dates, libellé des
 * virements. Ce que seul le client ou sa banque connaît (banque si elle n'est pas connue, numéro de
 * compte, lieu, date, signature, visa de la banque) est laissé en pointillés.
 */
@Component
class OpenPdfRenduAutorisation implements RenduAutorisationPdf {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String A_COMPLETER = "........................................................";
    private static final Color GRIS_CLAIR = new Color(0xEE, 0xEE, 0xEE);

    private static final Font TITRE = FontFactory.getFont(FontFactory.HELVETICA, 15, Font.BOLD);
    private static final Font SOUS_TITRE = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
    private static final Font SECTION = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.BOLD);
    private static final Font TEXTE = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);
    private static final Font GRAS = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD);
    private static final Font PETIT = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, Color.GRAY);

    @Override
    public byte[] rendre(ContenuAutorisation contenu) {
        if (contenu.autorisation().type() != TypeAutorisation.VIREMENT_PERMANENT) {
            throw new IllegalArgumentException("Mise en page indisponible pour " + contenu.autorisation().type());
        }
        Contrat c = contenu.contrat();
        String vendeur = contenu.raisonSociale();
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 45, 45, 30, 30);
        try {
            PdfWriter.getInstance(doc, sortie);
            doc.addTitle("Ordre de virement permanent " + c.numero());
            doc.addAuthor(vendeur);
            doc.addCreator(vendeur);
            HeaderFooter pied = new HeaderFooter(new Phrase("Autorisation de prélèvement — contrat n° " + c.numero()
                    + " — page ", PETIT), true);
            pied.setAlignment(Element.ALIGN_CENTER);
            pied.setBorder(Rectangle.NO_BORDER);
            doc.setFooter(pied);
            doc.open();

            entete(doc, contenu);
            donneurOrdre(doc, contenu.titulaire(), contenu);
            beneficiaire(doc, contenu.beneficiaire());
            ordre(doc, contenu);
            section(doc, "4. Durée et révocation");
            paragraphe(doc, "Cet ordre prend fin de lui-même après le dernier virement. Il garantit le paiement du "
                    + "prix fixé au contrat n° " + c.numero() + " : il ne peut être révoqué ni modifié avant le "
                    + "paiement complet qu'avec l'accord écrit de " + vendeur + ". Aucun intérêt ni aucune pénalité ne "
                    + "s'ajoutent aux montants ci-dessus. Si un virement ne peut être exécuté faute de provision, "
                    + "la banque en avise le titulaire ; " + vendeur + " le contacte pour le régulariser.");
            section(doc, "5. À faire par le titulaire");
            paragraphe(doc, "Signer cet ordre, le remettre à sa banque et transmettre à " + vendeur + " la copie visée par la "
                    + "banque au plus tard le " + date(contenu.dateLimite()) + ". Passé cette date, la vente est "
                    + "annulée sans frais.");
            signatures(doc);
            doc.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Mise en page de l'autorisation du contrat " + c.numero() + " impossible",
                    e);
        }
        return sortie.toByteArray();
    }

    private static void entete(Document doc, ContenuAutorisation contenu) {
        doc.add(LogoPdf.image(26));
        Paragraph titre = new Paragraph("AUTORISATION DE PRÉLÈVEMENT", TITRE);
        titre.setAlignment(Element.ALIGN_CENTER);
        doc.add(titre);
        Paragraph sous = new Paragraph("Ordre de virement permanent — à signer et à remettre à votre banque",
                SOUS_TITRE);
        sous.setAlignment(Element.ALIGN_CENTER);
        doc.add(sous);
        Paragraph ref = new Paragraph("Contrat n° " + contenu.contrat().numero() + " — émise le "
                + date(contenu.emiseLe()), GRAS);
        ref.setAlignment(Element.ALIGN_CENTER);
        ref.setSpacingBefore(6);
        ref.setSpacingAfter(4);
        doc.add(ref);
        Paragraph destinataire = new Paragraph();
        destinataire.add(new Chunk("À l'attention de la banque : ", GRAS));
        destinataire.add(new Chunk(contenu.banqueTitulaire().orElse(A_COMPLETER), TEXTE));
        destinataire.add(new Chunk("    Agence : ", GRAS));
        destinataire.add(new Chunk("..............................", TEXTE));
        doc.add(destinataire);
    }

    private static void donneurOrdre(Document doc, Acheteur a, ContenuAutorisation contenu) {
        section(doc, "1. Titulaire du compte à débiter (donneur d'ordre)");
        PdfPTable t = tableau(new float[] {2, 3});
        ligne(t, "Nom et prénom", a.prenom() + " " + a.nom());
        ligne(t, "NNI", a.nni());
        ligne(t, "Téléphone", a.telephone());
        ligne(t, "Banque", contenu.banqueTitulaire().orElse(A_COMPLETER));
        ligne(t, "Numéro de compte (RIB)", A_COMPLETER);
        doc.add(t);
    }

    private static void beneficiaire(Document doc, CompteBeneficiaire b) {
        section(doc, "2. Bénéficiaire des virements");
        PdfPTable t = tableau(new float[] {2, 3});
        ligne(t, "Titulaire", b.titulaire());
        ligne(t, "Banque", b.banque());
        ligne(t, "Numéro de compte (RIB)", b.numero());
        doc.add(t);
    }

    private static void ordre(Document doc, ContenuAutorisation contenu) {
        Contrat c = contenu.contrat();
        List<Echeance> echeances = c.echeances();
        Acheteur a = contenu.titulaire();
        section(doc, "3. Ordre de virement permanent");
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        p.add(new Chunk("Je soussigné(e) " + a.prenom() + " " + a.nom() + " demande à ma banque de virer, de mon "
                + "compte ci-dessus vers le compte de " + contenu.beneficiaire().titulaire() + ", les "
                + c.nombreMois() + " mensualités de mon contrat d'achat à tempérament, soit ", TEXTE));
        p.add(new Chunk(OpenPdfRenduContrat.montant(c.resteAFinancer()), GRAS));
        p.add(new Chunk(" au total, aux dates suivantes (jour de versement de mon salaire) :", TEXTE));
        doc.add(p);

        PdfPTable t = tableau(new float[] {1, 2, 2, 4});
        t.setHeaderRows(1);
        entete(t, "N°", Element.ALIGN_LEFT);
        entete(t, "Date d'exécution", Element.ALIGN_LEFT);
        entete(t, "Montant", Element.ALIGN_RIGHT);
        entete(t, "Libellé du virement", Element.ALIGN_LEFT);
        for (Echeance e : echeances) {
            t.addCell(cellule(String.valueOf(e.rang()), TEXTE, Element.ALIGN_LEFT));
            t.addCell(cellule(date(e.date()), TEXTE, Element.ALIGN_LEFT));
            t.addCell(cellule(OpenPdfRenduContrat.montant(e.montant()), TEXTE, Element.ALIGN_RIGHT));
            t.addCell(cellule(libelle(c, e), TEXTE, Element.ALIGN_LEFT));
        }
        PdfPCell total = cellule("Total", GRAS, Element.ALIGN_LEFT);
        total.setColspan(2);
        t.addCell(total);
        t.addCell(cellule(OpenPdfRenduContrat.montant(c.resteAFinancer()), GRAS, Element.ALIGN_RIGHT));
        t.addCell(cellule("", TEXTE, Element.ALIGN_LEFT));
        doc.add(t);

        paragraphe(doc, "Périodicité : mensuelle, " + c.nombreMois() + " virements, du " + date(echeances.getFirst().date())
                + " au " + date(echeances.getLast().date()) + ". Si une date tombe un jour non ouvré, le virement "
                + "est exécuté le jour ouvré suivant.");
    }

    /** Libellé repris sur le relevé du vendeur pour rapprocher le paiement : {@code SEH-2026-000001 M1/6}. */
    static String libelle(Contrat c, Echeance e) {
        return c.numero() + " M" + e.rang() + "/" + c.nombreMois();
    }

    private static void signatures(Document doc) {
        Paragraph fait = new Paragraph("Fait à ...................................., le ....../....../..........",
                TEXTE);
        fait.setSpacingBefore(6);
        fait.setSpacingAfter(4);
        doc.add(fait);
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.addCell(signature("Signature du titulaire du compte"));
        t.addCell(signature("Cachet et visa de la banque"));
        doc.add(t);
    }

    private static PdfPCell signature(String libelle) {
        PdfPCell cell = new PdfPCell(new Phrase(libelle, GRAS));
        cell.setBorderColor(Color.LIGHT_GRAY);
        cell.setPadding(5);
        cell.setFixedHeight(55);
        return cell;
    }

    private static void section(Document doc, String titre) {
        Paragraph p = new Paragraph(titre, SECTION);
        p.setSpacingBefore(7);
        p.setSpacingAfter(3);
        doc.add(p);
    }

    private static void paragraphe(Document doc, String texte) {
        Paragraph p = new Paragraph(texte, TEXTE);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        p.setSpacingBefore(3);
        doc.add(p);
    }

    private static PdfPTable tableau(float[] largeurs) {
        PdfPTable t = new PdfPTable(largeurs);
        t.setWidthPercentage(100);
        t.setSpacingBefore(3);
        t.setSpacingAfter(3);
        t.setKeepTogether(true);
        return t;
    }

    private static void ligne(PdfPTable t, String libelle, String valeur) {
        t.addCell(cellule(libelle, GRAS, Element.ALIGN_LEFT));
        t.addCell(cellule(valeur, TEXTE, Element.ALIGN_LEFT));
    }

    private static void entete(PdfPTable t, String libelle, int alignement) {
        PdfPCell cell = cellule(libelle, GRAS, alignement);
        cell.setBackgroundColor(GRIS_CLAIR);
        t.addCell(cell);
    }

    private static PdfPCell cellule(String texte, Font font, int alignement) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setHorizontalAlignment(alignement);
        cell.setPadding(3);
        cell.setBorderColor(Color.LIGHT_GRAY);
        return cell;
    }

    private static String date(LocalDate d) {
        return d.format(DATE);
    }
}
