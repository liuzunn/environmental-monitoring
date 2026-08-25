<template>
  <router-view />
</template>

<script setup>
</script>

<style>
html, body, #app {
  height: 100%;
  margin: 0;
  padding: 0;
  background: var(--bg-primary);
  color: var(--text-primary);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 全局选中色与链接 */
::selection {
  background: rgba(126, 226, 184, 0.25);
}

/* 页面切换过渡（全局） */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity var(--dur-slow) var(--ease-out), transform var(--dur-slow) var(--ease-out);
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(12px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
