package com.act2gether.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.owasp.encoder.Encode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 보안 관련 유틸리티 클래스
 * OWASP 보안 기준을 따라 구현
 */
@Component
@Slf4j
public class SecurityUtil {
    
    // 허용된 이미지 MIME 타입
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp"
    );
    
    // 허용된 파일 확장자
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".webp"
    );
    
    // XSS 공격 패턴
    private static final Pattern XSS_PATTERN = Pattern.compile(
            ".*(<script[^>]*>.*?</script>|javascript:|on\\w+\\s*=|<iframe|<object|<embed)", 
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // SQL Injection 공격 패턴
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            ".*(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|UNION|SCRIPT)\\b)", 
            Pattern.CASE_INSENSITIVE
    );
    
    // 허용된 검색 키워드 패턴 (한글, 영문, 숫자, 공백, 기본 특수문자)
    private static final Pattern ALLOWED_SEARCH_PATTERN = Pattern.compile(
            "^[가-힣a-zA-Z0-9\\s\\-_.,!?()\\[\\]]*$"
    );
    
    // 파일명에서 허용되지 않는 문자들
    private static final Pattern FILENAME_SANITIZE_PATTERN = Pattern.compile(
            "[^a-zA-Z0-9._-]"
    );
    
    /**
     * HTML 입력값 정화 (XSS 방지)
     */
    public String sanitizeHtml(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        // XSS 패턴 검사
        if (XSS_PATTERN.matcher(input).matches()) {
            log.warn("XSS 공격 시도 감지: {}", input.substring(0, Math.min(50, input.length())));
            throw new SecurityException("허용되지 않는 문자가 포함되어 있습니다.");
        }
        
        // OWASP Java Encoder 사용하여 HTML 인코딩
        return Encode.forHtml(input.trim());
    }
    
    /**
     * 검색 키워드 정화 (SQL Injection 방지)
     */
    public String sanitizeSearchKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return "";
        }
        
        keyword = keyword.trim();
        
        // 길이 제한 (DoS 공격 방지)
        if (keyword.length() > 100) {
            keyword = keyword.substring(0, 100);
        }
        
        // SQL Injection 패턴 검사
        if (SQL_INJECTION_PATTERN.matcher(keyword).matches()) {
            log.warn("SQL Injection 공격 시도 감지: {}", keyword);
            throw new SecurityException("허용되지 않는 검색어입니다.");
        }
        
        // 허용된 문자만 포함되어 있는지 검사
        if (!ALLOWED_SEARCH_PATTERN.matcher(keyword).matches()) {
            log.warn("허용되지 않는 검색어 문자 감지: {}", keyword);
            throw new SecurityException("허용되지 않는 문자가 포함되어 있습니다.");
        }
        
        // LIKE 쿼리에서 사용되는 특수문자 이스케이프
        return escapeLikePattern(keyword);
    }
    
    /**
     * LIKE 패턴 특수문자 이스케이프
     */
    private String escapeLikePattern(String input) {
        return input.replace("\\", "\\\\")
                   .replace("%", "\\%")
                   .replace("_", "\\_")
                   .replace("[", "\\[")
                   .replace("]", "\\]");
    }
    
    /**
     * 이미지 파일 보안 검증
     */
    public void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        
        // 파일 크기 검증 (3MB)
        long maxSize = 3 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new SecurityException("파일 크기는 3MB 이하여야 합니다.");
        }
        
        // 원본 파일명 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new SecurityException("유효하지 않은 파일명입니다.");
        }
        
        // 파일 확장자 검증
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new SecurityException("허용되지 않는 파일 형식입니다. (JPG, PNG, WebP만 허용)");
        }
        
        // MIME 타입 검증
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new SecurityException("허용되지 않는 파일 형식입니다.");
        }
        
        // 실제 파일 내용 검증 (MIME 타입 스푸핑 방지)
        validateImageContent(file);
        
        // 파일명 보안 검증
        validateFilename(originalFilename);
    }
    
    /**
     * 실제 이미지 파일 내용 검증
     */
    private void validateImageContent(MultipartFile file) {
        try {
            // 파일의 실제 내용이 이미지인지 확인
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (image == null) {
                throw new SecurityException("유효하지 않은 이미지 파일입니다.");
            }
            
            // 이미지 크기 제한 (메모리 공격 방지)
            int maxWidth = 4096;
            int maxHeight = 4096;
            if (image.getWidth() > maxWidth || image.getHeight() > maxHeight) {
                throw new SecurityException("이미지 크기가 너무 큽니다. (최대 4096x4096)");
            }
            
        } catch (IOException e) {
            log.warn("이미지 파일 검증 실패", e);
            throw new SecurityException("이미지 파일을 읽을 수 없습니다.");
        }
    }
    
    /**
     * 파일명 보안 검증
     */
    private void validateFilename(String filename) {
        // 경로 순회 공격 방지
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new SecurityException("허용되지 않는 파일명입니다.");
        }
        
        // NULL 바이트 공격 방지
        if (filename.contains("\0")) {
            throw new SecurityException("허용되지 않는 파일명입니다.");
        }
        
        // 파일명 길이 제한
        if (filename.length() > 255) {
            throw new SecurityException("파일명이 너무 깁니다.");
        }
        
        // 시스템 예약 파일명 검사 (Windows)
        String[] reservedNames = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", 
                                 "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", 
                                 "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", 
                                 "LPT7", "LPT8", "LPT9"};
        
        String nameWithoutExt = filename.substring(0, filename.lastIndexOf('.'));
        for (String reserved : reservedNames) {
            if (reserved.equalsIgnoreCase(nameWithoutExt)) {
                throw new SecurityException("예약된 파일명입니다.");
            }
        }
    }
    
    /**
     * 안전한 파일명 생성
     */
    public String generateSecureFilename(String originalFilename, Long userId) {
        String extension = getFileExtension(originalFilename);
        
        // 안전한 파일명 생성: userId_timestamp_uuid.ext
        long timestamp = System.currentTimeMillis();
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        
        return String.format("%d_%d_%s%s", userId, timestamp, uuid, extension);
    }
    
    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        
        return filename.substring(lastDotIndex).toLowerCase();
    }
    
    /**
     * IP 주소 검증 및 정화
     */
    public String sanitizeIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return "unknown";
        }
        
        // IPv4 패턴
        Pattern ipv4Pattern = Pattern.compile(
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        );
        
        // IPv6 패턴 (간단한 버전)
        Pattern ipv6Pattern = Pattern.compile(
            "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"
        );
        
        String cleanIp = ipAddress.trim();
        
        if (ipv4Pattern.matcher(cleanIp).matches() || ipv6Pattern.matcher(cleanIp).matches()) {
            return cleanIp;
        }
        
        log.warn("유효하지 않은 IP 주소: {}", ipAddress);
        return "invalid";
    }
    
    /**
     * 사용자 에이전트 정화
     */
    public String sanitizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return "unknown";
        }
        
        // 길이 제한 (DoS 공격 방지)
        if (userAgent.length() > 512) {
            userAgent = userAgent.substring(0, 512);
        }
        
        // 위험한 문자 제거
        return userAgent.replaceAll("[<>\"'&]", "");
    }
    
    /**
     * 비밀번호 강도 검증
     */
    public boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        // 최소 8자, 대문자, 소문자, 숫자, 특수문자 포함
        Pattern strongPasswordPattern = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
        );
        
        return strongPasswordPattern.matcher(password).matches();
    }
    
    /**
     * 세션 토큰 검증
     */
    public void validateSessionToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new SecurityException("세션 토큰이 없습니다.");
        }
        
        // JWT 토큰 형식 검증 (header.payload.signature)
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new SecurityException("유효하지 않은 토큰 형식입니다.");
        }
        
        // Base64 형식 검증
        Pattern base64Pattern = Pattern.compile("^[A-Za-z0-9+/]*={0,2}$");
        for (String part : parts) {
            if (!base64Pattern.matcher(part).matches()) {
                throw new SecurityException("유효하지 않은 토큰 형식입니다.");
            }
        }
    }
    
    /**
     * Rate Limiting을 위한 키 생성
     */
    public String generateRateLimitKey(String ipAddress, String endpoint) {
        return String.format("rate_limit:%s:%s", sanitizeIpAddress(ipAddress), endpoint);
    }
    
    /**
     * 보안 로그 기록
     */
    public void logSecurityEvent(String eventType, String details, String ipAddress, Long userId) {
        log.warn("보안 이벤트 - 타입: {}, 상세: {}, IP: {}, 사용자: {}", 
                eventType, details, sanitizeIpAddress(ipAddress), userId);
    }
}