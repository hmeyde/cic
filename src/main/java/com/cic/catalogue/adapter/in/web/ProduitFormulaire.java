package com.cic.catalogue.adapter.in.web;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.validation.Errors;

import com.cic.catalogue.domain.CategorieProduit;
import com.cic.catalogue.domain.Produit;
import com.cic.commun.domain.Montant;

/**
 * Saisie du formulaire produit. Le prix public est saisi en ouguiyas entières ; les espaces
 * (séparateurs de milliers, {@code 20 000}) sont acceptés.
 */
public class ProduitFormulaire {

    static final int REFERENCE_LONGUEUR_MAX = 50;
    static final int DESIGNATION_LONGUEUR_MAX = 200;
    static final int MARQUE_LONGUEUR_MAX = 100;
    /** Ouguiyas entières, douze chiffres au plus (colonne NUMERIC(12, 0)). */
    private static final Pattern PRIX = Pattern.compile("\\d{1,12}");
    private static final Pattern ESPACES = Pattern.compile("[\\s\\u00A0\\u202F]");

    private String reference = "";
    private String designation = "";
    private String marque = "";
    private String categorie = "";
    private String fournisseurId = "";
    private String prixPublic = "";
    private boolean disponible = true;

    static ProduitFormulaire de(Produit p) {
        ProduitFormulaire formulaire = new ProduitFormulaire();
        formulaire.reference = p.reference();
        formulaire.designation = p.designation();
        formulaire.marque = p.marque();
        formulaire.categorie = p.categorie().name();
        formulaire.fournisseurId = p.fournisseurId().toString();
        formulaire.prixPublic = p.prixPublic().valeur().toPlainString();
        formulaire.disponible = p.estDisponible();
        return formulaire;
    }

    /**
     * Vérifie la forme de la saisie ; les règles métier (unicité de la référence, fournisseur actif)
     * sont contrôlées par le cas d'usage. La référence n'est vérifiée qu'en création : elle ne
     * se modifie plus ensuite.
     */
    void valider(Errors erreurs, boolean creation) {
        if (creation) {
            texteObligatoire(erreurs, "reference", reference, REFERENCE_LONGUEUR_MAX);
        }
        texteObligatoire(erreurs, "designation", designation, DESIGNATION_LONGUEUR_MAX);
        texteObligatoire(erreurs, "marque", marque, MARQUE_LONGUEUR_MAX);
        if (categorieChoisie().isEmpty()) {
            erreurs.rejectValue("categorie", "bo.formulaire.obligatoire");
        }
        if (fournisseurChoisi().isEmpty()) {
            erreurs.rejectValue("fournisseurId", "bo.formulaire.obligatoire");
        }
        String prix = sansEspaces(prixPublic);
        if (prix.isEmpty()) {
            erreurs.rejectValue("prixPublic", "bo.formulaire.obligatoire");
        } else if (prixSaisi().isEmpty()) {
            erreurs.rejectValue("prixPublic", "bo.produit.prix.invalide");
        }
    }

    Optional<CategorieProduit> categorieChoisie() {
        String c = categorie == null ? "" : categorie.strip();
        return Arrays.stream(CategorieProduit.values()).filter(v -> v.name().equals(c)).findFirst();
    }

    Optional<UUID> fournisseurChoisi() {
        String f = fournisseurId == null ? "" : fournisseurId.strip();
        try {
            return f.isEmpty() ? Optional.empty() : Optional.of(UUID.fromString(f));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Prix public saisi, s'il est bien formé et strictement positif. */
    Optional<Montant> prixSaisi() {
        String p = sansEspaces(prixPublic);
        if (!PRIX.matcher(p).matches()) {
            return Optional.empty();
        }
        BigDecimal valeur = new BigDecimal(p);
        return valeur.signum() > 0 ? Optional.of(Montant.de(valeur)) : Optional.empty();
    }

    private static void texteObligatoire(Errors erreurs, String champ, String valeur, int longueurMax) {
        String v = valeur == null ? "" : valeur.strip();
        if (v.isEmpty()) {
            erreurs.rejectValue(champ, "bo.formulaire.obligatoire");
        } else if (v.length() > longueurMax) {
            erreurs.rejectValue(champ, "bo.formulaire.trop.long", new Object[] {longueurMax}, null);
        }
    }

    private static String sansEspaces(String valeur) {
        return valeur == null ? "" : ESPACES.matcher(valeur).replaceAll("");
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getMarque() {
        return marque;
    }

    public void setMarque(String marque) {
        this.marque = marque;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public String getFournisseurId() {
        return fournisseurId;
    }

    public void setFournisseurId(String fournisseurId) {
        this.fournisseurId = fournisseurId;
    }

    public String getPrixPublic() {
        return prixPublic;
    }

    public void setPrixPublic(String prixPublic) {
        this.prixPublic = prixPublic;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }
}
