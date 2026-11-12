package com.cic.paiement.application;

/**
 * Paiement tel que saisi au back-office, avant contrôle.
 *
 * @param reference référence indiquée par le client dans le motif du paiement Masrvi
 * @param montant   montant reçu en ouguiyas, sans décimale ; les espaces sont ignorés
 * @param recuLe    date du paiement au format ISO {@code 2026-11-12}
 */
public record SaisiePaiement(String reference, String montant, String recuLe) {
}
