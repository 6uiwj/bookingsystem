package com.bloodDonation.commons;

import com.bloodDonation.commons.exceptions.AlertBackException;
import com.bloodDonation.commons.exceptions.AlertException;
import com.bloodDonation.commons.exceptions.AlertRedirectException;
import com.bloodDonation.commons.exceptions.CommonException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;

public interface ExceptionProcessor {

    @ExceptionHandler(Exception.class)
    default String errorHandler(Exception e, HttpServletResponse response, HttpServletRequest request, Model model){


        //모든 예외는 500으로 고정
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR; //500

        //e가 CommonException의 객체인지 체크
        if(e instanceof CommonException){
            CommonException commonException = (CommonException) e;
            status = commonException.getStatus();
        }

        response.setStatus(status.value());
        e.printStackTrace();

        if (e instanceof AlertException) { // 자바스크립트 Alert형태로 응답
            String script = String.format("alert('%s');", e.getMessage());

            if (e instanceof AlertBackException) { // history.back(); 실행
                script += "history.back();";
            }

            if(e instanceof AlertRedirectException) {
                AlertRedirectException alertRedirectException = (AlertRedirectException) e;

                script += String.format("%s.location.replace('%s');", alertRedirectException.getTarget(), alertRedirectException.getRedirectUrl());
            }

            model.addAttribute("script", script);
            return "common/_execute_script";

        }


        model.addAttribute("status",status.value());
        model.addAttribute("path", request.getRequestURL());
        model.addAttribute("method", request.getMethod());
        model.addAttribute("message", e.getMessage());

        return "error/common";
    }
}
