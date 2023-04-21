/**
 * @file main store
 * @author Blueking
 */

import Vue from 'vue'
import Vuex from 'vuex'
import {
    modifyHtmlTitle
} from '@/utils'

Vue.use(Vuex)

const store = new Vuex.Store({
    state: {
        user: {},
        appHeight: window.innerHeight,
        permission: false,
        projectId: '',
        projectInfo: {},
        projectSetting: {},
        curPipeline: {},
        menuPipelineId: '',
        modelDetail: {},
        exceptionInfo: {
            type: 200
        },
        showStageReviewPanel: {
            isShow: false,
            stage: {},
            type: ''
        },
        messageNum: 0,
        showLoginDialog: false
    },
    getters: {},
    mutations: {
        updateUser (state, user) {
            state.user = Object.assign({}, user)
        },
        setProjectInfo (state, projectInfo) {
            state.projectId = projectInfo.projectCode
            state.projectInfo = projectInfo
        },
        setProjectSetting (state, projectSetting) {
            state.projectSetting = projectSetting || {}
        },
        setCurPipeline (state, pipeline) {
            modifyHtmlTitle(pipeline?.displayName)
            state.curPipeline = pipeline
        },
        setMenuPipelineId (state, pipelineId) {
            state.menuPipelineId = pipelineId
        },
        setModelDetail (state, detail) {
            state.modelDetail = detail
        },
        setExceptionInfo (state, exceptionInfo) {
            state.exceptionInfo = exceptionInfo
        },
        setPermission (state, permission) {
            state.permission = permission
        },
        toggleStageReviewPanel (state, showStageReviewPanel) {
            Object.assign(state.showStageReviewPanel, showStageReviewPanel)
        },
        updateMessageNum (state, num) {
            state.messageNum = num
        },
        updateShowLoginDialog (state, showLoginDialog) {
            state.showLoginDialog = showLoginDialog
        }
    },
    actions: {
        setProjectInfo ({ commit }, projectInfo) {
            commit('setProjectInfo', projectInfo)
        },
        setProjectSetting ({ commit }, projectSetting) {
            commit('setProjectSetting', projectSetting)
        },
        setCurPipeline ({ commit }, pipeline) {
            commit('setCurPipeline', pipeline)
        },
        setMenuPipelineId ({ commit }, pipelineId) {
            commit('setMenuPipelineId', pipelineId)
        },
        setModelDetail ({ commit }, detail) {
            commit('setModelDetail', detail)
        },
        setExceptionInfo ({ commit }, exceptionInfo) {
            commit('setExceptionInfo', exceptionInfo)
        },
        setPermission ({ commit }, permission) {
            commit('setPermission', permission)
        },
        setUser ({ commit }, user) {
            commit('updateUser', user)
        },
        toggleStageReviewPanel ({ commit }, showStageReviewPanel) {
            commit('toggleStageReviewPanel', showStageReviewPanel)
        },
        setMessageNum ({ commit }, num) {
            commit('updateMessageNum', num)
        },
        setShowLoginDialog ({ commit }, isShow) {
            commit('updateShowLoginDialog', isShow)
        }
    }
})

window.addEventListener('resize', () => {
    store.state.appHeight = window.innerHeight
})

export default store
