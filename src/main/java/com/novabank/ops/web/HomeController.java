package com.novabank.ops.web;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.novabank.ops.domain.Transaction;
import com.novabank.ops.service.AccountService;
import com.novabank.ops.service.DailyCloseService;
import com.novabank.ops.service.TransactionService;

/**
 * 대시보드.
 */
@Controller
public class HomeController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private DailyCloseService dailyCloseService;

    public static final int RECENT_TX_LIMIT = 10;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String home(Model model) {
        model.addAttribute("today", new Date());
        model.addAttribute("report", dailyCloseService.build(new Date()));
        model.addAttribute("accounts", accountService.findAll());
        List<Transaction> recent = transactionService.findAll();
        model.addAttribute("recent", recent.size() > RECENT_TX_LIMIT ? recent.subList(0, RECENT_TX_LIMIT) : recent);
        return "index";
    }
}
