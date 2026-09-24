/** 支持 Docker 构建的 OS，由平台能力决定 */
export const DOCKER_SUPPORTED_OS = ['LINUX']

/** label / tips 均为 i18n key，渲染处用 $t 取文案 */
export const INSTALL_TYPE_LIST = [
    { id: 'SERVICE', label: 'environment.installSession.installTypeService', tips: 'environment.installSession.installTypeServiceTips' },
    {
        id: 'TASK',
        label: 'environment.installSession.installTypeTask',
        tips: 'environment.installSession.installTypeTaskTips'
    },
]

/** 已接入节点轮询间隔（ms） */
export const POLL_INTERVAL = 8000
/** 第二步超过该时长（ms）仍无节点接入，提示用户确认命令是否执行成功 */
export const WAIT_TOO_LONG = 60000
/** 标签变化触发环境预览请求的防抖（ms） */
export const ENV_PREVIEW_DEBOUNCE = 500

/**
 * 最大构建并发数的约定默认值（表单留空时传给后端的值）。
 * 后端 PR #13584 的 AgentInstallSessionRequest.parallelTaskCount 为必填（0 表示无限制），
 * 该默认值在后端最终确认前以此常量管理，确认后仅调整此处
 */
export const DEFAULT_PARALLEL_TASK_COUNT = 4

/** 后端 AgentInstallSessionRequest.dockerParallelTaskCount 默认值（仅支持 Docker 的操作系统生效，0 表示无限制） */
export const DEFAULT_DOCKER_PARALLEL_TASK_COUNT = 4

/** 安装会话状态（后端 AgentInstallSessionStatus） */
export const SESSION_STATUS = {
    ACTIVE: 'ACTIVE',
    EXPIRED: 'EXPIRED',
    REVOKED: 'REVOKED'
}

/** 会话节点状态 → i18n key（渲染处用 $t 取文案，未知状态回退原始值） */
export const SESSION_NODE_STATUS_I18N = {
    PENDING: 'environment.installSession.statusPending',
    INSTALLING: 'environment.installSession.statusInstalling',
    IMPORTING: 'environment.installSession.statusImporting',
    SUCCEEDED: 'environment.installSession.statusSucceeded',
    FAILED: 'environment.installSession.statusFailed'
}

export const osOf = (node) => {
    const name = (node?.osName || '').toLowerCase()
    if (name.includes('windows')) return 'WINDOWS'
    if (name.includes('mac')) return 'MACOS'
    return 'LINUX'
}

export const formatTime = (d) => {
    const p = (n) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

/**
 * 格式化后端 LocalDateTime 字符串（如 2026-09-14T10:30:00）为 YYYY-MM-DD HH:mm。
 * 兼容已带空格分隔或空值的情况
 */
export const formatDateTime = (value) => {
    if (!value) return ''
    const normalized = String(value).replace('T', ' ')
    const d = new Date(normalized)
    return Number.isNaN(d.getTime()) ? normalized : formatTime(d)
}
