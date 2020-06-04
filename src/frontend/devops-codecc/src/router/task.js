const TaskList = () => import(/* webpackChunkName: 'task-list' */'../views/task/list')
const TaskNew = () => import(/* webpackChunkName: 'task-new' */'../views/task/new')
const TaskDetail = () => import(/* webpackChunkName: 'task-detail' */'../views/task/detail')

// 任务设置模块，使用嵌套路由方式
const TaskSettings = () => import(/* webpackChunkName: 'task-settings' */'../views/task/settings')
const ToolManage = () => import(/* webpackChunkName: 'tool-manage' */'../views/tool/manage')
const TaskSettingsBasic = () => import(/* webpackChunkName: 'task-settings' */'../views/task/settings-basic')
const TaskSettingsCode = () => import(/* webpackChunkName: 'task-settings-code' */'../views/task/settings-code')
const TaskSettingsTrigger = () => import(/* webpackChunkName: 'task-settings' */'../views/task/settings-trigger')
const TaskSettingsIgnore = () => import(/* webpackChunkName: 'task-settings' */'../views/task/settings-ignore')
const TaskSettingsManage = () => import(/* webpackChunkName: 'task-settings' */'../views/task/settings-manage')
const TaskSettingsBlank = () => import(/* webpackChunkName: 'task-settings' */'../views/task/white-blank')

const routes = [
    {
        path: '/codecc/:projectId/task/list',
        name: 'task-list',
        component: TaskList,
        meta: {
            layout: 'outer',
            title: '我的任务'
        }
    },

    // 新建任务，包括已有任务未添加工具，当访问路径中带有taskId则识别为已有任务
    {
        path: '/codecc/:projectId/task/:taskId?/new',
        name: 'task-new',
        component: TaskNew,
        meta: {
            layout: 'outer',
            title: '我的任务'
        }
    },
    {
        path: '/codecc/:projectId/task/:taskId/detail',
        name: 'task-detail',
        component: TaskDetail,
        meta: {
            layout: 'inner',
            record: 'none'
        }
    },
    {
        path: '/codecc/:projectId/task/:taskId/settings',
        name: 'task-settings',
        component: TaskSettings,
        children: [
            {
                path: '',
                redirect: { name: 'task-settings-basic' }
            },
            {
                path: 'basic',
                name: 'task-settings-basic',
                component: TaskSettingsBasic
            },
            // 工具管理
            {
                path: 'tools',
                name: 'task-settings-tools',
                component: ToolManage
            },
            {
                path: 'code',
                name: 'task-settings-code',
                component: TaskSettingsCode
            },
            {
                path: 'trigger',
                name: 'task-settings-trigger',
                component: TaskSettingsTrigger
            },
            // 路径屏蔽
            {
                path: 'ignore',
                name: 'task-settings-ignore',
                component: TaskSettingsIgnore
            },
            // 任务管理
            {
                path: 'manage',
                name: 'task-settings-manage',
                component: TaskSettingsManage
            },
            // 跳转中继站
            {
                path: 'blank',
                name: 'task-settings-blank',
                component: TaskSettingsBlank
            }
        ]
    }
]

export default routes
