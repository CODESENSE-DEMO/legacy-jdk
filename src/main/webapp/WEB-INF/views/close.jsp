<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="일마감" scope="request" />
<%@ include file="common/header.jspf" %>

<h2>일마감 리포트 <small>${report.closeDate} &middot; 지점 ${report.branchCode}</small></h2>

<form class="search" method="get" action="${ctx}/close">
    <label>마감일 <input type="text" name="date" value="${date}" placeholder="yyyy-MM-dd" size="10"></label>
    <button type="submit">조회</button>
    <a class="btn-link" href="${ctx}/api/close.xml?date=${date}" target="_blank">XML</a>
    <a class="btn-link" href="${ctx}/api/close.json?date=${date}" target="_blank">JSON</a>
</form>

<table class="kv">
    <tr><th>생성 시각</th><td>${report.generatedAt}</td></tr>
    <tr><th>계좌</th><td>${report.accountCount}좌 (정상 ${report.activeAccountCount})</td></tr>
    <tr><th>총 잔액</th><td><fmt:formatNumber value="${report.totalBalance}" type="number" /> 원</td></tr>
    <tr><th>거래 건수</th><td>${report.transactionCount}건</td></tr>
    <tr><th>입금 / 출금</th><td><fmt:formatNumber value="${report.depositAmount}" type="number" /> / <fmt:formatNumber value="${report.withdrawAmount}" type="number" /> 원</td></tr>
    <tr><th>이체출금 / 수수료</th><td><fmt:formatNumber value="${report.transferOutAmount}" type="number" /> / <fmt:formatNumber value="${report.feeAmount}" type="number" /> 원</td></tr>
    <tr><th>스케줄러 마지막 실행</th><td><fmt:formatDate value="${lastRunAt}" pattern="yyyy-MM-dd HH:mm:ss" /></td></tr>
</table>

<h3>거래 종류별</h3>
<table class="grid">
    <thead><tr><th>종류</th><th>건수</th><th>금액</th></tr></thead>
    <tbody>
    <c:forEach var="t" items="${report.byType}">
        <tr><td><span class="badge badge-${t.type}">${t.label}</span></td><td class="num">${t.count}</td><td class="num"><fmt:formatNumber value="${t.amount}" type="number" /></td></tr>
    </c:forEach>
    </tbody>
</table>

<%@ include file="common/footer.jspf" %>
