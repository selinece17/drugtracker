package com.drugtracker.model

import jakarta.validation.constraints.*
import java.time.LocalDate
import java.time.LocalDateTime

// =============================================================================
// Compound DTOs
// =============================================================================

/**
 * Request body for creating or updating a [Compound].
 * Bean validation annotations are enforced by Spring before the service is called.
 */
data class CompoundRequest(

    /** Unique compound name. Required, non-blank. */
    @field:NotBlank(message = "Compound name is required")
    @field:Size(max = 255, message = "Name must be 255 characters or fewer")
    val name: String,

    /** Optional CAS registry number, e.g. "50-78-2". */
    @field:Size(max = 50, message = "CAS number must be 50 characters or fewer")
    val casNumber: String? = null,

    /** Standard molecular formula, e.g. "C9H8O4". Required. */
    @field:NotBlank(message = "Molecular formula is required")
    @field:Size(max = 100)
    val molecularFormula: String,

    /** Molecular weight in g/mol. Optional but must be positive if provided. */
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Molecular weight must be positive")
    val molecularWeight: Double? = null,

    /** Development stage. Defaults to DISCOVERY if omitted. */
    val status: CompoundStatus = CompoundStatus.DISCOVERY,

    /** Medical field targeted. Required. */
    @field:NotNull(message = "Therapeutic area is required")
    val therapeuticArea: TherapeuticArea,

    /** Free-text description, max 2000 characters. */
    @field:Size(max = 2000, message = "Description must be 2000 characters or fewer")
    val description: String? = null,

    /** Target protein or receptor name. */
    @field:Size(max = 500)
    val targetProtein: String? = null,

    /** Mechanism of action description. */
    @field:Size(max = 500)
    val mechanismOfAction: String? = null,

    /** Whether this compound is a DEA-scheduled controlled substance. Defaults to false. */
    val isControlledSubstance: Boolean = false,

    /** DEA schedule number 1–5. Only meaningful when [isControlledSubstance] is true. */
    @field:Min(value = 1, message = "DEA schedule must be between 1 and 5")
    @field:Max(value = 5, message = "DEA schedule must be between 1 and 5")
    val deaSchedule: Int? = null
)

/**
 * Response DTO returned when reading a [Compound] from the API.
 * Includes counts of linked trials.html and inventory lots for quick overview.
 */
data class CompoundResponse(
    val id: Long,
    val name: String,
    val casNumber: String?,
    val molecularFormula: String,
    val molecularWeight: Double?,
    val status: CompoundStatus,
    val therapeuticArea: TherapeuticArea,
    val description: String?,
    val targetProtein: String?,
    val mechanismOfAction: String?,
    val isControlledSubstance: Boolean,
    val deaSchedule: Int?,
    /** Number of clinical trials.html associated with this compound. */
    val trialCount: Int,
    /** Number of inventory lots associated with this compound. */
    val inventoryCount: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        /**
         * Build a [CompoundResponse] from a [Compound] entity.
         * Must be called within a transaction so lazy-loaded [Compound.trials]
         * and [Compound.inventory] collections are accessible.
         *
         * @param c The entity to convert
         * @return Populated response DTO
         */
        fun from(c: Compound): CompoundResponse = CompoundResponse(
            id = c.id,
            name = c.name,
            casNumber = c.casNumber,
            molecularFormula = c.molecularFormula,
            molecularWeight = c.molecularWeight,
            status = c.status,
            therapeuticArea = c.therapeuticArea,
            description = c.description,
            targetProtein = c.targetProtein,
            mechanismOfAction = c.mechanismOfAction,
            isControlledSubstance = c.isControlledSubstance,
            deaSchedule = c.deaSchedule,
            trialCount = c.trials.size,
            inventoryCount = c.inventory.size,
            createdAt = c.createdAt,
            updatedAt = c.updatedAt
        )
    }
}

// =============================================================================
// Clinical Trial DTOs
// =============================================================================

/**
 * Request body for creating or updating a [ClinicalTrial].
 */
data class ClinicalTrialRequest(

    /** ID of the [Compound] this trial is testing. Required. */
    @field:NotNull(message = "Compound ID is required")
    val compoundId: Long?,

    /**
     * External trial identifier, typically an NCT number.
     * Must be unique across all trials.html.
     */
    @field:NotBlank(message = "Trial ID is required")
    @field:Size(max = 50)
    val trialId: String,

    /** Full descriptive title of the protocol. */
    @field:NotBlank(message = "Trial title is required")
    @field:Size(max = 500)
    val title: String,

    /** Clinical phase. Required. */
    @field:NotNull(message = "Trial phase is required")
    val phase: TrialPhase,

    /** Operational status. Defaults to PLANNING. */
    val status: TrialStatus = TrialStatus.PLANNING,

    /** Target participant count. Must be at least 1 if provided. */
    @field:Min(value = 1, message = "Target enrollment must be at least 1")
    val targetEnrollment: Int? = null,

    /** Current participant count. Cannot be negative if provided. */
    @field:Min(value = 0, message = "Actual enrollment cannot be negative")
    val actualEnrollment: Int? = null,

    /** Trial start date. */
    val startDate: LocalDate? = null,

    /** Trial end date. Must be on or after [startDate] (validated in service layer). */
    val endDate: LocalDate? = null,

    /** Name of the principal investigator. */
    @field:Size(max = 255)
    val principalInvestigator: String? = null,

    /** Sponsoring organisation. */
    @field:Size(max = 255)
    val sponsor: String? = null,

    /** Free-text notes and observations. */
    @field:Size(max = 2000)
    val notes: String? = null
)

/**
 * Response DTO returned when reading a [ClinicalTrial] from the API.
 * Includes denormalised [compoundName] for convenience.
 */
data class ClinicalTrialResponse(
    val id: Long,
    val compoundId: Long,
    /** Name of the compound being tested — included to avoid extra lookups on the client. */
    val compoundName: String,
    val trialId: String,
    val title: String,
    val phase: TrialPhase,
    val status: TrialStatus,
    val targetEnrollment: Int?,
    val actualEnrollment: Int?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val principalInvestigator: String?,
    val sponsor: String?,
    val notes: String?,
    val createdAt: LocalDateTime
) {
    companion object {
        /**
         * Build a [ClinicalTrialResponse] from a [ClinicalTrial] entity.
         *
         * @param t The entity to convert
         * @return Populated response DTO
         */
        fun from(t: ClinicalTrial): ClinicalTrialResponse = ClinicalTrialResponse(
            id = t.id,
            compoundId = t.compound.id,
            compoundName = t.compound.name,
            trialId = t.trialId,
            title = t.title,
            phase = t.phase,
            status = t.status,
            targetEnrollment = t.targetEnrollment,
            actualEnrollment = t.actualEnrollment,
            startDate = t.startDate,
            endDate = t.endDate,
            principalInvestigator = t.principalInvestigator,
            sponsor = t.sponsor,
            notes = t.notes,
            createdAt = t.createdAt
        )
    }
}

// =============================================================================
// Inventory DTOs
// =============================================================================

/**
 * Request body for creating or updating a [CompoundInventory] lot.
 */
data class InventoryRequest(

    /** ID of the [Compound] this lot belongs to. Required. */
    @field:NotNull(message = "Compound ID is required")
    val compoundId: Long,

    /**
     * Unique manufacturer or internal lot number.
     * Example: "ASP-2024-001"
     */
    @field:NotBlank(message = "Lot number is required")
    @field:Size(max = 100)
    val lotNumber: String,

    /** Quantity in the specified [unit]. Must be greater than zero. */
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than zero")
    val quantity: Double,

    /**
     * Unit of measurement for [quantity].
     * Examples: "mg", "g", "kg", "mL"
     */
    @field:NotBlank(message = "Unit is required")
    @field:Size(max = 20)
    val unit: String,

    /** Required storage conditions. Defaults to ROOM_TEMP. */
    val storageCondition: StorageCondition = StorageCondition.ROOM_TEMP,

    /** Physical location identifier, e.g. "Freezer B2, Box 4". */
    @field:Size(max = 255)
    val location: String? = null,

    /** Date after which this lot must not be used. Must be after [receivedDate] (validated in service). */
    val expiryDate: LocalDate? = null,

    /** Date this lot was received. Defaults to today. Cannot be in the future (validated in service). */
    val receivedDate: LocalDate = LocalDate.now(),

    /** Supplier name, e.g. "Sigma-Aldrich" or "Internal Synthesis". */
    @field:Size(max = 255)
    val supplier: String? = null,

    /** Chemical purity as a percentage. Must be between 0 and 100 if provided. */
    @field:DecimalMin(value = "0.0", message = "Purity cannot be negative")
    @field:DecimalMax(value = "100.0", message = "Purity cannot exceed 100%")
    val purityPercent: Double? = null
)

/**
 * Response DTO returned when reading a [CompoundInventory] from the API.
 */
data class InventoryResponse(
    val id: Long,
    val compoundId: Long,
    /** Name of the parent compound — included to avoid extra lookups on the client. */
    val compoundName: String,
    val lotNumber: String,
    val quantity: Double,
    val unit: String,
    val storageCondition: StorageCondition,
    val location: String?,
    val expiryDate: LocalDate?,
    val receivedDate: LocalDate,
    val supplier: String?,
    val purityPercent: Double?,
    /** False if this lot has been deactivated (soft-deleted). */
    val isActive: Boolean,
    val createdAt: LocalDateTime
) {
    companion object {
        /**
         * Build an [InventoryResponse] from a [CompoundInventory] entity.
         *
         * @param i The entity to convert
         * @return Populated response DTO
         */
        fun from(i: CompoundInventory): InventoryResponse = InventoryResponse(
            id = i.id,
            compoundId = i.compound.id,
            compoundName = i.compound.name,
            lotNumber = i.lotNumber,
            quantity = i.quantity,
            unit = i.unit,
            storageCondition = i.storageCondition,
            location = i.location,
            expiryDate = i.expiryDate,
            receivedDate = i.receivedDate,
            supplier = i.supplier,
            purityPercent = i.purityPercent,
            isActive = i.isActive,
            createdAt = i.createdAt
        )
    }
}

// =============================================================================
// Dashboard DTO
// =============================================================================

/**
 * Aggregated statistics for the dashboard endpoint.
 * All fields are computed in [com.drugtracker.service.CompoundService.getDashboardStats].
 */
data class DashboardStats(
    /** Total number of compound records in the database. */
    val totalCompounds: Long,

    /** Compound count grouped by [CompoundStatus] name. */
    val byStatus: Map<String, Long>,

    /** Compound count grouped by [TherapeuticArea] name. */
    val byTherapeuticArea: Map<String, Long>,

    /** Count of trials.html with status ACTIVE or RECRUITING. */
    val activeTrials: Long,

    /** Count of compounds flagged as controlled substances. */
    val controlledSubstances: Long,

    /** Count of inventory lots below a minimum quantity threshold (reserved for future use). */
    val lowInventoryAlerts: Int,

    /** Count of inventory lots expiring within the next 30 days. */
    val expiringSoonAlerts: Int
)
