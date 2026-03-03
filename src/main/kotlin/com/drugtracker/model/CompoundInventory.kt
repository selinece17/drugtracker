package com.drugtracker.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Required storage conditions for a compound lot.
 * Determines which type of storage location is suitable.
 */
enum class StorageCondition {
    ROOM_TEMP,    // 15–25°C standard lab storage
    REFRIGERATED, // 2–8°C (standard fridge)
    FROZEN,       // -20°C freezer
    ULTRA_COLD,   // -80°C ultra-low freezer
    CONTROLLED    // DEA-controlled substance vault
}

/**
 * Entity representing a physical inventory lot of a [Compound].
 *
 * Each lot has a unique lot number, quantity, storage location, and expiry date.
 * Lots can be deactivated (soft-deleted) rather than physically removed to preserve
 * audit history.
 *
 * Uses a standard class to avoid JPA issues with lazy-loaded [Compound]
 * reference in data-class-generated equals/hashCode.
 */
@Entity
@Table(
    name = "compound_inventory",
    indexes = [
        Index(name = "idx_inventory_compound", columnList = "compound_id"),
        Index(name = "idx_inventory_expiry", columnList = "expiry_date"),
        Index(name = "idx_inventory_active", columnList = "is_active")
    ]
)
class CompoundInventory(

    /** Auto-generated primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * The compound this inventory lot belongs to.
     * Many inventory lots can reference the same compound (many-to-one).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compound_id", nullable = false)
    val compound: Compound,

    /**
     * Manufacturer or internal lot number — unique identifier for this physical batch.
     * Example: "ASP-2024-001"
     */
    @field:NotBlank(message = "Lot number is required")
    @field:Size(max = 100)
    @Column(name = "lot_number", nullable = false, unique = true)
    val lotNumber: String,

    /**
     * Quantity of the compound in this lot.
     * Must be greater than zero.
     */
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than zero")
    @Column(nullable = false)
    val quantity: Double,

    /**
     * Unit of measurement for [quantity].
     * Common values: "mg", "g", "kg", "mL", "L"
     */
    @field:NotBlank(message = "Unit is required")
    @field:Size(max = 20)
    val unit: String,

    /** Required storage condition for this lot. Defaults to room temperature. */
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_condition", nullable = false)
    val storageCondition: StorageCondition = StorageCondition.ROOM_TEMP,

    /** Physical location of the lot, e.g. "Lab-A, Shelf 3" or "Freezer B2, Box 4". */
    @field:Size(max = 255)
    val location: String? = null,

    /** Date after which this lot should no longer be used. Null means no expiry. */
    @Column(name = "expiry_date")
    val expiryDate: LocalDate? = null,

    /** Date this lot was received or synthesised. Defaults to today. */
    @Column(name = "received_date", nullable = false)
    val receivedDate: LocalDate = LocalDate.now(),

    /** Name of the external supplier or "Internal Synthesis" for in-house batches. */
    @field:Size(max = 255)
    val supplier: String? = null,

    /**
     * Chemical purity of the lot as a percentage (0–100).
     * Higher purity (e.g. ≥98%) is required for clinical use.
     */
    @field:DecimalMin(value = "0.0", message = "Purity cannot be negative")
    @field:DecimalMax(value = "100.0", message = "Purity cannot exceed 100%")
    @Column(name = "purity_percent")
    val purityPercent: Double? = null,

    /**
     * Whether this lot is currently active/in-use.
     * Set to false to soft-delete (deactivate) a lot while retaining audit history.
     */
    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    /** Timestamp when this inventory record was created. Immutable. */
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

) {
    /**
     * Creates a copy of this inventory record with selective field overrides.
     * Preserves id and createdAt from the original.
     */
    fun copy(
        compound: Compound = this.compound,
        lotNumber: String = this.lotNumber,
        quantity: Double = this.quantity,
        unit: String = this.unit,
        storageCondition: StorageCondition = this.storageCondition,
        location: String? = this.location,
        expiryDate: LocalDate? = this.expiryDate,
        receivedDate: LocalDate = this.receivedDate,
        supplier: String? = this.supplier,
        purityPercent: Double? = this.purityPercent,
        isActive: Boolean = this.isActive
    ): CompoundInventory = CompoundInventory(
        id = this.id,
        compound = compound,
        lotNumber = lotNumber,
        quantity = quantity,
        unit = unit,
        storageCondition = storageCondition,
        location = location,
        expiryDate = expiryDate,
        receivedDate = receivedDate,
        supplier = supplier,
        purityPercent = purityPercent,
        isActive = isActive,
        createdAt = this.createdAt
    )

    /** ID-based equality to avoid lazy-loading the compound relationship. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CompoundInventory) return false
        if (id == 0L) return false
        return id == other.id
    }

    override fun hashCode(): Int = if (id == 0L) System.identityHashCode(this) else id.hashCode()

    override fun toString(): String =
        "CompoundInventory(id=$id, lotNumber='$lotNumber', quantity=$quantity $unit, isActive=$isActive)"
}
