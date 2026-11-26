<script setup lang="ts">
import EmbeddedConsole from '@/components/EmbeddedConsole.vue'

const MONITOR_CONSOLE_URL = 'http://192.168.5.96:32080/'
</script>

<template>
  <EmbeddedConsole :url="MONITOR_CONSOLE_URL" title="监控告警平台" feature-name="监控告警" />
</template>
