/**
 * @file app store
 * @author blueking
 */

import http from '@/api'

export default {
    namespaced: true,
    state: {
        list: {
            enableTasks: [],
            disableTasks: []
        },
        detail: {
            nameEn: '',
            nameCn: '',
            langs: [],
            enableToolList: [],
            disableToolList: []
        },
        taskLog: {
            enabaleToolList: [],
            taskLogPage: {
                totalPages: 0,
                number: 0,
                size: 0,
                content: []
            },
            lastAnalysisResult: {}
        },
        ignore: {},
        ignoreTree: {},
        codes: {},
        status: {}
    },
    getters: {
    },
    mutations: {
        updateList (state, list) {
            state.list = { ...state.list, ...list }
        },
        updateDetail (state, detail) {
            state.detail = Object.assign({}, state.detail, detail)
        },
        updateIgnore (state, ignore) {
            state.ignore = ignore
        },
        updateIgnoreTree (state, ignoreTree) {
            state.ignoreTree = ignoreTree
        },
        updateTaskLog (state, taskLog) {
            state.taskLog = Object.assign({}, state.taskLog, taskLog)
        },
        updateCodeBase (state, codes) {
            state.codes = codes
        },
        updateStatus (state, status) {
            state.status = status
        }
    },
    actions: {
        status ({ commit, state, rootState }) {
            if (!rootState.projectId) {
                return
            }
            return http.get(`${window.AJAX_URL_PREFIX}/task/api/user/task/status`).then(res => {
                const status = res.data || {}
                commit('updateStatus', status)
                return status
            }).catch(e => e)
        },
        list ({ commit, state, rootState }) {
            if (!rootState.projectId) {
                return
            }
            return http.get(`${window.AJAX_URL_PREFIX}/task/api/user/task`).then(res => {
                const list = res.data || {}
                return list
            }).catch(e => e)
        },
        basicList ({ commit, state, rootState }) {
            if (!rootState.projectId) {
                return
            }
            return http.get(`${window.AJAX_URL_PREFIX}/task/api/user/task/base`).then(res => {
                const list = res.data || {}
                commit('updateList', list)
                return list
            }).catch(e => e)
        },
        detail ({ commit, state, rootState }, config) {
            if (state.status.status === 1) {
                return
            }
            if (config.status !== 1 && config.hasOwnProperty('status')) {
                return
            }
            if (config.showLoading) {
                commit('setMainContentLoading', true, { root: true })
            }
            // return http.get('/task/index?invoke=detail').then(res => {
            return http.get(`${window.AJAX_URL_PREFIX}/task/api/user/task/taskInfo`).then(res => {
                const detail = res.data || []
                commit('updateDetail', detail)
                return detail
            }).catch(e => {
                console.error(e)
            }).finally(() => {
                if (config.showLoading) {
                    commit('setMainContentLoading', false, { root: true })
                }
            })
        },
        basicInfo ({ commit, state, rootState }, params) {
            return http.get(`${window.AJAX_URL_PREFIX}/task/api/user/task/taskId/${params.taskId}`).then(res => {
                const task = res.data || {}
                return task
            }).catch(e => e)
        },
        memberInfo ({ commit, state, rootState }) {
            return http.get(`${window.AJAX_URL_PREFIX}/task/api/user/task/memberList`).then(res => {
                const task = res.data || {}
                return task
            }).catch(e => e)
        },
        updateBasicInfo ({ commit, state, rootState }, params) {
            return http.put(`${window.AJAX_URL_PREFIX}/task/api/user/task`, params).then(res => {
                const data = res.data || {}
                return data
            }).catch(e => e)
        },
        log ({ commit, state, rootState }, data) {
            // return http.get(`${window.AJAX_URL_PREFIX}/task/api/user/taskLog`).then(res => {
            return http.post('/task/index?invoke=log', data).then(res => {
                const log = res.data || []
                commit('updateTaskLog', log)
                return log
            }).catch(e => e)
        },
        create ({ commit, rootState }, data) {
            data.projectName = (rootState.project.list.find(project => project.projectId === rootState.projectId) || {}).projectName
            // return http.post('/task/index?invoke=create', data).then(res => {
            return http.post(`${window.AJAX_URL_PREFIX}/task/api/user/task`, data).then(res => {
                const data = res.data || {}
                commit('updateTaskId', data.taskId, { root: true })
                return data
            })
        },
        update ({ commit, rootState }, data) {
            data.projectName = (rootState.project.list.find(project => project.projectId === rootState.projectId) || {}).projectName
            return http.put(`${window.AJAX_URL_PREFIX}/task/api/user/task`, data).then(res => {
                const data = res.data || {}
                commit('updateTaskId', data.taskId, { root: true })
                return data
            })
        },
        createIgnore ({ commit, rootState }, data) {
            // return http.post('/task/index?invoke=create', data).then(res => {
            return http.post(`${window.AJAX_URL_PREFIX}/task/api/user/task/filter/path`, data).then(res => {
                const create = res.data || []
                return create
            }).catch(e => e)
        },
        deleteIgnore ({ commit, rootState }, params) {
            return http.delete(`${window.AJAX_URL_PREFIX}/task/api/user/task/filter/path`, params).then(res => {
                const data = res.data || []
                return data
            }).catch(e => e)
        },
        ignore ({ commit, state, rootState }, taskId) {
            return http.get(`${window.AJAX_URL_PREFIX}/task/api/user/task/filter/path/${taskId}`).then(res => {
            // return http.get('/task/index?invoke=ignore').then(res => {
                const ignore = res.data || {}
                commit('updateIgnore', ignore)
                return ignore
            }).catch(e => e)
        },
        ignoreTree ({ commit, state, rootState }) {
            return http.get(`${window.AJAX_URL_PREFIX}/task/api/user/task/filter/path/tree`).then(res => {
            // return http.get('/task/index?invoke=ignoreTree').then(res => {
                const ignoreTree = res.data || {}
                commit('updateIgnoreTree', ignoreTree)
                return ignoreTree
            }).catch(e => e)
        },
        trigger ({ commit, rootState }, data) {
            return http.put(`${window.AJAX_URL_PREFIX}/task/api/user/task/timing`, data).then(res => {
                const data = res.data || {}
                return data
            })
        },
        getRepoList (store, params = {}) {
            // return http.get('/repo/index?invoke=list', { params }).then(res => {
            return http.get(`${window.AJAX_URL_PREFIX}/task/api/user/tool/repos/projCode/${params.projCode}`).then(res => {
                const data = res.data || []
                return data
            })
        },
        checkname (store, params) {
            // return http.get('/task/index?invoke=checkname', { params, globalError: false })
            return http.get(`${window.AJAX_URL_PREFIX}/task/api/user/task/duplicate/streamName/${params.nameEn}`, { globalError: false })
        },
        addTool ({ commit }, data) {
            return http.post(`${window.AJAX_URL_PREFIX}/task/api/user/tool`, data)
        },
        changeToolStatus ({ commit }, data, config) {
            if (config.showLoading) {
                commit('setMainContentLoading', true, { root: true })
            }
            return http.put(`${window.AJAX_URL_PREFIX}/task/api/user/tool/status`, data)
                .finally(() => {
                    if (config.showLoading) {
                        commit('setMainContentLoading', false, { root: true })
                    }
                })
        },
        overView ({ commit }, data, config) {
            if (config.showLoading) {
                commit('setMainContentLoading', true, { root: true })
            }
            // return http.post('/task/index?invoke=overview', data).then(res => {
            //     return res.data || {}
            // })
            return http.get(`${window.AJAX_URL_PREFIX}/task/api/user/task/overview/${data.taskId}`).then(res => {
                const data = res.data || []
                return data
            }).finally(() => {
                if (config.showLoading) {
                    commit('setMainContentLoading', false, { root: true })
                }
            })
        },
        startManage ({ commit }, taskId) {
            return http.put(`${window.AJAX_URL_PREFIX}/task/api/user/task/start`)
        },
        stopManage ({ commit }, data) {
            return http.put(`${window.AJAX_URL_PREFIX}/task/api/user/task/stop`, data)
        },
        getCodeMessage ({ commit }, store) {
            return http.get(`${window.AJAX_URL_PREFIX}/task/api/user/task/code/lib`).then(res => {
                const data = res.data || []
                commit('updateCodeBase', data)
                return data
            }).catch(e => e)
        },
        saveCodeMessage ({ commit }, params) {
            return http.put(`${window.AJAX_URL_PREFIX}/task/api/user/task/code/lib`, params)
        },
        triggerAnalyse ({ commit }) {
            return http.post(`${window.AJAX_URL_PREFIX}/task/api/user/task/execute`)
        },
        getBranches ({ commit }, data) {
            return http.get(`${window.AJAX_URL_PREFIX}/task/api/user/tool/branches?projCode=${data.projCode}&url=${data.url}&type=${data.type}`)
        }
    }
}
