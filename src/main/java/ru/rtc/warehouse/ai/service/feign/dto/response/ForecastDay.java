package ru.rtc.warehouse.ai.service.feign.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastDay {

    private String date;
    private Double lower;
    private Double median;
    private Double upper;
    private Double confidence;
    private String criticality;
}
