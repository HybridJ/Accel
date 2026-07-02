<template>
    <main class="user-page">
        <section class="user-panel">
            <header class="page-header">
                <div>
                    <p class="eyebrow">Account</p>
                    <h1>회원 정보 수정</h1>
                </div>
            </header>

            <form class="update-form" @submit.prevent="submitUpdate">
                <div class="profile-editor">
                    <img
                        class="profile-preview"
                        :class="{ 'default-profile-image': !selectedProfilePreviewUrl && !authStore.profileImageUrl }"
                        :src="profilePreviewUrl"
                        alt="현재 프로필 사진"
                    >
                    <div>
                        <label for="profileImage">프로필 사진</label>
                        <input
                            id="profileImage"
                            type="file"
                            accept="image/*"
                            @change="setProfileImage"
                        >
                    </div>
                </div>

                <label for="name">이름</label>
                <input id="name" v-model="form.name" type="text">

                <label for="nickname">닉네임</label>
                <input id="nickname" v-model="form.nickname" type="text">

                <label for="age">나이</label>
                <input id="age" v-model.number="form.age" type="number" min="0" max="150">

                <label for="email">이메일</label>
                <input id="email" v-model="form.email" type="email">

                <p v-if="errorMessage" class="status-message">{{ errorMessage }}</p>

                <div class="form-actions">
                    <RouterLink :to="{ name: 'myPage' }">
                        <button type="button" class="secondary-button">취소</button>
                    </RouterLink>
                    <button type="submit">저장</button>
                </div>
            </form>
        </section>
    </main>
</template>

<script setup>
import defaultProfileImage from '@/assets/ACCEL_Logo_white.png'
import { useAuthStore } from '@/stores/authStore'
import { storeToRefs } from 'pinia'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

const router = useRouter()
const authStore = useAuthStore()
const { user } = storeToRefs(authStore)
const errorMessage = ref('')
const selectedProfilePreviewUrl = ref('')
const maxProfileImageSize = 10 * 1024 * 1024
const form = ref({
    name: '',
    nickname: '',
    age: null,
    email: '',
    profileImage: null,
})

const profilePreviewUrl = computed(() => {
    return selectedProfilePreviewUrl.value || authStore.profileImageUrl || defaultProfileImage
})

const fillForm = () => {
    form.value = {
        name: user.value?.name ?? '',
        nickname: user.value?.nickname ?? '',
        age: user.value?.age ?? null,
        email: user.value?.email ?? '',
        profileImage: null,
    }
}

const setProfileImage = (event) => {
    const file = event.target.files?.[0] ?? null

    if (selectedProfilePreviewUrl.value) {
        URL.revokeObjectURL(selectedProfilePreviewUrl.value)
        selectedProfilePreviewUrl.value = ''
    }

    if (file && file.size > maxProfileImageSize) {
        alert('프로필 사진은 10MB 이하로 업로드해주세요.')
        event.target.value = ''
        form.value.profileImage = null
        return
    }

    form.value.profileImage = file
    selectedProfilePreviewUrl.value = file ? URL.createObjectURL(file) : ''
}

const submitUpdate = async () => {
    errorMessage.value = ''

    // 빈 나이 입력은 null로 정규화(빈 문자열이 그대로 전송되면 서버가 400)
    if (form.value.age === '') {
        form.value.age = null
    }

    // 백엔드와 동일한 나이 범위(0~150)를 제출 전에 검증해 명확히 안내한다.
    const age = form.value.age
    if (age !== null && (Number.isNaN(Number(age)) || age < 0 || age > 150)) {
        errorMessage.value = '나이는 0세에서 150세 사이로 입력해주세요.'
        return
    }

    try {
        await authStore.updateMyPage(form.value)
        await authStore.fetchMyPage()
        router.push({ name: 'myPage' })
    } catch (error) {
        const status = error.response?.status

        if (status === 409) {
            errorMessage.value = '이미 사용 중인 닉네임입니다.'
        } else if (status === 400) {
            errorMessage.value = '입력값을 확인해주세요. (이름·닉네임은 공백 불가, 나이는 0~150)'
        } else {
            errorMessage.value = '회원 정보를 수정하지 못했습니다.'
        }
    }
}

onBeforeUnmount(() => {
    if (selectedProfilePreviewUrl.value) {
        URL.revokeObjectURL(selectedProfilePreviewUrl.value)
    }
})

onMounted(async () => {
    try {
        if (!user.value?.email) {
            await authStore.fetchMyPage()
        }
        fillForm()
    } catch {
        errorMessage.value = '회원 정보를 불러오지 못했습니다.'
    }
})
</script>

<style scoped>
.user-page {
    max-width: 720px;
    margin: 0 auto;
    padding: 112px 20px 56px;
    color: var(--neon-text);
}

.user-panel {
    padding: 28px;
    background: rgba(0, 0, 0, 0.78);
    border: 1px solid var(--line-blue);
    border-radius: 8px;
    box-shadow: 0 0 18px rgba(4, 217, 255, 0.12);
}

.page-header {
    padding-bottom: 20px;
    border-bottom: 1px solid rgba(4, 217, 255, 0.18);
}

.page-header h1,
.eyebrow {
    margin: 0;
}

.eyebrow {
    color: var(--neon-cyan);
    font-size: 14px;
    font-weight: 700;
}

.page-header h1 {
    margin-top: 8px;
    font-size: 32px;
}

.update-form {
    display: grid;
    gap: 12px;
    margin-top: 24px;
}

.profile-editor {
    display: grid;
    grid-template-columns: 96px minmax(0, 1fr);
    align-items: center;
    gap: 18px;
    padding: 18px;
    margin-bottom: 8px;
    background:
        linear-gradient(135deg, rgba(255, 255, 255, 0.06), rgba(4, 217, 255, 0.04)),
        rgba(0, 10, 22, 0.58);
    border: 1px solid rgba(4, 217, 255, 0.24);
    border-radius: 8px;
}

.profile-preview {
    width: 96px;
    height: 96px;
    padding: 8px;
    object-fit: cover;
    border: 1px solid rgba(4, 217, 255, 0.68);
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.5);
    box-shadow:
        0 0 20px rgba(4, 217, 255, 0.28),
        0 0 38px rgba(35, 35, 255, 0.18);
}

.profile-preview.default-profile-image {
    object-fit: contain;
}

.update-form label {
    color: var(--neon-cyan);
    font-weight: 700;
}

.update-form input {
    width: 100%;
    padding: 12px;
}

.form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 8px;
}

.form-actions a {
    text-decoration: none;
}

.form-actions button {
    min-width: 92px;
    padding: 10px 16px;
}

.secondary-button {
    background: rgba(4, 217, 255, 0.08);
    box-shadow: none;
}

.status-message {
    margin: 4px 0;
    color: var(--muted-blue);
}

@media (max-width: 640px) {
    .user-page {
        padding: 96px 14px 40px;
    }

    .user-panel {
        padding: 20px;
    }

    .form-actions {
        flex-direction: column;
    }

    .profile-editor {
        grid-template-columns: 1fr;
        justify-items: center;
    }
}
</style>
