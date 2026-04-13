// Test kartları için veri seeder'ı
// GÜVENLİK: Artık sadece son 4 hane ve token kaydedilir (PCI DSS uyumlu)
// CVV asla kaydedilmez
package com.shop.ecommerce.config;

import com.shop.ecommerce.entities.PaymentCard;
import com.shop.ecommerce.entities.User;
import com.shop.ecommerce.enums.PaymentProvider;
import com.shop.ecommerce.enums.Role;
import com.shop.ecommerce.repository.PaymentCardRepository;
import com.shop.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(10)
public class CardDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PaymentCardRepository paymentCardRepository;

    @Override
    public void run(String... args) {
        if (paymentCardRepository.count() > 0) {
            log.info("Ödeme kartları zaten mevcut, seed atlanıyor.");
            return;
        }

        List<User> individualUsers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.INDIVIDUAL)
                .toList();

        if (individualUsers.isEmpty()) {
            log.info("Bireysel kullanıcı bulunamadı, kart seed atlanıyor.");
            return;
        }

        Random random = new Random(42);
        String[] cardTypes = {"VISA", "MASTERCARD", "TROY"};

        int cardsCreated = 0;
        for (User user : individualUsers) {
            int numCards = 1 + random.nextInt(2);
            for (int i = 0; i < numCards; i++) {
                PaymentCard card = new PaymentCard();
                card.setUser(user);

                String holderName = user.getName() + " " + user.getSurname();
                card.setCardHolderName(holderName.toUpperCase());

                // GÜVENLİK: Sadece son 4 hane kaydedilir
                String lastFour = String.format("%04d", random.nextInt(10000));
                card.setCardNumber(lastFour);

                card.setExpiryMonth(1 + random.nextInt(12));
                card.setExpiryYear(2026 + random.nextInt(5));

                // CVV KAYDEDILMEZ — PCI DSS standardı

                // Kart tipi
                String type = cardTypes[random.nextInt(cardTypes.length)];
                card.setCardType(type);

                // Token tabanlı yapı
                card.setPaymentProvider(PaymentProvider.STRIPE);
                card.setProviderToken("pm_test_" + System.currentTimeMillis() + "_" + random.nextInt(1000));
                card.setIsActive(true);

                paymentCardRepository.save(card);
                cardsCreated++;
            }
        }

        log.info("{} bireysel kullanıcı için {} test kartı oluşturuldu.", individualUsers.size(), cardsCreated);
    }
}
