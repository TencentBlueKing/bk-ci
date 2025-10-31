const experienceHome = () => import(/* webpackChunkName: "home" */ '../views/experience/index.vue') // 版本体验入口
const experienceList = () => import(/* webpackChunkName: "home" */ '../views/experience/experience_list.vue') // 发布管理
const experienceDetail = () => import(/* webpackChunkName: "detail" */ '../views/experience/experience_detail.vue') // 发布详情
const createExperience = () => import(/* webpackChunkName: "create" */ '../views/experience/create_experience.vue') // 新增体验
const setting = () => import(/* webpackChunkName: "setting" */ '../views/experience/setting.vue') // 设置

const routes = [
    {
        path: 'experience/:projectId?',
        component: experienceHome,
        children: [
            {
                path: '',
                name: 'experienceList',
                component: experienceList,
                meta: {
                    title: 'mobileRelease',
                    logo: 'experience',
                    header: 'experience',
                    to: 'experienceList'
                }
            },
            {
                path: 'experienceDetail/:experienceId/:type',
                name: 'experienceDetail',
                component: experienceDetail,
                meta: {
                    title: 'releaseDetail',
                    logo: 'experience',
                    header: 'experience',
                    to: 'experienceList'
                }
            },
            {
                path: 'createExperience',
                name: 'createExperience',
                component: createExperience,
                meta: {
                    title: 'addExp',
                    logo: 'experience',
                    header: 'experience',
                    to: 'experienceList'
                }
            },
            {
                path: 'editExperience/:experienceId',
                name: 'editExperience',
                component: createExperience,
                meta: {
                    title: 'editRelease',
                    logo: 'experience',
                    header: 'experience',
                    to: 'experienceList'
                }
            },
            {
                path: 'setting',
                name: 'setting',
                component: setting,
                meta: {
                    title: 'setting',
                    logo: 'experience',
                    header: 'experience',
                    to: 'experienceList'
                }
            }
        ]
    }
]

export default routes
