/// <reference path='./typings/index.d.ts' />

import Vue from 'vue'

import createRouter from '@/router'
import store from '@/store'
import eventBus from '@/utils/eventBus'
import Logo from '@/components/Logo/index.vue'
import iframeUtil from '@/utils/iframeUtil'
import Icon from '@/components/Icon/index.vue'
import EmptyTips from '@/components/EmptyTips/index.vue'
import ShowTooltip from '@/components/ShowTooltip/index.vue'
import DevopsFormItem from '@/components/DevopsFormItem/index.vue'
import AsideNav from '@/components/AsideNav/index.vue'
import ContentHeader from '@/components/ContentHeader/index.vue'
import BigSelect from '@/components/Select/index.vue'
import App from '@/views/App.vue'
import { actionMap, resourceMap, resourceTypeMap } from '../../common-lib/permission-conf'
import { BkciDocs } from '../../common-lib/docs'

import createLocale from '../../locale'

import VeeValidate from 'vee-validate'
import validationENMessages from 'vee-validate/dist/locale/en'
import validationCNMessages from 'vee-validate/dist/locale/zh_CN'
import ExtendsCustomRules from './utils/customRules'
import validDictionary from './utils/validDictionary'
import showAskPermissionDialog from './components/AskPermissionDialog'
import bsWebSocket from '@/utils/bsWebSocket.js'
import '@/assets/scss/index.scss'
import { judgementLsVersion } from './utils/util'
import '@icon-cool/bk-icon-devops/src/index'

// 全量引入 bk-magic-vue
import bkMagic from 'bk-magic-vue'
// 全量引入 bk-magic-vue 样式
require('bk-magic-vue/dist/bk-magic-vue.min.css') // eslint-disable-line

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

const { i18n, dynamicLoadModule, setLocale, localeList } = createLocale(require.context('@locale/nav/', false, /\.json$/), true)

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

const router = createRouter(store, dynamicLoadModule, i18n)
router.afterEach((route) => {
    bsWebSocket.changeRoute(route)
})
router.beforeEach((to, from, next) => {
    bsWebSocket.loginOut(from)
    next()
})
window.eventBus = eventBus
window.vuexStore = store
Vue.prototype.iframeUtil = iframeUtil(router)
Vue.prototype.$showAskPermissionDialog = showAskPermissionDialog
Vue.prototype.$setLocale = setLocale
Vue.prototype.$localeList = localeList
Vue.prototype.$permissionActionMap = actionMap
Vue.prototype.$permissionResourceMap = resourceMap
Vue.prototype.$permissionResourceTypeMap = resourceTypeMap
Vue.prototype.BKCI_DOCS = BkciDocs
Vue.prototype.$bkMessage = function (config) {
    config.ellipsisLine = config.ellipsisLine || 3
    bkMagic.bkMessage(config)
}

// 判断localStorage版本, 旧版本需要清空
judgementLsVersion()

Vue.mixin({
    methods: {
        async applyPermission (actionId, resourceId, instanceId = []) {
            try {
                const redirectUrl = await this.$store.dispatch('getPermRedirectUrl', [{
                    actionId,
                    resourceId,
                    instanceId
                }])
                window.open(redirectUrl, '_blank')
                this.$bkInfo({
                    title: this.$t('permissionRefreshtitle'),
                    subTitle: this.$t('permissionRefreshSubtitle'),
                    okText: this.$t('permissionRefreshOkText'),
                    cancelText: this.$t('close'),
                    confirmFn: () => {
                        location.reload()
                    }
                })
            } catch (e) {
                console.error(e)
            }
        }
    }
})

window.devops = new Vue({
    el: '#devops-root',
    i18n,
    router,
    store,
    render (h) {
        return h(App)
    }
})
