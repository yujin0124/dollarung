package com.buulgyeong.forexanalyzer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "company_input")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyInput {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal materialCostUsd;
    
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal materialRatio;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal sellingPriceKrw;
    
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal targetMarginRate;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal otherCostsKrw;
    
    @Column
    private String sessionId; // 세션별 데이터 관리
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
