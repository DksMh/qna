/**
 * QnA API 통신을 위한 클래스
 */
class QnaAPI {
    constructor() {
        this.baseURL = '/api/qna';
        this.token = this.getTokenFromStorage();
    }

    /**
     * 로컬 스토리지에서 JWT 토큰 가져오기
     */
    getTokenFromStorage() {
        return localStorage.getItem('jwt-token') || sessionStorage.getItem('jwt-token');
    }

    /**
     * JWT 토큰 설정
     */
    setToken(token) {
        this.token = token;
        localStorage.setItem('jwt-token', token);
    }

    /**
     * JWT 토큰 제거
     */
    removeToken() {
        this.token = null;
        localStorage.removeItem('jwt-token');
        sessionStorage.removeItem('jwt-token');
    }

    /**
     * HTTP 요청 헤더 생성
     */
    getHeaders(isFormData = false) {
        const headers = {};
        
        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }
        
        if (!isFormData) {
            headers['Content-Type'] = 'application/json';
        }
        
        return headers;
    }

    /**
     * API 요청 실행
     */
    async request(method, url, data = null, isFormData = false) {
        try {
            const config = {
                method: method.toUpperCase(),
                headers: this.getHeaders(isFormData)
            };

            if (data) {
                if (isFormData) {
                    config.body = data;
                } else {
                    config.body = JSON.stringify(data);
                }
            }

            const response = await fetch(url, config);
            const result = await response.json();

            if (!response.ok) {
                throw new Error(result.message || `HTTP ${response.status}`);
            }

            return result;
        } catch (error) {
            console.error('API 요청 실패:', error);
            throw error;
        }
    }

    /**
     * QnA 게시글 목록 조회
     */
    async getQnaPosts(params = {}) {
        const searchParams = new URLSearchParams();
        
        Object.keys(params).forEach(key => {
            if (params[key] !== null && params[key] !== undefined && params[key] !== '') {
                searchParams.append(key, params[key]);
            }
        });

        const url = `${this.baseURL}?${searchParams.toString()}`;
        return await this.request('GET', url);
    }

    /**
     * QnA 게시글 상세 조회
     */
    async getQnaPost(qnaId) {
        const url = `${this.baseURL}/${qnaId}`;
        return await this.request('GET', url);
    }

    /**
     * QnA 게시글 생성
     */
    async createQnaPost(postData) {
        const formData = new FormData();
        
        // 텍스트 데이터 추가
        formData.append('category', postData.category);
        formData.append('title', postData.title);
        formData.append('content', postData.content);
        formData.append('isLocked', postData.isLocked);
        
        // 이미지 파일 추가 (있는 경우)
        if (postData.imageFile) {
            formData.append('imageFile', postData.imageFile);
        }

        return await this.request('POST', this.baseURL, formData, true);
    }

    /**
     * QnA 게시글 수정
     */
    async updateQnaPost(qnaId, postData) {
        const formData = new FormData();
        
        // 수정된 데이터만 추가
        if (postData.category) {
            formData.append('category', postData.category);
        }
        if (postData.title) {
            formData.append('title', postData.title);
        }
        if (postData.content) {
            formData.append('content', postData.content);
        }
        if (postData.isLocked !== undefined) {
            formData.append('isLocked', postData.isLocked);
        }
        if (postData.deleteImage) {
            formData.append('deleteImage', postData.deleteImage);
        }
        if (postData.imageFile) {
            formData.append('imageFile', postData.imageFile);
        }

        const url = `${this.baseURL}/${qnaId}`;
        return await this.request('PUT', url, formData, true);
    }

    /**
     * QnA 게시글 삭제
     */
    async deleteQnaPost(qnaId) {
        const url = `${this.baseURL}/${qnaId}`;
        return await this.request('DELETE', url);
    }

    /**
     * QnA 답변 목록 조회
     */
    async getReplies(qnaId) {
        const url = `${this.baseURL}/${qnaId}/replies`;
        return await this.request('GET', url);
    }

    /**
     * QnA 답변 생성 (관리자만)
     */
    async createReply(qnaId, replyData) {
        const url = `${this.baseURL}/${qnaId}/replies`;
        return await this.request('POST', url, replyData);
    }

    /**
     * QnA 답변 수정 (관리자만)
     */
    async updateReply(replyId, replyData) {
        const url = `${this.baseURL}/replies/${replyId}`;
        return await this.request('PUT', url, replyData);
    }

    /**
     * QnA 답변 삭제 (관리자만)
     */
    async deleteReply(replyId) {
        const url = `${this.baseURL}/replies/${replyId}`;
        return await this.request('DELETE', url);
    }

    /**
     * 파일 크기 검증
     */
    validateFileSize(file, maxSizeMB = 3) {
        const maxSizeBytes = maxSizeMB * 1024 * 1024;
        return file.size <= maxSizeBytes;
    }

    /**
     * 이미지 파일 형식 검증
     */
    validateImageType(file) {
        const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
        return allowedTypes.includes(file.type);
    }

    /**
     * 파일 검증
     */
    validateFile(file) {
        const errors = [];

        if (!this.validateFileSize(file)) {
            errors.push('파일 크기는 3MB 이하여야 합니다.');
        }

        if (!this.validateImageType(file)) {
            errors.push('JPG, PNG, WebP 파일만 업로드 가능합니다.');
        }

        return {
            isValid: errors.length === 0,
            errors
        };
    }

    /**
     * 이미지 미리보기 URL 생성
     */
    createImagePreviewURL(file) {
        return URL.createObjectURL(file);
    }

    /**
     * 이미지 미리보기 URL 해제
     */
    revokeImagePreviewURL(url) {
        URL.revokeObjectURL(url);
    }

    /**
     * 현재 사용자 정보 확인 (JWT 토큰 디코딩)
     */
    getCurrentUser() {
        if (!this.token) {
            return null;
        }

        try {
            // JWT 토큰의 payload 부분 디코딩
            const payload = JSON.parse(atob(this.token.split('.')[1]));
            
            return {
                userId: payload.userId,
                userAccount: payload.userAccount,
                isAdmin: payload.userAccount === 'admin',
                exp: payload.exp
            };
        } catch (error) {
            console.error('토큰 디코딩 실패:', error);
            this.removeToken();
            return null;
        }
    }

    /**
     * 토큰 만료 확인
     */
    isTokenExpired() {
        const user = this.getCurrentUser();
        if (!user) {
            return true;
        }

        const currentTime = Math.floor(Date.now() / 1000);
        return user.exp < currentTime;
    }

    /**
     * 로그인 상태 확인
     */
    isLoggedIn() {
        return this.token && !this.isTokenExpired();
    }

    /**
     * 관리자 권한 확인
     */
    isAdmin() {
        const user = this.getCurrentUser();
        return user && user.isAdmin;
    }

    /**
     * 날짜 포맷팅
     */
    formatDate(dateString) {
        const date = new Date(dateString);
        const now = new Date();
        const diffTime = now - date;
        const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

        if (diffDays === 0) {
            // 오늘: 시간만 표시
            return date.toLocaleTimeString('ko-KR', {
                hour: '2-digit',
                minute: '2-digit'
            });
        } else if (diffDays === 1) {
            // 어제
            return '어제 ' + date.toLocaleTimeString('ko-KR', {
                hour: '2-digit',
                minute: '2-digit'
            });
        } else if (diffDays < 7) {
            // 일주일 이내: 요일과 시간
            return date.toLocaleDateString('ko-KR', {
                weekday: 'short',
                hour: '2-digit',
                minute: '2-digit'
            });
        } else {
            // 일주일 이후: 전체 날짜
            return date.toLocaleDateString('ko-KR', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
            });
        }
    }

    /**
     * 텍스트 길이 제한 및 말줄임표 처리
     */
    truncateText(text, maxLength = 100) {
        if (text.length <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + '...';
    }

    /**
     * HTML 태그 제거
     */
    stripHtml(html) {
        const div = document.createElement('div');
        div.innerHTML = html;
        return div.textContent || div.innerText || '';
    }

    /**
     * 디바운싱 함수
     */
    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    /**
     * 로컬 스토리지 안전 사용
     */
    safeLocalStorage = {
        getItem: (key) => {
            try {
                return localStorage.getItem(key);
            } catch (error) {
                console.warn('localStorage getItem 실패:', error);
                return null;
            }
        },

        setItem: (key, value) => {
            try {
                localStorage.setItem(key, value);
                return true;
            } catch (error) {
                console.warn('localStorage setItem 실패:', error);
                return false;
            }
        },

        removeItem: (key) => {
            try {
                localStorage.removeItem(key);
                return true;
            } catch (error) {
                console.warn('localStorage removeItem 실패:', error);
                return false;
            }
        }
    };
}

// QnaAPI 인스턴스를 전역으로 내보내기
window.qnaAPI = new QnaAPI();