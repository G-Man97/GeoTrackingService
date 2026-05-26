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
@Table(name = "GEO_TRACK")
public class GeoTrack implements Comparable<GeoTrack>{

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private GeoDevice geoDevice;

    private LocalDateTime dtBegin;

    private LocalDateTime dtEnd;

    private Float distance;


    @Override
    public int compareTo(GeoTrack o) {
        return this.dtBegin.compareTo(o.dtBegin);
    }

}
