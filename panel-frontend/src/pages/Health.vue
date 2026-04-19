<template>
  <div>
    <h1>{{ health }}</h1>
  </div>
</template>

<script setup lang="ts">
import { healthCheck } from '@/api/healthController.ts'
import { onMounted, ref } from 'vue'

const health = ref('')

onMounted(() => {
  healthCheck().then((res) => {
    health.value = res.data?.data || ''
    console.log(res)
  })
})
</script>
