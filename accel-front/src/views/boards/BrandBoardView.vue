<template>
    <main class="brand-board">
        <section class="board-header">
            <RouterLink class="back-to-category" :to="{ name: 'BrandCategory' }">
                <span aria-hidden="true">←</span>
                목록으로 돌아가기
            </RouterLink>

            <div class="board-title-block">
                <p class="board-kicker">타랑께</p>
                <h1 class="brand-title">
                    <span class="brand-name-ko">{{ brandNameKo }} 타랑께</span>
                    <span class="brand-name-divider" v-if="showBrandNameEn">/</span>
                    <span class="brand-name-en" v-if="showBrandNameEn">{{ brandNameEn }}</span>
                </h1>
            </div>

            <span class="result-count">{{ filteredArticles.length }}개 게시글</span>
        </section>

        <section class="board-controls" aria-label="타랑께 검색 및 필터">
            <div class="search-row">
                <input
                    type="search"
                    v-model="keyword"
                    placeholder="제목, 타랑께 검색"
                    aria-label="타랑께 검색"
                >
                <button type="button" :disabled="!hasActiveFilters" @click="resetFilters">
                    초기화
                </button>
            </div>

            <div class="filter-grid">
                <label class="filter-field">
                    <span>시승</span>
                    <select v-model="driveFilter">
                        <option value="">전체</option>
                        <option value="available">시승 가능</option>
                        <option value="unavailable">시승 불가</option>
                    </select>
                </label>

                <label class="filter-field">
                    <span>기간</span>
                    <select v-model="dateFilter">
                        <option value="">전체</option>
                        <option value="7">최근 7일</option>
                        <option value="30">최근 30일</option>
                        <option value="90">최근 90일</option>
                    </select>
                </label>

                <label class="filter-field">
                    <span>정렬</span>
                    <select v-model="sortBy">
                        <option value="latest">최신순</option>
                        <option value="popular">조회수순</option>
                        <option value="likes">좋아요순</option>
                        <option value="oldest">오래된순</option>
                        <option value="drive">시승 가능 우선</option>
                    </select>
                </label>
            </div>
        </section>

        <section class="write-action" aria-label="게시글 작성">
            <button type="button" class="write-link" @click="handleWriteClick">
                글쓰기
            </button>
        </section>
        <section class="article-grid" aria-label="브랜드 게시글 목록">
            <RouterLink
                class="article-card"
                v-for="article in paginatedArticles"
                :key="article.articleId"
                :to="{ name: 'BoardArticle', params: { articleId: `${article.articleId}` } }"
            >
                <div class="article-thumb">
                    <img :src="articleThumbnail(article)" :alt="`${article.title} thumbnail`">
                </div>

                <div class="article-card-body">
                    <div class="article-topline">
                        <span class="article-brand-name">{{ articleBrandName(article) }}</span>
                        <span>{{ formatDate(article.createdAt) }}</span>
                    </div>

                    <h2>{{ article.title }}</h2>

                    <div class="article-meta">
                        <span>{{ article.userId }}</span>
                        <div class="article-stats">
                            <span class="article-like-count" aria-label="좋아요 수">
                                <span aria-hidden="true">♥</span>
                                {{ articleLikeCount(article) }}
                            </span>
                            <span>조회 {{ article.viewCnt ?? 0 }}</span>
                        </div>
                    </div>

                    <span class="drive-badge" :class="{ available: article.testDriveAvailable }">
                        {{ article.testDriveAvailable ? '시승 가능' : '시승 불가' }}
                    </span>
                </div>
            </RouterLink>

            <p class="empty-message" v-if="filteredArticles.length === 0">
                조건에 맞는 게시글이 없습니다.
            </p>
        </section>

        <nav class="pagination" v-if="totalPages > 1" aria-label="브랜드 게시글 페이지">
            <button type="button" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">
                이전
            </button>
            <button
                type="button"
                v-for="page in visiblePages"
                :key="page"
                :class="{ active: page === currentPage }"
                @click="goToPage(page)"
            >
                {{ page }}
            </button>
            <button type="button" :disabled="currentPage === totalPages" @click="goToPage(currentPage + 1)">
                다음
            </button>
        </nav>

        <div
            class="login-required-modal"
            v-if="showLoginModal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="login-required-title"
            @click.self="closeLoginModal"
        >
            <div class="login-required-dialog">
                <h2 id="login-required-title">로그인이 필요합니다</h2>
                <p>글쓰기는 로그인 후 이용할 수 있습니다.</p>

                <div class="login-required-actions">
                    <button type="button" class="modal-cancel" @click="closeLoginModal">닫기</button>
                    <button type="button" class="modal-confirm" @click="goToLogin">로그인하러 가기</button>
                </div>
            </div>
        </div>
    </main>
</template>

<script setup>
import videoThumbnail from '@/assets/video_thumbnail.png';
import { resolveArticleImageUrls } from '@/lib/articleImage';
import { useAuthStore } from '@/stores/authStore';
import { useBoardStore } from '@/stores/boardStore';
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const store = useBoardStore()
const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const showLoginModal = ref(false)
const keyword = ref('')
const driveFilter = ref('')
const dateFilter = ref('')
const sortBy = ref('latest')
const currentPage = ref(1)
const pageSize = 12
const brands = ref([])

const selectedBrand = computed(() => {
    const routeBrandId = Number(route.params.brandId)

    return brands.value.find((brand) => brand.brandId === routeBrandId)
})

const cleanText = (value) => String(value ?? '').trim()

const formatBrandName = (ko, en, fallback = '') => {
    const koName = cleanText(ko)
    const enName = cleanText(en)

    if (koName && enName && koName.toLowerCase() !== enName.toLowerCase()) {
        return `${koName} / ${enName}`
    }

    return koName || enName || fallback
}

const brandNameEn = computed(() => {
    return selectedBrand.value?.boardName ?? `Brand ${route.params.brandId}`
})

const brandNameKo = computed(() => {
    return selectedBrand.value?.boardNameKo ?? brandNameEn.value
})

const showBrandNameEn = computed(() => {
    return cleanText(brandNameEn.value) && cleanText(brandNameEn.value).toLowerCase() !== cleanText(brandNameKo.value).toLowerCase()
})

const brandName = computed(() => {
    return formatBrandName(brandNameKo.value, brandNameEn.value, `Brand ${route.params.brandId}`)
})

const articleBrandName = (article) => {
    return formatBrandName(
        article.boardNameKo || brandNameKo.value,
        article.boardName || brandNameEn.value,
        brandName.value
    )
}

const articleImages = (article) => resolveArticleImageUrls(article)

const articleThumbnail = (article) => {
    return articleImages(article)[0] || videoThumbnail
}

const articleLikeCount = (article) => {
    return Number(article.likeCount) || 0
}

const hasActiveFilters = computed(() => {
    return Boolean(
        keyword.value.trim() ||
        driveFilter.value ||
        dateFilter.value ||
        sortBy.value !== 'latest'
    )
})

const normalize = (value) => cleanText(value).toLowerCase()

const articleDateTime = (date) => {
    if (!date) return 0

    return new Date(String(date).replace(' ', 'T')).getTime() || 0
}

const matchesDateFilter = (article) => {
    if (!dateFilter.value) return true

    const createdAt = articleDateTime(article.createdAt)
    if (!createdAt) return false

    const days = Number(dateFilter.value)
    const cutoff = Date.now() - days * 24 * 60 * 60 * 1000

    return createdAt >= cutoff
}

const filteredArticles = computed(() => {
    const search = normalize(keyword.value)

    return store.articles
        .filter((article) => {
            if (!search) return true

            return [article.title, article.boardName, article.boardNameKo]
                .filter(Boolean)
                .some((value) => normalize(value).includes(search))
        })
        .filter((article) => {
            if (driveFilter.value === 'available') return Boolean(article.testDriveAvailable)
            if (driveFilter.value === 'unavailable') return !article.testDriveAvailable
            return true
        })
        .filter(matchesDateFilter)
        .sort((a, b) => {
            if (sortBy.value === 'popular') {
                return (Number(b.viewCnt) || 0) - (Number(a.viewCnt) || 0)
            }

            if (sortBy.value === 'likes') {
                return articleLikeCount(b) - articleLikeCount(a)
                    || articleDateTime(b.createdAt) - articleDateTime(a.createdAt)
            }

            if (sortBy.value === 'oldest') {
                return articleDateTime(a.createdAt) - articleDateTime(b.createdAt)
            }

            if (sortBy.value === 'drive') {
                return Number(Boolean(b.testDriveAvailable)) - Number(Boolean(a.testDriveAvailable))
            }

            return articleDateTime(b.createdAt) - articleDateTime(a.createdAt)
        })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredArticles.value.length / pageSize)))
const paginatedArticles = computed(() => {
    const start = (currentPage.value - 1) * pageSize
    return filteredArticles.value.slice(start, start + pageSize)
})

const visiblePages = computed(() => {
    const maxVisible = 5
    const total = totalPages.value
    const start = Math.max(1, Math.min(currentPage.value - 2, total - maxVisible + 1))
    const end = Math.min(total, start + maxVisible - 1)

    return Array.from({ length: end - start + 1 }, (_, index) => start + index)
})

const formatDate = (date) => {
    return date ? String(date).slice(0, 10).replaceAll('-', '.') : ''
}

const resetFilters = () => {
    keyword.value = ''
    driveFilter.value = ''
    dateFilter.value = ''
    sortBy.value = 'latest'
}

const goToPage = (page) => {
    currentPage.value = Math.min(Math.max(page, 1), totalPages.value)
}

const handleWriteClick = () => {
    if (authStore.isLogin) {
        router.push({ name: 'BoardWrite', params: { brandId: `${route.params.brandId}` } })
        return
    }

    showLoginModal.value = true
}

const closeLoginModal = () => {
    showLoginModal.value = false
}

const goToLogin = () => {
    // 로그인 후 원래 접근하려던 글쓰기 페이지로 돌아오도록 redirect를 함께 전달한다.
    const redirect = router.resolve({
        name: 'BoardWrite',
        params: { brandId: `${route.params.brandId}` },
    }).fullPath

    router.push({ name: 'login', query: { redirect } })
}

const loadArticles = async () => {
    await store.fetchArticles(route.params.brandId)
    resetFilters()
    currentPage.value = 1
}

watch(() => route.params.brandId, loadArticles)

watch([keyword, driveFilter, dateFilter, sortBy], () => {
    currentPage.value = 1
})

watch(totalPages, (pageCount) => {
    if (currentPage.value > pageCount) {
        currentPage.value = pageCount
    }
})

onMounted(async () => {
    brands.value = await store.fetchBrands()
    await loadArticles()
})
</script>

<style scoped>
.brand-board {
    max-width: 1120px;
    margin: 0 auto;
    padding: 112px 24px 56px;
}

.board-header {
    display: grid;
    grid-template-columns: minmax(170px, 1fr) auto minmax(170px, 1fr);
    align-items: center;
    gap: 18px;
    margin-bottom: 24px;
    padding-bottom: 18px;
    border-bottom: 1px solid rgba(140, 207, 255, 0.18);
}

.back-to-category {
    justify-self: start;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    width: fit-content;
    min-width: 150px;
    min-height: 56px;
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

.back-to-category span {
    color: var(--muted-blue);
    font-size: 18px;
    line-height: 1;
}

.back-to-category:hover,
.back-to-category:focus-visible {
    transform: translateY(-1px);
    border-color: var(--neon-cyan);
    box-shadow: var(--glow-blue);
    outline: none;
}

.board-title-block {
    text-align: center;
}

.board-kicker {
    margin: 0 0 6px;
    color: var(--muted-blue);
    font-size: 14px;
    font-weight: 700;
}

.board-header h1 {
    margin: 0;
    color: #ffffff;
    font-size: 38px;
    line-height: 1.18;
}

.brand-title {
    display: flex;
    align-items: baseline;
    justify-content: center;
    flex-wrap: wrap;
    gap: 8px;
}

.brand-name-en,
.brand-name-divider {
    color: var(--muted-blue);
    font-size: 0.62em;
    font-weight: 800;
}

.result-count {
    justify-self: end;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 40px;
    padding: 0 14px;
    color: var(--neon-text);
    background: rgba(4, 217, 255, 0.08);
    border: 1px solid rgba(140, 207, 255, 0.24);
    border-radius: 999px;
    font-size: 13px;
    font-weight: 700;
}

.board-controls {
    margin-bottom: 28px;
    padding: 18px;
    background: rgba(4, 17, 36, 0.78);
    border: 1px solid rgba(140, 207, 255, 0.2);
    border-radius: 8px;
}

.write-action {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 28px;
}

.write-link {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 96px;
    min-height: 42px;
    padding: 0 16px;
    color: #ffffff;
    background: rgba(4, 217, 255, 0.1);
    border: 1px solid rgba(140, 207, 255, 0.3);
    border-radius: 8px;
    font-weight: 800;
    text-decoration: none;
    box-shadow: none;
    cursor: pointer;
    transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.write-link:hover,
.write-link:focus-visible {
    transform: translateY(-1px);
    border-color: var(--neon-cyan);
    box-shadow: var(--glow-blue);
    outline: none;
}

.search-row {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 12px;
    margin-bottom: 16px;
}

.search-row input,
.filter-field select {
    width: 100%;
    min-width: 0;
    min-height: 42px;
    box-sizing: border-box;
    padding: 10px 12px;
    color: var(--neon-text);
    background: rgba(0, 0, 0, 0.58);
    border: 1px solid rgba(140, 207, 255, 0.26);
    border-radius: 6px;
}

.search-row button {
    min-width: 86px;
    padding: 0 16px;
}

.filter-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 14px;
}

.filter-field {
    display: flex;
    flex-direction: column;
    gap: 7px;
}

.filter-field span {
    color: var(--muted-blue);
    font-size: 13px;
    font-weight: 700;
}

.article-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    align-content: start;
    gap: 24px;
    min-height: 70vh;
}

.article-card {
    min-height: 100%;
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
    position: relative;
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 14px 14px 52px;
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

.article-meta > span {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.article-stats {
    display: inline-flex;
    align-items: center;
    justify-content: flex-end;
    gap: 10px;
    flex: 0 0 auto;
    white-space: nowrap;
}

.article-like-count {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: #ffd9e2;
    font-weight: 800;
}

.article-like-count span {
    color: #ff6b8b;
    font-size: 14px;
    line-height: 1;
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

.drive-badge {
    position: absolute;
    right: 14px;
    bottom: 14px;
    padding: 6px 9px;
    border-radius: 999px;
    background: rgba(255, 209, 102, 0.14);
    border: 1px solid rgba(255, 209, 102, 0.32);
    color: #ffe8ad;
    font-size: 12px;
    font-weight: 700;
}

.drive-badge.available {
    background: rgba(92, 255, 176, 0.12);
    border-color: rgba(92, 255, 176, 0.3);
    color: #bfffe0;
}

.empty-message {
    grid-column: 1 / -1;
    padding: 52px 16px;
    color: var(--muted-blue);
    text-align: center;
}

.pagination {
    position: sticky;
    bottom: 16px;
    z-index: 30;
    display: flex;
    justify-content: center;
    flex-wrap: wrap;
    gap: 8px;
    width: min(100%, 520px);
    margin: 32px auto 0;
    padding: 10px;
    background: rgba(0, 0, 0, 0.82);
    border: 1px solid rgba(140, 207, 255, 0.24);
    border-radius: 999px;
    box-shadow: 0 14px 42px rgba(0, 0, 0, 0.4), var(--glow-blue);
    backdrop-filter: blur(12px);
}

.pagination button {
    min-width: 42px;
    min-height: 38px;
    padding: 0 12px;
    box-shadow: none;
}

.pagination button.active {
    border-color: var(--neon-cyan);
    box-shadow: var(--glow-blue);
}

@media (max-width: 760px) {
    .filter-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 640px) {
    .brand-board {
        padding: 96px 14px 48px;
    }

    .board-header {
        grid-template-columns: 1fr;
        align-items: stretch;
        gap: 12px;
    }

    .board-title-block {
        order: -1;
    }

    .back-to-category {
        justify-self: stretch;
        width: auto;
    }

    .board-header h1 {
        font-size: 30px;
    }

    .result-count {
        justify-self: stretch;
    }

    .board-controls {
        padding: 14px;
    }

    .write-action {
        justify-content: stretch;
    }

    .write-link {
        width: 100%;
    }

    .search-row,
    .filter-grid {
        grid-template-columns: 1fr;
    }

    .search-row button {
        min-height: 42px;
    }

    .pagination {
        bottom: 10px;
        width: 100%;
        border-radius: 8px;
    }
}

.login-required-modal {
    position: fixed;
    inset: 0;
    z-index: 2000;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    background: rgba(0, 0, 0, 0.72);
    backdrop-filter: blur(4px);
}

.login-required-dialog {
    width: min(100%, 380px);
    padding: 28px 24px 22px;
    text-align: center;
    background: var(--night-panel-strong);
    border: 1px solid var(--line-blue);
    border-radius: 12px;
    box-shadow: var(--glow-blue);
}

.login-required-dialog h2 {
    margin: 0 0 10px;
    color: #ffffff;
    font-size: 20px;
}

.login-required-dialog p {
    margin: 0 0 22px;
    color: var(--muted-blue);
    font-size: 14px;
    line-height: 1.6;
}

.login-required-actions {
    display: flex;
    justify-content: center;
    gap: 12px;
}

.login-required-actions button {
    min-width: 120px;
    min-height: 42px;
    padding: 0 16px;
    font-weight: 800;
    cursor: pointer;
}

.modal-cancel {
    background: rgba(4, 217, 255, 0.08);
    box-shadow: none;
}
</style>


