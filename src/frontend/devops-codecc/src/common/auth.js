/**
 * @file auth
 * @author blueking
 */

import _ from 'lodash'
import store from '@/store'

const ANONYMOUS_USER = {
    id: null,
    isAuthenticated: false,
    username: 'anonymous',
    avatarUrl: null,
    chineseName: 'anonymous',
    phone: null,
    email: null
}

let currentUser = {
    avatar_url: '',
    bkpaas_user_id: '',
    chinese_name: '',
    username: ''
}

/**
 * 转换 user 对象，注意 camelCase
 *
 * @param {Object} data 待转换的对象
 *
 * @return {Object} 结果
 */
const transformUserData = data => {
    const user = {}
    Object.keys(data).forEach((key, index) => {
        const value = data[key]
        key = _.camelCase(key)
        user[key] = value
    })
    return user
}

export default {
    /**
     * 未登录状态码
     */
    HTTP_STATUS_UNAUTHORIZED: 401,

    /**
     * 获取当前用户
     *
     * @return {Object} 当前用户信息
     */
    getCurrentUser () {
        return currentUser
    },

    /**
     * 跳转到登录页
     */
    redirectToLogin () {
        window.location.href = window.PAAS_SERVICE_URL + '/?c_url=' + window.location.href
    },

    /**
     * 请求当前用户信息
     *
     * @return {Promise} promise 对象
     */
    requestCurrentUser () {
        let promise = null
        if (currentUser.bkpaas_user_id) {
            promise = new Promise((resolve, reject) => {
                const user = transformUserData(currentUser)
                if (user.code && user.code === 'Unauthorized') {
                    user.isAuthenticated = false
                } else {
                    user.isAuthenticated = true
                }
                resolve(user)
            })
        } else {
            if (!store.state.user || !Object.keys(store.state.user).length) {
                const req = store.dispatch('userInfo')
                promise = new Promise((resolve, reject) => {
                    req.then(resp => {
                        const user = transformUserData(resp)
                        if (user.code && user.code === 'Unauthorized') {
                            user.isAuthenticated = false
                        } else {
                            user.isAuthenticated = true
                        }

                        // 存储当前用户信息(全局)
                        currentUser = store.getters.user
                        resolve(user)
                    }, err => {
                        if (err.response.status === this.HTTP_STATUS_UNAUTHORIZED || err.crossDomain) {
                            resolve({ ...ANONYMOUS_USER })
                        } else {
                            reject(err)
                        }
                    })
                })
            }
        }

        return promise
    }
}
