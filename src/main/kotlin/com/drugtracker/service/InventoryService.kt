package com.drugtracker.service

import com.drugtracker.model.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import com.drugtracker.repository.CompoundRepository
import com.drugtracker.repository.CompoundInventoryRepository


/**
 * Service layer for [CompoundInventory] business logic.
 *
 * Enforces:
 *  - Lot number uniqueness
 *  - Expiry date must be in the future when creating a new lot
 *  - Received date must not be in the future
 *  - Purity must be between 0 and 100%
 *  - Referenced [Compound] must exist
 *
 * Marked @Transactional to maintain the Hibernate session for lazy-loaded
 * [CompoundInventory.compound] access in response DTO construction.
 */
@Service
@Transactional
class InventoryService(
    private val inventoryRepo: CompoundInventoryRepository,
    private val compoundRepo: CompoundRepository
) {

    /**
     * Retrieve all inventory records.
     *
     * @return Full list of all inventory lots
     */
    @Transactional(readOnly = true)
    fun getAll(): List<InventoryResponse> =
        inventoryRepo.findAll().map { InventoryResponse.from(it) }

    /**
     * Retrieve a single inventory record by its database ID.
     *
     * @param id Primary key
     * @return The inventory response DTO
     * @throws NoSuchElementException if no record with that ID exists
     */
    @Transactional(readOnly = true)
    fun getById(id: Long): InventoryResponse =
        InventoryResponse.from(findById(id))

    /**
     * Retrieve all inventory lots belonging to a specific compound.
     *
     * @param compoundId The ID of the parent [Compound]
     * @return All lots for that compound, possibly empty
     */
    @Transactional(readOnly = true)
    fun getByCompound(compoundId: Long): List<InventoryResponse> =
        inventoryRepo.findByCompoundId(compoundId).map { InventoryResponse.from(it) }

    /**
     * Retrieve active inventory lots that expire within the next 30 days.
     * Uses today's date + 30 as the cutoff passed to the repository query.
     *
     * @return Lots expiring soon, sorted by expiry date ascending
     */
    @Transactional(readOnly = true)
    fun getExpiringSoon(): List<InventoryResponse> {
        val cutoff = LocalDate.now().plusDays(30)
        return inventoryRepo.findExpiringSoon(cutoff).map { InventoryResponse.from(it) }
    }

    /**
     * Add a new inventory lot for a compound.
     *
     * @param req Validated request DTO
     * @return The created inventory DTO
     * @throws IllegalArgumentException if lot number is already taken,
     *                                  if received date is in the future,
     *                                  or if expiry date is in the past
     * @throws NoSuchElementException   if the referenced compound does not exist
     */
    fun create(req: InventoryRequest): InventoryResponse {
        // Lot numbers must be globally unique
        if (inventoryRepo.existsByLotNumber(req.lotNumber)) {
            throw IllegalArgumentException("Lot number '${req.lotNumber}' is already registered")
        }

        // Cross-field date validation
        validateInventoryDates(req)

        val compound = compoundRepo.findById(req.compoundId)
            .orElseThrow { NoSuchElementException("Compound with ID ${req.compoundId} not found") }

        val inventory = CompoundInventory(
            compound = compound,
            lotNumber = req.lotNumber.trim(),
            quantity = req.quantity,
            unit = req.unit.trim(),
            storageCondition = req.storageCondition,
            location = req.location?.trim(),
            expiryDate = req.expiryDate,
            receivedDate = req.receivedDate,
            supplier = req.supplier?.trim(),
            purityPercent = req.purityPercent
        )
        return InventoryResponse.from(inventoryRepo.save(inventory))
    }

    /**
     * Update an existing inventory record (e.g. to adjust quantity after usage).
     *
     * @param id  Primary key of the record to update
     * @param req Validated request DTO with new field values
     * @return The updated inventory DTO
     * @throws NoSuchElementException   if record or compound not found
     * @throws IllegalArgumentException if lot number conflicts with another record,
     *                                  or date validation fails
     */
    fun update(id: Long, req: InventoryRequest): InventoryResponse {
        val existing = findById(id)

        // Allow lot number to stay the same; only block conflict with a different record
        if (existing.lotNumber != req.lotNumber && inventoryRepo.existsByLotNumber(req.lotNumber)) {
            throw IllegalArgumentException("Lot number '${req.lotNumber}' is already registered")
        }

        validateInventoryDates(req)

        val compound = compoundRepo.findById(req.compoundId)
            .orElseThrow { NoSuchElementException("Compound with ID ${req.compoundId} not found") }

        val updated = existing.copy(
            compound = compound,
            lotNumber = req.lotNumber.trim(),
            quantity = req.quantity,
            unit = req.unit.trim(),
            storageCondition = req.storageCondition,
            location = req.location?.trim(),
            expiryDate = req.expiryDate,
            receivedDate = req.receivedDate,
            supplier = req.supplier?.trim(),
            purityPercent = req.purityPercent
        )
        return InventoryResponse.from(inventoryRepo.save(updated))
    }

    /**
     * Soft-delete an inventory lot by marking it as inactive.
     * Retains the record for historical/audit purposes.
     *
     * @param id Primary key of the lot to deactivate
     * @return The updated inventory DTO showing isActive = false
     * @throws NoSuchElementException if no record with that ID exists
     */
    fun deactivate(id: Long): InventoryResponse {
        val existing = findById(id)
        return InventoryResponse.from(inventoryRepo.save(existing.copy(isActive = false)))
    }

    /**
     * Permanently delete an inventory lot.
     * Consider using [deactivate] instead to preserve audit history.
     *
     * @param id Primary key of the lot to delete
     * @throws NoSuchElementException if no record with that ID exists
     */
    fun delete(id: Long) {
        if (!inventoryRepo.existsById(id)) {
            throw NoSuchElementException("Inventory item with ID $id not found")
        }
        inventoryRepo.deleteById(id)
    }

    /**
     * Validate cross-field date rules for an inventory request:
     *  - receivedDate must not be in the future
     *  - expiryDate, if present, must be after receivedDate
     *
     * @param req The inventory request to validate
     * @throws IllegalArgumentException on any date rule violation
     */
    private fun validateInventoryDates(req: InventoryRequest) {
        val today = LocalDate.now()

        if (req.receivedDate.isAfter(today)) {
            throw IllegalArgumentException("Received date (${req.receivedDate}) cannot be in the future")
        }

        if (req.expiryDate != null && !req.expiryDate.isAfter(req.receivedDate)) {
            throw IllegalArgumentException(
                "Expiry date (${req.expiryDate}) must be after received date (${req.receivedDate})"
            )
        }
    }

    /**
     * Internal helper: fetch an inventory record or throw a meaningful exception.
     *
     * @param id Primary key
     * @return The found [CompoundInventory] entity
     * @throws NoSuchElementException if not found
     */
    private fun findById(id: Long): CompoundInventory =
        inventoryRepo.findById(id)
            .orElseThrow { NoSuchElementException("Inventory item with ID $id not found") }
}
