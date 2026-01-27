import { deepClone } from '@/utils/util'
import Vue from 'vue'
import {
    SET_INSTANCE_LIST,
    SET_RELEASE_BASE_ID,
    SET_RELEASE_ING,
    SET_TASK_DETAIL,
    SET_TEMPLATE_DETAIL,
    SHOW_TASK_DETAIL,
    UPDATE_INSTANCE_LIST,
    UPDATE_INSTANCE_PAGE_LOADING,
    UPDATE_TEMPLATE_REF,
    UPDATE_TEMPLATE_REF_TYPE,
    UPDATE_USE_TEMPLATE_SETTING,
    TRIGGER_MERGE_INSTANCES,
    SET_FETCH_PIPELINES_ERROR
} from './constants'
export default {
    [SET_INSTANCE_LIST]: (state, { list, init = true }) => {
        Vue.set(state, 'instanceList', list)
        if (init) {
            Vue.set(state, 'initialInstanceList', deepClone(list))
        }
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
    },
    [TRIGGER_MERGE_INSTANCES]: (state, value) => {
        Vue.set(state, 'shouldMergeInstances', value)
    },
    [SET_FETCH_PIPELINES_ERROR]: (state, value) => {
        Vue.set(state, 'fetchPipelinesError', value)
    }
}
