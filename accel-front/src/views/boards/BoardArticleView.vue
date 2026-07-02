<template>
    <main class="article-page">
        <header class="article-header">
            <RouterLink class="back-to-board" :to="backToListRoute">
                <span aria-hidden="true">&lt;</span>
                목록으로 돌아가기
            </RouterLink>
        </header>

        <article class="article-panel">
            <section class="live-preview article-preview" aria-label="게시글 상세">
                <div class="preview-image">
                    <BoardImageCarousel
                        v-model:active-index="activeImageIndex"
                        :images="articleImages"
                        :fallback-image="videoThumbnail"
                        :alt="`${article.title || '게시글'} 이미지`"
                    />
                </div>

                <div class="preview-body">
                    <div class="article-heading">
                        <span class="board-name">{{ article.boardName || '게시판' }}</span>
                        <h2>{{ article.title || '제목 없음' }}</h2>
                    </div>

                    <div class="article-info-band" aria-label="게시글 정보">
                        <dl class="article-info-list">
                            <div>
                                <dt>작성자</dt>
                                <dd>{{ article.userId || '-' }}</dd>
                            </div>
                            <div>
                                <dt>작성일</dt>
                                <dd>{{ formatDate(article.createdAt) }}</dd>
                            </div>
                            <div>
                                <dt>조회수</dt>
                                <dd>{{ formattedViewCount }}</dd>
                            </div>
                        </dl>

                        <div
                            class="drive-status"
                            :class="{ available: article.testDriveAvailable }"
                            aria-label="시승 가능 여부"
                        >
                            <span class="drive-status-icon" aria-hidden="true"></span>
                            <span>{{ article.testDriveAvailable ? '시승 가능' : '시승 불가' }}</span>
                        </div>
                    </div>

                    <div class="article-content" aria-label="게시글 본문">
                        <p>{{ article.content || '본문이 없습니다.' }}</p>
                    </div>

                    <div class="article-control-row">
                        <div class="like-bar">
                            <button
                                type="button"
                                class="like-button"
                                :class="{ liked: liked }"
                                :disabled="likeLoading"
                                :aria-pressed="liked"
                                @click="toggleLike"
                            >
                                <span class="like-heart" aria-hidden="true">{{ liked ? '♥' : '♡' }}</span>
                                <span>좋아요</span>
                                <span class="like-count">{{ likeCount }}</span>
                            </button>
                        </div>

                        <div class="article-actions card-actions" v-if="canManageArticle">
                            <button type="button" class="edit-button" @click="goToEdit">수정</button>
                            <button type="button" class="danger-button" @click="deleteArticle">삭제</button>
                        </div>
                    </div>
                </div>
            </section>
        </article>

        <form class="comment-composer" v-if="currentUserId" @submit.prevent="submitComment">
            <label for="comment-user-id">작성자 ID</label>
            <input id="comment-user-id" v-model="commentUserId" type="text" placeholder="작성자 ID" disabled>

            <label for="comment-content">댓글</label>
            <textarea id="comment-content" v-model="commentContent" rows="4" placeholder="댓글을 입력하세요"></textarea>
            <button type="submit" :disabled="!canSubmitComment">등록</button>
        </form>

        <section class="comment-login-required" v-else aria-label="댓글 로그인 안내">
            <strong>댓글 작성을 위해 로그인이 필요합니다.</strong>
            <RouterLink class="inline-login-link" :to="loginRoute">로그인하기</RouterLink>
        </section>

        <section class="comment-list" aria-label="댓글">
            <h2>댓글</h2>

            <article class="comment-item" v-for="comment in comments" :key="comment.commentId">
                <div class="comment-meta">
                    <strong>{{ comment.userId || '알 수 없음' }}</strong>
                    <span>{{ formatDate(comment.createdAt) }}</span>
                </div>

                <form
                    class="comment-edit-form"
                    v-if="editingCommentId === comment.commentId"
                    @submit.prevent="saveCommentEdit(comment)"
                >
                    <textarea
                        v-model="editingCommentContent"
                        rows="3"
                        aria-label="댓글 수정"
                        placeholder="댓글을 입력하세요"
                    ></textarea>
                    <div class="comment-edit-actions">
                        <button type="submit" class="edit-button" :disabled="!canSaveCommentEdit">저장</button>
                        <button type="button" class="secondary-button" @click="cancelCommentEdit">취소</button>
                    </div>
                </form>

                <p v-else>{{ comment.content }}</p>

                <div class="comment-actions">
                    <button
                        type="button"
                        class="comment-like-button"
                        :class="{ liked: comment.liked }"
                        :aria-pressed="comment.liked"
                        aria-label="댓글 좋아요"
                        @click="toggleCommentLike(comment)"
                    >
                        <span class="like-heart" aria-hidden="true">{{ comment.liked ? '♥' : '♡' }}</span>
                        <span class="comment-like-count">{{ comment.likeCount ?? 0 }}</span>
                    </button>

                    <template v-if="canManageComment(comment)">
                        <button
                            type="button"
                            class="comment-manage-button"
                            @click="startCommentEdit(comment)"
                        >
                            수정
                        </button>
                        <button
                            type="button"
                            class="comment-manage-button danger"
                            @click="deleteComment(comment)"
                        >
                            삭제
                        </button>
                    </template>
                </div>
            </article>

            <p class="empty-comments" v-if="comments.length === 0">
                아직 댓글이 없습니다.
            </p>
        </section>
    </main>
</template>

<script setup>
import videoThumbnail from '@/assets/video_thumbnail.png';
import BoardImageCarousel from '@/components/boards/BoardImageCarousel.vue';
import { resolveArticleImageUrls } from '@/lib/articleImage';
import { useAuthStore } from '@/stores/authStore';
import { useBoardStore } from '@/stores/boardStore';
import { storeToRefs } from 'pinia';
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute()
const router = useRouter()
const store = useBoardStore()
const authStore = useAuthStore()
const { article, comments } = storeToRefs(store)
const commentUserId = ref('')
const commentContent = ref('')
const activeImageIndex = ref(0)
const likeCount = ref(0)
const liked = ref(false)
const likeLoading = ref(false)
const editingCommentId = ref(null)
const editingCommentContent = ref('')
const tokenPayload = computed(() => {
    const token = authStore.accessToken

    if (!token) {
        return {}
    }

    try {
        const base64Url = token.split('.')[1]
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
        const json = decodeURIComponent(
            atob(base64)
                .split('')
                .map((char) => `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`)
                .join('')
        )

        return JSON.parse(json)
    } catch {
        return {}
    }
})

const currentUserId = computed(() => {
    return authStore.user?.userId
        ?? authStore.user?.id
        ?? tokenPayload.value.userId
        ?? tokenPayload.value.sub
        ?? ''
})

const currentRole = computed(() => {
    return authStore.user?.role
        ?? tokenPayload.value.role
        ?? ''
})

const isAdmin = computed(() => {
    return String(currentRole.value).toUpperCase().replace('ROLE_', '') === 'ADMIN'
})

const canManageArticle = computed(() => {
    return Boolean(currentUserId.value)
        && (currentUserId.value === article.value.userId || isAdmin.value)
})

const canSubmitComment = computed(() => {
    return Boolean(currentUserId.value && commentUserId.value.trim() && commentContent.value.trim())
})

const canSaveCommentEdit = computed(() => {
    return Boolean(editingCommentId.value && editingCommentContent.value.trim())
})

const formattedViewCount = computed(() => {
    return (Number(article.value.viewCnt) || 0).toLocaleString('ko-KR')
})

const backToListRoute = computed(() => {
    const brandId = article.value.brandId

    return brandId
        ? { name: 'BrandBoard', params: { brandId: `${brandId}` } }
        : { name: 'BrandCategory' }
})

const loginRoute = computed(() => {
    return {
        name: 'login',
        query: { redirect: route.fullPath },
    }
})

const articleImages = computed(() => resolveArticleImageUrls(article.value))

const loadLikeStatus = async () => {
    try {
        const status = await store.fetchArticleLikeStatus(
            route.params.articleId,
            currentUserId.value || undefined,
        )
        likeCount.value = Number(status.likeCount) || 0
        liked.value = Boolean(status.liked)
    } catch (error) {
        console.error('좋아요 정보를 불러오지 못했습니다:', error)
    }
}

const toggleLike = async () => {
    if (!currentUserId.value) {
        window.alert('로그인이 필요합니다.')
        return
    }

    if (likeLoading.value) {
        return
    }

    likeLoading.value = true

    try {
        const status = liked.value
            ? await store.unlikeArticle(route.params.articleId, currentUserId.value)
            : await store.likeArticle(route.params.articleId, currentUserId.value)
        likeCount.value = Number(status.likeCount) || 0
        liked.value = Boolean(status.liked)
    } catch (error) {
        console.error('좋아요 처리에 실패했습니다:', error)
    } finally {
        likeLoading.value = false
    }
}

const loadArticle = async () => {
    await store.fetchArticle(route.params.articleId, currentUserId.value || undefined)
    await store.increaseArticleViewCount(route.params.articleId)
    activeImageIndex.value = 0
    await loadLikeStatus()
}

const formatDate = (date) => {
    return date ? String(date).slice(0, 10).replaceAll('-', '.') : '-'
}

const goToEdit = () => {
    router.push({ name: 'BoardEdit', params: { articleId: `${route.params.articleId}` } })
}

const deleteArticle = async () => {
    if (!canManageArticle.value || !window.confirm('게시글을 삭제하시겠습니까?')) {
        return
    }

    const nextRoute = article.value.brandId
        ? { name: 'BrandBoard', params: { brandId: `${article.value.brandId}` } }
        : { name: 'BrandCategory' }

    await store.deleteArticle(route.params.articleId)
    router.push(nextRoute)
}

const submitComment = async () => {
    if (!currentUserId.value) {
        return
    }

    const content = commentContent.value.trim()

    if (!commentUserId.value.trim() || !content) {
        return
    }

    try {
        await store.createComment(route.params.articleId, {
            content,
        })
        commentContent.value = ''
    } catch (error) {
        console.error('댓글 등록에 실패했습니다:', error)
        window.alert('댓글 등록에 실패했습니다. 잠시 후 다시 시도해 주세요.')
    }
}

const canManageComment = (comment) => {
    return Boolean(currentUserId.value)
        && (currentUserId.value === comment.userId || isAdmin.value)
}

const startCommentEdit = (comment) => {
    if (!canManageComment(comment)) {
        return
    }

    editingCommentId.value = comment.commentId
    editingCommentContent.value = comment.content ?? ''
}

const cancelCommentEdit = () => {
    editingCommentId.value = null
    editingCommentContent.value = ''
}

const saveCommentEdit = async (comment) => {
    if (!canManageComment(comment) || !canSaveCommentEdit.value) {
        return
    }

    try {
        await store.updateComment(route.params.articleId, comment.commentId, {
            content: editingCommentContent.value.trim(),
        })
        cancelCommentEdit()
    } catch (error) {
        console.error('댓글 수정에 실패했습니다:', error)
        window.alert('댓글 수정에 실패했습니다. 잠시 후 다시 시도해 주세요.')
    }
}

const deleteComment = async (comment) => {
    if (!canManageComment(comment) || !window.confirm('정말 삭제하시겠어요?')) {
        return
    }

    try {
        await store.deleteComment(route.params.articleId, comment.commentId)

        if (editingCommentId.value === comment.commentId) {
            cancelCommentEdit()
        }
    } catch (error) {
        console.error('댓글 삭제에 실패했습니다:', error)
        window.alert('댓글 삭제에 실패했습니다. 잠시 후 다시 시도해 주세요.')
    }
}

const toggleCommentLike = async (comment) => {
    if (!currentUserId.value) {
        window.alert('로그인이 필요합니다.')
        return
    }

    try {
        const status = comment.liked
            ? await store.unlikeComment(comment.commentId, currentUserId.value)
            : await store.likeComment(comment.commentId, currentUserId.value)
        comment.likeCount = Number(status.likeCount) || 0
        comment.liked = Boolean(status.liked)
    } catch (error) {
        console.error('댓글 좋아요 처리에 실패했습니다:', error)
    }
}

watch(() => route.params.articleId, loadArticle)
watch(currentUserId, (userId) => {
    if (userId && !commentUserId.value) {
        commentUserId.value = userId
    }
}, { immediate: true })

onMounted(loadArticle)
</script>

<style scoped>
.article-page {
    position: relative;
    z-index: 1;
    max-width: 920px;
    margin: 0 auto;
    padding: 112px 20px 56px;
    color: var(--neon-text);
}

.article-header {
    display: flex;
    justify-content: flex-start;
    margin-bottom: 16px;
}

.back-to-board {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-height: 44px;
    padding: 0 14px;
    color: #ffffff;
    background: rgba(4, 217, 255, 0.08);
    border: 1px solid rgba(140, 207, 255, 0.24);
    border-radius: 8px;
    font-size: 14px;
    font-weight: 800;
    text-decoration: none;
    transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.back-to-board span {
    color: var(--muted-blue);
    font-size: 18px;
    line-height: 1;
}

.back-to-board:hover,
.back-to-board:focus-visible {
    transform: translateY(-1px);
    border-color: var(--neon-cyan);
    box-shadow: var(--glow-blue);
    outline: none;
}

.article-panel,
.comment-composer,
.comment-list {
    background: rgba(0, 0, 0, 0.78);
    border: 1px solid var(--line-blue);
    border-radius: 8px;
    box-shadow: 0 0 18px rgba(4, 217, 255, 0.12);
}

.article-panel {
    overflow: hidden;
    padding: 0;
}

.title-row {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
}

.article-actions,
.form-actions {
    display: flex;
    gap: 10px;
}

.article-actions {
    flex-shrink: 0;
    flex-wrap: wrap;
    justify-content: flex-end;
}

.article-actions button,
.form-actions button {
    min-width: 76px;
    padding: 9px 14px;
}

.secondary-button {
    background: rgba(4, 217, 255, 0.08);
    box-shadow: none;
}

.live-preview {
    overflow: hidden;
}

.preview-image {
    position: relative;
    aspect-ratio: 16 / 9;
    background: #000;
    border-bottom: 1px solid rgba(140, 207, 255, 0.22);
}

.preview-body {
    display: grid;
    gap: 18px;
    padding: 20px;
}

.article-heading {
    display: grid;
    gap: 8px;
}

.board-name {
    color: var(--muted-blue);
    font-size: 13px;
    font-weight: 800;
}

.article-info-band {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 18px;
    padding: 16px;
    background: rgba(4, 17, 36, 0.82);
    border: 1px solid rgba(140, 207, 255, 0.24);
    border-left: 4px solid var(--neon-cyan);
    border-radius: 8px;
}

.article-info-list {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 14px;
    flex: 1;
    min-width: 0;
    margin: 0;
}

.article-info-list div {
    display: grid;
    gap: 5px;
    min-width: 0;
}

.article-info-list dt {
    color: var(--muted-blue);
    font-size: 12px;
    font-weight: 800;
}

.article-info-list dd {
    margin: 0;
    color: #ffffff;
    font-size: 15px;
    font-weight: 800;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.preview-body h2 {
    margin: 0;
    color: #ffffff;
    font-size: 26px;
    line-height: 1.3;
    overflow-wrap: anywhere;
}

.drive-status {
    display: inline-flex;
    align-items: center;
    gap: 10px;
    flex: 0 0 auto;
    min-height: 48px;
    padding: 0 14px 0 8px;
    color: #ffe8ad;
    background: rgba(255, 209, 102, 0.12);
    border: 1px solid rgba(255, 209, 102, 0.34);
    border-radius: 999px;
    font-weight: 900;
}

.drive-status.available {
    color: #bfffe0;
    background: rgba(92, 255, 176, 0.14);
    border-color: rgba(92, 255, 176, 0.38);
}

.drive-status-icon {
    position: relative;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    color: currentColor;
    background: rgba(255, 255, 255, 0.08);
    border: 1px solid currentColor;
    border-radius: 50%;
    box-shadow: 0 0 14px rgba(255, 209, 102, 0.18);
}

.drive-status.available .drive-status-icon {
    box-shadow: 0 0 16px rgba(92, 255, 176, 0.28);
}

.drive-status-icon::before {
    content: '';
    position: absolute;
    width: 14px;
    height: 8px;
    border-left: 3px solid currentColor;
    border-bottom: 3px solid currentColor;
    transform: translateY(-1px) rotate(-45deg);
}

.drive-status:not(.available) .drive-status-icon::before {
    width: 16px;
    height: 3px;
    background: currentColor;
    border: 0;
    border-radius: 999px;
    transform: rotate(-35deg);
}

.article-content {
    padding-top: 2px;
}

.article-content p {
    min-height: 180px;
    margin: 0;
    line-height: 1.7;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
}

.article-control-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
    margin-top: 4px;
}

.like-bar {
    display: flex;
    align-items: center;
}

.like-button {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    min-height: 42px;
    padding: 8px 16px;
    color: var(--neon-text);
    background: rgba(4, 217, 255, 0.08);
    border: 1px solid var(--line-blue);
    border-radius: 999px;
    font-weight: 800;
    cursor: pointer;
    box-shadow: none;
    transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.like-button:hover,
.like-button:focus-visible {
    transform: translateY(-1px);
    border-color: var(--neon-cyan);
    box-shadow: var(--glow-blue);
    outline: none;
}

.like-button .like-heart {
    font-size: 18px;
    line-height: 1;
    color: #ff6b8b;
}

.like-button .like-count {
    min-width: 20px;
    padding: 2px 8px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.1);
    font-size: 13px;
    text-align: center;
}

.like-button.liked {
    color: #ffd9e2;
    background: rgba(255, 107, 139, 0.16);
    border-color: rgba(255, 107, 139, 0.6);
}

.like-button.liked:hover,
.like-button.liked:focus-visible {
    border-color: #ff6b8b;
    box-shadow: 0 0 18px rgba(255, 107, 139, 0.35);
}

.like-button.liked .like-heart {
    color: #ff4d77;
}

.card-actions {
    justify-content: flex-end;
    margin-top: 0;
}

.edit-button,
.danger-button {
    position: relative;
    min-width: 96px;
    min-height: 42px;
    padding: 10px 16px;
    border: 1px solid transparent;
    border-radius: 6px;
    color: #ffffff;
    font-weight: 800;
    cursor: pointer;
    transition:
        color 0.2s ease,
        background 0.2s ease,
        box-shadow 0.2s ease;
}

.edit-button {
    background:
        linear-gradient(180deg, rgba(13, 72, 82, 0.92), rgba(7, 40, 54, 0.96)) padding-box,
        linear-gradient(180deg, rgba(110, 255, 226, 0.32), rgba(35, 207, 255, 0.28)) border-box;
    box-shadow:
        inset 0 1px 0 rgba(255, 255, 255, 0.12),
        0 8px 20px rgba(0, 0, 0, 0.22);
}

.danger-button {
    background:
        linear-gradient(180deg, rgba(88, 24, 42, 0.92), rgba(50, 10, 24, 0.96)) padding-box,
        linear-gradient(180deg, rgba(255, 128, 152, 0.34), rgba(255, 58, 96, 0.3)) border-box;
    box-shadow:
        inset 0 1px 0 rgba(255, 255, 255, 0.1),
        0 8px 20px rgba(0, 0, 0, 0.22);
}

.edit-button:hover,
.edit-button:focus-visible {
    outline: none;
    background:
        linear-gradient(180deg, rgba(15, 88, 98, 0.96), rgba(8, 47, 63, 0.98)) padding-box,
        conic-gradient(
            from var(--glow-angle),
            rgba(13, 255, 207, 0.2),
            rgba(91, 255, 229, 0.96),
            rgba(35, 188, 255, 0.98),
            rgba(13, 255, 207, 0.22),
            rgba(91, 255, 229, 0.96),
            rgba(13, 255, 207, 0.2)
        ) border-box;
    box-shadow:
        0 0 16px rgba(13, 255, 207, 0.38),
        0 0 34px rgba(35, 188, 255, 0.28),
        inset 0 0 18px rgba(13, 255, 207, 0.1);
    animation: neon-border 1.4s linear infinite;
}

.danger-button:hover,
.danger-button:focus-visible {
    outline: none;
    background:
        linear-gradient(180deg, rgba(104, 27, 48, 0.96), rgba(58, 11, 27, 0.98)) padding-box,
        conic-gradient(
            from var(--glow-angle),
            rgba(255, 56, 104, 0.2),
            rgba(255, 128, 152, 0.95),
            rgba(255, 36, 84, 0.98),
            rgba(255, 56, 104, 0.22),
            rgba(255, 128, 152, 0.95),
            rgba(255, 56, 104, 0.2)
        ) border-box;
    box-shadow:
        0 0 16px rgba(255, 56, 104, 0.38),
        0 0 34px rgba(255, 36, 84, 0.25),
        inset 0 0 18px rgba(255, 56, 104, 0.1);
    animation: neon-border 1.4s linear infinite;
}

@property --glow-angle {
    syntax: '<angle>';
    inherits: false;
    initial-value: 0deg;
}

@keyframes neon-border {
    from {
        --glow-angle: 0deg;
    }

    to {
        --glow-angle: 360deg;
    }
}

.edit-form {
    display: grid;
    gap: 12px;
    padding: 28px;
}

.edit-form label {
    color: var(--neon-cyan);
    font-weight: 700;
}

.edit-form input[type="text"],
.edit-form input[type="url"],
.edit-form textarea {
    width: 100%;
    padding: 12px;
    box-sizing: border-box;
}

.edit-form textarea {
    resize: vertical;
}

.check-row {
    display: flex;
    align-items: center;
    gap: 10px;
    color: var(--neon-text);
}

.form-actions {
    justify-content: flex-end;
}

.comment-composer {
    display: grid;
    gap: 12px;
    margin-top: 24px;
    padding: 20px;
}

.comment-login-required {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 14px;
    margin-top: 24px;
    padding: 20px;
    background: rgba(4, 17, 36, 0.82);
    border: 1px solid rgba(140, 207, 255, 0.24);
    border-radius: 8px;
    color: var(--neon-text);
    box-shadow: 0 0 18px rgba(4, 217, 255, 0.1);
}

.comment-login-required strong {
    color: #ffffff;
    font-size: 15px;
}

.inline-login-link {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex: 0 0 auto;
    min-height: 38px;
    padding: 0 14px;
    color: #ffffff;
    background: rgba(4, 217, 255, 0.1);
    border: 1px solid rgba(140, 207, 255, 0.3);
    border-radius: 8px;
    font-size: 13px;
    font-weight: 800;
    text-decoration: none;
    transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.inline-login-link:hover,
.inline-login-link:focus-visible {
    transform: translateY(-1px);
    border-color: var(--neon-cyan);
    box-shadow: var(--glow-blue);
    outline: none;
}

.comment-composer label,
.comment-list h2 {
    color: var(--neon-cyan);
    font-weight: 700;
}

.comment-composer input,
.comment-composer textarea {
    width: 100%;
    padding: 12px;
    box-sizing: border-box;
}

.comment-composer textarea {
    resize: vertical;
}

.comment-composer button {
    justify-self: end;
    min-width: 96px;
    padding: 10px 16px;
}

.comment-list {
    margin-top: 24px;
    padding: 20px;
}

.comment-list h2 {
    margin: 0 0 16px;
    font-size: 22px;
}

.comment-item {
    padding: 16px 0;
    border-top: 1px solid rgba(4, 217, 255, 0.16);
}

.comment-meta {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    color: var(--muted-blue);
    font-size: 14px;
}

.comment-meta strong {
    color: var(--neon-text);
}

.comment-item p,
.empty-comments {
    margin: 10px 0 0;
    line-height: 1.6;
}

.comment-actions {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 10px;
}

.comment-edit-form {
    display: grid;
    gap: 10px;
    margin-top: 12px;
}

.comment-edit-form textarea {
    width: 100%;
    min-height: 84px;
    padding: 12px;
    box-sizing: border-box;
    resize: vertical;
}

.comment-edit-actions {
    display: flex;
    justify-content: flex-end;
    flex-wrap: wrap;
    gap: 8px;
}

.comment-edit-actions button {
    min-width: 72px;
    min-height: 36px;
    padding: 8px 12px;
}

.comment-like-button,
.comment-manage-button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    min-height: 32px;
    padding: 4px 12px;
    color: var(--neon-text);
    background: rgba(4, 217, 255, 0.08);
    border: 1px solid var(--line-blue);
    border-radius: 999px;
    font-size: 13px;
    font-weight: 700;
    cursor: pointer;
    box-shadow: none;
    transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.comment-like-button:hover,
.comment-like-button:focus-visible,
.comment-manage-button:hover,
.comment-manage-button:focus-visible {
    transform: translateY(-1px);
    border-color: var(--neon-cyan);
    box-shadow: var(--glow-blue);
    outline: none;
}

.comment-manage-button.danger {
    color: #ffd9e2;
    background: rgba(255, 107, 139, 0.12);
    border-color: rgba(255, 107, 139, 0.42);
}

.comment-manage-button.danger:hover,
.comment-manage-button.danger:focus-visible {
    border-color: #ff6b8b;
    box-shadow: 0 0 16px rgba(255, 107, 139, 0.32);
}

.comment-like-button .like-heart {
    font-size: 15px;
    line-height: 1;
    color: #ff6b8b;
}

.comment-like-button.liked {
    color: #ffd9e2;
    background: rgba(255, 107, 139, 0.16);
    border-color: rgba(255, 107, 139, 0.6);
}

.comment-like-button.liked:hover,
.comment-like-button.liked:focus-visible {
    border-color: #ff6b8b;
    box-shadow: 0 0 16px rgba(255, 107, 139, 0.32);
}

.comment-like-button.liked .like-heart {
    color: #ff4d77;
}

@media (max-width: 640px) {
    .article-page {
        padding: 96px 14px 40px;
    }

    .preview-body,
    .edit-form {
        padding: 20px;
    }

    .article-info-band {
        align-items: stretch;
        flex-direction: column;
    }

    .article-info-list {
        grid-template-columns: 1fr;
    }

    .drive-status {
        justify-content: center;
        width: 100%;
        box-sizing: border-box;
    }

    .comment-login-required {
        align-items: stretch;
        flex-direction: column;
    }

    .article-header {
        align-items: stretch;
    }

    .back-to-board {
        width: 100%;
    }

    .title-row,
    .form-actions {
        flex-direction: column;
        align-items: stretch;
    }
}
</style>
