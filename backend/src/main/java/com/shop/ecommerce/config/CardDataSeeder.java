package com.shop.ecommerce.config;

import com.shop.ecommerce.entities.PaymentCard;
import com.shop.ecommerce.entities.User;
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
@Order(10) // Run after other initializers
public class CardDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PaymentCardRepository paymentCardRepository;

    @Override
    public void run(String... args) {
        // Only seed if no cards exist
        if (paymentCardRepository.count() > 0) {
            log.info("Payment cards already exist, skipping seed.");
            return;
        }

        List<User> individualUsers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.INDIVIDUAL)
                .toList();

        if (individualUsers.isEmpty()) {
            log.info("No individual users found, skipping card seed.");
            return;
        }

        Random random = new Random(42);
        String[] cardPrefixes = {"4", "5", "9"}; // VISA, MASTERCARD, TROY

        int cardsCreated = 0;
        for (User user : individualUsers) {
            int numCards = 1 + random.nextInt(2); // 1 or 2 cards per user
            for (int i = 0; i < numCards; i++) {
                PaymentCard card = new PaymentCard();
                card.setUser(user);

                String holderName = user.getName() + " " + user.getSurname();
                card.setCardHolderName(holderName.toUpperCase());

                // Generate realistic card number
                String prefix = cardPrefixes[random.nextInt(cardPrefixes.length)];
                StringBuilder cardNum = new StringBuilder(prefix);
                for (int j = 1; j < 16; j++) {
                    cardNum.append(random.nextInt(10));
                }
                card.setCardNumber(cardNum.toString());

                card.setExpiryMonth(1 + random.nextInt(12));
                card.setExpiryYear(2026 + random.nextInt(5));
                card.setCvv(String.format("%03d", random.nextInt(1000)));

                // Detect card type
                if (prefix.equals("4")) card.setCardType("VISA");
                else if (prefix.equals("5")) card.setCardType("MASTERCARD");
                else card.setCardType("TROY");

                paymentCardRepository.save(card);
                cardsCreated++;
            }
        }

        log.info("Seeded {} payment cards for {} individual users.", cardsCreated, individualUsers.size());
    }
}
