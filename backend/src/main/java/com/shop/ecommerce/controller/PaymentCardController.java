package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.card.PaymentCardCreateDTO;
import com.shop.ecommerce.dto.card.PaymentCardDTO;
import com.shop.ecommerce.services.PaymentCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class PaymentCardController {

    private final PaymentCardService paymentCardService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<PaymentCardDTO>> getCards(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentCardService.getCardsByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<PaymentCardDTO> addCard(@RequestBody PaymentCardCreateDTO dto) {
        return ResponseEntity.ok(paymentCardService.addCard(dto));
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId, @RequestParam Long userId) {
        paymentCardService.deleteCard(cardId, userId);
        return ResponseEntity.ok().build();
    }
}
