package com.cic.identite.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.cic.identite.domain.CodeConnexion;
import com.cic.identite.domain.CodeConnexionRepository;

/** Copie à l'enregistrement et à la lecture, comme une vraie base. */
public class CodesEnMemoire implements CodeConnexionRepository {

    private final Map<String, CodeConnexion> codes = new HashMap<>();

    @Override
    public void enregistrer(CodeConnexion c) {
        codes.put(c.telephone(), copie(c));
    }

    @Override
    public Optional<CodeConnexion> parTelephone(String telephone) {
        return Optional.ofNullable(codes.get(telephone)).map(CodesEnMemoire::copie);
    }

    private static CodeConnexion copie(CodeConnexion c) {
        return CodeConnexion.reconstituer(c.telephone(), c.codeHash(), c.emisLe(), c.expireLe(), c.essaisMax(),
                c.essais(), c.estUtilise());
    }
}
