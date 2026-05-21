<template>
  <div class="home-page">
    <section class="hero" :style="{ '--hero-image': heroBackground }">
      <div class="hero-copy">
        <p class="eyebrow">云网安全控制台</p>
        <h1>集群安全概览</h1>
        <p class="summary">
          统一监控节点、Pod、命名空间和服务暴露情况，快速掌握集群运行与安全状态。
        </p>
        <a-space wrap>
          <a-button type="primary" @click="router.push('/nodes')">查看节点</a-button>
          <a-button @click="router.push('/services')">检查服务</a-button>
        </a-space>
      </div>
    </section>

    <section class="status-grid">
      <a-card class="status-card" :bordered="false">
        <span class="label">接口健康状态</span>
        <strong>{{ health || '检测中...' }}</strong>
      </a-card>
      <a-card
        v-for="item in resourceLinks"
        :key="item.path"
        class="status-card link-card"
        :bordered="false"
        @click="router.push(item.path)"
      >
        <span class="label">{{ item.label }}</span>
        <strong>{{ item.title }}</strong>
      </a-card>
    </section>
  </div>
</template>

<script setup lang="ts">
import { healthCheck } from '@/api/healthController.ts'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import heroImage from '@/assets/cluster-security-hero.png'

const router = useRouter()
const health = ref('')
const heroBackground = `url("${heroImage}")`
const resourceLinks = [
  { title: '节点', label: '基础设施', path: '/nodes' },
  { title: 'Pod', label: '工作负载', path: '/pods' },
  { title: '命名空间', label: '隔离域', path: '/namespaces' },
  { title: '服务', label: '网络入口', path: '/services' },
]

onMounted(() => {
  healthCheck().then((res) => {
    health.value = res.data?.data || ''
    console.log(res)
  })
})
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.hero {
  min-height: 340px;
  display: flex;
  align-items: center;
  padding: 40px;
  overflow: hidden;
  border-radius: 8px;
  background:
    linear-gradient(90deg, rgba(3, 18, 38, 0.94) 0%, rgba(3, 18, 38, 0.78) 38%, rgba(3, 18, 38, 0.1) 72%),
    var(--hero-image) center / cover no-repeat;
  color: #fff;
}

.hero-copy {
  width: min(520px, 100%);
}

.eyebrow {
  margin: 0 0 10px;
  color: #7dd3fc;
  font-size: 13px;
  font-weight: 600;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #fff;
  font-size: 34px;
  line-height: 1.16;
}

.summary {
  margin: 16px 0 24px;
  color: rgba(255, 255, 255, 0.78);
  font-size: 16px;
  line-height: 1.7;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.status-card {
  min-height: 104px;
  border-radius: 8px;
  background: #f8fafc;
}

.link-card {
  cursor: pointer;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.link-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

.label {
  display: block;
  margin-bottom: 8px;
  color: #64748b;
  font-size: 13px;
}

strong {
  color: #0f172a;
  font-size: 22px;
  line-height: 1.25;
}

@media (max-width: 720px) {
  .hero {
    min-height: 420px;
    align-items: flex-start;
    padding: 28px;
    background:
      linear-gradient(180deg, rgba(3, 18, 38, 0.96) 0%, rgba(3, 18, 38, 0.8) 48%, rgba(3, 18, 38, 0.15) 100%),
      var(--hero-image) center bottom / cover no-repeat;
  }

  h1 {
    font-size: 28px;
  }
}
</style>
