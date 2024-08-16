import Vue from 'vue'
import {
    UPDATE_USER_GROUP
} from './constants'

const prefix = 'quality/api'
const processPrefix = 'process/api'
const vue = new Vue()

const actions = {
    /**
     * 总览
     */
    requestOverview ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/counts/v2/${projectId}/overview`)
    },

    /**
     * 总览拦截流水线
     */
    requestPipelineIntercept ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/counts/v2/${projectId}/pipelineIntercept`)
    },

    /**
     * 拦截趋势
     */
    requestDailyIntercept ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/counts/v2/${projectId}/dailyIntercept`)
    },

    /**
     * 总览拦截规则
     */
    requestruleIntercept ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/counts/v2/${projectId}/ruleIntercept`)
    },

    /**
     * 创建规则权限
     */
    requestPermission ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/rules/v2/${projectId}/hasCreatePermission`)
    },

    /**
     * 拦截规则列表
     */
    requestRuleList ({ commit }, { projectId, page, pageSize }) {
        return vue.$ajax.get(`${prefix}/user/rules/v2/${projectId}/list?page=${page}&pageSize=${pageSize}`)
    },

    /**
     * 拦截历史列表
     */
    requestRecordList ({ commit }, { projectId, ruleHashId, page, pageSize }) {
        return vue.$ajax.get(`${prefix}/user/rules/v2/${projectId}/${ruleHashId}/interceptHistory?page=${page}&pageSize=${pageSize}`)
    },

    /**
     * 拦截规则详情
     */
    requestRuleDetail ({ commit }, { projectId, ruleHashId }) {
        return vue.$ajax.get(`${prefix}/user/rules/v2/${projectId}/${ruleHashId}`)
    },

    /**
     * 获取控制点插件列表
     */
    requestAtomList ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/controlPoints`)
    },

    /**
     * 获取拦截点指标
     */
    getControlPoints ({ commit }, { controlPointHashId }) {
        return vue.$ajax.get(`${prefix}/user/controlPoints/${controlPointHashId}`)
    },

    /**
     * 创建规则
     */
    createRule ({ commit }, { projectId, params }) {
        return vue.$ajax.post(`${prefix}/user/rules/v2/${projectId}`, params)
    },

    /**
     * 编辑规则
     */
    editRule ({ commit }, { projectId, ruleHashId, params }) {
        return vue.$ajax.put(`${prefix}/user/rules/v2/${projectId}/${ruleHashId}`, params)
    },

    /**
     * 启用/停用规则
     */
    toSwitchRule ({ commit }, { projectId, ruleHashId, isEnable }) {
        const changeStatus = isEnable ? 'disable' : 'enable'
        return vue.$ajax.put(`${prefix}/user/rules/v2/${projectId}/${ruleHashId}/${changeStatus}`)
    },

    /**
     * 删除规则
     */
    deleteRule ({ commit }, { projectId, ruleHashId }) {
        return vue.$ajax.delete(`${prefix}/user/rules/v2/${projectId}/${ruleHashId}`)
    },

    /**
     * 流水线列表
     */
    requestPipelineList ({ commit }, { projectId }) {
        return vue.$ajax.post(`${processPrefix}/user/pipeline/quality/${projectId}`)
    },

    /**
     * 生效流水线列表
     */
    requestEffectPipeline ({ commit }, { projectId, params }) {
        return vue.$ajax.post(`${processPrefix}/user/pipeline/quality/${projectId}`, params)
    },

    /**
     * 拦截记录列表
     */
    requestInterceptList ({ commit }, { projectId, page, pageSize, params }) {
        return vue.$ajax.get(`${prefix}/user/intercepts/v2/${projectId}?page=${page}&pageSize=${pageSize}`, { params })
    },

    /**
     * 通知组列表
     */
    requestGroupList ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/groups/${projectId}/list`)
    },
    
    /**
     * 用户组组列表
     */
    requestUserGroup ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/groups/${projectId}/projectGroupAndUsers`)
    },

    /**
     * 获取用户组详情
     */
    toGetGroupDetail ({ commit }, { projectId, groupHashId }) {
        return vue.$ajax.get(`${prefix}/user/groups/${projectId}/${groupHashId}`)
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
     * 修改用户组
     */
    editUserGroups ({ commit }, { projectId, groupHashId, params }) {
        return vue.$ajax.put(`${prefix}/user/groups/${projectId}/${groupHashId}`, params)
    },
    
    /**
     * 更新用户组
     */
    updateselectUserGroup ({ commit }, { userList }) {
        commit(UPDATE_USER_GROUP, {
            userList: userList
        })
    },

    /**
     * 获取所有指标集
     */
    requestIndicatorSet ({ commit }) {
        return vue.$ajax.get(`${prefix}/user/indicators/v2/listIndicatorSet`)
    },

    /**
     * 获取对应控制点所有指标
     */
    requestIndicators ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/indicators/v2/project/${projectId}/listIndicators`)
    },

    /**
     * 获取控制点列表
     */
    requestControlPoint ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/controlPoints/v2/list?projectId=${projectId}`)
    },

    /**
     * 获取流水线视图列表
     */
    requestPipelineViews ({ commit }, { projectId }) {
        return vue.$ajax.get(`${processPrefix}/user/quality/pipelineViews/${projectId}`)
    },

    /**
     * 获取流水线视图列表
     */
    requestPipelineTemplate ({ commit }, { projectId, params }) {
        return vue.$ajax.get(`${processPrefix}/user/template/pipelines/projects/${projectId}/listQualityViewTemplates`, { params })
    },

    /**
     * 获取视图流水线编排列表
     */
    requestViewPipelines ({ commit }, { projectId, params }) {
        return vue.$ajax.get(`${processPrefix}/user/pipeline/quality/projects/${projectId}/listQualityViewPipelines`, { params })
    },

    /**
     * 获取规则模板
     */
    requestRuleTemplate ({ commit }, { projectId }) {
        return vue.$ajax.get(`${prefix}/user/rules/v2/project/${projectId}/listTemplates`)
    },

    /**
     * 查询生效范围数据
     */
    requestRangePipeline ({ commit }, { params }) {
        return vue.$ajax.post(`${prefix}/user/rules/v2/listPipelineRangeDetail`, params)
    },
    
    /**
     * 查询模板生效范围数据
     */
    requestRangeTemplate ({ commit }, { params }) {
        return vue.$ajax.post(`${prefix}/user/rules/v2/listTemplateRangeDetail`, params)
    },

    /**
     * 获取指标列表
     */
    requestIndicatorList ({ commit }, { projectId, keyword }) {
        return vue.$ajax.get(`${prefix}/user/indicators/v2/project/${projectId}/queryIndicatorList?keyword=${keyword}`)
    },

    /**
     * 创建指标
     */
    createIndicator ({ commit }, { projectId, params }) {
        return vue.$ajax.post(`${prefix}/user/indicators/v2/project/${projectId}/create`, params)
    },

    /**
     * 编辑指标
     */
    editIndicator ({ commit }, { projectId, indicatorId, params }) {
        return vue.$ajax.put(`${prefix}/user/indicators/v2/project/${projectId}/indicator/${indicatorId}/update`, params)
    },

    /**
     * 指标详情
     */
    requestIndicatorDetail ({ commit }, { projectId, indicatorId }) {
        return vue.$ajax.get(`${prefix}/user/indicators/v2/project/${projectId}/indicator/${indicatorId}/get`)
    },

    /**
     * 获取流水线详情
     */
    getPipelineDetail ({ commit }, { projectId, pipelineId }) {
        return vue.$ajax.get(`${processPrefix}/user/pipeline/quality/project/${projectId}/pipeline/${pipelineId}/getPipelineInfo`)
    },

    /**
     * 获取模板详情
     */
    getTemplateDetail ({ commit }, { projectId, templateId }) {
        return vue.$ajax.get(`${processPrefix}/user/template/pipelines/project/${projectId}/template/${templateId}/getTemplateInfo`)
    },

    /**
     * 获取流水线控制点
     */
    getControlPoint ({ commit }, { element, projectId }) {
        return vue.$ajax.get(`${prefix}/user/controlPoints/v2/project/${projectId}/elementType/${element}/get`)
    },

    /**
     * 删除指标
     */
    deleteIndicator ({ commit }, { projectId, metaId }) {
        return vue.$ajax.delete(`${prefix}/user/indicators/v2/project/${projectId}/indicator/${metaId}/delete`)
    }
}

export default actions
