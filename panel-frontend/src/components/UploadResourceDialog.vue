<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { InboxOutlined } from '@ant-design/icons-vue'
import type { UploadProps } from 'ant-design-vue'
import {
  createDeployment,
  createConfigMap,
  createNamespace,
  createPod,
  createService,
} from '@/api/clusterController.ts'
import { summarizeK8sYaml, type ResourceType } from '@/utils/k8sYaml'

type CreatedMeta = { namespace?: string; name?: string } | undefined

const MAX_FILE_BYTES = 1024 * 1024

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
  ConfigMap: {
    create: async (yaml) => {
      const response = await createConfigMap({ yaml })
      return { code: response.data.code, metadata: response.data.data?.resource?.metadata }
    },
    detailPath: () => undefined,
  },
}

const router = useRouter()
const visible = ref(false)
const fileName = ref('')
const yamlContent = ref('')
const deploying = ref(false)

const summary = computed(() => summarizeK8sYaml(yamlContent.value))

const blockedReason = computed(() => {
  if (!fileName.value) {
    return ''
  }
  if (!yamlContent.value.trim() || summary.value.documentCount === 0) {
    return '文件内容为空'
  }
  if (summary.value.documentCount > 1) {
    return '文件里包含多个资源，请只保留一个 YAML 文档后再部署'
  }
  if (!summary.value.kind) {
    return summary.value.rawKind
      ? `暂不支持 ${summary.value.rawKind}，仅支持 Pod、Deployment、Service、Namespace、ConfigMap`
      : '未找到 kind 字段，无法识别资源类型'
  }
  return ''
})

const open = () => {
  fileName.value = ''
  yamlContent.value = ''
  visible.value = true
}

const handleCancel = () => {
  visible.value = false
}

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const name = file.name.toLowerCase()
  if (!name.endsWith('.yaml') && !name.endsWith('.yml')) {
    message.warning('请上传 .yaml 或 .yml 文件')
    return false
  }
  if (file.size > MAX_FILE_BYTES) {
    message.warning('文件不能超过 1MB')
    return false
  }

  file
    .text()
    .then((text) => {
      fileName.value = file.name
      yamlContent.value = text.replace(/^\uFEFF/, '')
    })
    .catch(() => {
      message.error('读取文件失败')
    })
  return false
}

const handleDeploy = async () => {
  if (!fileName.value || !yamlContent.value.trim()) {
    message.warning('请先选择 YAML 文件')
    return
  }
  if (blockedReason.value || !summary.value.kind) {
    message.warning(blockedReason.value || '无法识别资源类型')
    return
  }

  const config = resourceConfigs[summary.value.kind]
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
    console.error(`部署 ${summary.value.kind} 失败:`, error)
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
    title="上传并部署"
    width="720px"
    :footer="null"
    :destroy-on-close="false"
    wrap-class-name="upload-resource-modal"
    @cancel="handleCancel"
  >
    <div class="dialog-body">
      <p class="dialog-description">选择 Kubernetes YAML 文件，确认资源类型后直接部署到集群</p>

      <a-upload-dragger
        :before-upload="beforeUpload"
        :show-upload-list="false"
        :multiple="false"
        accept=".yaml,.yml,text/yaml,application/x-yaml"
      >
        <p class="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p class="ant-upload-text">{{ fileName ? '重新选择文件' : '点击或拖拽 YAML 文件到此处' }}</p>
        <p class="ant-upload-hint">支持 .yaml / .yml，单个 Pod、Deployment、Service 或 Namespace</p>
      </a-upload-dragger>

      <div v-if="fileName" class="file-summary">
        <div class="summary-row">
          <span>文件</span>
          <strong>{{ fileName }}</strong>
        </div>
        <div class="summary-row">
          <span>资源类型</span>
          <strong>{{ summary.kind || summary.rawKind || '未识别' }}</strong>
        </div>
        <div v-if="summary.name" class="summary-row">
          <span>名称</span>
          <strong>{{ summary.name }}</strong>
        </div>
        <div v-if="summary.namespace" class="summary-row">
          <span>命名空间</span>
          <strong>{{ summary.namespace }}</strong>
        </div>
      </div>

      <a-alert v-if="blockedReason" type="warning" show-icon :message="blockedReason" />

      <pre v-if="yamlContent" class="yaml-preview">{{ yamlContent }}</pre>

      <div class="dialog-footer">
        <a-button @click="handleCancel">取消</a-button>
        <a-button
          type="primary"
          :loading="deploying"
          :disabled="!fileName || !!blockedReason"
          @click="handleDeploy"
        >
          部署
        </a-button>
      </div>
    </div>
  </a-modal>
</template>

<style scoped>
.dialog-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dialog-description {
  margin: -4px 0 0;
  color: var(--app-text-secondary);
  font-size: 15px;
  line-height: 1.6;
}

.file-summary {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 16px;
  padding: 12px 14px;
  border: 1px solid var(--app-border-soft);
  border-radius: 6px;
  background: var(--app-surface-muted);
}

.summary-row {
  display: flex;
  gap: 8px;
  min-width: 0;
  color: var(--app-text);
  font-size: 14px;
}

.summary-row span {
  flex: none;
  color: var(--app-text-muted);
}

.summary-row strong {
  overflow: hidden;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.yaml-preview {
  max-height: 240px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  border: 1px solid var(--app-border);
  border-radius: 6px;
  background: var(--app-surface);
  color: var(--app-text);
  font-family: Consolas, 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:global(.upload-resource-modal .ant-modal) {
  top: 48px;
  padding-bottom: 48px;
}
</style>
