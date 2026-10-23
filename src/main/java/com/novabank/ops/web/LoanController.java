package com.novabank.ops.web;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.novabank.ops.domain.LoanSchedule;
import com.novabank.ops.service.LoanCalculatorService;
import com.novabank.ops.util.MoneyUtil;

/**
 * 대출 상환 계산기 화면·API.
 */
@Controller
public class LoanController {

    @Autowired
    private LoanCalculatorService loanCalculatorService;

    @RequestMapping(value = "/loan", method = RequestMethod.GET)
    public String form(Model model) {
        model.addAttribute("principal", "10,000,000");
        model.addAttribute("rate", "4.5");
        model.addAttribute("months", 12);
        model.addAttribute("method", LoanSchedule.EQUAL_PAYMENT);
        return "loan";
    }

    @RequestMapping(value = "/loan", method = RequestMethod.POST)
    public String calculate(@RequestParam("principal") String principal,
                            @RequestParam("rate") BigDecimal rate,
                            @RequestParam("months") int months,
                            @RequestParam("method") String method,
                            Model model) {
        LoanSchedule schedule = loanCalculatorService.schedule(MoneyUtil.parse(principal), rate, months, method);
        model.addAttribute("schedule", schedule);
        model.addAttribute("principal", principal);
        model.addAttribute("rate", rate);
        model.addAttribute("months", months);
        model.addAttribute("method", schedule.getMethod());
        return "loan";
    }

    @RequestMapping(value = "/api/loan/schedule", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public LoanSchedule apiSchedule(@RequestParam("principal") String principal,
                                    @RequestParam("rate") BigDecimal rate,
                                    @RequestParam("months") int months,
                                    @RequestParam(value = "method", required = false) String method) {
        return loanCalculatorService.schedule(MoneyUtil.parse(principal), rate, months, method);
    }
}
