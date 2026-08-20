/**
 * 构建终态详情 Popover 配置
 *
 * 数据流：buildEndInfo.endCategory → BUILD_END_INFO_CONFIG[category]
 *        buildEndInfo.endType     → endTypeRules[endType]
 *
 * 扩展方式：新增终态时在对应 category 下补充 endTypeRules 即可；
 * 文案差异通过 i18n key、主题色 theme、summaryLayout 等字段驱动。
 */

/** 终态大类，与 API buildEndInfo.endCategory 一致，决定 Popover 整体样式 */
export const BUILD_END_CATEGORY = {
    CANCEL: 'CANCEL',
    FAIL: 'FAIL',
    TIMEOUT: 'TIMEOUT',
    SUCCESS: 'SUCCESS'
}

export const BUILD_END_TYPE = {
    // 取消：用户取消 / 系统取消 / 父流水线取消
    CANCEL_USER: 'CANCEL_USER',
    CANCEL_SYSTEM: 'CANCEL_SYSTEM',
    CANCEL_PARENT_PIPELINE: 'CANCEL_PARENT_PIPELINE',

    // 失败：执行失败 / 质量红线 / 人工审核驳回 / 子流水线失败 / 多类失败
    FAIL_EXEC: 'FAIL_EXEC',
    FAIL_QUALITY: 'FAIL_QUALITY',
    FAIL_REVIEW: 'FAIL_REVIEW',
    FAIL_SUB_PIPELINE: 'FAIL_SUB_PIPELINE',
    FAIL_MULTIPLE: 'FAIL_MULTIPLE',

    // 超时：Job / 步骤 / 队列 / 心跳
    TIMEOUT_JOB: 'TIMEOUT_JOB',
    TIMEOUT_STEP: 'TIMEOUT_STEP',
    TIMEOUT_QUEUE: 'TIMEOUT_QUEUE',
    TIMEOUT_HEARTBEAT: 'TIMEOUT_HEARTBEAT',

    // 成功：普通成功 / 阶段准入驳回（构建仍成功）
    SUCCESS: 'SUCCESS',
    SUCCESS_STAGE_ABORT: 'SUCCESS_STAGE_ABORT'
}

/** 中间扩展区类型 */
export const EXTRA_SECTION_TYPE = {
    RELATED_PIPELINE: 'relatedPipeline', // 父流水线信息 + 跳转
    TEXT: 'text' // 失败原因 / 驳回原因等纯文本
}

/** 位置行尾操作类型 */
export const POSITION_ACTION_TYPE = {
    LOCATE: 'locate', // 定位（取消场景）
    LOCATE_LOG: 'locateLog', // 定位日志（失败/超时场景）
    VIEW: 'view', // 画布定位标（多类失败等）
    OPERATOR_TEXT: 'operatorText' // 展示「操作人 + 动作」纯文案，不可点击
}

/** 位置行状态描述样式：取消用 tag，失败/超时用 text */
export const STATUS_DESC_STYLE = {
    TAG: 'tag',
    TEXT: 'text'
}

/** 摘要区布局：默认类型+原因 / 仅总耗时 / 阶段准入 */
export const SUMMARY_LAYOUT = {
    TYPE_REASON: 'typeReason', // 失败、超时、取消、阶段驳回
    DURATION_ONLY: 'durationOnly', // 普通执行成功
    STAGE_GATE: 'stageGate' // 阶段准入驳回（与 typeReason 结构相同，配置语义区分）
}

/** 位置行状态文案来源 */
export const POSITION_STATUS_MODE = {
    ERROR_CODE: 'errorCode', // 「错误码 xxx」
    END_TYPE_DESC: 'endTypeDesc', // 位置级成因，如「质量红线未达标」
    STATUS_AT_END_DESC: 'statusAtEndDesc' // 终态描述，如「因失败即停被终止」
}

const LOCATE_ACTION = {
    type: POSITION_ACTION_TYPE.LOCATE,
    labelKey: 'details.locate'
}

const LOCATE_LOG_ACTION = {
    type: POSITION_ACTION_TYPE.LOCATE_LOG,
    labelKey: 'details.locateLog'
}

const VIEW_ACTION = {
    type: POSITION_ACTION_TYPE.VIEW,
    labelKey: 'details.view'
}

/** 行尾仅展示操作人文案，无 labelKey */
const OPERATOR_TEXT_ACTION = {
    type: POSITION_ACTION_TYPE.OPERATOR_TEXT
}

const TEXT_EXTRA = titleKey => ({
    type: EXTRA_SECTION_TYPE.TEXT,
    titleKey
})

/** 四类超时 endType 共用规则 */
const TIMEOUT_END_TYPE_RULE = {
    summaryMode: 'reason',
    metaMode: 'action',
    extraSection: null,
    positionAction: LOCATE_LOG_ACTION,
    positionStatusMode: POSITION_STATUS_MODE.ERROR_CODE
}

/**
 * 多类失败：每行按 position.endType 单独配置
 * _default 兜底「失败即停」等场景：灰色 statusAtEndDesc + 查看
 */
const FAIL_MULTIPLE_POSITION_RULES = {
    [BUILD_END_TYPE.FAIL_EXEC]: {
        positionAction: LOCATE_LOG_ACTION,
        positionStatusMode: POSITION_STATUS_MODE.ERROR_CODE
    },
    [BUILD_END_TYPE.FAIL_QUALITY]: {
        positionAction: LOCATE_LOG_ACTION,
        positionStatusMode: POSITION_STATUS_MODE.END_TYPE_DESC
    },
    [BUILD_END_TYPE.FAIL_SUB_PIPELINE]: {
        positionAction: VIEW_ACTION,
        positionStatusMode: POSITION_STATUS_MODE.END_TYPE_DESC
    },
    _default: {
        positionAction: VIEW_ACTION,
        positionStatusMode: POSITION_STATUS_MODE.STATUS_AT_END_DESC,
        positionStatusMuted: true
    }
}

/** 未知 endType 时的安全兜底，避免渲染报错 */
const FALLBACK_END_TYPE_RULE = {
    summaryMode: 'reason', // 'count' =>「N 处在途均已终止」；'reason' => buildEndInfo.reason
    metaMode: 'action', // 'operator' => 时间+操作人+动作(actionTextKey)；'action' => 时间+动作(actionTextKey)；'startTrigger' => 开始+触发人
    extraSection: null, // 中间扩展区，不需要时设为 null
    positionAction: LOCATE_ACTION  // 位置行尾操作
}

/**
 * category 级公共字段说明：
 * - actionTextKey：摘要 meta 动作文案（结束 / 取消）
 * - positionOperatorTextKey（endType 级）：位置行「操作人 + 动作」文案（驳回）
 * - durationLabelKey：耗时前缀（已运行 / 总耗时）
 */
export const BUILD_END_INFO_CONFIG = {
    [BUILD_END_CATEGORY.CANCEL]: {
        titleKey: 'details.statusMap.CANCELED',
        typeLabelKey: 'details.cancelType',
        positionsTitleKey: 'details.cancelTerminatedPositions',
        summaryCountKey: 'details.cancelTerminatedCount',
        locateFailedKey: 'details.cancelLocateFailed',
        actionTextKey: 'cancel',
        durationLabelKey: 'execedTimes',
        statusDescStyle: STATUS_DESC_STYLE.TAG,
        theme: {
            summaryBg: '#FDF4E8',
            accent: '#FF9C01',
            sectionDot: '#F59500'
        },
        endTypeRules: {
            [BUILD_END_TYPE.CANCEL_USER]: {
                summaryMode: 'count',
                metaMode: 'operator',
                extraSection: null,
                positionAction: LOCATE_ACTION
            },
            [BUILD_END_TYPE.CANCEL_SYSTEM]: {
                summaryMode: 'reason',
                metaMode: 'action',
                extraSection: null,
                positionAction: LOCATE_ACTION
            },
            [BUILD_END_TYPE.CANCEL_PARENT_PIPELINE]: {
                summaryMode: 'reason',
                metaMode: 'action',
                extraSection: {
                    type: EXTRA_SECTION_TYPE.RELATED_PIPELINE,
                    titleKey: 'details.cancelParentPipeline',
                    logo: 'pipeline'
                },
                positionAction: LOCATE_ACTION
            }
        }
    },
    [BUILD_END_CATEGORY.FAIL]: {
        titleKey: 'details.statusMap.FAILED',
        typeLabelKey: 'details.failType',
        positionsTitleKey: 'details.failPositions',
        locateFailedKey: 'details.failLocateFailed',
        actionTextKey: 'details.ended',
        durationLabelKey: 'details.totalCost',
        statusDescStyle: STATUS_DESC_STYLE.TEXT,
        theme: {
            summaryBg: '#FEF0F0',
            accent: '#EA3636',
            sectionDot: '#EA3636'
        },
        endTypeRules: {
            [BUILD_END_TYPE.FAIL_EXEC]: {
                summaryMode: 'reason',
                metaMode: 'action',
                extraSection: null,
                positionAction: LOCATE_LOG_ACTION,
                positionStatusMode: POSITION_STATUS_MODE.ERROR_CODE
            },
            [BUILD_END_TYPE.FAIL_QUALITY]: {
                summaryMode: 'reason',
                metaMode: 'action',
                extraSection: TEXT_EXTRA('details.failReason'),
                positionAction: LOCATE_LOG_ACTION,
                positionStatusMode: POSITION_STATUS_MODE.ERROR_CODE
            },
            [BUILD_END_TYPE.FAIL_REVIEW]: {
                summaryMode: 'reason',
                metaMode: 'action',
                extraSection: TEXT_EXTRA('details.rejectReason'),
                positionsTitleKey: 'details.rejectPositions',
                positionAction: OPERATOR_TEXT_ACTION,
                positionOperatorFromItem: true, // 操作人读 position.operator
                positionOperatorTextKey: 'details.reject'
            },
            [BUILD_END_TYPE.FAIL_SUB_PIPELINE]: {
                summaryMode: 'reason',
                metaMode: 'action',
                extraSection: null,
                positionAction: LOCATE_LOG_ACTION,
                positionStatusMode: POSITION_STATUS_MODE.ERROR_CODE
            },
            [BUILD_END_TYPE.FAIL_MULTIPLE]: {
                summaryMode: 'reason',
                metaMode: 'action',
                extraSection: null,
                positionActionMode: 'perPosition',
                positionEndTypeRules: FAIL_MULTIPLE_POSITION_RULES
            }
        }
    },
    [BUILD_END_CATEGORY.TIMEOUT]: {
        titleKey: 'details.statusMap.EXEC_TIMEOUT',
        typeLabelKey: 'details.timeoutType',
        positionsTitleKey: 'details.timeoutTerminatedPositions',
        locateFailedKey: 'details.timeoutLocateFailed',
        actionTextKey: 'details.ended',
        durationLabelKey: 'details.totalCost',
        statusDescStyle: STATUS_DESC_STYLE.TEXT,
        theme: {
            summaryBg: '#FEF0F0',
            accent: '#EA3636',
            sectionDot: '#EA3636'
        },
        endTypeRules: {
            [BUILD_END_TYPE.TIMEOUT_JOB]: TIMEOUT_END_TYPE_RULE,
            [BUILD_END_TYPE.TIMEOUT_STEP]: TIMEOUT_END_TYPE_RULE,
            [BUILD_END_TYPE.TIMEOUT_QUEUE]: TIMEOUT_END_TYPE_RULE,
            [BUILD_END_TYPE.TIMEOUT_HEARTBEAT]: TIMEOUT_END_TYPE_RULE
        }
    },
    [BUILD_END_CATEGORY.SUCCESS]: {
        titleKey: 'details.statusMap.SUCCEED',
        typeLabelKey: 'details.stageGateType',
        locateFailedKey: 'details.failLocateFailed',
        actionTextKey: 'details.ended', // 摘要 meta 用「结束」
        durationLabelKey: 'details.totalCost',
        endTypeRules: {
            [BUILD_END_TYPE.SUCCESS]: {
                summaryLayout: SUMMARY_LAYOUT.DURATION_ONLY,
                metaMode: 'startTrigger', // 开始时间 + 触发人，由 ExecDetail 传入 props
                theme: {
                    summaryBg: '#F5F7FA',
                    accent: '#2DCB56',
                    sectionDot: '#2DCB56'
                }
            },
            [BUILD_END_TYPE.SUCCESS_STAGE_ABORT]: {
                titleKey: 'details.stageSuccessTitle',
                summaryLayout: SUMMARY_LAYOUT.STAGE_GATE,
                metaMode: 'action',
                typeLabelKey: 'details.stageGateType',
                positionsTitleKey: 'details.rejectPositions',
                positionAction: OPERATOR_TEXT_ACTION,
                positionOperatorFromItem: true,
                positionOperatorTextKey: 'details.reject', // 位置行用「驳回」，与 meta「结束」区分
                positionLayout: 'stageReview', // 展示 reviewSuggest + 审核组信息
                theme: {
                    summaryBg: '#FDF4E8',
                    accent: '#FF9C01',
                    sectionDot: '#F59500'
                }
            }
        }
    }
}

/** 按 buildEndInfo.endCategory 取 category 配置；无配置返回 null */
export function getBuildEndInfoConfig (endCategory) {
    return BUILD_END_INFO_CONFIG[endCategory] || null
}

/** 按 endType 取展示规则；找不到时返回 FALLBACK_END_TYPE_RULE */
export function getBuildEndTypeRule (categoryConfig, endType) {
    return categoryConfig?.endTypeRules?.[endType] || FALLBACK_END_TYPE_RULE
}

/** 合并 category 与 endType 配置，endType 可覆盖 titleKey / theme / positionsTitleKey 等 */
export function getMergedEndTypeConfig (categoryConfig, endType) {
    if (!categoryConfig) return null
    const endTypeRule = getBuildEndTypeRule(categoryConfig, endType)
    return {
        ...categoryConfig,
        ...endTypeRule,
        theme: {
            ...categoryConfig.theme,
            ...endTypeRule.theme
        }
    }
}

/**
 * FAIL_MULTIPLE 等 perPosition 场景：按 position.endType 取行级规则
 * 非 perPosition 时直接返回 endTypeRule 本身
 */
export function getPositionEndTypeRule (endTypeRule, position) {
    if (endTypeRule?.positionActionMode !== 'perPosition') {
        return endTypeRule
    }
    const rules = endTypeRule.positionEndTypeRules || {}
    return rules[position?.endType] || rules._default || endTypeRule
}

const DEFAULT_POSITION_ACTION = {
    type: POSITION_ACTION_TYPE.LOCATE,
    labelKey: 'details.locate'
}

/** 解析位置行尾按钮类型与文案 key */
export function resolvePositionAction (endTypeRule, position) {
    const rule = getPositionEndTypeRule(endTypeRule, position)
    return rule?.positionAction || DEFAULT_POSITION_ACTION
}

/**
 * 解析位置行「操作人 + 动作」的 i18n key
 * 优先级：行级 > endType 级 > category 级 > actionTextKey
 */
export function resolvePositionOperatorTextKey (rule, endTypeRule, mergedConfig) {
    return rule?.positionOperatorTextKey
        || endTypeRule?.positionOperatorTextKey
        || mergedConfig?.positionOperatorTextKey
        || mergedConfig?.actionTextKey
}

/**
 * 解析位置行状态文案（错误码 / 成因 / 终态描述）
 * @param translate - i18n 函数，如 (key, params) => vm.$t(key, params)
 */
export function resolvePositionStatusText (item, rule, { endType, isStatusDescTag, translate }) {
    const mode = rule?.positionStatusMode
    if (mode === POSITION_STATUS_MODE.ERROR_CODE && item.errorCode) {
        return translate('details.errorCode', [item.errorCode])
    }
    if (mode === POSITION_STATUS_MODE.END_TYPE_DESC && item.endTypeDesc) {
        return item.endTypeDesc
    }
    if (mode === POSITION_STATUS_MODE.STATUS_AT_END_DESC && item.statusAtEndDesc) {
        return item.statusAtEndDesc
    }
    if (endType === BUILD_END_TYPE.FAIL_MULTIPLE) {
        if (item.errorCode) {
            return translate('details.errorCode', [item.errorCode])
        }
        if (item.endTypeDesc) return item.endTypeDesc
    }
    if (!isStatusDescTag && item.errorCode) {
        return translate('details.errorCode', [item.errorCode])
    }
    return item.statusAtEndDesc || ''
}
