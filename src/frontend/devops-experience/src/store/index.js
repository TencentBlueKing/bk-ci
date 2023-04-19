import actions from './actions'
import mutations from './mutations'

const store = {
    namespaced: true,
    state: {
        isShowExpired: false,
        userGroup: [],
        selectFileInfo: {
            file: ''
        },
        curReleaseDetail: null
    },
    getters: {
        getSelectedFile: state => state.selectFileInfo,
        getIsShowExpired: state => state.isShowExpired,
        getUserGroup: state => state.userGroup
    },
    mutations,
    actions
}

export default store
