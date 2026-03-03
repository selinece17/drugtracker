package com.drugtracker.service

import com.drugtracker.model.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.example.com.drugtracker.repository.CompoundRepository
import org.example.com.drugtracker.repository.ClinicalTrialRepository

/**
 * Service layer for [ClinicalTrial] business logic.
 *
 * Enforces:
 *  - Trial ID uniqueness (e.g. NCT numbers must not be duplicated)
 *  - End date must be on or after start date when both are provided
 *  - Referenced [Compound] must exist before a trial is created or updated
 *
 * Marked @Transactional to keep the Hibernate session open so that the
 * lazy-loaded [ClinicalTrial.compound] reference can be accessed when building
 * response DTOs.
 */
@Service
@Transactional
class ClinicalTrialService(
    private val trialRepo: ClinicalTrialRepository,
    private val compoundRepo: CompoundRepository
) {

    /**
     * Retrieve all clinical trials.html.
     *
     * @return Full list of all trial records
     */
    @Transactional(readOnly = true)
    fun getAll(): List<ClinicalTrialResponse> =
        trialRepo.findAll().map { ClinicalTrialResponse.from(it) }

    /**
     * Retrieve a single trial by its database ID.
     *
     * @param id Primary key
     * @return The trial response DTO
     * @throws NoSuchElementException if no trial with that ID exists
     */
    @Transactional(readOnly = true)
    fun getById(id: Long): ClinicalTrialResponse =
        ClinicalTrialResponse.from(findById(id))

    /**
     * Retrieve all trials.html linked to a specific compound.
     *
     * @param compoundId The ID of the parent compound
     * @return Trials associated with that compound, possibly empty
     */
    @Transactional(readOnly = true)
    fun getByCompound(compoundId: Long): List<ClinicalTrialResponse> =
        trialRepo.findByCompoundId(compoundId).map { ClinicalTrialResponse.from(it) }

    /**
     * Retrieve trials.html by operational status.
     *
     * @param status The [TrialStatus] to filter by
     * @return Matching trials.html
     */
    @Transactional(readOnly = true)
    fun getByStatus(status: TrialStatus): List<ClinicalTrialResponse> =
        trialRepo.findByStatus(status).map { ClinicalTrialResponse.from(it) }

    /**
     * Retrieve trials.html by clinical phase.
     *
     * @param phase The [TrialPhase] to filter by
     * @return Matching trials.html
     */
    @Transactional(readOnly = true)
    fun getByPhase(phase: TrialPhase): List<ClinicalTrialResponse> =
        trialRepo.findByPhase(phase).map { ClinicalTrialResponse.from(it) }

    /**
     * Create a new clinical trial linked to an existing compound.
     *
     * @param req Validated request DTO
     * @return The created trial DTO
     * @throws IllegalArgumentException if [ClinicalTrialRequest.trialId] is already registered,
     *                                  or if end date precedes start date
     * @throws NoSuchElementException   if the referenced compound does not exist
     */
    fun create(req: ClinicalTrialRequest): ClinicalTrialResponse {
        // Ensure trial ID is unique
        if (trialRepo.existsByTrialId(req.trialId)) {
            throw IllegalArgumentException("Trial ID '${req.trialId}' is already registered")
        }

        // Validate date ordering
        validateDates(req)

        val compound = compoundRepo.findById(req.compoundId!!)
            .orElseThrow { NoSuchElementException("Compound with ID ${req.compoundId} not found") }

        val trial = ClinicalTrial(
            compound = compound,
            trialId = req.trialId.trim(),
            title = req.title.trim(),
            phase = req.phase,
            status = req.status,
            targetEnrollment = req.targetEnrollment,
            actualEnrollment = req.actualEnrollment,
            startDate = req.startDate,
            endDate = req.endDate,
            principalInvestigator = req.principalInvestigator?.trim(),
            sponsor = req.sponsor?.trim(),
            notes = req.notes?.trim()
        )
        return ClinicalTrialResponse.from(trialRepo.save(trial))
    }

    /**
     * Update an existing clinical trial.
     *
     * @param id  Primary key of the trial to update
     * @param req Validated request DTO with new field values
     * @return The updated trial DTO
     * @throws NoSuchElementException   if neither the trial nor referenced compound exists
     * @throws IllegalArgumentException if trial ID conflicts with another trial,
     *                                  or end date precedes start date
     */
    fun update(id: Long, req: ClinicalTrialRequest): ClinicalTrialResponse {
        val existing = findById(id)

        // Allow trial ID to remain the same; only block if it belongs to a different record
        if (existing.trialId != req.trialId && trialRepo.existsByTrialId(req.trialId)) {
            throw IllegalArgumentException("Trial ID '${req.trialId}' is already registered")
        }

        // Validate date ordering
        validateDates(req)

        val compound = compoundRepo.findById(req.compoundId!!)
            .orElseThrow { NoSuchElementException("Compound with ID ${req.compoundId} not found") }

        val updated = existing.copy(
            compound = compound,
            trialId = req.trialId.trim(),
            title = req.title.trim(),
            phase = req.phase,
            status = req.status,
            targetEnrollment = req.targetEnrollment,
            actualEnrollment = req.actualEnrollment,
            startDate = req.startDate,
            endDate = req.endDate,
            principalInvestigator = req.principalInvestigator?.trim(),
            sponsor = req.sponsor?.trim(),
            notes = req.notes?.trim()
        )
        return ClinicalTrialResponse.from(trialRepo.save(updated))
    }

    /**
     * Delete a trial record.
     *
     * @param id Primary key of the trial to delete
     * @throws NoSuchElementException if no trial with that ID exists
     */
    fun delete(id: Long) {
        if (!trialRepo.existsById(id)) {
            throw NoSuchElementException("Trial with ID $id not found")
        }
        trialRepo.deleteById(id)
    }

    /**
     * Validate that endDate is not before startDate when both are supplied.
     *
     * @param req The trial request to validate
     * @throws IllegalArgumentException if end date precedes start date
     */
    private fun validateDates(req: ClinicalTrialRequest) {
        if (req.startDate != null && req.endDate != null && req.endDate.isBefore(req.startDate)) {
            throw IllegalArgumentException("End date (${req.endDate}) cannot be before start date (${req.startDate})")
        }
    }

    /**
     * Internal helper: fetch a trial or throw a meaningful exception.
     *
     * @param id Primary key
     * @return The found [ClinicalTrial] entity
     * @throws NoSuchElementException if not found
     */
    private fun findById(id: Long): ClinicalTrial =
        trialRepo.findById(id)
            .orElseThrow { NoSuchElementException("Trial with ID $id not found") }
}
