const CheckerList = () => import(/* webpackChunkName: 'checker' */'../views/checker/list')

const routes = [
    {
        path: '/codecc/:projectId/checker/list',
        name: 'checker-list',
        component: CheckerList,
        meta: {
            layout: 'outer'
        }
    }
]

export default routes
