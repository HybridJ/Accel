<script setup>
import { RouterLink, RouterView } from 'vue-router'
import Header from './components/common/Header.vue';
import Footer from './components/common/Footer.vue';
import ToastHost from './components/common/ToastHost.vue';
</script>

<template>
  <div class="app-shell">
    <header class="site-header">
      <Header />
    </header>

    <main class="page-content">
      <RouterView />
    </main>

    <footer>
      <Footer />
    </footer>

    <ToastHost />
  </div>
</template>

<style scoped>
:global(:root) {
  color-scheme: dark;
  --night-black: #000000;
  --night-panel: rgba(4, 17, 36, 0.88);
  --night-panel-strong: rgba(6, 26, 54, 0.96);
  --neon-blue: #2323ff;
  --neon-cyan: #04d9ff;
  --neon-text: #e9fbff;
  --muted-blue: #8ccfff;
  --line-blue: rgba(4, 217, 255, 0.42);
  --glow-blue: 0 0 18px rgba(35, 35, 255, 0.48), 0 0 36px rgba(4, 217, 255, 0.22);
}

:global(html),
:global(body),
:global(#app) {
  min-height: 100%;
  margin: 0;
  background: var(--night-black);
  color: var(--neon-text);
}

:global(*) {
  box-sizing: border-box;
}

:global(html) {
  overflow-x: hidden;
}

:global(body) {
  font-family: "Asta Sans", Arial, Helvetica, sans-serif;
  min-width: 320px;
  overflow-x: hidden;
}

:global(a) {
  color: var(--neon-cyan);
  text-decoration-color: rgba(4, 217, 255, 0.55);
}

:global(button),
:global(input),
:global(textarea) {
  font: inherit;
}

:global(button) {
  color: var(--neon-text);
  background: linear-gradient(135deg, rgba(35, 35, 255, 0.9), rgba(4, 217, 255, 0.22));
  border: 1px solid var(--line-blue);
  border-radius: 6px;
  box-shadow: var(--glow-blue);
}

:global(button:disabled) {
  cursor: not-allowed;
  opacity: 0.45;
  box-shadow: none;
}

:global(input),
:global(textarea) {
  color: var(--neon-text);
  background: rgba(0, 0, 0, 0.74);
  border: 1px solid var(--line-blue);
  border-radius: 6px;
  outline: none;
}

:global(input:focus),
:global(textarea:focus) {
  border-color: var(--neon-cyan);
  box-shadow: 0 0 0 3px rgba(4, 217, 255, 0.14), var(--glow-blue);
}

.app-shell {
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  background: var(--night-black);
  color: var(--neon-text);
}

.page-content {
  flex: 1;
  min-width: 0;
}
</style>
