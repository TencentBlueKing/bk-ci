import {
  createRouter,
  createWebHistory,
} from 'vue-router';

const HomeEntry = () => import(/* webpackChunkName: "HomeEntry" */ '../app.vue');
const ApplyProject = () => import(/* webpackChunkName: "ApplyProject" */ '../views/apply-project.vue');
// 项目管理
const ManageEntry = () => import(/* webpackChunkName: "ManageEntry" */ '../views/manage/manage-entry.vue');
const EditProject = () => import(/* webpackChunkName: "EditProject" */ '../views/manage/project/edit-project.vue');
const ShowProject = () => import(/* webpackChunkName: "ShowProject" */ '../views/manage/project/show-project.vue');
// 用户组管理
const UserGroup = () => import(/* webpackChunkName: "UserGroup" */ '../views/manage/group/user-group.vue');
const ExpandManage = () => import(/* webpackChunkName: "ExpandManage" */ '../views/manage/expand/list.vue');

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
          path: '/:projectCode',
          component: ManageEntry,
          props: true,
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
