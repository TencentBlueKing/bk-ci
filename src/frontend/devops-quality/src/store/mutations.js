import Vue from 'vue'
import {
    UPDATE_USER_GROUP
} from './constants'

const mutations = {
    [UPDATE_USER_GROUP]: (state, { userList }) => {
        Vue.set(state, 'userGroup', userList)
    }
}

export default mutations
