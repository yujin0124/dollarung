package com.buulgyeong.forexanalyzer.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class UpstageAiClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private final String apiUrl;
    private final String apiKey;

    public UpstageAiClient(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${external.upstage.api-url}") String apiUrl,
            @Value("${external.upstage.api-key:}") String apiKey) {
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    /**
     * 환율 상태 평가 생성
     */
    public String generateExchangeRateEvaluation(BigDecimal currentRate, BigDecimal breakEvenRate, 
                                                  BigDecimal targetRate, BigDecimal targetMarginRate) {
        if (apiKey == null || apiKey.isEmpty()) {
            return generateDefaultEvaluation(currentRate, breakEvenRate, targetRate, targetMarginRate);
        }
        
        String prompt = String.format("""
            당신은 제조업 환율 분석 전문가입니다.
            현재 환율 상황을 분석하고 발주 권장 여부를 평가해주세요.
            
            - 현재 환율: %.1f원/USD
            - 손익분기점 환율: %.1f원/USD
            - 목표 마진 달성 환율: %.1f원/USD
            - 목표 마진율: %.1f%%
            
            다음 형식으로 간결하게 답변해주세요 (3-4문장):
            1. 현재 환율 상태 평가 (발주 적합/부적합)
            2. 예상 마진 상황
            3. 발주 권장 여부
            """, 
            currentRate.doubleValue(),
            breakEvenRate.doubleValue(),
            targetRate.doubleValue(),
            targetMarginRate.doubleValue()
        );
        
        try {
            return callUpstageApi(prompt);
        } catch (Exception e) {
            log.warn("Upstage AI API 호출 실패: {}", e.getMessage());
            return generateDefaultEvaluation(currentRate, breakEvenRate, targetRate, targetMarginRate);
        }
    }
    
    /**
     * 모니터링 전략 생성
     */
    public String generateMonitoringStrategy(BigDecimal currentRate, BigDecimal breakEvenRate,
                                              BigDecimal targetRate, BigDecimal changeRate30Day) {
        if (apiKey == null || apiKey.isEmpty()) {
            return generateDefaultStrategy(currentRate, breakEvenRate, targetRate, changeRate30Day);
        }
        
        String prompt = String.format("""
            당신은 제조업 환율 위험 관리 전문가입니다.
            다음 환율 상황에 맞는 모니터링 전략을 제안해주세요.
            
            - 현재 환율: %.1f원/USD
            - 손익분기점 환율: %.1f원/USD
            - 목표 달성 환율: %.1f원/USD
            - 30일 환율 변동률: %.2f%%
            
            다음 형식으로 3가지 전략을 bullet point로 제안해주세요:
            • 환율 모니터링 기준점
            • 환위험 헷지 전략
            • 재고 관리 관련 조언
            """,
            currentRate.doubleValue(),
            breakEvenRate.doubleValue(),
            targetRate.doubleValue(),
            changeRate30Day.doubleValue()
        );
        
        try {
            return callUpstageApi(prompt);
        } catch (Exception e) {
            log.warn("Upstage AI API 호출 실패: {}", e.getMessage());
            return generateDefaultStrategy(currentRate, breakEvenRate, targetRate, changeRate30Day);
        }
    }
    
    private String callUpstageApi(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "solar-mini");
        requestBody.put("messages", List.of(
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("max_tokens", 500);
        requestBody.put("temperature", 0.7);
        
        String response = webClientBuilder.build()
            .post()
            .uri(apiUrl)
            .header("Authorization", "Bearer " + apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .block();
        
        if (response != null) {
            try {
                JsonNode rootNode = objectMapper.readTree(response);
                JsonNode choicesNode = rootNode.get("choices");
                if (choicesNode != null && choicesNode.isArray() && !choicesNode.isEmpty()) {
                    return choicesNode.get(0).get("message").get("content").asText();
                }
            } catch (Exception e) {
                log.error("Upstage API 응답 파싱 실패", e);
            }
        }
        
        throw new RuntimeException("Upstage API 응답 파싱 실패");
    }
    
    private String generateDefaultEvaluation(BigDecimal currentRate, BigDecimal breakEvenRate,
                                              BigDecimal targetRate, BigDecimal targetMarginRate) {
        double current = currentRate.doubleValue();
        double breakEven = breakEvenRate.doubleValue();
        double target = targetRate.doubleValue();
        
        if (current <= target) {
            return String.format(
                "현재 환율(%.1f원)은 목표 마진율 %.1f%%를 달성할 수 있는 최적의 구간입니다. " +
                "적극적인 발주를 권장하며, 환율 상승 전 충분한 재고 확보를 고려하세요.",
                current, targetMarginRate.doubleValue()
            );
        } else if (current <= breakEven) {
            return String.format(
                "현재 환율(%.1f원)은 흑자 유지가 가능하나 목표 마진에는 미달하는 구간입니다. " +
                "필수 물량 위주의 선별적 발주를 권장합니다.",
                current
            );
        } else {
            return String.format(
                "현재 환율(%.1f원)은 손익분기점(%.1f원)을 초과하여 적자 위험이 있는 구간입니다. " +
                "가능하다면 발주를 지연하고 환율 하락을 기다리는 것이 좋습니다.",
                current, breakEven
            );
        }
    }
    
    private String generateDefaultStrategy(BigDecimal currentRate, BigDecimal breakEvenRate,
                                            BigDecimal targetRate, BigDecimal changeRate30Day) {
        double target = targetRate.doubleValue();
        double breakEven = breakEvenRate.doubleValue();
        
        return String.format("""
            • 환율이 %.1f원 이하로 하락 시 즉시 발주를 검토하세요
            • 환율 상승 추세가 예상되면 선물환 계약을 통해 환율을 고정하는 것을 고려하세요
            • 재고 보유 기간(30일)을 고려하여 환율 변동 리스크를 관리하세요
            """, target);
    }
}
