package com.seheli.identite.application;

import java.time.LocalDate;

/**
 * Saisie de la création d'un compte client.
 *
 * @param telephone     téléphone dont la possession vient d'être prouvée par un code à usage unique
 * @param consentement  vrai si le client a coché le consentement explicite à l'usage de ses données
 */
public record NouveauCompteClient(String telephone, String nni, String nom, String prenom, LocalDate dateNaissance,
        boolean consentement) {
}
