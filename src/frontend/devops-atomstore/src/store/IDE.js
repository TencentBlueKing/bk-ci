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
}

export const getters = {
}

export const state = {
}

export const mutations = {
}
