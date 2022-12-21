import {
  createRouter,
  createWebHistory,
} from 'vue-router';

const MetricsEntry = () => import(/* webpackChunkName: "MetricsEntry" */ '../views/metrics-entry.vue');
const MetricsOverview = () => import(/* webpackChunkName: "MetricsOverview" */ '../views/child-view/metrics-overview/overview-main.vue');
const FailAnalysis = () => import(/* webpackChunkName: "FailAnalysis" */ '../views/child-view/fail-analysis/fail-main.vue');
const PluginTrendEntry = () => import(/* webpackChunkName: "PluginTrendEntry" */ '../views/child-view/plugin-trend/plugin-trend-entry.vue');
const PluginFailAnalysis = () => import(/* webpackChunkName: "PluginFailAnalysis" */ '../views/child-view/plugin-trend/plugin-fail-analysis/fail-analysis-main.vue');
const PluginRunAnalysis = () => import(/* webpackChunkName: "PluginRunAnalysis" */ '../views/child-view/plugin-trend/plugin-run-analysis/run-analysis-main.vue');

export default createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/metrics/:projectId/',
      component: MetricsEntry,
      redirect: () => {
        return { name: 'MetricsOverview' }
      },
      children: [
        {
          path: 'overview',
          name: 'MetricsOverview',
          component: MetricsOverview,
        },
        {
          path: 'fail-analysis',
          name: 'FailAnalysis',
          component: FailAnalysis,
        },
        {
          path: 'plugin-trend',
          name: 'PluginTrendEntry',
          component: PluginTrendEntry,
          children: [
            {
              path: 'fail-analysis',
              name: 'PluginFailAnalysis',
              component: PluginFailAnalysis,
            },
            {
              path: 'run-analysis',
              name: 'PluginRunAnalysis',
              component: PluginRunAnalysis,
            },
          ],
        },
      ],
    },
  ],
});
