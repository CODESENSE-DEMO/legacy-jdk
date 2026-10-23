# NovaBank Deposit Operations — 수신 업무 지원 시스템

영업점 수신 창구에서 쓰는 내부 웹 시스템이다. 계좌 개설·입출금·상태 관리, 계좌 이체(당행/타행·수수료·한도),
대출 상환 계산기, 일마감 리포트(본점 집계 전송용 XML)를 제공한다. 2013년 11월 구축본(2.3.1).

## 기술 구성

| 구분 | 내용 |
|---|---|
| 언어 | Java 21 |
| 프레임워크 | Spring Framework 6.1 (XML 설정) · Spring MVC |
| 화면(FE) | JSP 2.3 + JSTL 1.2 + jQuery 1.12 |
| 서버(BE) | Spring MVC 컨트롤러 · 서비스 · 메모리 저장소 · JSON/XML API |
| 빌드 | Maven 3, `war` 패키징 |
| 실행(개발) | `jetty-ee10-maven-plugin` 내장 Jetty 12 (Jakarta Servlet 6.0) |
| 로깅 | SLF4J + Log4j 1.2 |
| 기타 | Jackson 2.9(JSON), JAXB(XML 리포트), Joda-Time, Commons Lang 2 |
| 저장소 | 메모리(기동 시 표본 계좌 6개 생성). 계정계 DB 연동은 2차 과제 |

## 요구 사항

- JDK 21
- Maven 3.x
- 인터넷(최초 1회 의존성 다운로드)

## 빌드·실행·테스트

```bash
# 빌드 + 단위 테스트
mvn clean package

# 개발용 내장 서버(Jetty 12) 기동 → http://localhost:8080/novabank/
mvn jetty:run

# 테스트만
mvn test
```

## 화면

| 경로 | 화면 |
|---|---|
| `/` | 대시보드 — 관리 계좌·총 잔액·오늘 거래, 계좌 요약, 최근 거래 |
| `/accounts` | 계좌 목록, 신규 계좌 개설(보통/당좌/정기) |
| `/accounts/{accountNo}` | 계좌 상세 — 입금·출금·상태 변경(정상/거래정지/해지), 거래 내역 |
| `/transfer` | 계좌 이체 — 당행/타행, 수수료 실시간 미리보기, 이체 결과 |
| `/loan` | 대출 상환 계산기 — 원리금균등/원금균등 스케줄 |
| `/close` | 일마감 리포트 조회, XML/JSON 내려받기 |

## API

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/accounts` | 계좌 목록(JSON) |
| GET | `/api/accounts/{accountNo}` | 계좌 1건 |
| GET | `/api/accounts/{accountNo}/transactions` | 거래 내역(최신순) |
| GET | `/api/transfer/fee?toBank=KDB&amount=1000000` | 이체 수수료 미리보기 |
| GET | `/api/loan/schedule?principal=10000000&rate=4.5&months=12&method=EQUAL_PAYMENT` | 상환 스케줄 |
| GET | `/api/close.xml?date=yyyy-MM-dd` | 일마감 XML(본점 집계 연동 형식) |
| GET | `/api/close.json?date=yyyy-MM-dd` | 일마감 JSON |

## 패키지 구조

```
com.novabank.ops
├── domain       Account · Transaction · TransferResult · LoanSchedule · 상태/종류 enum
├── repository   저장소 인터페이스 + 메모리 구현
├── service      AccountService · TransactionService · TransferService · LoanCalculatorService · DailyCloseService
├── report       DailyCloseReport (JAXB)
├── scheduler    CloseScheduler (주기 리포트)
├── util         DateUtil · MoneyUtil · TxIdGenerator
└── web          컨트롤러 5종 + GlobalExceptionHandler
```

설정값은 `src/main/resources/application.properties`(이체 한도·수수료, 출금 한도, 대출 계산 범위, 스케줄 주기),
Spring 설정은 `src/main/webapp/WEB-INF/spring/*.xml` 에 있다.

## 업무 규칙 요약

- 계좌 상태: 정상(A) · 거래정지(F) · 해지(C). 거래정지·해지 계좌는 입출금·이체 불가. 해지는 잔액 0원일 때만, 되돌릴 수 없음.
- 정기예금 개설은 최초 입금 1,000,000원 이상. 금액은 원 단위 정수.
- 출금 1회 한도 10,000,000원. 이체 1회 한도 50,000,000원 · 1일 한도 100,000,000원(이체출금 누계).
- 이체 수수료: 당행 0원. 타행은 10만원 미만 500원 · 100만원 미만 800원 · 그 이상 1,500원, 영업시간(평일 09~18시) 외 +300원.
  수수료는 별도 거래(FEE)로 기록.
- 대출 계산: 연이율 1~20%, 1~480개월. 원리금균등은 마지막 회차에서 잔액을 0으로 맞춘다.
- 거래번호 = `NB` + 영숫자 6 + 체크 2.
