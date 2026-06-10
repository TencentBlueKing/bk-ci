/**
 * 触发事件描述（eventDesc）的前端组装配置。
 *
 * 后端会按 i18n 占位 key（如 bkTgitPushEventDesc）+ params 数组下发，
 * 前端根据 EVENT_DESC_PARAM_MAPPERS 把 params 数组解构为命名插槽数据，
 * 再交给 i18n 模板渲染，以避免后端拼 HTML 串导致的 XSS 风险与多语言问题。
 */

/**
 * `event.buildNum` 历史上是后端拼好的 `<a href="..." target="_blank">xxx</a>`，
 * 这里通过该正则把链接和文案分离，再用 safeUrl 校验后用 Vue 模板渲染。
 */
export const BUILD_NUM_LINK_REG = /^<a href="([^"]+)" target="_blank">([^<]+)<\/a>$/

/** 触发事件描述返回结构（后端可能下发对象、JSON 字符串或纯文本） */
export interface RawEventDescObject {
  code?: string
  params?: unknown[]
  defaultMessage?: string
}

export type RawEventDesc = RawEventDescObject | string | null | undefined

export interface NormalizedEventDesc {
  code: string
  params: unknown[]
  defaultMessage: string
}

/** 文案插槽类型 */
export type SlotDataType = 'text' | 'link' | 'user'

export interface SlotData {
  type: SlotDataType
  text: string
  href?: string
}

/** 把任意值转字符串（null/undefined → ''） */
export const toText = (value: unknown): string =>
  value === undefined || value === null ? '' : String(value)

/** 单个 slot 的工厂函数：拿到全部 params，返回单个 slot 的数据 */
type ParamFactory = (params: unknown[]) => SlotData

/** 纯文本参数 */
const textParam = (index: number): ParamFactory => (params) => ({
  type: 'text',
  text: toText(params[index]),
})

/** 用户参数（带 trigger-user 样式） */
const userParam = (index: number): ParamFactory => (params) => ({
  type: 'user',
  text: toText(params[index]),
})

/** 链接参数 */
const linkParam = (
  hrefIndex: number,
  textIndex: number,
  prefix = '',
  hrefFormatter?: (value: unknown) => string,
): ParamFactory => (params) => ({
  type: 'link',
  href: hrefFormatter ? hrefFormatter(params[hrefIndex]) : toText(params[hrefIndex]),
  text: `${prefix}${toText(params[textIndex])}`,
})

/** 把 { slotName: paramFactory } 的配置转为 params => { slotName: SlotData } */
export type ParamMapper = (params: unknown[]) => Record<string, SlotData>

const mapParams = (mapping: Record<string, ParamFactory>): ParamMapper => (params) =>
  Object.entries(mapping).reduce<Record<string, SlotData>>((acc, [name, mapper]) => {
    acc[name] = mapper(params)
    return acc
  }, {})

/** 全部支持的插槽名 */
export const EVENT_DESC_SLOT_NAMES = [
  'branch',
  'commit',
  'user',
  'issue',
  'mr',
  'source',
  'tag',
  'note',
  'review',
  'pr',
  'change',
  'action',
  'revision',
  'remoteUser',
  'pipeline',
  'event',
  'targetPipeline',
  'name',
  'version',
  'oldName',
  'newName',
] as const

export type EventDescSlotName = typeof EVENT_DESC_SLOT_NAMES[number]

// 复用预设
const tgitIssueParams = mapParams({
  issue: linkParam(0, 1, '!'),
  user: userParam(2),
})
const tgitMrParams = mapParams({
  mr: linkParam(0, 1, '!'),
  user: userParam(2),
})
const tgitReviewParams = mapParams({
  review: linkParam(0, 1),
  user: userParam(2),
})

/** 事件 code → 参数映射 */
export const EVENT_DESC_PARAM_MAPPERS: Record<string, ParamMapper> = {
  /* ===== TGit 事件 ===== */
  bkTgitPushEventDesc: mapParams({
    branch: textParam(0),
    commit: linkParam(1, 2),
    user: userParam(3),
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
    user: userParam(3),
  }),
  bkTgitTagDeleteEventDesc: mapParams({
    source: textParam(0),
    tag: linkParam(1, 2),
    user: userParam(3),
  }),
  bkTgitNoteEventDesc: mapParams({
    note: linkParam(0, 1),
    user: userParam(2),
  }),
  bkTgitReviewCreatedEventDesc: tgitReviewParams,
  bkTgitReviewApprovedEventDesc: tgitReviewParams,
  bkTgitReviewApprovingEventDesc: mapParams({ review: linkParam(0, 1) }),
  bkTgitReviewClosedEventDesc: mapParams({ review: linkParam(0, 1) }),
  bkTgitReviewChangeDeniedEventDesc: tgitReviewParams,
  bkTgitReviewChangeRequiredEventDesc: tgitReviewParams,

  /* ===== GitHub ===== */
  bkGithubPushEventDesc: mapParams({
    branch: textParam(0),
    commit: linkParam(1, 2),
    user: userParam(3),
  }),
  bkGithubCreateTagEventDesc: mapParams({
    tag: linkParam(0, 1),
    user: userParam(2),
  }),
  bkGithubCreateBranchEventDesc: mapParams({
    branch: linkParam(0, 1),
    user: userParam(2),
  }),
  bkGithubPrEventDesc: mapParams({
    pr: linkParam(0, 1, '!'),
    user: userParam(2),
    action: textParam(3),
  }),

  /* ===== P4 / SVN ===== */
  bkP4EventDesc: mapParams({
    change: textParam(0),
    user: userParam(1),
    action: textParam(2),
  }),
  bkSvnCommitEventDesc: mapParams({
    revision: textParam(0),
    user: userParam(1),
  }),

  /* ===== 手动 / 远程 / 服务 / 子流水线 / 定时 / 重放 / PAC ===== */
  bkManualStartEventDesc: mapParams({ user: userParam(0) }),
  bkRemoteStartEventDesc: (params) => ({
    remoteUser: {
      type: 'user',
      text: `${toText(params[0])} [${toText(params[1])}]`,
    },
  }),
  bkServiceStartEventDesc: mapParams({ user: userParam(0) }),
  bkPipelineStartEventDesc: mapParams({
    pipeline: linkParam(1, 2),
    user: userParam(0),
  }),
  bkTimingStartEventDesc: mapParams({ user: userParam(0) }),
  bkEventReplayDesc: mapParams({
    event: linkParam(0, 0, '', (value) => `?eventId=${toText(value)}`),
    user: userParam(1),
  }),
  bkRepoEnablePacEventDesc: mapParams({ user: userParam(0) }),

  /* ===== 通用 Git（参数与 TGit 同形） ===== */
  bkGitPushEventDesc: mapParams({
    branch: textParam(0),
    commit: linkParam(1, 2),
    user: userParam(3),
  }),
  bkGitPushCreateEventDesc: mapParams({ branch: linkParam(1, 0), user: userParam(3) }),
  bkGitPushDeleteEventDesc: mapParams({ branch: linkParam(1, 0), user: userParam(3) }),
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
    user: userParam(3),
  }),
  bkGitTagDeleteEventDesc: mapParams({
    source: textParam(0),
    tag: linkParam(1, 2),
    user: userParam(3),
  }),
  bkGitNoteEventDesc: mapParams({ note: linkParam(0, 1), user: userParam(2) }),
  bkGitReviewCreatedEventDesc: tgitReviewParams,
  bkGitReviewApprovedEventDesc: tgitReviewParams,
  bkGitReviewApprovingEventDesc: mapParams({ review: linkParam(0, 1) }),
  bkGitReviewClosedEventDesc: mapParams({ review: linkParam(0, 1) }),
  bkGitReviewChangeDeniedEventDesc: tgitReviewParams,
  bkGitReviewChangeRequiredEventDesc: tgitReviewParams,

  /* ===== 子流水线循环依赖 ===== */
  bkSubPipelineCircularDependencyErrorMessage: mapParams({
    pipeline: linkParam(0, 1),
  }),
  bkOtherSubPipelineCircularDependencyErrorMessage: mapParams({
    pipeline: linkParam(0, 1),
    targetPipeline: linkParam(2, 3),
  }),

  /* ===== YAML 流水线操作消息 ===== */
  bkYamlPipelineCreateSuccess: mapParams({
    pipeline: linkParam(0, 1),
    version: textParam(2),
  }),
  bkYamlPipelineUpdateSuccess: mapParams({
    pipeline: linkParam(0, 1),
    version: textParam(2),
  }),
  bkYamlPipelineRenameSuccess: mapParams({
    pipeline: linkParam(0, 1),
    version: textParam(2),
    oldName: textParam(3),
    newName: textParam(4),
  }),
  bkYamlPipelineDeleteVersionSuccess: mapParams({
    pipeline: linkParam(0, 1),
    version: textParam(2),
  }),
  bkYamlPipelineDeleteSuccess: mapParams({
    name: textParam(0),
    version: textParam(1),
  }),
  bkYamlPipelineCreateFailed: mapParams({}),
  bkYamlPipelineUpdateFailed: mapParams({ pipeline: linkParam(0, 1) }),
  bkYamlPipelineRenameFailed: mapParams({
    pipeline: linkParam(0, 1),
    oldName: textParam(2),
    newName: textParam(3),
  }),
  bkYamlPipelineDependencyUpgradeFailed: mapParams({ pipeline: linkParam(0, 1) }),
  bkYamlPipelineDeleteVersionFailed: mapParams({
    pipeline: linkParam(0, 1),
    version: textParam(2),
  }),
  bkYamlPipelineDeleteFailed: mapParams({ pipeline: linkParam(0, 1) }),
  bkYamlPipelineCloseFailed: mapParams({ pipeline: linkParam(0, 1) }),
  bkYamlInstancePullRequestClosed: mapParams({ mr: linkParam(0, 1) }),
}

/**
 * 将后端可能下发的三种形态（对象 / JSON 字符串 / 普通字符串）统一成
 * `{ code, params, defaultMessage }`。
 */
export function normalizeEventDesc(eventDesc: RawEventDesc): NormalizedEventDesc {
  if (eventDesc && typeof eventDesc === 'object') {
    return {
      code: eventDesc.code || '',
      params: eventDesc.params || [],
      defaultMessage: eventDesc.defaultMessage || '',
    }
  }
  if (typeof eventDesc === 'string') {
    try {
      const desc = JSON.parse(eventDesc) as RawEventDescObject
      return {
        code: desc.code || '',
        params: desc.params || [],
        defaultMessage: desc.defaultMessage || '',
      }
    } catch (e) {
      return { code: '', params: [], defaultMessage: eventDesc }
    }
  }
  return { code: '', params: [], defaultMessage: '' }
}

/**
 * URL 安全校验：只允许 http(s) 协议；保留 "/path"、"?xxx"、"#xxx" 这三种相对形式。
 * 校验失败返回空串。
 */
export function safeUrl(url: unknown): string {
  const value = toText(url).trim()
  if (!value) return ''

  try {
    const parsed = new URL(value, window.location.origin)
    if (!['http:', 'https:'].includes(parsed.protocol)) {
      return ''
    }
    // 原值是 "/path"（非 //schemaless）、"?xxx"、"#xxx" 时，原样保留
    return /^(\/(?!\/)|[?#])/.test(value) ? value : parsed.href
  } catch (e) {
    return ''
  }
}
