import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import axios from 'axios'
import { API_BASE_URL } from '@/constants/api'

export const useAuthStore = defineStore('auth', () => {
    const REST_URL = API_BASE_URL
    
    // 유저 정보 
    const user = ref(null)
    const accessToken = ref("")
    const isLogin = computed(() => {
        return user.value !== null
    })
    const profileImageUrl = computed(() => {
        const userId = user.value?.userId
        const profileImg = user.value?.profileImg

        if (!userId || !profileImg || profileImg === 'profile_default.png') {
            return ''
        }

        return `${REST_URL}/user/${encodeURIComponent(userId)}/profile-image?v=${encodeURIComponent(profileImg)}`
    })

    const login = async (loginForm) => {
        const response = await axios.post(`${REST_URL}/auth/login`, loginForm, {
            withCredentials: true,
        })
        accessToken.value = response.data.accessToken
        user.value = {
            userId: response.data.username,
            role: response.data.role,
        }
        
        return response.data
    }

    const register = async (registerForm) => {
        const { profileImage, ...user } = registerForm
        const formData = new FormData()

        Object.entries(user).forEach(([key, value]) => {
            formData.append(key, value ?? '')
        })

        if (profileImage) {
            formData.append('profileImage', profileImage)
        }

        await axios.post(`${REST_URL}/auth/signup`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        })
    }

    const authHeaders = () => {
        return accessToken.value
            ? { Authorization: `Bearer ${accessToken.value}` }
            : {}
    }

    // 동시 401/가드/부팅복원이 겹쳐도 refresh를 1회만 보내도록 단일 비행(single-flight) 처리.
    // 백엔드 리프레시 토큰은 1회용(rotation)이라, 동시 호출 시 두 번째가 무효 토큰을 써서 실패하는 것을 방지.
    let refreshPromise = null
    const refreshSession = () => {
        if (!refreshPromise) {
            refreshPromise = axios
                .post(`${REST_URL}/auth/refresh`, null, { withCredentials: true })
                .then((response) => {
                    accessToken.value = response.data.accessToken
                    user.value = {
                        ...user.value,
                        userId: response.data.username,
                        role: response.data.role,
                    }

                    return response.data
                })
                .finally(() => {
                    refreshPromise = null
                })
        }

        return refreshPromise
    }

    // 서버 호출 없이 로컬 세션만 비움(인터셉터의 갱신 실패 처리에서 사용)
    const clearSession = () => {
        user.value = null
        accessToken.value = ''
    }

    // 앱 부팅 시 리프레시 쿠키로 세션 복원 시도. 실패(비로그인)는 조용히 무시.
    const tryRestoreSession = async () => {
        try {
            await refreshSession()
        } catch {
            // 리프레시 토큰이 없거나 만료됨 → 비로그인 상태 유지
        }
    }

    const requestWithRefresh = async (request) => {
        try {
            return await request()
        } catch (error) {
            if (error.response?.status !== 401) {
                throw error
            }

            await refreshSession()
            return request()
        }
    }

    const fetchMyPage = async (targetUserId = user.value?.userId) => {
        if (!targetUserId) {
            throw new Error('User ID is required.')
        }

        const response = await requestWithRefresh(() => axios.get(`${REST_URL}/user/me/${targetUserId}`, {
            headers: authHeaders(),
        }))

        user.value = {
            ...user.value,
            ...response.data,
        }

        return user.value
    }

    const updateMyPage = async (updateForm, targetUserId = user.value?.userId) => {
        if (!targetUserId) {
            throw new Error('User ID is required.')
        }

        const { profileImage, ...userFields } = updateForm
        let response = await requestWithRefresh(() => axios.put(`${REST_URL}/user/me/${targetUserId}`, userFields, {
            headers: authHeaders(),
        }))

        if (profileImage) {
            const formData = new FormData()
            formData.append('profileImage', profileImage)

            response = await requestWithRefresh(() => axios.post(`${REST_URL}/user/me/${targetUserId}/profile-image`, formData, {
                headers: authHeaders(),
            }))
        }

        user.value = {
            ...user.value,
            ...response.data,
        }

        return user.value
    }

    const deleteMyPage = async (targetUserId = user.value?.userId) => {
        if (!targetUserId) {
            throw new Error('User ID is required.')
        }

        await requestWithRefresh(() => axios.delete(`${REST_URL}/user/me/${targetUserId}`, {
            headers: authHeaders(),
            withCredentials: true,
        }))

        await logout()
    }

    const logout = async () => {
        try {
            await axios.post(`${REST_URL}/auth/logout`, null, {
                headers: accessToken.value ? { 'X-Access-Token': accessToken.value } : {},
                withCredentials: true,
            })
        } catch (error) {
            console.error('로그아웃 요청에 실패했습니다:', error)
        }

        clearSession()
    }

    return {
        user,
        accessToken,
        isLogin,
        profileImageUrl,
        authHeaders,
        login,
        register,
        refreshSession,
        clearSession,
        tryRestoreSession,
        fetchMyPage,
        updateMyPage,
        deleteMyPage,
        logout,
    }
})
