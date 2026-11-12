package com.cic.paiement.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cic.commun.domain.ErreurMetier;
import com.cic.paiement.domain.Paiement;
import com.cic.paiement.domain.PaiementEnDoubleException;
import com.cic.paiement.domain.TypeEcart;

/**
 * Import d'un fichier CSV de paiements reçus (référence, montant, date ; voir
 * {@link LecteurCsvPaiements}).
 *
 * <p>Chaque ligne est traitée comme une saisie manuelle ({@link PaiementService#importer}), dans sa
 * propre transaction : contrôles, refus du doublon, rapprochement, journal d'audit et événement. Une
 * ligne refusée n'empêche pas d'importer les autres ; elle est signalée. Réimporter le même fichier
 * n'enregistre rien de plus (référence unique) : après correction d'un fichier en partie refusé, on
 * peut l'importer de nouveau en entier.
 */
@Service
public class ImportPaiementsService {

    /** Taille maximale d'un fichier importé, en octets (1 Mo). */
    public static final int TAILLE_MAX_OCTETS = LecteurCsvPaiements.TAILLE_MAX_OCTETS;

    /** Nombre maximal de lignes de paiement dans un fichier importé. */
    public static final int LIGNES_MAX = LecteurCsvPaiements.LIGNES_MAX;

    private static final Logger LOG = LoggerFactory.getLogger(ImportPaiementsService.class);

    private final PaiementService paiements;

    public ImportPaiementsService(PaiementService paiements) {
        this.paiements = paiements;
    }

    /**
     * @param auteur identifiant de connexion du back-office
     * @throws FichierCsvInvalideException fichier vide, trop gros ou trop long : rien n'est importé
     */
    public ResultatImport importer(byte[] contenu, String auteur) {
        Objects.requireNonNull(auteur, "auteur");
        List<LigneCsv> lignes = LecteurCsvPaiements.lire(contenu);
        List<LigneImportee> resultat = new ArrayList<>(lignes.size());
        for (LigneCsv ligne : lignes) {
            resultat.add(importer(ligne, auteur));
        }
        ResultatImport r = new ResultatImport(resultat);
        LOG.info("Import CSV de paiements par le back-office : {} lignes, {} enregistrées, {} écarts, {} ignorées",
                lignes.size(), r.enregistres(), r.ecarts().size(), r.ignorees().size());
        return r;
    }

    private LigneImportee importer(LigneCsv ligne, String auteur) {
        if (ligne.erreur().isPresent()) {
            return new LigneImportee(ligne.numero(), ligne.saisie(), IssueLigne.INVALIDE, Optional.empty(),
                    ligne.erreur());
        }
        try {
            Paiement p = paiements.importer(ligne.saisie(), auteur);
            return new LigneImportee(ligne.numero(), ligne.saisie(), issue(p), Optional.of(p), Optional.empty());
        } catch (PaiementEnDoubleException e) {
            return new LigneImportee(ligne.numero(), ligne.saisie(), IssueLigne.DEJA_ENREGISTRE,
                    e.paiementExistantId().flatMap(paiements::parId), Optional.empty());
        } catch (ErreurMetier e) {
            return new LigneImportee(ligne.numero(), ligne.saisie(), IssueLigne.INVALIDE, Optional.empty(),
                    Optional.of(e.code()));
        }
    }

    private static IssueLigne issue(Paiement p) {
        return p.ecart().map(e -> e == TypeEcart.MONTANT_DIFFERENT ? IssueLigne.MONTANT_DIFFERENT
                : IssueLigne.REFERENCE_INCONNUE).orElse(IssueLigne.CONFORME);
    }
}
