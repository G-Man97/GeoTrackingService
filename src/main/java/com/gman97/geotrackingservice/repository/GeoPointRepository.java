package com.gman97.geotrackingservice.repository;

import com.gman97.geotrackingservice.entity.GeoPoint;
import com.gman97.geotrackingservice.entity.GeoTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GeoPointRepository extends JpaRepository<GeoPoint, String> {

    @Query(value = "SELECT p.*, ROWNUM AS rn " +
                   "FROM (SELECT * " +
                   "      FROM GEO_POINT p " +
                   "      WHERE p.TRACK_ID IS NULL) p " +
                   "JOIN GEO_DEVICE d ON p.DEVICE_ID = d.ID " +
                   "WHERE ROWNUM <= 500 " +
                   "ORDER BY TO_NUMBER(p.ID, 'XXXXXXXXXXXXXXXX')",
           nativeQuery = true)
    List<GeoPoint> findFirst500ByTrackIdIsNull();

    @Modifying
    @Query("UPDATE GeoPoint p SET p.geoTrack = :newTrack WHERE p.geoTrack.id = :oldTrackId")
    void updateTrackId(String oldTrackId, GeoTrack newTrack);

    @Modifying
    @Query(value = "UPDATE geo_point SET track_id = :trackId, zone_id = :zoneId WHERE id = :pointId", nativeQuery = true)
    void updatePoint(String pointId, String trackId, Long zoneId);
}
