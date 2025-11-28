package com.buulgyeong.forexanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitLossAnalysisResponse {
    
    // 실시간 손익 분석
    private RealTimeProfitLoss realTimeProfitLoss;
    
    // 발주 타이밍 가이드
    private OrderTimingGuide orderTimingGuide;
    
    // 환율 상태 평가
    private ExchangeRateStatus exchangeRateStatus;
    
    // AI 모니터링 전략
    private String monitoringStrategy;
    
    // 환율 시나리오별 분석
    private List<ScenarioAnalysis> scenarioAnalysisList;
    
    // 환율 변동에 따른 마진율 변화
    private List<MarginRateChange> marginRateChanges;
    
    // 상세 원가 분석
    private DetailedCostAnalysis detailedCostAnalysis;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RealTimeProfitLoss {
        private BigDecimal currentCost;           // 현재 제품 원가(원)
        private BigDecimal costChangeRate30Day;   // 30일 전 대비 원가 변동률(%)
        private BigDecimal currentMargin;         // 현재 마진(원)
        private BigDecimal currentMarginRate;     // 현재 마진율(%)
        private BigDecimal targetMargin;          // 목표 마진(원)
        private BigDecimal targetMarginRate;      // 목표 마진율(%)
        private BigDecimal targetGap;             // 목표 대비 차이(원)
        private boolean targetAchieved;           // 목표 달성 여부
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderTimingGuide {
        private BigDecimal breakEvenExchangeRate;    // 손익분기점 환율
        private String breakEvenMessage;             // 손익분기점 안내 메시지
        private BigDecimal targetExchangeRate;       // 목표 달성 환율
        private String targetMessage;                // 목표 달성 안내 메시지
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExchangeRateStatus {
        private BigDecimal currentRate;              // 현재 환율
        private BigDecimal minRange;                 // 범위 최소값
        private BigDecimal maxRange;                 // 범위 최대값
        private BigDecimal position;                 // 현재 위치 (0~100%)
        private String statusLevel;                  // 상태 레벨 (EXCELLENT, GOOD, NORMAL, WARNING, DANGER)
        private String statusMessage;                // 상태 메시지
        private String aiEvaluation;                 // AI 평가 메시지
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScenarioAnalysis {
        private BigDecimal exchangeRate;     // 환율
        private BigDecimal cost;             // 원가
        private BigDecimal margin;           // 마진
        private BigDecimal marginRate;       // 마진율
        private boolean isCurrent;           // 현재 환율 여부
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarginRateChange {
        private BigDecimal exchangeRate;     // 환율
        private BigDecimal marginRate;       // 마진율
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailedCostAnalysis {
        private BigDecimal materialCostUsd;      // 원자재 비용(USD)
        private BigDecimal appliedExchangeRate;  // 적용 환율
        private BigDecimal materialCostKrw;      // 원자재 비용(KRW)
        private BigDecimal otherCosts;           // 기타 비용
        private BigDecimal totalCost;            // 총 원가
        private BigDecimal sellingPrice;         // 납품 단가
        private BigDecimal netMargin;            // 순 마진
        private BigDecimal netMarginRate;        // 순 마진율
    }
}
