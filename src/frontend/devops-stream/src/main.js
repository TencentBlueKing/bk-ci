/**
 * @file main entry
 * @author <%- author %>
 */

import Vue from 'vue'
import '../build/public-path'

import App from '@/App'
import '@/common/bkmagic'
import icon from '@/components/icon'
import '@/css/index.css'
import router from '@/router'
import store from '@/store'
import log from '@blueking/log'
import { bkMessage } from 'bk-magic-vue'
import bkPipeline from 'bkui-pipeline'
import VeeValidate from 'vee-validate'

import {
    getCookie, getLanguageMap
} from '@/utils'
import VueI18n from 'vue-i18n'
Vue.use(VueI18n)
const DEFAULT_LANG = 'zh-CN'
const COOKIE_KEY = 'blueking_language'

const lang = getLanguageMap(getCookie(COOKIE_KEY) || '') || DEFAULT_LANG
const i18n = new VueI18n({
    locale: lang,
    messages: {
        'zh-CN': require('../../locale/stream/zh-CN.json'),
        'en-US': require('../../locale/stream/en-US.json')
    }
})

Vue.component('Icon', icon)
Vue.use(log)
Vue.use(VeeValidate)

Vue.use(bkPipeline, {
    i18n
})

Vue.prototype.$bkMessage = function (config) {
    config.ellipsisLine = config.ellipsisLine || 3
    bkMessage(config)
}

window.changeFlag = false
window.mainComponent = new Vue({
    el: '#app',
    router,
    store,
    i18n,
    components: { App },
    template: '<App/>'
})
