import {
    FETCH_ERROR,
    PROCESS_API_URL_PREFIX
} from '@/store/constants'
import request from '@/utils/request'
const SET_PIPELINE = 'SET_PIPELINE'
const UPDATE_CONTAINER = 'UPDATE_CONTAINER'
const SET_PIPELINE_YAML = 'SET_PIPELINE_YAML'
const SET_PIPELINE_SETTING = 'SET_PIPELINE_SETTING'
const SET_TRIGGER_STAGE = 'SET_TRIGGER_STAGE'
const SET_PIPELINE_EDITING = 'SET_PIPELINE_EDITING'
const SET_EDIT_FROM = 'SET_EDIT_FROM'
function rootCommit (commit, ACTION_CONST, payload) {
    commit(ACTION_CONST, payload, { root: true })
}

/**
 * 对比是否更新
 * @param {Object} param 更新的参数
 * @param {Object} originElement 目标对象
 */
export function compareParam (param, originElement) {
    return Object.keys(param).some(key => {
        return param[key] !== originElement[key] && key !== 'isError'
    })
}

/**
 *  更新动作模板 【同时触发流水线更新操作】
 * @param {STRING} mutation 更新动作常量
 */
export function PipelineEditActionCreator (mutation) {
    return ({ state, commit }, payload = {}) => {
        if (!state.pipeline) {
            commit(mutation, payload)
            return
        }

        if (payload.container && payload.newParam) {
            if (compareParam(payload.newParam, payload.container)) {
                commit(SET_PIPELINE_EDITING, true)
            }
        } else if (payload.element && payload.newParam) {
            if (compareParam(payload.newParam, payload.element)) {
                commit(SET_PIPELINE_EDITING, true)
            }
        } else if (payload.atom && payload.newParam) {
            if (compareParam(payload.newParam, payload.atom)) {
                commit(SET_PIPELINE_EDITING, true)
            }
        } else if (payload.stage && payload.newParam) {
            if (compareParam(payload.newParam, payload.stage)) {
                commit(SET_PIPELINE_EDITING, true)
            }
        } else if ([UPDATE_ATOM_OUTPUT_NAMESPACE, ADD_STAGE].includes(mutation)) {
            commit(SET_PIPELINE_EDITING, true)
        }
        commit(mutation, payload)
    }
}

const state = {
    pipeline: null,
    pipelineYaml: '',
    pipelineSetting: null,
    triggerStage: null,
    isEditing: false,
    editfromImport: false
}

const getters = {

}

const mutations = {
    [SET_PIPELINE_EDITING]: (state, editing) => {
        state.isEditing = editing
    },
    [SET_EDIT_FROM]: (state, editfromImport = false) => {
        state.editfromImport = editfromImport
    }
}

const actions = {
    requestPipeline: async ({ commit, dispatch, getters }, { projectId, pipelineId, version }) => {
        try {
            const [pipelineRes, atomPropRes] = await Promise.all([
                request.get(`${PROCESS_API_URL_PREFIX}/user/version/projects/${projectId}/pipelines/${pipelineId}/versions/${version ?? ''}`),
                request.get(`/${PROCESS_API_URL_PREFIX}/user/pipeline/projects/${projectId}/pipelines/${pipelineId}/atom/prop/list`)
            ])
            const { setting, model } = pipelineRes.data.modelAndSetting
            const atomProp = atomPropRes.data
            const elements = getters.getAllElements(model.stages)
            elements.forEach(element => { // 将os属性设置到model内
                Object.assign(element, {
                    ...atomProp[element.atomCode]
                })
            })
            commit(SET_PIPELINE, {
                ...model,
                version,
                versionName: pipelineRes.data.versionName,
                canDebug: pipelineRes?.data?.canDebug
            })
            commit(SET_PIPELINE_SETTING, setting)
            commit(SET_PIPELINE_YAML, pipelineRes.data.yaml)
        } catch (e) {
            if (e.code === 403) {
                e.message = ''
            }
            rootCommit(commit, FETCH_ERROR, e)
        }
    },
    updateContainer: PipelineEditActionCreator(UPDATE_CONTAINER),
    setPipelineEditing ({ commit }, editing) {
        commit(SET_PIPELINE_EDITING, editing)
    }
}
export default {
    namespaced: true,
    state,
    getters,
    mutations,
    actions
}
