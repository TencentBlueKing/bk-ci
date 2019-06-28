import Vue from 'vue'
import Vuex from 'vuex'
import getters from './getters'
import actions from './actions'
import mutations from './mutations'
import { transformObj } from '../utils/util'
import {
    EMPTY_PROJECT
} from './constants'

const allServices: ObjectMap[] = window.allServices
const projectList: ObjectMap[] = window.projectList
const userInfo: User = transformObj(window.userInfo)
const modules:ObjectMap = {}

for (const key in window.Pages) {
    modules[key] = window.Pages[key].store
}
Vue.use(Vuex)
export default new Vuex.Store<RootState>({
    modules,
    mutations,
    actions,
    getters,
    state: {
        projectList,
        fetchError: null,
        moduleLoading: false,
        user: userInfo,
        services: allServices,
        related: null,
        news: null,
        demo: null,
        showProjectDialog: false,
        isAnyPopupShow: false,
        isShowPreviewTips: true,
        newProject: {
            ...EMPTY_PROJECT
        },
        headerConfig: {
            showProjectList: false,
            showNav: true
        }
    }
})
