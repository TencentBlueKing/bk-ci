/// <reference path='./typings/index.d.ts' />

import 'core-js/es7/array'
import Vue from 'vue'

import createRouter from '@/router'
import store from '@/store'
import eventBus from '@/utils/eventBus'
import App from '@/views/App.vue'
import Logo from '@/components/Logo/index.vue'
import Icon from '@/components/Icon/index.vue'
import EmptyTips from '@/components/EmptyTips/index.vue'
import ShowTooltip from '@/components/ShowTooltip/index.vue'
import DevopsFormItem from '@/components/DevopsFormItem/index.vue'
import iframeUtil from '@/utils/iframeUtil'

import VeeValidate from 'vee-validate'
import ExtendsCustomRules from './utils/customRules'
import validDictionary from './utils/validDictionary'
import showAskPermissionDialog from './components/AskPermissionDialog'
// 全量引入 bk-magic-vue
import bkMagic from 'bk-magic-vue'
import './assets/scss/index.scss'

declare module 'vue/types/vue' {
    interface Vue {
        $bkMessage: any
        $bkInfo: any
        $showAskPermissionDialog: any
        iframeUtil: any
    }
}
// 全量引入 bk-magic-vue 样式
require('bk-magic-vue/dist/bk-magic-vue.min.css') // eslint-disable-line

// @ts-ignore
Vue.use(VeeValidate, {
    fieldsBagName: 'veeFields',
    locale: 'cn'
})

VeeValidate.Validator.localize(validDictionary)
ExtendsCustomRules(VeeValidate.Validator.extend)

Vue.use(bkMagic)

Vue.component('Logo', Logo)
Vue.component('Icon', Icon)
Vue.component('EmptyTips', EmptyTips)
Vue.component('ShowTooltip', ShowTooltip)
Vue.component('DevopsFormItem', DevopsFormItem)

const router = createRouter(store)
window.eventBus = eventBus
Vue.prototype.iframeUtil = iframeUtil(router)
Vue.prototype.$showAskPermissionDialog = showAskPermissionDialog

window.devops = new Vue({
    el: '#devops-root',
    router,
    store,
    render (h) {
        return h(App)
    }
})
