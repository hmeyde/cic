package com.cic.identite.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Page de connexion du back-office ; le traitement du formulaire est assuré par Spring Security. */
@Controller
class ConnexionBackOfficeController {

    @GetMapping("/bo/login")
    String connexion() {
        return "bo/connexion";
    }
}
