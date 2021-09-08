const turbo = {
    namespaced: true,
    state: {
        paramConfig: []
    },
    mutations: {
        setParamConfig (state, res) {
            state.paramConfig = res
        }
    },
    actions: {
        setParamConfig ({ commit }, res) {
            commit('setParamConfig', res)
        }
    },
    getters: {
        paramConfig: state => state.paramConfig
    }
}

export default turbo
