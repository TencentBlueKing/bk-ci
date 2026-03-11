import Vue from 'vue'
import {

} from './constants'

const prefix = 'plugin/api'
const processPrefix = 'process/api'
const experiencePrefix = 'experience/api'
const artifactoryPre = 'artifactory/api'
const vue = new Vue()

const actions = {
    /**
     * 扫描记录列表
     */
    requestVsList ({ commit }, { projectId, page, pageSize }) {
        return vue.$ajax.get(`${prefix}/user/jingang/${projectId}/app?page=${page}&pageSize=${pageSize}`)
    },
    /**
     * 扫描报告
     */
    requestVsReport ({ commit }, { projectId, recordId }) {
        return vue.$ajax.get(`${prefix}/user/jingang/app/result/${recordId}`)
    },
    /**
     * 校验流水线执行权限
     */
    requestHasPermission ({ commit }, { projectId, payload }) {
        return vue.$ajax.get(`${experiencePrefix}/user/experiences/${projectId}/hasPermission`, {
            params: {
                path: payload.path,
                artifactoryType: payload.artifactoryType
            }
        })
    },
    /**
     * 启动扫描
     */
    toScanFile ({ commit }, { projectId, pipelineId, buildId, params }) {
        return vue.$ajax.post(`${prefix}/user/jingang/${projectId}/${pipelineId}/${buildId}/app/scan?buildNo=${params.buildNo}&file=${params.file}&isCustom=${params.isCustom}&runType=${params.runType}`)
    },
    /**
     * 流水线列表
     */
    requestPipelineList ({ commit }, { projectId, params }) {
        return vue.$ajax.get(`${processPrefix}/user/pipelines/${projectId}`, { params })
    },
    /**
     * 获取流水线构建列表
     */
    requestBuildList ({ commit }, { projectId, pipelineId, params }) {
        return vue.$ajax.get(`${processPrefix}/user/builds/${projectId}/${pipelineId}/history`, { params })
    },
    /**
     * 获取文件列表
     */
    requestFileList ({ commit }, { projectId, params }) {
        return vue.$ajax.post(`${artifactoryPre}/user/artifactories/${projectId}/searchFileAndProperty`, params)
    }
}

export default actions
