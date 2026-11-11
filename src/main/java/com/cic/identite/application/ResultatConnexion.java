package com.cic.identite.application;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.cic.identite.domain.CodeConnexion.Verification;

/**
 * Issue de la saisie d'un code de connexion.
 *
 * @param verification   résultat de la vérification
 * @param clientId       client authentifié, seulement si le code est valide
 * @param essaisRestants essais encore possibles avec le même code
 */
public record ResultatConnexion(Verification verification, Optional<UUID> clientId, int essaisRestants) {

    public ResultatConnexion {
        Objects.requireNonNull(verification, "verification");
        Objects.requireNonNull(clientId, "clientId");
        if ((verification == Verification.VALIDE) != clientId.isPresent()) {
            throw new IllegalArgumentException("Seul un code valide identifie le client");
        }
    }

    static ResultatConnexion valide(UUID clientId) {
        return new ResultatConnexion(Verification.VALIDE, Optional.of(clientId), 0);
    }

    static ResultatConnexion echec(Verification verification, int essaisRestants) {
        return new ResultatConnexion(verification, Optional.empty(), essaisRestants);
    }

    public boolean estValide() {
        return verification == Verification.VALIDE;
    }
}
