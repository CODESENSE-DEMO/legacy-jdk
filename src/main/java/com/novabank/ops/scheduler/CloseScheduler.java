package com.novabank.ops.scheduler;

import java.util.Date;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.novabank.ops.report.DailyCloseReport;
import com.novabank.ops.service.DailyCloseService;

/**
 * 일마감 리포트를 주기적으로 만들어 마지막 결과를 보관한다. (본점 전송은 별도 배치)
 */
@Component
public class CloseScheduler {

    private static final Logger log = LoggerFactory.getLogger(CloseScheduler.class);

    @Autowired
    private DailyCloseService dailyCloseService;

    private volatile DailyCloseReport lastReport;
    private volatile Date lastRunAt;

    @PostConstruct
    public void init() {
        log.info("CloseScheduler 시작");
        run();
    }

    @PreDestroy
    public void shutdown() {
        log.info("CloseScheduler 종료 (마지막 실행 {})", lastRunAt);
    }

    @Scheduled(fixedDelayString = "${close.schedule.fixed.delay:60000}", initialDelay = 60000)
    public void run() {
        Date now = new Date();
        lastReport = dailyCloseService.build(now);
        lastRunAt = now;
        log.debug("일마감 리포트 갱신: 거래 {}건, 총잔액 {}", lastReport.getTransactionCount(), lastReport.getTotalBalance());
    }

    public DailyCloseReport getLastReport() {
        return lastReport;
    }

    public Date getLastRunAt() {
        return lastRunAt;
    }
}
