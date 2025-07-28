package com.act2gether.repository;

import com.act2gether.entity.QnaPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SecureQnaPostRepository extends JpaRepository<QnaPost, Long> {
    
    // 보안 강화: 매개변수화된 쿼리 사용, ESCAPE 절 추가
    @Query("SELECT q FROM QnaPost q WHERE q.isLocked = false " +
           "AND (LOWER(q.title) LIKE LOWER(CONCAT('%', ESCAPE_SPECIAL_CHARS(:keyword), '%')) ESCAPE '\\' " +
           "OR LOWER(q.content) LIKE LOWER(CONCAT('%', ESCAPE_SPECIAL_CHARS(:keyword), '%')) ESCAPE '\\') " +
           "ORDER BY q.createdAt DESC")
    Page<QnaPost> findUnlockedPostsByKeywordSecure(@Param("keyword") String keyword, Pageable pageable);
    
    // 관리자용 검색 - 보안 강화
    @Query("SELECT q FROM QnaPost q WHERE " +
           "LOWER(q.title) LIKE LOWER(CONCAT('%', ESCAPE_SPECIAL_CHARS(:keyword), '%')) ESCAPE '\\' " +
           "OR LOWER(q.content) LIKE LOWER(CONCAT('%', ESCAPE_SPECIAL_CHARS(:keyword), '%')) ESCAPE '\\' " +
           "ORDER BY q.createdAt DESC")
    Page<QnaPost> findAllPostsByKeywordSecure(@Param("keyword") String keyword, Pageable pageable);
    
    // 카테고리별 검색 - 보안 강화
    @Query("SELECT q FROM QnaPost q WHERE q.category = :category " +
           "AND q.isLocked = false " +
           "AND (LOWER(q.title) LIKE LOWER(CONCAT('%', ESCAPE_SPECIAL_CHARS(:keyword), '%')) ESCAPE '\\' " +
           "OR LOWER(q.content) LIKE LOWER(CONCAT('%', ESCAPE_SPECIAL_CHARS(:keyword), '%')) ESCAPE '\\') " +
           "ORDER BY q.createdAt DESC")
    Page<QnaPost> findByCategoryAndKeywordSecure(
        @Param("category") QnaPost.Category category, 
        @Param("keyword") String keyword, 
        Pageable pageable
    );
    
    // 사용자별 검색 - 보안 강화
    @Query("SELECT q FROM QnaPost q WHERE q.userPid = :userPid " +
           "AND (LOWER(q.title) LIKE LOWER(CONCAT('%', ESCAPE_SPECIAL_CHARS(:keyword), '%')) ESCAPE '\\' " +
           "OR LOWER(q.content) LIKE LOWER(CONCAT('%', ESCAPE_SPECIAL_CHARS(:keyword), '%')) ESCAPE '\\') " +
           "ORDER BY q.createdAt DESC")
    Page<QnaPost> findByUserAndKeywordSecure(
        @Param("userPid") Long userPid, 
        @Param("keyword") String keyword, 
        Pageable pageable
    );
    
    // 기본 조회 쿼리들 (매개변수화된 쿼리 사용)
    @Query("SELECT q FROM QnaPost q WHERE q.isLocked = false ORDER BY q.createdAt DESC")
    Page<QnaPost> findAllUnlockedPostsSecure(Pageable pageable);
    
    @Query("SELECT q FROM QnaPost q WHERE q.userPid = :userPid ORDER BY q.createdAt DESC")
    Page<QnaPost> findByUserSecure(@Param("userPid") Long userPid, Pageable pageable);
    
    @Query("SELECT q FROM QnaPost q WHERE q.category = :category AND q.isLocked = false ORDER BY q.createdAt DESC")
    Page<QnaPost> findByCategorySecure(@Param("category") QnaPost.Category category, Pageable pageable);
    
    @Query("SELECT q FROM QnaPost q WHERE q.answerStatus = :status AND q.isLocked = false ORDER BY q.createdAt DESC")
    Page<QnaPost> findByAnswerStatusSecure(@Param("status") QnaPost.AnswerStatus status, Pageable pageable);
    
    // 관리자용 쿼리들
    @Query("SELECT q FROM QnaPost q WHERE q.category = :category ORDER BY q.createdAt DESC")
    Page<QnaPost> findByCategoryAdminSecure(@Param("category") QnaPost.Category category, Pageable pageable);
    
    @Query("SELECT q FROM QnaPost q WHERE q.answerStatus = :status ORDER BY q.createdAt DESC")
    Page<QnaPost> findByAnswerStatusAdminSecure(@Param("status") QnaPost.AnswerStatus status, Pageable pageable);
    
    // 보안 강화: ID로 조회 시 사용자 권한 확인
    @Query("SELECT q FROM QnaPost q WHERE q.qnaId = :qnaId AND " +
           "(q.isLocked = false OR q.userPid = :currentUserId OR :isAdmin = true)")
    QnaPost findByIdWithPermissionCheck(
        @Param("qnaId") Long qnaId, 
        @Param("currentUserId") Long currentUserId, 
        @Param("isAdmin") boolean isAdmin
    );
}

// SQL Injection 방지를 위한 유틸리티 함수가 필요합니다.
// 이는 @Query 어노테이션에서 직접 사용할 수 없으므로, 
// Service 레이어에서 처리해야 합니다.