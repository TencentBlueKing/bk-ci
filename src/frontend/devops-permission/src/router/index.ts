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
const MyApplyPermission = () => import(/* webpackChunkName: "Permission" */ '../views/my-permission/apply-permission.vue');

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
          component: PermissionEntry,
          children: [
            {
              path: 'permission',
              name: 'permission',
              component: MyPermission,
            },
            {
              path: 'apply',
              name: 'apply',
              component: MyApply,
            },
            {
              path: 'approval',
              name: 'approval',
              component: MyApproval,
            },
            {
              path: 'apply-permission',
              name: 'apply-permission',
              component: MyApplyPermission,
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
