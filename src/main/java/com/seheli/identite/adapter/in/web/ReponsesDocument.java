package com.seheli.identite.adapter.in.web;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.seheli.identite.application.ContenuDocument;
import com.seheli.identite.domain.Document;

/**
 * Réponse HTTP d'un justificatif : type MIME du format reconnu au contenu, affichage dans le
 * navigateur, nom de fichier générique (type et date, sans donnée personnelle), jamais mis en cache.
 */
final class ReponsesDocument {

    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private ReponsesDocument() {
    }

    static ResponseEntity<byte[]> servir(ContenuDocument c) {
        Document d = c.document();
        String nom = d.type().name().toLowerCase(Locale.ROOT).replace('_', '-') + "-"
                + JOUR.format(d.recuLe().atZone(ZoneOffset.UTC)) + "." + d.format().extension();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.format().typeMime()))
                .contentLength(c.contenu().length)
                .header("Content-Disposition", ContentDisposition.inline().filename(nom).build().toString())
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(c.contenu());
    }
}
