package com.novabank.ops.service;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.novabank.ops.domain.Account;
import com.novabank.ops.domain.Transaction;
import com.novabank.ops.domain.TransferResult;
import com.novabank.ops.domain.TxType;
import com.novabank.ops.util.DateUtil;
import com.novabank.ops.util.MoneyUtil;

/**
 * 계좌 이체. 당행(NovaBank) 이체는 수수료 없음·즉시 입금, 타행 이체는 금액 구간별 수수료
 * + 영업시간 외 가산. 1회·1일 한도를 검사한다.
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    public static final String OWN_BANK = "NOVA";

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Value("${transfer.limit.per.tx:50000000}")
    private long limitPerTx = 50000000L;

    @Value("${transfer.limit.per.day:100000000}")
    private long limitPerDay = 100000000L;

    @Value("${transfer.fee.other.bank.under.100000:500}")
    private long feeUnder100k = 500L;

    @Value("${transfer.fee.other.bank.under.1000000:800}")
    private long feeUnder1m = 800L;

    @Value("${transfer.fee.other.bank.over:1500}")
    private long feeOver = 1500L;

    @Value("${transfer.fee.after.hours.surcharge:300}")
    private long afterHoursSurcharge = 300L;

    public synchronized TransferResult transfer(String fromAccountNo, String toAccountNo, String toBank,
                                                BigDecimal amount, String memo) {
        TransactionService.requirePositive(amount);
        if (StringUtils.isBlank(toAccountNo)) {
            throw new IllegalArgumentException("입금 계좌번호는 필수입니다.");
        }
        if (amount.compareTo(MoneyUtil.won(limitPerTx)) > 0) {
            throw new IllegalArgumentException("1회 이체 한도를 초과했습니다. 한도 " + limitPerTx + "원");
        }

        Account from = accountService.getActiveAccount(fromAccountNo);
        boolean ownBank = isOwnBank(toBank);
        if (ownBank && from.getAccountNo().equals(toAccountNo.trim())) {
            throw new IllegalArgumentException("출금 계좌와 입금 계좌가 같습니다.");
        }
        Account to = ownBank ? accountService.getActiveAccount(toAccountNo) : null;

        BigDecimal todayTotal = transferredToday(from.getAccountNo(), new Date());
        if (todayTotal.add(amount).compareTo(MoneyUtil.won(limitPerDay)) > 0) {
            throw new IllegalArgumentException("1일 이체 한도를 초과했습니다. 오늘 누계 " + todayTotal + "원, 한도 " + limitPerDay + "원");
        }

        Date now = new Date();
        BigDecimal fee = calculateFee(toBank, amount, now);
        BigDecimal totalDebit = amount.add(fee);
        if (from.getBalance().compareTo(totalDebit) < 0) {
            throw new IllegalArgumentException("잔액이 부족합니다(수수료 포함 " + totalDebit + "원). 잔액 " + from.getBalance() + "원");
        }

        String counterparty = ownBank ? OWN_BANK + " " + to.getAccountNo() : toBank.trim().toUpperCase() + " " + toAccountNo.trim();
        BigDecimal fromAfter = from.getBalance().subtract(amount);
        accountService.applyBalance(from, fromAfter);
        Transaction out = transactionService.record(from, TxType.TRANSFER_OUT, amount, fromAfter, counterparty, memo);
        if (fee.signum() > 0) {
            fromAfter = fromAfter.subtract(fee);
            accountService.applyBalance(from, fromAfter);
            transactionService.record(from, TxType.FEE, fee, fromAfter, counterparty, "이체 수수료");
        }
        if (to != null) {
            BigDecimal toAfter = to.getBalance().add(amount);
            accountService.applyBalance(to, toAfter);
            transactionService.record(to, TxType.TRANSFER_IN, amount, toAfter, OWN_BANK + " " + from.getAccountNo(), memo);
        }

        TransferResult result = new TransferResult();
        result.setTxId(out.getTxId());
        result.setFromAccountNo(from.getAccountNo());
        result.setToAccountNo(toAccountNo.trim());
        result.setToBank(ownBank ? OWN_BANK : toBank.trim().toUpperCase());
        result.setAmount(amount);
        result.setFee(fee);
        result.setTotalDebit(totalDebit);
        result.setBalanceAfter(fromAfter);
        result.setExecutedAt(now);
        log.info("이체 {} {} -> {} {}원 (수수료 {})", out.getTxId(), from.getAccountNo(), counterparty, amount, fee);
        return result;
    }

    /** 타행 이체 수수료. 당행은 0 */
    public BigDecimal calculateFee(String toBank, BigDecimal amount, Date at) {
        if (isOwnBank(toBank)) {
            return BigDecimal.ZERO;
        }
        long fee;
        if (amount.compareTo(MoneyUtil.won(100000)) < 0) {
            fee = feeUnder100k;
        } else if (amount.compareTo(MoneyUtil.won(1000000)) < 0) {
            fee = feeUnder1m;
        } else {
            fee = feeOver;
        }
        if (!DateUtil.isBusinessHours(at)) {
            fee += afterHoursSurcharge;
        }
        return MoneyUtil.won(fee);
    }

    public static boolean isOwnBank(String bankCode) {
        return StringUtils.isBlank(bankCode) || OWN_BANK.equalsIgnoreCase(bankCode.trim());
    }

    /** 오늘 이체출금 누계 */
    public BigDecimal transferredToday(String accountNo, Date today) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Transaction tx : transactionService.history(accountNo)) {
            if (tx.getType() == TxType.TRANSFER_OUT && DateUtil.isSameDay(today, tx.getCreatedAt())) {
                sum = sum.add(tx.getAmount());
            }
        }
        return sum;
    }
}
