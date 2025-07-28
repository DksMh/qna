/**
 * QnA 게시판 UI 관리 클래스
 */
class QnaUI {
    constructor() {
        this.currentPage = 0;
        this.currentFilters = {
            keyword: '',
            category: '',
            answerStatus: '',
            myPostsOnly: false
        };
        this.isLoading = false;
        this.editingPostId = null;
        this.editingReplyId = null;
        
        this.initializeElements();
        this.bindEvents();
    }

    /**
     * DOM 요소 초기화
     */
    initializeElements() {
        // 검색 및 필터 요소
        this.searchInput = document.getElementById('searchInput');
        this.searchBtn = document.getElementById('searchBtn');
        this.categoryFilter = document.getElementById('categoryFilter');
        this.statusFilter = document.getElementById('statusFilter');
        this.myPostsOnlyCheckbox = document.getElementById('myPostsOnly');
        this.resetFiltersBtn = document.getElementById('resetFilters');

        // 게시글 관련 요소
        this.writeBtn = document.getElementById('writeBtn');
        this.postsList = document.getElementById('postsList');
        this.pagination = document.getElementById('pagination');
        this.totalCount = document.getElementById('totalCount');
        this.loadingIndicator = document.getElementById('loadingIndicator');

        // 모달 요소
        this.postModal = document.getElementById('postModal');
        this.detailModal = document.getElementById('detailModal');
        this.confirmDialog = document.getElementById('confirmDialog');

        // 폼 요소
        this.postForm = document.getElementById('postForm');
        this.modalTitle = document.getElementById('modalTitle');
        this.submitText = document.getElementById('submitText');

        // 토스트 알림
        this.toast = document.getElementById('toast');
        this.toastIcon = document.getElementById('toastIcon');
        this.toastMessage = document.getElementById('toastMessage');
    }

    /**
     * 이벤트 바인딩
     */
    bindEvents() {
        // 검색 이벤트
        this.searchBtn.addEventListener('click', () => this.handleSearch());
        this.searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.handleSearch();
            }
        });

        // 디바운싱된 실시간 검색
        this.searchInput.addEventListener('input', 
            qnaAPI.debounce(() => this.handleSearch(), 500)
        );

        // 필터 변경 이벤트
        this.categoryFilter.addEventListener('change', () => this.handleFilterChange());
        this.statusFilter.addEventListener('change', () => this.handleFilterChange());
        this.myPostsOnlyCheckbox.addEventListener('change', () => this.handleFilterChange());
        this.resetFiltersBtn.addEventListener('click', () => this.resetFilters());

        // 게시글 작성 버튼
        this.writeBtn.addEventListener('click', () => this.openPostModal());

        // 모달 닫기 이벤트
        document.getElementById('closeModal').addEventListener('click', () => this.closePostModal());
        document.getElementById('closeDetailModal').addEventListener('click', () => this.closeDetailModal());
        document.getElementById('cancelBtn').addEventListener('click', () => this.closePostModal());

        // 모달 외부 클릭 시 닫기
        this.postModal.addEventListener('click', (e) => {
            if (e.target === this.postModal) {
                this.closePostModal();
            }
        });
        this.detailModal.addEventListener('click', (e) => {
            if (e.target === this.detailModal) {
                this.closeDetailModal();
            }
        });

        // 폼 제출 이벤트
        this.postForm.addEventListener('submit', (e) => this.handleFormSubmit(e));

        // 파일 업로드 이벤트
        this.setupFileUpload();

        // 문자 수 카운터
        this.setupCharCounters();

        // 사용자 인증 이벤트 (헤더에서 처리하므로 제거)
        // this.loginBtn.addEventListener('click', () => this.handleLogin());
        // this.logoutBtn.addEventListener('click', () => this.handleLogout());

        // 확인 다이얼로그 이벤트
        document.getElementById('confirmCancel').addEventListener('click', () => this.closeConfirmDialog());
        document.getElementById('confirmOk').addEventListener('click', () => this.handleConfirmOk());
    }

    /**
     * 파일 업로드 설정
     */
    setupFileUpload() {
        const fileUploadArea = document.getElementById('fileUploadArea');
        const fileInput = document.getElementById('postImage');
        const imagePreview = document.getElementById('imagePreview');
        const previewImg = document.getElementById('previewImg');
        const removeImageBtn = document.getElementById('removeImage');

        // 드래그 앤 드롭 이벤트
        fileUploadArea.addEventListener('dragover', (e) => {
            e.preventDefault();
            fileUploadArea.classList.add('dragover');
        });

        fileUploadArea.addEventListener('dragleave', () => {
            fileUploadArea.classList.remove('dragover');
        });

        fileUploadArea.addEventListener('drop', (e) => {
            e.preventDefault();
            fileUploadArea.classList.remove('dragover');
            
            const files = e.dataTransfer.files;
            if (files.length > 0) {
                this.handleFileSelect(files[0]);
            }
        });

        // 파일 선택 이벤트
        fileInput.addEventListener('change', (e) => {
            if (e.target.files.length > 0) {
                this.handleFileSelect(e.target.files[0]);
            }
        });

        // 이미지 제거 버튼
        removeImageBtn.addEventListener('click', () => {
            this.removeSelectedImage();
        });
    }

    /**
     * 파일 선택 처리
     */
    handleFileSelect(file) {
        const validation = qnaAPI.validateFile(file);
        
        if (!validation.isValid) {
            this.showToast(validation.errors.join('\n'), 'error');
            return;
        }

        const fileInput = document.getElementById('postImage');
        const imagePreview = document.getElementById('imagePreview');
        const previewImg = document.getElementById('previewImg');

        // 파일 input에 설정
        const dataTransfer = new DataTransfer();
        dataTransfer.items.add(file);
        fileInput.files = dataTransfer.files;

        // 미리보기 표시
        const previewUrl = qnaAPI.createImagePreviewURL(file);
        previewImg.src = previewUrl;
        imagePreview.style.display = 'block';
        
        // 기존 미리보기 URL 정리
        if (previewImg.dataset.oldUrl) {
            qnaAPI.revokeImagePreviewURL(previewImg.dataset.oldUrl);
        }
        previewImg.dataset.oldUrl = previewUrl;
    }

    /**
     * 선택된 이미지 제거
     */
    removeSelectedImage() {
        const fileInput = document.getElementById('postImage');
        const imagePreview = document.getElementById('imagePreview');
        const previewImg = document.getElementById('previewImg');

        fileInput.value = '';
        imagePreview.style.display = 'none';
        
        if (previewImg.dataset.oldUrl) {
            qnaAPI.revokeImagePreviewURL(previewImg.dataset.oldUrl);
            delete previewImg.dataset.oldUrl;
        }
        
        previewImg.src = '';
    }

    /**
     * 문자 수 카운터 설정
     */
    setupCharCounters() {
        const titleInput = document.getElementById('postTitle');
        const contentTextarea = document.getElementById('postContent');
        const replyTextarea = document.getElementById('replyContent');
        
        const titleCounter = document.getElementById('titleCounter');
        const contentCounter = document.getElementById('contentCounter');
        const replyCounter = document.getElementById('replyCounter');

        titleInput.addEventListener('input', () => {
            titleCounter.textContent = titleInput.value.length;
        });

        contentTextarea.addEventListener('input', () => {
            contentCounter.textContent = contentTextarea.value.length;
        });

        if (replyTextarea && replyCounter) {
            replyTextarea.addEventListener('input', () => {
                replyCounter.textContent = replyTextarea.value.length;
            });
        }
    }

    /**
     * 검색 처리
     */
    handleSearch() {
        this.currentFilters.keyword = this.searchInput.value.trim();
        this.currentPage = 0;
        this.loadPosts();
    }

    /**
     * 필터 변경 처리
     */
    handleFilterChange() {
        this.currentFilters.category = this.categoryFilter.value;
        this.currentFilters.answerStatus = this.statusFilter.value;
        this.currentFilters.myPostsOnly = this.myPostsOnlyCheckbox.checked;
        this.currentPage = 0;
        this.loadPosts();
    }

    /**
     * 필터 초기화
     */
    resetFilters() {
        this.searchInput.value = '';
        this.categoryFilter.value = '';
        this.statusFilter.value = '';
        this.myPostsOnlyCheckbox.checked = false;
        
        this.currentFilters = {
            keyword: '',
            category: '',
            answerStatus: '',
            myPostsOnly: false
        };
        this.currentPage = 0;
        this.loadPosts();
    }

    /**
     * 게시글 목록 로드
     */
    async loadPosts() {
        if (this.isLoading) return;

        this.isLoading = true;
        this.showLoading(true);

        try {
            const params = {
                page: this.currentPage,
                size: 5,
                ...this.currentFilters
            };

            const response = await qnaAPI.getQnaPosts(params);
            
            if (response.success) {
                this.renderPosts(response.data);
                this.renderPagination(response);
                this.totalCount.textContent = response.totalElements || 0;
            }
        } catch (error) {
            console.error('게시글 로드 실패:', error);
            this.showToast('게시글을 불러오는데 실패했습니다.', 'error');
            this.renderEmptyState();
        } finally {
            this.isLoading = false;
            this.showLoading(false);
        }
    }

    /**
     * 게시글 목록 렌더링
     */
    renderPosts(posts) {
        if (!posts || posts.length === 0) {
            this.renderEmptyState();
            return;
        }

        const currentUser = qnaAPI.getCurrentUser();
        
        this.postsList.innerHTML = posts.map(post => `
            <div class="post-item ${post.isOwner ? 'my-post' : ''} ${post.isLocked ? 'locked' : ''}" 
                 data-post-id="${post.qnaId}"
                 onclick="qnaUI.openDetailModal(${post.qnaId})">
                <div class="post-header">
                    <div class="post-meta">
                        <span class="category-badge ${post.category}">${post.category}</span>
                        <span class="status-badge ${post.answerStatus}">${post.answerStatus}</span>
                        ${post.hasImage ? '<i class="fas fa-image" title="이미지 첨부"></i>' : ''}
                        ${post.isLocked ? '<i class="fas fa-lock" title="잠긴 게시글"></i>' : ''}
                    </div>
                </div>
                
                <h3 class="post-title">
                    ${post.isOwner ? '<span class="my-indicator">MY</span>' : ''}
                    ${qnaAPI.stripHtml(post.title)}
                </h3>
                
                <div class="post-info">
                    <span><i class="fas fa-user"></i> ${post.userNickname}</span>
                    <span><i class="fas fa-clock"></i> ${qnaAPI.formatDate(post.createdAt)}</span>
                    <span><i class="fas fa-eye"></i> ${post.viewCount}</span>
                    ${post.replyCount > 0 ? `<span><i class="fas fa-comments"></i> ${post.replyCount}</span>` : ''}
                </div>
            </div>
        `).join('');
    }

    /**
     * 빈 상태 렌더링
     */
    renderEmptyState() {
        this.postsList.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-inbox"></i>
                <h3>게시글이 없습니다</h3>
                <p>첫 번째 문의를 작성해보세요!</p>
            </div>
        `;
    }

    /**
     * 페이지네이션 렌더링
     */
    renderPagination(responseData) {
        const { currentPage, totalPages, hasPrevious, hasNext } = responseData;
        
        if (totalPages <= 1) {
            this.pagination.innerHTML = '';
            return;
        }

        let paginationHTML = '';

        // 이전 페이지 버튼
        paginationHTML += `
            <button class="page-btn" ${!hasPrevious ? 'disabled' : ''} 
                    onclick="qnaUI.goToPage(${currentPage - 1})">
                <i class="fas fa-chevron-left"></i>
            </button>
        `;

        // 페이지 번호 버튼들
        const startPage = Math.max(0, currentPage - 2);
        const endPage = Math.min(totalPages - 1, currentPage + 2);

        if (startPage > 0) {
            paginationHTML += `<button class="page-btn" onclick="qnaUI.goToPage(0)">1</button>`;
            if (startPage > 1) {
                paginationHTML += `<span class="page-btn disabled">...</span>`;
            }
        }

        for (let i = startPage; i <= endPage; i++) {
            paginationHTML += `
                <button class="page-btn ${i === currentPage ? 'active' : ''}" 
                        onclick="qnaUI.goToPage(${i})">
                    ${i + 1}
                </button>
            `;
        }

        if (endPage < totalPages - 1) {
            if (endPage < totalPages - 2) {
                paginationHTML += `<span class="page-btn disabled">...</span>`;
            }
            paginationHTML += `<button class="page-btn" onclick="qnaUI.goToPage(${totalPages - 1})">${totalPages}</button>`;
        }

        // 다음 페이지 버튼
        paginationHTML += `
            <button class="page-btn" ${!hasNext ? 'disabled' : ''} 
                    onclick="qnaUI.goToPage(${currentPage + 1})">
                <i class="fas fa-chevron-right"></i>
            </button>
        `;

        this.pagination.innerHTML = paginationHTML;
    }

    /**
     * 페이지 이동
     */
    goToPage(page) {
        this.currentPage = page;
        this.loadPosts();
    }

    /**
     * 로딩 표시/숨김
     */
    showLoading(show) {
        this.loadingIndicator.style.display = show ? 'flex' : 'none';
    }

    /**
     * 토스트 알림 표시
     */
    showToast(message, type = 'success') {
        this.toast.className = `toast ${type}`;
        this.toastMessage.textContent = message;
        
        // 아이콘 설정
        let iconClass = 'fas fa-check-circle';
        if (type === 'error') {
            iconClass = 'fas fa-exclamation-circle';
        } else if (type === 'warning') {
            iconClass = 'fas fa-exclamation-triangle';
        }
        this.toastIcon.className = `toast-icon ${iconClass}`;
        
        // 토스트 표시
        this.toast.classList.add('show');
        
        // 3초 후 자동 숨김
        setTimeout(() => {
            this.toast.classList.remove('show');
        }, 3000);
    }

    /**
     * 사용자 인터페이스 업데이트
     */
    updateUserInterface() {
        const user = qnaAPI.getCurrentUser();
        
        if (user) {
            // 문의 작성 버튼 표시
            this.writeBtn.style.display = 'inline-flex';
            
            // 내 글만 보기 필터 표시
            if (this.myPostsOnlyCheckbox && this.myPostsOnlyCheckbox.parentElement) {
                this.myPostsOnlyCheckbox.parentElement.style.display = 'flex';
            }
        } else {
            // 문의 작성 버튼 숨김
            this.writeBtn.style.display = 'none';
            
            // 내 글만 보기 필터 숨김
            if (this.myPostsOnlyCheckbox && this.myPostsOnlyCheckbox.parentElement) {
                this.myPostsOnlyCheckbox.parentElement.style.display = 'none';
                this.myPostsOnlyCheckbox.checked = false;
            }
        }
    }

    /**
     * 로그인 처리 (임시)
     */
    handleLogin() {
        // 실제 구현에서는 로그인 페이지로 리다이렉트 또는 로그인 모달 표시
        const token = prompt('JWT 토큰을 입력하세요 (개발용):');
        if (token) {
            qnaAPI.setToken(token);
            this.updateUserInterface();
            this.loadPosts();
            this.showToast('로그인되었습니다.', 'success');
        }
    }

    /**
     * 로그아웃 처리
     */
    handleLogout() {
        qnaAPI.removeToken();
        this.updateUserInterface();
        this.resetFilters();
        this.showToast('로그아웃되었습니다.', 'success');
    }

    /**
     * 게시글 작성 모달 열기
     */
    openPostModal(postId = null) {
        if (!qnaAPI.isLoggedIn()) {
            this.showToast('로그인이 필요합니다.', 'warning');
            return;
        }

        this.editingPostId = postId;
        
        if (postId) {
            this.modalTitle.textContent = '문의 수정';
            this.submitText.textContent = '수정하기';
            this.loadPostForEdit(postId);
        } else {
            this.modalTitle.textContent = '문의 작성';
            this.submitText.textContent = '작성하기';
            this.resetPostForm();
        }
        
        this.postModal.classList.add('show');
    }

    /**
     * 게시글 작성 모달 닫기
     */
    closePostModal() {
        this.postModal.classList.remove('show');
        this.editingPostId = null;
        this.resetPostForm();
    }

    /**
     * 게시글 폼 초기화
     */
    resetPostForm() {
        this.postForm.reset();
        this.removeSelectedImage();
        
        // 기본값 설정
        document.getElementById('isLocked').checked = true;
        
        // 문자 수 카운터 초기화
        document.getElementById('titleCounter').textContent = '0';
        document.getElementById('contentCounter').textContent = '0';
    }

    /**
     * 수정할 게시글 로드
     */
    async loadPostForEdit(postId) {
        try {
            const response = await qnaAPI.getQnaPost(postId);
            
            if (response.success) {
                const post = response.data;
                
                document.getElementById('postCategory').value = post.category;
                document.getElementById('postTitle').value = post.title;
                document.getElementById('postContent').value = post.content;
                document.getElementById('isLocked').checked = post.isLocked;
                
                // 문자 수 카운터 업데이트
                document.getElementById('titleCounter').textContent = post.title.length;
                document.getElementById('contentCounter').textContent = post.content.length;
                
                // 기존 이미지가 있는 경우 미리보기 표시
                if (post.imagePath) {
                    const imagePreview = document.getElementById('imagePreview');
                    const previewImg = document.getElementById('previewImg');
                    
                    previewImg.src = post.imagePath;
                    imagePreview.style.display = 'block';
                }
            }
        } catch (error) {
            console.error('게시글 로드 실패:', error);
            this.showToast('게시글을 불러오는데 실패했습니다.', 'error');
            this.closePostModal();
        }
    }

    /**
     * 폼 제출 처리
     */
    async handleFormSubmit(e) {
        e.preventDefault();
        
        if (!qnaAPI.isLoggedIn()) {
            this.showToast('로그인이 필요합니다.', 'warning');
            return;
        }

        const submitBtn = document.getElementById('submitBtn');
        const originalText = this.submitText.textContent;
        
        try {
            submitBtn.disabled = true;
            this.submitText.textContent = '처리 중...';
            
            const formData = this.getFormData();
            let response;
            
            if (this.editingPostId) {
                response = await qnaAPI.updateQnaPost(this.editingPostId, formData);
            } else {
                response = await qnaAPI.createQnaPost(formData);
            }
            
            if (response.success) {
                const action = this.editingPostId ? '수정' : '작성';
                this.showToast(`게시글이 성공적으로 ${action}되었습니다.`, 'success');
                this.closePostModal();
                this.loadPosts();
            }
        } catch (error) {
            console.error('게시글 저장 실패:', error);
            this.showToast(error.message || '게시글 저장에 실패했습니다.', 'error');
        } finally {
            submitBtn.disabled = false;
            this.submitText.textContent = originalText;
        }
    }

    /**
     * 폼 데이터 추출
     */
    getFormData() {
        const category = document.getElementById('postCategory').value;
        const title = document.getElementById('postTitle').value.trim();
        const content = document.getElementById('postContent').value.trim();
        const isLocked = document.getElementById('isLocked').checked;
        const imageFile = document.getElementById('postImage').files[0];

        return {
            category,
            title,
            content,
            isLocked,
            imageFile
        };
    }

    /**
     * 게시글 상세 모달 열기
     */
    async openDetailModal(postId) {
        try {
            const response = await qnaAPI.getQnaPost(postId);
            
            if (response.success) {
                this.renderPostDetail(response.data);
                this.detailModal.classList.add('show');
                
                // 답변 목록 로드
                await this.loadReplies(postId);
            }
        } catch (error) {
            console.error('게시글 상세 조회 실패:', error);
            
            if (error.message.includes('잠긴 게시글')) {
                this.showToast('잠긴 게시글입니다.', 'warning');
            } else {
                this.showToast('게시글을 불러오는데 실패했습니다.', 'error');
            }
        }
    }

    /**
     * 게시글 상세 내용 렌더링
     */
    renderPostDetail(post) {
        const currentUser = qnaAPI.getCurrentUser();
        const isOwner = currentUser && post.isOwner;
        const isAdmin = currentUser && currentUser.isAdmin;

        // 헤더 정보 설정
        document.getElementById('detailCategory').textContent = post.category;
        document.getElementById('detailCategory').className = `category-badge ${post.category}`;
        
        document.getElementById('detailStatus').textContent = post.answerStatus;
        document.getElementById('detailStatus').className = `status-badge ${post.answerStatus}`;
        
        document.getElementById('detailAuthor').innerHTML = `<i class="fas fa-user"></i> ${post.userNickname}`;
        document.getElementById('detailDate').innerHTML = `<i class="fas fa-clock"></i> ${qnaAPI.formatDate(post.createdAt)}`;
        document.getElementById('detailViews').innerHTML = `<i class="fas fa-eye"></i> ${post.viewCount}`;
        
        document.getElementById('detailTitle').textContent = post.title;

        // 내용 설정
        document.getElementById('detailContentText').textContent = post.content;

        // 이미지 설정
        const detailImage = document.getElementById('detailImage');
        const detailImg = document.getElementById('detailImg');
        
        if (post.imagePath) {
            detailImg.src = post.imagePath;
            detailImage.style.display = 'block';
        } else {
            detailImage.style.display = 'none';
        }

        // 수정/삭제 버튼 표시 (작성자 또는 관리자)
        const editBtn = document.getElementById('editPostBtn');
        const deleteBtn = document.getElementById('deletePostBtn');
        
        if (isOwner || isAdmin) {
            editBtn.style.display = 'inline-flex';
            deleteBtn.style.display = 'inline-flex';
            
            editBtn.onclick = () => {
                this.closeDetailModal();
                this.openPostModal(post.qnaId);
            };
            
            deleteBtn.onclick = () => this.confirmDeletePost(post.qnaId);
        } else {
            editBtn.style.display = 'none';
            deleteBtn.style.display = 'none';
        }

        // 답변 폼 표시 (관리자만)
        const replyForm = document.getElementById('replyForm');
        if (isAdmin) {
            replyForm.style.display = 'block';
            this.setupReplyForm(post.qnaId);
        } else {
            replyForm.style.display = 'none';
        }
    }

    /**
     * 게시글 상세 모달 닫기
     */
    closeDetailModal() {
        this.detailModal.classList.remove('show');
        this.editingReplyId = null;
        
        // 답변 폼 초기화
        const replyContent = document.getElementById('replyContent');
        const replyCounter = document.getElementById('replyCounter');
        
        if (replyContent) {
            replyContent.value = '';
        }
        if (replyCounter) {
            replyCounter.textContent = '0';
        }
    }

    /**
     * 답변 목록 로드
     */
    async loadReplies(qnaId) {
        try {
            const response = await qnaAPI.getReplies(qnaId);
            
            if (response.success) {
                this.renderReplies(response.data);
            }
        } catch (error) {
            console.error('답변 목록 로드 실패:', error);
        }
    }

    /**
     * 답변 목록 렌더링
     */
    renderReplies(replies) {
        const repliesList = document.getElementById('repliesList');
        const currentUser = qnaAPI.getCurrentUser();
        const isAdmin = currentUser && currentUser.isAdmin;

        if (!replies || replies.length === 0) {
            repliesList.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-comments"></i>
                    <h3>답변이 없습니다</h3>
                    <p>관리자의 답변을 기다려주세요.</p>
                </div>
            `;
            return;
        }

        repliesList.innerHTML = replies.map(reply => `
            <div class="reply-item" data-reply-id="${reply.replyId}">
                <div class="reply-header">
                    <span class="reply-author">
                        <i class="fas fa-user-shield"></i>
                        ${reply.adminNickname}
                    </span>
                    <span class="reply-date">${qnaAPI.formatDate(reply.createdAt)}</span>
                </div>
                <div class="reply-content">${reply.replyContent}</div>
                ${isAdmin && currentUser.userId === reply.adminUserPid ? `
                    <div class="reply-actions-inline">
                        <button class="btn btn-small btn-outline" onclick="qnaUI.editReply(${reply.replyId}, '${reply.replyContent.replace(/'/g, "\\'")}')">
                            <i class="fas fa-edit"></i> 수정
                        </button>
                        <button class="btn btn-small btn-danger" onclick="qnaUI.confirmDeleteReply(${reply.replyId})">
                            <i class="fas fa-trash"></i> 삭제
                        </button>
                    </div>
                ` : ''}
            </div>
        `).join('');
    }

    /**
     * 답변 폼 설정
     */
    setupReplyForm(qnaId) {
        const submitReplyBtn = document.getElementById('submitReply');
        
        submitReplyBtn.onclick = async () => {
            await this.handleReplySubmit(qnaId);
        };
    }

    /**
     * 답변 제출 처리
     */
    async handleReplySubmit(qnaId) {
        const replyContent = document.getElementById('replyContent').value.trim();
        
        if (!replyContent) {
            this.showToast('답변 내용을 입력해주세요.', 'warning');
            return;
        }

        const submitBtn = document.getElementById('submitReply');
        const originalText = submitBtn.innerHTML;
        
        try {
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 처리 중...';
            
            const replyData = { replyContent };
            let response;
            
            if (this.editingReplyId) {
                response = await qnaAPI.updateReply(this.editingReplyId, replyData);
            } else {
                response = await qnaAPI.createReply(qnaId, replyData);
            }
            
            if (response.success) {
                const action = this.editingReplyId ? '수정' : '작성';
                this.showToast(`답변이 성공적으로 ${action}되었습니다.`, 'success');
                
                // 답변 목록 새로고침
                await this.loadReplies(qnaId);
                
                // 폼 초기화
                document.getElementById('replyContent').value = '';
                document.getElementById('replyCounter').textContent = '0';
                this.editingReplyId = null;
                
                // 게시글 목록도 새로고침 (답변 상태 업데이트)
                this.loadPosts();
            }
        } catch (error) {
            console.error('답변 저장 실패:', error);
            this.showToast(error.message || '답변 저장에 실패했습니다.', 'error');
        } finally {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
        }
    }

    /**
     * 답변 수정
     */
    editReply(replyId, content) {
        this.editingReplyId = replyId;
        
        const replyContent = document.getElementById('replyContent');
        const replyCounter = document.getElementById('replyCounter');
        const submitBtn = document.getElementById('submitReply');
        
        replyContent.value = content;
        replyCounter.textContent = content.length;
        submitBtn.innerHTML = '<i class="fas fa-save"></i> 수정하기';
        
        // 답변 입력란으로 스크롤
        replyContent.scrollIntoView({ behavior: 'smooth' });
        replyContent.focus();
    }

    /**
     * 게시글 삭제 확인
     */
    confirmDeletePost(postId) {
        this.showConfirmDialog(
            '게시글 삭제',
            '정말로 이 게시글을 삭제하시겠습니까?\n삭제된 게시글은 복구할 수 없습니다.',
            async () => {
                await this.deletePost(postId);
            }
        );
    }

    /**
     * 답변 삭제 확인
     */
    confirmDeleteReply(replyId) {
        this.showConfirmDialog(
            '답변 삭제',
            '정말로 이 답변을 삭제하시겠습니까?\n삭제된 답변은 복구할 수 없습니다.',
            async () => {
                await this.deleteReply(replyId);
            }
        );
    }

    /**
     * 게시글 삭제
     */
    async deletePost(postId) {
        try {
            const response = await qnaAPI.deleteQnaPost(postId);
            
            if (response.success) {
                this.showToast('게시글이 성공적으로 삭제되었습니다.', 'success');
                this.closeDetailModal();
                this.loadPosts();
            }
        } catch (error) {
            console.error('게시글 삭제 실패:', error);
            this.showToast(error.message || '게시글 삭제에 실패했습니다.', 'error');
        }
    }

    /**
     * 답변 삭제
     */
    async deleteReply(replyId) {
        try {
            const response = await qnaAPI.deleteReply(replyId);
            
            if (response.success) {
                this.showToast('답변이 성공적으로 삭제되었습니다.', 'success');
                
                // 현재 보고 있는 게시글의 ID 추출
                const postId = this.getCurrentPostId();
                if (postId) {
                    await this.loadReplies(postId);
                    this.loadPosts(); // 답변 상태 업데이트
                }
            }
        } catch (error) {
            console.error('답변 삭제 실패:', error);
            this.showToast(error.message || '답변 삭제에 실패했습니다.', 'error');
        }
    }

    /**
     * 현재 상세보기 중인 게시글 ID 가져오기
     */
    getCurrentPostId() {
        // 답변 목록에서 첫 번째 답변의 qnaId를 찾거나,
        // 다른 방법으로 현재 게시글 ID를 추적할 수 있습니다.
        const firstReply = document.querySelector('.reply-item');
        if (firstReply) {
            // 실제 구현에서는 더 나은 방법을 사용해야 합니다.
            // 예: 상세 모달에 data 속성으로 postId 저장
            return this.currentDetailPostId;
        }
        return null;
    }

    /**
     * 확인 다이얼로그 표시
     */
    showConfirmDialog(title, message, onConfirm) {
        document.getElementById('confirmTitle').textContent = title;
        document.getElementById('confirmMessage').textContent = message;
        
        this.confirmCallback = onConfirm;
        this.confirmDialog.classList.add('show');
    }

    /**
     * 확인 다이얼로그 닫기
     */
    closeConfirmDialog() {
        this.confirmDialog.classList.remove('show');
        this.confirmCallback = null;
    }

    /**
     * 확인 다이얼로그 확인 버튼 처리
     */
    async handleConfirmOk() {
        if (this.confirmCallback) {
            await this.confirmCallback();
        }
        this.closeConfirmDialog();
    }

    /**
     * 초기화
     */
    async init() {
        this.updateUserInterface();
        await this.loadPosts();
    }
}

// QnaUI 인스턴스를 전역으로 내보내기
window.qnaUI = new QnaUI();