import Vue from 'vue'
const vue = new Vue()

const IMAGE_PREFIX = '/image/api/user/image'
export const state = {
    // 公共镜像
    imageLibrary: {
        // 存储列表数据
        dataList: []
    },
    // 项目镜像
    projectImage: {
        // 存储列表数据
        dataList: []
    },
    // 我的收藏
    myCollect: {
        // 存储列表数据
        dataList: []
    }
    // ,
    // // 单个镜像操作数据
    // singleImage: {
    //     collect: {},
    //     update: {}
    // }
}

export const mutations = {
    /**
     * 更新 store 中的 imageLibrary.dataList
     *
     * @param {Object} state store state
     * @param {list} list 项目列表
     */
    forceUpdateImageLibraryList (state, list) {
        state.imageLibrary.dataList.splice(0, state.imageLibrary.dataList.length, ...list)
    },

    /**
     * 更新 store 中的 projectImage.dataList
     *
     * @param {Object} state store state
     * @param {list} list 项目列表
     */
    forceUpdateProjectImageList (state, list) {
        state.projectImage.dataList.splice(0, state.projectImage.dataList.length, ...list)
    },

    /**
     * 更新 store 中的 myCollect.dataList
     *
     * @param {Object} state store state
     * @param {list} list 项目列表
     */
    forceUpdateMyCollectList (state, list) {
        state.myCollect.dataList.splice(0, state.myCollect.dataList.length, ...list)
    }
}

export const actions = {

    /**
     * 获取公共镜像
     * /backend/api/depot/public/?search_key=jdk_onion
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     *
     * @return {Promise} promise 对象
     */
    getImageLibrary ({ commit, state, dispatch }, params) {
        return vue.$ajax.get(`${IMAGE_PREFIX}/listPublicImages`, {
            params
        }).then(response => {
            const res = response
            commit('forceUpdateImageLibraryList', res.imageList || [])
            return res
        })
    },

    /**
     * 获取项目镜像
     * /backend/api/depot/project/000?search_key=jdk_onion
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     *
     * @return {Promise} promise 对象
     */
    getProjectImage ({ commit, state, dispatch }, query) {
        const { projectCode, ...restQuery } = query
        return vue.$ajax.get(`${IMAGE_PREFIX}/${projectCode}/listImages/`, {
            params: restQuery
        }).then(response => {
            const res = response
            commit('forceUpdateProjectImageList', res.imageList || [])
            return res
        })
    },

    /**
     * 获取我的镜像详情
     * /backend/api/depot/collect/?search_key=jdk_onion
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     *
     * @return {Promise} promise 对象
     */
    getImageLibraryDetail ({ commit, state, dispatch }, params) {
        return vue.$ajax.get(`${IMAGE_PREFIX}/getImageInfo`, {
            params
        }).then(response => {
            const res = response
            return res
        })
    },

    syncUploadStatus ({ commit, state, dispatch }, { projectCode, ...params }) {
        return vue.$ajax.get(`${IMAGE_PREFIX}/${projectCode}/queryUploadTask`, {
            params
        }).then(response => {
            return response
        })
    },
    setBuildImage ({ commit, state, dispatch }, { projectCode, imageRepo, imageTag }) {
        return vue.$ajax.post(`${IMAGE_PREFIX}/${projectCode}/setBuildImage?imageRepo=${imageRepo}&imageTag=${imageTag}`).then(response => {
            return response
        })
    }
}
