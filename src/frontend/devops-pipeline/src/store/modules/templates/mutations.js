import Vue from 'vue'
import { deepClone } from '@/utils/util'
import {
    SET_INSTANCE_LIST,
    SET_TEMPLATE_DETAIL,
    UPDATE_INSTANCE_LIST,
    UPDATE_USE_TEMPLATE_SETTING,
    SET_RELEASE_ING,
    SET_RELEASE_BASE_ID,
    SHOW_TASK_DETAIL,
    SET_TASK_DETAIL,
    UPDATE_TEMPLATE_REF_TYPE,
    UPDATE_TEMPLATE_REF,
    UPDATE_INSTANCE_PAGE_LOADING
} from './constants'
export default {
    [SET_INSTANCE_LIST]: (state, list) => {
        Vue.set(state, 'instanceList', list)
        Vue.set(state, 'initialInstanceList', deepClone(list))
    },
    [UPDATE_INSTANCE_LIST]: (state, { index, value }) => {
        const newList = state.instanceList.map(i => JSON.parse(JSON.stringify(i)))
        newList[index] = JSON.parse(JSON.stringify(value))
        Vue.set(state, 'instanceList', newList)
    },
    [UPDATE_USE_TEMPLATE_SETTING]: (state, value) => {
        Vue.set(state, 'useTemplateSettings', value)
    },
    [SET_TEMPLATE_DETAIL]: (state, data) => {
        Vue.set(state, 'templateDetail', data.templateDetail)
        Vue.set(state, 'templateVersion', data.templateVersion)
    },
    [SET_RELEASE_ING]: (state, value) => {
        Vue.set(state, 'isInstanceReleasing', value)
    },
    [SET_RELEASE_BASE_ID]: (state, value) => {
        Vue.set(state, 'releaseBaseId', value)
    },
    [SHOW_TASK_DETAIL]: (state, value) => {
        Vue.set(state, 'showTaskDetail', value)
    },
    [SET_TASK_DETAIL]: (state, value) => {
        Vue.set(state, 'instanceTaskDetail', value)
    },
    [UPDATE_TEMPLATE_REF_TYPE]: (state, value) => {
        Vue.set(state, 'templateRefType', value)
    },
    [UPDATE_TEMPLATE_REF]: (state, value) => {
        Vue.set(state, 'templateRef', value)
    },
    [UPDATE_INSTANCE_PAGE_LOADING]: (state, value) => {
        Vue.set(state, 'instancePageLoading', value)
    }
}
