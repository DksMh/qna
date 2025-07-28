package com.act2gether.controller;

import com.act2gether.dto.QnaPostDTO;
import com.act2gether.dto.QnaReplyDTO;
import com.act2gether.service.QnaPostService;
import com.act2gether.service.QnaReplyService;
import com.act2gether.util.JwtUtil;
import com.act2gether.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/qna")
@RequiredArgsConstructor
@Slf4j
@Validated
@CrossOrigin(origins = {"https://yourdomain.com"}) // 특정 도메인만 허용
public class SecureQnaController {
    
    private final QnaPostService qnaPostService;
    private final QnaReplyService qnaReplyService;
    private final JwtUtil jwtUtil;
    private final SecurityUtil securityUtil;
    
    // 보안 강화: 입력 검증 추가
    @GetMapping
    public ResponseEntity<Map<String, Object>> getQnaPosts(
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000) int page,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int size,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String answerStatus,
            @RequestParam(required = false, defaultValue = "false") Boolean myPostsOnly,
            HttpServletRequest request) {
        
        try {
            // 입력 검증 및 정화
            keyword = securityUtil.sanitizeSearchKeyword(keyword);
            
            UserInfo userInfo = extractUserInfo(request);
            
            QnaPostDTO.SearchRequest searchRequest = QnaPostDTO.SearchRequest.builder()
                    .page(page)
                    .size(size)
                    .keyword(keyword)
                    .category(validateCategory(category))
                    .answerStatus(validateAnswerStatus(answerStatus))
                    .myPostsOnly(myPostsOnly)
                    .build();
            
            Page<QnaPostDTO.ListResponse> posts = qnaPostService.getQnaPosts(
                    searchRequest, userInfo.getUserId(), userInfo.isAdmin());
            
            Map<String, Object> response = createSuccessResponse(posts);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 파라미터: {}", e.getMessage());
            return createErrorResponse("잘못된 요청입니다.", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("QnA 목록 조회 중 오류 발생", e);
            return createErrorResponse("서비스 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/{qnaId}")
    public ResponseEntity<Map<String, Object>> getQnaPost(
            @PathVariable @Positive Long qnaId,
            HttpServletRequest request) {
        
        try {
            UserInfo userInfo = extractUserInfo(request);
            
            QnaPostDTO.Response post = qnaPostService.getQnaPost(
                    qnaId, userInfo.getUserId(), userInfo.isAdmin());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", post);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("게시글 접근 거부 - ID: {}, 사유: {}", qnaId, e.getMessage());
            return createErrorResponse("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("게시글 조회 중 오류 발생 - ID: {}", qnaId, e);
            return createErrorResponse("게시글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
    }
    
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> createQnaPost(
            @RequestParam @NotBlank @Size(max = 20) String category,
            @RequestParam @NotBlank @Size(min = 1, max = 255) String title,
            @RequestParam @NotBlank @Size(min = 1, max = 5000) String content,
            @RequestParam(defaultValue = "true") Boolean isLocked,
            @RequestParam(required = false) MultipartFile imageFile,
            HttpServletRequest request) {
        
        try {
            UserInfo userInfo = extractUserInfoRequired(request);
            
            // 입력 값 정화 및 검증
            title = securityUtil.sanitizeHtml(title);
            content = securityUtil.sanitizeHtml(content);
            
            // 파일 검증
            if (imageFile != null && !imageFile.isEmpty()) {
                securityUtil.validateImageFile(imageFile);
            }
            
            QnaPostDTO.CreateRequest createRequest = QnaPostDTO.CreateRequest.builder()
                    .category(validateAndParseCategory(category))
                    .title(title)
                    .content(content)
                    .isLocked(isLocked)
                    .imageFile(imageFile)
                    .build();
            
            QnaPostDTO.Response createdPost = qnaPostService.createQnaPost(createRequest, userInfo.getUserId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", createdPost);
            response.put("message", "게시글이 성공적으로 작성되었습니다.");
            
            // 보안 로그
            log.info("QnA 게시글 생성 - 사용자: {}, 게시글ID: {}", userInfo.getUserId(), createdPost.getQnaId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (SecurityException e) {
            log.warn("보안 위반 - 게시글 생성: {}", e.getMessage());
            return createErrorResponse("보안 정책 위반입니다.", HttpStatus.BAD_REQUEST);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 입력값 - 게시글 생성: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("게시글 생성 중 오류 발생", e);
            return createErrorResponse("게시글 작성에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PutMapping("/{qnaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> updateQnaPost(
            @PathVariable @Positive Long qnaId,
            @RequestParam(required = false) @Size(max = 20) String category,
            @RequestParam(required = false) @Size(max = 255) String title,
            @RequestParam(required = false) @Size(max = 5000) String content,
            @RequestParam(required = false) Boolean isLocked,
            @RequestParam(required = false, defaultValue = "false") Boolean deleteImage,
            @RequestParam(required = false) MultipartFile imageFile,
            HttpServletRequest request) {
        
        try {
            UserInfo userInfo = extractUserInfoRequired(request);
            
            // 입력 값 정화
            if (title != null) title = securityUtil.sanitizeHtml(title);
            if (content != null) content = securityUtil.sanitizeHtml(content);
            
            // 파일 검증
            if (imageFile != null && !imageFile.isEmpty()) {
                securityUtil.validateImageFile(imageFile);
            }
            
            QnaPostDTO.UpdateRequest updateRequest = QnaPostDTO.UpdateRequest.builder()
                    .category(category != null ? validateAndParseCategory(category) : null)
                    .title(title)
                    .content(content)
                    .isLocked(isLocked)
                    .deleteImage(deleteImage)
                    .imageFile(imageFile)
                    .build();
            
            QnaPostDTO.Response updatedPost = qnaPostService.updateQnaPost(
                    qnaId, updateRequest, userInfo.getUserId(), userInfo.isAdmin());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", updatedPost);
            response.put("message", "게시글이 성공적으로 수정되었습니다.");
            
            // 보안 로그
            log.info("QnA 게시글 수정 - 사용자: {}, 게시글ID: {}", userInfo.getUserId(), qnaId);
            
            return ResponseEntity.ok(response);
            
        } catch (SecurityException e) {
            log.warn("보안 위반 - 게시글 수정: {}", e.getMessage());
            return createErrorResponse("보안 정책 위반입니다.", HttpStatus.BAD_REQUEST);
        } catch (IllegalArgumentException e) {
            log.warn("권한 없음 - 게시글 수정 시도 - ID: {}, 사유: {}", qnaId, e.getMessage());
            return createErrorResponse("수정 권한이 없습니다.", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("게시글 수정 중 오류 발생 - ID: {}", qnaId, e);
            return createErrorResponse("게시글 수정에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @DeleteMapping("/{qnaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> deleteQnaPost(
            @PathVariable @Positive Long qnaId,
            HttpServletRequest request) {
        
        try {
            UserInfo userInfo = extractUserInfoRequired(request);
            
            qnaPostService.deleteQnaPost(qnaId, userInfo.getUserId(), userInfo.isAdmin());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "게시글이 성공적으로 삭제되었습니다.");
            
            // 보안 로그
            log.info("QnA 게시글 삭제 - 사용자: {}, 게시글ID: {}", userInfo.getUserId(), qnaId);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("권한 없음 - 게시글 삭제 시도 - ID: {}, 사유: {}", qnaId, e.getMessage());
            return createErrorResponse("삭제 권한이 없습니다.", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("게시글 삭제 중 오류 발생 - ID: {}", qnaId, e);
            return createErrorResponse("게시글 삭제에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/{qnaId}/replies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createReply(
            @PathVariable @Positive Long qnaId,
            @Valid @RequestBody QnaReplyDTO.CreateRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            UserInfo userInfo = extractUserInfoRequired(httpRequest);
            
            // 관리자 권한 재확인
            if (!userInfo.isAdmin()) {
                throw new SecurityException("관리자 권한이 필요합니다.");
            }
            
            // 입력 값 정화
            request.setReplyContent(securityUtil.sanitizeHtml(request.getReplyContent()));
            
            QnaReplyDTO.Response createdReply = qnaReplyService.createReply(
                    qnaId, request, userInfo.getUserId(), userInfo.isAdmin());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", createdReply);
            response.put("message", "답변이 성공적으로 작성되었습니다.");
            
            // 보안 로그
            log.info("QnA 답변 생성 - 관리자: {}, 게시글ID: {}", userInfo.getUserId(), qnaId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (SecurityException e) {
            log.warn("권한 없음 - 답변 작성 시도: {}", e.getMessage());
            return createErrorResponse("관리자 권한이 필요합니다.", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("답변 작성 중 오류 발생 - QnA ID: {}", qnaId, e);
            return createErrorResponse("답변 작성에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Private helper methods
    
    private UserInfo extractUserInfo(HttpServletRequest request) {
        String token = jwtUtil.extractTokenFromRequest(request);
        
        if (token == null || token.isEmpty()) {
            return new UserInfo(null, false);
        }
        
        try {
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                String userAccount = jwtUtil.getUserAccountFromToken(token);
                boolean isAdmin = "admin".equals(userAccount);
                
                return new UserInfo(userId, isAdmin);
            }
        } catch (Exception e) {
            log.warn("JWT 토큰 검증 실패 - IP: {}", getClientIpAddress(request), e);
        }
        
        return new UserInfo(null, false);
    }
    
    private UserInfo extractUserInfoRequired(HttpServletRequest request) {
        UserInfo userInfo = extractUserInfo(request);
        
        if (userInfo.getUserId() == null) {
            throw new SecurityException("인증이 필요합니다.");
        }
        
        return userInfo;
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    private QnaPost.Category validateAndParseCategory(String category) {
        try {
            return QnaPost.Category.valueOf(category);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 카테고리입니다: " + category);
        }
    }
    
    private QnaPost.Category validateCategory(String category) {
        if (category == null || category.isEmpty()) {
            return null;
        }
        return validateAndParseCategory(category);
    }
    
    private QnaPost.AnswerStatus validateAnswerStatus(String status) {
        if (status == null || status.isEmpty()) {
            return null;
        }
        try {
            return QnaPost.AnswerStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 답변 상태입니다: " + status);
        }
    }
    
    private Map<String, Object> createSuccessResponse(Page<?> page) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", page.getContent());
        response.put("currentPage", page.getNumber());
        response.put("totalPages", page.getTotalPages());
        response.put("totalElements", page.getTotalElements());
        response.put("hasNext", page.hasNext());
        response.put("hasPrevious", page.hasPrevious());
        return response;
    }
    
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(status).body(response);
    }
    
    // Inner class for user information
    private static class UserInfo {
        private final Long userId;
        private final boolean admin;
        
        public UserInfo(Long userId, boolean admin) {
            this.userId = userId;
            this.admin = admin;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public boolean isAdmin() {
            return admin;
        }
    }
}