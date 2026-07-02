<template>
    <main class="write-page">
        <header class="write-header">
            <RouterLink class="back-link" :to="{ name: 'BrandBoard', params: { brandId: `${route.params.brandId}` } }">
                <span aria-hidden="true">&lt;</span>
                목록으로 돌아가기
            </RouterLink>

            <div class="title-block">
                <p>타랑께</p>
                <h1>{{ brandName }}</h1>
            </div>
        </header>

        <section class="write-layout">
            <article class="live-preview" aria-label="Article preview">
                <div class="preview-image">
                    <BoardImageCarousel
                        v-model:active-index="activePreviewIndex"
                        :images="previewImages"
                        :fallback-image="videoThumbnail"
                        :alt="`${previewTitle} image preview`"
                    />
                </div>

                <div class="preview-body">
                    <div class="preview-topline">
                        <span>{{ brandName }}</span>
                        <span>{{ todayText }}</span>
                    </div>

                    <h2>{{ previewTitle }}</h2>

                    <div class="preview-meta">
                        <span>{{ form.userId || 'Author' }}</span>
                        <span>{{ form.testDriveAvailable ? 'Test drive available' : 'No test drive' }}</span>
                    </div>

                    <p>{{ previewContent }}</p>
                </div>
            </article>

            <form class="write-form" @submit.prevent="submitArticle">
                <label for="article-user">Author</label>
                <input
                    id="article-user"
                    v-model="form.userId"
                    type="text"
                    placeholder="Author ID"
                    :disabled="Boolean(currentUserId)"
                >

                <label for="article-title">Title</label>
                <input id="article-title" v-model="form.title" type="text" placeholder="Write a title">

                <label for="article-content">Content</label>
                <textarea id="article-content" v-model="form.content" rows="12" placeholder="Write your post"></textarea>

                <label for="article-image">이미지 업로드</label>
                <input
                    id="article-image"
                    ref="imageInput"
                    type="file"
                    accept="image/jpeg,image/png,image/gif,image/webp"
                    multiple
                    @change="selectImages"
                >
                <p class="helper-text">
                    이미지는 최대 10장까지 업로드할 수 있으며, JPG, PNG, GIF, WebP 파일만 가능합니다.
                </p>

                <label class="check-row">
                    <input v-model="form.testDriveAvailable" type="checkbox">
                    Test drive available
                </label>

                <ul class="selected-image-list" v-if="imageFileNames.length" aria-label="선택된 이미지 목록">
                    <li
                        v-for="(fileName, index) in imageFileNames"
                        :key="`${imagePreviewUrls[index]}-${fileName}`"
                        :class="{ 'is-dragging': draggedImageIndex === index }"
                        draggable="true"
                        @dragstart="startImageDrag(index, $event)"
                        @dragover.prevent="hoverImageDropTarget($event)"
                        @drop="dropImage(index, $event)"
                        @dragend="endImageDrag"
                    >
                        <span class="drag-handle" aria-hidden="true">::</span>
                        <img :src="imagePreviewUrls[index]" :alt="`${fileName} 미리보기`">
                        <span class="image-item-text">
                            <strong>{{ fileName }}</strong>
                            <small>추가 이미지</small>
                        </span>
                        <button type="button" aria-label="이미지 제거" @click="removeSelectedImage(index)">x</button>
                    </li>
                </ul>
                <p class="form-error" v-if="submitError">{{ submitError }}</p>

                <div class="form-actions">
                    <button type="button" class="secondary-button" @click="resetForm">Reset</button>
                    <button type="submit" :disabled="isSubmitting || !canSubmit">
                        {{ isSubmitting ? 'Submitting...' : 'Submit' }}
                    </button>
                </div>
            </form>
        </section>
    </main>
</template>

<script setup>
import videoThumbnail from '@/assets/video_thumbnail.png'
import BoardImageCarousel from '@/components/boards/BoardImageCarousel.vue'
import { useAuthStore } from '@/stores/authStore'
import { useBoardStore } from '@/stores/boardStore'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const boardStore = useBoardStore()
const authStore = useAuthStore()
const brands = ref([])
const imagePreviewUrls = ref([])
const imageFileNames = ref([])
const imageFiles = ref([])
const imageInput = ref(null)
const draggedImageIndex = ref(null)
const activePreviewIndex = ref(0)
const isSubmitting = ref(false)
const submitError = ref('')
const form = ref({
    userId: '',
    title: '',
    content: '',
    testDriveAvailable: false,
})

const tokenPayload = computed(() => {
    const token = authStore.accessToken

    if (!token) {
        return {}
    }

    try {
        const base64Url = token.split('.')[1]
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
        const json = decodeURIComponent(
            atob(base64)
                .split('')
                .map((char) => `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`)
                .join('')
        )

        return JSON.parse(json)
    } catch {
        return {}
    }
})

const currentUserId = computed(() => {
    return authStore.user?.userId
        ?? authStore.user?.id
        ?? tokenPayload.value.userId
        ?? tokenPayload.value.sub
        ?? ''
})

const selectedBrand = computed(() => {
    const brandId = Number(route.params.brandId)
    return brands.value.find((brand) => brand.brandId === brandId)
})

const brandName = computed(() => {
    const ko = String(selectedBrand.value?.boardNameKo ?? '').trim()
    const en = String(selectedBrand.value?.boardName ?? '').trim()

    if (ko && en && ko.toLowerCase() !== en.toLowerCase()) {
        return `${ko} / ${en}`
    }

    return ko || en || `Brand ${route.params.brandId}`
})

const boardName = computed(() => {
    return selectedBrand.value?.boardName ?? `Brand ${route.params.brandId}`
})

const todayText = computed(() => {
    return new Date().toISOString().slice(0, 10).replaceAll('-', '.')
})

const previewTitle = computed(() => {
    return form.value.title.trim() || 'Title preview'
})

const previewContent = computed(() => {
    return form.value.content.trim() || 'Your article preview updates here while you write.'
})

const previewImages = computed(() => {
    return imagePreviewUrls.value.length > 0 ? imagePreviewUrls.value : [videoThumbnail]
})

const canSubmit = computed(() => {
    return Boolean(
        form.value.userId.trim() &&
        form.value.title.trim() &&
        form.value.content.trim() &&
        Number(route.params.brandId)
    )
})

const allowedImageTypes = new Set([
    'image/jpeg',
    'image/png',
    'image/gif',
    'image/webp',
])
const maxImageCount = 10
const maxImageSize = 10 * 1024 * 1024
const maxTotalImageSize = 50 * 1024 * 1024

const revokeImagePreview = () => {
    imagePreviewUrls.value.forEach((imageUrl) => URL.revokeObjectURL(imageUrl))
}

const resetImageInput = () => {
    if (imageInput.value) {
        imageInput.value.value = ''
    }
}

const selectImages = (event) => {
    const files = Array.from(event.target.files ?? [])

    if (files.length === 0) {
        resetImageInput()
        return
    }

    const nextImageFiles = [...imageFiles.value, ...files]

    if (nextImageFiles.length > maxImageCount) {
        submitError.value = `이미지는 최대 ${maxImageCount}장까지 업로드할 수 있습니다.`
        resetImageInput()
        return
    }

    const unsupportedFiles = files.filter((file) => !allowedImageTypes.has(file.type))

    if (unsupportedFiles.length > 0) {
        submitError.value = 'JPG, PNG, GIF, WebP 이미지 파일만 업로드할 수 있습니다.'
        resetImageInput()
        return
    }

    const oversizedFile = files.find((file) => file.size > maxImageSize)

    if (oversizedFile) {
        submitError.value = '각 이미지 파일은 10MB 이하만 업로드할 수 있습니다.'
        resetImageInput()
        return
    }

    const totalSize = nextImageFiles.reduce((sum, file) => sum + file.size, 0)

    if (totalSize > maxTotalImageSize) {
        submitError.value = '선택한 이미지 전체 용량은 50MB 이하만 업로드할 수 있습니다.'
        resetImageInput()
        return
    }

    imagePreviewUrls.value = [
        ...imagePreviewUrls.value,
        ...files.map((file) => URL.createObjectURL(file)),
    ]
    imageFileNames.value = [
        ...imageFileNames.value,
        ...files.map((file) => file.name),
    ]
    imageFiles.value = nextImageFiles
    activePreviewIndex.value = imagePreviewUrls.value.length - files.length
    submitError.value = ''
    resetImageInput()
}

const clearSelectedImages = () => {
    revokeImagePreview()
    imagePreviewUrls.value = []
    imageFileNames.value = []
    imageFiles.value = []
    activePreviewIndex.value = 0
    draggedImageIndex.value = null
    resetImageInput()
}

const removeSelectedImage = (index) => {
    const previewUrl = imagePreviewUrls.value[index]

    if (previewUrl) {
        URL.revokeObjectURL(previewUrl)
    }

    imagePreviewUrls.value = imagePreviewUrls.value.filter((_, fileIndex) => fileIndex !== index)
    imageFileNames.value = imageFileNames.value.filter((_, fileIndex) => fileIndex !== index)
    imageFiles.value = imageFiles.value.filter((_, fileIndex) => fileIndex !== index)

    if (activePreviewIndex.value >= imagePreviewUrls.value.length) {
        activePreviewIndex.value = Math.max(imagePreviewUrls.value.length - 1, 0)
    }

    submitError.value = ''
    draggedImageIndex.value = null
    resetImageInput()
}

const moveArrayItem = (items, fromIndex, toIndex) => {
    const nextItems = [...items]
    const [movedItem] = nextItems.splice(fromIndex, 1)

    if (movedItem === undefined) {
        return nextItems
    }

    nextItems.splice(toIndex, 0, movedItem)
    return nextItems
}

const moveImageItem = (fromIndex, toIndex) => {
    if (fromIndex === toIndex || fromIndex < 0 || toIndex < 0) {
        return
    }

    imagePreviewUrls.value = moveArrayItem(imagePreviewUrls.value, fromIndex, toIndex)
    imageFileNames.value = moveArrayItem(imageFileNames.value, fromIndex, toIndex)
    imageFiles.value = moveArrayItem(imageFiles.value, fromIndex, toIndex)
    activePreviewIndex.value = toIndex
}

const startImageDrag = (index, event) => {
    draggedImageIndex.value = index
    event.dataTransfer?.setData('text/plain', String(index))

    if (event.dataTransfer) {
        event.dataTransfer.effectAllowed = 'move'
    }
}

const hoverImageDropTarget = (event) => {
    if (event.dataTransfer) {
        event.dataTransfer.dropEffect = 'move'
    }
}

const dropImage = (index, event) => {
    event.preventDefault()

    const fallbackDragIndex = Number(event.dataTransfer?.getData('text/plain'))
    const dragIndex = draggedImageIndex.value ?? fallbackDragIndex

    if (Number.isInteger(dragIndex) && dragIndex >= 0) {
        moveImageItem(dragIndex, index)
    }

    draggedImageIndex.value = null
}

const endImageDrag = () => {
    draggedImageIndex.value = null
}

const resetForm = () => {
    form.value = {
        userId: currentUserId.value,
        title: '',
        content: '',
        testDriveAvailable: false,
    }
    clearSelectedImages()
    submitError.value = ''

    resetImageInput()
}

const submitArticle = async () => {
    if (!canSubmit.value || isSubmitting.value) {
        return
    }

    isSubmitting.value = true
    submitError.value = ''

    try {
        await boardStore.createArticle({
            userId: form.value.userId.trim(),
            title: form.value.title.trim(),
            content: form.value.content.trim(),
            imageUrl: '',
            imageFiles: imageFiles.value,
            testDriveAvailable: Boolean(form.value.testDriveAvailable),
            brandId: Number(route.params.brandId),
            boardName: boardName.value,
        })

        router.push({ name: 'BrandBoard', params: { brandId: `${route.params.brandId}` } })
    } catch (error) {
        submitError.value = 'Failed to submit the article.'
        console.error(error)
    } finally {
        isSubmitting.value = false
    }
}

onMounted(async () => {
    brands.value = await boardStore.fetchBrands()
    resetForm()
})

onBeforeUnmount(revokeImagePreview)
</script>

<style scoped>
.write-page {
    max-width: 1120px;
    margin: 0 auto;
    padding: 112px 24px 56px;
    color: var(--neon-text);
}

.write-header {
    display: grid;
    grid-template-columns: minmax(150px, 1fr) auto minmax(150px, 1fr);
    align-items: center;
    gap: 18px;
    margin-bottom: 24px;
    padding-bottom: 18px;
    border-bottom: 1px solid rgba(140, 207, 255, 0.18);
}

.back-link {
    justify-self: start;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-height: 44px;
    padding: 0 14px;
    color: #ffffff;
    background: rgba(4, 217, 255, 0.08);
    border: 1px solid rgba(140, 207, 255, 0.24);
    border-radius: 8px;
    font-size: 14px;
    font-weight: 800;
    text-decoration: none;
}

.title-block {
    text-align: center;
}

.title-block p {
    margin: 0 0 6px;
    color: var(--muted-blue);
    font-size: 14px;
    font-weight: 700;
}

.title-block h1 {
    margin: 0;
    color: #ffffff;
    font-size: 34px;
    line-height: 1.2;
}

.write-layout {
    display: grid;
    grid-template-columns: minmax(0, 0.95fr) minmax(320px, 1.05fr);
    gap: 24px;
    align-items: start;
}

.write-form,
.live-preview {
    background: rgba(0, 0, 0, 0.78);
    border: 1px solid rgba(140, 207, 255, 0.22);
    border-radius: 8px;
}

.write-form {
    display: grid;
    gap: 12px;
    padding: 20px;
}

.write-form label {
    color: var(--muted-blue);
    font-size: 13px;
    font-weight: 700;
}

.write-form input[type="text"],
.write-form input[type="file"],
.write-form textarea {
    width: 100%;
    min-width: 0;
    box-sizing: border-box;
    padding: 11px 12px;
    color: var(--neon-text);
    background: rgba(0, 0, 0, 0.58);
    border: 1px solid rgba(140, 207, 255, 0.26);
    border-radius: 6px;
}

.write-form textarea {
    resize: vertical;
}

.check-row {
    display: flex;
    align-items: center;
    gap: 10px;
    color: var(--neon-text);
}

.helper-text,
.form-error {
    margin: 0;
    font-size: 13px;
    line-height: 1.5;
}

.helper-text {
    color: var(--muted-blue);
}

.selected-image-list {
    display: grid;
    gap: 8px;
    margin: 0;
    padding: 0;
    list-style: none;
}

.selected-image-list li {
    display: grid;
    grid-template-columns: 18px 48px minmax(0, 1fr) 32px;
    align-items: center;
    gap: 10px;
    min-height: 58px;
    padding: 6px;
    color: var(--neon-text);
    background: rgba(4, 17, 36, 0.78);
    border: 1px solid rgba(140, 207, 255, 0.22);
    border-radius: 6px;
    cursor: grab;
    transition: border-color 0.18s ease, opacity 0.18s ease, transform 0.18s ease;
}

.selected-image-list li:hover,
.selected-image-list li:focus-within {
    border-color: rgba(4, 217, 255, 0.58);
}

.selected-image-list li.is-dragging {
    opacity: 0.58;
    transform: scale(0.99);
}

.drag-handle {
    color: var(--muted-blue);
    font-size: 13px;
    font-weight: 900;
    letter-spacing: 0;
    text-align: center;
}

.selected-image-list img {
    width: 48px;
    height: 48px;
    object-fit: cover;
    background: #000;
    border: 1px solid rgba(140, 207, 255, 0.22);
    border-radius: 6px;
}

.image-item-text {
    display: grid;
    gap: 3px;
    min-width: 0;
}

.image-item-text strong {
    overflow: hidden;
    font-size: 13px;
    font-weight: 700;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.image-item-text small {
    color: var(--muted-blue);
    font-size: 11px;
    font-weight: 700;
}

.selected-image-list button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    padding: 0;
    color: #ffd9e2;
    background: rgba(255, 107, 139, 0.12);
    border: 1px solid rgba(255, 107, 139, 0.42);
    border-radius: 50%;
    font-size: 14px;
    font-weight: 900;
    line-height: 1;
    box-shadow: none;
    cursor: pointer;
}

.selected-image-list button:hover,
.selected-image-list button:focus-visible {
    border-color: #ff6b8b;
    box-shadow: 0 0 14px rgba(255, 107, 139, 0.28);
    outline: none;
}

.form-error {
    color: #ff8aa5;
    font-weight: 700;
}

.form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
}

.form-actions button {
    min-width: 88px;
    min-height: 40px;
    padding: 0 16px;
}

.secondary-button {
    background: rgba(4, 217, 255, 0.08);
    box-shadow: none;
}

.live-preview {
    overflow: hidden;
    position: sticky;
    top: 96px;
}

.preview-image {
    position: relative;
    aspect-ratio: 16 / 9;
    background: #000;
    border-bottom: 1px solid rgba(140, 207, 255, 0.22);
}

.preview-body {
    display: grid;
    gap: 14px;
    padding: 20px;
}

.preview-topline,
.preview-meta {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    color: var(--muted-blue);
    font-size: 13px;
}

.preview-body h2 {
    margin: 0;
    color: #ffffff;
    font-size: 26px;
    line-height: 1.3;
    overflow-wrap: anywhere;
}

.preview-body p {
    min-height: 180px;
    margin: 0;
    line-height: 1.7;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
}

@media (max-width: 860px) {
    .write-layout {
        grid-template-columns: 1fr;
    }

    .live-preview {
        position: static;
    }
}

@media (max-width: 640px) {
    .write-page {
        padding: 96px 14px 40px;
    }

    .write-header {
        grid-template-columns: 1fr;
        align-items: stretch;
    }

    .title-block {
        order: -1;
    }

    .back-link {
        justify-self: stretch;
    }

    .form-actions {
        flex-direction: column;
    }
}
</style>
