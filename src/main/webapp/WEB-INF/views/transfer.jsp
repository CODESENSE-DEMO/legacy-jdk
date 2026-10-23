<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="계좌 이체" scope="request" />
<%@ include file="common/header.jspf" %>

<h2>계좌 이체</h2>

<div class="panel">
    <form method="post" action="${ctx}/transfer" class="form-grid" id="transferForm" data-fee-url="${ctx}/api/transfer/fee">
        <label>출금 계좌
            <select name="fromAccountNo" id="fromAccountNo" required>
                <c:forEach var="a" items="${accounts}">
                    <c:if test="${a.status == 'ACTIVE'}">
                        <option value="${a.accountNo}">${a.accountNo} ${a.maskedName} (잔액 <fmt:formatNumber value="${a.balance}" type="number" />)</option>
                    </c:if>
                </c:forEach>
            </select>
        </label>
        <label>입금 은행
            <select name="toBank" id="toBank">
                <c:forEach var="b" items="${banks}">
                    <option value="${b.key}">${b.value}</option>
                </c:forEach>
            </select>
        </label>
        <label>입금 계좌번호 <input type="text" name="toAccountNo" id="toAccountNo" required placeholder="110-0917-000002"></label>
        <label>이체 금액(원) <input type="text" name="amount" id="amount" class="money" required></label>
        <label>적요 <input type="text" name="memo" maxlength="30"></label>
        <button type="submit">이체</button>
    </form>
    <div id="feePreview" class="hint"></div>
    <p class="hint">1회 한도 50,000,000원 &middot; 1일 한도 100,000,000원 &middot; 당행 수수료 없음 &middot; 타행 500/800/1,500원(금액 구간), 영업시간 외 +300원</p>
</div>

<c:if test="${result != null}">
    <h3>이체 결과</h3>
    <table class="kv">
        <tr><th>거래번호</th><td><code>${result.txId}</code></td></tr>
        <tr><th>출금 계좌</th><td>${result.fromAccountNo}</td></tr>
        <tr><th>입금 계좌</th><td>${result.toBank} ${result.toAccountNo}</td></tr>
        <tr><th>이체 금액</th><td><fmt:formatNumber value="${result.amount}" type="number" /> 원</td></tr>
        <tr><th>수수료</th><td><fmt:formatNumber value="${result.fee}" type="number" /> 원</td></tr>
        <tr class="total"><th>출금 합계</th><td><strong><fmt:formatNumber value="${result.totalDebit}" type="number" /> 원</strong></td></tr>
        <tr><th>출금 후 잔액</th><td><fmt:formatNumber value="${result.balanceAfter}" type="number" /> 원</td></tr>
        <tr><th>처리 시각</th><td><fmt:formatDate value="${result.executedAt}" pattern="yyyy-MM-dd HH:mm:ss" /></td></tr>
    </table>
</c:if>

<%@ include file="common/footer.jspf" %>
