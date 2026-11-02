package com.novabank.ops.tc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import com.novabank.ops.service.LoanCalculatorService;
import com.novabank.ops.web.LoanController;
import com.novabank.ops.web.GlobalExceptionHandler;

/**
 * NovaBank 대출 상환 계산기 API 검증 TC.
 *
 * <p>기존 tc 패키지 패턴과 동일:
 * <ul>
 *   <li>MockMvcBuilders.standaloneSetup(controller)</li>
 *   <li>setControllerAdvice(new GlobalExceptionHandler())</li>
 *   <li>InternalResourceViewResolver: prefix /WEB-INF/views/, suffix .jsp</li>
 *   <li>ReflectionTestUtils.setField로 LoanCalculatorService 주입</li>
 * </ul>
 */
class LoanTcTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LoanCalculatorService loanService = new LoanCalculatorService();
        LoanController controller = new LoanController();
        ReflectionTestUtils.setField(controller, "loanCalculatorService", loanService);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setViewResolvers(viewResolver)
                .build();
    }

    // =========================================================================
    // TC-05-01: 원리금균등 1천만원 4.5% 12개월
    // =========================================================================
    @Test
    @DisplayName("TC-05-01 원리금균등 1천만원 4.5 12개월")
    void tc0501_equalPayment_10M_4_5_12M() throws Exception {
        mockMvc.perform(get("/api/loan/schedule")
                .param("principal", "10000000")
                .param("rate", "4.5")
                .param("months", "12")
                .param("method", "EQUAL_PAYMENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.method").value("EQUAL_PAYMENT"))
            .andExpect(jsonPath("$.methodLabel").value("원리금균등"))
            .andExpect(jsonPath("$.principal").value(10000000))
            .andExpect(jsonPath("$.annualRatePercent").value(4.5))
            .andExpect(jsonPath("$.months").value(12))
            .andExpect(jsonPath("$.totalInterest").value(245424))
            .andExpect(jsonPath("$.totalPayment").value(10245424))
            .andExpect(jsonPath("$.installments.length()").value(12))
            // 회차 1
            .andExpect(jsonPath("$.installments[0].payment").value(853785))
            .andExpect(jsonPath("$.installments[0].principal").value(816285))
            .andExpect(jsonPath("$.installments[0].interest").value(37500))
            .andExpect(jsonPath("$.installments[0].balance").value(9183715))
            // 회차 12
            .andExpect(jsonPath("$.installments[11].payment").value(853789))
            .andExpect(jsonPath("$.installments[11].principal").value(850599))
            .andExpect(jsonPath("$.installments[11].interest").value(3190))
            .andExpect(jsonPath("$.installments[11].balance").value(0));
    }

    // =========================================================================
    // TC-05-02: 원금균등 1천만원 4.5% 12개월
    // =========================================================================
    @Test
    @DisplayName("TC-05-02 원금균등 1천만원 4.5 12개월")
    void tc0502_equalPrincipal_10M_4_5_12M() throws Exception {
        mockMvc.perform(get("/api/loan/schedule")
                .param("principal", "10000000")
                .param("rate", "4.5")
                .param("months", "12")
                .param("method", "EQUAL_PRINCIPAL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.method").value("EQUAL_PRINCIPAL"))
            .andExpect(jsonPath("$.methodLabel").value("원금균등"))
            .andExpect(jsonPath("$.totalInterest").value(243750))
            .andExpect(jsonPath("$.totalPayment").value(10243750))
            // 회차 1
            .andExpect(jsonPath("$.installments[0].payment").value(870833))
            .andExpect(jsonPath("$.installments[0].principal").value(833333))
            .andExpect(jsonPath("$.installments[0].interest").value(37500))
            .andExpect(jsonPath("$.installments[0].balance").value(9166667))
            // 회차 12
            .andExpect(jsonPath("$.installments[11].payment").value(836462))
            .andExpect(jsonPath("$.installments[11].principal").value(833337))
            .andExpect(jsonPath("$.installments[11].interest").value(3125))
            .andExpect(jsonPath("$.installments[11].balance").value(0));
    }

    // =========================================================================
    // TC-05-03: 상환 방식 생략은 원리금균등, 콤마 원금
    // =========================================================================
    @Test
    @DisplayName("TC-05-03 상환 방식 생략은 원리금균등, 콤마 원금")
    void tc_05_03_상환_방식_생략_원리금균등_콤마_원금() throws Exception {
        mockMvc.perform(get("/api/loan/schedule")
                .param("principal", "10,000,000")
                .param("rate", "4.5")
                .param("months", "12"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.method").value("EQUAL_PAYMENT"))
            .andExpect(jsonPath("$.principal").value(10000000))
            .andExpect(jsonPath("$.installments.length()").value(12))
            // 회차 1
            .andExpect(jsonPath("$.installments[0].payment").value(853785))
            .andExpect(jsonPath("$.installments[0].principal").value(816285))
            .andExpect(jsonPath("$.installments[0].interest").value(37500))
            .andExpect(jsonPath("$.installments[0].balance").value(9183715))
            // 회차 12
            .andExpect(jsonPath("$.installments[11].payment").value(853789))
            .andExpect(jsonPath("$.installments[11].principal").value(850599))
            .andExpect(jsonPath("$.installments[11].interest").value(3190))
            .andExpect(jsonPath("$.installments[11].balance").value(0));
    }

    // =========================================================================
    // TC-05-04: 원리금균등 100만원 12% 3개월, 소문자 method
    // =========================================================================
    @Test
    @DisplayName("TC-05-04 원리금균등 100만원 12 3개월 소문자 method")
    void tc0504_equalPayment_1M_12_3M() throws Exception {
        mockMvc.perform(get("/api/loan/schedule")
                .param("principal", "1,000,000")
                .param("rate", "12")
                .param("months", "3")
                .param("method", "equal_payment"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.method").value("EQUAL_PAYMENT"))
            .andExpect(jsonPath("$.principal").value(1000000))
            .andExpect(jsonPath("$.annualRatePercent").value(12))
            .andExpect(jsonPath("$.months").value(3))
            .andExpect(jsonPath("$.totalInterest").value(20067))
            .andExpect(jsonPath("$.totalPayment").value(1020067))
            // 회차 1
            .andExpect(jsonPath("$.installments[0].payment").value(340022))
            .andExpect(jsonPath("$.installments[0].principal").value(330022))
            .andExpect(jsonPath("$.installments[0].interest").value(10000))
            .andExpect(jsonPath("$.installments[0].balance").value(669978))
            // 회차 2
            .andExpect(jsonPath("$.installments[1].payment").value(340022))
            .andExpect(jsonPath("$.installments[1].principal").value(333322))
            .andExpect(jsonPath("$.installments[1].interest").value(6700))
            .andExpect(jsonPath("$.installments[1].balance").value(336656))
            // 회차 3
            .andExpect(jsonPath("$.installments[2].payment").value(340023))
            .andExpect(jsonPath("$.installments[2].principal").value(336656))
            .andExpect(jsonPath("$.installments[2].interest").value(3367))
            .andExpect(jsonPath("$.installments[2].balance").value(0));
    }

    // =========================================================================
    // TC-05-05: 연이율 범위 초과 오류
    // =========================================================================
    @Test
    @DisplayName("TC-05-05 연이율 범위 초과 오류")
    void tc_05_05_연이율_범위_초과_오류() throws Exception {
        mockMvc.perform(get("/api/loan/schedule")
                .param("principal", "10000000")
                .param("rate", "25")
                .param("months", "12"))
            .andExpect(status().isBadRequest())
            .andExpect(view().name("error"))
            .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
            .andExpect(model().attribute("detail", "연이율은 1.0% ~ 20.0% 사이여야 합니다."))
            .andExpect(model().attribute("uri", "/api/loan/schedule"));
    }

    // =========================================================================
    // TC-05-06: 상환 개월 범위 초과 오류
    // =========================================================================
    @Test
    @DisplayName("TC-05-06 상환 개월 범위 초과 오류")
    void tc_05_06_상환_개월_범위_초과_오류() throws Exception {
        mockMvc.perform(get("/api/loan/schedule")
                .param("principal", "10000000")
                .param("rate", "4.5")
                .param("months", "481"))
            .andExpect(status().isBadRequest())
            .andExpect(view().name("error"))
            .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
            .andExpect(model().attribute("detail", "상환 개월 수는 1 ~ 480 사이여야 합니다."))
            .andExpect(model().attribute("uri", "/api/loan/schedule"));
    }

    // =========================================================================
    // TC-05-07: 원금 0 오류
    // =========================================================================
    @Test
    @DisplayName("TC-05-07 원금 0 오류")
    void tc_05_07_원금_0_오류() throws Exception {
        mockMvc.perform(get("/api/loan/schedule")
                .param("principal", "0")
                .param("rate", "4.5")
                .param("months", "12"))
            .andExpect(status().isBadRequest())
            .andExpect(view().name("error"))
            .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
            .andExpect(model().attribute("detail", "대출 원금은 0보다 커야 합니다."))
            .andExpect(model().attribute("uri", "/api/loan/schedule"));
    }

    // =========================================================================
    // TC-05-08: 알 수 없는 상환 방식 오류
    // =========================================================================
    @Test
    @DisplayName("TC-05-08 알 수 없는 상환 방식 오류")
    void tc_05_08_알_수_없는_상환_방식_오류() throws Exception {
        mockMvc.perform(get("/api/loan/schedule")
                .param("principal", "10000000")
                .param("rate", "4.5")
                .param("months", "12")
                .param("method", "BULLET"))
            .andExpect(status().isBadRequest())
            .andExpect(view().name("error"))
            .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
            .andExpect(model().attribute("detail", "알 수 없는 상환 방식: BULLET"))
            .andExpect(model().attribute("uri", "/api/loan/schedule"));
    }

    // =========================================================================
    // TC-05-09: 경계값 허용: 1개월, 이율 1.0과 20.0
    // =========================================================================
    @Test
    @DisplayName("TC-05-09 경계값 허용: 1개월, 이율 1.0과 20.0")
    void tc_05_09_경계값_허용() throws Exception {
        // ① months=1
        mockMvc.perform(get("/api/loan/schedule")
                .param("principal", "10000000")
                .param("rate", "4.5")
                .param("months", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.installments.length()").value(1))
            .andExpect(jsonPath("$.installments[0].payment").value(10037500))
            .andExpect(jsonPath("$.installments[0].principal").value(10000000))
            .andExpect(jsonPath("$.installments[0].interest").value(37500))
            .andExpect(jsonPath("$.installments[0].balance").value(0))
            .andExpect(jsonPath("$.totalInterest").value(37500))
            .andExpect(jsonPath("$.totalPayment").value(10037500));

        // ② rate=1.0
        mockMvc.perform(get("/api/loan/schedule")
                .param("principal", "10000000")
                .param("rate", "1.0")
                .param("months", "12"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.annualRatePercent").value(1.0));

        // ③ rate=20.0
        mockMvc.perform(get("/api/loan/schedule")
                .param("principal", "10000000")
                .param("rate", "20.0")
                .param("months", "12"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.annualRatePercent").value(20.0));

        // ④ rate=0.9 → 400
        mockMvc.perform(get("/api/loan/schedule")
                .param("principal", "10000000")
                .param("rate", "0.9")
                .param("months", "12"))
            .andExpect(status().isBadRequest())
            .andExpect(view().name("error"));

        // ⑤ rate=20.1 → 400
        mockMvc.perform(get("/api/loan/schedule")
                .param("principal", "10000000")
                .param("rate", "20.1")
                .param("months", "12"))
            .andExpect(status().isBadRequest())
            .andExpect(view().name("error"));
    }

    // =========================================================================
    // TC-05-10: 원리금균등 12회차 합계 검산 — 응답 내부 정합성
    // =========================================================================
    @Test
    @DisplayName("TC-05-10 원리금균등 12회차 합계 검산 — 응답 내부 정합성")
    void tc_05_10_응답_내부_정합성() throws Exception {
        // TC-05-01 응답
        String resp1 = mockMvc.perform(get("/api/loan/schedule")
                .param("principal", "10000000")
                .param("rate", "4.5")
                .param("months", "12")
                .param("method", "EQUAL_PAYMENT"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        // TC-05-02 응답
        String resp2 = mockMvc.perform(get("/api/loan/schedule")
                .param("principal", "10000000")
                .param("rate", "4.5")
                .param("months", "12")
                .param("method", "EQUAL_PRINCIPAL"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        // TC-05-04 응답
        String resp4 = mockMvc.perform(get("/api/loan/schedule")
                .param("principal", "1,000,000")
                .param("rate", "12")
                .param("months", "3")
                .param("method", "equal_payment"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        // JSON 파싱 및 검증 (Jackson ObjectMapper 사용)
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode root1 = mapper.readTree(resp1);
        com.fasterxml.jackson.databind.JsonNode root2 = mapper.readTree(resp2);
        com.fasterxml.jackson.databind.JsonNode root4 = mapper.readTree(resp4);

        // 각 응답에 대해 검증:
        // 1. installments[].payment 합 = totalPayment
        // 2. installments[].interest 합 = totalInterest
        // 3. 마지막 installments[].balance = 0
        // 4. 각 installments[].payment = principal + interest
        checkInternalConsistency(root1, "TC-05-01");
        checkInternalConsistency(root2, "TC-05-02");
        checkInternalConsistency(root4, "TC-05-04");
    }

    private void checkInternalConsistency(com.fasterxml.jackson.databind.JsonNode root, String label) throws AssertionError {
        com.fasterxml.jackson.databind.JsonNode installments = root.get("installments");
        long paymentSum = 0;
        long interestSum = 0;
        int lastMonth = installments.size() - 1;

        for (int i = 0; i < installments.size(); i++) {
            com.fasterxml.jackson.databind.JsonNode inst = installments.get(i);
            long payment = inst.get("payment").asLong();
            long principal = inst.get("principal").asLong();
            long interest = inst.get("interest").asLong();
            long balance = inst.get("balance").asLong();

            paymentSum += payment;
            interestSum += interest;

            // 각 회차 payment = principal + interest
            if (payment != principal + interest) {
                throw new AssertionError(label + " 회차 " + (i+1) + ": payment(" + payment + ") != principal(" + principal + ") + interest(" + interest + ")");
            }
        }

        // 마지막 balance = 0
        long lastBalance = installments.get(lastMonth).get("balance").asLong();
        if (lastBalance != 0) {
            throw new AssertionError(label + " 마지막 balance(" + lastBalance + ") != 0");
        }

        // payment 합 = totalPayment
        long totalPayment = root.get("totalPayment").asLong();
        if (paymentSum != totalPayment) {
            throw new AssertionError(label + " payment 합(" + paymentSum + ") != totalPayment(" + totalPayment + ")");
        }

        // interest 합 = totalInterest
        long totalInterest = root.get("totalInterest").asLong();
        if (interestSum != totalInterest) {
            throw new AssertionError(label + " interest 합(" + interestSum + ") != totalInterest(" + totalInterest + ")");
        }
    }
}
