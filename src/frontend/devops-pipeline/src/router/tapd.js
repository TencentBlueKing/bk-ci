// TAPD流水线定制页面
// TAPD特殊详情页
const pipelinesDetail = () => import(/* webpackChunkName: "pipelinesDetail" */'../views/tapd/content.vue')

// TAPD DEV一站式
const TapdCreatePipeline = () => import(/* webpackChunkName: "pipelineCreate" */'../views/tapd/create.vue')
const TapdHeader = () => import(/* webpackChunkName: "pipelinesDetail" */'../views/tapd/header.vue')
const tapdPipelinesSubpage = () => import(/* webpackChunkName: "tapdPipeline" */'../views/tapd/subpage.vue')
const tapdPipelinesEntry = () => import(/* webpackChunkName: "tapdPipeline" */'../views/tapd/index.vue')


export default [
    {
        // tapd特殊详情页
        path: ':pipelineId/detail/:buildNo/:type?',
        name: 'tapdPipelinesDetail',
        component: pipelinesDetail
    },
    {
        // 创建页
        path: 'create',
        name: 'tapdCreatePipeline',
        component: TapdCreatePipeline
    },
    {
        path: ':pipelineId',
        name: 'tapdPipelines',
        component: tapdPipelinesEntry,
        children: [
            {
                // 标准详情页
                path: 'stddetail/:buildNo/:type?',
                name: 'tapdPipelinesStdDetail',
                components: {
                    header: TapdHeader,
                    default: tapdPipelinesSubpage
                },
                meta: {
                    title: 'pipeline',
                    header: 'pipeline',
                    icon: 'pipeline',
                    to: 'pipelinesList'
                }
            },
            {
                // 编辑页
                path: 'edit/:tab?',
                name: 'tapdPipelinesEdit',
                components: {
                    header: TapdHeader,
                    default: tapdPipelinesSubpage
                },
                meta: {
                    title: 'pipeline',
                    header: 'pipeline',
                    icon: 'pipeline',
                    to: 'pipelinesList'
                }
            },
            {
                // 执行历史页
                path: 'history/:type?',
                name: 'tapdPipelinesHistory',
                components: {
                    header: TapdHeader,
                    default: tapdPipelinesSubpage
                },
                meta: {
                    title: 'pipeline',
                    header: 'pipeline',
                    icon: 'pipeline',
                    to: 'PipelineManageList'
                }
            },
            {
                // 执行页（预览）
                path: 'preview/:version?',
                name: 'tapdExecutePreview',
                components: {
                    header: TapdHeader,
                    default: tapdPipelinesSubpage
                },
                meta: {
                    title: 'pipeline',
                    header: 'pipeline',
                    icon: 'pipeline',
                    to: 'PipelineManageList'
                }
            }
        ]
    }
]
