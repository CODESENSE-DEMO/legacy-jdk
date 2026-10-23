package com.novabank.ops.web;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.novabank.ops.report.DailyCloseReport;
import com.novabank.ops.scheduler.CloseScheduler;
import com.novabank.ops.service.DailyCloseService;
import com.novabank.ops.util.DateUtil;

/**
 * 일마감 리포트 화면·API (XML 은 본점 집계 연동 형식).
 */
@Controller
public class CloseController {

    @Autowired
    private DailyCloseService dailyCloseService;

    @Autowired
    private CloseScheduler closeScheduler;

    @RequestMapping(value = "/close", method = RequestMethod.GET)
    public String view(@RequestParam(value = "date", required = false) String date, Model model) {
        Date target = date == null || date.isEmpty() ? new Date() : DateUtil.parseDate(date);
        model.addAttribute("report", dailyCloseService.build(target));
        model.addAttribute("date", DateUtil.formatDate(target));
        model.addAttribute("lastRunAt", closeScheduler.getLastRunAt());
        return "close";
    }

    @RequestMapping(value = "/api/close.xml", method = RequestMethod.GET, produces = "application/xml;charset=UTF-8")
    @ResponseBody
    public String apiXml(@RequestParam(value = "date", required = false) String date) {
        Date target = date == null || date.isEmpty() ? new Date() : DateUtil.parseDate(date);
        return dailyCloseService.toXml(dailyCloseService.build(target));
    }

    @RequestMapping(value = "/api/close.json", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public DailyCloseReport apiJson(@RequestParam(value = "date", required = false) String date) {
        Date target = date == null || date.isEmpty() ? new Date() : DateUtil.parseDate(date);
        return dailyCloseService.build(target);
    }
}
