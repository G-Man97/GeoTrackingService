package com.gman97.geotrackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PointUpdateDto {
    private String pointId;
    private String trackId;
    private Long zoneId;
}
