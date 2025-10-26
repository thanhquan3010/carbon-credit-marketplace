package com.carbonmarketplace.carboncreditservice.repository;

import com.carbonmarketplace.carboncreditservice.entity.CarbonCredit;
import com.carbonmarketplace.carboncreditservice.entity.CarbonCredit.CreditStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CarbonCredit entity
 */
@Repository
public interface CarbonCreditRepository extends JpaRepository<CarbonCredit, UUID> {
    
    Optional<CarbonCredit> findBySerialNumber(String serialNumber);
    
    Page<CarbonCredit> findByOwnerId(UUID ownerId, Pageable pageable);
    
    Page<CarbonCredit> findByOwnerIdAndStatus(UUID ownerId, CreditStatus status, Pageable pageable);
    
    List<CarbonCredit> findByOwnerIdAndStatusAndRetiredAtIsNull(UUID ownerId, CreditStatus status);
    
    @Query("SELECT SUM(c.amountTons) FROM CarbonCredit c WHERE c.ownerId = :ownerId " +
           "AND c.status = :status AND c.retiredAt IS NULL")
    BigDecimal calculateTotalCreditsByOwnerAndStatus(
            @Param("ownerId") UUID ownerId,
            @Param("status") CreditStatus status
    );
    
    @Query("SELECT SUM(c.amountTons) FROM CarbonCredit c WHERE c.ownerId = :ownerId " +
           "AND c.status = 'ISSUED' AND c.retiredAt IS NULL AND c.listingId IS NULL")
    BigDecimal calculateAvailableBalance(@Param("ownerId") UUID ownerId);
    
    @Query("SELECT SUM(c.amountTons) FROM CarbonCredit c WHERE c.ownerId = :ownerId " +
           "AND c.status = 'LISTED'")
    BigDecimal calculateListedBalance(@Param("ownerId") UUID ownerId);
    
    List<CarbonCredit> findByCreditIdIn(List<UUID> creditIds);
    
    Page<CarbonCredit> findByVerificationRequestVerificationId(UUID verificationId, Pageable pageable);
    
    @Query("SELECT c FROM CarbonCredit c WHERE c.ownerId = :ownerId " +
           "AND c.status = 'ISSUED' AND c.retiredAt IS NULL AND c.listingId IS NULL " +
           "ORDER BY c.createdAt ASC")
    List<CarbonCredit> findAvailableCreditsForRetirement(
            @Param("ownerId") UUID ownerId,
            Pageable pageable
    );
    
    @Query("SELECT COUNT(c) FROM CarbonCredit c WHERE c.originalOwnerId = :ownerId " +
           "AND c.createdAt >= :since")
    Long countCreditsIssuedSince(
            @Param("ownerId") UUID ownerId,
            @Param("since") LocalDateTime since
    );
    
    @Query("SELECT MAX(CAST(SUBSTRING(c.serialNumber, LENGTH(c.serialNumber) - 8, 9) AS integer)) " +
           "FROM CarbonCredit c WHERE c.serialNumber LIKE :prefix%")
    Integer findMaxSequenceNumberForPrefix(@Param("prefix") String prefix);
    
    @Query("SELECT c FROM CarbonCredit c WHERE c.vintageYear = :year " +
           "AND c.status = :status ORDER BY c.createdAt DESC")
    Page<CarbonCredit> findByVintageYearAndStatus(
            @Param("year") Integer year,
            @Param("status") CreditStatus status,
            Pageable pageable
    );
}
