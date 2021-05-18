/**
 * @file app store
 * @author blueking
 */

import http from '@/api'

export default {
    namespaced: true,
    state: {
        list: []
    },
    getters: {
    },
    mutations: {
        updateList (state, list) {
            state.list = list
        }
    },
    actions: {
        list ({ commit, state, rootState }) {
            if (rootState.loaded['project/updateList'] === true) {
                return state.list
            }

            // return http.get('/project/index?invoke=list').then(res => {
            return http.get(`${window.AJAX_URL_PREFIX}/task/api/user/projects`).then(res => {
                const list = res.data || []
                commit('updateList', list)
                return list
            }).catch(e => e)
        }
    }
}
