package com.seheli.identite.application;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.seheli.identite.domain.CodeConnexion.Verification;

/**
 * Issue de la saisie d'un code de connexion.
 *
 * <p>Un code valide prouve que le téléphone appartient à la personne. Si un client porte ce téléphone,
 * {@code clientId} l'identifie ; sinon il s'agit d'un nouveau client, qui doit créer son compte.
 *
 * @param verification   résultat de la vérification
 * @param clientId       client authentifié, seulement si le code est valide et que le client existe
 * @param essaisRestants essais encore possibles avec le même code
 */
public record ResultatConnexion(Verification verification, Optional<UUID> clientId, int essaisRestants) {

    public ResultatConnexion {
        Objects.requireNonNull(verification, "verification");
        Objects.requireNonNull(clientId, "clientId");
        if (clientId.isPresent() && verification != Verification.VALIDE) {
            throw new IllegalArgumentException("Seul un code valide identifie le client");
        }
    }

    static ResultatConnexion valide(UUID clientId) {
        return new ResultatConnexion(Verification.VALIDE, Optional.of(clientId), 0);
    }

    static ResultatConnexion nouveauClient() {
        return new ResultatConnexion(Verification.VALIDE, Optional.empty(), 0);
    }

    static ResultatConnexion echec(Verification verification, int essaisRestants) {
        return new ResultatConnexion(verification, Optional.empty(), essaisRestants);
    }

    public boolean estValide() {
        return verification == Verification.VALIDE;
    }

    /** Code valide, mais aucun compte n'existe encore pour ce téléphone. */
    public boolean estNouveauClient() {
        return estValide() && clientId.isEmpty();
    }
}
