# NovaBank Deposit Operations — 수신 업무 지원 시스템

영업점 수신 창구에서 쓰는 내부 웹 시스템이다. 계좌 개설·입출금·상태 관리, 계좌 이체 (당행/타행·수수료·한도),
대출 상환 계산기, 일마감 리포트 (본점 집계 전송용 XML) 를 제공한다. 2013 년 11 월 구축본 (v2.3.1).

## 기술 구성

| 구분 | 내용 |
|---|---|
| 언어 | Java 21 |
| 프레임워크 | Spring Framework 6.1 (XML 설정) · Spring MVC |
| 화면 (FE) | JSP 2.3 + JSTL 1.2 + jQuery 1.12 |
| 서버 (BE) | Spring MVC 컨트롤러 · 서비스 · 메모리 저장소 · JSON/XML API |
| 빌드 | Maven 3, `war` 패키징 |
| 실행 (개발) | `jetty-ee10-maven-plugin` 내장 Jetty 12 (Jakarta Servlet 6.0) |
| 로깅 | Logback (SLF4J) + Log4j 1.2 (병기) |
| 기타 | Jackson 2.15(JSON), JAXB 4.0(XML 리포트), Commons Lang 3, JUnit 5, Mockito 5 |
| 저장소 | 메모리 (기동 시 표본 계좌 6 개 생성). 계정계 DB 연동은 2 차 과제 |

## 요구 사항

- JDK 21
- Maven 3.x
- 인터넷 (최초 1 회 의존성 다운로드)

## 빌드·실행·테스트

```bash
# 빌드 + 단위 테스트
mvn clean package

# 개발용 내장 서버 (Jetty 12) 기동 → http://localhost:8080/novabank/
mvn jetty:run

# 테스트만
mvn test
```

## 화면

| 경로 | 화면 |
|---|---|
| `/` | 대시보드 — 관리 계좌·총 잔액·오늘 거래, 계좌 요약, 최근 거래 |
| `/accounts` | 계좌 목록, 신규 계좌 개설 (보통/당좌/정기) |
| `/accounts/{accountNo}` | 계좌 상세 — 입금·출금·상태 변경 (정상/거래정지/해지), 거래 내역 |
| `/transfer` | 계좌 이체 — 당행/타행, 수수료 실시간 미리보기, 이체 결과 |
| `/loan` | 대출 상환 계산기 — 원리금균등/원금균등 스케줄 |
| `/close` | 일마감 리포트 조회, XML/JSON 내려받기 |

## API

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/accounts` | 계좌 목록 (JSON) |
| GET | `/api/accounts/{accountNo}` | 계좌 1 건 |
| GET | `/api/accounts/{accountNo}/transactions` | 거래 내역 (최신순) |
| GET | `/api/transfer/fee?toBank=KDB&amount=1000000` | 이체 수수료 미리보기 |
| GET | `/api/loan/schedule?principal=10000000&rate=4.5&months=12&method=EQUAL_PAYMENT` | 상환 스케줄 |
| GET | `/api/close.xml?date=yyyy-MM-dd` | 일마감 XML(본점 집계 연동 형식) |
| GET | `/api/close.json?date=yyyy-MM-dd` | 일마감 JSON |

## 상세 패키지 구조

### 도메인 (`com.novabank.ops.domain`) — 8 개 파일

| 파일 | 줄 수 | 역할 |
|---|---|---|
| `Account.java` | 127 | 계좌 엔티티 — accountNo, customerName, type(SAVINGS/CHECKING/FIXED), balance, status(ACTIVE/FROZEN/CLOSED), openedAt, closedAt, getMaskedName() |
| `Transaction.java` | 104 | 거래 엔티티 — seq, txId, accountNo, type(DEPOSIT/WITHDRAW/TRANSFER/FEE), amount, balanceAfter, counterparty, memo, createdAt |
| `LoanSchedule.java` | 85 | 대출 상환 계획 — method(EQUAL_PAYMENT/EQUAL_PRINCIPAL), principal, annualRatePercent, months, totalInterest, totalPayment, installments(List<LoanInstallment>) |
| `LoanInstallment.java` | 69 | 단일 회차 — month, payment, principal, interest, balance |
| `TransferResult.java` | 95 | 이체 결과 — txId, fromAccountNo, toAccountNo, toBank, amount, fee, totalDebit, balanceAfter, executedAt |
| `AccountStatus.java` | 40 | Enum — ACTIVE, FROZEN, CLOSED |
| `AccountType.java` | 38 | Enum — SAVINGS, CHECKING, FIXED |
| `TxType.java` | 29 | Enum — DEPOSIT, WITHDRAW, TRANSFER, FEE |

### 서비스 (`com.novabank.ops.service`) — 5 개 파일

| 파일 | 줄 수 | 주요 기능 |
|---|---|---|
| `AccountService.java` | 114 | 계좌 개설 (open), 조회 (list, get), 상태 변경 (updateStatus), 해지 (close), 잔액 합산 (getTotalBalance), 이름 마스킹 (getMaskedName) |
| `TransactionService.java` | 120 | 입금 (deposit), 출금 (withdraw), 거래 내역 조회 (getTransactions), 출금 한도 검증 (withdrawLimitCheck) |
| `TransferService.java` | 146 | 이체 (executeTransfer), 수수료 계산 (calculateFee), 이체 한도 검증 (1 회/일일), 타행 수수료 (영업시간 외 +300 원) |
| `LoanCalculatorService.java` | 108 | 원리금균등상환 (equalPayment), 원금균등상환 (equalPrincipal), 이자 계산, 총 상환액 산출 |
| `DailyCloseService.java` | 112 | 일일 정산 리포트 생성 (generateReport), 거래 집계, XML/JSON 포맷 생성, DailyCloseReport(JAXB) |

### 저장소 (`com.novabank.ops.repository`) — 4 개 파일

| 파일 | 줄 수 | 역할 |
|---|---|---|
| `AccountRepository.java` | 20 | 계좌 데이터 접근 인터페이스 — save, findAll, findById, deleteById, countActive, getTotalBalance |
| `InMemoryAccountRepository.java` | 84 | In-Memory 계좌 저장소 — 기동 시 6 개 표본 계좌 생성 (계좌번호 110-0917-000001~000006) |
| `TransactionRepository.java` | 19 | 거래 데이터 접근 인터페이스 — save, findByAccountNoOrderBySeqDesc, countAll |
| `InMemoryTransactionRepository.java` | 50 | In-Memory 거래 저장소 — 시퀀스 번호로 동시성 보장 |

### 웹 레이어 (`com.novabank.ops.web`) — 6 개 파일

| 파일 | 줄 수 | 주요 엔드포인트 |
|---|---|---|
| `HomeController.java` | 41 | `GET /` — 대시보드 (오늘 날짜, 일마감 리포트, 계좌 목록, 최근 10 건 거래) |
| `AccountController.java` | 105 | `GET/POST /accounts` — 계좌 목록/개설, `GET/POST /accounts/{accountNo}` — 상세/입출금/상태변경, `GET /api/accounts` — JSON API |
| `CloseController.java` | 52 | `GET /close` — 일마감 화면, `GET /api/close.xml` — XML 리포트, `GET /api/close.json` — JSON 리포트 |
| `LoanController.java` | 58 | `GET /loan` — 대출 계산기 화면, `GET /api/loan/schedule` — 상환 스케줄 API |
| `TransferController.java` | 84 | `GET /transfer` — 이체 화면, `POST /transfer` — 이체 실행, `GET /api/transfer/fee` — 수수료 미리보기 |
| `GlobalExceptionHandler.java` | 42 | `@ControllerAdvice` — IllegalArgumentException → 400, Exception → 500, error.jsp 렌더링 |

### 스케줄러 (`com.novabank.ops.scheduler`) — 1 개 파일

| 파일 | 줄 수 | 역할 |
|---|---|---|
| `CloseScheduler.java` | 57 | `@Scheduled(fixedDelay = close.schedule.fixed.delay)` — 정기 일마감 리포트 자동 생성 |

### 리포트 (`com.novabank.ops.report`) — 1 개 파일

| 파일 | 줄 수 | 역할 |
|---|---|---|
| `DailyCloseReport.java` | 209 | JAXB 엔티티 — 지점코드, 집계일, 계좌수, 총잔액, 거래건수, 거래종류별 내역, XML/JSON 직렬화 |

### 유틸리티 (`com.novabank.ops.util`) — 3 개 파일

| 파일 | 줄 수 | 역할 |
|---|---|---|
| `DateUtil.java` | 77 | 날짜/시간 포맷팅 (yyyy-MM-dd, yyyy-MM-dd HH:mm, 영업시간 판정) |
| `MoneyUtil.java` | 41 | 금액 포맷팅 (원 단위, 콤마 구분), 금액 계산 |
| `TxIdGenerator.java` | 76 | 거래번호 생성 — `NB` + 영숫자 6 자 + 체크 2 자 (정규식: `^NB[A-Z0-9]{6}[0-9A-F]{2}$`) |

### 설정 (`src/main/resources`) — 3 개 파일

| 파일 | 줄 수 | 역할 |
|---|---|---|
| `application.properties` | 25 | 비즈니스 설정 — 이체 한도 (5 천만/1 억), 수수료 (500/800/1500/300), 출금 한도 (1 천만), 대출 범위 (1~20%, 480 개월), 스케줄 주기 (60 초) |
| `log4j.properties` | 8 | Log4j 1.x 설정 — 루트 INFO, com.novabank DEBUG, 콘솔 출력 |
| `logback.xml` | 17 | Logback 설정 — Log4j 와 동일한 패턴 (동시 존재 참고) |

### 웹 리소스 (`src/main/webapp`)

**WEB-INF/**
- `web.xml` (52 줄) — 서블릿 컨테이너 설정
- `spring/root-context.xml` (20 줄) — Spring 루트 컨텍스트 (빈 정의, 스캐닝)
- `spring/servlet-context.xml` (32 줄) — MVC 설정 (뷰 레졸버, 정적자원 매핑, 스케줄러 활성화)

**WEB-INF/views/ (JSP 7 개 + common 2 개)**
- `index.jsp` (69 줄) — 대시보드
- `accounts.jsp` (43 줄) — 계좌 목록
- `account.jsp` (74 줄) — 계좌 개설/상세
- `close.jsp` (36 줄) — 일마감 리포트
- `loan.jsp` (47 줄) — 대출 계산기
- `transfer.jsp` (50 줄) — 계좌 이체
- `error.jsp` (14 줄) — 오류 화면
- `common/header.jspf` (28 줄) — 공통 헤더
- `common/footer.jspf` (8 줄) — 공통 푸터

**resources/**
- `css/app.css` (66 줄) — 스타일 시트
- `js/app.js` (48 줄) — 애플리케이션 JavaScript
- `js/jquery-1.12.4.min.js` (95KB) — jQuery 1.12.4 (압축본)

## 테스트 구조

총 15 개 테스트 클래스가 4 개 패키지로 구성됩니다.

### 서비스 단위 테스트 (`service/`) — 5 개

| 클래스 | 주요 검증 항목 |
|---|---|
| `AccountServiceTest` | 계좌 개설/조회/상태변경/해지, 총잔액 합산, 이름 마스킹 |
| `TransactionServiceTest` | 입금/출금, 잔액 부족/한도 초과 예외, 거래 내역 최신순 정렬 |
| `TransferServiceTest` | 당행/타행 이체, 수수료 계산, 일일/회당 한도, 거래정지 계좌 이체 불가 |
| `LoanCalculatorServiceTest` | 원리금균등/원금균등, 이자 계산, 한도 예외 (금리 1~20%, 기간 1~480 개월) |
| `DailyCloseServiceTest` | 일종결 시 거래 집계, XML/JSON 포맷 생성 |

### 동작 테스트 (`tc/`) — 7 개

`MockMvc` 를 사용한 컨트롤러 단독 설정 테스트. 각 테스트마다 새로운 `InMemory` 저장소 인스턴스를 생성하여 상태 격리 보장.

| 클래스 | 줄 수 | TC 번호 | 주요 검증 항목 |
|---|---|---|---|
| `AccountQueryTcTest` | 290 | TC-01 | 계좌 목록 6 건 조회, 계좌 정보 JSON, 계좌별 거래 내역 |
| `AccountOpenStatusTcTest` | 353 | TC-02 | 보통예금 개설, 계좌 상태 변경 (정상/동결/해지) |
| `DepositWithdrawTcTest` | 348 | TC-03 | 입금 (콤마 형식 "500,000"), 출금, 잔액 부족, 한도 초과 |
| `TransferTcTest` | 504 | TC-04 | 수수료 미리보기 API, 이체 실행 (당행/타행), 거래번호 생성, 수수료 차감 |
| `LoanTcTest` | 380 | TC-05 | 원리금균등 1 천만원 4.5% 12 개월 — 월 상환액, 이자/원금 내역, 총이자, 12 개월 차 잔액 0 원 |
| `DailyCloseTcTest` | 387 | TC-06 | 일마감 리포트 조회 (초기/날짜지정), JSON/XML 응답 검증 |
| `ViewTcTest` | 645 | — | JSP 뷰 렌더링, 컨트롤러 응답 화면 검증 |

### 유틸리티 테스트 (`util/`) — 2 개

- `DateUtilTest` — 날짜 포맷팅, 영업시간 판정
- `MoneyUtilTest` — 금액 포맷팅, 콤마 처리

### 웹 테스트 (`web/`) — 1 개

- `AccountControllerTest` (95 줄) — 컨트롤러 단위 테스트

### 공통 테스트 패턴

1. `MockMvcBuilders.standaloneSetup(controller)` — 컨트롤러 단독 설정
2. `setControllerAdvice(new GlobalExceptionHandler())` — 전역 예외 처리기 연결
3. `InternalResourceViewResolver` — JSP 뷰 레졸버 (`/WEB-INF/views/` + `.jsp`)
4. `ReflectionTestUtils.setField()` — 서비스 클래스에 `InMemory` 저장소 주입
5. 각 테스트 메서드마다 새로운 저장소 인스턴스 생성 — 테스트 간 상태 격리

### 비교 규칙

- **JSON**: 필드 단위 비교, 키 순서·공백·들여쓰기 비교 제외, 숫자는 수치로 비교 (`3250000` = `3250000.0`)
- **XML**: 요소 이름·순서·텍스트 비교, 선언·들여쓰기 비교 제외
- **HTML**: HTTP 상태와 "포함 문구"만 비교
- **오류**: 입력 오류 → HTTP 400 + `error` 뷰, 그 외 예외 → 500
- **리다이렉트**: POST 성공 → 302, `Location` = `/accounts/{계좌번호}`, 플래시 메시지 포함

## 초기 표본 데이터

기동 시 `InMemoryAccountRepository` 가 다음 6 개 계좌를 생성합니다:

| 계좌번호 | 고객명 | 유형 | 잔액 | 상태 | 개설일 |
|---|---|---|---|---|---|
| 110-0917-000001 | 김민수 | 보통 (SAVINGS) | 3,250,000 | ACTIVE | 2011-03-14 |
| 110-0917-000002 | 이서연 | 당좌 (CHECKING) | 12,480,000 | ACTIVE | 2012-07-02 |
| 110-0917-000003 | 박지훈 | 정기 (FIXED) | 850,000 | ACTIVE | 2012-11-20 |
| 110-0917-000004 | 최유진 | 정기 (FIXED) | 30,000,000 | ACTIVE | 2013-01-10 |
| 110-0917-000005 | 정우성 | 보통 (SAVINGS) | 0 | FROZEN | 2013-05-15 |
| 110-0917-000006 | (주) 한빛상사 | 당좌 (CHECKING) | 58,900,000 | ACTIVE | 2013-09-01 |

- 초기 총 잔액 (해지 제외): 105,480,000 원
- 거래 내역: 0 건
- 새로 개설하는 계좌는 `110-0917-000007` 부터 순서대로 번호를 받음
- 지점 코드: `0917`
- 거래번호 접두어: `NB`

## 버전 정보

- 현재 버전: `2.3.1` (2013 년 11 월 구축본)
- `artifactId`: `novabank-legacy`
- `groupId`: `com.novabank`
- 빌드 타입: `war`
- 최종 파일명: `novabank.war`

## 업무 규칙 요약

- 계좌 상태: 정상 (A) · 거래정지 (F) · 해지 (C). 거래정지·해지 계좌는 입출금·이체 불가. 해지는 잔액 0 원일 때만, 되돌릴 수 없음.
- 정기예금 개설은 최초 입금 1,000,000 원 이상. 금액은 원 단위 정수.
- 출금 1 회 한도 10,000,000 원. 이체 1 회 한도 50,000,000 원 · 1 일 한도 100,000,000 원 (이체출금 누계).
- 이체 수수료: 당행 0 원. 타행은 10 만원 미만 500 원 · 100 만원 미만 800 원 · 그 이상 1,500 원, 영업시간 (평일 09~18 시) 외 +300 원.
  수수료는 별도 거래 (FEE) 로 기록.
- 대출 계산: 연이율 1~20%, 1~480 개월. 원리금균등은 마지막 회차에서 잔액을 0 으로 맞춘다.
- 거래번호 = `NB` + 영숫자 6 + 체크 2.
