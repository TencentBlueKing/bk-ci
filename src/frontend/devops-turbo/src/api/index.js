const Vue = window.Vue
const vue = new Vue()
const prefix = 'turbo-new/api/user'

module.exports = {
    getOverViewStatData (projectId) {
        return vue.$ajax.get(`${prefix}/turboDaySummary/statisticsRowData/${projectId}`)
    },

    getCompileNumberTrend (dateType, projectId) {
        return vue.$ajax.get(`${prefix}/turboDaySummary/compileNumber/dateType/${dateType}/projectId/${projectId}`)
    },

    getTimeConsumingTrend (dateType, projectId) {
        return vue.$ajax.get(`${prefix}/turboDaySummary/timeConsumingTrend/dateType/${dateType}/projectId/${projectId}`)
    },

    getPlanDetailById (planId) {
        return vue.$ajax.get(`${prefix}/turboPlan/planId/${planId}`)
    },

    getEngineList (projectId) {
        return vue.$ajax.get(`${prefix}/turboEngineConfig/list/projectId/${projectId}`)
    },

    modifyTaskBasic (form) {
        return vue.$ajax.put(`${prefix}/turboPlan/name/planId/${form.planId}`, form)
    },

    modifyTaskWhiteList (form) {
        return vue.$ajax.put(`${prefix}/turboPlan/whiteList/planId/${form.planId}`, form)
    },

    getHistoryList (queryData, postData) {
        const queryStrArr = []
        for (const key in queryData) {
            const val = queryData[key]
            if (![null, undefined].includes(val)) queryStrArr.push(`${key}=${val}`)
        }
        return vue.$ajax.post(`${prefix}/turboRecord/list?${queryStrArr.join('&')}`, postData)
    },

    getPlanList (projectId, pageNum) {
        return vue.$ajax.get(`${prefix}/turboPlan/detail/projectId/${projectId}`, { params: { pageNum, pageSize: 40 } })
    },

    getPlanInstanceDetail (turboPlanId, params) {
        return vue.$ajax.get(`${prefix}/planInstance/detail/turboPlanId/${turboPlanId}`, { params })
    },

    getHistorySearchList (projectId) {
        return vue.$ajax.get(`${prefix}/turboRecord/detail/projectId/${projectId}`)
    },

    modifyConfigParam (form) {
        return vue.$ajax.put(`${prefix}/turboPlan/configParam/planId/${form.planId}`, form)
    },

    addTurboPlan (form) {
        return vue.$ajax.post(`${prefix}/turboPlan`, form)
    },

    getTurboRecord (turboRecordId) {
        return vue.$ajax.get(`${prefix}/turboRecord/id/${turboRecordId}`)
    },

    modifyTurboPlanTopStatus (planId, topStatus) {
        return vue.$ajax.put(`${prefix}/turboPlan/topStatus/planId/${planId}/topStatus/${topStatus}`)
    },

    getRecommendList () {
        return vue.$ajax.get(`${prefix}/turboEngineConfig/recommend/list`)
    },

    getEngineDetail (engineCode) {
        return vue.$ajax.get(`${prefix}/turboEngineConfig/engineCode/${engineCode}`)
    }
}
