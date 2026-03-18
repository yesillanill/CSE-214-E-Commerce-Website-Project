package com.shop.ecommerce.exceptionHandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception, WebRequest request){
        Map<String, List<String>> errorsMap = new HashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String message = error.getDefaultMessage();
            errorsMap.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(message);
        });
        return ResponseEntity.badRequest().body(createApiError(request, exception.getMessage()));
    }

    private <E> ApiError<E> createApiError(WebRequest request, E message){
        ApiError<E> apiError = new ApiError<>();
        apiError.setStatus(HttpStatus.BAD_REQUEST.value());
        Exception<E> exception = new Exception<>();
        exception.setCreateTime(new Date());
        exception.setHostName(getHostName());
        exception.setPath(request.getDescription(false));
        exception.setMesaage(message);
        apiError.setException(exception);
        return apiError;
    }

    private String getHostName(){
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException exception){
            System.out.println("ERROR" + exception.getMessage());
        }
        return null;
    }
}
