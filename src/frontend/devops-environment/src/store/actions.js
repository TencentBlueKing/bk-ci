/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import Vue from 'vue'

const prefix = 'environment/api'
const vue = new Vue()

const actions = {
    /**
     * 环境列表
     */
    requestEnvList ({ commit }, { projectId, params }) {
        const query = new URLSearchParams(params).toString()
        return vue.$ajax.get(`${prefix}/user/environment/${projectId}?${query}`).then(response => {
            return response
        })
    },
    /**
     * 区域集群
     */
    requestClusterList ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/cluster/getClusterList`).then(response => {
            return response
        })
    },
    /**
     * 是否拥有创建环境权限
     */
    requestPermission ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/environment/${projectId}/hasCreatePermission`).then(response => {
            return response
        })
    },
    /**
     * 创建环境
     */
    createNewEnv ({ commit }, { projectId, params }) {
        return vue.$ajax.post(`${prefix}/user/environment/${projectId}`, params).then(response => {
            return response
        })
    },
    /**
     * 修改环境
     */
    toModifyEnv ({ commit }, { projectId, envHashId, params }) {
        return vue.$ajax.post(`${prefix}/user/environment/${projectId}/${envHashId}`, params).then(response => {
            return response
        })
    },
    /**
     * 导入节点到环境
     */
    importEnvNode ({ commit }, { projectId, envHashId, params }) {
        return vue.$ajax.post(`${prefix}/user/environment/${projectId}/${envHashId}/addNodes`, params).then(response => {
            return response
        })
    },
    /**
     * 删除环境
     */
    toDeleteEnv ({ commit }, { projectId, envHashId }) {
        return vue.$ajax.delete(`${prefix}/user/environment/${projectId}/${envHashId}`).then(response => {
            return response
        })
    },
    /**
     * 节点列表
     */
    requestNodeList ({ commit }, { projectId, params, tags }) {
        const query = new URLSearchParams(params).toString()
        return vue.$ajax.post(`${prefix}/user/envnode/${projectId}/fetchNodes?${query}`, { tags }).then(response => {
            return response
        })
    },
    /**
     * 节点标签列表
     */
    async requestNodeTagList ({ commit }, projectId) {
        try {
            const res = await vue.$ajax.get(`${prefix}/user/nodetag/fetchTag?projectId=${projectId}`)
            commit('setNodeTagList', res || [])
            return res
        } catch (err) {
            console.error(err)
            return []
        }
    },
    async requestGetCounts ({ commit }, projectId) {
        try {
            const res = await vue.$ajax.get(`${prefix}/user/envnode/${projectId}/nodesCount`)
            commit('setNodeCount', res || {})
            return res
        } catch (err) {
            console.error(err)
        }
    },
    createdNodeTag ({ commit }, { projectId, params }) {
        return vue.$ajax.post(`${prefix}/user/nodetag/create?projectId=${projectId}`, params).then(response => {
            return response
        })
    },
    deleteNodeTag ({ commit }, { projectId, tagKeyId }) {
        return vue.$ajax.delete(`${prefix}/user/nodetag/deleteTag?projectId=${projectId}&tagKeyId=${tagKeyId}`).then(response => {
            return response
        })
    },
    editNodeTag ({ commit }, { projectId, params }) {
        return vue.$ajax.put(`${prefix}/user/nodetag/updateTag?projectId=${projectId}`, params).then(response => {
            return response
        })
    },
    setNodeTag ({ commit }, { projectId, params }) {
        return vue.$ajax.post(`${prefix}/user/nodetag/editTag?projectId=${projectId}`, params).then(response => {
            return response
        })
    },
    batchEditTag ({ commit }, { projectId, params }) {
        return vue.$ajax.post(`${prefix}/user/nodetag/batchEditTag?projectId=${projectId}`, params).then(response => {
            return response
        })
    },
    /**
     * 环境的节点列表
     */
    requestEnvNodeList ({ commit }, { projectId, envHashId }) {
        return vue.$ajax.post(`${prefix}/user/environment/${projectId}/${envHashId}/listNodes`).then(response => {
            return response
        })
    },
    /**
     * 单个环境信息
     */
    requestEnvDetail ({ commit }, { projectId, envHashId }) {
        return vue.$ajax.get(`${prefix}/user/environment/${projectId}/${envHashId}`).then(response => {
            return response
        })
    },
    /**
     * 删除环境节点
     */
    toDeleteEnvNode ({ commit }, { projectId, envHashId, params }) {
        return vue.$ajax.post(`${prefix}/user/environment/${projectId}/${envHashId}/deleteNodes`, params).then(response => {
            return response
        })
    },
    /**
     * 是否拥有创建(导入)节点权限
     */
    requestNodePermission ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/envnode/${projectId}/hasCreatePermission`).then(response => {
            return response
        })
    },
    /**
     * 删除节点
     */
    toDeleteNode ({ commit }, { projectId, params }) {
        return vue.$ajax.post(`${prefix}/user/envnode/${projectId}/deleteNodes`, params).then(response => {
            return response
        })
    },
    /**
     * 生成构建机命令
     */
    requestDevCommand ({ commit }, { projectId, model, params }) {
        const queryString = new URLSearchParams(params).toString()

        return vue.$ajax.get(`${prefix}/user/environment/thirdPartyAgent/projects/${projectId}/os/${model}/generateBatchInstallLink?${queryString}`).then(response => {
            return response
        })
    },
    /**
     * 构建机Agent信息
     */
    requetConstructNode ({ commit }, { projectId, agentId }) {
        return vue.$ajax.get(`${prefix}/user/environment/thirdPartyAgent/projects/${projectId}/agents/${agentId}/status`).then(response => {
            return response
        })
    },
    /**
     * 导入第三方构建机
     */
    importConstructNode ({ commit }, { projectId, agentId }) {
        let params
        return vue.$ajax.post(`${prefix}/user/environment/thirdPartyAgent/projects/${projectId}/agents/${agentId}/import`, params).then(response => {
            return response
        })
    },
    /**
     * 是否启动第三方构建机接入
     */
    hasConstructPermission ({ commit }, { projectId, agentId }) {
        return vue.$ajax.get(`${prefix}/user/environment/thirdPartyAgent/projects/${projectId}/enable`).then(response => {
            return response
        })
    },
    /**
     * 获取网关列表
     */
    requestGateway ({ commit }, { projectId, model }) {
        return vue.$ajax.get(`${prefix}/user/environment/thirdPartyAgent/projects/${projectId}/os/${model}/gateway`).then(response => {
            return response
        })
    },
    /**
     * 修改节点名称
     */
    updateDisplayName ({ commit }, { projectId, nodeHashId, params }) {
        return vue.$ajax.post(`${prefix}/user/envnode/${projectId}/${nodeHashId}/updateDisplayName`, params).then(response => {
            return response
        })
    },
    /**
     * 获取节点详情
     */
    requestNodeDetail ({ commit }, { projectId, nodeHashId }) {
        return vue.$ajax.get(`${prefix}/user/environment/thirdPartyAgent/projects/${projectId}/nodes/${nodeHashId}/thirdPartyAgentDetail`).then(response => {
            return response
        })
    },

    /**
     * 获取agent环境变量
     */
    requestEnvs ({ commit }, { projectId, nodeHashId }) {
        return vue.$ajax.get(`${prefix}/user/environment/thirdPartyAgent/projects/${projectId}/nodes/${nodeHashId}/envs`).then(response => {
            return response
        })
    },

    /**
     * 保存agent环境变量
     */
    saveEnvs ({ commit }, { projectId, nodeHashId, params }) {
        return vue.$ajax.post(`${prefix}/user/environment/thirdPartyAgent/projects/${projectId}/nodes/${nodeHashId}/envs`, params).then(response => {
            return response
        })
    },

    /**
    * 获取第三方构建机任务
    */
    requestBuildList ({ commit }, { projectId, nodeHashId, page, pageSize }) {
        return vue.$ajax.get(`${prefix}/user/environment/thirdPartyAgent/projects/${projectId}/nodes/${nodeHashId}/listAgentBuilds?page=${page}&pageSize=${pageSize}`).then(response => {
            return response
        })
    },

    /**
    * 获取第三方构建机活动
    */
    requestActionList ({ commit }, { projectId, nodeHashId, page, pageSize }) {
        return vue.$ajax.get(`${prefix}/user/environment/thirdPartyAgent/projects/${projectId}/nodes/${nodeHashId}/listAgentActions?page=${page}&pageSize=${pageSize}`).then(response => {
            return response
        })
    },

    /**
    * 设置agent构建并发数
    */
    saveParallelTaskCount ({ commit }, { projectId, nodeHashId, count }) {
        return vue.$ajax.post(`${prefix}/user/environment/thirdPartyAgent/projects/${projectId}/nodes/${nodeHashId}/parallelTaskCount?parallelTaskCount=${count}`).then(response => {
            return response
        })
    },

    /**
    * 获取 CPU 使用率图表数据
    */
    getNodeCpuMetrics ({ commit }, { params }) {
        return vue.$ajax.get(`${prefix}/user/environment/thirdPartyAgent/projects/${params.projectId}/nodes/${params.nodeHashId}/queryCpuUsageMetrix?timeRange=${params.timeRange}`).then(response => {
            return response
        })
    },

    /**
    * 获取内存使用率图表数据
    */
    getNodeMemoryMetrics ({ commit }, { params }) {
        return vue.$ajax.get(`${prefix}/user/environment/thirdPartyAgent/projects/${params.projectId}/nodes/${params.nodeHashId}/queryMemoryUsageMetrix?timeRange=${params.timeRange}`).then(response => {
            return response
        })
    },

    /**
    * 获取网络图表数据
    */
    getNodeNetworkMetrics ({ commit }, { params }) {
        return vue.$ajax.get(`${prefix}/user/environment/thirdPartyAgent/projects/${params.projectId}/nodes/${params.nodeHashId}/queryNetMetrix?timeRange=${params.timeRange}`).then(response => {
            return response
        })
    },

    /**
    * 获取磁盘IO图表数据
    */
    getNodeDiskioMetrics ({ commit }, { params }) {
        return vue.$ajax.get(`${prefix}/user/environment/thirdPartyAgent/projects/${params.projectId}/nodes/${params.nodeHashId}/queryDiskioMetrix?timeRange=${params.timeRange}`).then(response => {
            return response
        })
    },

    requestShareEnvProjectList (_, { projectId, envHashId, ...query }) {
        return vue.$ajax.get(`${prefix}/user/environment/${projectId}/${envHashId}/list`, {
            params: query
        })
    },

    requestProjects (_, { projectId, envHashId, page, pageSize, search }) {
        return vue.$ajax.get(`${prefix}/user/environment/${projectId}/${envHashId}/list_user_project?page=${page}&pageSize=${pageSize}&search=${search}`)
    },

    shareEnv (_, { projectId, envHashId, body }) {
        return vue.$ajax.post(`${prefix}/user/environment/${projectId}/${envHashId}/share`, body)
    },

    removeProjectShare (_, { projectId, envHashId, sharedProjectId }) {
        return vue.$ajax.delete(`${prefix}/user/environment/${projectId}/${envHashId}/${sharedProjectId}/sharedProject`)
    },
    enableNode (_, { projectId, envHashId, nodeHashId, enableNode }) {
        return vue.$ajax.put(`${prefix}/user/environment/${projectId}/${envHashId}/enableNode/${nodeHashId}?enableNode=${enableNode}`)
    },
    /**
    * 设置docker构建并发数
    */
    saveDockerParallelTaskCount ({ commit }, { projectId, nodeHashId, count }) {
        return vue.$ajax.post(`${prefix}/user/environment/thirdPartyAgent/projects/${projectId}/nodes/${nodeHashId}/dockerParallelTaskCount?count=${count}`).then(response => {
            return response
        })
    },
    /**
     * 获取构建机最近执行记录
     */
    getLatestBuildPipelineList ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/environment/thirdPartyAgent/projects/${projectId}/listLatestBuildPipelines`)
    },

    exportNodeListCSV ({ commit }, { projectId, params }) {
        const queryString = new URLSearchParams(params).toString()
        return vue.$ajax.post(`${prefix}/user/envnode/${projectId}/listNew_export?${queryString}`, {}, {
            originalResponse: true
        }).then(response => {
            return response
        })
    },
    batchChangeImportUser ({ commit }, { projectId, nodeHashIds }) {
        return vue.$ajax.post(`${prefix}/user/envnode/${projectId}/batchChangeImportUser`, nodeHashIds).then(response => {
            return response
        })
    },
    batchUpdateParallelTaskCount ({ commit }, { projectId, params }) {
        return vue.$ajax.post(`${prefix}/user/environment/thirdPartyAgent/projects/${projectId}/nodes/batchUpdateParallelTaskCount`, params).then(response => {
            return response
        })
    },
}

export default actions
