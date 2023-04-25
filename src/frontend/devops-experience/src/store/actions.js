import Vue from 'vue'
import {
    UPDATE_SELECTED_FILE,
    UPDATE_IS_EXPIRED,
    UPDATE_USER_GROUP,
    SET_CURRELEASE_DETAIL
} from './constants'

const prefix = 'experience/api'
const processPrefix = 'process/api'
const artifactoryPre = 'artifactory/api'
const vue = new Vue()

const actions = {
    /**
     * 发布体验列表
     */
    requestExpList ({ commit }, { projectId, params }) {
        return vue.$ajax.get(`${prefix}/user/experiences/${projectId}/list`, { params })
    },
    /**
     * 下架发布体验
     */
    toOfflineExp ({ commit }, { projectId, experienceHashId }) {
        return vue.$ajax.put(`${prefix}/user/experiences/${projectId}/${experienceHashId}/offline`)
    },
    /**
     * 用户组列表
     */
    requestGroupList ({ commit }, { projectId, params }) {
        return vue.$ajax.get(`${prefix}/user/groups/${projectId}/list`, { params })
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
    },
    /**
     * 获取文件元数据
     */
    requestMetaList ({ commit }, { projectId, artifactoryType, path }) {
        return vue.$ajax.get(`${artifactoryPre}/user/artifactories/${projectId}/${artifactoryType}/properties`, { params: { path } })
    },
    /**
     * 新增用户组
     */
    createUserGroups ({ commit }, { projectId, params }) {
        return vue.$ajax.post(`${prefix}/user/groups/${projectId}`, params)
    },
    /**
     * 删除用户组
     */
    toDeleteGroups ({ commit }, { projectId, groupHashId }) {
        return vue.$ajax.delete(`${prefix}/user/groups/${projectId}/${groupHashId}`)
    },
    /**
     * 获取用户组详情
     */
    toGetGroupDetail ({ commit }, { projectId, groupHashId }) {
        return vue.$ajax.get(`${prefix}/user/groups/${projectId}/${groupHashId}`)
    },
    /**
     * 修改用户组
     */
    editUserGroups ({ commit }, { projectId, groupHashId, params }) {
        return vue.$ajax.put(`${prefix}/user/groups/${projectId}/${groupHashId}`, params)
    },
    /**
     * 获取组用户
     */
    requestGroupUser ({ commit }, { projectId, groupHashId }) {
        return vue.$ajax.get(`${prefix}/user/groups/${projectId}/${groupHashId}/users`)
    },
    /**
     * 新增体验
     */
    createExperience ({ commit }, { projectId, params }) {
        return vue.$ajax.post(`${prefix}/user/experiences/${projectId}`, params)
    },
    /**
     * 体验详情
     */
    requestExperienceDetail ({ commit }, { projectId, experienceHashId }) {
        return vue.$ajax.get(`${prefix}/user/experiences/${projectId}/${experienceHashId}`)
    },
    /**
     * 编辑体验
     */
    editExperience ({ commit }, { projectId, experienceHashId, params }) {
        return vue.$ajax.put(`${prefix}/user/experiences/${projectId}/${experienceHashId}`, params)
    },
    /**
     * 获取下载链接
     */
    requestExternalUrl ({ commit }, { projectId, experienceHashId }) {
        return vue.$ajax.get(`${prefix}/user/experiences/${projectId}/${experienceHashId}/externalUrl`)
    },
    /**
     * 获取安装包下载链接
     */
    downloadInstallation ({ commit }, { projectId, experienceHashId }) {
        return vue.$ajax.get(`${prefix}/user/experiences/${projectId}/${experienceHashId}/downloadUrl`)
    },
    /**
     * 体验下载统计
     */
    requestDownloadCount ({ commit }, { projectId, experienceHashId }) {
        return vue.$ajax.get(`${prefix}/user/experiences/${projectId}/${experienceHashId}/downloadCount`)
    },
    /**
     * 体验下载用户
     */
    requestDownloadUserCount ({ commit }, { projectId, experienceHashId }) {
        return vue.$ajax.get(`${prefix}/user/experiences/${projectId}/${experienceHashId}/downloadUserCount`)
    },
    /**
     * 校验创建体验权限
     */
    requestHasPermission ({ commit }, { projectId, payload }) {
        return vue.$ajax.get(`${prefix}/user/experiences/${projectId}/hasPermission`, {
            params: {
                path: payload.path,
                artifactoryType: payload.artifactoryType
            }
        })
    },
    /**
     * 获取项目组信息和所有人员
     */
    requestUserGroup ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/groups/${projectId}/projectGroupAndUsers`)
    },

    /**
     * 获取外部体验人员
     */
    fetchOutersList ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/experiences/outer/list?projectId=${projectId}`)
    },
    updateCurSelectedFile ({ commit }, { selectFile }) {
        commit(UPDATE_SELECTED_FILE, {
            fileInfo: selectFile
        })
    },
    updateIsExpired ({ commit }, { isExpired }) {
        commit(UPDATE_IS_EXPIRED, {
            isExpired: isExpired
        })
    },
    updateselectUserGroup ({ commit }, { userList }) {
        commit(UPDATE_USER_GROUP, {
            userList
        })
    },
    setCurReleaseDetail ({ commit }, curReleaseDetail) {
        commit(SET_CURRELEASE_DETAIL, curReleaseDetail)
    }
}

export default actions
