package com.cic.captures;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.impl.driver.Driver;
import com.microsoft.playwright.options.LoadState;

/**
 * Capture en PNG les écrans clés de l'application, en français et en arabe, dans {@code docs/captures/<langue>/}.
 *
 * <p>L'application doit tourner en profil {@code demo} (données fictives, code client {@code 000000}).
 * Parcours client et pages publiques en format mobile, back-office en format bureau. Chaque rôle a son propre
 * contexte de navigateur ; la langue est choisie par {@code ?lang=} puis mémorisée dans le cookie de l'application.
 *
 * <p>Variables d'environnement (facultatives) : {@code CIC_CAPTURES_URL} (défaut {@code http://localhost:8080}),
 * {@code CIC_CAPTURES_SORTIE} (défaut {@code docs/captures}), et les identifiants back-office
 * {@code CIC_BO_<ROLE>_IDENTIFIANT} / {@code CIC_BO_<ROLE>_MOT_DE_PASSE} (défauts du profil demo).
 */
public final class CapturesEcrans {

    private static final List<String> LANGUES = List.of("fr", "ar");
    /** Client fictif en COMMANDEE, remise à confirmer : l'exemple chiffré de CLAUDE.md. */
    private static final String TELEPHONE_CLIENT = "22100006";
    private static final String CODE_DEMO = "000000";

    private final String base;
    private final Path sortie;
    private final Browser navigateur;
    private int captures;

    private CapturesEcrans(String base, Path sortie, Browser navigateur) {
        this.base = base;
        this.sortie = sortie;
        this.navigateur = navigateur;
    }

    public static void main(String[] args) throws Exception {
        String base = env("CIC_CAPTURES_URL", "http://localhost:8080").replaceAll("/+$", "");
        Path sortie = Path.of(env("CIC_CAPTURES_SORTIE", "docs/captures")).toAbsolutePath();
        installerChromium();
        try (Playwright playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
             Browser navigateur = playwright.chromium().launch()) {
            CapturesEcrans c = new CapturesEcrans(base, sortie, navigateur);
            for (String langue : LANGUES) {
                Files.createDirectories(sortie.resolve(langue));
                c.pagesPubliques(langue);
                c.parcoursClient(langue);
                c.backOffice(langue);
            }
            System.out.println(c.captures + " captures écrites dans " + sortie);
        }
    }

    /** Télécharge seulement Chromium sans interface (par défaut, Playwright installerait tous les navigateurs). */
    private static void installerChromium() throws Exception {
        ProcessBuilder pb = Driver.ensureDriverInstalled(Map.of(), false).createProcessBuilder();
        pb.command().addAll(List.of("install", "chromium-headless-shell"));
        int code = pb.inheritIO().start().waitFor();
        if (code != 0) {
            throw new IllegalStateException("Installation de Chromium impossible (code " + code + ")");
        }
    }

    private void pagesPubliques(String langue) {
        try (BrowserContext ctx = mobile()) {
            Page p = ctx.newPage();
            ouvrir(p, "/?lang=" + langue, langue);
            capturer(p, langue, "01-accueil");
            ouvrir(p, "/inscription", langue);
            capturer(p, langue, "02-inscription");
        }
    }

    private void parcoursClient(String langue) {
        try (BrowserContext ctx = mobile()) {
            Page p = ctx.newPage();
            ouvrir(p, "/client/connexion?lang=" + langue, langue);
            capturer(p, langue, "03-client-connexion");
            p.fill("#telephone", TELEPHONE_CLIENT);
            soumettre(p);
            verifierPas(p, "/client/connexion?", "numéro de téléphone refusé");
            capturer(p, langue, "04-client-code");
            p.fill("#code", CODE_DEMO);
            soumettre(p);
            verifierPas(p, "/client/connexion", "connexion client refusée (profil demo actif ?)");

            ouvrir(p, "/client", langue);
            capturer(p, langue, "05-client-espace");
            ouvrir(p, "/client/catalogue", langue);
            capturer(p, langue, "06-client-catalogue");
            suivrePremierLien(p, "a[href*='/client/catalogue/']:not([href*='categorie'])", langue);
            capturer(p, langue, "07-client-produit");
            ouvrir(p, "/client/dossier", langue);
            capturer(p, langue, "08-client-dossier");
        }
    }

    private void backOffice(String langue) {
        try (BrowserContext ctx = bureau()) {
            Page p = ctx.newPage();
            ouvrir(p, "/bo/login?lang=" + langue, langue);
            capturer(p, langue, "10-bo-connexion");
        }
        try (BrowserContext ctx = bureau()) {
            Page p = connecterBackOffice(ctx, "ANALYSTE", "analyste", langue);
            ouvrir(p, "/bo", langue);
            capturer(p, langue, "11-bo-accueil");
            ouvrir(p, "/bo/dossiers", langue);
            capturer(p, langue, "12-bo-dossiers");
            suivrePremierLien(p, "table a[href*='/bo/dossiers/']", langue);
            capturer(p, langue, "13-bo-dossier-score");
            ouvrir(p, "/bo/autorisations", langue);
            capturer(p, langue, "14-bo-autorisations");
        }
        try (BrowserContext ctx = bureau()) {
            Page p = connecterBackOffice(ctx, "ADMIN", "admin", langue);
            ouvrir(p, "/bo/produits", langue);
            capturer(p, langue, "15-bo-produits");
            ouvrir(p, "/bo/commandes", langue);
            capturer(p, langue, "16-bo-commandes");
        }
        try (BrowserContext ctx = bureau()) {
            Page p = connecterBackOffice(ctx, "RECOUVREMENT", "recouvrement", langue);
            ouvrir(p, "/bo/paiements", langue);
            capturer(p, langue, "17-bo-paiements");
            ouvrir(p, "/bo/recouvrement", langue);
            capturer(p, langue, "18-bo-recouvrement");
            suivrePremierLien(p, "a[href*='/bo/recouvrement/dossiers/']", langue);
            capturer(p, langue, "19-bo-recouvrement-dossier");
        }
        try (BrowserContext ctx = bureau()) {
            Page p = connecterBackOffice(ctx, "ASSOCIE", "associe", langue);
            ouvrir(p, "/bo/pilotage", langue);
            capturer(p, langue, "20-bo-pilotage");
        }
    }

    private Page connecterBackOffice(BrowserContext ctx, String role, String defaut, String langue) {
        Page p = ctx.newPage();
        ouvrir(p, "/bo/login?lang=" + langue, langue);
        p.fill("#identifiant", env("CIC_BO_" + role + "_IDENTIFIANT", defaut));
        p.fill("#motDePasse", env("CIC_BO_" + role + "_MOT_DE_PASSE", defaut + "-demo"));
        soumettre(p);
        verifierPas(p, "/bo/login", "connexion back-office refusée pour le rôle " + role);
        return p;
    }

    private BrowserContext mobile() {
        return navigateur.newContext(new Browser.NewContextOptions()
                .setViewportSize(390, 844).setDeviceScaleFactor(2).setIsMobile(true).setHasTouch(true));
    }

    private BrowserContext bureau() {
        return navigateur.newContext(new Browser.NewContextOptions().setViewportSize(1366, 900));
    }

    private void ouvrir(Page p, String chemin, String langue) {
        var reponse = p.navigate(base + chemin);
        if (reponse == null || reponse.status() >= 400) {
            throw new IllegalStateException("Échec de " + chemin + " : HTTP "
                    + (reponse == null ? "?" : reponse.status()));
        }
        attendre(p, langue);
    }

    private void suivrePremierLien(Page p, String selecteur, String langue) {
        var lien = p.locator(selecteur).first();
        if (lien.count() == 0) {
            throw new IllegalStateException("Aucun lien « " + selecteur + " » sur " + p.url()
                    + " : données de démonstration chargées ?");
        }
        lien.click();
        attendre(p, langue);
    }

    private static void soumettre(Page p) {
        p.locator("form.formulaire button[type=submit]").first().click();
        p.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /** Attend la fin des requêtes HTMX et vérifie que la page est bien rendue dans la langue demandée. */
    private static void attendre(Page p, String langue) {
        p.waitForLoadState(LoadState.NETWORKIDLE);
        String lang = p.getAttribute("html", "lang");
        if (lang == null || !lang.startsWith(langue)) {
            throw new IllegalStateException("Page " + p.url() + " en « " + lang + " » au lieu de « " + langue + " »");
        }
    }

    private static void verifierPas(Page p, String fragmentUrl, String message) {
        if (p.url().contains(fragmentUrl)) {
            throw new IllegalStateException(message + " (" + p.url() + ")");
        }
    }

    private void capturer(Page p, String langue, String nom) {
        Path fichier = sortie.resolve(langue).resolve(nom + ".png");
        p.screenshot(new Page.ScreenshotOptions().setPath(fichier).setFullPage(true));
        captures++;
        System.out.println("  " + langue + "/" + nom + ".png");
    }

    private static String env(String nom, String defaut) {
        String v = System.getenv(nom);
        return v == null || v.isBlank() ? defaut : v;
    }
}
