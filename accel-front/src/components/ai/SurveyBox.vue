<template>
    <div>
        <div class="survey-box">
            <h1>{{ question.id }}. {{ question.question }}</h1>
            <h3 v-if="question.help">{{ question.help }}</h3>
            <div class="input-area">
                <component
                    :is="inputComponent"
                    v-if="inputComponent"
                    :question="question"
                    :model-value="result[questionIndex]?.answer"
                    @select="setAnswer"
                />
            </div>
            <div class="button-area">
                <button type="button" :disabled="isFirst || isSubmitting" @click="$emit('prev')">이전</button>
                <button type="button" :disabled="isSubmitting" @click="$emit('next')">
                    {{ nextButtonText }}
                </button>
            </div>
        </div>
    </div>
</template>

<script setup>
import { useAiStore } from '@/stores/aiStore';
import { storeToRefs } from 'pinia';
import { computed } from 'vue';
import Descriptive from './types/Descriptive.vue';
import MultipleOption from './types/MultipleOption.vue';
import NumberInput from './types/Number.vue';
import Option from './types/Option.vue';
import YesNo from './types/YesNo.vue';

const props = defineProps({
    question: {
        type: Object,
        required: true,
    },
    questionIndex: {
        type: Number,
        required: true,
    },
    isFirst: {
        type: Boolean,
        default: false,
    },
    isLast: {
        type: Boolean,
        default: false,
    },
    isSubmitting: {
        type: Boolean,
        default: false,
    },
})

defineEmits(['prev', 'next'])

const store = useAiStore()
const { result } = storeToRefs(store)

const inputComponentMap = {
    option: Option,
    multipleOption: MultipleOption,
    descriptive: Descriptive,
    number: NumberInput,
    yesNo: YesNo,
}

const inputComponent = computed(() => inputComponentMap[props.question.type])
const nextButtonText = computed(() => {
    if (props.isSubmitting) {
        return '제출 중'
    }

    return props.isLast ? '제출' : '다음'
})

const setAnswer = (answer) => {
    store.setResult(props.questionIndex, answer)
}
</script>

<style scoped>
.survey-box {
    width: 100%;
    min-width: 0;
    box-sizing: border-box;
    margin: 0 auto;
    padding: 32px;
    color: var(--neon-text);
    background: linear-gradient(180deg, rgba(4, 17, 36, 0.92), rgba(0, 0, 0, 0.88));
    border: 1px solid var(--line-blue);
    border-radius: 8px;
    box-shadow: var(--glow-blue);
}

.survey-box h1 {
    margin: 0 0 16px;
    color: var(--neon-text);
    font-size: clamp(22px, 4vw, 32px);
    line-height: 1.28;
    overflow-wrap: anywhere;
    text-shadow: 0 0 16px rgba(4, 217, 255, 0.38);
}

.survey-box h3 {
    margin: 0 0 24px;
    color: var(--muted-blue);
    font-weight: 500;
}

.input-area {
    margin: 24px 0;
    min-width: 0;
}

.button-area {
    display: flex;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
}

.button-area button {
    flex: 1 1 110px;
    min-width: 110px;
    padding: 12px 18px;
    cursor: pointer;
}

@media (max-width: 520px) {
    .survey-box {
        padding: 22px;
    }
}
</style>
