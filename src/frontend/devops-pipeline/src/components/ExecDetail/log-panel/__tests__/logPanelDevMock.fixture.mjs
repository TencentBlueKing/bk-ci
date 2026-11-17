const LONG_TITLE = '数据开发任务 - 生产环境依赖构建与质量检查 - 确认构建产物及发布步骤在长标题下仍可识别'
const SEARCH_STRESS_LINE_COUNT = 10000
const FORMAT_STRESS_LINE_COUNT = 100000
const FORMAT_STRESS_CHINESE_TARGET = '格式压力中文定位词'
const SEARCH_STRESS_TARGET = 'SEARCH_TARGET_2026'
const SEARCH_STRESS_CHINESE_TARGET = '搜索压力中文命中'
const DEVOPS_LINK_SAMPLE = 'MOCK_DEVOPS_LINK <a href="https://devops.example.com">https://devops.example.com</a>'
const TIMEOUT_ERROR_SAMPLE = 'Script command execution failed with exit code(1) Error message tracking: /tmp/devops_script_user_3831256842535267551.sh:行23: BK_CI_HOOK_BRANCH is ${{ BK_CI_HOOK_BRANCH }}: 错误的替换'
const RUNNING_PROGRESS_DETAIL = {
    progressDetail: {
        progress: { title: '执行主流程', value: 0.2, summary: '1/3' },
        subtasks: {
            title: '子任务进度',
            summary: '1/3',
            items: [
                { name: '初始化环境', status: 'succeeded', progress: 1 },
                { name: '执行主流程', status: 'running', progress: 0.2 },
                { name: '收尾与上报', status: 'pending', progress: 0 }
            ]
        }
    }
}

const QUEUE_DETAIL = {
    visible: true,
    serverTime: Date.now(),
    concurrencyGroup: 'frontend-build-mutex',
    queueTime: Date.now() - 300000,
    aheadCount: 5,
    total: 7,
    running: [{ buildId: 'mock-running', pipelineId: 'mock-running-pipeline', pipelineName: 'yamlv3-trigger', buildNum: 41, status: 'RUNNING', userId: 'fayewang', buildMsg: '推送 master: feat: enhance pipeline trigger ui', startTime: Date.now() - 1860000, detailVisible: true }],
    ahead: Array.from({ length: 5 }, (_, index) => ({
        buildId: `mock-ahead-${54 + index}`,
        pipelineId: `mock-pipeline-${index + 1}`,
        pipelineName: '部署流水线',
        buildNum: 54 + index,
        status: 'QUEUE',
        userId: 'lisi',
        buildMsg: '手动触发',
        queueTime: Date.now() - (index + 6) * 60000,
        detailVisible: true
    }))
}
const QUEUE_LONG_TITLE = '数据开发任务 - 生产环境依赖构建与质量检查 - 需要在狭窄卡片中验证长标题的截断和横向滚动行为'
const QUEUE_NO_RUNNING = {
    ...QUEUE_DETAIL,
    serverTime: Date.now(),
    queueTime: Date.now() - 12000,
    running: [],
    aheadCount: 0,
    total: 1,
    ahead: []
}
const QUEUE_REDACTED = {
    ...QUEUE_DETAIL,
    running: [{ ...QUEUE_DETAIL.running[0], pipelineName: 'SENSITIVE PIPELINE', userId: 'private-user', buildMsg: 'PRIVATE BUILD MESSAGE', detailVisible: false }],
    ahead: [{ ...QUEUE_DETAIL.ahead[0], pipelineName: 'PRIVATE DEPLOYMENT', userId: 'private-operator', buildMsg: 'SENSITIVE MESSAGE', detailVisible: false }]
}
const QUEUE_LONG_AHEAD = {
    ...QUEUE_DETAIL,
    running: [],
    aheadCount: 20,
    total: null,
    ahead: Array.from({ length: 20 }, (_, index) => ({
        ...QUEUE_DETAIL.ahead[0],
        buildId: `mock-ahead-${54 + index}`,
        pipelineId: `mock-pipeline-${index + 1}`,
        pipelineName: index === 0 ? QUEUE_LONG_TITLE : `测试流水线 ${index + 1}`,
        buildNum: 54 + index,
        status: index === 1 ? 'QUEUE_CACHE' : 'QUEUE',
        buildMsg: index === 0 ? '非常长的构建说明，用于观察卡片标题省略和横向滚动' : index === 1 ? '' : '手动触发',
        queueTime: Date.now() - (index + 1) * 60000,
        detailVisible: true
    }))
}

const MOCK_STATES = {
    running: { scope: 'step', status: 'RUNNING', elapsed: 75000, progress: 20, progressDetailData: RUNNING_PROGRESS_DETAIL, nodeName: 'dev-01' },
    canceled: { scope: 'step', status: 'CANCELED', elapsed: 75000, cancelUserId: 'daisyhong', nodeName: 'dev-01' },
    success: { scope: 'step', status: 'SUCCEED', elapsed: 75000, nodeName: 'dev-01' },
    failed: { scope: 'step', status: 'FAILED', elapsed: 75000, errorType: 'USER', errorCode: '2123015', messageKey: 'logPanel.mock.failedReason', summaryError: { errorType: 'USER', errorCode: '80221232322', errorMsgKey: 'logPanel.mock.failedSummary' }, nodeName: 'dev-01', canRetry: true, canSkip: true, locateLog: true },
    pause: { scope: 'step', status: 'PAUSE', labelKey: 'logPanel.mock.pauseLabel', messageKey: 'logPanel.mock.pauseMessage' },
    review: { scope: 'step', status: 'REVIEWING', elapsed: 75000, labelKey: 'logPanel.mock.reviewLabel', messageKey: 'logPanel.mock.reviewMessage', nodeName: 'dev-01' },
    queue: { scope: 'job', status: 'QUEUE', name: '前端构建', startTime: null, startEpoch: null, endTime: null, timeCost: { totalCost: null }, queueDetail: QUEUE_DETAIL, queueExpanded: false },
    'queue-expanded': { scope: 'job', status: 'LOOP_WAITING', name: '前端构建', elapsed: 300000, nodeName: 'bkci-docker-c-1001', startTime: null, startEpoch: null, endTime: null, timeCost: { totalCost: 300000 }, queueDetail: QUEUE_DETAIL, queueExpanded: true },
    'queue-no-running': { scope: 'job', status: 'QUEUE', name: '前端构建', startTime: null, startEpoch: null, endTime: null, timeCost: { totalCost: null }, queueDetail: QUEUE_NO_RUNNING, queueExpanded: true },
    'queue-redacted': { scope: 'job', status: 'QUEUE', name: '前端构建', startTime: null, startEpoch: null, endTime: null, timeCost: { totalCost: null }, queueDetail: QUEUE_REDACTED, queueExpanded: true },
    'queue-long-ahead': { scope: 'job', status: 'QUEUE', name: '前端构建', startTime: null, startEpoch: null, endTime: null, timeCost: { totalCost: null }, queueDetail: QUEUE_LONG_AHEAD, queueExpanded: true },
    unexecuted: { scope: 'step', status: 'UNEXEC' },
    'step-timeout': { scope: 'step', status: 'EXEC_TIMEOUT', elapsed: 75000, errorType: 'USER', errorCode: '2123015', messageKey: 'logPanel.mock.stepTimeoutMessage', summaryError: { errorType: 'USER', errorCode: '80221232322', errorMsg: TIMEOUT_ERROR_SAMPLE }, additionalOptions: { timeoutVar: null, timeout: 30 }, nodeName: 'dev-01', locateLog: true },
    'job-timeout': { scope: 'both', status: 'TERMINATE', elapsed: 75000, errorType: 'USER', errorCode: '2123015', timeoutEvidenceCode: 2199005, messageKey: 'logPanel.mock.jobTimeoutMessage', summaryError: { errorType: 'USER', errorCode: '2123015', errorMsgKey: 'logPanel.mock.jobTimeoutSummary' }, jobControlOption: { timeoutVar: null, timeout: 15 }, nodeName: 'dev-01', locateLog: true },
    'system-canceled': { scope: 'step', status: 'TERMINATE', elapsed: 75000, nodeName: 'dev-01', labelKey: 'logPanel.status.systemCanceled', messageKey: 'logPanel.status.systemCanceledDetail', locateLog: true },
    'parent-canceled': { scope: 'step', status: 'CANCELED', elapsed: 75000, nodeName: 'dev-01', cancelUserId: '', labelKey: 'logPanel.status.parentCanceled', parentCanceled: { nameKey: 'logPanel.mock.parentPipeline', buildNum: 130, cancelUser: 'fayewang' } },
    'running-subtasks': {
        scope: 'step',
        status: 'RUNNING',
        elapsed: 75000,
        progress: 20,
        progressDetailData: RUNNING_PROGRESS_DETAIL,
        nodeName: 'dev-01',
        progressExpanded: true,
        subtasks: [
            { name: '初始化环境', status: 'succeeded', progress: '', timeRange: '2026-04-17 21:11:22', elapsed: '8s' },
            { name: '执行主流程', status: 'running', progress: '处理中…', timeRange: '2026-04-17 21:11:30', elapsed: '处理中…', live: true },
            { name: '收尾与上报', status: 'pending', progress: '', timeRange: '', elapsed: '未开始', idle: true }
        ]
    },
    loading: { scope: 'both', forceLoading: true },
    'long-title': { scope: 'both', name: LONG_TITLE },
    'tabs-delay': { scope: 'step', delayTabsMs: 1000 },
    'search-stress': { scope: 'step' },
    'format-stress-100k': { scope: 'step' }
}

export function createLogPanelSearchStressLogs (environment = process.env.NODE_ENV) {
    if (environment !== 'development') return null

    const targetLines = new Set([5, 1250, 5000, 8750, 9998])
    const chineseTargetLines = new Set([7, 2501, 9990])
    const longLineNo = 5000
    const longTextLength = 10000
    const longMarker = ` ${SEARCH_STRESS_TARGET} `
    const longFillerLength = longTextLength - longMarker.length
    const longText = `${'x'.repeat(Math.floor(longFillerLength / 2))}${longMarker}${'y'.repeat(Math.ceil(longFillerLength / 2))}`
    const baseTimestamp = Date.now() - SEARCH_STRESS_LINE_COUNT * 1000

    const logs = Array.from({ length: SEARCH_STRESS_LINE_COUNT }, (_, index) => {
        const lineNo = index + 1
        const level = lineNo % 29 === 0 ? 'ERROR' : lineNo % 17 === 0 ? 'WARN' : 'INFO'
        const markers = [
            targetLines.has(lineNo) ? SEARCH_STRESS_TARGET : '',
            chineseTargetLines.has(lineNo) ? SEARCH_STRESS_CHINESE_TARGET : ''
        ].filter(Boolean).join(' ')
        const text = lineNo === longLineNo
            ? longText
            : lineNo === 9
                ? DEVOPS_LINK_SAMPLE
                : `Pipeline log line ${lineNo}${markers ? ` ${markers}` : ''}`

        return {
            lineNo,
            timestamp: baseTimestamp + lineNo * 1000,
            level,
            subTag: ['compile', 'test', 'deploy'][(lineNo - 1) % 3],
            text
        }
    })
    for (const lineNo of [5000, 10000]) {
        logs[lineNo - 1].text += ` ${DEVOPS_LINK_SAMPLE}`
    }
    return logs
}

export function createLogPanelFormatStressLogs (environment = process.env.NODE_ENV) {
    if (environment !== 'development') return null

    const timestamp = 1760000000000
    const rows = Array.from({ length: FORMAT_STRESS_LINE_COUNT }, (_, index) => {
        const lineNo = index + 1
        const level = ['INFO', 'WARN', 'ERROR', 'DEBUG'][(lineNo - 1) % 4]
        const subTag = ['compile', 'test', 'deploy'][(lineNo - 1) % 3]
        let text = `format stress line ${lineNo} (${level}/${subTag})`

        if (lineNo === 9) {
            text = DEVOPS_LINK_SAMPLE
        } else if (lineNo >= 10 && lineNo <= 25) {
            const colors = [30, 31, 32, 33, 34, 35, 36, 37, 90, 91, 92, 93, 94, 95, 96, 97]
            const color = colors[lineNo - 10]
            text = `\u001b[${color}mANSI_${color}_START ${lineNo % 2 ? 'bare [0m reset' : 'colored'}\u001b[39m default`
        } else if (lineNo >= 30 && lineNo <= 39) {
            const href = lineNo === 30 ? 'https://example.com/build?a=1&amp;b=2' : lineNo === 31 ? 'javascript:alert(1)' : `https://example.com/${lineNo}`
            text = `<a href="${href}">link ${lineNo} &amp; readable</a> <img src=x onerror=alert(1)>`
        } else if (lineNo >= 40 && lineNo <= 49) {
            const controls = ['##[command]echo hello', '##[info]info marker', '##[warning]warning marker', '##[error]error marker', '##[debug]debug marker', 'left\r\nright', 'A\tB\tC', 'ab\bX', 'inside ##[error] stays literal', 'unsupported \u001b[2J sequence']
            text = controls[lineNo - 40]
        } else if (lineNo >= 100 && lineNo <= 120) {
            if (lineNo === 100) text = '##[group]\u001b[31mouter group\u001b[0m <a href="https://example.com/outer-build">build link</a>'
            else if (lineNo === 105) text = '##[group]inner group'
            else if (lineNo === 110) text = 'nested group unique search needle'
            else if (lineNo === 115) text = '##[endgroup] closing summary needle'
            else if (lineNo === 120) text = '##[endgroup]'
        } else if (lineNo >= 999 && lineNo <= 1002) {
            text = lineNo === 999 ? '##[group]before page boundary' : lineNo === 1002 ? '##[endgroup]' : `boundary group ${lineNo}`
        } else if (lineNo === 50000) {
            const prefix = 'L'.repeat(5000)
            const infix = ` ${FORMAT_STRESS_CHINESE_TARGET} `
            text = `${prefix}${infix}${'R'.repeat(10000 - prefix.length - infix.length)}`
        } else if (lineNo >= 75000 && lineNo <= 75020) {
            text = lineNo === 75000 ? '##[group]second group' : lineNo === 75020 ? '##[endgroup]' : `second group row ${lineNo}`
        } else if (lineNo === 99990) text = '末尾中文样例：收尾完成'
        else if (lineNo === 99991) text = '##[error]末尾错误标记'
        else if (lineNo === 99992) text = '##[group]malformed open group'
        else if (lineNo === 99999) text = '孤立结构后仍可见'
        else if (lineNo === 100000) text = '##[warning]最后一行标记'

        return { lineNo, timestamp: timestamp + lineNo * 1000, level, subTag, text }
    })
    rows[9998].text = '##[endgroup] orphan endgroup marker'
    for (const lineNo of [5000, 10000, 100000]) {
        rows[lineNo - 1].text += ` ${DEVOPS_LINK_SAMPLE}`
    }
    return rows
}

export function filterLogPanelDevMockLogs (logs = [], levels = [], subTag = '') {
    const selectedLevels = new Set((Array.isArray(levels) ? levels : String(levels || '').split(','))
        .map(level => String(level || '').trim().toUpperCase())
        .filter(Boolean))
    const selectedSubTag = String(subTag || '')
    return logs.filter(log => (!selectedLevels.size || selectedLevels.has(String(log.level || '').toUpperCase()))
        && (!selectedSubTag || String(log.subTag || '') === selectedSubTag))
}

export function resolveLogPanelDevMock (queryValue, environment = process.env.NODE_ENV) {
    if (environment !== 'development') return null
    if (!Object.prototype.hasOwnProperty.call(MOCK_STATES, queryValue)) return null
    const state = MOCK_STATES[queryValue]
    return { state: queryValue, ...state }
}

export function applyLogPanelDevMock (source = {}, mock = null, scope = '') {
    if (!mock || (mock.scope !== scope && mock.scope !== 'both')) return source
    const overlay = {}
    const hasStatus = Object.prototype.hasOwnProperty.call(mock, 'status')
    if (hasStatus) {
        Object.assign(overlay, {
            errorType: null,
            errorCode: '',
            errorMsg: '',
            summaryError: null,
            canRetry: false,
            canSkip: false,
            cancelUserId: '',
            nodeName: null,
            progress: null,
            subtasks: null,
            queueDetail: null,
            queueExpanded: false,
            locateLog: false,
            startTime: null,
            startEpoch: null,
            endTime: null,
            elapsed: null,
            timeCost: { totalCost: null }
        })
    }
    if (mock.status) overlay.status = mock.status
    if (mock.name) overlay.name = mock.name
    for (const field of ['errorType', 'errorCode', 'errorMsg', 'summaryError', 'canRetry', 'canSkip', 'cancelUserId', 'nodeName', 'progress', 'subtasks', 'queueDetail', 'queueExpanded', 'locateLog', 'startTime', 'startEpoch', 'endTime', 'elapsed']) {
        if (Object.prototype.hasOwnProperty.call(mock, field)) overlay[field] = mock[field]
    }
    if (mock.additionalOptions) overlay.additionalOptions = { ...source.additionalOptions, ...mock.additionalOptions }
    if (mock.jobControlOption) overlay.jobControlOption = { ...source.jobControlOption, ...mock.jobControlOption }
    if (mock.timeCost) overlay.timeCost = { ...source.timeCost, ...mock.timeCost }
    else if (mock.elapsed !== undefined) overlay.timeCost = { ...source.timeCost, totalCost: mock.elapsed }
    return Object.keys(overlay).length ? { ...source, ...overlay } : source
}

export function isLogPanelDevMockLoading (mock, environment = process.env.NODE_ENV) {
    return environment === 'development' && Boolean(mock?.forceLoading)
}
