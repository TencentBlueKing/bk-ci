import routes from './router'
import store from './store'
import './scss/app.scss'

window.Pages = window.Pages || {}
window.Pages['artifactory'] = {
    title: VERSION_TYPE === 'tencent' ? '版本仓库' : '制品库',
    routes,
    store
}
