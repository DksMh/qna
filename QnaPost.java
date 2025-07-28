package com.act2gether.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "qna_posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QnaPost {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qna_id")
    private Long qnaId;
    
    @Column(name = "유저_pid", nullable = false)
    private Long userPid;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "카테고리", nullable = false)
    private Category category;
    
    @Column(name = "제목", nullable = false, length = 255)
    private String title;
    
    @Column(name = "내용", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "이미지경로", length = 500)
    private String imagePath;
    
    @Column(name = "잠금여부", nullable = false)
    @Builder.Default
    private Boolean isLocked = true;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "답변상태")
    @Builder.Default
    private AnswerStatus answerStatus = AnswerStatus.답변대기;
    
    @Column(name = "조회수")
    @Builder.Default
    private Integer viewCount = 0;
    
    @Column(name = "생성날", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "업데이트날")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // 연관관계 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "유저_pid", insertable = false, updatable = false)
    private User user;
    
    @OneToMany(mappedBy = "qnaPost", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QnaReply> replies;
    
    // Enum 정의
    public enum Category {
        일반문의, 신고
    }
    
    public enum AnswerStatus {
        답변대기, 답변완료
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // 비즈니스 로직
    public boolean isOwner(Long userId) {
        return this.userPid.equals(userId);
    }
    
    public boolean canView(Long userId, boolean isAdmin) {
        if (isAdmin) return true;
        if (!isLocked) return true;
        return isOwner(userId);
    }
    
    public void incrementViewCount() {
        this.viewCount++;
    }
}