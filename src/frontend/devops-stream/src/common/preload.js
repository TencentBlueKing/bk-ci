/**
 * @file 页面公共请求即每切换 router 时都必须要发送的请求
 * @author Blueking
 */

import store from '@/store'

const config = {
    fromCache: false,
    cancelWhenRouteChange: false
}

/**
 * 获取 user 信息
 *
 * @return {Promise} promise 对象
 */
function getUser () {
    return store.dispatch('userInfo', config)
}

export default function () {
    return Promise.all([
        getUser()
    ])
}
