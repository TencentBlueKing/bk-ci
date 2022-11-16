import {
  createRouter,
  createWebHistory,
} from 'vue-router';

const HomeEntry = () => import(/* webpackChunkName: "Manage" */ '../app.vue');
const ApplyProject = () => import(/* webpackChunkName: "Apply" */ '../views/apply-project.vue');
const ProjectList = () => import(/* webpackChunkName: "Manage" */ '../views/project-list.vue');
// 项目管理
const ManageEntry = () => import(/* webpackChunkName: "Manage" */ '../views/manage/manage-entry.vue');
const EditProject = () => import(/* webpackChunkName: "Manage" */ '../views/manage/project/edit-project.vue');
const ShowProject = () => import(/* webpackChunkName: "Manage" */ '../views/manage/project/show-project.vue');
// 用户组管理
const UserGroup = () => import(/* webpackChunkName: "Manage" */ '../views/manage/group/user-group.vue');

export default createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: HomeEntry,
      children: [
        {
          path: 'apply',
          component: ApplyProject,
        },
        {
          path: 'list',
          component: ProjectList,
        },
        {
          path: 'manage',
          component: ManageEntry,
          children: [
            {
              path: 'show',
              component: ShowProject,
            },
            {
              path: 'edit',
              component: EditProject,
            },
            {
              path: 'group',
              component: UserGroup,
            },
          ],
        },
      ],
    },
  ],
});
