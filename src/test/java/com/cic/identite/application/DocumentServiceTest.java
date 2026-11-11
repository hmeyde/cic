package com.cic.identite.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cic.identite.domain.Client;
import com.cic.identite.domain.Document;
import com.cic.identite.domain.FormatDocument;
import com.cic.identite.domain.TypeDocument;

class DocumentServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-11T09:00:00Z");
    static final byte[] PDF = "%PDF-1.7\ncontenu".getBytes(StandardCharsets.US_ASCII);
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 1, 2, 3};

    private final ClientsEnMemoire clients = new ClientsEnMemoire();
    private final DocumentsEnMemoire documents = new DocumentsEnMemoire();
    private final StockageEnMemoire stockage = new StockageEnMemoire();
    private final DocumentService service = new DocumentService(clients, documents, stockage,
            Clock.fixed(MAINTENANT, ZoneId.of("Africa/Nouakchott")));
    private final Client client = Client.nouveau("1234567890", "Ould Ahmed", "Sidi", LocalDate.of(1990, 1, 15),
            "22123456", MAINTENANT);
    private final Client autre = Client.nouveau("1234567891", "Mint Salem", "Aicha", LocalDate.of(1992, 5, 2),
            "22123457", MAINTENANT);

    DocumentServiceTest() {
        clients.enregistrer(client);
        clients.enregistrer(autre);
    }

    @Test
    void enregistre_le_justificatif_et_son_contenu() {
        Document d = service.envoyer(client.id(), TypeDocument.PIECE_IDENTITE, PDF);

        assertThat(d.clientId()).isEqualTo(client.id());
        assertThat(d.format()).isEqualTo(FormatDocument.PDF);
        assertThat(d.tailleOctets()).isEqualTo(PDF.length);
        assertThat(d.recuLe()).isEqualTo(MAINTENANT);
        assertThat(service.parClient(client.id())).containsExactly(d);
        assertThat(service.contenuPourClient(client.id(), d.id()))
                .hasValueSatisfying(c -> assertThat(c.contenu()).isEqualTo(PDF));
    }

    @Test
    void le_format_vient_du_contenu() {
        assertThat(service.envoyer(client.id(), TypeDocument.RIB, PNG).format()).isEqualTo(FormatDocument.PNG);
        assertThatThrownBy(() -> service.envoyer(client.id(), TypeDocument.RIB,
                "MZ executable".getBytes(StandardCharsets.US_ASCII)))
                .isInstanceOf(FormatDocumentRefuseException.class);
    }

    @Test
    void refuse_un_fichier_vide_ou_de_plus_de_cinq_mo() {
        assertThatThrownBy(() -> service.envoyer(client.id(), TypeDocument.RIB, new byte[0]))
                .isInstanceOf(DocumentVideException.class);
        assertThatThrownBy(() -> service.envoyer(client.id(), TypeDocument.RIB, null))
                .isInstanceOf(DocumentVideException.class);

        byte[] tropGros = Arrays.copyOf(PDF, (int) Document.TAILLE_MAX_OCTETS + 1);
        assertThatThrownBy(() -> service.envoyer(client.id(), TypeDocument.RIB, tropGros))
                .isInstanceOf(DocumentTropVolumineuxException.class);

        byte[] limite = Arrays.copyOf(PDF, (int) Document.TAILLE_MAX_OCTETS);
        assertThat(service.envoyer(client.id(), TypeDocument.RIB, limite).tailleOctets())
                .isEqualTo(Document.TAILLE_MAX_OCTETS);
        assertThat(stockage.nombre()).isEqualTo(1);
    }

    @Test
    void limite_le_nombre_de_documents_par_type() {
        for (int i = 0; i < DocumentService.MAX_PAR_TYPE; i++) {
            service.envoyer(client.id(), TypeDocument.BULLETIN_PAIE, PDF);
        }
        assertThatThrownBy(() -> service.envoyer(client.id(), TypeDocument.BULLETIN_PAIE, PDF))
                .isInstanceOf(TropDeDocumentsException.class);
        service.envoyer(client.id(), TypeDocument.RIB, PDF);
    }

    @Test
    void un_client_ne_voit_pas_les_documents_d_un_autre() {
        Document d = service.envoyer(autre.id(), TypeDocument.PIECE_IDENTITE, PDF);

        assertThat(service.parClient(client.id())).isEmpty();
        assertThat(service.contenuPourClient(client.id(), d.id())).isEmpty();
        assertThat(service.contenuPourClient(client.id(), UUID.randomUUID())).isEmpty();
    }

    @Test
    void le_back_office_voit_tous_les_documents_avec_le_nom_du_client() {
        Document d = service.envoyer(autre.id(), TypeDocument.PIECE_IDENTITE, PDF);

        assertThat(service.derniersRecus(10)).singleElement().satisfies(r -> {
            assertThat(r.document()).isEqualTo(d);
            assertThat(r.prenomClient()).isEqualTo("Aicha");
            assertThat(r.nomClient()).isEqualTo("Mint Salem");
        });
        assertThat(service.contenuPourBackOffice(d.id()))
                .hasValueSatisfying(c -> assertThat(c.contenu()).isEqualTo(PDF));
    }

    @Test
    void refuse_un_client_inconnu() {
        assertThatThrownBy(() -> service.envoyer(UUID.randomUUID(), TypeDocument.RIB, PDF))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
