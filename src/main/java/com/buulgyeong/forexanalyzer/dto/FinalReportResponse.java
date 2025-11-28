package com.buulgyeong.forexanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalReportResponse {
    private String reportMarkdown;   // AI 생성 리포트(마크다운)
    private String aiContextJson;    // AI에 보낸 요약 JSON (환율 등)
    private String fullAnalysisJson; // 전체 분석 JSON (원본)
    private Instant generatedAt;
}