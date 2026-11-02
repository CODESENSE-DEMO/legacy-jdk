package com.novabank.ops.tc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import static org.hamcrest.Matchers.nullValue;

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
 * NovaBank 동작테스트-TC §0 TC-02 계좌 개설·상태 변경 장 전 검증.
 *
 * <p>기존 AccountQueryTcTest과 동일한 설정 방식:
 * <ul>
 *   <li>MockMvcBuilders.standaloneSetup(controller)</li>
 *   <li>setControllerAdvice(new GlobalExceptionHandler())</li>
 *   <li>InternalResourceViewResolver: prefix /WEB-INF/views/, suffix .jsp</li>
 *   <li>AccountService / TransactionService에 ReflectionTestUtils.setField로
 *       실제 InMemoryAccountRepository / InMemoryTransactionRepository 연결</li>
 * </ul>
 */
class AccountOpenStatusTcTest {

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
    // TC-02-01 보통예금 개설 — POST /accounts
    // =========================================================================
    @Test
    @DisplayName("TC-02-01 보통예금 개설")
    void tc0201_보통예금_개설() throws Exception {
        // 계좌 개설 요청
        mockMvc.perform(post("/accounts")
                        .param("customerName", "테스트고객")
                        .param("type", "01")
                        .param("initialDeposit", "10000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/110-0917-000007"))
                .andExpect(flash().attribute("message", "계좌를 개설했습니다: 110-0917-000007"));

        // 개설된 계좌 조회
        mockMvc.perform(get("/api/accounts/110-0917-000007"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("테스트고객"))
                .andExpect(jsonPath("$.type").value("SAVINGS"))
                .andExpect(jsonPath("$.balance").value(10000))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.closedAt").value(nullValue()))
                .andExpect(jsonPath("$.maskedName").value("테***객"))
                .andExpect(jsonPath("$.active").value(true));

        // 계좌 목록 길이 확인
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(7));
    }

    // =========================================================================
    // TC-02-02 정기예금 개설, 종류 이름·콤마 금액 — POST /accounts
    // =========================================================================
    @Test
    @DisplayName("TC-02-02 정기예금 개설, 종류 이름·콤마 금액")
    void tc0202_정기예금_개설_종류이름_콤마금액() throws Exception {
        mockMvc.perform(post("/accounts")
                        .param("customerName", "정기고객")
                        .param("type", "FIXED")
                        .param("initialDeposit", "1,000,000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/110-0917-000007"))
                .andExpect(flash().attribute("message", "계좌를 개설했습니다: 110-0917-000007"));

        mockMvc.perform(get("/api/accounts/110-0917-000007"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FIXED"))
                .andExpect(jsonPath("$.balance").value(1000000))
                .andExpect(jsonPath("$.maskedName").value("정**객"));
    }

    // =========================================================================
    // TC-02-03 정기예금 최소 금액 미달 오류 — POST /accounts
    // =========================================================================
    @Test
    @DisplayName("TC-02-03 정기예금 최소 금액 미달 오류")
    void tc0203_정기예금_최소_금액_미달_오류() throws Exception {
        mockMvc.perform(post("/accounts")
                        .param("customerName", "정기저액")
                        .param("type", "03")
                        .param("initialDeposit", "999999"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "정기예금은 최초 입금액이 1,000,000원 이상이어야 합니다."))
                .andExpect(model().attribute("uri", "/accounts"));

        // 계좌 수는 그대로 6
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));
    }

    // =========================================================================
    // TC-02-04 예금주 공백 오류 — POST /accounts
    // =========================================================================
    @Test
    @DisplayName("TC-02-04 예금주 공백 오류")
    void tc0204_예금주_공백_오류() throws Exception {
        mockMvc.perform(post("/accounts")
                        .param("customerName", "")
                        .param("type", "01")
                        .param("initialDeposit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "예금주 이름은 필수입니다."))
                .andExpect(model().attribute("uri", "/accounts"));
    }

    // =========================================================================
    // TC-02-05 최초 입금액 생략은 0원 개설 — POST /accounts
    // =========================================================================
    @Test
    @DisplayName("TC-02-05 최초 입금액 생략은 0원 개설")
    void tc0205_최초_입금액_생략_0원_개설() throws Exception {
        mockMvc.perform(post("/accounts")
                        .param("customerName", "무입금")
                        .param("type", "02"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/110-0917-000007"))
                .andExpect(flash().attribute("message", "계좌를 개설했습니다: 110-0917-000007"));

        mockMvc.perform(get("/api/accounts/110-0917-000007"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CHECKING"))
                .andExpect(jsonPath("$.balance").value(0));
    }

    // =========================================================================
    // TC-02-06 알 수 없는 예금 종류 오류 — POST /accounts
    // =========================================================================
    @Test
    @DisplayName("TC-02-06 알 수 없는 예금 종류 오류")
    void tc0206_알_수_없는_예금_종류_오류() throws Exception {
        mockMvc.perform(post("/accounts")
                        .param("customerName", "홍길동")
                        .param("type", "99")
                        .param("initialDeposit", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "알 수 없는 예금 종류: 99"))
                .andExpect(model().attribute("uri", "/accounts"));
    }

    // =========================================================================
    // TC-02-07 음수 최초 입금액 오류 — POST /accounts
    // =========================================================================
    @Test
    @DisplayName("TC-02-07 음수 최초 입금액 오류")
    void tc0207_음수_최초_입금액_오류() throws Exception {
        mockMvc.perform(post("/accounts")
                        .param("customerName", "홍길동")
                        .param("type", "01")
                        .param("initialDeposit", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "최초 입금액은 0 이상이어야 합니다."))
                .andExpect(model().attribute("uri", "/accounts"));
    }

    // =========================================================================
    // TC-02-08 정상 → 거래정지 — POST /accounts/110-0917-000002/status
    // =========================================================================
    @Test
    @DisplayName("TC-02-08 정상 → 거래정지")
    void tc0208_정상_거래정지() throws Exception {
        mockMvc.perform(post("/accounts/110-0917-000002/status")
                        .param("status", "F"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/110-0917-000002"))
                .andExpect(flash().attribute("message", "110-0917-000002 상태를 거래정지(으)로 변경했습니다."));

        mockMvc.perform(get("/api/accounts/110-0917-000002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.balance").value(12480000));
    }

    // =========================================================================
    // TC-02-09 거래정지 계좌 입금 거부 — POST /accounts/110-0917-000002/deposit
    // =========================================================================
    @Test
    @DisplayName("TC-02-09 거래정지 계좌 입금 거부")
    void tc0209_거래정지_계좌_입금_거부() throws Exception {
        // 전제: 먼저 거래정지로 변경
        mockMvc.perform(post("/accounts/110-0917-000002/status")
                        .param("status", "F"));

        // 입금 시도 거부
        mockMvc.perform(post("/accounts/110-0917-000002/deposit")
                        .param("amount", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "거래할 수 없는 계좌 상태입니다(거래정지): 110-0917-000002"))
                .andExpect(model().attribute("uri", "/accounts/110-0917-000002/deposit"));
    }

    // =========================================================================
    // TC-02-10 거래정지 → 정상 복귀 — POST /accounts/110-0917-000002/status
    // =========================================================================
    @Test
    @DisplayName("TC-02-10 거래정지 → 정상 복귀")
    void tc0210_거래정지_정상_복귀() throws Exception {
        // 전제: 먼저 거래정지로 변경
        mockMvc.perform(post("/accounts/110-0917-000002/status")
                        .param("status", "F"));

        // 정상 복귀
        mockMvc.perform(post("/accounts/110-0917-000002/status")
                        .param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/110-0917-000002"))
                .andExpect(flash().attribute("message", "110-0917-000002 상태를 정상(으)로 변경했습니다."));

        mockMvc.perform(get("/api/accounts/110-0917-000002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.active").value(true));
    }

    // =========================================================================
    // TC-02-11 잔액 있는 계좌 해지 거부 — POST /accounts/110-0917-000003/status
    // =========================================================================
    @Test
    @DisplayName("TC-02-11 잔액 있는 계좌 해지 거부")
    void tc0211_잔액_있는_계좌_해지_거부() throws Exception {
        mockMvc.perform(post("/accounts/110-0917-000003/status")
                        .param("status", "C"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "잔액이 남아 있는 계좌는 해지할 수 없습니다. 잔액 850000원"))
                .andExpect(model().attribute("uri", "/accounts/110-0917-000003/status"));

        // 상태는 ACTIVE 그대로
        mockMvc.perform(get("/api/accounts/110-0917-000003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // =========================================================================
    // TC-02-12 잔액 0 원 계좌 해지 — POST /accounts/110-0917-000005/status
    // =========================================================================
    @Test
    @DisplayName("TC-02-12 잔액 0 원 계좌 해지")
    void tc0212_잔액_0원_계좌_해지() throws Exception {
        mockMvc.perform(post("/accounts/110-0917-000005/status")
                        .param("status", "CLOSED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/110-0917-000005"))
                .andExpect(flash().attribute("message", "110-0917-000005 상태를 해지(으)로 변경했습니다."));

        mockMvc.perform(get("/api/accounts/110-0917-000005"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.closedAt").exists());
    }

    // =========================================================================
    // TC-02-13 해지 계좌 되돌리기 거부 — POST /accounts/110-0917-000005/status
    // =========================================================================
    @Test
    @DisplayName("TC-02-13 해지 계좌 되돌리기 거부")
    void tc0213_해지_계좌_되돌리기_거부() throws Exception {
        // 전제: 먼저 해지
        mockMvc.perform(post("/accounts/110-0917-000005/status")
                        .param("status", "CLOSED"));

        // 되돌리기 시도 거부
        mockMvc.perform(post("/accounts/110-0917-000005/status")
                        .param("status", "A"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "해지된 계좌는 상태를 되돌릴 수 없습니다: 110-0917-000005"))
                .andExpect(model().attribute("uri", "/accounts/110-0917-000005/status"));
    }

    // =========================================================================
    // TC-02-14 알 수 없는 상태 코드 오류 — POST /accounts/110-0917-000001/status
    // =========================================================================
    @Test
    @DisplayName("TC-02-14 알 수 없는 상태 코드 오류")
    void tc0214_알_수_없는_상태_코드_오류() throws Exception {
        mockMvc.perform(post("/accounts/110-0917-000001/status")
                        .param("status", "X"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "알 수 없는 계좌 상태 코드: X"))
                .andExpect(model().attribute("uri", "/accounts/110-0917-000001/status"));
    }
}
