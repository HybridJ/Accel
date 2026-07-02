<template>
    <div class="site-header">
        <RouterLink :to="{ name: 'home' }">
            <img src="../../assets/ACCEL_Logo_white.png" alt="ACCEL logo" class="main-logo">
        </RouterLink>

        <div>
            <button class="menu-button" type="button" @click="openMenu" aria-label="Open menu">
                <svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 -960 960 960" width="24px"
                    fill="#1f1f1f" class="hamburger-menu">
                    <path d="M120-240v-80h720v80H120Zm0-200v-80h720v80H120Zm0-200v-80h720v80H120Z" />
                </svg>
            </button>
        </div>

        <div v-if="isMenuOpen" class="menu-backdrop" @click="closeMenu"></div>

        <nav class="side-nav" :class="{ open: isMenuOpen }" aria-label="Main navigation">
            <button class="close-button" type="button" @click="closeMenu" aria-label="Close menu">
                X
            </button>

            <NavBar />
        </nav>
    </div>
</template>

<script setup>
    import { ref, watch } from 'vue';
    import { RouterLink, useRoute } from 'vue-router';
    import NavBar from './NavBar.vue';

    const route = useRoute()
    const isMenuOpen = ref(false)

    const openMenu = function(){
        isMenuOpen.value = true
    }

    const closeMenu = function(){
        isMenuOpen.value = false
    }

    watch(
        () => route.fullPath,
        () => {
            closeMenu()
        }
    )

</script>

<style scoped>
.site-header {
    position: fixed;
    top: 0;
    left: 0;

    width: 100%;
    height: 72px;

    display: flex;
    justify-content: center;
    align-items: center;

    background: transparent;
    border-bottom: none;
    box-shadow: none;
    backdrop-filter: none;
    z-index: 1000;
}

.site-header > a {
    display: flex;
    align-items: center;
    height: 72px;
}

.main-logo {
    width: 200px;
    height: auto;
    display: block;
    filter: drop-shadow(0 0 12px rgba(4, 217, 255, 0.52));
}

.menu-button {
  position: absolute;
  top: 50%;
  right: 24px;
  transform: translateY(-50%);

  display: flex;
  justify-content: center;
  align-items: center;

  background: none;
  border: none;
  width: 52px;
  height: 52px;
  padding: 0;
  cursor: pointer;
}

.hamburger-menu {
  display: block;
  width: 36px;
  height: 36px;
  fill: var(--neon-cyan);
  filter: drop-shadow(0 0 12px rgba(4, 217, 255, 0.9));
}

.menu-backdrop {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.72);
  z-index: 1001;
}

.side-nav {
  position: fixed;
  top: 0;
  right: 0;

  width: 400px;
  max-width: min(92vw, 440px);
  height: 100vh;
  height: 100dvh;

  display: flex;
  flex-direction: column;
  padding: 24px;
  overflow-y: auto;
  overscroll-behavior: contain;

  background: linear-gradient(180deg, rgba(1, 8, 22, 0.98), rgba(0, 0, 0, 0.96));
  border-left: 1px solid var(--line-blue);
  box-shadow: -12px 0 34px rgba(4, 217, 255, 0.18);
  transform: translateX(100%);
  transition: transform 0.25s ease;
  z-index: 1002;
}

.side-nav.open {
  transform: translateX(0);
}

.close-button {
  align-self: flex-end;

  display: flex;
  justify-content: center;
  align-items: center;

  width: 36px;
  height: 36px;

  color: var(--neon-cyan);
  background: rgba(4, 217, 255, 0.08);
  border: 1px solid var(--line-blue);
  font-size: 20px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 0 14px rgba(4, 217, 255, 0.2);
}

@media (max-width: 520px) {
  .site-header,
  .site-header > a {
    height: 64px;
  }

  .main-logo {
    width: 156px;
  }

  .menu-button {
    right: 12px;
    width: 46px;
    height: 46px;
  }

  .side-nav {
    width: min(100vw, 380px);
    max-width: 100vw;
    padding: 18px;
  }
}

</style>
