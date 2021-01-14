/**
 * @file checker
 * @author blueking
 */

import http from '@/api'

export default {
    namespaced: true,
    state: {
    },
    mutations: {
    },
    actions: {
        count ({ commit, rootState }, data) {
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/checker/count`, data).then(res => {
                const data = res.data || {}
                return data
            }).catch(e => {
                console.error(e)
            })
        },
        list ({ commit, rootState }, params) {
            const { pageNum, pageSize, sortField, sortType, ...data } = params
            const query = { pageNum, pageSize, sortField, sortType }
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/checker/list`, data, { params: query }).then(res => {
                const data = res.data || {}
                return data
            }).catch(e => {
                console.error(e)
            })
        }
    }
}
