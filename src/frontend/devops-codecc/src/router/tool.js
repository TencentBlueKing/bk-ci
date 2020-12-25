const ToolRules = () => import(/* webpackChunkName: 'tool' */'../views/tool/rules')
const ToolLogs = () => import(/* webpackChunkName: 'tool' */'../views/tool/logs')

const routes = [
    // 规则配置
    {
        path: '/codecc/:projectId/task/:taskId/tool/:toolId/rules',
        name: 'tool-rules',
        component: ToolRules,
        meta: {
            breadcrumb: 'inside'
        }
    },
    // 分析日志
    {
        path: '/codecc/:projectId/task/:taskId/detail/tool/:toolId/logs',
        name: 'task-detail-logs',
        component: ToolLogs,
        meta: {
            breadcrumb: 'inside'
        }
    }
]

export default routes
