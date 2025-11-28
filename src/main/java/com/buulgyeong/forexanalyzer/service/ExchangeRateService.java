package com.buulgyeong.forexanalyzer.service;

import com.buulgyeong.forexanalyzer.dto.ExchangeRateResponse;
import com.buulgyeong.forexanalyzer.entity.ExchangeRateHistory;
import com.buulgyeong.forexanalyzer.external.ExchangeRateApiClient;
import com.buulgyeong.forexanalyzer.repository.ExchangeRateHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ExchangeRateService {
    
    private final ExchangeRateHistoryRepository exchangeRateHistoryRepository;
    private final ExchangeRateApiClient exchangeRateApiClient;
    
    private static final String USD = "USD";


    @PostConstruct
    @Transactional
    public void initSampleData() {
        if (exchangeRateHistoryRepository.count() == 0) {
            log.info("환율 데이터 초기화 시작");
            try {
                BigDecimal currentRate = exchangeRateApiClient.fetchCurrentExchangeRate();
                log.info("API에서 환율 조회 성공: {}", currentRate);
                generateHistoricalRates(currentRate);
            } catch (Exception e) {
                log.warn("API 호출 실패, 샘플 데이터 사용: {}", e.getMessage());
                generateSampleExchangeRates();
            }
            log.info("환율 데이터 초기화 완료");
        }
    }

    private void generateHistoricalRates(BigDecimal currentRate) {
        LocalDate today = LocalDate.now();
        Random random = new Random(42);
        List<ExchangeRateHistory> rates = new ArrayList<>();

        for (int i = 30; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            BigDecimal rate;

            if (i == 0) {
                // 오늘은 실제 환율 그대로 사용
                rate = currentRate;
            } else {
                double variation = (random.nextDouble() - 0.5) * 20;
                rate = currentRate.add(BigDecimal.valueOf(variation - (30 - i) * 0.2))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            rates.add(ExchangeRateHistory.builder()
                    .rateDate(date)
                    .rate(rate)
                    .currencyCode(USD)
                    .build());
        }

        exchangeRateHistoryRepository.saveAll(rates);
    }
    
    /**
     * 30일간의 샘플 환율 데이터 생성
     */
    private void generateSampleExchangeRates() {
        LocalDate today = LocalDate.now();
        BigDecimal baseRate = BigDecimal.valueOf(1380.0);
        Random random = new Random(42); // 일관된 샘플 데이터를 위한 시드
        
        List<ExchangeRateHistory> rates = new ArrayList<>();
        
        for (int i = 30; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            // -20 ~ +20 범위의 변동
            double variation = (random.nextDouble() - 0.5) * 40;
            BigDecimal rate = baseRate.add(BigDecimal.valueOf(variation)).setScale(2, RoundingMode.HALF_UP);
            
            // 추세를 반영 (최근으로 올수록 약간 상승 추세)
            rate = rate.add(BigDecimal.valueOf((30 - i) * 0.3));
            
            rates.add(ExchangeRateHistory.builder()
                    .rateDate(date)
                    .rate(rate)
                    .currencyCode(USD)
                    .build());
        }
        
        exchangeRateHistoryRepository.saveAll(rates);
    }
    
    /**
     * 실시간 환율 정보 조회
     */
    public ExchangeRateResponse getExchangeRateInfo() {
        LocalDate today = LocalDate.now();
        
        // 현재 환율 조회 (DB에서 최신 데이터)
        BigDecimal currentRate = getCurrentRate();
        
        // 과거 환율 조회
        BigDecimal rate1DayAgo = getRateByDate(today.minusDays(1));
        BigDecimal rate7DaysAgo = getRateByDate(today.minusDays(7));
        BigDecimal rate30DaysAgo = getRateByDate(today.minusDays(30));
        
        // 변동률 계산
        BigDecimal changeRate1Day = calculateChangeRate(currentRate, rate1DayAgo);
        BigDecimal changeRate7Day = calculateChangeRate(currentRate, rate7DaysAgo);
        BigDecimal changeRate30Day = calculateChangeRate(currentRate, rate30DaysAgo);
        
        // 30일 환율 추이 조회
        List<ExchangeRateResponse.DailyRate> last30DaysRates = getLast30DaysRates();
        
        return ExchangeRateResponse.builder()
                .currentRate(currentRate)
                .changeRate1Day(changeRate1Day)
                .changeRate7Day(changeRate7Day)
                .changeRate30Day(changeRate30Day)
                .rate1DayAgo(rate1DayAgo)
                .rate7DaysAgo(rate7DaysAgo)
                .rate30DaysAgo(rate30DaysAgo)
                .last30DaysRates(last30DaysRates)
                .lastUpdated(today)
                .build();
    }
    
    /**
     * 현재 환율 조회
     */
    public BigDecimal getCurrentRate() {
        return exchangeRateHistoryRepository.findLatestByCurrencyCode(USD)
                .map(ExchangeRateHistory::getRate)
                .orElse(BigDecimal.valueOf(1380.0));
    }
    
    /**
     * 특정 날짜의 환율 조회
     */
    private BigDecimal getRateByDate(LocalDate date) {
        // 해당 날짜에 데이터가 없으면 가장 가까운 이전 날짜 데이터 조회
        return exchangeRateHistoryRepository.findByRateDateAndCurrencyCode(date, USD)
                .map(ExchangeRateHistory::getRate)
                .orElseGet(() -> {
                    // 이전 날짜들 중 가장 가까운 데이터 찾기
                    for (int i = 1; i <= 5; i++) {
                        var prevRate = exchangeRateHistoryRepository
                                .findByRateDateAndCurrencyCode(date.minusDays(i), USD);
                        if (prevRate.isPresent()) {
                            return prevRate.get().getRate();
                        }
                    }
                    return getCurrentRate();
                });
    }
    
    /**
     * 환율 변동률 계산
     */
    private BigDecimal calculateChangeRate(BigDecimal current, BigDecimal past) {
        if (past.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(past)
                .divide(past, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 최근 30일 환율 추이 조회
     */
    private List<ExchangeRateResponse.DailyRate> getLast30DaysRates() {
        LocalDate startDate = LocalDate.now().minusDays(30);
        List<ExchangeRateHistory> histories = exchangeRateHistoryRepository
                .findByDateRangeAndCurrencyCode(startDate, USD);
        
        return histories.stream()
                .map(h -> ExchangeRateResponse.DailyRate.builder()
                        .date(h.getRateDate())
                        .rate(h.getRate())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * 환율 데이터 갱신 (스케줄러)
     */
    @Scheduled(cron = "0 0 9 * * MON-FRI") // 평일 오전 9시
    @Transactional
    public void updateExchangeRate() {
        try {
            BigDecimal newRate = exchangeRateApiClient.fetchCurrentExchangeRate();
            LocalDate today = LocalDate.now();
            
            if (!exchangeRateHistoryRepository.existsByRateDateAndCurrencyCode(today, USD)) {
                ExchangeRateHistory history = ExchangeRateHistory.builder()
                        .rateDate(today)
                        .rate(newRate)
                        .currencyCode(USD)
                        .build();
                exchangeRateHistoryRepository.save(history);
                log.info("환율 데이터 갱신 완료: {} = {}", today, newRate);
            }
        } catch (Exception e) {
            log.error("환율 데이터 갱신 실패", e);
        }
    }
}
