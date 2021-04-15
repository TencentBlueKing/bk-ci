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
        records: {},
        defaultCheckset: {
            1: {
                name: 'C#',
                ccn: 'codecc_default_ccn_csharp',
                dupc: 'codecc_default_dupc_csharp',
                cloc: 'cloc_csharp'
            },
            2: {
                name: 'C/C++',
                ccn: 'codecc_default_ccn_cpp',
                dupc: 'codecc_default_dupc_cpp',
                cloc: 'cloc_cpp'
            },
            4: {
                name: 'JAVA',
                ccn: 'codecc_default_ccn_java',
                dupc: 'codecc_default_dupc_java',
                cloc: 'cloc_java'
            },
            8: {
                name: 'PHP',
                ccn: 'codecc_default_ccn_php',
                cloc: 'cloc_php'
            },
            16: {
                name: 'OC/OC++',
                ccn: 'codecc_default_ccn_oc',
                dupc: 'codecc_default_dupc_oc',
                cloc: 'cloc_oc'
            },
            32: {
                name: 'Python',
                ccn: 'codecc_default_ccn_python',
                dupc: 'codecc_default_dupc_python',
                cloc: 'cloc_python'
            },
            64: {
                name: 'JS',
                ccn: 'codecc_default_ccn_js',
                dupc: 'codecc_default_dupc_js',
                cloc: 'cloc_js'
            },
            128: {
                name: 'Ruby',
                ccn: 'codecc_default_ccn_ruby',
                cloc: 'cloc_ruby'
            },
            256: {
                name: 'LUA',
                ccn: 'codecc_default_ccn_lua',
                cloc: 'cloc_lua'
            },
            512: {
                name: 'Golang',
                ccn: 'codecc_default_ccn_go',
                dupc: 'codecc_default_dupc_go',
                cloc: 'cloc_go'
            },
            1024: {
                name: 'Swift',
                ccn: 'codecc_default_ccn_swift',
                cloc: 'cloc_swift'
            },
            2048: {
                name: 'TS',
                cloc: 'cloc_ts'
            },
            4096: {
                name: 'Kotlin',
                dupc: 'codecc_default_dupc_kotlin',
                cloc: 'cloc_kotlin'
            },
            8192: {
                name: 'Dart',
                cloc: 'cloc_dart'
            },
            16384: {
                name: 'Solidity',
                cloc: 'cloc_solidity'
            },
            1073741824: {
                name: '其他',
                cloc: 'standard_cloc'
            }
        }
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
        lintParams ({ commit }, data) {
            return http.get(`${window.AJAX_URL_PREFIX}/defect/api/user/warn/checker/authors/toolName/${data.toolId}`).then(res => {
                const params = res.data || {}
                return params
            })
        },
        lintOtherParams ({ commit }, data) {
            return http.get(`${window.AJAX_URL_PREFIX}/defect/api/user/warn/checker/authors/toolName/${data.toolId}?status=${data.status}`).then(res => {
                const params = res.data || {}
                return params
            })
        },
        lintSearchParams ({ commit }, data) {
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/warn/initpage`, data, { cancelPrevious: false }).then(res => {
                const list = res.data || {}
                return list
            }).catch(e => {
                console.error(e)
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
        },
        lintListCloc ({ commit }, params) {
            return http.get(`${window.AJAX_URL_PREFIX}/defect/api/user/warn/list/toolName/${params.toolId}/orderBy/${params.type}`).then(res => {
                const params = res.data || {}
                return params
            })
        },
        oauthUrl ({ commit, state, dispatch }, params) {
            return http.get(`${window.AJAX_URL_PREFIX}/ms/defect/api/user/repo/oauth/url?toolName=${params.toolName}`).then(res => {
                return res.data || {}
            }).catch(e => {
                console.error(e)
            })
        }
    }
}
