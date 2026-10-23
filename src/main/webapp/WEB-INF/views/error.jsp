<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="오류" scope="request" />
<%@ include file="common/header.jspf" %>

<div class="alert alert-error">
    <h2>${title}</h2>
    <p>${detail}</p>
    <p class="hint">요청: ${uri}</p>
    <a class="btn-link" href="javascript:history.back()">이전 화면으로</a>
</div>

<%@ include file="common/footer.jspf" %>
