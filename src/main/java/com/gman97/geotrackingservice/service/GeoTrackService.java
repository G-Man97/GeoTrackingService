package com.gman97.geotrackingservice.service;

import com.gman97.geotrackingservice.entity.GeoPoint;
import com.gman97.geotrackingservice.entity.GeoTrack;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

import static com.gman97.geotrackingservice.service.ProcessService.trackMap;

@Service
@RequiredArgsConstructor
public class GeoTrackService {

    private final Long PAUSE_MINUTES = 10L;

    public void createAndEditTracks(Set<Long> deviceIds, List<GeoPoint> points, Set<GeoTrack> tracksToSave) {
        for (var point : points) {
            var deviceId = point.getGeoDevice().getId();
            deviceIds.add(deviceId);
            TreeSet<GeoTrack> trackTree;

            if ((trackTree = trackMap.get(deviceId)) == null) {
                var treeForMap = new TreeSet<GeoTrack>();
                var newTrack = createNewTrack(point);
                treeForMap.add(newTrack);
                tracksToSave.add(newTrack);

                trackMap.put(deviceId, treeForMap);
            } else {
                var mayBeTrack = findTrackInTreeByPoint(trackTree, point.getDtGps());
                GeoTrack track;

                if (mayBeTrack.isPresent()) {
                    track = mayBeTrack.get();
                    if (point.getDtGps().isAfter(track.getDtEnd())) {
                        track.setDtEnd(point.getDtGps());
                        tracksToSave.add(track);
                    }
                } else {
                    track = createNewTrack(point);
                    tracksToSave.add(track);
                }
                trackTree.add(track);
            }
        }
    }

    public Optional<GeoTrack> findTrackInTreeByPoint(TreeSet<GeoTrack> trackTree, LocalDateTime pointTime) {

        var searchLike = new GeoTrack();
        searchLike.setDtBegin(pointTime);

        // Ищем самый поздний трек, который начинается до или в момент pointTime
        GeoTrack floorTrack = trackTree.floor(searchLike);

        if (floorTrack != null && floorTrack.getDtEnd().plusMinutes(PAUSE_MINUTES).compareTo(pointTime) >= 0) {
            return Optional.of(floorTrack);
        }

        return Optional.empty();
    }

    public void mergeIntersectingTracks(Set<Long> deviceIds,
                                        Map<GeoTrack, GeoTrack> tracksToDelete, Set<GeoTrack> tracksToSave) {
        for (Long key : deviceIds) {
            TreeSet<GeoTrack> tracks;
            if ((tracks = trackMap.get(key)).size() >= 2) {

                var iterator = tracks.iterator();

                if (iterator.hasNext()) {
                    var prev = iterator.next();

                    while (iterator.hasNext()) {
                        var current = iterator.next();

                        if (prev.getDtEnd().plusMinutes(PAUSE_MINUTES).isAfter(current.getDtBegin())) {
                            if (current.getDtEnd().isAfter(prev.getDtEnd())) {
                                prev.setDtEnd(current.getDtEnd());
                                tracksToSave.add(prev);
                            }
                            tracksToDelete.put(current, prev);
                            tracksToSave.remove(current);
                            continue;
                        }
                        prev = current;
                    }
                    tracks.removeAll(tracksToDelete.keySet());
                }
            }
        }
    }

    private static GeoTrack createNewTrack(GeoPoint point) {
        var newTrack = new GeoTrack();

        newTrack.setId(point.getId());
        newTrack.setGeoDevice(point.getGeoDevice());
        newTrack.setDtBegin(point.getDtGps());
        newTrack.setDtEnd(point.getDtGps());
        return newTrack;
    }
}
