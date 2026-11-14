package com.seheli.identite.application;

import com.seheli.identite.domain.Document;

/** Un justificatif et son contenu, prêt à être servi. */
public record ContenuDocument(Document document, byte[] contenu) {
}
