<template>
    <div class="home-page">
        <section
            class="hero-section"
            :style="{ '--hero-scroll-progress': heroScrollProgress }"
            aria-label="ACCEL main visual"
        >
            <video
                ref="heroVideo"
                src="../assets/heroSection_final.mp4"
                class="hero-video"
                autoplay
                muted
                playsinline
                preload="auto"
                @ended="holdHeroVideoLastFrame"
            ></video>
            <div class="hero-overlay">
                <h1>"당신의 이동을 꿈같은 경험으로"</h1>
                <span>ACCEL은 유저 데이터와 AI 기반 서비스를 통해 모빌리티 경험을 향상시키고, 소비자가 일상 속에서 더 나은 선택을 하도록 돕고자 합니다.</span>
            </div>
        </section>

        <section class="project-section reveal-section" :class="{ visible: visibleSections.has('project') }" data-reveal-id="project">
            <p class="section-kicker">How do we improve the mobility industry?</p>
            <h2>모빌리티 선택을 더 선명하게 만드는 서비스</h2>
            <p>
                ACCEL은 사용자의 취향과 이동 맥락을 바탕으로 차량 탐색, 추천, 타랑께, 전기차 인프라 
                정보를 자연스럽게 이어주는 모빌리티 플랫폼입니다.
            </p>
        </section>

        <section class="services-section" aria-label="ACCEL services">
            <div class="section-heading reveal-section" :class="{ visible: visibleSections.has('services-heading') }" data-reveal-id="services-heading">
                <p class="section-kicker">Available Services</p>
                <h2>이용 가능한 서비스</h2>
            </div>

            <div class="service-list">
                <div
                    v-for="service in services"
                    :key="service.id"
                    class="service-card reveal-section"
                    :class="{ visible: visibleSections.has(service.id) }"
                    :data-reveal-id="service.id"
                    role="link"
                    tabindex="0"
                    @click="moveToService(service.route)"
                    @keydown.enter.prevent="moveToService(service.route)"
                    @keydown.space.prevent="moveToService(service.route)"
                >
                    <div class="service-copy">
                        <h3>{{ service.title }}</h3>
                        <p>{{ service.description }}</p>
                    </div>

                    <div class="service-media">
                        <div class="service-image-box">
                            <img
                                :src="service.image"
                                :alt="`${service.title} 서비스 이미지`"
                                :class="{ 'poster-image': service.isPosterImage }"
                            >
                        </div>
                    </div>
                </div>
            </div>
        </section>
    </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import carbtiImg from '@/assets/service-img/carbti.png'
import carsultingImg from '@/assets/service-img/carsulting.png'
import videosImg from '@/assets/service-img/videos.png'
import boardsImg from '@/assets/service-img/boards.png'
import evImg from '@/assets/service-img/ev.png'

const router = useRouter()
const heroVideo = ref(null)
const heroScrollProgress = ref(0)
const visibleSections = ref(new Set())
let observer = null

const services = [
    {
        id: 'carbti',
        title: 'CARBTI',
        description: '짧은 설문으로 나와 닮은 차량 캐릭터와 추천 모델을 확인합니다.',
        image: carbtiImg,
        isPosterImage: true,
        route: { name: 'AiSurveyByType', params: { agentType: 'carbti' } },
    },
    {
        id: 'carsulting',
        title: 'CARSULTING',
        description: '예산, 용도, 취향을 바탕으로 구매 후보를 AI가 함께 좁혀줍니다.',
        image: carsultingImg,
        isPosterImage: true,
        route: { name: 'AiSurveyByType', params: { agentType: 'carsulting' } },
    },
    {
        id: 'ev',
        title: 'EV Infra',
        description: '선택한 위치 주변 가용 충전소와 우리 동네 전기차 스코어를 확인합니다.',
        image: evImg,
        route: { name: 'EVInfra' },
    },
    {
        id: 'video',
        title: '자동차 극장',
        description: '차량 카테고리별 영상 콘텐츠로 모델의 분위기와 매력을 탐색합니다.',
        image: videosImg,
        route: { name: 'videoCategory' },
    },
    {
        id: 'community',
        title: '타랑께',
        description: '타랑께에서 사용자 경험과 차량 이야기를 나눕니다.',
        image: boardsImg,
        route: { name: 'BrandCategory' },
    },
    
]

const moveToService = (route) => {
    router.push(route)
}

const holdHeroVideoLastFrame = () => {
    const video = heroVideo.value

    if (!video) {
        return
    }

    video.pause()

    if (Number.isFinite(video.duration) && video.duration > 0) {
        video.currentTime = Math.max(video.duration - 0.04, 0)
    }
}

const updateHeroScrollMotion = () => {
    const viewportHeight = window.innerHeight || 1
    const progress = Math.min(Math.max(window.scrollY / (viewportHeight * 0.45), 0), 1)
    heroScrollProgress.value = Number(progress.toFixed(3))
}

const handleHeroScroll = () => {
    requestAnimationFrame(updateHeroScrollMotion)
}

onMounted(() => {
    observer = new IntersectionObserver(
        (entries) => {
            entries.forEach((entry) => {
                const id = entry.target.dataset.revealId

                if (entry.isIntersecting) {
                    visibleSections.value = new Set([...visibleSections.value, id])
                    return
                }

                const nextVisibleSections = new Set(visibleSections.value)
                nextVisibleSections.delete(id)
                visibleSections.value = nextVisibleSections
            })
        },
        {
            threshold: 0.22,
            rootMargin: '0px 0px -80px',
        },
    )

    document.querySelectorAll('.reveal-section').forEach((element) => {
        observer.observe(element)
    })

    updateHeroScrollMotion()
    window.addEventListener('scroll', handleHeroScroll, { passive: true })
    window.addEventListener('resize', updateHeroScrollMotion)
})

onBeforeUnmount(() => {
    observer?.disconnect()
    window.removeEventListener('scroll', handleHeroScroll)
    window.removeEventListener('resize', updateHeroScrollMotion)
})
</script>

<style scoped>
.home-page {
    width: 100%;
    min-height: 100vh;
    padding-bottom: 96px;
    color: var(--neon-text);
    background: #000000;
    text-align: center;
}

.hero-section {
    position: relative;
    width: 100vw;
    height: 100vh;
    min-height: 640px;
    margin-left: calc(50% - 50vw);
    display: flex;
    justify-content: center;
    align-items: center;
    overflow: hidden;
}

.hero-video {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
    opacity: calc(1 - (var(--hero-scroll-progress) * 0.92));
    transform: translateY(calc(var(--hero-scroll-progress) * -34px)) scale(calc(1 + (var(--hero-scroll-progress) * 0.035)));
    transition: opacity 0.08s linear, transform 0.08s linear;
}

.hero-section::after {
    content: '';
    position: absolute;
    inset: 0;
    opacity: calc(1 - (var(--hero-scroll-progress) * 0.92));
    background:
        linear-gradient(180deg, rgba(0, 0, 0, 0.18), rgba(0, 0, 0, 0.82)),
        radial-gradient(circle at 50% 62%, rgba(4, 217, 255, 0.16), transparent 42%);
    transition: opacity 0.08s linear;
}

.hero-overlay {
    position: relative;
    z-index: 1;
    width: min(1100px, calc(100% - 32px));
    margin-top: 34vh;
    opacity: calc(1 - var(--hero-scroll-progress));
    transform: translateY(calc(var(--hero-scroll-progress) * -72px));
    text-shadow: 0 0 24px rgba(4, 217, 255, 0.4);
    transition: opacity 0.08s linear, transform 0.08s linear;
}

.hero-overlay p,
.section-kicker {
    margin: 0 0 12px;
    color: var(--neon-cyan);
    font-size: 25px;
    font-weight: 900;
    letter-spacing: 0;
}

.hero-overlay h1 {
    margin: 0;
    color: #ffffff;
    font-size: clamp(38px, 7vw, 86px);
    line-height: 1.08;
}

.hero-overlay span {
    display: block;
    max-width: 720px;
    margin: 20px auto 0;
    color: var(--neon-text);
    font-size: 18px;
    line-height: 1.7;
}

.project-section,
.services-section {
    width: min(1120px, calc(100% - 40px));
    margin: 0 auto;
}

.project-section {
    padding: 112px 0 150px;
}

.project-section h2,
.section-heading h2 {
    margin: 0;
    color: #ffffff;
    font-size: clamp(30px, 4vw, 54px);
    line-height: 1.22;
}

.project-section > p:last-child {
    max-width: 760px;
    margin: 22px auto 0;
    color: var(--muted-blue);
    font-size: 18px;
    line-height: 1.85;
}

.services-section {
    padding: 36px 0 40px;
}

.section-heading {
    margin-bottom: 42px;
}

.service-list {
    display: grid;
    gap: 42px;
}

.service-card {
    display: grid;
    grid-template-columns: minmax(0, 1.1fr) minmax(260px, 0.9fr);
    align-items: center;
    gap: 32px;
    min-height: 300px;
    padding: 28px;
    border: 1px solid rgba(4, 217, 255, 0.38);
    border-radius: 8px;
    background:
        linear-gradient(135deg, rgba(0, 35, 84, 0.64), rgba(0, 0, 0, 0.88)),
        rgba(0, 0, 0, 0.86);
    box-shadow: 0 0 24px rgba(4, 217, 255, 0.12);
    cursor: pointer;
    overflow: hidden;
    transition:
        border-color 0.2s ease,
        box-shadow 0.2s ease,
        transform 0.2s ease;
}

.service-card:nth-child(even) {
    grid-template-columns: minmax(260px, 0.9fr) minmax(0, 1.1fr);
}

.service-card:nth-child(even) .service-copy {
    order: 2;
}

.service-card:nth-child(even) .service-media {
    order: 1;
    justify-items: start;
    padding-left: 20px;
    padding-right: 0;
}

.service-card:hover,
.service-card:focus-visible {
    border-color: rgba(107, 241, 255, 0.9);
    box-shadow: 0 0 28px rgba(4, 217, 255, 0.26), 0 0 52px rgba(35, 35, 255, 0.18);
    transform: translateY(-3px);
    outline: none;
}

.service-copy {
    text-align: left;
}

.service-copy span {
    color: var(--neon-cyan);
    font-size: 13px;
    font-weight: 900;
}

.service-copy h3 {
    margin: 12px 0 14px;
    color: #ffffff;
    font-size: clamp(28px, 4vw, 48px);
    line-height: 1.1;
}

.service-copy p {
    margin: 0;
    color: var(--neon-text);
    font-size: 17px;
    line-height: 1.7;
}

.service-media {
    height: 236px;
    display: grid;
    align-items: center;
    justify-items: end;
    padding-right: 20px;
    border: 0;
    border-radius: 0;
    background: transparent;
    overflow: hidden;
}

.service-image-box {
    width: min(220px, 100%);
    height: 220px;
    display: grid;
    place-items: center;
    overflow: hidden;
}

.service-image-box img {
    box-sizing: border-box;
    width: auto;
    height: auto;
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
    object-position: center;
    padding: 0;
    border-radius: 8px;
}

.service-image-box img.poster-image {
    width: auto;
    height: auto;
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
    object-position: center;
    padding: 0;
}

.reveal-section {
    opacity: 0;
    transform: translateY(54px);
    transition:
        opacity 0.72s ease,
        transform 0.72s ease;
}

.reveal-section.visible {
    opacity: 1;
    transform: translateY(0);
}

@media (max-width: 768px) {
    .hero-section {
        height: 86vh;
        min-height: 560px;
    }

    .hero-overlay {
        margin-top: 24vh;
    }

    .hero-overlay span {
        font-size: 16px;
    }

    .project-section,
    .services-section {
        width: min(100% - 28px, 1120px);
    }

    .project-section {
        padding: 78px 0 112px;
    }

    .service-card,
    .service-card:nth-child(even) {
        grid-template-columns: 1fr;
        gap: 22px;
        min-height: 0;
        padding: 22px;
    }

    .service-card:nth-child(even) .service-copy,
    .service-card:nth-child(even) .service-media {
        order: initial;
    }

    .service-copy {
        text-align: center;
    }

    .service-media {
        height: 210px;
        justify-items: center;
        padding-right: 0;
        padding-left: 0;
    }

    .service-image-box {
        width: min(200px, 100%);
        height: 200px;
    }
}
</style>
