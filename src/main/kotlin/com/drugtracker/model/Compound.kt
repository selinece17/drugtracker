package com.drugtracker.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import java.time.LocalDateTime

/**
 * Represents the lifecycle stage of a drug compound in development.
 * Ordered roughly from earliest to latest stage.
 */
enum class CompoundStatus {
    DISCOVERY,      // Initial identification
    PRECLINICAL,    // Lab and animal testing
    PHASE_1,        // Human safety trials.html (small group)
    PHASE_2,        // Human efficacy trials.html (larger group)
    PHASE_3,        // Large-scale confirmatory trials.html
    APPROVED,       // Regulatory approval granted
    DISCONTINUED    // Development halted
}

/**
 * Broad medical category a compound is intended to treat.
 */
enum class TherapeuticArea {
    ONCOLOGY,
    CARDIOLOGY,
    NEUROLOGY,
    IMMUNOLOGY,
    INFECTIOUS_DISEASE,
    METABOLIC,
    RESPIRATORY,
    OPHTHALMOLOGY,
    RARE_DISEASE,
    OTHER
}

/**
 * Core entity representing a drug compound under research or approved use.
 *
 * Uses a standard class (not data class) to avoid JPA pitfalls with lazy-loaded
 * collections in auto-generated equals/hashCode/toString methods, which can
 * cause LazyInitializationException or StackOverflow errors.
 *
 * Relationships:
 *  - One compound may have many [ClinicalTrial] records (one-to-many)
 *  - One compound may have many [CompoundInventory] records (one-to-many)
 */
@Entity
@Table(
    name = "compounds",
    indexes = [
        Index(name = "idx_compound_status", columnList = "status"),
        Index(name = "idx_compound_area", columnList = "therapeutic_area")
    ]
)
class Compound(

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** Human-readable unique name for the compound, e.g. "Aspirin" or "DRT-4821". */
    @field:NotBlank(message = "Compound name is required")
    @field:Size(max = 255, message = "Name must be 255 characters or fewer")
    @Column(nullable = false, unique = true)
    val name: String,

    /**
     * CAS Registry Number — a globally unique chemical identifier.
     * Optional for investigational compounds that have not yet been registered.
     * Format example: "50-78-2"
     */
    @field:Size(max = 50, message = "CAS number must be 50 characters or fewer")
    @Column(name = "cas_number", unique = true, nullable = true)
    val casNumber: String? = null,

    /**
     * Standard chemical molecular formula, e.g. "C9H8O4".
     * Required as the minimum meaningful chemical identifier.
     */
    @field:NotBlank(message = "Molecular formula is required")
    @field:Size(max = 100)
    @Column(name = "molecular_formula", nullable = false)
    val molecularFormula: String,

    /**
     * Molecular weight in grams per mole (g/mol).
     * Must be a positive number if provided.
     */
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Molecular weight must be positive")
    @Column(name = "molecular_weight")
    val molecularWeight: Double? = null,

    /** Current development stage of the compound. Defaults to DISCOVERY for new entries. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: CompoundStatus = CompoundStatus.DISCOVERY,

    /** Medical field this compound is intended to treat. Required. */
    @Enumerated(EnumType.STRING)
    @Column(name = "therapeutic_area", nullable = false)
    val therapeuticArea: TherapeuticArea,

    /** Free-text description of the compound's purpose, background, and characteristics. */
    @field:Size(max = 2000)
    @Column(length = 2000)
    val description: String? = null,

    /** Name of the protein or molecular target, e.g. "COX-1, COX-2" or "KRAS G12C". */
    @field:Size(max = 500)
    @Column(name = "target_protein")
    val targetProtein: String? = null,

    /** Description of how the compound achieves its therapeutic effect. */
    @field:Size(max = 500)
    @Column(name = "mechanism_of_action")
    val mechanismOfAction: String? = null,

    /** True if this compound is a DEA-scheduled controlled substance. */
    @Column(name = "is_controlled_substance", nullable = false)
    val isControlledSubstance: Boolean = false,

    /**
     * DEA Schedule number (I–V) applicable only when [isControlledSubstance] is true.
     * Null for non-controlled compounds.
     */
    @field:Min(value = 1, message = "DEA schedule must be between 1 and 5")
    @field:Max(value = 5, message = "DEA schedule must be between 1 and 5")
    @Column(name = "dea_schedule")
    val deaSchedule: Int? = null,

    /** Timestamp when this record was first created. Set automatically; never updated. */
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Timestamp of the most recent update to this record.
     * Mutable so the service layer can update it explicitly on every save.
     */
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Clinical trials.html associated with this compound.
     * LAZY loaded — must be accessed within an open Hibernate session/transaction.
     */
    @OneToMany(mappedBy = "compound", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val trials: MutableList<ClinicalTrial> = mutableListOf(),

    /**
     * Physical inventory lots of this compound.
     * LAZY loaded — must be accessed within an open Hibernate session/transaction.
     */
    @OneToMany(mappedBy = "compound", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val inventory: MutableList<CompoundInventory> = mutableListOf()

) {
    /**
     * Creates a copy of this Compound with selective field overrides.
     * Preserves id, createdAt, trials.html, and inventory lists from the original.
     *
     * @param name            New compound name (defaults to current value)
     * @param casNumber       New CAS number
     * @param molecularFormula New formula
     * @param molecularWeight  New weight
     * @param status           New status
     * @param therapeuticArea  New area
     * @param description      New description
     * @param targetProtein    New target
     * @param mechanismOfAction New MOA
     * @param isControlledSubstance Controlled flag
     * @param deaSchedule      DEA schedule number
     * @return A new Compound instance with updated fields and current timestamp
     */
    fun copy(
        name: String = this.name,
        casNumber: String? = this.casNumber,
        molecularFormula: String = this.molecularFormula,
        molecularWeight: Double? = this.molecularWeight,
        status: CompoundStatus = this.status,
        therapeuticArea: TherapeuticArea = this.therapeuticArea,
        description: String? = this.description,
        targetProtein: String? = this.targetProtein,
        mechanismOfAction: String? = this.mechanismOfAction,
        isControlledSubstance: Boolean = this.isControlledSubstance,
        deaSchedule: Int? = this.deaSchedule
    ): Compound = Compound(
        id = this.id,
        name = name,
        casNumber = casNumber,
        molecularFormula = molecularFormula,
        molecularWeight = molecularWeight,
        status = status,
        therapeuticArea = therapeuticArea,
        description = description,
        targetProtein = targetProtein,
        mechanismOfAction = mechanismOfAction,
        isControlledSubstance = isControlledSubstance,
        deaSchedule = deaSchedule,
        createdAt = this.createdAt,
        updatedAt = LocalDateTime.now(), // Always refresh on copy
        trials = this.trials,
        inventory = this.inventory
    )

    /**
     * Equality based solely on database ID to avoid triggering lazy collection loads.
     * Two transient (unsaved) compounds with id=0 are not considered equal.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Compound) return false
        if (id == 0L) return false // transient objects not equal
        return id == other.id
    }

    override fun hashCode(): Int = if (id == 0L) System.identityHashCode(this) else id.hashCode()

    override fun toString(): String =
        "Compound(id=$id, name='$name', status=$status, therapeuticArea=$therapeuticArea)"
}
