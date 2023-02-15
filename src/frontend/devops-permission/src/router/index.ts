import {
  createRouter,
  createWebHistory,
} from 'vue-router';

const HomeEntry = () => import(/* webpackChunkName: "Permission" */ '../app.vue');
const ApplyPermission = () => import(/* webpackChunkName: "Permission" */ '../views/applyPermission/apply-permission.vue');
const PermissionEntry = () => import(/* webpackChunkName: "Permission" */ '../views/my-permission/permission-entry.vue');
const MyApply = () => import(/* webpackChunkName: "Permission" */ '../views/my-permission/my-apply.vue');
const MyApproval = () => import(/* webpackChunkName: "Permission" */ '../views/my-permission/my-approval.vue');
const MyPermission = () => import(/* webpackChunkName: "Permission" */ '../views/my-permission/my-permission.vue');

const router = createRouter({
  history: createWebHistory('permission'),
  routes: [
    {
      path: '/',
      component: HomeEntry,
      children: [
        {
          path: '/:projectCode',
          component: PermissionEntry,
          redirect: () => ({ name: 'my-permission' }),
          children: [
            {
              path: 'apply',
              name: 'apply',
              component: ApplyPermission,
            },
            {
              path: 'my-permission',
              name: 'my-permission',
              component: MyPermission,
            },
            {
              path: 'my-apply',
              name: 'my-apply',
              component: MyApply,
            },
            {
              path: 'my-approval',
              name: 'my-approval',
              component: MyApproval,
            },
          ],
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
