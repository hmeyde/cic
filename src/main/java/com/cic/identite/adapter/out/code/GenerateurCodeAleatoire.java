package com.cic.identite.adapter.out.code;

import java.security.SecureRandom;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.cic.identite.domain.GenerateurCode;

/** Code aléatoire cryptographiquement sûr, dans tous les profils sauf demo. */
@Component
@Profile("!demo")
class GenerateurCodeAleatoire implements GenerateurCode {

    private static final int BORNE = (int) Math.pow(10, LONGUEUR);

    private final SecureRandom aleatoire = new SecureRandom();

    @Override
    public String generer() {
        return String.format("%0" + LONGUEUR + "d", aleatoire.nextInt(BORNE));
    }
}
