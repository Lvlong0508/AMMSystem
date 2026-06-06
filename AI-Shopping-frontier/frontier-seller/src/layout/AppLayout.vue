<template>
  <div class="app-layout">
    <AppSidebar />
    <el-main class="app-main">
      <AppTopBar />
      <div class="app-content">
        <router-view />
      </div>
    </el-main>
  </div>
</template>

<script setup>
import AppSidebar from './AppSidebar.vue'
import AppTopBar from './AppTopBar.vue'
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 0;
  background: var(--color-bg);
}

.app-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-6);
}
</style>
