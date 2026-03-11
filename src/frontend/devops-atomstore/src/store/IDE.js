const prefix = 'store/api'
const Vue = window.Vue
const vue = new Vue()

export const actions = {
    requestIDEClassifys ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/ideAtom/classifys`)
    },

    requestIDECategorys ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/ideAtom/categorys`)
    },

    requestIDELabel ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/ideAtom/label/labels`)
    },

    requestIDEHome ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/market/ideAtom/list/main?page=1&pageSize=8`)
    },

    /**
     * 流水线插件市场流水线IDE插件列表
     */
    requestMarketIDE ({ commit }, params) {
        return vue.$ajax.get(`${prefix}/user/market/ideAtom/atom/list`, { params })
    },

    /**
     * 流水线IDE插件详情
     */
    requestIDE ({ commit }, { atomCode }) {
        return vue.$ajax.get(`${prefix}/user/market/ideAtom/atomCodes/${atomCode}`)
    },

    /**
     * 评论列表
     */
    requestIDEComments ({ commit }, { code, page, pageSize }) {
        return vue.$ajax.get(`${prefix}/user/market/ideAtom/comment/atomCodes/${code}/comments?page=${page}&pageSize=${pageSize}`)
    },

    /**
     * 评分详情
     */
    requestIDEScoreDetail ({ commit }, code) {
        return vue.$ajax.get(`${prefix}/user/market/ideAtom/comment/score/atomCodes/${code}`)
    },

    /**
     * 添加评论回复
     */
    requestIDEReplyComment ({ commit }, { id, postData }) {
        return vue.$ajax.post(`${prefix}/user/market/ideAtom/comment/reply/comments/${id}/reply`, postData)
    },

    /**
     * 评论点赞
     */
    requestIDEPraiseComment ({ commit }, commentId) {
        return vue.$ajax.put(`${prefix}/user/market/ideAtom/comment/praise/${commentId}`)
    },

    /**
     * 获取评论回复列表
     */
    requestIDEReplyList ({ commit }, commentId) {
        return vue.$ajax.get(`${prefix}/user/market/ideAtom/comment/reply/comments/${commentId}/replys`)
    },

    /**
     * 新增评论
     */
    requestAddIDEComment ({ commit }, { id, code, postData }) {
        return vue.$ajax.post(`${prefix}/user/market/ideAtom/comment/atomIds/${id}/atomCodes/${code}/comment`, postData)
    },

    /**
     * 根据ID修改评论
     */
    requestIDEModifyComment ({ commit }, data) {
        return vue.$ajax.put(`${prefix}/user/market/ideAtom/comment/comments/${data.id}`, data.postData)
    },

    /**
     * 根据ID获取评论
     */
    requestIDEUserComment ({ commit }, id) {
        return vue.$ajax.get(`${prefix}/user/market/ideAtom/comment/comments/${id}`)
    }
}

export const getters = {
}

export const state = {
}

export const mutations = {
}
