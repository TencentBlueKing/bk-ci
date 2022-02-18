const home = () => import(/* webpackChunkName: 'home' */ '../views/index')
// over-view
const overview = () => import(/* webpackChunkName: 'overview' */ '../views/over-view/index.vue')
// task
const task = () => import(/* webpackChunkName: 'task' */ '../views/task/index.vue')
const taskInit = () => import(/* webpackChunkName: 'task' */ '../views/task/init.vue')
const taskList = () => import(/* webpackChunkName: 'task' */ '../views/task/list.vue')
const taskDetail = () => import(/* webpackChunkName: 'task' */ '../views/task/detail.vue')
const taskCreate = () => import(/* webpackChunkName: 'task' */ '../views/task/create.vue')
const taskSuccess = () => import(/* webpackChunkName: 'task' */ '../views/task/success.vue')
// history
const history = () => import(/* webpackChunkName: 'history' */ '../views/history/index.vue')
const historyList = () => import(/* webpackChunkName: 'history' */ '../views/history/list.vue')
const historyDetail = () => import(/* webpackChunkName: 'history' */ '../views/history/detail.vue')

const routers = [
    {
        path: 'turbo/:projectId?',
        component: home,
        children: [
            {
                path: 'overview',
                name: 'overview',
                component: overview
            },
            {
                path: 'task',
                name: 'task',
                component: task,
                children: [
                    {
                        path: '',
                        name: 'taskList',
                        component: taskList
                    },
                    {
                        path: 'init',
                        name: 'taskInit',
                        component: taskInit
                    },
                    {
                        path: 'detail/:id',
                        name: 'taskDetail',
                        component: taskDetail
                    },
                    {
                        path: 'create',
                        name: 'taskCreate',
                        component: taskCreate
                    },
                    {
                        path: 'success',
                        name: 'taskSuccess',
                        component: taskSuccess
                    }
                ]
            },
            {
                path: 'history',
                component: history,
                children: [
                    {
                        path: '',
                        name: 'history',
                        component: historyList
                    },
                    {
                        path: 'detail/:id',
                        name: 'historyDetail',
                        component: historyDetail
                    }
                ]
            }
        ]
    }
]

export default routers
