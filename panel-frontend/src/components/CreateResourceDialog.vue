<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { createPod } from '@/api/clusterController.ts'

const router = useRouter()
const visible = ref(false)
const resourceType = ref<'Pod'>('Pod')
const yamlContent = ref('')
const deploying = ref(false)
const lineNumberOffset = ref(0)

const resourceOptions = [{ label: 'Pod', value: 'Pod' }]

const lineNumbers = computed(() => {
  const lineCount = Math.max(yamlContent.value.split('\n').length, 1)
  return Array.from({ length: lineCount }, (_, index) => index + 1)
})

const open = () => {
  visible.value = true
}

const handleCancel = () => {
  visible.value = false
}

const handleEditorScroll = (event: Event) => {
  lineNumberOffset.value = (event.target as HTMLTextAreaElement).scrollTop
}

const handleDeploy = async () => {
  if (!yamlContent.value.trim()) {
    message.warning('请输入 YAML 配置')
    return
  }
  if (resourceType.value !== 'Pod') {
    message.warning('当前仅支持创建 Pod')
    return
  }

  deploying.value = true
  try {
    const response = await createPod({ yaml: yamlContent.value })
    if (response.data.code !== 0) {
      return
    }

    message.success('部署成功')
    visible.value = false

    const pod = response.data.data
    const namespace = pod?.metadata?.namespace
    const name = pod?.metadata?.name
    if (namespace && name) {
      router.push(`/pods/${namespace}/${name}`)
    }
  } catch (error) {
    console.error('部署 Pod 失败:', error)
    message.error('部署失败')
  } finally {
    deploying.value = false
  }
}

defineExpose({
  open,
})
</script>

<template>
  <a-modal
    v-model:open="visible"
    title="创建资源"
    width="960px"
    :footer="null"
    :destroy-on-close="false"
    wrap-class-name="create-resource-modal"
    @cancel="handleCancel"
  >
    <div class="dialog-body">
      <p class="dialog-description">粘贴 Kubernetes 资源 YAML 配置并部署到集群</p>

      <div class="form-block">
        <label class="form-label">资源类型</label>
        <a-select v-model:value="resourceType" class="resource-select" :options="resourceOptions" />
      </div>

      <div class="form-block">
        <label class="form-label">YAML 配置</label>
        <div class="yaml-editor">
          <div class="line-number-panel">
            <div
              class="line-number-inner"
              :style="{ transform: `translateY(-${lineNumberOffset}px)` }"
            >
              <span v-for="line in lineNumbers" :key="line">{{ line }}</span>
            </div>
          </div>
          <textarea
            v-model="yamlContent"
            class="yaml-textarea"
            spellcheck="false"
            placeholder="请输入 Pod YAML 配置"
            @scroll="handleEditorScroll"
          ></textarea>
        </div>
      </div>

      <div class="dialog-footer">
        <a-button @click="handleCancel">取消</a-button>
        <a-button type="primary" :loading="deploying" @click="handleDeploy">部署</a-button>
      </div>
    </div>
  </a-modal>
</template>

<style scoped>
.dialog-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.dialog-description {
  margin: -4px 0 2px;
  color: #666;
  font-size: 15px;
  line-height: 1.6;
}

.form-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  color: #1f1f1f;
  font-size: 14px;
  font-weight: 600;
}

.resource-select {
  width: 180px;
}

.yaml-editor {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  height: clamp(300px, 44vh, 460px);
  overflow: hidden;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
}

.line-number-panel {
  overflow: hidden;
  border-right: 1px solid #ececec;
  background: #fafafa;
}

.line-number-inner {
  padding: 10px 0;
  color: #36558f;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 14px;
  line-height: 22px;
  text-align: center;
  will-change: transform;
}

.line-number-inner span {
  display: block;
  height: 22px;
}

.yaml-textarea {
  width: 100%;
  height: 100%;
  padding: 10px 12px;
  border: 0;
  outline: none;
  resize: none;
  color: #1f1f1f;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 14px;
  line-height: 22px;
  tab-size: 2;
}

.yaml-textarea::placeholder {
  color: #bfbfbf;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:global(.create-resource-modal .ant-modal) {
  top: 48px;
  padding-bottom: 48px;
}
</style>
