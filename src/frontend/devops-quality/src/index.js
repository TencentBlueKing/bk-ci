import routes from './router'
import store from './store'
import './scss/app.scss'

window.Pages = window.Pages || {}
window.Pages.quality = {
    title: '质量红线',
    routes,
    store
}
