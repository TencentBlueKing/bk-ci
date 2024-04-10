// tapd流水线定制页面
const pipelinesDetail = () => import(/* webpackChunkName: "pipelinesDetail" */'../views/tapd/content.vue')

export default [{
    path: ':pipelineId/detail/:buildNo/:type?',
    name: 'tapdPipelinesDetail',
    component: pipelinesDetail
}]
