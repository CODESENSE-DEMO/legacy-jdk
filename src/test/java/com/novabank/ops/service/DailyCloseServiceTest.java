package com.novabank.ops.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.ops.report.DailyCloseReport;
import com.novabank.ops.repository.InMemoryAccountRepository;
import com.novabank.ops.repository.InMemoryTransactionRepository;
import com.novabank.ops.util.TxIdGenerator;

public class DailyCloseServiceTest {

    private DailyCloseService service;
    private TransactionService transactionService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() {
        AccountService accountService = new AccountService();
        ReflectionTestUtils.setField(accountService, "accountRepository", new InMemoryAccountRepository());
        transactionService = new TransactionService();
        ReflectionTestUtils.setField(transactionService, "transactionRepository", new InMemoryTransactionRepository());
        ReflectionTestUtils.setField(transactionService, "accountService", accountService);
        ReflectionTestUtils.setField(transactionService, "txIdGenerator", new TxIdGenerator());
        service = new DailyCloseService();
        ReflectionTestUtils.setField(service, "accountService", accountService);
        ReflectionTestUtils.setField(service, "transactionService", transactionService);
        ReflectionTestUtils.setField(service, "objectMapper", mapper);
    }

    @Test
    public void 오늘_거래를_종류별로_집계한다() {
        transactionService.deposit("110-0917-000001", new BigDecimal("100000"), null);
        transactionService.withdraw("110-0917-000001", new BigDecimal("30000"), null);
        DailyCloseReport r = service.build(new Date());
        assertEquals(6, r.getAccountCount());
        assertEquals(5, r.getActiveAccountCount());
        assertEquals(2, r.getTransactionCount());
        assertEquals(new BigDecimal("100000"), r.getDepositAmount());
        assertEquals(new BigDecimal("30000"), r.getWithdrawAmount());
        assertEquals(new BigDecimal("105550000"), r.getTotalBalance());
        assertEquals(5, r.getByType().size());
    }

    @Test
    public void XML_에는_루트와_종류별_요약이_들어간다() {
        String xml = service.toXml(service.build(new Date()));
        assertTrue(xml.contains("<dailyClose>"));
        assertTrue(xml.contains("<byType>"));
        assertTrue(xml.contains("<branchCode>0917</branchCode>"));
    }

    @Test
    public void JSON_은_다시_읽을_수_있다() throws Exception {
        JsonNode node = mapper.readTree(service.toJson(service.build(new Date())));
        assertEquals(6, node.get("accountCount").asInt());
        assertEquals(5, node.get("byType").size());
    }
}
