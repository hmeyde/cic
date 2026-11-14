package com.seheli.comptabilite.application;

/**
 * Apport en capital tel que saisi au back-office, avant contrôle.
 *
 * @param associe nom de l'associé qui a versé l'apport
 * @param montant montant versé en ouguiyas, sans décimale ; les espaces sont ignorés
 * @param verseLe date du versement au format ISO {@code 2026-11-12}
 */
public record SaisieApportCapital(String associe, String montant, String verseLe) {
}
