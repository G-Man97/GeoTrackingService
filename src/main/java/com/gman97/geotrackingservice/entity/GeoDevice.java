package com.gman97.geotrackingservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Table(name = "GEO_DEVICE")
public class GeoDevice {

    @Id
    private Long id;

    private String name;

}
