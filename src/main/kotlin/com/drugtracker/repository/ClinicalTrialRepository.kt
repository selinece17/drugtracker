package com.drugtracker.repository

import com.drugtracker.model.ClinicalTrial
import com.drugtracker.model.Compound
import com.drugtracker.model.TrialPhase
import com.drugtracker.model.TrialStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * JPA repository for [ClinicalTrial] entities.
 */
@Repository
interface ClinicalTrialRepository : JpaRepository<ClinicalTrial, Long> {

    /**
     * Find all trials.html associated with a given compound.
     *
     * @param compoundId The ID of the parent [Compound]
     * @return All trials.html for that compound, possibly empty
     */
    fun findByCompoundId(compoundId: Long): List<ClinicalTrial>

    /**
     * Find trials.html by current operational status.
     *
     * @param status The [TrialStatus] to filter by
     * @return Matching trials.html, possibly empty
     */
    fun findByStatus(status: TrialStatus): List<ClinicalTrial>

    /**
     * Find trials.html by clinical phase.
     *
     * @param phase The [TrialPhase] to filter by
     * @return Matching trials.html
     */
    fun findByPhase(phase: TrialPhase): List<ClinicalTrial>

    /**
     * Look up a trial by its external ID (e.g. NCT number).
     *
     * @param trialId The external trial identifier
     * @return The matching trial, or null if not found
     */
    fun findByTrialId(trialId: String): ClinicalTrial?

    /**
     * Check whether an external trial ID is already registered.
     *
     * @param trialId The external identifier to check
     * @return true if already registered
     */
    fun existsByTrialId(trialId: String): Boolean
}