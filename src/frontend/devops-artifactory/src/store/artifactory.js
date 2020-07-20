import Vue from 'vue'
const vue = new Vue()

const prefix = 'artifactory/api'

export const state = {
    projectList: [],
    curNodeOnTree: { // 树组件当前选中的节点的信息
        deepCount: 0, // 当前节点的深度
        index: -1, // 当前节点所在列表的索引
        item: null, // 当前节点的内容
        roadMap: '1', // 从root到当前节点的索引路径
        type: 'customDir'
    },
    curNodeOnDialogTree: {
        // deepCount: 0,
        roadMap: '0',
        fullPath: '/'
    },
    sideMenuList: [
        {
            list: [
                {
                    id: 'artifactory',
                    name: '所有构件',
                    icon: 'icon-myrepo',
                    showChildren: false
                },
                {
                    id: 'artifactoryList',
                    name: '自定义仓库',
                    icon: 'icon-artifactory',
                    isOpen: false,
                    isSelected: false,
                    childrenType: 'bk-tree',
                    fullPath: '/',
                    folder: true,
                    children: [],
                    params: {
                        type: 'customDir'
                    }
                },
                {
                    id: 'artifactoryList',
                    name: '流水线仓库',
                    icon: 'icon-pipeline',
                    isOpen: false,
                    isSelected: false,
                    childrenType: 'bk-tree',
                    fullPath: '/',
                    folder: true,
                    children: [],
                    params: {
                        type: 'pipelines'
                    }
                },
                {
                    id: 'depotMain',
                    name: '镜像仓库',
                    icon: 'icon-mirror-depot',
                    isSelected: false,
                    isOpen: false,
                    showChildren: true,
                    children: [
                        {
                            id: 'imageLibrary',
                            name: '公共镜像'
                        },
                        {
                            id: 'projectImage',
                            name: '项目镜像'
                        }
                    ]
                }
            ]
        }
        // {
        //     list: [
        //         {
        //             id: 'artifactoryRecent',
        //             name: '最近使用',
        //             icon: 'icon-file',
        //             isSelected: false
        //         },
        //         {
        //             id: 'artifactoryRecycle',
        //             name: '回收站',
        //             icon: 'icon-delete',
        //             isSelected: false
        //         }
        //     ]
        // }
    ]
}

export const getters = {
    getCurNodeOnTree: state => state.curNodeOnTree,
    getCurDialogTree: state => state.curNodeOnDialogTree,
    getSideMenuList: state => state.sideMenuList,
    getDialogTreeList: state => state.dialogTreeList
}

export const mutations = {
    /**
     * 更新树组件的状态
     * @param {Object} obj 包含树组件状态的对象
     */
    updateCurNodeOnTree (state, obj) {
        const status = state.curNodeOnTree

        for (const key in obj) {
            status[key] = obj[key]
        }
    },
    /**
     * 更新 store 中的 projectList
     *
     * @param {Object} state store state
     * @param {list} list 项目列表
     */
    forceUpdateProjectList (state, list) {
        state.projectList.splice(0, state.projectList.length, ...list)
    },
    /**
     * 更新菜单参数
     */
    updateRootSideMenuParams (state, { index, params }) {
        state.sideMenuList[0].list[index].params = params
    },
    updateRootSideMenuList (state, { index, children }) {
        state.sideMenuList[0].list[index].children = children
    },
    updateRootSideMenu (state, { index, key, value }) {
        state.sideMenuList[0].list[index][key] = value
    },
    /**
     * 更新dialog树组件的状态
     * @param {Object} obj 包含树组件状态的对象
     */
    updateDialogTree (state, obj) {
        const status = state.curNodeOnDialogTree

        for (const key in obj) {
            status[key] = obj[key]
        }
    }
}

export const actions = {
    /**
     * 获取文件的信息
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {Number} projectId 项目 id
     * @param {String} path 路径
     *
     * @return {Promise} promise 对象
     */
    requestPathInfo ({ commit, state, dispatch }, { projectCode, type, path }) {
        return vue.$ajax.get(`${prefix}/user/artifactories/${projectCode}/${type}/list?path=${path}`).then(response => {
            return response
        })
    },
    requestFileInfo ({ commit }, { projectCode, path, type }) {
        return vue.$ajax.get(`${prefix}/user/artifactories/${projectCode}/${type}/show?path=${path}`).then(response => {
            return response
        })
    },
    requestFilePipelineInfo ({ commit }, { projectCode, path, type }) {
        return vue.$ajax.get(`${prefix}/user/artifactories/${projectCode}/${type}/filePipelineInfo?path=${path}`).then(response => {
            return response
        })
    },
    /**
     * 获取文件下载地址
     *
     * @param {Function} commit store commit mutation handler
     * @param {Number} projectId 项目 id
     * @param {String} path 路径
     *
     * @return {Promise} promise 对象
     */
    requestDownloadUrl ({ commit }, { projectId, path, type }) {
        return vue.$ajax.post(`${prefix}/user/artifactories/${projectId}/${type}/downloadUrl?path=${encodeURIComponent(path)}`).then(response => {
            return response
        })
    },
    /**
     * 文件共享
     *
     * @param {Function} commit store commit mutation handler
     * @param {Number} projectId 项目 id
     * @param {String} path 路径
     * @param {Number} ttl 有效时间
     * @param {String} downloadUsers 下载用户
     *
     * @return {Promise} promise 对象
     */
    requestShareUrl ({ commit }, { projectId, path, type, ttl, downloadUsers }) {
        return vue.$ajax.post(`${prefix}/user/artifactories/${projectId}/${type}/shareUrl?path=${path}&ttl=${ttl}&downloadUsers=${downloadUsers}`).then(response => {
            return response
        })
    },
    /**
     * 新建文件夹
     *
     * @param {Function} commit store commit mutation handler
     * @param {Number} projectId 项目 id
     * @param {String} path 路径
     *
     * @return {Promise} promise 对象
     */
    requestMakeDir ({ commit }, { projectId, path }) {
        return vue.$ajax.post(`${prefix}/user/customDir/${projectId}/dir?path=${path}`).then(response => {
            return response
        })
    },
    /**
     * 重命名
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {Number} projectId 项目 id
     * @param {String} srcPath 源路径
     * @param {String} destPath 目标路径
     *
     * @return {Promise} promise 对象
     */
    requestRename ({ commit }, { projectId, srcPath, destPath }) {
        return vue.$ajax.post(`${prefix}/user/customDir/${projectId}/rename`, {
            srcPath,
            destPath
        }).then(response => {
            return response
        })
    },
    /**
     * 复制
     *
     * @param {Function} commit store commit mutation handler
     * @param {Number} projectId 项目 id
     * @param {Array} srcPath 源路径
     * @param {String} destPath 目标路径
     *
     * @return {Promise} promise 对象
     */
    requestCopy ({ commit }, { projectId, srcPaths, destPath }) {
        return vue.$ajax.post(`${prefix}/user/customDir/${projectId}/copy`, {
            srcPaths,
            destPath
        }).then(response => {
            return response
        })
    },
    /**
     * 移动
     *
     * @param {Function} commit store commit mutation handler
     * @param {Number} projectId 项目 id
     * @param {Array} srcPath 源路径
     * @param {String} destPath 目标路径
     *
     * @return {Promise} promise 对象
     */
    requestMove ({ commit }, { projectId, srcPaths, destPath }) {
        return vue.$ajax.post(`${prefix}/user/customDir/${projectId}/move`, {
            srcPaths,
            destPath
        }).then(response => {
            return response
        })
    },
    /**
     * 删除
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {Number} projectId 项目 id
     * @param {Array} paths 源路径
     *
     * @return {Promise} promise 对象
     */
    requestDelete ({ commit }, { projectId, paths }) {
        return vue.$ajax.delete(`${prefix}/user/customDir/${projectId}`, {
            data: { paths }
        }).then(response => {
            return response
        })
    },
    /**
     * 流水线文件列表
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {Number} projectId 项目 id
     * @param {Number} page 页码
     * @param {Number} pageSize 页码条数
     *
     * @return {Promise} promise 对象
     */
    requestOwnFileList ({ commit }, { projectId, page, pageSize }) {
        return vue.$ajax.get(`${prefix}/user/artifactories/${projectId}/ownFileList?page=${page}&pageSize=${pageSize}`).then(response => {
            return response
        })
    },
    /**
     * 获取文件夹大小
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     * @param {Number} projectId 项目 id
     * @param {Array} type 仓库类型
     * @param {Array} path path
     *
     * @return {Promise} promise 对象
     */
    requestFolderSize ({ commit }, { projectId, type, path }) {
        return vue.$ajax.get(`${prefix}/user/artifactories/${projectId}/${type}/folderSize?path=${path}`).then(response => {
            return response
        })
    },
    /**
     * 搜索
     *
     * @param {Function} commit store commit mutation handler
     * @param {Number} projectId 项目 id
     * @param {Number} page 页码
     * @param {Number} pageSize 页码条数
     * @param {Object} params 搜索参数
     *
     * @return {Promise} promise 对象
     */
    requestSearchList ({ commit }, { projectId, page, pageSize, params }) {
        return vue.$ajax.post(`${prefix}/user/artifactories/${projectId}/search?page=${page}&pageSize=${pageSize}`, params).then(response => {
            return response
        })
    },
    /**
     * 获取项目列表
     *
     * @param {Function} commit store commit mutation handler
     * @param {Object} state store state
     * @param {Function} dispatch store dispatch action handler
     *
     * @return {Promise} promise 对象
     */
    requestProjectList  ({ commit }) {
        return vue.$ajax.get('/project/api/user/projects/').then(response => {
            const res = response || []

            commit('forceUpdateProjectList', res)
            return res
        })
    }
}
