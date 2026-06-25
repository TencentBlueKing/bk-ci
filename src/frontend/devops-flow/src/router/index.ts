import { createRouter, createWebHistory } from 'vue-router'
import { fetchFlowInfo } from '../api/flowInfo'
import { FLOW_GROUP_TYPES } from '../constants/flowGroup'
import { ROUTE_NAMES } from '../constants/routes'
import { VERSION_STATUS_ENUM } from '../utils/flowConst'


declare module 'vue-router' {
  interface RouteMeta {
    websocket?: boolean
  }
}

declare global {
  interface Window {
    $syncUrl?: (path: string) => void
    WEBSOCKET_URL_PREFIX?: string
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/creative-stream/:projectId',
      redirect: (to) => ({
        name: ROUTE_NAMES.FLOW_LIST,
        params: {
          projectId: to.params.projectId,
          groupId: FLOW_GROUP_TYPES.ALL_FLOWS,
        },
      }),
      children: [
        {
          path: 'list/:groupId',
          component: () => import('../views/FlowList'),
          name: ROUTE_NAMES.FLOW_LIST,
          props: true,
          meta: { websocket: true },
        },
        {
          path: 'flow/:flowId',
          name: ROUTE_NAMES.FLOW_DETAIL,
          component: () => import('../views/Flow'),
          children: [
            {
              path: 'detail/:version',
              component: () => import('../views/Flow/Detail/index'),
              children: [
                {
                  path: 'execution-record',
                  component: () => import('../views/Flow/Detail/ExecutionRecord'),
                  name: ROUTE_NAMES.FLOW_DETAIL_EXECUTION_RECORD,
                  props: true,
                  meta: { websocket: true },
                },
                {
                  path: 'trigger-record',
                  component: () => import('../views/Flow/Detail/TriggerRecord'),
                  name: ROUTE_NAMES.FLOW_DETAIL_TRIGGER_RECORD,
                  props: true,
                },
                {
                  path: 'trigger-events',
                  component: () => import('../views/Flow/Detail/TriggerEvent'),
                  name: ROUTE_NAMES.FLOW_DETAIL_TRIGGER_EVENTS,
                  props: true,
                },
                {
                  path: 'workflow-orchestration',
                  component: () => import('../views/Flow/Detail/FlowModel'),
                  name: ROUTE_NAMES.FLOW_DETAIL_WORKFLOW_ORCHESTRATION,
                  props: true,
                },
                {
                  path: 'workflow-environment',
                  component: () => import('../views/Flow/Detail/AuthoringEnvTab'),
                  name: ROUTE_NAMES.FLOW_DETAIL_WORKFLOW_ENVIRONMENT,
                  props: true,
                },
                {
                  path: 'notification-config',
                  component: () => import('../views/Flow/Detail/Notice'),
                  name: ROUTE_NAMES.FLOW_DETAIL_NOTIFICATION_CONFIG,
                  props: true,
                },
                {
                  path: 'basic-settings',
                  component: () => import('../views/Flow/Detail/BasicSetting'),
                  name: ROUTE_NAMES.FLOW_DETAIL_BASIC_SETTINGS,
                  props: true,
                },
                {
                  path: 'permission-settings',
                  component: () => import('../views/Flow/Detail/PermissionSettings'),
                  name: ROUTE_NAMES.FLOW_DETAIL_PERMISSION_SETTINGS,
                  props: true,
                },
                {
                  path: 'permission-delegation',
                  component: () => import('../views/Flow/Detail/PermissionDelegation'),
                  name: ROUTE_NAMES.FLOW_DETAIL_PERMISSION_DELEGATION,
                  props: true,
                },
                {
                  path: 'operation-log',
                  component: () => import('../views/Flow/Detail/ChangeLog'),
                  name: ROUTE_NAMES.FLOW_DETAIL_OPERATION_LOG,
                  props: true,
                },
                // 兜底路由：处理非法的 tab
                {
                  path: ':invalidTab',
                  redirect: (to) => {
                    // 如果 tab 不合法，重定向到默认 tab，保留 version 参数
                    return {
                      name: ROUTE_NAMES.FLOW_DETAIL_EXECUTION_RECORD,
                      params: {
                        flowId: to.params.flowId,
                        version: to.params.version,
                      },
                    }
                  },
                },
              ],
            },
            {
              path: 'edit/:version?',
              component: () => import('../views/Flow/Edit/index'),
              children: [
                {
                  path: '',
                  redirect: (to) => ({
                    name: ROUTE_NAMES.FLOW_EDIT_WORKFLOW_ORCHESTRATION,
                    params: {
                      flowId: to.params.flowId,
                      version: to.params.version,
                    },
                  }),
                },
                {
                  path: 'workflow-orchestration',
                  component: () => import('../views/Flow/Edit/WorkflowOrchestration'),
                  name: ROUTE_NAMES.FLOW_EDIT_WORKFLOW_ORCHESTRATION,
                  props: true,
                },
                {
                  path: 'workflow-environment',
                  component: () => import('../views/Flow/Edit/WorkflowEnvironment'),
                  name: ROUTE_NAMES.FLOW_EDIT_WORKFLOW_ENVIRONMENT,
                  props: true,
                },
                {
                  path: 'trigger-events',
                  component: () => import('../views/Flow/Edit/TriggerEvents'),
                  name: ROUTE_NAMES.FLOW_EDIT_TRIGGER_EVENTS,
                  props: true,
                },
                {
                  path: 'notification-config',
                  component: () => import('../views/Flow/Edit/NotificationConfig'),
                  name: ROUTE_NAMES.FLOW_EDIT_NOTIFICATION_CONFIG,
                  props: true,
                },
                {
                  path: 'basic-settings',
                  component: () => import('../views/Flow/Edit/BasicSettings'),
                  name: ROUTE_NAMES.FLOW_EDIT_BASIC_SETTINGS,
                  props: true,
                },
                // 兜底路由：处理非法的 tab
                {
                  path: ':invalidTab',
                  redirect: (to) => {
                    return {
                      name: ROUTE_NAMES.FLOW_EDIT_WORKFLOW_ORCHESTRATION,
                      params: {
                        flowId: to.params.flowId,
                        version: to.params.version,
                      },
                    }
                  },
                },
              ],
            },
          ],
        },
        {
          path: 'flow/:flowId/preview/:version',
          component: () => import('../views/Flow/Preview/index'),
          name: ROUTE_NAMES.FLOW_PREVIEW,
          props: true,
        },
        {
          path: 'flow/:flowId/execute/:buildNo',
          component: () => import('../views/Flow/Execute/index'),
          children: [
            {
              path: '',
              redirect: (to) => ({
                name: ROUTE_NAMES.FLOW_DETAIL_EXECUTION_DETAIL_TAB,
              }),
            },
            {
              path: 'execute-detail',
              component: () => import('../views/Flow/Execute/ExecutionTab/ExecPipeline'),
              name: ROUTE_NAMES.FLOW_DETAIL_EXECUTION_DETAIL_TAB,
              meta: { websocket: true },
            },
            {
              path: 'artifacts',
              component: () => import('../views/Flow/Execute/ExecutionTab/Outputs'),
              name: ROUTE_NAMES.FLOW_DETAIL_ARTIFACTS,
            },
            {
              path: 'outputs',
              component: () => import('../views/Flow/Execute/ExecutionTab/Outputs'),
              name: ROUTE_NAMES.FLOW_DETAIL_OUTPUTS,
            },
            {
              path: 'start-params',
              component: () => import('../views/Flow/Execute/ExecutionTab/StartParams'),
              name: ROUTE_NAMES.FLOW_DETAIL_START_PARAMS,
            },
            // 兜底路由：处理非法的 tab
            {
              path: ':invalidTab',
              redirect: (to) => {
                return {
                  name: ROUTE_NAMES.FLOW_DETAIL_EXECUTION_DETAIL_TAB,
                  params: {
                    projectId: to.params.projectId,
                    flowId: to.params.flowId,
                    buildNo: to.params.buildNo,
                  },
                  query: to.query,
                }
              },
            },
          ],
        },
        {
          path: 'import-edit',
          component: () => import('../views/Flow'),
          children: [
            {
              path: '',
              component: () => import('../views/Flow/Edit/index'),
              children: [
                {
                  path: '',
                  redirect: { name: ROUTE_NAMES.FLOW_IMPORT_EDIT_WORKFLOW_ORCHESTRATION },
                },
                {
                  path: 'workflow-orchestration',
                  component: () => import('../views/Flow/Edit/WorkflowOrchestration'),
                  name: ROUTE_NAMES.FLOW_IMPORT_EDIT_WORKFLOW_ORCHESTRATION,
                  props: true,
                },
                {
                  path: 'workflow-environment',
                  component: () => import('../views/Flow/Edit/WorkflowEnvironment'),
                  name: ROUTE_NAMES.FLOW_IMPORT_EDIT_WORKFLOW_ENVIRONMENT,
                  props: true,
                },
                {
                  path: 'trigger-events',
                  component: () => import('../views/Flow/Edit/TriggerEvents'),
                  name: ROUTE_NAMES.FLOW_IMPORT_EDIT_TRIGGER_EVENTS,
                  props: true,
                },
                {
                  path: 'notification-config',
                  component: () => import('../views/Flow/Edit/NotificationConfig'),
                  name: ROUTE_NAMES.FLOW_IMPORT_EDIT_NOTIFICATION_CONFIG,
                  props: true,
                },
                {
                  path: 'basic-settings',
                  component: () => import('../views/Flow/Edit/BasicSettings'),
                  name: ROUTE_NAMES.FLOW_IMPORT_EDIT_BASIC_SETTINGS,
                  props: true,
                },
                {
                  path: ':invalidTab',
                  redirect: { name: ROUTE_NAMES.FLOW_IMPORT_EDIT_WORKFLOW_ORCHESTRATION },
                },
              ],
            },
          ],
        },
        {
          path: 'template',
          component: () => import('../views/Template'),
          name: ROUTE_NAMES.TEMPLATE,
        },
        {
          path: 'group',
          component: () => import('../views/FlowLabelGroup/index'),
          name: ROUTE_NAMES.FLOW_LABEL_GROUP,
        },
      ],
    },
    // 全局 404 兜底
    {
      path: '/:pathMatch(.*)*',
      redirect: { name: ROUTE_NAMES.FLOW_LIST, params: { groupId: FLOW_GROUP_TYPES.ALL_FLOWS } },
    },
  ],
})

router.beforeEach(async (to) => {
  // 只在直接访问 /flow/:flowId 路径时触发（路由名称为 FLOW_DETAIL）
  // 如果访问的是子路由（如 detail/:version/...），则直接通过
  if (to.name !== ROUTE_NAMES.FLOW_DETAIL) {
    return true
  }

  const projectId = to.params.projectId as string
  const flowId = to.params.flowId as string
  
  try {
    // 获取 flowInfo，拿到 releaseVersion
    const flowInfo = await fetchFlowInfo({ projectId, flowId })

    // 如果只有草稿版本，重定向到编辑页
    if (flowInfo.latestVersionStatus === VERSION_STATUS_ENUM.COMMITTING) {
      return {
        name: ROUTE_NAMES.FLOW_EDIT_WORKFLOW_ORCHESTRATION,
        params: {
          projectId,
          flowId,
          version: flowInfo.version?.toString(),
        },
      }
    }

    // 重定向到详情页，带上 releaseVersion
    return {
      name: ROUTE_NAMES.FLOW_DETAIL_EXECUTION_RECORD,
      params: {
        projectId,
        flowId,
        version: flowInfo.releaseVersion?.toString(),
      },
    }
  } catch (error) {
    console.error('Failed to fetch flow info for routing:', error)
    // 出错时仍然跳转到详情页，让详情页组件处理错误
    return {
      name: ROUTE_NAMES.FLOW_DETAIL_EXECUTION_RECORD,
      params: {
        projectId,
        flowId,
        version: '0', // 使用占位符，详情页会重新获取
      },
    }
  }
})

router.afterEach((to) => {
  window.$syncUrl?.(to.fullPath.replace(new RegExp('^/' + import.meta.env.BASE_URL + '/'), '/'))
})
export default router
