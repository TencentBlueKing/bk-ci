export const TYPE_ENUM = {
    atom: 'atom',
    template: 'template',
    image: 'image',
    service: 'service',
    DEVX: 'DEVX'
}

export const PUBLISH_STRATEGY = {
    AUTO: 'AUTO',
    MANUAL: 'MANUAL'
}
export const ReleaseTypeEnum = {
    NEW: 'NEW',
    INCOMPATIBILITY_UPGRADE: 'INCOMPATIBILITY_UPGRADE',
    COMPATIBILITY_UPGRADE: 'COMPATIBILITY_UPGRADE',
    COMPATIBILITY_FIX: 'COMPATIBILITY_FIX',
    CANCEL_RE_RELEASE: 'CANCEL_RE_RELEASE',
    HIS_VERSION_UPGRADE: 'HIS_VERSION_UPGRADE',
    BRANCH_TEST: 'BRANCH_TEST'
}
export const UpgradeStatus = {
    INIT: 'INIT',
    UNDERCARRIAGED: 'UNDERCARRIAGED',
    AUDIT_REJECT: 'AUDIT_REJECT',
    RELEASED: 'RELEASED',
    GROUNDING_SUSPENSION: 'GROUNDING_SUSPENSION',
}
export const ProgressStatus = {
    COMMITTING: 'COMMITTING', // 提交中
    BUILDING: 'BUILDING', // 构建中
    BUILD_FAIL: 'BUILD_FAIL', // 构建失败
    TESTING: 'TESTING', // 验证中
    EDITING: 'EDITING', // 填写资料中
    AUDITING: 'AUDITING', // 验证失败
    CODECCING: 'CODECCING', // 审核中
    CODECC_FAIL: 'CODECC_FAIL', // 审核驳回
}

export const CANCEL_RE_RELEASE = 'CANCEL_RE_RELEASE'
export const TargetNetBehavior = {
    UPLOAD: 'UPLOAD',
    DOWNLOAD: 'DOWNLOAD',
}
export const StaffType = {
    RTX: 'rtx',
    MAIL: 'email',
    ALL: 'all',
}

export const BASE_PREFIX = process.env.NODE_ENV === 'development' ? '/ms' : ''
export const STORE_TYPE = 'DEVX'
export const LOGO_UPLOAD_URL = `${BASE_PREFIX}/store/api/user/store/logo/upload?storeType=${STORE_TYPE}`
export const FILE_UPLOAD_URL = `${BASE_PREFIX}/misc/api/user/file/upload`

export const UPGRADE_STATUS = Object.keys(UpgradeStatus)
export const PROGRESS_STATUS = Object.keys(ProgressStatus)
export const LOGIN_URL = `http://login.o.woa.com?app_code=bk_ci&c_url=${encodeURIComponent(location.href)}`

export const OWNER_PERMISSION_LIST = ['开发', '版本发布', '私有配置', '可见范围', '审批', '成员管理']
export const DEVELOPER_PERMISSION_LIST = ['开发', '版本发布', '私有配置']

export const STEP_CODES = {
    COMMIT: 'commit',
    BUILD: 'build',
    TEST: 'test',
    EDIT: 'edit',
    APPROVE: 'approve'
}
