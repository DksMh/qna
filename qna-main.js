/**
 * QnA 게시판 메인 스크립트
 * 페이지 로드 시 초기화 및 전역 이벤트 처리
 */

// 페이지 로드 완료 시 초기화
document.addEventListener('DOMContentLoaded', async function() {
    try {
        console.log('QnA 게시판 초기화 시작...');
        
        // QnaUI 초기화
        await qnaUI.init();
        
        // 키보드 단축키 설정
        setupKeyboardShortcuts();
        
        // 전역 에러 핸들러 설정
        setupGlobalErrorHandler();
        
        // 브라우저 뒤로가기/앞으로가기 처리
        setupBrowserNavigation();
        
        // 페이지 가시성 변경 처리
        setupPageVisibilityHandler();
        
        console.log('QnA 게시판 초기화 완료');
        
    } catch (error) {
        console.error('QnA 게시판 초기화 실패:', error);
        qnaUI.showToast('페이지 초기화에 실패했습니다. 새로고침해주세요.', 'error');
    }
});

/**
 * 키보드 단축키 설정
 */
function setupKeyboardShortcuts() {
    document.addEventListener('keydown', function(e) {
        // Ctrl/Cmd + Enter: 폼 제출
        if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
            const activeModal = document.querySelector('.modal.show');
            if (activeModal) {
                e.preventDefault();
                
                if (activeModal.id === 'postModal') {
                    // 게시글 작성/수정 폼 제출
                    const submitBtn = document.getElementById('submitBtn');
                    if (submitBtn && !submitBtn.disabled) {
                        submitBtn.click();
                    }
                } else if (activeModal.id === 'detailModal') {
                    // 답변 작성 폼 제출 (관리자인 경우)
                    const submitReplyBtn = document.getElementById('submitReply');
                    if (submitReplyBtn && !submitReplyBtn.disabled && submitReplyBtn.style.display !== 'none') {
                        submitReplyBtn.click();
                    }
                }
            }
        }
        
        // ESC: 모달 닫기
        if (e.key === 'Escape') {
            const activeModal = document.querySelector('.modal.show');
            if (activeModal) {
                e.preventDefault();
                
                if (activeModal.id === 'postModal') {
                    qnaUI.closePostModal();
                } else if (activeModal.id === 'detailModal') {
                    qnaUI.closeDetailModal();
                } else if (activeModal.id === 'confirmDialog') {
                    qnaUI.closeConfirmDialog();
                }
            }
        }
        
        // Ctrl/Cmd + K: 검색 포커스
        if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
            e.preventDefault();
            const searchInput = document.getElementById('searchInput');
            if (searchInput) {
                searchInput.focus();
                searchInput.select();
            }
        }
        
        // N: 새 글 작성 (로그인된 경우)
        if (e.key === 'n' && !e.ctrlKey && !e.metaKey && !e.altKey) {
            // 입력 필드에 포커스가 있지 않은 경우에만
            if (!isInputFocused()) {
                e.preventDefault();
                if (qnaAPI.isLoggedIn()) {
                    qnaUI.openPostModal();
                } else {
                    qnaUI.showToast('로그인이 필요합니다.', 'warning');
                }
            }
        }
        
        // R: 새로고침
        if (e.key === 'r' && !e.ctrlKey && !e.metaKey && !e.altKey) {
            if (!isInputFocused()) {
                e.preventDefault();
                qnaUI.loadPosts();
                qnaUI.showToast('목록을 새로고침했습니다.', 'success');
            }
        }
    });
}

/**
 * 현재 입력 필드에 포커스가 있는지 확인
 */
function isInputFocused() {
    const activeElement = document.activeElement;
    return activeElement && (
        activeElement.tagName === 'INPUT' ||
        activeElement.tagName === 'TEXTAREA' ||
        activeElement.tagName === 'SELECT' ||
        activeElement.isContentEditable
    );
}

/**
 * 전역 에러 핸들러 설정
 */
function setupGlobalErrorHandler() {
    // JavaScript 에러 처리
    window.addEventListener('error', function(e) {
        console.error('전역 JavaScript 에러:', e.error);
        
        // 개발 환경에서만 상세 에러 표시
        if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
            qnaUI.showToast(`에러 발생: ${e.message}`, 'error');
        } else {
            qnaUI.showToast('예상치 못한 오류가 발생했습니다.', 'error');
        }
    });
    
    // Promise rejection 에러 처리
    window.addEventListener('unhandledrejection', function(e) {
        console.error('처리되지 않은 Promise 에러:', e.reason);
        
        if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
            qnaUI.showToast(`Promise 에러: ${e.reason}`, 'error');
        } else {
            qnaUI.showToast('네트워크 오류가 발생했습니다.', 'error');
        }
        
        e.preventDefault();
    });
}

/**
 * 브라우저 뒤로가기/앞으로가기 처리
 */
function setupBrowserNavigation() {
    // 현재 상태를 history에 저장
    function saveState() {
        const state = {
            page: qnaUI.currentPage,
            filters: { ...qnaUI.currentFilters }
        };
        
        const url = new URL(window.location);
        url.searchParams.set('page', qnaUI.currentPage);
        
        Object.keys(qnaUI.currentFilters).forEach(key => {
            if (qnaUI.currentFilters[key]) {
                url.searchParams.set(key, qnaUI.currentFilters[key]);
            } else {
                url.searchParams.delete(key);
            }
        });
        
        window.history.replaceState(state, '', url);
    }
    
    // 상태 복원
    function restoreState(state) {
        if (state) {
            qnaUI.currentPage = state.page || 0;
            qnaUI.currentFilters = { ...state.filters };
            
            // UI 요소 업데이트
            document.getElementById('searchInput').value = qnaUI.currentFilters.keyword || '';
            document.getElementById('categoryFilter').value = qnaUI.currentFilters.category || '';
            document.getElementById('statusFilter').value = qnaUI.currentFilters.answerStatus || '';
            document.getElementById('myPostsOnly').checked = qnaUI.currentFilters.myPostsOnly || false;
            
            // 게시글 목록 로드
            qnaUI.loadPosts();
        }
    }
    
    // URL 파라미터에서 초기 상태 복원
    function loadInitialState() {
        const urlParams = new URLSearchParams(window.location.search);
        
        qnaUI.currentPage = parseInt(urlParams.get('page')) || 0;
        qnaUI.currentFilters.keyword = urlParams.get('keyword') || '';
        qnaUI.currentFilters.category = urlParams.get('category') || '';
        qnaUI.currentFilters.answerStatus = urlParams.get('answerStatus') || '';
        qnaUI.currentFilters.myPostsOnly = urlParams.get('myPostsOnly') === 'true';
        
        // UI 요소 업데이트
        document.getElementById('searchInput').value = qnaUI.currentFilters.keyword;
        document.getElementById('categoryFilter').value = qnaUI.currentFilters.category;
        document.getElementById('statusFilter').value = qnaUI.currentFilters.answerStatus;
        document.getElementById('myPostsOnly').checked = qnaUI.currentFilters.myPostsOnly;
    }
    
    // popstate 이벤트 처리 (뒤로가기/앞으로가기)
    window.addEventListener('popstate', function(e) {
        restoreState(e.state);
    });
    
    // 초기 상태 로드
    loadInitialState();
    
    // 검색/필터 변경 시 상태 저장
    const originalLoadPosts = qnaUI.loadPosts;
    qnaUI.loadPosts = function() {
        const result = originalLoadPosts.call(this);
        saveState();
        return result;
    };
}

/**
 * 페이지 가시성 변경 처리
 */
function setupPageVisibilityHandler() {
    let isPageHidden = false;
    
    document.addEventListener('visibilitychange', function() {
        if (document.hidden) {
            isPageHidden = true;
        } else if (isPageHidden) {
            // 페이지가 다시 보이게 되었을 때
            isPageHidden = false;
            
            // 토큰 만료 확인
            if (qnaAPI.isTokenExpired()) {
                qnaAPI.removeToken();
                qnaUI.updateUserInterface();
                qnaUI.showToast('세션이 만료되었습니다. 다시 로그인해주세요.', 'warning');
            }
            
            // 목록이 오래되었다면 새로고침 (5분 이상)
            const lastLoadTime = qnaUI.lastLoadTime || 0;
            const now = Date.now();
            const fiveMinutes = 5 * 60 * 1000;
            
            if (now - lastLoadTime > fiveMinutes) {
                qnaUI.loadPosts();
            }
        }
    });
}

/**
 * 이미지 로드 에러 처리
 */
function setupImageErrorHandling() {
    document.addEventListener('error', function(e) {
        if (e.target.tagName === 'IMG') {
            // 이미지 로드 실패 시 기본 이미지로 대체
            e.target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIGZpbGw9Im5vbmUiIHN0cm9rZT0iY3VycmVudENvbG9yIiBzdHJva2Utd2lkdGg9IjIiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCIgY2xhc3M9ImZlYXRoZXIgZmVhdGhlci1pbWFnZSI+PHJlY3QgeD0iMyIgeT0iMyIgd2lkdGg9IjE4IiBoZWlnaHQ9IjE4IiByeD0iMiIgcnk9IjIiLz48Y2lyY2xlIGN4PSI4LjUiIGN5PSI4LjUiIHI9IjEuNSIvPjxwb2x5bGluZSBwb2ludHM9IjIxLDE1IDEyLDYgOSw5Ii8+PC9zdmc+';
            e.target.style.opacity = '0.3';
            e.target.title = '이미지를 불러올 수 없습니다';
        }
    }, true);
}

/**
 * 접근성 개선
 */
function setupAccessibility() {
    // 포커스 트랩 (모달 내에서만 포커스 이동)
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Tab') {
            const activeModal = document.querySelector('.modal.show');
            if (activeModal) {
                const focusableElements = activeModal.querySelectorAll(
                    'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'
                );
                
                if (focusableElements.length > 0) {
                    const firstElement = focusableElements[0];
                    const lastElement = focusableElements[focusableElements.length - 1];
                    
                    if (e.shiftKey && document.activeElement === firstElement) {
                        e.preventDefault();
                        lastElement.focus();
                    } else if (!e.shiftKey && document.activeElement === lastElement) {
                        e.preventDefault();
                        firstElement.focus();
                    }
                }
            }
        }
    });
    
    // ARIA 레이블 동적 업데이트
    function updateAriaLabels() {
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.setAttribute('aria-label', '게시글 검색');
        }
        
        const posts = document.querySelectorAll('.post-item');
        posts.forEach((post, index) => {
            post.setAttribute('role', 'button');
            post.setAttribute('tabindex', '0');
            post.setAttribute('aria-label', `게시글 ${index + 1}: ${post.querySelector('.post-title')?.textContent || ''}`);
        });
    }
    
    // 게시글 목록 로드 후 ARIA 레이블 업데이트
    const originalRenderPosts = qnaUI.renderPosts;
    qnaUI.renderPosts = function(posts) {
        const result = originalRenderPosts.call(this, posts);
        setTimeout(updateAriaLabels, 100);
        return result;
    };
}

/**
 * 성능 최적화
 */
function setupPerformanceOptimization() {
    // 이미지 지연 로딩
    if ('IntersectionObserver' in window) {
        const imageObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const img = entry.target;
                    if (img.dataset.src) {
                        img.src = img.dataset.src;
                        img.removeAttribute('data-src');
                        imageObserver.unobserve(img);
                    }
                }
            });
        }, {
            threshold: 0.1,
            rootMargin: '50px'
        });
        
        // 게시글 목록 렌더링 시 이미지 관찰 시작
        const originalRenderPosts = qnaUI.renderPosts;
        qnaUI.renderPosts = function(posts) {
            const result = originalRenderPosts.call(this, posts);
            
            // 지연 로딩할 이미지들 관찰 시작
            setTimeout(() => {
                const lazyImages = document.querySelectorAll('img[data-src]');
                lazyImages.forEach(img => imageObserver.observe(img));
            }, 100);
            
            return result;
        };
    }
    
    // 스크롤 성능 최적화
    let scrollTimeout;
    window.addEventListener('scroll', function() {
        if (scrollTimeout) {
            clearTimeout(scrollTimeout);
        }
        
        scrollTimeout = setTimeout(() => {
            // 스크롤이 멈춘 후 실행할 작업들
            updateScrollPosition();
        }, 150);
    }, { passive: true });
    
    function updateScrollPosition() {
        // 현재 스크롤 위치 저장 (페이지 새로고침 시 복원용)
        sessionStorage.setItem('qna-scroll-position', window.scrollY.toString());
    }
    
    // 페이지 로드 시 스크롤 위치 복원
    function restoreScrollPosition() {
        const savedPosition = sessionStorage.getItem('qna-scroll-position');
        if (savedPosition) {
            window.scrollTo(0, parseInt(savedPosition));
            sessionStorage.removeItem('qna-scroll-position');
        }
    }
    
    // 게시글 목록 로드 후 스크롤 위치 복원
    const originalLoadPosts = qnaUI.loadPosts;
    qnaUI.loadPosts = function() {
        const result = originalLoadPosts.call(this);
        
        // 첫 페이지 로드 시에만 스크롤 위치 복원
        if (this.currentPage === 0 && !this.hasRestoredScroll) {
            setTimeout(restoreScrollPosition, 300);
            this.hasRestoredScroll = true;
        }
        
        // 로드 시간 기록
        this.lastLoadTime = Date.now();
        
        return result;
    };
}

/**
 * 개발 도구 (개발 환경에서만)
 */
function setupDevelopmentTools() {
    if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
        // 개발용 전역 함수들
        window.qnaDevTools = {
            // 테스트용 토큰 설정
            setTestToken: function(userAccount = 'testuser') {
                const testToken = btoa(JSON.stringify({
                    userId: userAccount === 'admin' ? 1 : 2,
                    userAccount: userAccount,
                    exp: Math.floor(Date.now() / 1000) + 86400 // 24시간 후 만료
                }));
                qnaAPI.setToken('header.' + testToken + '.signature');
                qnaUI.updateUserInterface();
                console.log(`테스트 토큰 설정됨: ${userAccount}`);
            },
            
            // 관리자 토큰 설정
            setAdminToken: function() {
                this.setTestToken('admin');
            },
            
            // 현재 상태 출력
            getState: function() {
                return {
                    currentPage: qnaUI.currentPage,
                    currentFilters: qnaUI.currentFilters,
                    isLoggedIn: qnaAPI.isLoggedIn(),
                    isAdmin: qnaAPI.isAdmin(),
                    user: qnaAPI.getCurrentUser()
                };
            },
            
            // API 테스트
            testAPI: async function() {
                try {
                    const response = await qnaAPI.getQnaPosts({ page: 0, size: 1 });
                    console.log('API 테스트 성공:', response);
                    return response;
                } catch (error) {
                    console.error('API 테스트 실패:', error);
                    return error;
                }
            },
            
            // 샘플 데이터 생성
            createSamplePost: async function() {
                if (!qnaAPI.isLoggedIn()) {
                    console.warn('로그인이 필요합니다.');
                    return;
                }
                
                const sampleData = {
                    category: '일반문의',
                    title: `테스트 게시글 ${Date.now()}`,
                    content: '이것은 테스트용 게시글입니다.\n\n개발 도구에서 생성되었습니다.',
                    isLocked: Math.random() > 0.5
                };
                
                try {
                    const response = await qnaAPI.createQnaPost(sampleData);
                    console.log('샘플 게시글 생성 완료:', response);
                    qnaUI.loadPosts();
                    return response;
                } catch (error) {
                    console.error('샘플 게시글 생성 실패:', error);
                    return error;
                }
            }
        };
        
        console.log('QnA 개발 도구가 준비되었습니다.');
        console.log('사용 가능한 명령어:');
        console.log('- qnaDevTools.setTestToken() : 테스트 사용자 토큰 설정');
        console.log('- qnaDevTools.setAdminToken() : 관리자 토큰 설정');
        console.log('- qnaDevTools.getState() : 현재 상태 확인');
        console.log('- qnaDevTools.testAPI() : API 연결 테스트');
        console.log('- qnaDevTools.createSamplePost() : 샘플 게시글 생성');
    }
}

/**
 * 브라우저 호환성 검사
 */
function checkBrowserCompatibility() {
    const requiredFeatures = [
        'fetch',
        'Promise',
        'localStorage',
        'JSON'
    ];
    
    const missingFeatures = requiredFeatures.filter(feature => {
        return !(feature in window);
    });
    
    if (missingFeatures.length > 0) {
        const message = `이 브라우저는 지원되지 않습니다. 누락된 기능: ${missingFeatures.join(', ')}`;
        alert(message);
        console.error(message);
        return false;
    }
    
    // 추가 기능 검사
    if (!window.FormData) {
        console.warn('FormData가 지원되지 않습니다. 파일 업로드가 제한될 수 있습니다.');
    }
    
    if (!window.URLSearchParams) {
        console.warn('URLSearchParams가 지원되지 않습니다. URL 파라미터 처리가 제한될 수 있습니다.');
    }
    
    return true;
}

/**
 * 서비스 워커 등록 (PWA 기능, 선택사항)
 */
function registerServiceWorker() {
    if ('serviceWorker' in navigator && window.location.protocol === 'https:') {
        navigator.serviceWorker.register('/sw.js')
            .then(registration => {
                console.log('Service Worker 등록 성공:', registration.scope);
            })
            .catch(error => {
                console.log('Service Worker 등록 실패:', error);
            });
    }
}

/**
 * 사용자 경험 개선
 */
function setupUserExperience() {
    // 터치 디바이스 감지
    let isTouchDevice = false;
    
    window.addEventListener('touchstart', function() {
        isTouchDevice = true;
        document.body.classList.add('touch-device');
    }, { once: true });
    
    // 마우스 사용 감지
    window.addEventListener('mousemove', function() {
        if (!isTouchDevice) {
            document.body.classList.add('mouse-device');
        }
    }, { once: true });
    
    // 네트워크 상태 모니터링
    if ('connection' in navigator) {
        function updateNetworkStatus() {
            const connection = navigator.connection;
            const isSlowConnection = connection.effectiveType === 'slow-2g' || connection.effectiveType === '2g';
            
            if (isSlowConnection) {
                document.body.classList.add('slow-connection');
                qnaUI.showToast('느린 네트워크가 감지되었습니다. 일부 기능이 제한될 수 있습니다.', 'warning');
            } else {
                document.body.classList.remove('slow-connection');
            }
        }
        
        navigator.connection.addEventListener('change', updateNetworkStatus);
        updateNetworkStatus();
    }
    
    // 오프라인 상태 처리
    window.addEventListener('online', function() {
        qnaUI.showToast('인터넷에 다시 연결되었습니다.', 'success');
        qnaUI.loadPosts(); // 목록 새로고침
    });
    
    window.addEventListener('offline', function() {
        qnaUI.showToast('인터넷 연결이 끊어졌습니다.', 'warning');
    });
}

/**
 * 분석 및 모니터링 (선택사항)
 */
function setupAnalytics() {
    // 페이지 조회 추적
    function trackPageView() {
        // Google Analytics, 자체 분석 시스템 등과 연동
        console.log('페이지 조회:', window.location.pathname);
    }
    
    // 사용자 액션 추적
    function trackUserAction(action, details = {}) {
        console.log('사용자 액션:', action, details);
        
        // 실제 분석 시스템으로 데이터 전송
        // analytics.track(action, details);
    }
    
    // 주요 액션들에 대한 추적 이벤트 바인딩
    document.addEventListener('click', function(e) {
        if (e.target.matches('.btn-primary')) {
            trackUserAction('button_click', { 
                button_type: 'primary',
                button_text: e.target.textContent.trim()
            });
        }
        
        if (e.target.matches('.post-item')) {
            trackUserAction('post_view', {
                post_id: e.target.dataset.postId
            });
        }
    });
    
    trackPageView();
}

// 초기화 함수들 실행
document.addEventListener('DOMContentLoaded', function() {
    // 브라우저 호환성 검사
    if (!checkBrowserCompatibility()) {
        return;
    }
    
    // 기능별 초기화
    setupImageErrorHandling();
    setupAccessibility();
    setupPerformanceOptimization();
    setupUserExperience();
    setupDevelopmentTools();
    setupAnalytics();
    
    // PWA 기능 (선택사항)
    // registerServiceWorker();
    
    console.log('QnA 게시판 고급 기능 초기화 완료');
});

// 전역 유틸리티 함수들
window.qnaUtils = {
    /**
     * 클립보드에 텍스트 복사
     */
    copyToClipboard: async function(text) {
        try {
            if (navigator.clipboard) {
                await navigator.clipboard.writeText(text);
            } else {
                // Fallback for older browsers
                const textArea = document.createElement('textarea');
                textArea.value = text;
                document.body.appendChild(textArea);
                textArea.select();
                document.execCommand('copy');
                document.body.removeChild(textArea);
            }
            qnaUI.showToast('클립보드에 복사되었습니다.', 'success');
            return true;
        } catch (error) {
            console.error('클립보드 복사 실패:', error);
            qnaUI.showToast('클립보드 복사에 실패했습니다.', 'error');
            return false;
        }
    },
    
    /**
     * URL 공유
     */
    shareUrl: async function(url, title) {
        if (navigator.share) {
            try {
                await navigator.share({
                    title: title,
                    url: url
                });
                return true;
            } catch (error) {
                if (error.name !== 'AbortError') {
                    console.error('공유 실패:', error);
                }
            }
        }
        
        // Fallback: URL 복사
        return await this.copyToClipboard(url);
    },
    
    /**
     * 파일 다운로드
     */
    downloadFile: function(blob, filename) {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    },
    
    /**
     * 반응형 디자인 브레이크포인트 확인
     */
    getBreakpoint: function() {
        const width = window.innerWidth;
        if (width < 480) return 'xs';
        if (width < 768) return 'sm';
        if (width < 1024) return 'md';
        if (width < 1200) return 'lg';
        return 'xl';
    },
    
    /**
     * 디바이스 타입 감지
     */
    getDeviceType: function() {
        const ua = navigator.userAgent;
        if (/(tablet|ipad|playbook|silk)|(android(?!.*mobi))/i.test(ua)) {
            return 'tablet';
        }
        if (/Mobile|Android|iP(hone|od)|IEMobile|BlackBerry|Kindle|Silk-Accelerated|(hpw|web)OS|Opera M(obi|ini)/.test(ua)) {
            return 'mobile';
        }
        return 'desktop';
    }
};

// 마지막으로 성능 메트릭 로깅
window.addEventListener('load', function() {
    setTimeout(() => {
        if (window.performance && window.performance.timing) {
            const timing = window.performance.timing;
            const loadTime = timing.loadEventEnd - timing.navigationStart;
            console.log(`페이지 로드 시간: ${loadTime}ms`);
            
            // 분석 시스템으로 성능 데이터 전송
            // analytics.track('page_load_time', { load_time: loadTime });
        }
    }, 1000);
});