package com.novabank.ops.service;

import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.ops.domain.Account;
import com.novabank.ops.domain.AccountStatus;
import com.novabank.ops.domain.Transaction;
import com.novabank.ops.domain.TxType;
import com.novabank.ops.report.DailyCloseReport;
import com.novabank.ops.report.DailyCloseReport.TypeSummary;
import com.novabank.ops.util.DateUtil;

/**
 * 일마감 리포트 생성. 본점 전송은 XML, 화면은 JSON.
 */
@Service
public class DailyCloseService {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.branch.code:0917}")
    private String branchCode = "0917";

    public DailyCloseReport build(Date closeDate) {
        DailyCloseReport report = new DailyCloseReport();
        report.setCloseDate(DateUtil.formatDate(closeDate));
        report.setGeneratedAt(DateUtil.formatDateTime(new Date()));
        report.setBranchCode(branchCode);

        for (Account a : accountService.findAll()) {
            report.setAccountCount(report.getAccountCount() + 1);
            if (a.getStatus() == AccountStatus.ACTIVE) {
                report.setActiveAccountCount(report.getActiveAccountCount() + 1);
            }
        }
        report.setTotalBalance(accountService.totalBalance());

        Map<TxType, TypeSummary> byType = new LinkedHashMap<TxType, TypeSummary>();
        for (TxType t : TxType.values()) {
            byType.put(t, new TypeSummary(t.name(), t.getLabel()));
        }
        for (Transaction tx : transactionService.findAll()) {
            if (!DateUtil.isSameDay(closeDate, tx.getCreatedAt())) {
                continue;
            }
            report.setTransactionCount(report.getTransactionCount() + 1);
            TypeSummary summary = byType.get(tx.getType());
            summary.setCount(summary.getCount() + 1);
            summary.setAmount(summary.getAmount().add(tx.getAmount()));
            switch (tx.getType()) {
                case DEPOSIT:
                    report.setDepositAmount(report.getDepositAmount().add(tx.getAmount()));
                    break;
                case WITHDRAW:
                    report.setWithdrawAmount(report.getWithdrawAmount().add(tx.getAmount()));
                    break;
                case TRANSFER_OUT:
                    report.setTransferOutAmount(report.getTransferOutAmount().add(tx.getAmount()));
                    break;
                case FEE:
                    report.setFeeAmount(report.getFeeAmount().add(tx.getAmount()));
                    break;
                default:
                    break;
            }
        }
        report.getByType().addAll(byType.values());
        return report;
    }

    public String toXml(DailyCloseReport report) {
        try {
            JAXBContext context = JAXBContext.newInstance(DailyCloseReport.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            StringWriter writer = new StringWriter();
            marshaller.marshal(report, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new IllegalStateException("일마감 XML 변환 실패", e);
        }
    }

    public String toJson(DailyCloseReport report) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("일마감 JSON 변환 실패", e);
        }
    }
}
