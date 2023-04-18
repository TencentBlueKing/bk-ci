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
import focus from './directives/focus/index.js'
import createRouter from './router'
import store from './store'

import '@icon-cool/bk-icon-devops'
import '@icon-cool/bk-icon-devops/src/index'
import mavonEditor from 'mavon-editor'
import 'mavon-editor/dist/css/index.css'
import PortalVue from "portal-vue"; // eslint-disable-line
import VeeValidate from 'vee-validate'
import validationENMessages from 'vee-validate/dist/locale/en'
import validationCNMessages from 'vee-validate/dist/locale/zh_CN'
import createLocale from '../../locale'
import ExtendsCustomRules from './utils/customRules'
import validDictionary from './utils/validDictionary'

import bkMagic from 'bk-magic-vue'
import BkPipeline from 'bkui-pipeline'
import { pipelineDocs } from '../../common-lib/docs'
import {
    actionMap,
    resourceMap,
    resourceTypeMap
} from '../../common-lib/permission-conf'

// 全量引入 bk-magic-vue 样式
require('bk-magic-vue/dist/bk-magic-vue.min.css')

const { i18n, setLocale } = createLocale(
    require.context('@locale/pipeline/', false, /\.json$/)
)

Vue.use(focus)
Vue.use(bkMagic)
Vue.use(PortalVue)
Vue.use(mavonEditor)

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
console.log(i18n.locale)
Vue.use(BkPipeline, {
    i18n
})

Vue.prototype.$setLocale = setLocale
Vue.prototype.$permissionActionMap = actionMap
Vue.prototype.$permissionResourceMap = resourceMap
Vue.prototype.$permissionResourceTypeMap = resourceTypeMap
Vue.prototype.$pipelineDocs = pipelineDocs
Vue.prototype.$bkMessage = function (config) {
    config.ellipsisLine = config.ellipsisLine || 3
    bkMagic.bkMessage(config)
}
/* eslint-disable */
// 扩展字符串，判断是否为蓝盾变量格式
String.prototype.isBkVar = function () {
    return /\$\{{2}([\w\_\.\s-]+)\}{2}/g.test(this) || /\$\{([\w\_\.\s-]+)\}/g.test(this)
}

/* eslint-disable */

Vue.mixin({
    methods: {
        // handleError (e, permissionAction, instance, projectId, resourceMap = this.$permissionResourceMap.pipeline) {
        handleError(e, noPermissionList) {
            if (e.code === 403) {
                // 没有权限编辑
                // this.setPermissionConfig(resourceMap, permissionAction, instance ? [instance] : [], projectId)
                this.$showAskPermissionDialog({
                    noPermissionList,
                });
            } else {
                this.$showTips({
                    message: e.message || e,
                    theme: "error",
                });
            }
        },
        /**
         * 设置权限弹窗的参数
         */
        setPermissionConfig(
            resourceId,
            actionId,
            instanceId = [],
            projectId = this.$route.params.projectId
        ) {
            this.$showAskPermissionDialog({
                noPermissionList: [
                    {
                        actionId,
                        resourceId,
                        instanceId,
                        projectId,
                    },
                ],
            });
        },
    },
});

if (window.top === window.self) {
    // 只能以iframe形式嵌入
    location.href = `${WEB_URL_PREFIX}${location.pathname}`;
}

global.pipelineVue = new Vue({
    el: "#app",
    router: createRouter(store),
    i18n,
    store,
    components: {
        App,
    },
    template: "<App/>",
});
