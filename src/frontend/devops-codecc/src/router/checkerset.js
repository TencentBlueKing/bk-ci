const CheckersetList = () => import(/* webpackChunkName: 'checkerset' */'../views/checkerset/list')
const CheckersetManage = () => import(/* webpackChunkName: 'checkerset' */'../views/checkerset/manage')

const routes = [
    {
        path: '/codecc/:projectId/checkerset/list',
        name: 'checkerset-list',
        component: CheckersetList,
        meta: {
            layout: 'outer'
        }
    },
    {
        path: '/codecc/:projectId/checkerset/:checkersetId/:version/manage',
        name: 'checkerset-manage',
        component: CheckersetManage,
        meta: {
            layout: 'outer'
        }
    }
]

export default routes
