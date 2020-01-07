import request from './request'
import { transformObj } from './util'
import { PROJECT_API_URL_PREFIX } from '../store/constants'

let currentUser: User

export default {

    getCurrentUser () {
        return currentUser
    },
    getAnonymousUser () {
        return {
            id: '',
            isAuthenticated: false,
            username: 'anonymous',
            chineseName: 'anonymous'
        }
    },
    redirectToLogin () {
        window.location.href = LOGIN_SERVICE_URL + '/?c_url=' + window.location.href
    },
    async requestCurrentUser (refresh = false) {
       if (currentUser && !refresh) { // 如果已经获取到用户了， 直接返回
            return currentUser
        }

        const endpoint = `${PROJECT_API_URL_PREFIX}/user/users`
        const response = await request.get(endpoint)
        // 存储当前用户信息(全局)
        currentUser = {
            ...transformObj(response),
            isAuthenticated: true
        }
        return currentUser
    }
}
