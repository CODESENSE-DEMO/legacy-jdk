package com.novabank.ops.tc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.novabank.ops.repository.InMemoryAccountRepository;
import com.novabank.ops.repository.InMemoryTransactionRepository;
import com.novabank.ops.service.AccountService;
import com.novabank.ops.service.TransactionService;
import com.novabank.ops.util.TxIdGenerator;
import com.novabank.ops.web.AccountController;
import com.novabank.ops.web.GlobalExceptionHandler;

/**
 * NovaBank 동작테스트-TC §0 계좌 조회 API 장 전 검증.
 *
 * <p>기존 AccountControllerTest과 동일한 설정 방식:
 * <ul>
 *   <li>MockMvcBuilders.standaloneSetup(controller)</li>
 *   <li>setControllerAdvice(new GlobalExceptionHandler())</li>
 *   <li>InternalResourceViewResolver: prefix /WEB-INF/views/, suffix .jsp</li>
 *   <li>AccountService / TransactionService에 ReflectionTestUtils.setField로
 *       실제 InMemoryAccountRepository / InMemoryTransactionRepository 연결</li>
 *   <li>컨트롤러의 @RequestMapping은 /accounts, /api/accounts,
 *       /api/accounts/{accountNo}/transactions 등</li>
 *   <li>MockMvc 실행 시 컨텍스트 경로(/novabank) 없이 경로만 사용</li>
 * </ul>
 */
class AccountQueryTcTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // 각 테스트마다 새로운 저장소 인스턴스 생성 — 독립적인 초기 상태 보장
        AccountService accountService = new AccountService();
        ReflectionTestUtils.setField(accountService, "accountRepository", new InMemoryAccountRepository());

        TransactionService transactionService = new TransactionService();
        ReflectionTestUtils.setField(transactionService, "transactionRepository", new InMemoryTransactionRepository());
        ReflectionTestUtils.setField(transactionService, "accountService", accountService);
        ReflectionTestUtils.setField(transactionService, "txIdGenerator", new TxIdGenerator());

        AccountController controller = new AccountController();
        ReflectionTestUtils.setField(controller, "accountService", accountService);
        ReflectionTestUtils.setField(controller, "transactionService", transactionService);

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setViewResolvers(viewResolver)
                .build();
    }

    // =========================================================================
    // TC-01-01 계좌 목록 조회 6건 — GET /api/accounts
    // =========================================================================
    @Test
    @DisplayName("TC-01-01 계좌 목록 조회 6건")
    void tc0101_계좌목록_조회_6건() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(6))

                // --- 계좌 1: 110-0917-000001 김민수 ---
                .andExpect(jsonPath("$[0].accountNo").value("110-0917-000001"))
                .andExpect(jsonPath("$[0].customerName").value("김민수"))
                .andExpect(jsonPath("$[0].type").value("SAVINGS"))
                .andExpect(jsonPath("$[0].balance").value(3250000))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].maskedName").value("김*수"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].closedAt").value(nullValue()))
                .andExpect(jsonPath("$[0].openedAt").value(notNullValue()))

                // --- 계좌 2: 110-0917-000002 이서연 ---
                .andExpect(jsonPath("$[1].accountNo").value("110-0917-000002"))
                .andExpect(jsonPath("$[1].customerName").value("이서연"))
                .andExpect(jsonPath("$[1].type").value("CHECKING"))
                .andExpect(jsonPath("$[1].balance").value(12480000))
                .andExpect(jsonPath("$[1].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].maskedName").value("이*연"))
                .andExpect(jsonPath("$[1].active").value(true))
                .andExpect(jsonPath("$[1].closedAt").value(nullValue()))
                .andExpect(jsonPath("$[1].openedAt").value(notNullValue()))

                // --- 계좌 3: 110-0917-000003 박지훈 ---
                .andExpect(jsonPath("$[2].accountNo").value("110-0917-000003"))
                .andExpect(jsonPath("$[2].customerName").value("박지훈"))
                .andExpect(jsonPath("$[2].type").value("SAVINGS"))
                .andExpect(jsonPath("$[2].balance").value(850000))
                .andExpect(jsonPath("$[2].status").value("ACTIVE"))
                .andExpect(jsonPath("$[2].maskedName").value("박*훈"))
                .andExpect(jsonPath("$[2].active").value(true))
                .andExpect(jsonPath("$[2].closedAt").value(nullValue()))
                .andExpect(jsonPath("$[2].openedAt").value(notNullValue()))

                // --- 계좌 4: 110-0917-000004 최유진 ---
                .andExpect(jsonPath("$[3].accountNo").value("110-0917-000004"))
                .andExpect(jsonPath("$[3].customerName").value("최유진"))
                .andExpect(jsonPath("$[3].type").value("FIXED"))
                .andExpect(jsonPath("$[3].balance").value(30000000))
                .andExpect(jsonPath("$[3].status").value("ACTIVE"))
                .andExpect(jsonPath("$[3].maskedName").value("최*진"))
                .andExpect(jsonPath("$[3].active").value(true))
                .andExpect(jsonPath("$[3].closedAt").value(nullValue()))
                .andExpect(jsonPath("$[3].openedAt").value(notNullValue()))

                // --- 계좌 5: 110-0917-000005 정우성 ---
                .andExpect(jsonPath("$[4].accountNo").value("110-0917-000005"))
                .andExpect(jsonPath("$[4].customerName").value("정우성"))
                .andExpect(jsonPath("$[4].type").value("SAVINGS"))
                .andExpect(jsonPath("$[4].balance").value(0))
                .andExpect(jsonPath("$[4].status").value("FROZEN"))
                .andExpect(jsonPath("$[4].maskedName").value("정*성"))
                .andExpect(jsonPath("$[4].active").value(false))
                .andExpect(jsonPath("$[4].closedAt").value(nullValue()))
                .andExpect(jsonPath("$[4].openedAt").value(notNullValue()))

                // --- 계좌 6: 110-0917-000006 (주)한빛상사 ---
                .andExpect(jsonPath("$[5].accountNo").value("110-0917-000006"))
                .andExpect(jsonPath("$[5].customerName").value("(주)한빛상사"))
                .andExpect(jsonPath("$[5].type").value("CHECKING"))
                .andExpect(jsonPath("$[5].balance").value(58900000))
                .andExpect(jsonPath("$[5].status").value("ACTIVE"))
                .andExpect(jsonPath("$[5].maskedName").value("(*****사"))
                .andExpect(jsonPath("$[5].active").value(true))
                .andExpect(jsonPath("$[5].closedAt").value(nullValue()))
                .andExpect(jsonPath("$[5].openedAt").value(notNullValue()));
    }

    // =========================================================================
    // TC-01-02 계좌 1건 조회 — GET /api/accounts/110-0917-000001
    // =========================================================================
    @Test
    @DisplayName("TC-01-02 계좌 1건 조회")
    void tc0102_계좌_1건_조회() throws Exception {
        mockMvc.perform(get("/api/accounts/110-0917-000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNo").value("110-0917-000001"))
                .andExpect(jsonPath("$.customerName").value("김민수"))
                .andExpect(jsonPath("$.type").value("SAVINGS"))
                .andExpect(jsonPath("$.balance").value(3250000))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.closedAt").value(nullValue()))
                .andExpect(jsonPath("$.maskedName").value("김*수"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.openedAt").value(notNullValue()));
    }

    // =========================================================================
    // TC-01-03 거래정지 계좌 조회 — GET /api/accounts/110-0917-000005
    // =========================================================================
    @Test
    @DisplayName("TC-01-03 거래정지 계좌 조회")
    void tc0103_거래정지_계좌_조회() throws Exception {
        mockMvc.perform(get("/api/accounts/110-0917-000005"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.closedAt").value(nullValue()))
                .andExpect(jsonPath("$.maskedName").value("정*성"));
    }

    // =========================================================================
    // TC-01-04 없는 계좌 조회 오류 — GET /api/accounts/110-0917-999999
    // =========================================================================
    @Test
    @DisplayName("TC-01-04 없는 계좌 조회 오류")
    void tc0104_없는_계좌_조회_오류() throws Exception {
        mockMvc.perform(get("/api/accounts/110-0917-999999"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "존재하지 않는 계좌입니다: 110-0917-999999"))
                .andExpect(model().attribute("uri", "/api/accounts/110-0917-999999"));
    }

    // =========================================================================
    // TC-01-05 거래 내역 초기 0건 — GET /api/accounts/110-0917-000001/transactions
    // =========================================================================
    @Test
    @DisplayName("TC-01-05 거래 내역 초기 0건")
    void tc0105_거래내역_초기_0건() throws Exception {
        mockMvc.perform(get("/api/accounts/110-0917-000001/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // =========================================================================
    // TC-01-06 없는 계좌 거래 내역 오류 — GET /api/accounts/110-0917-999999/transactions
    // =========================================================================
    @Test
    @DisplayName("TC-01-06 없는 계좌 거래 내역 오류")
    void tc0106_없는_계좌_거래내역_오류() throws Exception {
        mockMvc.perform(get("/api/accounts/110-0917-999999/transactions"))
                .andExpect(status().isBadRequest())
                .andExpect(model().attribute("detail", "존재하지 않는 계좌입니다: 110-0917-999999"));
    }

    // =========================================================================
    // TC-01-07 거래 내역은 최신순 — GET /api/accounts/110-0917-000001/transactions
    //
    // 전제: 먼저 입금 500,000원(memo=`TC 입금`) → 출금 200,000원(memo=`TC 출금`)
    //       → 입금 100.50원(memo=비어있음, 원 단위 절사=100)을 순서대로 실행
    // =========================================================================
    @Test
    @DisplayName("TC-01-07 거래 내역은 최신순")
    void tc0107_거래내역_최신순() throws Exception {
        // --- 전제: 3건의 거래 실행 ---
        // 1. 입금 500,000원 (memo="TC 입금")
        mockMvc.perform(post("/accounts/110-0917-000001/deposit")
                        .param("amount", "500000")
                        .param("memo", "TC 입금"))
                .andExpect(status().is3xxRedirection());

        // 2. 출금 200,000원 (memo="TC 출금")
        mockMvc.perform(post("/accounts/110-0917-000001/withdraw")
                        .param("amount", "200000")
                        .param("memo", "TC 출금"))
                .andExpect(status().is3xxRedirection());

        // 3. 입금 100.50원 (memo=비어있음, 원 단위 절사=100)
        mockMvc.perform(post("/accounts/110-0917-000001/deposit")
                        .param("amount", "100.50")
                        .param("memo", ""))
                .andExpect(status().is3xxRedirection());

        // --- 검증: 거래 내역 조회 ---
        mockMvc.perform(get("/api/accounts/110-0917-000001/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))

                // 1번: 최신 거래 (seq=3, 입금 100원)
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[0].amount").value(100))
                .andExpect(jsonPath("$[0].balanceAfter").value(3550100))
                .andExpect(jsonPath("$[0].memo").value(""))
                .andExpect(jsonPath("$[0].counterparty").value(nullValue()))
                .andExpect(jsonPath("$[0].seq").value(3L))
                // 비교 제외: txId, createdAt
                .andExpect(jsonPath("$[0].txId").isNotEmpty())
                .andExpect(jsonPath("$[0].createdAt").value(greaterThan(0L)))

                // 2번: 중간 거래 (seq=2, 출금 200,000원)
                .andExpect(jsonPath("$[1].type").value("WITHDRAW"))
                .andExpect(jsonPath("$[1].amount").value(200000))
                .andExpect(jsonPath("$[1].balanceAfter").value(3550000))
                .andExpect(jsonPath("$[1].memo").value("TC 출금"))
                .andExpect(jsonPath("$[1].counterparty").value(nullValue()))
                .andExpect(jsonPath("$[1].seq").value(2L))
                .andExpect(jsonPath("$[1].txId").isNotEmpty())
                .andExpect(jsonPath("$[1].createdAt").value(greaterThan(0L)))

                // 3번: 최초 거래 (seq=1, 입금 500,000원)
                .andExpect(jsonPath("$[2].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[2].amount").value(500000))
                .andExpect(jsonPath("$[2].balanceAfter").value(3750000))
                .andExpect(jsonPath("$[2].memo").value("TC 입금"))
                .andExpect(jsonPath("$[2].counterparty").value(nullValue()))
                .andExpect(jsonPath("$[2].seq").value(1L))
                .andExpect(jsonPath("$[2].txId").isNotEmpty())
                .andExpect(jsonPath("$[2].createdAt").value(greaterThan(0L)));
    }
}
