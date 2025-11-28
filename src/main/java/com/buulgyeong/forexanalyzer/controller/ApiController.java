package com.buulgyeong.forexanalyzer.controller;

import com.buulgyeong.forexanalyzer.dto.CompanyInputRequest;
import com.buulgyeong.forexanalyzer.dto.DashboardResponse;
import com.buulgyeong.forexanalyzer.dto.ExchangeRateResponse;
import com.buulgyeong.forexanalyzer.dto.ProfitLossAnalysisResponse;
import com.buulgyeong.forexanalyzer.service.ExchangeRateService;
import com.buulgyeong.forexanalyzer.service.ProfitLossAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    
    private final ExchangeRateService exchangeRateService;
    private final ProfitLossAnalysisService profitLossAnalysisService;
    
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
}
