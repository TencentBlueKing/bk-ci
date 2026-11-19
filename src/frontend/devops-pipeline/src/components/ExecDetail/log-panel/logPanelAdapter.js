import { parseLogLine, renderFormattedLogHtml } from './logLineFormat.js'

const LOG_LEVEL_ORDER = ['INFO', 'WARN', 'ERROR', 'DEBUG']

const LIVE_STATUSES = new Set([
    'RUNNING',
    'REVIEWING',
    'PREPARE_ENV',
    'QUEUE',
    'DEPENDENT_WAITING',
    'LOOP_WAITING',
    'CALL_WAITING',
])
const ROUND_SKIPPED_STATUSES = new Set([
    'SUCCEED', 'STAGE_SUCCESS', 'FAILED', 'CANCELED', 'TERMINATE', 'SKIP',
    'REVIEW_ABORT', 'REVIEW_PROCESSED', 'QUALITY_CHECK_PASS', 'QUALITY_CHECK_FAIL',
    'QUOTA_FAILED', 'QUEUE_TIMEOUT', 'EXEC_TIMEOUT', 'HEARTBEAT_TIMEOUT'
])
const ISO_TIMESTAMP_PATTERN = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.(\d{1,3}))?(Z|[+-]\d{2}:\d{2})$/

const ESC = String.fromCharCode(27)
const FULL_ANSI_PATTERN = new RegExp(`(?:${ESC}|\\u009b)\\[([0-?]*[ -/]*[@-~])`, 'g')
const BARE_SGR_PATTERN = /\[(?:\d+;)*\d+m/g
const ANSI_FOREGROUND = {
    30: '#000000', 31: '#ff6666', 32: '#7fca54', 33: '#ff9c01',
    34: '#5fb4e1', 35: '#ff00ff', 36: '#00ffff', 37: '#ffffff', 90: '#83828c'
}

export function isLiveElapsedStatus (status) {
    return LIVE_STATUSES.has(String(status || '').toUpperCase())
}

export function isTaskElapsedStatus (status) {
    return ['RUNNING', 'REVIEWING', 'PREPARE_ENV'].includes(String(status || '').toUpperCase())
}

export function normalizeLogLevels (levels = []) {
    const selected = new Set((Array.isArray(levels) ? levels : []).map(level => String(level || '').toUpperCase()).filter(Boolean))
    return LOG_LEVEL_ORDER.filter(level => selected.has(level))
}

export function findLogMatchIds (logs = [], keyword = '') {
    const search = String(keyword || '').toLowerCase()
    if (!search) return []
    return logs.filter(log => getLogText(log).toLowerCase().includes(search)).map(log => log.lineNo ?? log.line)
}

export function resolveNextLogMatchIndex (matchCount, currentMatch, step = 1) {
    const total = Math.max(0, Math.floor(Number(matchCount) || 0))
    if (!total) return 0
    const direction = Number(step) < 0 ? -1 : 1
    const current = Number(currentMatch)
    const currentIndex = Number.isFinite(current) && current >= 1
        ? Math.min(Math.floor(current), total) - 1
        : direction < 0 ? 0 : -1
    return ((currentIndex + direction + total) % total) + 1
}

export function shouldApplyLogSearchScroll ({ generation, currentGeneration, search, currentSearch, target, currentTarget, matchIds = [] } = {}) {
    if (generation !== currentGeneration || !search || search !== currentSearch) return false
    if (target === null || target === undefined || target === '' || String(target) !== String(currentTarget)) return false
    return matchIds.some(lineNo => String(lineNo) === String(target))
}

export function shouldContinueSearchBackfill ({ searching = false, stopped = false, hasBefore = false, succeeded = false, addedCount = 0, previousCursor, nextCursor } = {}) {
    if (!searching || stopped || !hasBefore || !succeeded) return false
    if (Number(addedCount) > 0) return true
    const previous = Number(previousCursor)
    const next = Number(nextCursor)
    return Number.isSafeInteger(previous) && Number.isSafeInteger(next) && next < previous
}

export function serializeLogLevels (levels = []) {
    const normalized = normalizeLogLevels(levels)
    return normalized.length === LOG_LEVEL_ORDER.length ? LOG_LEVEL_ORDER.join(',') : normalized.join(',')
}

export function formatLevelSummary (levels = [], allSelected) {
    const selected = normalizeLogLevels(levels)
    if (allSelected === true || (allSelected === undefined && selected.length === LOG_LEVEL_ORDER.length)) return 'ALL'
    if (selected.length <= 1) return selected[0] || ''
    if (selected.join(',') === 'INFO,WARN,ERROR') return selected.join(',')
    if (selected.length === LOG_LEVEL_ORDER.length) return selected.join(',')
    return `${selected[0]} +${selected.length - 1}`
}

export function shouldShowLogMinimap ({ lineCount = 0 } = {}) {
    return Number(lineCount) > 0
}

export function shouldShowProgressTab ({ data, percent, subtasks = [], error = false } = {}) {
    if (error) return false
    return percent !== null && percent !== undefined
        || Number.isFinite(Number(data?.progress?.value))
        || (Array.isArray(subtasks) && subtasks.length > 0)
}

export function shouldLoadStepLog ({ status } = {}) {
    return !['UNEXEC', 'WAITING', 'DISABLED'].includes(String(status || '').toUpperCase())
}

export function shouldMountLogPanel ({ isHistoricalExecuteCount = false, historyRecordState = 'idle', status } = {}) {
    if (!isHistoricalExecuteCount) return shouldLoadStepLog({ status })
    if (historyRecordState === 'ready') return shouldLoadStepLog({ status })
    return historyRecordState === 'unavailable'
}

export function normalizeLogPanelResponse (response, { strictLatest = false } = {}) {
    if (strictLatest) {
        const invalid = () => {
            throw new Error('Invalid latest log response')
        }
        let data = response
        let inheritedStatus
        if (!data || typeof data !== 'object' || Array.isArray(data)) return invalid()
        while (Object.prototype.hasOwnProperty.call(data, 'data')) {
            if (!data.data || typeof data.data !== 'object' || Array.isArray(data.data)) return invalid()
            if (Object.prototype.hasOwnProperty.call(data, 'status') || Object.prototype.hasOwnProperty.call(data, 'code')) {
                const wrapperStatus = Object.prototype.hasOwnProperty.call(data, 'status') ? data.status : data.code
                const transportStatus = (typeof wrapperStatus === 'number' || typeof wrapperStatus === 'string')
                    && Number.isInteger(Number(wrapperStatus)) && Number(wrapperStatus) >= 200 && Number(wrapperStatus) < 300
                if (!transportStatus) inheritedStatus = wrapperStatus
            }
            data = data.data
        }
        if (!data || typeof data !== 'object' || Array.isArray(data)) return invalid()
        const hasStatus = Object.prototype.hasOwnProperty.call(data, 'status') || Object.prototype.hasOwnProperty.call(data, 'code')
        const rawStatus = hasStatus ? (Object.prototype.hasOwnProperty.call(data, 'status') ? data.status : data.code) : inheritedStatus
        let status
        if (rawStatus === undefined) {
            if (Array.isArray(data.logs)) status = 0
            else if (data.cleaned === true || data.emptyStatus === true) status = 2
            else return invalid()
        } else {
            if (typeof rawStatus !== 'number' && typeof rawStatus !== 'string') return invalid()
            if (typeof rawStatus === 'string' && !/^\d+$/.test(rawStatus.trim())) return invalid()
            status = Number(rawStatus)
            if (!Number.isInteger(status) || ![0, 1, 2, 3, 999].includes(status)) return invalid()
        }
        if (data.logs !== undefined && !Array.isArray(data.logs)) return invalid()
        if (status === 0 && !Array.isArray(data.logs) && data.cleaned !== true && data.emptyStatus !== true) return invalid()
        const cursor = data.cursor && typeof data.cursor === 'object' && !Array.isArray(data.cursor) ? data.cursor : {}
        return {
            status,
            message: data.message || '',
            logs: Array.isArray(data.logs) ? data.logs : [],
            subTags: Array.isArray(data.subTags) ? data.subTags : [],
            cursor,
            hasBefore: typeof data.hasBefore === 'boolean' ? data.hasBefore : Boolean(cursor.hasBefore),
            hasAfter: typeof data.hasAfter === 'boolean' ? data.hasAfter : Boolean(cursor.hasAfter),
            finished: data.finished !== false,
            clean: status === 2 || status === 3 || data.cleaned === true || data.emptyStatus,
            startLineNo: data.startLineNo ?? cursor.startLineNo,
            endLineNo: data.endLineNo ?? cursor.endLineNo,
            matchedTotal: data.matchedTotal,
            levels: data.levels
        }
    }
    const payload = response?.data ?? response ?? {}
    const data = payload.data && !Array.isArray(payload.data) ? payload.data : payload
    const status = Number(data.status ?? data.code ?? 0)
    const cursor = data.cursor || {}
    return {
        status,
        message: data.message || '',
        logs: Array.isArray(data.logs) ? data.logs : [],
        subTags: Array.isArray(data.subTags) ? data.subTags : [],
        cursor,
        hasBefore: typeof data.hasBefore === 'boolean' ? data.hasBefore : Boolean(cursor.hasBefore),
        hasAfter: typeof data.hasAfter === 'boolean' ? data.hasAfter : Boolean(cursor.hasAfter),
        finished: data.finished !== false,
        clean: status === 2 || status === 3 || data.cleaned === true || data.emptyStatus,
        startLineNo: data.startLineNo ?? cursor.startLineNo,
        endLineNo: data.endLineNo ?? cursor.endLineNo,
        matchedTotal: data.matchedTotal,
        levels: data.levels,
    }
}

export function isPermissionError (error) {
    const httpStatus = Number(error?.response?.status || error?.httpStatus || error?.status)
    return httpStatus === 403 || Number(error?.code) === 403
}

export function resolveLogLoadErrorKey (error) {
    const status = Number(error?.response?.status || error?.httpStatus || error?.status || error?.code)
    return status === 404 || status === 501 ? 'logPanel.loadUnavailable' : 'logPanel.loadError'
}

function isSafeNonNegativeInteger (value) {
    if (value === null || value === undefined) return false
    return Number.isSafeInteger(value) && value >= 0
}

export function latestDisplayBase (matchedTotal, pageLength) {
    const total = Number(matchedTotal)
    return Number.isFinite(total)
        ? Math.max(1, total - pageLength + 1)
        : 1
}

export function shouldBackfillSmallLog (session = {}) {
    if (!session.finished || session.hasAfter || !session.hasBefore) return false
    const { matchedTotal, loadedCount } = session
    if (!isSafeNonNegativeInteger(matchedTotal) || !isSafeNonNegativeInteger(loadedCount)) return false
    if (matchedTotal > 3000) return false
    return loadedCount < Math.min(matchedTotal, 3000)
}

export function shouldReloadOnLevelsChange ({ suppressLevelsReload = false } = {}) {
    return !suppressLevelsReload
}

export function shouldContinueAutoBackfillAfterBefore ({ succeeded = false, addedCount = 0 } = {}) {
    return succeeded && addedCount > 0
}

export function resolveCleanResponseMessage (resultMessage) {
    return String(resultMessage ?? '')
}

export function resolveCleanStatusFallbackKey (cleanStatus) {
    if (cleanStatus === 'clean' || cleanStatus === 'closed') return 'logExpired'
    if (cleanStatus === 'empty') return 'empty'
    return ''
}

export function resolveCleanStatusText (serverMessage, cleanStatus, localizedFallback = '') {
    if (!cleanStatus) return ''
    return serverMessage || localizedFallback
}

export function shouldShowStatusEmptyLine (cleanStatus, emptyLineText) {
    return cleanStatus === 'empty' && Boolean(String(emptyLineText ?? '').trim())
}

const STATUS_MESSAGE_KEYS = {
    UNEXEC: 'notExecuted',
    SKIP: 'notExecuted',
    PAUSE: 'pausedBeforeRun',
    REVIEWING: 'reviewWaiting',
    HEARTBEAT_TIMEOUT: 'heartbeatTimeout',
    WAITING: 'waiting',
    DEPENDENT_WAITING: 'waiting',
    LOOP_WAITING: 'waiting',
    CALL_WAITING: 'waiting',
    QUEUE: 'queueWaiting',
}

export function resolveStatusMessageKey (status) {
    return STATUS_MESSAGE_KEYS[String(status || '').toUpperCase()] || ''
}

export function resolveConclusionStatusMessageKey (element = {}, context = {}) {
    const message = context.message ?? resolveConclusionMessage(element)
    if (message) return ''
    const status = String(element.status || context.status || '').toUpperCase()
    return resolveStatusMessageKey(status)
}

export function resolveConclusionDisplayMessage (conclusion = {}, translate = key => key) {
    if (conclusion.message) return conclusion.message
    const key = conclusion.statusMessageKey
    if (!key) return ''
    const localeKey = `logPanel.status.${key}`
    const translated = translate(localeKey)
    return translated === localeKey ? '' : translated
}

const ERROR_TYPE_KEYS = {
    0: 'system', 1: 'user', 2: 'thirdParty', 3: 'plugin', 4: 'container',
    SYSTEM: 'system', USER: 'user', THIRD_PARTY: 'thirdParty', THIRD_PARTY_ERROR: 'thirdParty',
    PLUGIN: 'plugin', CONTAINER: 'container', BUILD_MACHINE: 'container'
}

export function resolveLogErrorTypeKey (errorType) {
    return ERROR_TYPE_KEYS[String(errorType ?? '').toUpperCase()] || ERROR_TYPE_KEYS[errorType] || ''
}

export function stripAnsiForDisplay (value) {
    const text = String(value ?? '')
    const output = []
    let pendingOsc = null
    let index = 0

    while (index < text.length) {
        const code = text.charCodeAt(index)
        const next = text.charCodeAt(index + 1)
        const isEscOsc = code === 0x1b && next === 0x5d
        const isC1Osc = code === 0x9d

        if (pendingOsc) {
            if (code === 0x07 || code === 0x9c || (code === 0x1b && next === 0x5c)) {
                pendingOsc = null
                index += code === 0x1b ? 2 : 1
                continue
            }
            if (isEscOsc || isC1Osc) {
                output.push(pendingOsc.join(''))
                pendingOsc = []
                index += isEscOsc ? 2 : 1
                continue
            }
            pendingOsc.push(text[index])
            index += 1
            continue
        }

        if (isEscOsc || isC1Osc) {
            pendingOsc = []
            index += isEscOsc ? 2 : 1
            continue
        }

        if ((code === 0x1b && next === 0x5b) || code === 0x9b) {
            const prefixLength = code === 0x9b ? 1 : 2
            let end = index + prefixLength
            while (end < text.length) {
                const current = text.charCodeAt(end)
                if (current >= 0x40 && current <= 0x7e) break
                if (current < 0x20 || current > 0x3f) break
                end += 1
            }
            if (end < text.length && text.charCodeAt(end) >= 0x40 && text.charCodeAt(end) <= 0x7e) {
                index = end + 1
            } else {
                index += prefixLength
            }
            continue
        }

        if (code === 0x1b) {
            let end = index + 1
            while (end < text.length && text.charCodeAt(end) >= 0x20 && text.charCodeAt(end) <= 0x2f) end += 1
            if (end < text.length && text.charCodeAt(end) >= 0x30 && text.charCodeAt(end) <= 0x7e) index = end + 1
            else index += 1
            continue
        }

        if (code >= 0x80 && code <= 0x9f) {
            index += 1
            continue
        }

        output.push(text[index])
        index += 1
    }

    if (pendingOsc) output.push(pendingOsc.join(''))
    return output.join('').replace(/\\(?:u001b|x1b)\[[0-?]*[ -/]*[@-~]/gi, '')
}

export function resolveLogErrorSummary (errorInfoList = [], task = {}, fallback = {}) {
    const taskId = task.id ?? task.taskId
    const containerId = task.containerId ?? task.jobId
    const matched = (Array.isArray(errorInfoList) ? errorInfoList : []).find(error => {
        const errorTaskId = error.taskId ?? error.elementId
        const errorContainerId = error.containerId ?? error.jobId
        return String(errorTaskId ?? '') === String(taskId ?? '') && String(errorContainerId ?? '') === String(containerId ?? '')
    })
    const source = matched || fallback
    if (!source || !(source.errorType !== undefined || source.errorCode || source.errorMsg)) return null
    return {
        errorType: source.errorType,
        errorCode: source.errorCode || '',
        errorMsg: stripAnsiForDisplay(source.errorMsg || source.errorDetail || source.detail || '')
    }
}

export function resolveNodeLogError (errorInfoList = [], task = {}, node = {}) {
    const taskId = task.id ?? task.taskId
    const containerId = task.containerId ?? task.jobId
    const matched = (Array.isArray(errorInfoList) ? errorInfoList : []).find(error => {
        const errorTaskId = error.taskId ?? error.elementId
        const errorContainerId = error.containerId ?? error.jobId
        return String(errorTaskId ?? '') === String(taskId ?? '') && String(errorContainerId ?? '') === String(containerId ?? '')
    }) || {}
    const nodeMessage = node.errorMsg || node.errorDetail || node.detail || ''
    const matchedMessage = matched.errorMsg || matched.errorDetail || matched.detail || ''
    const message = taskId === ''
        ? nodeMessage || matchedMessage
        : matchedMessage || nodeMessage
    const errorCode = node.errorCode || matched.errorCode || ''
    const errorType = node.errorType ?? matched.errorType
    if (!message && !errorCode && errorType === undefined) return null
    return { errorType, errorCode, errorMsg: normalizeJobSummaryMessage(message) }
}

function isExplicitAutoContinue (step = {}) {
    const options = step.additionalOptions || {}
    if (options.manualSkip !== false) return false
    const hasBoolean = typeof options.continueWhenFailed === 'boolean'
    const hasFailControl = Array.isArray(options.failControl)
    const listedForContinue = hasFailControl && options.failControl.includes('continueWhenFailed')
    if (hasBoolean && hasFailControl) return options.continueWhenFailed && listedForContinue
    return hasBoolean ? options.continueWhenFailed : listedForContinue
}

export function resolveJobFailureStep (steps = [], errorInfoList = [], job = {}) {
    const jobStatus = String(job.status || '').toUpperCase()
    if (!Array.isArray(steps) || !steps.length || jobStatus !== 'FAILED') return null
    return steps.find(step => {
        const status = String(step.status || '').toUpperCase()
        if (status === 'SKIP' || !['FAILED', 'EXEC_TIMEOUT', 'HEARTBEAT_TIMEOUT', 'QUALITY_CHECK_FAIL', 'REVIEW_ABORT', 'TERMINATE'].includes(status)) return false
        if (status !== 'TERMINATE' && isExplicitAutoContinue(step)) return false
        if (status !== 'FAILED') {
            const error = resolveNodeLogError(errorInfoList, { id: step.id, containerId: job.id }, step)
            if (!error || !(error.errorCode || error.errorMsg)) return false
            if (status === 'TERMINATE') {
                if (isJobTimeoutTerminatedStep(status, error.errorCode, errorInfoList, { id: step.id, containerId: job.id })) return false
                if (isSystemAgentTermination(status, error.errorType, error.errorCode, errorInfoList, { id: step.id, containerId: job.id })) return false
            }
        }
        return true
    }) || null
}

export function isJobTimeoutTerminatedStep (status, errorCode, errorInfoList, task) {
    if (String(status || '').toUpperCase() !== 'TERMINATE') return false
    if (Number(errorCode) === 2199005) return true
    const matchingError = resolveLogErrorSummary(errorInfoList, task, {})
    return Number(matchingError?.errorCode) === 2199005
}

export function isSystemAgentTermination (status, errorType, errorCode, errorInfoList, task) {
    if (String(status || '').toUpperCase() !== 'TERMINATE') return false
    if ((String(errorType || '').toUpperCase() === 'BUILD_MACHINE' || Number(errorType) === 4) && Number(errorCode) === 2199502) return true
    const matchingError = resolveLogErrorSummary(errorInfoList, task, {})
    return (String(matchingError?.errorType || '').toUpperCase() === 'BUILD_MACHINE' || Number(matchingError?.errorType) === 4) && Number(matchingError?.errorCode) === 2199502
}

// Job 执行超时由引擎终止运行中的步骤，Job 以 TERMINATE/FAILED 收尾，超时码可能只落在 Job、错误列表或被终止的步骤上
export function resolveJobExecTimeoutCode (job = {}, errorInfoList = []) {
    if (!['FAILED', 'TERMINATE'].includes(String(job?.status || '').toUpperCase())) return ''
    const jobId = String(job.id ?? '')
    const codes = [
        job.errorCode,
        ...(Array.isArray(errorInfoList) ? errorInfoList : [])
            .filter(error => String(error?.containerId ?? error?.jobId ?? '') === jobId)
            .map(error => error.errorCode),
        ...(Array.isArray(job.elements) ? job.elements : [])
            .filter(step => String(step?.status || '').toUpperCase() === 'TERMINATE')
            .map(step => step.errorCode)
    ]
    return codes.find(code => Number(code) === 2199005) ?? ''
}

// Agent 失联只把运行中的步骤置为 TERMINATE，Job 自身以 FAILED 收尾
export function isJobSystemAgentTermination (job = {}, errorInfoList = []) {
    if (!['FAILED', 'TERMINATE'].includes(String(job?.status || '').toUpperCase())) return false
    if (isSystemAgentTermination(job.status, job.errorType, job.errorCode, errorInfoList, { id: '', containerId: job.id })) return true
    return (Array.isArray(job.elements) ? job.elements : []).some(step => isSystemAgentTermination(
        step?.status,
        step?.errorType,
        step?.errorCode,
        errorInfoList,
        { id: step?.id, containerId: job.id }
    ))
}

export function getRawLogText (log) {
    return String(log?.text ?? log?.content ?? log?.message ?? '')
}

export function getLogText (log) {
    return parseLogLine(log).plainText
}

export function getParsedLogLine (log) {
    return parseLogLine(log)
}

export function getLogIssueLevel (line) {
    const level = String(line?.level || '').toLowerCase()
    if (level === 'error') return 'error'
    const marker = parseLogLine(line).marker?.type
    if (marker === 'error') return 'error'
    if (marker === 'warning' || marker === 'warn') return 'warn'
    return level === 'warn' || level === 'warning' ? 'warn' : ''
}

const LOCATOR_TEMPLATE_WORDS = new Set([
    'a', 'an', 'and', 'are', 'as', 'at', 'by', 'command', 'error', 'errors', 'faq', 'failed', 'failure',
    'for', 'from', 'help', 'how', 'in', 'message', 'of', 'on', 'or', 'please', 'script', 'see', 'support',
    'the', 'to', 'tracking', 'unknown', 'was', 'with'
])

export function normalizeLocateText (value) {
    return stripAnsiForDisplay(value)
        .replace(/<br\s*\/?>/gi, '\n')
        .replace(/<\/?(?:a|abbr|b|blockquote|code|dd|div|dl|dt|em|h[1-6]|i|li|ol|p|pre|s|small|span|strong|sub|sup|u|ul)\b[^>]*>/gi, '')
        .replace(/&(#x[\da-f]+|#\d+|amp|lt|gt|quot|apos|nbsp);/gi, (entity, name) => {
            const normalized = name.toLowerCase()
            if (normalized === 'amp') return '&'
            if (normalized === 'lt') return '<'
            if (normalized === 'gt') return '>'
            if (normalized === 'quot') return '"'
            if (normalized === 'apos') return "'"
            if (normalized === 'nbsp') return ' '
            const codePoint = normalized.startsWith('#x')
                ? parseInt(normalized.slice(2), 16)
                : parseInt(normalized.slice(1), 10)
            return Number.isFinite(codePoint) && codePoint >= 0 && codePoint <= 0x10ffff
                ? String.fromCodePoint(codePoint)
                : entity
        })
        .replace(/\r\n?/g, '\n')
        .split('\n')
        .map(line => line.replace(/^\s*##\[(?:error|warning|warn)\]\s*/i, '').replace(/[ \t]+/g, ' ').trim())
        .filter(Boolean)
        .join('\n')
}

export function normalizeJobSummaryMessage (value) {
    return normalizeLocateText(parseLogLine(value).plainText)
        .replace(/<\s*\/?\s*a\b[^>]*(?:>|$)/gi, '')
        .trim()
}

function locateTextInformation (value) {
    const text = String(value || '').trim()
    if (text.length < 16 || /^(?:0x[\da-f]+|[a-z]{0,8}[-_]?\d{3,})$/i.test(text)) return 0
    const words = text.match(/[a-z][a-z0-9_./:-]*/gi) || []
    const latinInformation = words.reduce((count, word) => count + (word.length > 2 && !LOCATOR_TEMPLATE_WORDS.has(word.toLowerCase()) ? 1 : 0), 0)
    const cjkCharacters = text.match(/[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff\u3040-\u30ff\uac00-\ud7af]/g) || []
    const cjkInformation = Math.floor(cjkCharacters.length / 2)
    return Math.max(latinInformation, cjkInformation)
}

function locateCandidates (messages = []) {
    const candidates = new Map()
    messages.forEach((message, sourceIndex) => {
        const normalized = normalizeLocateText(message)
        if (!normalized) return
        const bodies = [normalized.replace(/\n+/g, ' '), ...normalized.split('\n')]
        bodies.forEach((text, candidateIndex) => {
            const info = locateTextInformation(text)
            if (info < 2) return
            const existing = candidates.get(text)
            const candidate = { text, info, sourceIndex, rank: candidateIndex === 0 ? 4 : 3 }
            if (!existing || candidate.rank > existing.rank || (candidate.rank === existing.rank && sourceIndex < existing.sourceIndex)) candidates.set(text, candidate)
        })
    })
    return [...candidates.values()]
}

export function resolveLocateLogTarget (logs = [], messages = []) {
    const candidates = locateCandidates(Array.isArray(messages) ? messages : [messages])
    if (!candidates.length) {
        const line = logs.find(log => getLogIssueLevel(log) === 'error') || logs.find(log => getLogIssueLevel(log) === 'warn')
        return { hasTarget: false, lineNo: line?.lineNo ?? line?.line ?? null, miss: false }
    }

    let best = null
    logs.forEach((log, logIndex) => {
        const text = normalizeLocateText(getRawLogText(log)).replace(/\n+/g, ' ')
        const logInfo = locateTextInformation(text)
        if (!text || !logInfo) return
        const level = getLogIssueLevel(log)
        candidates.forEach(candidate => {
            let rank = 0
            if (text === candidate.text) rank = candidate.rank
            else if (text.includes(candidate.text)) rank = 2
            else if (candidate.text.includes(text)) rank = 1
            if (!rank || (level !== 'error' && level !== 'warn' && rank < 2)) return
            const levelPriority = level === 'error' ? 3 : level === 'warn' ? 2 : 1
            const score = [levelPriority, rank, -candidate.sourceIndex, candidate.info, candidate.text.length, logIndex]
            let isBetter = !best
            if (best) {
                for (let scoreIndex = 0; scoreIndex < score.length; scoreIndex += 1) {
                    if (score[scoreIndex] === best.score[scoreIndex]) continue
                    isBetter = score[scoreIndex] > best.score[scoreIndex]
                    break
                }
            }
            if (isBetter) {
                best = { score, lineNo: log.lineNo ?? log.line ?? null }
            }
        })
    })
    const lineNo = best?.lineNo ?? null
    return { hasTarget: true, lineNo, miss: lineNo === null }
}

export function sampleLogIssueMarks (logs = [], cap = 400) {
    const total = logs.length
    if (!total) return []

    const issues = []
    logs.forEach((line, index) => {
        const level = getLogIssueLevel(line)
        const counts = line?._logGroup?.issueCounts || {}
        const levels = new Set([level, Number(counts.error) > 0 ? 'error' : '', Number(counts.warn) > 0 ? 'warn' : ''].filter(Boolean))
        levels.forEach(issueLevel => issues.push({
            index,
            level: issueLevel,
            hiddenCount: Number(counts[issueLevel]) || 0
        }))
    })
    if (!issues.length) return []

    const limit = Math.max(Number(cap) || 0, 1)
    if (issues.length <= limit) {
        return issues.map(({ index, level, hiddenCount }) => ({
            key: `${level}-${index}`,
            cls: `is-${level}`,
            index,
            hiddenCount,
            top: (index / total) * 100
        }))
    }

    const mustInclude = new Set()
    const lastErrorPos = [...issues.entries()].reverse().find(([, item]) => item.level === 'error')?.[0]
    if (lastErrorPos !== undefined) mustInclude.add(lastErrorPos)
    const firstErrorPos = issues.findIndex(item => item.level === 'error')
    if (firstErrorPos >= 0) mustInclude.add(firstErrorPos)

    const picked = new Set(mustInclude)
    const remaining = limit - picked.size
    if (remaining > 0) {
        const candidates = issues.map((_, pos) => pos).filter(pos => !picked.has(pos))
        if (candidates.length <= remaining) {
            candidates.forEach(pos => picked.add(pos))
        } else {
            for (let i = 0; i < remaining; i++) {
                picked.add(candidates[Math.round(i * (candidates.length - 1) / Math.max(remaining - 1, 1))])
            }
        }
    }

    return [...picked].sort((left, right) => left - right).slice(0, limit).map(pos => {
        const { index, level } = issues[pos]
        return {
            key: `${level}-${index}`,
            cls: `is-${level}`,
            index,
            hiddenCount: issues[pos].hiddenCount,
            top: (index / total) * 100
        }
    })
}

function estimateMinimapLineHeight (text, { textWidth = 186, fontSize = 4, lineHeight = 8 } = {}) {
    const lines = String(text ?? '').replace(/\r\n?/g, '\n').split('\n')
    const widthOf = line => Array.from(line).reduce((width, char) => {
        const wide = /[\u1100-\u11ff\u2e80-\u9fff\uf900-\ufaff\uac00-\ud7af\uff01-\uff60]/u.test(char)
        return width + fontSize * (wide ? 1 : 0.6)
    }, 0)
    return Math.max(1, lines.reduce((total, line) => total + Math.max(1, Math.ceil(widthOf(line) / textWidth)), 0)) * lineHeight
}

function resolveMinimapSampleIndexes (logs, sampleCount) {
    if (sampleCount >= logs.length) return logs.map((_, index) => index)
    if (sampleCount <= 1) return [0]
    return Array.from({ length: sampleCount }, (_, slot) => Math.round(slot * (logs.length - 1) / (sampleCount - 1)))
}

export function resolveLogMinimapSampleIndexes (logs = [], sampleCount = 240) {
    return resolveMinimapSampleIndexes(logs, sampleCount)
}

function clampValue (value, min, max) {
    return Math.max(min, Math.min(max, Number(value) || 0))
}

export function resolveLogMinimapVisualMetrics (scale, sourceHeight = 20) {
    const projectedScale = Math.max(0, Number(scale) || 0)
    const firstVisualHeight = Math.min(20, Math.max(0, Number(sourceHeight) || 0))
    const period = 20 * projectedScale
    const firstCenterPx = projectedScale * firstVisualHeight / 2
    const thickness = Math.min(1.5, period / 3, firstCenterPx * 2)
    return {
        scale: projectedScale,
        period,
        thickness,
        firstVisualHeight,
        firstCenterPx,
        firstStartPx: firstCenterPx - thickness / 2,
        firstEndPx: firstCenterPx + thickness / 2,
        markerDiameter: Math.min(6, period * 0.75)
    }
}

function createMinimapMapping ({ logs, heightIndex, trackHeight, lineOrigin, scrollMetrics }) {
    const track = Math.max(0, Number(trackHeight) || 0)
    const viewportHeight = Math.max(0, Number(scrollMetrics?.clientHeight) || 0)
    const contentHeight = Math.max(0, Number(scrollMetrics?.scrollHeight ?? scrollMetrics?.contentHeight) || 0)
    if (!logs.length || !heightIndex || heightIndex.size !== logs.length || track <= 0 || viewportHeight <= 0 || contentHeight <= 0) {
        return { interactive: false, rows: [], marks: [], trackHeight: track, contentHeight: 0 }
    }

    const sourceHeight = contentHeight
    const maxScroll = Math.max(sourceHeight - viewportHeight, 0)
    const scrollTop = clampValue(scrollMetrics.scrollTop, 0, maxScroll)
    const ratio = maxScroll > 0 ? scrollTop / maxScroll : 0
    const minThumb = Math.min(32, track / 2)
    const fittedThumb = clampValue(0.4 * viewportHeight, minThumb, track / 2)
    const scale = fittedThumb / viewportHeight
    const contentHeightPx = Math.min(track, scale * sourceHeight)
    const sourceWindowHeight = contentHeightPx / scale
    const sourceWindowTop = ratio * Math.max(sourceHeight - sourceWindowHeight, 0)
    const sourceWindowEnd = Math.min(sourceHeight, sourceWindowTop + sourceWindowHeight)
    const viewportHeightPx = Math.min(contentHeightPx, scale * viewportHeight)
    const viewportTopPx = ratio * Math.max(contentHeightPx - viewportHeightPx, 0)

    const indexStart = heightIndex.findIndexByOffset(Math.max(0, sourceWindowTop - lineOrigin))
    const indexEnd = heightIndex.findEndIndex(Math.max(0, sourceWindowEnd - lineOrigin))
    const firstIndex = Math.max(0, indexStart - 1)
    const endIndex = Math.min(logs.length, indexEnd + 1)
    const rows = []
    const marks = []
    let sourceTop = lineOrigin + heightIndex.prefix(firstIndex)
    for (let index = firstIndex; index < endIndex; index++) {
        const sourceHeight = heightIndex.heights[index] || heightIndex.defaultHeight
        if (sourceTop + sourceHeight > sourceWindowTop && sourceTop < sourceWindowEnd) {
            const visualMetrics = resolveLogMinimapVisualMetrics(scale, sourceHeight)
            const row = {
                line: logs[index],
                index,
                sourceTop,
                sourceHeight,
                firstVisualHeight: visualMetrics.firstVisualHeight,
                topPx: scale * (sourceTop - sourceWindowTop),
                heightPx: scale * sourceHeight
            }
            rows.push(row)
            const groupCounts = row.line?._logGroup?.issueCounts || {}
            const issueLevels = new Set([
                getLogIssueLevel(row.line),
                Number(groupCounts.error) > 0 ? 'error' : '',
                Number(groupCounts.warn) > 0 ? 'warn' : ''
            ].filter(Boolean))
            const markSourceTop = sourceTop + visualMetrics.firstVisualHeight / 2
            issueLevels.forEach(level => {
                if (markSourceTop < sourceWindowTop || markSourceTop >= sourceWindowEnd) return
                marks.push({
                    key: `${level}-${index}`,
                    index,
                    cls: `is-${level}`,
                    hiddenCount: Number(groupCounts[level]) || 0,
                    topPx: visualMetrics.scale * (markSourceTop - sourceWindowTop),
                    firstVisualHeight: visualMetrics.firstVisualHeight,
                    isLocalTop: true
                })
            })
        }
        sourceTop += sourceHeight
    }

    return {
        interactive: maxScroll > 0,
        trackHeight: track,
        sourceHeight,
        viewportHeight,
        scrollTop,
        maxScroll,
        ratio,
        scale,
        contentHeightPx,
        sourceWindowHeight,
        sourceWindowTop,
        sourceWindowEnd,
        viewportHeightPx,
        viewportTopPx,
        rows,
        marks
    }
}

export function resolveLogMinimapTarget (layout = {}, y = 0) {
    const mapping = layout.mapping
    const heightIndex = layout.heightIndex
    if (!mapping || !heightIndex?.size || !mapping.scale) return null
    const sourcePosition = clampValue(mapping.sourceWindowTop + clampValue(y, 0, mapping.contentHeightPx) / mapping.scale, 0, mapping.sourceHeight)
    const index = heightIndex.findIndexByOffset(clampValue(sourcePosition - layout.lineOrigin, 0, heightIndex.total()))
    const sourceTop = layout.lineOrigin + heightIndex.prefix(index)
    const sourceHeight = heightIndex.heights[index] || heightIndex.defaultHeight
    const localOffset = clampValue(sourcePosition - sourceTop, 0, sourceHeight)
    return {
        index,
        key: heightIndex.keys[index],
        line: layout.logs?.[index],
        sourcePosition,
        localOffset,
        targetScrollTop: clampValue(sourcePosition - mapping.viewportHeight / 2, 0, mapping.maxScroll)
    }
}

export function resolveLogMinimapLayout (logs = [], { textWidth = 186, fontSize = 4, lineHeight = 8, previewCap = 240, trackHeight = 0, heightIndex = null, indexVersion = 0, lineOrigin = 0, contentHeight = 0, scrollMetrics = null, sampleIndexes: suppliedSampleIndexes = null } = {}) {
    if (scrollMetrics) {
        const mapping = createMinimapMapping({ logs, heightIndex, trackHeight, lineOrigin, scrollMetrics })
        if (!mapping.scale) {
            return {
                logs, lineTops: [], lineHeights: [], totalHeight: 0, sourceHeight: 0,
                lineOrigin, previewTotalHeight: 0, previewRows: [], issueMarks: [],
                isSampled: false, isContinuousPreview: true, isInteractive: false,
                heightIndex, indexVersion, mapping: null
            }
        }
        return {
            logs,
            lineTops: [],
            lineHeights: [],
            totalHeight: mapping.sourceHeight,
            sourceHeight: mapping.sourceHeight,
            lineOrigin: Number(lineOrigin) || 0,
            previewTotalHeight: mapping.contentHeightPx,
            previewRows: mapping.rows,
            issueMarks: mapping.marks,
            isSampled: logs.length > mapping.rows.length,
            isContinuousPreview: true,
            isInteractive: mapping.interactive,
            heightIndex,
            indexVersion,
            mapping
        }
    }
    if (heightIndex && heightIndex.size === logs.length) {
        const totalHeight = heightIndex.total()
        const sourceHeight = Math.max(Number(contentHeight) || 0, totalHeight + (Number(lineOrigin) || 0) + 8)
        const isSampled = logs.length > previewCap || (trackHeight > 0 && sourceHeight > trackHeight)
        const sampleIndexes = suppliedSampleIndexes || (!isSampled
            ? logs.map((_, index) => index)
            : resolveMinimapSampleIndexes(logs, Math.min(Math.max(0, Math.floor(Number(previewCap) || 0)), logs.length)))
        const previewRows = sampleIndexes.map(index => {
            const sourceHeight = heightIndex.heights[index] || heightIndex.defaultHeight
            return {
                line: logs[index],
                index,
                topPx: (Number(lineOrigin) || 0) + heightIndex.prefix(index),
                heightPx: sourceHeight,
                firstVisualHeight: resolveLogMinimapVisualMetrics(1, sourceHeight).firstVisualHeight
            }
        })
        previewRows.forEach((row, index) => {
            row.nextTopPx = previewRows[index + 1]?.topPx
        })
        const issueMarks = sampleLogIssueMarks(logs, Math.min(400, previewCap)).map(mark => {
            const firstVisualHeight = resolveLogMinimapVisualMetrics(1, heightIndex.heights[mark.index] || heightIndex.defaultHeight).firstVisualHeight
            return {
                ...mark,
                key: `${mark.cls}-${mark.index}`,
                firstVisualHeight,
                topPx: (Number(lineOrigin) || 0) + heightIndex.prefix(mark.index) + firstVisualHeight / 2
            }
        })
        return {
            lineTops: [],
            lineHeights: [],
            totalHeight,
            sourceHeight,
            lineOrigin: Number(lineOrigin) || 0,
            previewTotalHeight: sourceHeight,
            previewRows,
            issueMarks,
            isSampled,
            heightIndex,
            indexVersion
        }
    }
    const lineTops = []
    const lineHeights = []
    let totalHeight = 0
    logs.forEach(line => {
        lineTops.push(totalHeight)
        const height = estimateMinimapLineHeight(getLogText(line), { textWidth, fontSize, lineHeight })
        lineHeights.push(height)
        totalHeight += height
    })

    const previewRows = []
    const sampleIndexes = []
    const isSampled = logs.length > previewCap || (trackHeight > 0 && totalHeight > trackHeight)
    if (!isSampled) {
        logs.forEach((line, index) => sampleIndexes.push(index))
    } else {
        const sampleCount = Math.min(Math.max(0, Math.floor(Number(previewCap) || 0)), logs.length)
        sampleIndexes.push(...resolveMinimapSampleIndexes(logs, sampleCount))
    }

    sampleIndexes.forEach(index => {
        previewRows.push({
            line: logs[index],
            index,
            topPx: lineTops[index],
            heightPx: lineHeights[index],
            firstVisualHeight: resolveLogMinimapVisualMetrics(1, Math.min(lineHeight, lineHeights[index])).firstVisualHeight
        })
    })
    previewRows.forEach((row, index) => {
        row.nextTopPx = previewRows[index + 1]?.topPx
    })

    const issueMarks = sampleLogIssueMarks(logs, Math.min(400, previewCap)).map(mark => {
        const firstVisualHeight = resolveLogMinimapVisualMetrics(1, Math.min(lineHeight, lineHeights[mark.index] || lineHeight)).firstVisualHeight
        return {
            ...mark,
            key: `${mark.cls}-${mark.index}`,
            firstVisualHeight,
            topPx: (lineTops[mark.index] || 0) + firstVisualHeight / 2
        }
    })
    return { lineTops, lineHeights, totalHeight, previewTotalHeight: totalHeight, previewRows, issueMarks, isSampled }
}

export function resolveLogMinimapCoordinates (layout = {}, viewport = {}, trackHeight = 0, scrollMetrics = null) {
    if (layout.mapping) {
        const mapping = layout.mapping
        return {
            interactive: mapping.interactive,
            contentHeightPx: mapping.contentHeightPx,
            contentOffsetPx: 0,
            scale: mapping.scale,
            hitScale: 1 / mapping.scale,
            viewportTopPx: mapping.viewportTopPx,
            viewportHeightPx: mapping.viewportHeightPx,
            visibleTopPx: mapping.viewportTopPx,
            visibleHeightPx: mapping.viewportHeightPx,
            trackHeightPx: mapping.trackHeight,
            sourceWindowTop: mapping.sourceWindowTop,
            maxScroll: mapping.maxScroll,
            panRatio: mapping.ratio,
            dragRange: Math.max(mapping.contentHeightPx - mapping.viewportHeightPx, 0)
        }
    }
    if (layout.isContinuousPreview) {
        return {
            interactive: false,
            contentHeightPx: 0,
            contentOffsetPx: 0,
            scale: 0,
            hitScale: 1,
            viewportTopPx: 0,
            viewportHeightPx: 0,
            visibleTopPx: 0,
            visibleHeightPx: 0,
            trackHeightPx: Math.max(0, Number(trackHeight) || 0),
            sourceWindowTop: 0,
            maxScroll: 0,
            panRatio: 0,
            dragRange: 0
        }
    }
    if (layout.heightIndex && scrollMetrics) {
        const sourceHeight = Math.max(Number(scrollMetrics.contentHeight) || layout.sourceHeight || 0, 1)
        const clientHeight = Math.max(Number(scrollMetrics.clientHeight) || 0, 0)
        const scrollTop = Math.max(0, Number(scrollMetrics.scrollTop) || 0)
        const track = Math.max(Number(trackHeight) || 0, 0)
        const scale = track / sourceHeight
        const panRange = Math.max(sourceHeight - clientHeight, 0)
        const panRatio = panRange ? Math.min(1, scrollTop / panRange) : 0
        const visibleTopPx = scrollTop * scale
        const visibleHeightPx = Math.min(track, clientHeight * scale)
        const viewportHeightPx = Math.min(track, Math.max(32, visibleHeightPx))
        return {
            contentHeightPx: track,
            contentOffsetPx: 0,
            scale,
            hitScale: scale ? 1 / scale : 1,
            viewportTopPx: panRatio * Math.max(track - viewportHeightPx, 0),
            viewportHeightPx,
            visibleTopPx,
            visibleHeightPx,
            trackHeightPx: track
        }
    }
    const sourceHeight = Math.max(Number(layout.totalHeight ?? layout.previewTotalHeight) || 0, 0)
    const track = Math.max(Number(trackHeight) || 0, 0)
    const top = Math.max(0, Math.min(100, Number(viewport.top) || 0))
    const height = Math.max(0, Math.min(100, Number(viewport.height) || 0))
    const isScrollable = track > 0 && sourceHeight > 0 && height < 100
    const scale = sourceHeight > track && track > 0 ? track / sourceHeight : 1
    const contentHeightPx = sourceHeight * scale
    const visibleTopPx = Math.min(contentHeightPx, top / 100 * contentHeightPx)
    const visibleHeightPx = Math.min(contentHeightPx, height / 100 * contentHeightPx)
    if (!isScrollable) {
        return {
            contentHeightPx,
            contentOffsetPx: 0,
            scale,
            hitScale: 1,
            viewportTopPx: 0,
            viewportHeightPx: Math.min(track, height >= 100 ? Math.max(16, contentHeightPx) : Math.max(16, track * height / 100)),
            visibleTopPx: 0,
            visibleHeightPx: contentHeightPx,
            trackHeightPx: track
        }
    }
    const panRange = Math.max(100 - height, 0.001)
    const panRatio = Math.max(0, Math.min(1, top / panRange))
    const contentOffsetPx = 0
    const viewportHeightPx = Math.min(track, Math.max(32, track * height / 100))
    return {
        contentHeightPx,
        contentOffsetPx,
        scale,
        hitScale: 1,
        viewportTopPx: panRatio * Math.max(track - viewportHeightPx, 0),
        viewportHeightPx,
        visibleTopPx,
        visibleHeightPx,
        trackHeightPx: track
    }
}

export function resolveLogMinimapLineIndex (layout = {}, y = 0, contentOffset = 0, scale = 1, hitScale = 1) {
    if (layout.mapping) return resolveLogMinimapTarget(layout, y)?.index ?? -1
    if (layout.heightIndex) {
        const effectiveScale = Number(scale) > 0 ? Number(scale) : 1
        const sourceOffset = ((Number(y) || 0) + (Number(contentOffset) || 0)) / effectiveScale
        const lineOffset = sourceOffset - (Number(layout.lineOrigin) || 0)
        return layout.heightIndex.findIndexByOffset(Math.max(0, lineOffset))
    }
    const previewRows = layout.previewRows || []
    if (previewRows.length) {
        const effectiveScale = Number(scale) > 0 ? Number(scale) : 1
        const target = Math.max(0, (((Number(y) || 0) + (Number(contentOffset) || 0)) * (Number(hitScale) || 1)) / effectiveScale)
        let low = 0
        let high = previewRows.length
        while (low < high) {
            const mid = (low + high) >>> 1
            if (previewRows[mid].topPx <= target) low = mid + 1
            else high = mid
        }
        return previewRows[Math.max(0, low - 1)].index
    }
    const tops = layout.lineTops || []
    if (!tops.length) return -1
    const effectiveScale = Number(scale) > 0 ? Number(scale) : 1
    const target = Math.max(0, Math.min((Number(layout.totalHeight) || 0) - 1, ((Number(y) || 0) + (Number(contentOffset) || 0)) / effectiveScale))
    let low = 0
    let high = tops.length
    while (low < high) {
        const mid = (low + high) >>> 1
        if (tops[mid] <= target) low = mid + 1
        else high = mid
    }
    return Math.max(0, low - 1)
}

export function resolveLogMinimapMarkTop (mark = {}, coordinates = {}) {
    if (mark.isLocalTop) return Number(mark.topPx) || 0
    const topPx = (Number(mark.topPx) || 0) * (Number(coordinates.scale) || 1)
    if (!Number.isFinite(coordinates.trackHeightPx)) return topPx
    const edge = Math.min(3, Math.max(0, coordinates.trackHeightPx / 2))
    return Math.max(edge, Math.min(coordinates.trackHeightPx - edge, topPx))
}

export function resolveLogMinimapPreviewRowGeometry (row = {}, coordinates = {}) {
    if (Number.isFinite(row.sourceTop) && Number.isFinite(coordinates.scale)) {
        const topPx = coordinates.scale * (row.sourceTop - (Number(coordinates.sourceWindowTop) || 0))
        const heightPx = coordinates.scale * (Number(row.sourceHeight) || 0)
        return { topPx, sourceHeightPx: heightPx, heightPx }
    }
    const scale = Number(coordinates.scale) || 1
    const topPx = (Number(row.topPx) || 0) * scale
    const sourceHeightPx = (Number(row.heightPx) || 0) * scale
    const nextTopPx = Number.isFinite(row.nextTopPx)
        ? Number(row.nextTopPx) * scale
        : Number(coordinates.contentHeightPx) || topPx + sourceHeightPx
    const availableHeightPx = Math.max(0, nextTopPx - topPx)
    const renderHeightPx = Math.max(8, sourceHeightPx)
    return {
        topPx,
        sourceHeightPx,
        heightPx: Math.min(renderHeightPx, availableHeightPx || renderHeightPx)
    }
}

export function resolveLogMinimapPanRatio ({ pointerY, trackTop = 0, grabOffset = 0, trackHeight = 0, viewportHeight = 0, currentRatio = 0, deltaY = 0, tallContent = false } = {}) {
    const range = Math.max(trackHeight - viewportHeight, 0)
    const ratio = tallContent
        ? currentRatio + deltaY / Math.max(range, 1)
        : (range ? (pointerY - trackTop - grabOffset) / range : 0)
    return Math.max(0, Math.min(1, Number.isFinite(ratio) ? ratio : 0))
}

export function resolveLogMinimapKeyboardRatio (ratio, key) {
    const value = Math.max(0, Math.min(1, Number(ratio) || 0))
    const step = key === 'PageUp' || key === 'PageDown' ? 0.2 : 0.05
    const next = {
        ArrowUp: value - step,
        ArrowDown: value + step,
        PageUp: value - step,
        PageDown: value + step,
        Home: 0,
        End: 1
    }[key]
    return next === undefined ? null : Math.max(0, Math.min(1, next))
}

function escapeHtml (text) {
    return String(text).replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]))
}

function splitByKeyword (text, keyword) {
    const search = String(keyword || '')
    if (!search) return [{ highlighted: false, text }]
    const pattern = new RegExp(search.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'ig')
    const parts = []
    let lastIndex = 0
    let match = pattern.exec(text)
    while (match) {
        if (match.index > lastIndex) {
            parts.push({ highlighted: false, text: text.slice(lastIndex, match.index) })
        }
        parts.push({ highlighted: true, text: match[0] })
        lastIndex = pattern.lastIndex
        if (match[0].length === 0) pattern.lastIndex += 1
        match = pattern.exec(text)
    }
    if (lastIndex < text.length) {
        parts.push({ highlighted: false, text: text.slice(lastIndex) })
    }
    return parts.length ? parts : [{ highlighted: false, text: '' }]
}

function serializeTextParts (parts) {
    return parts.map(part => (
        part.highlighted
            ? `<mark class="lp-hl">${escapeHtml(part.text)}</mark>`
            : escapeHtml(part.text)
    )).join('')
}

function applyAnsiCodes (codes, state) {
    const rawCodes = String(codes)
    const tokens = (rawCodes.endsWith('m') ? rawCodes.slice(0, -1) : rawCodes).split(';')
    for (let index = 0; index < tokens.length; index++) {
        if (!tokens[index]) continue
        const value = Number(tokens[index])
        if (!Number.isInteger(value)) continue
        if (value === 38 || value === 48) {
            const mode = Number(tokens[index + 1])
            if (mode === 5) index = Math.min(tokens.length - 1, index + 2)
            else if (mode === 2) index = Math.min(tokens.length - 1, index + 4)
            continue
        }
        if (value === 0 || value === 39) state.currentColor = ''
        else if (ANSI_FOREGROUND[value]) state.currentColor = ANSI_FOREGROUND[value]
    }
}

function collectAnsiMatches (text) {
    const matches = []
    FULL_ANSI_PATTERN.lastIndex = 0
    let match = FULL_ANSI_PATTERN.exec(text)
    while (match) {
        matches.push({ index: match.index, length: match[0].length, codes: match[1] })
        match = FULL_ANSI_PATTERN.exec(text)
    }
    BARE_SGR_PATTERN.lastIndex = 0
    match = BARE_SGR_PATTERN.exec(text)
    while (match) {
        const overlapped = matches.some(item => (
            match.index >= item.index && match.index < item.index + item.length
        ))
        if (!overlapped) {
            matches.push({ index: match.index, length: match[0].length, codes: match[0].slice(1, -1) })
        }
        match = BARE_SGR_PATTERN.exec(text)
    }
    return matches.sort((left, right) => left.index - right.index)
}

function renderAnsiWithHighlight (message, keyword) {
    const text = String(message ?? '')
    const ansiMatches = collectAnsiMatches(text)
    const segments = []
    const state = { currentColor: '' }
    let lastIndex = 0
    ansiMatches.forEach(item => {
        if (item.index > lastIndex) segments.push({ text: text.slice(lastIndex, item.index), color: state.currentColor })
        applyAnsiCodes(item.codes, state)
        lastIndex = item.index + item.length
    })
    if (lastIndex < text.length) segments.push({ text: text.slice(lastIndex), color: state.currentColor })
    if (!segments.length) segments.push({ text, color: '' })

    const visibleText = segments.map(segment => segment.text).join('')
    const highlightedRanges = []
    let visibleIndex = 0
    splitByKeyword(visibleText, keyword).forEach(part => {
        if (part.highlighted) highlightedRanges.push({ start: visibleIndex, end: visibleIndex + part.text.length })
        visibleIndex += part.text.length
    })

    let segmentStart = 0
    return segments.map(segment => {
        const segmentEnd = segmentStart + segment.text.length
        const ranges = highlightedRanges
            .filter(range => range.start < segmentEnd && range.end > segmentStart)
            .map(range => ({
                start: Math.max(range.start, segmentStart) - segmentStart,
                end: Math.min(range.end, segmentEnd) - segmentStart
            }))
        const chunks = []
        let chunkStart = 0
        ranges.forEach(range => {
            if (range.start > chunkStart) chunks.push(escapeHtml(segment.text.slice(chunkStart, range.start)))
            chunks.push(`<mark class="lp-hl">${escapeHtml(segment.text.slice(range.start, range.end))}</mark>`)
            chunkStart = range.end
        })
        if (chunkStart < segment.text.length) chunks.push(escapeHtml(segment.text.slice(chunkStart)))
        const content = chunks.join('')
        segmentStart = segmentEnd
        return segment.color ? `<span style="color:${segment.color}">${content}</span>` : content
    }).join('')
}

export function renderLogHtml (message, keyword, allowLinks = true) {
    if (typeof message === 'string') return renderAnsiWithHighlight(message, keyword)
    return renderFormattedLogHtml(parseLogLine(message), keyword, allowLinks)
}

function formatDuration (milliseconds) {
    const totalSeconds = Math.max(0, Math.floor(Number(milliseconds) / 1000))
    const totalMinutes = Math.floor(totalSeconds / 60)
    const totalHours = Math.floor(totalMinutes / 60)
    const days = Math.floor(totalHours / 24)
    const hours = totalHours % 24
    const minutes = totalMinutes % 60
    const seconds = totalSeconds % 60
    if (days > 0) return `${days}d ${hours}h ${minutes}m ${seconds}s`
    if (totalHours > 0) return `${totalHours}h ${minutes}m ${seconds}s`
    if (minutes > 0) return `${minutes}m ${seconds}s`
    return `${seconds}s`
}

function hasEndTime (endTime) {
    return endTime !== null && endTime !== undefined && endTime !== ''
}

function isValidFiniteNumber (value) {
    return value !== null && value !== undefined && Number.isFinite(Number(value))
}

export function resolveElapsedTimestamp (value) {
    if (value === null || value === undefined || value === '' || typeof value === 'boolean') return null
    if (typeof value === 'number') {
        const numeric = value
        return Number.isFinite(numeric) && numeric >= 0 ? numeric : null
    }
    if (typeof value !== 'string' || !value.trim()) return null
    const text = value.trim()
    if (/^(?:\d+(?:\.\d*)?|\.\d+)$/.test(text)) {
        const numeric = Number(text)
        return Number.isFinite(numeric) ? numeric : null
    }
    const match = text.match(ISO_TIMESTAMP_PATTERN)
    if (!match) return null
    const [, year, month, day, hour, minute, second] = match
    const calendar = new Date(Date.UTC(Number(year), Number(month) - 1, Number(day)))
    if (
        calendar.getUTCFullYear() !== Number(year)
        || calendar.getUTCMonth() !== Number(month) - 1
        || calendar.getUTCDate() !== Number(day)
        || Number(hour) > 23
        || Number(minute) > 59
        || Number(second) > 59
    ) return null
    const timestamp = Date.parse(text)
    return Number.isFinite(timestamp) ? timestamp : null
}

export function formatElapsed (value, endTime, status, valueKind) {
    if (valueKind !== 'elapsed' && valueKind !== 'timestamp') return ''
    if (!isValidFiniteNumber(value)) return ''

    const numeric = Number(value)
    const endProvided = hasEndTime(endTime)

    if (valueKind === 'elapsed') {
        if (endProvided || numeric < 0) return ''
        return formatDuration(numeric)
    }

    if (endProvided) {
        const end = Number(endTime)
        if (!Number.isFinite(end) || end < numeric) return ''
        return formatDuration(end - numeric)
    }

    if (!LIVE_STATUSES.has(String(status || '').toUpperCase())) return ''
    return formatDuration(Date.now() - numeric)
}

function formatTimestamp (value) {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return String(value)
    const pad = (part, length = 2) => String(part).padStart(length, '0')
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}:${pad(date.getMilliseconds(), 3)}`
}

function extractLogTimeValue (input) {
    if (input && typeof input === 'object') return input.time ?? input.timestamp
    return input
}

export function formatLogTime (input) {
    const value = extractLogTimeValue(input)
    if (value === null || value === undefined || value === '') return ''
    return formatTimestamp(value)
}

export function withDisplayLineNumbers (logs = [], displayBase = 1) {
    const base = Number(displayBase)
    const start = Number.isFinite(base) ? base : 1
    return logs.map((log, index) => {
        if (log.displayLineNo !== null && log.displayLineNo !== undefined && log.displayLineNo !== '') return log
        return { ...log, displayLineNo: start + index }
    })
}

export function resolveLogLineNumberColumnWidth (logs = []) {
    return logs.reduce((width, log) => Math.max(width, String(log.displayLineNo ?? '').length), 1)
}

const DANGER_STATUSES = new Set(['FAILED', 'EXEC_TIMEOUT', 'QUALITY_CHECK_FAIL'])
const WARNING_STATUSES = new Set(['CANCELED', 'TERMINATE', 'HEARTBEAT_TIMEOUT'])
const NO_NODE_STATUSES = new Set(['QUEUE', 'PAUSE', 'UNEXEC', 'SKIP', 'WAITING', 'PREPARE_ENV', 'DEPENDENT_WAITING'])
const NOT_EXECUTED_STATUSES = new Set(['UNEXEC', 'SKIP', 'DISABLED', 'PAUSE', 'QUEUE', 'WAITING', 'PREPARE_ENV', 'DEPENDENT_WAITING'])
const LIVE_OUTPUT_STATUSES = new Set(['RUNNING', 'QUEUE', 'WAITING', 'PREPARE_ENV', 'DEPENDENT_WAITING', 'LOOP_WAITING', 'CALL_WAITING'])
const THIRD_PARTY_BUILD_TYPES = new Set(['THIRD_PARTY_AGENT_ID', 'THIRD_PARTY_AGENT_ENV', 'THIRD_PARTY_AGENT_NAME'])
const OUTPUT_TYPE_NAMES = new Set([
    'string', 'textarea', 'artifact', 'report', 'quality', 'certificate',
    'password', 'enum', 'boolean', 'int', 'long', 'float', 'double'
])
const DEFAULT_RUN_CONDITIONS = new Set(['PRE_TASK_SUCCESS', 'STAGE_RUNNING', ''])

export function shouldShowLocateLog (status) {
    return DANGER_STATUSES.has(String(status || '').toUpperCase())
}

function isThirdPartyContainer (container = {}) {
    const buildType = container?.dispatchType?.buildType
    return typeof buildType === 'string' && (THIRD_PARTY_BUILD_TYPES.has(buildType) || buildType.indexOf('THIRD_PARTY_') === 0)
}

export function positionCode (pos, depth = 3) {
    if (!pos) return ''
    const parts = [pos.stageIndex, pos.containerIndex, pos.elementIndex]
        .slice(0, depth)
        .filter(index => index !== undefined && index !== null)
        .map(index => index + 1)
    return parts.length ? parts.join('-') : ''
}

export function nodeDisplay (container = {}) {
    if (!container || typeof container !== 'object') return ''
    if (!isThirdPartyContainer(container)) {
        if (container.dockerContainerName) return String(container.dockerContainerName)
        if (container.containerName) return String(container.containerName)
    }
    if (container.nodeName) return String(container.nodeName)
    if (container.nodeAlias) return String(container.nodeAlias)
    const displayName = container.dispatchType?.displayName
    if (displayName) return String(displayName)
    if (container.thirdPartyAgentId) return String(container.thirdPartyAgentId)
    if (container.dockerContainerName) return String(container.dockerContainerName)
    return ''
}

export function nodeIp (container = {}) {
    if (!isThirdPartyContainer(container)) return ''
    if (container.hostIp) return String(container.hostIp)
    if (container.nodeIp) return String(container.nodeIp)
    return ''
}

export function nodeLink (container = {}, projectId = '') {
    if (!isThirdPartyContainer(container)) return ''
    const nodeHashId = container.nodeHashId
    if (!nodeHashId || !projectId) return ''
    return `/console/environment/${projectId}/pipeline/node/allNode?nodeHashId=${encodeURIComponent(nodeHashId)}`
}

function conclusionTone (status) {
    const normalized = String(status || '').toUpperCase()
    if (normalized === 'SUCCEED' || normalized === 'STAGE_SUCCESS') return 'success'
    if (DANGER_STATUSES.has(normalized)) return 'danger'
    if (WARNING_STATUSES.has(normalized)) return 'canceled'
    if (normalized === 'PAUSE') return 'pause'
    if (normalized === 'QUEUE') return 'queue'
    if (['REVIEWING', 'WAITING', 'DEPENDENT_WAITING'].includes(normalized)) return 'waiting'
    if (normalized === 'RUNNING') return 'running'
    return 'idle'
}

export function resolveConclusionMessage (element = {}) {
    if (element.errorMsg) return stripAnsiForDisplay(element.errorMsg)
    if (element.reviewAbortSuggest) return String(element.reviewAbortSuggest)
    return ''
}

export function resolveConclusionElapsed (element = {}, options = {}) {
    const status = String(element.status || '').toUpperCase()
    const now = options.now
    const isLive = LIVE_STATUSES.has(status)
    const endedStatuses = new Set([
        'SUCCEED', 'FAILED', 'CANCELED', 'TERMINATE', 'REVIEW_ABORT', 'REVIEW_PROCESSED',
        'QUALITY_CHECK_PASS', 'QUALITY_CHECK_FAIL', 'QUOTA_FAILED', 'QUEUE_TIMEOUT', 'EXEC_TIMEOUT',
        'HEARTBEAT_TIMEOUT', 'SKIP'
    ])
    const isEnded = endedStatuses.has(status)

    function validDuration (value) {
        if (typeof value === 'number') return Number.isFinite(value) && value >= 0 ? value : null
        if (typeof value !== 'string' || !/^(?:\d+(?:\.\d*)?|\.\d+)$/.test(value.trim())) return null
        const numeric = Number(value)
        return Number.isFinite(numeric) && numeric >= 0 ? numeric : null
    }

    // Job 的 startEpoch 要到构建机启动完成才写入，准备环境阶段只有 startTime；后端 totalCost 也按 startTime 计算
    const start = options.preferStartTime
        ? resolveElapsedTimestamp(element.startTime) ?? resolveElapsedTimestamp(element.startEpoch)
        : resolveElapsedTimestamp(element.startEpoch) ?? resolveElapsedTimestamp(element.startTime)
    if (isLive && start !== null) {
        const end = hasEndTime(element.endTime)
            ? resolveElapsedTimestamp(element.endTime)
            : (isValidFiniteNumber(now) ? Number(now) : Date.now())
        return end === null ? '' : formatElapsed(start, end, status, 'timestamp')
    }

    const totalCost = validDuration(element.timeCost?.totalCost)
    const elapsed = validDuration(element.elapsed)
    if (isEnded && totalCost !== null) {
        return formatElapsed(totalCost, null, status, 'elapsed')
    }
    if (elapsed !== null) {
        return formatElapsed(elapsed, null, status, 'elapsed')
    }
    if (!isEnded && totalCost !== null) {
        return formatElapsed(totalCost, null, status, 'elapsed')
    }
    if (isEnded) {
        const to = resolveElapsedTimestamp(element.endTime)
        return start !== null && to !== null && to >= start
            ? formatElapsed(start, to, status, 'timestamp')
            : ''
    }
    if (start !== null) {
        const end = hasEndTime(element.endTime)
            ? resolveElapsedTimestamp(element.endTime)
            : (isLive ? (isValidFiniteNumber(now) ? Number(now) : Date.now()) : null)
        return end === null ? '' : formatElapsed(start, end, status, 'timestamp')
    }
    return ''
}

function pickTimeoutRaw (timeoutVar, timeout) {
    if (timeout !== undefined && timeout !== null && String(timeout).trim() !== '') return timeout
    if (timeoutVar !== undefined && timeoutVar !== null && String(timeoutVar).trim() !== '') {
        return timeoutVar
    }
    return timeout
}

export function resolveExecTimeoutThreshold (source = {}, scope = 'step') {
    const raw = scope === 'job'
        ? pickTimeoutRaw(source.jobControlOption?.timeoutVar, source.jobControlOption?.timeout)
        : pickTimeoutRaw(source.additionalOptions?.timeoutVar, source.additionalOptions?.timeout)
    return formatTimeoutValue(raw ?? '')
}

// 准备 Job 也按启动插件 ID（startVM-{jobId}）作为 tag 过滤，不能用 jobId 拉取整个 Job 日志
export function buildLogTarget (item = {}) {
    if (item.id) return { tag: String(item.id) }
    return {}
}

function queueElapsed (serverTime, timestamp) {
    const end = Number(serverTime)
    const start = Number(timestamp)
    if (!Number.isFinite(end) || !Number.isFinite(start) || start <= 0 || end < start) return ''
    return end - start
}

export function isConcurrencyQueueCandidate (buildStatus) {
    return ['QUEUE', 'QUEUE_CACHE', 'LOOP_WAITING'].includes(String(buildStatus || '').toUpperCase())
}

export function isVisibleConcurrencyQueueResponse (response) {
    return response?.visible === true
}

export function canShowJobConclusion (jobStatus, queueDetail) {
    const status = String(jobStatus || '').toUpperCase()
    if (status === 'DISABLED') return false
    if (!status || status === 'UNEXEC' || status === 'WAITING') return Boolean(queueDetail?.visible === true)
    return true
}

// 结论条隐藏时（历史轮次加载中/不可用、未执行、等待等），执行次数选择器需单独成行，保证仍可切换轮次
export function shouldShowStandaloneExecuteCount ({ tab, showConclusion, latestCount, matrix = false } = {}) {
    return tab === 'log' && !showConclusion && !matrix && Number(latestCount) > 1
}

export function shouldClearQueueAfterError (finalRequest) {
    return finalRequest === true
}

export function isQueueCardStatusVisible (status, kind) {
    const normalized = String(status || '').trim().toUpperCase()
    if (!normalized) return false
    if (kind === 'running') return normalized !== 'RUNNING'
    return !['QUEUE', 'QUEUE_CACHE'].includes(normalized)
}

export function resolveQueueCardStatusLabel (status, translate) {
    const normalized = String(status || '').trim().toUpperCase()
    const key = `details.statusMap.${normalized}`
    const translated = typeof translate === 'function' ? translate(key) : key
    return translated && translated !== key ? translated : normalized
}

export function splitQueueItems (items = [], visibleCount = 0) {
    const list = Array.isArray(items) ? items : []
    const count = Math.max(0, Math.floor(Number(visibleCount) || 0))
    return { visible: list.slice(0, count), overflow: list.slice(count) }
}

export function isRoundSkippedStep (item = {}, executeCount) {
    if (!item || item.isSetup) return false
    const itemCount = parsePositiveInteger(item.executeCount)
    const currentCount = parsePositiveInteger(executeCount)
    if (itemCount === null || currentCount === null || itemCount >= currentCount) return false
    const status = String(item.status || '').trim().toUpperCase()
    return ROUND_SKIPPED_STATUSES.has(status)
}

export function resolveJobDirectoryItems (items = [], executeCount) {
    return (Array.isArray(items) ? items : []).map(item => {
        if (!isRoundSkippedStep(item, executeCount)) return item
        return {
            ...item,
            status: 'SKIP',
            timeCost: null,
            elapsed: null,
            startTime: null,
            startEpoch: null,
            endTime: null,
            roundSkipped: true
        }
    })
}

export function resolveJobQueueDetail (response = null, context = {}) {
    const result = response?.data ?? response
    const data = result?.data ?? result
    if (Number(result?.status ?? 0) !== 0 || data?.visible !== true) return null
    const projectId = context.projectId
    const receivedAt = Number(context.receivedAt)
    const now = Number(context.now)
    const serverTime = Number(data.serverTime) + (Number.isFinite(receivedAt) && Number.isFinite(now) && now >= receivedAt ? now - receivedAt : 0)
    const aheadCount = data.aheadCount !== null && data.aheadCount !== undefined && String(data.aheadCount).trim() !== '' && Number.isSafeInteger(Number(data.aheadCount)) && Number(data.aheadCount) >= 0
        ? Number(data.aheadCount)
        : null
    const total = data.total !== null && data.total !== undefined && String(data.total).trim() !== '' && Number.isSafeInteger(Number(data.total)) && Number(data.total) >= 0 && (aheadCount === null || Number(data.total) >= aheadCount + 1)
        ? Number(data.total)
        : null
    const runningItems = Array.isArray(data.running) ? data.running : []
    const makeBuild = (item = {}, kind) => {
        const detailVisible = item.detailVisible === true
        const status = String(item.status || '').trim().toUpperCase()
        const rawElapsed = kind === 'running'
            ? queueElapsed(serverTime, item.startTime)
            : queueElapsed(serverTime, item.queueTime)
        const elapsedMinutes = rawElapsed === '' ? '' : Math.max(1, Math.ceil(rawElapsed / 60000))
        const buildNum = item.buildNum === null || item.buildNum === undefined ? '' : String(item.buildNum)
        const pipelineName = detailVisible ? String(item.pipelineName || '') : ''
        const buildMsg = detailVisible ? String(item.buildMsg || '').trim() : ''
        const title = [pipelineName, buildMsg].filter(Boolean).join(' · ')
        const url = detailVisible && projectId && item.pipelineId && item.buildId
            ? `${context.webUrlPrefix || WEB_URL_PREFIX}/pipeline/${projectId}/${item.pipelineId}/detail/${item.buildId}`
            : ''
        return {
            buildNum,
            status,
            showStatus: isQueueCardStatusVisible(status, kind),
            title,
            owner: detailVisible ? String(item.userId || '') : '',
            elapsedMinutes,
            linkUrl: url,
            detailVisible
        }
    }
    return {
        visible: true,
        group: String(data.concurrencyGroup || ''),
        waitedMillis: queueElapsed(serverTime, data.queueTime),
        waitedMinutes: (() => {
            const value = queueElapsed(serverTime, data.queueTime)
            return value === '' ? '' : Math.max(1, Math.ceil(value / 60000))
        })(),
        position: aheadCount === null ? null : aheadCount + 1,
        total,
        aheadCount: aheadCount ?? 0,
        running: runningItems.map(item => makeBuild(item, 'running')),
        ahead: (Array.isArray(data.ahead) ? data.ahead : []).map(item => makeBuild(item, 'ahead'))
    }
}

export function hasLogTarget (target = {}) {
    return Boolean(target.tag || target.jobId)
}

export function resolveConclusionTimeoutThreshold (context = {}) {
    const status = String(context.effectiveStatus || context.status || '').toUpperCase()
    if (status !== 'EXEC_TIMEOUT') return ''
    if (String(context.jobStatus || '').toUpperCase() === 'EXEC_TIMEOUT') {
        return resolveExecTimeoutThreshold(context.container || {}, 'job')
    }
    if (String(context.stepStatus || '').toUpperCase() === 'EXEC_TIMEOUT') {
        return resolveExecTimeoutThreshold(context.element || {}, 'step')
    }
    return ''
}

const CANCEL_NOT_STARTED_STATUSES = new Set(['', 'UNEXEC', 'QUEUE', 'QUEUE_CACHE'])

// 取消位置只记录被打断的在途插件/Job：自身以取消收尾的组件同属这次取消；
// 构建在排队中被取消时没有任何位置，从未启动的组件也是因这次取消而没有执行
function isUnpositionedCancelTarget (positions, status) {
    const normalized = String(status || '').toUpperCase()
    if (normalized === 'CANCELED') return true
    return !positions.length && CANCEL_NOT_STARTED_STATUSES.has(normalized)
}

export function resolveCancelConclusion (buildEndInfo = {}, context = {}) {
    if (buildEndInfo?.endCategory !== 'CANCEL') return null
    const positions = Array.isArray(buildEndInfo.positions) ? buildEndInfo.positions : []
    const taskId = context.taskId === null || context.taskId === undefined ? '' : String(context.taskId)
    const containerId = context.containerId === null || context.containerId === undefined ? '' : String(context.containerId)
    const positionMatches = positions.some(position => {
        const positionContainerId = position?.containerId || position?.containerHashId
        const positionTaskId = position?.taskId ?? position?.elementId
        const containerMatches = containerId
            ? String(positionContainerId ?? '') === containerId
            : !positionContainerId
        const taskMatches = !taskId || String(positionTaskId ?? '') === taskId
        return containerMatches && taskMatches
    })
    const fallbackMatches = context.status !== undefined
        && !isJobExecTimeoutCancel(buildEndInfo)
        && isUnpositionedCancelTarget(positions, context.status)
    if (!positionMatches && !fallbackMatches) return null

    const endType = String(buildEndInfo.endType || '').toUpperCase()
    const type = endType === 'CANCEL_USER'
        ? 'user'
        : endType === 'CANCEL_SYSTEM'
            ? 'system'
            : endType === 'CANCEL_PARENT_PIPELINE'
                ? 'parent'
                : ''
    if (!type) return null

    const parent = buildEndInfo.parentPipelineInfo || {}
    const webUrlPrefix = context.webUrlPrefix
        ?? (typeof WEB_URL_PREFIX === 'undefined' ? '' : WEB_URL_PREFIX)
    const parentBuildUrl = type === 'parent' && parent.projectId && parent.pipelineId && parent.buildId
        ? `${webUrlPrefix}/pipeline/${parent.projectId}/${parent.pipelineId}/detail/${parent.buildId}`
        : ''
    return {
        type,
        operator: String(buildEndInfo.operator || ''),
        reason: String(buildEndInfo.reason || ''),
        reasonCode: String(buildEndInfo.reasonCode || ''),
        parentPipelineInfo: parent,
        parentBuildUrl
    }
}

// 系统/父流水线取消时记录里的取消人是 System 或触发新构建的用户，不能展示成取消人
export function resolveCancelUserId (cancelConclusion, fallback = '') {
    if (!cancelConclusion || cancelConclusion.type === 'user') return fallback || ''
    return ''
}

// 未下发终态详情的构建只有取消人可用；系统取消记录的取消人为 System
export function resolveFallbackCancelLabelKey ({ status, cancelUserId, buildEndInfo } = {}) {
    if (buildEndInfo || String(status || '').toUpperCase() !== 'CANCELED') return ''
    const operator = String(cancelUserId || '').trim()
    return operator && operator.toLowerCase() !== 'system' ? 'userCanceled' : ''
}

// 构建记录被清理后步骤不再带状态，只有整次构建成功时才能确定步骤成功
export function resolveRecordElementStatus (status, buildStatus) {
    if (status) return status
    return String(buildStatus || '').toUpperCase() === 'SUCCEED' ? 'SUCCEED' : status
}

export function isJobExecTimeoutCancel (buildEndInfo = {}) {
    return String(buildEndInfo?.reasonCode || '') === 'bkBuildCancelSystemJobExecTimeout'
}

export function resolveCancelDisplay (cancelConclusion, buildEndInfo = {}, translate = key => key) {
    if (!cancelConclusion) return null
    const text = key => {
        const translated = translate(`logPanel.status.${key}`)
        return translated === `logPanel.status.${key}` ? '' : translated
    }
    const reason = cancelConclusion.reason || String(buildEndInfo.reason || '')
    const parent = cancelConclusion.parentPipelineInfo || buildEndInfo.parentPipelineInfo || {}
    const parentName = String(parent.pipelineName || '')
    const parentBuildNum = parent.buildNum === null || parent.buildNum === undefined ? '' : String(parent.buildNum)
    const parentOperator = String(parent.operator || '')
    const parentComplete = Boolean(parentName && parentBuildNum && parentOperator)

    if (cancelConclusion.type === 'user') {
        return {
            label: text('userCanceled'),
            message: reason,
            cancelUserId: cancelConclusion.operator,
            parentBuildUrl: cancelConclusion.parentBuildUrl || ''
        }
    }
    if (cancelConclusion.type === 'system') {
        return {
            label: text('systemCanceled'),
            message: reason || text('systemCanceledFallback'),
            cancelUserId: '',
            parentBuildUrl: cancelConclusion.parentBuildUrl || ''
        }
    }
    if (cancelConclusion.type === 'parent') {
        return {
            label: text('parentCanceled'),
            message: parentComplete
                ? translate('logPanel.status.parentCanceledDetail', [parentName, parentBuildNum, parentOperator])
                : reason || translate('logPanel.queue.parentCanceled'),
            cancelUserId: '',
            parentBuildUrl: cancelConclusion.parentBuildUrl || ''
        }
    }
    return null
}

export function buildConclusion (element = {}, container = {}, context = {}) {
    const status = String(element.status || container.status || '').toUpperCase()
    const showNode = status && !NO_NODE_STATUSES.has(status) && !context.hideNode
    const message = context.message ?? resolveConclusionMessage(element)
    const cancelUserId = status === 'CANCELED' && context.cancelUserId
        ? String(context.cancelUserId)
        : ''
    const timeoutThreshold = resolveConclusionTimeoutThreshold({
        ...context,
        effectiveStatus: context.effectiveStatus || status
    })
    return {
        status,
        tone: conclusionTone(status),
        label: status,
        elapsed: resolveConclusionElapsed(element, { now: context.now, preferStartTime: context.preferStartTime }),
        node: showNode ? nodeDisplay(container) : '',
        nodeIp: showNode ? nodeIp(container) : '',
        nodeLink: showNode ? nodeLink(container, context.projectId) : '',
        message,
        cancelUserId,
        timeoutThreshold,
        errorCode: element.errorCode || '',
        locateLog: !!context.locateLog,
        canRetry: !!context.canRetry,
        canSkip: !!context.canSkip,
        continueConfig: !!context.continueConfig,
        review: !!context.review,
        conditionLink: !!context.conditionLink,
        statusMessageKey: resolveConclusionStatusMessageKey(element, {
            ...context,
            message,
            status
        })
    }
}

export function shouldApplyProgressResponse ({ requestId, currentRequestId, contextKey, currentContextKey } = {}) {
    return requestId === currentRequestId && contextKey === currentContextKey
}

export function shouldScheduleProgressPoll (status, applyResponse) {
    return !!applyResponse && shouldPollTaskProgress(status)
}

export function normalizeProgressRatio (value) {
    const numeric = Number(value)
    if (!Number.isFinite(numeric)) return null
    return Math.max(0, Math.min(1, numeric))
}

export const BEFORE_SCROLL_THRESHOLD = 40
export const MAX_BEFORE_RETRIES = 3

export function encodeDownloadFileName (value) {
    return encodeURI(encodeURI(String(value ?? '')))
}

export function shouldUseDirectJobDownload ({ tag, jobId, downloadTag, downloadJobId } = {}) {
    const effectiveTag = downloadTag ?? tag
    const effectiveJobId = downloadJobId ?? jobId
    return !effectiveTag && !!effectiveJobId
}

export function appendExecuteCountToUrl (url, executeCount) {
    const link = String(url || '')
    if (!link) return ''
    if (/[?&]executeCount=/.test(link)) return link
    const separator = link.includes('?') ? '&' : '?'
    return `${link}${separator}executeCount=${executeCount}`
}

export function resolveDirectJobDownloadUrl ({ downloadLink, executeCount } = {}) {
    return appendExecuteCountToUrl(downloadLink, executeCount)
}

export function shouldRetryBeforeLoad ({ status, beforeRetries, maxRetries = MAX_BEFORE_RETRIES } = {}) {
    return Number(status) === 999 && beforeRetries < maxRetries
}

export function shouldScheduleBeforeRetry ({ scrollTop, threshold = BEFORE_SCROLL_THRESHOLD } = {}) {
    return Number(scrollTop) <= threshold
}

export function shouldFetchTaskProgress (status) {
    return shouldPollTaskProgress(status)
}

export function applyProgressMockTransition (previousMockState, nextMockState, status, actions = {}) {
    if (previousMockState === nextMockState) return false
    actions.stopPoll?.()
    actions.invalidate?.()
    actions.reset?.()
    if (!previousMockState || nextMockState || !shouldFetchTaskProgress(status)) return false
    actions.fetch?.()
    return true
}

export function parseTaskProgressPercent (data) {
    const structured = data?.progressDetail?.progress
    const structuredValue = structured?.value
    if (structured != null && structuredValue != null && Number.isFinite(Number(structuredValue))) {
        const ratio = normalizeProgressRatio(Number(structuredValue))
        if (ratio != null) return Math.round(ratio * 10000) / 100
    }
    const legacy = data?.taskProgressRete
    if (legacy == null || !Number.isFinite(Number(legacy))) return null
    const legacyNum = Number(legacy)
    if (legacyNum === 0) return null
    const ratio = normalizeProgressRatio(legacyNum)
    if (ratio == null) return null
    return Math.round(ratio * 10000) / 100
}

export function resolveDisplayProgressPercent (status, percent) {
    if (!shouldPollTaskProgress(status)) return null
    return percent == null ? null : percent
}

export function normalizeSubtaskStatus (status) {
    const key = String(status || 'pending').toLowerCase()
    if (['succeeded', 'success'].includes(key)) return 'succeeded'
    if (['failed', 'failure'].includes(key)) return 'failed'
    if (key === 'running') return 'running'
    return 'pending'
}

export function subtaskStatusIcon (status) {
    const iconMap = {
        succeeded: 'check-circle',
        failed: 'close-circle',
        running: 'circle-2-1',
        pending: 'clock'
    }
    return iconMap[normalizeSubtaskStatus(status)] || iconMap.pending
}

function padTimelinePart (value) {
    return String(value).padStart(2, '0')
}

function formatTimelineClock (timestamp, includeDate = false) {
    const date = new Date(timestamp)
    const clock = [
        padTimelinePart(date.getHours()),
        padTimelinePart(date.getMinutes()),
        padTimelinePart(date.getSeconds())
    ].join(':')
    if (!includeDate) return clock
    return `${padTimelinePart(date.getMonth() + 1)}-${padTimelinePart(date.getDate())} ${clock}`
}

function isSameLocalDay (left, right) {
    const leftDate = new Date(left)
    const rightDate = new Date(right)
    return leftDate.getFullYear() === rightDate.getFullYear()
        && leftDate.getMonth() === rightDate.getMonth()
        && leftDate.getDate() === rightDate.getDate()
}

function parseTimelineStartMillis (startTime) {
    // Backend BuildTaskProgressTimelineItem.startTime is an ISO 8601 instant.
    // Reject numeric values instead of guessing whether they are seconds or milliseconds.
    if (typeof startTime !== 'string' || !startTime.trim()) return null
    const timestamp = Date.parse(startTime)
    return Number.isFinite(timestamp) ? timestamp : null
}

function parseTimelineDurationMillis (duration) {
    // Backend BuildTaskProgressTimelineItem.duration is explicitly milliseconds.
    if (duration === null) return null
    if (!isValidFiniteNumber(duration)) return undefined
    const milliseconds = Number(duration)
    return milliseconds >= 0 ? milliseconds : undefined
}

function resolveSubtaskTiming (status, timeline, now) {
    const live = status === 'running'
    const idle = status === 'pending'
    if (idle) return { elapsed: '', timeRange: '', live: false, idle: true }
    if (!timeline) return { elapsed: '', timeRange: '', live, idle: false }

    const startMillis = parseTimelineStartMillis(timeline.startTime)
    if (startMillis === null) return { elapsed: '', timeRange: '', live, idle: false }

    const durationMillis = parseTimelineDurationMillis(timeline.duration)
    if (durationMillis === null) {
        if (!live) return { elapsed: '', timeRange: '', live: false, idle: false }
        const currentTime = Number(now)
        const elapsed = Number.isFinite(currentTime) && currentTime >= startMillis
            ? formatDuration(currentTime - startMillis)
            : ''
        return {
            elapsed,
            timeRange: formatTimelineClock(startMillis),
            live,
            idle: false
        }
    }
    if (durationMillis === undefined) return { elapsed: '', timeRange: '', live, idle: false }

    const endMillis = startMillis + durationMillis
    const includeDate = !isSameLocalDay(startMillis, endMillis)
    return {
        elapsed: formatDuration(durationMillis),
        timeRange: `${formatTimelineClock(startMillis, includeDate)} ~ ${formatTimelineClock(endMillis, includeDate)}`,
        live: false,
        idle: false
    }
}

export function parseTaskProgressSubtasks (data, { now = Date.now() } = {}) {
    const items = data?.progressDetail?.subtasks?.items
    if (!Array.isArray(items)) return []
    const timelines = Array.isArray(data?.progressDetail?.timeline?.items)
        ? data.progressDetail.timeline.items
        : []
    const timelinesByName = timelines.reduce((result, timeline) => {
        const name = timeline?.name || ''
        if (!result.has(name)) result.set(name, [])
        result.get(name).push(timeline)
        return result
    }, new Map())
    const occurrenceByName = new Map()

    return items.map(item => {
        const status = normalizeSubtaskStatus(item.status)
        const progressRatio = item.progress != null && Number.isFinite(Number(item.progress))
            ? normalizeProgressRatio(Number(item.progress))
            : null
        const name = item.name || ''
        const occurrence = occurrenceByName.get(name) || 0
        occurrenceByName.set(name, occurrence + 1)
        const timeline = timelinesByName.get(name)?.[occurrence]
        const timing = resolveSubtaskTiming(status, timeline, now)
        return {
            name,
            status,
            progress: progressRatio != null ? `${Math.round(progressRatio * 10000) / 100}%` : '',
            ...timing
        }
    })
}

export function shouldPollTaskProgress (status) {
    return String(status || '').toUpperCase() === 'RUNNING'
}

export function shouldResumeLogPolling (previousStatus, status) {
    const previous = String(previousStatus || '').toUpperCase()
    const next = String(status || '').toUpperCase()
    return previous === 'REVIEWING' && next !== 'REVIEWING'
        || previous !== next && next === 'RUNNING'
}

export function hasLogIssues (logs = []) {
    return logs.some(log => Boolean(getLogIssueLevel(log)))
}

export function resolveLogHighlightState ({
    logs = [],
    matchIds = [],
    highlightedLineNo = '',
    currentMatch = 0,
    reset = false,
    searching = false
} = {}) {
    if (matchIds.length) {
        const retainedIndex = reset ? -1 : matchIds.findIndex(id => String(id) === String(highlightedLineNo))
        const current = retainedIndex >= 0
            ? retainedIndex + 1
            : reset
                ? 1
                : Math.min(Math.max(currentMatch || 1, 1), matchIds.length)
        return { current, highlightedLineNo: matchIds[current - 1] ?? '' }
    }

    const target = String(highlightedLineNo ?? '')
    const stillLoaded = Boolean(target) && logs.some(log => String(log.lineNo ?? log.line ?? '') === target)
    return {
        current: 0,
        highlightedLineNo: !reset && !searching && stillLoaded ? highlightedLineNo : ''
    }
}

export function hasCustomRunCondition (element = {}) {
    const condition = element?.additionalOptions?.runCondition
    return !!(condition && !DEFAULT_RUN_CONDITIONS.has(condition))
}

export function isConditionSkipped (element = {}) {
    const runCondition = String(element?.additionalOptions?.runCondition || '').trim()
    if (String(element.status || '').toUpperCase() !== 'SKIP' || !runCondition) return false
    const hasStarted = value => {
        const timestamp = resolveElapsedTimestamp(value)
        return timestamp !== null && timestamp > 0
    }
    return !hasStarted(element.startEpoch) && !hasStarted(element.startTime)
}

export function isJobConditionSkipped (job = {}) {
    if (String(job.status || '').toUpperCase() !== 'SKIP') return false
    const jobControlOption = job.jobControlOption || job.controlOption?.jobControlOption
    return jobControlOption?.enable !== false
}

export function resolveConditionSkipReason (logs = []) {
    const candidates = (Array.isArray(logs) ? logs : [])
        .map(getLogText)
        .filter(text => /(?:检查(?:插件|Job)运行条件|check (?:task|job) run condition)/i.test(text)
            && /(?:skip\s*=\s*true|skip\s+when|will be skipped|跳过|将跳过|即将跳过)/i.test(text))
    return candidates[candidates.length - 1] || ''
}

export function countConditionSkippedSteps (elements = []) {
    return (Array.isArray(elements) ? elements : []).filter(isConditionSkipped).length
}

function looksExpression (value) {
    const text = String(value ?? '')
    return /\$\{\{[\s\S]+?\}\}/.test(text) || /\$\{[^{][^}]*\}/.test(text)
}

function isSecretKey (key) {
    const name = String(key || '')
    return /password|token|secret|credential|oauth|passwd|pwd/i.test(name) || name.startsWith('_')
}

function stringifyParamValue (value) {
    if (value == null) return ''
    if (typeof value === 'object') {
        try {
            return JSON.stringify(value)
        } catch (_) {
            return String(value)
        }
    }
    return String(value)
}

function isOutputTypeName (value) {
    return OUTPUT_TYPE_NAMES.has(String(value || '').trim().toLowerCase())
}

function toTypeNamePlaceholderRow (key) {
    return {
        key,
        value: '',
        expr: undefined,
        secret: false
    }
}

function toParamRows (source, { excludeTypeNames = false, blankTypeNames = false } = {}) {
    if (Array.isArray(source)) {
        return source
            .filter(row => row && (row.key || row.name))
            .map((row) => {
                const key = row.key || row.name
                const rawValue = stringifyParamValue(row.value)
                if (isOutputTypeName(rawValue)) {
                    if (excludeTypeNames) return null
                    if (blankTypeNames) return toTypeNamePlaceholderRow(key)
                }
                const secret = !!(row.secret || row.hidden || isSecretKey(key))
                return {
                    key,
                    value: secret ? '******' : rawValue,
                    expr: secret ? undefined : row.expr,
                    secret
                }
            })
            .filter(Boolean)
    }
    if (!source || typeof source !== 'object') return []
    return Object.keys(source).map((key) => {
        const raw = source[key]
        let value = raw
        let expr
        if (raw && typeof raw === 'object' && !Array.isArray(raw) && ('value' in raw || 'default' in raw)) {
            value = raw.value != null ? raw.value : raw.default
            expr = raw.expr
        }
        value = stringifyParamValue(value)
        if (isOutputTypeName(value)) {
            if (excludeTypeNames) return null
            if (blankTypeNames) return toTypeNamePlaceholderRow(key)
        }
        const secret = isSecretKey(key) || value === '******'
        if (!secret && !expr && looksExpression(value)) expr = value
        return {
            key,
            value: secret ? '******' : value,
            expr: secret ? undefined : expr,
            secret
        }
    }).filter(Boolean)
}

export function visibleParamRows (rows = [], { keyword = '', expanded = false, limit = 20 } = {}) {
    const query = String(keyword).trim().toLowerCase()
    const filtered = query
        ? rows.filter(row => [
            row.key,
            row.value,
            row.secret ? '' : row.expr
        ].some(value => String(value ?? '').toLowerCase().includes(query)))
        : rows
    return query || expanded ? filtered : filtered.slice(0, limit)
}

export function paramCopyText (row = {}) {
    return row.secret ? '******' : String(row.value ?? '')
}

export function resolveRecordElement (execDetail = {}, position = {}) {
    const stage = execDetail?.model?.stages?.[position.stageIndex]
    const container = stage?.containers?.[position.containerIndex]
    const target = position.containerGroupIndex === undefined
        ? container
        : container?.groupContainers?.[position.containerGroupIndex]
    return target?.elements?.[position.elementIndex] || {}
}

function hasIdentityValue (value) {
    if (typeof value === 'number') return Number.isFinite(value)
    return typeof value === 'string' && value.trim() !== ''
}

function sameIdentityValue (left, right) {
    return hasIdentityValue(left) && hasIdentityValue(right) && String(left) === String(right)
}

function isPositiveTimestamp (value) {
    if (typeof value === 'number') return Number.isFinite(value) && value > 0
    if (typeof value !== 'string' || !/^\d+(?:\.\d+)?$/.test(value.trim())) return false
    return Number.isFinite(Number(value)) && Number(value) > 0
}

function parsePositiveInteger (value) {
    if (typeof value === 'number') return Number.isSafeInteger(value) && value > 0 ? value : null
    if (typeof value !== 'string' || !/^\d+$/.test(value.trim())) return null
    const parsed = Number(value)
    return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : null
}

function hasHistoricalErrorEvidence (errorInfoList) {
    return Array.isArray(errorInfoList) && errorInfoList.some(error => {
        if (!error || typeof error !== 'object' || Array.isArray(error)) return false
        const hasNodeIdentity = hasIdentityValue(error.containerId ?? error.jobId)
            && (Object.prototype.hasOwnProperty.call(error, 'taskId') || Object.prototype.hasOwnProperty.call(error, 'elementId'))
        const hasErrorInformation = hasIdentityValue(error.errorCode)
            || hasIdentityValue(error.errorMsg)
            || hasIdentityValue(error.errorDetail)
            || hasIdentityValue(error.detail)
        return hasNodeIdentity && hasErrorInformation
    })
}

function hasHistoricalEntityEvidence (stages, executeCount) {
    const requestedCount = parsePositiveInteger(executeCount)
    if (!Array.isArray(stages) || requestedCount === null) return false
    let hasExplicitCount = false
    let isConsistent = true
    const visit = entity => {
        if (!entity || typeof entity !== 'object' || !isConsistent) return
        if (Object.prototype.hasOwnProperty.call(entity, 'executeCount')
            && entity.executeCount !== null
            && entity.executeCount !== undefined
            && entity.executeCount !== '') {
            const count = parsePositiveInteger(entity.executeCount)
            if (count === null || count > requestedCount) {
                isConsistent = false
                return
            }
            hasExplicitCount = true
        }
        if (Array.isArray(entity.groupContainers)) entity.groupContainers.forEach(visit)
        if (Array.isArray(entity.elements)) entity.elements.forEach(visit)
    }
    stages.forEach(stage => {
        if (Array.isArray(stage?.containers)) stage.containers.forEach(visit)
    })
    return isConsistent && hasExplicitCount
}

export function validateHistoricalLogRecord (detail, context = {}) {
    if (!detail || typeof detail !== 'object' || Array.isArray(detail)) return false
    if (context.isCurrent === false) return false
    if (context.generation !== undefined && context.currentGeneration !== undefined && context.generation !== context.currentGeneration) return false
    if (context.contextKey !== undefined && context.currentContextKey !== undefined && context.contextKey !== context.currentContextKey) return false

    const requestedCount = parsePositiveInteger(context.executeCount)
    if (requestedCount === null) return false
    if (!sameIdentityValue(detail.id, context.buildNo) || !sameIdentityValue(detail.pipelineId, context.pipelineId)) return false
    if (parsePositiveInteger(detail.executeCount) !== requestedCount) return false
    const stages = detail.model?.stages
    if (!Array.isArray(stages) || !stages.some(stage => stage && hasIdentityValue(stage.id) && Array.isArray(stage.containers))) return false

    const hasSnapshotEvidence = isPositiveTimestamp(detail.startTime)
        || isPositiveTimestamp(detail.endTime)
        || hasHistoricalErrorEvidence(detail.errorInfoList)
        || (
            (isPositiveTimestamp(detail.queueTime) || isPositiveTimestamp(detail.currentTimestamp))
            && hasHistoricalEntityEvidence(stages, requestedCount)
        )
    return hasSnapshotEvidence
}

function hasEntityCountAtOrBefore (entity, executeCount) {
    if (!entity || typeof entity !== 'object') return false
    if (entity.executeCount === null || entity.executeCount === undefined || entity.executeCount === '') return true
    const count = parsePositiveInteger(entity.executeCount)
    const requestedCount = parsePositiveInteger(executeCount)
    return count !== null && requestedCount !== null && count <= requestedCount
}

function matchesContainerIdentity (source, candidate) {
    const sourceHash = source?.containerHashId
    const candidateHash = candidate?.containerHashId
    if (hasIdentityValue(sourceHash) && hasIdentityValue(candidateHash)) {
        return sameIdentityValue(sourceHash, candidateHash)
    }

    const sourceId = source?.id
    const candidateId = candidate?.id
    const sourceContainerId = source?.containerId
    const candidateContainerId = candidate?.containerId
    if (hasIdentityValue(sourceId) && hasIdentityValue(candidateId) && !sameIdentityValue(sourceId, candidateId)) return false
    if (hasIdentityValue(sourceContainerId) && hasIdentityValue(candidateContainerId) && !sameIdentityValue(sourceContainerId, candidateContainerId)) return false
    return sameIdentityValue(sourceId, candidateId) || sameIdentityValue(sourceContainerId, candidateContainerId)
}

function uniqueMatch (items, predicate) {
    if (!Array.isArray(items)) return null
    const matches = items.filter(predicate)
    return matches.length === 1 ? matches[0] : null
}

export function resolveHistoricalJobSnapshot (sourceExecDetail = {}, historicalDetail = {}, position = {}, executeCount) {
    const sourceStages = sourceExecDetail?.model?.stages
    const sourceStage = sourceStages?.[position.stageIndex]
    const sourceParent = sourceStage?.containers?.[position.containerIndex]
    const sourceContainer = position.containerGroupIndex === undefined
        ? sourceParent
        : sourceParent?.groupContainers?.[position.containerGroupIndex]
    if (!sourceStage || !hasIdentityValue(sourceStage.id) || !sourceParent || !sourceContainer) return null

    const stage = uniqueMatch(historicalDetail?.model?.stages, item => sameIdentityValue(item?.id, sourceStage.id))
    if (!stage) return null
    const parentContainer = uniqueMatch(stage.containers, item => {
        if (Boolean(item?.matrixGroupFlag) !== Boolean(sourceParent.matrixGroupFlag)) return false
        return matchesContainerIdentity(sourceParent, item)
    })
    if (!parentContainer || !hasEntityCountAtOrBefore(parentContainer, executeCount)) return null

    let container = parentContainer
    if (position.containerGroupIndex !== undefined) {
        container = uniqueMatch(parentContainer.groupContainers, item => matchesContainerIdentity(sourceContainer, item))
        if (!container || !hasEntityCountAtOrBefore(container, executeCount)) return null
    }
    if (Array.isArray(container.elements) && container.elements.some(element => !hasEntityCountAtOrBefore(element, executeCount))) return null
    return { stage, parentContainer, container }
}

export function resolveHistoricalPluginSnapshot (sourceExecDetail = {}, historicalDetail = {}, position = {}, executeCount) {
    const jobSnapshot = resolveHistoricalJobSnapshot(sourceExecDetail, historicalDetail, position, executeCount)
    const sourceElement = sourceExecDetail?.model?.stages?.[position.stageIndex]
        ?.containers?.[position.containerIndex]
        ?.groupContainers?.[position.containerGroupIndex]
        ?.elements?.[position.elementIndex]
        ?? sourceExecDetail?.model?.stages?.[position.stageIndex]
            ?.containers?.[position.containerIndex]
            ?.elements?.[position.elementIndex]
    if (!jobSnapshot || !hasIdentityValue(sourceElement?.id)) return null
    const element = uniqueMatch(jobSnapshot.container.elements, item => sameIdentityValue(item?.id, sourceElement.id))
    if (!element || !hasEntityCountAtOrBefore(element, executeCount)) return null
    return { ...jobSnapshot, element }
}

export function shouldFetchParamsRecord (selectedExecuteCount, latestExecuteCount) {
    let selected = null
    if (typeof selectedExecuteCount === 'number') {
        if (Number.isInteger(selectedExecuteCount) && selectedExecuteCount >= 1) {
            selected = selectedExecuteCount
        }
    } else if (typeof selectedExecuteCount === 'string') {
        const trimmed = selectedExecuteCount.trim()
        if (trimmed && /^\d+$/.test(trimmed)) {
            const numeric = Number(trimmed)
            if (Number.isInteger(numeric) && numeric >= 1) selected = numeric
        }
    }
    if (selected === null) return false
    return selected !== Number(latestExecuteCount)
}

export function resolveParamsElementSource ({
    execDetail = {},
    paramsExecDetail = null,
    position = {},
    selectedExecuteCount,
    latestExecuteCount
} = {}) {
    const historical = shouldFetchParamsRecord(selectedExecuteCount, latestExecuteCount)
    if (historical && !paramsExecDetail) return {}
    return resolveRecordElement(historical ? paramsExecDetail : execDetail, position)
}

export function buildParamsRecordRequest ({
    projectId,
    pipelineId,
    buildNo,
    executeCount,
    archiveFlag
} = {}) {
    const payload = {
        projectId,
        pipelineId,
        buildNo,
        executeCount,
        skipSetPipelineDetail: true
    }
    if (
        typeof archiveFlag === 'boolean'
        || archiveFlag === 'true'
        || archiveFlag === 'false'
    ) {
        payload.archiveFlag = archiveFlag
    }
    return payload
}

export function buildHistoricalRecordRequest ({
    projectId,
    pipelineId,
    buildNo,
    executeCount,
    archiveFlag
} = {}) {
    const payload = { projectId, pipelineId, buildNo, executeCount }
    if (
        typeof archiveFlag === 'boolean'
        || archiveFlag === 'true'
        || archiveFlag === 'false'
    ) {
        payload.archiveFlag = archiveFlag
    }
    return payload
}

export function buildParamsModel (element = {}) {
    const status = element.status || ''
    const data = element.data || {}
    const input = toParamRows(data.input)
    const running = LIVE_OUTPUT_STATUSES.has(status)
    const output = toParamRows(data.output, running
        ? { excludeTypeNames: true }
        : { blankTypeNames: true })
    const env = toParamRows(element.customEnv)
    const sections = [
        { id: 'input', rows: input },
        { id: 'output', rows: output, pending: running && !output.length },
        { id: 'env', rows: env }
    ]
    sections.notExecuted = NOT_EXECUTED_STATUSES.has(status)
    sections.status = status
    return sections
}

function formatTimeoutValue (value) {
    const raw = String(value ?? '').trim()
    if (!raw) return ''
    if (/^\d+(?:\.\d+)?$/.test(raw) && Number(raw) > 0) return `${Number(raw)}m`
    return raw
}
