<template>
    <div class="video-article-page" v-if="store.video">
        <header class="article-header">
            <button class="back-to-list-button" type="button" @click="toShowroom">
                <span aria-hidden="true">&lt;</span>
                목록으로
            </button>
            <h1 class="vehicle-name">
                {{ store.video.brandNameKo }} {{ store.video.vehicleNameKo }}
            </h1>
            <span class="header-spacer" aria-hidden="true"></span>
        </header>

        <div class="content-layout" :class="{ 'has-side': hasSideColumn }">
            <div class="main-column">
                <section class="main-video" v-if="selectedVideo">
                    <div class="youtube-video">
                        <iframe :src="selectedVideo.url"
                                title="YouTube video"
                                frameborder="0"
                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                                allowfullscreen></iframe>
                    </div>

                    <div class="video-meta">
                        <p class="video-meta-title" :title="selectedVideo.title">{{ selectedVideo.title }}</p>
                        <p class="video-meta-channel">{{ selectedVideo.channelName }}</p>
                    </div>
                </section>
                <p v-else class="no-video">등록된 영상이 없습니다.</p>

                <section class="info-section">
                    <div class="spec-card">
                        <h2 class="spec-heading">차량 상세 스펙</h2>
                        <dl class="spec-grid">
                            <div class="spec-item">
                                <dt>브랜드</dt>
                                <dd>{{ store.video.brandNameKo || '-' }}</dd>
                            </div>
                            <div class="spec-item">
                                <dt>세그먼트</dt>
                                <dd>{{ store.video.segment || '-' }}</dd>
                            </div>
                            <div class="spec-item">
                                <dt>차종</dt>
                                <dd>{{ store.video.bodyType || '-' }}</dd>
                            </div>
                            <div class="spec-item">
                                <dt>연료</dt>
                                <dd>{{ fuelText }}</dd>
                            </div>
                            <div class="spec-item">
                                <dt>좌석</dt>
                                <dd>{{ store.video.seating ? store.video.seating + '인승' : '-' }}</dd>
                            </div>
                            <div class="spec-item">
                                <dt>가격</dt>
                                <dd>{{ priceText }}</dd>
                            </div>
                        </dl>

                        <p class="spec-description" v-if="store.video.description">
                            {{ store.video.description }}
                        </p>
                    </div>

                    <button class="brand-board-button" type="button" @click="toBrandBoard">
                        {{ store.video.brandNameKo }} 타랑께로 이동
                    </button>
                </section>
            </div>

            <div class="side-column" v-if="hasSideColumn">
                <aside class="side-videos" v-if="sideVideos.length">
                    <h2 class="side-heading">연관 영상</h2>
                    <div class="side-video-list">
                        <button class="side-video"
                                v-for="video in sideVideos"
                                :key="video.videoId || video.url"
                                type="button"
                                @click="selectVideo(video)">
                            <img :src="store.youtubeThumbnail(video.url, 'mqdefault') || defaultThumbnail"
                                 :alt="`${video.title} thumbnail`"
                                 @error="event => event.target.src = defaultThumbnail">
                            <span>
                                <strong>{{ video.title }}</strong>
                                <small>{{ video.channelName }}</small>
                            </span>
                        </button>
                    </div>
                </aside>

                <section class="recommend" v-if="recommendedVehicles.length">
                    <h2 class="recommend-heading">이런 차량은 어때요?</h2>
                    <ul class="recommend-list">
                        <li v-for="vehicle in recommendedVehicles" :key="vehicle.vehicleId">
                            <button class="recommend-item" type="button" @click="goToVehicle(vehicle)">
                                <img :src="store.youtubeThumbnail(vehicle.url, 'mqdefault') || defaultThumbnail"
                                     :alt="`${vehicle.brandNameKo} ${vehicle.vehicleNameKo}`"
                                     @error="event => event.target.src = defaultThumbnail">
                                <span class="recommend-info">
                                    <strong>{{ vehicle.brandNameKo }} {{ vehicle.vehicleNameKo }}</strong>
                                    <small v-if="vehicle.minPrice">{{ formatPrice(vehicle.minPrice) }}~</small>
                                </span>
                            </button>
                        </li>
                    </ul>
                </section>
            </div>
        </div>

    </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useVideoStore } from '@/stores/videoStore';
import defaultThumbnail from '@/assets/video_thumbnail.png';

const store = useVideoStore()
const route = useRoute()
const router = useRouter()
const selectedVideo = ref(null)

const videoList = computed(() => {
    if (!store.video) return []
    if (store.video.videos?.length) return store.video.videos
    return store.video.url ? [store.video] : []
})

const sideVideos = computed(() => {
    return videoList.value.filter((video) => video.url !== selectedVideo.value?.url)
})

const recommendedVehicles = computed(() => {
    const currentId = store.video?.vehicleId
    return (store.recommendations ?? [])
        .filter((vehicle) => vehicle.vehicleId !== currentId)
        .slice(0, 6)
})

const hasSideColumn = computed(
    () => sideVideos.value.length > 0 || recommendedVehicles.value.length > 0
)

const fuelText = computed(() => {
    const fuels = store.video?.fuelTypes
    return fuels && fuels.length ? fuels.join(' · ') : '-'
})

const formatPrice = (won) => {
    if (won == null) return null
    return Math.round(won / 10000).toLocaleString('ko-KR') + '만원'
}

const priceText = computed(() => {
    const min = formatPrice(store.video?.minPrice)
    const max = formatPrice(store.video?.maxPrice)
    if (min && max) return `${min} ~ ${max}`
    return min || max || '-'
})

const selectVideo = function(video) {
    selectedVideo.value = video
}

const goToVehicle = function(vehicle) {
    router.push({ name: 'videoArticle', params: { vehicleId: `${vehicle.vehicleId}` } })
}

const toShowroom = function(){
    const category = store.video?.category || store.category
    if (category) {
        return router.push({ name: 'videoShowroom', params: { category } })
    }
    return router.push({ name: 'videoCategory' })
}

const toBrandBoard = function(){
    const brandId = store.video?.brandId
    if (brandId == null) return
    return router.push({
        name: 'BrandBoard',
        params: { brandId: `${brandId}` },
    })
}

watch(
    () => route.params.vehicleId,
    async(vehicleId) => {
        if(!vehicleId) return

        store.setVideoId(vehicleId)
        await store.fetchVideo(vehicleId)
        selectedVideo.value = videoList.value[0] ?? null

        if (store.video?.category) {
            await store.fetchRecommendations(store.video.category)
        }
        window.scrollTo({ top: 0 })
    },
    {immediate: true}
)
</script>

<style scoped>
.video-article-page {
    width: min(100%, 2000px);
    margin: 0 auto;
    padding: 112px 32px 56px;
    box-sizing: border-box;
    color: var(--neon-text);
}

.article-header {
    display: grid;
    grid-template-columns: minmax(140px, 1fr) auto minmax(140px, 1fr);
    align-items: center;
    gap: 18px;
    margin-bottom: 28px;
    text-align: center;
}

.back-to-list-button {
    justify-self: start;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    width: fit-content;
    min-width: 112px;
    min-height: 44px;
    padding: 0 14px;
    color: #ffffff;
    background: rgba(4, 217, 255, 0.08);
    border: 1px solid rgba(140, 207, 255, 0.24);
    border-radius: 8px;
    font-size: 14px;
    font-weight: 800;
    text-decoration: none;
    box-shadow: none;
    cursor: pointer;
    transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.back-to-list-button span {
    color: var(--muted-blue);
    font-size: 18px;
    line-height: 1;
}

.back-to-list-button:hover,
.back-to-list-button:focus-visible {
    transform: translateY(-1px);
    border-color: var(--neon-cyan);
    box-shadow: var(--glow-blue);
    outline: none;
}

.header-spacer {
    min-width: 112px;
}

.vehicle-name {
    margin: 0;
    font-size: 28px;
    font-weight: 700;
    color: var(--neon-text);
    text-shadow: 0 0 18px rgba(4, 217, 255, 0.4);
}

.content-layout {
    display: grid;
    grid-template-columns: 1fr;
    gap: 24px;
    align-items: start;
}

.content-layout.has-side {
    grid-template-columns: minmax(0, 1fr) 480px;
}

.main-column {
    min-width: 0;
}

.main-video {
    min-width: 0;
}

.youtube-video {
    width: 100%;
    aspect-ratio: 16 / 9;
    border-radius: 10px;
    overflow: hidden;
    border: 1px solid var(--line-blue);
    box-shadow: var(--glow-blue);
    background: #05070f;
}

.youtube-video iframe {
    width: 100%;
    height: 100%;
    display: block;
}

.no-video {
    padding: 48px 0;
    text-align: center;
    color: var(--muted-blue);
}

.video-meta {
    margin-top: 16px;
    padding-bottom: 18px;
    border-bottom: 1px solid var(--line-blue);
}

.video-meta-title {
    margin: 0 0 8px;
    font-size: 18px;
    font-weight: 600;
    line-height: 1.45;
    color: var(--neon-text);
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
    min-height: 2.9em;
}

.video-meta-channel {
    margin: 0;
    font-size: 14px;
    color: var(--muted-blue);
}

.side-column {
    display: flex;
    flex-direction: column;
    gap: 24px;
    min-width: 0;
}

.side-videos {
    display: flex;
    flex-direction: column;
    gap: 14px;
    padding: 16px;
    background: var(--night-panel);
    border: 1px solid var(--line-blue);
    border-radius: 10px;
    box-shadow: 0 0 18px rgba(4, 217, 255, 0.1);
    /* 메인 영상(16:9) 높이에 맞춤. 568px = 좌우 패딩(64) + 사이드바(480) + 간격(24) */
    height: calc((min(100vw, 2000px) - 568px) * 9 / 16);
    box-sizing: border-box;
}

.side-heading {
    margin: 0;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--line-blue);
    font-size: 16px;
    font-weight: 700;
    color: var(--neon-cyan);
}

.side-video-list {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 16px;
    overflow-y: auto;
    min-height: 0;
    padding-right: 4px;
}

.side-video {
    display: grid;
    grid-template-columns: 240px minmax(0, 1fr);
    gap: 14px;
    width: 100%;
    padding: 0;
    border: 0;
    background: transparent;
    color: inherit;
    text-align: left;
    cursor: pointer;
}

.side-video img {
    width: 240px;
    aspect-ratio: 16 / 9;
    border-radius: 6px;
    object-fit: cover;
    background-color: rgba(4, 217, 255, 0.08);
}

.side-video span {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.side-video strong {
    color: var(--neon-text);
    font-size: 15px;
    line-height: 1.35;
    font-weight: 600;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
}

.side-video small {
    color: var(--muted-blue);
    font-size: 13px;
    line-height: 1.3;
}

.recommend {
    display: flex;
    flex-direction: column;
    gap: 14px;
    padding: 16px;
    background: var(--night-panel);
    border: 1px solid var(--line-blue);
    border-radius: 10px;
    box-shadow: 0 0 18px rgba(4, 217, 255, 0.1);
    box-sizing: border-box;
}

.recommend-heading {
    margin: 0;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--line-blue);
    font-size: 16px;
    font-weight: 700;
    color: var(--neon-cyan);
}

.recommend-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;
}

.recommend-item {
    display: flex;
    flex-direction: column;
    gap: 8px;
    width: 100%;
    padding: 0;
    border: 0;
    background: transparent;
    box-shadow: none;
    color: inherit;
    text-align: left;
    cursor: pointer;
}

.recommend-item img {
    width: 100%;
    aspect-ratio: 16 / 9;
    border-radius: 6px;
    object-fit: cover;
    background-color: rgba(4, 217, 255, 0.08);
    transition: box-shadow 0.2s ease;
}

.recommend-item:hover img {
    box-shadow: var(--glow-blue);
}

.recommend-item:hover strong {
    color: var(--neon-cyan);
}

.recommend-info {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.recommend-item strong {
    color: var(--neon-text);
    font-size: 14px;
    font-weight: 600;
    line-height: 1.3;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
}

.recommend-item small {
    color: var(--muted-blue);
    font-size: 12px;
}

.info-section {
    margin-top: 28px;
}

.spec-card {
    padding: 24px;
    background: var(--night-panel);
    border: 1px solid var(--line-blue);
    border-radius: 10px;
    box-shadow: 0 0 18px rgba(4, 217, 255, 0.13);
}

.spec-heading {
    margin: 0 0 18px;
    font-size: 18px;
    font-weight: 700;
    color: var(--neon-cyan);
}

.spec-grid {
    margin: 0;
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
}

.spec-item {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 14px 16px;
    background: rgba(4, 217, 255, 0.05);
    border: 1px solid var(--line-blue);
    border-radius: 8px;
}

.spec-item dt {
    font-size: 12px;
    color: var(--muted-blue);
}

.spec-item dd {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: var(--neon-text);
}

.spec-description {
    margin: 12px 0 0;
    padding: 16px;
    background: rgba(4, 217, 255, 0.05);
    border: 1px solid var(--line-blue);
    border-radius: 8px;
    font-size: 15px;
    line-height: 1.7;
    color: var(--neon-text);
}

.brand-board-button {
    position: relative;
    width: 100%;
    min-height: 64px;
    margin-top: 16px;
    padding: 18px 20px;
    color: var(--neon-text);
    background:
        linear-gradient(180deg, rgba(68, 72, 82, 0.82), rgba(33, 37, 46, 0.9)) padding-box,
        linear-gradient(180deg, rgba(190, 198, 210, 0.32), rgba(96, 104, 118, 0.28)) border-box;
    border: 1px solid transparent;
    border-radius: 8px;
    box-shadow:
        inset 0 1px 0 rgba(255, 255, 255, 0.12),
        0 8px 20px rgba(0, 0, 0, 0.22);
    font-size: 16px;
    font-weight: 800;
    cursor: pointer;
    transition:
        color 0.2s ease,
        background 0.2s ease,
        box-shadow 0.2s ease;
}

.brand-board-button:hover,
.brand-board-button:focus-visible {
    color: #ffffff;
    outline: none;
    background:
        linear-gradient(180deg, rgba(79, 84, 96, 0.92), rgba(39, 44, 55, 0.96)) padding-box,
        conic-gradient(
            from var(--glow-angle),
            rgba(4, 217, 255, 0.2),
            rgba(107, 241, 255, 0.95),
            rgba(35, 93, 255, 0.98),
            rgba(4, 217, 255, 0.22),
            rgba(107, 241, 255, 0.95),
            rgba(4, 217, 255, 0.2)
        ) border-box;
    box-shadow:
        0 0 16px rgba(4, 217, 255, 0.38),
        0 0 34px rgba(35, 93, 255, 0.28),
        inset 0 0 18px rgba(4, 217, 255, 0.1);
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

@media (max-width: 900px) {
    .content-layout.has-side {
        grid-template-columns: 1fr;
    }

    .side-videos {
        position: static;
        height: auto;
    }

    .side-video-list {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        overflow: visible;
    }
}

@media (max-width: 560px) {
    .video-article-page {
        padding: 96px 20px 40px;
    }

    .article-header {
        grid-template-columns: 1fr;
        align-items: stretch;
    }

    .back-to-list-button {
        justify-self: stretch;
        width: 100%;
    }

    .header-spacer {
        display: none;
    }

    .side-video-list,
    .recommend-list,
    .spec-grid {
        grid-template-columns: 1fr;
    }
}
</style>
