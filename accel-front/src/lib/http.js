import axios from 'axios'
import { useAuthStore } from '@/stores/authStore'

// 인증이 필요한(액세스 토큰을 싣는) 요청 전용 axios 인스턴스.
// - 요청 인터셉터: 로그인 상태면 Authorization: Bearer <token> 자동 부착
// - 응답 인터셉터: 401이면 refreshSession으로 1회 자동 갱신 후 원요청 재시도.
//   갱신 실패 시 세션을 비우고 원래 에러를 전파(라우터 가드가 로그인 유도 담당).
//
// 주의: 공개(비인증) GET에는 이 인스턴스를 쓰지 말 것.
// 만료/무효 토큰이 실리면 permitAll 엔드포인트도 401을 반환하기 때문(백엔드 확인됨).
export const http = axios.create()

http.interceptors.request.use((config) => {
    const authStore = useAuthStore()

    if (authStore.accessToken) {
        config.headers = config.headers ?? {}
        config.headers.Authorization = `Bearer ${authStore.accessToken}`
    }

    return config
})

http.interceptors.response.use(
    (response) => response,
    async (error) => {
        const original = error.config
        const authStore = useAuthStore()

        // 401이 아니거나, 이미 한 번 재시도했거나, 갱신할 세션 자체가 없으면 그대로 전파
        if (error.response?.status !== 401 || !original || original.__isRetry || !authStore.accessToken) {
            throw error
        }

        try {
            await authStore.refreshSession()
        } catch {
            authStore.clearSession()
            throw error
        }

        original.__isRetry = true
        return http(original)
    },
)
