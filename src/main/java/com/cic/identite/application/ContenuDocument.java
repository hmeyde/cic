package com.cic.identite.application;

import com.cic.identite.domain.Document;

/** Un justificatif et son contenu, prêt à être servi. */
public record ContenuDocument(Document document, byte[] contenu) {
}
