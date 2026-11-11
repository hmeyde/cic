package com.cic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/**
 * Sécurité web.
 *
 * <p>Back-office ({@code /bo/**}) : connexion par identifiant et mot de passe (BCrypt), réservée aux
 * rôles de l'équipe CIC ; la gestion du catalogue ({@code /bo/fournisseurs/**}) est réservée au rôle ADMIN.
 * Les comptes sont fournis par le module identite (bean {@code UserDetailsService}).
 * Les autres pages restent publiques en attendant l'espace client (code à usage unique).
 * CSRF actif partout ; en-têtes de sécurité par défaut de Spring Security, plus une politique de référent.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Coût BCrypt : environ 250 ms par vérification sur un serveur courant. */
    private static final int COUT_BCRYPT = 12;

    static final String[] ROLES_BACK_OFFICE = {"ANALYSTE", "RECOUVREMENT", "ADMIN", "ASSOCIE"};

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(COUT_BCRYPT);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/bo/login").permitAll()
                .requestMatchers("/bo/fournisseurs", "/bo/fournisseurs/**").hasRole("ADMIN")
                .requestMatchers("/bo", "/bo/**").hasAnyRole(ROLES_BACK_OFFICE)
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/bo/login")
                .usernameParameter("identifiant")
                .passwordParameter("motDePasse")
                .defaultSuccessUrl("/bo/")
                .failureUrl("/bo/login?erreur")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/bo/logout")
                .logoutSuccessUrl("/bo/login?deconnexion")
                .permitAll()
            )
            .headers(headers -> headers
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.SAME_ORIGIN))
            );
        return http.build();
    }
}
