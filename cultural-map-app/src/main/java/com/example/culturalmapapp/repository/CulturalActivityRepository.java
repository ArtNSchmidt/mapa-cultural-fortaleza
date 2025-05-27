package com.example.culturalmapapp.repository;

import com.example.culturalmapapp.model.CulturalActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List; // Keep for non-paginated results if any specific method needs it

public interface CulturalActivityRepository extends JpaRepository<CulturalActivity, Long> { // JpaRepository extends PagingAndSortingRepository
    Page<CulturalActivity> findByCategory(String category, Pageable pageable);

    // This query is for a list, if it needs pagination, it has to be changed.
    // For now, assuming findByLocationBoundingBox is used for a smaller, filtered list
    // that might not need pagination itself, or pagination will be applied in-memory after this DB call.
    // If this list can be very large, this query should also be adapted to return Page<CulturalActivity>.
    @Query("SELECT ca FROM CulturalActivity ca WHERE ca.latitude BETWEEN :minLat AND :maxLat AND ca.longitude BETWEEN :minLon AND :maxLon")
    List<CulturalActivity> findByLocationBoundingBox( // Returning List for now, will be filtered and paginated in memory in service
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon
    );
    
    // Example if findByLocationBoundingBox needed pagination directly from DB (more complex for non-PostGIS)
    // @Query("SELECT ca FROM CulturalActivity ca WHERE ca.latitude BETWEEN :minLat AND :maxLat AND ca.longitude BETWEEN :minLon AND :maxLon")
    // Page<CulturalActivity> findByLocationBoundingBoxPaginated(
    //        @Param("minLat") Double minLat,
    //        @Param("maxLat") Double maxLat,
    //        @Param("minLon") Double minLon,
    //        @Param("maxLon") Double maxLon,
    //        Pageable pageable
    // );
}
