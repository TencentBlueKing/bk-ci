import axios from 'axios'
import Vue from 'vue'

const request = axios.create({
    baseURL: API_URL_PREFIX,
    validateStatus: status => {
        if (status > 400) {
            console.warn(`HTTP 请求出错 status: ${status}`)
        }
        return status >= 200 && status <= 503
    },
    withCredentials: true,
    xsrfCookieName: 'backend_csrftoken', // 注入csrfToken
    xsrfHeaderName: 'X-CSRFToken' // 注入csrfToken
})

function errorHandler (error: object) {
    console.log('error catch', error)
    return Promise.reject(Error('网络出现问题，请检查你的网络是否正常'))
}

request.interceptors.response.use(response => {
    const { data: { code, data, message, status }, status: httpStatus } = response
    if (httpStatus === 401) {
        location.href = window.getLoginUrl()
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

Vue.prototype.$ajax = request

export default request
