import routes from './router'
import store from './store'

import './scss/app.scss'
const page = {
    title: '发布体验',
    routes,
    store
}

window.Pages = window.Pages || {}
window.Pages.experience = page
