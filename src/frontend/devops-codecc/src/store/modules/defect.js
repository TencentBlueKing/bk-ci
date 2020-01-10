/**
 * @file app store
 * @author blueking
 */

import http from '@/api'

export default {
    namespaced: true,
    state: {
        lintListData: {
            lintFileList: {
                content: []
            }
        },
        lintDetail: {
            fileContent: ''
        },
        dupcList: [],
        dupcDetail: {
            blockInfoList: [{}]
        },
        records: {}
    },
    mutations: {
        updateLintList (state, list) {
            state.lintListData = { ...state.lintListData, ...list }
        },
        updateDupcList (state, list) {
            state.dupcList = list
        },
        updateDupcDetail (state, detail) {
            state.dupcDetail = { ...state.dupcDetail, ...detail }
        },
        updateLintDetail (state, detail) {
            state.lintDetail = { ...state.lintDetail, ...detail }
        },
        updateOperateRecords (state, records) {
            state.records = records
        }
    },
    actions: {
        lintDetail ({ commit, state, dispatch }, params, config) {
            if (config.showLoading) {
                commit('setMainContentLoading', true, { root: true })
            }
            const { sortField, sortType, ...data } = params
            const query = { sortField, sortType }
            // return http.get('/defect/index?invoke=lintdetail', data, { params: query }).then(res => {
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/warn/detail`, data, { params: query }).then(res => {
                const detail = res.data || {}
                return detail
            }).catch(e => {
                console.error(e)
            }).finally(() => {
                if (config.showLoading) {
                    commit('setMainContentLoading', false, { root: true })
                }
            })
        },
        lintList ({ commit, state, dispatch }, params, config) {
            if (config.showLoading) {
                commit('setMainContentLoading', true, { root: true })
            }
            const { pageNum, pageSize, sortField, sortType, ...data } = params
            const query = { pageNum, pageSize, sortField, sortType }
            // return http.post('/defect/index?invoke=lintlist', data, { params: query }).then(res => {
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/warn/list`, data, { params: query }).then(res => {
                const list = res.data || {}
                commit('updateLintList', list)
                return list
            }).catch(e => {
                console.error(e)
            }).finally(() => {
                if (config.showLoading) {
                    commit('setMainContentLoading', false, { root: true })
                }
            })
        },
        lintParams ({ commit }, params) {
            // return http.get('/defect/index?invoke=lintparams').then(res => {
            return http.get(`${window.AJAX_URL_PREFIX}/defect/api/user/warn/checker/authors/toolName/${params.toolId}`).then(res => {
                const params = res.data || {}
                return params
            })
        },
        authorEdit ({ commit }, data, config) {
            if (config.showLoading) {
                commit('setMainContentLoading', true, { root: true })
            }
            return http.put(`${window.AJAX_URL_PREFIX}/defect/api/user/warn/author`, data)
                .finally(() => {
                    if (config.showLoading) {
                        commit('setMainContentLoading', false, { root: true })
                    }
                })
        },
        dupcList ({ commit, state, dispatch }) {
            return http.get('/defect/index?invoke=dupclist').then(res => {
                const list = res.data || []
                commit('updateDupcList', list)
                return list
            }).catch(e => e)
        },
        dupcDetail ({ commit, state, dispatch }) {
            return http.get('/defect/index?invoke=dupcdetail').then(res => {
                const detail = res.data || {}
                commit('updateDupcDetail', detail)
                return detail
            })
        },
        publish ({ commit, state, dispatch }, index) {
            const data = { name: state.list[index].name }
            return http.post(`${window.AJAX_URL_PREFIX}/tool/publish`, data).then(res => {
                commit('updateTool', index)
                return res
            }).catch(e => e)
        },
        sort ({ commit, state, dispatch }, data) {
            return http.post(`${window.AJAX_URL_PREFIX}/tool/sort`, data)
        },
        report ({ commit, state, dispatch }, data, config) {
            if (config.showLoading) {
                commit('setMainContentLoading', true, { root: true })
            }
            return http.get(`${window.AJAX_URL_PREFIX}/defect/api/user/report/toolName/${data.toolId}`, data).then(res => {
                const charts = res.data || []
                return charts
            }).catch(e => {
                console.error(e)
            }).finally(() => {
                if (config.showLoading) {
                    commit('setMainContentLoading', false, { root: true })
                }
            })
        },
        fileContent ({ commit, state, dispatch }, params) {
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/warn/fileContentSegment`, params).then(res => {
                const detail = res.data || {}
                return detail
            }).catch(e => {
                console.error(e)
            })
        },
        getOperatreRecords ({ commit, state, dispatch, rootState }, data) {
            if (rootState.task.status.status === 1) {
                return
            }
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/operation/taskId/${data.taskId}?toolName=${data.toolName}`, data.funcId).then(res => {
                const records = res.data || []
                commit('updateOperateRecords', records)
                return records
            }).catch(e => e)
        }
    }
}
