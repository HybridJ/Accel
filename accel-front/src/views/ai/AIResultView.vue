<template>
    <section class="ai-result-page">
        <div class="result-shell">
            <p class="result-label">{{ resultCopy.label }}</p>
            <h1>{{ resultTitle }}</h1>

            <div v-if="resultImage" class="result-car-image">
                <img :src="resultImage" :alt="`${aiResult.model} 결과 이미지`">
                <div class="image-glow"></div>
            </div>

            <p v-if="aiResult.summary" class="result-summary">
                {{ aiResult.summary }}
            </p>

            <div class="result-description">
                <h2>{{ descriptionTitle }}</h2>
                <p v-for="paragraph in resultParagraphs" :key="paragraph">
                    {{ paragraph }}
                </p>
            </div>

            <div class="car-review-redirection">
                <template v-if="isRecommendationIncomplete">
                    <RouterLink class="result-action" :to="retrySurveyRoute">
                        다시 설문하기
                    </RouterLink>
                    <RouterLink class="result-action ghost" :to="{ name: 'home' }">
                        메인 페이지로 돌아가기
                    </RouterLink>
                </template>
                <template v-else>
                    <RouterLink
                        v-if="aiResult.vehicleId"
                        class="result-action"
                        :to="{ name: 'videoArticle', params: { vehicleId: aiResult.vehicleId } }"
                    >
                        {{ aiResult.model }} {{ resultCopy.videoAction }}
                    </RouterLink>
                    <RouterLink
                        v-if="aiResult.brandId"
                        class="result-action ghost"
                        :to="{ name: 'BrandBoard', params: { brandId: aiResult.brandId } }"
                    >
                        {{ resultCopy.communityAction }}
                    </RouterLink>
                </template>
            </div>
        </div>
    </section>
</template>

<script setup>
import { useAiStore } from '@/stores/aiStore';
import { computed } from 'vue';
import { RouterLink, useRoute } from 'vue-router';

const store = useAiStore()
const route = useRoute()
const resultImages = import.meta.glob('@/assets/carbti-img/*.png', {
    eager: true,
    import: 'default',
})
const resultImageByVehicleId = Object.fromEntries(
    Object.entries(resultImages).map(([path, image]) => {
        const fileName = path.split('/').pop()
        const vehicleId = fileName?.replace('.png', '')

        return [vehicleId, image]
    })
)

const isCarbti = computed(() => route.params.agentType === 'carbti')

const resultCopy = computed(() => {
    if (isCarbti.value) {
        return {
            label: 'CarBTI Result',
            reasonTitle: '나와 닮은 포인트',
            videoAction: '자동차 극장으로 더 보기',
            communityAction: '내 CarBTI 타랑께로 이동',
        }
    }

    return {
        label: route.params.agentType,
        reasonTitle: '추천 이유',
        videoAction: '자동차 극장 보러가기',
        communityAction: '해당 차량 타랑께로 이동',
    }
})

const fallbackResults = {
    carbti: {
        brandId: null,
        vehicleId: null,
        model: 'ACCEL CarBTI',
        summary: '',
        resaon: '아직 CarBTI 결과가 없습니다. 설문을 완료하면 나와 닮은 차량과 성향 포인트가 표시됩니다.',
    },
    default: {
        brandId: null,
        vehicleId: null,
        model: 'ACCEL Recommended Car',
        summary: '',
        resaon: '아직 AI 추천 결과가 없습니다. 설문을 완료하면 추천 차량과 추천 이유가 표시됩니다.',
    },
}

const fallbackResult = computed(() => isCarbti.value ? fallbackResults.carbti : fallbackResults.default)

const aiResult = computed(() => {
    const response = store.aiResponse;
    const fallback = fallbackResult.value;

    if (!response) {
        return fallback;
    }

    if (typeof response === 'string') {
        try {
            return JSON.parse(response);
        } catch {
            return {
                ...fallback,
                resaon: response,
            };
        }
    }

    return {
        brandId: response.brandId,
        vehicleId: response.vehicleId,
        model: response.model || fallback.model,
        summary: response.summary || '',
        resaon: response.resaon || response.reason || response.description || response.answer || fallback.resaon,
    };
})

const resultTitle = computed(() => {
    return isCarbti.value ? `${aiResult.value.model}` : aiResult.value.model
})

const resultImage = computed(() => {
    const vehicleId = aiResult.value.vehicleId

    if (!vehicleId) {
        return ''
    }

    return resultImageByVehicleId[String(vehicleId)] ?? ''
})

const isRecommendationIncomplete = computed(() => {
    return !Number(aiResult.value.vehicleId) && !Number(aiResult.value.brandId)
})

const descriptionTitle = computed(() => {
    return isRecommendationIncomplete.value && !isCarbti.value
        ? '추천을 완료하지 못한 이유'
        : resultCopy.value.reasonTitle
})

const retrySurveyRoute = computed(() => {
    return {
        name: 'AiSurveyByType',
        params: { agentType: route.params.agentType },
    }
})

const normalizeReasonText = (text) => {
    return String(text ?? '')
        .replace(/DB\s*조회/gi, '차량 정보 확인')
        .replace(/\bDB\b/gi, '서비스 차량 정보')
}

const resultParagraphs = computed(() => {
    return normalizeReasonText(aiResult.value.resaon)
        .split(/\n+/)
        .map((paragraph) => paragraph.trim())
        .filter(Boolean)
})
</script>

<style scoped>
.ai-result-page {
    min-height: 100vh;
    padding: 128px 24px 72px;
    color: var(--neon-text);
}

.result-shell {
    width: min(100%, 1040px);
    margin: 0 auto;
    text-align: center;
}

.result-label {
    margin: 0 0 10px;
    color: var(--neon-cyan);
    font-size: 14px;
    font-weight: 700;
    letter-spacing: 0;
    text-transform: uppercase;
}

.result-shell h1 {
    margin: 0 0 30px;
    font-size: clamp(34px, 5vw, 64px);
    line-height: 1.05;
    color: #ffffff;
    text-shadow: 0 0 18px rgba(4, 217, 255, 0.48), 0 0 42px rgba(35, 35, 255, 0.36);
}

.result-car-image {
    position: relative;
    width: min(100%, 640px);
    margin: 0 auto 46px;
    height: clamp(420px, 62vw, 660px);
    display: flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    padding: 0;
}

.result-car-image img {
    position: relative;
    z-index: 2;
    max-width: 100%;
    max-height: 100%;
    width: auto;
    height: auto;
    object-fit: contain;
    filter: drop-shadow(0 0 24px rgba(4, 217, 255, 0.52));
}

.image-glow {
    position: absolute;
    inset: auto 8% 3%;
    z-index: 1;
    height: 22%;
    background: radial-gradient(ellipse, rgba(4, 217, 255, 0.24), transparent 70%);
    filter: blur(18px);
}

.result-description {
    width: min(100%, 760px);
    margin: 0 auto;
    padding: 34px 36px 36px;
    text-align: left;
    background: linear-gradient(180deg, rgba(4, 17, 36, 0.78), rgba(0, 0, 0, 0.78));
    border: 1px solid rgba(4, 217, 255, 0.26);
    border-radius: 8px;
    box-shadow: 0 0 22px rgba(4, 217, 255, 0.12);
}

.result-summary {
    position: relative;
    width: min(100%, 760px);
    box-sizing: border-box;
    margin: -20px auto 42px;
    padding: 0 34px;
    color: #ffffff;
    font-size: clamp(20px, 2.5vw, 28px);
    font-weight: 700;
    line-height: 1.55;
    text-align: center;
    text-shadow: 0 0 18px rgba(4, 217, 255, 0.32);
}

.result-summary::before,
.result-summary::after {
    position: absolute;
    color: var(--neon-cyan);
    font-size: 48px;
    line-height: 1;
    opacity: 0.72;
}

.result-summary::before {
    content: "“";
    top: -12px;
    left: 0;
}

.result-summary::after {
    content: "”";
    right: 0;
    bottom: -28px;
}

.result-description h2 {
    margin: 0 0 20px;
    color: var(--neon-cyan);
    font-size: 22px;
    line-height: 1.35;
}

.result-description p {
    margin: 0;
    color: var(--neon-text);
    font-size: 17px;
    line-height: 1.9;
}

.result-description p + p {
    margin-top: 18px;
}

.car-review-redirection {
    display: flex;
    justify-content: center;
    gap: 14px;
    flex-wrap: wrap;
    margin-top: 42px;
}

.result-action {
    min-width: 220px;
    padding: 13px 18px;
    color: var(--neon-text);
    background: linear-gradient(135deg, rgba(35, 35, 255, 0.9), rgba(4, 217, 255, 0.22));
    border: 1px solid var(--line-blue);
    border-radius: 6px;
    box-shadow: var(--glow-blue);
    text-align: center;
    text-decoration: none;
    font-weight: 700;
}

.result-action.ghost {
    background: rgba(4, 217, 255, 0.08);
}

@media (max-width: 640px) {
    .ai-result-page {
        padding: 108px 16px 52px;
    }

    .result-shell h1 {
        font-size: 34px;
    }

    .result-car-image {
        width: min(100%, 440px);
        height: min(64vh, 520px);
        min-height: 340px;
    }

    .result-description {
        padding: 26px 20px 28px;
    }

    .result-summary {
        margin: -12px auto 34px;
        padding: 0 26px;
        font-size: 20px;
    }

    .result-description h2 {
        margin-bottom: 16px;
    }

    .result-description p {
        font-size: 16px;
        line-height: 1.8;
    }

    .result-action {
        width: 100%;
    }
}
</style>
