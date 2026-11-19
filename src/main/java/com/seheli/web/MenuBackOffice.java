package com.seheli.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Menu latéral du back-office : les entrées visibles dépendent du rôle (mêmes règles que
 * {@code SecurityConfig}) et l'entrée active se déduit du chemin de la page (préfixe le plus long).
 *
 * @param identifiant identifiant de connexion du membre du back-office
 * @param role        rôle sans préfixe {@code ROLE_} (ANALYSTE, RECOUVREMENT, ADMIN, ASSOCIE)
 * @param groupes     groupes non vides, dans l'ordre d'affichage
 */
public record MenuBackOffice(String identifiant, String role, List<Groupe> groupes) {

    public static final Set<String> ROLES = Set.of("ANALYSTE", "RECOUVREMENT", "ADMIN", "ASSOCIE");

    /** Groupes du menu, dans l'ordre d'affichage ; le code sert de clé i18n {@code bo.menu.groupe.*}. */
    public enum CodeGroupe { GENERAL, ANALYSE, RECOUVREMENT, PILOTAGE, ADMINISTRATION }

    /** Entrées du menu ; le code sert de clé i18n {@code bo.menu.<code>} et d'icône. */
    public enum Entree {
        ACCUEIL(CodeGroupe.GENERAL, "/bo", Set.of("ANALYSTE", "RECOUVREMENT", "ADMIN", "ASSOCIE"), false),
        DOSSIERS(CodeGroupe.ANALYSE, "/bo/dossiers", Set.of("ANALYSTE"), false),
        JUSTIFICATIFS(CodeGroupe.ANALYSE, "/bo/justificatifs", Set.of("ANALYSTE", "ADMIN"), false),
        AUTORISATIONS(CodeGroupe.ANALYSE, "/bo/autorisations", Set.of("ANALYSTE"), false),
        COMMANDES(CodeGroupe.ANALYSE, "/bo/commandes", Set.of("ANALYSTE", "ADMIN"), false),
        RECOUVREMENT(CodeGroupe.RECOUVREMENT, "/bo/recouvrement", Set.of("RECOUVREMENT", "ADMIN"), false),
        PAIEMENTS(CodeGroupe.RECOUVREMENT, "/bo/paiements", Set.of("RECOUVREMENT", "ADMIN"), false),
        IMPORT_PAIEMENTS(CodeGroupe.RECOUVREMENT, "/bo/paiements/import", Set.of("RECOUVREMENT", "ADMIN"), false),
        PILOTAGE(CodeGroupe.PILOTAGE, "/bo/pilotage", Set.of("ASSOCIE", "ADMIN"), false),
        CAPITAL(CodeGroupe.PILOTAGE, "/bo/capital", Set.of("ASSOCIE", "ADMIN"), false),
        INSCRIPTIONS(CodeGroupe.PILOTAGE, "/bo/inscriptions", Set.of("ASSOCIE", "ADMIN"), false),
        FOURNISSEURS(CodeGroupe.ADMINISTRATION, "/bo/fournisseurs", Set.of("ADMIN"), false),
        PRODUITS(CodeGroupe.ADMINISTRATION, "/bo/produits", Set.of("ADMIN"), false),
        DEMONSTRATION(CodeGroupe.ADMINISTRATION, "/bo/demonstration/reinitialisation", Set.of("ADMIN"), true);

        private final CodeGroupe groupe;
        private final String chemin;
        private final Set<String> roles;
        private final boolean demonstrationSeulement;

        Entree(CodeGroupe groupe, String chemin, Set<String> roles, boolean demonstrationSeulement) {
            this.groupe = groupe;
            this.chemin = chemin;
            this.roles = roles;
            this.demonstrationSeulement = demonstrationSeulement;
        }

        public String chemin() {
            return chemin;
        }

        boolean visiblePour(String role, boolean demonstration) {
            return roles.contains(role) && (demonstration || !demonstrationSeulement);
        }

        boolean couvre(String c) {
            return c.equals(chemin) || (this != ACCUEIL && c.startsWith(chemin + "/"));
        }
    }

    public record Lien(Entree entree, String chemin, boolean actif) {
        public String code() {
            return entree.name();
        }
    }

    public record Groupe(CodeGroupe code, List<Lien> liens) {
    }

    /** Menu d'un membre du back-office ; vide pour un rôle inconnu ou hors du back-office connecté. */
    public static Optional<MenuBackOffice> pour(String chemin, String identifiant, Collection<String> roles,
            boolean demonstration) {
        if (chemin == null || identifiant == null) {
            return Optional.empty();
        }
        String c = chemin.endsWith("/") && chemin.length() > 1 ? chemin.substring(0, chemin.length() - 1) : chemin;
        if (!(c.equals("/bo") || c.startsWith("/bo/")) || c.equals("/bo/login")) {
            return Optional.empty();
        }
        Optional<String> role = roles.stream().filter(ROLES::contains).findFirst();
        if (role.isEmpty()) {
            return Optional.empty();
        }
        List<Entree> visibles = Arrays.stream(Entree.values())
                .filter(e -> e.visiblePour(role.get(), demonstration)).toList();
        Optional<Entree> active = visibles.stream().filter(e -> e.couvre(c))
                .max(Comparator.comparingInt(e -> e.chemin.length()));
        List<Groupe> groupes = new ArrayList<>();
        for (CodeGroupe g : CodeGroupe.values()) {
            List<Lien> liens = visibles.stream().filter(e -> e.groupe == g)
                    .map(e -> new Lien(e, e.chemin, active.filter(e::equals).isPresent())).toList();
            if (!liens.isEmpty()) {
                groupes.add(new Groupe(g, liens));
            }
        }
        return Optional.of(new MenuBackOffice(identifiant, role.get(), List.copyOf(groupes)));
    }

    /** Liens du menu hors accueil, pour les raccourcis de la page d'accueil du back-office. */
    public List<Lien> raccourcis() {
        return groupes.stream().filter(g -> g.code() != CodeGroupe.GENERAL).flatMap(g -> g.liens().stream()).toList();
    }
}
