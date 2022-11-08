import routes from './router'
import store from './store'
import './scss/app.scss'

window.Pages = window.Pages || {}
window.Pages.vs = {
    title: '漏洞扫描',
    routes,
    store
}
