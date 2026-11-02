package com.novabank.ops.tc;

import com.novabank.ops.service.AccountService;
import com.novabank.ops.service.TransactionService;
import com.novabank.ops.repository.InMemoryAccountRepository;
import com.novabank.ops.repository.InMemoryTransactionRepository;
import com.novabank.ops.util.TxIdGenerator;
import com.novabank.ops.web.AccountController;
import com.novabank.ops.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * NovaBank 동작테스트-TC §4 TC-03 입금·출금 전역 검증.
 *
 * <p>TC-03-01 ~ TC-03-03: 같은 저장소 인스턴스 위에서 순차 실행
 * <p>TC-03-04 ~ TC-03-11: 각각 새로운 저장소 인스턴스 사용 (독립 상태)
 */
class DepositWithdrawTcTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
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
    // TC-03-01 입금 500,000원(콤마 입력)
    // =========================================================================
    @Test
    @DisplayName("TC-03-01 입금 500,000원(콤마 입력)")
    void tc0301_입금_500000원_콤마입력() throws Exception {
        mockMvc.perform(post("/accounts/110-0917-000001/deposit")
                        .param("amount", "500,000")
                        .param("memo", "TC 입금"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/110-0917-000001"))
                .andExpect(flash().attributeExists("message"))
                .andExpect(flash().attribute("message", stringContainsInOrder("입금 완료 (거래번호 NB")));

        mockMvc.perform(get("/api/accounts/110-0917-000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(3750000));

        mockMvc.perform(get("/api/accounts/110-0917-000001/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[0].amount").value(500000))
                .andExpect(jsonPath("$[0].balanceAfter").value(3750000))
                .andExpect(jsonPath("$[0].memo").value("TC 입금"))
                .andExpect(jsonPath("$[0].seq").value(1L))
                .andExpect(jsonPath("$[0].txId").isNotEmpty())
                .andExpect(jsonPath("$[0].createdAt", greaterThan(0L)));
    }

    // =========================================================================
    // TC-03-02 출금 200,000원
    // 전제: TC-03-01 입금 500,000원 실행 후 (같은 저장소 인스턴스 공유)
    // =========================================================================
    @Test
    @DisplayName("TC-03-02 출금 200,000원")
    void tc0302_출금_200000원() throws Exception {
        // 전제: TC-03-01 입금 500,000원
        mockMvc.perform(post("/accounts/110-0917-000001/deposit")
                        .param("amount", "500,000")
                        .param("memo", "TC 입금"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/110-0917-000001"))
                .andExpect(flash().attributeExists("message"))
                .andExpect(flash().attribute("message", stringContainsInOrder("입금 완료 (거래번호 NB")));

        mockMvc.perform(get("/api/accounts/110-0917-000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(3750000));

        // TC-03-02 본 테스트: 출금 200,000원
        mockMvc.perform(post("/accounts/110-0917-000001/withdraw")
                        .param("amount", "200000")
                        .param("memo", "TC 출금"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/110-0917-000001"))
                .andExpect(flash().attributeExists("message"))
                .andExpect(flash().attribute("message", stringContainsInOrder("출금 완료 (거래번호 NB")));

        mockMvc.perform(get("/api/accounts/110-0917-000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(3550000));

        mockMvc.perform(get("/api/accounts/110-0917-000001/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("WITHDRAW"))
                .andExpect(jsonPath("$[0].amount").value(200000))
                .andExpect(jsonPath("$[0].balanceAfter").value(3550000))
                .andExpect(jsonPath("$[0].memo").value("TC 출금"))
                .andExpect(jsonPath("$[0].seq").value(2L));
    }

    // =========================================================================
    // TC-03-03 소수 금액은 원 단위 절사
    // 전제: TC-03-01 입금 → TC-03-02 출금 실행 후 (같은 저장소 인스턴스 공유)
    // =========================================================================
    @Test
    @DisplayName("TC-03-03 소수 금액은 원 단위 절사")
    void tc0303_소수금액_원단위절사() throws Exception {
        // 전제: TC-03-01 입금 500,000원
        mockMvc.perform(post("/accounts/110-0917-000001/deposit")
                        .param("amount", "500,000")
                        .param("memo", "TC 입금"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/110-0917-000001"));

        // 전제: TC-03-02 출금 200,000원
        mockMvc.perform(post("/accounts/110-0917-000001/withdraw")
                        .param("amount", "200000")
                        .param("memo", "TC 출금"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/110-0917-000001"));

        // TC-03-03 본 테스트: 소수 입금 100.5
        mockMvc.perform(post("/accounts/110-0917-000001/deposit")
                        .param("amount", "100.5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/110-0917-000001"));

        mockMvc.perform(get("/api/accounts/110-0917-000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(3550100));

        mockMvc.perform(get("/api/accounts/110-0917-000001/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[0].amount").value(100))
                .andExpect(jsonPath("$[0].balanceAfter").value(3550100))
                .andExpect(jsonPath("$[0].memo").value(""))
                .andExpect(jsonPath("$[0].seq").value(3L));
    }

    // =========================================================================
    // TC-03-04 잔액 초과 출금 오류
    // =========================================================================
    @Test
    @DisplayName("TC-03-04 잔액 초과 출금 오류")
    void tc0304_잔액초과출금오류() throws Exception {
        mockMvc = buildFreshMvc();

        mockMvc.perform(post("/accounts/110-0917-000003/withdraw")
                        .param("amount", "900000"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "잔액이 부족합니다. 잔액 850000원, 요청 900000원"));

        mockMvc.perform(get("/api/accounts/110-0917-000003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(850000));

        mockMvc.perform(get("/api/accounts/110-0917-000003/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // =========================================================================
    // TC-03-05 1회 출금 한도 초과 오류
    // =========================================================================
    @Test
    @DisplayName("TC-03-05 1회 출금 한도 초과 오류")
    void tc0305_1회출금한도초과오류() throws Exception {
        mockMvc = buildFreshMvc();

        mockMvc.perform(post("/accounts/110-0917-000002/withdraw")
                        .param("amount", "10000001"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "1회 출금 한도를 초과했습니다. 한도 10000000원"));
    }

    // =========================================================================
    // TC-03-06 1회 출금 한도 경계 10,000,000원
    // =========================================================================
    @Test
    @DisplayName("TC-03-06 1회 출금 한도 경계 10,000,000원")
    void tc0306_1회출금한도경계() throws Exception {
        mockMvc = buildFreshMvc();

        mockMvc.perform(post("/accounts/110-0917-000002/withdraw")
                        .param("amount", "10,000,000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/110-0917-000002"))
                .andExpect(flash().attributeExists("message"))
                .andExpect(flash().attribute("message", stringContainsInOrder("출금 완료 (거래번호 NB")));

        mockMvc.perform(get("/api/accounts/110-0917-000002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(2480000));

        mockMvc.perform(get("/api/accounts/110-0917-000002/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("WITHDRAW"))
                .andExpect(jsonPath("$[0].amount").value(10000000))
                .andExpect(jsonPath("$[0].balanceAfter").value(2480000));
    }

    // =========================================================================
    // TC-03-07 거래정지 계좌 입금 오류
    // =========================================================================
    @Test
    @DisplayName("TC-03-07 거래정지 계좌 입금 오류")
    void tc0307_거래정지계좌입금오류() throws Exception {
        mockMvc = buildFreshMvc();

        mockMvc.perform(post("/accounts/110-0917-000005/deposit")
                        .param("amount", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "거래할 수 없는 계좌 상태입니다(거래정지): 110-0917-000005"));
    }

    // =========================================================================
    // TC-03-08 금액 0원 오류
    // =========================================================================
    @Test
    @DisplayName("TC-03-08 금액 0원 오류")
    void tc0308_금액0원오류() throws Exception {
        mockMvc = buildFreshMvc();

        mockMvc.perform(post("/accounts/110-0917-000001/deposit")
                        .param("amount", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "금액은 0보다 커야 합니다."));
    }

    // =========================================================================
    // TC-03-09 금액 형식 오류
    // =========================================================================
    @Test
    @DisplayName("TC-03-09 금액 형식 오류")
    void tc0309_금액형식오류() throws Exception {
        mockMvc = buildFreshMvc();

        mockMvc.perform(post("/accounts/110-0917-000001/deposit")
                        .param("amount", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "금액 형식이 올바르지 않습니다: abc"));
    }

    // =========================================================================
    // TC-03-10 없는 계좌 입금 오류
    // =========================================================================
    @Test
    @DisplayName("TC-03-10 없는 계좌 입금 오류")
    void tc0310_없는계좌입금오류() throws Exception {
        mockMvc = buildFreshMvc();

        mockMvc.perform(post("/accounts/110-0917-999999/deposit")
                        .param("amount", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "존재하지 않는 계좌입니다: 110-0917-999999"));
    }

    // =========================================================================
    // TC-03-11 빈 금액은 0원으로 보아 오류
    // =========================================================================
    @Test
    @DisplayName("TC-03-11 빈 금액은 0원으로 보아 오류")
    void tc0311_빈금액_0원_오류() throws Exception {
        mockMvc = buildFreshMvc();

        mockMvc.perform(post("/accounts/110-0917-000001/withdraw")
                        .param("amount", ""))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "금액은 0보다 커야 합니다."));
    }

    // =========================================================================
    // Helper: 개별 테스트마다 새로운 저장소 인스턴스로 MockMvc 재구성
    // =========================================================================
    private MockMvc buildFreshMvc() {
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

        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setViewResolvers(viewResolver)
                .build();
    }
}
