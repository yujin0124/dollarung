package com.buulgyeong.forexanalyzer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_rate_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private LocalDate rateDate;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal rate;
    
    @Column(nullable = false)
    private String currencyCode; // USD
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
