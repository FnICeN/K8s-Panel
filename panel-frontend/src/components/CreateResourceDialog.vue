<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  createDeployment,
  createNamespace,
  createPod,
  createService,
} from '@/api/clusterController.ts'

type ResourceType = 'Pod' | 'Deployment' | 'Service' | 'Namespace'

type CreatedMeta = { namespace?: string; name?: string } | undefined

const resourceConfigs: Record<
  ResourceType,
  {
    create: (yaml: string) => Promise<{ code?: number; metadata?: CreatedMeta }>
    detailPath: (meta: CreatedMeta) => string | undefined
  }
> = {
  Pod: {
    create: async (yaml) => {
      const response = await createPod({ yaml })
      return { code: response.data.code, metadata: response.data.data?.metadata }
    },
    detailPath: (meta) =>
      meta?.namespace && meta?.name ? `/pods/${meta.namespace}/${meta.name}` : undefined,
  },
  Deployment: {
    create: async (yaml) => {
      const response = await createDeployment({ yaml })
      return { code: response.data.code, metadata: response.data.data?.metadata }
    },
    detailPath: (meta) =>
      meta?.namespace && meta?.name ? `/deployments/${meta.namespace}/${meta.name}` : undefined,
  },
  Service: {
    create: async (yaml) => {
      const response = await createService({ yaml })
      return { code: response.data.code, metadata: response.data.data?.metadata }
    },
    detailPath: (meta) =>
      meta?.namespace && meta?.name ? `/services/${meta.namespace}/${meta.name}` : undefined,
  },
  Namespace: {
    create: async (yaml) => {
      const response = await createNamespace({ yaml })
      return { code: response.data.code, metadata: response.data.data?.metadata }
    },
    detailPath: () => '/namespaces',
  },
}

const router = useRouter()
const visible = ref(false)
const resourceType = ref<ResourceType>('Pod')
const yamlContent = ref('')
const deploying = ref(false)
const lineNumberOffset = ref(0)

const resourceOptions = (Object.keys(resourceConfigs) as ResourceType[]).map((type) => ({
  label: type,
  value: type,
}))

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

  const config = resourceConfigs[resourceType.value]
  deploying.value = true
  try {
    const result = await config.create(yamlContent.value)
    if (result.code !== 0) {
      return
    }

    message.success('部署成功')
    visible.value = false

    const path = config.detailPath(result.metadata)
    if (path) {
      router.push(path)
    }
  } catch (error) {
    console.error(`部署 ${resourceType.value} 失败:`, error)
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
            :placeholder="`请输入 ${resourceType} YAML 配置`"
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
