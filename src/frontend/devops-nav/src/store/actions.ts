import { ActionTree, ActionContext } from 'vuex'
import Request from '../utils/request'
import { transformObj } from '../utils/util'
import {
    SET_USER_INFO,
    SET_PROJECT_LIST,
    FETCH_ERROR,
    SET_LINKS,
    SET_DEMO_PROJECT,
    SET_DEMO_PIPELINE_ID,
    UPDATE_NEW_PROJECT,
    TOGGLE_PROJECT_DIALOG,
    BACKEND_API_URL_PREFIX,
    PROCESS_API_URL_PREFIX,
    PROJECT_API_URL_PREFIX,
    SUPPORT_API_URL_PREFIX,
    EMPTY_PROJECT,
    RESET_NEW_PROJECT,
    SET_POPUP_SHOW,
    UPDATE_HEADER_CONFIG,
    CLOSE_PREVIEW_TIPS,
    TOGGLE_MODULE_LOADING,
    UPDATE_CURRENT_PAGE,
    SET_SERVICES,
    TOGGLE_PERMISSION_DIALOG
} from './constants'

const actions: ActionTree<RootState, any> = {
    togglePermissionDialog ({ commit }: ActionContext<RootState, any>, visible: boolean) {
        this.commit(TOGGLE_PERMISSION_DIALOG, visible)  
    },
    updateCurrentPage ({ commit }: ActionContext<RootState, any>, page: object) {
        commit(UPDATE_CURRENT_PAGE, page)
    },
    toggleModuleLoading ({ commit }: ActionContext<RootState, any>, moduleLoading: boolean) {
        commit(TOGGLE_MODULE_LOADING, moduleLoading)
    },
    upadteHeaderConfig ({ commit }: ActionContext<RootState, any>, headerConfig: object) {
        commit(UPDATE_HEADER_CONFIG, headerConfig)
    },
    setUserInfo ({ commit }: ActionContext<RootState, any>, user: User) {
        commit(SET_USER_INFO, { user: transformObj(user) })
    },
    async toggleServiceCollect (_, { serviceId, isCollected }: any) {
        return Request.put(`${PROJECT_API_URL_PREFIX}/user/services/${serviceId}?collector=${isCollected}`)
    },
    async fetchLinks ({ commit }, { type }) {
        try {
            const links = await Request.get(`${PROJECT_API_URL_PREFIX}/user/activities/types/${type}`)
            commit(SET_LINKS, {
                links,
                type
            })
        } catch (e) {
            commit(FETCH_ERROR, e)
        }
    },
    createDemo (_, payload) {
        return Request.post(`${PROCESS_API_URL_PREFIX}/user/accesses/`, payload)
    },
    setProjectList ({ commit }: ActionContext<RootState, any>, projectList: object) {
        commit(SET_PROJECT_LIST, { projectList })
    },
    setServices ({ commit }: ActionContext<RootState, any>, services: object[]) {
        console.log(services)
        commit(SET_SERVICES, { services })
    },
    async getProjects ({ dispatch }: ActionContext<RootState, any>, includeDisable = false) {
        const res: any = await Request.get(`${PROJECT_API_URL_PREFIX}/user/projects/?includeDisable=${includeDisable}`)
        const projectList: Project[] = res
        dispatch('setProjectList', projectList)
        window.setLsCacheItem('projectList', projectList.filter((project: Project) => project.enabled))
    },
    getDepartmentInfo (_, { type, id }) {
        return Request.get(`${PROJECT_API_URL_PREFIX}/user/organizations/types/${type}/ids/${id}`)
    },
    ajaxUpdatePM (_, { id, data }) {
        return Request.put(`${PROJECT_API_URL_PREFIX}/user/projects/${id}/`, data)
    },
    ajaxAddPM (_, data) {
        return Request.post(`${PROJECT_API_URL_PREFIX}/user/projects/`, data)
    },
    toggleProjectEnable (_, { projectId, enabled }) {
        return Request.put(`${PROJECT_API_URL_PREFIX}/user/projects/${projectId}/enable?enabled=${enabled}`)
    },
    getMyDepartmentInfo () {
        return Request.get(`${PROJECT_API_URL_PREFIX}/user/users/detail/`)
    },
    selectDemoProject ({ commit }, { project }) {
        commit(SET_DEMO_PROJECT, {
            project
        })
    },
    setDemoPipelineId ({ commit }, { pipelineId }) {
        commit(SET_DEMO_PIPELINE_ID, {
            pipelineId
        })
    },
    updateNewProject ({ commit }, payload) {
        commit(UPDATE_NEW_PROJECT, payload)
    },
    resetNewProject ({ commit }, project = EMPTY_PROJECT) {
        commit(RESET_NEW_PROJECT, project)
    },
    toggleProjectDialog ({ commit }, payload) {
        commit(RESET_NEW_PROJECT, payload.project || EMPTY_PROJECT)
        commit(TOGGLE_PROJECT_DIALOG, payload)
        if (payload.project) {
            commit(UPDATE_NEW_PROJECT, payload.project)
        }
    },
    togglePopupShow ({ commit }, payload) {
        commit(SET_POPUP_SHOW, payload)
    },
    getDocList () {
        return Request.get(`${BACKEND_API_URL_PREFIX}/ci/docs/?format=json`)
    },
    changeProjectLogo (_, { projectId, formData }) {
        return Request.put(`${PROJECT_API_URL_PREFIX}/user/projects/${projectId}/logo/`, formData)
    },
    closePreviewTips ({ commit }) {
        commit(CLOSE_PREVIEW_TIPS)
    },
    getAnnouncement () {
        return Request.get(`${SUPPORT_API_URL_PREFIX}/user/notice/valid`)
    }
}

export default actions
