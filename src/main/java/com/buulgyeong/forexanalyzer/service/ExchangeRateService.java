package com.buulgyeong.forexanalyzer.service;

import com.buulgyeong.forexanalyzer.dto.ExchangeRateResponse;
import com.buulgyeong.forexanalyzer.dto.HistoricalRate;
import com.buulgyeong.forexanalyzer.entity.ExchangeRateHistory;
import com.buulgyeong.forexanalyzer.external.ExchangeRateApiClient;
import com.buulgyeong.forexanalyzer.repository.ExchangeRateHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
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
    public void initHistoricalData() {
        if (exchangeRateHistoryRepository.count() == 0) {
            log.info("환율 데이터 초기화 시작");
            List<HistoricalRate> last30Days = exchangeRateApiClient.fetchLast30Days();

            List<ExchangeRateHistory> entities = last30Days.stream()
                    .map(h -> ExchangeRateHistory.builder()
                            .rateDate(h.getDate())
                            .rate(h.getRate())
                            .currencyCode(USD)
                            .build())
                    .collect(Collectors.toList());

            exchangeRateHistoryRepository.saveAll(entities);
            log.info("지난 30일치 환율 DB 저장 완료");
        }
    }
    /**
     * 실시간 환율 정보 조회
     */
    public ExchangeRateResponse getExchangeRateInfo() {
        LocalDate today = LocalDate.now();

        // 현재 환율 → NAVER 스크래핑
        BigDecimal currentRate = exchangeRateApiClient.fetchCurrentExchangeRateFromNaver();

        // 과거 30일 환율 → fetchLast30Days 사용
        List<HistoricalRate> last30Days = exchangeRateApiClient.fetchLast30Days();

        // 변동률 계산
        BigDecimal rate1DayAgo = last30Days.stream()
                .filter(r -> r.getDate().equals(today.minusDays(1)))
                .map(HistoricalRate::getRate)
                .findFirst()
                .orElse(currentRate);

        BigDecimal rate7DaysAgo = last30Days.stream()
                .filter(r -> r.getDate().equals(today.minusDays(7)))
                .map(HistoricalRate::getRate)
                .findFirst()
                .orElse(currentRate);

        BigDecimal rate30DaysAgo = last30Days.stream()
                .filter(r -> r.getDate().equals(today.minusDays(30)))
                .map(HistoricalRate::getRate)
                .findFirst()
                .orElse(currentRate);

        // 30일 환율 추이
        List<ExchangeRateResponse.DailyRate> last30DaysRates = last30Days.stream()
                .map(r -> new ExchangeRateResponse.DailyRate(r.getDate(), r.getRate()))
                .collect(Collectors.toList());

        // 응답 빌드
        return ExchangeRateResponse.builder()
                .currentRate(currentRate)
                .changeRate1Day(calculateChangeRate(currentRate, rate1DayAgo))
                .changeRate7Day(calculateChangeRate(currentRate, rate7DaysAgo))
                .changeRate30Day(calculateChangeRate(currentRate, rate30DaysAgo))
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

}
