package com.seheli.identite.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seheli.identite.domain.Client;
import com.seheli.identite.domain.ClientRepository;
import com.seheli.identite.domain.CodeConnexion;
import com.seheli.identite.domain.CodeConnexion.Verification;
import com.seheli.identite.domain.CodeConnexionRepository;
import com.seheli.identite.domain.GenerateurCode;
import com.seheli.notification.domain.Message;
import com.seheli.notification.domain.Notification;
import com.seheli.parametrage.application.ParametreService;
import com.seheli.parametrage.domain.CleParametre;

/**
 * Connexion du client par téléphone et code à usage unique.
 *
 * <p>Le code (6 chiffres) est envoyé par SMS via le port {@link Notification} ; sa durée de validité,
 * le nombre d'essais et le délai avant renvoi viennent de la table {@code parametre}.
 * Seul le hachage du code est conservé. Tout numéro bien formé reçoit un code : la réponse à la demande
 * ne révèle donc pas quels numéros sont clients ; c'est après un code valide qu'un nouveau client est
 * invité à créer son compte. Aucun téléphone n'est journalisé.
 */
@Service
public class ConnexionClientService {

    private static final Logger LOG = LoggerFactory.getLogger(ConnexionClientService.class);

    private final ClientRepository clients;
    private final CodeConnexionRepository codes;
    private final GenerateurCode generateur;
    private final PasswordEncoder encodeur;
    private final Notification notification;
    private final ParametreService parametres;
    private final MessageSource messages;
    private final Clock clock;

    public ConnexionClientService(ClientRepository clients, CodeConnexionRepository codes, GenerateurCode generateur,
            PasswordEncoder encodeur, Notification notification, ParametreService parametres, MessageSource messages,
            Clock clock) {
        this.clients = clients;
        this.codes = codes;
        this.generateur = generateur;
        this.encodeur = encodeur;
        this.notification = notification;
        this.parametres = parametres;
        this.messages = messages;
        this.clock = clock;
    }

    /**
     * Envoie un code à ce téléphone, qu'il soit celui d'un client ou non : un nouveau client prouve ainsi
     * que le numéro est à lui avant de créer son compte. Si un code envoyé il y a moins de
     * {@code connexion.code.renvoi.secondes} est encore utilisable, aucun nouveau code n'est envoyé.
     *
     * @return le téléphone normalisé, à conserver pour l'étape de saisie du code
     * @throws TelephoneInvalideException si le numéro est mal formé
     */
    @Transactional
    public String demanderCode(String telephoneSaisi, Locale langue) {
        String telephone = normaliser(telephoneSaisi);
        Instant maintenant = clock.instant();
        if (attenteAvantRenvoi(telephone, maintenant).isPositive()) {
            LOG.info("Code de connexion redemandé trop tôt : le code en cours reste valable");
            return telephone;
        }
        int validiteMinutes = parametres.entier(CleParametre.CONNEXION_CODE_VALIDITE_MINUTES);
        int essaisMax = parametres.entier(CleParametre.CONNEXION_CODE_ESSAIS_MAX);
        String code = generateur.generer();
        codes.enregistrer(CodeConnexion.emettre(telephone, encodeur.encode(code), maintenant,
                Duration.ofMinutes(validiteMinutes), essaisMax));
        String texte = messages.getMessage("client.connexion.code.sms", new Object[] {code, validiteMinutes}, langue);
        notification.envoyer(Message.sms(telephone, texte));
        return telephone;
    }

    /**
     * Nombre de secondes (arrondi au-dessus) avant qu'un nouveau code puisse être envoyé à ce téléphone :
     * 0 s'il n'y a aucun code en cours, si le code en cours ne peut plus servir ou si le délai
     * {@code connexion.code.renvoi.secondes} est écoulé.
     */
    @Transactional(readOnly = true)
    public long secondesAvantRenvoi(String telephoneSaisi) {
        Duration attente = attenteAvantRenvoi(normaliser(telephoneSaisi), clock.instant());
        return attente.isPositive() ? (attente.toMillis() + 999) / 1000 : 0;
    }

    private Duration attenteAvantRenvoi(String telephone, Instant maintenant) {
        Optional<CodeConnexion> enCours = codes.parTelephone(telephone);
        if (enCours.isEmpty() || enCours.get().estInutilisableA(maintenant)) {
            return Duration.ZERO;
        }
        Duration renvoi = Duration.ofSeconds(parametres.entier(CleParametre.CONNEXION_CODE_RENVOI_SECONDES));
        return Duration.between(maintenant, enCours.get().emisLe().plus(renvoi));
    }

    /**
     * Vérifie le code saisi pour ce téléphone ; chaque essai est décompté. Un code valide identifie le
     * client qui porte ce téléphone, ou signale un nouveau client s'il n'y en a aucun.
     */
    @Transactional
    public ResultatConnexion verifierCode(String telephoneSaisi, String codeSaisi) {
        Objects.requireNonNull(codeSaisi, "codeSaisi");
        String telephone = normaliser(telephoneSaisi);
        Optional<CodeConnexion> code = codes.parTelephone(telephone);
        if (code.isEmpty()) {
            // Aucun code demandé pour ce numéro : même réponse qu'un code expiré.
            return ResultatConnexion.echec(Verification.EXPIRE, 0);
        }
        CodeConnexion c = code.get();
        Verification verification = c.verifier(nettoyerCode(codeSaisi), clock.instant(), encodeur::matches);
        codes.enregistrer(c);
        if (verification != Verification.VALIDE) {
            LOG.info("Code de connexion refusé ({})", verification);
            return ResultatConnexion.echec(verification, c.essaisRestants());
        }
        Optional<Client> client = clients.parTelephone(telephone);
        if (client.isEmpty()) {
            LOG.info("Code de connexion valide pour un numéro sans compte : création du compte à suivre");
            return ResultatConnexion.nouveauClient();
        }
        LOG.info("Connexion réussie de {}", client.get());
        return ResultatConnexion.valide(client.get().id());
    }

    /**
     * Code tel que saisi ou collé : espaces, tirets et points retirés ; chiffres arabes orientaux
     * (٠-٩ et ۰-۹, claviers arabes des téléphones) ramenés aux chiffres 0-9.
     */
    public static String nettoyerCode(String codeSaisi) {
        StringBuilder code = new StringBuilder(codeSaisi.length());
        codeSaisi.codePoints().forEach(c -> {
            if (c >= '٠' && c <= '٩') {
                code.append((char) ('0' + c - '٠'));
            } else if (c >= '۰' && c <= '۹') {
                code.append((char) ('0' + c - '۰'));
            } else if (!Character.isWhitespace(c) && !Character.isSpaceChar(c) && c != '-' && c != '.') {
                code.appendCodePoint(c);
            }
        });
        return code.toString();
    }

    private static String normaliser(String telephone) {
        try {
            return Client.normaliserTelephone(telephone);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new TelephoneInvalideException();
        }
    }
}
