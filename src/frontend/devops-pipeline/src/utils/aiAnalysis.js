const AI_ANALYSIS_ACTION = 'requestAiAnalysis'

export function normalizeAiError (error = {}) {
    if (Array.isArray(error)) return error.map(item => normalizeAiError(item))
    return {
        taskName: error.taskName || '',
        errorCode: error.errorCode || '',
        errorMsg: error.errorMsg || '',
        errorType: error.errorType || '',
        stageId: error.stageId || '',
        containerId: error.containerId || '',
        taskId: error.taskId || '',
        atomCode: error.atomCode || ''
    }
}

function normalizeAiContext (context = {}) {
    const version = context?.version
    return version === undefined || version === null
        ? {}
        : { version: String(version) }
}

function normalizeAiAttachment (attachment) {
    if (!attachment || typeof attachment !== 'object') return undefined
    const title = String(attachment.title || '').trim()
    const body = String(attachment.body || '').trim()
    if (!title && !body) return undefined
    return { title, body }
}

export function formatBuildNumPrefix (buildNum) {
    if (buildNum === undefined || buildNum === null || buildNum === '') return ''
    return `#${buildNum} `
}

export function formatPositionPrefix (position) {
    const text = position == null ? '' : String(position).trim()
    return text ? `${text} ` : ''
}

export function joinLabeledLine (label, value) {
    const text = value == null ? '' : String(value).trim()
    if (!text) return ''
    return `${label} ${text}`.trim()
}

export function formatFailureItem (error = {}, separator = ' · ') {
    return [error.taskName, error.errorCode, error.errorMsg]
        .map(value => (value === undefined || value === null || value === '' ? '' : String(value).trim()))
        .filter(Boolean)
        .join(separator)
}

export function joinFailureItems (errors = [], separator = ' · ') {
    const list = Array.isArray(errors) ? errors : [errors]
    return list.map(error => formatFailureItem(error, separator)).filter(Boolean)
}

export function summarizePipelineStructure (pipeline) {
    const stages = pipeline?.stages || []
    return stages.map((stage) => {
        const containers = stage.containers || []
        if (containers[0]?.['@type'] === 'trigger') return ''
        const jobs = containers.map((container) => {
            const plugins = (container.elements || [])
                .map(element => element?.name)
                .filter(Boolean)
            const jobName = container.name || ''
            if (!jobName && !plugins.length) return ''
            return plugins.length ? `${jobName}（${plugins.join(' / ')}）` : jobName
        }).filter(Boolean)
        const stageName = stage.name || ''
        if (!stageName && !jobs.length) return ''
        return `${stageName}: ${jobs.join('；')}`
    }).filter(Boolean).join('\n')
}

export function requestAiAnalysis (error, prompt, context = {}, options = {}) {
    if (!window.parent || window.parent === window) return
    const attachment = normalizeAiAttachment(options.attachment)
    window.parent.postMessage({
        action: AI_ANALYSIS_ACTION,
        params: {
            prompt,
            error: normalizeAiError(error),
            context: normalizeAiContext(context),
            autoSend: options.autoSend !== false,
            ...(attachment ? { attachment } : {})
        }
    }, '*')
}
