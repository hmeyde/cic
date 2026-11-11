package com.cic.catalogue.application;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cic.catalogue.domain.CategorieProduit;
import com.cic.catalogue.domain.Fournisseur;
import com.cic.catalogue.domain.FournisseurRepository;
import com.cic.catalogue.domain.Produit;
import com.cic.catalogue.domain.ProduitRepository;
import com.cic.commun.domain.Montant;

/**
 * Profils dev et demo : crée au démarrage un catalogue de démonstration (deux fournisseurs, quelques
 * téléphones et appareils d'électroménager) si aucun fournisseur n'existe encore. Le produit
 * TEL-DEMO-01 reprend l'exemple chiffré de référence : prix public 20 000 MRU, remise 8 %.
 */
@Component
@Profile({"dev", "demo"})
class CatalogueDemonstration implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogueDemonstration.class);

    private final FournisseurRepository fournisseurs;
    private final ProduitRepository produits;

    CatalogueDemonstration(FournisseurRepository fournisseurs, ProduitRepository produits) {
        this.fournisseurs = fournisseurs;
        this.produits = produits;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!fournisseurs.tous().isEmpty()) {
            return;
        }
        Fournisseur telephonie = fournisseur("Nouakchott Mobiles (démo)", "8");
        Fournisseur maison = fournisseur("Équipement Maison (démo)", "6");

        produit("TEL-DEMO-01", "Galaxy A35 128 Go", "Samsung", CategorieProduit.TELEPHONE, telephonie, 20_000);
        produit("TEL-DEMO-02", "Redmi Note 13 256 Go", "Xiaomi", CategorieProduit.TELEPHONE, telephonie, 12_500);
        produit("TEL-DEMO-03", "iPhone 15 128 Go", "Apple", CategorieProduit.TELEPHONE, telephonie, 42_000);
        produit("ELM-DEMO-01", "Réfrigérateur combiné 300 L", "LG", CategorieProduit.ELECTROMENAGER, maison, 28_000);
        produit("ELM-DEMO-02", "Climatiseur split 12 000 BTU", "Gree", CategorieProduit.ELECTROMENAGER, maison, 24_000);
        produit("ELM-DEMO-03", "Machine à laver 7 kg", "Beko", CategorieProduit.ELECTROMENAGER, maison, 18_500);
        LOG.info("Catalogue de démonstration créé");
    }

    private Fournisseur fournisseur(String nom, String remisePct) {
        Fournisseur f = Fournisseur.nouveau(nom, new BigDecimal(remisePct));
        fournisseurs.enregistrer(f);
        return f;
    }

    private void produit(String reference, String designation, String marque, CategorieProduit categorie,
            Fournisseur fournisseur, long prixPublic) {
        produits.enregistrer(Produit.nouveau(reference, designation, marque, categorie, fournisseur.id(),
                Montant.de(prixPublic)));
    }
}
