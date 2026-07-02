<template>
    <div>
        <label v-for="option in question.options" :key="option.id">
            <input
                type="checkbox"
                :checked="isSelected(option)"
                @change="toggleOption(option)"
            >
            {{ option.answer }}
        </label>
    </div>
</template>

<script setup>
const props = defineProps({
    question: {
        type: Object,
        required: true,
    },
    modelValue: {
        type: Array,
        default: () => [],
    },
})

const emit = defineEmits(['select'])

const isSelected = (option) => {
    return props.modelValue.some((answer) => answer.id === option.id)
}

const toggleOption = (option) => {
    if (isSelected(option)) {
        emit('select', props.modelValue.filter((answer) => answer.id !== option.id))
        return
    }

    emit('select', [...props.modelValue, option])
}
</script>

<style scoped>
div {
    display: grid;
    gap: 10px;
}

label {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 13px 14px;
    color: var(--neon-text);
    background: rgba(4, 217, 255, 0.08);
    border: 1px solid var(--line-blue);
    border-radius: 6px;
}

input {
    accent-color: var(--neon-cyan);
}

</style>
