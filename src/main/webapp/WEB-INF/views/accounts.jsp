<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="계좌 관리" scope="request" />
<%@ include file="common/header.jspf" %>

<h2>계좌 관리</h2>

<div class="panel">
    <h3>신규 계좌 개설</h3>
    <form method="post" action="${ctx}/accounts" class="form-grid">
        <label>예금주 <input type="text" name="customerName" required maxlength="40"></label>
        <label>예금 종류
            <select name="type">
                <c:forEach var="t" items="${types}">
                    <option value="${t.code}">${t.label}</option>
                </c:forEach>
            </select>
        </label>
        <label>최초 입금액(원) <input type="text" name="initialDeposit" value="0" class="money"></label>
        <button type="submit">개설</button>
    </form>
    <p class="hint">정기예금은 최초 입금액 1,000,000원 이상.</p>
</div>

<table class="grid">
    <thead><tr><th>계좌번호</th><th>예금주</th><th>종류</th><th>잔액</th><th>상태</th><th>개설일</th><th></th></tr></thead>
    <tbody>
    <c:forEach var="a" items="${accounts}">
        <tr class="status-${a.status}">
            <td><a href="${ctx}/accounts/${a.accountNo}"><strong>${a.accountNo}</strong></a></td>
            <td>${a.customerName}</td>
            <td>${a.type.label}</td>
            <td class="num"><fmt:formatNumber value="${a.balance}" type="number" /></td>
            <td><span class="badge badge-${a.status}">${a.status.label}</span></td>
            <td><fmt:formatDate value="${a.openedAt}" pattern="yyyy-MM-dd" /></td>
            <td><a class="btn-link" href="${ctx}/accounts/${a.accountNo}">거래</a></td>
        </tr>
    </c:forEach>
    </tbody>
</table>

<%@ include file="common/footer.jspf" %>
