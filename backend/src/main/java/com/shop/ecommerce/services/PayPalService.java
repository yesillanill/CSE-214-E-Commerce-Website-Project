// PayPal Sandbox ödeme servisi
// PayPal REST API'ye doğrudan HTTP çağrısı yapar (SDK yerine RestTemplate kullanır)
// GÜVENLİK: Client secret server-side tutulur, frontend'e açılmaz
package com.shop.ecommerce.services;

import com.shop.ecommerce.config.PayPalConfig;
import com.shop.ecommerce.dto.payment.PaymentRequest;
import com.shop.ecommerce.dto.payment.PaymentResponse;
import com.shop.ecommerce.exceptionHandler.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayPalService {

    private final PayPalConfig payPalConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * PayPal OAuth2 access token alır
     * GÜVENLİK: Client ID ve Secret sadece server-side kullanılır
     */
    public String getAccessToken() {
        try {
            String url = payPalConfig.getBaseUrl() + "/v1/oauth2/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(payPalConfig.getClientId(), payPalConfig.getClientSecret());
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> entity = new HttpEntity<>("grant_type=client_credentials", headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
            throw new PaymentException("PayPal access token alınamadı");
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("PayPal access token hatası: {}", e.getMessage());
            throw new PaymentException("PayPal bağlantı hatası: " + e.getMessage(), e);
        }
    }

    /**
     * PayPal siparişi oluşturur ve onay URL'i döner
     */
    public PaymentResponse createOrder(PaymentRequest request) {
        try {
            String accessToken = getAccessToken();
            String url = payPalConfig.getBaseUrl() + "/v2/checkout/orders";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // PayPal order body oluştur
            Map<String, Object> orderBody = new LinkedHashMap<>();
            orderBody.put("intent", "CAPTURE");

            Map<String, Object> amount = new LinkedHashMap<>();
            amount.put("currency_code", request.getCurrency() != null ? request.getCurrency() : "USD");
            amount.put("value", String.format("%.2f", request.getAmount()));

            Map<String, Object> purchaseUnit = new LinkedHashMap<>();
            purchaseUnit.put("amount", amount);
            purchaseUnit.put("description", request.getDescription() != null ? request.getDescription() : "E-Commerce Ödeme");

            orderBody.put("purchase_units", List.of(purchaseUnit));

            // Return URL'leri ekle
            Map<String, String> applicationContext = new LinkedHashMap<>();
            applicationContext.put("return_url", "http://localhost:4200/payment?paypal=success");
            applicationContext.put("cancel_url", "http://localhost:4200/payment?paypal=cancel");
            orderBody.put("application_context", applicationContext);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(orderBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getBody() != null) {
                String orderId = (String) response.getBody().get("id");
                String status = (String) response.getBody().get("status");

                // Approval URL'ini bul
                String approvalUrl = "";
                List<Map<String, String>> links = (List<Map<String, String>>) response.getBody().get("links");
                if (links != null) {
                    approvalUrl = links.stream()
                            .filter(link -> "approve".equals(link.get("rel")))
                            .map(link -> link.get("href"))
                            .findFirst()
                            .orElse("");
                }

                log.info("PayPal order oluşturuldu: {}", orderId);

                return PaymentResponse.builder()
                        .transactionId(orderId)
                        .status(status)
                        .approvalUrl(approvalUrl)
                        .message("PayPal ödeme sayfasına yönlendiriliyorsunuz")
                        .build();
            }
            throw new PaymentException("PayPal order oluşturulamadı");
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("PayPal order oluşturma hatası: {}", e.getMessage());
            throw new PaymentException("PayPal ödeme hatası: " + e.getMessage(), e);
        }
    }

    /**
     * PayPal siparişini onaylar (capture)
     * Frontend'den dönen orderId ile ödeme tamamlanır
     */
    public PaymentResponse captureOrder(String orderId) {
        try {
            String accessToken = getAccessToken();
            String url = payPalConfig.getBaseUrl() + "/v2/checkout/orders/" + orderId + "/capture";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>("", headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getBody() != null) {
                String status = (String) response.getBody().get("status");
                log.info("PayPal order capture edildi: {} - Status: {}", orderId, status);

                return PaymentResponse.builder()
                        .transactionId(orderId)
                        .status(status)
                        .providerResponse(status)
                        .message("COMPLETED".equals(status) ? "PayPal ödemesi başarıyla tamamlandı" : "PayPal ödeme durumu: " + status)
                        .build();
            }
            throw new PaymentException("PayPal ödeme onayı alınamadı");
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("PayPal capture hatası: {}", e.getMessage());
            throw new PaymentException("PayPal ödeme onay hatası: " + e.getMessage(), e);
        }
    }
}
