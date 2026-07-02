<template>
    <div class="login-page">
        <h1 class="login-title">로그인</h1>

        <div class="login-box">
            <form class="login-form" @submit.prevent="handleLogin">
                <div class="form-row">
                    <label for="userId">아이디</label>
                    <input id="userId" v-model="loginForm.userId" type="text" name="userId" placeholder="아이디">
                </div>

                <div class="form-row">
                    <label for="password">비밀번호</label>
                    <input id="password" v-model="loginForm.password" type="password" name="password"
                        placeholder="비밀번호">
                </div>

                <p class="login-error" v-if="loginError" role="alert">{{ loginError }}</p>

                <div class="login-actions">
                    <button type="submit" class="login-submit">로그인</button>
                    <button type="button" class="login-register" @click="toRegister">회원가입</button>
                </div>
            </form>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import { showToast } from '@/composables/useToast';

const route = useRoute()
const router = useRouter()
const store = useAuthStore()

const loginForm = ref({
    userId: '',
    password: '',
})

const loginError = ref('')

const homeRoute = { name: 'home' }

const redirectTarget = () => {
    const redirect = Array.isArray(route.query.redirect)
        ? route.query.redirect[0]
        : route.query.redirect

    if (!redirect || !redirect.startsWith('/') || redirect.startsWith('//')) {
        return homeRoute
    }

    const resolved = router.resolve(redirect)

    if (resolved.name === 'login' || resolved.name === 'register') {
        return homeRoute
    }

    return redirect
}

const handleLogin = async function () {
    loginError.value = ''

    try {
        const result = await store.login(loginForm.value)

        const displayName = result?.username || loginForm.value.userId
        showToast(`${displayName}님 환영합니다!`)

        router.push(redirectTarget())
    } catch (error) {
        console.error('로그인 실패:', error)

        const status = error.response?.status

        if (status === 401) {
            loginError.value = '아이디 또는 비밀번호가 올바르지 않습니다.'
        } else if (!error.response) {
            loginError.value = '서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.'
        } else {
            loginError.value = '로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.'
        }
    }
}

const toRegister = function () {
    router.push({
        name: 'register',
        query: route.query.redirect ? { redirect: route.query.redirect } : {},
    })
}
</script>

<style scoped>
.login-page {
    display: grid;
    align-content: center;
    justify-items: center;
    gap: 24px;
    width: 100%;
    min-height: 100vh;
    min-height: 100dvh;
    padding: 96px 16px 32px;
}

.login-title {
    margin: 0;
    color: #ffffff;
    font-size: clamp(38px, 6vw, 60px);
    font-weight: 800;
    letter-spacing: 0.02em;
    line-height: 1.1;
    text-align: center;
    text-shadow: 0 0 16px rgba(4, 217, 255, 0.38);
}

.login-box {
    width: min(480px, 100%);
    padding: 3.5rem 2.75rem;
    color: var(--neon-text);
    background: var(--night-panel);
    border: 1px solid var(--line-blue);
    border-radius: 0.5rem;
    box-shadow: var(--glow-blue);
    backdrop-filter: blur(12px);
}

.login-form {
    display: grid;
    gap: 24px;
}

.login-error {
    margin: 0;
    color: #ff8aa5;
    font-weight: 700;
    text-align: center;
}

.form-row {
    display: grid;
    grid-template-columns: 92px minmax(0, 1fr);
    align-items: center;
    gap: 14px;
}

.login-box label {
    color: var(--muted-blue);
    font-weight: 800;
    line-height: 1.3;
    text-align: center;
    white-space: nowrap;
}

.login-box input {
    width: 100%;
    min-width: 0;
    margin: 0;
    padding: 13px 14px;
}

.login-actions {
    display: grid;
    gap: 12px;
    margin-top: 16px;
}

.login-actions button {
    width: 100%;
    min-height: 52px;
    padding: 12px 16px;
    font-weight: 800;
    cursor: pointer;
    transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.login-actions button:hover,
.login-actions button:focus-visible {
    transform: translateY(-1px);
    border-color: var(--neon-cyan);
    box-shadow: var(--glow-blue);
    outline: none;
}

.login-register {
    background: rgba(4, 217, 255, 0.08);
    box-shadow: none;
}

.login-register:hover,
.login-register:focus-visible {
    background: rgba(4, 217, 255, 0.16);
}

@media (max-height: 620px),
(max-width: 520px) {
    .login-page {
        align-content: start;
        padding-top: 88px;
    }

    .login-box {
        padding: 1.5rem;
    }
}

@media (max-width: 520px) {
    .form-row {
        grid-template-columns: 76px minmax(0, 1fr);
        gap: 12px;
    }

    .login-box label {
        font-size: 14px;
    }
}
</style>
