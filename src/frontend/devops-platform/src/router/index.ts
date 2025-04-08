import {
  createRouter,
  createWebHistory,
} from 'vue-router';
const PlatformEntry = () => import('@/views/PlatformEntry');
// 系统管理员
const SystemManage = () => import('@/views/SystemManage/index');
// 代码源管理
const CodeSourceManage = () => import('@/views/RepositoryService/CodeSourceManage');
// 新增代码源
const CodeConfigForm = () => import('@/views/RepositoryService/CodeConfigForm');

const router = createRouter({
  history: createWebHistory('platform'),
  routes: [
    {
      path: '/',
      name: 'platform',
      component: PlatformEntry,
      redirect: () => {
        return { name: 'Config' }
      },
      children: [
        {
          path: 'config',
          name: 'Config',
          component: CodeSourceManage,
        },
        {
          path: 'config/form/:action?',
          name: 'ConfigForm',
          component: CodeConfigForm,
        },
        {
          path: 'systemManage',
          name: 'SystemManage',
          component: SystemManage,
        },
      ]
    },
  ]
});

// afterEach
router.afterEach((to) => {
  // 同步导航数据
  if (!location.search.includes('disableSyncUrl=true')) {
    window.$syncUrl?.(to.fullPath);
  }
});
export default router;
