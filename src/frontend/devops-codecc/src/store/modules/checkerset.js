/**
 * @file checker
 * @author blueking
 */

import http from '@/api'

export default {
    namespaced: true,
    state: {
        categoryList: [],
        codeLangs: [],
        checkerSetLanguage: []
    },
    mutations: {
        updateCategoryList (state, categoryList) {
            state.categoryList = categoryList
        },
        updateCodeLangs (state, codeLangs) {
            state.codeLangs = codeLangs
        },
        updateCheckerSetLanguage (state, checkerSetLanguage) {
            state.checkerSetLanguage = checkerSetLanguage
        }
    },
    actions: {
        count ({ commit, rootState }, data) {
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/checkerSet/count`, data).then(res => {
                const data = res.data || {}
                commit('updateCheckerSetLanguage', data)
                return data
            }).catch(e => {
                console.error(e)
            })
        },
        list ({ commit, rootState }, params) {
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/checkerSet/list`, params).then(res => {
                const data = res.data || {}
                return data
            }).catch(e => {
                console.error(e)
            })
        },
        otherList ({ commit, rootState }, params) {
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/checkerSet/otherList`, params).then(res => {
                const data = res.data || {}
                return data
            }).catch(e => {
                console.error(e)
            })
        },
        params ({ commit, rootState }) {
            return http.get(`${window.AJAX_URL_PREFIX}/defect/api/user/checkerSet/params`).then(res => {
                const data = res.data || {}
                const categoryList = res.data.catatories || []
                const codeLangs = res.data.codeLangs || []
                commit('updateCategoryList', categoryList)
                commit('updateCodeLangs', codeLangs)
                return data
            }).catch(e => {
                console.error(e)
            })
        },
        create ({ commit, rootState }, params) {
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/checkerSet`, params).then(res => {
                const data = res || {}
                return data
            }).catch(e => {
                console.error(e)
            })
        },
        edit ({ commit, rootState }, params) {
            const payload = {
                checkerSetName: params.checkerSetName,
                description: params.description,
                catagories: params.catagories
            }
            return http.put(`${window.AJAX_URL_PREFIX}/defect/api/user/checkerSet/${params.checkersetId}/baseInfo`, payload).then(res => {
                const data = res || {}
                return data
            }).catch(e => {
                console.error(e)
            })
        },
        detail ({ commit, rootState }, params) {
            return http.get(`${window.AJAX_URL_PREFIX}/defect/api/user/checkerSet/${params.checkersetId}/versions/${params.version}/detail`).then(res => {
                const data = res.data || {}
                return data
            }).catch(e => {
                console.error(e)
            })
        },
        save ({ commit, rootState }, params) {
            const payload = { checkerProps: params.checkerProps }
            return http.put(`${window.AJAX_URL_PREFIX}/defect/api/user/checkerSet/checkerSets/${params.checkersetId}/checkers`, payload).then(res => {
                const data = res || {}
                return data
            }).catch(e => {
                console.error(e)
            })
        },
        batchsave ({ commit, rootState }, params) {
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/checkerSet/checkers/all`, params).then(res => {
                const data = res || {}
                return data
            }).catch(e => {
                console.error(e)
            })
        },
        categoryList ({ commit, rootState }, params) {
            return http.get(`${window.AJAX_URL_PREFIX}/defect/api/user/checkerSet/categoryList`).then(res => {
                const data = res.data || {}
                return data
            }).catch(e => {
                console.error(e)
            })
        },
        manage ({ commit, rootState }, params) {
            const checkerSetId = params.checkerSetId
            delete params.checkerSetId
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/checkerSet/${checkerSetId}/management`, params).then(res => {
                const data = res || {}
                return data
            }).catch(e => {
                console.error(e)
            })
        },
        install ({ commit, rootState }, params) {
            const checkerSetId = params.checkerSetId
            delete params.checkerSetId
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/checkerSet/${checkerSetId}/relationships`, params).then(res => {
                const data = res || {}
                return data
            }).catch(e => {
                console.error(e)
            })
        },
        permission ({ commit, rootState }, params) {
            return http.post(`${window.AJAX_URL_PREFIX}/defect/api/user/checkerSet/userManagementPermission`, params).then(res => {
                const data = res || {}
                return data
            }).catch(e => {
                console.error(e)
            })
        }
    }
}
