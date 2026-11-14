package com.seheli.demande.adapter.out.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import com.seheli.demande.domain.Recommandation;

/** Score d'une demande, même clé que la demande. Immuable une fois calculé. */
@Entity
@Table(name = "score")
class ScoreEntity {

    @Id
    @Column(name = "demande_id")
    private UUID demandeId;

    @Column(name = "version_grille", nullable = false, length = 10)
    private String versionGrille;

    @Column(name = "eligible", nullable = false)
    private boolean eligible;

    @Column(name = "note", nullable = false)
    private int note;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommandation_note", nullable = false, length = 20)
    private Recommandation recommandationNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommandation", nullable = false, length = 20)
    private Recommandation recommandation;

    @Column(name = "calcule_le", nullable = false)
    private Instant calculeLe;

    @ElementCollection
    @CollectionTable(name = "score_condition", joinColumns = @JoinColumn(name = "demande_id"))
    @OrderBy("rang")
    private List<ScoreConditionEmbeddable> conditions = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "score_critere", joinColumns = @JoinColumn(name = "demande_id"))
    @OrderBy("rang")
    private List<ScoreCritereEmbeddable> criteres = new ArrayList<>();

    protected ScoreEntity() {
    }

    ScoreEntity(UUID demandeId, String versionGrille, boolean eligible, int note, Recommandation recommandationNote,
            Recommandation recommandation, Instant calculeLe, List<ScoreConditionEmbeddable> conditions,
            List<ScoreCritereEmbeddable> criteres) {
        this.demandeId = demandeId;
        this.versionGrille = versionGrille;
        this.eligible = eligible;
        this.note = note;
        this.recommandationNote = recommandationNote;
        this.recommandation = recommandation;
        this.calculeLe = calculeLe;
        this.conditions = new ArrayList<>(conditions);
        this.criteres = new ArrayList<>(criteres);
    }

    UUID getDemandeId() {
        return demandeId;
    }

    String getVersionGrille() {
        return versionGrille;
    }

    Recommandation getRecommandationNote() {
        return recommandationNote;
    }

    List<ScoreConditionEmbeddable> getConditions() {
        return conditions;
    }

    List<ScoreCritereEmbeddable> getCriteres() {
        return criteres;
    }
}
