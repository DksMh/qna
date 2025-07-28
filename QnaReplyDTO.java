package com.act2gether.dto;

import com.act2gether.entity.QnaReply;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

public class QnaReplyDTO {
    
    // 답변 생성 요청 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        
        @NotBlank(message = "답변 내용은 필수입니다.")
        @Size(max = 2000, message = "답변은 2000자 이내로 입력해주세요.")
        private String replyContent;
    }
    
    // 답변 수정 요청 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        
        @NotBlank(message = "답변 내용은 필수입니다.")
        @Size(max = 2000, message = "답변은 2000자 이내로 입력해주세요.")
        private String replyContent;
    }
    
    // 답변 응답 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        
        private Long replyId;
        private Long qnaId;
        private Long adminUserPid;
        private String adminNickname; // 관리자 닉네임
        private String replyContent;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updatedAt;
        
        // Entity to DTO 변환
        public static Response from(QnaReply entity) {
            return Response.builder()
                    .replyId(entity.getReplyId())
                    .qnaId(entity.getQnaId())
                    .adminUserPid(entity.getAdminUserPid())
                    .adminNickname(entity.getAdminUser() != null ? entity.getAdminUser().get유저아이디() : "관리자")
                    .replyContent(entity.getReplyContent())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        }
    }
}