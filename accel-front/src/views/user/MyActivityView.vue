<template>
    <main class="activity-page">
        <section class="activity-header">
            <RouterLink class="back-link" :to="{ name: 'myPage' }">
                <span aria-hidden="true">←</span>
                마이페이지로
            </RouterLink>

            <div class="activity-title-block">
                <p class="activity-kicker">My Activity</p>
                <h1>{{ activityConfig.title }}</h1>
            </div>

            <span class="result-count">{{ articles.length }}개</span>
        </section>

        <p class="activity-message" v-if="!currentUserId">
            로그인이 필요합니다.
            <RouterLink class="inline-link" :to="{ name: 'login', query: { redirect: route.fullPath } }">로그인하러 가기</RouterLink>
        </p>

        <p class="activity-message" v-else-if="loading">불러오는 중...</p>

        <template v-else>
            <section class="article-grid" v-if="articles.length" aria-label="게시글 목록">
                <RouterLink
                    class="article-card"
                    v-for="item in articles"
                    :key="item.articleId"
                    :to="{ name: 'BoardArticle', params: { articleId: `${item.articleId}` } }"
                >
                    <div class="article-thumb">
                        <img :src="articleThumbnail(item)" :alt="`${item.title} thumbnail`">
                    </div>

                    <div class="article-card-body">
                        <div class="article-topline">
                            <span class="article-brand-name">{{ item.boardName || '게시판' }}</span>
                            <span>{{ formatDate(item.createdAt) }}</span>
                        </div>

                        <h2>{{ item.title }}</h2>

                        <div class="article-meta">
                            <span>{{ item.userId }}</span>
                            <span>조회 {{ item.viewCnt ?? 0 }}</span>
                        </div>
                    </div>
                </RouterLink>
            </section>

            <p class="activity-message" v-else>{{ activityConfig.empty }}</p>
        </template>
    </main>
</template>

<script setup>
import videoThumbnail from '@/assets/video_thumbnail.png'
import { useAuthStore } from '@/stores/authStore'
import { useBoardStore } from '@/stores/boardStore'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const store = useBoardStore()
const authStore = useAuthStore()
const articles = ref([])
const loading = ref(false)

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

const ACTIVITY_CONFIG = {
    written: { title: '내가 작성한 글', fetch: 'fetchMyArticles', empty: '작성한 글이 없습니다.' },
    commented: { title: '내가 댓글 단 글', fetch: 'fetchMyCommentedArticles', empty: '댓글을 단 글이 없습니다.' },
    liked: { title: '내가 좋아요한 글', fetch: 'fetchMyLikedArticles', empty: '좋아요한 글이 없습니다.' },
}

const activityConfig = computed(() => ACTIVITY_CONFIG[route.params.type] ?? ACTIVITY_CONFIG.written)

const articleImages = (item) => {
    if (Array.isArray(item.imageUrls) && item.imageUrls.length > 0) {
        return item.imageUrls
    }

    return String(item.imageUrl ?? '')
        .split('||')
        .map((url) => url.trim())
        .filter(Boolean)
}

const articleThumbnail = (item) => {
    return articleImages(item)[0] || videoThumbnail
}

const formatDate = (date) => {
    return date ? String(date).slice(0, 10).replaceAll('-', '.') : ''
}

const loadActivity = async () => {
    if (!currentUserId.value) {
        articles.value = []
        return
    }

    loading.value = true

    try {
        const fetcher = store[activityConfig.value.fetch]
        articles.value = await fetcher(currentUserId.value)
    } catch (error) {
        console.error('목록을 불러오지 못했습니다:', error)
        articles.value = []
    } finally {
        loading.value = false
    }
}

watch(() => route.params.type, loadActivity)
watch(currentUserId, loadActivity)

onMounted(loadActivity)
</script>

<style scoped>
.activity-page {
    max-width: 1120px;
    margin: 0 auto;
    padding: 112px 24px 56px;
    color: var(--neon-text);
}

.activity-header {
    display: grid;
    grid-template-columns: minmax(150px, 1fr) auto minmax(150px, 1fr);
    align-items: center;
    gap: 18px;
    margin-bottom: 28px;
    padding-bottom: 18px;
    border-bottom: 1px solid rgba(140, 207, 255, 0.18);
}

.back-link {
    justify-self: start;
    display: inline-flex;
    align-items: center;
    gap: 8px;
    min-height: 48px;
    padding: 0 16px;
    color: #ffffff;
    background: rgba(4, 217, 255, 0.08);
    border: 1px solid rgba(140, 207, 255, 0.24);
    border-radius: 8px;
    font-size: 14px;
    font-weight: 800;
    text-decoration: none;
    transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.back-link span {
    color: var(--muted-blue);
    font-size: 18px;
    line-height: 1;
}

.back-link:hover,
.back-link:focus-visible {
    transform: translateY(-1px);
    border-color: var(--neon-cyan);
    box-shadow: var(--glow-blue);
    outline: none;
}

.activity-title-block {
    text-align: center;
}

.activity-kicker {
    margin: 0 0 6px;
    color: var(--muted-blue);
    font-size: 14px;
    font-weight: 700;
}

.activity-header h1 {
    margin: 0;
    color: #ffffff;
    font-size: 34px;
    line-height: 1.18;
}

.result-count {
    justify-self: end;
    display: inline-flex;
    align-items: center;
    min-height: 40px;
    padding: 0 14px;
    color: var(--neon-text);
    background: rgba(4, 217, 255, 0.08);
    border: 1px solid rgba(140, 207, 255, 0.24);
    border-radius: 999px;
    font-size: 13px;
    font-weight: 700;
}

.activity-message {
    margin: 48px 0;
    color: var(--muted-blue);
    text-align: center;
    line-height: 1.7;
}

.inline-link {
    color: var(--neon-cyan);
    font-weight: 800;
}

.article-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    align-content: start;
    gap: 24px;
}

.article-card {
    overflow: hidden;
    display: flex;
    flex-direction: column;
    color: #fff;
    text-decoration: none;
    background: var(--night-panel);
    border: 1px solid rgba(4, 217, 255, 0.18);
    border-radius: 8px;
    cursor: pointer;
    transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.article-card:hover,
.article-card:focus-visible {
    border-color: var(--neon-cyan);
    box-shadow: var(--glow-blue);
    transform: translateY(-2px);
    outline: none;
}

.article-thumb {
    height: 118px;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    background: #000;
    border-bottom: 1px solid rgba(140, 207, 255, 0.22);
}

.article-thumb img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
}

.article-card-body {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 14px;
}

.article-topline,
.article-meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    color: var(--muted-blue);
    font-size: 13px;
}

.article-brand-name {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.article-topline span:last-child {
    flex: 0 0 auto;
}

.article-card h2 {
    margin: 0;
    color: #ffffff;
    font-size: 17px;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

@media (max-width: 640px) {
    .activity-page {
        padding: 96px 14px 48px;
    }

    .activity-header {
        grid-template-columns: 1fr;
        align-items: stretch;
        gap: 12px;
    }

    .activity-title-block {
        order: -1;
    }

    .back-link {
        justify-self: stretch;
    }

    .result-count {
        justify-self: stretch;
        justify-content: center;
    }
}
</style>
