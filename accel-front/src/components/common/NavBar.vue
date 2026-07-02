<template>
    <div class="nav-menu">
        <div v-if="store.accessToken === '' " class="auth-actions">
            <RouterLink :to="loginRoute">
                <button class="auth-button" type="button">로그인</button>
            </RouterLink>

            <RouterLink :to="{ name: 'register' }">
                <button class="auth-button" type="button">회원가입</button>
            </RouterLink>
        </div>

        <div v-if="store.accessToken !== ''" class="auth-card">
            <div class="member-info">
                <img
                    class="profile-image"
                    :class="{ 'default-profile-image': !store.profileImageUrl || !showProfileImage }"
                    :src="profileImageSrc"
                    :alt="`${memberNickname} 프로필 사진`"
                    @error="showProfileImage = false"
                >
                <div class="member-copy">
                    <span>Signed in as</span>
                    <strong>{{ memberNickname }}</strong>
                </div>
            </div>

            <div class="auth-actions">
                <RouterLink :to="{ name: 'myPage' }">
                    <button class="auth-button" type="button">마이페이지</button>
                </RouterLink>
                <button class="auth-button" type="button" @click="logout">로그아웃</button>
            </div>
        </div>

        <div class="service-label" aria-hidden="true">
            <span>이용 가능한 서비스</span>
        </div>

        <div class="service-actions">
            <RouterLink :to="{ name: 'AiSurveyByType', params: { agentType: 'carbti' } }">
                <button class="service-button" type="button">CARBTI</button>
            </RouterLink>

            <RouterLink :to="{ name: 'AiSurveyByType', params: { agentType: 'carsulting' } }">
                <button class="service-button" type="button">CARSULTING</button>
            </RouterLink>

            <RouterLink :to="{ name: 'EVInfra' }">
                <button class="service-button" type="button">EV Infra</button>
            </RouterLink>

            <RouterLink :to="{ name: 'videoCategory' }">
                <button class="service-button" type="button">자동차 극장</button>
            </RouterLink>

            <RouterLink :to="{ name: 'BrandCategory' }">
                <button class="service-button" type="button">타랑께</button>
            </RouterLink>
        </div>
    </div>
</template>

<script setup>
import defaultProfileImage from '@/assets/ACCEL_Logo_white.png'
import { useAuthStore } from '@/stores/authStore';
import { showToast } from '@/composables/useToast';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { computed, onMounted, ref, watch } from 'vue'

const store = useAuthStore()
const route = useRoute()
const router = useRouter()
const showProfileImage = ref(true)

const loginRoute = computed(() => {
    return {
        name: 'login',
        query: { redirect: route.fullPath },
    }
})

const memberNickname = computed(() => {
    return store.user?.nickname || store.user?.userId || 'ACCEL Member'
})
const profileImageSrc = computed(() => {
    return showProfileImage.value && store.profileImageUrl
        ? store.profileImageUrl
        : defaultProfileImage
})

const ensureMemberProfile = async () => {
    if (!store.accessToken) {
        try {
            await store.refreshSession()
        } catch {
            return
        }
    }

    if (store.accessToken && store.user?.userId && !store.user?.nickname) {
        try {
            await store.fetchMyPage()
        } catch (error) {
            console.error('회원 정보를 불러오지 못했습니다:', error)
        }
    }
}

const logout = async () => {
    await store.logout()
    showToast('로그아웃 되었습니다!')
    router.push({ name: 'home' })
}

onMounted(ensureMemberProfile)

watch(
    () => store.accessToken,
    () => {
        showProfileImage.value = true
        ensureMemberProfile()
    }
)

watch(
    () => store.profileImageUrl,
    () => {
        showProfileImage.value = true
    }
)
</script>

<style scoped>
.nav-menu {
    --nav-menu-offset: clamp(32px, 8vh, 96px);

    display: flex;
    flex-direction: column;
    min-height: 100%;
    padding-top: var(--nav-menu-offset);
    color: var(--neon-text);
}

.nav-menu a {
    text-decoration: none;
}

.auth-actions {
    display: flex;
    align-items: center;
    gap: 20px;
    width: 100%;
    margin: 0 0 22px;
}

.auth-actions a {
    flex: 1 1 0;
}

.auth-actions > .auth-button {
    flex: 1 1 0;
}

.auth-card {
    min-height: 172px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    padding: 24px 18px;
    margin: 0 0 22px;
    border: 1px solid rgba(255, 255, 255, 0.22);
    border-radius: 8px;
    background:
        linear-gradient(135deg, rgba(255, 255, 255, 0.08), rgba(4, 217, 255, 0.05)),
        rgba(0, 10, 22, 0.72);
    box-shadow:
        0 0 18px rgba(255, 255, 255, 0.08),
        inset 0 0 20px rgba(4, 217, 255, 0.05);
}

.auth-card .auth-actions {
    margin: 18px 0 0;
}

.member-info {
    display: flex;
    align-items: center;
    gap: 16px;
    text-align: left;
}

.profile-image {
    flex: 0 0 auto;
    width: 72px;
    height: 72px;
    border-radius: 50%;
    border: 2px solid rgba(4, 217, 255, 0.58);
    box-shadow: 0 0 16px rgba(4, 217, 255, 0.22);
    object-fit: cover;
    padding: 4px;
    background: rgba(4, 217, 255, 0.08);
}

.profile-image.default-profile-image {
    object-fit: contain;
}

.member-copy {
    min-width: 0;
    display: grid;
    gap: 4px;
}

.member-copy span {
    color: var(--muted-blue);
    font-size: 13px;
    font-weight: 800;
}

.member-copy strong {
    color: #ffffff;
    font-size: 21px;
    line-height: 1.25;
    word-break: break-word;
}

.nav-menu button {
    width: 100%;
    color: var(--neon-text);
    border-radius: 6px;
    cursor: pointer;
}

.auth-button {
    min-height: 36px;
    padding: 8px 10px;
    font-size: 13px;
    font-weight: 800;
    background: rgba(255, 255, 255, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.28);
    box-shadow: 0 0 14px rgba(255, 255, 255, 0.08);
}

.auth-card .auth-button {
    min-height: 44px;
    font-size: 14px;
}

.service-label {
    display: flex;
    align-items: center;
    gap: 12px;
    margin: 0 0 24px;
    color: var(--neon-cyan);
    font-size: 12px;
    font-weight: 900;
    letter-spacing: 0;
    text-transform: uppercase;
}

.service-label::before,
.service-label::after {
    content: '';
    height: 1px;
    flex: 1 1 auto;
    background: linear-gradient(90deg, transparent, rgba(4, 217, 255, 0.72));
    box-shadow: 0 0 10px rgba(4, 217, 255, 0.28);
}

.service-label::after {
    background: linear-gradient(90deg, rgba(4, 217, 255, 0.72), transparent);
}

.service-actions {
    flex: 1 1 auto;
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
    gap: clamp(18px, 3.4vh, 34px);
    min-height: 0;
}

.service-button {
    position: relative;
    min-height: 72px;
    padding: 20px 16px;
    font-weight: 800;
    background:
        linear-gradient(180deg, rgba(68, 72, 82, 0.82), rgba(33, 37, 46, 0.9)) padding-box,
        linear-gradient(180deg, rgba(190, 198, 210, 0.32), rgba(96, 104, 118, 0.28)) border-box;
    border: 1px solid transparent;
    box-shadow:
        inset 0 1px 0 rgba(255, 255, 255, 0.12),
        0 8px 20px rgba(0, 0, 0, 0.22);
    transition:
        color 0.2s ease,
        background 0.2s ease,
        box-shadow 0.2s ease;
}

.auth-button:hover {
    border-color: rgba(255, 255, 255, 0.64);
    box-shadow: 0 0 18px rgba(255, 255, 255, 0.18);
}

.service-button:hover {
    color: #ffffff;
    background:
        linear-gradient(180deg, rgba(79, 84, 96, 0.92), rgba(39, 44, 55, 0.96)) padding-box,
        conic-gradient(
            from var(--glow-angle),
            rgba(4, 217, 255, 0.2),
            rgba(107, 241, 255, 0.95),
            rgba(35, 93, 255, 0.98),
            rgba(4, 217, 255, 0.22),
            rgba(107, 241, 255, 0.95),
            rgba(4, 217, 255, 0.2)
        ) border-box;
    box-shadow:
        0 0 16px rgba(4, 217, 255, 0.38),
        0 0 34px rgba(35, 93, 255, 0.28),
        inset 0 0 18px rgba(4, 217, 255, 0.1);
    animation: neon-border 1.4s linear infinite;
}

.service-button:focus-visible {
    outline: none;
    background:
        linear-gradient(180deg, rgba(79, 84, 96, 0.92), rgba(39, 44, 55, 0.96)) padding-box,
        conic-gradient(
            from var(--glow-angle),
            rgba(4, 217, 255, 0.2),
            rgba(107, 241, 255, 0.95),
            rgba(35, 93, 255, 0.98),
            rgba(4, 217, 255, 0.22),
            rgba(107, 241, 255, 0.95),
            rgba(4, 217, 255, 0.2)
        ) border-box;
    box-shadow:
        0 0 16px rgba(4, 217, 255, 0.42),
        0 0 34px rgba(35, 93, 255, 0.3),
        inset 0 0 18px rgba(4, 217, 255, 0.1);
    animation: neon-border 1.4s linear infinite;
}

@property --glow-angle {
    syntax: '<angle>';
    inherits: false;
    initial-value: 0deg;
}

@keyframes neon-border {
    from {
        --glow-angle: 0deg;
    }

    to {
        --glow-angle: 360deg;
    }
}

@media (max-height: 720px) {
    .nav-menu {
        --nav-menu-offset: 18px;
    }

    .auth-card {
        min-height: 0;
        padding: 14px 12px;
    }

    .service-label {
        margin-bottom: 14px;
    }

    .service-button {
        min-height: 58px;
        padding: 16px 12px;
    }
}
</style>
