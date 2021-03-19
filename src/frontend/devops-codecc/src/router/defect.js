const DefectList = () => import(/* webpackChunkName: 'defect-list' */'../views/defect/list')
const DefectCoverityList = () => import(/* webpackChunkName: 'defect-list' */'../views/defect/coverity-list')
const DefectCoverityCharts = () => import(/* webpackChunkName: 'defect-charts' */'../views/defect/coverity-charts')
const DefectDupcList = () => import(/* webpackChunkName: 'defect-list' */'../views/defect/dupc-list')
const DefectDupcDetail = () => import(/* webpackChunkName: 'dupc-detail' */'../views/defect/dupc-detail')
const DefectDupcCharts = () => import(/* webpackChunkName: 'defect-charts' */'../views/defect/dupc-charts')
const DefectCcnList = () => import(/* webpackChunkName: 'defect-list' */'../views/defect/ccn-list')
const DefectCcnCharts = () => import(/* webpackChunkName: 'defect-charts' */'../views/defect/ccn-charts')
const DefectClocList = () => import(/* webpackChunkName: 'defect-list' */'../views/defect/cloc-list')
const DefectClocLang = () => import(/* webpackChunkName: 'defect-list' */'../views/defect/cloc-lang')
// const DefectLintList = () => import(/* webpackChunkName: 'defect-list' */'../views/defect/lint-list')
const DefectLintCharts = () => import(/* webpackChunkName: 'defect-charts' */'../views/defect/lint-charts')

const routes = [
    {
        path: '/codecc/:projectId/task/:taskId/defect/tool/:toolId/list',
        name: 'defect-list',
        component: DefectList,
        meta: {
            breadcrumb: 'inside'
        }
    },
    // 按工具模型划分路由，如果模型下暂只有一个工具，则仅使用模型表示，如 ccn/list
    // 如果以后面模型下扩展出新工具则修改为 ccn/:toolId/list
    {
        path: '/codecc/:projectId/task/:taskId/defect/compile/:toolId/list',
        name: 'defect-coverity-list',
        component: DefectCoverityList,
        meta: {
            breadcrumb: 'inside'
        }
    },
    {
        path: '/codecc/:projectId/task/:taskId/defect/compile/:toolId/list',
        name: 'defect-klocwork-list',
        component: DefectCoverityList,
        meta: {
            breadcrumb: 'inside'
        }
    },
    {
        path: '/codecc/:projectId/task/:taskId/defect/compile/:toolId/list',
        name: 'defect-pinpoint-list',
        component: DefectCoverityList,
        meta: {
            breadcrumb: 'inside'
        }
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
    {
        path: '/codecc/:projectId/task/:taskId/defect/cloc/list/:path*',
        name: 'defect-cloc-list',
        component: DefectClocList,
        meta: {
            breadcrumb: 'inside'
        }
    },
    {
        path: '/codecc/:projectId/task/:taskId/defect/cloc/language',
        name: 'defect-cloc-lang',
        component: DefectClocLang,
        meta: {
            breadcrumb: 'inside'
        }
    },

    // 一个 lint 模型对应多个工具，使用 lint/:toolId 组织路由
    {
        path: '/codecc/:projectId/task/:taskId/defect/lint/:toolId/list',
        name: 'defect-lint-list',
        component: DefectList,
        meta: {
            breadcrumb: 'inside'
        }
    },

    // 数据图表，本质上是问题的一种展现形式，因此属于问题模块，并且同样使用工具模块划分
    {
        path: '/codecc/:projectId/task/:taskId/defect/compile/:toolId/charts',
        name: 'defect-coverity-charts',
        component: DefectCoverityCharts,
        meta: {
            breadcrumb: 'inside'
        }
    },
    {
        path: '/codecc/:projectId/task/:taskId/defect/compile/:toolId/charts',
        name: 'defect-klocwork-charts',
        component: DefectCoverityCharts,
        meta: {
            breadcrumb: 'inside'
        }
    },
    {
        path: '/codecc/:projectId/task/:taskId/defect/compile/:toolId/charts',
        name: 'defect-pinpoint-charts',
        component: DefectCoverityCharts,
        meta: {
            breadcrumb: 'inside'
        }
    },
    {
        path: '/codecc/:projectId/task/:taskId/defect/lint/:toolId/charts',
        name: 'defect-lint-charts',
        component: DefectLintCharts,
        meta: {
            breadcrumb: 'inside'
        }
    },
    {
        path: '/codecc/:projectId/task/:taskId/defect/dupc/charts',
        name: 'defect-dupc-charts',
        component: DefectDupcCharts,
        meta: {
            breadcrumb: 'inside'
        }
    },
    {
        path: '/codecc/:projectId/task/:taskId/defect/ccn/charts',
        name: 'defect-ccn-charts',
        component: DefectCcnCharts,
        meta: {
            breadcrumb: 'inside'
        }
    }
]

export default routes
