/**
 * 构建结束信息 Popover 配置
 * 扩展时按 status 补充配置即可；差异主要在文案、主题色、endType 展示规则
 */

export const BUILD_END_TYPE = {
    CANCEL_USER: 'CANCEL_USER',
    CANCEL_SYSTEM: 'CANCEL_SYSTEM',
    CANCEL_PARENT_PIPELINE: 'CANCEL_PARENT_PIPELINE'
}

export const EXTRA_SECTION_TYPE = {
    RELATED_PIPELINE: 'relatedPipeline',
    TEXT: 'text'
}

export const BUILD_END_INFO_CONFIG = {
    CANCELED: {
        titleKey: 'details.statusMap.CANCELED',
        typeLabelKey: 'details.cancelType',
        positionsTitleKey: 'details.cancelTerminatedPositions',
        summaryCountKey: 'details.cancelTerminatedCount',
        locateFailedKey: 'details.cancelLocateFailed',
        actionTextKey: 'cancel',
        theme: {
            summaryBg: '#FDF4E8',
            accent: '#FF9C01',
            sectionDot: '#F59500'
        },
        endTypeRules: {
            [BUILD_END_TYPE.CANCEL_USER]: {
                summaryMode: 'count',
                metaMode: 'operator',
                extraSection: null
            },
            [BUILD_END_TYPE.CANCEL_SYSTEM]: {
                summaryMode: 'reason',
                metaMode: 'action',
                extraSection: null
            },
            [BUILD_END_TYPE.CANCEL_PARENT_PIPELINE]: {
                summaryMode: 'reason',
                metaMode: 'action',
                extraSection: {
                    type: EXTRA_SECTION_TYPE.RELATED_PIPELINE,  // relatedPipeline（关联流水线）| text（驳回/失败原因等纯文本）
                    titleKey: 'details.cancelParentPipeline',
                    logo: 'pipeline'  // 按需配置不同 logo，不配则不展示图标
                    // contentField: 'detailReason'
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
    const rule = statusConfig?.endTypeRules?.[endType]
    if (rule) return rule

    return {
        summaryMode: 'reason',  // 'count' 显示「N 处在途均已终止」；'reason' 显示 buildEndInfo.reason
        metaMode: 'action',  // 'operator' => 时间 + 操作人 + actionTextKey；'action' => 时间 + actionTextKey
        extraSection: null  // 中间扩展区；null 表示不展示
    }
}
