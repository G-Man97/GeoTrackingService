package com.gman97.geotrackingservice.repository;

import com.gman97.geotrackingservice.entity.GeoZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GeoZoneRepository extends JpaRepository<GeoZone, Long> {

    /*
    * За максимальный размер геозоны принята окружность радиусом 25 км или квадрат со стороной 50 км
    * Соответственно, для геозон-окружностей ищем кандидатов, центры которых попадают в радиус 50 км вокруг точки,
    * для геозон-многоугольников ищем кандидатов, если хотя бы одна из точек, составляющих многоугольник, попадает в
    * радиус окружности 35 км.
    * Таким образом создаётся кэш-зона вокруг точки из запроса - окружность радиусом 25 км. Если следующие точки
    * попадают в эту зону, то запрос в БД не нужно делать, т.к. зоны-кандидаты из этой кэш-зоны хранятся какое-то время
    * в приложении.
    * */
    @Query(value = "WITH candidate_zones AS (" +
            "           SELECT gz.ID," +
            "                  gz.OBJECT_TYPE," +
            "                  gz.OBJECT_ID," +
            "                  gz.NAME," +
            "                  gz.GEOMETRY," +
            "                  gz.LAT_CENTER," +
            "                  gz.LON_CENTER," +
            "                  gz.RADIUS" +
            "           FROM geo_zone gz" +
            "           WHERE (gz.GEOMETRY = 'circle'" +
            "                  AND gz.LAT_CENTER BETWEEN (:lat - 50000 / 111320) AND (:lat + 50000 / 111320)" +
            "                  AND gz.LON_CENTER BETWEEN (:lon - 50000 / (111320 * COS(:lat * 3.14159265359 / 180)))" +
            "                                    AND (:lon + 50000 / (111320 * COS(:lat * 3.14159265359 / 180))))" +
            "                 OR (gz.GEOMETRY = 'poly'" +
            "                     AND EXISTS (SELECT 1" +
            "                                 FROM geo_zone_point gzp" +
            "                                 WHERE gzp.ZONE_ID = gz.ID" +
            "                                       AND gzp.LAT BETWEEN (:lat - 35000 / 111320) AND (:lat + 35000 / 111320)" +
            "                                       AND gzp.LON BETWEEN (:lon - 35000 / (111320 * COS(:lat * 3.14159265359 / 180))) " +
            "                                                   AND (:lon + 35000 / (111320 * COS(:lat * 3.14159265359 / 180))))))" +
            "       SELECT * FROM candidate_zones " +
            "       ORDER BY GEOMETRY"
            , nativeQuery = true)
    List<GeoZone> findCandidatesByPointCoordinates(Float lat, Float lon);

}
