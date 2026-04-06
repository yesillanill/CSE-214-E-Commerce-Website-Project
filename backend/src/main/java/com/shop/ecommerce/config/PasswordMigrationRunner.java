package com.shop.ecommerce.config;

import com.shop.ecommerce.entities.User;
import com.shop.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Uygulama başlatıldığında veritabanındaki düz metin şifreleri BCrypt ile hash'ler.
 * Zaten BCrypt ile hash'lenmiş şifreleri ($2a$, $2b$, $2y$ prefix) atlar.
 * Bu sınıf, tüm şifreler migrate edildikten sonra kaldırılabilir.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordMigrationRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        List<User> users = userRepository.findAll();
        int migratedCount = 0;

        for (User user : users) {
            String password = user.getPassword();
            // BCrypt hash'i $2a$, $2b$ veya $2y$ ile başlar ve 60 karakter uzunluğundadır
            if (password != null && !password.startsWith("$2a$") && !password.startsWith("$2b$") && !password.startsWith("$2y$")) {
                user.setPassword(passwordEncoder.encode(password));
                userRepository.save(user);
                migratedCount++;
                log.info("Migrated password for user: {} (ID: {})", user.getEmail(), user.getId());
            }
        }

        if (migratedCount > 0) {
            log.info("Password migration completed. {} passwords migrated to BCrypt.", migratedCount);
        } else {
            log.info("Password migration check completed. No plain-text passwords found.");
        }
    }
}
