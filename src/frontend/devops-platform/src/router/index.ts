import {
  createRouter,
  createWebHistory,
} from 'vue-router';
const PaltformEntry = () => import('@/views/PaltformEntry');
// 系统管理员
const SystemManage = () => import('@/views/SystemManage');
// 代码源管理
const CodeSourceManage = () => import('@/views/RepositoryService/CodeSourceManage');
// 新增代码源
const CreateCodeSource = () => import('@/views/RepositoryService/CreateCodeSource');

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/paltform',
      name: 'paltform',
      component: PaltformEntry,
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
export default router;
