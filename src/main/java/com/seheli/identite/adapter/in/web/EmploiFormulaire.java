package com.seheli.identite.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.validation.Errors;

import com.seheli.commun.domain.Montant;
import com.seheli.identite.application.SaisieEmploi;
import com.seheli.identite.domain.Emploi;
import com.seheli.identite.domain.ModeRemboursement;
import com.seheli.identite.domain.TypeContrat;

/**
 * Saisie de l'écran emploi du client. Les valeurs sont reçues en texte pour signaler proprement une saisie
 * mal formée : dates au format ISO ({@code <input type="date">}), salaire en ouguiyas entières (espaces
 * tolérés). La date de fin n'est lue que pour un CDD.
 */
public class EmploiFormulaire {

    /** NUMERIC(12, 0) en base. */
    static final int SALAIRE_CHIFFRES_MAX = 12;
    private static final Pattern ENTIER = Pattern.compile("\\d+");

    private String employeur = "";
    private String typeContrat = "";
    private String dateDebut = "";
    private String dateFin = "";
    private String salaireNet = "";
    private String jourPaie = "";
    private boolean salaireVerseSurCompte;
    private boolean salaireDomicilieBmci;
    private String modeRemboursement = "";

    static EmploiFormulaire depuis(Emploi e) {
        EmploiFormulaire f = new EmploiFormulaire();
        f.employeur = e.employeur();
        f.typeContrat = e.typeContrat().name();
        f.dateDebut = e.dateDebut().toString();
        f.dateFin = e.dateFin().map(LocalDate::toString).orElse("");
        f.salaireNet = e.salaireNet().valeur().toPlainString();
        f.jourPaie = Integer.toString(e.jourPaie());
        f.salaireVerseSurCompte = e.estSalaireVerseSurCompte();
        f.salaireDomicilieBmci = e.estSalaireDomicilieBmci();
        f.modeRemboursement = e.modeRemboursement().name();
        return f;
    }

    /** Vérifie la forme et la cohérence de la saisie ; l'emploi en cours est contrôlé par le cas d'usage. */
    void valider(Errors erreurs) {
        String emp = texte(employeur);
        if (emp.isEmpty()) {
            erreurs.rejectValue("employeur", "client.compte.obligatoire");
        } else if (emp.length() > Emploi.EMPLOYEUR_LONGUEUR_MAX) {
            erreurs.rejectValue("employeur", "client.compte.trop.long", new Object[] {Emploi.EMPLOYEUR_LONGUEUR_MAX},
                    null);
        }
        Optional<TypeContrat> type = typeContratSaisi();
        if (type.isEmpty()) {
            erreurs.rejectValue("typeContrat", "client.emploi.choix.obligatoire");
        }
        Optional<LocalDate> debut = date(dateDebut);
        if (texte(dateDebut).isEmpty()) {
            erreurs.rejectValue("dateDebut", "client.compte.obligatoire");
        } else if (debut.isEmpty()) {
            erreurs.rejectValue("dateDebut", "client.emploi.date.invalide");
        }
        if (type.orElse(null) == TypeContrat.CDD) {
            Optional<LocalDate> fin = date(dateFin);
            if (texte(dateFin).isEmpty()) {
                erreurs.rejectValue("dateFin", "client.emploi.date-fin.obligatoire");
            } else if (fin.isEmpty()) {
                erreurs.rejectValue("dateFin", "client.emploi.date.invalide");
            } else if (debut.isPresent() && !fin.get().isAfter(debut.get())) {
                erreurs.rejectValue("dateFin", "client.emploi.date-fin.avant-debut");
            }
        }
        String salaire = salaireNormalise();
        if (salaire.isEmpty()) {
            erreurs.rejectValue("salaireNet", "client.compte.obligatoire");
        } else if (!ENTIER.matcher(salaire).matches() || salaire.length() > SALAIRE_CHIFFRES_MAX) {
            erreurs.rejectValue("salaireNet", "client.emploi.salaire.invalide");
        } else if (new BigDecimal(salaire).signum() == 0) {
            erreurs.rejectValue("salaireNet", "client.emploi.salaire.invalide");
        }
        if (texte(jourPaie).isEmpty()) {
            erreurs.rejectValue("jourPaie", "client.compte.obligatoire");
        } else if (jourPaieSaisi().isEmpty()) {
            erreurs.rejectValue("jourPaie", "client.emploi.jour-paie.invalide");
        }
        if (salaireDomicilieBmci && !salaireVerseSurCompte) {
            erreurs.rejectValue("salaireDomicilieBmci", "client.emploi.bmci.sans-compte");
        }
        Optional<ModeRemboursement> mode = modeSaisi();
        if (mode.isEmpty()) {
            erreurs.rejectValue("modeRemboursement", "client.emploi.choix.obligatoire");
        } else if (mode.get() == ModeRemboursement.DOMICILIATION_BMCI && !salaireDomicilieBmci) {
            erreurs.rejectValue("modeRemboursement", "client.emploi.mode.domiciliation-sans-bmci");
        } else if (mode.get() == ModeRemboursement.VIREMENT_PERMANENT && !salaireVerseSurCompte) {
            erreurs.rejectValue("modeRemboursement", "client.emploi.mode.virement-sans-compte");
        }
    }

    /** Saisie prête pour le cas d'usage ; à n'appeler qu'après une validation sans erreur. */
    SaisieEmploi versCommande() {
        TypeContrat type = typeContratSaisi().orElseThrow();
        LocalDate fin = type == TypeContrat.CDD ? date(dateFin).orElseThrow() : null;
        return new SaisieEmploi(texte(employeur), type, date(dateDebut).orElseThrow(), fin,
                Montant.de(new BigDecimal(salaireNormalise())), jourPaieSaisi().orElseThrow(), salaireVerseSurCompte,
                salaireDomicilieBmci, modeSaisi().orElseThrow());
    }

    private Optional<TypeContrat> typeContratSaisi() {
        return enumeration(TypeContrat.class, typeContrat);
    }

    private Optional<ModeRemboursement> modeSaisi() {
        return enumeration(ModeRemboursement.class, modeRemboursement);
    }

    private Optional<Integer> jourPaieSaisi() {
        String j = texte(jourPaie);
        if (!ENTIER.matcher(j).matches() || j.length() > 2) {
            return Optional.empty();
        }
        int jour = Integer.parseInt(j);
        return jour >= 1 && jour <= 31 ? Optional.of(jour) : Optional.empty();
    }

    private String salaireNormalise() {
        return texte(salaireNet).replace(" ", "").replace(" ", "").replace(" ", "");
    }

    private static <E extends Enum<E>> Optional<E> enumeration(Class<E> type, String valeur) {
        try {
            return Optional.of(Enum.valueOf(type, texte(valeur)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Optional<LocalDate> date(String valeur) {
        try {
            return Optional.of(LocalDate.parse(texte(valeur)));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static String texte(String valeur) {
        return valeur == null ? "" : valeur.strip();
    }

    public String getEmployeur() {
        return employeur;
    }

    public void setEmployeur(String employeur) {
        this.employeur = employeur;
    }

    public String getTypeContrat() {
        return typeContrat;
    }

    public void setTypeContrat(String typeContrat) {
        this.typeContrat = typeContrat;
    }

    public String getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(String dateDebut) {
        this.dateDebut = dateDebut;
    }

    public String getDateFin() {
        return dateFin;
    }

    public void setDateFin(String dateFin) {
        this.dateFin = dateFin;
    }

    public String getSalaireNet() {
        return salaireNet;
    }

    public void setSalaireNet(String salaireNet) {
        this.salaireNet = salaireNet;
    }

    public String getJourPaie() {
        return jourPaie;
    }

    public void setJourPaie(String jourPaie) {
        this.jourPaie = jourPaie;
    }

    public boolean isSalaireVerseSurCompte() {
        return salaireVerseSurCompte;
    }

    public void setSalaireVerseSurCompte(boolean salaireVerseSurCompte) {
        this.salaireVerseSurCompte = salaireVerseSurCompte;
    }

    public boolean isSalaireDomicilieBmci() {
        return salaireDomicilieBmci;
    }

    public void setSalaireDomicilieBmci(boolean salaireDomicilieBmci) {
        this.salaireDomicilieBmci = salaireDomicilieBmci;
    }

    public String getModeRemboursement() {
        return modeRemboursement;
    }

    public void setModeRemboursement(String modeRemboursement) {
        this.modeRemboursement = modeRemboursement;
    }
}
