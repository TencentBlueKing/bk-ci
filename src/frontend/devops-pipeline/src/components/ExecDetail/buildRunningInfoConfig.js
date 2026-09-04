/**
 * 构建运行态详情 Popover 配置
 * 数据流：buildRunningInfo.runningCategory → BUILD_RUNNING_INFO_CONFIG[category]
 */

/**
 * 运行态大类，与 API buildRunningInfo.runningCategory 一致
 */
export const BUILD_RUNNING_CATEGORY = {
    QUEUE: 'QUEUE',
    RUNNING: 'RUNNING'
}

export const BUILD_RUNNING_TYPE = {
    QUEUE_WAITING: 'QUEUE_WAITING',
    QUEUE_CONCURRENCY: 'QUEUE_CONCURRENCY',
    RUNNING_NORMAL: 'RUNNING_NORMAL',
    RUNNING_JOB_WAITING: 'RUNNING_JOB_WAITING'
}

/**
 * 待人工处理项类型
 */
export const PENDING_ITEM_TYPE = {
    TASK_PAUSE: 'TASK_PAUSE',
    TASK_REVIEW: 'TASK_REVIEW',
    TASK_QUALITY_GATE: 'TASK_QUALITY_GATE',
    STAGE_REVIEW: 'STAGE_REVIEW',
    STAGE_QUALITY_GATE: 'STAGE_QUALITY_GATE'
}

export const PENDING_ITEM_ACTION = {
    PROCESS: 'process',
    VIEW: 'view'
}

const QUEUE_THEME = {
    summaryBg: '#F0F5FF',
    accent: '#3A84FF',
    dotOccupying: '#2DCB56',
    dotAhead: '#FF9C01'
}

const RUNNING_THEME = {
    summaryBg: '#F0F5FF',
    accent: '#3A84FF',
    dotWaiting: '#FF9C01'
}

/**
 * category 级配置
 */
export const BUILD_RUNNING_INFO_CONFIG = {
    [BUILD_RUNNING_CATEGORY.QUEUE]: {
        theme: QUEUE_THEME,
        locateFailedKey: 'details.runningInfoLocateFailed'
    },
    [BUILD_RUNNING_CATEGORY.RUNNING]: {
        theme: RUNNING_THEME,
        locateFailedKey: 'details.runningInfoLocateFailed'
    }
}

/**
 * 待人工处理项：图标与操作
 */
export const PENDING_ITEM_CONFIG = {
    [PENDING_ITEM_TYPE.TASK_PAUSE]: {
        iconClass: 'icon-pause',
        iconBg: '#FFE8C3',
        iconColor: '#FF9C01',
        action: PENDING_ITEM_ACTION.PROCESS,
        actionLabelKey: 'details.goProcess'
    },
    [PENDING_ITEM_TYPE.TASK_REVIEW]: {
        iconClass: 'icon-edit',
        iconBg: '#E1ECFF',
        iconColor: '#3A84FF',
        action: PENDING_ITEM_ACTION.PROCESS,
        actionLabelKey: 'details.goProcess'
    },
    [PENDING_ITEM_TYPE.TASK_QUALITY_GATE]: {
        iconClass: 'icon-stop-shape',
        iconBg: '#FFDDDD',
        iconColor: '#EA3636',
        action: PENDING_ITEM_ACTION.VIEW,
        actionLabelKey: 'details.view'
    },
    [PENDING_ITEM_TYPE.STAGE_REVIEW]: {
        iconClass: 'icon-edit',
        iconBg: '#E1ECFF',
        iconColor: '#3A84FF',
        action: PENDING_ITEM_ACTION.PROCESS,
        actionLabelKey: 'details.goProcess'
    },
    [PENDING_ITEM_TYPE.STAGE_QUALITY_GATE]: {
        iconClass: 'icon-stop-shape',
        iconBg: '#FFDDDD',
        iconColor: '#EA3636',
        action: PENDING_ITEM_ACTION.VIEW,
        actionLabelKey: 'details.view'
    }
}

/**
 * 按 runningCategory 取配置
 */
export function getBuildRunningInfoConfig (category) {
    return BUILD_RUNNING_INFO_CONFIG[category] || null
}

/**
 * 待人工处理项配置，未知类型走默认
 */
export function getPendingItemConfig (itemType) {
    return PENDING_ITEM_CONFIG[itemType] || {
        iconClass: 'icon-info-circle',
        iconBg: '#F0F1F5',
        iconColor: '#979BA5',
        action: PENDING_ITEM_ACTION.PROCESS,
        actionLabelKey: 'details.goProcess'
    }
}

/**
 * 结合 currentTimestamp 计算实时时长（毫秒）
 */
export function calcLiveDurationMs (snapshotMs, currentTimestamp, now = Date.now()) {
    const base = Number(snapshotMs)
    const ts = Number(currentTimestamp)
    if (!Number.isFinite(base) || base < 0) return 0
    if (!Number.isFinite(ts) || ts <= 0) return base
    return base + Math.max(0, now - ts)
}

/**
 * 关联构建详情页 URL，跳转须用 brief.projectId
 */
export function resolveRelatedBuildUrl (brief = {}) {
    const { projectId, pipelineId, buildId } = brief
    if (!projectId || !pipelineId || !buildId) return ''
    return `${WEB_URL_PREFIX}/pipeline/${projectId}/${pipelineId}/detail/${buildId}`
}
