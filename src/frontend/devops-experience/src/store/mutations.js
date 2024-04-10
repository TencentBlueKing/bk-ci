import Vue from 'vue'
import {
    UPDATE_SELECTED_FILE,
    UPDATE_IS_EXPIRED,
    UPDATE_USER_GROUP,
    SET_CURRELEASE_DETAIL
} from './constants'

const mutations = {
    [UPDATE_SELECTED_FILE]: (state, { fileInfo }) => {
        Vue.set(state, 'selectFileInfo', fileInfo)
    },
    [UPDATE_IS_EXPIRED]: (state, { isExpired }) => {
        Vue.set(state, 'isShowExpired', isExpired)
    },
    [UPDATE_USER_GROUP]: (state, { userList }) => {
        Vue.set(state, 'userGroup', userList)
    },
    [SET_CURRELEASE_DETAIL]: (state, curReleaseDetail) => {
        Vue.set(state, 'curReleaseDetail', curReleaseDetail)
    }
}

export default mutations
