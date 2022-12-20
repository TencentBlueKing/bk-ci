import {
  createRouter,
  createWebHistory,
} from 'vue-router';

const HomeEntry = () => import(/* webpackChunkName: "Permission" */ '../app.vue');
const ApplyPermission = () => import(/* webpackChunkName: "Permission" */ '../views/applyPermission/apply-permission.vue');
const MyPermission = () => import(/* webpackChunkName: "Permission" */ '../views/my-permission.vue');

const router = createRouter({
  history: createWebHistory('permission'),
  routes: [
    {
      path: '/',
      component: HomeEntry,
      children: [
        {
          path: 'apply',
          name: 'apply',
          component: ApplyPermission,
        },
        {
          path: '/:projectCode',
          component: MyPermission,
        },
      ],
    },
  ],
});

// afterEach
router.afterEach((to) => {
  // 同步导航数据
  window.$syncUrl?.(to.path);
});

// 导出默认数据
export default router;
