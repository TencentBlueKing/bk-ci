
const App = () => import(/* webpackChunkName: "turbo~home" */'../views/App')
const Overviews = () => import(/* webpackChunkName: "turbo~overview" */'../views/Overview')
const taskRegistration = () => import(/* webpackChunkName: "turbo~register" */'../views/TaskRegistration')
const acceleration = () => import(/* webpackChunkName: "turbo~register" */'../views/Acceleration')
const record = () => import(/* webpackChunkName: "turbo~register" */'../views/Record')

const routes = [
    {
        path: 'turbo/:projectId?',
        component: App,
        children: [
            {
                path: '',
                name: 'turboOverview',
                component: Overviews,
                meta: {
                    title: '总览',
                    header: '编译加速',
                    logo: 'turbo',
                    to: 'turboOverview'
                }
                // ,
                // redirect: {
                //     name: 'registration'
                // }
            },
            {
                path: 'registration',
                name: 'registration',
                component: taskRegistration,
                meta: {
                    title: '任务注册',
                    header: '编译加速',
                    logo: 'turbo',
                    to: 'turboOverview'
                }
            },
            {
                path: 'acceleration',
                name: 'acceleration',
                component: acceleration,
                meta: {
                    title: '加速任务',
                    header: '编译加速',
                    logo: 'turbo',
                    to: 'turboOverview'
                }
            },
            {
                path: 'record',
                name: 'record',
                component: record,
                meta: {
                    title: '加速记录',
                    header: '编译加速',
                    logo: 'turbo',
                    to: 'turboOverview'
                }
            }
        ]
    }
]

export default routes
