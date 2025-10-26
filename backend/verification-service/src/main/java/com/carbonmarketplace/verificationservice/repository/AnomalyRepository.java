package com.carbonmarketplace.verificationservice.repository;

import com.carbonmarketplace.verificationservice.entity.Anomaly;
import com.carbonmarketplace.verificationservice.entity.Anomaly.AnomalyType;
import com.carbonmarketplace.verificationservice.entity.Anomaly.ResolutionStatus;
import com.carbonmarketplace.verificationservice.entity.Anomaly.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for anomaly operations.
 */
@Repository
public interface AnomalyRepository extends JpaRepository<Anomaly, UUID> {

    List<Anomaly> findByVerificationId(UUID verificationId);

    List<Anomaly> findByTripId(UUID tripId);

    List<Anomaly> findByVerificationIdAndResolutionStatus(UUID verificationId, ResolutionStatus status);

    Page<Anomaly> findBySeverity(Severity severity, Pageable pageable);

    Page<Anomaly> findByAnomalyType(AnomalyType type, Pageable pageable);

    @Query("SELECT a FROM Anomaly a WHERE a.verificationId = :verificationId " +
           "AND a.severity IN ('HIGH', 'CRITICAL') AND a.resolutionStatus = 'UNRESOLVED'")
    List<Anomaly> findCriticalUnresolvedByVerification(@Param("verificationId") UUID verificationId);

    @Query("SELECT COUNT(a) FROM Anomaly a WHERE a.verificationId = :verificationId " +
           "AND a.resolutionStatus = :status")
    Long countByVerificationAndStatus(@Param("verificationId") UUID verificationId, 
                                      @Param("status") ResolutionStatus status);

    @Query("SELECT a.anomalyType, COUNT(a) FROM Anomaly a " +
           "WHERE a.detectedAt >= :startDate GROUP BY a.anomalyType")
    List<Object[]> getAnomalyTypeDistribution(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT a.severity, COUNT(a) FROM Anomaly a " +
           "WHERE a.verificationId = :verificationId GROUP BY a.severity")
    List<Object[]> getSeverityDistributionByVerification(@Param("verificationId") UUID verificationId);

    @Query("SELECT AVG(a.deviationPercentage) FROM Anomaly a " +
           "WHERE a.anomalyType = :type AND a.deviationPercentage IS NOT NULL")
    Double getAverageDeviationByType(@Param("type") AnomalyType type);

    @Query("SELECT COUNT(a) FROM Anomaly a WHERE a.resolutionStatus = 'UNRESOLVED' " +
           "AND a.detectedAt < :threshold")
    Long countOldUnresolvedAnomalies(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT a FROM Anomaly a WHERE a.resolutionStatus IN ('UNRESOLVED', 'UNDER_REVIEW') " +
           "ORDER BY " +
           "CASE a.severity " +
           "  WHEN 'CRITICAL' THEN 1 " +
           "  WHEN 'HIGH' THEN 2 " +
           "  WHEN 'MEDIUM' THEN 3 " +
           "  WHEN 'LOW' THEN 4 " +
           "END, " +
           "a.detectedAt ASC")
    Page<Anomaly> findUnresolvedOrderedBySeverity(Pageable pageable);

    @Query("SELECT a.resolvedBy, COUNT(a) FROM Anomaly a " +
           "WHERE a.resolvedAt >= :startDate AND a.resolvedBy IS NOT NULL " +
           "GROUP BY a.resolvedBy")
    List<Object[]> getAuditorResolutionStats(@Param("startDate") LocalDateTime startDate);
}
