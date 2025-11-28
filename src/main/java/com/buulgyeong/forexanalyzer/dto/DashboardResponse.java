package com.buulgyeong.forexanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    
    private ExchangeRateResponse exchangeRate;
    private ProfitLossAnalysisResponse analysis;
    private CompanyInputRequest companyInput;
}
