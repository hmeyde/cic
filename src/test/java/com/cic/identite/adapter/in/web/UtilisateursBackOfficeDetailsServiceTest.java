package com.cic.identite.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.cic.identite.application.CompteBackOfficeService;
import com.cic.identite.application.UtilisateursEnMemoire;
import com.cic.identite.domain.RoleBackOffice;
import com.cic.identite.domain.UtilisateurBackOffice;

class UtilisateursBackOfficeDetailsServiceTest {

    private final UtilisateursEnMemoire repository = new UtilisateursEnMemoire();
    private final CompteBackOfficeService comptes = new CompteBackOfficeService(repository, new BCryptPasswordEncoder(4));
    private final UtilisateursBackOfficeDetailsService service = new UtilisateursBackOfficeDetailsService(comptes);

    @Test
    void le_role_devient_une_autorite_spring_security() {
        comptes.creer("recouvreur", "motdepasse-solide", RoleBackOffice.RECOUVREMENT);

        UserDetails details = service.loadUserByUsername("Recouvreur");

        assertThat(details.getUsername()).isEqualTo("recouvreur");
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_RECOUVREMENT");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void un_compte_inactif_est_desactive() {
        UtilisateurBackOffice u = comptes.creer("associe", "motdepasse-solide", RoleBackOffice.ASSOCIE);
        u.desactiver();
        repository.enregistrer(u);

        assertThat(service.loadUserByUsername("associe").isEnabled()).isFalse();
    }

    @Test
    void un_compte_inconnu_est_refuse_sans_reveler_l_identifiant() {
        assertThatThrownBy(() -> service.loadUserByUsername("personne.x"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageNotContaining("personne.x");
    }
}
