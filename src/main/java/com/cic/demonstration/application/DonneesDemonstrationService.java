package com.cic.demonstration.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.cic.contrat.domain.Contrat;
import com.cic.contrat.domain.Echeance;
import com.cic.demonstration.domain.HorlogeSimulee;
import com.cic.demonstration.domain.Simulation;
import com.cic.identite.domain.ModeRemboursement;
import com.cic.identite.domain.TypeContrat;
import com.cic.identite.domain.TypeDocument;

/**
 * Données de démonstration (profil demo) : 10 clients fictifs dont l'historique est rejoué, du premier apport en
 * capital des associés jusqu'à aujourd'hui, par les vrais cas d'usage et avec les traitements planifiés de
 * chaque jour. Les dates sont relatives au jour du chargement (J) : les données sont fraîches à chaque nouvelle
 * base.
 *
 * <p>Un dossier par statut durable du cycle de vie (DEMANDE_SOUMISE et LIVREE ne durent qu'un instant : on les
 * lit dans le journal du dossier) :
 * <table>
 *   <caption>Clients fictifs (connexion par téléphone, code 000000 en profil demo)</caption>
 *   <tr><th>Téléphone</th><th>Client</th><th>Dossier en cours</th></tr>
 *   <tr><td>22100001</td><td>Mariem Mint Ahmed</td><td>EN_ANALYSE, score FAVORABLE (un premier plan SOLDEE)</td></tr>
 *   <tr><td>22100002</td><td>Sidi Ould Mohamed</td><td>EN_ANALYSE, justificatif demandé (une demande REFUSEE)</td></tr>
 *   <tr><td>22100003</td><td>Cheikh Ould Sidi</td><td>ACCEPTEE, autorisation signée, apport attendu</td></tr>
 *   <tr><td>22100004</td><td>Khadijetou Mint Salem</td><td>ANNULEE (délai de 7 jours dépassé)</td></tr>
 *   <tr><td>22100005</td><td>Mohamed Lemine Ould Abdallahi</td><td>APPORT_ET_AUTORISATION, à commander</td></tr>
 *   <tr><td>22100006</td><td>Fatimetou Mint Brahim</td><td>COMMANDEE, remise à confirmer (exemple chiffré)</td></tr>
 *   <tr><td>22100007</td><td>Ahmed Salem Ould Vall</td><td>EN_REMBOURSEMENT, après un retard régularisé</td></tr>
 *   <tr><td>22100008</td><td>Brahim Ould Cheikh</td><td>EN_RETARD depuis 8 jours environ, relances J+3 et J+7</td></tr>
 *   <tr><td>22100009</td><td>Oumar Ba</td><td>RECOUVREMENT, retard de plus de 30 jours (provision)</td></tr>
 *   <tr><td>22100010</td><td>Vatimetou Mint Mahmoud</td><td>SOLDEE</td></tr>
 * </table>
 *
 * <p>S'y ajoutent les apports en capital des associés, un paiement à identifier, des pré-inscriptions et des
 * notes de recouvrement. Le chargement n'a lieu qu'une fois : rien n'est fait si le premier client fictif
 * existe déjà.
 */
@Service
@Profile("demo")
public class DonneesDemonstrationService {

    private static final Logger LOG = LoggerFactory.getLogger(DonneesDemonstrationService.class);

    /** Premier jour simulé, avant J : les associés apportent le capital. */
    private static final int JOURS_D_HISTORIQUE = 250;

    private static final LocalTime MATIN = LocalTime.of(10, 0);
    private static final LocalTime MIDI = LocalTime.of(12, 0);
    private static final LocalTime APRES_MIDI = LocalTime.of(15, 0);
    private static final LocalTime FIN_D_APRES_MIDI = LocalTime.of(16, 30);
    private static final LocalTime SOIR = LocalTime.of(19, 0);
    private static final LocalTime PRELEVEMENT = LocalTime.of(11, 0);

    private final HorlogeSimulee horloge;
    private final ParcoursDemonstration parcours;
    private final TachesPlanifieesSimulees taches;

    DonneesDemonstrationService(HorlogeSimulee horloge, ParcoursDemonstration parcours,
            TachesPlanifieesSimulees taches) {
        this.horloge = horloge;
        this.parcours = parcours;
        this.taches = taches;
    }

    /** Charge les données de démonstration si elles ne le sont pas déjà ; {@code false} si le chargement a échoué. */
    public boolean chargerSiAbsentes() {
        Simulation simulation = new Simulation(horloge);
        LocalDate j = simulation.aujourdhui();
        List<ProfilClient> profils = profils(j);
        if (parcours.clientExiste(profils.getFirst())) {
            LOG.info("Données de démonstration déjà chargées");
            return true;
        }
        LOG.info("Chargement des données de démonstration : {} jours d'historique rejoués", JOURS_D_HISTORIQUE);
        long debut = System.nanoTime();
        try {
            planifier(simulation, j, profils.stream().map(DossierDemo::new).toList());
            taches.planifier(simulation, j.minusDays(JOURS_D_HISTORIQUE));
            simulation.derouler();
        } catch (RuntimeException e) {
            LOG.error("Chargement des données de démonstration interrompu : réinitialiser la démonstration pour recommencer", e);
            return false;
        }
        LOG.info("Données de démonstration chargées en {} s : {} clients fictifs", (System.nanoTime() - debut) / 1_000_000_000,
                profils.size());
        return true;
    }

    // --- Clients fictifs -----------------------------------------------------------------------------

    /** Jour de paie du client en retard : environ 8 jours avant J, pour des relances J+3 et J+7 déjà parties. */
    static int jourPaieDuClientEnRetard(LocalDate j) {
        return Math.min(28, j.minusDays(8).getDayOfMonth());
    }

    private static List<ProfilClient> profils(LocalDate j) {
        return List.of(
                new ProfilClient("22100001", "9990000101", "Mariem", "Mint Ahmed", LocalDate.of(1986, 3, 14),
                        "Ministère de la Santé", TypeContrat.FONCTIONNAIRE, LocalDate.of(2016, 9, 1), null, 45_000,
                        25, ModeRemboursement.DOMICILIATION_BMCI),
                new ProfilClient("22100002", "9990000102", "Sidi", "Ould Mohamed", LocalDate.of(1995, 7, 2),
                        "Mauritanie BTP (démo)", TypeContrat.CDD, j.minusMonths(14), j.plusMonths(20), 16_000, 28,
                        ModeRemboursement.VIREMENT_PERMANENT),
                new ProfilClient("22100003", "9990000103", "Cheikh", "Ould Sidi", LocalDate.of(1990, 11, 23),
                        "Sahel Télécom (démo)", TypeContrat.CDI, LocalDate.of(2019, 2, 1), null, 30_000, 27,
                        ModeRemboursement.VIREMENT_PERMANENT),
                new ProfilClient("22100004", "9990000104", "Khadijetou", "Mint Salem", LocalDate.of(1983, 5, 9),
                        "Ministère de l'Éducation nationale", TypeContrat.FONCTIONNAIRE, LocalDate.of(2012, 10, 1),
                        null, 22_000, 25, ModeRemboursement.RETENUE_EMPLOYEUR),
                new ProfilClient("22100005", "9990000105", "Mohamed Lemine", "Ould Abdallahi",
                        LocalDate.of(1988, 1, 30), "Atlantique Pêche SA (démo)", TypeContrat.CDI,
                        LocalDate.of(2018, 4, 15), null, 25_000, 30, ModeRemboursement.VIREMENT_PERMANENT),
                new ProfilClient("22100006", "9990000106", "Fatimetou", "Mint Brahim", LocalDate.of(1992, 8, 17),
                        "Banque Nouakchott Commerce (démo)", TypeContrat.CDI, LocalDate.of(2020, 1, 6), null, 35_000,
                        26, ModeRemboursement.DOMICILIATION_BMCI),
                new ProfilClient("22100007", "9990000107", "Ahmed Salem", "Ould Vall", LocalDate.of(1985, 12, 3),
                        "Sahel Télécom (démo)", TypeContrat.CDI, LocalDate.of(2015, 6, 1), null, 38_000, 20,
                        ModeRemboursement.VIREMENT_PERMANENT),
                new ProfilClient("22100008", "9990000108", "Brahim", "Ould Cheikh", LocalDate.of(1993, 4, 21),
                        "Chinguetti Logistique (démo)", TypeContrat.CDI, LocalDate.of(2021, 9, 1), null, 20_000,
                        jourPaieDuClientEnRetard(j), ModeRemboursement.VIREMENT_PERMANENT),
                new ProfilClient("22100009", "9990000109", "Oumar", "Ba", LocalDate.of(1989, 2, 11),
                        "Mauritanie BTP (démo)", TypeContrat.CDI, LocalDate.of(2017, 3, 1), null, 18_000, 5,
                        ModeRemboursement.VIREMENT_PERMANENT),
                new ProfilClient("22100010", "9990000110", "Vatimetou", "Mint Mahmoud", LocalDate.of(1980, 6, 28),
                        "Ministère de la Santé", TypeContrat.FONCTIONNAIRE, LocalDate.of(2014, 1, 2), null, 28_000,
                        25, ModeRemboursement.RETENUE_EMPLOYEUR));
    }

    // --- Scénarios -----------------------------------------------------------------------------------

    private void planifier(Simulation s, LocalDate j, List<DossierDemo> d) {
        capital(s, j);
        mariemSoldePuisEnAnalyse(s, j, d.get(0));
        sidiRefusePuisEnAnalyse(s, j, d.get(1));
        cheikhAccepte(s, j, d.get(2));
        khadijetouAnnulee(s, j, d.get(3));
        mohamedLemineApportEtAutorisation(s, j, d.get(4));
        fatimetouCommandee(s, j, d.get(5));
        ahmedSalemEnRemboursement(s, j, d.get(6));
        brahimEnRetard(s, j, d.get(7));
        oumarEnRecouvrement(s, j, d.get(8));
        vatimetouSoldee(s, j, d.get(9));
        s.planifier(j.minusDays(12), APRES_MIDI, () -> parcours.paiementSansReference("MASRVI4471", 3_000,
                j.minusDays(12)));
        preInscriptions(s, j);
    }

    private void capital(Simulation s, LocalDate j) {
        LocalDate creation = j.minusDays(JOURS_D_HISTORIQUE);
        s.planifier(creation, MATIN, () -> {
            parcours.apportCapital("Mohamed Vall Ould Cheikh (démo)", 400_000, creation);
            parcours.apportCapital("Aïcha Mint Ely (démo)", 300_000, creation);
            parcours.apportCapital("Abdallahi Ould Brahim (démo)", 300_000, creation);
        });
        LocalDate renfort = j.minusDays(120);
        s.planifier(renfort, MATIN, () -> parcours.apportCapital("Aïcha Mint Ely (démo)", 150_000, renfort));
    }

    /** Un premier petit plan remboursé à temps puis SOLDEE ; nouvelle demande hier, FAVORABLE grâce à l'historique. */
    private void mariemSoldePuisEnAnalyse(Simulation s, LocalDate j, DossierDemo d) {
        LocalDate acceptation = j.minusDays(240);
        ouvrirCompte(s, d, acceptation.minusDays(5));
        s.planifier(acceptation.minusDays(2), MATIN, () -> parcours.soumettre(d, "TEL-DEMO-04", 3, null));
        parcoursComplet(s, d, acceptation, c -> {
            prelevements(s, j, d, c, Set.of(), Map.of());
            s.planifier(c.echeances().getLast().date().plusDays(1), MATIN, () -> parcours.solder(d));
        });
        s.planifier(j.minusDays(1), LocalTime.of(18, 30), () -> parcours.soumettre(d, "TEL-DEMO-03", 6, 22_000L));
    }

    /** Climatiseur refusé (CDD récent, mensualité lourde) ; fontaine à eau demandée il y a 3 jours, bulletin réclamé. */
    private void sidiRefusePuisEnAnalyse(Simulation s, LocalDate j, DossierDemo d) {
        ouvrirCompte(s, d, j.minusDays(37));
        s.planifier(j.minusDays(35), MATIN, () -> parcours.soumettre(d, "ELM-DEMO-06", 6, null));
        s.planifier(j.minusDays(33), MIDI, () -> parcours.refuser(d, "Mensualité trop lourde pour un CDD récent "
                + "(plus de 20 % du salaire sur 6 mois) : une demande plus modeste peut être étudiée."));
        s.planifier(j.minusDays(3), SOIR, () -> parcours.soumettre(d, "ELM-DEMO-07", 3, 4_000L));
        s.planifier(j.minusDays(2), MATIN, () -> parcours.demanderJustificatif(d, TypeDocument.BULLETIN_PAIE,
                "Bulletin de paie du dernier mois, lisible : celui envoyé est flou."));
        s.planifier(j.minusDays(1), LocalTime.of(20, 15),
                () -> parcours.envoyerJustificatif(d, TypeDocument.BULLETIN_PAIE));
    }

    /** Acceptée avant-hier : autorisation signée déposée, apport Masrvi attendu (il reste 5 jours). */
    private void cheikhAccepte(Simulation s, LocalDate j, DossierDemo d) {
        ouvrirCompte(s, d, j.minusDays(6));
        s.planifier(j.minusDays(4), MIDI, () -> parcours.soumettre(d, "TEL-DEMO-02", 4, null));
        s.planifier(j.minusDays(2), APRES_MIDI, () -> parcours.accepter(d));
        s.planifier(j.minusDays(1), MATIN, () -> parcours.deposerAutorisationSignee(d));
    }

    /** Acceptée il y a 46 jours, ni apport ni autorisation : annulée par le traitement planifié 7 jours après. */
    private void khadijetouAnnulee(Simulation s, LocalDate j, DossierDemo d) {
        ouvrirCompte(s, d, j.minusDays(50));
        s.planifier(j.minusDays(48), MATIN, () -> parcours.soumettre(d, "ELM-DEMO-05", 3, null));
        s.planifier(j.minusDays(46), APRES_MIDI, () -> parcours.accepter(d));
    }

    /** Apport encaissé, autorisation acceptée hier par la banque : en attente du paiement fournisseur. */
    private void mohamedLemineApportEtAutorisation(Simulation s, LocalDate j, DossierDemo d) {
        ouvrirCompte(s, d, j.minusDays(10));
        s.planifier(j.minusDays(8), PRELEVEMENT, () -> parcours.soumettre(d, "ELM-DEMO-01", 6, null));
        s.planifier(j.minusDays(6), APRES_MIDI, () -> parcours.accepter(d));
        s.planifier(j.minusDays(5), MATIN, () -> parcours.deposerAutorisationSignee(d));
        s.planifier(j.minusDays(4), MATIN, () -> parcours.transmettreAutorisation(d));
        s.planifier(j.minusDays(4), FIN_D_APRES_MIDI, () -> parcours.payerApport(d, j.minusDays(4)));
        s.planifier(j.minusDays(1), PRELEVEMENT, () -> parcours.accepterAutorisation(d));
    }

    /** L'exemple chiffré de référence (20 000 MRU, 6 × 2 080) : commandé il y a 3 jours, remise à confirmer. */
    private void fatimetouCommandee(Simulation s, LocalDate j, DossierDemo d) {
        ouvrirCompte(s, d, j.minusDays(11));
        s.planifier(j.minusDays(9), PRELEVEMENT, () -> parcours.soumettre(d, "TEL-DEMO-01", 6, null));
        s.planifier(j.minusDays(7), APRES_MIDI, () -> parcours.accepter(d));
        s.planifier(j.minusDays(6), MATIN, () -> parcours.deposerAutorisationSignee(d));
        s.planifier(j.minusDays(6), FIN_D_APRES_MIDI, () -> parcours.payerApport(d, j.minusDays(6)));
        s.planifier(j.minusDays(5), MATIN, () -> parcours.transmettreAutorisation(d));
        s.planifier(j.minusDays(4), PRELEVEMENT, () -> parcours.accepterAutorisation(d));
        s.planifier(j.minusDays(3), MATIN, () -> parcours.commander(d));
    }

    /** Remboursement en cours ; la 2e échéance, payée avec 5 jours de retard, a été régularisée. */
    private void ahmedSalemEnRemboursement(Simulation s, LocalDate j, DossierDemo d) {
        LocalDate acceptation = j.minusDays(95);
        ouvrirCompte(s, d, acceptation.minusDays(5));
        s.planifier(acceptation.minusDays(2), MATIN, () -> parcours.soumettre(d, "ELM-DEMO-02", 6, null));
        parcoursComplet(s, d, acceptation, c -> {
            prelevements(s, j, d, c, Set.of(), Map.of(2, 5));
            s.planifier(c.echeance(2).orElseThrow().date().plusDays(2), MATIN, () -> parcours.noter(d,
                    "Appel du client : salaire du mois versé en retard par l'employeur, règlement promis sous 3 jours."));
        });
    }

    /** 3e échéance impayée il y a environ 8 jours : EN_RETARD, relances J+3 et J+7 envoyées, client joint. */
    private void brahimEnRetard(Simulation s, LocalDate j, DossierDemo d) {
        LocalDate x = j.minusDays(8);
        LocalDate troisieme = x.withDayOfMonth(jourPaieDuClientEnRetard(j));
        LocalDate premiere = troisieme.minusMonths(2);
        LocalDate acceptation = premiere.minusDays(12);
        ouvrirCompte(s, d, acceptation.minusDays(5));
        s.planifier(acceptation.minusDays(2), MATIN, () -> parcours.soumettre(d, "TEL-DEMO-08", 5, null));
        parcoursComplet(s, d, acceptation, c -> {
            prelevements(s, j, d, c, Set.of(3, 4, 5), Map.of());
            LocalDate echeance = c.echeance(3).orElseThrow().date();
            s.planifier(echeance.plusDays(4), MATIN, () -> parcours.noter(d, "Client joint par téléphone après la "
                    + "relance J+3 : il attend son salaire et réglera par Masrvi avec la référence de l'échéance."));
        });
    }

    /** Impayés depuis la 3e échéance, relances sans effet : passé en RECOUVREMENT 25 jours après. */
    private void oumarEnRecouvrement(Simulation s, LocalDate j, DossierDemo d) {
        LocalDate acceptation = j.minusDays(150);
        ouvrirCompte(s, d, acceptation.minusDays(5));
        s.planifier(acceptation.minusDays(2), MATIN, () -> parcours.soumettre(d, "ELM-DEMO-04", 6, null));
        parcoursComplet(s, d, acceptation, c -> {
            prelevements(s, j, d, c, Set.of(3, 4, 5, 6), Map.of());
            LocalDate echeance = c.echeance(3).orElseThrow().date();
            s.planifier(echeance.plusDays(4), MATIN,
                    () -> parcours.noter(d, "Relance J+3 envoyée ; client injoignable au téléphone."));
            s.planifier(echeance.plusDays(9), MATIN, () -> parcours.noter(d,
                    "Relance J+7 sans effet. Courrier à l'employeur pour demander une retenue sur salaire."));
            s.planifier(echeance.plusDays(25), APRES_MIDI, () -> parcours.passerEnRecouvrement(d,
                    "Aucun paiement 25 jours après l'échéance malgré les relances J+3 et J+7 et le courrier à "
                            + "l'employeur."));
        });
    }

    /** Machine à laver remboursée en 3 mois, toutes les échéances à temps : SOLDEE. */
    private void vatimetouSoldee(Simulation s, LocalDate j, DossierDemo d) {
        LocalDate acceptation = j.minusDays(140);
        ouvrirCompte(s, d, acceptation.minusDays(5));
        s.planifier(acceptation.minusDays(2), MATIN, () -> parcours.soumettre(d, "ELM-DEMO-03", 3, 8_000L));
        parcoursComplet(s, d, acceptation, c -> {
            prelevements(s, j, d, c, Set.of(), Map.of());
            s.planifier(c.echeances().getLast().date().plusDays(1), MATIN, () -> parcours.solder(d));
        });
    }

    private void preInscriptions(Simulation s, LocalDate j) {
        record Prospect(int jours, String nom, String telephone, String employeur, String contrat, String tranche,
                String produit) {
        }
        List<Prospect> prospects = List.of(
                new Prospect(25, "Aminetou Mint Sidi", "36100001", "Ministère de la Santé", "FONCTIONNAIRE",
                        "DE_20000_A_40000", "Réfrigérateur"),
                new Prospect(21, "Yahya Ould Ahmed", "46100002", "Sahel Télécom (démo)", "CDI", "DE_20000_A_40000",
                        "iPhone 15"),
                new Prospect(18, "Mariem Ba", "22100103", "Mauritanie BTP (démo)", "CDD", "DE_10000_A_20000",
                        "Téléphone Samsung"),
                new Prospect(14, "El Hadj Ould Mahmoud", "36100004", "Ministère de l'Éducation nationale",
                        "FONCTIONNAIRE", "DE_10000_A_20000", "Climatiseur"),
                new Prospect(10, "Zeinabou Mint Moctar", "46100005", "Atlantique Pêche SA (démo)", "CDI",
                        "MOINS_DE_10000", "Fontaine à eau"),
                new Prospect(7, "Sid'Ahmed Ould Baba", "22100106", "Banque Nouakchott Commerce (démo)", "CDI",
                        "PLUS_DE_40000", "Galaxy S24"),
                new Prospect(4, "Coumba Sy", "36100007", "Chinguetti Logistique (démo)", "CDD", "DE_10000_A_20000",
                        "Machine à laver"),
                new Prospect(2, "Moulaye Ould Ely", "46100008", "Ministère de la Santé", "FONCTIONNAIRE",
                        "DE_20000_A_40000", "Climatiseur et réfrigérateur"));
        for (Prospect p : prospects) {
            s.planifier(j.minusDays(p.jours()), LocalTime.of(21, 0), () -> parcours.preInscription(p.nom(),
                    p.telephone(), p.employeur(), p.contrat(), p.tranche(), p.produit()));
        }
    }

    // --- Enchaînements communs -----------------------------------------------------------------------

    /** Le client crée son compte et envoie ses justificatifs ; le back-office vérifie son identité le lendemain. */
    private void ouvrirCompte(Simulation s, DossierDemo d, LocalDate jour) {
        s.planifier(jour, LocalTime.of(20, 0), () -> parcours.ouvrirCompte(d));
        s.planifier(jour.plusDays(1), MATIN, () -> parcours.verifierIdentite(d));
    }

    /**
     * De l'acceptation à la remise du produit : autorisation signée et apport le lendemain, transmission à la
     * banque, acceptation (APPORT_ET_AUTORISATION), paiement fournisseur (COMMANDEE), remise confirmée par le
     * client (LIVREE puis EN_REMBOURSEMENT), avant la première échéance. {@code apresRemise} planifie la suite.
     */
    private void parcoursComplet(Simulation s, DossierDemo d, LocalDate acceptation, Consumer<Contrat> apresRemise) {
        s.planifier(acceptation, APRES_MIDI, () -> parcours.accepter(d));
        s.planifier(acceptation.plusDays(1), MATIN, () -> parcours.deposerAutorisationSignee(d));
        s.planifier(acceptation.plusDays(1), FIN_D_APRES_MIDI, () -> parcours.payerApport(d, acceptation.plusDays(1)));
        s.planifier(acceptation.plusDays(2), MATIN, () -> parcours.transmettreAutorisation(d));
        s.planifier(acceptation.plusDays(3), PRELEVEMENT, () -> parcours.accepterAutorisation(d));
        s.planifier(acceptation.plusDays(4), MATIN, () -> parcours.commander(d));
        s.planifier(acceptation.plusDays(6), SOIR, () -> {
            parcours.confirmerRemise(d);
            apresRemise.accept(parcours.contrat(d));
        });
    }

    /**
     * Prélèvements reçus le jour de chaque échéance passée, sauf les rangs {@code impayes} ; un rang de
     * {@code retards} est payé ce nombre de jours après sa date, puis le dossier est régularisé. Le premier
     * prélèvement active l'autorisation.
     */
    private void prelevements(Simulation s, LocalDate j, DossierDemo d, Contrat c, Set<Integer> impayes,
            Map<Integer, Integer> retards) {
        for (Echeance e : c.echeances()) {
            int rang = e.rang();
            if (impayes.contains(rang)) {
                continue;
            }
            int retard = retards.getOrDefault(rang, 0);
            LocalDate jour = e.date().plusDays(retard);
            if (jour.isAfter(j)) {
                continue;
            }
            s.planifier(jour, PRELEVEMENT, () -> parcours.payerEcheance(d, rang, jour));
            if (rang == 1) {
                s.planifier(jour, FIN_D_APRES_MIDI, () -> parcours.activerAutorisation(d));
            }
            if (retard > 0) {
                s.planifier(jour, APRES_MIDI, () -> parcours.regulariser(d));
            }
        }
    }
}
