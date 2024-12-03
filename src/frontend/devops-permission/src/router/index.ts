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
const MyProject = () => import(/* webpackChunkName: "Permission" */ '../views/my-permission/my-project.vue');
const GroupDetail = () => import(/* webpackChunkName: "Permission" */ '../components/itsm-group-detail.vue');
const AuthEntry = () => import(/* webpackChunkName: "Permission" */ '../views/auth/auth-entry.vue');
const OauthHome = () => import(/* webpackChunkName: "Permission" */ '../views/auth/oauth/oauth-home.vue');
const MyAuth = () => import(/* webpackChunkName: "Permission" */ '../views/auth/permission/my-auth.vue');
const router = createRouter({
  history: createWebHistory('permission'),
  routes: [
    {
      path: '/',
      component: HomeEntry,
      children: [
        {
          path: '/',
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
              path: 'my-apply/:applyId?',
              name: 'my-apply',
              component: MyApply,
            },
            {
              path: 'my-approval',
              name: 'my-approval',
              component: MyApproval,
            },
            {
              path: 'my-project',
              name: 'my-project',
              component: MyProject,
            },
          ],
        },
        {
          path: '/auth',
          component: AuthEntry,
          children: [
            {
              path: 'oauth',
              name: 'oauth',
              component: OauthHome,
            },
            {
              path: 'repertory',
              name: 'repertory',
              component: MyAuth,
            },
            {
              path: 'pipeline',
              name: 'pipeline',
              component: MyAuth,
            },
            {
              path: 'envNode',
              name: 'envNode',
              component: MyAuth,
            }
          ]
        }
      ],
    },
    {
      path: '/group/detail',
      name: 'group/detail',
      component: GroupDetail,
    }
  ],
});

// afterEach
router.afterEach((to) => {
  // 同步导航数据
  window.$syncUrl?.(to.fullPath);
});

// 导出默认数据
export default router;
