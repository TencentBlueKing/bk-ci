const ToolRules = () => import(/* webpackChunkName: 'tool-rules' */'../views/tool/rules')
const ToolLogs = () => import(/* webpackChunkName: 'tool-logs' */'../views/tool/logs')

const routes = [
    // 规则配置
    {
        path: '/codecc/:projectId/task/:taskId/tool/:toolId/rules',
        name: 'tool-rules',
        component: ToolRules
    },
    // 分析日志
    {
        path: '/codecc/:projectId/task/:taskId/detail/tool/:toolId/logs',
        name: 'tool-logs',
        component: ToolLogs,
        meta: {
            breadcrumb: 'inside'
        }
    }
]

export default routes
