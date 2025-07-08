/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import axios from 'axios'
import Vue from 'vue'
import { bus } from './bus'

const request = axios.create({
    baseURL: API_URL_PREFIX,
    maxRedirects: 0,
    validateStatus: status => {
        if (status > 400) {
            console.warn(`HTTP 请求出错 status: ${status}`)
        }
        return status >= 200 && status <= 503
    },
    withCredentials: true
})

function errorHandler (error) {
    if (typeof error.response.data === 'undefined') {
        // HACK REDIRECT 302
        bus.$toggleLoginDialog(true)
    }
    return Promise.reject(error)
}

request.interceptors.response.use(response => {
    const { data: { data, status, message, code, result } } = response
    const httpStatus = response.status
    if (httpStatus === 401) {
        bus.$toggleLoginDialog(true)
    } else if (httpStatus === 503) {
        const errMsg = {
            status: httpStatus,
            message: (window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('err503')) || 'service is in deployment'
        }
        return Promise.reject(errMsg)
    } else if (httpStatus === 403) {
        const errorMsg = { httpStatus, code: httpStatus, message: message ?? (window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('err403')) }
        return Promise.reject(errorMsg)
    } else if ((typeof status !== 'undefined' && status !== 0) || (typeof result !== 'undefined' && !result)) {
        const errorMsg = { httpStatus, message, code: code || status, data }
        return Promise.reject(errorMsg)
    } else if (httpStatus === 400) {
        const errorMsg = { httpStatus, message: message ?? (window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('err400')) ?? 'service is abnormal' }
        return Promise.reject(errorMsg)
    } else if (httpStatus > 400) {
        const err = {
            message: `unknow Error httpStatus: ${httpStatus}`,
            httpStatus
        }
        return Promise.reject(err)
    }

    return response.data
}, errorHandler)

Vue.prototype.$ajax = request

export default request
