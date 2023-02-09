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
import { rely } from '../../utils/util'
import { PLUGIN_URL_PARAM_REG, pluginUrlParse } from '@/utils/pipelineConst'

export default {
    props: {
        options: {
            type: Array,
            default: []
        },
        optionsConf: {
            type: Object,
            default: () => ({})
        },
        getAtomKeyModal: {
            type: Function,
            default: () => () => {}
        },
        container: {
            type: Object
        }
    },
    data () {
        return {
            webUrl: WEB_URL_PREFIX
        }
    },
    computed: {
        addItemUrl () {
            const { webUrl, urlParse, mergedOptionsConf: { itemTargetUrl }, $route: { params } } = this
            const originUrl = /^(http|https):\/\//.test(itemTargetUrl) ? itemTargetUrl : webUrl + itemTargetUrl

            return urlParse(originUrl, {
                bkPoolType: this?.container?.dispatchType?.buildType,
                ...params
            })
        },
        mergedOptionsConf () {
            return Object.assign({}, {
                url: '',
                paramId: 'id',
                paramName: 'name',
                searchable: false,
                clearable: false,
                multiple: false
            }, this.optionsConf)
        },
        hasUrl () {
            return this.mergedOptionsConf && this.mergedOptionsConf.url && typeof this.mergedOptionsConf.url === 'string'
        },
        urlParamKeys () {
            if (this.hasUrl) {
                const paramKey = this.mergedOptionsConf.url.match(PLUGIN_URL_PARAM_REG)
                return paramKey
                    ? paramKey.map(key => key.replace(PLUGIN_URL_PARAM_REG, (...args) => {
                        const [, s1, s2] = args
                        return JSON.stringify({
                            key: s1,
                            optional: !!s2
                        })
                    })).map(item => JSON.parse(item))
                    : []
            }
            return []
        },
        queryParams () {
            const { atomValue = {}, $route: { params = {} } } = this
            return {
                bkPoolType: this?.container?.dispatchType?.buildType,
                ...params,
                ...atomValue
            }
        },
        isLackParam () {
            return this.urlParamKeys.some(({ key, optional }) => {
                if (optional) return false
                if (Object.prototype.hasOwnProperty.call(this.atomValue, key)) {
                    const keyModal = this.getAtomKeyModal(key)
                    if (!keyModal) {
                        return false
                    }
                    if (
                        !keyModal.required // 字段允许为空时
                        || keyModal.hidden // 字段不可见时
                        || (keyModal.rely && !rely(keyModal, this.atomValue)) // 字段配置了rely且返回false时，字段不可见
                    ) {
                        return false
                    }
                }
                return this.isNullValue(this.queryParams[key])
            })
        }
    },
    methods: {
        isParamsChanged (newQueryParams, oldQueryParams) {
            return this.urlParamKeys.some(({ key }) => newQueryParams[key] !== oldQueryParams[key])
        },
        isNullValue (val) {
            return typeof val === 'undefined' || val === null || val === ''
        },
        urlParse: pluginUrlParse
    }
}
