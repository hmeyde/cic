package com.seheli.web;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expose aux gabarits l'onglet actif de l'espace client ({@code ongletClient}, nom de
 * {@link OngletClient} ou {@code null}). Le layout affiche la barre d'onglets quand il est présent.
 */
@ControllerAdvice
class NavigationClientAdvice {

    @ModelAttribute("ongletClient")
    String ongletClient(HttpServletRequest request) {
        String chemin = request.getRequestURI().substring(request.getContextPath().length());
        return OngletClient.pour(chemin).map(Enum::name).orElse(null);
    }
}
