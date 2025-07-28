package com.act2gether.repository;

import com.act2gether.entity.QnaReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QnaReplyRepository extends JpaRepository<QnaReply, Long> {
    
    // 특정 QnA 게시글의 모든 답변 조회 (최신순)
    List<QnaReply> findByQnaIdOrderByCreatedAtDesc(Long qnaId);
    
    // 특정 QnA 게시글의 답변 개수
    Long countByQnaId(Long qnaId);
    
    // 특정 관리자가 작성한 답변 조회
    List<QnaReply> findByAdminUserPidOrderByCreatedAtDesc(Long adminUserPid);
    
    // QnA별 최신 답변 조회
    @Query("SELECT r FROM QnaReply r WHERE r.qnaId = :qnaId ORDER BY r.createdAt DESC LIMIT 1")
    QnaReply findLatestReplyByQnaId(@Param("qnaId") Long qnaId);
}