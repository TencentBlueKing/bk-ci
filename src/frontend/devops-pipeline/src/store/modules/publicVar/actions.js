import {
    PROCESS_API_URL_PREFIX
} from '@/store/constants'
import {
    UPDATE_GROUP_DATA,
    UPDATE_PUBLIC_VAR_YAML,
    UPDATE_PUBLIC_VAR_MODE,
    UPDATE_OPERATE_TYPE
} from './constants'
import ajax from '@/utils/request'

const actions = {
    // 新建变量组
    saveVariableGroup (_, { projectId, type, params }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/public/var/groups/projects/${projectId}/add?operateType=${type}`, params).then(response => {
            return response.data
        })
    },
    // 导入公共变量组（yaml）
    importVarByYaml (_, { projectId, type, yaml }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/public/var/groups/projects/${projectId}/import?operateType=${type}`, {
            yaml
        }).then(response => {
            return response.data
        })
    },
    // 导出公共变量组（yaml）
    exportVariable (_, { projectId, groupName }) {
        const fn = (blob) => {
            const a = document.createElement('a')
            const url = window.URL || window.webkitURL || window.moxURL
            a.href = url.createObjectURL(blob)
            if (groupName) a.download = `${groupName}.yaml`
            document.body.appendChild(a)
            a.click()
            document.body.removeChild(a)
        }

        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/public/var/groups/projects/${projectId}/groups/${groupName}/export`).then(res => {
            const blob = new Blob([res], { type: 'text/yaml;charset=utf-8' })
            fn(blob)
        })
    },
    // 获取变量组列表
    fetchVariableGroup (_, { projectId, params }) {
        const query = new URLSearchParams({
            ...params,
        }).toString()
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/public/var/groups/projects/${projectId}/list?${query}`, params).then(response => {
            return response.data
        })
    },
    // 获取流水线下使用的变量组列表
    fetchAllVariableGroupByPipeline (_, { pipelineId, referType, versionName }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/public/var/groups/refers/${pipelineId}/group/info?referType=${referType}&versionName=${versionName}`).then(response => {
            return response.data
        })
    },
    // 删除变量组
    deleteVariableGroup (_, { groupName }) {
        return ajax.delete(`${PROCESS_API_URL_PREFIX}/user/pipeline/public/var/groups/${groupName}`).then(response => {
            return response.data
        })
    },
    // 获取变量组变量列表
    getVariables (_, { groupName }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/public/var/group/${groupName}/variables`).then(response => {
            return response.data
        })
    },
    // 获取变量组引用列表
    getReferenceList (_, { groupName, params }) {
        const query = new URLSearchParams({
            ...params,
        }).toString()
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/public/var/groups/${groupName}/references?${query}`).then(response => {
            return response.data
        })
    },
    // 获取变量组发布记录
    getReferenceHistory (_, { groupName, page, pageSize }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/public/var/groups/${groupName}/releaseHistory?page=${page}&pageSize=${pageSize}`).then(response => {
            return response.data
        })
    },
    // 变量组转换为YAML内容
    switchToYaml (_, { projectId, params }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/public/var/groups/projects/${projectId}/convert`, params).then(response => {
            return response.data
        })
    },
    // YAML内转换为容变量组
    switchToUI (_, { projectId, params }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/public/var/groups/projects/${projectId}/yaml/convert`, params).then(response => {
            return response.data
        })
    },
    // 发布变量组-变更预览
    getChangePreview (_, params) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/public/var/groups/changePreview`, params).then(response => {
            return response.data
        })
    },
    updatePublicVarMode ({ commit }, mode) {
        commit(UPDATE_PUBLIC_VAR_MODE, mode)
    },
    updateGroupData ({ commit }, data) {
        commit(UPDATE_GROUP_DATA, data)
    },
    updatePublicVarYaml ({ commit }, yaml) {
        commit(UPDATE_PUBLIC_VAR_YAML, yaml)
    },
    updateOperateType ({ commit }, value) {
        commit(UPDATE_OPERATE_TYPE, value)
    }
}

export default actions
