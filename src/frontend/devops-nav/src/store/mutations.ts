/// <reference path='../typings/vuex.d.ts' />
import Vue from 'vue'
import { MutationTree } from 'vuex'
import {
    CLOSE_PREVIEW_TIPS,
    FETCH_ERROR,
    RESET_NEW_PROJECT,
    SET_CURRENT_NOTICE,
    SET_DEMO_PIPELINE_ID,
    SET_DEMO_PROJECT,
    SET_DISCLOSURE_AGREEMENT_CANCEL_HANDLER,
    SET_DISCLOSURE_AGREEMENT_CONFIG,
    SET_LINKS,
    SET_POPUP_SHOW,
    SET_PROJECT_LIST,
    SET_SERVICE_HOOKS,
    SET_SERVICES,
    SET_USER_INFO,
    TOGGLE_MODULE_LOADING,
    TOGGLE_NOTICE_DIALOG,
    TOGGLE_PERMISSION_DIALOG,
    TOGGLE_PROJECT_DIALOG,
    TOGGLE_SIGNATURE_DIALOG,
    UPDATE_CURRENT_PAGE,
    UPDATE_HEADER_CONFIG,
    UPDATE_NEW_PROJECT
} from './constants'

const mutations: MutationTree<RootState> = {
    [SET_SERVICE_HOOKS]: (state: RootState, { serviceId, extHooks }: any) => {
        Vue.set(state, 'hookMap', {
            ...state.hookMap,
            [serviceId]: extHooks
        })
    },
    [TOGGLE_PERMISSION_DIALOG]: (state: RootState, visible: boolean) => {
        Vue.set(state, 'isPermissionDialogShow', visible)
    },
    [UPDATE_CURRENT_PAGE]: (state: RootState, page: object) => {
        Vue.set(state, 'currentPage', page)
        return state
    },
    [UPDATE_HEADER_CONFIG]: (state: RootState, headerConfig: object) => {
        Vue.set(state, 'headerConfig', {
            ...state.headerConfig,
            ...headerConfig
        })
        return state
    },
    [SET_USER_INFO]: (state: RootState, { user }: any) => {
        Vue.set(state, 'user', user)
        return state
    },
    [SET_PROJECT_LIST]: (state: RootState, { projectList }: any) => {
        Vue.set(state, 'projectList', projectList)
        return state
    },
    [SET_SERVICES]: (state: RootState, { services }: any) => {
        Vue.set(state, 'services', services)
        return state
    },
    [SET_LINKS]: (state: RootState, { links, type }: any) => {
        Vue.set(state, type, links)
        return state
    },
    [FETCH_ERROR]: (state: RootState, error: object) => {
        console.warn(error)
        Vue.set(state, 'fetchError', error)
    },
    [SET_DEMO_PROJECT]: (state: RootState, { project }: any) => {
        Vue.set(state, 'demo', {
            projectId: project.projectCode,
            projectName: project.projectName
        })
        return state
    },
    [SET_DEMO_PIPELINE_ID]: (state: RootState, { pipelineId }: any) => {
        Vue.set(state, 'demo', {
            ...state.demo,
            pipelineId
        })
        return state
    },
    [UPDATE_NEW_PROJECT]: (state: RootState, payload: any) => {
        Vue.set(state, 'newProject', {
            ...state.newProject,
            ...payload
        })
        return state
    },
    [RESET_NEW_PROJECT]: (state: RootState, project: Project) => {
        Vue.set(state, 'newProject', {
            ...project
        })
        return state
    },
    [TOGGLE_PROJECT_DIALOG]: (state: RootState, payload: any) => {
        Vue.set(state, 'showProjectDialog', payload.showProjectDialog)
    },
    [SET_POPUP_SHOW]: (state: RootState, isAnyPopupShow: boolean) => {
        Vue.set(state, 'isAnyPopupShow', isAnyPopupShow)
    },
    [CLOSE_PREVIEW_TIPS]: (state: RootState) => {
        Vue.set(state, 'isShowPreviewTips', false)
    },
    [TOGGLE_MODULE_LOADING]: (state: RootState, moduleLoading: boolean) => {
        Vue.set(state, 'moduleLoading', moduleLoading)
    },
    [TOGGLE_NOTICE_DIALOG]: (state: RootState, isShow: boolean) => {
        Vue.set(state, 'showNotice', isShow)
    },
    [SET_CURRENT_NOTICE]: (state: RootState, notice: object) => {
        Vue.set(state, 'currentNotice', notice)
    },
    [TOGGLE_SIGNATURE_DIALOG]: (state: RootState, isShow: boolean) => {
        Vue.set(state, 'isShowNonDisclosureAgreement', isShow)
    },
    [SET_DISCLOSURE_AGREEMENT_CONFIG]: (state: RootState, config: NonDisclosureAgreementConfig) => {
        Vue.set(state, 'nonDisclosureAgreementConfig', config)
    },
    [SET_DISCLOSURE_AGREEMENT_CANCEL_HANDLER]: (state: RootState, handler) => {
        Vue.set(state, 'cancelDisclosureHandler', handler)
    }
}

export default mutations
