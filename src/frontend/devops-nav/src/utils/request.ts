import { showLoginPopup } from '@/utils/util'
import axios, { AxiosError, AxiosResponse, CreateAxiosDefaults } from 'axios'
import cookie from 'js-cookie'
import Vue from 'vue'

const request = axios.create({
    baseURL: API_URL_PREFIX,
    maxRedirects: 0,
    validateStatus: status => {
        if (status > 400) {
            console.warn(`HTTP 请求出错 status: ${status}`)
        }
        return status >= 200 && status <= 503
    },
    withCredentials: true,
    xsrfCookieName: 'paas_perm_csrftoken', // 注入csrfToken
    xsrfHeaderName: 'X-CSRFToken' // 注入csrfToken
} as CreateAxiosDefaults)

function errorHandler (error: AxiosError) {
    if (typeof error.response.data === 'undefined') {
        // HACK REDIRECT 302
        showLoginPopup()
    }
    return Promise.reject(Error('网络出现问题，请检查你的网络是否正常'))
}

request.interceptors.request.use(config => {
    if (/(\/?ms\/backend|\/?backend)/.test(config.url)) {
        return config
    }

    const routePid = getCurrentPid()
    return {
        ...config,
        headers: routePid
            ? {
                'X-DEVOPS-PROJECT-ID': routePid,
                ...(config.headers || {})
            }
            : config.headers
    }
}, function (error) {
    return Promise.reject(error)
})

request.interceptors.response.use((response: AxiosResponse) => {
    const { data: { code, data, message, status }, status: httpStatus } = response

    if (httpStatus === 401) {
        showLoginPopup()
    } else if (httpStatus === 503) {
        return Promise.reject({ // eslint-disable-line
            status: httpStatus,
            message: '服务维护中，请稍候...'
        })
    } else if (httpStatus === 418) {
        console.log('no permission')
    } else if (httpStatus === 403) {
        const errorMsg = { httpStatus, code: httpStatus, message }
        return Promise.reject(errorMsg)
    } else if (httpStatus >= 400) {
        const err = {
            message: message || `unknow Error httpStatus: ${httpStatus}`,
            status,
            httpStatus
        }
        return Promise.reject(err)
    } else if ((typeof code !== 'undefined' && code !== 0) || (typeof status !== 'undefined' && status !== 0)) {
        let msg = message
        if (Object.prototype.toString.call(message) === '[object Object]') {
            msg = Object.keys(message).map(key => message[key].join(';')).join(';')
        } else if (Object.prototype.toString.call(message) === '[object Array]') {
            msg = message.join(';')
        }
        const errorMsg = { httpStatus, message: msg, code: code || status }
        return Promise.reject(errorMsg)
    }

    return data
}, errorHandler)

const getCurrentPid = () => {
    try {
        // @ts-ignore
        const cookiePid = cookie.get(X_DEVOPS_PROJECT_ID)
        return window.GLOBAL_PID || cookiePid
    } catch (e) {
        return undefined
    }
}

Vue.prototype.$ajax = request

export default request
