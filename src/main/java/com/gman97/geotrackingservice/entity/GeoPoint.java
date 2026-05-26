package com.gman97.geotrackingservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Table(name = "GEO_POINT")
public class GeoPoint {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private GeoDevice geoDevice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id")
    private GeoTrack geoTrack;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private GeoZone geoZone;

    private Long tsDevice;

    private LocalDateTime dtDevice;

    private LocalDateTime dtGps;

    private Float lon;

    private Float lat;

    private Float distance;

    private Float speed;

    private Double alt;

    private Float bearing;

}