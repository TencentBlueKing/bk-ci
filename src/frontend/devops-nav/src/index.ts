/// <reference path='./typings/index.d.ts' />

import 'core-js/es7/array'
import Vue from 'vue'

import createRouter from '@/router'
import store from '@/store'
import eventBus from '@/utils/eventBus'
import Logo from './components/Logo/index.vue'
import iframeUtil from './utils/iframeUtil'
import Icon from './components/Icon/index.vue'
import EmptyTips from './components/EmptyTips/index.vue'
import ShowTooltip from './components/ShowTooltip/index.vue'
import DevopsFormItem from './components/DevopsFormItem/index.vue'
import AsideNav from './components/AsideNav/index.vue'
import ContentHeader from './components/ContentHeader/index.vue'
import BigSelect from './components/Select/index.vue'
import App from './views/App.vue'

import createLocale from '../../locale'

import VeeValidate from 'vee-validate'
import validationENMessages from 'vee-validate/dist/locale/en';
import validationCNMessages from 'vee-validate/dist/locale/zh_CN';
import ExtendsCustomRules from './utils/customRules'
import validDictionary from './utils/validDictionary'
import showAskPermissionDialog from './components/AskPermissionDialog'
import bsWebSocket from './utils/bsWebSocket.js'
// 全量引入 bk-magic-vue
import bkMagic from 'bk-magic-vue'
// 全量引入 bk-magic-vue 样式
require('bk-magic-vue/dist/bk-magic-vue.min.css') // eslint-disable-line
import './assets/scss/index.scss'

declare module 'vue/types/vue' {
    interface Vue {
        $bkMessage: any
        $bkInfo: any
        $showAskPermissionDialog: any
        iframeUtil: any
    }
}

Vue.use(bkMagic)
Vue.component('AsideNav', AsideNav)
Vue.component('ContentHeader', ContentHeader)
Vue.component('Logo', Logo)
Vue.component('Icon', Icon)
Vue.component('EmptyTips', EmptyTips)
Vue.component('ShowTooltip', ShowTooltip)
Vue.component('DevopsFormItem', DevopsFormItem)
Vue.component('BigSelect', BigSelect)

const { i18n, dynamicLoadModule, setLocale, localeList } = createLocale(require.context('@locale/nav/', false, /\.json$/))

// @ts-ignore
Vue.use(VeeValidate, {
    i18nRootKey: 'validations', // customize the root path for validation messages.
    i18n,
    fieldsBagName: 'veeFields',
    dictionary: {
        'en-US': validationENMessages,
        'zh-CN': validationCNMessages
    }
})

VeeValidate.Validator.localize(validDictionary)
ExtendsCustomRules(VeeValidate.Validator.extend)

const router = createRouter(store, dynamicLoadModule)
router.afterEach((route) => {
    bsWebSocket.changeRoute(route)
})
router.beforeEach((to, from, next) => {
    bsWebSocket.loginOut(from)
    next()
})
window.eventBus = eventBus
Vue.prototype.iframeUtil = iframeUtil(router)
Vue.prototype.$showAskPermissionDialog = showAskPermissionDialog
Vue.prototype.$setLocale = setLocale
Vue.prototype.$localeList = localeList

window.devops = new Vue({
    el: '#devops-root',
    i18n,
    router,
    store,
    render (h) {
        return h(App)
    }
})
