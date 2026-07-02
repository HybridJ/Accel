<template>
    <main class="video-showroom-page">
        <section class="showroom-header">
            <RouterLink class="back-to-category" :to="{ name: 'videoCategory' }">
                <span aria-hidden="true">←</span>
                목록으로 돌아가기
            </RouterLink>

            <div class="showroom-title-block">
                <p class="showroom-kicker">Review Collection</p>
                <h1>자동차 극장</h1>
            </div>

            <div class="header-summary">
                <div class="showroom-context">
                    <span>카테고리</span>
                    <strong>{{ displayCategory }}</strong>
                </div>

                <div class="result-count">
                    <span>검색 결과</span>
                    <strong>{{ filteredVideos.length }}대</strong>
                </div>
            </div>
        </section>

        <section class="showroom-controls" aria-label="자동차 극장 필터">
            <div class="search-row">
                <input
                    v-model="keyword"
                    type="search"
                    placeholder="차량명, 브랜드, 영상 제목 검색"
                    aria-label="자동차 극장 검색"
                >
                <button type="button" class="reset-button" :disabled="!hasActiveFilters" @click="resetFilters">
                    초기화
                </button>
            </div>

            <div class="filter-grid">
                <label class="filter-field">
                    <span>브랜드</span>
                    <select v-model="selectedBrand">
                        <option value="">전체</option>
                        <option v-for="brand in brandOptions" :key="brand" :value="brand">
                            {{ brand }}
                        </option>
                    </select>
                </label>

                <label class="filter-field">
                    <span>세그먼트</span>
                    <select v-model="selectedSegment">
                        <option value="">전체</option>
                        <option v-for="segment in segmentOptions" :key="segment" :value="segment">
                            {{ segment }}
                        </option>
                    </select>
                </label>

                <label class="filter-field">
                    <span>연료</span>
                    <select v-model="selectedFuelType">
                        <option value="">전체</option>
                        <option v-for="fuelType in fuelTypeOptions" :key="fuelType" :value="fuelType">
                            {{ fuelType }}
                        </option>
                    </select>
                </label>

                <label class="filter-field">
                    <span>좌석</span>
                    <select v-model="selectedSeating">
                        <option value="">전체</option>
                        <option v-for="seating in seatingOptions" :key="seating" :value="String(seating)">
                            {{ seating }}인승
                        </option>
                    </select>
                </label>

                <label class="filter-field">
                    <span>가격대</span>
                    <select v-model="selectedPriceRange">
                        <option v-for="range in priceRanges" :key="range.value" :value="range.value">
                            {{ range.label }}
                        </option>
                    </select>
                </label>

                <label class="filter-field">
                    <span>바디 타입</span>
                    <select v-model="selectedBodyType">
                        <option value="">전체</option>
                        <option v-for="bodyType in bodyTypeOptions" :key="bodyType" :value="bodyType">
                            {{ bodyType }}
                        </option>
                    </select>
                </label>

                <label class="filter-field">
                    <span>정렬</span>
                    <select v-model="sortBy">
                        <option value="name">차량명순</option>
                        <option value="price-low">낮은 가격순</option>
                        <option value="price-high">높은 가격순</option>
                        <option value="seating-high">좌석 많은순</option>
                    </select>
                </label>
            </div>
        </section>

        <section class="video-grid" aria-label="자동차 극장 목록">
            <RouterLink
                class="idv-video-frame"
                v-for="video in paginatedVideos"
                :key="video.vehicleId"
                :to="{ name: 'videoArticle', params: { vehicleId: video.vehicleId } }"
            >
                <div class="thumbnail">
                    <img
                        :src="store.youtubeThumbnail(video.url) || defaultThumbnail"
                        :alt="`${video.brandNameKo || video.brandName} ${video.vehicleNameKo || video.vehicleName} thumbnail`"
                        @error="event => event.target.src = defaultThumbnail"
                    >
                </div>

                <div class="video-card-body">
                    <div class="video-heading">
                        <span class="video-brand">{{ video.brandNameKo || video.brandName }}</span>
                        <strong class="video-model">{{ video.vehicleNameKo || video.vehicleName }}</strong>
                    </div>

                    <div class="video-meta">
                        <span>{{ video.segment || '-' }}</span>
                        <span>{{ video.seating ? `${video.seating}인승` : '-' }}</span>
                        <span>{{ priceText(video) }}</span>
                    </div>

                    <div class="fuel-list" v-if="video.fuelTypes?.length">
                        <span v-for="fuelType in video.fuelTypes" :key="fuelType">
                            {{ fuelType }}
                        </span>
                    </div>
                </div>
            </RouterLink>

            <p class="empty-message" v-if="filteredVideos.length === 0">
                조건에 맞는 자동차 극장 콘텐츠가 없습니다.
            </p>
        </section>

        <nav class="pagination" v-if="totalPages > 1" aria-label="자동차 극장 페이지">
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
    </main>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useVideoStore } from '@/stores/videoStore';
import defaultThumbnail from '@/assets/video_thumbnail.png';

const store = useVideoStore()
const route = useRoute()

const pageSize = 12
const keyword = ref('')
const selectedBrand = ref('')
const selectedSegment = ref('')
const selectedFuelType = ref('')
const selectedSeating = ref('')
const selectedPriceRange = ref('')
const selectedBodyType = ref('')
const sortBy = ref('name')
const currentPage = ref(1)

const priceRanges = [
    { value: '', label: '전체', min: null, max: null },
    { value: 'under-3000', label: '3천만원 이하', min: null, max: 30000000 },
    { value: '3000-6000', label: '3천만~6천만원', min: 30000000, max: 60000000 },
    { value: '6000-10000', label: '6천만~1억원', min: 60000000, max: 100000000 },
    { value: '10000-20000', label: '1억~2억원', min: 100000000, max: 200000000 },
    { value: 'over-20000', label: '2억원 이상', min: 200000000, max: null },
]

const uniqueOptions = (values) => {
    return [...new Set(values.filter(Boolean))].sort((a, b) => String(a).localeCompare(String(b)))
}

const displayCategory = computed(() => {
    const category = String(route.params.category || 'All')
    return category.charAt(0).toUpperCase() + category.slice(1)
})
const brandOptions = computed(() => uniqueOptions(store.videos.map((video) => video.brandNameKo || video.brandName)))
const segmentOptions = computed(() => uniqueOptions(store.videos.map((video) => video.segment)))
const fuelTypeOptions = computed(() => uniqueOptions(store.videos.flatMap((video) => video.fuelTypes ?? [])))
const seatingOptions = computed(() => uniqueOptions(store.videos.map((video) => video.seating)).sort((a, b) => a - b))
const bodyTypeOptions = computed(() => uniqueOptions(store.videos.map((video) => video.bodyType)))

const hasActiveFilters = computed(() => {
    return Boolean(
        keyword.value.trim() ||
        selectedBrand.value ||
        selectedSegment.value ||
        selectedFuelType.value ||
        selectedSeating.value ||
        selectedPriceRange.value ||
        selectedBodyType.value ||
        sortBy.value !== 'name'
    )
})

const normalize = (value) => String(value ?? '').trim().toLowerCase()

const matchesKeyword = (video, search) => {
    if (!search) return true

    return [
        video.brandName,
        video.brandNameKo,
        video.vehicleName,
        video.vehicleNameKo,
        video.segment,
        video.bodyType,
        video.channelName,
        video.title,
        ...(video.fuelTypes ?? []),
    ]
        .filter(Boolean)
        .some((value) => normalize(value).includes(search))
}

const matchesPriceRange = (video) => {
    const range = priceRanges.find((item) => item.value === selectedPriceRange.value)
    if (!range || (!range.min && !range.max)) return true

    const minPrice = Number(video.minPrice) || 0
    const maxPrice = Number(video.maxPrice) || minPrice

    if (range.min && maxPrice < range.min) return false
    if (range.max && minPrice > range.max) return false

    return true
}

const filteredVideos = computed(() => {
    const search = normalize(keyword.value)

    return store.videos
        .filter((video) => matchesKeyword(video, search))
        .filter((video) => !selectedBrand.value || (video.brandNameKo || video.brandName) === selectedBrand.value)
        .filter((video) => !selectedSegment.value || video.segment === selectedSegment.value)
        .filter((video) => !selectedFuelType.value || video.fuelTypes?.includes(selectedFuelType.value))
        .filter((video) => !selectedSeating.value || String(video.seating) === selectedSeating.value)
        .filter((video) => !selectedBodyType.value || video.bodyType === selectedBodyType.value)
        .filter(matchesPriceRange)
        .sort((a, b) => {
            if (sortBy.value === 'price-low') {
                return (Number(a.minPrice) || 0) - (Number(b.minPrice) || 0)
            }

            if (sortBy.value === 'price-high') {
                return (Number(b.maxPrice) || 0) - (Number(a.maxPrice) || 0)
            }

            if (sortBy.value === 'seating-high') {
                return (Number(b.seating) || 0) - (Number(a.seating) || 0)
            }

            return `${a.brandNameKo || a.brandName} ${a.vehicleNameKo || a.vehicleName}`
                .localeCompare(`${b.brandNameKo || b.brandName} ${b.vehicleNameKo || b.vehicleName}`)
        })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredVideos.value.length / pageSize)))
const paginatedVideos = computed(() => {
    const start = (currentPage.value - 1) * pageSize
    return filteredVideos.value.slice(start, start + pageSize)
})

const visiblePages = computed(() => {
    const maxVisible = 5
    const total = totalPages.value
    const start = Math.max(1, Math.min(currentPage.value - 2, total - maxVisible + 1))
    const end = Math.min(total, start + maxVisible - 1)

    return Array.from({ length: end - start + 1 }, (_, index) => start + index)
})

const formatPrice = (price) => {
    const value = Number(price)
    if (!value) return ''

    return `${Math.round(value / 10000).toLocaleString()}만원`
}

const priceText = (video) => {
    const min = formatPrice(video.minPrice)
    const max = formatPrice(video.maxPrice)

    if (min && max) return `${min}~${max}`
    return min || max || '가격 정보 없음'
}

const resetFilters = () => {
    keyword.value = ''
    selectedBrand.value = ''
    selectedSegment.value = ''
    selectedFuelType.value = ''
    selectedSeating.value = ''
    selectedPriceRange.value = ''
    selectedBodyType.value = ''
    sortBy.value = 'name'
}

const goToPage = (page) => {
    currentPage.value = Math.min(Math.max(page, 1), totalPages.value)
}

watch(
    () => route.params.category,
    async (category) => {
        if (!category) return

        store.setCategory(category)
        await store.fetchShowroom(category)
        resetFilters()
        currentPage.value = 1
    },
    { immediate: true }
)

watch(
    [keyword, selectedBrand, selectedSegment, selectedFuelType, selectedSeating, selectedPriceRange, selectedBodyType, sortBy],
    () => {
        currentPage.value = 1
    }
)

watch(totalPages, (pageCount) => {
    if (currentPage.value > pageCount) {
        currentPage.value = pageCount
    }
})
</script>

<style scoped>
.video-showroom-page {
    width: min(100%, 1180px);
    margin: 0 auto;
    padding: 112px 24px 56px;
    box-sizing: border-box;
}

.showroom-header {
    display: grid;
    grid-template-columns: minmax(170px, 1fr) auto minmax(250px, 1fr);
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

.header-summary {
    justify-self: end;
    display: flex;
    align-items: stretch;
    justify-content: flex-end;
    gap: 10px;
    min-width: 0;
}

.showroom-context,
.result-count {
    display: inline-flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 4px;
    width: fit-content;
    min-width: 112px;
    padding: 10px 14px;
    background: rgba(4, 217, 255, 0.08);
    border: 1px solid rgba(140, 207, 255, 0.24);
    border-radius: 8px;
    text-align: center;
}

.showroom-context span,
.result-count span {
    color: var(--muted-blue);
    font-size: 12px;
    font-weight: 700;
}

.showroom-context strong,
.result-count strong {
    color: #ffffff;
    font-size: 16px;
    line-height: 1.2;
}

.showroom-title-block {
    text-align: center;
}

.showroom-kicker {
    margin: 0 0 6px;
    color: var(--muted-blue);
    font-size: 14px;
    font-weight: 700;
}

.showroom-header h1 {
    margin: 0;
    color: #ffffff;
    font-size: 38px;
    line-height: 1.18;
}

.showroom-controls {
    margin-bottom: 28px;
    padding: 18px;
    background: rgba(4, 17, 36, 0.78);
    border: 1px solid rgba(140, 207, 255, 0.2);
    border-radius: 8px;
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
    box-sizing: border-box;
    min-height: 42px;
    padding: 10px 12px;
    color: var(--neon-text);
    background: rgba(0, 0, 0, 0.58);
    border: 1px solid rgba(140, 207, 255, 0.26);
    border-radius: 6px;
}

.reset-button {
    min-width: 86px;
    padding: 0 16px;
}

.filter-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
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

.video-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    align-content: start;
    gap: 24px;
    min-height: 70vh;
}

.idv-video-frame {
    overflow: hidden;
    display: flex;
    flex-direction: column;
    min-height: 100%;
    color: inherit;
    text-decoration: none;
    background: var(--night-panel);
    border: 1px solid var(--line-blue);
    border-radius: 8px;
    box-shadow: 0 0 18px rgba(4, 217, 255, 0.13);
    cursor: pointer;
    transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.idv-video-frame:hover,
.idv-video-frame:focus-visible {
    border-color: var(--neon-cyan);
    transform: translateY(-4px);
    box-shadow: var(--glow-blue);
    outline: none;
}

.thumbnail {
    width: 100%;
    aspect-ratio: 16 / 9;
    overflow: hidden;
    background-color: rgba(4, 217, 255, 0.08);
}

.thumbnail img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
}

.video-card-body {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 16px;
}

.video-heading {
    min-height: 48px;
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.video-brand {
    color: var(--muted-blue);
    font-size: 13px;
    font-weight: 700;
    line-height: 1.25;
}

.video-model {
    color: var(--neon-text);
    font-size: 18px;
    font-weight: 800;
    line-height: 1.32;
    overflow-wrap: anywhere;
}

.video-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    color: var(--muted-blue);
    font-size: 13px;
}

.video-meta span {
    padding: 5px 8px;
    background: rgba(4, 217, 255, 0.08);
    border-radius: 999px;
}

.fuel-list {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: auto;
}

.fuel-list span {
    padding: 5px 8px;
    color: #ffffff;
    background: rgba(92, 255, 176, 0.1);
    border: 1px solid rgba(92, 255, 176, 0.26);
    border-radius: 999px;
    font-size: 12px;
    font-weight: 700;
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

@media (max-width: 960px) {
    .filter-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .video-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 640px) {
    .video-showroom-page {
        padding: 96px 14px 48px;
    }

    .showroom-header {
        grid-template-columns: 1fr;
        align-items: stretch;
        gap: 12px;
    }

    .back-to-category {
        justify-self: stretch;
        width: auto;
    }

    .showroom-header h1 {
        font-size: 30px;
    }

    .showroom-title-block {
        order: -1;
        text-align: center;
    }

    .header-summary {
        justify-self: stretch;
        justify-content: stretch;
    }

    .showroom-context,
    .result-count {
        flex: 1;
        width: auto;
        min-width: 0;
    }

    .showroom-controls {
        padding: 14px;
    }

    .search-row,
    .filter-grid,
    .video-grid {
        grid-template-columns: 1fr;
    }

    .reset-button {
        min-height: 42px;
    }

    .pagination {
        bottom: 10px;
        width: 100%;
        border-radius: 8px;
    }
}
</style>
