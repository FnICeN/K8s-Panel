import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '@/layouts/BasicLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { createNamespace } from '@/api/clusterController'
import { message } from 'ant-design-vue'

const gatekeeperAccessYaml = `apiVersion: v1
kind: Namespace
metadata:
  name: loa-test-level5
`

// 首次打开受限地址时回到首页，以便布局挂载并显示认证弹窗。
const blockGatekeeperNavigation = () => router.currentRoute.value.name ? false : { name: 'home' }

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: BasicLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('../pages/HomePage.vue'),
        },
        {
          path: 'health',
          name: 'health',
          component: () => import('../pages/Health.vue'),
        },
        {
          path: 'audit-alerts',
          name: 'auditAlerts',
          component: () => import('../pages/AuditAlerts.vue'),
        },
        {
          path: 'nodes',
          name: 'nodes',
          component: () => import('../pages/cluster/NodesManager.vue'),
        },
        {
          path: 'nodes/:name',
          name: 'nodeSpec',
          component: () => import('../pages/cluster/NodeSpec.vue'),
        },
        {
          path: 'pods',
          name: 'pods',
          component: () => import('../pages/cluster/PodManager.vue'),
        },
        {
          path: 'pods/:namespace/:name',
          name: 'podSpec',
          component: () => import('../pages/cluster/PodSpec.vue'),
        },
        {
          path: 'services',
          name: 'services',
          component: () => import('../pages/cluster/ServiceManager.vue'),
        },
        {
          path: 'services/:namespace/:name',
          name: 'serviceSpec',
          component: () => import('../pages/cluster/ServiceSpec.vue'),
        },
        {
          path: 'deployments',
          name: 'deployments',
          component: () => import('../pages/cluster/DeploymentManager.vue'),
        },
        {
          path: 'deployments/:namespace/:name',
          name: 'deploymentSpec',
          component: () => import('../pages/cluster/DeploymentSpec.vue'),
        },
        {
          path: 'namespaces',
          name: 'namespaces',
          component: () => import('../pages/cluster/NamespaceManager.vue'),
        },
        {
          path: 'roles',
          name: 'roles',
          component: () => import('../pages/cluster/RoleManager.vue'),
        },
        {
          path: 'gatekeeper',
          name: 'gatekeeper',
          component: () => import('../pages/cluster/GatekeeperManager.vue'),
        },
        {
          path: 'backup',
          name: 'backup',
          component: () => import('../pages/BackupConsole.vue'),
        },
        {
          path: 'monitor',
          name: 'monitor',
          component: () => import('../pages/MonitorConsole.vue'),
        },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../pages/DashboardConsole.vue'),
        },
      ],
    },
  ],
})

// 路由级登录守卫：Gatekeeper 页面需先完成 5 级准入检查，其余页面保留原有未登录空态行为。
router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  const loggedIn = await authStore.refreshLoginStatus()
  if (!loggedIn) {
    authStore.requestLoginDialog()
    return to.name === 'gatekeeper' ? blockGatekeeperNavigation() : true
  }
  if (to.name === 'gatekeeper') {
    try {
      // 等待服务端 dry-run 的准入结果；Gatekeeper 拒绝时现有响应拦截器会弹出重新认证。
      const response = await createNamespace({ yaml: gatekeeperAccessYaml }, { params: { dryRun: true } })
      if (response.data.code !== 0) {
        if (response.data.code === 40100) authStore.requestLoginDialog()
        return blockGatekeeperNavigation()
      }
    } catch (error) {
      console.error('Gatekeeper 页面权限校验失败:', error)
      message.error('Gatekeeper 页面权限校验失败')
      return blockGatekeeperNavigation()
    }
  }
  return true
})

export default router
