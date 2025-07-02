package com.nsmm.esg.scope_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Scope Service Spring Security 설정
 * 
 * Gateway에서 JWT 인증 처리 완료 후 헤더로 사용자 정보 전달
 * 따라서 모든 API 요청을 허용하고 컨트롤러 레벨에서 헤더 검증
 * 
 * @author ESG Project Team
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            // Gateway에서 JWT 인증 완료 후 헤더로 전달받으므로 모든 요청 허용
            .anyRequest().permitAll());
    return http.build();
  }
}