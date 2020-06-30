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

export default {
    props: {
        options: {
            type: Array,
            default: []
        },
        optionsConf: {
            type: Object,
            default: () => ({})
        }
    },
    data () {
        return {
            webUrl: WEB_URL_PIRFIX
        }
    },
    watch: {
    },
    computed: {
        addItemUrl () {
            const { webUrl, urlParse, mergedOptionsConf: { itemTargetUrl }, $route: { params } } = this
            const originUrl = /^(http|https):\/\//.test(itemTargetUrl) ? itemTargetUrl : webUrl + itemTargetUrl

            return urlParse(originUrl, params)
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
                const paramKey = this.mergedOptionsConf.url.match(/\{(.*?)\}/g)
                return paramKey ? paramKey.map(key => key.replace(/\{(.*?)\}/, '$1')) : []
            }
            return []
        },
        queryParams () {
            const { atomValue = {}, $route: { params = {} } } = this
            return {
                ...params,
                ...atomValue
            }
        },
        isLackParam () {
            return this.urlParamKeys.some(key => {
                return this.queryParams.hasOwnProperty(key) && (typeof this.queryParams[key] === 'undefined' || this.queryParams[key] === null || this.queryParams[key] === '')
            })
        }
    },
    methods: {
        urlParse (originUrl, query) {
            /* eslint-disable */
            return new Function('ctx', `return '${originUrl.replace(/\{(.*?)\}/g, '\'\+ ctx.$1 \+\'')}'`)(query)
            /* eslint-enable */
        }
    }
}
