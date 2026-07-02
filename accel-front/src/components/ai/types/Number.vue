<template>
    <div>
        <input
            type="number"
            :value="modelValue"
            @input="$emit('select', $event.target.value === '' ? null : Number($event.target.value))"
        >
        <span> 만원</span>
    </div>
</template>

<script setup>
defineProps({
    question: {
        type: Object,
        required: true,
    },
    modelValue: {
        type: [Number, null],
        default: null,
    },
})

defineEmits(['select'])
</script>

<style scoped>
input {
    width: min(100%, 320px);
    padding: 12px 14px;
}

</style>
