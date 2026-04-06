package com.shop.ecommerce.services;

import com.shop.ecommerce.dto.card.PaymentCardCreateDTO;
import com.shop.ecommerce.dto.card.PaymentCardDTO;
import com.shop.ecommerce.entities.PaymentCard;
import com.shop.ecommerce.entities.User;
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

    public List<PaymentCardDTO> getCardsByUserId(Long userId) {
        return paymentCardRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public PaymentCardDTO addCard(PaymentCardCreateDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        PaymentCard card = new PaymentCard();
        card.setUser(user);
        card.setCardHolderName(dto.getCardHolderName());
        card.setCardNumber(dto.getCardNumber());
        card.setExpiryMonth(dto.getExpiryMonth());
        card.setExpiryYear(dto.getExpiryYear());
        card.setCvv(dto.getCvv());
        card.setCardType(detectCardType(dto.getCardNumber()));

        card = paymentCardRepository.save(card);
        return toDTO(card);
    }

    @Transactional
    public void deleteCard(Long cardId, Long userId) {
        PaymentCard card = paymentCardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));
        if (!card.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: Cannot delete another user's card");
        }
        paymentCardRepository.delete(card);
    }

    private PaymentCardDTO toDTO(PaymentCard card) {
        PaymentCardDTO dto = new PaymentCardDTO();
        dto.setId(card.getId());
        dto.setCardHolderName(card.getCardHolderName());
        dto.setMaskedCardNumber(maskCardNumber(card.getCardNumber()));
        dto.setExpiryMonth(card.getExpiryMonth());
        dto.setExpiryYear(card.getExpiryYear());
        dto.setCardType(card.getCardType());
        return dto;
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        String last4 = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + last4;
    }

    private String detectCardType(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) return "UNKNOWN";
        if (cardNumber.startsWith("4")) return "VISA";
        if (cardNumber.startsWith("5")) return "MASTERCARD";
        if (cardNumber.startsWith("9")) return "TROY";
        return "OTHER";
    }
}
