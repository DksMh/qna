package com.act2gether.service;

import com.act2gether.dto.QnaReplyDTO;
import com.act2gether.entity.QnaPost;
import com.act2gether.entity.QnaReply;
import com.act2gether.repository.QnaPostRepository;
import com.act2gether.repository.QnaReplyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QnaReplyService {
    
    private final QnaReplyRepository qnaReplyRepository;
    private final QnaPostRepository qnaPostRepository;
    
    /**
     * QnA 답변 목록 조회
     */
    public List<QnaReplyDTO.Response> getRepliesByQnaId(Long qnaId) {
        List<QnaReply> replies = qnaReplyRepository.findByQnaIdOrderByCreatedAtDesc(qnaId);
        return replies.stream()
                .map(QnaReplyDTO.Response::from)
                .toList();
    }
    
    /**
     * QnA 답변 생성 (관리자만)
     */
    @Transactional
    public QnaReplyDTO.Response createReply(Long qnaId, QnaReplyDTO.CreateRequest request, Long adminUserId, boolean isAdmin) {
        // 관리자 권한 확인
        if (!isAdmin) {
            throw new IllegalArgumentException("답변 작성 권한이 없습니다.");
        }
        
        // QnA 게시글 존재 확인
        QnaPost qnaPost = qnaPostRepository.findById(qnaId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        
        // 답변 생성
        QnaReply reply = QnaReply.builder()
                .qnaId(qnaId)
                .adminUserPid(adminUserId)
                .replyContent(request.getReplyContent())
                .build();
        
        QnaReply savedReply = qnaReplyRepository.save(reply);
        
        // QnA 게시글 답변 상태 업데이트
        qnaPost.setAnswerStatus(QnaPost.AnswerStatus.답변완료);
        qnaPostRepository.save(qnaPost);
        
        log.info("QnA 답변 생성 완료 - QnA ID: {}, 관리자 ID: {}", qnaId, adminUserId);
        
        return QnaReplyDTO.Response.from(savedReply);
    }
    
    /**
     * QnA 답변 수정 (관리자만)
     */
    @Transactional
    public QnaReplyDTO.Response updateReply(Long replyId, QnaReplyDTO.UpdateRequest request, Long adminUserId, boolean isAdmin) {
        // 관리자 권한 확인
        if (!isAdmin) {
            throw new IllegalArgumentException("답변 수정 권한이 없습니다.");
        }
        
        QnaReply reply = qnaReplyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("답변을 찾을 수 없습니다."));
        
        // 답변 작성자 확인 (다른 관리자가 작성한 답변 수정 방지)
        if (!reply.getAdminUserPid().equals(adminUserId)) {
            throw new IllegalArgumentException("다른 관리자의 답변은 수정할 수 없습니다.");
        }
        
        // 답변 내용 업데이트
        reply.setReplyContent(request.getReplyContent());
        
        QnaReply updatedReply = qnaReplyRepository.save(reply);
        
        log.info("QnA 답변 수정 완료 - 답변 ID: {}, 관리자 ID: {}", replyId, adminUserId);
        
        return QnaReplyDTO.Response.from(updatedReply);
    }
    
    /**
     * QnA 답변 삭제 (관리자만)
     */
    @Transactional
    public void deleteReply(Long replyId, Long adminUserId, boolean isAdmin) {
        // 관리자 권한 확인
        if (!isAdmin) {
            throw new IllegalArgumentException("답변 삭제 권한이 없습니다.");
        }
        
        QnaReply reply = qnaReplyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("답변을 찾을 수 없습니다."));
        
        // 답변 작성자 확인 (다른 관리자가 작성한 답변 삭제 방지)
        if (!reply.getAdminUserPid().equals(adminUserId)) {
            throw new IllegalArgumentException("다른 관리자의 답변은 삭제할 수 없습니다.");
        }
        
        Long qnaId = reply.getQnaId();
        
        // 답변 삭제
        qnaReplyRepository.delete(reply);
        
        // 해당 QnA의 다른 답변이 있는지 확인
        Long remainingReplyCount = qnaReplyRepository.countByQnaId(qnaId);
        
        // 답변이 더 이상 없으면 QnA 상태를 '답변대기'로 변경
        if (remainingReplyCount == 0) {
            QnaPost qnaPost = qnaPostRepository.findById(qnaId)
                    .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
            qnaPost.setAnswerStatus(QnaPost.AnswerStatus.답변대기);
            qnaPostRepository.save(qnaPost);
        }
        
        log.info("QnA 답변 삭제 완료 - 답변 ID: {}, 관리자 ID: {}", replyId, adminUserId);
    }
    
    /**
     * 특정 QnA의 답변 개수 조회
     */
    public Long getReplyCount(Long qnaId) {
        return qnaReplyRepository.countByQnaId(qnaId);
    }
    
    /**
     * 특정 관리자가 작성한 답변 목록 조회
     */
    public List<QnaReplyDTO.Response> getRepliesByAdmin(Long adminUserId) {
        List<QnaReply> replies = qnaReplyRepository.findByAdminUserPidOrderByCreatedAtDesc(adminUserId);
        return replies.stream()
                .map(QnaReplyDTO.Response::from)
                .toList();
    }
}