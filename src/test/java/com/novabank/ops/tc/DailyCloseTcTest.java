package com.novabank.ops.tc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
import com.novabank.ops.service.DailyCloseService;
import com.novabank.ops.service.TransactionService;
import com.novabank.ops.service.TransferService;
import com.novabank.ops.util.TxIdGenerator;
import com.novabank.ops.web.CloseController;
import com.novabank.ops.web.GlobalExceptionHandler;
import com.novabank.ops.report.DailyCloseReport;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * NovaBank 동작테스트-TC §6 TC-06 일별 정산 리포트 전역 검증.
 *
 * <p>TC-06-01 ~ TC-06-06: MockMvc를 사용한 초기 상태 및 날짜 지정 리포트 검증
 * <p>TC-06-07 ~ TC-06-09: 서비스 단위 테스트로 거래 전제 후 집계 검증
 */
class DailyCloseTcTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AccountService accountService = new AccountService();
        ReflectionTestUtils.setField(accountService, "accountRepository", new InMemoryAccountRepository());

        TransactionService transactionService = new TransactionService();
        ReflectionTestUtils.setField(transactionService, "transactionRepository", new InMemoryTransactionRepository());
        ReflectionTestUtils.setField(transactionService, "accountService", accountService);
        ReflectionTestUtils.setField(transactionService, "txIdGenerator", new TxIdGenerator());

        CloseController controller = new CloseController();
        DailyCloseService dailyCloseService = new DailyCloseService();
        ReflectionTestUtils.setField(dailyCloseService, "accountService", accountService);
        ReflectionTestUtils.setField(dailyCloseService, "transactionService", transactionService);
        ReflectionTestUtils.setField(controller, "dailyCloseService", dailyCloseService);

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setViewResolvers(viewResolver)
                .build();
    }

    // =========================================================================
    // TC-06-01 초기 상태 JSON 리포트
    // =========================================================================
    @Test
    @DisplayName("TC-06-01 초기 상태 JSON 리포트")
    void tc0601_초기상태_JSON_리포트() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/close.json"))
                .andExpect(status().isOk())
                .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(result.getResponse().getContentAsString());

        assertThat(node.get("branchCode").asText(), equalTo("0917"));
        assertThat(node.get("accountCount").asInt(), equalTo(6));
        assertThat(node.get("activeAccountCount").asInt(), equalTo(5));
        assertThat(node.get("totalBalance").asLong(), equalTo(105480000L));
        assertThat(node.get("transactionCount").asInt(), equalTo(0));
        assertThat(node.get("depositAmount").asLong(), equalTo(0L));
        assertThat(node.get("withdrawAmount").asLong(), equalTo(0L));
        assertThat(node.get("transferOutAmount").asLong(), equalTo(0L));
        assertThat(node.get("feeAmount").asLong(), equalTo(0L));

        JsonNode byType = node.get("byType");
        assertThat(byType.size(), equalTo(5));

        // DEPOSIT
        assertThat(byType.get(0).get("type").asText(), equalTo("DEPOSIT"));
        assertThat(byType.get(0).get("label").asText(), equalTo("입금"));
        assertThat(byType.get(0).get("count").asInt(), equalTo(0));
        assertThat(byType.get(0).get("amount").asLong(), equalTo(0L));
        // WITHDRAW
        assertThat(byType.get(1).get("type").asText(), equalTo("WITHDRAW"));
        assertThat(byType.get(1).get("label").asText(), equalTo("출금"));
        assertThat(byType.get(1).get("count").asInt(), equalTo(0));
        assertThat(byType.get(1).get("amount").asLong(), equalTo(0L));
        // TRANSFER_IN
        assertThat(byType.get(2).get("type").asText(), equalTo("TRANSFER_IN"));
        assertThat(byType.get(2).get("label").asText(), equalTo("이체입금"));
        assertThat(byType.get(2).get("count").asInt(), equalTo(0));
        assertThat(byType.get(2).get("amount").asLong(), equalTo(0L));
        // TRANSFER_OUT
        assertThat(byType.get(3).get("type").asText(), equalTo("TRANSFER_OUT"));
        assertThat(byType.get(3).get("label").asText(), equalTo("이체출금"));
        assertThat(byType.get(3).get("count").asInt(), equalTo(0));
        assertThat(byType.get(3).get("amount").asLong(), equalTo(0L));
        // FEE
        assertThat(byType.get(4).get("type").asText(), equalTo("FEE"));
        assertThat(byType.get(4).get("label").asText(), equalTo("수수료"));
        assertThat(byType.get(4).get("count").asInt(), equalTo(0));
        assertThat(byType.get(4).get("amount").asLong(), equalTo(0L));
    }

    // =========================================================================
    // TC-06-02 초기 상태 XML 리포트
    // =========================================================================
    @Test
    @DisplayName("TC-06-02 초기 상태 XML 리포트")
    void tc0602_초기상태_XML_리포트() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/close.xml"))
                .andExpect(status().isOk())
                .andReturn();

        String xml = result.getResponse().getContentAsString();
        assertThat(xml, containsString("<dailyClose>"));
        assertThat(xml, containsString("<branchCode>0917</branchCode>"));
        assertThat(xml, containsString("<accountCount>6</accountCount>"));
        assertThat(xml, containsString("<activeAccountCount>5</activeAccountCount>"));
        assertThat(xml, containsString("<totalBalance>105480000</totalBalance>"));
        assertThat(xml, containsString("<transactionCount>0</transactionCount>"));
        // byType 개별 요소로 분리 검증
        assertThat(xml, containsString("<type>DEPOSIT</type>"));
        assertThat(xml, containsString("<label>입금</label>"));
        assertThat(xml, containsString("<count>0</count>"));
        assertThat(xml, containsString("<amount>0</amount>"));
    }

    // =========================================================================
    // TC-06-03 날짜 지정 조회 JSON
    // =========================================================================
    @Test
    @DisplayName("TC-06-03 날짜 지정 조회 JSON")
    void tc0603_날짜지정_JSON() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/close.json").param("date", "2013-11-29"))
                .andExpect(status().isOk())
                .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(result.getResponse().getContentAsString());

        assertThat(node.get("closeDate").asText(), equalTo("2013-11-29"));
        assertThat(node.get("branchCode").asText(), equalTo("0917"));
        assertThat(node.get("accountCount").asInt(), equalTo(6));
        assertThat(node.get("activeAccountCount").asInt(), equalTo(5));
        assertThat(node.get("totalBalance").asLong(), equalTo(105480000L));
        assertThat(node.get("transactionCount").asInt(), equalTo(0));
    }

    // =========================================================================
    // TC-06-04 날짜 지정 XML
    // =========================================================================
    @Test
    @DisplayName("TC-06-04 날짜 지정 XML")
    void tc0604_날짜지정_XML() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/close.xml").param("date", "2013-11-29"))
                .andExpect(status().isOk())
                .andReturn();

        String xml = result.getResponse().getContentAsString();
        assertThat(xml, containsString("<closeDate>2013-11-29</closeDate>"));
        assertThat(xml, containsString("<accountCount>6</accountCount>"));
        assertThat(xml, containsString("<totalBalance>105480000</totalBalance>"));
    }

    // =========================================================================
    // TC-06-05 날짜 형식 오류
    // =========================================================================
    @Test
    @DisplayName("TC-06-05 날짜 형식 오류")
    void tc0605_날짜형식_오류() throws Exception {
        mockMvc.perform(get("/api/close.json").param("date", "2013/11/29"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("title", "입력값을 확인해 주세요."))
                .andExpect(model().attribute("detail", "날짜 형식이 올바르지 않습니다(yyyy-MM-dd): 2013/11/29"))
                .andExpect(model().attribute("uri", "/api/close.json"));
    }

    // =========================================================================
    // TC-06-06 빈 날짜는 오늘
    // =========================================================================
    @Test
    @DisplayName("TC-06-06 빈 날짜는 오늘")
    void tc0606_빈날짜_오늘() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/close.json").param("date", ""))
                .andExpect(status().isOk())
                .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(result.getResponse().getContentAsString());

        assertThat(node.get("branchCode").asText(), equalTo("0917"));
        assertThat(node.get("accountCount").asInt(), equalTo(6));
        assertThat(node.get("totalBalance").asLong(), equalTo(105480000L));
    }

    // =========================================================================
    // TC-06-07 거래 반영 후 집계 (서비스 단위 테스트)
    // =========================================================================
    @Test
    @DisplayName("TC-06-07 거래 반영 후 집계")
    void tc0607_거래반영후_집계() throws Exception {
        // 전제(순서): 초기 상태에서 다음 거래를 순서대로 수행
        // ① 입금 500,000 ② 출금 200,000 ③ 입금 100
        // ④ 보통예금 개설 10,000 ⑤ 정기예금 개설 1,000,000
        // ⑥ 000005 해지 ⑦ 당행 이체 1,000,000 ⑧ 타행 이체 250,000

        InMemoryAccountRepository accRepo = new InMemoryAccountRepository();
        InMemoryTransactionRepository txRepo = new InMemoryTransactionRepository();
        AccountService accSvc = new AccountService();
        ReflectionTestUtils.setField(accSvc, "accountRepository", accRepo);
        TransactionService txSvc = new TransactionService();
        ReflectionTestUtils.setField(txSvc, "transactionRepository", txRepo);
        ReflectionTestUtils.setField(txSvc, "accountService", accSvc);
        ReflectionTestUtils.setField(txSvc, "txIdGenerator", new TxIdGenerator());
        TransferService transferSvc = new TransferService();
        ReflectionTestUtils.setField(transferSvc, "transactionService", txSvc);
        ReflectionTestUtils.setField(transferSvc, "accountService", accSvc);

        Date now = new Date();

        // ① 입금 500,000 → 110-0917-000001
        txSvc.deposit("110-0917-000001", new BigDecimal("500000"), "TC-06-07-1");
        // ② 출금 200,000 → 110-0917-000001
        txSvc.withdraw("110-0917-000001", new BigDecimal("200000"), "TC-06-07-2");
        // ③ 입금 100 → 110-0917-000001
        txSvc.deposit("110-0917-000001", new BigDecimal("100"), "TC-06-07-3");
        // ④ 보통예금 개설 10,000
        accSvc.open("새고객1", "01", new BigDecimal("10000"));
        // ⑤ 정기예금 개설 1,000,000
        accSvc.open("새고객2", "02", new BigDecimal("1000000"));
        // ⑥ 000005 해지
        accSvc.updateStatus("110-0917-000005", "C");
        // ⑦ 당행 이체 1,000,000
        transferSvc.transfer("110-0917-000006", "110-0917-000001", "NOVA", new BigDecimal("1000000"), "TC-06-07-7");
        // ⑧ 타행 이체 250,000
        transferSvc.transfer("110-0917-000006", "110-0917-000001", "KDB", new BigDecimal("250000"), "TC-06-07-8");

        // 리포트 생성
        DailyCloseService closeSvc = new DailyCloseService();
        ReflectionTestUtils.setField(closeSvc, "accountService", accSvc);
        ReflectionTestUtils.setField(closeSvc, "transactionService", txSvc);
        DailyCloseReport report = closeSvc.build(now);

        // 검증
        assertThat(report.getAccountCount(), equalTo(8));
        assertThat(report.getActiveAccountCount(), equalTo(7));
        assertThat(report.getTotalBalance().longValue(), equalTo(106539300L));
        assertThat(report.getTransactionCount(), equalTo(7));
        assertThat(report.getDepositAmount().longValue(), equalTo(500100L));
        assertThat(report.getWithdrawAmount().longValue(), equalTo(200000L));
        assertThat(report.getTransferOutAmount().longValue(), equalTo(1250000L));
        assertThat(report.getFeeAmount().longValue(), equalTo(800L));

        List<DailyCloseReport.TypeSummary> byType = report.getByType();
        assertThat(byType.size(), equalTo(5));
        assertThat(byType.get(0).getType(), equalTo("DEPOSIT"));
        assertThat(byType.get(0).getCount(), equalTo(2));
        assertThat(byType.get(0).getAmount().longValue(), equalTo(500100L));
        assertThat(byType.get(1).getType(), equalTo("WITHDRAW"));
        assertThat(byType.get(1).getCount(), equalTo(1));
        assertThat(byType.get(1).getAmount().longValue(), equalTo(200000L));
        assertThat(byType.get(2).getType(), equalTo("TRANSFER_IN"));
        assertThat(byType.get(2).getCount(), equalTo(1));
        assertThat(byType.get(2).getAmount().longValue(), equalTo(1000000L));
        assertThat(byType.get(3).getType(), equalTo("TRANSFER_OUT"));
        assertThat(byType.get(3).getCount(), equalTo(2));
        assertThat(byType.get(3).getAmount().longValue(), equalTo(1250000L));
        assertThat(byType.get(4).getType(), equalTo("FEE"));
        assertThat(byType.get(4).getCount(), equalTo(1));
        assertThat(byType.get(4).getAmount().longValue(), equalTo(800L));
    }

    // =========================================================================
    // TC-06-08 거래 반영 후 XML
    // =========================================================================
    @Test
    @DisplayName("TC-06-08 거래 반영 후 XML")
    void tc0608_거래반영후_XML() throws Exception {
        InMemoryAccountRepository accRepo = new InMemoryAccountRepository();
        InMemoryTransactionRepository txRepo = new InMemoryTransactionRepository();
        AccountService accSvc = new AccountService();
        ReflectionTestUtils.setField(accSvc, "accountRepository", accRepo);
        TransactionService txSvc = new TransactionService();
        ReflectionTestUtils.setField(txSvc, "transactionRepository", txRepo);
        ReflectionTestUtils.setField(txSvc, "accountService", accSvc);
        ReflectionTestUtils.setField(txSvc, "txIdGenerator", new TxIdGenerator());
        TransferService transferSvc = new TransferService();
        ReflectionTestUtils.setField(transferSvc, "transactionService", txSvc);
        ReflectionTestUtils.setField(transferSvc, "accountService", accSvc);

        Date now = new Date();
        txSvc.deposit("110-0917-000001", new BigDecimal("500000"), "TC-06-08-1");
        txSvc.withdraw("110-0917-000001", new BigDecimal("200000"), "TC-06-08-2");
        txSvc.deposit("110-0917-000001", new BigDecimal("100"), "TC-06-08-3");
        accSvc.open("새고객1", "01", new BigDecimal("10000"));
        accSvc.open("새고객2", "02", new BigDecimal("1000000"));
        accSvc.updateStatus("110-0917-000005", "C");
        transferSvc.transfer("110-0917-000006", "110-0917-000001", "NOVA", new BigDecimal("1000000"), "TC-06-08-7");
        transferSvc.transfer("110-0917-000006", "110-0917-000001", "KDB", new BigDecimal("250000"), "TC-06-08-8");

        DailyCloseService closeSvc = new DailyCloseService();
        ReflectionTestUtils.setField(closeSvc, "accountService", accSvc);
        ReflectionTestUtils.setField(closeSvc, "transactionService", txSvc);
        String xml = closeSvc.toXml(closeSvc.build(now));

        assertThat(xml, containsString("<transactionCount>7</transactionCount>"));
        assertThat(xml, containsString("<feeAmount>800</feeAmount>"));
        assertThat(xml, containsString("<totalBalance>106539300</totalBalance>"));
        // byType 개별 요소로 분리 검증
        assertThat(xml, containsString("<type>WITHDRAW</type>"));
        assertThat(xml, containsString("<label>출금</label>"));
        assertThat(xml, containsString("<count>1</count>"));
        assertThat(xml, containsString("<amount>200000</amount>"));
    }

    // =========================================================================
    // TC-06-09 다른 날짜에는 오늘 거래가 집계되지 않음
    // =========================================================================
    @Test
    @DisplayName("TC-06-09 다른 날짜에는 오늘 거래가 집계되지 않음")
    void tc0609_다른날짜_거래미집계() throws Exception {
        InMemoryAccountRepository accRepo = new InMemoryAccountRepository();
        InMemoryTransactionRepository txRepo = new InMemoryTransactionRepository();
        AccountService accSvc = new AccountService();
        ReflectionTestUtils.setField(accSvc, "accountRepository", accRepo);
        TransactionService txSvc = new TransactionService();
        ReflectionTestUtils.setField(txSvc, "transactionRepository", txRepo);
        ReflectionTestUtils.setField(txSvc, "accountService", accSvc);
        ReflectionTestUtils.setField(txSvc, "txIdGenerator", new TxIdGenerator());
        TransferService transferSvc = new TransferService();
        ReflectionTestUtils.setField(transferSvc, "transactionService", txSvc);
        ReflectionTestUtils.setField(transferSvc, "accountService", accSvc);

        Date now = new Date();
        txSvc.deposit("110-0917-000001", new BigDecimal("500000"), "TC-06-09-1");
        txSvc.withdraw("110-0917-000001", new BigDecimal("200000"), "TC-06-09-2");
        txSvc.deposit("110-0917-000001", new BigDecimal("100"), "TC-06-09-3");
        accSvc.open("새고객1", "01", new BigDecimal("10000"));
        accSvc.open("새고객2", "02", new BigDecimal("1000000"));
        accSvc.updateStatus("110-0917-000005", "C");
        transferSvc.transfer("110-0917-000006", "110-0917-000001", "NOVA", new BigDecimal("1000000"), "TC-06-09-7");
        transferSvc.transfer("110-0917-000006", "110-0917-000001", "KDB", new BigDecimal("250000"), "TC-06-09-8");

        DailyCloseService closeSvc = new DailyCloseService();
        ReflectionTestUtils.setField(closeSvc, "accountService", accSvc);
        ReflectionTestUtils.setField(closeSvc, "transactionService", txSvc);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        Date pastDate = sdf.parse("2013-11-29");
        DailyCloseReport report = closeSvc.build(pastDate);

        assertThat(report.getAccountCount(), equalTo(8));
        assertThat(report.getActiveAccountCount(), equalTo(7));
        assertThat(report.getTotalBalance().longValue(), equalTo(106539300L));
        assertThat(report.getTransactionCount(), equalTo(0));
        assertThat(report.getDepositAmount().longValue(), equalTo(0L));
        assertThat(report.getWithdrawAmount().longValue(), equalTo(0L));
        assertThat(report.getTransferOutAmount().longValue(), equalTo(0L));
        assertThat(report.getFeeAmount().longValue(), equalTo(0L));
    }
}
