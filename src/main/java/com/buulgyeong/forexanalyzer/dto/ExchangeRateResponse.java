package com.buulgyeong.forexanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateResponse {
    
    private BigDecimal currentRate;       // 현재 환율
    private BigDecimal changeRate1Day;    // 1일 변동률(%)
    private BigDecimal changeRate7Day;    // 7일 변동률(%)
    private BigDecimal changeRate30Day;   // 30일 변동률(%)
    
    private BigDecimal rate1DayAgo;       // 1일 전 환율
    private BigDecimal rate7DaysAgo;      // 7일 전 환율
    private BigDecimal rate30DaysAgo;     // 30일 전 환율
    
    private List<DailyRate> last30DaysRates; // 30일 환율 추이
    
    private LocalDate lastUpdated;        // 마지막 업데이트 시간
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRate {
        private LocalDate date;
        private BigDecimal rate;
    }
}
