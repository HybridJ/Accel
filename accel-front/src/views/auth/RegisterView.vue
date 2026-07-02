<template>
    <div class="register-page">
        <h1 class="register-title">회원가입</h1>

        <div class="register-box">
            <form class="register-form" @submit.prevent="handleRegister">
                <div class="form-row">
                    <label for="userId">아이디</label>
                    <input id="userId" v-model="registerForm.userId" type="text" name="userId" placeholder="아이디">
                </div>

                <div class="form-row">
                    <label for="password">비밀번호</label>
                    <input id="password" v-model="registerForm.password" type="password" name="password" placeholder="비밀번호">
                </div>

                <div class="form-row">
                    <label for="name">이름</label>
                    <input id="name" v-model="registerForm.name" type="text" name="name" placeholder="이름">
                </div>

                <div class="form-row">
                    <label for="nickname">닉네임</label>
                    <input id="nickname" v-model="registerForm.nickname" type="text" name="nickname" placeholder="닉네임">
                </div>

                <div class="form-row">
                    <label for="age">나이</label>
                    <input id="age" v-model="registerForm.age" type="number" name="age" min="0" max="150" placeholder="나이">
                </div>

                <div class="form-row">
                    <label for="email">이메일</label>
                    <input id="email" v-model="registerForm.email" type="email" name="email" placeholder="이메일">
                </div>

                <div class="form-row">
                    <label for="profileImage">프로필 사진</label>
                    <input
                        id="profileImage"
                        type="file"
                        name="profileImage"
                        accept="image/*"
                        @change="setProfileImage"
                    >
                </div>

                <div class="register-actions">
                    <button type="button" class="register-cancel" @click="handleCancel">취소</button>
                    <button type="submit" class="register-submit">가입 신청</button>
                </div>
            </form>
        </div>
    </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/authStore'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from '@/composables/useToast'

const router = useRouter()
const store = useAuthStore()

const registerForm = ref({
    userId: '',
    password: '',
    name: '',
    nickname: '',
    age: 0,
    email: '',
    profileImage: null,
})
const maxProfileImageSize = 10 * 1024 * 1024

const setProfileImage = (event) => {
    const file = event.target.files?.[0] ?? null

    if (file && file.size > maxProfileImageSize) {
        alert('프로필 사진은 10MB 이하로 업로드해주세요.')
        event.target.value = ''
        registerForm.value.profileImage = null
        return
    }

    registerForm.value.profileImage = file
}

const handleRegister = async function () {
    // 백엔드와 동일한 나이 범위(0~150)를 제출 전에 검증한다.
    const ageInput = String(registerForm.value.age ?? '').trim()
    if (ageInput !== '') {
        const age = Number(ageInput)
        if (Number.isNaN(age) || age < 0 || age > 150) {
            showToast('나이는 0세에서 150세 사이로 입력해주세요.', { type: 'error' })
            return
        }
    }

    try {
        await store.register(registerForm.value)

        const displayName = registerForm.value.nickname || registerForm.value.name || registerForm.value.userId
        showToast(`${displayName}님 accel에 오신 걸 환영합니다!`)

        router.push({ name: 'login' })
    } catch (error) {
        console.error('회원가입 실패: ', error)
    }
}

const handleCancel = () => {
    router.push({ name: 'login' })
}
</script>

<style scoped>
.register-page {
    display: grid;
    align-content: center;
    justify-items: center;
    gap: 18px;
    width: 100%;
    min-height: 100vh;
    min-height: 100dvh;
    padding: 96px 16px 32px;
}

.register-title {
    margin: 0;
    color: #ffffff;
    font-size: clamp(30px, 5vw, 48px);
    line-height: 1.15;
    text-align: center;
    text-shadow: 0 0 18px rgba(4, 217, 255, 0.42);
}

.register-box {
    width: min(540px, 100%);
    padding: 2rem;
    color: var(--neon-text);
    background: var(--night-panel);
    border: 1px solid var(--line-blue);
    border-radius: 0.5rem;
    box-shadow: var(--glow-blue);
    backdrop-filter: blur(12px);
}

.register-form {
    display: grid;
    gap: 14px;
}

.form-row {
    display: grid;
    grid-template-columns: 126px minmax(0, 1fr);
    align-items: center;
    gap: 18px;
}

.register-box label {
    color: var(--muted-blue);
    font-weight: 800;
    line-height: 1.3;
    text-align: center;
    white-space: nowrap;
}

.register-box input {
    width: 100%;
    min-width: 0;
    margin: 0;
    padding: 11px 12px;
}

.register-actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
    margin-top: 8px;
}

.register-actions button {
    min-height: 48px;
    padding: 10px 18px;
    font-weight: 800;
    cursor: pointer;
    transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.register-actions button:hover,
.register-actions button:focus-visible {
    transform: translateY(-1px);
    outline: none;
}

.register-submit:hover,
.register-submit:focus-visible {
    border-color: var(--neon-cyan);
    box-shadow: var(--glow-blue);
}

.register-cancel {
    background: rgba(255, 120, 120, 0.12);
    border-color: rgba(255, 120, 120, 0.45);
    box-shadow: none;
}

.register-cancel:hover,
.register-cancel:focus-visible {
    border-color: rgba(255, 120, 120, 0.85);
    background: rgba(255, 120, 120, 0.2);
    box-shadow: 0 0 18px rgba(255, 120, 120, 0.35);
}

@media (max-height: 760px), (max-width: 520px) {
    .register-page {
        align-content: start;
        padding-top: 88px;
    }

    .register-box {
        padding: 1.5rem;
    }
}

@media (max-width: 520px) {
    .form-row {
        grid-template-columns: 94px minmax(0, 1fr);
        gap: 12px;
    }

    .register-box label {
        font-size: 14px;
    }
}
</style>
