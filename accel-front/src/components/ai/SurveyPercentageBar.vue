<template>
    <div class="survey-percentage" role="meter" :aria-valuenow="surveyProgress" aria-valuemin="0" aria-valuemax="100">
        <div class="survey-percentage__meta">
            <span>{{ answeredCount }} / {{ questions.length }}</span>
            <strong>{{ surveyProgress }}%</strong>
        </div>
        <div class="survey-percentage__track">
            <div class="survey-percentage__fill" :style="{ width: `${surveyProgress}%` }"></div>
        </div>
    </div>
</template>

<script setup>
import { useAiStore } from '@/stores/aiStore'
import { storeToRefs } from 'pinia'

const store = useAiStore()
const { answeredCount, questions, surveyProgress } = storeToRefs(store)
</script>

<style scoped>
.survey-percentage {
    width: 100%;
    margin: 0;
}

.survey-percentage__meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    color: var(--neon-text);
    font-size: 14px;
    font-weight: 800;
}

.survey-percentage__meta strong {
    color: var(--neon-cyan);
    font-size: 16px;
}

.survey-percentage__track {
    width: 100%;
    height: 6px;
    border-radius: 999px;
    background: rgba(4, 217, 255, 0.14);
    box-shadow: var(--glow-blue);
    overflow: hidden;
}

.survey-percentage__fill {
    height: 100%;
    border-radius: inherit;
    background: linear-gradient(90deg, rgba(35, 35, 255, 0.9), rgba(4, 217, 255, 0.9));
    transition: width 0.25s ease;
}
</style>
