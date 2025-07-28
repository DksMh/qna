package com.act2gether.dto;

import com.act2gether.entity.CustomerSupport;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

public class CustomerSupportDTO {
    
    // 게시글 생성 요청 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        
        @NotBlank(message = "문의 유형은 필수입니다.")
        private String inquiryType; // "일반문의", "신고"
        
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 255, message = "제목은 255자 이내로 입력해주세요.")
        private String title;
        
        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = 5000, message = "내용은 5000자 이내로 입력해주세요.")
        private String content;
        
        @Builder.Default
        private Boolean isPrivate = true; // 기본값: 비공개
        
        // 파일 업로드용 (향후 확장)
        private MultipartFile imageFile;
    }
    
    // 게시글 수정 요청 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        
        private String inquiryType;
        
        @Size(max = 255, message = "제목은 255자 이내로 입력해주세요.")
        private String title;
        
        @Size(max = 5000, message = "내용은 5000자 이내로 입력해주세요.")
        private String content;
        
        private Boolean isPrivate;
        
        // 향후 이미지 업로드 확장용
        private MultipartFile imageFile;
    }
    
    // 답변 생성/수정 요청 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseRequest {
        
        @NotBlank(message = "답변 내용은 필수입니다.")
        @Size(max = 2000, message = "답변은 2000자 이내로 입력해주세요.")
        private String response;
    }
    
    // 게시글 응답 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        
        private Integer supportId;
        private Integer userId;
        private String userNickname; // 작성자 닉네임
        private String inquiryType;
        private String title;
        private String content;
        private String responder;
        private String response;
        private String status;
        private Boolean isPrivate;
        private Boolean isOwner; // 현재 로그인 유저가 작성자인지
        private Boolean hasResponse; // 답변이 있는지
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updatedAt;
        
        // Entity to DTO 변환
        public static Response from(CustomerSupport entity, Integer currentUserId) {
            return Response.builder()
                    .supportId(entity.getSupportId())
                    .userId(entity.getUserId())
                    .userNickname(entity.getUser() != null ? entity.getUser().get유저아이디() : "Unknown")
                    .inquiryType(entity.getInquiryType())
                    .title(entity.getTitle())
                    .content(entity.getContent())
                    .responder(entity.getResponder())
                    .response(entity.getResponse())
                    .status(entity.getStatus())
                    .isPrivate(entity.getIsPrivate())
                    .isOwner(entity.isOwner(currentUserId))
                    .hasResponse(entity.hasResponse())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        }
    }
    
    // 게시글 목록 응답 DTO (간소화된 정보)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        
        private Integer supportId;
        private String userNickname;
        private String inquiryType;
        private String title;
        private String status;
        private Boolean isPrivate;
        private Boolean isOwner;
        private Boolean hasResponse;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
        private LocalDateTime createdAt;
        
        public static ListResponse from(CustomerSupport entity, Integer currentUserId) {
            return ListResponse.builder()
                    .supportId(entity.getSupportId())
                    .userNickname(entity.getUser() != null ? entity.getUser().get유저아이디() : "Unknown")
                    .inquiryType(entity.getInquiryType())
                    .title(entity.getTitle())
                    .status(entity.getStatus())
                    .isPrivate(entity.getIsPrivate())
                    .isOwner(entity.isOwner(currentUserId))
                    .hasResponse(entity.hasResponse())
                    .createdAt(entity.getCreatedAt())
                    .build();
        }
    }
    
    // 검색 요청 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRequest {
        
        private String keyword;
        private String inquiryType; // "일반문의", "신고"
        private String status; // "답변대기", "답변완료"
        private Boolean myPostsOnly; // 내 글만 보기
        
        @Builder.Default
        private int page = 0;
        
        @Builder.Default
        private int size = 5;
    }
}