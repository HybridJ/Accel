<template>
    <main class="user-page">
        <section class="user-panel">
            <header class="profile-header">
                <div class="profile-summary">
                    <img
                        class="profile-image"
                        :class="{ 'default-profile-image': !authStore.profileImageUrl || !showProfileImage }"
                        :src="profileImageSrc"
                        :alt="`${memberName} 프로필 사진`"
                        @error="showProfileImage = false"
                    >

                    <div class="profile-copy">
                        <p>My Page</p>
                        <h1>{{ memberName }}</h1>
                        <span>{{ user?.email || '등록된 이메일이 없습니다.' }}</span>
                    </div>
                </div>

                <RouterLink :to="{ name: 'updateInfo' }">
                    <button type="button">정보 수정</button>
                </RouterLink>
            </header>

            <div class="info-grid">
                <div v-for="item in memberInfo" :key="item.label" class="info-item">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                </div>
            </div>

            <p v-if="errorMessage" class="status-message">{{ errorMessage }}</p>
        </section>

        <nav class="activity-nav" aria-label="내 활동 바로가기">
            <RouterLink class="activity-button" :to="{ name: 'myActivity', params: { type: 'written' } }">
                <svg class="activity-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M12 20h9" />
                    <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z" />
                </svg>
                <span class="activity-label">내가 작성한 글</span>
            </RouterLink>
            <RouterLink class="activity-button" :to="{ name: 'myActivity', params: { type: 'commented' } }">
                <svg class="activity-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                    <circle cx="8" cy="10" r="1" fill="currentColor" stroke="none" />
                    <circle cx="12" cy="10" r="1" fill="currentColor" stroke="none" />
                    <circle cx="16" cy="10" r="1" fill="currentColor" stroke="none" />
                </svg>
                <span class="activity-label">내가 댓글 단 글</span>
            </RouterLink>
            <RouterLink class="activity-button" :to="{ name: 'myActivity', params: { type: 'liked' } }">
                <svg class="activity-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                </svg>
                <span class="activity-label">내가 좋아요한 글</span>
            </RouterLink>
        </nav>

        <section class="danger-zone">
            <div>
                <strong>계정 삭제</strong>
                <p>계정을 삭제하면 회원 정보와 관련 활동 내역이 함께 삭제됩니다.</p>
            </div>
            <button type="button" class="danger-button" @click="deleteAccount">삭제</button>
        </section>
    </main>
</template>

<script setup>
import defaultProfileImage from '@/assets/ACCEL_Logo_white.png'
import { useAuthStore } from '@/stores/authStore'
import { storeToRefs } from 'pinia'
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

const router = useRouter()
const authStore = useAuthStore()
const { user } = storeToRefs(authStore)
const errorMessage = ref('')
const showProfileImage = ref(true)

const memberName = computed(() => user.value?.nickname || user.value?.userId || 'ACCEL Member')
const profileImageSrc = computed(() => {
    return showProfileImage.value && authStore.profileImageUrl
        ? authStore.profileImageUrl
        : defaultProfileImage
})
const memberInfo = computed(() => [
    { label: '아이디', value: user.value?.userId || '-' },
    { label: '권한', value: user.value?.role || '-' },
    { label: '이름', value: user.value?.name || '-' },
    { label: '닉네임', value: user.value?.nickname || '-' },
    { label: '나이', value: user.value?.age ?? '-' },
    { label: '이메일', value: user.value?.email || '-' },
])

const deleteAccount = async () => {
    if (!window.confirm('계정을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.')) {
        return
    }

    try {
        await authStore.deleteMyPage()
        router.push({ name: 'home' })
    } catch {
        errorMessage.value = '계정을 삭제하지 못했습니다.'
    }
}

onMounted(async () => {
    try {
        await authStore.fetchMyPage()
    } catch {
        errorMessage.value = '회원 정보를 불러오지 못했습니다.'
    }
})

watch(
    () => authStore.profileImageUrl,
    () => {
        showProfileImage.value = true
    }
)
</script>

<style scoped>
.user-page {
    width: min(100%, 980px);
    margin: 0 auto;
    padding: 112px 20px 56px;
    color: var(--neon-text);
}

.user-panel {
    padding: 30px;
    background:
        linear-gradient(180deg, rgba(4, 17, 36, 0.92), rgba(0, 0, 0, 0.88)),
        rgba(0, 0, 0, 0.82);
    border: 1px solid var(--line-blue);
    border-radius: 8px;
    box-shadow: var(--glow-blue);
    backdrop-filter: blur(10px);
}

.profile-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 24px;
    padding-bottom: 26px;
    border-bottom: 1px solid rgba(4, 217, 255, 0.2);
}

.profile-summary {
    min-width: 0;
    display: flex;
    align-items: center;
    gap: 18px;
}

.profile-image {
    flex: 0 0 auto;
    width: 96px;
    height: 96px;
    border-radius: 50%;
    border: 1px solid rgba(4, 217, 255, 0.68);
    box-shadow:
        0 0 20px rgba(4, 217, 255, 0.28),
        0 0 38px rgba(35, 35, 255, 0.18);
    object-fit: cover;
    padding: 10px;
    background: rgba(4, 217, 255, 0.08);
}

.profile-image.default-profile-image {
    object-fit: contain;
}

.profile-copy {
    min-width: 0;
    text-align: left;
}

.profile-copy p {
    margin: 0 0 8px;
    color: var(--neon-cyan);
    font-size: 14px;
    font-weight: 900;
    letter-spacing: 0;
    text-transform: uppercase;
}

.profile-copy h1 {
    margin: 0;
    color: #ffffff;
    font-size: clamp(30px, 5vw, 44px);
    line-height: 1.1;
    overflow-wrap: anywhere;
    text-shadow: 0 0 18px rgba(4, 217, 255, 0.34);
}

.profile-copy span {
    display: block;
    margin-top: 10px;
    color: var(--muted-blue);
    font-size: 15px;
    overflow-wrap: anywhere;
}

.profile-header a {
    flex: 0 0 auto;
    text-decoration: none;
}

.profile-header button {
    min-width: 108px;
    padding: 11px 16px;
    cursor: pointer;
}

.info-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;
    margin-top: 26px;
}

.info-item {
    min-width: 0;
    padding: 18px;
    background:
        linear-gradient(135deg, rgba(255, 255, 255, 0.06), rgba(4, 217, 255, 0.04)),
        rgba(0, 10, 22, 0.58);
    border: 1px solid rgba(4, 217, 255, 0.24);
    border-radius: 8px;
    box-shadow:
        inset 0 0 18px rgba(4, 217, 255, 0.04),
        0 0 16px rgba(4, 217, 255, 0.08);
}

.info-item span {
    display: block;
    margin-bottom: 9px;
    color: var(--muted-blue);
    font-size: 13px;
    font-weight: 900;
}

.info-item strong {
    color: #ffffff;
    font-size: 18px;
    line-height: 1.35;
    overflow-wrap: anywhere;
}

.activity-nav {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 16px;
    margin-top: 24px;
}

.activity-button {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 14px;
    min-height: 150px;
    padding: 22px 16px;
    color: #ffffff;
    text-align: center;
    text-decoration: none;
    background:
        linear-gradient(135deg, rgba(255, 255, 255, 0.06), rgba(4, 217, 255, 0.05)),
        rgba(0, 10, 22, 0.58);
    border: 1px solid rgba(4, 217, 255, 0.28);
    border-radius: 12px;
    font-weight: 800;
    font-size: 16px;
    box-shadow: inset 0 0 18px rgba(4, 217, 255, 0.04);
    transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.activity-button:hover,
.activity-button:focus-visible {
    transform: translateY(-2px);
    border-color: var(--neon-cyan);
    box-shadow: var(--glow-blue);
    outline: none;
}

.activity-icon {
    width: 40px;
    height: 40px;
    color: var(--neon-cyan);
}

.activity-label {
    overflow-wrap: anywhere;
}

.status-message {
    margin: 20px 0 0;
    color: var(--muted-blue);
}

.danger-zone {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-top: 28px;
    padding: 18px;
    background: rgba(255, 68, 111, 0.08);
    border: 1px solid rgba(255, 68, 111, 0.36);
    border-radius: 8px;
    box-shadow: 0 0 18px rgba(255, 68, 111, 0.08);
}

.danger-zone strong {
    color: #ffffff;
    font-size: 18px;
}

.danger-zone p {
    margin: 6px 0 0;
    color: var(--muted-blue);
    line-height: 1.5;
}

.danger-button {
    min-width: 96px;
    padding: 10px 16px;
    background: rgba(255, 68, 111, 0.18);
    border-color: rgba(255, 68, 111, 0.7);
    box-shadow: 0 0 18px rgba(255, 68, 111, 0.16);
}

@media (max-width: 700px) {
    .user-page {
        padding: 96px 14px 40px;
    }

    .user-panel {
        padding: 22px;
    }

    .profile-header,
    .danger-zone {
        flex-direction: column;
        align-items: stretch;
    }

    .profile-summary {
        flex-direction: column;
        align-items: center;
        text-align: center;
    }

    .profile-copy {
        text-align: center;
    }

    .profile-header button,
    .danger-button {
        width: 100%;
    }

    .info-grid {
        grid-template-columns: 1fr;
    }

    .activity-nav {
        gap: 10px;
    }

    .activity-button {
        min-height: 104px;
        gap: 10px;
        padding: 14px 8px;
        font-size: 13px;
    }

    .activity-icon {
        width: 28px;
        height: 28px;
    }
}
</style>
