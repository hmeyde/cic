package com.cic.parametrage.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface ParametreJpaRepository extends JpaRepository<ParametreEntity, String> {
}
