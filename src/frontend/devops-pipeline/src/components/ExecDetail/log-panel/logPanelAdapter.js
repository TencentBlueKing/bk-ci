const STATUS_LABEL = {
    SUCCEED: '执行成功',
    FAILED: '执行失败',
    RUNNING: '执行中',
    CANCELED: '已取消',
    PAUSE: '执行前暂停',
    REVIEWING: '等待人工决策',
    QUEUE: '排队中',
    SKIP: '已跳过',
    UNEXEC: '未执行',
    EXEC_TIMEOUT: '执行超时',
    HEARTBEAT_TIMEOUT: '心跳超时',
    PREPARE_ENV: '准备环境'
}

const FAIL = ['FAILED', 'EXEC_TIMEOUT', 'HEARTBEAT_TIMEOUT', 'QUALITY_CHECK_FAIL', 'TERMINATE']
const NO_NODE = ['QUEUE', 'PAUSE', 'UNEXEC', 'SKIP', 'WAITING', 'PREPARE_ENV', 'DEPENDENT_WAITING']
const THIRD_PARTY = ['THIRD_PARTY_AGENT_ID', 'THIRD_PARTY_AGENT_ENV', 'THIRD_PARTY_AGENT_NAME']

export function positionCode (pos, depth = 3) {
    if (!pos) return ''
    const parts = [pos.stageIndex, pos.containerIndex, pos.elementIndex]
        .slice(0, depth)
        .filter(n => n !== undefined && n !== null)
        .map(n => n + 1)
    return parts.length ? parts.join('-') : ''
}

const LIVE_STATUS = ['RUNNING', 'PREPARE_ENV', 'QUEUE', 'WAITING']

export function formatElapsed (start, end, status) {
    if (start == null || start === '') return ''
    // 插件 elapsed 是耗时毫秒（远小于时间戳 1e12）；startTime/endTime 才是时间戳
    const TIMESTAMP_MS = 1e12
    let ms
    if (typeof start === 'number' && start < TIMESTAMP_MS) {
        ms = start
    } else if (typeof end === 'number' && end >= TIMESTAMP_MS) {
        ms = end - start
    } else if (LIVE_STATUS.includes(status)) {
        ms = Date.now() - start
    } else {
        return ''
    }
    if (ms < 0) return ''
    const s = Math.round(ms / 1000)
    if (s < 60) return `${s}s`
    const m = Math.floor(s / 60)
    const rs = s % 60
    if (m < 60) return `${m}m${rs ? ` ${rs}s` : ''}`
    return `${Math.floor(m / 60)}h ${m % 60}m`
}

function isThirdParty (job) {
    return !!(job && job.dispatchType && THIRD_PARTY.includes(job.dispatchType.buildType))
}

export function nodeDisplay (job) {
    if (!job) return ''
    if (isThirdParty(job)) {
        return job.nodeName || job.nodeAlias || (job.dispatchType && job.dispatchType.displayName) || ''
    }
    return job.dockerContainerName || ''
}

export function nodeIp (job) {
    if (!isThirdParty(job)) return ''
    return job.hostIp || job.nodeIp || ''
}

export function nodeLink (job, projectId) {
    if (!isThirdParty(job)) return ''
    const nodeHashId = job.nodeHashId || (job.dispatchType && job.dispatchType.value)
    if (!nodeHashId || !projectId) return ''
    return `/console/environment/${projectId}/node/THIRDPARTY?nodeHashId=${nodeHashId}`
}

function toneOf (status) {
    if (status === 'SUCCEED') return 'success'
    if (FAIL.includes(status)) return 'failed'
    if (status === 'CANCELED') return 'canceled'
    if (status === 'PAUSE') return 'pause'
    if (['REVIEWING', 'QUEUE', 'WAITING'].includes(status)) return 'waiting'
    if (status === 'RUNNING') return 'running'
    return 'idle'
}

export function buildConclusion (el, job, ctx = {}) {
    const status = (el && el.status) || ''
    const showNode = status && !NO_NODE.includes(status)
    const jobRef = job || {}
    return {
        status,
        tone: toneOf(status),
        label: STATUS_LABEL[status] || status || '--',
        elapsed: formatElapsed(el.elapsed || el.startTime || el.startEpoch, el.endTime, status),
        node: showNode ? nodeDisplay(jobRef) : '',
        nodeIp: showNode ? nodeIp(jobRef) : '',
        nodeLink: showNode ? nodeLink(jobRef, ctx.projectId) : '',
        message: el.errorMsg || el.reviewAbortSuggest || '',
        errorCode: el.errorCode || '',
        locateLog: FAIL.includes(status),
        askAssistant: FAIL.includes(status) && ctx.askAssistant,
        canRetry: FAIL.includes(status) && ctx.canRetry,
        handleAction: status === 'PAUSE' ? '继续执行' : (status === 'REVIEWING' ? '去处理' : ''),
        emptyText: status === 'UNEXEC' || status === 'SKIP'
            ? '尚未执行，无日志。'
            : (status === 'PAUSE' ? '已在执行前暂停，无日志。' : '')
    }
}

export function formatClock (ts) {
    if (!ts) return ''
    const d = new Date(ts)
    const pad = n => String(n).padStart(2, '0')
    return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const NOT_EXECUTED = ['QUEUE', 'PAUSE', 'UNEXEC', 'SKIP', 'DEPENDENT_WAITING', 'WAITING', 'PREPARE_ENV']
const LIVE_FOR_OUTPUT = ['RUNNING', 'QUEUE', 'WAITING', 'PREPARE_ENV', 'LOOP_WAITING', 'CALL_WAITING']
const OUTPUT_TYPE_NAMES = [
    'string', 'textarea', 'artifact', 'report', 'quality', 'certificate',
    'password', 'enum', 'boolean', 'int', 'long', 'float', 'double'
]

function looksExpr (val) {
    const s = String(val == null ? '' : val)
    return /\$\{\{[\s\S]+?\}\}/.test(s) || /\$\{[^{][^}]*\}/.test(s)
}

function isSecretKey (key) {
    return /password|token|secret|credential|oauth|passwd|pwd/i.test(String(key || ''))
        || String(key || '').startsWith('_')
}

function stringifyVal (val) {
    if (val == null) return ''
    if (typeof val === 'object') {
        try {
            return JSON.stringify(val)
        } catch (e) {
            return String(val)
        }
    }
    return String(val)
}

function toParamRows (objOrArr) {
    if (Array.isArray(objOrArr)) {
        return objOrArr.filter(r => r && (r.key || r.name)).map(r => ({
            key: r.key || r.name,
            value: r.value,
            expr: r.expr,
            secret: !!(r.secret || r.hidden || isSecretKey(r.key || r.name))
        }))
    }
    if (!objOrArr || typeof objOrArr !== 'object') return []
    return Object.keys(objOrArr).map(key => {
        const raw = objOrArr[key]
        let value = raw
        let expr
        if (raw && typeof raw === 'object' && !Array.isArray(raw) && ('value' in raw || 'default' in raw)) {
            value = raw.value != null ? raw.value : raw.default
            expr = raw.expr
        }
        value = stringifyVal(value)
        const secret = isSecretKey(key) || value === '******'
        if (!expr && looksExpr(value)) expr = value
        return { key, value, expr, secret }
    })
}

export function buildParamsModel (element = {}) {
    const status = element.status || ''
    const data = element.data || {}
    const input = toParamRows(data.input)
    const outputRaw = toParamRows(data.output)
    const output = outputRaw.map(r => {
        const v = String(r.value || '').toLowerCase()
        if (OUTPUT_TYPE_NAMES.includes(v)) return { ...r, value: '' }
        return r
    })
    const hasOutputValue = output.some(r => r.value)
    const running = LIVE_FOR_OUTPUT.includes(status)
    return {
        id: element.id,
        status,
        notExecuted: NOT_EXECUTED.includes(status),
        params: {
            input,
            output: hasOutputValue ? output : (running ? [] : output),
            env: toParamRows(element.customEnv),
            outputPending: running && !hasOutputValue,
            open: { input: true, output: !running, env: true }
        }
    }
}

export function buildJobConfigRows (job = {}) {
    const rows = []
    const dt = job.dispatchType || {}
    if (dt.buildType) rows.push({ key: 'dispatchType', value: dt.buildType })
    const image = dt.imageName || dt.value || job.dockerContainerName
    if (image) rows.push({ key: 'image', value: image })
    if (dt.imageVersion) rows.push({ key: 'imageVersion', value: dt.imageVersion })
    const opt = job.jobControlOption || {}
    if (opt.timeoutVar || opt.timeout) {
        rows.push({ key: 'timeout', value: String(opt.timeoutVar || opt.timeout) })
    }
    const mutex = job.mutexGroup || {}
    if (mutex.enable && mutex.mutexGroupName) {
        rows.push({ key: 'mutexGroup', value: mutex.mutexGroupName })
    }
    if (job.jobId) rows.push({ key: 'jobId', value: job.jobId })
    return rows
}
