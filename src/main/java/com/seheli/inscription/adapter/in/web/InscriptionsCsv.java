package com.seheli.inscription.adapter.in.web;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.seheli.inscription.domain.Inscription;

/**
 * Export CSV des pré-inscriptions, prévu pour être ouvert dans un tableur réglé en français :
 * séparateur point-virgule, UTF-8 avec BOM (accents et arabe lus correctement), fins de ligne CRLF,
 * dates locales de Nouakchott. Les types de contrat et tranches de salaire restent sous forme de code,
 * stables pour filtrer ou réimporter.
 *
 * <p>Protection contre l'injection de formules : une cellule qui commence par {@code = + - @}, une
 * tabulation ou un retour chariot est précédée d'une apostrophe, pour qu'un texte saisi sur la page
 * publique ne soit jamais exécuté par le tableur.
 */
final class InscriptionsCsv {

    static final String SEPARATEUR = ";";
    static final String FIN_LIGNE = "\r\n";
    static final String ENTETE = String.join(SEPARATEUR, "Inscrit le", "Nom", "Téléphone", "Employeur",
            "Type de contrat", "Tranche de salaire", "Produit souhaité", "Consentement le");

    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private InscriptionsCsv() {
    }

    static byte[] ecrire(List<Inscription> inscriptions, ZoneId fuseau) {
        DateTimeFormatter dates = FORMAT_DATE.withZone(fuseau);
        StringBuilder csv = new StringBuilder(ENTETE).append(FIN_LIGNE);
        for (Inscription i : inscriptions) {
            csv.append(String.join(SEPARATEUR,
                    dates.format(i.creeLe()),
                    cellule(i.nom()),
                    cellule(i.telephone()),
                    cellule(i.employeur()),
                    i.typeContrat().name(),
                    i.trancheSalaire().name(),
                    cellule(i.produitSouhaite()),
                    dates.format(i.consentementLe())))
                    .append(FIN_LIGNE);
        }
        byte[] texte = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] fichier = new byte[BOM.length + texte.length];
        System.arraycopy(BOM, 0, fichier, 0, BOM.length);
        System.arraycopy(texte, 0, fichier, BOM.length, texte.length);
        return fichier;
    }

    static String cellule(String valeur) {
        String v = valeur;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        if (v.contains(SEPARATEUR) || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
