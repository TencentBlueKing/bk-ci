/**
 * 触发事件描述的配置与工具函数（codelib 服务使用）
 */

/** 事件描述 i18n-t 支持的所有插槽名称 */
export const EVENT_DESC_SLOT_NAMES = [
    'branch', 'commit', 'user', 'issue', 'mr', 'source', 'tag', 'note',
    'review', 'pr', 'change', 'action', 'revision', 'remoteUser', 'pipeline',
    'event', 'targetPipeline', 'name', 'version', 'oldName', 'newName'
]

export const BUILD_NUM_LINK_REG = /^<a href="([^"]+)" target="_blank">([^<]+)<\/a>$/

export const toText = value => (value === undefined || value === null ? '' : String(value))

const textParam = index => params => ({ type: 'text', text: toText(params[index]) })
const userParam = index => params => ({ type: 'user', text: toText(params[index]) })
const linkParam = (hrefIndex, textIndex, prefix = '', hrefFormatter) => params => ({
    type: 'link',
    href: hrefFormatter ? hrefFormatter(params[hrefIndex]) : params[hrefIndex],
    text: `${prefix}${toText(params[textIndex])}`
})
const mapParams = mapping => params => Object.entries(mapping).reduce((acc, [name, mapper]) => {
    acc[name] = mapper(params)
    return acc
}, {})

const tgitIssueParams = mapParams({
    issue: linkParam(0, 1, '!'),
    user: userParam(2)
})
const tgitMrParams = mapParams({
    mr: linkParam(0, 1, '!'),
    user: userParam(2)
})
const tgitReviewParams = mapParams({
    review: linkParam(0, 1),
    user: userParam(2)
})

/**
 * 事件描述 i18n key -> 参数映射表
 * 每个 mapper 接收 params 数组，返回 slotName -> { type, text, href? } 的映射对象
 */
export const EVENT_DESC_PARAM_MAPPERS = {
    bkTgitPushEventDesc: mapParams({
        branch: textParam(0),
        commit: linkParam(1, 2),
        user: userParam(3)
    }),
    bkTgitIssueCreatedEventDesc: tgitIssueParams,
    bkTgitIssueUpdatedEventDesc: tgitIssueParams,
    bkTgitIssueClosedEventDesc: tgitIssueParams,
    bkTgitIssueReopenedEventDesc: tgitIssueParams,
    bkTgitMrCreatedEventDesc: tgitMrParams,
    bkTgitMrUpdatedEventDesc: tgitMrParams,
    bkTgitMrClosedEventDesc: tgitMrParams,
    bkTgitMrReopenedEventDesc: tgitMrParams,
    bkTgitMrPushUpdatedEventDesc: tgitMrParams,
    bkTgitMrMergedEventDesc: tgitMrParams,
    bkTgitTagPushEventDesc: mapParams({
        source: textParam(0),
        tag: linkParam(1, 2),
        user: userParam(3)
    }),
    bkTgitTagDeleteEventDesc: mapParams({
        source: textParam(0),
        tag: linkParam(1, 2),
        user: userParam(3)
    }),
    bkTgitNoteEventDesc: mapParams({
        note: linkParam(0, 1),
        user: userParam(2)
    }),
    bkTgitReviewCreatedEventDesc: tgitReviewParams,
    bkTgitReviewApprovedEventDesc: tgitReviewParams,
    bkTgitReviewApprovingEventDesc: mapParams({
        review: linkParam(0, 1)
    }),
    bkTgitReviewClosedEventDesc: mapParams({
        review: linkParam(0, 1)
    }),
    bkTgitReviewChangeDeniedEventDesc: tgitReviewParams,
    bkTgitReviewChangeRequiredEventDesc: tgitReviewParams,
    bkGithubPushEventDesc: mapParams({
        branch: textParam(0),
        commit: linkParam(1, 2),
        user: userParam(3)
    }),
    bkGithubCreateTagEventDesc: mapParams({
        tag: linkParam(0, 1),
        user: userParam(2)
    }),
    bkGithubCreateBranchEventDesc: mapParams({
        branch: linkParam(0, 1),
        user: userParam(2)
    }),
    bkGithubPrEventDesc: mapParams({
        pr: linkParam(0, 1, '!'),
        user: userParam(2),
        action: textParam(3)
    }),
    bkP4EventDesc: mapParams({
        change: textParam(0),
        user: userParam(1),
        action: textParam(2)
    }),
    bkSvnCommitEventDesc: mapParams({
        revision: textParam(0),
        user: userParam(1)
    }),
    bkManualStartEventDesc: mapParams({
        user: userParam(0)
    }),
    bkRemoteStartEventDesc: params => ({
        remoteUser: {
            type: 'user',
            text: `${toText(params[0])} [${toText(params[1])}]`
        }
    }),
    bkServiceStartEventDesc: mapParams({
        user: userParam(0)
    }),
    bkPipelineStartEventDesc: mapParams({
        pipeline: linkParam(1, 2),
        user: userParam(0)
    }),
    bkTimingStartEventDesc: mapParams({
        user: userParam(0)
    }),
    bkEventReplayDesc: mapParams({
        event: linkParam(0, 0, '', value => `?eventId=${value}`),
        user: userParam(1)
    }),
    bkRepoEnablePacEventDesc: mapParams({
        user: userParam(0)
    }),
    // bkGit* 事件（通用 Git 平台，参数格式同 bkTgit*）
    bkGitPushEventDesc: mapParams({
        branch: textParam(0),
        commit: linkParam(1, 2),
        user: userParam(3)
    }),
    bkGitPushCreateEventDesc: mapParams({
        branch: linkParam(1, 0),
        user: userParam(3)
    }),
    bkGitPushDeleteEventDesc: mapParams({
        branch: linkParam(1, 0),
        user: userParam(3)
    }),
    bkGitIssueOpenedEventDesc: tgitIssueParams,
    bkGitIssueUpdatedEventDesc: tgitIssueParams,
    bkGitIssueClosedEventDesc: tgitIssueParams,
    bkGitIssueReopenedEventDesc: tgitIssueParams,
    bkGitPrCreatedEventDesc: tgitMrParams,
    bkGitPrUpdatedEventDesc: tgitMrParams,
    bkGitPrClosedEventDesc: tgitMrParams,
    bkGitPrReopenedEventDesc: tgitMrParams,
    bkGitPrPushUpdatedEventDesc: tgitMrParams,
    bkGitPrMergedEventDesc: tgitMrParams,
    bkGitTagPushEventDesc: mapParams({
        source: textParam(0),
        tag: linkParam(1, 2),
        user: userParam(3)
    }),
    bkGitTagDeleteEventDesc: mapParams({
        source: textParam(0),
        tag: linkParam(1, 2),
        user: userParam(3)
    }),
    bkGitNoteEventDesc: mapParams({
        note: linkParam(0, 1),
        user: userParam(2)
    }),
    bkGitReviewCreatedEventDesc: tgitReviewParams,
    bkGitReviewApprovedEventDesc: tgitReviewParams,
    bkGitReviewApprovingEventDesc: mapParams({
        review: linkParam(0, 1)
    }),
    bkGitReviewClosedEventDesc: mapParams({
        review: linkParam(0, 1)
    }),
    bkGitReviewChangeDeniedEventDesc: tgitReviewParams,
    bkGitReviewChangeRequiredEventDesc: tgitReviewParams,
    // 子流水线循环依赖
    bkSubPipelineCircularDependencyErrorMessage: mapParams({
        pipeline: linkParam(0, 1)
    }),
    bkOtherSubPipelineCircularDependencyErrorMessage: mapParams({
        pipeline: linkParam(0, 1),
        targetPipeline: linkParam(2, 3)
    }),
    // YAML 流水线操作消息
    bkYamlPipelineCreateSuccess: mapParams({
        pipeline: linkParam(0, 1),
        version: textParam(2)
    }),
    bkYamlPipelineUpdateSuccess: mapParams({
        pipeline: linkParam(0, 1),
        version: textParam(2)
    }),
    bkYamlPipelineRenameSuccess: mapParams({
        pipeline: linkParam(0, 1),
        version: textParam(2),
        oldName: textParam(3),
        newName: textParam(4)
    }),
    bkYamlPipelineDeleteVersionSuccess: mapParams({
        pipeline: linkParam(0, 1),
        version: textParam(2)
    }),
    bkYamlPipelineDeleteSuccess: mapParams({
        name: textParam(0),
        version: textParam(1)
    }),
    bkYamlPipelineCreateFailed: mapParams({}),
    bkYamlPipelineUpdateFailed: mapParams({
        pipeline: linkParam(0, 1)
    }),
    bkYamlPipelineRenameFailed: mapParams({
        pipeline: linkParam(0, 1),
        oldName: textParam(2),
        newName: textParam(3)
    }),
    bkYamlPipelineDependencyUpgradeFailed: mapParams({
        pipeline: linkParam(0, 1)
    }),
    bkYamlPipelineDeleteVersionFailed: mapParams({
        pipeline: linkParam(0, 1),
        version: textParam(2)
    }),
    bkYamlPipelineDeleteFailed: mapParams({
        pipeline: linkParam(0, 1)
    }),
    bkYamlPipelineCloseFailed: mapParams({
        pipeline: linkParam(0, 1)
    }),
    bkYamlInstancePullRequestClosed: mapParams({
        mr: linkParam(0, 1)
    })
}

/**
 * 规范化事件描述数据
 * @param {Object|string} eventDesc - 事件描述，可能是对象或 JSON 字符串
 * @returns {{ code: string, params: Array, defaultMessage: string }}
 */
export function normalizeEventDesc (eventDesc) {
    if (eventDesc && typeof eventDesc === 'object') {
        return {
            code: eventDesc.code || '',
            params: eventDesc.params || [],
            defaultMessage: eventDesc.defaultMessage || ''
        }
    }
    if (typeof eventDesc === 'string') {
        try {
            const desc = JSON.parse(eventDesc)
            return {
                code: desc.code || '',
                params: desc.params || [],
                defaultMessage: desc.defaultMessage || ''
            }
        } catch (e) {
            return {
                code: '',
                params: [],
                defaultMessage: eventDesc
            }
        }
    }
    return {
        code: '',
        params: [],
        defaultMessage: ''
    }
}

/**
 * 安全校验 URL，仅允许 http/https 协议
 * @param {string} url
 * @returns {string} 合法 URL 或空字符串
 */
export function safeUrl (url) {
    const value = toText(url).trim()
    if (!value) {
        return ''
    }

    try {
        const parsed = new URL(value, window.location.origin)
        if (!['http:', 'https:'].includes(parsed.protocol)) {
            return ''
        }
        return /^(\/(?!\/)|[?#])/.test(value) ? value : parsed.href
    } catch (e) {
        return ''
    }
}
