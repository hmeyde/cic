package com.seheli.paiement.application;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.seheli.commun.domain.Montant;
import com.seheli.paiement.domain.InstructionsApport;
import com.seheli.paiement.domain.ReferenceApport;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.CleParametre;

/**
 * Instructions de paiement de l'apport par Masrvi : montant, code marchand de Seheli (paramètre
 * {@code masrvi.code.marchand}, modifiable sans redéploiement) et référence unique du dossier.
 */
@Service
public class InstructionsApportService {

    private final ParametreService parametres;

    public InstructionsApportService(ParametreService parametres) {
        this.parametres = parametres;
    }

    /**
     * @param numeroContrat numéro du contrat du dossier accepté
     * @param apport apport fixé au contrat
     */
    public InstructionsApport pour(String numeroContrat, Montant apport) {
        Objects.requireNonNull(apport, "apport");
        return new InstructionsApport(apport, parametres.texte(CleParametre.MASRVI_CODE_MARCHAND).strip(),
                ReferenceApport.pourContrat(numeroContrat));
    }
}
