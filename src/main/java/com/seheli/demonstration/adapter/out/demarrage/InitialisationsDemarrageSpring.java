package com.seheli.demonstration.adapter.out.demarrage;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import com.seheli.demonstration.domain.InitialisationsDemarrage;

/**
 * Rejoue les {@link ApplicationRunner} des modules (catalogue, client et comptes du back-office de
 * démonstration), dans l'ordre de Spring Boot, sans dépendre de leurs classes internes.
 */
@Component
@Profile("demo")
class InitialisationsDemarrageSpring implements InitialisationsDemarrage {

    private final ObjectProvider<ApplicationRunner> runners;
    private final ObjectProvider<ApplicationArguments> arguments;

    InitialisationsDemarrageSpring(ObjectProvider<ApplicationRunner> runners,
            ObjectProvider<ApplicationArguments> arguments) {
        this.runners = runners;
        this.arguments = arguments;
    }

    @Override
    public void rejouer() {
        ApplicationArguments args = arguments.getIfAvailable(DefaultApplicationArguments::new);
        List<ApplicationRunner> tries = new ArrayList<>(runners.stream().toList());
        AnnotationAwareOrderComparator.sort(tries);
        for (ApplicationRunner runner : tries) {
            try {
                runner.run(args);
            } catch (Exception e) {
                throw new IllegalStateException("Initialisation de démarrage en échec : " + runner.getClass().getName(), e);
            }
        }
    }
}
