package com.carbonmarketplace.verificationservice.repository;

import com.carbonmarketplace.verificationservice.entity.CarbonCredit;
import com.carbonmarketplace.verificationservice.entity.CarbonCredit.CreditStatus;
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
 * Repository for carbon credit operations.
 */
@Repository
public interface CarbonCreditRepository extends JpaRepository<CarbonCredit, UUID> {

    Optional<CarbonCredit> findBySerialNumber(String serialNumber);

    List<CarbonCredit> findByVerificationId(UUID verificationId);

    Page<CarbonCredit> findByOwnerId(UUID ownerId, Pageable pageable);

    Page<CarbonCredit> findByOwnerIdAndStatus(UUID ownerId, CreditStatus status, Pageable pageable);

    List<CarbonCredit> findByOriginalOwnerIdAndStatus(UUID originalOwnerId, CreditStatus status);

    @Query("SELECT COUNT(cc) FROM CarbonCredit cc WHERE cc.serialNumber LIKE :prefix%")
    Long countBySerialNumberPrefix(@Param("prefix") String prefix);

    @Query("SELECT cc FROM CarbonCredit cc WHERE cc.status = :status AND cc.vintageYear = :year")
    List<CarbonCredit> findByStatusAndVintageYear(@Param("status") CreditStatus status, 
                                                  @Param("year") Integer year);

    @Query("SELECT SUM(cc.amountTons) FROM CarbonCredit cc WHERE cc.ownerId = :ownerId " +
           "AND cc.status = :status")
    BigDecimal getTotalCreditsByOwnerAndStatus(@Param("ownerId") UUID ownerId, 
                                               @Param("status") CreditStatus status);

    @Query("SELECT SUM(cc.amountTons) FROM CarbonCredit cc WHERE cc.status = 'ISSUED' " +
           "AND cc.createdAt >= :startDate")
    BigDecimal getTotalCreditsIssuedSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(DISTINCT cc.ownerId) FROM CarbonCredit cc WHERE cc.status = 'ISSUED'")
    Long countUniqueOwners();

    @Query("SELECT cc.region, SUM(cc.amountTons) FROM CarbonCredit cc " +
           "WHERE cc.status = 'ISSUED' GROUP BY cc.region")
    List<Object[]> getCreditDistributionByRegion();

    @Query("SELECT cc.vintageYear, COUNT(cc), SUM(cc.amountTons) FROM CarbonCredit cc " +
           "WHERE cc.status = 'ISSUED' GROUP BY cc.vintageYear ORDER BY cc.vintageYear DESC")
    List<Object[]> getCreditStatisticsByVintageYear();

    @Query("SELECT MAX(CAST(SUBSTRING(cc.serialNumber, LENGTH(:prefix) + 1) AS BIGINT)) " +
           "FROM CarbonCredit cc WHERE cc.serialNumber LIKE :prefix%")
    Long getMaxSerialNumberSequence(@Param("prefix") String prefix);

    @Query("SELECT cc FROM CarbonCredit cc WHERE cc.status = 'LISTED' AND cc.listingId IS NULL")
    List<CarbonCredit> findOrphanedListings();
}
