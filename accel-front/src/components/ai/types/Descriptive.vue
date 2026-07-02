<template>
    <div>
        <textarea
            :value="modelValue"
            @input="$emit('select', $event.target.value)"
        />
    </div>
</template>

<script setup>
defineProps({
    question: {
        type: Object,
        required: true,
    },
    modelValue: {
        type: String,
        default: '',
    },
})

defineEmits(['select'])
</script>

<style scoped>
textarea {
    width: 100%;
    min-height: 160px;
    padding: 14px 16px;
    box-sizing: border-box;
    resize: vertical;
}

</style>
