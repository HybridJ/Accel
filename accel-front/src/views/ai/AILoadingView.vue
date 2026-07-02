<template>
    <main class="ai-loading-page">
        <section class="loading-shell" aria-live="polite">
            <div class="loading-visual" aria-hidden="true">
                <div class="loading-image-orb">
                    <img :src="loadingImage" :alt="`${agentCopy.title} 이미지`">
                </div>
                <div class="scan-ring"></div>
                <div class="scan-line"></div>
            </div>

            <div class="loading-copy">
                <p class="loading-kicker">{{ agentCopy.kicker }}</p>
                <h1>{{ agentCopy.title }}</h1>
                <p>{{ currentStep }}</p>
            </div>

            <div class="loading-progress" role="progressbar" aria-label="AI 분석 진행 중">
                <span></span>
            </div>

            <div class="loading-actions" v-if="errorMessage">
                <p>{{ errorMessage }}</p>
                <button type="button" @click="runAnalysis">다시 시도</button>
                <RouterLink :to="{ name: 'AiSurveyByType', params: { agentType } }">
                    설문으로 돌아가기
                </RouterLink>
            </div>
        </section>
    </main>
</template>

<script setup>
import carbtiImage from '@/assets/service-img/carbti.png'
import carsultingImage from '@/assets/service-img/carsulting.png'
import { useAiStore } from '@/stores/aiStore'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const store = useAiStore()
const currentStepIndex = ref(0)
const errorMessage = ref('')
let stepTimer = null

const agentType = computed(() => String(route.params.agentType || ''))
const isCarbti = computed(() => agentType.value === 'carbti')
const loadingImage = computed(() => isCarbti.value ? carbtiImage : carsultingImage)

const agentCopy = computed(() => {
    if (isCarbti.value) {
        return {
            kicker: 'CarBTI 분석 중',
            title: '성향에 맞는 차량을 찾고 있습니다',
            steps: [
                '답변에서 운전 성향을 읽고 있습니다.',
                '라이프스타일과 차량 특징을 맞춰보고 있습니다.',
                '가장 어울리는 결과를 정리하고 있습니다.',
            ],
        }
    }

    return {
        kicker: 'Carsulting 분석 중',
        title: '조건에 맞는 차량을 추천하고 있습니다',
        steps: [
            '예산과 용도 조건을 확인하고 있습니다.',
            '후보 차량을 비교하고 있습니다.',
            '추천 이유를 정리하고 있습니다.',
        ],
    }
})

const currentStep = computed(() => {
    return agentCopy.value.steps[currentStepIndex.value % agentCopy.value.steps.length]
})

const hasCompleteSurvey = () => {
    return store.questions.length > 0 && store.getMissingQuestionNumbers().length === 0
}

const startStepRotation = () => {
    stepTimer = window.setInterval(() => {
        currentStepIndex.value += 1
    }, 1400)
}

const runAnalysis = async () => {
    errorMessage.value = ''
    currentStepIndex.value = 0

    if (agentType.value !== 'carbti' && agentType.value !== 'carsulting') {
        router.replace({ name: 'home' })
        return
    }

    if (store.agent !== agentType.value) {
        store.agent = agentType.value
    }

    if (!hasCompleteSurvey()) {
        router.replace({ name: 'AiSurveyByType', params: { agentType: agentType.value } })
        return
    }

    try {
        await Promise.all([
            store.fetchAiService(),
            new Promise((resolve) => window.setTimeout(resolve, 1200)),
        ])
        router.replace({
            name: 'AiResult',
            params: { agentType: agentType.value },
        })
    } catch (error) {
        console.error('AI 분석에 실패했습니다:', error)
        errorMessage.value = '분석을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요.'
    }
}

onMounted(() => {
    startStepRotation()
    runAnalysis()
})

onBeforeUnmount(() => {
    if (stepTimer) {
        window.clearInterval(stepTimer)
    }
})
</script>

<style scoped>
.ai-loading-page {
    display: grid;
    place-items: center;
    min-height: 100vh;
    min-height: 100dvh;
    box-sizing: border-box;
    padding: 96px 20px 40px;
    color: var(--neon-text);
}

.loading-shell {
    display: grid;
    justify-items: center;
    gap: 24px;
    width: min(100%, 720px);
    text-align: center;
}

.loading-visual {
    position: relative;
    display: grid;
    place-items: center;
    width: min(100%, 380px);
    aspect-ratio: 1;
}

.loading-image-orb {
    position: relative;
    z-index: 2;
    display: grid;
    place-items: center;
    width: min(68%, 258px);
    aspect-ratio: 1;
    overflow: hidden;
    background:
        radial-gradient(circle at 50% 42%, rgba(255, 255, 255, 0.16), transparent 34%),
        linear-gradient(145deg, rgba(4, 217, 255, 0.28), rgba(35, 35, 255, 0.12) 52%, rgba(0, 0, 0, 0.72));
    border: 1px solid rgba(140, 207, 255, 0.38);
    border-radius: 50%;
    box-shadow:
        0 0 28px rgba(4, 217, 255, 0.28),
        inset 0 0 32px rgba(4, 217, 255, 0.12);
}

.loading-image-orb::after {
    content: '';
    position: absolute;
    inset: 8%;
    border: 1px solid rgba(255, 255, 255, 0.12);
    border-radius: inherit;
    pointer-events: none;
}

.loading-image-orb img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transform: scale(1.08);
    filter: saturate(1.08) contrast(1.04);
}

.scan-ring {
    position: absolute;
    inset: 8%;
    border: 1px solid rgba(4, 217, 255, 0.32);
    border-radius: 50%;
    box-shadow: inset 0 0 28px rgba(4, 217, 255, 0.08), 0 0 28px rgba(4, 217, 255, 0.14);
    animation: pulse-ring 1.9s ease-in-out infinite;
}

.scan-ring::before,
.scan-ring::after {
    content: '';
    position: absolute;
    inset: 13%;
    border: 1px solid rgba(140, 207, 255, 0.22);
    border-radius: 50%;
}

.scan-ring::after {
    inset: 28%;
}

.scan-line {
    position: absolute;
    inset: 11% auto 11% 50%;
    width: 2px;
    transform-origin: 50% 50%;
    background: linear-gradient(180deg, transparent, rgba(4, 217, 255, 0.95), transparent);
    box-shadow: 0 0 18px rgba(4, 217, 255, 0.8);
    animation: rotate-scan 1.65s linear infinite;
}

.loading-copy {
    display: grid;
    gap: 10px;
}

.loading-kicker {
    margin: 0;
    color: var(--neon-cyan);
    font-size: 14px;
    font-weight: 900;
}

.loading-copy h1 {
    margin: 0;
    color: #ffffff;
    font-size: clamp(28px, 5vw, 48px);
    line-height: 1.18;
    text-shadow: 0 0 18px rgba(4, 217, 255, 0.44);
}

.loading-copy p:last-child {
    margin: 0;
    color: var(--muted-blue);
    font-size: 16px;
    font-weight: 700;
}

.loading-progress {
    width: min(100%, 460px);
    height: 7px;
    overflow: hidden;
    border-radius: 999px;
    background: rgba(4, 217, 255, 0.12);
    box-shadow: var(--glow-blue);
}

.loading-progress span {
    display: block;
    width: 42%;
    height: 100%;
    border-radius: inherit;
    background: linear-gradient(90deg, rgba(35, 35, 255, 0.9), rgba(4, 217, 255, 0.95), rgba(110, 255, 226, 0.92));
    animation: progress-sweep 1.45s ease-in-out infinite;
}

.loading-actions {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-wrap: wrap;
    gap: 12px;
    width: 100%;
}

.loading-actions p {
    flex: 1 0 100%;
    margin: 0 0 4px;
    color: #ff9bb0;
    font-weight: 800;
}

.loading-actions button,
.loading-actions a {
    min-width: 140px;
    min-height: 42px;
    box-sizing: border-box;
    padding: 10px 16px;
    color: #ffffff;
    background: rgba(4, 217, 255, 0.1);
    border: 1px solid rgba(140, 207, 255, 0.3);
    border-radius: 8px;
    font-weight: 800;
    text-decoration: none;
    cursor: pointer;
}

@keyframes rotate-scan {
    from {
        transform: translateX(-50%) rotate(0deg);
    }

    to {
        transform: translateX(-50%) rotate(360deg);
    }
}

@keyframes pulse-ring {
    0%,
    100% {
        transform: scale(0.98);
        opacity: 0.7;
    }

    50% {
        transform: scale(1.02);
        opacity: 1;
    }
}

@keyframes progress-sweep {
    0% {
        transform: translateX(-110%);
    }

    100% {
        transform: translateX(245%);
    }
}

@media (max-width: 640px) {
    .ai-loading-page {
        padding: 88px 16px 32px;
    }

    .loading-shell {
        gap: 20px;
    }

    .loading-visual {
        width: min(100%, 300px);
    }
}
</style>
