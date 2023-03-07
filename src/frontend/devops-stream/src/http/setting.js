import api from './ajax'
import { ENVIRNMENT_PERFIX, TICKET_PERFIX, EXP_PERFIX, STREAM_PERFIX, DISPATCH_STREAM_PERFIX } from './perfix'

export default {
    getThirdAgentZoneList (projectId, os) {
        return api.get(`${ENVIRNMENT_PERFIX}/user/environment/thirdPartyAgent/projects/${projectId}/os/${os}/gateway`)
    },

    getThirdAgentLink (projectId, os, zone) {
        return api.get(`${ENVIRNMENT_PERFIX}/user/environment/thirdPartyAgent/projects/${projectId}/os/${os}/generateLink?zoneName=${zone}`)
    },

    getThirdAgentStatus (projectId, agentId) {
        return api.get(`${ENVIRNMENT_PERFIX}/user/environment/thirdPartyAgent/projects/${projectId}/agents/${agentId}/status`)
    },

    getTicketList (projectId, params) {
        return api.get(`${TICKET_PERFIX}/user/credentials/${projectId}`, { params })
    },

    getTicketDetail (projectId, credentialId) {
        return api.get(`${TICKET_PERFIX}/user/credentials/${projectId}/${credentialId}`)
    },

    createTicket (projectId, params) {
        return api.post(`${TICKET_PERFIX}/user/credentials/${projectId}`, params)
    },

    modifyTicket (projectId, params, credentialId) {
        return api.put(`${TICKET_PERFIX}/user/credentials/${projectId}/${credentialId}`, params)
    },

    updateTicketSetting (projectId, params, credentialId) {
        return api.put(`${TICKET_PERFIX}/user/credentials/${projectId}/${credentialId}/setting`, params)
    },

    deleteTicket (projectId, credentialId) {
        return api.delete(`${TICKET_PERFIX}/user/credentials/${projectId}/${credentialId}`)
    },

    getExpGroupList (projectId, params) {
        return api.get(`${EXP_PERFIX}/user/groups/${projectId}/list`, { params })
    },

    createExpGroup (projectId, params) {
        return api.post(`${EXP_PERFIX}/user/groups/${projectId}`, params)
    },

    modifyExpGroup (projectId, params, groupId) {
        return api.put(`${EXP_PERFIX}/user/groups/${projectId}/${groupId}`, params)
    },

    deleteExpGroup (projectId, groupId) {
        return api.delete(`${EXP_PERFIX}/user/groups/${projectId}/${groupId}`)
    },

    getOuterUserList (projectId) {
        return api.get(`${EXP_PERFIX}/user/experiences/outer/list?projectId=${projectId}`)
    },

    saveSetting (projectId, params) {
        return api.post(`${STREAM_PERFIX}/user/basic/setting/${projectId}/save`, params)
    },

    saveTriggerSetting (projectId, params) {
        return api.post(`${STREAM_PERFIX}/user/basic/setting/${projectId}/save_review_setting`, params)
    },

    getSetting (projectId) {
        return api.get(`${STREAM_PERFIX}/user/basic/setting/${projectId}`)
    },

    getSystemPoolDetail () {
        return api.get(`${DISPATCH_STREAM_PERFIX}/user/dockerhost/dockerhost-load`)
    },

    getEnvironmentList (projectId) {
        return api.get(`${ENVIRNMENT_PERFIX}/user/environment/${projectId}`)
    },

    getSystemNodeList (projectId) {
        return api.get(`${ENVIRNMENT_PERFIX}/user/envnode/${projectId}`)
    },

    getNodeList (projectId, poolHash) {
        return api.post(`${ENVIRNMENT_PERFIX}/user/environment/${projectId}/${poolHash}/listNodes`)
    },

    deleteEnvNode (projectId, envHashId, params) {
        return api.post(`${ENVIRNMENT_PERFIX}/user/environment/${projectId}/${envHashId}/deleteNodes`, params).then(response => {
            return response
        })
    },

    addEnvironment (projectId, params) {
        return api.post(`${ENVIRNMENT_PERFIX}/user/environment/${projectId}`, params)
    },

    deleteEnvironment (projectId, envHashId) {
        return api.delete(`${ENVIRNMENT_PERFIX}/user/environment/${projectId}/${envHashId}`)
    },

    addNodeToPool (projectId, poolId, params) {
        return api.post(`${ENVIRNMENT_PERFIX}/user/environment/${projectId}/${poolId}/addNodes`, params)
    },

    addNodeToSystem (projectId, agentId) {
        return api.post(`${ENVIRNMENT_PERFIX}/user/environment/thirdPartyAgent/projects/${projectId}/agents/${agentId}/import`)
    },

    toggleEnableCi (enabled, projectInfo) {
        return api.post(`${STREAM_PERFIX}/user/basic/setting/enable?enabled=${enabled}`, projectInfo)
    },

    resetAuthorization (gitProjectId) {
        return api.get(`${STREAM_PERFIX}/user/basic/setting/isOauth`, {
            params: {
                gitProjectId,
                refreshToken: true,
                redirectUrl: location.href,
                redirectUrlType: 'SPEC'
            }
        }).then((res) => {
            if (res.status === 403) {
                location.href = res.url
                return Promise.reject(res.message)
            } else {
                return res
            }
        })
    },

    requestNodeList (projectId) {
        return api.get(`${ENVIRNMENT_PERFIX}/user/envnode/${projectId}`)
    },

    importEnvNode (projectId, envHashId, params) {
        return api.post(`${ENVIRNMENT_PERFIX}/user/environment/${projectId}/${envHashId}/addNodes`, params).then(response => {
            return response
        })
    },

    /**
     * 修改节点名称
     */
    updateDisplayName (projectId, nodeHashId, params) {
        return api.post(`${ENVIRNMENT_PERFIX}/user/envnode/${projectId}/${nodeHashId}/updateDisplayName`, params)
    },

    requestNodeDetail (projectId, nodeHashId) {
        return api.get(`${ENVIRNMENT_PERFIX}/user/environment/thirdPartyAgent/projects/${projectId}/nodes/${nodeHashId}/thirdPartyAgentDetail`).then(response => {
            return response
        })
    },

    /**
    * 获取 CPU 使用率图表数据
    */
    getNodeCpuMetrics (params) {
        return api.get(`${ENVIRNMENT_PERFIX}/user/environment/thirdPartyAgent/projects/${params.projectId}/nodes/${params.nodeHashId}/queryCpuUsageMetrix?timeRange=${params.timeRange}`).then(response => {
            return response
        })
    },

    /**
    * 获取内存使用率图表数据
    */
    getNodeMemoryMetrics (params) {
        return api.get(`${ENVIRNMENT_PERFIX}/user/environment/thirdPartyAgent/projects/${params.projectId}/nodes/${params.nodeHashId}/queryMemoryUsageMetrix?timeRange=${params.timeRange}`).then(response => {
            return response
        })
    },

    /**
    * 获取网络图表数据
    */
    getNodeNetworkMetrics (params) {
        return api.get(`${ENVIRNMENT_PERFIX}/user/environment/thirdPartyAgent/projects/${params.projectId}/nodes/${params.nodeHashId}/queryNetMetrix?timeRange=${params.timeRange}`).then(response => {
            return response
        })
    },

    /**
    * 获取磁盘IO图表数据
    */
    getNodeDiskioMetrics (params) {
        return api.get(`${ENVIRNMENT_PERFIX}/user/environment/thirdPartyAgent/projects/${params.projectId}/nodes/${params.nodeHashId}/queryDiskioMetrix?timeRange=${params.timeRange}`).then(response => {
            return response
        })
    },

    /**
    * 获取第三方构建机任务
    */
    requestBuildList ({ projectId, nodeHashId, page, pageSize }) {
        return api.get(`${STREAM_PERFIX}/user/basic/setting/projects/${projectId}/nodes/${nodeHashId}/listAgentBuilds?page=${page}&pageSize=${pageSize}`).then(response => {
            return response
        })
    },

    /**
    * 获取第三方构建机活动
    */
    requestActionList ({ projectId, nodeHashId, page, pageSize }) {
        return api.get(`${ENVIRNMENT_PERFIX}/user/environment/thirdPartyAgent/projects/${projectId}/nodes/${nodeHashId}/listAgentActions?page=${page}&pageSize=${pageSize}`).then(response => {
            return response
        })
    },

    /**
    * 设置agent构建并发数
    */
    saveParallelTaskCount (projectId, nodeHashId, parallelTaskCount) {
        return api.post(`${ENVIRNMENT_PERFIX}/user/environment/thirdPartyAgent/projects/${projectId}/nodes/${nodeHashId}/parallelTaskCount?parallelTaskCount=${parallelTaskCount}`).then(response => {
            return response
        })
    },

    getShareProjectList (projectId, envHashId, page = 1, pageSize = 100) {
        return api.get(`${ENVIRNMENT_PERFIX}/user/environment/${projectId}/${envHashId}/list?offset=${page - 1}&limit=${pageSize}`)
    },

    setSharePool (projectId, envHashId, params) {
        return api.post(`${ENVIRNMENT_PERFIX}/user/environment/${projectId}/${envHashId}/share`, params)
    },

    deleteShare (projectId, envHashId, sharedProjectId) {
        return api.delete(`${ENVIRNMENT_PERFIX}/user/environment/${projectId}/${envHashId}/${sharedProjectId}/sharedProject`)
    }
}
