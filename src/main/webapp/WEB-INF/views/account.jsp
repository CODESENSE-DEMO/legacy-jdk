<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="계좌 상세" scope="request" />
<%@ include file="common/header.jspf" %>

<h2>${account.accountNo} <small>${account.customerName} &middot; ${account.type.label}</small></h2>

<table class="kv">
    <tr><th>잔액</th><td><strong><fmt:formatNumber value="${account.balance}" type="number" /> 원</strong></td></tr>
    <tr><th>상태</th><td><span class="badge badge-${account.status}">${account.status.label}</span></td></tr>
    <tr><th>개설일</th><td><fmt:formatDate value="${account.openedAt}" pattern="yyyy-MM-dd" /></td></tr>
    <c:if test="${account.closedAt != null}">
        <tr><th>해지일</th><td><fmt:formatDate value="${account.closedAt}" pattern="yyyy-MM-dd" /></td></tr>
    </c:if>
</table>

<div class="panel-row">
    <div class="panel">
        <h3>입금</h3>
        <form method="post" action="${ctx}/accounts/${account.accountNo}/deposit" class="form-grid">
            <label>금액(원) <input type="text" name="amount" class="money" required></label>
            <label>적요 <input type="text" name="memo" maxlength="30"></label>
            <button type="submit">입금</button>
        </form>
    </div>
    <div class="panel">
        <h3>출금</h3>
        <form method="post" action="${ctx}/accounts/${account.accountNo}/withdraw" class="form-grid">
            <label>금액(원) <input type="text" name="amount" class="money" required></label>
            <label>적요 <input type="text" name="memo" maxlength="30"></label>
            <button type="submit" class="danger">출금</button>
        </form>
        <p class="hint">1회 출금 한도 10,000,000원</p>
    </div>
    <div class="panel">
        <h3>상태 변경</h3>
        <form method="post" action="${ctx}/accounts/${account.accountNo}/status" class="form-grid">
            <label>상태
                <select name="status">
                    <option value="ACTIVE">정상</option>
                    <option value="FROZEN">거래정지</option>
                    <option value="CLOSED">해지</option>
                </select>
            </label>
            <button type="submit" class="small">변경</button>
        </form>
        <p class="hint">해지는 잔액 0원일 때만 가능</p>
    </div>
</div>

<h3>거래 내역 <small>${fn:length(transactions)}건</small></h3>
<table class="grid">
    <thead><tr><th>거래번호</th><th>종류</th><th>금액</th><th>거래 후 잔액</th><th>상대</th><th>적요</th><th>시각</th></tr></thead>
    <tbody>
    <c:forEach var="t" items="${transactions}">
        <tr>
            <td><code>${t.txId}</code></td>
            <td><span class="badge badge-${t.type}">${t.type.label}</span></td>
            <td class="num ${t.type.credit ? 'credit' : 'debit'}"><c:if test="${!t.type.credit}">-</c:if><fmt:formatNumber value="${t.amount}" type="number" /></td>
            <td class="num"><fmt:formatNumber value="${t.balanceAfter}" type="number" /></td>
            <td>${t.counterparty}</td>
            <td>${t.memo}</td>
            <td><fmt:formatDate value="${t.createdAt}" pattern="yyyy-MM-dd HH:mm:ss" /></td>
        </tr>
    </c:forEach>
    <c:if test="${empty transactions}">
        <tr><td colspan="7" class="empty">거래 내역이 없습니다.</td></tr>
    </c:if>
    </tbody>
</table>

<%@ include file="common/footer.jspf" %>
