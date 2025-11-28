package com.buulgyeong.forexanalyzer.repository;

import com.buulgyeong.forexanalyzer.entity.ExchangeRateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateHistoryRepository extends JpaRepository<ExchangeRateHistory, Long> {
    
    Optional<ExchangeRateHistory> findByRateDateAndCurrencyCode(LocalDate rateDate, String currencyCode);
    
    @Query("SELECT e FROM ExchangeRateHistory e WHERE e.currencyCode = :currencyCode AND e.rateDate >= :startDate ORDER BY e.rateDate ASC")
    List<ExchangeRateHistory> findByDateRangeAndCurrencyCode(@Param("startDate") LocalDate startDate, @Param("currencyCode") String currencyCode);
    
    @Query("SELECT e FROM ExchangeRateHistory e WHERE e.currencyCode = :currencyCode ORDER BY e.rateDate DESC LIMIT 1")
    Optional<ExchangeRateHistory> findLatestByCurrencyCode(@Param("currencyCode") String currencyCode);
    
    boolean existsByRateDateAndCurrencyCode(LocalDate rateDate, String currencyCode);
}
