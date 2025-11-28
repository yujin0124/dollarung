package com.buulgyeong.forexanalyzer.repository;

import com.buulgyeong.forexanalyzer.entity.CompanyInput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyInputRepository extends JpaRepository<CompanyInput, Long> {
    
    Optional<CompanyInput> findBySessionId(String sessionId);
    
    void deleteBySessionId(String sessionId);
}
