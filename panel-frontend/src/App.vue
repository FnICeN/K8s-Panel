<script setup lang="ts">
import { computed } from 'vue'
import { theme as antdTheme } from 'ant-design-vue'
import { useTheme } from '@/composables/useTheme'

const { colorMode } = useTheme()
const theme = computed(() => ({
  algorithm: colorMode.value === 'dark' ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
  token: {
    colorPrimary: '#409eff',
    colorInfo: '#409eff',
    colorSuccess: '#67c23a',
    colorWarning: '#e6a23c',
    colorError: '#f56c6c',
    colorText: colorMode.value === 'dark' ? '#e5edf5' : '#303133',
    colorTextSecondary: colorMode.value === 'dark' ? '#a8b9ca' : '#606266',
    colorBgBase: colorMode.value === 'dark' ? '#0e1d2d' : '#fff',
    colorBorder: colorMode.value === 'dark' ? '#34465a' : '#dcdfe6',
    borderRadius: 4,
    fontSize: 14,
    controlHeight: 32,
  },
}))
</script>

<template>
  <a-config-provider :theme="theme">
    <RouterView />
  </a-config-provider>
</template>

<style>
:root {
  --app-bg: #f5f7fa;
  --app-surface: #fff;
  --app-surface-muted: #f5f7fa;
  --app-border: #dcdfe6;
  --app-border-soft: #ebeef5;
  --app-text: #303133;
  --app-text-secondary: #606266;
  --app-text-muted: #909399;
  --app-accent: #409eff;
  --app-accent-soft: #ecf5ff;
  --app-accent-border: #d9ecff;
}

:root[data-theme='dark'] {
  --app-bg: #071422;
  --app-surface: #102236;
  --app-surface-muted: #172c42;
  --app-border: #34465a;
  --app-border-soft: #283e54;
  --app-text: #e5edf5;
  --app-text-secondary: #a8b9ca;
  --app-text-muted: #8499ad;
  --app-accent: #66b1ff;
  --app-accent-soft: #163755;
  --app-accent-border: #27547a;
}

body {
  background: var(--app-bg);
  color: var(--app-text);
}
</style>
