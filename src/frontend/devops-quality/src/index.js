import routes from './router'
import store from './store'
import './scss/app.scss'

/* eslint-disable */
// 扩展字符串，判断是否为蓝盾变量格式
String.prototype.isBkVar = function () {
    return /^\$\{{2}([\w\_\.-]+)\}{2}$/g.test(this) || /^\$\{([\w\_\.-]+)\}$/g.test(this)
}

window.Pages = window.Pages || {}
window.Pages.quality = {
    title: '质量红线',
    routes,
    store
}
