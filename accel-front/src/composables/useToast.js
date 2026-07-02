import { ref } from 'vue'

// 앱 전역에서 공유하는 토스트 큐. 모듈 레벨 상태라 어디서 호출해도 같은 큐를 사용한다.
// 화면 렌더링은 App.vue에 한 번 올라가는 <ToastHost />가 담당한다.
const toasts = ref([])
let nextId = 0

/**
 * 네온 테마 토스트를 띄운다.
 * @param {string} message 표시할 문구
 * @param {{ type?: 'success' | 'info' | 'error', duration?: number }} options
 * @returns {number} 토스트 id
 */
export function showToast(message, options = {}) {
    const { type = 'success', duration = 3000 } = options
    const id = ++nextId

    toasts.value.push({ id, message, type })

    if (duration > 0) {
        setTimeout(() => removeToast(id), duration)
    }

    return id
}

export function removeToast(id) {
    toasts.value = toasts.value.filter((toast) => toast.id !== id)
}

export function useToast() {
    return { toasts, showToast, removeToast }
}
