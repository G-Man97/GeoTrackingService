package com.gman97.geotrackingservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Table(name = "GEO_ZONE_POINT")
public class GeoZonePoint {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private GeoZone geoZone;

    private Integer serialIndex;

    private Float lat;

    private Float lon;

}