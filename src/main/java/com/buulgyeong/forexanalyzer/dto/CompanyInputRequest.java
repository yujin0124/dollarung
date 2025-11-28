package com.buulgyeong.forexanalyzer.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyInputRequest {
    
    @NotNull(message = "원자재 단가(USD)는 필수입니다")
    @Positive(message = "원자재 단가는 0보다 커야 합니다")
    private BigDecimal materialCostUsd;  // 원자재 단가(USD)
    
    @NotNull(message = "원자재 사용 비중(%)은 필수입니다")
    @DecimalMin(value = "0.01", message = "원자재 비중은 0.01% 이상이어야 합니다")
    @DecimalMax(value = "100", message = "원자재 비중은 100% 이하여야 합니다")
    private BigDecimal materialRatio;    // 원자재 사용 비중(%)
    
    @NotNull(message = "납품 단가(KRW)는 필수입니다")
    @Positive(message = "납품 단가는 0보다 커야 합니다")
    private BigDecimal sellingPriceKrw;  // 제품 납품 단가(KRW)
    
    @NotNull(message = "목표 마진율(%)은 필수입니다")
    @DecimalMin(value = "0", message = "목표 마진율은 0% 이상이어야 합니다")
    @DecimalMax(value = "100", message = "목표 마진율은 100% 이하여야 합니다")
    private BigDecimal targetMarginRate; // 목표 마진율(%)
    
    @NotNull(message = "기타 비용(KRW)은 필수입니다")
    private BigDecimal otherCostsKrw;    // 기타 비용(KRW) - 물류비, 관세, 가공비 등
}
