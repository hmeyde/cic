package com.cic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/**
 * Sécurité web.
 *
 * <p>Espace client ({@code /client/**}) : connexion par téléphone et code à usage unique, traitée par le
 * module identite ({@code ConnexionClientController}) ; seules les pages de connexion (dont la création du
 * compte d'un nouveau client, après un code valide) sont publiques, le
 * reste exige le rôle CLIENT (dont le catalogue {@code /client/catalogue/**}, les demandes
 * {@code /client/demandes} et le suivi du dossier {@code /client/dossier}).
 *
 * <p>Back-office ({@code /bo/**}) : connexion par identifiant et mot de passe (BCrypt), réservée aux
 * rôles de l'équipe CIC ; la gestion du catalogue ({@code /bo/fournisseurs/**}, {@code /bo/produits/**}) est réservée au rôle ADMIN.
 * Les justificatifs des clients ({@code /bo/justificatifs/**}) sont réservés aux rôles ANALYSTE et ADMIN.
 * La file des dossiers à analyser ({@code /bo/dossiers}) et la fiche de chaque dossier
 * ({@code /bo/dossiers/{id}}), avec la décision et le journal du dossier, sont réservées au rôle ANALYSTE.
 * Le suivi des autorisations de prélèvement ({@code /bo/autorisations/**}) est réservé au rôle ANALYSTE.
 * Les commandes fournisseur ({@code /bo/commandes/**}) sont ouvertes aux rôles ANALYSTE et ADMIN ; la
 * validation du paiement fournisseur est refusée à la personne qui a décidé du dossier (règle du domaine).
 * La saisie des paiements reçus et leur rapprochement ({@code /bo/paiements/**}) sont réservés aux rôles
 * RECOUVREMENT et ADMIN : l'encaissement est tenu par d'autres personnes que l'analyste qui décide.
 * L'écran du recouvrement ({@code /bo/recouvrement/**}) : prélèvements, notes datées, régularisation,
 * passage en recouvrement et solde des dossiers, est réservé aux mêmes rôles RECOUVREMENT et ADMIN.
 * Le tableau de bord des associés ({@code /bo/pilotage}) et la liste des apports en capital
 * ({@code GET /bo/capital}) sont ouverts aux rôles ASSOCIE et ADMIN ; la saisie d'un apport en capital
 * ({@code POST /bo/capital}) est réservée au rôle ADMIN.
 * Le suivi des pré-inscriptions ({@code GET /bo/inscriptions} : nombre d'inscrits et de demandes complètes)
 * est ouvert aux rôles ASSOCIE et ADMIN ; l'export CSV ({@code /bo/inscriptions/export.csv}), qui contient
 * des données personnelles, est réservé au rôle ADMIN.
 * Les comptes sont fournis par le module identite (bean {@code UserDetailsService}).
 *
 * <p>Les autres pages restent publiques, dont la pré-inscription ({@code /inscription}). CSRF actif partout ; en-têtes de sécurité par défaut de
 * Spring Security, plus une politique de référent.
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
    @Order(1)
    public SecurityFilterChain espaceClientFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/client", "/client/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/client/connexion", "/client/connexion/**").permitAll()
                .anyRequest().hasRole("CLIENT")
            )
            .exceptionHandling(e -> e
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/client/connexion"))
            )
            .logout(logout -> logout
                .logoutUrl("/client/logout")
                .logoutSuccessUrl("/client/connexion?deconnexion")
                .permitAll()
            )
            .headers(SecurityConfig::entetes);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/bo/login").permitAll()
                .requestMatchers("/bo/fournisseurs", "/bo/fournisseurs/**", "/bo/produits", "/bo/produits/**").hasRole("ADMIN")
                .requestMatchers("/bo/justificatifs", "/bo/justificatifs/**").hasAnyRole("ANALYSTE", "ADMIN")
                .requestMatchers("/bo/dossiers", "/bo/dossiers/**").hasRole("ANALYSTE")
                .requestMatchers("/bo/autorisations", "/bo/autorisations/**").hasRole("ANALYSTE")
                .requestMatchers("/bo/commandes", "/bo/commandes/**").hasAnyRole("ANALYSTE", "ADMIN")
                .requestMatchers("/bo/paiements", "/bo/paiements/**").hasAnyRole("RECOUVREMENT", "ADMIN")
                .requestMatchers("/bo/recouvrement", "/bo/recouvrement/**").hasAnyRole("RECOUVREMENT", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/bo/capital").hasAnyRole("ASSOCIE", "ADMIN")
                .requestMatchers("/bo/capital", "/bo/capital/**").hasRole("ADMIN")
                .requestMatchers("/bo/pilotage", "/bo/pilotage/**").hasAnyRole("ASSOCIE", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/bo/inscriptions").hasAnyRole("ASSOCIE", "ADMIN")
                .requestMatchers("/bo/inscriptions", "/bo/inscriptions/**").hasRole("ADMIN")
                .requestMatchers("/bo/demonstration", "/bo/demonstration/**").hasRole("ADMIN")
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
            .headers(SecurityConfig::entetes);
        return http.build();
    }

    private static void entetes(HeadersConfigurer<HttpSecurity> headers) {
        headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.SAME_ORIGIN));
    }
}
