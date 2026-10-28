package com.novabank.ops.tc;

import com.novabank.ops.web.TransferController;
import com.novabank.ops.web.AccountController;
import com.novabank.ops.web.GlobalExceptionHandler;
import com.novabank.ops.service.AccountService;
import com.novabank.ops.service.TransferService;
import com.novabank.ops.service.TransactionService;
import com.novabank.ops.repository.InMemoryAccountRepository;
import com.novabank.ops.repository.InMemoryTransactionRepository;
import com.novabank.ops.util.TxIdGenerator;
import com.novabank.ops.util.DateUtil;
import com.novabank.ops.domain.Account;
import com.novabank.ops.domain.TransferResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Date;
import java.util.Calendar;
import java.math.BigDecimal;

/**
 * NovaBank 동작테스트-TC §4 TC-04 이체 전역 검증.
 *
 * <p>TC-04-01 ~ TC-04-09: 수수료 미리보기 API (GET /api/transfer/fee)
 * <p>TC-04-10 ~ TC-04-19: 이체 실행 (POST /transfer)
 * <p>TC-04-20 ~ TC-04-21: 서비스 단위 테스트 (직접 메서드 호출)
 */
class TransferTcTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AccountService accountService = new AccountService();
        ReflectionTestUtils.setField(accountService, "accountRepository", new InMemoryAccountRepository());

        TransactionService transactionService = new TransactionService();
        ReflectionTestUtils.setField(transactionService, "transactionRepository", new InMemoryTransactionRepository());
        ReflectionTestUtils.setField(transactionService, "accountService", accountService);
        ReflectionTestUtils.setField(transactionService, "txIdGenerator", new TxIdGenerator());

        TransferService transferService = new TransferService();
        ReflectionTestUtils.setField(transferService, "transactionService", transactionService);
        ReflectionTestUtils.setField(transferService, "accountService", accountService);

        TransferController controller = new TransferController();
        ReflectionTestUtils.setField(controller, "transferService", transferService);
        ReflectionTestUtils.setField(controller, "accountService", accountService);

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setViewResolvers(viewResolver)
                .build();
    }

    // =========================================================================
    // TC-04-01 ~ TC-04-09: 수수료 미리보기 API (GET /api/transfer/fee)
    // =========================================================================

    @Test
    @DisplayName("TC-04-01 수수료 미리보기 - 당행 1,000,000원")
    void tc0401_수수료미리보기_당행_1000000원() throws Exception {
        mockMvc.perform(get("/api/transfer/fee")
                        .param("toBank", "NOVA")
                        .param("amount", "1000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fee").value(0))
                .andExpect(jsonPath("$.totalDebit").value(1000000))
                .andExpect(jsonPath("$.toBank").value("NOVA"))
                .andExpect(jsonPath("$.amount").value(1000000));
    }

    @Test
    @DisplayName("TC-04-02 수수료 미리보기 - empty toBank")
    void tc0402_수수료미리보기_emptyToBank() throws Exception {
        mockMvc.perform(get("/api/transfer/fee")
                        .param("toBank", "")
                        .param("amount", "1000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toBank").value("NOVA"))
                .andExpect(jsonPath("$.fee").value(0))
                .andExpect(jsonPath("$.totalDebit").value(1000000));
    }

    @Test
    @DisplayName("TC-04-03 수수료 미리보기 - KDB 50,000원 (영업시간 내)")
    void tc0403_수수료미리보기_KDB_50000원_영업시간내() throws Exception {
        mockMvc.perform(get("/api/transfer/fee")
                        .param("toBank", "KDB")
                        .param("amount", "50000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fee").value(500))
                .andExpect(jsonPath("$.totalDebit").value(50500));
    }

    @Test
    @DisplayName("TC-04-04 수수료 미리보기 - KDB 99,999원 (영업시간 내)")
    void tc0404_수수료미리보기_KDB_99999원_영업시간내() throws Exception {
        mockMvc.perform(get("/api/transfer/fee")
                        .param("toBank", "KDB")
                        .param("amount", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fee").value(500))
                .andExpect(jsonPath("$.totalDebit").value(100499));
    }

    @Test
    @DisplayName("TC-04-05 수수료 미리보기 - KDB 100,000원 (영업시간 내)")
    void tc0405_수수료미리보기_KDB_100000원_영업시간내() throws Exception {
        mockMvc.perform(get("/api/transfer/fee")
                        .param("toBank", "KDB")
                        .param("amount", "100000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fee").value(800))
                .andExpect(jsonPath("$.totalDebit").value(100800));
    }

    @Test
    @DisplayName("TC-04-06 수수료 미리보기 - KDB 999,999원 (영업시간 내)")
    void tc0406_수수료미리보기_KDB_999999원_영업시간내() throws Exception {
        mockMvc.perform(get("/api/transfer/fee")
                        .param("toBank", "KDB")
                        .param("amount", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fee").value(800))
                .andExpect(jsonPath("$.totalDebit").value(1000799));
    }

    @Test
    @DisplayName("TC-04-07 수수료 미리보기 - KDB 1,000,000원 (영업시간 내)")
    void tc0407_수수료미리보기_KDB_1000000원_영업시간내() throws Exception {
        mockMvc.perform(get("/api/transfer/fee")
                        .param("toBank", "KDB")
                        .param("amount", "1,000,000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fee").value(1500))
                .andExpect(jsonPath("$.totalDebit").value(1001500))
                .andExpect(jsonPath("$.amount").value(1000000));
    }

    @Test
    @DisplayName("TC-04-08 수수료 미리보기 - ibk 5,000,000원 (영업시간 내)")
    void tc0408_수수료미리보기_ibk_5000000원_영업시간내() throws Exception {
        mockMvc.perform(get("/api/transfer/fee")
                        .param("toBank", "ibk")
                        .param("amount", "5000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toBank").value("IBK"))
                .andExpect(jsonPath("$.fee").value(1500))
                .andExpect(jsonPath("$.totalDebit").value(5001500));
    }

    @Test
    @DisplayName("TC-04-09 수수료 미리보기 - nova 50,000원 (대소문자 무관)")
    void tc0409_수수료미리보기_nova_50000원() throws Exception {
        mockMvc.perform(get("/api/transfer/fee")
                        .param("toBank", "nova")
                        .param("amount", "50000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toBank").value("NOVA"))
                .andExpect(jsonPath("$.fee").value(0))
                .andExpect(jsonPath("$.totalDebit").value(50000));
    }

    // =========================================================================
    // TC-04-10 ~ TC-04-19: 이체 실행 (POST /transfer)
    // =========================================================================

    @Test
    @DisplayName("TC-04-10 당행 이체 1,000,000원")
    void tc0410_당행이체_1000000원() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("fromAccountNo", "110-0917-000002")
                        .param("toBank", "NOVA")
                        .param("toAccountNo", "110-0917-000003")
                        .param("amount", "1,000,000")
                        .param("memo", "당행 이체"))
                .andExpect(status().isOk())
                .andExpect(view().name("transfer"))
                .andExpect(model().attributeExists("result"))
                .andExpect(model().attribute("message", stringContainsInOrder("이체가 완료됐습니다. 거래번호 NB")))
                .andExpect(model().attributeExists("result")).andDo(result -> {
                    TransferResult r = (TransferResult) result.getModelAndView().getModel().get("result");
                    assertThat(r.getToBank(), is("NOVA"));
                })
                .andExpect(model().attributeExists("result")).andDo(result -> {
                    TransferResult r = (TransferResult) result.getModelAndView().getModel().get("result");
                    assertThat(r.getAmount(), is(new BigDecimal("1000000")));
                    assertThat(r.getFee(), is(BigDecimal.ZERO));
                    assertThat(r.getTotalDebit(), is(new BigDecimal("1000000")));
                    assertThat(r.getBalanceAfter(), is(new BigDecimal("11480000")));
                });
    }

    @Test
    @DisplayName("TC-04-11 타행 이체 250,000원 수수료 800원 (영업시간 내)")
    void tc0411_타행이체_250000원_수수료800원() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("fromAccountNo", "110-0917-000006")
                        .param("toBank", "KDB")
                        .param("toAccountNo", "123-456-789")
                        .param("amount", "250000")
                        .param("memo", "타행 이체"))
                .andExpect(status().isOk())
                .andExpect(view().name("transfer"))
                .andExpect(model().attributeExists("result"))
                .andExpect(model().attributeExists("result")).andDo(result -> {
                    TransferResult r = (TransferResult) result.getModelAndView().getModel().get("result");
                    assertThat(r.getToBank(), is("KDB"));
                })
                .andExpect(model().attributeExists("result")).andDo(result -> {
                    TransferResult r = (TransferResult) result.getModelAndView().getModel().get("result");
                    assertThat(r.getAmount(), is(new BigDecimal("250000")));
                    assertThat(r.getFee(), is(new BigDecimal("800")));
                    assertThat(r.getTotalDebit(), is(new BigDecimal("250800")));
                    assertThat(r.getBalanceAfter(), is(new BigDecimal("58649200")));
                });
    }

    @Test
    @DisplayName("TC-04-12 출금/입금 계좌 같으면 오류")
    void tc0412_출금입금계좌같으면오류() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("fromAccountNo", "110-0917-000002")
                        .param("toBank", "NOVA")
                        .param("toAccountNo", "110-0917-000002")
                        .param("amount", "1000")
                        .param("memo", "동일계좌"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "출금 계좌와 입금 계좌가 같습니다."));
    }

    @Test
    @DisplayName("TC-04-13 거래정지 계좌로 이체 오류")
    void tc0413_거래정지계좌로이체오류() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("fromAccountNo", "110-0917-000002")
                        .param("toBank", "NOVA")
                        .param("toAccountNo", "110-0917-000005")
                        .param("amount", "1000")
                        .param("memo", "거래정지계좌"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "거래할 수 없는 계좌 상태입니다(거래정지): 110-0917-000005"));
    }

    @Test
    @DisplayName("TC-04-14 1회 이체 한도 초과")
    void tc0414_1회이체한도초과() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("fromAccountNo", "110-0917-000006")
                        .param("toBank", "NOVA")
                        .param("toAccountNo", "110-0917-000001")
                        .param("amount", "50000001")
                        .param("memo", "한도초과"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "1회 이체 한도를 초과했습니다. 한도 50000000원"));
    }

    @Test
    @DisplayName("TC-04-15 수수료 포함 잔액 부족 (영업시간 내)")
    void tc0415_수수료포함잔액부족() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("fromAccountNo", "110-0917-000003")
                        .param("toBank", "KDB")
                        .param("toAccountNo", "1")
                        .param("amount", "5000000")
                        .param("memo", "잔액부족"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "잔액이 부족합니다(수수료 포함 5001500원). 잔액 850000원"));
    }

    @Test
    @DisplayName("TC-04-16 입금 계좌번호 공백 오류")
    void tc0416_입금계좌번호공백오류() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("fromAccountNo", "110-0917-000002")
                        .param("toBank", "KDB")
                        .param("toAccountNo", "")
                        .param("amount", "1000")
                        .param("memo", "계좌공백"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "입금 계좌번호는 필수입니다."));
    }

    @Test
    @DisplayName("TC-04-17 출금 계좌 거래정지 오류")
    void tc0417_출금계좌거래정지오류() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("fromAccountNo", "110-0917-000005")
                        .param("toBank", "NOVA")
                        .param("toAccountNo", "110-0917-000001")
                        .param("amount", "1000")
                        .param("memo", "출금거래정지"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "거래할 수 없는 계좌 상태입니다(거래정지): 110-0917-000005"));
    }

    @Test
    @DisplayName("TC-04-18 타행 입금 계좌 존재 확인 없음 (영업시간 내)")
    void tc0418_타행입금계좌존재확인없음() throws Exception {
        mockMvc.perform(post("/transfer")
                        .param("fromAccountNo", "110-0917-000001")
                        .param("toBank", "ibk")
                        .param("toAccountNo", "abc-1")
                        .param("amount", "50000")
                        .param("memo", "타행계좌미존재"))
                .andExpect(status().isOk())
                .andExpect(view().name("transfer"))
                .andExpect(model().attributeExists("result"))
                .andExpect(model().attributeExists("result")).andDo(result -> {
                    TransferResult r = (TransferResult) result.getModelAndView().getModel().get("result");
                    assertThat(r.getToBank(), is("IBK"));
                })
                .andExpect(model().attributeExists("result")).andDo(result -> {
                    TransferResult r = (TransferResult) result.getModelAndView().getModel().get("result");
                    assertThat(r.getAmount(), is(new BigDecimal("50000")));
                    assertThat(r.getFee(), is(new BigDecimal("500")));
                    assertThat(r.getTotalDebit(), is(new BigDecimal("50500")));
                    assertThat(r.getBalanceAfter(), is(new BigDecimal("3199500")));
                });
    }

    @Test
    @DisplayName("TC-04-19 1일 이체 한도 100,000,000원 초과 오류")
    void tc0419_1일이체한도초과() throws Exception {
        // 위키 전제(순서): ① POST 입금 100,000,000 → ② POST 이체 50,000,000 → ③ 같은 이체 한 번 더 → ④ 1,000원 이체는 400
        AccountService accountService = new AccountService();
        ReflectionTestUtils.setField(accountService, "accountRepository", new InMemoryAccountRepository());
        TransactionService transactionService = new TransactionService();
        ReflectionTestUtils.setField(transactionService, "transactionRepository", new InMemoryTransactionRepository());
        ReflectionTestUtils.setField(transactionService, "accountService", accountService);
        ReflectionTestUtils.setField(transactionService, "txIdGenerator", new TxIdGenerator());
        TransferService transferService = new TransferService();
        ReflectionTestUtils.setField(transferService, "transactionService", transactionService);
        ReflectionTestUtils.setField(transferService, "accountService", accountService);
        TransferController transferController = new TransferController();
        ReflectionTestUtils.setField(transferController, "transferService", transferService);
        ReflectionTestUtils.setField(transferController, "accountService", accountService);
        AccountController accountController = new AccountController();
        ReflectionTestUtils.setField(accountController, "accountService", accountService);
        ReflectionTestUtils.setField(accountController, "transactionService", transactionService);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(accountController, transferController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setViewResolvers(viewResolver)
                .build();

        // ① 입금 100,000,000 → 잔액 158,900,000
        mvc.perform(post("/accounts/110-0917-000006/deposit").param("amount", "100000000"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/accounts/110-0917-000006"))
                .andExpect(jsonPath("$.balance").value(158900000));

        // ② ③ 당행 이체 50,000,000 두 번 → 200, 누계 100,000,000, 잔액 58,900,000
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/transfer")
                            .param("fromAccountNo", "110-0917-000006")
                            .param("toBank", "NOVA")
                            .param("toAccountNo", "110-0917-000001")
                            .param("amount", "50000000"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("transfer"));
        }
        mvc.perform(get("/api/accounts/110-0917-000006"))
                .andExpect(jsonPath("$.balance").value(58900000));

        // ④ 1,000원 이체 → 400, 1일 한도 초과
        mvc.perform(post("/transfer")
                        .param("fromAccountNo", "110-0917-000006")
                        .param("toBank", "NOVA")
                        .param("toAccountNo", "110-0917-000001")
                        .param("amount", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("detail", "1일 이체 한도를 초과했습니다. 오늘 누계 100000000원, 한도 100000000원"));

        // 000001 잔액 103,250,000
        mvc.perform(get("/api/accounts/110-0917-000001"))
                .andExpect(jsonPath("$.balance").value(103250000));
    }

    // =========================================================================
    // TC-04-20 ~ TC-04-21: 서비스 단위 테스트 (직접 메서드 호출)
    // =========================================================================

    @Test
    @DisplayName("TC-04-20 영업시간 판정 경계")
    void tc0420_영업시간판정경계() {
        Calendar cal = Calendar.getInstance();

        // 2026-10-27(화) 08:59
        cal.set(2026, Calendar.OCTOBER, 27, 8, 59, 0);
        cal.set(Calendar.MILLISECOND, 0);
        assertThat(DateUtil.isBusinessHours(cal.getTime()), is(false));

        // 09:00
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        assertThat(DateUtil.isBusinessHours(cal.getTime()), is(true));

        // 17:59
        cal.set(Calendar.HOUR_OF_DAY, 17);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        assertThat(DateUtil.isBusinessHours(cal.getTime()), is(true));

        // 18:00
        cal.set(Calendar.HOUR_OF_DAY, 18);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        assertThat(DateUtil.isBusinessHours(cal.getTime()), is(false));

        // 2026-10-31(토) 10:00
        cal.set(2026, Calendar.OCTOBER, 31, 10, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        assertThat(DateUtil.isBusinessHours(cal.getTime()), is(false));

        // 2026-11-01(일) 10:00
        cal.set(2026, Calendar.NOVEMBER, 1, 10, 0, 0);
        assertThat(DateUtil.isBusinessHours(cal.getTime()), is(false));
    }

    @Test
    @DisplayName("TC-04-21 수수료 계산 고정 시각")
    void tc0421_수수료계산고정시각() {
        TransferService svc = new TransferService();
        Calendar cal = Calendar.getInstance();

        // 화 10:00
        cal.set(2026, Calendar.OCTOBER, 27, 10, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date base = cal.getTime();

        assertThat(svc.calculateFee("KDB", new BigDecimal("50000"), base), is(new BigDecimal("500")));

        // 화 20:00 (영업시간 외)
        cal.set(Calendar.HOUR_OF_DAY, 20);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date evening = cal.getTime();
        assertThat(svc.calculateFee("KDB", new BigDecimal("50000"), evening), is(new BigDecimal("800")));

        // 화 10:00, 100000
        cal.set(Calendar.HOUR_OF_DAY, 10);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        base = cal.getTime();
        assertThat(svc.calculateFee("KDB", new BigDecimal("100000"), base), is(new BigDecimal("800")));

        // 토 10:00, 1000000
        cal.set(2026, Calendar.OCTOBER, 31, 10, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date sat = cal.getTime();
        assertThat(svc.calculateFee("KDB", new BigDecimal("1000000"), sat), is(new BigDecimal("1800")));

        // NOVA, 토 10:00
        assertThat(svc.calculateFee("NOVA", new BigDecimal("1000000"), sat), is(BigDecimal.ZERO));

        // "", 화 20:00
        cal.set(2026, Calendar.OCTOBER, 27, 20, 0, 0);
        evening = cal.getTime();
        assertThat(svc.calculateFee("", new BigDecimal("1"), evening), is(BigDecimal.ZERO));

        // kdb, 99999, 화 10:00
        cal.set(2026, Calendar.OCTOBER, 27, 10, 0, 0);
        base = cal.getTime();
        assertThat(svc.calculateFee("kdb", new BigDecimal("99999"), base), is(new BigDecimal("500")));
    }
}
