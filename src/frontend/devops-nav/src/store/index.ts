import { Store } from 'vuex'
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
export default new Store<RootState>({
    modules,
    mutations,
    actions,
    getters,
    state: {
        isPermissionDialogShow: false,
        hookMap: {},
        projectList,
        fetchError: null,
        moduleLoading: false,
        user: userInfo,
        services: allServices,
        currentPage: window.currentPage,
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
        },
        showNotice: false,
        currentNotice: {}
    }
})
