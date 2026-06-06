<template>
  <AppLayout v-if="authStore.isLoggedIn">
    <router-view />
  </AppLayout>
  <router-view v-else />
</template>

<script setup>
import { useAuthStore } from '@/store/auth'
import AppLayout from '@/layout/AppLayout.vue'

const authStore = useAuthStore()
</script>

<style>
:root {
  --el-color-primary: var(--color-primary);
  --el-color-primary-light-3: var(--color-primary-light);
  --el-color-primary-light-5: oklch(0.75 0.06 260);
  --el-color-primary-light-7: oklch(0.85 0.04 260);
  --el-color-primary-dark-2: var(--color-primary-hover);
  --el-color-success: var(--color-success);
  --el-color-warning: var(--color-warning);
  --el-color-danger: var(--color-danger);
  --el-color-info: var(--color-text-tertiary);

  --el-text-color-primary: var(--color-text);
  --el-text-color-regular: var(--color-text-secondary);
  --el-text-color-secondary: var(--color-text-tertiary);
  --el-text-color-placeholder: var(--color-text-tertiary);

  --el-bg-color: white;
  --el-bg-color-page: var(--color-bg);
  --el-bg-color-overlay: white;
  --el-border-color: var(--color-border);
  --el-border-color-light: var(--color-border-light);
  --el-border-color-lighter: oklch(0.95 0.003 260);
  --el-border-radius-base: var(--radius-md);
  --el-border-radius-small: var(--radius-sm);

  --el-font-size-base: var(--text-base);
  --el-font-size-small: var(--text-sm);
  --el-font-size-large: var(--text-md);
  --el-font-family: var(--font-sans);
  --el-font-weight-primary: 500;

  --el-fill-color: var(--color-card);
  --el-fill-color-light: oklch(0.96 0.005 260);
  --el-fill-color-blank: white;
}

.el-table {
  --el-table-border-color: var(--color-border-light);
  --el-table-header-bg-color: var(--color-card);
  --el-table-row-hover-bg-color: var(--color-primary-bg);
  --el-table-header-text-color: var(--color-text-secondary);
}

.el-table th.el-table__cell {
  font-weight: 600;
  font-size: var(--text-xs);
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--color-text-secondary);
}

.el-table--striped .el-table__body tr.el-table__row--striped td {
  background: var(--color-card);
}

.el-dialog {
  --el-dialog-border-radius: var(--radius-xl);
}

.el-dialog__header {
  padding: var(--space-5) var(--space-6);
  border-bottom: 1px solid var(--color-border-light);
  margin: 0;
}

.el-dialog__title {
  font-size: var(--text-lg);
  font-weight: 600;
}

.el-dialog__body {
  padding: var(--space-6);
}

.el-dialog__footer {
  padding: var(--space-4) var(--space-6);
  border-top: 1px solid var(--color-border-light);
  background: var(--color-card);
}

.el-form-item__label {
  font-weight: 500;
  font-size: var(--text-sm);
  color: var(--color-text);
  padding-bottom: var(--space-1);
}

.el-input__wrapper {
  box-shadow: 0 0 0 1px var(--el-border-color) inset;
  border-radius: var(--radius-md);
}

.el-input__wrapper:hover {
  box-shadow: 0 0 0 1px var(--color-primary-light) inset;
}

.el-input__wrapper.is-focus {
  box-shadow: 0 0 0 1px var(--color-primary) inset;
}

.el-button--primary {
  --el-button-bg-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
  --el-button-hover-bg-color: var(--color-primary-hover);
  --el-button-hover-border-color: var(--color-primary-hover);
  --el-button-active-bg-color: var(--color-primary-hover);
}

.el-button--small {
  border-radius: var(--radius-md);
}

.el-tag {
  --el-tag-border-radius: var(--radius-md);
}

.el-card {
  --el-card-border-radius: var(--radius-lg);
  --el-card-padding: var(--space-6);
  border: 1px solid var(--color-border-light);
}

.el-pagination {
  --el-pagination-button-bg-color: transparent;
}

/* 全局过渡 */
* {
  transition-property: background-color, border-color, color, opacity, box-shadow;
  transition-duration: var(--transition-fast);
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
}

a, button, [role="button"] {
  cursor: pointer;
}
</style>
