const Vue = window.Vue
const vue = new Vue()
const prefix = 'store/api'
const processPerfix = 'process/api'
const qualityPerfix = 'quality/api'

export default {
    getMemberView (params) {
        return vue.$ajax.get(`${prefix}/user/market/desk/store/member/view`, { params })
    },

    getMemberList (params) {
        return vue.$ajax.get(`${prefix}/user/market/desk/store/member/list`, { params })
    },

    requestDeleteMember (params) {
        return vue.$ajax.delete(`${prefix}/user/market/desk/store/member/delete`, { params })
    },

    requestAddMember (params) {
        return vue.$ajax.post(`${prefix}/user/market/desk/store/member/add`, params)
    },

    requestChangeProject (params) {
        return vue.$ajax.put(`${prefix}/user/market/desk/store/member/test/project/change?projectCode=${params.projectCode}&storeCode=${params.storeCode}&storeType=${params.storeType}&storeMember=${params.storeMember}`)
    },

    requestStaticChartData (storeType, storeCode, params) {
        return vue.$ajax.get(`${prefix}/user/store/statistic/types/${storeType}/codes/${storeCode}/trend/data`, { params })
    },

    requestSensitiveApiList (storeType, storeCode, params) {
        return vue.$ajax.get(`${prefix}/user/sdk/${storeType}/${storeCode}/sensitiveApi/list`, { params })
    },

    requestUnApprovalApiList (storeType, storeCode, params) {
        return vue.$ajax.get(`${prefix}/user/sdk/${storeType}/${storeCode}/sensitiveApi/unApprovalApiList`, { params })
    },

    requestApplySensitiveApi (storeType, storeCode, postData) {
        return vue.$ajax.post(`${prefix}/user/sdk/${storeType}/${storeCode}/sensitiveApi/apply`, postData)
    },

    requestCancelSensitiveApi (storeType, storeCode, id) {
        return vue.$ajax.put(`${prefix}/user/sdk/${storeType}/${storeCode}/sensitiveApi/cancel/${id}`)
    },

    requestStatisticPipeline (code, params) {
        return vue.$ajax.get(`${processPerfix}/user/pipeline/atoms/${code}/rel/list`, { params })
    },

    requestSavePipelinesAsCsv (code, params) {
        const query = []
        for (const key in params) {
            const val = params[key]
            if (val) query.push(`${key}=${val}`)
        }
        return fetch(`${processPerfix}/user/pipeline/atoms/${code}/rel/csv/export?${query.join('&')}`, {
            method: 'POST',
            headers: {
                'content-type': 'application/json'
            }
        })
    },

    requestAtomQuality (code) {
        return vue.$ajax.get(`${qualityPerfix}/user/metadata/market/atom/${code}/indicator/list`)
    },

    requestAtomVersionDetail (code) {
        return vue.$ajax.get(`${prefix}/user/market/atoms/${code}/showVersionInfo`)
    },

    requestAtomOutputList (code) {
        return vue.$ajax.get(`${prefix}/user/market/atoms/${code}/output`)
    },

    getVersionLogs (storeType, code, params) {
        return vue.$ajax.get(`${prefix}/user/store/components/${storeType}/${code}/getVersionLogs`, { params })
    }
}
