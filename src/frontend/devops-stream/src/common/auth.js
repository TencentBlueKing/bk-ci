/**
 * @file auth
 * @author <%- author %>
 */

import store from '@/store'

const ANONYMOUS_USER = {
    id: null,
    isAuthenticated: false,
    username: 'anonymous'
}

let currentUser = {
    id: '',
    username: ''
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
        window.location.href = LOGIN_URL + '/?c_url=' + window.location.href + '&is_from_logout=1'
    },

    /**
     * 请求当前用户信息
     *
     * @return {Promise} promise 对象
     */
    requestCurrentUser () {
        let promise = null
        if (currentUser.isAuthenticated) {
            promise = new Promise((resolve, reject) => {
                resolve(currentUser)
            })
        } else {
            if (!store.state.user || !Object.keys(store.state.user).length) {
                // store action userInfo 里，如果请求成功会更新 state.user
                const req = store.dispatch('userInfo')
                promise = new Promise((resolve, reject) => {
                    req.then(resp => {
                        // 存储当前用户信息(全局)
                        currentUser = store.getters.user
                        currentUser.isAuthenticated = true
                        resolve(currentUser)
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
