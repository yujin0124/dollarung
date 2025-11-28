package com.buulgyeong.forexanalyzer.service;

import com.buulgyeong.forexanalyzer.dto.CompanyInputRequest;
import com.buulgyeong.forexanalyzer.dto.ExchangeRateResponse;
import com.buulgyeong.forexanalyzer.dto.FinalReportResponse;
import com.buulgyeong.forexanalyzer.dto.ProfitLossAnalysisResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinalReportService {

    private final ExchangeRateService exchangeRateService;
    private final ProfitLossAnalysisService profitLossAnalysisService;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${external.upstage.api-url}")
    private String upstageBaseUrl;

    @Value("${external.upstage.api-key:}")
    private String apiKey;

    /**
     * 엔트리 포인트: 입력을 받아 (1) 환율 요약 JSON, (2) 손익 분석 전체 JSON 취합,
     * (3) AI에 전달 -> Markdown 보고서 수신 또는 fallback 생성 -> FinalReportResponse 반환
     */
    public FinalReportResponse generateFinalReportForInput(CompanyInputRequest input) {
        Instant started = Instant.now();

        // 1) 환율 요약 JSON (토큰 절약용) & 전체 분석 JSON
        String exchangeRateJson;
        String analysisJson;
        try {
            ExchangeRateResponse ex = exchangeRateService.getExchangeRateInfo();
            exchangeRateJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ex);
        } catch (Exception e) {
            log.warn("환율 요약 직렬화 실패: {}", e.getMessage());
            exchangeRateJson = "{}";
        }

        try {
            ProfitLossAnalysisResponse analysis = profitLossAnalysisService.analyze(input);
            analysisJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(analysis);
        } catch (Exception e) {
            log.error("손익 분석 취합 실패: {}", e.getMessage(), e);
            analysisJson = "{}";
        }

        // 2) 프롬프트 생성 & AI 호출
        String reportMarkdown;
        try {
            reportMarkdown = generateFinalReport(exchangeRateJson, analysisJson);
            if (reportMarkdown == null || reportMarkdown.isBlank()) {
                log.warn("AI 응답 비어있음 - fallback 리포트 생성");
                reportMarkdown = generateFallbackReport(analysisJson, exchangeRateJson);
            }
        } catch (Exception e) {
            log.error("AI 호출 실패: {}", e.getMessage(), e);
            reportMarkdown = generateFallbackReport(analysisJson, exchangeRateJson);
        }

        FinalReportResponse response = FinalReportResponse.builder()
                .reportMarkdown(reportMarkdown)
                .aiContextJson(exchangeRateJson)   // 요약(환율) JSON을 aiContext로 전달
                .fullAnalysisJson(analysisJson)
                .generatedAt(started)
                .build();

        return response;
    }

    /**
     * 프롬프트 생성 및 Upstage 호출
     */
    private String generateFinalReport(String exchangeRateData, String profitLossAnalysisResponse) {
        String prompt = String.format("""
                당신은 기업의 재무·원가·환율·손익 구조를 설명하는 전문 애널리스트입니다.
                아래 제공되는 OUTPUT DATA는 특정 기업의 실시간 손익 분석 시스템에서 산출된 결과입니다.
                데이터를 기반으로, 해당 기업의 현재 원가 구조, 환율 영향, 마진 상태, 목표 달성 여부를
                전문 분석 리포트 형태로 작성하세요.
                                
                [OUTPUT DATA]
                환율 요약(JSON): %s
                손익 분석 전체(JSON): %s
                                
                [작성 규칙]
                • 데이터 기반 분석만 작성하고, 과도한 추측이나 데이터에 없는 정보는 포함하지 않는다.
                • 비즈니스 전략·원가 관리 측면에서 해석을 명확히 포함한다.
                • **절대로** #, ##, ###, **, -, * 등 마크다운 문법을 사용하지 마라.
                • 굵은 글씨, 기울임, 리스트 기호 사용 금지.
                • 문단이나 항목 사이에 \n\n 을 넣어서 빈줄로 문단 및 항목을 구분하라.
                                                 
                [리포트 구성]
                다음 형식으로 리포트를 구성하는 각 항목들을 bullet point로 제안해주세요:
                
                📑 요약(Summary)
                💹 원가 구조 분석 (환율 영향 포함)
                💵 마진 분석 (현재 vs 목표)
                💼 리스크 및 개선 포인트 (우선순위 포함)
                💡 종합 의견(Conclusion)
                📊 전략적 접근 및 팁
                
                """, exchangeRateData, profitLossAnalysisResponse);

        return callUpstageApi(prompt);
    }

    /**
     * Upstage(혹은 Solar) API 호출 + 응답 파싱
     */
    private String callUpstageApi(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Upstage API key 미설정 - AI 호출 불가");
            throw new IllegalStateException("Upstage API key is not configured.");
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "solar-pro2");
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        requestBody.put("max_tokens", 1200);
        requestBody.put("temperature", 0.2);

        String rawResponse = webClientBuilder.build()
                .post()
                .uri(upstageBaseUrl) // 실제 엔드포인트에 맞게 조정하세요
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (rawResponse == null) {
            throw new RuntimeException("Upstage API 응답이 없음");
        }

        // 응답 파싱: choices[0].message.content 또는 choices[0].text 등 가능성 처리
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode first = choices.get(0);
                JsonNode message = first.path("message");
                if (!message.isMissingNode() && message.path("content").isTextual()) {
                    return message.path("content").asText();
                }
                if (first.path("text").isTextual()) {
                    return first.path("text").asText();
                }
            }
            // fallback: "text" root
            if (root.path("text").isTextual()) return root.path("text").asText();
        } catch (Exception e) {
            log.error("Upstage 응답 파싱 오류: {}", e.getMessage(), e);
        }

        throw new RuntimeException("Upstage API 응답 파싱 실패");
    }

    /**
     * AI 실패 시 local fallback 리포트 생성 (간단, 데이터 기반 요약)
     */
    private String generateFallbackReport(String analysisJson, String exchangeRateJson) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("# 자동 생성 리포트 (Fallback)\n\n");

            // try extract a few safe fields
            JsonNode analysis = objectMapper.readTree(analysisJson);
            JsonNode realtime = analysis.path("realTimeProfitLoss");
            if (!realtime.isMissingNode() && realtime.has("currentCost")) {
                sb.append("## 요약\n");
                sb.append(String.format("- 현재 원가: %s원\n", realtime.path("currentCost").asText("N/A")));
                sb.append(String.format("- 현재 마진: %s원 (%s%%)\n", realtime.path("currentMargin").asText("N/A"),
                        realtime.path("currentMarginRate").asText("N/A")));
                sb.append(String.format("- 목표 달성 여부: %s\n\n", realtime.path("targetAchieved").asBoolean(false) ? "달성" : "미달성"));
            } else {
                // If no realtime block, fallback to exchangeRateJson minimal info
                JsonNode ex = objectMapper.readTree(exchangeRateJson);
                sb.append("## 요약\n");
                sb.append(String.format("- 현재 환율: %s원\n", ex.path("currentRate").asText("N/A")));
            }

            sb.append("## 권고(간단)\n");
            sb.append("- 환율이 손익분기 근처에 있으면 발주 분할 및 헷지 검토\n");
            sb.append("- 상세 리포트 생성 실패 시 수동 검토 권장\n");
            return sb.toString();
        } catch (Exception e) {
            log.warn("Fallback 리포트 생성 중 오류: {}", e.getMessage());
            return "# 자동 생성 리포트\n\n- 데이터 부족 또는 처리 오류로 인해 간단 요약만 제공됩니다.";
        }
    }
}