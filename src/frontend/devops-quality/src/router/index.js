const qualityHome = () => import(/* webpackChunkName: "quality-overview" */ '@/views/index.vue') // 质量红线
const overview = () => import(/* webpackChunkName: "quality-overview" */ '@/views/overview.vue') // 总览
const ruleList = () => import(/* webpackChunkName: "quality-list" */ '@/views/rule_list.vue') // 拦截规则
const createRule = () => import(/* webpackChunkName: "quality-rule" */ '@/views/create_rules.vue') // 创建规则
const interceptHistory = () => import(/* webpackChunkName: "quality-history" */ '@/views/intercept_history.vue') // 拦截历史
const noticeGroup = () => import(/* webpackChunkName: "quality-notice" */ '@/views/notice_group.vue') // 通知组
const metadataList = () => import(/* webpackChunkName: "quality-metadata" */ '@/views/metadata_list.vue') // 指标列表
const createMeta = () => import(/* webpackChunkName: "create-meta" */ '@/views/create_meta.vue') // 自定义指标

const routes = [
    {
        path: 'quality/:projectId?',
        component: qualityHome,
        children: [
            {
                path: '',
                name: 'qualityOverview',
                component: overview,
                meta: {
                    title: '总览',
                    logo: 'quality',
                    header: 'quality',
                    to: 'qualityOverview'
                }
            },
            {
                path: 'ruleList',
                name: 'ruleList',
                component: ruleList,
                meta: {
                    title: '红线规则',
                    logo: 'quality',
                    header: 'quality',
                    to: 'qualityOverview'
                }
            },
            {
                path: 'createRule',
                name: 'createRule',
                component: createRule,
                meta: {
                    title: '创建红线规则',
                    logo: 'quality',
                    header: 'quality',
                    to: 'qualityOverview'
                }
            },
            {
                path: 'editRule/:ruleId',
                name: 'editRule',
                component: createRule,
                meta: {
                    title: '编辑红线规则',
                    logo: 'quality',
                    header: 'quality',
                    to: 'qualityOverview'
                }
            },
            {
                path: 'interceptHistory',
                name: 'interceptHistory',
                component: interceptHistory,
                meta: {
                    title: '红线记录',
                    logo: 'quality',
                    header: 'quality',
                    to: 'qualityOverview'
                }
            },
            {
                path: 'metadataList',
                name: 'metadataList',
                component: metadataList,
                meta: {
                    title: '指标列表',
                    logo: 'quality',
                    header: 'quality',
                    to: 'qualityOverview'
                }
            },
            {
                path: 'createMeta',
                name: 'createMeta',
                component: createMeta,
                meta: {
                    title: '创建脚本任务指标',
                    logo: 'quality',
                    header: 'quality',
                    to: 'qualityOverview'
                }
            },
            {
                path: 'editMeta/:metaId',
                name: 'editMeta',
                component: createMeta,
                meta: {
                    title: '编辑脚本任务指标',
                    logo: 'quality',
                    header: 'quality',
                    to: 'qualityOverview'
                }
            },
            {
                path: 'noticeGroup',
                name: 'noticeGroup',
                component: noticeGroup,
                meta: {
                    title: '通知组',
                    logo: 'quality',
                    header: 'quality',
                    to: 'qualityOverview'
                }
            }
        ]
    }
]

export default routes
