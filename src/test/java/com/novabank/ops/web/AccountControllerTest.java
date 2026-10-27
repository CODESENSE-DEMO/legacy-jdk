package com.novabank.ops.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
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

class AccountControllerTest {

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

        // 단독 설정에는 뷰 리졸버가 없어 뷰 이름이 요청 경로와 같으면 순환으로 본다 — 운영 설정과 같게 둔다
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    void 계좌_목록_화면() throws Exception {
        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(view().name("accounts"))
                .andExpect(model().attributeExists("accounts", "types"));
    }

    @Test
    void 계좌_상세_화면() throws Exception {
        mockMvc.perform(get("/accounts/110-0917-000001"))
                .andExpect(status().isOk())
                .andExpect(view().name("account"))
                .andExpect(model().attributeExists("account", "transactions"));
    }

    @Test
    void 계좌_API_는_JSON() throws Exception {
        mockMvc.perform(get("/api/accounts/110-0917-000002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNo").value("110-0917-000002"))
                .andExpect(jsonPath("$.type").value("CHECKING"))
                .andExpect(jsonPath("$.balance").value(12480000));
    }

    @Test
    void 입금_후_상세로_돌아간다() throws Exception {
        mockMvc.perform(post("/accounts/110-0917-000001/deposit").param("amount", "1,000").param("memo", "테스트"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts/110-0917-000001"));
    }

    @Test
    void 잘못된_입력은_400_오류_화면() throws Exception {
        mockMvc.perform(post("/accounts/110-0917-000001/withdraw").param("amount", "99,999,999"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"));
    }

    @Test
    void 없는_계좌는_400() throws Exception {
        mockMvc.perform(get("/api/accounts/000-0000-000000"))
                .andExpect(status().isBadRequest());
    }
}
