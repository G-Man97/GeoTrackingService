package com.gman97.geotrackingservice.service;

import com.gman97.geotrackingservice.dto.PointUpdateDto;
import com.gman97.geotrackingservice.entity.GeoPoint;
import com.gman97.geotrackingservice.entity.GeoTrack;
import com.gman97.geotrackingservice.entity.GeoZone;
import com.gman97.geotrackingservice.repository.GeoPointRepository;
import com.gman97.geotrackingservice.repository.GeoTrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProcessService {

    private final GeoTrackService geoTrackService;
    private final GeoZoneService geoZoneService;
    private final GeoPointRepository geoPointRepository;
    private final GeoTrackRepository geoTrackRepository;
    public static final Map<Long, TreeSet<GeoTrack>> trackMap = new HashMap<>();


    @Transactional
    @Scheduled(fixedDelay = 4, timeUnit = TimeUnit.SECONDS)
    public void processGeoPoints() {
        var points = geoPointRepository.findFirst500ByTrackIdIsNull();

        if (points.isEmpty()) {
            return;
        }

        var deviceIds = new HashSet<Long>();
        var tracksToSave = new TreeSet<GeoTrack>();
        var tracksToDelete = new HashMap<GeoTrack, GeoTrack>();

        geoTrackService.createAndEditTracks(deviceIds, points, tracksToSave);

        geoTrackService.mergeIntersectingTracks(deviceIds, tracksToDelete, tracksToSave);

        geoTrackRepository.saveAllAndFlush(tracksToSave);

        var pointUpdates = new ArrayList<PointUpdateDto>(500);
        setTracksAndGeoZonesToPoints(points, pointUpdates);

        pointUpdates.forEach(update ->
                geoPointRepository.updatePoint(
                        update.getPointId(),
                        update.getTrackId(),
                        update.getZoneId()
                )
        );

        if (!tracksToDelete.isEmpty()) {
            updateOldPoints(tracksToDelete);
            geoTrackRepository.deleteAll(tracksToDelete.keySet());
        }

    }

    private void setTracksAndGeoZonesToPoints(List<GeoPoint> points, List<PointUpdateDto> updateDtos) {
        for (var point : points) {
            var track = geoTrackService.findTrackInTreeByPoint(
                    trackMap.get(point.getGeoDevice().getId())
                    , point.getDtGps()).get();
            var zone = geoZoneService.findZonesForPoint(point);

            updateDtos.add(new PointUpdateDto(
                    point.getId(),
                    track.getId(),
                    zone.map(GeoZone::getId).orElse(0L)
            ));
        }
    }

    private void updateOldPoints(Map<GeoTrack, GeoTrack> trackMap) {
        for (var entry : trackMap.entrySet()) {
            geoPointRepository.updateTrackId(entry.getKey().getId(), entry.getValue());
        }
    }

}

