package com.seouldate.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. CSRF 비활성화: JWT를 사용하고, 브라우저 세션을 사용하지 않으므로 불필요함
            .csrf(AbstractHttpConfigurer::disable)
            
            // 2. HTTP Basic Auth 및 Form 로그인 비활성화 (Gateway가 JWT 검증을 대행하므로)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            
            // 3. 세션 관리: Spring Security에서 세션을 생성하거나 사용하지 않도록 STATELESS 설정
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 4. 경로별 권한 설정
            .authorizeHttpRequests(auth -> auth
            .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
            .requestMatchers("/error").permitAll()
            .requestMatchers("/api/auth/**", "/actuator/**").permitAll()
            .anyRequest().authenticated()
            );

        return http.build();
    }
}
