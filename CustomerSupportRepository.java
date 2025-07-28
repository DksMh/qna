package com.act2gether.repository;

import com.act2gether.entity.CustomerSupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerSupportRepository extends JpaRepository<CustomerSupport, Integer> {
    
    // 공개 게시글 목록 (비공개가 아닌 글)
    @Query("SELECT cs FROM CustomerSupport cs WHERE cs.isPrivate = false ORDER BY cs.createdAt DESC")
    Page<CustomerSupport> findPublicPosts(Pageable pageable);
    
    // 관리자용 전체 게시글 목록
    Page<CustomerSupport> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // 특정 사용자의 게시글 목록
    Page<CustomerSupport> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
    
    // 카테고리별 공개 게시글
    @Query("SELECT cs FROM CustomerSupport cs WHERE cs.inquiryType = :inquiryType AND cs.isPrivate = false ORDER BY cs.createdAt DESC")
    Page<CustomerSupport> findByInquiryTypeAndPublic(@Param("inquiryType") String inquiryType, Pageable pageable);
    
    // 관리자용 카테고리별 게시글
    Page<CustomerSupport> findByInquiryTypeOrderByCreatedAtDesc(String inquiryType, Pageable pageable);
    
    // 상태별 공개 게시글
    @Query("SELECT cs FROM CustomerSupport cs WHERE cs.status = :status AND cs.isPrivate = false ORDER BY cs.createdAt DESC")
    Page<CustomerSupport> findByStatusAndPublic(@Param("status") String status, Pageable pageable);
    
    // 관리자용 상태별 게시글
    Page<CustomerSupport> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    
    // 제목+내용 검색 (공개 게시글)
    @Query("SELECT cs FROM CustomerSupport cs WHERE cs.isPrivate = false AND " +
           "(LOWER(cs.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(cs.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY cs.createdAt DESC")
    Page<CustomerSupport> searchPublicPosts(@Param("keyword") String keyword, Pageable pageable);
    
    // 관리자용 제목+내용 검색
    @Query("SELECT cs FROM CustomerSupport cs WHERE " +
           "LOWER(cs.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(cs.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY cs.createdAt DESC")
    Page<CustomerSupport> searchAllPosts(@Param("keyword") String keyword, Pageable pageable);
    
    // 사용자별 검색
    @Query("SELECT cs FROM CustomerSupport cs WHERE cs.userId = :userId AND " +
           "(LOWER(cs.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(cs.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY cs.createdAt DESC")
    Page<CustomerSupport> searchByUser(@Param("userId") Integer userId, @Param("keyword") String keyword, Pageable pageable);
    
    // 복합 검색 (카테고리 + 키워드, 공개 게시글)
    @Query("SELECT cs FROM CustomerSupport cs WHERE cs.inquiryType = :inquiryType AND cs.isPrivate = false AND " +
           "(LOWER(cs.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(cs.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY cs.createdAt DESC")
    Page<CustomerSupport> searchByInquiryTypeAndPublic(@Param("inquiryType") String inquiryType, 
                                                       @Param("keyword") String keyword, 
                                                       Pageable pageable);
    
    // 관리자용 복합 검색
    @Query("SELECT cs FROM CustomerSupport cs WHERE cs.inquiryType = :inquiryType AND " +
           "(LOWER(cs.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(cs.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY cs.createdAt DESC")
    Page<CustomerSupport> searchByInquiryTypeAndKeyword(@Param("inquiryType") String inquiryType, 
                                                        @Param("keyword") String keyword, 
                                                        Pageable pageable);
    
    // 권한 체크와 함께 조회
    @Query("SELECT cs FROM CustomerSupport cs WHERE cs.supportId = :supportId AND " +
           "(cs.isPrivate = false OR cs.userId = :currentUserId OR :isAdmin = true)")
    CustomerSupport findByIdWithPermissionCheck(@Param("supportId") Integer supportId, 
                                               @Param("currentUserId") Integer currentUserId, 
                                               @Param("isAdmin") boolean isAdmin);
    
    // 답변 대기 중인 게시글 수 (관리자용)
    @Query("SELECT COUNT(cs) FROM CustomerSupport cs WHERE cs.status = '답변대기'")
    Long countPendingPosts();
    
    // 특정 사용자의 게시글 수
    Long countByUserId(Integer userId);
}