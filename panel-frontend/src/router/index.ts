import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '@/layouts/BasicLayout.vue'

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
      ],
    },
  ],
})

export default router
