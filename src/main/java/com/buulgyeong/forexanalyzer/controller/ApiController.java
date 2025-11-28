package com.buulgyeong.forexanalyzer.controller;

import com.buulgyeong.forexanalyzer.dto.*;
import com.buulgyeong.forexanalyzer.service.ExchangeRateService;
import com.buulgyeong.forexanalyzer.service.FinalReportService;
import com.buulgyeong.forexanalyzer.service.ProfitLossAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    
    private final ExchangeRateService exchangeRateService;
    private final ProfitLossAnalysisService profitLossAnalysisService;
    private final FinalReportService finalReportService;
    
    /**
     * 실시간 환율 정보 조회
     */
    @GetMapping("/exchange-rate")
    public ResponseEntity<ExchangeRateResponse> getExchangeRate() {
        ExchangeRateResponse response = exchangeRateService.getExchangeRateInfo();
        return ResponseEntity.ok(response);
    }
    
    /**
     * 손익 분석 수행
     */
    @PostMapping("/analyze")
    public ResponseEntity<ProfitLossAnalysisResponse> analyze(@Valid @RequestBody CompanyInputRequest request) {
        ProfitLossAnalysisResponse response = profitLossAnalysisService.analyze(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 대시보드 전체 데이터 조회
     */
    @PostMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(@Valid @RequestBody CompanyInputRequest request) {
        ExchangeRateResponse exchangeRate = exchangeRateService.getExchangeRateInfo();
        ProfitLossAnalysisResponse analysis = profitLossAnalysisService.analyze(request);
        
        DashboardResponse response = DashboardResponse.builder()
                .exchangeRate(exchangeRate)
                .analysis(analysis)
                .companyInput(request)
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * 최종 AI 분석 리포트 생성 (AI -> Markdown 반환)
     *
     * 요청: CompanyInputRequest (JSON)
     * 응답: FinalReportResponse { reportMarkdown, aiContextJson, fullAnalysisJson, generatedAt }
     *
     * 주의: AI 호출이 포함되어 있어 응답에 시간이 걸릴 수 있음. 필요하면 비동기 작업으로 변경 권장.
     */
    @PostMapping("/report/final")
    public ResponseEntity<FinalReportResponse> generateFinalReport(@Valid @RequestBody CompanyInputRequest request) {
        try {
            FinalReportResponse report = finalReportService.generateFinalReportForInput(request);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            log.error("최종 리포트 생성 실패", e);
            FinalReportResponse err = FinalReportResponse.builder()
                    .reportMarkdown("최종 리포트 생성에 실패했습니다: " + e.getMessage())
                    .aiContextJson("{}")
                    .fullAnalysisJson("{}")
                    .generatedAt(java.time.Instant.now())
                    .build();
            return ResponseEntity.internalServerError().body(err);
        }
    }
}
