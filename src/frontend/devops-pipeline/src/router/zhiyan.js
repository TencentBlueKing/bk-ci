// 智研交付流流水线定制页面
const EditHeader = () => import(/* webpackChunkName: "pipelinesEdit" */'../components/PipelineHeader/EditHeader.vue')
const DetailHeader = () => import(/* webpackChunkName: "pipelinesDetail" */'../components/PipelineHeader/DetailHeader.vue')
const PreviewHeader = () => import(/* webpackChunkName: "pipelinesPreview" */'../components/PipelineHeader/PreviewHeader.vue')
const zyPipelinesEntry = () => import(/* webpackChunkName: "zyPipeline" */'../views/zhiyan/index.vue')
const zyPipelinesSubpage = () => import(/* webpackChunkName: "zyPipeline" */'../views/zhiyan/subpage.vue')

export default [{
    path: 'template/:templateId/edit',
    component: zyPipelinesEntry,
    name: 'zyTemplateEdit'
}, {
    path: ':pipelineId',
    component: zyPipelinesEntry,
    name: 'zyPipelines',
    children: [
        {
            // 详情
            path: 'detail/:buildNo/:type?',
            name: 'zyPipelinesDetail',
            components: {
                header: DetailHeader,
                default: zyPipelinesSubpage
            },
            meta: {
                title: 'pipeline',
                header: 'pipeline',
                icon: 'pipeline',
                to: 'pipelinesList'
            }
        },
        {
            // 流水线编辑
            path: 'edit/:tab?',
            name: 'zyPipelinesEdit',
            meta: {
                icon: 'pipeline',
                title: 'pipeline',
                header: 'pipeline',
                to: 'pipelinesList'
            },
            components: {
                header: EditHeader,
                default: zyPipelinesSubpage
            }
        },
        {
            // 流水线执行可选插件
            path: 'preview',
            name: 'zyPipelinesPreview',
            meta: {
                icon: 'pipeline',
                title: 'pipeline',
                header: 'pipeline',
                to: 'pipelinesList'
            },
            components: {
                header: PreviewHeader,
                default: zyPipelinesSubpage
            }
        }
    ]
}]
