package com.cic.identite.adapter.in.web;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.cic.identite.application.CompteBackOfficeService;
import com.cic.identite.domain.UtilisateurBackOffice;

/**
 * Pont entre les comptes du back-office et Spring Security : un rôle {@code X} devient
 * l'autorité {@code ROLE_X}. Un compte inactif est refusé.
 */
@Component
class UtilisateursBackOfficeDetailsService implements UserDetailsService {

    private final CompteBackOfficeService comptes;

    UtilisateursBackOfficeDetailsService(CompteBackOfficeService comptes) {
        this.comptes = comptes;
    }

    @Override
    public UserDetails loadUserByUsername(String identifiant) {
        UtilisateurBackOffice u = comptes.parIdentifiant(identifiant)
                // Message neutre : ni l'identifiant ni son existence ne doivent fuiter dans les journaux.
                .orElseThrow(() -> new UsernameNotFoundException("Compte inconnu"));
        return User.withUsername(u.identifiant())
                .password(u.motDePasseHash())
                .roles(u.role().name())
                .disabled(!u.estActif())
                .build();
    }
}
