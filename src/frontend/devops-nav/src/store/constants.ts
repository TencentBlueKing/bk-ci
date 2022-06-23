export const SET_USER_INFO: string = 'SET_USER_INFO'
export const SET_PROJECT_LIST: string = 'SET_PROJECT_LIST'
export const FETCH_ERROR: string = 'FETCH_ERROR'
export const SET_SERVICES: string = 'SET_SERVICES'
export const SET_LINKS: string = 'SET_LINKS'
export const SET_DEMO_PROJECT: string = 'SET_DEMO_PROJECT'
export const SET_DEMO_PIPELINE_ID: string = 'SET_DEMO_PIPELINE_ID'
export const UPDATE_NEW_PROJECT: string = 'UPDATE_NEW_PROJECT'
export const TOGGLE_PROJECT_DIALOG: string = 'TOGGLE_PROJECT_DIALOG'
export const UPDATE_PROJECT_MUTATION: string = 'UPDATE_PROJECT_MUTATION'
export const RESET_NEW_PROJECT: string = 'RESET_NEW_PROJECT'
export const SET_POPUP_SHOW: string = 'SET_POPUP_SHOW'
export const UPDATE_HEADER_CONFIG: string = 'UPDATE_HEADER_CONFIG'
export const CLOSE_PREVIEW_TIPS: string = 'CLOSE_PREVIEW_TIPS'
export const TOGGLE_MODULE_LOADING: string = 'TOGGLE_MODULE_LOADING'
export const UPDATE_CURRENT_PAGE: string = 'UPDATE_CURRENT_PAGE'
export const TOGGLE_PERMISSION_DIALOG: string = 'TOGGLE_PERMISSION_DIALOG'
export const TOGGLE_NOTICE_DIALOG: string = 'TOGGLE_NOTICE_DIALOG'
export const SET_CURRENT_NOTICE: string = 'SET_CURRENT_NOTICE'
export const SET_SERVICE_HOOKS: string = 'SET_SERVICE_HOOKS'

// 服务列表
const serviceList = [
    'project',
    'backend',
    'support',
    'process',
    'plugin',
    'artifactory',
    'dispatch',
    'environment',
    'log',
    'measure',
    'notify',
    'repository',
    'ticket',
    'store',
    'auth'
]
export const [
    PROJECT_API_URL_PREFIX,
    BACKEND_API_URL_PREFIX,
    SUPPORT_API_URL_PREFIX,
    PROCESS_API_URL_PREFIX,
    PLUGIN_API_URL_PREFIX,
    ARTIFACTORY_API_URL_PREFIX,
    DISPATCH_API_URL_PREFIX,
    ENVIRONMENT_API_URL_PREFIX,
    LOG_API_URL_PREFIX,
    MEASURE_API_URL_PREFIX,
    NOTIFY_API_URL_PREFIX,
    REPOSITORY_API_URL_PREFIX,
    TICKET_API_URL_PREFIX,
    STORE_API_URL_PREFIX,
    AUTH_API_URL_PREFIX
] = serviceList.map(s => `${s}/api`)

export const EMPTY_PROJECT: Project = {
    projectName: '',
    englishName: '',
    projectType: '',
    description: '',
    bgId: '',
    bgName: '',
    deptId: '',
    deptName: '',
    centerId: '',
    centerName: '',
    isSecrecy: false,
    deployType: [],
    kind: '0'
}
