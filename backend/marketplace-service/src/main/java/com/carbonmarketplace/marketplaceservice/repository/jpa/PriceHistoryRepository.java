package com.carbonmarketplace.marketplaceservice.repository.jpa;

import com.carbonmarketplace.marketplaceservice.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for PriceHistory entity.
 */
@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {
    
    Optional<PriceHistory> findByDateAndRegionAndVintageYear(LocalDate date, String region, Integer vintageYear);
    
    List<PriceHistory> findByRegionAndDateBetweenOrderByDateDesc(String region, LocalDate startDate, LocalDate endDate);
    
    List<PriceHistory> findByDateBetweenOrderByDateDesc(LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT p FROM PriceHistory p WHERE p.date >= :since " +
           "AND (:region IS NULL OR p.region = :region) " +
           "AND (:vintageYear IS NULL OR p.vintageYear = :vintageYear) " +
           "ORDER BY p.date DESC")
    List<PriceHistory> findRecentHistory(@Param("since") LocalDate since,
                                          @Param("region") String region,
                                          @Param("vintageYear") Integer vintageYear);
    
    @Query("SELECT p FROM PriceHistory p WHERE p.date = (SELECT MAX(p2.date) FROM PriceHistory p2 WHERE p2.region = p.region)")
    List<PriceHistory> findLatestByRegion();
    
    @Query("SELECT DISTINCT p.region FROM PriceHistory p WHERE p.date >= :since")
    List<String> findActiveRegions(@Param("since") LocalDate since);
}
