<script setup lang="ts">
import EmbeddedConsole from '@/components/EmbeddedConsole.vue'

const BACKUP_CONSOLE_URL = 'http://192.168.5.96:30876/'
</script>

<template>
  <EmbeddedConsole :url="BACKUP_CONSOLE_URL" title="备份控制台" feature-name="备份管理" />
</template>
