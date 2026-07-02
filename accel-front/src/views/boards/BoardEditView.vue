<template>
    <main class="write-page">
        <header class="write-header">
            <RouterLink class="back-link" :to="{ name: 'BoardArticle', params: { articleId: `${route.params.articleId}` } }">
                <span aria-hidden="true">&lt;</span>
                게시글로 돌아가기
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
                        <span>{{ articleDate }}</span>
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
                <input id="article-user" v-model="form.userId" type="text" disabled>

                <label for="article-title">Title</label>
                <input id="article-title" v-model="form.title" type="text" placeholder="Write a title">

                <label for="article-content">Content</label>
                <textarea id="article-content" v-model="form.content" rows="12" placeholder="Write your post"></textarea>

                <label class="check-row">
                    <input v-model="form.testDriveAvailable" type="checkbox">
                    Test drive available
                </label>

                <label for="article-image">이미지 관리</label>
                <input
                    id="article-image"
                    ref="imageInput"
                    type="file"
                    accept="image/jpeg,image/png,image/gif,image/webp"
                    multiple
                    @change="selectImages"
                >
                <p class="helper-text">
                    이미지는 최대 10장까지 유지할 수 있으며, 기존 이미지를 삭제하거나 새 이미지를 추가할 수 있습니다.
                </p>

                <ul class="selected-image-list" v-if="imageItems.length" aria-label="게시글 이미지 목록">
                    <li
                        v-for="(imageItem, index) in imageItems"
                        :key="imageItem.id"
                        :class="{ 'is-dragging': draggedImageIndex === index }"
                        draggable="true"
                        @dragstart="startImageDrag(index, $event)"
                        @dragover.prevent="hoverImageDropTarget($event)"
                        @drop="dropImage(index, $event)"
                        @dragend="endImageDrag"
                    >
                        <span class="drag-handle" aria-hidden="true">::</span>
                        <img :src="imageItem.previewUrl" :alt="`${imageItem.name} 미리보기`">
                        <span class="image-item-text">
                            <strong>{{ imageItem.name }}</strong>
                            <small>{{ imageItem.kind === 'existing' ? '등록된 이미지' : '추가 이미지' }}</small>
                        </span>
                        <button type="button" aria-label="이미지 제거" @click="removeImageItem(index)">x</button>
                    </li>
                </ul>
                <p class="helper-text" v-else>
                    저장하면 게시글 이미지가 모두 삭제됩니다.
                </p>
                <p class="form-error" v-if="submitError">{{ submitError }}</p>

                <div class="form-actions">
                    <button type="button" class="secondary-button" @click="resetForm">Reset</button>
                    <button type="submit" :disabled="isSubmitting || !canSubmit">
                        {{ isSubmitting ? 'Saving...' : 'Save' }}
                    </button>
                </div>
            </form>
        </section>
    </main>
</template>

<script setup>
import videoThumbnail from '@/assets/video_thumbnail.png'
import BoardImageCarousel from '@/components/boards/BoardImageCarousel.vue'
import { resolveArticleImageUrl } from '@/lib/articleImage'
import { useAuthStore } from '@/stores/authStore'
import { useBoardStore } from '@/stores/boardStore'
import { storeToRefs } from 'pinia'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const boardStore = useBoardStore()
const authStore = useAuthStore()
const { article } = storeToRefs(boardStore)
const activePreviewIndex = ref(0)
const isSubmitting = ref(false)
const submitError = ref('')
const imageInput = ref(null)
const imageItems = ref([])
const draggedImageIndex = ref(null)
const nextImageItemId = ref(0)
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

const currentRole = computed(() => {
    return authStore.user?.role
        ?? tokenPayload.value.role
        ?? ''
})

const isAdmin = computed(() => {
    return String(currentRole.value).toUpperCase().replace('ROLE_', '') === 'ADMIN'
})

const canManageArticle = computed(() => {
    return Boolean(currentUserId.value)
        && (currentUserId.value === article.value.userId || isAdmin.value)
})

const brandName = computed(() => {
    return article.value.boardName || `Brand ${article.value.brandId ?? ''}`.trim()
})

const articleDate = computed(() => {
    return formatDate(article.value.createdAt)
})

const previewTitle = computed(() => {
    return form.value.title.trim() || 'Title preview'
})

const previewContent = computed(() => {
    return form.value.content.trim() || 'Your article preview updates here while you write.'
})

const previewImages = computed(() => {
    const imageUrls = imageItems.value
        .map((imageItem) => imageItem.previewUrl)
        .filter(Boolean)

    return imageUrls.length > 0 ? imageUrls : [videoThumbnail]
})

const canSubmit = computed(() => {
    return Boolean(
        canManageArticle.value &&
        form.value.title.trim() &&
        form.value.content.trim()
    )
})

const formatDate = (date) => {
    return date ? String(date).slice(0, 10).replaceAll('-', '.') : '-'
}

const allowedImageTypes = new Set([
    'image/jpeg',
    'image/png',
    'image/gif',
    'image/webp',
])
const maxImageCount = 10
const maxImageSize = 10 * 1024 * 1024
const maxTotalImageSize = 50 * 1024 * 1024

const resetImageInput = () => {
    if (imageInput.value) {
        imageInput.value.value = ''
    }
}

const rawArticleImageUrls = computed(() => {
    const rawUrls = Array.isArray(article.value?.imageUrls) && article.value.imageUrls.length > 0
        ? article.value.imageUrls
        : String(article.value?.imageUrl ?? '').split('||')

    return rawUrls
        .map((imageUrl) => String(imageUrl ?? '').trim())
        .filter(Boolean)
})

const extractImageName = (imageUrl, fallback) => {
    const url = String(imageUrl ?? '')
    const objectNameMatch = url.match(/[?&]objectName=([^&]+)/)
    const rawName = objectNameMatch ? objectNameMatch[1] : url.split('/').pop()

    if (!rawName) {
        return fallback
    }

    try {
        return decodeURIComponent(rawName).split('/').pop() || fallback
    } catch {
        return rawName.split('/').pop() || fallback
    }
}

const createExistingImageItem = (imageUrl, index) => ({
    id: `existing-${nextImageItemId.value++}`,
    kind: 'existing',
    rawUrl: imageUrl,
    previewUrl: resolveArticleImageUrl(imageUrl),
    name: extractImageName(imageUrl, `기존 이미지 ${index + 1}`),
})

const createNewImageItem = (file) => ({
    id: `new-${nextImageItemId.value++}`,
    kind: 'new',
    file,
    previewUrl: URL.createObjectURL(file),
    name: file.name,
})

const revokeNewImagePreviews = () => {
    imageItems.value
        .filter((imageItem) => imageItem.kind === 'new' && imageItem.previewUrl)
        .forEach((imageItem) => URL.revokeObjectURL(imageItem.previewUrl))
}

const setImageItemsFromArticle = () => {
    revokeNewImagePreviews()
    imageItems.value = rawArticleImageUrls.value.map(createExistingImageItem)
    activePreviewIndex.value = 0
    draggedImageIndex.value = null
    resetImageInput()
}

const removeImageItem = (index) => {
    const imageItem = imageItems.value[index]

    if (imageItem?.kind === 'new' && imageItem.previewUrl) {
        URL.revokeObjectURL(imageItem.previewUrl)
    }

    imageItems.value = imageItems.value.filter((_, imageIndex) => imageIndex !== index)

    if (activePreviewIndex.value >= imageItems.value.length) {
        activePreviewIndex.value = Math.max(imageItems.value.length - 1, 0)
    }

    submitError.value = ''
    resetImageInput()
}

const moveImageItem = (fromIndex, toIndex) => {
    if (fromIndex === toIndex || fromIndex < 0 || toIndex < 0) {
        return
    }

    const nextImageItems = [...imageItems.value]
    const [movedItem] = nextImageItems.splice(fromIndex, 1)

    if (!movedItem) {
        return
    }

    nextImageItems.splice(toIndex, 0, movedItem)
    imageItems.value = nextImageItems
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

const selectImages = (event) => {
    const files = Array.from(event.target.files ?? [])

    if (files.length === 0) {
        resetImageInput()
        return
    }

    if (imageItems.value.length + files.length > maxImageCount) {
        submitError.value = `이미지는 최대 ${maxImageCount}장까지 유지할 수 있습니다.`
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

    const currentNewFiles = imageItems.value
        .filter((imageItem) => imageItem.kind === 'new')
        .map((imageItem) => imageItem.file)
    const nextImageFiles = [...currentNewFiles, ...files]
    const totalSize = nextImageFiles.reduce((sum, file) => sum + file.size, 0)

    if (totalSize > maxTotalImageSize) {
        submitError.value = '선택한 이미지 전체 용량은 50MB 이하만 업로드할 수 있습니다.'
        resetImageInput()
        return
    }

    imageItems.value = [
        ...imageItems.value,
        ...files.map(createNewImageItem),
    ]
    activePreviewIndex.value = imageItems.value.length - files.length
    submitError.value = ''
    resetImageInput()
}

const applyArticleToForm = () => {
    form.value = {
        userId: article.value.userId ?? '',
        title: article.value.title ?? '',
        content: article.value.content ?? '',
        testDriveAvailable: Boolean(article.value.testDriveAvailable),
    }
    setImageItemsFromArticle()
}

const loadArticle = async () => {
    await boardStore.fetchArticle(route.params.articleId)
    applyArticleToForm()
}

const resetForm = () => {
    applyArticleToForm()
    submitError.value = ''
}

const buildImagePayload = () => {
    const newImageItems = imageItems.value.filter((imageItem) => imageItem.kind === 'new')
    const newImageIndexById = new Map(
        newImageItems.map((imageItem, index) => [imageItem.id, index])
    )

    return {
        imageFiles: newImageItems.map((imageItem) => imageItem.file),
        imageOrder: imageItems.value.map((imageItem) => {
            if (imageItem.kind === 'existing') {
                return `existing|${imageItem.rawUrl}`
            }

            return `new|${newImageIndexById.get(imageItem.id)}`
        }),
    }
}

const submitArticle = async () => {
    if (!canSubmit.value || isSubmitting.value) {
        return
    }

    isSubmitting.value = true
    submitError.value = ''

    try {
        const imagePayload = buildImagePayload()

        await boardStore.updateArticle(route.params.articleId, {
            ...article.value,
            title: form.value.title.trim(),
            content: form.value.content.trim(),
            testDriveAvailable: Boolean(form.value.testDriveAvailable),
            ...imagePayload,
        })

        router.push({ name: 'BoardArticle', params: { articleId: `${route.params.articleId}` } })
    } catch (error) {
        submitError.value = 'Failed to save the article.'
        console.error(error)
    } finally {
        isSubmitting.value = false
    }
}

watch(() => route.params.articleId, loadArticle)

onMounted(loadArticle)
onBeforeUnmount(revokeNewImagePreviews)
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

.write-form input:disabled {
    opacity: 0.72;
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
