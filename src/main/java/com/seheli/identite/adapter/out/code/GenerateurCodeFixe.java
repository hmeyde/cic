package com.seheli.identite.adapter.out.code;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.seheli.identite.domain.GenerateurCode;

/** Profil demo uniquement : toujours {@value #CODE}, pour que la démonstration se déroule sans lire les journaux. */
@Component
@Profile("demo")
class GenerateurCodeFixe implements GenerateurCode {

    static final String CODE = "000000";

    @Override
    public String generer() {
        return CODE;
    }
}
