package com.act2gether.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qna_replies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QnaReply {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "답변_id")
    private Long replyId;
    
    @Column(name = "qna_id", nullable = false)
    private Long qnaId;
    
    @Column(name = "관리자_유저_pid", nullable = false)
    private Long adminUserPid;
    
    @Column(name = "답변내용", nullable = false, columnDefinition = "TEXT")
    private String replyContent;
    
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
    @JoinColumn(name = "qna_id", insertable = false, updatable = false)
    private QnaPost qnaPost;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "관리자_유저_pid", insertable = false, updatable = false)
    private User adminUser;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}