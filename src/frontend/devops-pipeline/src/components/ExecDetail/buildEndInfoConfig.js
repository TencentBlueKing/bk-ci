/**
 * 执行状态信息 Popover 配置
 * 扩展时按 status 补充配置即可；差异主要在文案、主题色、endType 展示规则
 */

// 已取消类型：用户取消 / 系统取消 / 父流水线取消
export const BUILD_END_TYPE = {
    CANCEL_USER: 'CANCEL_USER',
    CANCEL_SYSTEM: 'CANCEL_SYSTEM',
    CANCEL_PARENT_PIPELINE: 'CANCEL_PARENT_PIPELINE'
}

// 扩展区类型：relatedPipeline（关联流水线）| text（驳回/失败原因等纯文本）
export const EXTRA_SECTION_TYPE = {
    RELATED_PIPELINE: 'relatedPipeline',
    TEXT: 'text'
}

// 位置行尾操作：locate定位 / locateLog定位日志 / view查看 / operatorText展示「操作人 + actionText」，不可点击（如驳回）
export const POSITION_ACTION_TYPE = {
    LOCATE: 'locate',
    LOCATE_LOG: 'locateLog',
    VIEW: 'view',
    OPERATOR_TEXT: 'operatorText'
}

// 位置行状态展示：tag标签 / text文字（失败/超时用红色文字）
export const STATUS_DESC_STYLE = {
    TAG: 'tag',
    TEXT: 'text'
}

const FALLBACK_END_TYPE_RULE = {
    summaryMode: 'reason',  // 'count' 显示「N 处在途均已终止」；'reason' 显示 buildEndInfo.reason
    metaMode: 'action',  // 'operator' => 时间 + 操作人 + actionTextKey；'action' => 时间 + actionTextKey
    extraSection: null, // 中间扩展区，不需要时设为 null
    positionAction: {  // 位置行尾操作
        type: POSITION_ACTION_TYPE.LOCATE,
        labelKey: 'details.locate'
    }
}

export const BUILD_END_INFO_CONFIG = {
    CANCELED: {
        titleKey: 'details.statusMap.CANCELED',
        typeLabelKey: 'details.cancelType',
        positionsTitleKey: 'details.cancelTerminatedPositions',
        summaryCountKey: 'details.cancelTerminatedCount',
        locateFailedKey: 'details.cancelLocateFailed',
        actionTextKey: 'cancel',
        durationLabelKey: 'execedTimes',    // 耗时前文案（已运行 / 总耗时）
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
                positionAction: {
                    type: POSITION_ACTION_TYPE.LOCATE,
                    labelKey: 'details.locate'
                }
            },
            [BUILD_END_TYPE.CANCEL_SYSTEM]: {
                summaryMode: 'reason',
                metaMode: 'action',
                extraSection: null,
                positionAction: {
                    type: POSITION_ACTION_TYPE.LOCATE,
                    labelKey: 'details.locate'
                }
            },
            [BUILD_END_TYPE.CANCEL_PARENT_PIPELINE]: {
                summaryMode: 'reason',
                metaMode: 'action',
                extraSection: {
                    type: EXTRA_SECTION_TYPE.RELATED_PIPELINE,
                    titleKey: 'details.cancelParentPipeline',
                    logo: 'pipeline'
                },
                positionAction: {
                    type: POSITION_ACTION_TYPE.LOCATE,
                    labelKey: 'details.locate'
                }
            }
        }
    },
}

/** 按构建状态取配置；无配置返回 null（同时用于判断是否展示 Popover） */
export function getBuildEndInfoConfig (status) {
    return BUILD_END_INFO_CONFIG[status] || null
}

/**
 * 按 endType 取展示规则
 * 直接返回 endTypeRules[endType]；找不到时返回安全兜底，避免未知 endType 导致页面报错
 */
export function getBuildEndTypeRule (statusConfig, endType) {
    return statusConfig?.endTypeRules?.[endType] || FALLBACK_END_TYPE_RULE
}
