package com.novabank.ops.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.novabank.ops.domain.Account;
import com.novabank.ops.domain.AccountType;
import com.novabank.ops.domain.Transaction;
import com.novabank.ops.service.AccountService;
import com.novabank.ops.service.TransactionService;
import com.novabank.ops.util.MoneyUtil;

/**
 * 계좌 관리·입출금 화면과 API.
 */
@Controller
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @RequestMapping(value = "/accounts", method = RequestMethod.GET)
    public String list(Model model) {
        model.addAttribute("accounts", accountService.findAll());
        model.addAttribute("types", AccountType.values());
        return "accounts";
    }

    @RequestMapping(value = "/accounts", method = RequestMethod.POST)
    public String open(@RequestParam("customerName") String customerName,
                       @RequestParam("type") String type,
                       @RequestParam(value = "initialDeposit", required = false) String initialDeposit,
                       RedirectAttributes redirect) {
        Account account = accountService.open(customerName, type, MoneyUtil.parse(initialDeposit));
        redirect.addFlashAttribute("message", "계좌를 개설했습니다: " + account.getAccountNo());
        return "redirect:/accounts/" + account.getAccountNo();
    }

    @RequestMapping(value = "/accounts/{accountNo}", method = RequestMethod.GET)
    public String detail(@PathVariable("accountNo") String accountNo, Model model) {
        model.addAttribute("account", accountService.getAccount(accountNo));
        model.addAttribute("transactions", transactionService.history(accountNo));
        return "account";
    }

    @RequestMapping(value = "/accounts/{accountNo}/status", method = RequestMethod.POST)
    public String updateStatus(@PathVariable("accountNo") String accountNo,
                               @RequestParam("status") String status,
                               RedirectAttributes redirect) {
        Account account = accountService.updateStatus(accountNo, status);
        redirect.addFlashAttribute("message", account.getAccountNo() + " 상태를 " + account.getStatus().getLabel() + "(으)로 변경했습니다.");
        return "redirect:/accounts/" + accountNo;
    }

    @RequestMapping(value = "/accounts/{accountNo}/deposit", method = RequestMethod.POST)
    public String deposit(@PathVariable("accountNo") String accountNo,
                          @RequestParam("amount") String amount,
                          @RequestParam(value = "memo", required = false) String memo,
                          RedirectAttributes redirect) {
        Transaction tx = transactionService.deposit(accountNo, MoneyUtil.parse(amount), memo);
        redirect.addFlashAttribute("message", "입금 완료 (거래번호 " + tx.getTxId() + ")");
        return "redirect:/accounts/" + accountNo;
    }

    @RequestMapping(value = "/accounts/{accountNo}/withdraw", method = RequestMethod.POST)
    public String withdraw(@PathVariable("accountNo") String accountNo,
                           @RequestParam("amount") String amount,
                           @RequestParam(value = "memo", required = false) String memo,
                           RedirectAttributes redirect) {
        Transaction tx = transactionService.withdraw(accountNo, MoneyUtil.parse(amount), memo);
        redirect.addFlashAttribute("message", "출금 완료 (거래번호 " + tx.getTxId() + ")");
        return "redirect:/accounts/" + accountNo;
    }

    @RequestMapping(value = "/api/accounts", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public List<Account> apiList() {
        return accountService.findAll();
    }

    @RequestMapping(value = "/api/accounts/{accountNo}", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Account apiGet(@PathVariable("accountNo") String accountNo) {
        return accountService.getAccount(accountNo);
    }

    @RequestMapping(value = "/api/accounts/{accountNo}/transactions", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public List<Transaction> apiTransactions(@PathVariable("accountNo") String accountNo) {
        accountService.getAccount(accountNo);
        return transactionService.history(accountNo);
    }
}
