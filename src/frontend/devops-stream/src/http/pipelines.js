import api from './ajax'
import { LOG_PERFIX, ARTIFACTORY_PREFIX, PROCESS_PREFIX, STREAM_PERFIX, DISPATCH_STREAM_PERFIX, QUALITY_PREFIX } from './perfix'

export default {
    // 第一次拉取日志
    getInitLog ({ projectId, pipelineId, buildId, tag, currentExe, subTag, debug, jobId }) {
        const queryType = jobId ? { jobId } : { tag }
        return api.get(`${LOG_PERFIX}/user/logs/${projectId}/${pipelineId}/${buildId}`, {
            params: {
                executeCount: currentExe,
                subTag,
                debug,
                ...queryType
            }
        })
    },

    // 后续拉取日志
    getAfterLog ({ projectId, pipelineId, buildId, tag, currentExe, lineNo, subTag, debug, jobId }) {
        const queryType = jobId ? { jobId } : { tag }
        return api.get(`${LOG_PERFIX}/user/logs/${projectId}/${pipelineId}/${buildId}/after`, {
            params: {
                start: lineNo,
                executeCount: currentExe,
                subTag,
                debug,
                ...queryType
            }
        })
    },

    getLogStatus ({ projectId, pipelineId, buildId, tag, executeCount }) {
        return api.get(`${LOG_PERFIX}/user/logs/${projectId}/${pipelineId}/${buildId}/mode`, { params: { tag, executeCount } })
    },

    getDownloadLogFromArtifactory ({ projectId, pipelineId, buildId, tag, executeCount }) {
        return api.get(`${ARTIFACTORY_PREFIX}/user/artifactories/log/plugin/${projectId}/${pipelineId}/${buildId}/${tag}/${executeCount}`).then((res) => {
            const data = res.data || {}
            return data.url || ''
        })
    },

    requestPartFile ({ projectId, params }) {
        return api.post(`${ARTIFACTORY_PREFIX}/user/artifactories/${projectId}/search`, params)
    },

    requestExecPipPermission ({ projectId, pipelineId, permission }) {
        return api.get(`${PROCESS_PREFIX}/user/pipelines/${projectId}/${pipelineId}/hasPermission?permission=${permission}`)
    },

    requestPermission (projectId) {
        return api.get(`${STREAM_PERFIX}/user/permission/projects/${projectId}/resource/validate`)
    },

    requestDevnetGateway () {
        return api.get(`${ARTIFACTORY_PREFIX}/user/artifactories/checkDevnetGateway`)
    },

    requestDownloadUrl ({ projectId, artifactoryType, path }) {
        return api.post(`${ARTIFACTORY_PREFIX}/user/artifactories/${projectId}/${artifactoryType}/downloadUrl?path=${encodeURIComponent(path)}`)
    },

    requestArtifactExternalUrl ({ projectId, artifactoryType, path }) {
        return api.post(`${ARTIFACTORY_PREFIX}/user/artifactories/${projectId}/${artifactoryType}/externalUrl?path=${encodeURIComponent(path)}`)
    },

    requestReportList ({ projectId, pipelineId, buildId }) {
        return api.get(`${STREAM_PERFIX}/user/current/build/projects/${projectId}/pipelines/${pipelineId}/builds/${buildId}/report`)
    },

    getPipelineList ({ projectId, ...params }) {
        return api.get(`${STREAM_PERFIX}/user/pipelines/${projectId}/list`, { params })
    },

    getPipelineInfoList ({ projectId, ...params }) {
        return api.get(`${STREAM_PERFIX}/user/pipelines/${projectId}/listInfo`, { params })
    },

    getPipelineBuildList (projectId, params) {
        return api.post(`${STREAM_PERFIX}/user/history/build/list/${projectId}`, params)
    },

    getPipelineBuildBranchList (projectId, params = {}) {
        return api.get(`${STREAM_PERFIX}/user/history/build/branch/list/${projectId}`, { params })
    },

    getPipelineBuildMemberList (projectId) {
        return api.get(`${STREAM_PERFIX}/user/gitcode/projects/members?projectId=${projectId}`)
    },

    getPipelineBuildDetail (projectId, params) {
        return api.get(`${STREAM_PERFIX}/user/current/build/detail/${projectId}`, { params })
    },

    getPipelineBuildYaml (projectId, buildId) {
        return api.get(`${STREAM_PERFIX}/user/trigger/build/getYaml/${projectId}/${buildId}`)
    },

    addPipelineYamlFile (projectId, params) {
        return api.post(`${STREAM_PERFIX}/user/gitcode/projects/repository/files?projectId=${projectId}`, params)
    },

    getPipelineBranches (params) {
        return api.get(`${STREAM_PERFIX}/user/gitcode/projects/repository/branches`, { params })
    },

    getPipelineBuildBranches (params) {
        return api.get(`${STREAM_PERFIX}/user/gitcode/projects/repository/local_branches`, { params })
    },

    getPipelineCommits (params) {
        return api.get(`${STREAM_PERFIX}/user/gitcode/projects/commits`, { params })
    },

    getPipelineBranchYaml (projectId, pipelineId, params) {
        return api.get(`${STREAM_PERFIX}/user/trigger/build/${projectId}/${pipelineId}/yaml`, { params })
    },

    trigglePipeline (pipelineId, params) {
        return api.post(`${STREAM_PERFIX}/user/trigger/build/${pipelineId}/startup`, params)
    },

    toggleEnablePipeline (projectId, pipelineId, enabled) {
        return api.post(`${STREAM_PERFIX}/user/pipelines/${projectId}/${pipelineId}/enable?enabled=${enabled}`)
    },

    updateRemark (projectId, pipelineId, buildId, remark) {
        return api.post(`${PROCESS_PREFIX}/user/builds/${projectId}/${pipelineId}/${buildId}/updateRemark`, { remark })
    },

    rebuildPipeline (projectId, pipelineId, buildId, params = {}) {
        const queryStr = Object.keys(params).reduce((query, key) => {
            const value = params[key]
            if (value !== undefined) {
                const queryVal = `${key}=${value}`
                query += (query === '' ? '?' : '&')
                query += queryVal
            }
            return query
        }, '')
        return api.post(`${STREAM_PERFIX}/user/builds/${projectId}/${pipelineId}/${buildId}/retry${queryStr}`)
    },

    cancelBuildPipeline (projectId, pipelineId, buildId) {
        return api.delete(`${STREAM_PERFIX}/user/builds/${projectId}/${pipelineId}/${buildId}`)
    },

    reviewTrigger (projectId, pipelineId, buildId, approve) {
        const queryStr = `?pipelineId=${pipelineId}&buildId=${buildId}&approve=${approve}`
        return api.post(`${STREAM_PERFIX}/user/current/build/detail/${projectId}/review${queryStr}`)
    },

    getContainerInfoByBuildId (projectId, pipelineId, buildId, vmSeqId) {
        return api.get(`${DISPATCH_STREAM_PERFIX}/user/dockerhost/getContainerInfo/${projectId}/${pipelineId}/${buildId}/${vmSeqId}`)
    },

    startDebugDocker (params) {
        return api.post(`${DISPATCH_STREAM_PERFIX}/user/docker/debug/start`, params)
    },

    stopDebugDocker (projectId, pipelineId, vmSeqId, dispatchType) {
        return api.post(`${DISPATCH_STREAM_PERFIX}/user/docker/debug/stop/projects/${projectId}/pipelines/${pipelineId}/vmseqs/${vmSeqId}?dispatchType=${dispatchType}`).then(res => {
            return res
        })
    },

    resizeTerm (resizeUrl, params) {
        const protocol = document.location.protocol || 'http:'
        return api.post(`${protocol}//${DEVNET_HOST}/${resizeUrl}`, params).then(res => {
            return res && res.Id
        })
    },

    getBuildInfoByBuildNum (projectId, pipelineId, buildNum) {
        return api.get(`${PROCESS_PREFIX}/user/builds/${projectId}/${pipelineId}/detail/${buildNum}`)
    },

    checkYaml (yaml) {
        return api.post(`${STREAM_PERFIX}/user/trigger/build/checkYaml`, { yaml })
    },

    requestQualityGate (projectId, pipelineId, buildId, ids, checkTimes) {
        return api.post(`${QUALITY_PREFIX}/user/intercepts/v2/pipeline/list?projectId=${projectId}&pipelineId=${pipelineId}&buildId=${buildId}&checkTimes=${checkTimes}`, ids)
    },

    triggerStage ({ projectId, pipelineId, buildId, stageId, cancel, reviewParams, id, suggest }) {
        return api.post(`${PROCESS_PREFIX}/user/builds/projects/${projectId}/pipelines/${pipelineId}/builds/${buildId}/stages/${stageId}/manualStart?cancel=${cancel}`, { reviewParams, id, suggest })
    },

    changeGateWayStatus (val, hashId) {
        return api.put(`${QUALITY_PREFIX}/user/rules/v3/update/${hashId}?pass=${val}`)
    },

    getEventList () {
        return api.get(`${STREAM_PERFIX}/user/request/eventType`)
    },

    getPipelineDirList (projectId, params) {
        return api.get(`${STREAM_PERFIX}/user/pipelines/${projectId}/dir_list`, { params })
    },

    getPipelineParamJson (projectId, pipelineId, params) {
        return api.get(`${STREAM_PERFIX}/user/trigger/build/${projectId}/${pipelineId}/manual`, { params })
    }
}
