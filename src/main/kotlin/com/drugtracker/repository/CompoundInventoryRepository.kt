package com.drugtracker.repository

import com.drugtracker.model.Compound
import com.drugtracker.model.CompoundInventory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * JPA repository for [CompoundInventory] entities.
 */
@Repository
interface CompoundInventoryRepository : JpaRepository<CompoundInventory, Long> {

    /**
     * Find all inventory lots belonging to a specific compound.
     *
     * @param compoundId The ID of the parent [Compound]
     * @return All inventory lots for that compound
     */
    fun findByCompoundId(compoundId: Long): List<CompoundInventory>

    /**
     * Look up an inventory lot by its manufacturer or internal lot number.
     *
     * @param lotNumber The unique lot identifier
     * @return The matching inventory record, or null if not found
     */
    fun findByLotNumber(lotNumber: String): CompoundInventory?

    /**
     * Find all inventory lots that are currently active (not deactivated).
     *
     * @return All active inventory records
     */
    fun findByIsActiveTrue(): List<CompoundInventory>

    /**
     * Check whether a lot number is already in use.
     *
     * @param lotNumber The lot number to check
     * @return true if already registered
     */
    fun existsByLotNumber(lotNumber: String): Boolean

    /**
     * Find active inventory lots expiring on or before a given cutoff date.
     *
     * Note: The cutoff date is passed as a parameter (rather than computed in JPQL)
     * for database portability. H2 does not support CURRENT_DATE arithmetic in JPQL.
     *
     * @param cutoffDate Lots with expiryDate <= this date will be returned
     * @return Active lots expiring at or before the cutoff
     */
    @Query("""
        SELECT i FROM CompoundInventory i 
        WHERE i.expiryDate IS NOT NULL 
          AND i.expiryDate <= :cutoffDate 
          AND i.isActive = true 
        ORDER BY i.expiryDate ASC
    """)
    fun findExpiringSoon(@Param("cutoffDate") cutoffDate: LocalDate): List<CompoundInventory>
}
