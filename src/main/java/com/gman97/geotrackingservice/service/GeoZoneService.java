package com.gman97.geotrackingservice.service;

import com.gman97.geotrackingservice.entity.GeoPoint;
import com.gman97.geotrackingservice.entity.GeoZone;
import com.gman97.geotrackingservice.entity.Geometry;
import com.gman97.geotrackingservice.repository.GeoZonePointRepository;
import com.gman97.geotrackingservice.repository.GeoZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GeoZoneService {

    private final GeoZoneRepository geoZoneRepository;
    private final GeoZonePointRepository geoZonePointRepository;

    private static final int GEO_ZONE_CACHE_SIZE = 60;
    private static final int MAX_DEVICE_FOR_CACHE = 10;
    private static final int CANDIDATES_CACHE_SIZE = 10;
    private static final float CACHEABLE_RADIUS = 25000F;
    private static final int MAX_CACHE_SIZE_FOR_ONE_DEVICE = 6;
    private final LinkedHashMap<Long, GeoZone> geoZoneCache = new LinkedHashMap<>(80); // 60 геозон
    private final LinkedHashMap<Long, LinkedList<Long>> lastGeoZoneIdsByDeviceIdKey = new LinkedHashMap<>(14); // 10 устройств
    private final LinkedHashMap<GeoPoint, List<GeoZone>> geoZoneCandidatesCache = new LinkedHashMap<>(14); // 10 областей


    public Optional<GeoZone> findZonesForPoint(GeoPoint point) {
        var pointLat = point.getLat();
        var pointLon = point.getLon();
        var deviceId = point.getGeoDevice().getId();

        // Сначала проверяем в кэше геозон
        var mayBeZone = checkCache(deviceId, pointLat, pointLon);
        if (mayBeZone.isPresent()) return mayBeZone;

        // Теперь проверяем в кэше геозон-кандитатов
        mayBeZone = checkCandidatesCache(pointLat, pointLon, deviceId);
        if (mayBeZone.isPresent()) {
            if (mayBeZone.get().getId() == null) {
                return Optional.empty();
            }
            return mayBeZone;
        }

        // Получаем кандидатов из БД
        var candidates = geoZoneRepository.findCandidatesByPointCoordinates(pointLat, pointLon);

        if (geoZoneCandidatesCache.size() == CANDIDATES_CACHE_SIZE) {
            geoZoneCandidatesCache.pollLastEntry();
        }

        geoZoneCandidatesCache.putFirst(point, candidates);

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        mayBeZone = candidates.stream()
                .filter(z -> isPointInCircleOrPolygon(pointLat, pointLon, z))
                .findFirst();

        mayBeZone.ifPresent(zone -> updateCache(deviceId, zone));

        return mayBeZone;
    }


    private Optional<GeoZone> checkCandidatesCache(Float pointLat, Float pointLon, Long deviceId) {
        if (geoZoneCandidatesCache.isEmpty()) {
            return Optional.empty();
        }

        var entries = new ArrayList<>(geoZoneCandidatesCache.entrySet());

        var geoZoneForCache = new GeoZone();
        geoZoneForCache.setRadius(CACHEABLE_RADIUS);

        var flag = false;

        for (var entry : entries) {
            var key = entry.getKey();
            var candidates = entry.getValue();

            // Проверяем попадание в общую зону кэша
            geoZoneForCache.setLatCenter(key.getLat());
            geoZoneForCache.setLonCenter(key.getLon());

            if (!isPointInCircle(pointLat, pointLon, geoZoneForCache)) {
                continue; // пропускаем, если не в радиусе
            }

            // Если список пустой (поблизости нет геозон), то возвращаем геозону с id = null
            // чтобы в вызывающем методе понять, что поиск не нужно продолжать
            if (candidates.isEmpty()) {
                return Optional.of(geoZoneForCache);
            }

            // Проверяем каждую геозону-кандидата
            for (var candidate : candidates) {
                flag = true;
                if (isPointInCircleOrPolygon(pointLat, pointLon, candidate)) {
                    geoZoneCandidatesCache.remove(key);
                    geoZoneCandidatesCache.putFirst(key, candidates);

                    var zoneIds = lastGeoZoneIdsByDeviceIdKey.get(deviceId);

                    if (candidate.getGeometry().equals(Geometry.CIRCLE)) {
                        if (zoneIds != null) {
                            if (zoneIds.size() == MAX_CACHE_SIZE_FOR_ONE_DEVICE) {
                                zoneIds.removeLast();
                            }
                        } else {
                            zoneIds = new LinkedList<>();
                            lastGeoZoneIdsByDeviceIdKey.put(deviceId, zoneIds);
                        }

                        zoneIds.addFirst(candidate.getId());
                    }

                    if (geoZoneCache.size() == GEO_ZONE_CACHE_SIZE) {
                        geoZoneCache.pollLastEntry();
                    }

                    geoZoneCache.putFirst(candidate.getId(), candidate);

                    return Optional.of(candidate);
                }
            }
        }
        return flag ? Optional.of(geoZoneForCache)
                    : Optional.empty();
    }

    private void updateCache(Long deviceId, GeoZone geoZone) {
        var zoneIdList = lastGeoZoneIdsByDeviceIdKey.get(deviceId);
        var geoZoneId = geoZone.getId();

        if (geoZone.getGeometry().equals(Geometry.CIRCLE)) {
            if (zoneIdList == null) {
                var list = new LinkedList<Long>();
                list.addFirst(geoZoneId);

                if (lastGeoZoneIdsByDeviceIdKey.size() == MAX_DEVICE_FOR_CACHE) {
                    lastGeoZoneIdsByDeviceIdKey.pollLastEntry();
                }

                lastGeoZoneIdsByDeviceIdKey.putFirst(deviceId, list);
            } else {
                if (zoneIdList.size() == MAX_CACHE_SIZE_FOR_ONE_DEVICE) {
                    zoneIdList.removeLast();
                }
                zoneIdList.addFirst(geoZoneId);
            }
        }

        if (geoZoneCache.size() == GEO_ZONE_CACHE_SIZE) {
            geoZoneCache.pollLastEntry();
        }
        geoZoneCache.putFirst(geoZoneId, geoZone);
    }

    private Optional<GeoZone> checkCache(Long deviceId, Float pointLat, Float pointLon) {
        var zoneIds = lastGeoZoneIdsByDeviceIdKey.get(deviceId);

        if (zoneIds == null || zoneIds.isEmpty()) {
            return Optional.empty();
        }

        var iterator = zoneIds.iterator();

        while (iterator.hasNext()) {
            var zoneId = iterator.next();
            var geoZone = geoZoneCache.get(zoneId);

            if (geoZone == null) {
                var mayBeZone = geoZoneRepository.findById(zoneId);
                if (mayBeZone.isPresent()) {
                    geoZone = mayBeZone.get();
                } else {
                    iterator.remove();
                    continue;
                }
            }

            if (isPointInCircleOrPolygon(pointLat, pointLon, geoZone)) {
                iterator.remove();
                zoneIds.addFirst(zoneId);

                if (geoZoneCache.size() == GEO_ZONE_CACHE_SIZE) {
                    geoZoneCache.pollLastEntry();
                }

                geoZoneCache.putFirst(geoZone.getId(), geoZone);
                return Optional.of(geoZone);
            }
        }

        return Optional.empty();
    }


    // Проверка попадания точки в окружность
    private boolean isPointInCircle(Float pointLat, Float pointLon, GeoZone circle) {
        double distance = calculateDistance(pointLat, pointLon,
                circle.getLatCenter(), circle.getLonCenter());
        return distance <= circle.getRadius();
    }

    private boolean isPointInCircleOrPolygon(Float pointLat, Float pointLon, GeoZone geoZone) {
        return geoZone.getGeometry().equals(Geometry.CIRCLE)
                ? isPointInCircle(pointLat, pointLon, geoZone)
                : isPointInPolygon(pointLat, pointLon, geoZone);
    }

    // Расчёт расстояния между точками (формула гаверсинуса)
    private double calculateDistance(Float pointLat, Float pointLon, Float circleLat, Float circleLon) {
        final int R = 6371000; // средний радиус Земли в метрах
        double latDistance = Math.toRadians(circleLat - pointLat);
        double lonDistance = Math.toRadians(circleLon - pointLon);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(pointLat)) * Math.cos(Math.toRadians(circleLat))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Проверка попадания точки в многоугольник (ray casting algorithm)
    private boolean isPointInPolygon(Float pointLat, Float pointLon, GeoZone geoZone) {
        var polygonPoints = geoZone.getPoints();
        if (polygonPoints == null || polygonPoints.isEmpty()) {
            geoZone.setPoints(geoZonePointRepository.findAllByGeoZoneIdOrderBySerialIndex(geoZone.getId()));
        }

        if (polygonPoints == null || polygonPoints.size() < 3) return false;

        boolean inside = false;
        int j = polygonPoints.size() - 1;

        for (int i = 0; i < polygonPoints.size(); i++) {
            var pi = polygonPoints.get(i);
            var pj = polygonPoints.get(j);

            if (((pi.getLat() <= pointLat && pointLat < pj.getLat()) ||
                    (pj.getLat() <= pointLat && pointLat < pi.getLat())) &&
                    (pointLon < (pj.getLon() - pi.getLon())
                            * (pointLat - pi.getLat()) / (pj.getLat() - pi.getLat()) + pi.getLon())) {
                inside = !inside;
            }
            j = i;
        }
        return inside;
    }

}
