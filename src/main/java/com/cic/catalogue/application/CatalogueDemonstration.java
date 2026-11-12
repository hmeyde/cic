package com.cic.catalogue.application;

import java.math.BigDecimal;
import java.util.List;

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
 * Profils dev et demo : charge au démarrage le catalogue de démonstration — 3 fournisseurs et
 * 15 produits (téléphones et électroménager), prix publics en MRU proches du marché de Nouakchott.
 *
 * <p>Le chargement est idempotent : un fournisseur est créé seulement si aucun fournisseur ne porte
 * déjà ce nom, un produit seulement si sa référence n'existe pas. Une base qui contient déjà une
 * partie du catalogue est donc complétée, sans doublon ni écrasement des modifications faites au
 * back-office. Le produit TEL-DEMO-01 reprend l'exemple chiffré de référence : prix public
 * 20 000 MRU, remise 8 %.
 */
@Component
@Profile({"dev", "demo"})
class CatalogueDemonstration implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogueDemonstration.class);

    private static final String NOUAKCHOTT_MOBILES = "Nouakchott Mobiles (démo)";
    private static final String EQUIPEMENT_MAISON = "Équipement Maison (démo)";
    private static final String SAHEL_ELECTRONIQUE = "Sahel Électronique (démo)";

    private static final List<FournisseurDemo> FOURNISSEURS = List.of(
            new FournisseurDemo(NOUAKCHOTT_MOBILES, "8"),
            new FournisseurDemo(EQUIPEMENT_MAISON, "6"),
            new FournisseurDemo(SAHEL_ELECTRONIQUE, "10"));

    private static final List<ProduitDemo> PRODUITS = List.of(
            // Téléphones
            telephone("TEL-DEMO-01", "Galaxy A35 128 Go", "Samsung", NOUAKCHOTT_MOBILES, 20_000),
            telephone("TEL-DEMO-02", "Redmi Note 13 256 Go", "Xiaomi", NOUAKCHOTT_MOBILES, 12_500),
            telephone("TEL-DEMO-03", "iPhone 15 128 Go", "Apple", NOUAKCHOTT_MOBILES, 42_000),
            telephone("TEL-DEMO-04", "Galaxy A15 128 Go", "Samsung", NOUAKCHOTT_MOBILES, 9_500),
            telephone("TEL-DEMO-05", "Galaxy S24 256 Go", "Samsung", NOUAKCHOTT_MOBILES, 46_000),
            telephone("TEL-DEMO-06", "Spark 20 Pro 256 Go", "Tecno", SAHEL_ELECTRONIQUE, 7_500),
            telephone("TEL-DEMO-07", "Hot 40 Pro 256 Go", "Infinix", SAHEL_ELECTRONIQUE, 7_000),
            telephone("TEL-DEMO-08", "iPhone 13 128 Go", "Apple", SAHEL_ELECTRONIQUE, 30_000),
            // Électroménager
            electromenager("ELM-DEMO-01", "Réfrigérateur combiné 300 L", "LG", EQUIPEMENT_MAISON, 28_000),
            electromenager("ELM-DEMO-02", "Climatiseur split 12 000 BTU", "Gree", EQUIPEMENT_MAISON, 24_000),
            electromenager("ELM-DEMO-03", "Machine à laver 7 kg", "Beko", EQUIPEMENT_MAISON, 18_500),
            electromenager("ELM-DEMO-04", "Congélateur coffre 250 L", "Hisense", EQUIPEMENT_MAISON, 17_000),
            electromenager("ELM-DEMO-05", "Cuisinière à gaz 4 feux avec four", "Beko", EQUIPEMENT_MAISON, 13_500),
            electromenager("ELM-DEMO-06", "Climatiseur split 18 000 BTU", "Midea", SAHEL_ELECTRONIQUE, 32_000),
            electromenager("ELM-DEMO-07", "Fontaine à eau chaude et froide", "Midea", SAHEL_ELECTRONIQUE, 6_500));

    private final FournisseurRepository fournisseurs;
    private final ProduitRepository produits;

    CatalogueDemonstration(FournisseurRepository fournisseurs, ProduitRepository produits) {
        this.fournisseurs = fournisseurs;
        this.produits = produits;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int fournisseursCrees = 0;
        for (FournisseurDemo f : FOURNISSEURS) {
            if (fournisseurs.parNom(f.nom()).isEmpty()) {
                fournisseurs.enregistrer(Fournisseur.nouveau(f.nom(), new BigDecimal(f.remisePct())));
                fournisseursCrees++;
            }
        }
        int produitsCrees = 0;
        for (ProduitDemo p : PRODUITS) {
            if (produits.parReference(p.reference()).isPresent()) {
                continue;
            }
            Fournisseur fournisseur = fournisseurs.parNom(p.fournisseur()).orElseThrow();
            produits.enregistrer(Produit.nouveau(p.reference(), p.designation(), p.marque(), p.categorie(),
                    fournisseur.id(), Montant.de(p.prixPublic())));
            produitsCrees++;
        }
        if (fournisseursCrees > 0 || produitsCrees > 0) {
            LOG.info("Catalogue de démonstration chargé : {} fournisseur(s) et {} produit(s) créés",
                    fournisseursCrees, produitsCrees);
        }
    }

    private static ProduitDemo telephone(String reference, String designation, String marque, String fournisseur,
            long prixPublic) {
        return new ProduitDemo(reference, designation, marque, CategorieProduit.TELEPHONE, fournisseur, prixPublic);
    }

    private static ProduitDemo electromenager(String reference, String designation, String marque,
            String fournisseur, long prixPublic) {
        return new ProduitDemo(reference, designation, marque, CategorieProduit.ELECTROMENAGER, fournisseur,
                prixPublic);
    }

    private record FournisseurDemo(String nom, String remisePct) {
    }

    private record ProduitDemo(String reference, String designation, String marque, CategorieProduit categorie,
            String fournisseur, long prixPublic) {
    }
}
