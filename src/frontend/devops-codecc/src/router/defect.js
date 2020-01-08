const DefectCoverityList = () => import(/* webpackChunkName: 'coverity-list' */'../views/defect/coverity-list')
const DefectDupcList = () => import(/* webpackChunkName: 'dupc-list' */'../views/defect/dupc-list')
const DefectDupcDetail = () => import(/* webpackChunkName: 'dupc-detail' */'../views/defect/dupc-detail')
const DefectDupcCharts = () => import(/* webpackChunkName: 'dupc-charts' */'../views/defect/dupc-charts')
const DefectCcnList = () => import(/* webpackChunkName: 'ccn-list' */'../views/defect/ccn-list')
const DefectCcnCharts = () => import(/* webpackChunkName: 'ccn-charts' */'../views/defect/ccn-charts')
const DefectLintList = () => import(/* webpackChunkName: 'lint-list' */'../views/defect/lint-list')
const DefectLintCharts = () => import(/* webpackChunkName: 'lint-charts' */'../views/defect/lint-charts')

const routes = [
    // 按工具模型划分路由，如果模型下暂只有一个工具，则仅使用模型表示，如 ccn/list
    // 如果以后面模型下扩展出新工具则修改为 ccn/:toolId/list
    {
        path: '/codecc/:projectId/task/:taskId/defect/coverity/list',
        name: 'defect-coverity-list',
        component: DefectCoverityList
    },
    {
        path: '/codecc/:projectId/task/:taskId/defect/dupc/list',
        name: 'defect-dupc-list',
        component: DefectDupcList,
        meta: {
            breadcrumb: 'inside'
        }
    },
    {
        path: '/codecc/:projectId/task/:taskId/defect/dupc/detail',
        name: 'defect-dupc-detail',
        component: DefectDupcDetail,
        meta: {
            layout: 'full'
        }
    },
    {
        path: '/codecc/:projectId/task/:taskId/defect/ccn/list',
        name: 'defect-ccn-list',
        component: DefectCcnList,
        meta: {
            breadcrumb: 'inside'
        }
    },

    // 一个 lint 模型对应多个工具，使用 lint/:toolId 组织路由
    {
        path: '/codecc/:projectId/task/:taskId/defect/lint/:toolId/list',
        name: 'defect-lint-list',
        component: DefectLintList,
        meta: {
            breadcrumb: 'inside'
        }
    },

    // 数据图表，本质上是告警的一种展现形式，因此属于告警模块，并且同样使用工具模块划分
    {
        path: '/codecc/:projectId/task/:taskId/defect/lint/:toolId/charts',
        name: 'defect-lint-charts',
        component: DefectLintCharts,
        meta: {
            record: 'none'
        }
    },
    {
        path: '/codecc/:projectId/task/:taskId/defect/dupc/charts',
        name: 'defect-dupc-charts',
        component: DefectDupcCharts,
        meta: {
            record: 'none'
        }
    },
    {
        path: '/codecc/:projectId/task/:taskId/defect/ccn/charts',
        name: 'defect-ccn-charts',
        component: DefectCcnCharts,
        meta: {
            record: 'none'
        }
    }
]

export default routes
