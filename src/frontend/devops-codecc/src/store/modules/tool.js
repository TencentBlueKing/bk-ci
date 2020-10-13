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
        mapSimpleList: {},
        rules: {},
        checkers: []
    },
    mutations: {
        updateList (state, list) {
            // 注入视图层所需数据，如工具的代码问题页面路由名
            const mapList = {}
            list.forEach(tool => {
                tool.routes = {
                    defectList: `defect-${tool.pattern.toLocaleLowerCase()}-list`
                }
                mapList[tool.name] = tool
            })

            state.mapList = mapList
        },
        updateSimpleList (state, list) {
            const mapList = {}
            list.forEach(tool => {
                mapList[tool.name] = tool
            })

            state.mapSimpleList = mapList
        },
        updateRules (state, rules) {
            state.rules = rules
        },
        updateCheckers (state, checkers) {
            state.checkers = checkers
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
            commit('setMainContentLoading', true, { root: true })
            return http.get(`${window.AJAX_URL_PREFIX}/task/api/user/toolList`, { params: { isDetail: false } }).then(res => {
                const list = res.data || []
                commit('updateSimpleList', list)
                return list
            }).catch(e => e).finally(() => {
                commit('setMainContentLoading', false, { root: true })
            })
        },
        rules ({ commit, state, rootState }, params) {
            // return http.get('/tool/index?invoke=rules').then(res => {
            return http.get(`${window.AJAX_URL_PREFIX}/defect/api/user/checker/tasks/${params.taskId}/toolName/${params.toolName}/checkers`).then(res => {
                const rules = res.data || {}
                commit('updateRules', rules)
                return rules
            }).catch(e => e)
        },
        checker ({ commit, state, rootState }, params) {
            // return http.get('/tool/index?invoke=rules').then(res => {
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/checker/tasks/${params.taskId}/checkerSets`, { toolNames: params.toolNames }).then(res => {
                const checkers = res.data || {}
                commit('updateCheckers', checkers)
                return checkers
            }).catch(e => e)
        },
        relateCheckers ({ commit, state, rootState }, params) {
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/checker/tasks/${params.taskId}/checkerSets/relationship`, { toolCheckerSets: params.toolCheckerSets }).then(res => {
                const data = res.data || {}
                // commit('updateCheckers', checkers)
                return data
            }).catch(e => e)
        },
        updateCheckers ({ commit, state, rootState }, params) {
            return http.put(`${window.AJAX_URL_PREFIX}/defect/api/user/checker/tasks/${params.taskId}/tools/${params.toolName}/checkerSets/${params.checkerSetId}`, params.data).then(res => {
                const data = res.data || {}
                // commit('updateCheckers', checkers)
                return data
            }).catch(e => e)
        },
        updateCheckerParam ({ commit, state, rootState }, params) {
            return http.put(`${window.AJAX_URL_PREFIX}/defect/api/user/checker/taskId/${params.taskId}/tools/${params.toolName}/param/${params.paramValue}?checkerKey=${params.checkerKey}`).then(res => {
                const data = res.data || {}
                return data
            }).catch(e => e)
        },
        // 用户规则集列表
        checkerList ({ commit, state, rootState }, params) {
            // return http.get('/tool/index?invoke=rules').then(res => {
            return http.get(`${window.AJAX_URL_PREFIX}/defect/api/user/checker/tools/${params.toolName}/userCreatedCheckerSets`).then(res => {
                const data = res.data || {}
                // commit('updateCheckers', checkers)
                return data
            }).catch(e => e)
        },
        createChecker ({ commit, state, rootState }, params) {
            // return http.get('/tool/index?invoke=rules').then(res => {
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/checker/tasks/${params.taskId}/tools/${params.toolName}/checkerSets`, params.data).then(res => {
                // const data = res.data || {}
                return res
            }).catch(e => e)
        },
        changeRules ({ commit, rootState }, params) {
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/checker/tasks/${params.taskId}/toolName/${params.toolName}/checkers/configuration`, params).then(res => {
                const data = res.data || {}
                return data
            }).catch(e => e)
        },
        toolLog ({ commit, state, rootState }, params) {
            return http.get(`${window.AJAX_URL_PREFIX}/defect/api/user/tasklog?${json2Query(params)}`).then(res => {
                const data = res.data || {}
                return data
            }).catch(e => e)
        },
        // deprecated
        updateParamsAndCheckerSets ({ commit, state, rootState }, params) {
            return http.put(`${window.AJAX_URL_PREFIX}/task/api/user/tool/tasks/${params.taskId}/tools/paramJsonAndCheckerSets`, params).then(res => {
                const data = res.data || {}
                return data
            }).catch(e => e)
        }
    }
}
