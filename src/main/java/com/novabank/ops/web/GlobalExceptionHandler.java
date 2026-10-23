package com.novabank.ops.web;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * 입력 오류는 400 + 오류 화면, 그 밖은 500 + 오류 화면.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView badRequest(HttpServletRequest request, IllegalArgumentException e) {
        log.warn("잘못된 요청 {} : {}", request.getRequestURI(), e.getMessage());
        return errorView(request, "입력값을 확인해 주세요.", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView serverError(HttpServletRequest request, Exception e) {
        log.error("처리 중 오류 " + request.getRequestURI(), e);
        return errorView(request, "처리 중 오류가 발생했습니다.", e.getMessage());
    }

    private ModelAndView errorView(HttpServletRequest request, String title, String detail) {
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("title", title);
        mav.addObject("detail", detail);
        mav.addObject("uri", request.getRequestURI());
        return mav;
    }
}
