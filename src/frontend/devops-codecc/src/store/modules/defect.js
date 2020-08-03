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
        lintDetail ({ commit, state, dispatch }, params, config = {}) {
            if (config.showLoading) {
                commit('setMainContentLoading', true, { root: true })
            }
            const { sortField, sortType, ...data } = params
            const query = { sortField, sortType }
            // return http.get('/defect/index?invoke=lintdetail', data, { params: query }).then(res => {
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/warn/detail`, data, { params: query, cancelPrevious: false }).then(res => {
                const detail = res.data || {}
                return detail
            }).catch(e => {
                console.error(e)
                return e
            }).finally(() => {
                if (config.showLoading) {
                    commit('setMainContentLoading', false, { root: true })
                }
            })
        },
        lintList ({ commit, state, dispatch }, params, config = {}) {
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
        batchEdit ({ commit }, data) {
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/warn/batch`, data).then(res => {
                return res
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
        report ({ commit, state, dispatch }, data) {
            if (data.showLoading) {
                commit('setMainContentLoading', true, { root: true })
            }
            const { startTime, endTime } = data
            const query = { startTime, endTime }
            return http.get(`${window.AJAX_URL_PREFIX}/defect/api/user/report/toolName/${data.toolId}`, { params: query }).then(res => {
                const charts = res.data || []
                return charts
            }).catch(e => {
                console.error(e)
            }).finally(() => {
                if (data.showLoading) {
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
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/operation/taskId/${data.taskId}`, data.funcId).then(res => {
                const records = res.data || []
                commit('updateOperateRecords', records)
                return records
            }).catch(e => e)
        },
        newVersion ({ commit, state, dispatch }, params) {
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/checker/tools/${params.toolName}/checkerSets/${params.checkerSetId}/versions/difference`, params).then(res => {
                const data = res.data || {}
                return data
            }).catch(e => {
                console.error(e)
            })
        },
        getWarnContent ({ commit, state, dispatch }, data) {
            return http.get(`${window.AJAX_URL_PREFIX}/defect/api/user/checker/detail/toolName/${data.toolName}?checkerKey=${data.checkerKey}`, data).then(res => {
                const content = res.data || []
                return content
            }).catch(e => {
                console.error(e)
            })
        },
        getTransferAuthorList ({ commit, rootState }) {
            return http.get(`${window.AJAX_URL_PREFIX}/defect/api/user/transferAuthor/list`).then(res => {
                const data = res.data || {}
                return data
            }).catch(e => {
                console.error(e)
            })
        },
        getBuildList ({ commit, rootState }, data) {
            return http.get(`${window.AJAX_URL_PREFIX}/defect/api/user/warn/tasks/${data.taskId}/buildInfos`).then(res => {
                const data = res.data || {}
                return data
            }).catch(e => {
                console.error(e)
            })
        },
        commentDefect ({ commit, state, dispatch }, params) {
            const { singleCommentId, userName, comment, ...query } = params
            const data = { singleCommentId, userName, comment }
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/warn/codeComment/toolName/${query.toolName}`, data, { params: query }).then(res => {
                return res || {}
            }).catch(e => {
                console.error(e)
            })
        },
        deleteComment ({ commit, state, dispatch }, params) {
            const { commentId, singleCommentId, toolName } = params
            return http.delete(`${window.AJAX_URL_PREFIX}/defect/api/user/warn/codeComment/commentId/${commentId}/singleCommentId/${singleCommentId}/toolName/${toolName}`).then(res => {
                return res || {}
            }).catch(e => {
                console.error(e)
            })
        },
        updateComment ({ commit, state, dispatch }, params) {
            const { commentId, toolName, singleCommentId, userName, comment } = params
            const data = { singleCommentId, userName, comment }
            return http.put(`${window.AJAX_URL_PREFIX}/defect/api/user/warn/codeComment/commentId/${commentId}/toolName/${toolName}`, data).then(res => {
                return res || {}
            }).catch(e => {
                console.error(e)
            })
        },
        gatherFile ({ commit, state, dispatch }, params) {
            const { taskId, toolName } = params
            return http.get(`${window.AJAX_URL_PREFIX}/defect/api/user/warn/gather/taskId/${taskId}/toolName/${toolName}`).then(res => {
                return res.data || {}
            }).catch(e => {
                console.error(e)
            })
        }
    }
}
