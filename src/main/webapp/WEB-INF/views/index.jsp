<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="대시보드" scope="request" />
<%@ include file="common/header.jspf" %>

<h2>오늘의 영업 현황 <small><fmt:formatDate value="${today}" pattern="yyyy-MM-dd (E)" /> &middot; 지점 ${report.branchCode}</small></h2>

<div class="cards">
    <div class="card">
        <div class="card-label">관리 계좌</div>
        <div class="card-value">${report.accountCount}<small>좌</small></div>
        <div class="card-sub">정상 ${report.activeAccountCount}좌</div>
    </div>
    <div class="card">
        <div class="card-label">총 잔액</div>
        <div class="card-value"><fmt:formatNumber value="${report.totalBalance}" type="number" /><small>원</small></div>
        <div class="card-sub">해지 계좌 제외</div>
    </div>
    <div class="card">
        <div class="card-label">오늘 거래</div>
        <div class="card-value">${report.transactionCount}<small>건</small></div>
        <div class="card-sub">입금 <fmt:formatNumber value="${report.depositAmount}" type="number" /> &middot; 출금 <fmt:formatNumber value="${report.withdrawAmount}" type="number" /></div>
    </div>
    <div class="card">
        <div class="card-label">오늘 이체·수수료</div>
        <div class="card-value"><fmt:formatNumber value="${report.transferOutAmount}" type="number" /><small>원</small></div>
        <div class="card-sub">수수료 수입 <fmt:formatNumber value="${report.feeAmount}" type="number" />원</div>
    </div>
</div>

<h3>계좌 요약</h3>
<table class="grid">
    <thead><tr><th>계좌번호</th><th>예금주</th><th>종류</th><th>잔액</th><th>상태</th></tr></thead>
    <tbody>
    <c:forEach var="a" items="${accounts}">
        <tr class="status-${a.status}">
            <td><a href="${ctx}/accounts/${a.accountNo}">${a.accountNo}</a></td>
            <td>${a.maskedName}</td>
            <td>${a.type.label}</td>
            <td class="num"><fmt:formatNumber value="${a.balance}" type="number" /></td>
            <td><span class="badge badge-${a.status}">${a.status.label}</span></td>
        </tr>
    </c:forEach>
    </tbody>
</table>

<h3>최근 거래</h3>
<table class="grid">
    <thead><tr><th>거래번호</th><th>계좌</th><th>종류</th><th>금액</th><th>거래 후 잔액</th><th>상대</th><th>시각</th></tr></thead>
    <tbody>
    <c:forEach var="t" items="${recent}">
        <tr>
            <td><code>${t.txId}</code></td>
            <td><a href="${ctx}/accounts/${t.accountNo}">${t.accountNo}</a></td>
            <td><span class="badge badge-${t.type}">${t.type.label}</span></td>
            <td class="num"><fmt:formatNumber value="${t.amount}" type="number" /></td>
            <td class="num"><fmt:formatNumber value="${t.balanceAfter}" type="number" /></td>
            <td>${t.counterparty}</td>
            <td><fmt:formatDate value="${t.createdAt}" pattern="MM-dd HH:mm" /></td>
        </tr>
    </c:forEach>
    <c:if test="${empty recent}">
        <tr><td colspan="7" class="empty">오늘 거래가 없습니다.</td></tr>
    </c:if>
    </tbody>
</table>

<%@ include file="common/footer.jspf" %>
