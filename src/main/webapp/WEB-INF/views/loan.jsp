<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="대출 계산기" scope="request" />
<%@ include file="common/header.jspf" %>

<h2>대출 상환 계산기</h2>

<div class="panel">
    <form method="post" action="${ctx}/loan" class="form-grid">
        <label>대출 원금(원) <input type="text" name="principal" value="${principal}" class="money" required></label>
        <label>연이율(%) <input type="text" name="rate" value="${rate}" size="6" required></label>
        <label>상환 개월 <input type="number" name="months" value="${months}" min="1" max="480" required></label>
        <label>상환 방식
            <select name="method">
                <option value="EQUAL_PAYMENT" <c:if test="${method == 'EQUAL_PAYMENT'}">selected</c:if>>원리금균등</option>
                <option value="EQUAL_PRINCIPAL" <c:if test="${method == 'EQUAL_PRINCIPAL'}">selected</c:if>>원금균등</option>
            </select>
        </label>
        <button type="submit">계산</button>
    </form>
</div>

<c:if test="${schedule != null}">
    <h3>계산 결과 <small>${schedule.methodLabel} &middot; ${schedule.months}개월 &middot; 연 ${schedule.annualRatePercent}%</small></h3>
    <table class="kv">
        <tr><th>대출 원금</th><td><fmt:formatNumber value="${schedule.principal}" type="number" /> 원</td></tr>
        <tr><th>총 이자</th><td><fmt:formatNumber value="${schedule.totalInterest}" type="number" /> 원</td></tr>
        <tr class="total"><th>총 상환액</th><td><strong><fmt:formatNumber value="${schedule.totalPayment}" type="number" /> 원</strong></td></tr>
    </table>
    <table class="grid">
        <thead><tr><th>회차</th><th>납입액</th><th>원금</th><th>이자</th><th>잔액</th></tr></thead>
        <tbody>
        <c:forEach var="i" items="${schedule.installments}">
            <tr>
                <td class="num">${i.month}</td>
                <td class="num"><fmt:formatNumber value="${i.payment}" type="number" /></td>
                <td class="num"><fmt:formatNumber value="${i.principal}" type="number" /></td>
                <td class="num"><fmt:formatNumber value="${i.interest}" type="number" /></td>
                <td class="num"><fmt:formatNumber value="${i.balance}" type="number" /></td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</c:if>

<%@ include file="common/footer.jspf" %>
