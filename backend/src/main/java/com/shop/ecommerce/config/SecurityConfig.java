package com.shop.ecommerce.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF korumasını devre dışı bırak (stateless JWT kullanıyoruz)
                .csrf(csrf -> csrf.disable())

                // 2. CORS ayarlarını aktif et
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. İstekleri yönet — endpoint bazlı yetkilendirme
                .authorizeHttpRequests(auth -> auth
                        // Public — Auth endpoint'leri
                        .requestMatchers("/api/auth/**").permitAll()

                        // Public — Actuator (health checks for Docker/K8s)
                        .requestMatchers("/actuator/**").permitAll()

                        // Public — Genel istatistikler (auth gerektirmez)
                        .requestMatchers("/api/public/**").permitAll()

                        // Public — Ürünler (sadece GET)
                        .requestMatchers(HttpMethod.GET, "/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/products/categories").permitAll()

                        // Public — Yorumları görüntüleme (GET product reviews)
                        .requestMatchers(HttpMethod.GET, "/api/reviews/product/**").permitAll()

                        // INDIVIDUAL rolü — Kendi yorumlarını görüntüleme, ekleme, silme
                        .requestMatchers(HttpMethod.GET, "/api/reviews/user").hasRole("INDIVIDUAL")
                        .requestMatchers(HttpMethod.POST, "/api/reviews").hasRole("INDIVIDUAL")
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/**").hasRole("INDIVIDUAL")

                        // Public — Chat AI assistant (JWT token varsa kullanıcı tanınır, yoksa guest)
                        .requestMatchers("/api/chat/ask").permitAll()

                        // Text2SQL chatbot — SQL execution
                        .requestMatchers("/api/chat/execute").permitAll()

                        // Support — Individual ve Corporate kullanıcılar destek talebi oluşturabilir
                        .requestMatchers(HttpMethod.POST, "/api/support").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/support/my-tickets/**").authenticated()
                        .requestMatchers("/api/support/admin/**").hasRole("ADMIN")

                        // Authenticated — Profil ve Analytics
                        .requestMatchers("/api/users/profile/**").authenticated()
                        .requestMatchers("/api/analytics/individual").hasRole("INDIVIDUAL")
                        .requestMatchers("/api/analytics/corporate").hasRole("CORPORATE")
                        .requestMatchers("/api/analytics/admin").hasRole("ADMIN")

                        // INDIVIDUAL rolü — Sepet, Wishlist, Sipariş, Kartlar
                        .requestMatchers("/cart/**").hasRole("INDIVIDUAL")
                        .requestMatchers("/wishlist/**").hasRole("INDIVIDUAL")
                        .requestMatchers("/api/orders/**").hasRole("INDIVIDUAL")
                        .requestMatchers("/api/cards/**").hasRole("INDIVIDUAL")

                        // Ödeme endpoint'leri — INDIVIDUAL kullanıcılar
                        .requestMatchers("/api/payments/create").hasRole("INDIVIDUAL")
                        .requestMatchers("/api/payments/cards/**").hasRole("INDIVIDUAL")
                        .requestMatchers("/api/payments/history/**").hasRole("INDIVIDUAL")
                        .requestMatchers("/api/payments/order/**").hasRole("INDIVIDUAL")
                        .requestMatchers("/api/payments/paypal/capture").hasRole("INDIVIDUAL")

                        // Ödeme webhook'ları — public (Stripe/PayPal dışarıdan çağırır)
                        .requestMatchers("/api/payments/webhook/**").permitAll()

                        // Ödeme konfigürasyon endpoint'leri — authenticated
                        .requestMatchers("/api/payments/config/**").authenticated()

                        // Kapıda ödeme teslimat onayı — CORPORATE veya ADMIN
                        .requestMatchers("/api/payments/*/cod-confirm").hasAnyRole("CORPORATE", "ADMIN")

                        // CORPORATE rolü — Envanter, Mağaza Siparişleri, Mağaza, Kargo
                        .requestMatchers("/api/inventory/**").hasRole("CORPORATE")
                        .requestMatchers("/api/store-orders/**").hasRole("CORPORATE")
                        .requestMatchers("/api/stores/**").hasRole("CORPORATE")
                        .requestMatchers("/api/shipments/**").hasRole("CORPORATE")

                        // ADMIN rolü — Admin paneli, Diyagnostik
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/diagnostic/**").hasRole("ADMIN")

                        // Diğer tüm istekler => authenticated
                        .anyRequest().authenticated()
                )

                // 4. Session yönetimi — stateless (JWT kullanıyoruz)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 5. Authentication provider
                .authenticationProvider(authenticationProvider())

                // 6. JWT filtresini UsernamePasswordAuthenticationFilter'dan önce ekle
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // CORS ayarlarını detaylandır (Angular'ın erişebilmesi için)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost:8000", "http://localhost")); // Angular + Chainlit + Docker nginx
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}