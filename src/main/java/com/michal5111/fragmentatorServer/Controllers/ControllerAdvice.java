package com.michal5111.fragmentatorServer.Controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ControllerAdvice {

    @ExceptionHandler(Throwable.class)
    public Map<String, Object> forbiddenHandler(Throwable throwable, WebRequest request, HttpServletResponse response) {
        Map<String, Object> map = new HashMap<>();
        int status = getStatusFromAnnotation(throwable).value();
        response.setStatus(status);
        map.put("timestamp", new Timestamp(System.currentTimeMillis()));
        map.put("status", response.getStatus());
        map.put("error", throwable.getClass().getSimpleName());
        map.put("message", throwable.getMessage());
        map.put("path", request.getContextPath());
        return map;
    }

    private HttpStatus getStatusFromAnnotation(Throwable throwable) {
        ResponseStatus responseStatus = throwable.getClass().getAnnotation(ResponseStatus.class);
        if (responseStatus != null) {
            return responseStatus.value();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

}
