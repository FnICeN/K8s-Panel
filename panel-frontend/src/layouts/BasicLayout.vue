<template>
  <a-layout class="basic-layout">
    <a-layout-header class="header">
      <div class="header-left">
        <h1 class="platform-name">天基云网安全管理平台</h1>
      </div>
      <div class="header-right">
        <a-button type="primary">登录</a-button>
      </div>
    </a-layout-header>
    <a-layout>
      <a-layout-sider width="200" style="background: #fff">
        <div class="logo" />
        <a-menu
          id="main-menu"
          v-model:openKeys="openKeys"
          v-model:selectedKeys="selectedKeys"
          mode="inline"
          :items="items"
          style="height: 100%; border-right: 0"
          @click="handleClick"
        ></a-menu>
      </a-layout-sider>
      <a-layout>
        <a-layout-content
          :style="{ margin: '24px 16px', padding: '24px', background: '#fff', minHeight: '280px' }"
        >
          <RouterView />
        </a-layout-content>
      </a-layout>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { reactive, ref, watch, h, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { HomeOutlined, CheckCircleOutlined } from '@ant-design/icons-vue';
import type { MenuProps, ItemType } from 'ant-design-vue';

const router = useRouter();
const route = useRoute();

const selectedKeys = ref<string[]>(['/']);
const openKeys = ref<string[]>([]);

function getItem(
  label: string,
  key: string,
  icon?: any,
  children?: ItemType[],
  type?: 'group',
): ItemType {
  return {
    key,
    icon,
    children,
    label,
    type,
  } as ItemType;
}

const items: ItemType[] = reactive([
  getItem('首页', '/', () => h(HomeOutlined)),
  getItem('集群管理', '/health', () => h(CheckCircleOutlined), [
    getItem('节点管理', '/nodes'),
    getItem('Pod管理', '/pods'),
    getItem('命名空间管理', '/namespaces'),
    getItem('服务管理', '/services'),
  ]),
]);

const handleClick: MenuProps['onClick'] = (e) => {
  console.log('click', e);
  router.push(e.key.toString());
};

onMounted(() => {
  selectedKeys.value = [route.path];
  // 处理嵌套路由的 openKeys
  updateOpenKeys(route.path);
});

watch(
  () => route.path,
  (newPath) => {
    selectedKeys.value = [newPath];
    // 处理嵌套路由的 openKeys
    updateOpenKeys(newPath);
  }
);

function updateOpenKeys(path: string) {
  // 根据当前路径更新 openKeys
  if (path === '/health' || path.startsWith('/nodes') || path.startsWith('/pods') || path.startsWith('/namespaces') || path.startsWith('/services')) {
    openKeys.value = ['/health'];
  } else {
    openKeys.value = [];
  }
}
</script>

<style scoped>
.basic-layout {
  min-height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  background: #001529;
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.header-left {
  display: flex;
  align-items: center;
}

.platform-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
}

.logo {
  height: 32px;
  margin: 16px;
  background: rgba(255, 255, 255, 0.3);
}
</style>
