const vsHome = () => import(/* webpackChunkName: "vs-home" */ '@/views/index.vue') // 漏洞扫描入口
const vsList = () => import(/* webpackChunkName: "vs-home" */ '@/views/vs_list.vue') // 扫描列表
const vsReport = () => import(/* webpackChunkName: "vs-detail" */ '@/views/vs_report') // 扫描报告
const createVs = () => import(/* webpackChunkName: "vs-create" */ '@/views/create_vs.vue') // 漏洞扫描入口

const routes = [
    {
        path: 'vs/:projectId?',
        component: vsHome,
        children: [
            {
                path: '',
                name: 'vsList',
                component: vsList,
                meta: {
                    title: '扫描记录',
                    logo: 'vs',
                    header: 'vs',
                    to: 'vsList'
                }
            },
            {
                path: 'vsReport/:vsId',
                name: 'vsReport',
                component: vsReport,
                meta: {
                    title: () => '查看报告',
                    logo: 'vs',
                    header: 'vs',
                    to: 'vsList'
                }
            },
            {
                path: 'createVs',
                name: 'createVs',
                component: createVs,
                meta: {
                    title: '新增扫描',
                    logo: 'vs',
                    header: 'vs',
                    to: 'vsList'
                }
            }
        ]
    }
]

export default routes
