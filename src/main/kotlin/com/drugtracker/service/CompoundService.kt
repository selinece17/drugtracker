package com.drugtracker.service

import com.drugtracker.model.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import com.drugtracker.repository.CompoundRepository
import com.drugtracker.repository.ClinicalTrialRepository
import com.drugtracker.repository.CompoundInventoryRepository



/**
 * Service layer for all [Compound] business logic.
 *
 * Marked @Transactional so that lazy-loaded collections ([Compound.trials],
 * [Compound.inventory]) are accessible within the same Hibernate session as the
 * initial entity fetch. Read-only methods use readOnly=true for performance.
 *
 * Validation rules enforced here (beyond bean validation):
 *  - Compound name must be globally unique (case-insensitive)
 *  - Compound must exist before update or delete
 */
@Service
@Transactional
class CompoundService(
    private val compoundRepo: CompoundRepository,
    private val trialRepo: ClinicalTrialRepository,
    private val inventoryRepo: CompoundInventoryRepository
) {

    /**
     * Retrieve all compounds.
     *
     * @return Full list of all compound records
     */
    @Transactional(readOnly = true)
    fun getAll(): List<CompoundResponse> =
        compoundRepo.findAll().map { CompoundResponse.from(it) }

    /**
     * Retrieve a single compound by its database ID.
     *
     * @param id Primary key of the compound
     * @return The compound response DTO
     * @throws NoSuchElementException if no compound with that ID exists
     */
    @Transactional(readOnly = true)
    fun getById(id: Long): CompoundResponse =
        CompoundResponse.from(findCompoundById(id))

    /**
     * Search compounds by partial name match (case-insensitive).
     *
     * @param name Substring to search for within compound names
     * @return Matching compound DTOs, possibly empty
     */
    @Transactional(readOnly = true)
    fun search(name: String): List<CompoundResponse> =
        compoundRepo.findByNameContainingIgnoreCase(name).map { CompoundResponse.from(it) }

    /**
     * Retrieve all compounds at a specific development stage.
     *
     * @param status The [CompoundStatus] to filter by
     * @return Matching compounds
     */
    @Transactional(readOnly = true)
    fun getByStatus(status: CompoundStatus): List<CompoundResponse> =
        compoundRepo.findByStatus(status).map { CompoundResponse.from(it) }

    /**
     * Retrieve all compounds targeting a specific therapeutic area.
     *
     * @param area The [TherapeuticArea] to filter by
     * @return Matching compounds
     */
    @Transactional(readOnly = true)
    fun getByTherapeuticArea(area: TherapeuticArea): List<CompoundResponse> =
        compoundRepo.findByTherapeuticArea(area).map { CompoundResponse.from(it) }

    /**
     * Retrieve all DEA-scheduled controlled substances.
     *
     * @return Compounds flagged as controlled substances, sorted by DEA schedule
     */
    @Transactional(readOnly = true)
    fun getControlledSubstances(): List<CompoundResponse> =
        compoundRepo.findControlledSubstances().map { CompoundResponse.from(it) }

    /**
     * Create a new compound record.
     *
     * @param req Validated request DTO with compound details
     * @return The newly created compound
     * @throws IllegalArgumentException if the compound name is already taken
     */
    fun create(req: CompoundRequest): CompoundResponse {
        // Enforce unique name constraint at the service layer for a clear error message
        if (compoundRepo.existsByNameIgnoreCase(req.name)) {
            throw IllegalArgumentException("A compound named '${req.name}' already exists")
        }

        val compound = Compound(
            name = req.name.trim(),
            casNumber = req.casNumber?.trim(),
            molecularFormula = req.molecularFormula.trim(),
            molecularWeight = req.molecularWeight,
            status = req.status,
            therapeuticArea = req.therapeuticArea,
            description = req.description?.trim(),
            targetProtein = req.targetProtein?.trim(),
            mechanismOfAction = req.mechanismOfAction?.trim(),
            isControlledSubstance = req.isControlledSubstance,
            deaSchedule = if (req.isControlledSubstance) req.deaSchedule else null
        )
        return CompoundResponse.from(compoundRepo.save(compound))
    }

    /**
     * Update an existing compound.
     * Preserves creation timestamp and relationships; sets a new updatedAt timestamp.
     *
     * @param id  Primary key of the compound to update
     * @param req Validated request DTO with new field values
     * @return The updated compound
     * @throws NoSuchElementException   if no compound with that ID exists
     * @throws IllegalArgumentException if the new name conflicts with another compound
     */
    fun update(id: Long, req: CompoundRequest): CompoundResponse {
        val existing = findCompoundById(id)

        // Allow the name to remain the same; only block if it belongs to a DIFFERENT compound
        if (!existing.name.equals(req.name, ignoreCase = true) &&
            compoundRepo.existsByNameIgnoreCase(req.name)) {
            throw IllegalArgumentException("A compound named '${req.name}' already exists")
        }

        // Use the custom copy() method which also sets updatedAt = now()
        val updated = existing.copy(
            name = req.name.trim(),
            casNumber = req.casNumber?.trim(),
            molecularFormula = req.molecularFormula.trim(),
            molecularWeight = req.molecularWeight,
            status = req.status,
            therapeuticArea = req.therapeuticArea,
            description = req.description?.trim(),
            targetProtein = req.targetProtein?.trim(),
            mechanismOfAction = req.mechanismOfAction?.trim(),
            isControlledSubstance = req.isControlledSubstance,
            deaSchedule = if (req.isControlledSubstance) req.deaSchedule else null
        )
        return CompoundResponse.from(compoundRepo.save(updated))
    }

    /**
     * Permanently delete a compound and its associated trials.html and inventory
     * (cascade delete is configured on the entity).
     *
     * @param id Primary key of the compound to delete
     * @throws NoSuchElementException if no compound with that ID exists
     */
    fun delete(id: Long) {
        if (!compoundRepo.existsById(id)) {
            throw NoSuchElementException("Compound with ID $id not found")
        }
        compoundRepo.deleteById(id)
    }

    /**
     * Compute high-level dashboard statistics about the compound portfolio.
     *
     * @return [DashboardStats] aggregating counts across compounds, trials.html, and inventory
     */
    @Transactional(readOnly = true)
    fun getDashboardStats(): DashboardStats {
        val compounds = compoundRepo.findAll()

        // Group compound counts by status and therapeutic area
        val byStatus = compounds
            .groupBy { it.status.name }
            .mapValues { it.value.size.toLong() }

        val byArea = compounds
            .groupBy { it.therapeuticArea.name }
            .mapValues { it.value.size.toLong() }

        // Count in-progress trials.html (recruiting + active)
        val activeTrialCount = trialRepo.findByStatus(TrialStatus.ACTIVE).size.toLong() +
            trialRepo.findByStatus(TrialStatus.RECRUITING).size.toLong()

        // Items expiring within 30 days from today
        val cutoff = LocalDate.now().plusDays(30)
        val expiringSoon = inventoryRepo.findExpiringSoon(cutoff).size

        return DashboardStats(
            totalCompounds = compounds.size.toLong(),
            byStatus = byStatus,
            byTherapeuticArea = byArea,
            activeTrials = activeTrialCount,
            controlledSubstances = compounds.count { it.isControlledSubstance }.toLong(),
            lowInventoryAlerts = 0,      // Reserved for future threshold-based alerting
            expiringSoonAlerts = expiringSoon
        )
    }

    /**
     * Internal helper: fetch a compound or throw a meaningful exception.
     *
     * @param id Primary key
     * @return The found [Compound] entity
     * @throws NoSuchElementException if not found
     */
    private fun findCompoundById(id: Long): Compound =
        compoundRepo.findById(id)
            .orElseThrow { NoSuchElementException("Compound with ID $id not found") }
}
