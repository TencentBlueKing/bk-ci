import routes from './router'
import store from './store'
import './scss/app.scss'

window.Pages = window.Pages || {}
window.Pages.artifactory = {
    title: '制品库',
    routes,
    store
}
