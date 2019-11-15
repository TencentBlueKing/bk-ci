/**
 * @file app store
 * @author blueking
 */

import http from '@/api'
import { json2Query } from '@/common/util'

export default {
    namespaced: true,
    state: {
        mapList: {},
        rules: {}
    },
    mutations: {
        updateList (state, list) {
            // 注入视图层所需数据，如工具的告警管理页面路由名
            const mapList = {}
            list.forEach(tool => {
                tool.routes = {
                    defectList: `defect-${tool.pattern.toLocaleLowerCase()}-list`
                }
                mapList[tool.name] = tool
            })

            state.mapList = mapList
        },
        updateRules (state, rules) {
            state.rules = rules
        }
    },
    actions: {
        list ({ commit, state, rootState }) {
            if (!rootState.projectId) {
                return
            }
            if (rootState.loaded['tool/updateList'] === true) {
                return state.list
            }

            // return http.get('/tool/index?invoke=list').then(res => {
            return http.get(`${window.AJAX_URL_PREFIX}/task/api/user/toolList`, { params: { isDetail: true } }).then(res => {
                const list = res.data || []
                commit('updateList', list)
                return list
            }).catch(e => e)
        },
        updated ({ commit, state, rootState }) {
            return http.get(`${window.AJAX_URL_PREFIX}/task/api/user/toolList`, { params: { isDetail: true } }).then(res => {
                const list = res.data || []
                commit('updateList', list)
                return list
            }).catch(e => e)
        },
        rules ({ commit, state, rootState }, toolName) {
            // return http.get('/tool/index?invoke=rules').then(res => {
            return http.get(`${window.AJAX_URL_PREFIX}/defect/api/user/checker/toolName/${toolName}`).then(res => {
                const rules = res.data || {}
                commit('updateRules', rules)
                return rules
            }).catch(e => e)
        },
        changeRules ({ commit, rootState }, params) {
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/checker/config/package`, params).then(res => {
                const data = res.data || {}
                return data
            }).catch(e => e)
        },
        toolLog ({ commit, state, rootState }, params) {
            return http.get(`${window.AJAX_URL_PREFIX}/defect/api/user/tasklog?${json2Query(params)}`).then(res => {
                const data = res.data || {}
                return data
            }).catch(e => e)
        }
    }
}
