// Token tabanlı kart yönetim servisi
// GÜVENLİK: CVV hiçbir zaman kaydedilmez (PCI DSS)
// GÜVENLİK: Kart numarası yerine sadece son 4 hane saklanır
// GÜVENLİK: API yanıtlarında providerToken (pm_xxx) asla frontend'e gönderilmez
package com.shop.ecommerce.services;

import com.shop.ecommerce.dto.card.PaymentCardCreateDTO;
import com.shop.ecommerce.dto.card.PaymentCardDTO;
import com.shop.ecommerce.entities.PaymentCard;
import com.shop.ecommerce.entities.User;
import com.shop.ecommerce.enums.PaymentProvider;
import com.shop.ecommerce.repository.PaymentCardRepository;
import com.shop.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;

    /**
     * Kullanıcının aktif kartlarını getirir
     * GÜVENLİK: Token bilgisi DTO'ya eklenmez, frontend'e gitmez
     */
    public List<PaymentCardDTO> getCardsByUserId(Long userId) {
        return paymentCardRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Token tabanlı kart ekleme
     * GÜVENLİK: Frontend'den CVV veya tam kart numarası GELMEZ
     * Sadece Stripe token (pm_xxx) + son 4 hane kaydedilir
     */
    @Transactional
    public PaymentCardDTO addCard(PaymentCardCreateDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        PaymentCard card = new PaymentCard();
        card.setUser(user);
        card.setCardHolderName(dto.getCardHolderName());

        // GÜVENLİK: Sadece son 4 hane kaydedilir
        card.setCardNumber(dto.getLastFour());

        card.setExpiryMonth(dto.getExpiryMonth());
        card.setExpiryYear(dto.getExpiryYear());
        card.setCardType(dto.getCardType());

        // Stripe token'ı kaydet
        card.setProviderToken(dto.getCardToken());

        // Provider belirle (varsayılan STRIPE)
        card.setPaymentProvider(
                dto.getPaymentProvider() != null
                        ? PaymentProvider.valueOf(dto.getPaymentProvider().toUpperCase())
                        : PaymentProvider.STRIPE
        );

        card.setIsActive(true);

        card = paymentCardRepository.save(card);
        return toDTO(card);
    }

    /**
     * Kartı deaktif et (soft delete)
     */
    @Transactional
    public void deleteCard(Long cardId, Long userId) {
        PaymentCard card = paymentCardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Kart bulunamadı"));
        if (!card.getUser().getId().equals(userId)) {
            throw new RuntimeException("Yetkisiz: Başka kullanıcının kartını silemezsiniz");
        }
        // Soft delete — kartı deaktif et
        card.setIsActive(false);
        paymentCardRepository.save(card);
    }

    /**
     * Entity → DTO dönüşümü
     * GÜVENLİK: providerToken ASLA DTO'ya eklenmez
     */
    private PaymentCardDTO toDTO(PaymentCard card) {
        PaymentCardDTO dto = new PaymentCardDTO();
        dto.setId(card.getId());
        dto.setCardHolderName(card.getCardHolderName());
        dto.setLastFour(card.getCardNumber());
        dto.setMaskedCardNumber("**** **** **** " + card.getCardNumber());
        dto.setExpiryMonth(card.getExpiryMonth());
        dto.setExpiryYear(card.getExpiryYear());
        dto.setCardType(card.getCardType());
        dto.setPaymentProvider(card.getPaymentProvider() != null ? card.getPaymentProvider().name() : "STRIPE");
        return dto;
    }
}
