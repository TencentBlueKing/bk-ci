// ftp流水线定制页面
const FtpHeader = () => import(/* webpackChunkName: "pipelinesDetail" */'../views/ftp/header.vue')
const pipelinesDetail = () => import(/* webpackChunkName: "pipelinesDetail" */'../views/ftp/content.vue')
const ftpPipelinesEntry = () => import(/* webpackChunkName: "ftpPipeline" */'../views/ftp/index.vue')

export default [{
    path: ':pipelineId',
    name: 'ftpPipelines',
    component: ftpPipelinesEntry,
    children: [
        {
            // 详情
            path: 'detail/:buildNo/:type?',
            name: 'ftpPipelinesDetail',
            components: {
                header: FtpHeader,
                default: pipelinesDetail
            },
            meta: {
                title: 'pipeline',
                header: 'pipeline',
                icon: 'pipeline',
                to: 'pipelinesList'
            }
        },
        {
            // 编辑
            path: 'edit/:tab?',
            name: 'ftpPipelinesEdit',
            components: {
                header: FtpHeader,
                default: pipelinesDetail
            },
            meta: {
                title: 'pipeline',
                header: 'pipeline',
                icon: 'pipeline',
                to: 'pipelinesList'
            }
        },
        {
            // 执行历史
            path: 'history/:type?',
            name: 'ftpPipelinesHistory',
            components: {
                header: FtpHeader,
                default: pipelinesDetail
            },
            meta: {
                title: 'pipeline',
                header: 'pipeline',
                icon: 'pipeline',
                to: 'PipelineManageList'
            }
        }
    ]
}]
