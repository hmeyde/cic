package com.cic.identite.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.cic.identite.application.ClientsEnMemoire;
import com.cic.identite.application.DocumentService;
import com.cic.identite.application.DocumentsEnMemoire;
import com.cic.identite.application.StockageEnMemoire;
import com.cic.identite.domain.Client;
import com.cic.identite.domain.Document;
import com.cic.identite.domain.TypeDocument;

class JustificatifsClientControllerTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-11T09:00:00Z");
    private static final byte[] PDF = "%PDF-1.7\ncontenu".getBytes(StandardCharsets.US_ASCII);

    private final Clock clock = Clock.fixed(MAINTENANT, ZoneId.of("Africa/Nouakchott"));
    private final ClientsEnMemoire clients = new ClientsEnMemoire();
    private final DocumentsEnMemoire documents = new DocumentsEnMemoire();
    private final DocumentService service = new DocumentService(clients, documents, new StockageEnMemoire(), clock);
    private final JustificatifsClientController controller = new JustificatifsClientController(service, clock);
    private final Client client = Client.nouveau("1234567890", "Ould Ahmed", "Sidi", LocalDate.of(1990, 1, 15),
            "22123456", MAINTENANT);
    private final Client autre = Client.nouveau("1234567891", "Mint Salem", "Aicha", LocalDate.of(1992, 5, 2),
            "22123457", MAINTENANT);
    private final Principal principal = () -> client.id().toString();

    JustificatifsClientControllerTest() {
        clients.enregistrer(client);
        clients.enregistrer(autre);
    }

    private static MockMultipartFile fichier(byte[] contenu) {
        return new MockMultipartFile("fichier", "carte-sidi.pdf", "application/pdf", contenu);
    }

    @Test
    void le_client_envoie_puis_revoit_son_justificatif() throws IOException {
        RedirectAttributesModelMap redirection = new RedirectAttributesModelMap();

        String vue = controller.envoyer(principal, "PIECE_IDENTITE", fichier(PDF), new ExtendedModelMap(), redirection);

        assertThat(vue).isEqualTo("redirect:/client/justificatifs");
        assertThat(redirection.getFlashAttributes().get("succes")).isEqualTo("client.justificatifs.envoye");

        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(controller.liste(principal, model)).isEqualTo("client/justificatifs");
        @SuppressWarnings("unchecked")
        List<LigneDocument> lignes = (List<LigneDocument>) model.get("documents");
        assertThat(lignes).singleElement().satisfies(l -> {
            assertThat(l.type()).isEqualTo(TypeDocument.PIECE_IDENTITE);
            assertThat(l.format()).isEqualTo("PDF");
            assertThat(l.recuLe()).isEqualTo("11/11/2026 09:00");
            assertThat(l.tailleKo()).isEqualTo(1);
        });
        assertThat(model.get("typesRecus")).isEqualTo(Set.of(TypeDocument.PIECE_IDENTITE));

        ResponseEntity<byte[]> reponse = controller.voir(principal, lignes.getFirst().id());
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(reponse.getHeaders().getFirst("Content-Disposition"))
                .isEqualTo("inline; filename=\"piece-identite-2026-11-11.pdf\"");
        assertThat(reponse.getHeaders().getCacheControl()).contains("no-store");
        assertThat(reponse.getBody()).isEqualTo(PDF);
    }

    @Test
    void reaffiche_la_page_avec_l_erreur() throws IOException {
        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(controller.envoyer(principal, "", fichier(PDF), model, new RedirectAttributesModelMap()))
                .isEqualTo("client/justificatifs");
        assertThat(model).containsEntry("erreur", "client.justificatifs.type.obligatoire");

        model = new ExtendedModelMap();
        controller.envoyer(principal, "RIB", fichier(new byte[0]), model, new RedirectAttributesModelMap());
        assertThat(model).containsEntry("erreur", "client.justificatifs.fichier.obligatoire")
                .containsEntry("typeChoisi", "RIB");

        model = new ExtendedModelMap();
        controller.envoyer(principal, "RIB", null, model, new RedirectAttributesModelMap());
        assertThat(model).containsEntry("erreur", "client.justificatifs.fichier.obligatoire");

        model = new ExtendedModelMap();
        controller.envoyer(principal, "RIB", fichier("GIF89a".getBytes(StandardCharsets.US_ASCII)), model,
                new RedirectAttributesModelMap());
        assertThat(model).containsEntry("erreur", "client.justificatifs.format.refuse");

        model = new ExtendedModelMap();
        controller.envoyer(principal, "RIB", fichier(new byte[(int) Document.TAILLE_MAX_OCTETS + 1]), model,
                new RedirectAttributesModelMap());
        assertThat(model).containsEntry("erreur", "client.justificatifs.fichier.trop-volumineux");

        assertThat(documents.parClient(client.id())).isEmpty();
    }

    @Test
    void le_document_d_un_autre_client_est_introuvable() {
        Document d = service.envoyer(autre.id(), TypeDocument.RIB, PDF);

        assertThatThrownBy(() -> controller.voir(principal, d.id()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
