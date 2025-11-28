package com.buulgyeong.forexanalyzer.external;

import com.buulgyeong.forexanalyzer.dto.HistoricalRate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
     * 네이버페이 증권에서 실시간 환율 스크래핑
     */
    public BigDecimal fetchCurrentExchangeRateFromNaver() {
        try {
            Connection connection = Jsoup.connect("https://finance.naver.com/marketindex/exchangeDetail.naver?marketindexCd=FX_USDKRW");
            Document document = connection.get();

            Element p = document.select("p.no_today").first();
            String rateStr = p.select("span:not(.txt_won)").stream()
                    .map(Element::text)
                    .collect(Collectors.joining())
                    .replace(",", "");

            return new BigDecimal(rateStr);

        } catch (Exception e) {
            log.warn("NAVER 환율 스크래핑 실패: {}", e.getMessage());
            return BigDecimal.valueOf(1380.0);
        }
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

    public List<HistoricalRate> fetchLast30Days() {
        List<HistoricalRate> rates = new ArrayList<>();
        Map<LocalDate, BigDecimal> cache = new HashMap<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 30; i++) {
            LocalDate targetDate = today.minusDays(i);
            BigDecimal rate = null;
            LocalDate cursor = targetDate;

            int maxLookbackDays = 7;
            int lookback = 0;

            while (rate == null && lookback < maxLookbackDays) {
                if (cache.containsKey(cursor)) {
                    rate = cache.get(cursor);
                } else {
                    Optional<BigDecimal> rateOpt = fetchExchangeRateFromKoreaExim(cursor);
                    if (rateOpt.isPresent()) {
                        rate = rateOpt.get();
                        cache.put(cursor, rate);
                    } else {
                        cursor = cursor.minusDays(1);
                        lookback++;
                    }
                }
            }

            if (rate == null) {
                rate = BigDecimal.valueOf(1380.0);
            }

            rates.add(0, new HistoricalRate(targetDate, rate));
            cache.put(targetDate, rate);
        }

        return rates;
    }

}
