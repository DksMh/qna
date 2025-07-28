package com.act2gether.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 보안 강화된 JWT 토큰 유틸리티
 * OWASP JWT 보안 가이드라인 준수
 */
@Component
@Slf4j
public class SecureJwtUtil {
    
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    
    @Value("${app.jwt.expiration:3600000}") // 1시간 (보안 강화)
    private long jwtExpiration;
    
    @Value("${app.jwt.refresh-expiration:604800000}") // 7일
    private long refreshTokenExpiration;
    
    @Value("${app.jwt.issuer:act2gether}")
    private String jwtIssuer;
    
    @Value("${app.jwt.audience:act2gether-users}")
    private String jwtAudience;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * 안전한 서명 키 생성
     */
    private SecretKey getSigningKey() {
        // 환경변수에서 비밀키가 설정되지 않은 경우 예외 발생
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT 비밀키가 설정되지 않았거나 길이가 부족합니다. (최소 32자)");
        }
        
        // Base64로 인코딩된 키 사용
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * 액세스 토큰 생성
     */
    public String generateAccessToken(Long userId, String userAccount, String ipAddress) {
        return generateToken(userId, userAccount, ipAddress, jwtExpiration, "access");
    }
    
    /**
     * 리프레시 토큰 생성
     */
    public String generateRefreshToken(Long userId, String userAccount, String ipAddress) {
        return generateToken(userId, userAccount, ipAddress, refreshTokenExpiration, "refresh");
    }
    
    /**
     * JWT 토큰 생성 (보안 강화)
     */
    private String generateToken(Long userId, String userAccount, String ipAddress, 
                               long expirationTime, String tokenType) {
        
        Instant now = Instant.now();
        Instant expiration = now.plus(expirationTime, ChronoUnit.MILLIS);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("userAccount", userAccount);
        claims.put("tokenType", tokenType);
        claims.put("ipAddress", ipAddress);
        
        // JTI (JWT ID) 추가 - 토큰 무효화를 위한 고유 식별자
        String jti = generateSecureRandomString(32);
        claims.put("jti", jti);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userAccount)
                .setIssuer(jwtIssuer)
                .setAudience(jwtAudience)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .setNotBefore(Date.from(now)) // 토큰이 유효해지는 시간
                .signWith(getSigningKey(), SignatureAlgorithm.HS512) // HS512 사용
                .compact();
    }
    
    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object userId = claims.get("userId");
        
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        } else if (userId instanceof Long) {
            return (Long) userId;
        } else {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID 형식입니다.");
        }
    }
    
    /**
     * 토큰에서 사용자 계정 추출
     */
    public String getUserAccountFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userAccount", String.class);
    }
    
    /**
     * 토큰에서 IP 주소 추출
     */
    public String getIpAddressFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("ipAddress", String.class);
    }
    
    /**
     * 토큰 타입 확인
     */
    public String getTokenTypeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("tokenType", String.class);
    }
    
    /**
     * JTI (JWT ID) 추출
     */
    public String getJtiFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("jti", String.class);
    }
    
    /**
     * 토큰 만료일 추출
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }
    
    /**
     * 토큰 유효성 검증 (보안 강화)
     */
    public boolean validateToken(String token, String currentIpAddress) {
        try {
            Claims claims = getClaimsFromToken(token);
            
            // 기본 유효성 검증
            if (isTokenExpired(token)) {
                log.warn("만료된 토큰 사용 시도");
                return false;
            }
            
            // IP 주소 검증 (선택적)
            if (currentIpAddress != null) {
                String tokenIpAddress = getIpAddressFromToken(token);
                if (!currentIpAddress.equals(tokenIpAddress)) {
                    log.warn("토큰의 IP 주소와 현재 IP 주소가 다름 - 토큰: {}, 현재: {}", 
                            tokenIpAddress, currentIpAddress);
                    // IP 검증은 경고만 하고 토큰은 유효한 것으로 처리 (모바일 환경 고려)
                }
            }
            
            // 발급자 검증
            if (!jwtIssuer.equals(claims.getIssuer())) {
                log.warn("유효하지 않은 토큰 발급자: {}", claims.getIssuer());
                return false;
            }
            
            // 대상자 검증
            if (!jwtAudience.equals(claims.getAudience())) {
                log.warn("유효하지 않은 토큰 대상자: {}", claims.getAudience());
                return false;
            }
            
            return true;
            
        } catch (SecurityException e) {
            log.error("JWT 서명 검증 실패", e);
        } catch (MalformedJwtException e) {
            log.error("잘못된 JWT 토큰 형식", e);
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰", e);
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰", e);
        } catch (IllegalArgumentException e) {
            log.error("JWT claims가 비어있습니다", e);
        } catch (Exception e) {
            log.error("JWT 토큰 검증 중 알 수 없는 오류", e);
        }
        
        return false;
    }
    
    /**
     * HTTP 요청에서 토큰 추출 (보안 강화)
     */
    public String extractTokenFromRequest(HttpServletRequest request) {
        // Authorization 헤더에서 추출
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ") && bearerToken.length() > 7) {
            return bearerToken.substring(7);
        }
        
        // 쿠키에서 추출 (HttpOnly 쿠키)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }
    
    /**
     * 안전한 쿠키로 토큰 설정
     */
    public void setTokenCookie(HttpServletResponse response, String token, boolean isRefreshToken) {
        Cookie cookie = new Cookie(isRefreshToken ? "refresh_token" : "access_token", token);
        
        // 보안 설정
        cookie.setHttpOnly(true); // XSS 방지
        cookie.setSecure(true); // HTTPS에서만 전송
        cookie.setPath("/");
        cookie.setSameSite(Cookie.SameSite.STRICT); // CSRF 방지
        
        // 만료 시간 설정
        int maxAge = isRefreshToken ? 
                (int) (refreshTokenExpiration / 1000) : 
                (int) (jwtExpiration / 1000);
        cookie.setMaxAge(maxAge);
        
        response.addCookie(cookie);
    }
    
    /**
     * 토큰 쿠키 삭제
     */
    public void clearTokenCookies(HttpServletResponse response) {
        Cookie accessTokenCookie = new Cookie("access_token", "");
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0);
        response.addCookie(accessTokenCookie);
        
        Cookie refreshTokenCookie = new Cookie("refresh_token", "");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0);
        response.addCookie(refreshTokenCookie);
    }
    
    /**
     * 토큰 새로고침
     */
    public String refreshToken(String refreshToken, String currentIpAddress) {
        if (!validateToken(refreshToken, currentIpAddress)) {
            throw new SecurityException("유효하지 않은 리프레시 토큰입니다.");
        }
        
        String tokenType = getTokenTypeFromToken(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new SecurityException("리프레시 토큰이 아닙니다.");
        }
        
        Long userId = getUserIdFromToken(refreshToken);
        String userAccount = getUserAccountFromToken(refreshToken);
        
        return generateAccessToken(userId, userAccount, currentIpAddress);
    }
    
    // Private helper methods
    
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .requireIssuer(jwtIssuer)
                .requireAudience(jwtAudience)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    private boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
    
    private String generateSecureRandomString(int length) {
        byte[] randomBytes = new byte[length];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    /**
     * 토큰 블랙리스트 확인 (Redis 등과 연동 필요)
     */
    public boolean isTokenBlacklisted(String jti) {
        // TODO: Redis 또는 Database와 연동하여 토큰 블랙리스트 확인
        // 현재는 기본 구현으로 false 반환
        return false;
    }
    
    /**
     * 토큰을 블랙리스트에 추가
     */
    public void blacklistToken(String jti, Date expiration) {
        // TODO: Redis 또는 Database에 토큰 블랙리스트 저장
        log.info("토큰 블랙리스트 추가: {}", jti);
    }
}