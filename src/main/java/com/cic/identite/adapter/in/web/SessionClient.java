package com.cic.identite.adapter.in.web;

import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Ouverture de la session authentifiée d'un client (rôle CLIENT). Le principal est l'identifiant du
 * client (UUID), pas son téléphone. L'identifiant de session est renouvelé (fixation de session).
 */
@Component
class SessionClient {

    static final String ROLE_CLIENT = "ROLE_CLIENT";

    private final SecurityContextRepository contextes = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy strategie = SecurityContextHolder.getContextHolderStrategy();

    void ouvrir(UUID clientId, HttpServletRequest request, HttpServletResponse response) {
        request.changeSessionId();
        var authentification = UsernamePasswordAuthenticationToken.authenticated(clientId.toString(), null,
                List.of(new SimpleGrantedAuthority(ROLE_CLIENT)));
        SecurityContext contexte = strategie.createEmptyContext();
        contexte.setAuthentication(authentification);
        strategie.setContext(contexte);
        contextes.saveContext(contexte, request, response);
    }
}
