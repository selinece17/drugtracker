package org.example.com.drugtracker.repository

import com.drugtracker.model.Compound
import com.drugtracker.model.CompoundStatus
import com.drugtracker.model.TherapeuticArea
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository


/**
 * JPA repository for [Compound] entities.
 *
 * Spring Data auto-implements all standard CRUD operations via [JpaRepository].
 * Custom finder methods use Spring Data's method naming convention, and complex
 * queries use JPQL via @Query.
 */
@Repository
interface CompoundRepository : JpaRepository<Compound, Long> {

    /**
     * Find all compounds at a given development stage.
     *
     * @param status The [CompoundStatus] to filter by
     * @return List of matching compounds, possibly empty
     */
    fun findByStatus(status: CompoundStatus): List<Compound>

    /**
     * Find all compounds targeting a specific therapeutic area.
     *
     * @param area The [TherapeuticArea] to filter by
     * @return List of matching compounds, possibly empty
     */
    fun findByTherapeuticArea(area: TherapeuticArea): List<Compound>

    /**
     * Case-insensitive substring search on compound name.
     * Useful for front-end search/autocomplete features.
     *
     * @param name Substring to search for within compound names
     * @return List of compounds whose name contains the given string
     */
    fun findByNameContainingIgnoreCase(name: String): List<Compound>

    /**
     * Find a compound by its exact CAS Registry Number.
     *
     * @param casNumber The CAS number, e.g. "50-78-2"
     * @return The matching compound, or null if not found
     */
    fun findByCasNumber(casNumber: String): Compound?

    /**
     * Check whether a compound name already exists (case-insensitive).
     * Used to enforce uniqueness before creating a new compound.
     *
     * @param name The compound name to check
     * @return true if a compound with that name already exists
     */
    fun existsByNameIgnoreCase(name: String): Boolean

    /**
     * Retrieve all DEA-scheduled controlled substances.
     *
     * @return All compounds with [Compound.isControlledSubstance] == true
     */
    @Query("SELECT c FROM Compound c WHERE c.isControlledSubstance = true ORDER BY c.deaSchedule ASC")
    fun findControlledSubstances(): List<Compound>

    /**
     * Find compounds whose status is one of the provided values.
     * Useful for pipeline views covering multiple stages at once.
     *
     * @param statuses List of [CompoundStatus] values to include
     * @return Matching compounds
     */
    @Query("SELECT c FROM Compound c WHERE c.status IN :statuses ORDER BY c.name ASC")
    fun findByStatuses(@Param("statuses") statuses: List<CompoundStatus>): List<Compound>
}