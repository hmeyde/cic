package com.seheli.identite.adapter.in.web;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.validation.Errors;

import com.seheli.identite.application.NouveauCompteClient;

/**
 * Saisie du formulaire de création du compte client. La date de naissance est reçue en texte au format
 * ISO ({@code AAAA-MM-JJ}, celui du champ {@code <input type="date">}) pour signaler proprement une
 * saisie mal formée.
 */
public class CompteClientFormulaire {

    static final int NOM_LONGUEUR_MAX = 100;
    private static final Pattern NNI = Pattern.compile("\\d{10}");

    private String nni = "";
    private String nom = "";
    private String prenom = "";
    private String dateNaissance = "";
    private boolean consentement;

    /** Vérifie la forme de la saisie ; les règles métier (âge, unicité du NNI) sont contrôlées par le cas d'usage. */
    void valider(Errors erreurs) {
        if (nniNormalise().isEmpty()) {
            erreurs.rejectValue("nni", "client.compte.obligatoire");
        } else if (!NNI.matcher(nniNormalise()).matches()) {
            erreurs.rejectValue("nni", "client.compte.nni.invalide");
        }
        validerTexte("nom", nom, erreurs);
        validerTexte("prenom", prenom, erreurs);
        if (texte(dateNaissance).isEmpty()) {
            erreurs.rejectValue("dateNaissance", "client.compte.obligatoire");
        } else if (dateNaissanceSaisie().isEmpty()) {
            erreurs.rejectValue("dateNaissance", "client.compte.date-naissance.invalide");
        }
        if (!consentement) {
            erreurs.rejectValue("consentement", "client.compte.consentement.manquant");
        }
    }

    /** Saisie prête pour le cas d'usage ; à n'appeler qu'après une validation sans erreur. */
    NouveauCompteClient versCommande(String telephone) {
        return new NouveauCompteClient(telephone, nniNormalise(), nom.strip(), prenom.strip(),
                dateNaissanceSaisie().orElseThrow(), consentement);
    }

    private static void validerTexte(String champ, String valeur, Errors erreurs) {
        String v = texte(valeur);
        if (v.isEmpty()) {
            erreurs.rejectValue(champ, "client.compte.obligatoire");
        } else if (v.length() > NOM_LONGUEUR_MAX) {
            erreurs.rejectValue(champ, "client.compte.trop.long", new Object[] {NOM_LONGUEUR_MAX}, null);
        }
    }

    private String nniNormalise() {
        return texte(nni).replace(" ", "");
    }

    Optional<LocalDate> dateNaissanceSaisie() {
        try {
            return Optional.of(LocalDate.parse(texte(dateNaissance)));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static String texte(String valeur) {
        return valeur == null ? "" : valeur.strip();
    }

    public String getNni() {
        return nni;
    }

    public void setNni(String nni) {
        this.nni = nni;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(String dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public boolean isConsentement() {
        return consentement;
    }

    public void setConsentement(boolean consentement) {
        this.consentement = consentement;
    }
}
