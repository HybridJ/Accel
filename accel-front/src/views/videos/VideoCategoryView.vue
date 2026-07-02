<template>
    <main class="video-category-page">
        <header class="category-hero">
            <p class="category-kicker">자동차 극장</p>
            <h1>차량 카테고리</h1>
        </header>

        <section class="category-section">
            <div class="category-grid">
                <RouterLink
                    class="category-card"
                    v-for="category in categoryCards"
                    :key="category.id"
                    :class="category.label"
                    :to="categoryRoute(category.label)"
                >
                    <div class="category-image-box">
                        <img
                            class="category-image"
                            :src="category.image"
                            :alt="`${category.name} 카테고리`"
                        >
                    </div>

                    <div class="category-card-body">
                        <h3>{{ category.name }}</h3>
                        <p>{{ category.description }}</p>
                    </div>
                </RouterLink>
            </div>
        </section>
    </main>
</template>

<script setup>
import { computed } from 'vue';
import { useVideoStore } from '@/stores/videoStore';

const store = useVideoStore()

const categoryDetails = {
    sedan: '정숙한 주행과 안정적인 실루엣',
    suv: '넓은 공간과 다양한 라이프스타일',
    sports: '날카로운 성능과 드라이빙 감각',
    micro: '도심 이동에 가벼운 선택지',
    hatchback: '실용성과 경쾌함의 균형',
    truck: '적재와 작업성을 우선한 라인업',
}

const categoryCards = computed(() => {
    return store.categories.map((category) => ({
        ...category,
        description: categoryDetails[category.label] ?? '다양한 자동차 극장 콘텐츠',
    }))
})

const categoryRoute = (category) => {
    return {
        name: 'videoShowroom',
        params: { category },
    }
}
</script>

<style scoped>
.video-category-page {
    width: min(100%, 1180px);
    margin: 0 auto;
    padding: 116px 24px 64px;
    box-sizing: border-box;
}

.category-hero {
    margin-bottom: 42px;
    padding-bottom: 22px;
    border-bottom: 1px solid rgba(140, 207, 255, 0.18);
}

.category-kicker {
    margin: 0 0 8px;
    color: var(--muted-blue);
    font-size: 14px;
    font-weight: 700;
}

.category-hero h1 {
    margin: 0;
    color: #ffffff;
    font-size: 40px;
    line-height: 1.18;
    font-weight: 800;
}

.category-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 22px;
}

.category-card {
    --card-accent: var(--neon-cyan);
    position: relative;
    overflow: hidden;
    min-height: 276px;
    display: flex;
    flex-direction: column;
    color: var(--neon-text);
    text-decoration: none;
    background: linear-gradient(180deg, rgba(8, 22, 42, 0.94), rgba(2, 7, 18, 0.94));
    border: 1px solid rgba(140, 207, 255, 0.18);
    border-radius: 8px;
    cursor: pointer;
    transition:
        transform 0.18s ease,
        border-color 0.18s ease,
        box-shadow 0.18s ease,
        background 0.18s ease;
}

.category-card.sedan {
    --card-accent: #5cffb0;
}

.category-card.suv {
    --card-accent: var(--neon-cyan);
}

.category-card.sports {
    --card-accent: #ff6b8a;
}

.category-card.micro {
    --card-accent: #ffd166;
}

.category-card.hatchback {
    --card-accent: #a78bfa;
}

.category-card.truck {
    --card-accent: #f59e0b;
}

.category-card::before {
    content: '';
    position: absolute;
    top: 0;
    right: 0;
    left: 0;
    height: 2px;
    background: linear-gradient(90deg, var(--card-accent), transparent);
    opacity: 0.76;
}

.category-card:hover,
.category-card:focus-visible {
    border-color: color-mix(in srgb, var(--card-accent) 72%, rgba(255, 255, 255, 0.2));
    background: linear-gradient(180deg, rgba(10, 30, 56, 0.98), rgba(2, 9, 22, 0.98));
    box-shadow: 0 14px 38px rgba(0, 0, 0, 0.34), 0 0 22px rgba(4, 217, 255, 0.16);
    outline: none;
    transform: translateY(-3px);
}

.category-image-box {
    position: relative;
    height: 150px;
    margin: 12px 12px 0;
    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;
    background:
        radial-gradient(circle at 50% 42%, color-mix(in srgb, var(--card-accent) 28%, transparent), transparent 62%),
        rgba(0, 0, 0, 0.42);
    border: 1px solid rgba(140, 207, 255, 0.18);
    border-radius: 6px;
}

.category-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
    transition: transform 0.18s ease;
}

.category-card:hover .category-image,
.category-card:focus-visible .category-image {
    transform: scale(1.03);
}

.category-card-body {
    display: flex;
    flex-direction: column;
    gap: 8px;
    padding: 16px;
}

.category-label {
    width: fit-content;
    padding: 5px 8px;
    color: var(--card-accent);
    background: color-mix(in srgb, var(--card-accent) 12%, transparent);
    border: 1px solid color-mix(in srgb, var(--card-accent) 28%, transparent);
    border-radius: 999px;
    font-size: 12px;
    font-weight: 800;
    line-height: 1;
    text-transform: uppercase;
}

.category-card h3 {
    margin: 0;
    color: #ffffff;
    font-size: 21px;
    line-height: 1.25;
    font-weight: 800;
}

.category-card p {
    margin: 0;
    color: var(--muted-blue);
    font-size: 14px;
    line-height: 1.5;
}

@media (max-width: 640px) {
    .video-category-page {
        padding: 96px 14px 44px;
    }

    .category-hero {
        margin-bottom: 34px;
    }

    .category-hero h1 {
        font-size: 30px;
    }

    .category-grid {
        grid-template-columns: 1fr;
        gap: 14px;
    }

    .category-card {
        min-height: 248px;
    }

    .category-image-box {
        height: 136px;
        margin: 10px 10px 0;
    }

    .category-card-body {
        padding: 14px;
    }
}
</style>
