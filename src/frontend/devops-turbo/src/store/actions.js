import Vue from 'vue'
import {
    TEST_ACTION
} from './constants'

const prefix = 'turbo/api'
const processPrefix = 'process/api'
const vue = new Vue()

const actions = {
    testAction ({ commit, state, dispatch }, codelib) {
        commit(TEST_ACTION, {
            turbo: 'Hello turbossssssssssssss'
        })
    },
    /**
     * 任务注册
     */
    commitRegist ({ commit }, { params, projectName }) {
        // console.dir(registParam)
        return vue.$ajax.post(`${prefix}/user/turbo/task/register?projectName=${projectName}`, params)
    },
    /**
     * 任务构建配置
     */
    commitBuild ({ commit }, { params, operType, projectName }) {
        return vue.$ajax.put(`${prefix}/user/turbo/task?operType=${operType}&projectName=${projectName}`, params)
    },
    /**
     * 第三方构建机配置
     */
    commitMachine ({ commit }, { params, taskId, operType }) {
        return vue.$ajax.post(`${prefix}/user/turbo/task/machine/${taskId}?operType=${operType}`, params)
    },
    /**
     * 下载distcc客户端
     */
    downloadDistcc ({ commit }, { projectId, taskId, machineId, operType }) {
        return vue.$ajax.post(`${prefix}/user/turbo/distcc/client/${projectId}/${taskId}/${machineId}?operType=${operType}`)
    },
    /**
     * 查询客户端下载状态
     */
    requestDownloadStatus ({ commit }, { params }) {
        return vue.$ajax.post(`${prefix}/user/turbo/client/status`, params)
    },
    
    requestMachineIp ({ commit }, { projectId }) {
        return vue.$ajax.get(`environment/api/user/envnode/${projectId}`)
    },

    setInstallOnly ({ commit }, { installIndex, param }) {
        commit('setInstallStatus', { installIndex, param })
        commit('clearInstallTimer', { installIndex })
    },
    /**
     * 流水线列表
     */
    requestPipelineList ({ commit }, { projectId }) {
        return vue.$ajax.get(`${processPrefix}/user/pipelineInfos/${projectId}`)
    },
    /**
     * 流水线权限列表
     */
    requestPipelineListPermission ({ commit }, { projectId }) {
        // return vue.$ajax.get(`${processPrefix}/user/pipelineInfos/${projectId}`)
        // return vue.$ajax.get(`${processPrefix}/user/pipelines/${projectId}/hasPermissionList?permission=EXECUTE&limit=-1`)
        return vue.$ajax.get(`${processPrefix}/user/pipelines/projects/${projectId}/listViewPipelines?page=1&pageSize=-1&sortType=CREATE_TIME&viewId=allPipeline`)
    },
    /**
     * 单条流水线信息
     */
    requestContainerList ({ commit }, { projectId, pipelineId }) {
        return vue.$ajax.get(`${processPrefix}/user/pipelines/${projectId}/${pipelineId}`)
    },
    /**
     * 第三方构建机列表
     */
    requestMachineList ({ commit }, { projectId }) {
        return vue.$ajax.get(`environment/api/user/environment/thirdPartyAgent/projects/${projectId}/os/LINUX/list`)
    },

    /**
     * 根据项目id查询所有构建机信息
     */
    requestMachineByProject ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/turbo/machine/project/${projectId}`)
    },

    /**
     * 根据项目id查询所有构建机信息
     */
    requestSoftInstall ({ commit }, { projectId, ip }) {
        return vue.$ajax.get(`${prefix}/user/turbo/machine/ip/${projectId}/${ip}`)
    },

    /**
     * 根据任务id查询第三种接入方式IP列表
     */
    requestIpList ({ commit }, { taskId }) {
        return vue.$ajax.get(`${prefix}/user/turbo/task/ipList/${taskId}`)
    },
    
    /**
     * 总览数据
     */
    requestOverview ({ commit }, projectId) {
        console.log(`${prefix}/user/turbo/project/report/${projectId}`)
        return vue.$ajax.get(`${prefix}/user/turbo/project/report/${projectId}`)
    },

    /**
     * 加速任务列表
     */
    requestTaskList ({ commit }, { params, projectId }) {
        const hash = hashSplice(params)
        return vue.$ajax.get(`${prefix}/user/turbo/task/project/${projectId}` + hash)
    },
    /**
     * 删除加速任务
     */
    deleteTask ({ commit }, { taskId }) {
        return vue.$ajax.delete(`${prefix}/user/turbo/task/${taskId}`)
    },
    /**
     * 加速任务状态
     */
    requestTaskStatus ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/turbo/task/all-status`)
    },
    /**
     * 单任务图表查询
     */
    requestTaskChart ({ commit }, { taskId, machineIp }) {
        return vue.$ajax.get(`${prefix}/user/turbo/task/report/${taskId}?machineIp=${machineIp}`)
    },
    /**
     * 单任务信息查询
     */
    requestTaskInfo ({ commit }, { taskId }) {
        return vue.$ajax.get(`${prefix}/user/turbo/task/machine/${taskId}`)
    },
    /**
     * 加速记录
     */
    requestRecord ({ commit }, { params, projectId }) {
        const hash = hashSplice(params)
        return vue.$ajax.get(`${prefix}/user/turbo/task/record/${projectId}` + hash)
    },
    /**
     * 加速记录状态
     */
    requestRecordStatus ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/turbo/report/all-status`)
    },
    /**
     * 查询编译器配置类型
     */
    getCompilerConfig ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/turbo/compile/configure/${projectId}`)
    }
}

function hashSplice (params) {
    let hash = ''
    for (const key in params) {
        params[key] && (hash += '&' + key + '=' + params[key])
    }
    return hash ? '?' + hash.slice(1) : ''
}

export default actions
