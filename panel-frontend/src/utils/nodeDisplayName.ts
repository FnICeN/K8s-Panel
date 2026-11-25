/** Kubernetes 节点 hostname → 平台展示名称 */
const NODE_DISPLAY_NAME_MAP: Record<string, string> = {
  master: '云网控制节点',
  'node5-desktop': '监控数据存储节点',
  'node6-desktop': '告警与漏洞扫描节点',
  'node7-desktop': '镜像仓库节点',
  'node8-desktop': '监控看板节点',
  worker2: '遥感图像处理节点',
  worker3: '服务网关节点',
  worker4: '身份认证与监控节点',
  worker8: '备份节点',
}

export function getNodeDisplayName(nodeName?: string | null): string {
  if (!nodeName) return '-'
  return NODE_DISPLAY_NAME_MAP[nodeName] || nodeName
}

export function isSimKubeVirtualNode(nodeName?: string | null): boolean {
  return !!nodeName && nodeName.startsWith('sk-virtual-')
}
