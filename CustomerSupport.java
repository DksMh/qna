package com.act2gether.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_support")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSupport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "support_id")
    private Integer supportId;
    
    @Column(name = "user_id")
    private Integer userId;
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "responder")
    private String responder;
    
    @Column(name = "response", columnDefinition = "TEXT")
    private String response;
    
    @Column(name = "inquiry_type")
    private String inquiryType; // "일반문의", "신고"
    
    @Column(name = "status")
    private String status; // "답변대기", "답변완료"
    
    @Column(name = "is_private")
    private Boolean isPrivate; // 비공개 여부 (잠금 기능)
    
    @Column(name = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // 연관관계 매핑 (선택사항)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    // 비즈니스 로직 메서드들
    public boolean isOwner(Integer currentUserId) {
        return this.userId != null && this.userId.equals(currentUserId);
    }
    
    public boolean canView(Integer currentUserId, boolean isAdmin) {
        // 관리자는 모든 글 볼 수 있음
        if (isAdmin) return true;
        
        // 공개 글은 누구나 볼 수 있음
        if (!Boolean.TRUE.equals(isPrivate)) return true;
        
        // 비공개 글은 작성자만 볼 수 있음
        return isOwner(currentUserId);
    }
    
    public boolean hasResponse() {
        return response != null && !response.trim().isEmpty();
    }
    
    public void markAsAnswered() {
        this.status = "답변완료";
        this.updatedAt = LocalDateTime.now();
    }
    
    public void markAsWaiting() {
        this.status = "답변대기";
        this.updatedAt = LocalDateTime.now();
    }
    
    // 답변 추가/수정
    public void setResponseData(String responderName, String responseContent) {
        this.responder = responderName;
        this.response = responseContent;
        this.markAsAnswered();
    }
    
    // 답변 삭제
    public void clearResponse() {
        this.responder = null;
        this.response = null;
        this.markAsWaiting();
    }
    
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        
        // 기본값 설정
        if (this.status == null) {
            this.status = "답변대기";
        }
        if (this.isPrivate == null) {
            this.isPrivate = true; // 기본적으로 비공개
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}