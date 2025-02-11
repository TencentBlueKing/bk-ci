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
const CreateCodeSource = () => import('@/views/RepositoryService/CreateCodeSource');

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/platform',
      name: 'platform',
      component: PlatformEntry,
      redirect: () => {
        return { name: 'CodeSourceManage' }
      },
      children: [
        {
          path: 'codeSourceManage',
          name: 'CodeSourceManage',
          component: CodeSourceManage,
        },
        {
          path: 'createCodeSource',
          name: 'CreateCodeSource',
          component: CreateCodeSource,
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
  window.$syncUrl?.(to.fullPath);
});
export default router;
