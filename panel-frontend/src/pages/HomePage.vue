<template>
  <main class="space-console" :style="{ '--earth-background': earthBackground }">
    <header class="console-header">
      <div>
        <h1>天基云网节点态势</h1>
        <p class="subtitle">集群资源实时监测</p>
      </div>
      <div class="header-actions">
        <div class="system-state">
          <span class="state-dot"></span>
          {{ readyCount }}/{{ linkedSatellites.length }} 节点在线
        </div>
        <button class="refresh-button" type="button" :disabled="loading" @click="loadNodeData">
          <span :class="{ spinning: loading }">↻</span>
          刷新
        </button>
      </div>
    </header>

    <section class="orbital-stage" aria-label="集群节点卫星轨道图">
      <div class="stars stars-far"></div>
      <div class="stars stars-near"></div>
      <div class="earth-glow"></div>
      <div class="earth" aria-hidden="true"></div>

      <svg class="orbit-lines" viewBox="0 0 1000 650" preserveAspectRatio="none" aria-hidden="true">
        <ellipse class="orbit orbit-high" cx="500" cy="330" rx="455" ry="286" />
        <ellipse class="orbit orbit-medium" cx="500" cy="340" rx="365" ry="218" />
        <ellipse class="orbit-flow" cx="500" cy="340" rx="365" ry="218" />
      </svg>

      <div class="earth-caption">
        <span>SPACE · CLOUD · SECURITY</span>
        <strong>云网资源核心</strong>
      </div>

      <component
        :is="satellite.decorative ? 'div' : 'button'"
        v-for="satellite in satellites"
        :key="satellite.nodeName"
        :type="satellite.decorative ? undefined : 'button'"
        class="satellite"
        :class="[
          `satellite-${satellite.orbit}`,
          satellite.decorative ? 'satellite-decorative' : nodeState(satellite.nodeName).statusClass,
          { selected: !satellite.decorative && selectedNodeName === satellite.nodeName },
        ]"
        :style="satelliteStyle(satellite)"
        :aria-label="satellite.decorative ? undefined : `查看${satellite.displayName}资源使用情况`"
        :aria-hidden="satellite.decorative ? true : undefined"
        @click="selectSatellite(satellite)"
      >
        <span class="satellite-label">{{ satellite.displayName }}</span>
        <span class="satellite-model" aria-hidden="true">
          <span class="solar-panel panel-left"></span>
          <span class="satellite-body">
            <span class="satellite-lens"></span>
          </span>
          <span class="solar-panel panel-right"></span>
          <span class="signal-wave wave-one"></span>
          <span class="signal-wave wave-two"></span>
        </span>
        <span class="satellite-status" :class="{ 'satellite-status-spacer': satellite.decorative }">
          {{ satellite.decorative ? '占位' : nodeState(satellite.nodeName).statusText }}
        </span>
      </component>

      <aside v-if="selectedSatellite" class="resource-panel">
        <div class="panel-heading">
          <div>
            <h2>{{ selectedSatellite.displayName }}</h2>
            <p>{{ selectedSatellite.nodeName }}</p>
          </div>
          <span class="panel-status" :class="selectedState.statusClass">
            <i></i>{{ selectedState.statusText }}
          </span>
        </div>

        <div class="metric-block">
          <div class="metric-title">
            <span>{{ selectedState.memoryTitle }}</span>
            <strong>{{ selectedState.memoryLabel }}</strong>
          </div>
          <div class="metric-track">
            <span :style="{ width: `${selectedState.memoryPercent}%` }"></span>
          </div>
          <div class="metric-foot">
            <span>{{ selectedState.usageKind === 'allocated' ? '分配率' : '使用率' }}
              {{ selectedState.memoryPercent }}%</span>
            <span>{{ selectedState.memoryUsed }} / {{ selectedState.memoryTotal }}</span>
          </div>
        </div>

        <div class="metric-block">
          <div class="metric-title">
            <span>{{ selectedState.cpuTitle }}</span>
            <strong>{{ selectedState.cpuLabel }}</strong>
          </div>
          <div class="metric-track cpu-track">
            <span :style="{ width: `${selectedState.cpuPercent}%` }"></span>
          </div>
          <div class="metric-foot">
            <span>{{ selectedState.usageKind === 'allocated' ? '分配率' : '使用率' }}
              {{ selectedState.cpuPercent }}%</span>
            <span>{{ selectedState.cpuUsed }} / {{ selectedState.cpuTotal }}</span>
          </div>
        </div>

        <div class="panel-footer">
          <span>更新于 {{ lastUpdated || '--:--:--' }}</span>
          <button type="button" @click="openNodeDetail(selectedSatellite.nodeName)">
            查看节点详情 →
          </button>
        </div>
      </aside>

      <div class="orbit-legend">
        <span><i class="legend-ready"></i>运行正常</span>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getNodeMetrics, getNodes } from '@/api/clusterController'
import earthImage from '@/assets/space-earth-background.png'

type OrbitLevel = 'medium' | 'high'

interface SatelliteDefinition {
  nodeName: string
  displayName: string
  orbit: OrbitLevel
  x: number
  y: number
  delay: number
  /** 仅轨道图示，不关联集群节点 */
  decorative?: boolean
}

interface NodeDisplayState {
  statusText: string
  statusClass: 'status-ready' | 'status-error' | 'status-unknown'
  memoryTitle: string
  cpuTitle: string
  usageKind: 'actual' | 'allocated'
  memoryUsed: string
  memoryTotal: string
  memoryLabel: string
  memoryPercent: number
  cpuUsed: string
  cpuTotal: string
  cpuLabel: string
  cpuPercent: number
}

const router = useRouter()
const earthBackground = `url("${earthImage}")`
const nodes = ref<API.NodeVO[]>([])
const metrics = ref<API.NodeMetricsVO[]>([])
const loading = ref(false)
const lastUpdated = ref('')
const selectedNodeName = ref('master')
let refreshTimer: number | undefined

const satellites: SatelliteDefinition[] = [
  {
    nodeName: 'decorative-high-relay',
    displayName: '云网总控卫星3',
    orbit: 'high',
    x: 34,
    y: 11,
    delay: 0,
    decorative: true,
  },
  {
    nodeName: 'decorative-high-nav',
    displayName: '云网总控卫星2',
    orbit: 'high',
    x: 58,
    y: 11,
    delay: 0,
    decorative: true,
  },
  {
    nodeName: 'master',
    displayName: '云网总控卫星',
    orbit: 'high',
    x: 82,
    y: 11,
    delay: 0,
  },
  {
    nodeName: 'node5-desktop',
    displayName: '协同计算节点',
    orbit: 'medium',
    x: 49,
    y: 22,
    delay: -0.4,
  },
  {
    nodeName: 'node6-desktop',
    displayName: '协同计算节点',
    orbit: 'medium',
    x: 73,
    y: 30,
    delay: -1.2,
  },
  {
    nodeName: 'node7-desktop',
    displayName: '协同计算节点',
    orbit: 'medium',
    x: 86,
    y: 52,
    delay: -2,
  },
  {
    nodeName: 'node8-desktop',
    displayName: '协同计算节点',
    orbit: 'medium',
    x: 66,
    y: 73,
    delay: -2.8,
  },
  {
    nodeName: 'worker2',
    displayName: '协同计算节点',
    orbit: 'medium',
    x: 50,
    y: 87,
    delay: -3.6,
  },
  {
    nodeName: 'worker3',
    displayName: '协同计算节点',
    orbit: 'medium',
    x: 25,
    y: 77,
    delay: -4.4,
  },
  {
    nodeName: 'worker4',
    displayName: '协同计算节点',
    orbit: 'medium',
    x: 13,
    y: 53,
    delay: -5.2,
  },
  {
    nodeName: 'worker8',
    displayName: '协同计算节点',
    orbit: 'medium',
    x: 25,
    y: 30,
    delay: -6,
  },
]

const linkedSatellites = satellites.filter((satellite) => !satellite.decorative)

const selectedSatellite = computed(
  () => linkedSatellites.find((item) => item.nodeName === selectedNodeName.value) ?? linkedSatellites[0],
)

const selectedState = computed(() => nodeState(selectedNodeName.value))

const readyCount = computed(
  () =>
    linkedSatellites.filter(
      (satellite) => nodeState(satellite.nodeName).statusClass === 'status-ready',
    ).length,
)

function satelliteStyle(satellite: SatelliteDefinition) {
  return {
    left: `${satellite.x}%`,
    top: `${satellite.y}%`,
    '--float-delay': `${satellite.delay}s`,
  }
}

function selectSatellite(satellite: SatelliteDefinition) {
  if (satellite.decorative) return
  selectedNodeName.value = satellite.nodeName
}

function isNodeReady(node?: API.NodeVO) {
  return node?.status?.conditions?.find((condition) => condition.type === 'Ready')?.status === 'True'
}

function parseKubernetesQuantity(value?: string): number | null {
  if (!value) return null
  const match = value.trim().match(/^([+-]?\d+(?:\.\d+)?)([a-zA-Z]+)?$/)
  if (!match) return null

  const amount = Number(match[1])
  const suffix = match[2] || ''
  const multipliers: Record<string, number> = {
    n: 1e-9,
    u: 1e-6,
    m: 1e-3,
    k: 1e3,
    K: 1e3,
    M: 1e6,
    G: 1e9,
    T: 1e12,
    Ki: 1024,
    Mi: 1024 ** 2,
    Gi: 1024 ** 3,
    Ti: 1024 ** 4,
  }
  return amount * (multipliers[suffix] ?? 1)
}

function formatMemory(bytes: number | null) {
  if (bytes == null) return '暂无数据'
  const gibibytes = bytes / 1024 ** 3
  if (gibibytes >= 1) return `${gibibytes.toFixed(gibibytes >= 10 ? 1 : 2)} GiB`
  return `${(bytes / 1024 ** 2).toFixed(0)} MiB`
}

function formatCpu(cores: number | null) {
  if (cores == null) return '暂无数据'
  return cores < 1 ? `${Math.round(cores * 1000)}m` : `${cores.toFixed(cores >= 10 ? 1 : 2)} 核`
}

function usagePercent(used: number | null, total: number | null) {
  if (used == null || !total) return 0
  return Math.min(100, Math.max(0, Math.round((used / total) * 100)))
}

function nodeState(nodeName: string): NodeDisplayState {
  const node = nodes.value.find((item) => item.metadata?.name === nodeName)
  const metric = metrics.value.find((item) => item.metadata?.name === nodeName)
  const memoryUsedBytes = parseKubernetesQuantity(metric?.usage?.memory)
  const memoryTotalBytes = parseKubernetesQuantity(node?.status?.capacity?.memory)
  const cpuUsedCores = parseKubernetesQuantity(metric?.usage?.cpu)
  const cpuTotalCores = parseKubernetesQuantity(node?.status?.capacity?.cpu)
  const hasNode = Boolean(node)
  const ready = isNodeReady(node)
  const allocated = metric?.source === 'allocated'
  const hasUsage = memoryUsedBytes != null || cpuUsedCores != null

  return {
    statusText: !hasNode ? '未连接' : ready ? '运行正常' : '节点异常',
    statusClass: !hasNode ? 'status-unknown' : ready ? 'status-ready' : 'status-error',
    memoryTitle: allocated ? '已分配内存' : '内存使用',
    cpuTitle: allocated ? '已分配 CPU' : 'CPU 使用',
    usageKind: allocated ? 'allocated' : 'actual',
    memoryUsed: formatMemory(memoryUsedBytes),
    memoryTotal: formatMemory(memoryTotalBytes),
    memoryLabel:
      memoryUsedBytes != null && memoryTotalBytes
        ? `${usagePercent(memoryUsedBytes, memoryTotalBytes)}%`
        : hasUsage
          ? '采集中'
          : '暂无指标',
    memoryPercent: usagePercent(memoryUsedBytes, memoryTotalBytes),
    cpuUsed: formatCpu(cpuUsedCores),
    cpuTotal: formatCpu(cpuTotalCores),
    cpuLabel:
      cpuUsedCores != null && cpuTotalCores
        ? `${usagePercent(cpuUsedCores, cpuTotalCores)}%`
        : hasUsage
          ? '采集中'
          : '暂无指标',
    cpuPercent: usagePercent(cpuUsedCores, cpuTotalCores),
  }
}

async function loadNodeData() {
  if (loading.value) return
  loading.value = true
  const [nodeResult, metricResult] = await Promise.allSettled([getNodes(), getNodeMetrics()])

  if (nodeResult.status === 'fulfilled' && nodeResult.value.data?.code === 0) {
    nodes.value = nodeResult.value.data.data?.items ?? []
  }
  if (metricResult.status === 'fulfilled' && metricResult.value.data?.code === 0) {
    metrics.value = metricResult.value.data.data?.items ?? []
  }

  lastUpdated.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  loading.value = false
}

function openNodeDetail(nodeName: string) {
  router.push(`/nodes/${encodeURIComponent(nodeName)}`)
}

onMounted(() => {
  loadNodeData()
  refreshTimer = window.setInterval(loadNodeData, 30_000)
})

onBeforeUnmount(() => {
  if (refreshTimer) window.clearInterval(refreshTimer)
})
</script>

<style scoped>
.space-console {
  --cyan: #42e8ff;
  --blue: #2f7cff;
  position: relative;
  min-height: calc(100vh - 64px);
  overflow: hidden;
  background: #020916;
  color: #fff;
  font-family:
    Inter, "Microsoft YaHei", "PingFang SC", sans-serif;
}

.console-header {
  position: absolute;
  z-index: 20;
  top: 0;
  right: 0;
  left: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 30px 38px;
  pointer-events: none;
}

.console-header h1 {
  margin: 0;
  color: #f2fbff;
  font-size: clamp(32px, 2.9vw, 48px);
  font-weight: 600;
  letter-spacing: 0.08em;
  text-shadow: 0 0 24px rgba(66, 232, 255, 0.2);
}

.subtitle {
  margin: 8px 0 0;
  color: rgba(214, 236, 248, 0.82);
  font-size: 19px;
  letter-spacing: 0.18em;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  pointer-events: auto;
}

.system-state,
.refresh-button {
  height: 46px;
  border: 1px solid rgba(108, 220, 244, 0.2);
  border-radius: 4px;
  background: rgba(6, 27, 48, 0.68);
  color: rgba(220, 244, 251, 0.82);
  font-size: 18px;
  backdrop-filter: blur(12px);
}

.system-state {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 13px;
}

.state-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #4fffb0;
  box-shadow: 0 0 9px #4fffb0;
}

.refresh-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.refresh-button:hover {
  border-color: rgba(66, 232, 255, 0.65);
}

.refresh-button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.refresh-button span {
  color: var(--cyan);
  font-size: 24px;
}

.spinning {
  animation: spin 0.8s linear infinite;
}

.orbital-stage {
  position: relative;
  min-height: calc(100vh - 64px);
  background-color: #071422;
  background-image:
    linear-gradient(180deg, rgba(4, 16, 36, 0.05), rgba(4, 14, 32, 0.22)),
    var(--earth-background);
  background-position: center, center calc(50% + 40px);
  background-size: auto, cover;
  background-repeat: no-repeat;
  isolation: isolate;
}

.orbital-stage::before {
  position: absolute;
  z-index: 1;
  inset: 0;
  background:
    radial-gradient(ellipse at 50% 58%, rgba(56, 168, 235, 0.2) 0%, transparent 42%),
    linear-gradient(90deg, rgba(3, 14, 32, 0.38) 0%, transparent 24%, transparent 76%, rgba(3, 14, 32, 0.22) 100%),
    linear-gradient(180deg, rgba(3, 14, 32, 0.16) 0%, transparent 22%, transparent 68%, rgba(3, 14, 32, 0.34) 100%);
  content: "";
  pointer-events: none;
}

.stars {
  position: absolute;
  z-index: 2;
  inset: 0;
  pointer-events: none;
}

.stars-far {
  background-image:
    radial-gradient(circle at 18% 28%, rgba(156, 234, 255, 0.8) 0 1px, transparent 1.5px),
    radial-gradient(circle at 78% 64%, rgba(156, 234, 255, 0.65) 0 1px, transparent 1.5px),
    radial-gradient(circle at 66% 23%, rgba(255, 255, 255, 0.72) 0 1px, transparent 1.5px);
  background-size: 210px 190px, 270px 250px, 330px 310px;
  animation: starDrift 35s linear infinite;
}

.stars-near {
  background-image:
    radial-gradient(circle at 38% 74%, rgba(91, 218, 255, 0.9) 0 1px, transparent 2px),
    radial-gradient(circle at 86% 16%, rgba(255, 255, 255, 0.8) 0 1px, transparent 2px);
  background-size: 460px 390px, 520px 470px;
  animation: starPulse 4s ease-in-out infinite alternate;
}

.earth {
  position: absolute;
  z-index: 3;
  top: calc(50% + 40px);
  left: 50%;
  width: min(31vw, 500px);
  aspect-ratio: 1;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  box-shadow:
    0 0 48px rgba(72, 198, 255, 0.28),
    0 0 140px rgba(28, 120, 210, 0.22);
  animation: earthBreath 5s ease-in-out infinite alternate;
}

.earth-glow {
  position: absolute;
  z-index: 2;
  top: calc(50% + 40px);
  left: 50%;
  width: min(52vw, 820px);
  aspect-ratio: 1;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(86, 198, 255, 0.08) 36%, rgba(46, 150, 235, 0.28) 56%, transparent 72%);
  transform: translate(-50%, -50%);
  animation: earthBreath 5s ease-in-out infinite alternate;
}

.orbit-lines {
  position: absolute;
  z-index: 4;
  inset: 12% 5% 0;
  width: 90%;
  height: 88%;
  overflow: visible;
  pointer-events: none;
}

.orbit {
  fill: none;
  vector-effect: non-scaling-stroke;
}

.orbit-high {
  stroke: rgba(255, 196, 92, 0.42);
  stroke-dasharray: 2 8;
}

.orbit-medium {
  stroke: rgba(126, 230, 255, 0.55);
  stroke-width: 1.2;
  stroke-dasharray: 3 7;
}

.orbit-flow {
  fill: none;
  stroke: rgba(106, 232, 255, 0.72);
  stroke-width: 1.1;
  stroke-linecap: round;
  stroke-dasharray: 70 700;
  vector-effect: non-scaling-stroke;
  animation: orbitFlow 10s linear infinite;
  filter: drop-shadow(0 0 5px rgba(66, 232, 255, 0.6));
}

.earth-caption {
  position: absolute;
  z-index: 6;
  top: calc(70% + 40px);
  left: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  transform: translateX(-50%);
  text-align: center;
  text-shadow: 0 1px 10px #010915;
}

.earth-caption span {
  color: rgba(94, 221, 250, 0.62);
  font-size: 15px;
  letter-spacing: 0.26em;
}

.earth-caption strong {
  margin-top: 4px;
  color: rgba(228, 248, 255, 0.85);
  font-size: 19px;
  font-weight: 500;
  letter-spacing: 0.18em;
}

.satellite {
  position: absolute;
  z-index: 10;
  display: flex;
  width: 220px;
  flex-direction: column;
  align-items: center;
  padding: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: #dff8ff;
  cursor: pointer;
  transform: translate(-50%, -50%);
}

.satellite::before {
  position: absolute;
  top: 36px;
  left: 50%;
  width: 38px;
  height: 38px;
  border: 1px solid rgba(68, 222, 255, 0.22);
  border-radius: 50%;
  content: "";
  transform: translate(-50%, -50%);
  transition:
    width 0.25s ease,
    height 0.25s ease,
    border-color 0.25s ease;
}

.satellite:hover,
.satellite:focus-visible,
.satellite.selected {
  z-index: 12;
}

.satellite:hover::before,
.satellite:focus-visible::before,
.satellite.selected::before {
  width: 58px;
  height: 58px;
  border-color: rgba(78, 229, 255, 0.55);
  box-shadow: 0 0 22px rgba(43, 194, 255, 0.18);
}

.satellite-label {
  min-height: 28px;
  padding: 3px 8px;
  border: 1px solid transparent;
  border-radius: 3px;
  color: rgba(222, 247, 255, 0.88);
  font-size: 17px;
  line-height: 22px;
  white-space: nowrap;
  text-shadow: 0 1px 8px #000;
  transition:
    color 0.2s ease,
    background 0.2s ease,
    border-color 0.2s ease;
}

.satellite:hover .satellite-label,
.satellite:focus-visible .satellite-label,
.satellite.selected .satellite-label {
  border-color: rgba(77, 222, 255, 0.32);
  background: rgba(4, 26, 47, 0.84);
  color: #fff;
}

.satellite-model {
  position: relative;
  display: flex;
  height: 32px;
  align-items: center;
  margin: 1px 0 0;
  animation: satelliteFloat 3.8s ease-in-out infinite;
  animation-delay: var(--float-delay);
  filter: drop-shadow(0 0 5px rgba(67, 211, 255, 0.5));
}

.satellite-body {
  position: relative;
  z-index: 2;
  width: 21px;
  height: 22px;
  border: 1px solid #9befff;
  border-radius: 4px;
  background: linear-gradient(135deg, #d9faff 0%, #4aa5c4 44%, #102d4e 100%);
  box-shadow: inset 0 0 5px rgba(255, 255, 255, 0.55);
}

.satellite-body::before {
  position: absolute;
  top: -7px;
  left: 8px;
  width: 5px;
  height: 7px;
  background: #7dddf2;
  content: "";
}

.satellite-body::after {
  position: absolute;
  top: -11px;
  left: 5px;
  width: 11px;
  height: 5px;
  border: 1px solid #7dddf2;
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
  content: "";
}

.satellite-lens {
  position: absolute;
  top: 7px;
  left: 7px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #a8f7ff;
  box-shadow: 0 0 8px #5feaff;
}

.solar-panel {
  position: relative;
  width: 25px;
  height: 12px;
  border: 1px solid #4fb9dc;
  background:
    linear-gradient(90deg, transparent 46%, rgba(121, 225, 255, 0.55) 48% 52%, transparent 54%),
    linear-gradient(0deg, transparent 44%, rgba(121, 225, 255, 0.4) 46% 54%, transparent 56%),
    linear-gradient(135deg, #123862, #1978a3);
}

.panel-left {
  border-radius: 2px 0 0 2px;
}

.panel-right {
  border-radius: 0 2px 2px 0;
}

.signal-wave {
  position: absolute;
  top: -12px;
  left: 50%;
  width: 15px;
  height: 8px;
  border: 1px solid rgba(101, 237, 255, 0.75);
  border-right-color: transparent;
  border-bottom-color: transparent;
  border-left-color: transparent;
  border-radius: 50%;
  transform: translateX(-50%);
  animation: signal 2s ease-out infinite;
}

.wave-two {
  animation-delay: 0.8s;
}

.satellite-status {
  position: relative;
  margin-top: 1px;
  padding-left: 12px;
  color: rgba(198, 228, 238, 0.82);
  font-size: 15px;
}

.satellite-status::before {
  position: absolute;
  top: 50%;
  left: 1px;
  transform: translateY(-50%);
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #8095a1;
  content: "";
}

.status-ready .satellite-status::before {
  background: #42f0a0;
  box-shadow: 0 0 5px #42f0a0;
}

.status-error .satellite-status::before {
  background: #ff657f;
  box-shadow: 0 0 5px #ff657f;
}

.satellite-high .satellite-label {
  color: #ffd58a;
}

.satellite-high .satellite-model {
  filter: drop-shadow(0 0 7px rgba(255, 190, 76, 0.65));
}

.satellite-high::before {
  border-color: rgba(255, 191, 76, 0.32);
}

.satellite-decorative {
  z-index: 9;
  width: 176px;
  cursor: default;
  pointer-events: none;
}

.satellite-status-spacer {
  visibility: hidden;
}

.resource-panel {
  position: absolute;
  z-index: 15;
  right: 34px;
  bottom: 34px;
  width: min(480px, calc(100% - 68px));
  padding: 18px 19px 15px;
  border: 1px solid rgba(120, 220, 255, 0.42);
  border-radius: 6px;
  background:
    linear-gradient(160deg, rgba(22, 72, 112, 0.82), rgba(8, 32, 58, 0.88));
  box-shadow:
    0 16px 36px rgba(0, 8, 20, 0.28),
    0 0 28px rgba(48, 168, 230, 0.16),
    inset 0 1px rgba(190, 240, 255, 0.16);
  backdrop-filter: blur(16px);
}

.resource-panel::before,
.resource-panel::after {
  position: absolute;
  width: 15px;
  height: 15px;
  content: "";
  pointer-events: none;
}

.resource-panel::before {
  top: -1px;
  left: -1px;
  border-top: 2px solid var(--cyan);
  border-left: 2px solid var(--cyan);
}

.resource-panel::after {
  right: -1px;
  bottom: -1px;
  border-right: 2px solid var(--cyan);
  border-bottom: 2px solid var(--cyan);
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 17px;
}

.panel-heading h2 {
  margin: 0 0 1px;
  color: #f0fbff;
  font-size: 24px;
  font-weight: 600;
}

.panel-heading p {
  margin: 0;
  color: rgba(176, 214, 228, 0.72);
  font-size: 16px;
}

.panel-status {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  color: rgba(190, 222, 231, 0.68);
  font-size: 16px;
  white-space: nowrap;
}

.panel-status i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #8095a1;
}

.panel-status.status-ready i {
  background: #42f0a0;
  box-shadow: 0 0 7px #42f0a0;
}

.panel-status.status-error i {
  background: #ff657f;
  box-shadow: 0 0 7px #ff657f;
}

.metric-block + .metric-block {
  margin-top: 14px;
}

.metric-title,
.metric-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.metric-title {
  color: rgba(194, 224, 234, 0.72);
  font-size: 17px;
}

.metric-title strong {
  color: #dff9ff;
  font-size: 18px;
  font-weight: 600;
}

.metric-track {
  height: 5px;
  margin: 7px 0 5px;
  overflow: hidden;
  border-radius: 4px;
  background: rgba(84, 157, 180, 0.14);
}

.metric-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #1e8fff, #4eecff);
  box-shadow: 0 0 10px rgba(65, 223, 255, 0.55);
  transition: width 0.55s ease;
}

.cpu-track span {
  background: linear-gradient(90deg, #715bff, #b678ff);
  box-shadow: 0 0 10px rgba(169, 107, 255, 0.5);
}

.metric-foot {
  color: rgba(145, 190, 205, 0.46);
  font-size: 15px;
}

.panel-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 11px;
  border-top: 1px solid rgba(94, 190, 216, 0.12);
}

.panel-footer span {
  color: rgba(140, 183, 197, 0.38);
  font-size: 15px;
}

.panel-footer button {
  padding: 0;
  border: 0;
  background: transparent;
  color: #55daf1;
  font-size: 16px;
  cursor: pointer;
}

.orbit-legend {
  position: absolute;
  z-index: 15;
  bottom: 35px;
  left: 37px;
  display: flex;
  gap: 18px;
  color: rgba(198, 228, 238, 0.78);
  font-size: 15px;
  letter-spacing: 0.04em;
}

.orbit-legend span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.orbit-legend i {
  width: 15px;
  height: 1px;
  background: rgba(126, 230, 255, 0.85);
}

.orbit-legend .legend-ready {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #42f0a0;
  box-shadow: 0 0 5px #42f0a0;
}

@keyframes orbitFlow {
  to {
    stroke-dashoffset: -770;
  }
}

@keyframes satelliteFloat {
  0%,
  100% {
    transform: translateY(-2px);
  }
  50% {
    transform: translateY(4px);
  }
}

@keyframes signal {
  0% {
    opacity: 0;
    transform: translateX(-50%) scale(0.5);
  }
  30% {
    opacity: 0.9;
  }
  100% {
    opacity: 0;
    transform: translateX(-50%) translateY(-6px) scale(1.6);
  }
}

@keyframes earthBreath {
  to {
    opacity: 0.82;
    transform: translate(-50%, -50%) scale(1.025);
  }
}

@keyframes starDrift {
  to {
    background-position: 210px 190px, -270px 250px, 330px -310px;
  }
}

@keyframes starPulse {
  to {
    opacity: 0.45;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1050px) {
  .satellite {
    width: 176px;
  }

  .satellite-decorative {
    width: 148px;
  }

  .satellite-label {
    overflow: hidden;
    max-width: 176px;
    text-overflow: ellipsis;
  }

  .resource-panel {
    width: 400px;
  }

  .orbit-legend {
    display: none;
  }
}

@media (max-width: 760px) {
  .space-console,
  .orbital-stage {
    min-height: 760px;
  }

  .console-header {
    padding: 22px 20px;
  }

  .subtitle,
  .system-state {
    display: none;
  }

  .orbital-stage {
    background-position: center, center calc(50% + 40px);
  }

  .satellite {
    width: 120px;
  }

  .satellite-decorative {
    width: 108px;
  }

  .satellite-label {
    max-width: 120px;
    font-size: 15px;
  }

  .satellite-status {
    display: none;
  }

  .resource-panel {
    right: 20px;
    bottom: 20px;
    left: 20px;
    width: auto;
  }
}

@media (prefers-reduced-motion: reduce) {
  .stars,
  .earth,
  .earth-glow,
  .orbit-flow,
  .satellite-model,
  .signal-wave {
    animation: none;
  }
}
</style>
