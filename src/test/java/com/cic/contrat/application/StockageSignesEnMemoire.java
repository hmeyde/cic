package com.cic.contrat.application;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.cic.contrat.domain.StockageDocumentsSignes;

public class StockageSignesEnMemoire implements StockageDocumentsSignes {

    private final Map<UUID, byte[]> contenus = new HashMap<>();

    @Override
    public void ecrire(UUID documentId, byte[] contenu) {
        contenus.put(documentId, contenu.clone());
    }

    @Override
    public byte[] lire(UUID documentId) {
        byte[] contenu = contenus.get(documentId);
        if (contenu == null) {
            throw new IllegalStateException("Contenu introuvable");
        }
        return contenu.clone();
    }

    @Override
    public void supprimer(UUID documentId) {
        contenus.remove(documentId);
    }

    public int nombre() {
        return contenus.size();
    }
}
