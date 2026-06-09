<template>
  <div class="dashboard">
    <div class="dashboard__card">
      <h2>{{ T.WELCOME_BACK }}，{{ shopName }}</h2>
      <p>{{ T.SUBTITLE }}</p>
    </div>
  </div>
</template>
<script setup>
import { useDashboard } from './Dashboard.js'
const { T, shopName } = useDashboard()
</script>
<style scoped src="./Dashboard.css"></style>
