package com.cic.parametrage.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cic.parametrage.application.ChargementParametres;
import com.cic.parametrage.domain.Parametre;
import com.cic.parametrage.domain.ParametreInvalideException;
import com.cic.parametrage.domain.TypeParametre;

@Component
class ParametrePersistenceAdapter implements ChargementParametres {

    private final ParametreJpaRepository repository;

    ParametrePersistenceAdapter(ParametreJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Parametre> chargerTous() {
        return repository.findAll().stream().map(ParametrePersistenceAdapter::versDomaine).toList();
    }

    private static Parametre versDomaine(ParametreEntity e) {
        TypeParametre type;
        try {
            type = TypeParametre.valueOf(e.getType());
        } catch (IllegalArgumentException ex) {
            throw new ParametreInvalideException("Paramètre " + e.getCle() + " : type inconnu " + e.getType(), ex);
        }
        return new Parametre(e.getCle(), e.getValeur(), type);
    }
}
