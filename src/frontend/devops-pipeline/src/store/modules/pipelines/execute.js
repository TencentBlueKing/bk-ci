const SET_EXECUTE_PARAMS = 'SET_EXECUTE_PARAMS'
const SET_SKIPED_ATOM_IDS = 'SET_SKIPED_ATOM_IDS'
const SET_EXECUTE_STEP = 'SET_EXECUTE_STEP'
const SET_EXECUTE_STATUS = 'SET_EXECUTE_STATUS'
const RESET_EXECUTE_CONFIG = 'RESET_EXECUTE_CONFIG'
const state = {
    executeParams: new Map(),
    skipedAtomIds: new Map(),
    isExecuteParamsValid: true,
    executeStatus: false,
    executeStep: 1
}

const getters = {
    getExecuteParams: state => pipelineId => {
        return state.executeParams.get(pipelineId)
    },
    getSkipedAtomIds: state => pipelineId => {
        return state.skipedAtomIds.get(pipelineId)?.reduce((result, item) => {
            result[item] = true
            return result
        }, {}) ?? {}
    }
}

const mutations = {
    [SET_EXECUTE_PARAMS]: (state, { pipelineId, ...params }) => {
        const oldVal = state.executeParams.get(pipelineId)
        state.executeParams.set(pipelineId, {
            ...oldVal,
            ...params
        })
    },
    [SET_SKIPED_ATOM_IDS]: (state, { pipelineId, skipedAtomIds }) => {
        state.skipedAtomIds.set(pipelineId, skipedAtomIds)
    },
    [SET_EXECUTE_STEP]: (state, step) => {
        state.executeStep = step
    },
    [SET_EXECUTE_STATUS]: (state, status) => {
        return Object.assign(state, {
            executeStatus: status
        })
    },
    [RESET_EXECUTE_CONFIG]: (state, pipelineId) => {
        state.executeParams.delete(pipelineId)
        state.skipedAtomIds.delete(pipelineId)
        return Object.assign(state, {
            executeStatus: false,
            executeStep: 1
        })
    }
}

const actions = {
    setExecuteParams ({ commit }, params) {
        commit(SET_EXECUTE_PARAMS, params)
    },
    setSkipedAtomIds ({ commit }, skipedAtomIds) {
        commit(SET_SKIPED_ATOM_IDS, skipedAtomIds)
    },
    setExecuteStep ({ commit }, step) {
        commit(SET_EXECUTE_STEP, step)
    },
    setExecuteStatus ({ commit }, status) {
        commit(SET_EXECUTE_STATUS, status)
    },
    resetExecuteConfig ({ commit }, pipelineId) {
        commit(RESET_EXECUTE_CONFIG, pipelineId)
    }
}
export default {
    namespaced: true,
    state,
    getters,
    mutations,
    actions
}
