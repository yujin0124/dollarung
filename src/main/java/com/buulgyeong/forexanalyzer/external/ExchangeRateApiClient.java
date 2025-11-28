package com.buulgyeong.forexanalyzer.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
@Slf4j
public class ExchangeRateApiClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private final String koreaeximUrl;
    private final String koreaeximApiKey;
    private final String backupUrl;

    public ExchangeRateApiClient(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${external.exchange-rate.koreaexim-url}") String koreaeximUrl,
            @Value("${external.exchange-rate.koreaexim-api-key:}") String koreaeximApiKey,
            @Value("${external.exchange-rate.backup-url}") String backupUrl) {
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
        this.koreaeximUrl = koreaeximUrl;
        this.koreaeximApiKey = koreaeximApiKey;
        this.backupUrl = backupUrl;
    }
    
    /**
     * 한국수출입은행 API에서 USD 환율 조회
     */
    public Optional<BigDecimal> fetchExchangeRateFromKoreaExim(LocalDate date) {
        try {
            String formattedDate = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            String response = webClientBuilder.build()
                    .get()
                    .uri(koreaeximUrl + "?authkey=" + koreaeximApiKey + "&searchdate=" + formattedDate + "&data=AP01")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response != null && !response.isEmpty()) {
                JsonNode rootNode = objectMapper.readTree(response);
                
                for (JsonNode node : rootNode) {
                    String currencyCode = node.get("cur_unit").asText();
                    if ("USD".equals(currencyCode)) {
                        String rateStr = node.get("deal_bas_r").asText().replace(",", "");
                        return Optional.of(new BigDecimal(rateStr));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("한국수출입은행 API 호출 실패: {}", e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * 백업 API에서 USD/KRW 환율 조회
     */
    public Optional<BigDecimal> fetchExchangeRateFromBackup() {
        try {
            String response = webClientBuilder.build()
                    .get()
                    .uri(backupUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            if (response != null) {
                JsonNode rootNode = objectMapper.readTree(response);
                JsonNode ratesNode = rootNode.get("rates");
                
                if (ratesNode != null && ratesNode.has("KRW")) {
                    double krwRate = ratesNode.get("KRW").asDouble();
                    return Optional.of(BigDecimal.valueOf(krwRate));
                }
            }
        } catch (Exception e) {
            log.warn("백업 환율 API 호출 실패: {}", e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * 환율 조회 (메인 API 실패 시 백업 API 사용)
     */
    public BigDecimal fetchCurrentExchangeRate() {
        // 먼저 한국수출입은행 API 시도
        Optional<BigDecimal> rate = fetchExchangeRateFromKoreaExim(LocalDate.now());
        
        if (rate.isEmpty()) {
            // 주말이나 공휴일의 경우 전날 데이터 시도
            rate = fetchExchangeRateFromKoreaExim(LocalDate.now().minusDays(1));
        }
        
        if (rate.isEmpty()) {
            // 백업 API 시도
            rate = fetchExchangeRateFromBackup();
        }
        
        // 모든 API 실패 시 기본값 반환 (실제 운영에서는 에러 처리 필요)
        return rate.orElse(BigDecimal.valueOf(1380.0));
    }
}
