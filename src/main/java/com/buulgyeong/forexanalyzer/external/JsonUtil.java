package com.buulgyeong.forexanalyzer.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * DTO 객체를 JSON 문자열로 변환 (Pretty JSON)
     */
    public static String toJson(Object dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("DTO → JSON 변환 실패: " + e.getMessage(), e);
        }
    }

    /**
     * DTO 객체를 JSON 문자열로 변환 (단일 라인 JSON)
     */
    public static String toCompactJson(Object dto) {
        try {
            ObjectMapper compactMapper = new ObjectMapper();
            return compactMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("DTO → JSON 변환 실패: " + e.getMessage(), e);
        }
    }

//    public static void dtoToJson(String[] args) throws Exception {
//
//        // DTO 예시
//        ExchangeRateResponse response = ExchangeRateResponse.builder()
//                .currentRate(new BigDecimal("1380.50"))
//                .changeRate1Day(new BigDecimal("0.48"))
//                .changeRate7Day(new BigDecimal("0.09"))
//                .changeRate30Day(new BigDecimal("0.00"))
//                .rate1DayAgo(new BigDecimal("1374.80"))
//                .rate7DaysAgo(new BigDecimal("1379.20"))
//                .rate30DaysAgo(new BigDecimal("1380.50"))
//                .lastUpdated(LocalDate.now())
//                .build();
//
//        ObjectMapper objectMapper = new ObjectMapper();
//        String jsonString = objectMapper.writeValueAsString(response);
//
//        // JSON 문자열을 다른 문자열 안에 삽입
//        String finalString = "여기 환율 정보가 있습니다: " + jsonString;
//
//        System.out.println(finalString);
//    }
}
