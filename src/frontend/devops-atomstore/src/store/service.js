import {
    UPDATE_CURRENT_SERVICE
} from '@/store/constants'
const prefix = 'store/api'
const ticket = 'ticket/api'
const service = 'service/api'
const project = 'project/api'
const Vue = window.Vue
const vue = new Vue()

export const actions = {
    /***
     * 获取扩展部署相关信息
     */
    requestDeployInfo ({ commit }, serviceCode) {
        return vue.$ajax.get(`${prefix}/user/ext/services/deployment/services/${serviceCode}/status`)
    },

    /***
     * 重新部署
     */
    requestRedeploy ({ commit }, { serviceCode, version }) {
        return vue.$ajax.get(`${prefix}/user/ext/services/deployment/services/${serviceCode}/versions/${version}/deploy`)
    },

    /***
     * 获取ReadMe文件信息
     */
    requestReadMe ({ commit }, serviceCode) {
        return vue.$ajax.get(`${prefix}/user/market/service/repositorys/serviceCodes/${serviceCode}/readme`)
    },

    /***
     * 卸载扩展服务
     */
    uninstallService ({ commit }, { serviceCode, projectCode }) {
        const reasonList = [{
            reasonId: '',
            note: ''
        }]
        return vue.$ajax.put(`${prefix}/user/market/service/project/${projectCode}/serviceCodes/${serviceCode}/uninstalled`, { reasonList })
    },

    /***
     * 获取已安装的扩展列表
     */
    requesInstalledServiceList ({ commit }, projectCode) {
        return vue.$ajax.get(`${prefix}/user/market/service/project/${projectCode}/installed/service`)
    },

    /***
     * 微扩展重新授权
     */
    resetServiceGit ({ commit }, { serviceCode, projectCode }) {
        return vue.$ajax.put(`${prefix}/user/market/service/repositorys/${serviceCode}?projectCode=${projectCode}`)
    },

    /***
     * 返回测试状态
     */
    requestBackToTest ({ commit }, serviceId) {
        return vue.$ajax.post(`${prefix}/user/market/serviceIds/${serviceId}/ext/back`)
    },

    /***
     * 获取微扩展打分相关信息
     */
    requestServiceStic ({ commit }, serviceCode) {
        return vue.$ajax.get(`${prefix}/user/market/service/stat/serviceCodes/${serviceCode}`)
    },

    /***
     * 获取版本信息列表
     */
    requestVersionLog ({ commit }, serviceCode) {
        return vue.$ajax.get(`${prefix}/user/market/service/version/list?serviceCode=${serviceCode}`)
    },

    /***
     *  提交相关信息
     */
    requestCommitServiceInfo ({ commit }, { serviceId, commitInfo }) {
        return vue.$ajax.post(`${prefix}/user/market/serviceIds/${serviceId}/ext/submitInfo?serviceId=${serviceId}`, commitInfo)
    },

    /***
     * 获取微扩展的标签
     */
    requestServiceLabel ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/service/label/labels`)
    },

    /***
     * 获取微扩展支持的语言
     */
    requestServiceLanguage ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/desk/service/desk/service/language`)
    },

    /***
     * 获取微扩展点列表
     */
    requestServiceItemList ({ commit }) {
        return vue.$ajax.get(`${project}/user/ext/items`)
    },

    /**
     * 更新扩展信息
     */
    requestUpdateServiceInfo ({ commit }, { serviceCode, data }) {
        return vue.$ajax.put(`${prefix}/user/market/baseInfo/serviceCodes/${serviceCode}/serviceIds/${data.serviceId}`, data)
    },
    /**
     * 删除扩展成员
     */
    requestDeleteServiceMem ({ commit }, { id, serviceCode }) {
        return vue.$ajax.delete(`${prefix}/user/market/service/members/delete?id=${id}&serviceCode=${serviceCode}`)
    },
    /**
     * 添加扩展成员
     */
    requestAddServiceMem ({ commit }, data) {
        return vue.$ajax.post(`${prefix}/user/market/service/members/add`, data)
    },
    /**
     * 获取扩展成员列表
     */
    requestServiceMemList ({ commit }, serviceCode) {
        return vue.$ajax.get(`${prefix}/user/market/service/members/list?serviceCode=${serviceCode}`)
    },
    /**
     * 查看扩展成员信息
     */
    requestGetServiceMemInfo ({ commit }, serviceCode) {
        return vue.$ajax.get(`${prefix}/user/market/service/members/view?serviceCode=${serviceCode}`)
    },
    /**
     * 获取扩展版本列表
     */
    requestServiceVersionList ({ commit }, { serviceCode, page, pageSize }) {
        return vue.$ajax.get(`${prefix}/user/market/service/version/list?page=${page}&pageSize=${pageSize}&serviceCode=${serviceCode}`)
    },
    /**
     * 添加可见范围
     */
    setServiceVisableDept ({ commit }, { params }) {
        return vue.$ajax.post(`${prefix}/user/market/service/visible/dept`, params)
    },
    /**
     * 删除可见范围
     */
    requestDeleteServiceVis ({ commit }, { serviceCode, deptIds }) {
        return vue.$ajax.delete(`${prefix}/user/market/service/visible/dept/${serviceCode}?deptIds=${deptIds}`)
    },
    /**
     * 获取可见范围列表
     */
    requestServiceVisableList ({ commit }, serviceCode) {
        return vue.$ajax.get(`${prefix}/user/market/service/visible/dept/${serviceCode}`)
    },
    /**
     * 安装扩展到项目
     */
    installService ({ commit }, params) {
        return vue.$ajax.post(`${prefix}/user/market/service/project/install`, params)
    },
    /**
     * 已安装扩展项目
     */
    requestRelativeServiceProject ({ commit }, serviceCode) {
        return vue.$ajax.get(`${prefix}/user/market/service/project/installedProjects/${serviceCode}`)
    },
    /**
     * 重新构建
     */
    requestRebuildService ({ commit }, { id, projectCode }) {
        return vue.$ajax.put(`${prefix}/user/market/desk/service/release/rebuild/${id}?projectCode=${projectCode}`)
    },

    /**
     * 确认扩展通过测试
     */
    requestServicePassTest ({ commit }, serviceId) {
        return vue.$ajax.put(`${prefix}/user/market/desk/service/release/passTest/${serviceId}`)
    },

    /**
     * 取消上架扩展
     */
    requestServiceCancelRelease ({ commit }, serviceId) {
        return vue.$ajax.put(`${prefix}/user/market/desk/service/release/cancel/${serviceId}`)
    },

    /**
     * 扩展进度
     */
    requestServiceProcess ({ commit }, serviceId) {
        return vue.$ajax.get(`${prefix}/user/market/desk/service/release/process/${serviceId}`)
    },

    /**
     * 上架/升级扩展
     */
    requestReleaseService ({ commit }, params) {
        return vue.$ajax.put(`${prefix}/user/market/desk/service`, params)
    },

    /**
     * 获取项目下的扩展tag列表
     */
    requestServiceTagList ({ commit }, { serviceRepo, serviceId }) {
        const serviceRepoName = window.encodeURIComponent(window.encodeURIComponent(serviceRepo))
        return vue.$ajax.get(`${prefix}/user/market/service/repo/bk/names/${serviceRepoName}?serviceId=${serviceId}`)
    },

    /**
     * 获取项目下的扩展列表
     */
    requestServiceList ({ commit }, projectCode) {
        return vue.$ajax.get(`${service}/user/service/${projectCode}/listAllProjectServices/?searchKey=&filters=all`)
    },

    /**
     * 获取扩展详情
     */
    requestServiceDetailByCode ({ dispatch }, serviceCode) {
        return vue.$ajax.get(`${prefix}/user/market/service/${serviceCode}`).then((res) => {
            if (res.descInputType === 'FILE') {
                return dispatch('requestReadMe', serviceCode).then((description) => {
                    res.description = description
                    return res
                })
            } else {
                return res
            }
        })
    },

    /**
     * 获取扩展详情
     */
    requestServiceDetail ({ dispatch }, serviceId) {
        return vue.$ajax.get(`${prefix}/user/market/desk/service/${serviceId}`).then((res) => {
            if (res.descInputType === 'FILE') {
                return dispatch('requestReadMe', res.serviceCode).then((description) => {
                    res.description = description
                    return res
                })
            } else {
                return res
            }
        })
    },

    /**
     * 下架扩展
     */
    requestOfflineService ({ commit }, { serviceCode, params }) {
        return vue.$ajax.put(`${prefix}/user/market/desk/service/${serviceCode}/offline`, params)
    },
    
    /**
     * 删除扩展
     */
    requestDelService ({ commit }, serviceCode) {
        return vue.$ajax.delete(`${prefix}/user/market/desk/service/${serviceCode}/delete`)
    },

    /**
     * 关联扩展
     */
    requestAddService ({ commit }, params) {
        return vue.$ajax.post(`${prefix}/user/market/desk/service`, params)
    },

    /**
     * 获取凭证列表
     */
    requestTicketList ({ commit }, { projectCode }) {
        return vue.$ajax.get(`${ticket}/user/credentials/${projectCode}?credentialTypes=USERNAME_PASSWORD&page=1&pageSize=1000`)
    },

    /**
     * 工作台获取所有扩展列表
     */
    requestDeskServiceList ({ commit }, { serviceName, page, pageSize }) {
        return vue.$ajax.get(`${prefix}/user/market/desk/service/list?serviceName=${serviceName}&page=${page}&pageSize=${pageSize}`)
    },

    requestServiceClassifys ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/service/classifys`)
    },

    requestServiceCategorys ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/service/categorys`)
    },

    requestServiceHome ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/service/list/main?page=1&pageSize=8`)
    },

    /**
     * 流水线插件市场流水线Service插件列表
     */
    requestMarketService ({ commit }, params) {
        return vue.$ajax.get(`${prefix}/user/market/service/list`, { params })
    },

    /**
     * 流水线Service插件详情
     */
    requestService ({ dispatch }, { serviceCode }) {
        return vue.$ajax.get(`${prefix}/user/market/service/${serviceCode}`).then((res) => {
            if (res.descInputType === 'FILE') {
                return dispatch('requestReadMe', serviceCode).then((description) => {
                    res.description = description
                    return res
                })
            } else {
                return res
            }
        })
    },

    /**
     * 评论列表
     */
    requestServiceComments ({ commit }, { code, page, pageSize }) {
        return vue.$ajax.get(`${prefix}/user/market/service/comment/serviceCodes/${code}/comments?page=${page}&pageSize=${pageSize}`)
    },

    /**
     * 评分详情
     */
    requestServiceScoreDetail ({ commit }, code) {
        return vue.$ajax.get(`${prefix}/user/market/service/comment/score/serviceCodes/${code}`)
    },

    /**
     * 添加评论回复
     */
    requestServiceReplyComment ({ commit }, { id, postData }) {
        return vue.$ajax.post(`${prefix}/user/market/service/comment/reply/comments/${id}/reply`, postData)
    },

    /**
     * 评论点赞
     */
    requestServicePraiseComment ({ commit }, commentId) {
        return vue.$ajax.put(`${prefix}/user/market/service/comment/praise/${commentId}`)
    },

    /**
     * 获取评论回复列表
     */
    requestServiceReplyList ({ commit }, commentId) {
        return vue.$ajax.get(`${prefix}/user/market/service/comment/reply/comments/${commentId}/replys`)
    },

    /**
     * 新增评论
     */
    requestAddServiceComment ({ commit }, { id, code, postData }) {
        return vue.$ajax.post(`${prefix}/user/market/service/comment/serviceIds/${id}/serviceCodes/${code}/comment`, postData)
    },

    /**
     * 根据ID修改评论
     */
    requestServiceModifyComment ({ commit }, data) {
        return vue.$ajax.put(`${prefix}/user/market/service/comment/comments/${data.id}`, data.postData)
    },

    /**
     * 根据ID获取评论
     */
    requestServiceUserComment ({ commit }, id) {
        return vue.$ajax.get(`${prefix}/user/market/service/comment/comments/${id}`)
    },

    updateCurrentService ({ commit }, res) {
        commit(UPDATE_CURRENT_SERVICE, res)
    },

    updateServiceMemInfo ({ commit }, res) {
        commit('updateServiceMemInfo', res)
    }
}

export const getters = {
    getCurrentService: state => state.currentService,
    getServiceMeminfo: state => state.serviceMemInfo
}

export const state = {
    currentService: {},
    serviceMemInfo: {}
}

export const mutations = {
    [UPDATE_CURRENT_SERVICE]: (state, res) => {
        Vue.set(state, 'currentService', res)
    },

    updateServiceMemInfo: (state, res) => {
        Vue.set(state, 'serviceMemInfo', res)
    }
}
