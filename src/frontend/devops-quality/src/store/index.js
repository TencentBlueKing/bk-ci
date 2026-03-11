import actions from './actions'
import mutations from './mutations'

const store = {
    namespaced: true,
    state: {
        userGroup: []
    },
    getters: {
        getUserGroup: state => state.userGroup
    },
    mutations,
    actions
}

export default store
