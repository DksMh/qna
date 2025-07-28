package com.act2gether.dto;

import com.act2gether.entity.QnaPost;
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
import java.util.List;

public class QnaPostDTO {
    
    // 게시글 생성 요청 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        
        @NotNull(message = "카테고리는 필수입니다.")
        private QnaPost.Category category;
        
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 255, message = "제목은 255자 이내로 입력해주세요.")
        private String title;
        
        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = 5000, message = "내용은 5000자 이내로 입력해주세요.")
        private String content;
        
        @Builder.Default
        private Boolean isLocked = true; // 기본값: 잠금 활성화
        
        // 파일 업로드용 (Multipart)
        private MultipartFile imageFile;
    }
    
    // 게시글 수정 요청 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        
        private QnaPost.Category category;
        
        @Size(max = 255, message = "제목은 255자 이내로 입력해주세요.")
        private String title;
        
        @Size(max = 5000, message = "내용은 5000자 이내로 입력해주세요.")
        private String content;
        
        private Boolean isLocked;
        
        // 이미지 삭제 여부
        private Boolean deleteImage;
        
        // 새 이미지 업로드
        private MultipartFile imageFile;
    }
    
    // 게시글 응답 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        
        private Long qnaId;
        private Long userPid;
        private String userNickname; // 작성자 닉네임
        private QnaPost.Category category;
        private String title;
        private String content;
        private String imagePath;
        private Boolean isLocked;
        private QnaPost.AnswerStatus answerStatus;
        private Integer viewCount;
        private Integer replyCount; // 답변 개수
        private Boolean isOwner; // 현재 로그인 유저가 작성자인지
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updatedAt;
        
        // 답변 목록 (상세 조회 시에만 포함)
        private List<QnaReplyDTO.Response> replies;
        
        // Entity to DTO 변환
        public static Response from(QnaPost entity, Long currentUserId, boolean includeReplies) {
            ResponseBuilder builder = Response.builder()
                    .qnaId(entity.getQnaId())
                    .userPid(entity.getUserPid())
                    .userNickname(entity.getUser() != null ? entity.getUser().get유저아이디() : "Unknown")
                    .category(entity.getCategory())
                    .title(entity.getTitle())
                    .content(entity.getContent())
                    .imagePath(entity.getImagePath())
                    .isLocked(entity.getIsLocked())
                    .answerStatus(entity.getAnswerStatus())
                    .viewCount(entity.getViewCount())
                    .replyCount(entity.getReplies() != null ? entity.getReplies().size() : 0)
                    .isOwner(entity.isOwner(currentUserId))
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt());
            
            if (includeReplies && entity.getReplies() != null) {
                List<QnaReplyDTO.Response> replyDTOs = entity.getReplies().stream()
                        .map(QnaReplyDTO.Response::from)
                        .toList();
                builder.replies(replyDTOs);
            }
            
            return builder.build();
        }
    }
    
    // 게시글 목록 응답 DTO (간소화된 정보)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        
        private Long qnaId;
        private String userNickname;
        private QnaPost.Category category;
        private String title;
        private Boolean isLocked;
        private QnaPost.AnswerStatus answerStatus;
        private Integer viewCount;
        private Integer replyCount;
        private Boolean isOwner;
        private Boolean hasImage; // 이미지 첨부 여부
        
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
        private LocalDateTime createdAt;
        
        public static ListResponse from(QnaPost entity, Long currentUserId) {
            return ListResponse.builder()
                    .qnaId(entity.getQnaId())
                    .userNickname(entity.getUser() != null ? entity.getUser().get유저아이디() : "Unknown")
                    .category(entity.getCategory())
                    .title(entity.getTitle())
                    .isLocked(entity.getIsLocked())
                    .answerStatus(entity.getAnswerStatus())
                    .viewCount(entity.getViewCount())
                    .replyCount(entity.getReplies() != null ? entity.getReplies().size() : 0)
                    .isOwner(entity.isOwner(currentUserId))
                    .hasImage(entity.getImagePath() != null && !entity.getImagePath().isEmpty())
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
        private QnaPost.Category category;
        private QnaPost.AnswerStatus answerStatus;
        private Boolean myPostsOnly; // 내 글만 보기
        
        @Builder.Default
        private int page = 0;
        
        @Builder.Default
        private int size = 5;
    }
}