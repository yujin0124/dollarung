package com.buulgyeong.forexanalyzer.service;

import com.buulgyeong.forexanalyzer.dto.CompanyInputRequest;
import com.buulgyeong.forexanalyzer.dto.ExchangeRateResponse;
import com.buulgyeong.forexanalyzer.dto.ProfitLossAnalysisResponse;
import com.buulgyeong.forexanalyzer.dto.ProfitLossAnalysisResponse.*;
import com.buulgyeong.forexanalyzer.external.UpstageAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfitLossAnalysisService {
    
    private final ExchangeRateService exchangeRateService;
    private final UpstageAiClient upstageAiClient;
    
    /**
     * 종합 손익 분석 수행
     */
    public ProfitLossAnalysisResponse analyze(CompanyInputRequest input) {
        ExchangeRateResponse exchangeRateInfo = exchangeRateService.getExchangeRateInfo();
        BigDecimal currentRate = exchangeRateInfo.getCurrentRate();
        BigDecimal rate30DaysAgo = exchangeRateInfo.getRate30DaysAgo();
        
        // 1. 실시간 손익 분석
        RealTimeProfitLoss realTimeProfitLoss = calculateRealTimeProfitLoss(input, currentRate, rate30DaysAgo);
        
        // 2. 발주 타이밍 가이드
        OrderTimingGuide orderTimingGuide = calculateOrderTimingGuide(input);
        
        // 3. 환율 상태 평가
        ExchangeRateStatus exchangeRateStatus = evaluateExchangeRateStatus(
                input, currentRate, orderTimingGuide.getBreakEvenExchangeRate(), 
                orderTimingGuide.getTargetExchangeRate()
        );
        
        // 4. AI 모니터링 전략
        String monitoringStrategy = upstageAiClient.generateMonitoringStrategy(
                currentRate,
                orderTimingGuide.getBreakEvenExchangeRate(),
                orderTimingGuide.getTargetExchangeRate(),
                exchangeRateInfo.getChangeRate30Day()
        );
        
        // 5. 환율 시나리오별 분석
        List<ScenarioAnalysis> scenarioAnalysisList = generateScenarioAnalysis(input, currentRate);
        
        // 6. 환율 변동에 따른 마진율 변화
        List<MarginRateChange> marginRateChanges = generateMarginRateChanges(input, currentRate);
        
        // 7. 상세 원가 분석
        DetailedCostAnalysis detailedCostAnalysis = calculateDetailedCostAnalysis(input, currentRate);
        
        return ProfitLossAnalysisResponse.builder()
                .realTimeProfitLoss(realTimeProfitLoss)
                .orderTimingGuide(orderTimingGuide)
                .exchangeRateStatus(exchangeRateStatus)
                .monitoringStrategy(monitoringStrategy)
                .scenarioAnalysisList(scenarioAnalysisList)
                .marginRateChanges(marginRateChanges)
                .detailedCostAnalysis(detailedCostAnalysis)
                .build();
    }
    
    /**
     * 실시간 손익 분석 계산
     */
    private RealTimeProfitLoss calculateRealTimeProfitLoss(CompanyInputRequest input, 
                                                           BigDecimal currentRate, 
                                                           BigDecimal rate30DaysAgo) {
        // 현재 원가 계산
        BigDecimal currentCost = calculateTotalCost(input, currentRate);
        BigDecimal cost30DaysAgo = calculateTotalCost(input, rate30DaysAgo);
        
        // 원가 변동률
        BigDecimal costChangeRate30Day = calculatePercentageChange(currentCost, cost30DaysAgo);
        
        // 현재 마진
        BigDecimal currentMargin = input.getSellingPriceKrw().subtract(currentCost);
        BigDecimal currentMarginRate = currentMargin
                .divide(input.getSellingPriceKrw(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        
        // 목표 마진
        BigDecimal targetMarginRate = input.getTargetMarginRate();
        BigDecimal targetMargin = input.getSellingPriceKrw()
                .multiply(targetMarginRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        
        // 목표 대비 차이
        BigDecimal targetGap = currentMargin.subtract(targetMargin);
        boolean targetAchieved = targetGap.compareTo(BigDecimal.ZERO) >= 0;
        
        return RealTimeProfitLoss.builder()
                .currentCost(currentCost.setScale(0, RoundingMode.HALF_UP))
                .costChangeRate30Day(costChangeRate30Day)
                .currentMargin(currentMargin.setScale(0, RoundingMode.HALF_UP))
                .currentMarginRate(currentMarginRate)
                .targetMargin(targetMargin.setScale(0, RoundingMode.HALF_UP))
                .targetMarginRate(targetMarginRate)
                .targetGap(targetGap.setScale(0, RoundingMode.HALF_UP))
                .targetAchieved(targetAchieved)
                .build();
    }
    
    /**
     * 발주 타이밍 가이드 계산
     */
    private OrderTimingGuide calculateOrderTimingGuide(CompanyInputRequest input) {
        // 손익분기점 환율 계산
        // 총원가 = 납품단가일 때의 환율
        // 원자재비용(KRW) + 기타비용 = 납품단가
        // 원자재비용(USD) * 환율 / (원자재비중/100) + 기타비용 = 납품단가
        // 환율 = (납품단가 - 기타비용) * (원자재비중/100) / 원자재비용(USD)
        
        BigDecimal materialRatioDecimal = input.getMaterialRatio().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        
        BigDecimal breakEvenRate = input.getSellingPriceKrw()
                .subtract(input.getOtherCostsKrw())
                .multiply(materialRatioDecimal)
                .divide(input.getMaterialCostUsd(), 2, RoundingMode.HALF_UP);
        
        // 목표 마진 달성 환율 계산
        // 납품단가 - 총원가 = 납품단가 * 목표마진율
        // 총원가 = 납품단가 * (1 - 목표마진율)
        BigDecimal targetMarginRateDecimal = input.getTargetMarginRate().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal maxAllowedCost = input.getSellingPriceKrw()
                .multiply(BigDecimal.ONE.subtract(targetMarginRateDecimal));
        
        BigDecimal targetExchangeRate = maxAllowedCost
                .subtract(input.getOtherCostsKrw())
                .multiply(materialRatioDecimal)
                .divide(input.getMaterialCostUsd(), 2, RoundingMode.HALF_UP);
        
        String breakEvenMessage = String.format("해당 환율(%.1f원/USD) 이하에서 발주 시 흑자 전환", breakEvenRate);
        String targetMessage = String.format("목표 마진율 %.1f%% 달성 가능", input.getTargetMarginRate());
        
        return OrderTimingGuide.builder()
                .breakEvenExchangeRate(breakEvenRate)
                .breakEvenMessage(breakEvenMessage)
                .targetExchangeRate(targetExchangeRate)
                .targetMessage(targetMessage)
                .build();
    }
    
    /**
     * 환율 상태 평가
     */
    private ExchangeRateStatus evaluateExchangeRateStatus(CompanyInputRequest input,
                                                          BigDecimal currentRate,
                                                          BigDecimal breakEvenRate,
                                                          BigDecimal targetRate) {
        // 범위 설정 (목표환율 기준 ±75원)
        BigDecimal minRange = targetRate.subtract(BigDecimal.valueOf(75));
        BigDecimal maxRange = targetRate.add(BigDecimal.valueOf(75));
        
        // 현재 위치 계산 (0~100%)
        BigDecimal range = maxRange.subtract(minRange);
        BigDecimal position = currentRate.subtract(minRange)
                .divide(range, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
        
        // 위치가 0~100 범위를 벗어나면 조정
        if (position.compareTo(BigDecimal.ZERO) < 0) position = BigDecimal.ZERO;
        if (position.compareTo(BigDecimal.valueOf(100)) > 0) position = BigDecimal.valueOf(100);
        
        // 상태 레벨 결정
        String statusLevel;
        String statusMessage;
        
        if (currentRate.compareTo(targetRate) <= 0) {
            statusLevel = "EXCELLENT";
            statusMessage = "최적 발주 구간 (적극 권장)";
        } else if (currentRate.compareTo(breakEvenRate) <= 0) {
            statusLevel = "GOOD";
            statusMessage = "양호한 발주 구간 (권장)";
        } else if (currentRate.compareTo(breakEvenRate.add(BigDecimal.valueOf(20))) <= 0) {
            statusLevel = "NORMAL";
            statusMessage = "보통 구간 (선별적 발주)";
        } else if (currentRate.compareTo(breakEvenRate.add(BigDecimal.valueOf(40))) <= 0) {
            statusLevel = "WARNING";
            statusMessage = "주의 구간 (발주 자제 권장)";
        } else {
            statusLevel = "DANGER";
            statusMessage = "위험 구간 (발주 지연 권장)";
        }
        
        // AI 평가 생성
        String aiEvaluation = upstageAiClient.generateExchangeRateEvaluation(
                currentRate, breakEvenRate, targetRate, input.getTargetMarginRate()
        );
        
        return ExchangeRateStatus.builder()
                .currentRate(currentRate)
                .minRange(minRange)
                .maxRange(maxRange)
                .position(position)
                .statusLevel(statusLevel)
                .statusMessage(statusMessage)
                .aiEvaluation(aiEvaluation)
                .build();
    }
    
    /**
     * 환율 시나리오별 분석 생성 (20원 단위, 5개 범주)
     */
    private List<ScenarioAnalysis> generateScenarioAnalysis(CompanyInputRequest input, BigDecimal currentRate) {
        List<ScenarioAnalysis> scenarios = new ArrayList<>();
        
        // 현재 환율을 20원 단위로 반올림
        BigDecimal roundedRate = currentRate.divide(BigDecimal.valueOf(20), 0, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(20));
        
        // -40원 ~ +40원 범위로 5개 시나리오 생성
        for (int i = -2; i <= 2; i++) {
            BigDecimal scenarioRate = roundedRate.add(BigDecimal.valueOf(i * 20));
            BigDecimal cost = calculateTotalCost(input, scenarioRate);
            BigDecimal margin = input.getSellingPriceKrw().subtract(cost);
            BigDecimal marginRate = margin
                    .divide(input.getSellingPriceKrw(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            
            boolean isCurrent = scenarioRate.compareTo(roundedRate) == 0;
            
            scenarios.add(ScenarioAnalysis.builder()
                    .exchangeRate(scenarioRate)
                    .cost(cost.setScale(0, RoundingMode.HALF_UP))
                    .margin(margin.setScale(0, RoundingMode.HALF_UP))
                    .marginRate(marginRate)
                    .isCurrent(isCurrent)
                    .build());
        }
        
        return scenarios;
    }
    
    /**
     * 환율 변동에 따른 마진율 변화 데이터 생성
     */
    private List<MarginRateChange> generateMarginRateChanges(CompanyInputRequest input, BigDecimal currentRate) {
        List<MarginRateChange> changes = new ArrayList<>();
        
        // 현재 환율 기준 -100원 ~ +100원 범위, 10원 단위
        BigDecimal startRate = currentRate.subtract(BigDecimal.valueOf(100));
        
        for (int i = 0; i <= 20; i++) {
            BigDecimal rate = startRate.add(BigDecimal.valueOf(i * 10));
            BigDecimal cost = calculateTotalCost(input, rate);
            BigDecimal margin = input.getSellingPriceKrw().subtract(cost);
            BigDecimal marginRate = margin
                    .divide(input.getSellingPriceKrw(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            
            changes.add(MarginRateChange.builder()
                    .exchangeRate(rate)
                    .marginRate(marginRate)
                    .build());
        }
        
        return changes;
    }
    
    /**
     * 상세 원가 분석 계산
     */
    private DetailedCostAnalysis calculateDetailedCostAnalysis(CompanyInputRequest input, BigDecimal currentRate) {
        BigDecimal materialCostKrw = input.getMaterialCostUsd().multiply(currentRate)
                .setScale(0, RoundingMode.HALF_UP);
        
        // 총 원가 = 원자재비용(KRW) / 원자재비중 + 기타비용
        BigDecimal materialRatioDecimal = input.getMaterialRatio().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal totalCost = materialCostKrw
                .divide(materialRatioDecimal, 0, RoundingMode.HALF_UP)
                .add(input.getOtherCostsKrw());
        
        BigDecimal netMargin = input.getSellingPriceKrw().subtract(totalCost);
        BigDecimal netMarginRate = netMargin
                .divide(input.getSellingPriceKrw(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        
        return DetailedCostAnalysis.builder()
                .materialCostUsd(input.getMaterialCostUsd())
                .appliedExchangeRate(currentRate)
                .materialCostKrw(materialCostKrw)
                .otherCosts(input.getOtherCostsKrw())
                .totalCost(totalCost)
                .sellingPrice(input.getSellingPriceKrw())
                .netMargin(netMargin)
                .netMarginRate(netMarginRate)
                .build();
    }
    
    /**
     * 총 원가 계산
     */
    private BigDecimal calculateTotalCost(CompanyInputRequest input, BigDecimal exchangeRate) {
        BigDecimal materialCostKrw = input.getMaterialCostUsd().multiply(exchangeRate);
        BigDecimal materialRatioDecimal = input.getMaterialRatio().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        
        // 총 원가 = 원자재비용(KRW) / 원자재비중 + 기타비용
        return materialCostKrw
                .divide(materialRatioDecimal, 2, RoundingMode.HALF_UP)
                .add(input.getOtherCostsKrw());
    }
    
    /**
     * 변동률 계산
     */
    private BigDecimal calculatePercentageChange(BigDecimal current, BigDecimal past) {
        if (past.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(past)
                .divide(past, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
