const Vue = window.Vue
const vue = new Vue()
const prefix = 'turbo/api/user'

export function getOverViewStatData (projectId) {
    return vue.$ajax.get(`${prefix}/turboDaySummary/statisticsRowData/${projectId}`)
}

export function getCompileNumberTrend (dateType, projectId) {
    return vue.$ajax.get(`${prefix}/turboDaySummary/compileNumber/dateType/${dateType}/projectId/${projectId}`)
}

export function getTimeConsumingTrend (dateType, projectId) {
    return vue.$ajax.get(`${prefix}/turboDaySummary/timeConsumingTrend/dateType/${dateType}/projectId/${projectId}`)
}

export function getPlanDetailById (planId) {
    return vue.$ajax.get(`${prefix}/turboPlan/planId/${planId}`)
}

export function getEngineList (projectId) {
    return vue.$ajax.get(`${prefix}/turboEngineConfig/list/projectId/${projectId}`)
}

export function modifyTaskBasic (form) {
    return vue.$ajax.put(`${prefix}/turboPlan/name/planId/${form.planId}`, form)
}

export function modifyTaskWhiteList (form) {
    return vue.$ajax.put(`${prefix}/turboPlan/whiteList/planId/${form.planId}`, form)
}

export function getHistoryList (queryData, postData) {
    const queryStrArr = []
    for (const key in queryData) {
        const val = queryData[key]
        if (![null, undefined].includes(val)) queryStrArr.push(`${key}=${val}`)
    }
    return vue.$ajax.post(`${prefix}/turboRecord/list?${queryStrArr.join('&')}`, postData)
}

export function getPlanList (projectId, pageNum) {
    return vue.$ajax.get(`${prefix}/turboPlan/detail/projectId/${projectId}`, { params: { pageNum, pageSize: 40 } })
}

export function getPlanInstanceDetail (turboPlanId, params) {
    return vue.$ajax.get(`${prefix}/planInstance/detail/turboPlanId/${turboPlanId}`, { params })
}

export function getHistorySearchList (projectId) {
    return vue.$ajax.get(`${prefix}/turboRecord/detail/projectId/${projectId}`)
}

export function modifyConfigParam (form) {
    return vue.$ajax.put(`${prefix}/turboPlan/configParam/planId/${form.planId}`, form)
}

export function addTurboPlan (form) {
    return vue.$ajax.post(`${prefix}/turboPlan`, form)
}

export function getTurboRecord (turboRecordId) {
    return vue.$ajax.get(`${prefix}/turboRecord/id/${turboRecordId}`)
}

export function modifyTurboPlanTopStatus (planId, topStatus) {
    return vue.$ajax.put(`${prefix}/turboPlan/topStatus/planId/${planId}/topStatus/${topStatus}`)
}

export function getRecommendList () {
    return vue.$ajax.get(`${prefix}/turboEngineConfig/recommend/list`)
}

export function getEngineDetail (engineCode) {
    return vue.$ajax.get(`${prefix}/turboEngineConfig/engineCode/${engineCode}`)
}

export const http = vue.$ajax
