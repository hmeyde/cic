package com.seheli.recouvrement.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.contrat.application.ContratService;
import com.seheli.contrat.domain.Echeance;
import com.seheli.contrat.domain.EcheanceAPrelever;
import com.seheli.demande.application.StatutDossierService;
import com.seheli.demande.domain.StatutDossier;
import com.seheli.identite.application.ClientService;
import com.seheli.identite.domain.Client;
import com.seheli.notification.domain.Message;
import com.seheli.notification.domain.Notification;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.CleParametre;
import com.seheli.recouvrement.domain.LienWhatsApp;
import com.seheli.recouvrement.domain.Relance;
import com.seheli.recouvrement.domain.RelanceRepository;
import com.seheli.recouvrement.domain.TypeRelance;

/**
 * Rappel envoyé au client {@code relance.avant.prelevement.jours} jours avant chaque prélèvement
 * (la veille au départ) : montant, date, numéro de contrat et lien wa.me prérempli vers le WhatsApp
 * de Seheli ({@code cic.whatsapp.numero}) pour poser une question.
 *
 * <p>Sont rappelées les échéances A_VENIR dont la date tombe de demain à J+N : un passage manqué est
 * ainsi rattrapé tant que le prélèvement n'a pas eu lieu. Seuls les dossiers dont les prélèvements
 * sont en place reçoivent un rappel ({@link StatutDossier#estEnPrelevement()}). Chaque rappel est
 * enregistré comme {@link Relance} dans la même transaction que l'envoi : une échéance n'est rappelée
 * qu'une fois, même si le job repasse.
 *
 * <p>La langue du client n'étant pas encore enregistrée, le message est rédigé en français.
 */
@Service
public class RappelPrelevementService {

    private static final Logger LOG = LoggerFactory.getLogger(RappelPrelevementService.class);
    private static final Locale LANGUE = Locale.FRENCH;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ContratService contrats;
    private final StatutDossierService dossiers;
    private final ClientService clients;
    private final RelanceRepository relances;
    private final Notification notification;
    private final ParametreService parametres;
    private final MessageSource messages;
    private final Clock clock;

    public RappelPrelevementService(ContratService contrats, StatutDossierService dossiers, ClientService clients,
            RelanceRepository relances, Notification notification, ParametreService parametres,
            MessageSource messages, Clock clock) {
        this.contrats = contrats;
        this.dossiers = dossiers;
        this.clients = clients;
        this.relances = relances;
        this.notification = notification;
        this.parametres = parametres;
        this.messages = messages;
        this.clock = clock;
    }

    /** Échéances A_VENIR prélevées de demain à J+{@code relance.avant.prelevement.jours}, par date. */
    @Transactional(readOnly = true)
    public List<EcheanceAPrelever> echeancesARappeler() {
        LocalDate aujourdhui = LocalDate.now(clock);
        int jours = Math.max(1, parametres.entier(CleParametre.RELANCE_AVANT_PRELEVEMENT_JOURS));
        return contrats.echeancesAVenirEntre(aujourdhui.plusDays(1), aujourdhui.plusDays(jours));
    }

    /**
     * Envoie le rappel de cette échéance s'il n'a pas déjà été envoyé et que le dossier est en prélèvement.
     *
     * @return {@code true} si le message vient d'être envoyé
     */
    @Transactional
    public boolean rappeler(EcheanceAPrelever a) {
        Objects.requireNonNull(a, "a");
        Echeance echeance = a.echeance();
        if (relances.existe(echeance.id(), TypeRelance.AVANT_PRELEVEMENT)) {
            return false;
        }
        Optional<StatutDossier> statut = dossiers.statut(a.demandeId());
        if (statut.isEmpty() || !statut.get().estEnPrelevement()) {
            return false;
        }
        Optional<Client> client = clients.parId(a.clientId());
        if (client.isEmpty()) {
            LOG.warn("Rappel de l'échéance {} du contrat {} impossible : client introuvable", echeance.rang(),
                    a.numeroContrat());
            return false;
        }
        Instant maintenant = clock.instant();
        relances.enregistrer(Relance.avantPrelevement(echeance.id(), a.contratId(), echeance.date(), maintenant));
        notification.envoyer(Message.sms(client.get().telephone(), texte(a)));
        LOG.info("Rappel avant prélèvement envoyé : échéance {}/{} du contrat {}, prélevée le {}", echeance.rang(),
                a.nombreEcheances(), a.numeroContrat(), echeance.date());
        return true;
    }

    private String texte(EcheanceAPrelever a) {
        Echeance e = a.echeance();
        String rang = String.valueOf(e.rang());
        String total = String.valueOf(a.nombreEcheances());
        String date = DATE.format(e.date());
        String question = messages.getMessage("recouvrement.rappel.prelevement.whatsapp",
                new Object[] {rang, total, date, a.numeroContrat()}, LANGUE);
        String lien = LienWhatsApp.vers(parametres.texte(CleParametre.SEHELI_WHATSAPP_NUMERO), question);
        return messages.getMessage("recouvrement.rappel.prelevement.sms",
                new Object[] {rang, total, e.montant().valeur().toPlainString(), a.numeroContrat(), date, lien},
                LANGUE);
    }
}
