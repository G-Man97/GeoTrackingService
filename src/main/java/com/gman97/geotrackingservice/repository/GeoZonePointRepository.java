package com.gman97.geotrackingservice.repository;

import com.gman97.geotrackingservice.entity.GeoZonePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GeoZonePointRepository extends JpaRepository<GeoZonePoint, Integer> {

    List<GeoZonePoint> findAllByGeoZoneIdOrderBySerialIndex(Long zoneId);
}
