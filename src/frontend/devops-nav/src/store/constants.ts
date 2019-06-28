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
export const SET_POPUP_SHOW = 'SET_POPUP_SHOW'
export const UPDATE_HEADER_CONFIG = 'UPDATE_HEADER_CONFIG'
export const CLOSE_PREVIEW_TIPS = 'CLOSE_PREVIEW_TIPS'
export const TOGGLE_MODULE_LOADING = 'TOGGLE_MODULE_LOADING'

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
    'ticket'
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
    TICKET_API_URL_PREFIX
] = serviceList.map(s => `${s}/api`)

export const EMPTY_PROJECT: Project = {
    project_name: '',
    english_name: '',
    project_type: '',
    description: '',
    bg_id: '',
    bg_name: '',
    dept_id: '',
    dept_name: '',
    center_id: '',
    center_name: '',
    is_secrecy: false,
    deploy_type: [],
    kind: '0'
}
