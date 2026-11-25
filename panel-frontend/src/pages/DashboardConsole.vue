<script setup lang="ts">
import EmbeddedConsole from '@/components/EmbeddedConsole.vue'

// Grafana 禁止 iframe（X-Frame-Options: deny），且自身 appUrl 指向 localhost:3000。
// 经 Vite 同源代理后才能内嵌，并由开发机去访问集群上的 192.168.5.96:30300。
const DASHBOARD_CONSOLE_URL = '/grafana-proxy/'
</script>

<template>
  <EmbeddedConsole
    :url="DASHBOARD_CONSOLE_URL"
    title="云网监控仪表盘"
    feature-name="云网监控仪表盘"
  />
</template>
