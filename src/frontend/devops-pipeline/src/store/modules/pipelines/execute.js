const SET_EXECUTE_PARAMS = 'SET_EXECUTE_PARAMS'
const SET_EXECUTE_STATUS = 'SET_EXECUTE_STATUS'
const RESET_EXECUTE_CONFIG = 'RESET_EXECUTE_CONFIG'
const state = {
    executeParams: new Map(),
    isExecuteParamsValid: true,
    executeStatus: false
}

const getters = {
    getExecuteParams: state => pipelineId => {
        return state.executeParams.get(pipelineId)
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
    [SET_EXECUTE_STATUS]: (state, status) => {
        return Object.assign(state, {
            executeStatus: status
        })
    },
    [RESET_EXECUTE_CONFIG]: (state, pipelineId) => {
        state.executeParams.delete(pipelineId)
        return Object.assign(state, {
            executeStatus: false
        })
    }
}

const actions = {
    setExecuteParams ({ commit }, params) {
        commit(SET_EXECUTE_PARAMS, params)
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
