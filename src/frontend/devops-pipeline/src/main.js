/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/**
 * @file main entry
 */

import Vue from 'vue'
import App from './App'
import createRouter from './router'
import store from './store'
import focus from './directives/focus/index.js'

import VeeValidate from 'vee-validate'
import validationENMessages from 'vee-validate/dist/locale/en'
import validationCNMessages from 'vee-validate/dist/locale/zh_CN'
import ExtendsCustomRules from './utils/customRules'
import validDictionary from './utils/validDictionary'
import PortalVue from 'portal-vue' // eslint-disable-line
import createLocale from '../../locale'
import '@icon-cool/bk-icon-devops/src/index'
import log from '@blueking/log'
import { actionMap, resourceMap, resourceTypeMap } from '../../common-lib/permission-conf'
import bkMagic from 'bk-magic-vue'
// 全量引入 bk-magic-vue 样式
require('bk-magic-vue/dist/bk-magic-vue.min.css')

const { i18n, setLocale } = createLocale(require.context('@locale/pipeline/', false, /\.json$/))

Vue.use(focus)
Vue.use(bkMagic)
Vue.use(PortalVue)
Vue.use(log)

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

Vue.prototype.$setLocale = setLocale
Vue.prototype.$permissionActionMap = actionMap
Vue.prototype.$permissionResourceMap = resourceMap
Vue.prototype.$permissionResourceTypeMap = resourceTypeMap

Vue.mixin({
    methods: {
        // handleError (e, permissionAction, instance, projectId, resourceMap = this.$permissionResourceMap.pipeline) {
        handleError (e, noPermissionList) {
            if (e.code === 403) { // 没有权限编辑
                // this.setPermissionConfig(resourceMap, permissionAction, instance ? [instance] : [], projectId)
                this.$showAskPermissionDialog({
                    noPermissionList
                })
            } else {
                this.$showTips({
                    message: e.message || e,
                    theme: 'error'
                })
            }
        },
        /**
         * 设置权限弹窗的参数
         */
        setPermissionConfig (resourceId, actionId, instanceId = [], projectId = this.$route.params.projectId) {
            this.$showAskPermissionDialog({
                noPermissionList: [{
                    actionId,
                    resourceId,
                    instanceId,
                    projectId
                }]
            })
        }
    }
})

global.pipelineVue = new Vue({
    el: '#app',
    router: createRouter(store),
    i18n,
    store,
    components: {
        App
    },
    template: '<App/>'
})
