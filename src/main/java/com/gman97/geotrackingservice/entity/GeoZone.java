package com.gman97.geotrackingservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Table(name = "GEO_ZONE")
public class GeoZone {

    @Id
    private Long id;

    private String objectType;

    private Long objectId;

    private String name;

    @Convert(converter = GeometryConverter.class)
    private Geometry geometry;

    private Float latCenter;

    private Float lonCenter;

    private Float radius;

    @Getter
    @Setter
    @Transient
    private List<GeoZonePoint> points;

}