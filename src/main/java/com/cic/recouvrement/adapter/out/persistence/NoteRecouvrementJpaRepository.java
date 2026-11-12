package com.cic.recouvrement.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface NoteRecouvrementJpaRepository extends JpaRepository<NoteRecouvrementEntity, UUID> {

    List<NoteRecouvrementEntity> findByDemandeIdOrderByEcriteLeDescIdDesc(UUID demandeId);
}
