package com.seheli.paiement.application;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lecture d'un fichier CSV de paiements reçus : trois colonnes, dans cet ordre, référence, montant et
 * date du paiement.
 *
 * <ul>
 * <li>Encodage UTF-8 (marque d'ordre des octets ignorée). Séparateur point-virgule (Excel en
 * français) ou virgule, déduit de la première ligne.</li>
 * <li>En-tête facultatif : la première ligne est un en-tête si sa première colonne commence par
 * « ref » (« reference », « Référence »…).</li>
 * <li>Guillemets autour d'une valeur retirés ; lignes vides ignorées ; colonnes vides en fin de ligne
 * tolérées.</li>
 * <li>Date au format ISO {@code 2026-11-12} ou {@code 12/11/2026} ; montant en ouguiyas, espaces
 * tolérées et décimales nulles ({@code 8320.00}) retirées. Le contrôle des valeurs reste celui de la
 * saisie manuelle.</li>
 * </ul>
 */
final class LecteurCsvPaiements {

    static final int TAILLE_MAX_OCTETS = 1024 * 1024;
    static final int LIGNES_MAX = 2_000;

    private static final Pattern DATE_FR = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})");
    private static final Pattern DECIMALES_NULLES = Pattern.compile("[.,]0+$");
    static final String COLONNES_INVALIDES = "bo.import.ligne.colonnes";

    private LecteurCsvPaiements() {
    }

    /** @throws FichierCsvInvalideException fichier vide, trop gros ou trop long */
    static List<LigneCsv> lire(byte[] contenu) {
        if (contenu == null || contenu.length == 0) {
            throw new FichierCsvInvalideException(FichierCsvInvalideException.VIDE);
        }
        if (contenu.length > TAILLE_MAX_OCTETS) {
            throw new FichierCsvInvalideException(FichierCsvInvalideException.TROP_GROS);
        }
        String texte = new String(contenu, StandardCharsets.UTF_8);
        if (texte.startsWith("﻿")) {
            texte = texte.substring(1);
        }
        String[] lignes = texte.split("\\r\\n|\\r|\\n", -1);
        List<LigneCsv> resultat = new ArrayList<>();
        Character separateur = null;
        for (int i = 0; i < lignes.length; i++) {
            String ligne = lignes[i];
            if (ligne.isBlank()) {
                continue;
            }
            if (separateur == null) {
                separateur = ligne.indexOf(';') >= 0 ? ';' : ',';
                if (estEnTete(ligne, separateur)) {
                    continue;
                }
            }
            if (resultat.size() == LIGNES_MAX) {
                throw new FichierCsvInvalideException(FichierCsvInvalideException.TROP_DE_LIGNES);
            }
            resultat.add(ligne(i + 1, ligne, separateur));
        }
        if (resultat.isEmpty()) {
            throw new FichierCsvInvalideException(FichierCsvInvalideException.VIDE);
        }
        return resultat;
    }

    private static boolean estEnTete(String ligne, char separateur) {
        String premiere = Normalizer.normalize(colonnes(ligne, separateur).getFirst(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return premiere.startsWith("ref");
    }

    private static LigneCsv ligne(int numero, String ligne, char separateur) {
        List<String> c = new ArrayList<>(colonnes(ligne, separateur));
        while (c.size() > 3 && c.getLast().isEmpty()) {
            c.removeLast();
        }
        if (c.size() != 3) {
            String reference = c.isEmpty() ? "" : c.getFirst();
            return new LigneCsv(numero, new SaisiePaiement(reference, "", ""), Optional.of(COLONNES_INVALIDES));
        }
        return new LigneCsv(numero, new SaisiePaiement(c.get(0), montant(c.get(1)), date(c.get(2))),
                Optional.empty());
    }

    private static List<String> colonnes(String ligne, char separateur) {
        List<String> valeurs = new ArrayList<>();
        for (String v : ligne.split(Pattern.quote(String.valueOf(separateur)), -1)) {
            String s = v.strip();
            if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
                s = s.substring(1, s.length() - 1).replace("\"\"", "\"").strip();
            }
            valeurs.add(s);
        }
        return valeurs;
    }

    private static String montant(String valeur) {
        return DECIMALES_NULLES.matcher(valeur).replaceFirst("");
    }

    private static String date(String valeur) {
        Matcher m = DATE_FR.matcher(valeur);
        if (m.matches()) {
            return "%s-%02d-%02d".formatted(m.group(3), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
        }
        return valeur;
    }
}
