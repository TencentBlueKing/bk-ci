import {
    createRouter,
    createWebHistory,
} from 'vue-router';
const AppEntry = () => import(/* webpackChunkName: "Platform" */ '../App.tsx');
// const RepoManage = () => import(/* webpackChunkName */ '../App.tsx');

const router = createRouter({
    history: createWebHistory(''),
    routes: [
      {
        path: '/',
        component: AppEntry,
        children: []
      }
    ]
});
export default router;
