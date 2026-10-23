package com.novabank.ops.web;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.novabank.ops.domain.TransferResult;
import com.novabank.ops.service.AccountService;
import com.novabank.ops.service.TransferService;
import com.novabank.ops.util.DateUtil;
import com.novabank.ops.util.MoneyUtil;

/**
 * 계좌 이체 화면·API.
 */
@Controller
public class TransferController {

    /** 상대 은행 코드 (화면 선택 박스) */
    private static final Map<String, String> BANKS = new LinkedHashMap<String, String>();

    static {
        BANKS.put(TransferService.OWN_BANK, "NovaBank (당행)");
        BANKS.put("KDB", "한국산업은행");
        BANKS.put("IBK", "기업은행");
        BANKS.put("NH", "농협은행");
        BANKS.put("SC", "SC제일은행");
        BANKS.put("CITI", "씨티은행");
    }

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountService accountService;

    @RequestMapping(value = "/transfer", method = RequestMethod.GET)
    public String form(Model model) {
        model.addAttribute("accounts", accountService.findAll());
        model.addAttribute("banks", BANKS);
        return "transfer";
    }

    @RequestMapping(value = "/transfer", method = RequestMethod.POST)
    public String transfer(@RequestParam("fromAccountNo") String fromAccountNo,
                           @RequestParam("toBank") String toBank,
                           @RequestParam("toAccountNo") String toAccountNo,
                           @RequestParam("amount") String amount,
                           @RequestParam(value = "memo", required = false) String memo,
                           Model model) {
        TransferResult result = transferService.transfer(fromAccountNo, toAccountNo, toBank, MoneyUtil.parse(amount), memo);
        model.addAttribute("result", result);
        model.addAttribute("accounts", accountService.findAll());
        model.addAttribute("banks", BANKS);
        model.addAttribute("message", "이체가 완료됐습니다. 거래번호 " + result.getTxId());
        return "transfer";
    }

    /** 수수료 미리보기 */
    @RequestMapping(value = "/api/transfer/fee", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Map<String, Object> apiFee(@RequestParam("toBank") String toBank,
                                      @RequestParam("amount") String amount) {
        BigDecimal amt = MoneyUtil.parse(amount);
        Date now = new Date();
        BigDecimal fee = transferService.calculateFee(toBank, amt, now);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("toBank", TransferService.isOwnBank(toBank) ? TransferService.OWN_BANK : toBank.toUpperCase());
        body.put("amount", amt);
        body.put("fee", fee);
        body.put("totalDebit", amt.add(fee));
        body.put("businessHours", DateUtil.isBusinessHours(now));
        return body;
    }
}
