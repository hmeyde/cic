package com.cic.paiement.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Référence portée par un paiement reçu, telle que le client l'a indiquée dans le motif de son
 * paiement Masrvi. Elle est unique : deux paiements ne peuvent pas porter la même référence, ce qui
 * rend la saisie idempotente (un paiement saisi deux fois est refusé la seconde fois).
 *
 * <p>Normalisée à la lecture : espaces, tirets et points retirés, lettres en majuscules ; puis
 * lettres latines et chiffres seulement, {@value #LONGUEUR_MAX} caractères au plus. Une référence
 * peut être bien formée sans être une référence d'apport valide : le paiement est alors enregistré
 * mais reste non rapproché.
 */
public record ReferencePaiement(String valeur) {

    public static final int LONGUEUR_MAX = 30;

    public ReferencePaiement {
        Objects.requireNonNull(valeur, "valeur");
        if (!valeur.matches("[A-Z0-9]{1," + LONGUEUR_MAX + "}")) {
            throw new IllegalArgumentException("Référence de paiement invalide");
        }
    }

    /**
     * @throws IllegalArgumentException saisie vide, trop longue ou avec d'autres caractères que des
     *         lettres latines et des chiffres
     */
    public static ReferencePaiement lire(String saisie) {
        Objects.requireNonNull(saisie, "saisie");
        return new ReferencePaiement(saisie.replaceAll("[\\s.\\-]", "").toUpperCase(Locale.ROOT));
    }

    /** Référence d'apport d'un dossier si la référence en a la forme et la clé. */
    public Optional<ReferenceApport> commeReferenceApport() {
        return ReferenceApport.estValide(valeur) ? Optional.of(new ReferenceApport(valeur)) : Optional.empty();
    }

    @Override
    public String toString() {
        return valeur;
    }
}
