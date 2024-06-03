import { Store } from 'vuex'
import { transformObj } from '../utils/util'
import actions from './actions'
import {
    EMPTY_PROJECT
} from './constants'
import getters from './getters'
import mutations from './mutations'

const allServices: ObjectMap[] = window.allServices
const projectList: ObjectMap[] = window.projectList
const userInfo: User = transformObj(window.userInfo)
const modules:ObjectMap = {}

for (const key in window.Pages) {
    modules[key] = window.Pages[key].store
}
export default new Store<RootState>({
    modules,
    mutations,
    actions,
    getters,
    state: {
        isPermissionDialogShow: false,
        projectList,
        fetchError: null,
        moduleLoading: false,
        user: userInfo,
        services: allServices,
        currentPage: window.currentPage,
        related: [],
        news: [],
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
        },
        showNotice: false,
        currentNotice: {}
    }
})
