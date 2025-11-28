package com.buulgyeong.forexanalyzer.controller;

import com.buulgyeong.forexanalyzer.dto.CompanyInputRequest;
import com.buulgyeong.forexanalyzer.dto.ExchangeRateResponse;
import com.buulgyeong.forexanalyzer.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
public class MainController {
    
    private final ExchangeRateService exchangeRateService;
    
    @GetMapping("/")
    public String home(Model model) {
        // 환율 정보 로드
        ExchangeRateResponse exchangeRate = exchangeRateService.getExchangeRateInfo();
        model.addAttribute("exchangeRate", exchangeRate);
        
        // 기본 입력값 설정
        CompanyInputRequest defaultInput = CompanyInputRequest.builder()
                .materialCostUsd(BigDecimal.valueOf(1000))
                .materialRatio(BigDecimal.valueOf(60))
                .sellingPriceKrw(BigDecimal.valueOf(2500000))
                .targetMarginRate(BigDecimal.valueOf(20))
                .otherCostsKrw(BigDecimal.valueOf(200000))
                .build();
        model.addAttribute("companyInput", defaultInput);
        
        return "index";
    }
}
