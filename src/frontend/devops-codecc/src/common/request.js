import axios from 'axios'
import Vue from 'vue'
import * as cookie from 'js-cookie'

const request = axios.create({
    baseURL: window.DEVOPS_API_URL,
    withCredentials: true
})

request.interceptors.request.use(config => {
    const routePid = getCurrentPid()
    return {
        ...config,
        headers: routePid ? {
            ...(config.headers || {}),
            'X-DEVOPS-PROJECT-ID': routePid
        } : config.headers
    }
}, function (error) {
    return Promise.reject(error)
})

request.interceptors.response.use(response => {
    const httpStatus = response.status
    if (httpStatus !== 200) {
        const errMsg = {
            status: httpStatus,
            message: response.data.message
        }
        return Promise.reject(errMsg)
    }
    return response.data
}, function (error) {
    return Promise.reject(error)
})

function getCurrentPid () {
    try {
        const pathPid = window.mainComponent && window.mainComponent.$route && window.mainComponent.$route.params && window.mainComponent.$route.params.projectId
        const cookiePid = cookie.get('X_DEVOPS_PROJECT_ID')
        return pathPid || cookiePid
    } catch (e) {
        return undefined
    }
}

Vue.prototype.$ajax = request

export default request
