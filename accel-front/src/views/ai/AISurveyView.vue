<template>
    <div class="ai-survey-page">
        <section class="survey-shell" aria-label="AI survey">
            <div class="survey-title"><h1>{{ route.params.agentType.toUpperCase() }}</h1></div>
            <SurveyPercentageBar />
            <p v-if="validationMessage" class="validation-message">{{ validationMessage }}</p>
            <SurveyBox
                v-if="currentQuestion"
                :question="currentQuestion"
                :question-index="currentIndex"
                :is-first="currentIndex === 0"
                :is-last="currentIndex === questions.length - 1"
                :is-submitting="isSubmitting"
                @prev="goPrev"
                @next="goNext"
            />
        </section>
    </div>
</template>

<script setup>
import SurveyBox from '@/components/ai/SurveyBox.vue'
import SurveyPercentageBar from '@/components/ai/SurveyPercentageBar.vue';
import { useAiStore } from '@/stores/aiStore';
import { storeToRefs } from 'pinia';
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute()
const router = useRouter()
const store = useAiStore()
const { questions } = storeToRefs(store)

const currentIndex = ref(0)
const isSubmitting = ref(false)
const validationMessage = ref('')
const currentQuestion = computed(() => questions.value[currentIndex.value])

watch(
    () => route.params.agentType,
    (agentType) => {
        if (agentType === 'carbti' || agentType === 'carsulting') {
            store.agent = agentType
            store.resetResult()
            currentIndex.value = 0
            validationMessage.value = ''
        }
    },
    { immediate: true }
)

const goPrev = () => {
    if (currentIndex.value > 0) {
        currentIndex.value--
        validationMessage.value = ''
    }
}

const goNext = async () => {
    if (currentIndex.value < questions.value.length - 1) {
        currentIndex.value++
        validationMessage.value = ''
        return
    }

    if (isSubmitting.value) {
        return
    }

    const missingQuestionNumbers = store.getMissingQuestionNumbers()

    if (missingQuestionNumbers.length > 0) {
        validationMessage.value = `${missingQuestionNumbers.join(', ')}번 문항에 답변해주세요.`
        const firstMissingIndex = questions.value.findIndex((question) => question.id === missingQuestionNumbers[0])

        if (firstMissingIndex !== -1) {
            currentIndex.value = firstMissingIndex
        }

        return
    }

    try {
        isSubmitting.value = true
        await router.push({
            name: 'AiLoading',
            params: { agentType: store.agent },
        })
    } finally {
        isSubmitting.value = false
    }
}
</script>

<style scoped>
.ai-survey-page {
    display: grid;
    place-items: center;
    width: 100%;
    min-height: 100vh;
    min-height: 100dvh;
    box-sizing: border-box;
    padding: 84px 20px 24px;
}

.survey-shell {
    display: grid;
    gap: 18px;
    width: min(100%, 860px);
}

.survey-title h1 {
    margin: 0;
    color: var(--neon-text);
    text-align: center;
    text-shadow: 0 0 18px rgba(4, 217, 255, 0.4);
}

.validation-message {
    width: 100%;
    box-sizing: border-box;
    margin: 0;
    padding: 12px 16px;
    color: #04d9ff;
    background: rgba(4, 217, 255, 0.1);
    border: 1px solid var(--line-blue);
    border-radius: 6px;
    box-shadow: 0 0 20px rgba(4, 217, 255, 0.16);
}

.validation-message {
    font-weight: 700;
}

@media (max-width: 640px) {
    .ai-survey-page {
        place-items: start center;
        padding: 84px 14px 28px;
    }

    .survey-shell {
        gap: 14px;
    }
}
</style>
