package com.carbonmarketplace.marketplaceservice.repository.elasticsearch;

import com.carbonmarketplace.marketplaceservice.entity.ListingDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Elasticsearch repository for listing search and filtering.
 */
@Repository
public interface ListingSearchRepository extends ElasticsearchRepository<ListingDocument, String> {
    
    Page<ListingDocument> findByStatus(String status, Pageable pageable);
    
    Page<ListingDocument> findByRegionAndStatus(String region, String status, Pageable pageable);
    
    Page<ListingDocument> findByListingTypeAndStatus(String listingType, String status, Pageable pageable);
    
    @Query("{\"bool\": {" +
           "  \"must\": [" +
           "    {\"term\": {\"status\": \"?0\"}}," +
           "    {\"range\": {\"pricePerTonVnd\": {\"gte\": ?1, \"lte\": ?2}}}" +
           "  ]" +
           "}}")
    Page<ListingDocument> findByPriceRange(String status, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    @Query("{\"bool\": {" +
           "  \"must\": [" +
           "    {\"term\": {\"status\": \"ACTIVE\"}}," +
           "    {\"multi_match\": {" +
           "      \"query\": \"?0\"," +
           "      \"fields\": [\"searchableText\", \"description\", \"sellerName\", \"cvaOrganization\"]," +
           "      \"type\": \"best_fields\"," +
           "      \"fuzziness\": \"AUTO\"" +
           "    }}" +
           "  ]" +
           "}}")
    Page<ListingDocument> searchByText(String searchText, Pageable pageable);
    
    @Query("{\"bool\": {" +
           "  \"must\": [" +
           "    {\"term\": {\"listingType\": \"AUCTION\"}}," +
           "    {\"term\": {\"status\": \"ACTIVE\"}}," +
           "    {\"range\": {\"auctionEndTime\": {\"gte\": \"now\", \"lte\": \"?0\"}}}" +
           "  ]" +
           "}}")
    List<ListingDocument> findEndingSoonAuctions(LocalDateTime endTime);
    
    @Query("{\"bool\": {" +
           "  \"must\": [" +
           "    {\"term\": {\"sellerId\": \"?0\"}}," +
           "    {\"terms\": {\"status\": ?1}}" +
           "  ]" +
           "}}")
    Page<ListingDocument> findBySellerAndStatuses(String sellerId, List<String> statuses, Pageable pageable);
    
    Page<ListingDocument> findByRegionInAndStatus(List<String> regions, String status, Pageable pageable);
    
    List<ListingDocument> findTop10ByStatusOrderByViewsCountDesc(String status);
    
    @Query("{\"bool\": {" +
           "  \"filter\": [" +
           "    {\"geo_distance\": {" +
           "      \"distance\": \"?1km\"," +
           "      \"location\": {" +
           "        \"lat\": ?2," +
           "        \"lon\": ?3" +
           "      }" +
           "    }}," +
           "    {\"term\": {\"status\": \"?0\"}}" +
           "  ]" +
           "}}")
    Page<ListingDocument> findNearbyListings(String status, double distanceKm, double lat, double lon, Pageable pageable);
}
