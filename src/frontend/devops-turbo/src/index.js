import routes from './router'
import store from './store'

import './assets/scss/index.scss'
window.Pages = window.Pages || {}
window.Pages['turbo'] = {
    title: '编译加速',
    routes,
    store
}

// const router = new VueRouter({
//     mode: 'history',
//     routes
// })

// new Vue({
//     el: '#root',
//     router,
//     components: {
//         App: import('./views/App')
//     },
//     store: new Vuex.Store({
//         modules: {
//             turbo: store
//         }
//     }),
//     template: `<router-view></router-view>`
// })
