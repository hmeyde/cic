package com.seheli.demonstration.domain;

/** Une réinitialisation de la démonstration est déjà en cours. */
public class ReinitialisationEnCoursException extends RuntimeException {

    public ReinitialisationEnCoursException() {
        super("Une réinitialisation de la démonstration est déjà en cours");
    }
}
