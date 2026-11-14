package com.seheli.catalogue.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.catalogue.domain.Fournisseur;
import com.seheli.catalogue.domain.FournisseurRepository;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.CleParametre;
import com.seheli.parametrage.domain.Parametres;

/**
 * Création et modification des fournisseurs (back-office, rôle ADMIN).
 *
 * <p>La remise négociée doit rester dans les bornes de la table {@code parametre}
 * ({@code remise.fournisseur.min.pct} – {@code remise.fournisseur.max.pct}). Le nom est unique
 * sans tenir compte de la casse. Une nouvelle remise ne vaut que pour les achats futurs.
 */
@Service
public class FournisseurService {

    private final FournisseurRepository repository;
    private final ParametreService parametres;

    public FournisseurService(FournisseurRepository repository, ParametreService parametres) {
        this.repository = repository;
        this.parametres = parametres;
    }

    /** Tous les fournisseurs, triés par nom. */
    @Transactional(readOnly = true)
    public List<Fournisseur> lister() {
        return repository.tous();
    }

    /** @throws FournisseurIntrouvableException si l'identifiant est inconnu */
    @Transactional(readOnly = true)
    public Fournisseur parId(UUID id) {
        return repository.parId(id).orElseThrow(FournisseurIntrouvableException::new);
    }

    /**
     * Crée un fournisseur actif.
     *
     * @throws RemiseHorsBornesException si la remise sort des bornes en vigueur
     * @throws NomFournisseurDejaUtiliseException si le nom est déjà pris
     */
    @Transactional
    public Fournisseur creer(String nom, BigDecimal remiseNegocieePct) {
        verifierRemise(remiseNegocieePct);
        Fournisseur fournisseur = Fournisseur.nouveau(nom, remiseNegocieePct);
        verifierNomLibre(fournisseur.nom(), null);
        repository.enregistrer(fournisseur);
        return fournisseur;
    }

    /**
     * Modifie le nom, la remise et l'état d'un fournisseur.
     *
     * <p>Les bornes de remise ne sont contrôlées que si la remise change : un fournisseur dont la
     * remise est sortie des bornes après une modification des paramètres peut encore être renommé
     * ou désactivé.
     *
     * @throws FournisseurIntrouvableException si l'identifiant est inconnu
     * @throws RemiseHorsBornesException si la nouvelle remise sort des bornes en vigueur
     * @throws NomFournisseurDejaUtiliseException si le nom est pris par un autre fournisseur
     */
    @Transactional
    public Fournisseur modifier(UUID id, String nom, BigDecimal remiseNegocieePct, boolean actif) {
        Objects.requireNonNull(remiseNegocieePct, "remiseNegocieePct");
        Fournisseur fournisseur = parId(id);
        if (remiseNegocieePct.compareTo(fournisseur.remiseNegocieePct()) != 0) {
            verifierRemise(remiseNegocieePct);
            fournisseur.negocierRemise(remiseNegocieePct);
        }
        fournisseur.renommer(nom);
        verifierNomLibre(fournisseur.nom(), fournisseur.id());
        if (actif) {
            fournisseur.activer();
        } else {
            fournisseur.desactiver();
        }
        repository.enregistrer(fournisseur);
        return fournisseur;
    }

    /** Bornes de remise en vigueur, pour l'affichage du formulaire. */
    public BornesRemise bornesRemise() {
        Parametres p = parametres.courants();
        return new BornesRemise(
                p.pourcentage(CleParametre.REMISE_FOURNISSEUR_MIN_PCT),
                p.pourcentage(CleParametre.REMISE_FOURNISSEUR_MAX_PCT));
    }

    private void verifierRemise(BigDecimal remisePct) {
        Objects.requireNonNull(remisePct, "remiseNegocieePct");
        BornesRemise bornes = bornesRemise();
        if (!bornes.contient(remisePct)) {
            throw new RemiseHorsBornesException(remisePct, bornes.minPct(), bornes.maxPct());
        }
    }

    private void verifierNomLibre(String nom, UUID idCourant) {
        repository.parNom(nom)
                .filter(existant -> !existant.id().equals(idCourant))
                .ifPresent(existant -> {
                    throw new NomFournisseurDejaUtiliseException();
                });
    }
}
