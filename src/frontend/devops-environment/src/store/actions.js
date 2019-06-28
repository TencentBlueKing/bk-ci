/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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
import {
    
} from './constants'

const prefix = 'environment/api'
const vue = new Vue()

const actions = {
    /**
     * 环境列表
     */
    requestEnvList ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/environment/${projectId}`).then(response => {
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
    requestNodeList ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/envnode/${projectId}`).then(response => {
            return response
        })
    },
    /**
     * 环境的节点列表
     */
    requestEnvNodeList ({ commit }, { projectId, envHashId }) {
        return vue.$ajax.post(`${prefix}/user/environment/${projectId}/${envHashId}/listNodes`, { }).then(response => {
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
     * 修改节点导入人
     */
    changeCreatedUser ({ commit }, { projectId, nodeHashId, params }) {
        return vue.$ajax.post(`${prefix}/user/envnode/${projectId}/${nodeHashId}/changeCreatedUser`, params).then(response => {
            return response
        })
    },
    /**
     * 生成构建机命令
     */
    requestDevCommand ({ commit }, { projectId, model, zoneName }) {
        const urls = zoneName ? `${prefix}/user/environment/thirdPartyAgent/projects/${projectId}/os/${model}/generateLink?zoneName=${zoneName}` : `${prefix}/user/environment/thirdPartyAgent/projects/${projectId}/os/${model}/generateLink`
        return vue.$ajax.get(urls).then(response => {
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
    saveParallelTaskCount ({ commit }, { projectId, nodeHashId, parallelTaskCount }) {
        return vue.$ajax.post(`${prefix}/user/environment/thirdPartyAgent/projects/${projectId}/nodes/${nodeHashId}/parallelTaskCount?parallelTaskCount=${parallelTaskCount}`).then(response => {
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
    }
}

export default actions
