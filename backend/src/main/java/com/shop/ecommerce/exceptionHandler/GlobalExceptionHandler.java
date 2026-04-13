// Global hata yakalayıcı
// Stripe, PayPal, Coinbase ve Kapıda Ödeme hataları dahil tüm API hatalarını yakalar
// Standart JSON formatında hata yanıtı döner: { "error": "...", "message": "...", "status": 400 }
package com.shop.ecommerce.exceptionHandler;

import com.stripe.exception.StripeException;
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

    // Mevcut validation hatası handler'ı
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

    // Stripe ödeme hatası → 402 Payment Required
    @ExceptionHandler(StripeException.class)
    public ResponseEntity<Map<String, Object>> handleStripeException(StripeException e) {
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("error", "StripeException");
        errorBody.put("message", "Stripe ödeme hatası: " + e.getMessage());
        errorBody.put("status", 402);
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(errorBody);
    }

    // Genel ödeme hatası → 402 Payment Required
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentException(PaymentException e) {
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("error", "PaymentError");
        errorBody.put("message", e.getMessage());
        errorBody.put("status", 402);
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(errorBody);
    }

    // Kapıda ödeme limit aşımı → 400 Bad Request
    @ExceptionHandler(CodLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleCodLimitExceeded(CodLimitExceededException e) {
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("error", "CodLimitExceeded");
        errorBody.put("message", e.getMessage());
        errorBody.put("status", 400);
        return ResponseEntity.badRequest().body(errorBody);
    }

    // IllegalArgumentException → 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("error", "BadRequest");
        errorBody.put("message", e.getMessage());
        errorBody.put("status", 400);
        return ResponseEntity.badRequest().body(errorBody);
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
