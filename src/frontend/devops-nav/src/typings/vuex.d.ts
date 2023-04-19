
/// <reference path='./index.d.ts' />
interface RootState {
    services: object[]
    currentPage: object | null,
    user: object | null
    projectList: object[] | null
    related: Link[] | null
    news: Link[] | null
    demo: Demo | null
    newProject: Project | null
    showProjectDialog: boolean
    isAnyPopupShow: boolean
    isShowPreviewTips: boolean
    fetchError: object
    headerConfig: object,
    showNotice: boolean,
    currentNotice: object,
    moduleLoading: boolean,
    isPermissionDialogShow: boolean
    hookMap: any
}

interface Link {
    'name': string
    'link': string
    'create_time': string
}

interface Demo {
    projectId: string
    projectName: string
    pipelineId: string
}

interface Project {
    projectCode?: string
    projectId?: string
    projectName: string
    englishName: string
    description: string
    projectType: string
    useBk?: boolean
    ccAppId?: number
    deployType?: object[]
    kind?: string
    bgId?: string
    bgName?: string
    deptId?: string
    deptName?: string
    centerId?: string
    centerName?: string
    isSecrecy?: boolean
    ccAppName?: string
    approvalStatus?: number
    enabled?: boolean
    logoAddr?: string
    gray?: boolean
    routerTag?: string
}
