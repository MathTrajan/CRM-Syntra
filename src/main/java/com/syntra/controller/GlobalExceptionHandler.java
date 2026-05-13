package com.syntra.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        HttpStatus status = isNotFound(ex.getMessage()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return isApiRequest(request) ? json(status, ex.getMessage()) : view(status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Dados inválidos.");

        return isApiRequest(request) ? json(HttpStatus.BAD_REQUEST, message) : view(HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Object handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return isApiRequest(request) ? json(HttpStatus.BAD_REQUEST, "Requisição inválida.") : view(HttpStatus.BAD_REQUEST);
    }

    private boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI() != null && request.getRequestURI().startsWith("/api/");
    }

    private boolean isNotFound(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("não encontrado") || lower.contains("nao encontrado");
    }

    private ResponseEntity<Map<String, Object>> json(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("erro", message);
        return ResponseEntity.status(status).body(body);
    }

    private ModelAndView view(HttpStatus status) {
        ModelAndView mv = new ModelAndView();
        mv.setStatus(status);
        if (status == HttpStatus.NOT_FOUND) {
            mv.setViewName("error/404");
        } else if (status == HttpStatus.BAD_REQUEST) {
            mv.setViewName("error/400");
        } else {
            mv.setViewName("error/500");
        }
        return mv;
    }
}
