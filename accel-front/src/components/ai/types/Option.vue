<!-- 단일 객관식 -->
<template>
    <div v-for="option in question.options" :key="option.id">
        <button
            type="button"
            :class="{ selected: modelValue?.id === option.id }"
            @click="$emit('select', option)"
        >
            {{ option.id }}: {{ option.answer }}
        </button>
    </div>
</template>

<script setup>
defineProps({
    question: {
        type: Object,
        required: true,
    },
    modelValue: {
        type: Object,
        default: null,
    },
})

defineEmits(['select'])
</script>

<style scoped>
div {
    margin: 10px 0;
}

button {
    width: 100%;
    padding: 14px 16px;
    text-align: left;
    cursor: pointer;
    background: rgba(4, 217, 255, 0.08);
    border: 1px solid var(--line-blue);
    box-shadow: none;
}

button:hover {
    border-color: var(--neon-cyan);
    box-shadow: 0 0 18px rgba(4, 217, 255, 0.2);
}

.selected {
    font-weight: 700;
    background: linear-gradient(135deg, rgba(35, 35, 255, 0.72), rgba(4, 217, 255, 0.22));
    border-color: var(--neon-cyan);
    box-shadow: var(--glow-blue);
}
</style>
