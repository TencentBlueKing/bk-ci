import { getCurrentInstance } from 'vue'
import { Store } from 'vuex'
import { transformObj } from '../utils/util'
import actions from './actions'
import {
    EMPTY_PROJECT
} from './constants'
import getters from './getters'
import platFormConfig from './modules/platform-config'
import mutations from './mutations'

const allServices: ObjectMap[] = window.allServices
const projectList: ObjectMap[] = window.projectList
const userInfo: User = transformObj(window.userInfo)
const modules:ObjectMap = {}

for (const key in window.Pages) {
    modules[key] = window.Pages[key].store
}

export function useStore (): Store<RootState> {
    const vm = getCurrentInstance()
    if (!vm) {
        throw new Error('useStore must be called within a setup function')
    }
    return vm.proxy.$store
}

export default new Store<RootState>({
    modules: {
        ...modules,
        platFormConfig
    },
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
        currentNotice: {},
        isShowNonDisclosureAgreement: false,
        nonDisclosureAgreementConfig: null,
        cancelDisclosureHandler: null
    }
})
