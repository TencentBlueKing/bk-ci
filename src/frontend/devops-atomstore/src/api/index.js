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

    getAllEnv ({ storeType, storeCode, scope, varName }) {
        return vue.$ajax.get(`${prefix}/user/store/env/var/latest/types/${storeType}/codes/${storeCode}?scope=${scope}&varName=${varName}`)
    },

    getEnvChangeList ({ storeType, storeCode, varName, scope }) {
        return vue.$ajax.get(`${prefix}/user/store/env/var/change/log/types/${storeType}/codes/${storeCode}/vars/${varName}?scope=${scope}`)
    },

    addEnv (params) {
        const { variableId, option } = params
        delete params.option
        if (option === 'update') {
            delete params.variableId
            return vue.$ajax.post(`${prefix}/user/store/env/var/${option}?variableId=${variableId}`, params)
        }
        return vue.$ajax.post(`${prefix}/user/store/env/var/${option}`, params)
    },

    deleteEnv ({ storeType, storeCode, varNames, scope }) {
        return vue.$ajax.delete(`${prefix}/user/store/env/var/types/${storeType}/codes/${storeCode}?varNames=${varNames}&scope=${scope}`)
    },

    getCodeScore (storeType, storeCode, params) {
        return vue.$ajax.get(`${prefix}/user/store/codecc/types/${storeType}/codes/${storeCode}/measurement`, { params })
    },

    startCodecc (storeType, storeCode, storeId) {
        const queryStr = storeId ? `?storeId=${storeId}` : ''
        return vue.$ajax.post(`${prefix}/user/store/codecc/types/${storeType}/codes/${storeCode}/task/start${queryStr}`)
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

    requestAtomOutputList (code) {
        return vue.$ajax.get(`${prefix}/user/market/atoms/${code}/output`)
    },
    requestAtomVersionDetail (code) {
        return vue.$ajax.get(`${prefix}/user/market/atoms/${code}/showVersionInfo`)
    },
    requestAtomErrorCode (storeType, storeCode) {
        return vue.$ajax.get(`${prefix}/user/store/statistic/types/${storeType}/codes/${storeCode}/errorCode`)
    }
}
