export const titlesMap = {
    pipeline: 'documentTitlePipeline',
    codelib: 'documentTitleCodelib',
    artifactory: 'documentTitleArtifactory',
    codecc: 'documentTitleCodecc',
    experience: 'documentTitleExperience',
    turbo: 'documentTitleTurbo',
    repo: 'documentTitleRepo',
    preci: 'documentTitlePreci',
    stream: 'documentTitleStream',
    wetest: 'documentTitleWetest',
    quality: 'documentTitleQuality',
    xinghai: 'documentTitleXinghai',
    bcs: 'documentTitleBcs',
    job: 'documentTitleJob',
    environment: 'documentTitleEnvironment',
    vs: 'documentTitleVs',
    apk: 'documentTitleApk',
    monitor: 'documentTitleMonitor',
    perm: 'documentTitlePerm',
    ticket: 'documentTitleTicket',
    store: 'documentTitleStore',
    metrics: 'documentTitleMetrics',
    manage: 'documentTitleManage',
    permission: 'documentTitlePermission'
}
export function mapDocumnetTitle (service) {
    return titlesMap[service] || 'documentTitleHome'
}

/**
 * AI 小鲸 iframe 桥接协议。
 * - `ACTIONS` 是跨 iframe `postMessage` 的 action 名，需与 devops-pipeline /
 *   devops-ai 双方保持一致（属于线上协议，不要随意改名）。
 * - `EVENTS` 是 iframeUtil 解码消息后在 eventBus 上转发的事件名，仅
 *   devops-nav 内部使用。
 */
export const AI_IFRAME_ACTIONS = {
    UPDATE_SUB_CONTEXT: 'updateAiSubContext',
    REQUEST_CONTEXT: 'requestAiContext',
    CLOSE_PANEL: 'CLOSE_DEVOPS_AI_PANEL',
    SYNC_CONTEXT: 'syncAiContext'
} as const

export const AI_IFRAME_EVENTS = {
    UPDATE_SUB_CONTEXT: 'update-ai-sub-context',
    REQUEST_CONTEXT: 'request-ai-context',
    CLOSE_PANEL: 'close-devops-ai-panel'
} as const
