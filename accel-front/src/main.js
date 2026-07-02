import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAuthStore } from '@/stores/authStore'

const app = createApp(App)

app.use(createPinia())

app.use(router)

// 새로고침해도 httpOnly 리프레시 쿠키로 로그인 세션을 복원 시도(비로그인이면 조용히 무시)
useAuthStore().tryRestoreSession()

app.mount('#app')
