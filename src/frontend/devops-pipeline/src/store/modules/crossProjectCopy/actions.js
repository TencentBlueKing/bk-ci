/**
 * 跨项目复制 Actions
 */

import {
    PROCESS_API_URL_PREFIX,
    REPOSITORY_API_URL_PREFIX
} from '@/store/constants'
import ajax from '@/utils/request'

export default {
    /**
     * 创建流水线批量任务√
     */
    async createBatchTask ({ commit }, { projectId, params }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/batch/task/${projectId}/tasks`, params).then(res => {
            return res.data
        })
    },
    /**
     * 查询流水线批量任务明细状态汇总√
     */
    async getTaskStatusSummary ({ commit }, { projectId, taskId }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/batch/task/${projectId}/tasks/${taskId}/details/status/summary?taskType=PIPELINE_COPY`).then(res => {
            return res.data
        })
    },
    /**
     * 查询流水线批量任务明细√
     */
    async getTaskDetails ({ commit }, { projectId, taskId, params }) {
        const query = new URLSearchParams(params).toString()
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/batch/task/${projectId}/tasks/${taskId}/details?${query}`).then(res => {
            return res.data
        })
    },
    /**
     * 获取流水线复制任务详情√
     */
    async getCopyTaskDetail ({ commit }, { projectId, taskId }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/copy/${projectId}/tasks/${taskId}`).then(res => {
            return res.data
        })
    },
    /**
     * 排除流水线批量任务明细√
     */
    async excludeTaskDetail ({ commit }, { projectId, taskId, pipelineId }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/batch/task/${projectId}/tasks/${taskId}/pipelines/${pipelineId}/exclude`).then(res => {
            return res.data
        })
    },
    /**
     * 恢复流水线批量任务明细√
     */
    async restoreTaskDetail ({ commit }, { projectId, taskId, pipelineId }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/batch/task/${projectId}/tasks/${taskId}/pipelines/${pipelineId}/restore`).then(res => {
            return res.data
        })
    },
    /**
     * 回复全部已排除的流水线批量任务明细√
     */
    async restoreAllExcludedTaskDetail ({ commit }, { projectId, taskId }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/batch/task/${projectId}/tasks/${taskId}/pipelines/restore`).then(res => {
            return res.data
        })
    },
    /**
     * 分析流水线复制资源依赖√
     */
    async analyzeResourceDepend ({ commit }, { projectId, taskId, params }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/copy/${projectId}/tasks/${taskId}/resources/depend/analysis`, params).then(res => {
            return res.data
        })
    },
    /**
     * 保存流水线复制配置草稿√
     */
    async saveConfigDraft ({ commit }, { projectId, taskId, params }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/copy/${projectId}/tasks/${taskId}/config/draft`, params).then(res => {
            return res.data
        })
    },

    /**
     * 列举流水线复制资源详情√
     */
    async listResourceDetails ({ commit }, { projectId, taskId, params }) {
        const query = new URLSearchParams(params).toString()
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/copy/${projectId}/tasks/${taskId}/resources?${query}`).then(res => {
            return res.data
        })
    },

    /**
     * 获取项目列表√
     */
    async getProjectList ({ commit }) {
        return ajax.get('/project/api/user/projects/').then(res => {
            return res.data
        })
    },

    /**
     * 获取目标项目有使用权限的凭据列表√
     */
    async getCredentialList ({ commit }, { projectId }) {
        return ajax.get(`/ticket/api/user/credentials/${projectId}/hasPermissionList?permission=USE&page=1&pageSize=10000`).then(res => {
            return res.data
        })
    },
    /**
     * 获取OAuth用户列表
     */
    async getOauthUserList ({ commit }, { scmCode }) {
        return ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/oauth/userList?scmCode=${scmCode}`)
    },
    /**
     * 保存流水线复制资源草稿√
     */
    async saveResourceDraft ({ commit }, { projectId, taskId, params }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/copy/${projectId}/tasks/${taskId}/resources/draft`, params).then(res => {
            return res.data
        })
    },
    /**
     * 准备执行流水线复制（下一步：任务执行）√
     */
    async prepareExecute ({ commit }, { projectId, taskId, params }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/copy/${projectId}/tasks/${taskId}/execute/prepare`, params).then(res => {
            return res.data
        })
    },
    /**
     * 执行流水线复制√
     */
    async executeCopy ({ commit }, { projectId, taskId }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/copy/${projectId}/tasks/${taskId}/execute`).then(res => {
            return res.data
        })
    },
    /**
     * 获取流水线复制执行进度√
     */
    async getExecuteProgress ({ commit }, { projectId, taskId }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/copy/${projectId}/tasks/${taskId}/execute/progress`).then(res => {
            return res.data
        })
    },
    /**
     * 重试单个失败流水线批量任务明细√
     */
    async retryFailedTaskDetail ({ commit }, { projectId, taskId, pipelineId }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/batch/task/${projectId}/tasks/${taskId}/pipelines/${pipelineId}/retry`).then(res => {
            return res.data
        })
    },
    /**
     * 重试失败流水线批量任务
     */
    async retryFailedTask ({ commit }, { projectId, taskId }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/batch/task/${projectId}/tasks/${taskId}/retry`).then(res => {
            return res.data
        })
    },
    /**
     * 获取流水线复制执行汇总
     */
    async getExecuteSummary ({ commit }, { projectId, taskId }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/copy/${projectId}/tasks/${taskId}/execute/summary`).then(res => {
            return res.data
        })
    },
    /**
     * 确认流水线复制资源
     */
    async confirmResource ({ commit }, { projectId, taskId, resourceType, resourceId }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/copy/${projectId}/tasks/${taskId}/resources/${resourceType}/${resourceId}/confirm`).then(res => {
            return res.data
        })
    },
    /**
     * 查询流水线复制资源关联的流水线
     */
    async getResourcePipelines ({ commit }, { projectId, taskId, resourceType, resourceId, params }) {
        const query = new URLSearchParams(params).toString()
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/copy/${projectId}/tasks/${taskId}/resources/${resourceType}/${resourceId}/pipelines?${query}`).then(res => {
            return res.data
        })
    },
    /**
     * 查询流水线批量任务列表
     */
    async getTaskList ({ commit }, { projectId, params }) {
        const query = new URLSearchParams(params).toString()
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/batch/task/${projectId}/tasks?${query}`).then(res => {
            return res.data
        })
    },
    /**
     * 删除流水线批量任务
     */
    async deleteTask ({ commit }, { projectId, taskId }) {
        return ajax.delete(`${PROCESS_API_URL_PREFIX}/user/pipeline/batch/task/${projectId}/tasks/${taskId}`).then(res => {
            return res.data
        })
    },
    /**
     * 查询流水线批量任务数量
     */
    async getTaskCount ({ commit }, { projectId, status }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/batch/task/${projectId}/tasks/count?status=${status}`).then(res => {
            return res.data
        })
    },
    /**
     * 查询流水线批量任务状态汇总
     */
    async getHistoryTaskStatusSummary ({ commit }, { projectId }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/batch/task/${projectId}/tasks/status/summary`).then(res => {
            return res.data
        })
    },
    /**
     * 一键设置流水线复制资源策略
     */
    async setResourceStrategy ({ commit }, { projectId, taskId }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/copy/${projectId}/tasks/${taskId}/resources/strategy/auto`).then(res => {
            return res.data
        })
    }

}
