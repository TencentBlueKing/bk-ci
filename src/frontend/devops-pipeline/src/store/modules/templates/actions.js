import {
    PROCESS_API_URL_PREFIX,
    REPOSITORY_API_URL_PREFIX
} from '@/store/constants'
import ajax from '@/utils/request'
import {
    SET_TEMPLATE_DETAIL,
    UPDATE_INSTANCE_PAGE_LOADING
} from './constants'
const actions = {
    hasPipelineTemplatePermission (_, { projectId, permission }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/hasPipelineTemplatePermission?&permission=${permission}`).then(response => {
            return response.data
        })
    },
    templateCopy (_, { projectId, params }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/copy`, params).then(response => {
            return response.data
        })
    },
    deleteTemplate (_, { projectId, templateId }) {
        return ajax.delete(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/${templateId}/delete`).then(response => {
            return response.data
        })
    },
    getType2Count (_, { projectId }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/getType2Count`).then(response => {
            return response.data
        })
    },
    getTemplateList (_, params) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${params.projectId}/list`, params).then(response => {
            return response.data
        })
    },
    createTemplate (_, { projectId, params }) {
        return ajax.post(`/${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/create`, params).then(response => {
            return response.data
        })
    },
    importTemplateFromStore (_, { projectId, templateId, params }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/create/market${templateId ? `?templateId=${templateId}` : ''}`, params)
    },
    getSourceCount (_, params) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${params.projectId}/getSource2Count`, params).then(response => {
            return response.data
        })
    },
    exportYamlTemplate (_, { projectId, templateId, version }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/${templateId}/export`, version)
    },
    requestInstanceList (_, { projectId, templateId, page, pageSize, ...params }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/template/instances/v2/projects/${projectId}/templates/${templateId}?page=${page}&pageSize=${pageSize}`, { params }).then(response => {
            return response.data
        })
    },
    requestTemplateChangelogs (_, { projectId, templateId, ...params }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/${templateId}/operationLog`, { params }).then(response => response.data)
    },
    requestTemplateOperatorList (_, { projectId, templateId, ...params }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/${templateId}/operatorList`, { params }).then(response => response.data)
    },
    deleteTempalteVersion (_, { projectId, templateId, version }) {
        return ajax.delete(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/${templateId}/${version}`)
    },
    rollbackTemplateVersion (_, { projectId, templateId, version }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/${templateId}/rollbackDraft?version=${version}`).then(response => response.data)
    },
    requestTemplateVersionList (_, params) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${params.projectId}/${params.templateId}/versions`, params).then(response => response.data)
    },
    fetchPipelineDetailById ({ commit }, { projectId, templateId, pipelineIds }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/template/instances/v2/projects/${projectId}/templates/${templateId}/pipelines`, pipelineIds).then(res => {
            return res.data
        })
    },
    requestVersionCompare (_, { projectId, templateId, pipelineId, comparedVersion }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/template/instances/v2/projects/${projectId}/${templateId}/compare?pipelineId=${pipelineId}&comparedVersion=${comparedVersion}`)
    },
    templatePreviewDetail (_, { projectId, templateId }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/${templateId}/latest/details`).then(response => response.data)
    },
    transformTemplateToCustom (_, { projectId, templateId }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/${templateId}/transformTemplateToCustom`).then(response => response.data)
    },
    templateUpdateUpgradeStrategy (_, { projectId, templateId }) {
        return ajax.put(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/${templateId}/updateUpgradeStrategy`).then(response => response.data)
    },
    updateInstance ({ commit }, { projectId, templateId, version, params }) {
        return ajax.put(`${PROCESS_API_URL_PREFIX}/user/template/instances/v2/projects/${projectId}/templates/${templateId}/async/update?version=${version}`, params)
    },
    releaseInstance ({ commit }, { projectId, templateId, version, params }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/template/instances/v2/projects/${projectId}/templates/async/${templateId}?version=${version}`, params)
    },
    // 获取模板实例化发布时的版本信息
    fetchTemplateReleasePreFetch ({ commit }, { projectId, templateId, version, params }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/template/instances/v2/projects/${projectId}/templates/${templateId}/${version}/preFetch?version=${version}`, params)
    },
    // 重试发布实例化
    retryReleaseInstance ({ commit }, { projectId, baseId }) {
        return ajax.post(`${PROCESS_API_URL_PREFIX}/user/template/instances/v2/projects/${projectId}/task/${baseId}/retry`).then(res => {
            return res.data
        })
    },
    // 发布失败修改配置，获取发布实例化任务的参数
    fetchTaskDetailParams ({ commit }, { projectId, baseId, status }) {
        const url = status
            ? `${PROCESS_API_URL_PREFIX}/user/template/instances/v2/projects/${projectId}/task/${baseId}/detail?status=${status}`
            : `${PROCESS_API_URL_PREFIX}/user/template/instances/v2/projects/${projectId}/task/${baseId}/detail`
        return ajax.get(url)
    },
    // 获取正在发布的实例化任务列表
    fetchReleaseTaskList ({ commit }, { projectId, templateId }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/template/instances/v2/projects/${projectId}/templates/${templateId}/task`)
    },
    // 获取实例化发布状态（轮询）
    fetchReleaseTaskStatus ({ commit }, { projectId, baseId }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/template/instances/v2/projects/${projectId}/task/${baseId}/result`)
    },
    updateTemplateData ({ commit }, { data, version }) {
        const { triggerElements, param, buildNo, ...restData } = data
        const triggerConfigs = triggerElements.map(i => ({
            atomCode: i.atomCode,
            stepId: i.stepId ?? '',
            disabled: Object.hasOwnProperty.call(i?.additionalOptions ?? {}, 'enable') ? !i?.additionalOptions?.enable : false,
            cron: i.advanceExpression,
            name: i.name,
            version: i.version,
            isFollowTemplate: true,
            ...(
                i.startParams ? {
                    variables: JSON.parse(i.startParams)
                } : {}
            )
        }))
        commit(SET_TEMPLATE_DETAIL, {
            templateDetail: {
                ...restData,
                enablePac: false,
                param: param.map(p => {
                    return {
                        ...p,
                        isRequiredParam: p.required,
                        isFollowTemplate: false
                    }
                }),
                ...(buildNo ? {
                    buildNo: {
                        ...buildNo,
                        isRequiredParam: buildNo.required,
                        isFollowTemplate: false
                    }
                } : undefined),
                triggerConfigs
            },
            templateVersion: version
        })
    },
    fetchTemplateDetailByVersion ({ commit }, { projectId, templateId, version }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/${templateId}/${version}/details/`).then(res => {
            return res.data
        })
    },
    fetchTemplateDetailByRef ({ commit }, { projectId, templateId, ref }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/${templateId}/ref/details?ref=${ref}`).then(res => {
            return res.data
        })
    },
    // 根据模板ID，版本号获取模板配置参数
    fetchTemplateParamsById ({ commit }, { projectId, templateId, version }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/template/instances/v2/projects/${projectId}/templates/${templateId}/instanceParamsById?version=${version}`).then(res => {
            return res.data
        })
    },
    
    // 根据引用获取模板配置参数
    fetchTemplateParamsByRef ({ commit }, { projectId, templateId, ref }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/template/instances/v2/projects/${projectId}/templates/${templateId}/instanceParamsByRef?ref=${ref}`).then(res => {
            return res.data
        })
    },
    getBranchesListByProjectId ({ commit }, { projectId, searchKey, repoHashId }) {
        return ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/scm/repository/api/${projectId}/listBranches?repositoryType=ID&repoHashIdOrName=${repoHashId}&page=1&pageSize=200&search=${searchKey}`)
    },
    getTagsListByProjectId ({ commit }, { projectId, searchKey, repoHashId }) {
        return ajax.post(`${REPOSITORY_API_URL_PREFIX}/user/scm/repository/api/${projectId}/listTags?repositoryType=ID&repoHashIdOrName=${repoHashId}&page=1&pageSize=200&search=${searchKey}`)
    },
    updateInstancePageLoading ({ commit} , value) {
        commit(UPDATE_INSTANCE_PAGE_LOADING, value)
    },
    requestTemplatePreviewByVersion (_, { projectId, templateId, ...params }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/pipeline/template/v2/${projectId}/${templateId}/preview`, {
            params
        }).then(response => {
            return response.data
        })
    },
    checkTemplatePipelineRollback ({ commit }, { projectId, pipelineId, version }) {
        return ajax.get(`${PROCESS_API_URL_PREFIX}/user/version/projects/${projectId}/pipelines/${pipelineId}/canRollback?version=${version}`).then(response => {
            return response
        })
    }
}

export default actions
