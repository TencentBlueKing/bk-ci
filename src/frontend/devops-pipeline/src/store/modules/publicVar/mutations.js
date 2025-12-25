import Vue from 'vue'
import {
    UPDATE_PUBLIC_VAR_YAML,
    UPDATE_GROUP_DATA,
    UPDATE_PUBLIC_VAR_MODE,
    UPDATE_OPERATE_TYPE
} from './constants'
export default {
    [UPDATE_GROUP_DATA]: (state, data) => {
        Vue.set(state, 'groupData', data)
    },
    [UPDATE_PUBLIC_VAR_MODE]: (state, mode) => {
        Vue.set(state, 'publicVarMode', mode)
    },
    [UPDATE_PUBLIC_VAR_YAML]: (state, yaml) => {
        Vue.set(state, 'publicVarYaml', yaml)
    },
    [UPDATE_OPERATE_TYPE]: (state, yaml) => {
        Vue.set(state, 'operateType', yaml)
    }
}
