import {
    UPDATE_CURRENT_IMAGE
} from '@/store/constants'
const prefix = 'store/api'
const ticket = 'ticket/api'
const image = 'image/api'
const Vue = window.Vue
const vue = new Vue()

export const actions = {
    /**
     * 更新镜像信息
     */
    requestUpdateImageInfo ({ commit }, { imageCode, data }) {
        return vue.$ajax.put(`${prefix}/user/market/baseInfo/images/${imageCode}`, data)
    },
    /**
     * 删除镜像成员
     */
    requestDeleteImageMem ({ commit }, { id, imageCode }) {
        return vue.$ajax.delete(`${prefix}/user/market/desk/image/member/delete?id=${id}&imageCode=${imageCode}`)
    },
    /**
     * 添加镜像成员
     */
    requestAddImageMem ({ commit }, data) {
        return vue.$ajax.post(`${prefix}/user/market/desk/image/member/add`, data)
    },
    /**
     * 获取镜像成员列表
     */
    requestImageMemList ({ commit }, imageCode) {
        return vue.$ajax.get(`${prefix}/user/market/desk/image/member/list?imageCode=${imageCode}`)
    },
    /**
     * 查看镜像成员信息
     */
    requestGetMemInfo ({ commit }, imageCode) {
        return vue.$ajax.get(`${prefix}/user/market/desk/image/member/view?imageCode=${imageCode}`)
    },
    /**
     * 获取镜像版本列表
     */
    requestImageVersionList ({ commit }, { imageCode, page, pageSize }) {
        return vue.$ajax.get(`${prefix}/user/market/image/imageCodes/${imageCode}/version/list?page=${page}&pageSize=${pageSize}`)
    },
    /**
     * 添加可见范围
     */
    setImageVisableDept ({ commit }, { params }) {
        return vue.$ajax.post(`${prefix}/user/market/desk/image/visible/dept`, params)
    },
    /**
     * 删除可见范围
     */
    requestDeleteImageVis ({ commit }, { imageCode, deptIds }) {
        return vue.$ajax.delete(`${prefix}/user/market/desk/image/visible/dept/${imageCode}?deptIds=${deptIds}`)
    },
    /**
     * 获取可见范围列表
     */
    requestImageVisableList ({ commit }, imageCode) {
        return vue.$ajax.get(`${prefix}/user/market/desk/image/visible/dept/${imageCode}`)
    },
    /**
     * 安装镜像到项目
     */
    installImage ({ commit }, params) {
        return vue.$ajax.post(`${prefix}/user/market/image/install`, params)
    },
    /**
     * 已安装镜像项目
     */
    requestRelativeImageProject ({ commit }, imageCode) {
        return vue.$ajax.get(`${prefix}/user/market/image/installedProjects/${imageCode}`)
    },
    /**
     * 重新验证镜像
     */
    requestRecheckImage ({ commit }, imageId) {
        return vue.$ajax.put(`${prefix}/user/market/desk/image/release/recheck/imageIds/${imageId}`)
    },

    /**
     * 确认镜像通过测试
     */
    requestImagePassTest ({ commit }, imageId) {
        return vue.$ajax.put(`${prefix}/user/market/desk/image/release/passTest/imageIds/${imageId}`)
    },

    /**
     * 取消上架镜像
     */
    requestCancelRelease ({ commit }, imageId) {
        return vue.$ajax.put(`${prefix}/user/market/desk/image/release/cancel/imageIds/${imageId}`)
    },

    /**
     * 镜像进度
     */
    requestImageProcess ({ commit }, imageId) {
        return vue.$ajax.get(`${prefix}/user/market/desk/image/release/process/imageIds/${imageId}`)
    },

    /**
     * 上架/升级镜像
     */
    requestReleaseImage ({ commit }, params) {
        return vue.$ajax.put(`${prefix}/user/market/desk/image/release`, params)
    },

    /**
     * 获取项目下的镜像tag列表
     */
    requestImageTagList ({ commit }, { imageRepo, imageId }) {
        const imageRepoName = window.encodeURIComponent(window.encodeURIComponent(imageRepo))
        return vue.$ajax.get(`${prefix}/user/market/image/repo/bk/names/${imageRepoName}?imageId=${imageId}`)
    },

    /**
     * 获取项目下的镜像列表
     */
    requestImageList ({ commit }, projectCode) {
        return vue.$ajax.get(`${image}/user/image/${projectCode}/listAllProjectImages/?searchKey=&filters=all`)
    },

    /**
     * 获取镜像详情
     */
    requestImageDetailByCode ({ commit }, imageCode) {
        return vue.$ajax.get(`${prefix}/user/market/image/imageCodes/${imageCode}`)
    },

    /**
     * 获取镜像详情
     */
    requestImageDetail ({ commit }, imageId) {
        return vue.$ajax.get(`${prefix}/user/market/image/imageIds/${imageId}`)
    },

    /**
     * 下架镜像
     */
    requestOfflineImage ({ commit }, { imageCode, params }) {
        return vue.$ajax.put(`${prefix}/user/market/desk/image/offline/imageCodes/${imageCode}/versions`, params)
    },
    
    /**
     * 删除镜像
     */
    requestDelImage ({ commit }, imageCode) {
        return vue.$ajax.delete(`${prefix}/user/market/image/imageCodes/${imageCode}`)
    },
    /**
     * 关联镜像
     */
    requestRelImage ({ commit }, { imageCode, params }) {
        return vue.$ajax.post(`${prefix}/user/market/image/imageCodes/${imageCode}/store/rel`, params)
    },

    /**
     * 获取凭证列表
     */
    requestTicketList ({ commit }, { projectCode }) {
        return vue.$ajax.get(`${ticket}/user/credentials/${projectCode}?credentialTypes=USERNAME_PASSWORD&page=1&pageSize=1000`)
    },

    /**
     * 工作台获取所有镜像列表
     */
    requestDeskImageList ({ commit }, { imageName, page, pageSize }) {
        return vue.$ajax.get(`${prefix}/user/market/desk/image/list?imageName=${imageName}&page=${page}&pageSize=${pageSize}`)
    },

    requestImageClassifys ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/image/classifys`)
    },

    requestImageCategorys ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/image/categorys`)
    },

    requestImageLabel ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/image/label/labels`)
    },

    requestImageHome ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/image/list/main?page=1&pageSize=8`)
    },

    /**
     * 流水线插件市场流水线Image插件列表
     */
    requestMarketImage ({ commit }, params) {
        return vue.$ajax.get(`${prefix}/user/market/image/list`, { params })
    },

    /**
     * 流水线Image插件详情
     */
    requestImage ({ commit }, { imageCode }) {
        return vue.$ajax.get(`${prefix}/user/market/image/imageCodes/${imageCode}`)
    },

    /**
     * 评论列表
     */
    requestImageComments ({ commit }, { code, page, pageSize }) {
        return vue.$ajax.get(`${prefix}/user/market/image/comment/imageCodes/${code}/comments?page=${page}&pageSize=${pageSize}`)
    },

    /**
     * 评分详情
     */
    requestImageScoreDetail ({ commit }, code) {
        return vue.$ajax.get(`${prefix}/user/market/image/comment/score/imageCodes/${code}`)
    },

    /**
     * 添加评论回复
     */
    requestImageReplyComment ({ commit }, { id, postData }) {
        return vue.$ajax.post(`${prefix}/user/market/image/comment/reply/comments/${id}/reply`, postData)
    },

    /**
     * 评论点赞
     */
    requestImagePraiseComment ({ commit }, commentId) {
        return vue.$ajax.put(`${prefix}/user/market/image/comment/praise/${commentId}`)
    },

    /**
     * 获取评论回复列表
     */
    requestImageReplyList ({ commit }, commentId) {
        return vue.$ajax.get(`${prefix}/user/market/image/comment/reply/comments/${commentId}/replys`)
    },

    /**
     * 新增评论
     */
    requestAddImageComment ({ commit }, { id, code, postData }) {
        return vue.$ajax.post(`${prefix}/user/market/image/comment/imageIds/${id}/imageCodes/${code}/comment`, postData)
    },

    /**
     * 根据ID修改评论
     */
    requestImageModifyComment ({ commit }, data) {
        return vue.$ajax.put(`${prefix}/user/market/image/comment/comments/${data.id}`, data.postData)
    },

    /**
     * 根据ID获取评论
     */
    requestImageUserComment ({ commit }, id) {
        return vue.$ajax.get(`${prefix}/user/market/image/comment/comments/${id}`)
    },

    updateCurrentImage ({ commit }, res) {
        commit(UPDATE_CURRENT_IMAGE, res)
    },

    updateImageMemInfo ({ commit }, res) {
        commit('updateImageMemInfo', res)
    },

    // 获取适用机器
    fetchAgentTypes ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/image/agentType/list`)
    },
    getImageVersionInfo ({ commit }, imageCode) {
        return vue.$ajax.get(`${prefix}/user/market/images/${imageCode}/showVersionInfo`)
    }
}

export const getters = {
    getCurrentImage: state => state.currentImage,
    getImageMeminfo: state => state.imageMemInfo
}

export const state = {
    currentImage: {},
    imageMemInfo: {}
}

export const mutations = {
    [UPDATE_CURRENT_IMAGE]: (state, res) => {
        Vue.set(state, 'currentImage', res)
    },

    updateImageMemInfo: (state, res) => {
        Vue.set(state, 'imageMemInfo', res)
    }
}
