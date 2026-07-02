<template>
    <div>
        <button
            type="button"
            :class="{ selected: modelValue === true }"
            @click="$emit('select', true)"
        >
            예
        </button>
        <button
            type="button"
            :class="{ selected: modelValue === false }"
            @click="$emit('select', false)"
        >
            아니오
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
        type: [Boolean, null],
        default: null,
    },
})

defineEmits(['select'])
</script>

<style scoped>
div {
    display: flex;
    gap: 12px;
}

button {
    min-width: 120px;
    padding: 12px 18px;
    cursor: pointer;
}

.selected {
    font-weight: 700;
    border-color: var(--neon-cyan);
    box-shadow: var(--glow-blue);
}
</style>
