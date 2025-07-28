package com.act2gether.service;

import com.act2gether.dto.QnaPostDTO;
import com.act2gether.entity.QnaPost;
import com.act2gether.repository.QnaPostRepository;
import com.act2gether.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QnaPostService {
    
    private final QnaPostRepository qnaPostRepository;
    private final FileUploadUtil fileUploadUtil;
    
    /**
     * QnA 게시글 목록 조회 (페이징)
     */
    public Page<QnaPostDTO.ListResponse> getQnaPosts(QnaPostDTO.SearchRequest searchRequest, Long currentUserId, boolean isAdmin) {
        Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize());
        Page<QnaPost> posts;
        
        // 검색 조건에 따른 쿼리 분기
        if (searchRequest.getMyPostsOnly() != null && searchRequest.getMyPostsOnly()) {
            // 내 글만 보기
            posts = getMyPosts(searchRequest, currentUserId, pageable);
        } else if (isAdmin) {
            // 관리자는 모든 글 조회 가능
            posts = getAdminPosts(searchRequest, pageable);
        } else {
            // 일반 사용자는 잠금 해제된 글만 조회
            posts = getPublicPosts(searchRequest, pageable);
        }
        
        return posts.map(post -> QnaPostDTO.ListResponse.from(post, currentUserId));
    }
    
    /**
     * QnA 게시글 상세 조회
     */
    @Transactional
    public QnaPostDTO.Response getQnaPost(Long qnaId, Long currentUserId, boolean isAdmin) {
        QnaPost post = qnaPostRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        
        // 접근 권한 확인
        if (!post.canView(currentUserId, isAdmin)) {
            throw new IllegalArgumentException("잠긴 게시글입니다.");
        }
        
        // 조회수 증가 (본인 글이 아닌 경우만)
        if (!post.isOwner(currentUserId)) {
            post.incrementViewCount();
            qnaPostRepository.save(post);
        }
        
        return QnaPostDTO.Response.from(post, currentUserId, true);
    }
    
    /**
     * QnA 게시글 생성
     */
    @Transactional
    public QnaPostDTO.Response createQnaPost(QnaPostDTO.CreateRequest request, Long userId) {
        String imagePath = null;
        
        // 이미지 업로드 처리
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            try {
                imagePath = fileUploadUtil.uploadQnaImage(request.getImageFile(), userId);
            } catch (IOException e) {
                log.error("이미지 업로드 실패: ", e);
                throw new RuntimeException("이미지 업로드에 실패했습니다.");
            }
        }
        
        QnaPost post = QnaPost.builder()
                .userPid(userId)
                .category(request.getCategory())
                .title(request.getTitle())
                .content(request.getContent())
                .imagePath(imagePath)
                .isLocked(request.getIsLocked())
                .build();
        
        QnaPost savedPost = qnaPostRepository.save(post);
        return QnaPostDTO.Response.from(savedPost, userId, false);
    }
    
    /**
     * QnA 게시글 수정
     */
    @Transactional
    public QnaPostDTO.Response updateQnaPost(Long qnaId, QnaPostDTO.UpdateRequest request, Long userId, boolean isAdmin) {
        QnaPost post = qnaPostRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        
        // 수정 권한 확인 (작성자 또는 관리자)
        if (!post.isOwner(userId) && !isAdmin) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }
        
        // 필드 업데이트
        if (request.getCategory() != null) {
            post.setCategory(request.getCategory());
        }
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            post.setTitle(request.getTitle());
        }
        if (request.getContent() != null && !request.getContent().trim().isEmpty()) {
            post.setContent(request.getContent());
        }
        if (request.getIsLocked() != null) {
            post.setIsLocked(request.getIsLocked());
        }
        
        // 이미지 처리
        handleImageUpdate(post, request, userId);
        
        QnaPost updatedPost = qnaPostRepository.save(post);
        return QnaPostDTO.Response.from(updatedPost, userId, true);
    }
    
    /**
     * QnA 게시글 삭제
     */
    @Transactional
    public void deleteQnaPost(Long qnaId, Long userId, boolean isAdmin) {
        QnaPost post = qnaPostRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        
        // 삭제 권한 확인 (작성자 또는 관리자)
        if (!post.isOwner(userId) && !isAdmin) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }
        
        // 이미지 파일 삭제
        if (post.getImagePath() != null && !post.getImagePath().isEmpty()) {
            try {
                fileUploadUtil.deleteFile(post.getImagePath());
            } catch (Exception e) {
                log.warn("이미지 파일 삭제 실패: " + post.getImagePath(), e);
            }
        }
        
        qnaPostRepository.delete(post);
    }
    
    // Private helper methods
    
    private Page<QnaPost> getMyPosts(QnaPostDTO.SearchRequest searchRequest, Long userId, Pageable pageable) {
        if (searchRequest.getKeyword() != null && !searchRequest.getKeyword().trim().isEmpty()) {
            return qnaPostRepository.searchMyPosts(userId, searchRequest.getKeyword().trim(), pageable);
        }
        return qnaPostRepository.findByUserPidOrderByCreatedAtDesc(userId, pageable);
    }
    
    private Page<QnaPost> getAdminPosts(QnaPostDTO.SearchRequest searchRequest, Pageable pageable) {
        String keyword = searchRequest.getKeyword();
        QnaPost.Category category = searchRequest.getCategory();
        QnaPost.AnswerStatus status = searchRequest.getAnswerStatus();
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            if (category != null) {
                return qnaPostRepository.searchByCategoryAndKeyword(category, keyword.trim(), pageable);
            }
            return qnaPostRepository.searchAllPosts(keyword.trim(), pageable);
        }
        
        if (category != null) {
            return qnaPostRepository.findByCategoryOrderByCreatedAtDesc(category, pageable);
        }
        
        if (status != null) {
            return qnaPostRepository.findByAnswerStatusOrderByCreatedAtDesc(status, pageable);
        }
        
        return qnaPostRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    private Page<QnaPost> getPublicPosts(QnaPostDTO.SearchRequest searchRequest, Pageable pageable) {
        String keyword = searchRequest.getKeyword();
        QnaPost.Category category = searchRequest.getCategory();
        QnaPost.AnswerStatus status = searchRequest.getAnswerStatus();
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            if (category != null) {
                return qnaPostRepository.searchByCategoryAndUnlocked(category, keyword.trim(), pageable);
            }
            return qnaPostRepository.searchUnlockedPosts(keyword.trim(), pageable);
        }
        
        if (category != null) {
            return qnaPostRepository.findByCategoryAndUnlocked(category, pageable);
        }
        
        if (status != null) {
            return qnaPostRepository.findByAnswerStatusAndUnlocked(status, pageable);
        }
        
        return qnaPostRepository.findAllUnlockedPosts(pageable);
    }
    
    private void handleImageUpdate(QnaPost post, QnaPostDTO.UpdateRequest request, Long userId) {
        // 기존 이미지 삭제 요청 처리
        if (request.getDeleteImage() != null && request.getDeleteImage() && post.getImagePath() != null) {
            try {
                fileUploadUtil.deleteFile(post.getImagePath());
                post.setImagePath(null);
            } catch (Exception e) {
                log.warn("기존 이미지 삭제 실패: " + post.getImagePath(), e);
            }
        }
        
        // 새 이미지 업로드
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            try {
                // 기존 이미지가 있다면 삭제
                if (post.getImagePath() != null) {
                    fileUploadUtil.deleteFile(post.getImagePath());
                }
                
                String newImagePath = fileUploadUtil.uploadQnaImage(request.getImageFile(), userId);
                post.setImagePath(newImagePath);
            } catch (IOException e) {
                log.error("새 이미지 업로드 실패: ", e);
                throw new RuntimeException("이미지 업로드에 실패했습니다.");
            }
        }
    }
}