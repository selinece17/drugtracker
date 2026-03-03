package com.drugtracker.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Phases of clinical drug development per FDA/ICH guidelines.
 */
enum class TrialPhase {
    PHASE_1,   // Safety, dosing — small healthy volunteer group
    PHASE_2,   // Efficacy and side effects — larger patient group
    PHASE_3,   // Confirmatory large-scale trials.html
    PHASE_4    // Post-approval surveillance / pharmacovigilance
}

/**
 * Operational status of a clinical trial.
 */
enum class TrialStatus {
    PLANNING,    // Protocol designed, not yet started
    RECRUITING,  // Actively enrolling participants
    ACTIVE,      // Enrollment complete, treatment ongoing
    COMPLETED,   // All participants finished, analysis underway
    SUSPENDED,   // Temporarily halted (e.g. safety review)
    TERMINATED   // Permanently stopped before completion
}

/**
 * Entity representing a clinical trial linked to a specific [Compound].
 *
 * A single compound may have multiple trials.html (e.g. different phases, indications,
 * or populations). Uses a standard class rather than data class to avoid JPA
 * issues with lazy-loaded [Compound] reference in equals/hashCode.
 *
 * References:
 *  - clinicaltrials.gov — standard source for NCT identifiers and trial metadata
 */
@Entity
@Table(
    name = "clinical_trials",
    indexes = [
        Index(name = "idx_trial_status", columnList = "status"),
        Index(name = "idx_trial_compound", columnList = "compound_id")
    ]
)
class ClinicalTrial(

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * The compound being tested in this trial.
     * Many trials.html can reference the same compound (many-to-one).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compound_id", nullable = false)
    val compound: Compound,

    /**
     * External trial identifier, typically an NCT number from ClinicalTrials.gov.
     * Example: "NCT04761068". Must be unique across all trials.html.
     */
    @field:NotBlank(message = "Trial ID is required")
    @field:Size(max = 50)
    @Column(name = "trial_id", unique = true, nullable = false)
    val trialId: String,

    /** Full descriptive title of the clinical trial protocol. */
    @field:NotBlank(message = "Trial title is required")
    @field:Size(max = 500)
    @Column(nullable = false)
    val title: String,

    /** Phase of clinical development for this trial. Required. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val phase: TrialPhase,

    /** Current operational status. Defaults to PLANNING for newly created records. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: TrialStatus = TrialStatus.PLANNING,

    /**
     * Target number of participants to be enrolled.
     * Must be at least 1 if provided.
     */
    @field:Min(value = 1, message = "Target enrollment must be at least 1")
    @Column(name = "target_enrollment")
    val targetEnrollment: Int? = null,

    /**
     * Number of participants currently enrolled.
     * Should not exceed [targetEnrollment] but this is not enforced at DB level.
     */
    @field:Min(value = 0, message = "Actual enrollment cannot be negative")
    @Column(name = "actual_enrollment")
    val actualEnrollment: Int? = null,

    /** Date the trial began (or is planned to begin). */
    @Column(name = "start_date")
    val startDate: LocalDate? = null,

    /** Date the trial ended (or is planned to end). Must be on or after [startDate]. */
    @Column(name = "end_date")
    val endDate: LocalDate? = null,

    /** Name of the lead researcher responsible for the trial protocol. */
    @field:Size(max = 255)
    @Column(name = "principal_investigator")
    val principalInvestigator: String? = null,

    /** Organisation sponsoring the trial (e.g. pharmaceutical company or academic institution). */
    @field:Size(max = 255)
    val sponsor: String? = null,

    /** Free-text notes about the trial, e.g. interim results, protocol amendments. */
    @field:Size(max = 2000)
    @Column(length = 2000)
    val notes: String? = null,

    /** Timestamp when this trial record was created. Immutable. */
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

) {
    /**
     * Creates a copy of this trial with selective field overrides.
     * Preserves id and createdAt from the original.
     */
    fun copy(
        compound: Compound = this.compound,
        trialId: String = this.trialId,
        title: String = this.title,
        phase: TrialPhase = this.phase,
        status: TrialStatus = this.status,
        targetEnrollment: Int? = this.targetEnrollment,
        actualEnrollment: Int? = this.actualEnrollment,
        startDate: LocalDate? = this.startDate,
        endDate: LocalDate? = this.endDate,
        principalInvestigator: String? = this.principalInvestigator,
        sponsor: String? = this.sponsor,
        notes: String? = this.notes
    ): ClinicalTrial = ClinicalTrial(
        id = this.id,
        compound = compound,
        trialId = trialId,
        title = title,
        phase = phase,
        status = status,
        targetEnrollment = targetEnrollment,
        actualEnrollment = actualEnrollment,
        startDate = startDate,
        endDate = endDate,
        principalInvestigator = principalInvestigator,
        sponsor = sponsor,
        notes = notes,
        createdAt = this.createdAt
    )

    /** ID-based equality to avoid lazy-loading the compound relationship in comparisons. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClinicalTrial) return false
        if (id == 0L) return false
        return id == other.id
    }

    override fun hashCode(): Int = if (id == 0L) System.identityHashCode(this) else id.hashCode()

    override fun toString(): String =
        "ClinicalTrial(id=$id, trialId='$trialId', phase=$phase, status=$status)"
}
