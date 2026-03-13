import { STORE_TYPE, BASE_PREFIX } from '@/utils/constants'
const prefix = 'store/api'
const projectPrefix = 'project/api'
const artifactoryPrefix = 'artifactory/api'
const repositoryPrefix = 'repository/api'
const Vue = window.Vue
const vue = new Vue()

export const state = {
    appDetail: {},
    isLoading: false
}

export const mutations = {
    SET_APP_DETAIL: (state, detail) => {
        Vue.set(state, 'appDetail', detail)
    },
    RESET_APP_DETAIL: (state) => {
        Vue.set(state, 'appDetail', {})
    }
}

export const getters = {
    getAppDetail: state => state.appDetail,
}

export const actions = {
    /**
     * 获取应用列表
     */
    getComponentsList ({ commit }, params) {
        const filteredParams = Object.keys(params).reduce((acc, key) => {
            if (params[key] !== '' && params[key] !== null && params[key] !== undefined) {
                acc[key] = params[key]
            }
            return acc
        }, {})
        return vue.$ajax.get(`${prefix}/user/store/components/desk/types/${STORE_TYPE}/component/list`, { params: filteredParams })
    },

    /**
     * 创建应用
     */
    createApp ({ commit }, params) {
        return vue.$ajax.post(`${prefix}/user/store/releases/component/create`, params)
    },

    /**
     * 获取应用详情
     */
    getComponentDetail ({ commit }, storeCode) {
        return vue.$ajax.get(`${prefix}/user/store/components/types/${STORE_TYPE}/codes/${storeCode}/component/detail`)
    },

    /**
     * 根据版本ID获取应用详情
     */
    getComponentDetailByVersion ({ commit }, { storeId }) {
        return vue.$ajax.get(`${prefix}/user/store/components/types/${STORE_TYPE}/ids/${storeId}/component/detail`)
    },

    /**
     * 更新应用版本
     */
    addVersion ({ commit }, params) {
        return vue.$ajax.put(`${prefix}/user/store/releases/component/update`, params)
    },

    /**
     * 获取分类列表
     */
    fetchClassifyList ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/store/classifies/types/${STORE_TYPE}/list`)
    },

    /**
     * 下架版本
     */
    takeDownVersion ({ commit }, params) {
        return vue.$ajax.put(`${prefix}/user/store/releases/component/offline`, params)
    },

    /**
     * 获取进度信息
     */
    fetchProgress ({ commit }, storeId) {
        return vue.$ajax.get(`${prefix}/user/store/releases/components/${storeId}/process/info`)
    },

    /**
     * 重新构建组件
     */
    rebuildComponent ({ commit }, storeId) {
        return vue.$ajax.put(`${prefix}/user/store/releases/components/${storeId}/rebuild`)
    },

    /**
     * 通过测试
     */
    passComponent ({ commit }, storeId) {
        return vue.$ajax.put(`${prefix}/user/store/releases/components/${storeId}/test/pass`)
    },

    /**
     * 取消发布
     */
    cancelClientRelease ({ commit }, storeId) {
        return vue.$ajax.put(`${prefix}/user/store/releases/components/${storeId}/cancel`)
    },

    /**
     * 更新基本信息
     */
    updateBasicInfo ({ commit }, { storeCode, params }) {
        return vue.$ajax.put(`${prefix}/user/store/components/types/${STORE_TYPE}/codes/${storeCode}/component/base/info/update`, params)
    },

    /**
     * 获取日志
     */
    fetchLog ({ commit }, { projectCode, pipelineId, buildId }) {
        return vue.$ajax.get(`${prefix}/user/store/logs/types/${STORE_TYPE}/projects/${projectCode}/pipelines/${pipelineId}/builds/${buildId}`)
    },

    /**
     * 获取日志(after)
     */
    fetchLogAfter ({ commit }, { projectCode, pipelineId, buildId, ...query }) {
        const queryStr = new URLSearchParams(query).toString()
        return vue.$ajax.get(`${prefix}/user/store/logs/types/${STORE_TYPE}/projects/${projectCode}/pipelines/${pipelineId}/builds/${buildId}/after?${queryStr}`)
    },

    /**
     * 获取更多日志
     */
    fetchMoreLog ({ commit }, { projectCode, pipelineId, buildId, ...query }) {
        const queryStr = new URLSearchParams(query).toString()
        return vue.$ajax.get(`${prefix}/user/store/logs/types/${STORE_TYPE}/projects/${projectCode}/pipelines/${pipelineId}/builds/${buildId}/more?${queryStr}`)
    },

    /**
     * 下载应用
     */
    downloadApp ({ commit }, { storeCode, version }) {
        return vue.$ajax.get(`${artifactoryPrefix}/user/artifactories/store/component/types/${STORE_TYPE}/codes/${storeCode}/versions/${version}/pkg/download/url/get`)
    },

    /**
     * 上传Markdown图片
     */
    uploadMdImg ({ commit }, formData) {
        return vue.$ajax.post(`${artifactoryPrefix}/user/bkrepo/statics/file/upload?type=${STORE_TYPE}`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        })
    },

    /**
     * 删除应用
     */
    deleteApp ({ commit }, storeCode) {
        return vue.$ajax.delete(`${prefix}/user/store/components/desk/types/${STORE_TYPE}/codes/${storeCode}/component/delete`)
    },

    /**
     * 获取版本信息
     */
    showVersionInfo ({ commit }, storeCode) {
        return vue.$ajax.get(`${prefix}/user/store/components/types/${STORE_TYPE}/codes/${storeCode}/component/showVersionInfo`)
    },

    /**
     * 获取发布者列表
     */
    getPublisherList ({ commit }, storeCode) {
        return vue.$ajax.get(`${prefix}/user/market/publishers/get?storeCode=${storeCode}&storeType=${STORE_TYPE}`)
    },

    /**
     * 获取Git仓库列表
     */
    getRepositoryList ({ commit }, search) {
        const query = new URLSearchParams({
            projectId: '',
            ...(search && { search })
        }).toString()
        return vue.$ajax.get(`${repositoryPrefix}/user/git/getProject?${query}`)
    },

    /**
     * 获取用户信息
     */
    getUserInfo ({ commit }, storeCode) {
        return vue.$ajax.get(`${prefix}/user/market/desk/store/member/view?storeCode=${storeCode}&storeType=${STORE_TYPE}`)
    },

    /**
     * 更新授权者
     */
    authorizerUpdate ({ commit }, storeCode) {
        return vue.$ajax.put(`${prefix}/user/store/components/types/${STORE_TYPE}/codes/${storeCode}/component/repository/authorizer/update`)
    },

    /**
     * 获取测试信息
     */
    testInfoGet ({ commit }, storeCode) {
        return vue.$ajax.get(`${prefix}/user/store/components/types/${STORE_TYPE}/codes/${storeCode}/component/test/info/get`)
    },

    /**
     * 保存测试信息
     */
    testInfoSave ({ commit }, { storeCode, params }) {
        return vue.$ajax.put(`${prefix}/user/store/components/types/${STORE_TYPE}/codes/${storeCode}/component/test/info/save`, params)
    },

    /**
     * 编辑进度信息
     */
    progressInfoEdit ({ commit }, { storeId, params }) {
        return vue.$ajax.put(`${prefix}/user/store/releases/components/${storeId}/release/info/edit`, params)
    },

    /**
     * 获取媒体信息
     */
    mediaInfoGet ({ commit }, storeCode) {
        return vue.$ajax.get(`${prefix}/user/store/components/types/${STORE_TYPE}/codes/${storeCode}/component/media/info/get`)
    },

    /**
     * 上传文件
     */
    uploadFiles ({ commit }, formData) {
        return vue.$ajax.post(`${BASE_PREFIX}/misc/api/user/file/upload`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        })
    },

    /**
     * 步骤回退
     */
    stepBack ({ commit }, storeId) {
        return vue.$ajax.put(`${prefix}/user/store/releases/components/${storeId}/step/back`)
    },

    /**
     * 获取可见范围
     */
    visibilitiesGet ({ commit }, { storeCode, deptStatusInfos }) {
        return vue.$ajax.get(`${prefix}/user/store/visibilities/types/${STORE_TYPE}/codes/${storeCode}/get?deptStatusInfos=${deptStatusInfos}`)
    },

    /**
     * 获取可见范围列表（不带状态过滤）
     */
    getVisibilitiesList ({ commit }, storeCode) {
        return vue.$ajax.get(`${prefix}/user/store/visibilities/types/${STORE_TYPE}/codes/${storeCode}/get`)
    },

    /**
     * 添加可见范围
     */
    addVisibilitiesList ({ commit }, params) {
        return vue.$ajax.post(`${prefix}/user/store/visibilities/add`, params)
    },

    /**
     * 删除可见范围
     */
    deleteVisibilitiesList ({ commit }, { storeCode, deptIds }) {
        return vue.$ajax.delete(`${prefix}/user/store/visibilities/types/${STORE_TYPE}/codes/${storeCode}/delete?deptIds=${deptIds}`)
    },

    /**
     * 获取发布版本列表
     */
    getRelaseVersionList ({ commit }, { storeCode, ...params }) {
        const filteredParams = Object.keys(params).reduce((acc, key) => {
            if (params[key] !== '' && params[key] !== null && params[key] !== undefined) {
                acc[key] = params[key]
            }
            return acc
        }, {})
        return vue.$ajax.get(`${prefix}/user/store/components/desk/types/${STORE_TYPE}/codes/${storeCode}/component/version/list`, { params: filteredParams })
    },

    /**
     * 搜索云桌面
     */
    workspacesSearch ({ commit }) {
        return vue.$ajax.post('/remotedev/api/user/workspaces/search?page=1&pageSize=6666', {})
    },
}

