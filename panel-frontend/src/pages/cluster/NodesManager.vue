<script setup lang="ts">
import { getNodes } from '@/api/clusterController.ts'
import { ref } from 'vue'

const nodes = ref<API.Node[]>([])
getNodes().then((res) => {
  console.log(res.data.items)
  nodes.value = res.data.items
})
</script>

<template>
  {{ nodes }}
</template>

<style scoped></style>
