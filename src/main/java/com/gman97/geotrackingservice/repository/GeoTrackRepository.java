package com.gman97.geotrackingservice.repository;

import com.gman97.geotrackingservice.entity.GeoTrack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GeoTrackRepository extends JpaRepository<GeoTrack, String> {

}
