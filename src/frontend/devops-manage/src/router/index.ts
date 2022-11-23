import {
  createRouter,
  createWebHistory,
} from 'vue-router';

const HomeEntry = () => import(/* webpackChunkName: "Manage" */ '../app.vue');
const ApplyProject = () => import(/* webpackChunkName: "Apply" */ '../views/apply-project.vue');
// 项目管理
const ManageEntry = () => import(/* webpackChunkName: "Manage" */ '../views/manage/manage-entry.vue');
const EditProject = () => import(/* webpackChunkName: "Manage" */ '../views/manage/project/edit-project.vue');
const ShowProject = () => import(/* webpackChunkName: "Manage" */ '../views/manage/project/show-project.vue');
// 用户组管理
const UserGroup = () => import(/* webpackChunkName: "Manage" */ '../views/manage/group/user-group.vue');
const ExpandManage = () => import(/* webpackChunkName: "Manage" */ '../views/manage/expand/list.vue');

const router = createRouter({
  history: createWebHistory('manage'),
  routes: [
    {
      path: '/',
      component: HomeEntry,
      children: [
        {
          path: 'apply',
          name: 'apply',
          component: ApplyProject,
        },
        {
          path: '/:projectId',
          component: ManageEntry,
          redirect: () => ({ name: 'show' }),
          children: [
            {
              path: 'show',
              name: 'show',
              component: ShowProject,
            },
            {
              path: 'edit',
              name: 'edit',
              component: EditProject,
            },
            {
              path: 'group',
              name: 'group',
              component: UserGroup,
            },
            {
              path: 'expand',
              name: 'expand',
              component: ExpandManage,
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
