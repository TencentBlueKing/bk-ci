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
import { pluginUrlParse } from '@/utils/pipelineConst'
const atomFieldMixin = {
    props: {
        type: {
            type: String
        },
        name: {
            type: String
        },
        value: {
            type: String,
            default: ''
        },
        disabled: {
            type: Boolean,
            default: false
        },
        handleChange: {
            type: Function,
            default: () => () => {}
        },
        element: {
            type: Object,
            default: () => ({})
        },
        container: {
            type: Object,
            default: () => ({})
        },
        rule: {
            type: Object,
            default: () => ({})
        },
        component: String,
        required: Boolean,
        hasError: {
            type: Boolean
        },
        hidden: {
            type: Boolean,
            default: false
        },
        descTooltips: {
            type: String,
            default: ''
        }
    },
    data () {
        return {
            title: '',
            readOnly: false
        }
    },
    watch: {
        value (value, oldValue) {
            value !== oldValue && this.$emit('input', value)
        }
    },
    mounted () {
        const ele = document.querySelector('.atom-form-box')
        if (this.descTooltips.length && this.disabled) {
            this.title = this.descTooltips
            this.readOnly = true
        } else if ((ele && ele.classList.contains('readonly')) || this.disabled) {
            this.title = this.value
            this.readOnly = true
        }
    },
    methods: {
        urlParse: pluginUrlParse,

        getResponseData (response, dataPath = 'data.records', defaultVal = []) {
            try {
                switch (true) {
                    case Array.isArray(response.data): {
                        return response.data
                    }
                    case response.data && response.data.record && Array.isArray(response.data.record): {
                        return response.data.record
                    }
                    default: {
                        const path = dataPath.split('.')
                        let result = response
                        let pos = 0
                        while (path[pos] && result) {
                            const key = path[pos]
                            result = result[key]
                            pos++
                        }
                        if (pos === path.length && Object.prototype.toString.call(result) === Object.prototype.toString.call(defaultVal)) {
                            return result
                        } else {
                            throw Error(this.$t('editPage.failToGetData'))
                        }
                    }
                }
            } catch (e) {
                console.error(e)
                return defaultVal
            }
        }
    }
}

export default atomFieldMixin
