<template>
    <div class="toast-host" aria-live="polite" aria-atomic="true">
        <TransitionGroup name="toast">
            <div
                v-for="toast in toasts"
                :key="toast.id"
                class="toast"
                :class="`toast--${toast.type}`"
                role="status"
                @click="removeToast(toast.id)"
            >
                <span class="toast__icon" aria-hidden="true">{{ iconFor(toast.type) }}</span>
                <p class="toast__message">{{ toast.message }}</p>
            </div>
        </TransitionGroup>
    </div>
</template>

<script setup>
import { useToast } from '@/composables/useToast'

const { toasts, removeToast } = useToast()

const iconFor = (type) => {
    if (type === 'error') {
        return '!'
    }

    if (type === 'info') {
        return 'i'
    }

    return '✓'
}
</script>

<style scoped>
.toast-host {
    position: fixed;
    top: clamp(16px, 4vh, 32px);
    left: 50%;
    transform: translateX(-50%);
    z-index: 1100;
    display: flex;
    flex-direction: column;
    gap: 12px;
    width: min(440px, calc(100vw - 32px));
    pointer-events: none;
}

.toast {
    pointer-events: auto;
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 15px 18px;
    color: var(--neon-text);
    background:
        linear-gradient(135deg, rgba(4, 217, 255, 0.12), rgba(35, 35, 255, 0.06)),
        var(--night-panel-strong, rgba(6, 26, 54, 0.96));
    border: 1px solid var(--line-blue);
    border-left: 4px solid var(--neon-cyan);
    border-radius: 10px;
    box-shadow: var(--glow-blue);
    backdrop-filter: blur(12px);
    cursor: pointer;
    font-weight: 700;
}

.toast--error {
    border-left-color: #ff6b8b;
    box-shadow: 0 0 18px rgba(255, 107, 139, 0.45), 0 0 36px rgba(255, 107, 139, 0.2);
}

.toast--info {
    border-left-color: var(--muted-blue);
}

.toast__icon {
    flex: 0 0 auto;
    display: grid;
    place-items: center;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    font-size: 15px;
    font-weight: 900;
    color: var(--night-black, #000);
    background: var(--neon-cyan);
    box-shadow: 0 0 12px rgba(4, 217, 255, 0.6);
}

.toast--error .toast__icon {
    background: #ff6b8b;
    box-shadow: 0 0 12px rgba(255, 107, 139, 0.6);
}

.toast--info .toast__icon {
    background: var(--muted-blue);
    box-shadow: 0 0 12px rgba(140, 207, 255, 0.55);
}

.toast__message {
    margin: 0;
    line-height: 1.4;
    word-break: break-word;
}

/* 진입/이탈 + 재정렬 애니메이션 */
.toast-enter-active,
.toast-leave-active {
    transition: opacity 0.32s cubic-bezier(0.22, 1, 0.36, 1),
        transform 0.32s cubic-bezier(0.22, 1, 0.36, 1);
}

.toast-enter-from,
.toast-leave-to {
    opacity: 0;
    transform: translateY(-14px) scale(0.98);
}

.toast-move {
    transition: transform 0.32s cubic-bezier(0.22, 1, 0.36, 1);
}

@media (prefers-reduced-motion: reduce) {
    .toast-enter-active,
    .toast-leave-active,
    .toast-move {
        transition: none;
    }
}
</style>
