package com.act2gether.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * Spring Security 보안 설정
 * OWASP 보안 가이드라인 준수
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 보호 설정
            .csrf()
                .csrfTokenRepository(org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringAntMatchers("/api/auth/**") // 인증 API는 CSRF 제외
                .and()
            
            // 세션 관리 설정 (JWT 사용으로 Stateless)
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
            
            // 보안 헤더 설정
            .headers(headers -> headers
                // X-Content-Type-Options
                .contentTypeOptions().and()
                
                // X-Frame-Options
                .frameOptions().deny()
                
                // X-XSS-Protection
                .addHeaderWriter(new XXssProtectionHeaderWriter())
                
                // Referrer Policy
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                
                // HSTS (HTTPS Strict Transport Security)
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000) // 1년
                    .includeSubdomains(true)
                    .preload(true)
                )
                
                // Content Security Policy
                .contentSecurityPolicy(
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; " +
                    "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; " +
                    "img-src 'self' data: blob:; " +
                    "font-src 'self' https://cdnjs.cloudflare.com; " +
                    "connect-src 'self'; " +
                    "media-src 'self'; " +
                    "object-src 'none'; " +
                    "frame-src 'none'; " +
                    "base-uri 'self'; " +
                    "form-action 'self';"
                )
            )
            
            // CORS 설정
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 인증/인가 설정
            .authorizeHttpRequests(authz -> authz
                // 공개 엔드포인트
                .antMatchers("/", "/static/**", "/css/**", "/js/**", "/images/**").permitAll()
                .antMatchers("/api/auth/**").permitAll()
                .antMatchers("/api/public/**").permitAll()
                .antMatchers("/actuator/health").permitAll()
                
                // QnA 읽기는 모든 사용자 허용
                .antMatchers("GET", "/api/qna", "/api/qna/*/replies").permitAll()
                .antMatchers("GET", "/api/qna/*").permitAll()
                
                // QnA 쓰기는 인증된 사용자만
                .antMatchers("POST", "/api/qna").authenticated()
                .antMatchers("PUT", "/api/qna/*").authenticated()
                .antMatchers("DELETE", "/api/qna/*").authenticated()
                
                // 답변 작성은 관리자만
                .antMatchers("/api/qna/*/replies").hasRole("ADMIN")
                .antMatchers("/api/qna/replies/*").hasRole("ADMIN")
                
                // 관리자 전용 엔드포인트
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                
                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )
            
            // 예외 처리 설정
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":false,\"message\":\"인증이 필요합니다.\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":false,\"message\":\"접근 권한이 없습니다.\"}");
                })
            );
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용된 도메인 (프로덕션에서는 특정 도메인만 허용)
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "https://yourdomain.com",
            "https://*.yourdomain.com",
            "http://localhost:*" // 개발 환경용
        ));
        
        // 허용된 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        
        // 허용된 헤더
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Cache-Control",
            "X-File-Name"
        ));
        
        // 노출할 헤더
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Disposition"
        ));
        
        // 자격 증명 허용
        configuration.setAllowCredentials(true);
        
        // 캐시 시간 (초)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt 사용 (강도 12)
        return new BCryptPasswordEncoder(12);
    }
}

/**
 * 보안 설정을 위한 application.yml 추가 설정
 */
/*
server:
  # HTTPS 설정 (프로덕션 환경)
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: tomcat
  
  # 보안 헤더 설정
  servlet:
    session:
      cookie:
        http-only: true
        secure: true
        same-site: strict

# JWT 보안 설정
app:
  jwt:
    secret: ${JWT_SECRET} # 환경변수에서 가져오기
    expiration: 3600000 # 1시간
    refresh-expiration: 604800000 # 7일
    issuer: act2gether
    audience: act2gether-users

# 파일 업로드 보안 설정
spring:
  servlet:
    multipart:
      max-file-size: 3MB
      max-request-size: 10MB
      enabled: true
      
  # 데이터베이스 보안 설정
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      # 연결 풀 보안 설정
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      
# 로깅 보안 설정
logging:
  level:
    org.springframework.security: INFO
    com.act2gether.security: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    
# Actuator 보안 설정
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
  security:
    enabled: true
*/