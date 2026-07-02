<template>
    <div class="board-image-carousel">
        <img :src="activeImage" :alt="alt">

        <template v-if="displayImages.length > 1">
            <button
                type="button"
                class="carousel-side carousel-side-prev"
                aria-label="이전 이미지"
                @click="goToImage(activeIndex - 1)"
            >
                <span aria-hidden="true">&lt;</span>
            </button>

            <button
                type="button"
                class="carousel-side carousel-side-next"
                aria-label="다음 이미지"
                @click="goToImage(activeIndex + 1)"
            >
                <span aria-hidden="true">&gt;</span>
            </button>

            <div class="carousel-indicator" aria-live="polite">
                {{ safeActiveIndex + 1 }} / {{ displayImages.length }}
            </div>
        </template>
    </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
    images: {
        type: Array,
        default: () => [],
    },
    activeIndex: {
        type: Number,
        default: 0,
    },
    fallbackImage: {
        type: String,
        required: true,
    },
    alt: {
        type: String,
        default: '게시글 이미지',
    },
})

const emit = defineEmits(['update:activeIndex'])

const displayImages = computed(() => {
    const images = props.images.filter(Boolean)
    return images.length > 0 ? images : [props.fallbackImage]
})

const safeActiveIndex = computed(() => {
    const imageCount = displayImages.value.length
    return ((props.activeIndex % imageCount) + imageCount) % imageCount
})

const activeImage = computed(() => {
    return displayImages.value[safeActiveIndex.value] ?? props.fallbackImage
})

const goToImage = (index) => {
    const imageCount = displayImages.value.length
    emit('update:activeIndex', ((index % imageCount) + imageCount) % imageCount)
}
</script>

<style scoped>
.board-image-carousel {
    position: relative;
    width: 100%;
    height: 100%;
    overflow: hidden;
    background: #000;
}

.board-image-carousel img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.carousel-indicator {
    position: absolute;
    left: 50%;
    bottom: 12px;
    min-width: 54px;
    padding: 6px 12px;
    transform: translateX(-50%);
    color: #ffffff;
    background: rgba(88, 92, 102, 0.86);
    border: 1px solid rgba(255, 255, 255, 0.18);
    border-radius: 999px;
    font-size: 13px;
    font-weight: 800;
    line-height: 1;
    text-align: center;
    box-shadow: 0 8px 18px rgba(0, 0, 0, 0.24);
    pointer-events: none;
}

.carousel-side {
    position: absolute;
    top: 0;
    bottom: 0;
    width: 34%;
    padding: 0;
    color: #ffffff;
    background: transparent;
    border: 0;
    cursor: pointer;
    opacity: 0;
    transition: opacity 0.18s ease, background 0.18s ease;
    box-shadow: none;
}

.carousel-side-prev {
    left: 0;
    background: linear-gradient(90deg, rgba(0, 0, 0, 0.34), rgba(0, 0, 0, 0));
}

.carousel-side-next {
    right: 0;
    background: linear-gradient(270deg, rgba(0, 0, 0, 0.34), rgba(0, 0, 0, 0));
}

.carousel-side span {
    position: absolute;
    top: 50%;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 42px;
    height: 42px;
    transform: translateY(-50%);
    color: rgba(255, 255, 255, 0.9);
    background: rgba(0, 0, 0, 0.34);
    border: 1px solid rgba(255, 255, 255, 0.18);
    border-radius: 50%;
    font-size: 30px;
    font-weight: 700;
    line-height: 1;
}

.carousel-side-prev span {
    left: 18px;
}

.carousel-side-next span {
    right: 18px;
}

.carousel-side:hover,
.carousel-side:focus-visible {
    opacity: 1;
    outline: none;
}

@media (max-width: 640px) {
    .carousel-side {
        width: 42%;
    }

    .carousel-side span {
        width: 36px;
        height: 36px;
        font-size: 26px;
    }
}
</style>
