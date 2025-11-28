# ForexPulse - 부울경 제조업 환율 손익 분석 서비스

부울경(부산·울산·경남) 지역 원자재 가공 중심 제조업체들을 위한 실시간 환율 손익 분석 서비스입니다.

## 🎯 서비스 목적

원-달러 환율 변동에 따른 손익 계산 및 최적 발주 타이밍 분석을 통해 영세 제조업 사업자들의 의사결정을 지원합니다.

## ✨ 주요 기능

### 1. 실시간 환율 정보
- 현재 USD/KRW 환율 표시
- 1일, 7일, 30일 변동률 (%) 표시
- 30일 환율 추이 그래프 시각화

### 2. 기업 정보 입력
- 원자재 단가 (USD)
- 원자재 사용 비중 (%)
- 제품 납품 단가 (KRW)
- 목표 마진율 (%)
- 기타 비용 (KRW)

### 3. 실시간 손익 분석
- 현재 제품 원가 (30일 전 대비 비교)
- 현재 마진 및 마진율
- 목표 마진 달성 여부

### 4. 발주 타이밍 가이드
- 손익분기점 환율 정보
- 목표 달성 환율 정보
- 환율 상태 시각화 Bar UI
- AI 기반 환율 평가 및 모니터링 전략

### 5. 시나리오 분석
- 환율 시나리오별 원가/마진 비교 그래프
- 환율 변동에 따른 마진율 변화 그래프
- 상세 원가 분석 요약

## 🛠 기술 스택

### Backend
- Java 17
- Spring Boot 3.2
- Spring Data JPA
- H2 Database (In-memory)
- WebFlux (External API 호출)

### Frontend
- Thymeleaf
- HTML5 / CSS3
- Vanilla JavaScript
- Chart.js

### External APIs
- 한국수출입은행 환율 API
- Upstage AI API (환율 평가 및 전략 생성)

## 📁 프로젝트 구조

```
forex-analyzer/
├── build.gradle
├── settings.gradle
├── gradlew
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── src/
│   ├── main/
│   │   ├── java/com/buulgyeong/forexanalyzer/
│   │   │   ├── ForexAnalyzerApplication.java
│   │   │   ├── config/
│   │   │   │   └── WebClientConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── MainController.java
│   │   │   │   └── ApiController.java
│   │   │   ├── dto/
│   │   │   │   ├── CompanyInputRequest.java
│   │   │   │   ├── DashboardResponse.java
│   │   │   │   ├── ExchangeRateResponse.java
│   │   │   │   └── ProfitLossAnalysisResponse.java
│   │   │   ├── entity/
│   │   │   │   ├── CompanyInput.java
│   │   │   │   └── ExchangeRateHistory.java
│   │   │   ├── external/
│   │   │   │   ├── ExchangeRateApiClient.java
│   │   │   │   └── UpstageAiClient.java
│   │   │   ├── repository/
│   │   │   │   ├── CompanyInputRepository.java
│   │   │   │   └── ExchangeRateHistoryRepository.java
│   │   │   └── service/
│   │   │       ├── ExchangeRateService.java
│   │   │       └── ProfitLossAnalysisService.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── static/
│   │       │   ├── css/
│   │       │   │   └── style.css
│   │       │   └── js/
│   │       │       └── app.js
│   │       └── templates/
│   │           └── index.html
│   └── test/
│       └── java/com/buulgyeong/forexanalyzer/
└── README.md
```

## 🚀 실행 방법

### 1. 환경 변수 설정 (선택)
```bash
# 한국수출입은행 API 키 (없으면 샘플 데이터 사용)
export KOREAEXIM_API_KEY=your_api_key

# Upstage AI API 키 (없으면 기본 메시지 생성)
export UPSTAGE_API_KEY=your_api_key
```

### 2. 애플리케이션 실행
```bash
# Gradle Wrapper 사용
./gradlew bootRun

# 또는 JAR 빌드 후 실행
./gradlew build
java -jar build/libs/forex-analyzer-0.0.1-SNAPSHOT.jar
```

### 3. 접속
- 웹 브라우저에서 `http://localhost:8080` 접속

## 🎨 UI/UX 특징

- 다크모드/라이트모드 전환 지원
- 테마 컬러: #ffcd00 (Yellow) / #2c2c2c (Dark Gray)
- 반응형 디자인
- Chart.js 기반 인터랙티브 그래프
- 직관적인 환율 상태 시각화 Bar

## 📊 API 엔드포인트

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | 메인 페이지 |
| GET | `/api/exchange-rate` | 실시간 환율 정보 조회 |
| POST | `/api/analyze` | 손익 분석 수행 |
| POST | `/api/dashboard` | 전체 대시보드 데이터 조회 |

## 📝 라이선스

This project is developed for the Busan-Ulsan-Gyeongnam Hackathon 2024.

## 👥 팀 정보

부울경 지역 문제 해결 해커톤 참가 프로젝트
