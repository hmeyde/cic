package com.seheli.web;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expose aux gabarits le menu latéral du back-office ({@code menuBo}, {@link MenuBackOffice} ou
 * {@code null}). Le layout passe en mise en page « ordinateur » (menu + contenu) quand il est présent.
 */
@ControllerAdvice
class NavigationBackOfficeAdvice {

    private static final String PREFIXE_ROLE = "ROLE_";

    private final boolean demonstration;

    NavigationBackOfficeAdvice(Environment environment) {
        this.demonstration = environment.matchesProfiles("demo");
    }

    @ModelAttribute("menuBo")
    MenuBackOffice menuBo(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String chemin = request.getRequestURI().substring(request.getContextPath().length());
        return MenuBackOffice.pour(chemin, auth.getName(), auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a != null && a.startsWith(PREFIXE_ROLE))
                .map(a -> a.substring(PREFIXE_ROLE.length()))
                .toList(), demonstration).orElse(null);
    }
}
