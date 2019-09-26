import routes from './router'
import store from './store'
import './scss/app.scss'

window.Pages = window.Pages || {}
window.Pages['artifactory'] = {
    title: '版本仓库',
    routes,
    store
}
