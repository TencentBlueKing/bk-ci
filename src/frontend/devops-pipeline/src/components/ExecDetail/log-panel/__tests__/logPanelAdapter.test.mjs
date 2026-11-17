import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import test from 'node:test'
import Vue, { isReactive, markRaw, shallowRef } from 'vue'
import * as adapter from '../logPanelAdapter.js'
import * as devMock from '../logPanelDevMock.js'
import * as logStructure from '../logLineStructure.js'
import * as logTail from '../logPanelTail.js'
import * as virtualWindow from '../logVirtualWindow.js'
import { resolveReviewUsers } from '../../../../utils/reviewUsers.js'
import webSocketMessage from '../../../../utils/webSocketMessage.js'

function loadComponentOptions (path, dependencies, globals = {}) {
    const source = readFileSync(new URL(path, import.meta.url), 'utf8')
    const script = source.match(/<script>([\s\S]*?)<\/script>/)[1]
        .replace(/^\s*import[\s\S]*?\sfrom\s+['"][^'"]+['"];?\s*$/gm, '')
        .replace(/export default\s*\{/, 'return {')
    const values = {
        angleDownIcon: '/mock-angle-down.svg',
        LogExecuteCountSelect: {},
        isConditionSkipped: adapter.isConditionSkipped,
        isJobConditionSkipped: adapter.isJobConditionSkipped,
        resolveConditionSkipReason: adapter.resolveConditionSkipReason,
        countConditionSkippedSteps: adapter.countConditionSkippedSteps,
        resolveCancelConclusion: adapter.resolveCancelConclusion,
        resolveCancelDisplay: adapter.resolveCancelDisplay,
        resolveCancelUserId: adapter.resolveCancelUserId,
        isJobExecTimeoutCancel: adapter.isJobExecTimeoutCancel,
        isJobSystemAgentTermination: adapter.isJobSystemAgentTermination,
        resolveJobExecTimeoutCode: adapter.resolveJobExecTimeoutCode,
        resolveFallbackCancelLabelKey: adapter.resolveFallbackCancelLabelKey,
        resolveRecordElementStatus: adapter.resolveRecordElementStatus,
        ...dependencies,
        ...globals
    }
    const names = Object.keys(values).filter(name => /^[A-Za-z_$][\w$]*$/.test(name))
    return new Function(...names, script)(...names.map(name => values[name]))
}

test('review user resolution honors actual users and falls back only for missing or invalid fields', () => {
    assert.deepEqual(resolveReviewUsers({
        actualReviewUsers: [' actual-a ; actual-b '],
        reviewUsers: ['fallback']
    }), ['actual-a', 'actual-b'])
    assert.deepEqual(resolveReviewUsers({
        actualReviewUsers: [],
        reviewUsers: [' review-a ;review-b ']
    }), [])
    assert.deepEqual(resolveReviewUsers({
        actualReviewUsers: 'invalid',
        reviewUsers: '',
        data: { input: { reviewers: [' input-reviewer '] } }
    }), ['input-reviewer'])
    assert.deepEqual(resolveReviewUsers({ actualReviewUsers: [null, '', 3], reviewUsers: ['ignored'] }), [])
    assert.deepEqual(resolveReviewUsers({
        actualReviewUsers: 'invalid',
        reviewUsers: [null, ''],
        data: { input: { reviewers: [false, ''] } }
    }), [])
    assert.equal(resolveReviewUsers({ actualReviewUsers: ['someone-else'] }).includes('current-user'), false)
})

test('record action supports return-only reads', () => {
    const source = readFileSync(new URL('../../../../store/modules/atom/actions.js', import.meta.url), 'utf8')
    assert.match(source, /skipSetPipelineDetail/)
    assert.match(source, /skipSetPipelineDetail = false, \.\.\.query/)
    assert.match(source, /params:\s*query/)
    assert.match(source, /if \(!skipSetPipelineDetail\)/)
    assert.match(source, /return response\.data/)
    assert.match(source, /throw e/)
    assert.match(source, /if \(!skipSetPipelineDetail\) rootCommit\(commit, FETCH_ERROR, e\)/)
})

test('pipeline log pages do not import the shared websocket bridge', () => {
    const files = [
        '../../../../views/subpages/ExecDetail.vue',
        '../../../BuildHistoryTable/index.vue',
        '../../../../mixins/pipeline-action-mixin.js'
    ]
    files.forEach(path => {
        const source = readFileSync(new URL(path, import.meta.url), 'utf8')
        assert.doesNotMatch(source, /common-lib\/websocket\/bridge/)
        assert.doesNotMatch(source, /watchRealtime/)
    })
})

test('ExecDetail isolates detail requests and restores permission state for a new request', async () => {
    const execDetail = readFileSync(new URL('../../../../views/subpages/ExecDetail.vue', import.meta.url), 'utf8')
    assert.match(execDetail, /fetchPipelineExecDetail/)
    assert.match(execDetail, /detailRequestId:\s*0/)
    assert.match(execDetail, /getDetailRequestKey \(params\) \{[\s\S]*params\.buildNo[\s\S]*params\.executeCount[\s\S]*params\.version[\s\S]*params\.archiveFlag/)
    assert.match(execDetail, /loadPipelineExecDetail \(\) \{[\s\S]*this\.detailRequestId[\s\S]*isCurrentRequest/)
    assert.match(execDetail, /!this\._isBeingDestroyed[\s\S]*!this\._isDestroyed/)
    assert.match(execDetail, /this\.hasNoPermission = false/)
    assert.doesNotMatch(execDetail, /mapState\(\['fetchError'\]\)/)
    assert.doesNotMatch(execDetail, /fetchError \(error\)/)

    const lifecycle = execDetail.match(/loadPipelineExecDetail \(\) \{([\s\S]*?)\n            \},\n            handleRealtimeDetail/)
    assert.ok(lifecycle, 'ExecDetail uses a local request lifecycle')
    const loadPipelineExecDetail = new Function(`return function loadPipelineExecDetail () {${lifecycle[1]}}`)()
    const requests = []
    const applied = []
    const context = {
        detailRequestId: 0,
        _isBeingDestroyed: false,
        _isDestroyed: false,
        hasNoPermission: true,
        isLoading: false,
        routerParams: {
            projectId: 'project-a',
            pipelineId: 'pipeline-a',
            buildNo: 'build-a',
            executeCount: '1',
            version: '1',
            archiveFlag: ''
        },
        getDetailRequestKey: params => [
            params.projectId,
            params.pipelineId,
            params.buildNo,
            params.executeCount,
            params.version,
            params.archiveFlag
        ].join('|'),
        setPipelineDetail: value => applied.push(value),
        fetchPipelineExecDetail: params => new Promise((resolve, reject) => {
            requests.push({ params, resolve, reject })
        })
    }

    const firstRequest = loadPipelineExecDetail.call(context)
    context.routerParams = {
        ...context.routerParams,
        buildNo: 'build-b',
        executeCount: '2',
        version: '2',
        archiveFlag: 'true'
    }
    const secondRequest = loadPipelineExecDetail.call(context)
    assert.equal(context.hasNoPermission, false, 'a new request clears an earlier permission error')
    assert.equal(context.isLoading, true)

    applied.length = 0
    requests[1].resolve({ buildNo: 'build-b' })
    await secondRequest
    requests[0].resolve({ buildNo: 'build-a' })
    await firstRequest
    assert.deepEqual(applied.filter(Boolean), [{ buildNo: 'build-b' }], 'a stale response cannot overwrite the current build')

    context.hasNoPermission = false
    context.routerParams = { ...context.routerParams, buildNo: 'build-c' }
    const thirdRequest = loadPipelineExecDetail.call(context)
    requests[2].reject({ code: 403 })
    await thirdRequest
    assert.equal(context.hasNoPermission, true)

    context._isDestroyed = true
    context.routerParams = { ...context.routerParams, buildNo: 'build-d' }
    const destroyedRequest = loadPipelineExecDetail.call(context)
    requests[3].resolve({ buildNo: 'build-d' })
    await destroyedRequest
    assert.deepEqual(applied.filter(Boolean), [{ buildNo: 'build-b' }], 'a destroyed component ignores late detail data')
})

test('ExecDetail detail watcher reacts to every request identity parameter', async () => {
    const source = readFileSync(new URL('../../../../views/subpages/ExecDetail.vue', import.meta.url), 'utf8')
    assert.match(source, /detailRequestKey \(\) \{[\s\S]*getDetailRequestKey\(this\.routerParams\)/)
    assert.match(source, /detailRequestKey \(\) \{[\s\S]*this\.loadPipelineExecDetail\(\)/)
    assert.equal((source.match(/detailRequestKey \(\)/g) || []).length, 2)

    const state = Vue.observable({
        params: {
            projectId: 'project-a',
            pipelineId: 'pipeline-a',
            buildNo: 'build-a',
            executeCount: '1'
        },
        query: {
            version: '1',
            archiveFlag: ''
        }
    })
    const changes = []
    const vm = new Vue({
        data: () => ({ route: state }),
        computed: {
            routerParams () {
                return { ...this.route.params, ...this.route.query }
            },
            detailRequestKey () {
                return [
                    this.routerParams.projectId,
                    this.routerParams.pipelineId,
                    this.routerParams.buildNo,
                    this.routerParams.executeCount,
                    this.routerParams.version,
                    this.routerParams.archiveFlag
                ].map(value => value ?? '').join('|')
            }
        },
        watch: {
            detailRequestKey () {
                changes.push(this.detailRequestKey)
            }
        }
    }).$mount()

    state.params.buildNo = 'build-b'
    await Vue.nextTick()
    state.params.executeCount = '2'
    await Vue.nextTick()
    state.query.version = '2'
    await Vue.nextTick()
    state.query.archiveFlag = 'true'
    await Vue.nextTick()

    assert.equal(changes.length, 4)
    vm.$destroy()
})

test('detail, history, and pipeline list websocket callbacks use current instances and reconnect refreshes', () => {
    const execDetail = readFileSync(new URL('../../../../views/subpages/ExecDetail.vue', import.meta.url), 'utf8')
    const history = readFileSync(new URL('../../../BuildHistoryTable/index.vue', import.meta.url), 'utf8')
    const manageList = readFileSync(new URL('../../../../views/PipelineList/PipelineManageList.vue', import.meta.url), 'utf8')
    assert.match(execDetail, /webSocketMessage\.installWsMessage\(\(data\) => this\.handleRealtimeDetail\(data\)\)/)
    assert.match(execDetail, /webSocketMessage\.registeOnReconnect\(\(\) => \{\s*this\.loadPipelineExecDetail\(\)/)
    assert.match(execDetail, /beforeDestroy \(\) \{[\s\S]*webSocketMessage\.unInstallWsMessage\(\)/)
    assert.match(history, /webSocketMessage\.installWsMessage\(\(\) => this\.requestHistory\(\)\)/)
    assert.match(history, /webSocketMessage\.registeOnReconnect\(\(\) => this\.requestHistory\(\)\)/)
    assert.doesNotMatch(history, /realtimeHistory|watchRealtime/)
    assert.match(manageList, /webSocketMessage\.installWsMessage\(\(data\) => this\.\$refs\.pipelineBox\?\.updatePipelineStatus\?\.\(data\)\)/)
    assert.match(manageList, /webSocketMessage\.registeOnReconnect\(\(\) => this\.\$refs\.pipelineBox\?\.refresh\?\.\(\)\)/)
})

test('a realtime detail update invalidates an older in-flight HTTP snapshot', async () => {
    const execDetail = readFileSync(new URL('../../../../views/subpages/ExecDetail.vue', import.meta.url), 'utf8')
    const lifecycle = execDetail.match(/loadPipelineExecDetail \(\) \{([\s\S]*?)\n            \},\n            handleRealtimeDetail/)
    const realtime = execDetail.match(/handleRealtimeDetail \(data\) \{([\s\S]*?)\n            \},\n            \.\.\.mapActions/)
    assert.ok(lifecycle, 'detail request lifecycle is available')
    assert.ok(realtime, 'realtime detail updates have an explicit lifecycle')
    const loadPipelineExecDetail = new Function(`return function loadPipelineExecDetail () {${lifecycle[1]}}`)()
    const handleRealtimeDetail = new Function(`return function handleRealtimeDetail (data) {${realtime[1]}}`)()
    const requests = []
    const applied = []
    const context = {
        detailRequestId: 0,
        _isBeingDestroyed: false,
        _isDestroyed: false,
        hasNoPermission: false,
        isLoading: false,
        routerParams: {
            projectId: 'project-a',
            pipelineId: 'pipeline-a',
            buildNo: 'build-a',
            executeCount: '1',
            version: '1',
            archiveFlag: ''
        },
        getDetailRequestKey: params => [
            params.projectId,
            params.pipelineId,
            params.buildNo,
            params.executeCount,
            params.version,
            params.archiveFlag
        ].join('|'),
        setPipelineDetail: value => applied.push(value),
        fetchPipelineExecDetail: params => new Promise((resolve, reject) => {
            requests.push({ params, resolve, reject })
        })
    }

    const request = loadPipelineExecDetail.call(context)
    handleRealtimeDetail.call(context, { buildNo: 'build-a', status: 'SUCCEED' })
    requests[0].resolve({ buildNo: 'build-a', status: 'RUNNING' })
    await request

    assert.deepEqual(applied.filter(Boolean), [{ buildNo: 'build-a', status: 'SUCCEED' }])
    assert.equal(context.isLoading, false)
})

test('stage approval refreshes through the guarded detail lifecycle across build switches', async () => {
    const execDetail = readFileSync(new URL('../../../../views/subpages/ExecDetail.vue', import.meta.url), 'utf8')
    assert.match(execDetail, /<stage-review-panel[\s\S]*?@approve="loadPipelineExecDetail"/)
    assert.doesNotMatch(execDetail, /requestPipelineExecDetail/)

    const lifecycle = execDetail.match(/loadPipelineExecDetail \(\) \{([\s\S]*?)\n            \},\n            handleRealtimeDetail/)
    assert.ok(lifecycle, 'detail request lifecycle is available')
    const loadPipelineExecDetail = new Function(`return function loadPipelineExecDetail () {${lifecycle[1]}}`)()
    const requests = []
    const applied = []
    const context = {
        detailRequestId: 0,
        _isBeingDestroyed: false,
        _isDestroyed: false,
        hasNoPermission: false,
        isLoading: false,
        routerParams: { projectId: 'project-a', pipelineId: 'pipeline-a', buildNo: 'build-a', executeCount: '1' },
        getDetailRequestKey: params => [params.projectId, params.pipelineId, params.buildNo, params.executeCount].join('|'),
        setPipelineDetail: value => applied.push(value),
        fetchPipelineExecDetail: params => new Promise((resolve, reject) => {
            requests.push({ params, resolve, reject })
        })
    }

    const approveA = loadPipelineExecDetail.call(context)
    context.routerParams = { ...context.routerParams, buildNo: 'build-b' }
    const loadB = loadPipelineExecDetail.call(context)
    assert.equal(applied[applied.length - 1], null, 'switching builds clears the previous detail')
    assert.equal(context.isLoading, true)

    requests[1].resolve({ buildNo: 'build-b' })
    await loadB
    requests[0].resolve({ buildNo: 'build-a' })
    await approveA
    assert.deepEqual(applied[applied.length - 1], { buildNo: 'build-b' }, 'a late approval refresh cannot overwrite the current build')

    context.routerParams = { ...context.routerParams, buildNo: 'build-a' }
    const approveAgain = loadPipelineExecDetail.call(context)
    context.routerParams = { ...context.routerParams, buildNo: 'build-b' }
    const reloadB = loadPipelineExecDetail.call(context)
    requests[3].resolve({ buildNo: 'build-b' })
    await reloadB
    requests[2].reject({ code: 403 })
    await approveAgain
    assert.equal(context.hasNoPermission, false, 'a late 403 from another build does not mark the current build as forbidden')
    assert.deepEqual(applied[applied.length - 1], { buildNo: 'build-b' })

    context.routerParams = { ...context.routerParams, buildNo: 'build-c' }
    const loadC = loadPipelineExecDetail.call(context)
    requests[4].reject({ code: 500 })
    await loadC
    assert.equal(applied[applied.length - 1], null, 'a failed request does not keep showing the previous build')
    assert.equal(context.isLoading, false)
    assert.equal(context.hasNoPermission, false)
})

test('pipeline list keeps websocket callbacks independent from layout component instances', () => {
    const pipelineAction = readFileSync(new URL('../../../../mixins/pipeline-action-mixin.js', import.meta.url), 'utf8')
    const manageList = readFileSync(new URL('../../../../views/PipelineList/PipelineManageList.vue', import.meta.url), 'utf8')
    assert.doesNotMatch(pipelineAction, /common-lib\/websocket\/bridge|watchRealtime|realtimeStatus/)
    assert.match(manageList, /webSocketMessage\.installWsMessage\(\(data\) => this\.\$refs\.pipelineBox\?\.updatePipelineStatus\?\.\(data\)\)/)
    assert.match(manageList, /webSocketMessage\.registeOnReconnect\(\(\) => this\.\$refs\.pipelineBox\?\.refresh\?\.\(\)\)/)
    assert.doesNotMatch(manageList, /realtimeStatus/)

})

test('atom output lookup skips requests until elements are initialized', () => {
    const source = readFileSync(new URL('../../../../store/modules/atom/actions.js', import.meta.url), 'utf8')
    assert.match(source, /fetchAtomsOutput:[\s\S]*?const stages = state\.pipeline\?\.stages[\s\S]*?if \(!Array\.isArray\(stages\) \|\| !stages\.length\) return[\s\S]*?const elements = getters\.getAllElements\(stages\)[\s\S]*?if \(!Array\.isArray\(elements\) \|\| !elements\.length\) return[\s\S]*?const arr = elements\.map/)
})

test('Step keeps configuration as the top tab and exposes parameters as an inline setting view', () => {
    const source = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    assert.match(source, /\{ name: 'progress', show: true \}/)
    assert.match(source, /\{ name: 'setting', show: true \}/)
    assert.match(source, /return 'log'/)
    assert.match(source, /CONFIG:\s*'setting'/)
    assert.doesNotMatch(source, /name: 'params', show:/)
    assert.match(source, /settingView:\s*'config'/)
    assert.match(source, /paramsViewEnabled:\s*false/)
    assert.match(source, /:aria-selected="String\(currentTab === tab\.name\)"/)
    assert.match(source, /v-if="paramsViewEnabled && currentTab === 'setting' && settingView === 'config'"[\s\S]*class="plugin-setting-tools"[\s\S]*\$t\('logPanel\.params\.viewParams'\)/)
    assert.doesNotMatch(source, /<reference-variable|import ReferenceVariable|globalEnvs|mapState\('atom'|allow-tool-overflow/)
    assert.match(source, /v-if="currentTab === 'setting' && settingView === 'params'"/)
    assert.match(source, /v-show="currentTab === key && \(key !== 'setting' \|\| settingView === 'config'\)"/)
    assert.doesNotMatch(source, /if \(this\.isRunningStatus && this\.hasProgressTab\)/)
    assert.match(source, /if \(key === 'progress'\) return/)
    assert.match(source, /detailData: this\.progressPanelData/)
    assert.match(source, /this\.progressDetailData = data \|\| null/)
    assert.match(source, /this\.devLogMock \? \(this\.devLogMock\.progressDetailData \|\| null\) : this\.progressDetailData/)
    assert.match(source, /tabLabel \(name\)/)
    assert.match(source, /name === 'setting'.*execDetail\.setting/)
    assert.doesNotMatch(source, /name === 'params'.*execDetail\.parameter/)
    assert.match(source, /currentTab === 'setting' && settingView === 'params'[\s\S]*shortcut-label="\$t\('logPanel\.params\.backConfig'\)"[\s\S]*@view-config="showConfigView"/)
    assert.match(source, /showParamsView \(\) \{\s*if \(!this\.paramsViewEnabled\) return\s*this\.currentTab = 'setting'[\s\S]*this\.settingView = 'params'/)
    assert.match(source, /showConfigView \(\)[\s\S]*this\.settingView = 'config'/)
    assert.match(source, /selectTab \(name\)[\s\S]*if \(name === 'setting'\) this\.settingView = 'config'[\s\S]*this\.currentTab = name/)
    assert.match(source, /\.plugin-setting-tools\s*\{[\s\S]*display:\s*flex;[\s\S]*align-items:\s*center;[\s\S]*flex:\s*0 0 auto;[\s\S]*width:\s*max-content;[\s\S]*min-width:\s*max-content;[\s\S]*margin-left:\s*auto;[\s\S]*white-space:\s*nowrap/)
    assert.match(source, /::v-deep \.plugin-setting-tools > \.head-tool:last-child\s*\{\s*margin-right:\s*0/)
    assert.match(source, /::v-deep \.plugin-setting-tools \.reference-var\s*\{\s*padding:\s*0 0 21px;\s*margin-bottom:\s*-21px/)
    assert.match(source, /::v-deep \.plugin-setting-tools \.env-layout\s*\{\s*top:\s*60px/)
    assert.match(source, /::v-deep \.plugin-setting-tools \.env-layout-container::before\s*\{\s*content:\s*none/)
    assert.match(source, /::v-deep \.plugin-setting-tools \.reference-var > span\s*\{\s*position:\s*relative/)
    assert.match(source, /::v-deep \.plugin-setting-tools \.reference-var > span::after\s*\{\s*content:\s*'';[\s\S]*position:\s*absolute;[\s\S]*top:\s*calc\(100% \+ 17px\);[\s\S]*left:\s*50%;[\s\S]*width:\s*12px;[\s\S]*height:\s*7px;[\s\S]*background:\s*#fff;[\s\S]*clip-path:\s*polygon\(50% 0, 100% 100%, 0 100%\);[\s\S]*transform:\s*translateX\(-50%\);[\s\S]*visibility:\s*hidden;[\s\S]*pointer-events:\s*none;[\s\S]*z-index:\s*1113/)
    assert.match(source, /\.plugin-setting-tools \.reference-var:hover > span::after\s*\{\s*visibility:\s*visible/)
    assert.match(source, /::v-deep \.plugin-setting-tools \.env-layout \.bk-table\s*\{\s*border-top:\s*0/)
    assert.doesNotMatch(source, /\.plugin-setting-tools \.reference-var\s*\{[^}]*position:\s*relative|\.plugin-setting-tools \.reference-var::after/)
    assert.match(source, /setting:\s*\{\s*component: AtomContent/)
    assert.match(source, /@go-condition="handleMockablePanelAction\('condition'\)"/)
    assert.doesNotMatch(source, /<bk-sideslider/)
    assert.match(source, /currentTab === 'setting' && this\.settingView === 'params'/)
    assert.match(source, /this\.currentTab !== 'setting'[\s\S]*this\.settingView !== 'params'/)
    assert.match(source, /settingView \(view\)[\s\S]*this\.currentTab === 'setting' && view === 'params'[\s\S]*this\.fetchParamsRecord\(\)[\s\S]*this\.invalidateParamsRecord\(\)/)
    const progress = readFileSync(new URL('../../../ProgressDetailPanel.vue', import.meta.url), 'utf8')
    assert.match(progress, /if \(props\.detailData\) \{[\s\S]*progressData\.value = props\.detailData[\s\S]*return/)
    assert.match(progress, /const canRefresh = computed\(\(\) => shouldRefresh\.value && !props\.controlled && !props\.detailData\)/)
})

test('Step parameter history is return-only and rejects stale responses', () => {
    const source = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    assert.match(source, /resolveParamsElementSource/)
    assert.match(source, /buildParamsRecordRequest/)
    assert.match(source, /shouldFetchParamsRecord/)
    assert.match(source, /paramsExecDetail:\s*null/)
    assert.match(source, /paramsRequestId:\s*0/)
    assert.match(source, /paramsLoading:\s*false/)
    assert.match(source, /paramsLoadError:\s*''/)
    assert.match(source, /`\$\{this\.currentElement\.id\}\|\$\{this\.activeExecuteCount\}`/)
    assert.match(source, /requestId === this\.paramsRequestId/)
    assert.match(source, /this\.paramsRequestId \+= 1/)
    assert.match(source, /<log-params-view[\s\S]*:loading="paramsLoading"/)
    assert.match(source, /<log-params-view[\s\S]*:load-error="paramsLoadError"/)
    assert.doesNotMatch(source, /<log-params-view[\s\S]*v-bkloading=/)
})

test('parameter subview shortcut text is configurable while preserving its navigation event', () => {
    const source = readFileSync(new URL('../LogParamsView.vue', import.meta.url), 'utf8')
    assert.match(source, /shortcutLabel \|\| \$t\('logPanel\.params\.viewConfig'\)/)
    assert.match(source, /shortcutLabel: \{ type: String, default: '' \}/)
    assert.match(source, /@click="\$emit\('view-config'\)"/)
    for (const locale of ['zh-CN', 'en-US', 'ja-JP']) {
        const messages = JSON.parse(readFileSync(new URL(`../../../../../../locale/pipeline/${locale}.json`, import.meta.url), 'utf8'))
        assert.ok(messages.logPanel.params.viewParams)
        assert.ok(messages.logPanel.params.backConfig)
    }
})

test('Job exposes only log and complete inline configuration', () => {
    const source = readFileSync(new URL('../../job.vue', import.meta.url), 'utf8')
    assert.match(source, /\$t\('execDetail\.setting'\)/)
    assert.match(source, /<container-content\s+v-if="currentTab === 'setting'"/)
    assert.doesNotMatch(source, /JobConfigView/)
    assert.doesNotMatch(source, /buildJobConfigRows/)
    assert.doesNotMatch(source, /jobConfigRows/)
    assert.doesNotMatch(source, /configOpen/)
    assert.doesNotMatch(source, /<bk-sideslider/)
})

test('Job download uses the parent Job link and execute count without a selected StepLogPanel', () => {
    const location = { href: '' }
    const component = loadComponentOptions('../JobLogPanel.vue', { ...adapter, StepLogPanel: {}, LogStatus: {} }, { location })
    const vm = { $attrs: { 'download-link': '/api/job/log?jobId=root', 'execute-count': '4' }, $refs: {} }
    Object.keys(component.methods).forEach((name) => {
        vm[name] = component.methods[name].bind(vm)
    })
    assert.equal(vm.download(), '/api/job/log?jobId=root&executeCount=4')
    assert.equal(location.href, '/api/job/log?jobId=root&executeCount=4')
    assert.equal(vm.$refs.panel, undefined)
})

test('job directory marks completed plugins from an earlier execution as skipped', () => {
    const currentRound = 2
    const completed = { id: 'completed', executeCount: 1, status: 'SUCCEED', timeCost: { totalCost: 22000 }, elapsed: 22000 }
    const queued = { id: 'queued', executeCount: 1, status: 'QUEUE', timeCost: { totalCost: null } }
    const unexecuted = { id: 'unexecuted', executeCount: 1, status: 'UNEXEC' }
    const setup = { id: 'setup', executeCount: 1, status: 'SUCCEED', isSetup: true }
    assert.equal(adapter.isRoundSkippedStep(completed, currentRound), true)
    assert.equal(adapter.isRoundSkippedStep(queued, currentRound), false)
    assert.equal(adapter.isRoundSkippedStep(unexecuted, currentRound), false)
    assert.equal(adapter.isRoundSkippedStep({ id: 'waiting', executeCount: 1, status: 'QUALITY_CHECK_WAIT' }, currentRound), false)
    assert.equal(adapter.isRoundSkippedStep(setup, currentRound), false)
    assert.equal(adapter.isRoundSkippedStep({ id: 'missing', status: 'SUCCEED' }, currentRound), false)
    assert.deepEqual(adapter.resolveJobDirectoryItems([completed, queued, setup], currentRound), [
        { ...completed, status: 'SKIP', timeCost: null, elapsed: null, startTime: null, startEpoch: null, endTime: null, roundSkipped: true },
        queued,
        setup
    ])
})

test('job directory follows the prototype rows and shows only elapsed time in the right column', () => {
    const source = readFileSync(new URL('../JobLogPanel.vue', import.meta.url), 'utf8')
    const template = source.match(/<template>[\s\S]*<\/template>/)?.[0] || ''
    assert.doesNotMatch(source, /job-log-index/)
    assert.doesNotMatch(template, /statusLabel\(item\.status\) \}\}<\/small>|elapsed\(item\) \|\|/)
    assert.match(template, /<small v-if="elapsedById\[item\.id\]">\{\{ elapsedById\[item\.id\] \}\}<\/small>/)
    assert.match(source, /\.job-log-directory button \{[^}]*height: 36px;[^}]*padding: 0 12px;/)
    assert.match(source, /\.job-log-name \.log-status \{ flex: 0 0 16px; width: 16px; height: 16px; padding: 0; line-height: 16px; \}/)
    const component = loadComponentOptions('../JobLogPanel.vue', { ...adapter, StepLogPanel: {}, LogStatus: {} })
    const vm = {
        elapsedNow: 0,
        pluginList: [
            { id: 'setup', status: 'SUCCEED', isSetup: true },
            { id: 'done', status: 'SUCCEED', timeCost: { totalCost: 0 } },
            { id: 'failed', status: 'FAILED', timeCost: { totalCost: 65000 } },
            { id: 'never', status: 'UNEXEC' }
        ]
    }
    vm.elapsed = component.methods.elapsed.bind(vm)
    assert.deepEqual(component.computed.elapsedById.call(vm), { setup: '', done: '0s', failed: '1m 5s', never: '' })
})

test('job directory does not mount logs for a plugin skipped in the current execution round', () => {
    const source = readFileSync(new URL('../JobLogPanel.vue', import.meta.url), 'utf8')
    const component = loadComponentOptions('../JobLogPanel.vue', { ...adapter, StepLogPanel: {}, LogStatus: {} })
    assert.equal(component.computed.canLoadStepLogForSelection.call({ selectedItem: { status: 'SUCCEED', roundSkipped: true } }), false)
    assert.match(source, /selectedItem\.roundSkipped/)
    assert.match(source, /logPanel\.status\.skippedInRound/)
})

test('queue item splitting keeps visible cards separate from overflow cards', () => {
    const items = ['one', 'two', 'three', 'four', 'five', 'six']
    assert.deepEqual(adapter.splitQueueItems(items, 3), {
        visible: ['one', 'two', 'three'],
        overflow: ['four', 'five', 'six']
    })
    assert.deepEqual(adapter.splitQueueItems(items, 0), {
        visible: [],
        overflow: items
    })
})

test('queue detail keeps returned ahead items visible when the reported count is zero', () => {
    const component = loadComponentOptions('../JobQueueDetail.vue', {
        resolveQueueCardStatusLabel: () => '',
        splitQueueItems: adapter.splitQueueItems
    })
    const sections = component.computed.queueSections.call({
        queue: { running: [], ahead: [{ buildNum: 1 }, { buildNum: 2 }], aheadCount: 0 }
    })
    assert.deepEqual(sections.map(section => section.kind), ['queued'])
    assert.equal(sections[0].visible.length, 2)
})

test('Job summary view and its locale copy are removed', () => {
    assert.equal(existsSync(new URL('../JobConfigView.vue', import.meta.url)), false)
    for (const locale of ['zh-CN', 'en-US', 'ja-JP']) {
        const source = readFileSync(new URL(`../../../../../../locale/pipeline/${locale}.json`, import.meta.url), 'utf8')
        assert.doesNotMatch(source, /"viewFullConfig"/)
        assert.doesNotMatch(source, /"jobConfig"/)
    }
})

test('legacy review entry resolves id-only tasks to the full atom before permission checks', () => {
    const source = readFileSync(new URL('../../../ExecPipeline.vue', import.meta.url), 'utf8')
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    assert.match(source, /this\.curPipelineAllElements\.find\(element => element\.id === atom\?\.id\)/)
    assert.match(source, /import \{ resolveReviewUsers \} from '\@\/utils\/reviewUsers'/)
    assert.match(source, /if \(!resolveReviewUsers\(fullAtom\)\.includes\(this\.userName\)\) \{\s*this\.\$showTips\(\{ theme: 'error', message: this\.\$t\('noAuthToCheck'\) \}\)\s*return/)
    assert.match(source, /this\.currentAtom = fullAtom/)
    assert.match(source, /this\.toggleCheckDialog\(true\)/)
    assert.doesNotMatch(plugin, /resolveReviewUsers|canReview/)
})

test('new Job and Step actions reuse the existing execDetailPanel ref', () => {
    const source = readFileSync(new URL('../../../../views/subpages/ExecDetail.vue', import.meta.url), 'utf8')
    assert.match(source, /ref="execDetailPanel"/)
    assert.match(source, /this\.\$refs\.execDetailPanel\.handleContinue/)
    assert.match(source, /this\.\$refs\.execDetailPanel\.reviewAtom/)
    assert.doesNotMatch(source, /this\.\$refs\.currentPanel/)
    const reviewHandler = source.match(/async handlePluginReview \(atom\) \{([\s\S]*?)\n            \},\n            normalizeIndex/)
    assert.ok(reviewHandler)
    assert.match(reviewHandler[1], /typeof this\.\$refs\.execDetailPanel\.reviewAtom !== 'function'/)
    assert.match(reviewHandler[1], /await this\.\$refs\.execDetailPanel\.reviewAtom\(atom\)/)
    assert.doesNotMatch(reviewHandler[1], /reviewUsers|\.includes\(/)
})

test('parameter view exposes the approved read-only configuration shortcut', () => {
    const source = readFileSync(new URL('../LogParamsView.vue', import.meta.url), 'utf8')
    assert.match(source, /showConfigShortcut: \{ type: Boolean, default: false \}/)
    assert.match(source, /@click="\$emit\('view-config'\)"/)
    for (const locale of ['zh-CN', 'en-US', 'ja-JP']) {
        const messages = JSON.parse(readFileSync(new URL(`../../../../../../locale/pipeline/${locale}.json`, import.meta.url), 'utf8'))
        assert.ok(messages.logPanel.params.viewConfig)
    }
})

test('unexecuted parameter state has two-line copy in all supported locales', () => {
    const source = readFileSync(new URL('../LogParamsView.vue', import.meta.url), 'utf8')
    assert.match(source, /params\.notExecutedTitle/)
    assert.match(source, /params\.notExecutedDescription/)
    for (const locale of ['zh-CN', 'en-US', 'ja-JP']) {
        const messages = JSON.parse(readFileSync(new URL(`../../../../../../locale/pipeline/${locale}.json`, import.meta.url), 'utf8'))
        assert.ok(messages.logPanel.params.notExecutedTitle)
        assert.ok(messages.logPanel.params.notExecutedDescription)
    }
})

test('queue position and ahead duration copy exist in all supported locales', () => {
    for (const locale of ['zh-CN', 'en-US', 'ja-JP']) {
        const messages = JSON.parse(readFileSync(new URL(`../../../../../../locale/pipeline/${locale}.json`, import.meta.url), 'utf8'))
        assert.ok(messages.logPanel.queue.positionTotalValue)
        assert.ok(messages.logPanel.queue.queuedMinutes)
    }
})

test('tail cursor handles 150000 log lines without argument spreading', () => {
    const lines = Array.from({ length: 150000 }, (_, index) => ({ lineNo: index + 1, timestamp: index + 1000 }))
    const startedAt = Date.now()
    assert.deepEqual(logTail.nextAfterCursor(lines), { startLineNo: 150000, sinceTimestamp: 150999 })
    assert.ok(Date.now() - startedAt < 5000)
})

test('parameter view uses the approved preview search copy and state contracts', () => {
    const source = readFileSync(new URL('../LogParamsView.vue', import.meta.url), 'utf8')
    assert.match(source, /visibleParamRows/)
    assert.match(source, /paramCopyText/)
    assert.match(source, /loading:\s*\{\s*type:\s*Boolean/)
    assert.match(source, /open:\s*\{\s*input:\s*true,\s*output:\s*true,\s*env:\s*false\s*\}/)
    assert.match(source, /v-bk-tooltips/)
    assert.match(source, /expandAll/)
    assert.match(source, /emptyText/)
    assert.match(source, /copyValue/)
    assert.match(source, /loadError:\s*\{\s*type:\s*String/)
    assert.match(source, /\$emit\('retry'\)/)
})

test('parameter search expands only sections with hits', () => {
    const source = readFileSync(new URL('../LogParamsView.vue', import.meta.url), 'utf8')
    assert.match(source, /if \(this\.searching\) return sec\.visibleRows\.length > 0/)
    assert.doesNotMatch(source, /if \(this\.searching\) return true/)
})

test('conclusion bar uses REQ completed-subtask copy', () => {
    const source = readFileSync(new URL('../LogConclusionBar.vue', import.meta.url), 'utf8')
    assert.match(source, /logPanel\.subtaskSummary/)
    assert.doesNotMatch(source, /`\$\{done\}\/\$\{list\.length\}`/)
    assert.match(source, /\.lp-status\s*\{[^}]*box-sizing:\s*border-box;\s*height:\s*37px/)
    assert.match(source, /\.lp-status-row\s*\{[^}]*height:\s*36px;\s*min-height:\s*36px;\s*padding:\s*8px 16px;/)
    assert.match(source, /\.lp-status\.is-expanded\s*\{\s*height:\s*auto/)
    assert.match(source, /\.lp-subtasks\s*\{[^}]*padding:\s*12px 64px/)
})

test('parameter expression tooltip explicitly excludes defensive secret rows', () => {
    const source = readFileSync(new URL('../LogParamsView.vue', import.meta.url), 'utf8')
    assert.match(
        source,
        /v-if="row\.expr && !row\.secret"\s+v-bk-tooltips="expressionTooltip\(row\)"/
    )
    assert.doesNotMatch(source, /v-if="row\.expr"\s+v-bk-tooltips=/)

    const defensiveSecret = { secret: true, expr: 'TOP_SECRET' }
    const entersExpressionTooltipBranch = Boolean(defensiveSecret.expr && !defensiveSecret.secret)
    assert.equal(entersExpressionTooltipBranch, false)
})

test('parameter copy control stays keyboard focusable while visually hidden', () => {
    const source = readFileSync(new URL('../LogParamsView.vue', import.meta.url), 'utf8')
    assert.doesNotMatch(source, /\.pm-copy\s*\{[\s\S]*visibility:\s*hidden/)
    assert.match(source, /tr:focus-within \.pm-copy/)
    assert.match(source, /\.pm-copy:focus-visible/)
    assert.match(source, /outline:\s*2px solid #3a84ff/)
})

test('normalizes non-empty level combinations and serializes ALL explicitly', () => {
    assert.deepEqual(adapter.normalizeLogLevels(['DEBUG', 'INFO', 'DEBUG']), ['INFO', 'DEBUG'])
    assert.equal(adapter.serializeLogLevels(['INFO', 'WARN', 'ERROR', 'DEBUG']), 'INFO,WARN,ERROR,DEBUG')
    assert.equal(adapter.formatLevelSummary(['INFO', 'WARN', 'ERROR', 'DEBUG']), 'ALL')
    assert.equal(adapter.formatLevelSummary(['INFO', 'WARN', 'ERROR', 'DEBUG'], false), 'INFO,WARN,ERROR,DEBUG')
    assert.equal(adapter.formatLevelSummary(['INFO', 'WARN', 'ERROR', 'DEBUG'], true), 'ALL')
})

test('toolbar keeps ALL distinct from explicit four-level selection without duplicate requests', () => {
    const toolbar = loadComponentOptions('../LogPanelToolbar.vue', {
        formatLevelSummary: adapter.formatLevelSummary,
        normalizeLogLevels: adapter.normalizeLogLevels
    })
    const controls = { levels: ['INFO', 'WARN', 'ERROR', 'DEBUG'], levelAll: false }
    const emitted = []
    const vm = {
        controls,
        disabled: false,
        $set: (target, key, value) => {
            target[key] = value
        },
        $emit: (name, value) => emitted.push([name, value])
    }
    Object.defineProperty(vm, 'selectedLevels', { get: () => controls.levels })
    Object.assign(vm, toolbar.methods)
    vm.selectAllLevels()
    assert.equal(controls.levelAll, true)
    assert.deepEqual(controls.levels, ['INFO', 'WARN', 'ERROR', 'DEBUG'])
    assert.equal(emitted.length, 0, 'ALL state alone does not reload identical effective levels')

    vm.toggleLevel('ERROR', { target: { checked: true } })
    assert.equal(controls.levelAll, false)
    assert.deepEqual(controls.levels, ['ERROR'])
    vm.toggleLevel('WARN', { target: { checked: true } })
    assert.deepEqual(controls.levels, ['WARN', 'ERROR'])
    vm.toggleLevel('ERROR', { target: { checked: false } })
    assert.deepEqual(controls.levels, ['WARN'])
    vm.toggleLevel('WARN', { target: { checked: false } })
    assert.equal(controls.levelAll, true, 'removing the last explicit level returns to ALL')
    assert.deepEqual(controls.levels, ['INFO', 'WARN', 'ERROR', 'DEBUG'])

    controls.levelAll = false
    controls.levels = ['INFO', 'WARN', 'ERROR']
    vm.toggleLevel('DEBUG', { target: { checked: true } })
    assert.equal(controls.levelAll, false)
    assert.equal(toolbar.computed.levelSummary.call(vm), 'INFO,WARN,ERROR,DEBUG')
    assert.equal(emitted.filter(([name]) => name === 'levels-change').length, 5)
})

test('log search matching is case-insensitive, literal, and cycles current hits', () => {
    const logs = [
        { lineNo: 10, text: 'Build A+B completed' },
        { lineNo: 11, text: '\u001b[31m[ERROR]\u001b[0m a+b failed' },
        { lineNo: 12, text: 'No match' }
    ]
    assert.deepEqual(adapter.findLogMatchIds(logs, 'A+B'), [10, 11])
    assert.deepEqual(adapter.findLogMatchIds(logs, '[error]'), [11])
    assert.deepEqual(adapter.findLogMatchIds(logs, '.*'), [])
    assert.deepEqual(adapter.findLogMatchIds(logs, ''), [])
    assert.equal(adapter.resolveNextLogMatchIndex(0, 0, 1), 0)
    assert.equal(adapter.resolveNextLogMatchIndex(1, 0, 1), 1)
    assert.equal(adapter.resolveNextLogMatchIndex(1, 1, 1), 1)
    assert.equal(adapter.resolveNextLogMatchIndex(3, 1, 1), 2)
    assert.equal(adapter.resolveNextLogMatchIndex(3, 3, 1), 1)
    assert.equal(adapter.resolveNextLogMatchIndex(3, 1, -1), 3)
})

test('deferred search scrolls are ignored after the query, target, or log generation changes', () => {
    const canScroll = adapter.shouldApplyLogSearchScroll
    const current = {
        generation: 4,
        currentGeneration: 4,
        search: 'compile',
        currentSearch: 'compile',
        target: 12,
        currentTarget: 12,
        matchIds: [12, 20]
    }
    assert.equal(canScroll(current), true)
    assert.equal(canScroll({ ...current, currentGeneration: 5 }), false)
    assert.equal(canScroll({ ...current, currentSearch: '' }), false)
    assert.equal(canScroll({ ...current, currentSearch: 'link' }), false)
    assert.equal(canScroll({ ...current, currentTarget: 20 }), false)
    assert.equal(canScroll({ ...current, matchIds: [20] }), false)
})

test('search backfill continues only with active search, usable history, and page progress', () => {
    const shouldContinue = adapter.shouldContinueSearchBackfill
    assert.equal(shouldContinue({ searching: true, hasBefore: true, succeeded: true, addedCount: 5 }), true)
    assert.equal(shouldContinue({ searching: true, hasBefore: true, succeeded: true, addedCount: 0, previousCursor: 50, nextCursor: 40 }), true)
    assert.equal(shouldContinue({ searching: true, hasBefore: false, succeeded: true, addedCount: 5 }), false)
    assert.equal(shouldContinue({ searching: false, hasBefore: true, succeeded: true, addedCount: 5 }), false)
    assert.equal(shouldContinue({ searching: true, hasBefore: true, succeeded: false, addedCount: 5 }), false)
    assert.equal(shouldContinue({ searching: true, stopped: true, hasBefore: true, succeeded: true, addedCount: 5 }), false)
    assert.equal(shouldContinue({ searching: true, hasBefore: true, succeeded: true, addedCount: 0, previousCursor: 50, nextCursor: 50 }), false)
})

test('toolbar search preserves debounce, clear, Enter, and IME behavior; panel isolates search pagination', () => {
    const toolbar = readFileSync(new URL('../LogPanelToolbar.vue', import.meta.url), 'utf8')
    const step = readFileSync(new URL('../StepLogPanel.vue', import.meta.url), 'utf8')
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    const job = readFileSync(new URL('../../job.vue', import.meta.url), 'utf8')
    assert.match(toolbar, /setTimeout\(this\.commitSearch, 300\)/)
    assert.match(toolbar, /@keydown\.enter\.prevent="onSearchEnter"/)
    assert.match(toolbar, /@compositionstart="onSearchCompositionStart"[\s\S]*@compositionend="onSearchCompositionEnd"/)
    assert.match(toolbar, /v-if="searchInput"[\s\S]*class="search-clear"[\s\S]*@click\.stop="clearSearch"/)
    assert.match(toolbar, /onSearchEnter \(\) \{[\s\S]*const searchChanged[\s\S]*if \(!searchChanged\) this\.\$emit\('move', 1\)/)
    assert.match(step, /'controls\.search' \(value, previousValue\)[\s\S]*this\.updateMatches\(true\)[\s\S]*this\.continueSearchBackfill/)
    assert.match(step, /continueSearchBackfill \(\) \{[\s\S]*!this\.controls\.search[\s\S]*!this\.hasBefore[\s\S]*this\.loadBefore\(\)/)
    assert.match(step, /this\.controls\.current = 0[\s\S]*this\.controls\.total = 0[\s\S]*this\.highlightedLogId = ''/)
    assert.match(step, /shouldContinueSearchBackfill\(\{[\s\S]*searching: searchActive[\s\S]*hasBefore: this\.hasBefore[\s\S]*succeeded: beforeSucceeded/)
    assert.match(step, /shouldApplyLogSearchScroll\(\{[\s\S]*currentGeneration: this\.generation[\s\S]*currentSearch: String\(this\.controls\.search \|\| ''\)[\s\S]*currentTarget: this\.highlightedLogId[\s\S]*matchIds: this\.matchIds/)
    assert.match(step, /moveMatch \(step\)[\s\S]*const generation = this\.generation[\s\S]*shouldApplyLogSearchScroll/)
    assert.match(step, /const search = String\(this\.controls\.search \|\| ''\)[\s\S]*if \(!latestSucceeded \|\| generation !== this\.generation \|\| search !== String\(this\.controls\.search \|\| ''\)\)/)
    assert.match(step, /this\.\$nextTick\(\(\) => \{\s*if \(generation !== this\.generation\) return\s*const searchHitNeedsLocate/)
    assert.match(step, /if \(this\.bottomFollow && !this\.controls\.search\)/)
    assert.match(plugin, /this\.realLogControls\.search = ''[\s\S]*this\.realLogControls\.current = 0[\s\S]*this\.realLogControls\.total = 0/)
    assert.match(job, /this\.realLogControls\.search = ''[\s\S]*this\.realLogControls\.current = 0[\s\S]*this\.realLogControls\.total = 0/)
    assert.equal((toolbar.match(/<svg\s+class="match-nav-icon"\s+viewBox="0 0 32 32"/g) || []).length, 2)
    assert.doesNotMatch(toolbar, /icon-angle-right match-nav-icon|rotate\(180deg\)|bk-log-icon/)
    assert.match(toolbar, /\.match-nav \.match-nav-icon \{ flex: 0 0 12px; width: 12px; height: 12px; \}/)
    assert.doesNotMatch(toolbar, /bk-log-icon|log-icon-angle-(left|right)/)
})

test('minimap issue badges are static statistics and cannot filter or scroll the track', () => {
    const minimap = readFileSync(new URL('../LogErrorMinimap.vue', import.meta.url), 'utf8')
    const lines = readFileSync(new URL('../LogLinesView.vue', import.meta.url), 'utf8')
    const step = readFileSync(new URL('../StepLogPanel.vue', import.meta.url), 'utf8')
    assert.match(minimap, /<div\s+class="lp-minimap-badges"\s+@click\.stop[\s\S]*<span\s+v-if="errorCount > 0"[\s\S]*role="img"[\s\S]*ERROR \$\{errorCount\}/)
    assert.match(minimap, /<span\s+v-if="warnCount > 0"[\s\S]*role="img"[\s\S]*WARN \$\{warnCount\}/)
    assert.doesNotMatch(minimap, /errorFilterActive|warnFilterActive|filterBadgeLabel|filter-level|aria-pressed/)
    assert.doesNotMatch(lines, /@filter-level|filter-level/)
    assert.doesNotMatch(step, /handleQuickLevelFilter|@filter-level/)
})

test('LogErrorMinimap waits for the drag threshold, then maps the full drag range without a second click locate', () => {
    const documentListeners = {}
    const windowListeners = {}
    const fakeDocument = {
        addEventListener: (name, callback) => {
            documentListeners[name] = callback
        },
        removeEventListener: (name) => {
            delete documentListeners[name]
        }
    }
    const fakeWindow = {
        addEventListener: (name, callback) => {
            windowListeners[name] = callback
        },
        removeEventListener: (name) => {
            delete windowListeners[name]
        }
    }
    const component = loadComponentOptions('../LogErrorMinimap.vue', adapter, { document: fakeDocument, window: fakeWindow })
    const track = { getBoundingClientRect: () => ({ top: 100, height: 400 }) }
    const logs = [{ lineNo: 10 }, { lineNo: 20 }, { lineNo: 30 }]
    const events = []
    const vm = {
        ...component.methods,
        $refs: { track },
        logs,
        heightIndex: { keys: [] },
        coordinates: { interactive: true, viewportTopPx: 30, viewportHeightPx: 40, scale: 1, contentOffsetPx: 0, hitScale: 1, dragRange: 80, contentHeightPx: 100, panRatio: 0.5 },
        viewport: { top: 20, height: 10 },
        scrollMetrics: { clientHeight: 100, scrollTop: 20 },
        minimapLayout: { sourceHeight: 4000 },
        $emit: (name, value) => events.push([name, value])
    }
    Object.keys(component.methods).forEach((name) => {
        vm[name] = component.methods[name].bind(vm)
    })
    const previewTarget = { closest: () => null }
    const down = { button: 0, clientY: 150, target: previewTarget }
    vm.onTrackDown(down)
    assert.deepEqual(events, [], 'pressing a row does not jump before click or drag is known')
    documentListeners.mousemove({ clientY: 330 })
    vm.onTrackUp()
    assert.equal(documentListeners.mousemove, undefined, 'mouse release ends the drag listener')
    assert.equal(windowListeners.blur, undefined, 'mouse release removes blur cancellation listener')
    vm.onPreviewClick({ detail: 1 }, logs[1])
    assert.equal(events.filter(([name]) => name === 'locate').length, 0, 'release click is suppressed after a drag')
    assert.equal(events.filter(([name]) => name === 'pan').length, 1)
    assert.equal(events.find(([name]) => name === 'pan')[1], 1)
    vm.clearDragResetTimer()

    events.length = 0
    const blankTarget = { closest: () => null }
    vm.onTrackDown({ button: 0, clientY: 200, target: blankTarget })
    assert.deepEqual(events, [], 'track press is treated as a click until the threshold is crossed')
    documentListeners.mousemove({ clientY: 495 })
    vm.onTrackUp()
    vm.onTrackClick({ clientY: 495 })
    assert.deepEqual(events, [['pan', 1]], 'drag clamps to the true end and release does not perform a second locate')
    vm.clearDragResetTimer()
})

test('LogErrorMinimap cleanup removes global drag listeners and keyboard preview click remains locatable', () => {
    const documentListeners = {}
    const windowListeners = {}
    const fakeDocument = {
        addEventListener: (name, callback) => {
            documentListeners[name] = callback
        },
        removeEventListener: (name) => {
            delete documentListeners[name]
        }
    }
    const fakeWindow = {
        addEventListener: (name, callback) => {
            windowListeners[name] = callback
        },
        removeEventListener: (name) => {
            delete windowListeners[name]
        }
    }
    const component = loadComponentOptions('../LogErrorMinimap.vue', adapter, { document: fakeDocument, window: fakeWindow })
    const located = []
    const vm = {
        ...component.methods,
        $refs: { track: { getBoundingClientRect: () => ({ top: 0, height: 100 }) } },
        logs: [{ lineNo: 1 }],
        previewRows: [],
        coordinates: { viewportHeightPx: 20 },
        viewport: { top: 0, height: 20 },
        scrollMetrics: { clientHeight: 20 },
        minimapLayout: { sourceHeight: 100 },
        $emit: (name, value) => {
            if (name === 'locate') located.push(value)
        }
    }
    Object.keys(component.methods).forEach((name) => {
        vm[name] = component.methods[name].bind(vm)
    })
    vm.onTrackDown({ button: 0, clientY: 10, target: { closest: () => null } })
    assert.ok(documentListeners.mousemove)
    assert.ok(windowListeners.blur)
    vm.onDragCancel()
    assert.equal(documentListeners.mousemove, undefined)
    assert.equal(documentListeners.mouseup, undefined)
    assert.equal(windowListeners.blur, undefined)
    vm.onPreviewClick({ detail: 0 }, vm.logs[0])
    assert.deepEqual(located, [vm.logs[0]])
})

test('LogErrorMinimap suppresses the synthetic pointer click after Escape or index changes without swallowing keyboard activation', () => {
    const logs = Array.from({ length: 100000 }, (_, index) => ({ lineNo: index + 1, text: `line-${index + 1}` }))
    const heightIndex = virtualWindow.createHeightIndex().rebuild(logs, {}, false)
    const layout = adapter.resolveLogMinimapLayout(logs, {
        trackHeight: 698,
        heightIndex,
        lineOrigin: 8,
        scrollMetrics: { scrollTop: 999647, clientHeight: 722, scrollHeight: 2000016 }
    })
    const previewRow = layout.previewRows.find(row => row.index === 49956)
    const coordinates = adapter.resolveLogMinimapCoordinates(layout, null, 698)
    for (const cancelBy of ['escape', 'indexVersion']) {
        const documentListeners = {}
        const windowListeners = {}
        const fakeDocument = {
            addEventListener: (name, callback) => {
                documentListeners[name] = callback
            },
            removeEventListener: name => {
                delete documentListeners[name]
            }
        }
        const fakeWindow = {
            addEventListener: (name, callback) => {
                windowListeners[name] = callback
            },
            removeEventListener: name => {
                delete windowListeners[name]
            }
        }
        const component = loadComponentOptions('../LogErrorMinimap.vue', adapter, { document: fakeDocument, window: fakeWindow })
        const events = []
        const vm = {
            ...component.methods,
            $refs: { track: { getBoundingClientRect: () => ({ top: 0, height: 698 }) } },
            logs,
            heightIndex: { keys: [] },
            previewRows: layout.previewRows,
            minimapLayout: layout,
            coordinates,
            $emit: (name, value) => events.push([name, value])
        }
        Object.keys(component.methods).forEach(name => {
            vm[name] = component.methods[name].bind(vm)
        })
        if (cancelBy === 'escape') {
            vm.onThumbDown({ clientY: 300 })
            vm.onDragKeydown({ key: 'Escape' })
        } else {
            vm.onTrackDown({ button: 0, clientY: 300, target: { closest: () => null } })
            vm.heightIndex.keys = []
            component.watch.indexVersion.call(vm)
        }
        assert.equal(documentListeners.mousemove, undefined)
        assert.equal(documentListeners.mouseup, undefined)
        assert.equal(windowListeners.blur, undefined)

        vm.onTrackClick({ detail: 1, clientY: 300 })
        assert.deepEqual(events, [], `${cancelBy} cancellation consumes the following pointer click`)
        vm.onPreviewClick({ detail: 0, currentTarget: { dataset: { index: String(previewRow.index) } } }, previewRow.line)
        assert.equal(events.length, 1, 'a later keyboard activation remains available')
        assert.equal(events[0][0], 'locate-target')
        assert.equal(events[0][1].index, previewRow.index)
    }
})

test('LogErrorMinimap preserves thumb grab offset and excludes badge presses from track gestures', () => {
    const documentListeners = {}
    const fakeDocument = {
        addEventListener: (name, callback) => {
            documentListeners[name] = callback
        },
        removeEventListener: (name) => {
            delete documentListeners[name]
        }
    }
    const fakeWindow = { addEventListener: () => {}, removeEventListener: () => {} }
    const component = loadComponentOptions('../LogErrorMinimap.vue', adapter, { document: fakeDocument, window: fakeWindow })
    const events = []
    const track = { getBoundingClientRect: () => ({ top: 100, height: 100 }) }
    const vm = {
        ...component.methods,
        $refs: { track },
        logs: [{ lineNo: 1 }, { lineNo: 2 }],
        heightIndex: { keys: [] },
        coordinates: { interactive: true, panRatio: 0.25, dragRange: 80, viewportTopPx: 20, viewportHeightPx: 20, trackHeightPx: 100, contentHeightPx: 100, scale: 1, contentOffsetPx: 0, hitScale: 1 },
        viewport: { top: 20, height: 20 },
        scrollMetrics: { clientHeight: 100, scrollTop: 200 },
        minimapLayout: { sourceHeight: 1000 },
        $emit: (name, value) => events.push([name, value])
    }
    Object.keys(component.methods).forEach((name) => {
        vm[name] = component.methods[name].bind(vm)
    })
    vm.onTrackDown({ button: 0, clientY: 110, target: { closest: selector => selector.includes('.lp-badge') ? {} : null } })
    assert.equal(documentListeners.mousemove, undefined, 'filter badge does not start a drag')
    vm.onThumbDown({ clientY: 125 })
    documentListeners.mousemove({ clientY: 145 })
    assert.equal(events.at(-1)[0], 'pan')
    assert.equal(events.at(-1)[1], 0.5, 'thumb keeps the original 5px grab offset')
    documentListeners.mouseup()
    vm.clearDragResetTimer()
})

test('LogErrorMinimap track and thumb continue panning after real row-height measurements rebase the gesture', () => {
    const documentListeners = {}
    const windowListeners = {}
    const addCounts = {}
    const fakeDocument = {
        addEventListener: (name, callback) => {
            addCounts[name] = (addCounts[name] || 0) + 1
            documentListeners[name] = callback
        },
        removeEventListener: name => {
            delete documentListeners[name]
        }
    }
    const fakeWindow = {
        addEventListener: (name, callback) => {
            windowListeners[name] = callback
        },
        removeEventListener: name => {
            delete windowListeners[name]
        }
    }
    const logs = [{ lineNo: 1, text: 'long line' }, { lineNo: 2, text: 'second line' }]
    const heightIndex = virtualWindow.createHeightIndex().rebuild(logs, {}, true)
    const viewOptions = loadComponentOptions('../LogLinesView.vue', {
        ...adapter,
        ...virtualWindow,
        markRaw,
        shallowRef
    }, { LogErrorMinimap: {} })
    const measuredRows = logs.map((log, index) => ({
        dataset: { lineKey: String(log.lineNo) },
        offsetHeight: index === 0 ? 220 : 40
    }))
    const view = {
        wrap: true,
        heightIndex,
        heightMap: { 1: 20, 2: 20 },
        indexVersion: 0,
        scrollGeneration: 0,
        scrollTop: 0,
        _pendingWheel: null,
        pendingAnchor: null,
        activeLocate: null,
        $refs: { box: { scrollTop: 0, querySelectorAll: () => measuredRows } },
        captureAnchor: () => null,
        $nextTick: () => {},
        updateViewport () {}
    }
    const component = loadComponentOptions('../LogErrorMinimap.vue', adapter, { document: fakeDocument, window: fakeWindow })
    for (const kind of ['track', 'thumb']) {
        measuredRows[0].offsetHeight = 220
        measuredRows[1].offsetHeight = 40
        heightIndex.rebuild(logs, {}, true)
        view.heightMap = { 1: 20, 2: 20 }
        view.indexVersion = 0
        const events = []
        const vm = {
            ...component.methods,
            $refs: { track: { getBoundingClientRect: () => ({ top: 100, height: 500 }) } },
            logs,
            heightIndex,
            coordinates: { interactive: true, panRatio: 0.2, dragRange: 200, contentHeightPx: 300, viewportHeightPx: 100, viewportTopPx: 40 },
            indexVersion: 0,
            $emit: (name, value) => events.push([name, value])
        }
        Object.keys(component.methods).forEach(name => {
            vm[name] = component.methods[name].bind(vm)
        })
        if (kind === 'track') {
            vm.onTrackDown({ button: 0, clientY: 140, target: { closest: () => null } })
            documentListeners.mousemove({ clientY: 140 })
        } else {
            vm.onThumbDown({ clientY: 140 })
        }
        const moveListener = documentListeners.mousemove
        assert.ok(moveListener)

        viewOptions.methods.measureVisibleRows.call(view)
        assert.equal(heightIndex.heights[0], 220, 'the actual LogLinesView measurement updates the long row from 20px to 220px')
        assert.equal(view.indexVersion, 1)
        vm.indexVersion = view.indexVersion
        component.watch.indexVersion.call(vm)
        assert.equal(vm._trackGesture.needsRebase, true)
        assert.equal(documentListeners.mousemove, moveListener, `${kind} drag remains bound through row measurement`)
        assert.equal(addCounts.mousemove, kind === 'track' ? 1 : 2, 'measurement does not register duplicate listeners')

        measuredRows[0].offsetHeight = 240
        viewOptions.methods.measureVisibleRows.call(view)
        vm.indexVersion = view.indexVersion
        component.watch.indexVersion.call(vm)
        assert.equal(view.indexVersion, 2, 'a second measurement advances the same projection version')
        assert.equal(vm._trackGesture.needsRebase, true, 'multiple measurements coalesce into a pending rebase')
        assert.equal(documentListeners.mousemove, moveListener, 'repeated measurements retain the existing listener')

        vm.coordinates = { ...vm.coordinates, panRatio: 0.35, dragRange: 220 }
        moveListener({ clientY: 162 })
        assert.equal(events.at(-1)[0], 'pan')
        assert.ok(Math.abs(events.at(-1)[1] - 0.45) < 1e-12, `${kind} drag rebases at 140px and maps 22px / 220px to 0.45`)
        assert.equal(vm._trackGesture.lastY, 162)
        vm.onTrackUp()
        assert.equal(documentListeners.mousemove, undefined)
        vm.clearDragResetTimer()
    }
})

test('empty filtered logs keep the minimap mounted without invalid geometry', () => {
    const lines = readFileSync(new URL('../LogLinesView.vue', import.meta.url), 'utf8')
    assert.match(lines, /'has-minimap': showMinimap/)
    assert.match(lines, /<log-error-minimap\s+v-if="showMinimap"[\s\S]*:logs="renderLines"/)
    assert.doesNotMatch(lines, /v-if="showMinimap && displayLines\.length"/)
    const layout = adapter.resolveLogMinimapLayout([])
    const coordinates = adapter.resolveLogMinimapCoordinates(layout, { top: 0, height: 100 }, 500)
    assert.deepEqual(layout.previewRows, [])
    assert.deepEqual(layout.issueMarks, [])
    assert.equal(coordinates.contentHeightPx, 0)
    assert.equal(coordinates.viewportHeightPx, 16)
})

test('new log interface errors are classified for explicit reload without legacy fallback', () => {
    assert.equal(adapter.resolveLogLoadErrorKey({ response: { status: 404 } }), 'logPanel.loadUnavailable')
    assert.equal(adapter.resolveLogLoadErrorKey({ status: 501 }), 'logPanel.loadUnavailable')
    for (const status of [400, 429, 500, 0, 999]) assert.equal(adapter.resolveLogLoadErrorKey({ status }), 'logPanel.loadError')
})

test('strict latest response decoding accepts legacy success wrappers and rejects malformed pseudo-empty payloads', () => {
    const panel = { logs: [], hasBefore: false }
    const valid = [
        { status: 0, logs: [] },
        { data: { status: 0, logs: [] } },
        { status: 0, data: panel },
        { data: { status: 0, data: panel } },
        { status: 200, data: { status: 0, data: panel } },
        { status: 200, data: { logs: [] } },
        { logs: [] },
        { status: 1 }, { status: 2 }, { status: 3 }, { cleaned: true }, { emptyStatus: true },
        { status: 0, data: { cleaned: true } }, { status: 200, data: { cleaned: true } }, { status: 999 }
    ]
    for (const response of valid) assert.doesNotThrow(() => adapter.normalizeLogPanelResponse(response, { strictLatest: true }))
    assert.deepEqual(adapter.normalizeLogPanelResponse({ status: 0, data: panel }, { strictLatest: true }).logs, [])
    assert.equal(adapter.normalizeLogPanelResponse({ status: 200, data: { status: 0, data: panel } }, { strictLatest: true }).status, 0)
    assert.equal(adapter.normalizeLogPanelResponse({ status: 0, data: { cleaned: true } }, { strictLatest: true }).clean, true)
    assert.equal(adapter.normalizeLogPanelResponse({ status: 1, finished: false, hasAfter: true }, { strictLatest: true }).finished, false)
    for (const response of [
        null, undefined, '<html>failure</html>', 'failure', [], {}, { data: null }, { status: 0, data: null },
        { status: 0, logs: {} }, { logs: {} }, { status: '', logs: [] }, { status: ' ', logs: [] },
        { status: true, logs: [] }, { status: 4, logs: [] }, { status: 0, logs: null }
    ]) assert.throws(() => adapter.normalizeLogPanelResponse(response, { strictLatest: true }), /Invalid latest log response/)
})

test('latest response decoding remains permissive for non-latest pagination responses', () => {
    assert.deepEqual(adapter.normalizeLogPanelResponse({ status: 0, logs: 'unexpected' }).logs, [])
    assert.deepEqual(adapter.normalizeLogPanelResponse(null).logs, [])
})

test('latest request clears stale pagination and only successful current-search results may backfill', () => {
    const source = readFileSync(new URL('../StepLogPanel.vue', import.meta.url), 'utf8')
    const reload = source.match(/reload \(\) \{([\s\S]*?)\n            \},\n            applyResult/)[1]
    for (const reset of ['this.hasBefore = false', 'this.hasAfter = false', 'this.startLineNo = 0', 'this.endLineNo = 0', 'this.beforeCursor = null', 'this.maxLineNo = 0', 'this.maxTimestamp = null', 'this.finished = true']) {
        assert.ok(reload.includes(reset), `reload resets ${reset}`)
    }
    assert.match(reload, /normalizeLogPanelResponse\(response, \{ strictLatest: true \}\)/)
    assert.match(reload, /let latestSucceeded = false/)
    assert.match(reload, /if \(!latestSucceeded \|\| generation !== this\.generation \|\| search !== String\(this\.controls\.search \|\| ''\)\)/)
    assert.doesNotMatch(reload, /searchAtStart/)
    assert.match(reload, /latestSucceeded = result\.status === 0 && !result\.clean/)
})

test('search entered before or during latest loading backfills after success, while failed latest never follows stale cursors', async () => {
    const names = [
        'mapActions', 'markRaw', 'shallowRef', 'LogGroupBar', 'LogLinesView', 'findLogMatchIds', 'formatLogTime', 'getLogIssueLevel',
        'isPermissionError', 'latestDisplayBase', 'normalizeLogPanelResponse', 'serializeLogLevels', 'shouldBackfillSmallLog',
        'shouldApplyLogSearchScroll', 'shouldContinueSearchBackfill', 'resolveCleanResponseMessage', 'resolveCleanStatusFallbackKey', 'resolveCleanStatusText',
        'resolveDirectJobDownloadUrl', 'resolveLocateLogTarget', 'resolveLogHighlightState', 'resolveNextLogMatchIndex',
        'shouldContinueAutoBackfillAfterBefore', 'resolveLogLoadErrorKey', 'shouldReloadOnLevelsChange', 'shouldRetryBeforeLoad', 'shouldScheduleBeforeRetry',
        'shouldShowStatusEmptyLine', 'shouldUseDirectJobDownload', 'withDisplayLineNumbers', 'countNewLogPanelLines', 'mergeLogPanelLines', 'nextAfterCursor',
        'createLogPanelFormatStressLogs', 'createLogPanelSearchStressLogs', 'filterLogPanelDevMockLogs', 'buildLogStructure', 'isLogStructureFilterEligible',
        'projectLogStructure', 'resolveLogGroupAncestors'
    ]
    const options = loadComponentOptions('../StepLogPanel.vue', Object.fromEntries(names.map(name => [name, {
        mapActions: () => ({}),
        normalizeLogPanelResponse: adapter.normalizeLogPanelResponse,
        serializeLogLevels: adapter.serializeLogLevels,
        isPermissionError: adapter.isPermissionError,
        resolveLogLoadErrorKey: adapter.resolveLogLoadErrorKey
    }[name] || (() => {})])))

    async function scenario (initialSearch, latestResponse) {
        let resolveLatest
        const callbacks = []
        let beforeCalls = 0
        const vm = {
            tag: 'step', jobId: '', executeCount: 1, archiveFlag: '', controls: { levels: ['INFO'], search: initialSearch, current: 0, total: 0 }, selectedSubTag: '',
            retryContext: '', beforeRetries: 0, afterRetries: 0, generation: 0, searchBackfillStopped: false, logs: [], highlightedLogId: '',
            hasBefore: true, hasAfter: true, finished: false, startLineNo: 101, endLineNo: 500, maxLineNo: 500, maxTimestamp: 500, beforeCursor: 101,
            tailIdentity: {}, displayBase: 0, matchedTotal: null, bottomFollow: true, loadingInitial: false, loadingBefore: false,
            permissionDenied: false, cleanStatus: '', serverMessage: '', errorMessage: '', devMockState: '', projectId: 'p', pipelineId: 'pipe', buildId: 'build',
            $emit () {},
            $nextTick: callback => callbacks.push(callback),
            requestParams: () => ({}),
            invalidate () {
                this.generation += 1
            },
            getLogPanelLatest: () => new Promise(resolve => {
                resolveLatest = resolve
            }),
            applyResponse (result) {
                if (result.status === 0 && !result.clean) this.hasBefore = result.hasBefore
            },
            loadBefore () {
                beforeCalls += 1
            },
            continueSearchBackfill () {
                return options.methods.continueSearchBackfill.call(this)
            },
            scrollToBottom () {},
            maybeAutoBackfill () {}
        }
        options.methods.reload.call(vm)
        if (initialSearch) options.methods.continueSearchBackfill.call(vm)
        vm.controls.search = 'needle'
        if (!initialSearch) options.methods.continueSearchBackfill.call(vm)
        resolveLatest(latestResponse)
        await new Promise(resolve => setTimeout(resolve, 0))
        callbacks.forEach(callback => callback())
        return { vm, beforeCalls }
    }

    const beforeRequest = await scenario('needle', { status: 0, logs: [], hasBefore: true })
    assert.equal(beforeRequest.beforeCalls, 1, 'a search already active before latest is backfilled after its success')
    const duringRequest = await scenario('', { status: 0, logs: [], hasBefore: true })
    assert.equal(duringRequest.beforeCalls, 1, 'a search entered while latest is pending is backfilled after success')
    const failed = await scenario('', { status: 999, message: 'latest unavailable' })
    assert.equal(failed.beforeCalls, 0)
    assert.equal(failed.vm.hasBefore, false, 'failed latest cannot reuse the stale before cursor')
    assert.equal(failed.vm.startLineNo, 0)
})

test('string ANSI extended-color parameters are consumed without changing foreground or swallowing later SGR', () => {
    const esc = '\u001b['
    const cases = [
        [`${esc}31m<needle>${esc}38;5;30m tail`, '#ff6666'],
        [`${esc}31m<needle>${esc}48;2;31;0;39m tail`, '#ff6666'],
        [`${esc}38;2;1;2;30;31m<needle>`, '#ff6666'],
        [`${esc}31mred ${esc}38;5m<needle>`, '#ff6666'],
        [`${esc}31mred ${esc}48;2m<needle>`, '#ff6666']
    ]
    for (const [raw, color] of cases) {
        const stringHtml = adapter.renderLogHtml(raw, 'needle')
        const objectHtml = adapter.renderLogHtml({ message: raw }, 'needle')
        assert.match(stringHtml, new RegExp(`color:${color}`))
        assert.match(stringHtml, /&lt;<mark class="lp-hl">needle<\/mark>&gt;/)
        assert.match(objectHtml, new RegExp(`color:${color}`), 'object route keeps the ordinary foreground palette')
        assert.match(objectHtml, /&lt;<mark class="lp-hl">needle<\/mark>&gt;/, 'object route keeps search and HTML escaping')
    }
    for (const raw of ['[38;5;30m<needle>', '[48;2;31;0;39m<needle>']) {
        const html = adapter.renderLogHtml(raw, 'needle')
        assert.doesNotMatch(html, /style="color:/)
        assert.match(html, /&lt;<mark class="lp-hl">needle<\/mark>&gt;/)
    }
})

test('Step log failures stay in the new panel and offer an explicit disabled-while-loading reload', () => {
    const source = readFileSync(new URL('../StepLogPanel.vue', import.meta.url), 'utf8')
    const job = readFileSync(new URL('../../job.vue', import.meta.url), 'utf8')
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    assert.match(source, /log-panel-load-error[\s\S]*\{\{ errorMessageText \}\}[\s\S]*:disabled="loadingInitial"[\s\S]*@click="reload"/)
    assert.match(source, /this\.errorMessage = resolveLogLoadErrorKey\(error\)/)
    assert.match(source, /this\.permissionDenied = true[\s\S]*this\.\$emit\('permission', error\)/)
    assert.doesNotMatch(source, /\$emit\('fallback'/)
    assert.doesNotMatch(job, /@fallback|realLogFallback|<job-log(?:\s|>)/)
    assert.doesNotMatch(plugin, /@fallback|realLogFallback|<plugin-log(?:\s|>)/)
    assert.match(job, /<plugin-log[\s\S]*type="containerLog"[\s\S]*v-if="currentJob\.matrixGroupFlag"/)
})

test('toolbar actions use immediate non-HTML tooltips while preserving accessible labels and pressed state', () => {
    const source = readFileSync(new URL('../LogPanelToolbar.vue', import.meta.url), 'utf8')
    const actions = [...source.matchAll(/<button(?=[^>]*class="toolbar-action)[^>]*>/g)]
    assert.equal(actions.length, 3)
    for (const [button] of actions) {
        assert.match(button, /v-bk-tooltips="\{ content:[^}]*delay: \[0, 0\],[^}]*allowHTML: false,[^}]*disabled \}/)
        assert.match(button, /:aria-label=/)
        assert.doesNotMatch(button, /:title=/)
    }
    assert.match(source, /:aria-pressed="String\(controls\.showTime\)"/)
    assert.match(source, /\.toolbar-action \{ display: inline-flex; width: 24px; height: 24px;/)
    assert.match(source, /\.toolbar-action \+ \.toolbar-action \{ margin-left: 8px; \}/)
    assert.match(source, /<svg\s+class="toolbar-action-icon time-icon"\s+viewBox="0 0 16 16"/)
})

test('toolbar match navigation and time toggle use the prototype SVG icons', () => {
    const source = readFileSync(new URL('../LogPanelToolbar.vue', import.meta.url), 'utf8')
    const template = source.match(/<template>[\s\S]*<\/template>/)?.[0] || ''
    assert.doesNotMatch(template, /devops-icon icon-angle-right|bk-icon icon-time/)
    const [previous, next] = [...template.matchAll(/<svg\s+class="match-nav-icon"\s+viewBox="0 0 32 32"[\s\S]*?<\/svg>/g)].map(([svg]) => svg)
    assert.match(previous, /d="M22 30c-0\.26 0-0\.54-0\.1-0\.74-0\.32l-12-13/)
    assert.match(next, /d="M10 30c-0\.24 0-0\.48-0\.080-0\.68-0\.26/)
    const timeIcon = template.match(/<svg\s+class="toolbar-action-icon time-icon"[\s\S]*?<\/svg>/)?.[0] || ''
    assert.match(timeIcon, /viewBox="0 0 16 16"/)
    assert.match(timeIcon, /M7 9V4H8V8H12V9H7Z"/)
    assert.match(timeIcon, /fill="currentColor"/)
    assert.match(source, /\.match-nav \.match-nav-icon \{ flex: 0 0 12px; width: 12px; height: 12px; \}/)
})

test('toolbar hover hints are immediate everywhere and toggles share a class-based selected state with compact icons', () => {
    const source = readFileSync(new URL('../LogPanelToolbar.vue', import.meta.url), 'utf8')
    const template = source.match(/<template>[\s\S]*<\/template>/)?.[0] || ''
    assert.doesNotMatch(template, /:?title=/)
    assert.match(template, /class="level-trigger"[\s\S]*?v-bk-tooltips="\{ content: \$t\('logPanel\.level'\), delay: \[0, 0\], allowHTML: false, disabled: disabled \|\| levelPopupOpen \}"/)
    assert.match(template, /class="search-clear"[\s\S]*?v-bk-tooltips="\{ content: \$t\('clearSelection'\), delay: \[0, 0\], allowHTML: false, disabled \}"/)
    for (const key of ['previousHit', 'nextHit']) {
        assert.match(template, new RegExp(`v-bk-tooltips="\\{ content: \\$t\\('logPanel\\.${key}'\\), delay: \\[0, 0\\], allowHTML: false, disabled: disabled \\|\\| !controls\\.total \\}"`))
    }
    const timeButton = template.match(/<button(?=[^>]*:aria-pressed="String\(controls\.showTime\)")[^>]*>/)?.[0] || ''
    assert.match(timeButton, /:class="\{ 'is-active': controls\.showTime \}"/)
    assert.match(template, /class="toolbar-action is-wrap-toggle"\s+:class="\{ 'is-active': controls\.wrap \}"/)
    assert.doesNotMatch(source, /is-wrap-active|\[aria-pressed="true"\]/)
    assert.match(source, /\.toolbar-action:not\(:disabled\):hover \{ color: #fff; \}/)
    assert.doesNotMatch(source, /rgba\(255, 255, 255, \.08\)/)
    assert.match(source, /\.toolbar-action\.is-active,\s*\.toolbar-action\.is-active:not\(:disabled\):hover \{ color: #3a84ff; background: rgba\(58, 132, 255, \.16\); \}/)
    assert.match(source, /\.toolbar-action-icon \{ flex: 0 0 16px; width: 16px; height: 16px; \}/)
    assert.match(source, /\.toolbar-action \.wrap-icon \{ flex: 0 0 16px; width: 16px; height: 16px; \}/)
})

test('toolbar matches prototype spacing, pager buttons and an inset keyboard-only focus ring', () => {
    const source = readFileSync(new URL('../LogPanelToolbar.vue', import.meta.url), 'utf8')
    assert.match(source, /\.log-panel-toolbar > \* \+ \* \{ margin-left: 8px; \}/)
    assert.match(source, /button:focus:not\(:focus-visible\) \{ outline: none; box-shadow: none; \}/)
    assert.match(source, /button:focus-visible \{\s*outline: 1px solid #979ba5;\s*outline-offset: -1px;\s*\}/)
    assert.doesNotMatch(source, /outline: 2px solid #3c96ff;\s*outline-offset: 2px/)
    assert.match(source, /button:disabled \{ opacity: \.4; cursor: not-allowed; \}/)
    assert.match(source, /\.match-nav > button \{ display: inline-flex; align-items: center; justify-content: center; height: 24px; padding: 0; color: #c4c6cc; border-radius: 4px; \}/)
    assert.match(source, /\.match-nav > button:not\(:disabled\):hover \{ color: #fff; \}/)
    assert.match(source, /\.match-count \{[^}]*min-width: 36px;[^}]*color: #fff;/)
})

test('log panel header and job directory opt into prototype outline status icons', () => {
    const status = readFileSync(new URL('../../status.vue', import.meta.url), 'utf8')
    const container = readFileSync(new URL('../../detailContainer.vue', import.meta.url), 'utf8')
    const jobDirectory = readFileSync(new URL('../JobLogPanel.vue', import.meta.url), 'utf8')
    assert.match(status, /import \{ statusColorMap, statusIconMap \} from '@\/utils\/pipelineStatus'/)
    assert.match(status, /outline: Boolean/)
    assert.match(status, /v-else-if="outlineIconName"[\s\S]*:name="outlineIconName"[\s\S]*:size="16"[\s\S]*:style="outlineColorStyle"/)
    assert.match(status, /name="circle-2-1"[\s\S]*class="executing running"[\s\S]*:style="outlineColorStyle"/)
    assert.match(container, /<status-icon\s+:status="status"\s+:is-hook="isHook"\s+outline\s*><\/status-icon>/)
    assert.match(jobDirectory, /<log-status\s+:status="item\.status"\s+:is-hook="item\.additionalOptions\?\.elementPostInfo"\s+outline\s*\/>/)

    const mapSource = readFileSync(new URL('../../../../utils/pipelineStatus.js', import.meta.url), 'utf8')
    const toCjs = mapSource.replace(/export const (\w+) =/g, 'exports.$1 =').replace(/export function (\w+)/g, 'exports.$1 = function $1')
    const maps = {}
    new Function('exports', toCjs)(maps)
    const script = status.match(/<script>([\s\S]*?)<\/script>/)[1]
        .replace(/import Logo from '@\/components\/Logo'/, 'const Logo = {}')
        .replace(/import \{ statusColorMap, statusIconMap \} from '@\/utils\/pipelineStatus'/, 'const { statusColorMap, statusIconMap } = maps')
        .replace('export default', 'return')
    const options = new Function('maps', script)(maps)
    const compute = (props) => {
        const vm = { ...props }
        for (const [key, fn] of Object.entries(options.computed)) Object.defineProperty(vm, key, { get: fn.bind(vm) })
        return vm
    }
    assert.equal(compute({ status: 'SUCCEED', outline: true }).outlineIconName, 'check-circle')
    assert.deepEqual(compute({ status: 'SUCCEED', outline: true }).outlineColorStyle, { color: '#2DCB56' })
    assert.equal(compute({ status: 'FAILED', outline: true }).outlineIconName, 'close-circle')
    assert.equal(compute({ status: 'FAILED', outline: true, isHook: true }).outlineIconName, 'build-hooks')
    assert.deepEqual(compute({ status: '', outline: true }).outlineColorStyle, { color: '#63656e' })
    assert.equal(compute({ status: '', outline: true }).outlineIconName, 'circle')
    assert.equal(compute({ status: 'WAITING', outline: true }).outlineIconName, '')
    assert.equal(compute({ status: 'SUCCEED', outline: false }).outlineIconName, '')
    assert.equal(compute({ status: 'SUCCEED', outline: false }).outlineColorStyle, null)
})

test('backfills only completed logs whose reliable total is at most 3000', () => {
    assert.equal(adapter.shouldBackfillSmallLog({
        finished: true,
        hasAfter: false,
        hasBefore: true,
        matchedTotal: 3000,
        loadedCount: 1000
    }), true)
    assert.equal(adapter.shouldBackfillSmallLog({
        finished: true,
        hasAfter: false,
        hasBefore: true,
        matchedTotal: 3001,
        loadedCount: 1000
    }), false)
})

test('rejects invalid matchedTotal and fully loaded sessions', () => {
    assert.equal(adapter.shouldBackfillSmallLog({
        finished: true,
        hasAfter: false,
        hasBefore: true,
        matchedTotal: null,
        loadedCount: 0
    }), false)
    assert.equal(adapter.shouldBackfillSmallLog({
        finished: true,
        hasAfter: false,
        hasBefore: true,
        matchedTotal: -1,
        loadedCount: 0
    }), false)
    assert.equal(adapter.shouldBackfillSmallLog({
        finished: true,
        hasAfter: false,
        hasBefore: true,
        matchedTotal: 100,
        loadedCount: 100
    }), false)
})

test('formats timestamps with date and milliseconds', () => {
    const value = new Date(2026, 8, 18, 17, 1, 2, 3).getTime()
    assert.equal(adapter.formatLogTime(value), '2026-09-18 17:01:02:003')
})

test('formats log object timestamps with date and milliseconds', () => {
    const value = new Date(2026, 8, 18, 17, 1, 2, 3).getTime()
    assert.equal(adapter.formatLogTime({ timestamp: value }), '2026-09-18 17:01:02:003')
    assert.equal(adapter.formatLogTime({ time: value }), '2026-09-18 17:01:02:003')
})

test('escapes HTML, renders ANSI, then highlights keyword', () => {
    const html = adapter.renderLogHtml('\u001b[31m<error>\u001b[0m', 'error')
    assert.match(html, /color:#ff6666/)
    assert.match(html, /&lt;<mark class="lp-hl">error<\/mark>&gt;/)
    assert.doesNotMatch(html, /<error>/)
})

test('highlights a literal keyword across ANSI color segments while escaping HTML', () => {
    const html = adapter.renderLogHtml('\u001b[31m<Err\u001b[0m\u001b[33mor>\u001b[0m', 'error')
    assert.match(html, /<span style="color:#ff6666">&lt;<mark class="lp-hl">Err<\/mark><\/span>/)
    assert.match(html, /<span style="color:#ff9c01"><mark class="lp-hl">or<\/mark>&gt;<\/span>/)
    assert.doesNotMatch(html, /<Err|<or/)
})

test('renders safe log links with the existing dark-content link color', () => {
    const lines = readFileSync(new URL('../LogLinesView.vue', import.meta.url), 'utf8')
    const errorSummary = readFileSync(new URL('../../ErrorSummary.vue', import.meta.url), 'utf8')
    assert.match(lines, /::v-deep \.lp-text a,[\s\S]*::v-deep \.lp-text a:visited\s*\{\s*color:\s*#3c96ff/)
    assert.match(errorSummary, /a\s*\{\s*color:\s*#3c96ff;\s*&:visited\s*\{\s*color:\s*#3c96ff/)
})

test('renders bare ANSI sequences without ESC prefix', () => {
    const html = adapter.renderLogHtml('[31m<error>[0m', 'error')
    assert.match(html, /color:#ff6666/)
    assert.match(html, /&lt;<mark class="lp-hl">error<\/mark>&gt;/)
})

test('preserves bracketed log text that is not bare SGR', () => {
    assert.equal(adapter.renderLogHtml('[INFO] ready'), '[INFO] ready')
    assert.equal(adapter.renderLogHtml('[2026-09-18] started'), '[2026-09-18] started')
})

test('keyword highlight escapes matches without breaking entities', () => {
    assert.equal(adapter.renderLogHtml('<', 'lt'), '&lt;')
    assert.equal(adapter.renderLogHtml('alpha & beta', '&'), 'alpha <mark class="lp-hl">&amp;</mark> beta')
    assert.doesNotMatch(adapter.renderLogHtml('<tag>', 'lt'), /&<mark/)
})

test('formatElapsed requires explicit valueKind for elapsed durations', () => {
    assert.equal(adapter.formatElapsed(65000, null, 'SUCCEED', 'elapsed'), '1m 5s')
    assert.equal(adapter.formatElapsed(65000, null, 'SUCCEED'), '')
    assert.equal(adapter.formatElapsed(65000, null, 'SUCCEED', 'auto'), '')
    assert.equal(adapter.formatElapsed(65000, 70000, 'SUCCEED', 'elapsed'), '')
    assert.equal(adapter.formatElapsed(null, null, 'RUNNING', 'elapsed'), '')
    assert.equal(adapter.formatElapsed(Infinity, null, 'RUNNING', 'elapsed'), '')
})

test('formatElapsed uses explicit valueKind without duration ceiling', () => {
    assert.equal(adapter.formatElapsed(86_400_000, null, 'RUNNING', 'elapsed'), '1d 0h 0m 0s')
    assert.equal(adapter.formatElapsed(172_800_000, null, 'RUNNING', 'elapsed'), '2d 0h 0m 0s')
    assert.equal(adapter.formatElapsed(26 * 60 * 60 * 1000 + 3_000, null, 'REVIEWING', 'elapsed'), '1d 2h 0m 3s')
})

test('formatElapsed uses timestamp kind with endTime or live status only', () => {
    assert.equal(adapter.formatElapsed(1_700_000_000_000, 1_700_000_360_000, 'SUCCEED', 'timestamp'), '6m 0s')
    assert.equal(adapter.formatElapsed(1_700_000_000_000, null, 'SUCCEED', 'timestamp'), '')
    assert.equal(adapter.formatElapsed(1_700_000_000_000, 1_699_999_000_000, 'SUCCEED', 'timestamp'), '')

    const start = 1_700_000_000_000
    const originalNow = Date.now
    Date.now = () => start + 65000
    try {
        assert.equal(adapter.formatElapsed(start, null, 'RUNNING', 'timestamp'), '1m 5s')
        assert.equal(adapter.formatElapsed(start, null, 'PREPARE_ENV', 'timestamp'), '1m 5s')
        assert.equal(adapter.formatElapsed(start, null, 'SUCCEED', 'timestamp'), '')
    } finally {
        Date.now = originalNow
    }
})

test('maps display numbers independently from storage line numbers', () => {
    assert.deepEqual(
        adapter.withDisplayLineNumbers(
            [{ lineNo: 12001 }, { lineNo: 13000 }],
            2341
        ).map(item => item.displayLineNo),
        [2341, 2342]
    )
    const alreadyNumbered = [{ lineNo: 8, displayLineNo: 77 }]
    assert.equal(adapter.withDisplayLineNumbers(alreadyNumbered, 1)[0], alreadyNumbered[0], 'existing full-source display numbers preserve row identity')
})

test('StepLogPanel search navigation expands hidden ancestors before guarded scroll and keeps valid collapse ids across append', () => {
    const options = loadComponentOptions('../StepLogPanel.vue', {
        ...adapter,
        ...devMock,
        ...logTail,
        ...logStructure,
        shallowRef,
        markRaw
    }, {
        mapActions: () => ({}),
        LogGroupBar: {},
        LogLinesView: {}
    })
    const allLevels = ['INFO', 'WARN', 'ERROR', 'DEBUG']
    const controls = { levels: allLevels, search: 'needle', current: 0, total: 0, wrap: true, showTime: false }
    const Step = Vue.extend(options)
    const vm = new Step({
        propsData: {
            projectId: 'p', pipelineId: 'pipe', buildId: 'b', executeCount: 1,
            logContext: 'job|step', controls, devMockLoading: true
        }
    })
    const logs = [
        { lineNo: 1, timestamp: 1, level: 'INFO', text: '##[group]build' },
        { lineNo: 2, timestamp: 2, level: 'INFO', text: 'needle in collapsed group' },
        { lineNo: 3, timestamp: 3, level: 'INFO', text: '##[endgroup]' },
        { lineNo: 4, timestamp: 4, level: 'INFO', text: 'after group' }
    ]
    vm.logs = logs
    vm.finished = true
    vm.matchedTotal = logs.length
    vm.hasBefore = false
    vm.hasAfter = false
    assert.deepEqual(vm.numberedLogs.map(row => row.displayLineNo), [1, 2, 3, 4])
    assert.deepEqual(vm.visibleLogs.map(row => row.displayLineNo), [1, 2, 3, 4], 'expanded rows keep continuous full-source display numbers')
    const groupId = vm.logStructure.groups[0].id
    vm.collapsedGroupIds = [groupId]
    const queued = []
    const scrolled = []
    vm.$nextTick = callback => queued.push(callback)
    vm.$refs.linesView = { scrollToLine: lineNo => scrolled.push(lineNo) }

    vm.collapsedGroupIds = [groupId]
    vm.updateMatches(true)
    assert.deepEqual(vm.collapsedGroupIds, [], 'the initial search reveals the matching ancestor synchronously')
    assert.equal(queued.length, 1)
    queued.shift()()
    assert.deepEqual(scrolled, [2])

    vm.collapsedGroupIds = [groupId]
    vm.moveMatch(1)
    assert.deepEqual(vm.collapsedGroupIds, [], 'search navigation reveals the collapsed matching ancestor synchronously')
    assert.equal(queued.length, 1)
    queued.shift()()
    assert.deepEqual(scrolled, [2, 2])

    vm.collapsedGroupIds = [groupId]
    vm.moveMatch(1)
    vm.generation += 1
    queued.shift()()
    assert.deepEqual(scrolled, [2, 2], 'stale navigation callbacks remain blocked by generation')

    vm.collapsedGroupIds = [groupId]
    controls.search = 'needle'
    controls.current = 1
    vm.highlightedLogId = 2
    vm.applyResult({
        logs: [{ lineNo: 5, timestamp: 5, level: 'INFO', text: 'appended' }],
        matchedTotal: 5, hasBefore: false, hasAfter: false, finished: true,
        subTags: [], startLineNo: 1, endLineNo: 5
    }, 'after')
    assert.deepEqual(vm.collapsedGroupIds, [groupId], 'after append preserves the collapsed matching group during a search')
    assert.equal(queued.length, 0, 'background append does not schedule a search locate')
    vm.applyResult({
        logs: [{ lineNo: 0, timestamp: 0, level: 'INFO', text: 'prepended' }],
        matchedTotal: 6, hasBefore: false, hasAfter: false, finished: true,
        subTags: [], startLineNo: 0, endLineNo: 5
    }, 'before')
    assert.deepEqual(vm.collapsedGroupIds, [groupId], 'before prepend preserves the collapsed matching group during a search')
    assert.equal(queued.length, 0, 'background prepend does not schedule a search locate')
    vm.applyResult({
        logs: [],
        matchedTotal: 6, hasBefore: false, hasAfter: false, finished: true,
        subTags: [], startLineNo: 0, endLineNo: 5
    }, 'after')
    assert.deepEqual(vm.collapsedGroupIds, [groupId], 'empty after response preserves the user collapse state')
    assert.equal(queued.length, 0, 'empty background response does not schedule a search locate')

    vm.moveMatch(1)
    assert.deepEqual(vm.collapsedGroupIds, [], 'explicit next-match navigation still reveals the collapsed group')
    assert.equal(queued.length, 1)
    queued.shift()()
    assert.deepEqual(scrolled, [2, 2, 2])

    vm.invalidate()
    assert.deepEqual(vm.collapsedGroupIds, [], 'data-generation reset clears collapse state')
    assert.equal(isReactive(vm.logs), false, '100k source log array stays shallow/non-reactive')
    vm.$destroy()
})

test('StepLogPanel keeps polling while viewing history and only accelerates on a follow transition', () => {
    const options = loadComponentOptions('../StepLogPanel.vue', {
        ...adapter,
        ...devMock,
        ...logTail,
        ...logStructure,
        shallowRef,
        markRaw
    }, { mapActions: () => ({}), LogGroupBar: {}, LogLinesView: {} })
    const originalSetTimeout = global.setTimeout
    const originalClearTimeout = global.clearTimeout
    const scheduled = []
    const cleared = []
    global.setTimeout = (callback, delay) => {
        const timer = { callback, delay }
        scheduled.push(timer)
        return timer
    }
    global.clearTimeout = timer => cleared.push(timer)
    try {
        const vm = {
            devMockState: '', bottomFollow: false, loadingAfter: false, finished: false, hasAfter: true,
            logs: [{ lineNo: 12, timestamp: 100 }], generation: 4, afterRetries: 0,
            maxLineNo: 12, maxTimestamp: 100, endLineNo: 12, requestTimer: null,
            controls: { search: '' }, $refs: { linesView: { captureAnchor: () => ({ lineNo: '5' }) } },
            requestParams: () => ({ tag: 'step-a' }),
            getLogPanelAfter: () => new Promise(() => {}),
            $emit: () => {},
            $set (target, key, value) {
                target[key] = value
            }
        }
        Object.keys(options.methods).forEach(name => {
            vm[name] = options.methods[name].bind(vm)
        })
        vm.scheduleAfter()
        assert.equal(scheduled.length, 1, 'history reading does not stop the periodic After timer')
        scheduled[0].callback()
        assert.equal(vm.loadingAfter, true, 'the delayed callback issues a request while follow is paused')

        let immediateRequests = 0
        vm.loadAfter = () => {
            immediateRequests += 1
        }
        vm.loadingAfter = false
        vm.bottomFollow = true
        vm.onStickBottomChange(true)
        assert.equal(immediateRequests, 0, 'a repeated true notification does not issue another request')
        vm.onStickBottomChange(false)
        assert.equal(immediateRequests, 0, 'moving away from the tail does not stop polling or issue an immediate request')
        vm.onStickBottomChange(true)
        assert.equal(immediateRequests, 1, 'returning from history to the tail gets one immediate request')
        vm.onStickBottomChange(true)
        assert.equal(immediateRequests, 1, 'duplicate tail notifications remain inert')
    } finally {
        global.setTimeout = originalSetTimeout
        global.clearTimeout = originalClearTimeout
    }
})

test('StepLogPanel history Before response keeps the user at the tail and restores only a response-time anchor', async () => {
    const options = loadComponentOptions('../StepLogPanel.vue', {
        ...adapter,
        ...devMock,
        ...logTail,
        ...logStructure,
        shallowRef,
        markRaw
    }, { mapActions: () => ({}), LogGroupBar: {}, LogLinesView: {} })
    const createVm = () => {
        let resolveBefore
        const ticks = []
        const calls = { restore: [], preserve: 0, bottom: 0 }
        const view = {
            layoutInteractionToken: 0,
            anchorLineNo: '50',
            captureAnchor () {
                return { lineNo: this.anchorLineNo }
            },
            getScrollHeight: () => 1000,
            restoreAnchor: anchor => calls.restore.push(anchor),
            preserveScrollOnPrepend: () => {
                calls.preserve += 1
            },
            scrollToBottom: () => {
                calls.bottom += 1
            }
        }
        const vm = {
            loadingBefore: false, hasBefore: true, logs: [{ lineNo: 50, timestamp: 50 }], startLineNo: 50, beforeCursor: null,
            generation: 2, bottomFollow: false, highlightedLogId: null, searchBackfillStopped: false, beforeTimer: null,
            controls: { search: '' }, $refs: { linesView: view },
            getLogPanelBefore: () => new Promise(resolve => {
                resolveBefore = resolve
            }),
            $nextTick: callback => ticks.push(callback),
            $emit: () => {}
        }
        Object.keys(options.methods).forEach(name => {
            vm[name] = options.methods[name].bind(vm)
        })
        vm.requestParams = () => ({ tag: 'step-a' })
        vm.applyResponse = () => {}
        vm.maybeAutoBackfill = () => {}
        vm.continueSearchBackfill = () => {}
        const settle = async () => {
            resolveBefore({ status: 0, logs: [] })
            await new Promise(resolve => setImmediate(resolve))
            ticks.splice(0).forEach(callback => callback())
        }
        return { vm, view, calls, settle }
    }

    const returned = createVm()
    returned.vm.loadBefore()
    returned.view.layoutInteractionToken += 1
    returned.vm.bottomFollow = true
    await returned.settle()
    assert.deepEqual(returned.calls.restore, [], 'returning to the tail while History is pending does not pull the user back')
    assert.equal(returned.calls.preserve, 0)
    assert.equal(returned.calls.bottom, 1, 'the tail-following user stays at the bottom after prepend')

    const reading = createVm()
    reading.vm.loadBefore()
    reading.view.layoutInteractionToken += 1
    reading.view.anchorLineNo = '80'
    await reading.settle()
    assert.deepEqual(reading.calls.restore, [{ lineNo: '80' }], 'the anchor is captured when the response is applied, not at request start')
    assert.equal(reading.calls.bottom, 0)
})

test('minimap counts use complete filtered issues while visible group headers carry hidden issue marks', () => {
    const logs = [
        { lineNo: 10, timestamp: 10, level: 'INFO', text: '##[group]build', displayLineNo: 41 },
        { lineNo: 11, timestamp: 11, level: 'ERROR', text: 'compiler failed', displayLineNo: 42 },
        { lineNo: 12, timestamp: 12, level: 'WARN', text: 'retry', displayLineNo: 43 },
        { lineNo: 13, timestamp: 13, level: 'INFO', text: '##[endgroup]', displayLineNo: 44 },
        { lineNo: 14, timestamp: 14, level: 'ERROR', text: 'outside failure', displayLineNo: 45 }
    ]
    const model = logStructure.buildLogStructure(logs, { context: 'job|step', complete: true })
    const projected = logStructure.projectLogStructure(model, { enabled: true, collapsedIds: [model.groups[0].id] })
    const options = loadComponentOptions('../LogErrorMinimap.vue', adapter)
    assert.equal(options.computed.errorCount.call({ issueLogs: logs, logs: projected }), 2)
    assert.equal(options.computed.warnCount.call({ issueLogs: logs, logs: projected }), 1)
    const marks = adapter.sampleLogIssueMarks(projected)
    assert.deepEqual(marks.map(mark => [mark.cls, mark.index, mark.hiddenCount]), [
        ['is-error', 0, 1], ['is-warn', 0, 1], ['is-error', 1, 0]
    ])
    const linesView = readFileSync(new URL('../LogLinesView.vue', import.meta.url), 'utf8')
    assert.match(linesView, /lp-group-issue-count is-error/)
    assert.match(linesView, /lp-group-issue-count is-warn/)
})

test('LogLinesView group issue counters safely render ordinary and incomplete group rows', () => {
    const options = loadComponentOptions('../LogLinesView.vue', {
        ...adapter,
        ...virtualWindow,
        markRaw,
        shallowRef
    }, { LogErrorMinimap: {} })
    const Lines = Vue.extend(options)
    const vm = new Lines({ propsData: { logs: [], displayBase: 1 } })
    const mixedRows = [
        { lineNo: 1, text: 'ordinary log row' },
        { lineNo: 2, text: 'group without issue counts', _logGroup: { id: 'a', collapsed: true } },
        { lineNo: 3, text: 'group with empty issue counts', _logGroup: { id: 'b', issueCounts: null } },
        { lineNo: 4, text: 'group with issue counts', _logGroup: { id: 'c', issueCounts: { error: 2, warn: 3 } } }
    ]
    assert.doesNotThrow(() => mixedRows.forEach(row => {
        vm.groupIssueCount(row, 'error')
        vm.groupIssueCount(row, 'warn')
    }))
    assert.deepEqual(mixedRows.map(row => [vm.groupIssueCount(row, 'error'), vm.groupIssueCount(row, 'warn')]), [
        [0, 0], [0, 0], [0, 0], [2, 3]
    ])
    const template = readFileSync(new URL('../LogLinesView.vue', import.meta.url), 'utf8')
    assert.match(template, /v-if="groupIssueCount\(row\.line, 'error'\)"/)
    assert.match(template, /v-if="groupIssueCount\(row\.line, 'warn'\)"/)
    vm.$destroy()
})

test('search locate keeps the actual highlighted text in view across wrap and no-wrap layouts', () => {
    const options = loadComponentOptions('../LogLinesView.vue', {
        ...adapter,
        ...virtualWindow,
        markRaw,
        shallowRef
    }, { LogErrorMinimap: {} })
    let hitRect = { left: 36494, right: 36602, top: 300, bottom: 320 }
    const hit = { getBoundingClientRect: () => hitRect }
    const row = { querySelector: selector => selector === '.lp-hl' ? hit : null }
    const box = {
        clientLeft: 0,
        clientTop: 0,
        clientWidth: 994,
        clientHeight: 744,
        scrollLeft: 0,
        scrollTop: 1000,
        scrollWidth: 72397,
        scrollHeight: 3000000,
        getBoundingClientRect: () => ({ left: 280, top: 141 }),
        querySelectorAll: () => []
    }
    const context = {
        wrap: false,
        locateGeneration: 4,
        scrollTop: 1000,
        scrollDelta: 0,
        updateViewport () {},
        captureAnchor: () => null,
        $refs: { box },
        findLineRow: lineNo => lineNo === 50000 ? row : null
    }
    options.methods.revealSearchHitInViewport.call(context, 50000, 4)
    assert.equal(box.scrollLeft, 35328)
    box.scrollLeft = 0
    options.methods.revealSearchHitInViewport.call(context, 50000, 3)
    assert.equal(box.scrollLeft, 0, 'stale locate generations cannot move the horizontal viewport')
    context.wrap = true
    context.locateGeneration = 5
    box.scrollLeft = 12
    hitRect = { left: 36494, right: 36602, top: 1526, bottom: 1540 }
    options.methods.revealSearchHitInViewport.call(context, 50000, 5)
    assert.equal(box.scrollLeft, 12)
    assert.equal(box.scrollTop, 1655, 'wrapped long-line hits scroll vertically by the mark rect, not by the full row')
    const source = readFileSync(new URL('../LogLinesView.vue', import.meta.url), 'utf8')
    assert.match(source, /wrap \(val\)[\s\S]*this\.cancelLocateCalibration\(\)[\s\S]*revealSearchHitInViewport\(this\.highlightedLineNo, generation\)/)
    assert.match(source, /showTime \(\)[\s\S]*this\.cancelLocateCalibration\(\)[\s\S]*revealSearchHitInViewport\(this\.highlightedLineNo, generation\)/)
})

test('sizes the shared line number column to the widest displayed line number', () => {
    const twoDigits = adapter.withDisplayLineNumbers(Array.from({ length: 37 }, () => ({})), 1)
    const fourDigits = adapter.withDisplayLineNumbers(Array.from({ length: 1001 }, () => ({})), 1)
    assert.equal(adapter.resolveLogLineNumberColumnWidth(twoDigits), 2)
    assert.equal(adapter.resolveLogLineNumberColumnWidth(fourDigits), 4)
    assert.equal(adapter.resolveLogLineNumberColumnWidth([]), 1)
})

test('getRawLogText preserves ANSI while getLogText strips for search', () => {
    const log = { message: '\u001b[31mfail\u001b[0m' }
    assert.match(adapter.getRawLogText(log), /\u001b\[31m/)
    assert.equal(adapter.getLogText(log), 'fail')
})

test('log issue levels and ANSI colors support minimap and main log rendering safely', () => {
    assert.equal(adapter.getLogIssueLevel({ level: 'ERROR', text: 'failed' }), 'error')
    assert.equal(adapter.getLogIssueLevel({ text: '##[warning] slow' }), 'warn')
    assert.equal(adapter.getLogIssueLevel({ text: '##[warn] slow' }), 'warn')
    const mixed = adapter.renderLogHtml('plain \u001b[31m<error>\u001b[0m \u001b[33mwarning\u001b[0m', '')
    assert.match(mixed, /plain <span style="color:#ff6666">&lt;error&gt;<\/span>/)
    assert.match(mixed, /<span style="color:#ff9c01">warning<\/span>/)
    assert.doesNotMatch(mixed, /<error>/)
    const minimap = readFileSync(new URL('../LogErrorMinimap.vue', import.meta.url), 'utf8')
    const linesView = readFileSync(new URL('../LogLinesView.vue', import.meta.url), 'utf8')
    assert.match(minimap, /class="lp-minimap-bar"[\s\S]*:style="previewBarStyle\(row\)"/)
    assert.doesNotMatch(minimap, /v-html|previewText\(/)
    assert.match(minimap, /'--lp-bar-color': color/)
    const linesTemplate = linesView.split('<script>')[0]
    assert.match(linesTemplate, /logLineClass\(row\.line\)/)
    assert.doesNotMatch(linesTemplate, /getLogIssueLevel/)
    assert.match(linesView, /logLineClass \(line\) \{[\s\S]*getLogIssueLevel\(line\) \|\| String\(line\.level/)
})

test('user-facing error summaries and conclusion details strip ANSI without changing ordinary brackets or raw logs', () => {
    const screenshotText = '\u001b[1;31mfailed\u001b[m / \u001b[0m'
    const escapedText = String.raw`\u001b[1;31mfailed\u001b[0m`
    assert.equal(adapter.stripAnsiForDisplay(screenshotText), 'failed / ')
    assert.equal(adapter.stripAnsiForDisplay(escapedText), 'failed')
    assert.equal(adapter.stripAnsiForDisplay('[ERROR] failed [ordinary text]'), '[ERROR] failed [ordinary text]')
    assert.equal(adapter.stripAnsiForDisplay('[1;31m is user text without an ESC control byte'), '[1;31m is user text without an ESC control byte')
    assert.equal(adapter.stripAnsiForDisplay('\u009b31mfailed\u009bm'), 'failed')
    assert.equal(adapter.stripAnsiForDisplay('before\u001b]0;window title\u0007after'), 'beforeafter')
    assert.equal(adapter.stripAnsiForDisplay('before\u001b]8;;https://example.test\u001b\\link\u001b]8;;\u001b\\after'), 'beforelinkafter')
    assert.equal(adapter.stripAnsiForDisplay('before\u009d0;title\u009cafter'), 'beforeafter')
    assert.equal(adapter.resolveLogErrorSummary([{ taskId: 'step', containerId: 'job', errorMsg: screenshotText }], { id: 'step', containerId: 'job' }).errorMsg, 'failed / ')
    assert.equal(adapter.resolveConclusionMessage({ errorMsg: screenshotText }), 'failed / ')
    const raw = { message: screenshotText }
    assert.equal(adapter.getRawLogText(raw), screenshotText)
    assert.equal(adapter.getLogIssueLevel({ text: '\u001b[1;31m##[error] failed\u001b[m' }), 'error')
})

test('unterminated repeated OSC sequences are handled linearly and preserve trailing text', () => {
    const malformed = '\u001b]'.repeat(10_000) + 'tail'
    const startedAt = Date.now()
    assert.equal(adapter.stripAnsiForDisplay(malformed), 'tail')
    assert.ok(Date.now() - startedAt < 1000)
    assert.equal(adapter.stripAnsiForDisplay('before\u001b]unterminated payload'), 'beforeunterminated payload')
})

test('renderLogHtml via raw getter keeps ESC and bare SGR color', () => {
    const escHtml = adapter.renderLogHtml(adapter.getRawLogText({ message: '\u001b[31mfail\u001b[0m' }), '')
    assert.match(escHtml, /color:#ff6666/)
    const bareHtml = adapter.renderLogHtml(adapter.getRawLogText({ message: '[31mfail[0m' }), '')
    assert.match(bareHtml, /color:#ff6666/)
})

test('sampleLogIssueMarks scans full log and keeps tail ERROR within cap', () => {
    const logs = [
        ...Array.from({ length: 500 }, (_, i) => ({ level: 'WARN', lineNo: i + 1 })),
        { level: 'ERROR', lineNo: 501 }
    ]
    const marks = adapter.sampleLogIssueMarks(logs, 400)
    assert.equal(marks.length, 400)
    assert.ok(marks.some(mark => mark.key === 'error-500'))
})

test('sampleLogIssueMarks retains head issues when sampling', () => {
    const logs = [
        { level: 'ERROR', lineNo: 1 },
        ...Array.from({ length: 600 }, (_, i) => ({ level: 'WARN', lineNo: i + 2 }))
    ]
    const marks = adapter.sampleLogIssueMarks(logs, 400)
    assert.ok(marks.some(mark => mark.key === 'error-0'))
})

test('minimap layout keeps short logs compact and wraps wide text at design density', () => {
    const compact = adapter.resolveLogMinimapLayout([
        { text: 'first' },
        { text: 'second' },
        { text: 'third' }
    ])
    assert.equal(compact.totalHeight, 24)
    assert.equal(compact.isSampled, false)
    assert.deepEqual(compact.previewRows.map(row => row.topPx), [0, 8, 16])

    const wrapped = adapter.resolveLogMinimapLayout([
        { text: '汉'.repeat(50) },
        { text: 'x'.repeat(100) },
        { text: 'first\nsecond' }
    ])
    assert.deepEqual(wrapped.lineHeights, [16, 16, 16])
    assert.deepEqual(wrapped.previewRows.map(row => row.topPx), [0, 16, 32])
})

test('minimap shares line, issue, viewport, drag, and keyboard coordinates', () => {
    const logs = Array.from({ length: 800 }, (_, index) => ({
        lineNo: index + 1,
        text: index === 0 ? '##[error] first issue' : index === 799 ? '##[warning] tail issue' : `line ${index + 1}`
    }))
    const layout = adapter.resolveLogMinimapLayout(logs)
    assert.equal(layout.previewRows.length, 240)
    assert.equal(layout.isSampled, true)
    assert.equal(layout.previewRows[0].index, 0)
    assert.equal(layout.previewRows.at(-1).index, 799)
    assert.equal(layout.previewRows[1].topPx, layout.lineTops[layout.previewRows[1].index])
    assert.equal(layout.previewRows[2].topPx, layout.lineTops[layout.previewRows[2].index])
    assert.equal(layout.previewRows[1].heightPx, 8)
    assert.deepEqual(layout.issueMarks.map(mark => mark.cls), ['is-error', 'is-warn'])
    assert.equal(layout.issueMarks[0].topPx, layout.lineTops[0] + 4)
    assert.equal(adapter.resolveLogMinimapLineIndex(layout, 0), 0)
    assert.equal(adapter.resolveLogMinimapLineIndex(layout, layout.totalHeight, 0), 799)

    const viewport = adapter.resolveLogMinimapCoordinates(layout, { top: 50, height: 2.5 }, 500)
    assert.ok(layout.issueMarks.every(mark => mark.topPx * viewport.scale < 500))
    assert.equal(viewport.contentOffsetPx, 0)
    const firstIssueTop = adapter.resolveLogMinimapMarkTop(layout.issueMarks[0], viewport)
    assert.equal(firstIssueTop, Math.max(3, layout.issueMarks[0].topPx * viewport.scale))
    assert.ok(viewport.viewportTopPx > 0)
    assert.equal(viewport.viewportHeightPx, 32)
    assert.equal(viewport.scale, 500 / layout.totalHeight)
    const shortViewport = adapter.resolveLogMinimapCoordinates(
        adapter.resolveLogMinimapLayout([{ text: 'one' }, { text: 'two' }]),
        { top: 25, height: 50 },
        500
    )
    assert.equal(shortViewport.contentOffsetPx, 0)
    assert.equal(shortViewport.viewportTopPx, 125)
    assert.equal(shortViewport.viewportHeightPx, 250)
    const unscrollableViewport = adapter.resolveLogMinimapCoordinates(
        adapter.resolveLogMinimapLayout([{ text: 'one' }, { text: 'two' }]),
        { top: 0, height: 100 },
        500
    )
    assert.equal(unscrollableViewport.viewportHeightPx, unscrollableViewport.contentHeightPx)
    assert.ok(unscrollableViewport.viewportHeightPx < 500)

    assert.equal(adapter.resolveLogMinimapPanRatio({ pointerY: 150, trackTop: 100, grabOffset: 10, trackHeight: 100, viewportHeight: 20 }), 0.5)
    assert.equal(adapter.resolveLogMinimapPanRatio({ trackHeight: 100, viewportHeight: 20, currentRatio: 0.5, deltaY: 20, tallContent: true }), 0.75)
    assert.equal(adapter.resolveLogMinimapKeyboardRatio(0.5, 'ArrowDown'), 0.55)
    assert.equal(adapter.resolveLogMinimapKeyboardRatio(0.5, 'PageUp'), 0.3)
    assert.equal(adapter.resolveLogMinimapKeyboardRatio(0.5, 'Home'), 0)
    assert.equal(adapter.resolveLogMinimapKeyboardRatio(0.5, 'End'), 1)
    assert.equal(adapter.resolveLogMinimapKeyboardRatio(0.5, 'Escape'), null)

    const minimap = readFileSync(new URL('../LogErrorMinimap.vue', import.meta.url), 'utf8')
    const linesView = readFileSync(new URL('../LogLinesView.vue', import.meta.url), 'utf8')
    assert.match(minimap, /resolveLogMinimapLayout\(this\.logs, \{[\s\S]*heightIndex: this\.heightIndex,[\s\S]*scrollMetrics: this\.scrollMetrics[\s\S]*\}\)/)
    assert.match(minimap, /previewRowStyle \(row\)[\s\S]*resolveLogMinimapPreviewRowGeometry\(row, this\.coordinates\)/)
    assert.match(minimap, /issueMarkTop \(mark\)[\s\S]*resolveLogMinimapMarkTop\(mark, this\.coordinates\)/)
    assert.match(minimap, /getLogIssueLevel\(line\)/)
    assert.match(minimap, /resolveLogMinimapTarget\(this\.minimapLayout, clientY - rect\.top\)/)
    assert.match(minimap, /resolveLogMinimapPanRatio\(/)
    assert.match(minimap, /resolveLogMinimapKeyboardRatio\(/)
    assert.match(linesView, /:logs="renderLines"[\s\S]*:viewport="viewport"/)
    assert.match(minimap, /previewBarStyle \(row\)[\s\S]*resolveLogMinimapVisualMetrics\(this\.coordinates\.scale, row\.firstVisualHeight \?\? row\.sourceHeight \?\? row\.heightPx\)/)
    assert.match(minimap, /width: calc\(\(100% - 30px\) \* 0\.8\)/)
    assert.match(minimap, /background: repeating-linear-gradient\([\s\S]*var\(--lp-bar-first-start\)[\s\S]*var\(--lp-bar-first-end\)[\s\S]*var\(--lp-bar-period\)/)
    assert.match(minimap, /\.lp-minimap-line\.is-error::before \{ background: #313328; \}/)
    assert.match(minimap, /\.lp-minimap-badges[\s\S]*padding-left:\s*9px/)
    assert.match(minimap, /icon-close-circle-fill lp-dot is-error[\s\S]*class="lp-badge-count">\{\{ errorCount \}\}/)
    assert.match(minimap, /icon-exclamation-circle-shape lp-dot is-warn[\s\S]*class="lp-badge-count">\{\{ warnCount \}\}/)
    assert.match(minimap, /\.lp-minimap-badges \{[\s\S]*column-gap:\s*8px;[\s\S]*height:\s*8px/)
    assert.doesNotMatch(minimap, /\.lp-minimap-badges\s*>\s*\*\s*\+\s*\*\s*\{\s*margin-left/)
    assert.match(minimap, /\.lp-badge \{[\s\S]*column-gap:\s*2px;[\s\S]*height:\s*8px;[\s\S]*font-family:\s*-apple-system, system-ui,[\s\S]*font-size:\s*6px;[\s\S]*line-height:\s*8px/)
    assert.match(minimap, /\.lp-badge-count \{[\s\S]*height:\s*8px;[\s\S]*font:\s*6px\/8px -apple-system, system-ui/)
    assert.match(minimap, /\.lp-dot \{[\s\S]*flex:\s*0 0 8px;[\s\S]*align-items:\s*center;[\s\S]*width:\s*8px;[\s\S]*height:\s*8px/)
    assert.match(minimap, /icon-close-circle-fill lp-dot is-error/)
    assert.match(minimap, /icon-exclamation-circle-shape lp-dot is-warn/)
    assert.match(minimap, /mark\.cls === 'is-error' \? 'icon-close-circle-fill' : 'icon-exclamation-circle-shape'/)
    assert.match(minimap, /\.lp-minimap-tick \{[\s\S]*left:\s*4px;[\s\S]*width:\s*4px;[\s\S]*height:\s*4px;[\s\S]*margin-top:\s*-2px;[\s\S]*pointer-events:\s*none;/)
    assert.match(minimap, /\.lp-minimap-tick \{ font-size: 4px; line-height: 4px; \}/)
    assert.match(minimap, /v-if="coordinates\.interactive"/)
    assert.doesNotMatch(minimap, /min-height:\s*32px/)
    assert.match(minimap, /\.lp-minimap-line\.is-warn::before \{ background: #313328; \}/)
    assert.match(minimap, /background: rgba\(151, 155, 165, 0\.22\)/)
    assert.doesNotMatch(minimap, /lp-viewport-visible-range/)
    assert.match(minimap, /contentStyle \(\) \{\s*return \{ height: this\.coordinates\.contentHeightPx \+ 'px' \}/)
    assert.doesNotMatch(minimap, /translateY\(/)
    assert.doesNotMatch(minimap, /flex:\s*1 1 0/)
    assert.match(linesView, /const totalHeight = el\.scrollHeight[\s\S]*this\.scrollTop \/ totalHeight/)
})

test('minimap uses real viewport scrollability for medium-length logs', () => {
    const logs = Array.from({ length: 40 }, (_, index) => ({ lineNo: index + 1, text: `line ${index + 1}` }))
    const layout = adapter.resolveLogMinimapLayout(logs)
    assert.equal(layout.totalHeight, 320)

    const middleViewport = adapter.resolveLogMinimapCoordinates(layout, { top: 12.5, height: 75 }, 600)
    assert.equal(middleViewport.scale, 1)
    assert.equal(middleViewport.contentHeightPx, 320)
    assert.equal(middleViewport.viewportHeightPx, 450)
    assert.ok(middleViewport.viewportHeightPx < 600)
    const firstRowGeometry = adapter.resolveLogMinimapPreviewRowGeometry(layout.previewRows[19], middleViewport)
    const nextRowGeometry = adapter.resolveLogMinimapPreviewRowGeometry(layout.previewRows[20], middleViewport)
    assert.equal(firstRowGeometry.heightPx, 8)
    assert.equal(nextRowGeometry.topPx, firstRowGeometry.topPx + firstRowGeometry.heightPx)
    assert.equal(nextRowGeometry.heightPx, 8)
    const nextRowHitY = nextRowGeometry.topPx - middleViewport.contentOffsetPx + nextRowGeometry.heightPx / 2
    assert.equal(adapter.resolveLogMinimapLineIndex(layout, nextRowHitY, middleViewport.contentOffsetPx, middleViewport.scale), 20)
    const middleClickIndex = adapter.resolveLogMinimapLineIndex(layout, 300, middleViewport.contentOffsetPx, middleViewport.scale, middleViewport.hitScale)
    assert.ok(middleClickIndex >= 35 && middleClickIndex <= 39)

    const endViewport = adapter.resolveLogMinimapCoordinates(layout, { top: 25, height: 75 }, 600)
    const endClickIndex = adapter.resolveLogMinimapLineIndex(layout, 599, endViewport.contentOffsetPx, endViewport.scale)
    assert.equal(endClickIndex, 39)
    assert.equal(adapter.resolveLogMinimapPanRatio({ trackHeight: 600, viewportHeight: middleViewport.viewportHeightPx, currentRatio: 0.5, deltaY: 75, tallContent: true }), 1)
})

test('sampled minimap fits its track with clipped rows while preserving log coverage', () => {
    const logs = Array.from({ length: 1000 }, (_, index) => ({ lineNo: index + 1, text: `line-${index + 1} ${'very long '.repeat(80)}` }))
    const layout = adapter.resolveLogMinimapLayout(logs, { previewCap: 10 })
    assert.equal(layout.isSampled, true)
    assert.equal(layout.previewRows.length, 10)
    assert.equal(layout.previewRows[0].index, 0)
    assert.equal(layout.previewRows.at(-1).index, 999)
    assert.deepEqual(layout.previewRows.map(row => row.topPx), layout.previewRows.map(row => layout.lineTops[row.index]))
    assert.equal(layout.previewTotalHeight, layout.totalHeight)
    const sampleCoordinates = adapter.resolveLogMinimapCoordinates(layout, { top: 0, height: 0 }, 80)
    assert.equal(adapter.resolveLogMinimapLineIndex(layout, 0), 0)
    assert.equal(adapter.resolveLogMinimapLineIndex(layout, 80, 0, sampleCoordinates.scale), 999)
    const minimap = readFileSync(new URL('../LogErrorMinimap.vue', import.meta.url), 'utf8')
    assert.match(minimap, /Math\.floor\(this\.trackHeight \/ 8\)/)
    assert.match(minimap, /\.lp-minimap-line \{[\s\S]*overflow: hidden;[\s\S]*pointer-events: auto;/)
    assert.match(minimap, /\.lp-minimap-bar \{[\s\S]*left:\s*10px;[\s\S]*width:\s*calc\(\(100% - 30px\) \* 0\.8\);[\s\S]*height:\s*100%/)
})

test('minimap samples wrapped short logs when estimated content exceeds the track', () => {
    const logs = [
        { lineNo: 1, level: 'ERROR', text: 'first issue' },
        { lineNo: 2, text: 'second' },
        { lineNo: 3, text: 'third' },
        { lineNo: 4, level: 'WARN', text: 'x'.repeat(900) }
    ]
    const layout = adapter.resolveLogMinimapLayout(logs, { previewCap: 4, trackHeight: 36 })
    assert.equal(layout.totalHeight, 120)
    assert.equal(layout.isSampled, true)
    assert.equal(layout.previewRows.length, 4)
    assert.deepEqual(layout.previewRows.map(row => row.topPx), layout.previewRows.map(row => layout.lineTops[row.index]))
    assert.equal(layout.previewTotalHeight, layout.totalHeight)
    assert.equal(layout.previewRows[0].index, 0)
    assert.equal(layout.previewRows.at(-1).index, 3)

    const coordinates = adapter.resolveLogMinimapCoordinates(layout, { top: 0, height: 50 }, 36)
    const tailIndex = adapter.resolveLogMinimapLineIndex(layout, 35, coordinates.contentOffsetPx, coordinates.scale, coordinates.hitScale)
    assert.equal(tailIndex, 3)
    assert.equal(logs[tailIndex].lineNo, 4)
    assert.equal(coordinates.viewportHeightPx, 32)
    assert.equal(coordinates.contentOffsetPx, 0)
    assert.ok(layout.issueMarks.every(mark => mark.topPx >= 0 && mark.topPx < layout.totalHeight))
    assert.ok(layout.issueMarks.every(mark => mark.topPx * coordinates.scale < 36))
})

test('sampled minimap never repeats rows when wrapped content exceeds track height', () => {
    const logs = Array.from({ length: 20 }, (_, index) => ({
        lineNo: index + 1,
        level: index === 0 ? 'ERROR' : index === 19 ? 'WARN' : 'INFO',
        text: 'x'.repeat(300)
    }))
    const layout = adapter.resolveLogMinimapLayout(logs, { previewCap: 70, trackHeight: 562 })
    const rowIndexes = layout.previewRows.map(row => row.index)
    assert.equal(layout.totalHeight, 640)
    assert.equal(layout.previewRows.length, 20)
    assert.equal(new Set(rowIndexes).size, 20)
    assert.equal(rowIndexes[0], 0)
    assert.equal(rowIndexes.at(-1), 19)
    assert.deepEqual(layout.previewRows.map(row => row.topPx), layout.lineTops)

    const coordinates = adapter.resolveLogMinimapCoordinates(layout, { top: 0, height: 50 }, 562)
    const tailIndex = adapter.resolveLogMinimapLineIndex(layout, 561, coordinates.contentOffsetPx, coordinates.scale, coordinates.hitScale)
    assert.equal(tailIndex, 19)
    assert.deepEqual(layout.issueMarks.map(mark => mark.index), [0, 19])
    assert.ok(layout.issueMarks.every(mark => mark.topPx >= 0 && mark.topPx < layout.totalHeight))
    assert.ok(layout.issueMarks.every(mark => mark.topPx * coordinates.scale < 562))
})

test('static minimap sampling retains ordinary source-order rows instead of prioritizing issue levels', () => {
    const logs = Array.from({ length: 50 }, (_, index) => ({
        lineNo: index + 1,
        level: [20, 21, 22].includes(index) ? 'WARN' : index === 40 ? 'ERROR' : 'INFO',
        text: `line-${index + 1}`
    }))
    const layout = adapter.resolveLogMinimapLayout(logs, { previewCap: 10, trackHeight: 80 })
    assert.equal(layout.isSampled, true)
    assert.equal(layout.previewRows.length, 10)
    assert.equal(new Set(layout.previewRows.map(row => row.index)).size, 10)
    assert.equal(layout.previewRows[0].index, 0)
    assert.equal(layout.previewRows.at(-1).index, 49)
    assert.deepEqual(layout.previewRows.map(row => row.index), [0, 5, 11, 16, 22, 27, 33, 38, 44, 49])
    assert.ok(layout.previewRows.some(row => row.line.level === 'INFO'))
    assert.deepEqual(layout.issueMarks.map(mark => mark.index), [20, 21, 22, 40])
    layout.issueMarks.forEach(mark => {
        assert.equal(mark.topPx, layout.lineTops[mark.index] + Math.min(8, layout.lineHeights[mark.index]) / 2)
        assert.ok(mark.topPx >= 0)
        assert.equal(adapter.getLogIssueLevel(logs[mark.index]), mark.cls === 'is-error' ? 'error' : 'warn')
    })

    const issueHeavyLogs = Array.from({ length: 50 }, (_, index) => ({
        lineNo: index + 1,
        level: index >= 10 && index < 40 ? 'WARN' : 'INFO',
        text: `line-${index + 1}`
    }))
    const capped = adapter.resolveLogMinimapLayout(issueHeavyLogs, { previewCap: 8, trackHeight: 64 })
    assert.equal(capped.previewRows.length, 8)
    assert.ok(capped.issueMarks.length <= 8)
    assert.ok(capped.issueMarks.every(mark => mark.index >= 0 && mark.index < issueHeavyLogs.length
        && mark.topPx === capped.lineTops[mark.index] + Math.min(8, capped.lineHeights[mark.index]) / 2))
})

test('continuous minimap uses one source-pixel window for 100k rows, viewport box, clicks, and full-range drag', () => {
    const logs = Array.from({ length: 100000 }, (_, index) => ({ lineNo: index + 1, level: 'INFO', text: `line-${index + 1}` }))
    const heightIndex = virtualWindow.createHeightIndex().rebuild(logs, {}, false)
    const options = { trackHeight: 698, heightIndex, lineOrigin: 8, contentHeight: 2000016 }
    const at = scrollTop => adapter.resolveLogMinimapLayout(logs, {
        ...options,
        scrollMetrics: { scrollTop, clientHeight: 722, contentHeight: 2000016, scrollHeight: 2000016 }
    })

    const top = at(0)
    const topCoordinates = adapter.resolveLogMinimapCoordinates(top, null, 698)
    assert.equal(top.mapping.scale, 0.4)
    assert.equal(top.mapping.contentHeightPx, 698)
    assert.equal(top.mapping.sourceWindowTop, 0)
    assert.equal(topCoordinates.viewportTopPx, 0)
    assert.equal(topCoordinates.viewportHeightPx, 288.8)
    assert.equal(top.previewRows[0].index, 0)

    const middle = at(999647)
    const coordinates = adapter.resolveLogMinimapCoordinates(middle, null, 698)
    assert.equal(middle.mapping.sourceWindowTop, 999135.5)
    assert.equal(middle.mapping.sourceWindowHeight, 1745)
    assert.equal(coordinates.viewportTopPx, 204.6)
    assert.equal(coordinates.viewportHeightPx, 288.8)
    assert.equal(middle.previewRows.length, 88)
    assert.equal(middle.previewRows[0].index, 49956)
    assert.equal(middle.previewRows[0].topPx, -3)
    assert.equal(middle.previewRows[0].heightPx, 8)
    const middleFirstGeometry = adapter.resolveLogMinimapPreviewRowGeometry(middle.previewRows[0], coordinates)
    assert.ok(Math.abs(middleFirstGeometry.topPx - -3) < 1e-9)
    assert.equal(middleFirstGeometry.heightPx, 8)
    assert.equal(middle.previewRows[1].index, 49957)
    assert.equal(middle.previewRows[1].topPx, 5)
    assert.ok(Math.abs(adapter.resolveLogMinimapPreviewRowGeometry(middle.previewRows[1], coordinates).topPx - 5) < 1e-9)
    assert.equal(middle.previewRows.at(-1).index, 50043)
    assert.ok(middle.previewRows.every((row, index) => index === 0 || row.index === middle.previewRows[index - 1].index + 1))
    const click = adapter.resolveLogMinimapTarget(middle, 349)
    assert.equal(click.sourcePosition, 1000008)
    assert.equal(click.index, 50000)
    assert.equal(click.localOffset, 0)
    assert.equal(click.targetScrollTop, 999647)
    assert.equal(adapter.resolveLogMinimapLineIndex(middle, 349), 50000)
    assert.ok(Math.abs(coordinates.viewportTopPx - coordinates.scale * (middle.mapping.scrollTop - middle.mapping.sourceWindowTop)) < 1e-9)

    const end = at(1999294)
    const endCoordinates = adapter.resolveLogMinimapCoordinates(end, null, 698)
    assert.equal(end.mapping.sourceWindowTop, 1998271)
    assert.equal(endCoordinates.viewportTopPx, 409.2)
    const endFirstGeometry = adapter.resolveLogMinimapPreviewRowGeometry(end.previewRows[0], endCoordinates)
    assert.ok(Math.abs(endFirstGeometry.topPx - -1.2) < 1e-9)
    assert.equal(adapter.resolveLogMinimapTarget(end, 698).index, 99999)
    assert.equal(adapter.resolveLogMinimapPanRatio({ pointerY: 0, trackTop: 0, grabOffset: 0, trackHeight: 698, viewportHeight: 288.8 }), 0)
    assert.equal(adapter.resolveLogMinimapPanRatio({ pointerY: 204.6, trackTop: 0, grabOffset: 0, trackHeight: 698, viewportHeight: 288.8 }), 0.5)
    assert.equal(adapter.resolveLogMinimapPanRatio({ pointerY: 409.2, trackTop: 0, grabOffset: 0, trackHeight: 698, viewportHeight: 288.8 }), 1)
})

test('LogErrorMinimap keyboard activates the matching source row inside a local 100k preview window', () => {
    const logs = Array.from({ length: 100000 }, (_, index) => ({ lineNo: index + 1, text: `line-${index + 1}` }))
    const heightIndex = virtualWindow.createHeightIndex().rebuild(logs, {}, false)
    const layout = adapter.resolveLogMinimapLayout(logs, {
        trackHeight: 698,
        heightIndex,
        lineOrigin: 8,
        scrollMetrics: { scrollTop: 200, clientHeight: 722, scrollHeight: 2000016 }
    })
    const row = layout.previewRows.find(item => item.index === 9)
    assert.equal(row.line.lineNo, 10)

    const component = loadComponentOptions('../LogErrorMinimap.vue', adapter)
    const emitted = []
    const vm = {
        ...component.methods,
        logs,
        previewRows: layout.previewRows,
        minimapLayout: layout,
        coordinates: adapter.resolveLogMinimapCoordinates(layout, null, 698),
        $refs: { track: { getBoundingClientRect: () => ({ top: 0, height: 698 }) } },
        $emit: (name, value) => emitted.push([name, value])
    }
    Object.keys(component.methods).forEach(name => {
        vm[name] = component.methods[name].bind(vm)
    })

    vm.onPreviewClick({ detail: 0, currentTarget: { dataset: { index: String(row.index) } } }, row.line)
    assert.equal(emitted[0][0], 'locate-target')
    assert.equal(emitted[0][1].index, 9)
    assert.equal(emitted[0][1].line.lineNo, 10)
})

test('continuous minimap preview keeps ordinary rows in source order and issue marks on the same projected rows', () => {
    const logs = Array.from({ length: 10 }, (_, index) => ({
        lineNo: index + 1,
        level: index === 5 ? 'WARN' : index === 7 ? 'ERROR' : 'INFO',
        text: `ordinary source row ${index + 1}`
    }))
    const heightIndex = virtualWindow.createHeightIndex().rebuild(logs)
    const layout = adapter.resolveLogMinimapLayout(logs, {
        trackHeight: 100,
        heightIndex,
        lineOrigin: 8,
        scrollMetrics: { scrollTop: 80, clientHeight: 40, scrollHeight: 216 }
    })

    assert.ok(layout.previewRows.some(row => row.line.text.includes('ordinary source row')))
    assert.deepEqual(layout.previewRows.map(row => row.index), Array.from({ length: layout.previewRows.length }, (_, offset) => layout.previewRows[0].index + offset))
    assert.deepEqual(layout.issueMarks.map(mark => [mark.index, mark.cls]), [[5, 'is-warn'], [7, 'is-error']])
    layout.issueMarks.forEach(mark => {
        const row = layout.previewRows.find(item => item.index === mark.index)
        assert.ok(row)
        assert.ok(mark.topPx >= row.topPx && mark.topPx <= row.topPx + row.heightPx)
    })
    assert.equal(adapter.resolveLogMinimapTarget(layout, layout.previewRows.find(row => row.index === 5).topPx + 4).index, 5)
})

test('continuous minimap preserves intra-line offsets for 1660px and 2080px single-row logs', () => {
    const line = { lineNo: 1, text: 'long line' }
    const logs = [line]
    const index1660 = virtualWindow.createHeightIndex().rebuild(logs, { 1: 1660 }, true)
    const layout1660 = adapter.resolveLogMinimapLayout(logs, {
        trackHeight: 698,
        heightIndex: index1660,
        lineOrigin: 8,
        contentHeight: 1676,
        scrollMetrics: { scrollTop: 477, clientHeight: 722, contentHeight: 1676, scrollHeight: 1676 }
    })
    const coordinates1660 = adapter.resolveLogMinimapCoordinates(layout1660, null, 698)
    assert.equal(layout1660.mapping.scale, 0.4)
    assert.ok(Math.abs(layout1660.mapping.contentHeightPx - 670.4) < 1e-9)
    assert.equal(layout1660.mapping.sourceWindowTop, 0)
    assert.deepEqual(adapter.resolveLogMinimapPreviewRowGeometry(layout1660.previewRows[0], coordinates1660), {
        topPx: 3.2, sourceHeightPx: 664, heightPx: 664
    })
    assert.ok(Math.abs(coordinates1660.viewportTopPx - 190.8) < 1e-9)
    const click1660 = adapter.resolveLogMinimapTarget(layout1660, 300)
    assert.equal(click1660.key, '1')
    assert.equal(click1660.localOffset, 742)
    assert.equal(click1660.targetScrollTop, 389)

    const index2080 = virtualWindow.createHeightIndex().rebuild(logs, { 1: 2080 }, true)
    const layout2080 = adapter.resolveLogMinimapLayout(logs, {
        trackHeight: 698,
        heightIndex: index2080,
        lineOrigin: 8,
        contentHeight: 2096,
        scrollMetrics: { scrollTop: 687, clientHeight: 722, contentHeight: 2096, scrollHeight: 2096 }
    })
    const coordinates2080 = adapter.resolveLogMinimapCoordinates(layout2080, null, 698)
    assert.ok(Math.abs(layout2080.mapping.sourceWindowTop - 175.5) < 1e-9)
    assert.ok(Math.abs(layout2080.previewRows[0].topPx - -67) < 1e-9)
    assert.ok(Math.abs(layout2080.previewRows[0].heightPx - 832) < 1e-9)
    assert.ok(Math.abs(coordinates2080.viewportTopPx - 204.6) < 1e-9)
    const geometry2080 = adapter.resolveLogMinimapPreviewRowGeometry(layout2080.previewRows[0], coordinates2080)
    assert.ok(Math.abs(geometry2080.topPx - -67) < 1e-9)
    assert.ok(Math.abs(geometry2080.heightPx - 832) < 1e-9)
    const click2080 = adapter.resolveLogMinimapTarget(layout2080, 349)
    assert.equal(click2080.localOffset, 1040)
    assert.equal(click2080.targetScrollTop, 687)
})

test('minimap issue markers align with first visual bars at normal, short, tiny-scale, and clipped positions', () => {
    const logs = Array.from({ length: 25 }, (_, index) => ({
        lineNo: index + 1,
        level: index === 0 ? 'ERROR' : index === 1 ? 'WARN' : index === 2 ? 'ERROR' : 'INFO',
        text: index === 1 ? 'wrapped first line\ncontinuation' : `line ${index + 1}`
    }))
    const heightIndex = virtualWindow.createHeightIndex().rebuild(logs, { 1: 20, 2: 100, 3: 8 })
    const layout = adapter.resolveLogMinimapLayout(logs, {
        trackHeight: 100,
        heightIndex,
        scrollMetrics: { scrollTop: 0, clientHeight: 80, scrollHeight: heightIndex.total() }
    })
    assert.equal(layout.mapping.scale, 0.4)
    const markers = new Map(layout.issueMarks.map(mark => [mark.index, mark]))
    const rowByIndex = new Map(layout.previewRows.map(row => [row.index, row]))
    const minimap = loadComponentOptions('../LogErrorMinimap.vue', adapter)
    const singleMetrics = adapter.resolveLogMinimapVisualMetrics(layout.mapping.scale, rowByIndex.get(0).sourceHeight)
    const multiMetrics = adapter.resolveLogMinimapVisualMetrics(layout.mapping.scale, rowByIndex.get(1).sourceHeight)
    const shortMetrics = adapter.resolveLogMinimapVisualMetrics(layout.mapping.scale, rowByIndex.get(2).sourceHeight)
    const barStyle = minimap.methods.previewBarStyle.call({
        coordinates: { scale: layout.mapping.scale },
        levelOf: line => line.level.toLowerCase()
    }, rowByIndex.get(0))
    const markerStyle = minimap.methods.issueMarkStyle.call({
        coordinates: { scale: layout.mapping.scale },
        issueMarkTop: mark => mark.topPx
    }, markers.get(0))
    const warnMarkerStyle = minimap.methods.issueMarkStyle.call({
        coordinates: { scale: layout.mapping.scale },
        issueMarkTop: mark => mark.topPx
    }, markers.get(1))
    assert.equal(barStyle['--lp-bar-period'], `${singleMetrics.period}px`)
    assert.equal(barStyle['--lp-bar-first-start'], `${singleMetrics.firstStartPx}px`)
    assert.equal(barStyle['--lp-bar-first-end'], `${singleMetrics.firstEndPx}px`)
    assert.equal(Number.parseFloat(markerStyle.top) + Number.parseFloat(markerStyle.marginTop) + Number.parseFloat(markerStyle.height) / 2, markers.get(0).topPx,
        'the actual icon box center equals the actual projected marker coordinate')
    assert.equal(Number.parseFloat(markerStyle.width), 4)
    assert.equal(Number.parseFloat(markerStyle.height), singleMetrics.markerDiameter * 2 / 3)
    assert.equal(Number.parseFloat(markerStyle.fontSize), 4)
    assert.equal(Number.parseFloat(markerStyle.lineHeight), 4)
    assert.equal(Number.parseFloat(markerStyle.left) + Number.parseFloat(markerStyle.width) / 2, 6,
        'the smaller ERROR mark retains its horizontal center')
    assert.equal(Number.parseFloat(markerStyle.top) + Number.parseFloat(markerStyle.marginTop) + Number.parseFloat(markerStyle.height) / 2, markers.get(0).topPx,
        'the smaller ERROR mark retains its vertical center')
    assert.equal(Number.parseFloat(warnMarkerStyle.height), 4, 'WARN uses the same reduced diameter')
    assert.equal(Number.parseFloat(warnMarkerStyle.top) + Number.parseFloat(warnMarkerStyle.marginTop) + Number.parseFloat(warnMarkerStyle.height) / 2, markers.get(1).topPx)
    assert.ok(Math.abs(markers.get(0).topPx - rowByIndex.get(0).topPx - singleMetrics.firstCenterPx) <= 1,
        'single-row ERROR marker must share the first stripe center')
    assert.ok(Math.abs(markers.get(1).topPx - rowByIndex.get(1).topPx - multiMetrics.firstCenterPx) <= 1,
        'multi-row WARN marker must align with its first visual row, not the record midpoint')
    assert.equal(markers.get(1).topPx, 12)
    assert.ok(Math.abs(markers.get(2).topPx - rowByIndex.get(2).topPx - shortMetrics.firstCenterPx) <= 1,
        'a record shorter than 20 source pixels uses its actual first-row height')

    const tinyLogs = Array.from({ length: 10000 }, (_, index) => ({
        lineNo: index + 1,
        level: index === 0 ? 'ERROR' : 'INFO',
        text: `line-${index + 1}`
    }))
    const tinyIndex = virtualWindow.createHeightIndex().rebuild(tinyLogs)
    const tinyLayout = adapter.resolveLogMinimapLayout(tinyLogs, {
        trackHeight: 100,
        heightIndex: tinyIndex,
        scrollMetrics: { scrollTop: 0, clientHeight: 10000, scrollHeight: tinyIndex.total() }
    })
    const tinyMetrics = adapter.resolveLogMinimapVisualMetrics(tinyLayout.mapping.scale, 20)
    const tinyMarkStyle = minimap.methods.issueMarkStyle.call({
        coordinates: { scale: tinyLayout.mapping.scale },
        issueMarkTop: mark => mark.topPx
    }, tinyLayout.issueMarks[0])
    assert.ok(tinyLayout.mapping.scale < 0.05)
    assert.equal(tinyMetrics.period, 20 * tinyLayout.mapping.scale, 'bar period and mark projection share the same clamped scale')
    assert.ok(Math.abs(tinyLayout.issueMarks[0].topPx - tinyMetrics.firstCenterPx) < 1e-9)
    assert.ok(Math.abs(Number.parseFloat(tinyMarkStyle.width) - tinyMetrics.markerDiameter * 2 / 3) < 1e-9,
        'small-scale marks remain proportional after the fixed reduction')
    assert.ok(Math.abs(Number.parseFloat(tinyMarkStyle.top) + Number.parseFloat(tinyMarkStyle.marginTop) + Number.parseFloat(tinyMarkStyle.height) / 2 - tinyLayout.issueMarks[0].topPx) < 1e-9,
        'small-scale mark center remains unchanged')

    const clipLogs = Array.from({ length: 25 }, (_, index) => ({
        lineNo: index + 1,
        level: index === 2 ? 'ERROR' : 'INFO',
        text: `row-${index + 1}`
    }))
    const clipIndex = virtualWindow.createHeightIndex().rebuild(clipLogs, { 1: 20, 2: 20, 3: 100 })
    const clipTop = 51
    const clipSourceHeight = clipIndex.total()
    const clipWindowHeight = 250
    const clipScrollRange = clipSourceHeight - clipWindowHeight
    const clipMaxScroll = clipSourceHeight - 80
    const clipped = adapter.resolveLogMinimapLayout(clipLogs, {
        trackHeight: 100,
        heightIndex: clipIndex,
        scrollMetrics: {
            scrollTop: clipTop / clipScrollRange * clipMaxScroll,
            clientHeight: 80,
            scrollHeight: clipSourceHeight
        }
    })
    assert.equal(clipped.mapping.sourceWindowTop, clipTop)
    assert.ok(clipped.previewRows.some(row => row.index === 2), 'the record body remains visible after its first row')
    assert.equal(clipped.issueMarks.some(mark => mark.index === 2), false, 'a clipped-off first-row center is not replaced by a border dot')
})

test('minimap fallback preview passes its first visual row height to the bar style', () => {
    const logs = [{ lineNo: 1, level: 'ERROR', text: 'a\nb' }]
    const layout = adapter.resolveLogMinimapLayout(logs)
    const row = layout.previewRows[0]
    const mark = layout.issueMarks[0]
    const minimap = loadComponentOptions('../LogErrorMinimap.vue', adapter)
    const barStyle = minimap.methods.previewBarStyle.call({
        coordinates: { scale: 1 },
        levelOf: () => 'error'
    }, row)
    const barCenter = (Number.parseFloat(barStyle['--lp-bar-first-start']) + Number.parseFloat(barStyle['--lp-bar-first-end'])) / 2
    assert.equal(row.heightPx, 16)
    assert.equal(mark.topPx, 4)
    assert.equal(barCenter, mark.topPx)
    assert.equal(row.firstVisualHeight, 8)
})

test('minimap returns an empty noninteractive map until track, viewport, and height index are measured', () => {
    const logs = [{ lineNo: 1, text: 'line' }]
    const index = virtualWindow.createHeightIndex().rebuild(logs)
    const layout = adapter.resolveLogMinimapLayout(logs, {
        trackHeight: 0,
        heightIndex: index,
        lineOrigin: 8,
        scrollMetrics: { scrollTop: 0, clientHeight: 0, scrollHeight: 28 }
    })
    assert.equal(layout.mapping, null)
    assert.deepEqual(layout.previewRows, [])
    assert.equal(adapter.resolveLogMinimapCoordinates(layout, null, 0).interactive, false)
})

test('LogLinesView applies a minimap target by stable key without recentering the whole long row', () => {
    const logs = [{ lineNo: 1, text: 'long line' }]
    const heightIndex = virtualWindow.createHeightIndex().rebuild(logs, { 1: 1660 }, true)
    const layout = adapter.resolveLogMinimapLayout(logs, {
        trackHeight: 698,
        heightIndex,
        lineOrigin: 8,
        contentHeight: 1676,
        scrollMetrics: { scrollTop: 477, clientHeight: 722, contentHeight: 1676, scrollHeight: 1676 }
    })
    const target = adapter.resolveLogMinimapTarget(layout, 300)
    const options = loadComponentOptions('../LogLinesView.vue', { ...adapter, ...virtualWindow, markRaw, shallowRef, LogErrorMinimap: {} })
    const queued = []
    const box = { scrollTop: 477, scrollLeft: 0, clientHeight: 722, scrollHeight: 1676 }
    const vm = {
        ...options.methods,
        $refs: { box },
        $nextTick: callback => queued.push(callback),
        $emit: () => {},
        heightIndex,
        displayLines: logs,
        clientHeight: 722,
        scrollTop: 477,
        scrollDelta: 0,
        pendingAnchor: null,
        _panGeneration: 0,
        _pendingPanGeneration: 0,
        cancelPendingWheel () {},
        cancelLocateCalibration () {
            this.locateGeneration += 1
        },
        lineOrigin: () => 8,
        updateViewport () {
            this.viewportUpdated = true
        }
    }
    Object.keys(options.methods).forEach(name => {
        vm[name] = options.methods[name].bind(vm)
    })
    vm.captureAnchor = () => null
    vm.updateViewport = () => {
        vm.viewportUpdated = true
    }
    vm.scrollToSourceTarget(target)
    assert.equal(vm.scrollTop, 389)
    assert.equal(box.scrollTop, 477, 'DOM commit waits for the render transaction')
    assert.equal(vm.sourceLocateLineNo, 1)
    queued[0]()
    assert.equal(box.scrollTop, 389)
    assert.equal(vm.viewportUpdated, true)
})

test('LogLinesView keeps one active locate highlight and lets a new locate replace the minimap source', () => {
    const options = loadComponentOptions('../LogLinesView.vue', { ...adapter, ...virtualWindow, markRaw, shallowRef, LogErrorMinimap: {} })
    const computed = options.computed.currentLocateLineNo
    assert.equal(computed.call({ sourceLocateLineNo: 'mini-20', highlightedLineNo: 'summary-30' }), 'mini-20')
    assert.equal(computed.call({ sourceLocateLineNo: '', highlightedLineNo: 'summary-30' }), 'summary-30')
    const vm = {
        $refs: { box: { scrollTop: 0, scrollHeight: 400, clientHeight: 100 } },
        locateGeneration: 0, activeLocate: { generation: 0 }, pendingLocateScrollTop: 100,
        sourceLocateLineNo: 'mini-20', pendingAnchor: null, displayLines: [{ lineNo: 'summary-30' }], useVirtual: false,
        cancelPendingWheel () {}, cancelLocateCalibration () {}, beginUserInteraction () {}, $emit () {}, findLineRow: () => null
    }
    options.methods.scrollToLine.call(vm, 'summary-30')
    assert.equal(vm.sourceLocateLineNo, '', 'a summary or search locate clears the old minimap source highlight')
    assert.equal(computed.call({ ...vm, highlightedLineNo: 'summary-30' }), 'summary-30')
})

test('LogGroupBar arrows follow actual overflow, reset when overflow disappears, and disconnect ResizeObserver', () => {
    let observerInstance
    class FakeResizeObserver {
        constructor (callback) {
            this.callback = callback
            observerInstance = this
        }
        observe (target) {
            this.target = target
        }
        disconnect () {
            this.disconnected = true
        }
    }
    const component = loadComponentOptions('../LogGroupBar.vue', {}, {
        ResizeObserver: FakeResizeObserver,
        window: {
            addEventListener () {},
            removeEventListener () {},
            getComputedStyle: () => ({ paddingLeft: '16px', paddingRight: '16px' })
        }
    })
    const track = { scrollWidth: 200, clientWidth: 200, scrollLeft: 0 }
    const root = { clientWidth: 512 }
    const vm = {
        ...component.data(),
        $refs: { root, track },
        $nextTick: callback => callback(),
        ...component.methods
    }
    Object.keys(component.methods).forEach(name => {
        vm[name] = component.methods[name].bind(vm)
    })
    component.mounted.call(vm)
    assert.equal(vm.hasOverflow, false, 'short labels do not reserve arrow space')
    assert.equal(vm.canScrollLeft, false)
    assert.equal(vm.canScrollRight, false)
    assert.equal(observerInstance.target, root)

    track.scrollWidth = 450
    vm.measure()
    assert.equal(vm.hasOverflow, false, '450px of labels fit the 480px content width without arrows')

    track.scrollWidth = 800
    track.clientWidth = 432
    vm.measure()
    assert.equal(vm.hasOverflow, true)
    assert.equal(vm.canScrollRight, true)
    track.scrollLeft = 368
    vm.updateScrollState()
    assert.equal(vm.canScrollLeft, true)
    assert.equal(vm.canScrollRight, false)

    root.clientWidth = 200
    track.scrollWidth = 170
    track.clientWidth = 120
    vm.measure()
    assert.equal(vm.hasOverflow, true, 'narrow content width excludes both 16px root paddings')
    track.scrollWidth = 160
    track.clientWidth = 160
    vm.measure()
    assert.equal(vm.hasOverflow, false)
    assert.equal(track.scrollLeft, 0)
    assert.equal(vm.canScrollLeft, false)
    assert.equal(vm.canScrollRight, false)
    component.beforeDestroy.call(vm)
    assert.equal(observerInstance.disconnected, true)
})

test('sub-thousandth minimap scales map start, middle, and end using source geometry with or without HeightIndex', () => {
    const logs = Array.from({ length: 100000 }, (_, index) => ({
        lineNo: 120001 + index,
        displayLineNo: 1 + index,
        level: 'INFO',
        text: `source-${index}`
    }))
    const trackHeight = 500
    const sourceHeight = logs.length * virtualWindow.DEFAULT_LINE_HEIGHT

    const fallbackLayout = adapter.resolveLogMinimapLayout(logs, { previewCap: 100, trackHeight })
    const fallbackCoordinates = adapter.resolveLogMinimapCoordinates(fallbackLayout, { top: 0, height: 0 }, trackHeight)
    assert.ok(fallbackCoordinates.scale < 0.001)
    assert.equal(fallbackCoordinates.scale, trackHeight / fallbackLayout.totalHeight)
    assert.equal(adapter.resolveLogMinimapLineIndex(fallbackLayout, 0, 0, fallbackCoordinates.scale), 0)
    assert.ok(Math.abs(adapter.resolveLogMinimapLineIndex(fallbackLayout, 250, 0, fallbackCoordinates.scale) - 50000) <= 1500)
    assert.equal(adapter.resolveLogMinimapLineIndex(fallbackLayout, trackHeight, 0, fallbackCoordinates.scale), 99999)

    const heightIndex = virtualWindow.createHeightIndex().rebuild(logs)
    const indexedLayout = adapter.resolveLogMinimapLayout(logs, {
        previewCap: 100,
        trackHeight,
        heightIndex,
        contentHeight: sourceHeight
    })
    const expectedIndexScale = trackHeight / sourceHeight
    assert.ok(expectedIndexScale < 0.001)
    const coordinatesAtStart = adapter.resolveLogMinimapCoordinates(indexedLayout, {}, trackHeight, {
        scrollTop: 0, clientHeight: 800, contentHeight: sourceHeight
    })
    const coordinatesAtMiddle = adapter.resolveLogMinimapCoordinates(indexedLayout, {}, trackHeight, {
        scrollTop: 400000, clientHeight: 800, contentHeight: sourceHeight
    })
    const coordinatesAtEnd = adapter.resolveLogMinimapCoordinates(indexedLayout, {}, trackHeight, {
        scrollTop: sourceHeight - 800, clientHeight: 800, contentHeight: sourceHeight
    })
    assert.equal(coordinatesAtMiddle.scale, expectedIndexScale)
    assert.equal(adapter.resolveLogMinimapLineIndex(indexedLayout, 0, 0, coordinatesAtStart.scale), 0)
    assert.equal(adapter.resolveLogMinimapLineIndex(indexedLayout, 250, 0, coordinatesAtMiddle.scale), 50000)
    assert.equal(adapter.resolveLogMinimapLineIndex(indexedLayout, trackHeight, 0, coordinatesAtEnd.scale), 99999)
})

test('minimap layout remains anchored after append, top backfill, and filtering', () => {
    const base = [1, 2, 3, 4].map(lineNo => ({ lineNo, text: `line-${lineNo}` }))
    const appended = adapter.resolveLogMinimapLayout([...base, { lineNo: 5, text: 'line-5' }])
    assert.equal(appended.previewRows.at(-1).line.lineNo, 5)
    const backfilled = adapter.resolveLogMinimapLayout([{ lineNo: 0, text: 'older' }, ...base])
    assert.equal(backfilled.previewRows[0].line.lineNo, 0)
    assert.equal(backfilled.previewRows[1].topPx, 8)
    const filtered = adapter.resolveLogMinimapLayout(base.filter(line => line.lineNo % 2 === 0))
    assert.deepEqual(filtered.previewRows.map(row => row.line.lineNo), [2, 4])
    assert.equal(adapter.resolveLogMinimapLineIndex(filtered, filtered.lineTops[1]), 1)
})

test('minimap uses shared measured row heights for preview positions, marks, viewport, and track clicks', () => {
    const logs = [
        { lineNo: 1, text: 'short' },
        { lineNo: 2, text: 'wrapped' },
        { lineNo: 3, level: 'ERROR', text: 'long row' },
        { lineNo: 4, text: 'tail' }
    ]
    const index = virtualWindow.createHeightIndex().rebuild(logs, { 1: 20, 2: 40, 3: 80, 4: 160 })
    const layout = adapter.resolveLogMinimapLayout(logs, {
        heightIndex: index,
        indexVersion: 3,
        lineOrigin: 8,
        contentHeight: 316,
        previewCap: 3,
        trackHeight: 158
    })
    assert.equal(layout.totalHeight, 300)
    assert.equal(layout.sourceHeight, 316)
    assert.deepEqual(layout.previewRows.map(row => row.index), [0, 2, 3])
    assert.deepEqual(layout.previewRows.map(row => row.topPx), [8, 68, 148])
    assert.deepEqual(layout.previewRows.map(row => row.heightPx), [20, 80, 160])
    assert.equal(layout.issueMarks[0].index, 2)
    assert.equal(layout.issueMarks[0].topPx, 78)

    const coordinates = adapter.resolveLogMinimapCoordinates(layout, null, 158, {
        scrollTop: 100,
        clientHeight: 100,
        contentHeight: 316
    })
    assert.equal(coordinates.scale, 0.5)
    assert.equal(coordinates.visibleTopPx, 50)
    assert.equal(coordinates.visibleHeightPx, 50)
    assert.equal(coordinates.viewportTopPx, 50)
    assert.equal(coordinates.viewportHeightPx, 50)
    assert.equal(adapter.resolveLogMinimapMarkTop(layout.issueMarks[0], coordinates), 39)

    const secondRowCenter = (8 + index.prefix(1) + index.heights[1] / 2) * coordinates.scale
    assert.equal(adapter.resolveLogMinimapLineIndex(layout, secondRowCenter, 0, coordinates.scale), 1)
    const tailCenter = (8 + index.prefix(3) + index.heights[3] / 2) * coordinates.scale
    assert.equal(adapter.resolveLogMinimapLineIndex(layout, tailCenter, 0, coordinates.scale), 3)
    const sampledRow = layout.previewRows[2]
    assert.deepEqual(adapter.resolveLogMinimapPreviewRowGeometry(sampledRow, coordinates), { topPx: 74, sourceHeightPx: 80, heightPx: 80 })
})

test('shared minimap coordinates remain precise below one-thousandth scale on long logs', () => {
    const logs = Array.from({ length: 100000 }, (_, index) => ({ lineNo: index + 1, text: 'line' }))
    const index = virtualWindow.createHeightIndex().rebuild(logs, {}, false)
    const layout = adapter.resolveLogMinimapLayout(logs, {
        heightIndex: index,
        lineOrigin: 8,
        contentHeight: 2000016,
        previewCap: 20,
        trackHeight: 1000
    })
    const coordinates = adapter.resolveLogMinimapCoordinates(layout, null, 1000, {
        scrollTop: 1999000,
        clientHeight: 600,
        contentHeight: 2000016
    })
    assert.ok(coordinates.scale > 0 && coordinates.scale < 0.001)
    const previewRow = layout.previewRows[10]
    const previewGeometry = adapter.resolveLogMinimapPreviewRowGeometry(previewRow, coordinates)
    assert.equal(previewGeometry.topPx, previewRow.topPx * coordinates.scale)
    assert.ok(previewGeometry.sourceHeightPx < 1)
    assert.equal(previewGeometry.heightPx, 8)
    const targetTop = (8 + index.prefix(99999)) * coordinates.scale
    assert.equal(adapter.resolveLogMinimapLineIndex(layout, targetTop + 2 * coordinates.scale, 0, coordinates.scale), 99999)
})

test('sampled minimap keeps exact source tops and clips crowded issue footprints without overlap', () => {
    const logs = Array.from({ length: 100 }, (_, index) => ({
        lineNo: index + 1,
        level: index < 30 ? 'ERROR' : 'INFO',
        text: `line-${index + 1}`
    }))
    const index = virtualWindow.createHeightIndex().rebuild(logs, {}, false)
    const layout = adapter.resolveLogMinimapLayout(logs, {
        heightIndex: index,
        lineOrigin: 8,
        contentHeight: index.total() + 16,
        previewCap: 10,
        trackHeight: 80
    })
    const coordinates = adapter.resolveLogMinimapCoordinates(layout, null, 80, {
        scrollTop: 0,
        clientHeight: 20,
        contentHeight: index.total() + 16
    })
    const geometries = layout.previewRows.map(row => adapter.resolveLogMinimapPreviewRowGeometry(row, coordinates))
    assert.ok(geometries.some(geometry => geometry.heightPx < 8))
    geometries.forEach((geometry, rowIndex) => {
        assert.equal(geometry.topPx, layout.previewRows[rowIndex].topPx * coordinates.scale)
        if (rowIndex < geometries.length - 1) {
            const gap = geometries[rowIndex + 1].topPx - geometry.topPx
            assert.ok(geometry.heightPx <= gap)
        }
    })
})

test('minimap recomputes a bounded source window from scroll metrics and retains stable source props', () => {
    const minimap = readFileSync(new URL('../LogErrorMinimap.vue', import.meta.url), 'utf8')
    const linesView = readFileSync(new URL('../LogLinesView.vue', import.meta.url), 'utf8')
    assert.doesNotMatch(minimap, /resolvePreviewSampleIndexes|_previewSampleCache/)
    assert.match(minimap, /scrollMetrics: this\.scrollMetrics/)
    assert.match(minimap, /resolveLogMinimapCoordinates\(this\.minimapLayout, this\.viewport, this\.trackHeight, this\.scrollMetrics\)/)
    assert.match(linesView, /:height-index="heightIndex"[\s\S]*:index-version="indexVersion"[\s\S]*:line-origin="minimapLineOrigin"[\s\S]*:content-height="contentHeight"[\s\S]*:scroll-metrics="minimapScrollMetrics"/)
    assert.match(linesView, /:text-metrics="minimapTextMetrics"/)
    assert.match(linesView, /this\.scrollDelta = el\.scrollTop - this\.scrollTop/)
    assert.match(linesView, /dynamicOverscan: true/)
    assert.match(linesView, /this\._resizeFrame = requestAnimationFrame\(/)
    assert.match(minimap, /class="lp-viewport"[\s\S]*:style="viewportStyle"/)
    assert.doesNotMatch(minimap, /lp-viewport-visible-range/)
    assert.match(minimap, /previewRowStyle \(row\)[\s\S]*height: `\$\{geometry\.heightPx\}px`/)
    assert.match(minimap, /\.lp-minimap-line \{[\s\S]*min-height:\s*0/)
    assert.match(minimap, /\.lp-minimap-track \{[\s\S]*overflow:\s*clip;[\s\S]*overscroll-behavior:\s*none/)
})

test('computes latest display base from reliable total', () => {
    assert.equal(adapter.latestDisplayBase(3340, 1000), 2341)
})

test('does not interpret before or after matchedTotal zero as an empty result', () => {
    assert.equal(adapter.latestDisplayBase(0, 0), 1)
    assert.equal(adapter.shouldBackfillSmallLog({
        finished: false,
        hasAfter: true,
        hasBefore: true,
        matchedTotal: 0,
        loadedCount: 1000
    }), false)
})

test('does not reload while legacy levels migration is suppressed', () => {
    assert.equal(adapter.shouldReloadOnLevelsChange({ suppressLevelsReload: true }), false)
})

test('reloads once levels control is ready for user changes', () => {
    assert.equal(adapter.shouldReloadOnLevelsChange({ suppressLevelsReload: false }), true)
})

const LOG_EXPIRED_FALLBACK = 'Build logs have exceeded the retention period and were cleaned up; they are no longer available.'

test('shouldShowStatusEmptyLine only for empty status with text', () => {
    assert.equal(adapter.shouldShowStatusEmptyLine('empty', 'not started'), true)
    assert.equal(adapter.shouldShowStatusEmptyLine('empty', ''), false)
    assert.equal(adapter.shouldShowStatusEmptyLine('clean', 'ignored'), false)
    assert.equal(adapter.shouldShowStatusEmptyLine('closed', 'ignored'), false)
    assert.equal(adapter.shouldShowStatusEmptyLine('', 'text'), false)
})

test('resolveCleanStatusFallbackKey maps clean and closed to logExpired', () => {
    assert.equal(adapter.resolveCleanStatusFallbackKey('clean'), 'logExpired')
    assert.equal(adapter.resolveCleanStatusFallbackKey('closed'), 'logExpired')
    assert.equal(adapter.resolveCleanStatusFallbackKey('empty'), 'empty')
    assert.equal(adapter.resolveCleanStatusFallbackKey(''), '')
})

test('clean response with empty message uses full logExpired fallback', () => {
    const previous = 'stale warning from latest'
    const current = adapter.resolveCleanResponseMessage('')
    assert.equal(current, '')
    assert.equal(adapter.resolveCleanStatusText(current, 'clean', LOG_EXPIRED_FALLBACK), LOG_EXPIRED_FALLBACK)
    assert.equal(adapter.resolveCleanStatusText(current, 'closed', LOG_EXPIRED_FALLBACK), LOG_EXPIRED_FALLBACK)
    assert.notEqual(adapter.resolveCleanStatusText(previous, 'clean', LOG_EXPIRED_FALLBACK), LOG_EXPIRED_FALLBACK)
})

test('clean response keeps current message when provided', () => {
    assert.equal(adapter.resolveCleanResponseMessage('Logs purged by retention'), 'Logs purged by retention')
    assert.equal(
        adapter.resolveCleanStatusText('Logs purged by retention', 'closed', LOG_EXPIRED_FALLBACK),
        'Logs purged by retention'
    )
})

test('resolveStatusMessageKey covers figma waiting and heartbeat states', () => {
    assert.equal(adapter.resolveStatusMessageKey('UNEXEC'), 'notExecuted')
    assert.equal(adapter.resolveStatusMessageKey('PAUSE'), 'pausedBeforeRun')
    assert.equal(adapter.resolveStatusMessageKey('REVIEWING'), 'reviewWaiting')
    assert.equal(adapter.resolveStatusMessageKey('HEARTBEAT_TIMEOUT'), 'heartbeatTimeout')
    assert.equal(adapter.resolveStatusMessageKey('WAITING'), 'waiting')
    assert.equal(adapter.resolveStatusMessageKey('QUEUE'), 'queueWaiting')
    assert.equal(adapter.resolveStatusMessageKey('FAILED'), '')
})

test('buildConclusion exposes statusMessageKey when no explicit message', () => {
    assert.equal(adapter.buildConclusion({ status: 'UNEXEC' }, {}).statusMessageKey, 'notExecuted')
    assert.equal(adapter.buildConclusion({ status: 'PAUSE' }, {}).statusMessageKey, 'pausedBeforeRun')
    assert.equal(adapter.buildConclusion({ status: 'REVIEWING' }, {}).statusMessageKey, 'reviewWaiting')
    assert.equal(adapter.buildConclusion({ status: 'HEARTBEAT_TIMEOUT' }, {}).statusMessageKey, 'heartbeatTimeout')
    assert.equal(adapter.buildConclusion({ status: 'FAILED', errorMsg: 'boom' }, {}).statusMessageKey, '')
})

test('error summaries match task and container ids and localize numeric and enum error types', () => {
    const errors = [
        { taskId: 'other', containerId: 'job', errorType: 1, errorCode: 'wrong' },
        { taskId: 'step', containerId: 'job', errorType: 1, errorCode: '80221232322', errorMsg: 'detail' }
    ]
    assert.deepEqual(adapter.resolveLogErrorSummary(errors, { id: 'step', containerId: 'job' }, {}), {
        errorType: 1, errorCode: '80221232322', errorMsg: 'detail'
    })
    assert.equal(adapter.resolveLogErrorSummary(errors, { id: 'missing', containerId: 'job' }, { errorType: 'USER', errorMsg: 'fallback' }).errorMsg, 'fallback')
    for (const [value, key] of [[0, 'system'], [1, 'user'], [2, 'thirdParty'], [3, 'plugin'], [4, 'container'], ['SYSTEM', 'system'], ['USER', 'user'], ['THIRD_PARTY', 'thirdParty'], ['PLUGIN', 'plugin'], ['BUILD_MACHINE', 'container']]) {
        assert.equal(adapter.resolveLogErrorTypeKey(value), key)
    }
    const summary = readFileSync(new URL('../../ErrorSummary.vue', import.meta.url), 'utf8')
    assert.match(summary, /resolveLogErrorTypeKey\(this\.error\?\.errorType\)/)
    assert.match(summary, /border-bottom:\s*1px solid #313238/)
    for (const locale of ['zh-CN', 'en-US', 'ja-JP']) {
        const messages = JSON.parse(readFileSync(new URL(`../../../../../../locale/pipeline/${locale}.json`, import.meta.url), 'utf8')).logPanel
        for (const type of ['system', 'user', 'thirdParty', 'plugin', 'container']) assert.ok(messages.errorType[type])
    }
})

test('job timeout projection requires the explicit terminated-step timeout code', () => {
    assert.equal(adapter.isJobTimeoutTerminatedStep('TERMINATE', 2199005, [], {}), true)
    assert.equal(adapter.isJobTimeoutTerminatedStep('SUCCEED', 2199005, [], {}), false)
    assert.equal(adapter.isJobTimeoutTerminatedStep('TERMINATE', 0, [
        { taskId: 'other', containerId: 'job', errorCode: 2199005 }
    ], { id: 'step', containerId: 'job' }), false)
    assert.equal(adapter.isJobTimeoutTerminatedStep('TERMINATE', 0, [
        { taskId: 'step', containerId: 'job', errorCode: 2199005 }
    ], { id: 'step', containerId: 'job' }), true)
})

test('system cancellation requires explicit build-machine termination evidence', () => {
    assert.equal(adapter.isSystemAgentTermination('TERMINATE', 'BUILD_MACHINE', 2199502, [], {}), true)
    assert.equal(adapter.isSystemAgentTermination('TERMINATE', 'USER', 2199502, [], {}), false)
    assert.equal(adapter.isSystemAgentTermination('TERMINATE', undefined, 0, [
        { taskId: 'step', containerId: 'job', errorType: 4, errorCode: 2199502 }
    ], { id: 'step', containerId: 'job' }), true)
    assert.equal(adapter.isSystemAgentTermination('TERMINATE', 'BUILD_MACHINE', 0, [
        { taskId: 'other', containerId: 'job', errorType: 'BUILD_MACHINE', errorCode: 2199502 }
    ], { id: 'step', containerId: 'job' }), false)
})

test('failure and timeout Figma states keep distinct status and summary copy', () => {
    const failed = devMock.resolveLogPanelDevMock('failed', 'development')
    assert.equal(failed.messageKey, 'logPanel.mock.failedReason')
    assert.equal(failed.summaryError.errorMsgKey, 'logPanel.mock.failedSummary')
    assert.equal(failed.nodeName, 'dev-01')
    assert.equal(failed.locateLog, true)
    assert.equal(failed.canRetry, true)
    assert.equal(failed.canSkip, true)
    const bar = readFileSync(new URL('../LogConclusionBar.vue', import.meta.url), 'utf8')
    assert.doesNotMatch(bar, /conclusion\.timeoutThreshold[\s\S]{0,80}timeoutThreshold/)
    assert.match(bar, /\.lp-status\s*\{[^}]*box-sizing:\s*border-box;\s*height:\s*37px/)
    assert.match(bar, /\.lp-status-row\s*\{[^}]*height:\s*36px;\s*min-height:\s*36px;\s*padding:\s*8px 16px;/)
    assert.match(bar, /\$t\(conclusion\.queueDetail \? 'logPanel\.runningNode' : 'logPanel\.statusNode'\)/)
    assert.match(bar, /\.lp-msg\s*\{[^}]*text-overflow:\s*ellipsis/)
})

test('search-stress dev fixture is lazy, searchable across the full log, and production-gated', () => {
    const mock = devMock.resolveLogPanelDevMock('search-stress', 'development')
    assert.equal(mock.scope, 'step')
    const logs = devMock.createLogPanelSearchStressLogs('development')
    assert.equal(logs.length, 10000)
    assert.ok(logs.every(log => Number.isInteger(log.lineNo) && Number.isFinite(log.timestamp) && ['INFO', 'WARN', 'ERROR'].includes(log.level) && typeof log.text === 'string'))

    const keywordLines = logs.filter(log => log.text.includes('SEARCH_TARGET_2026')).map(log => log.lineNo)
    assert.deepEqual(keywordLines, [5, 1250, 5000, 8750, 9998])
    const chineseTargetLines = logs.filter(log => log.text.includes('搜索压力中文命中')).map(log => log.lineNo)
    assert.deepEqual(chineseTargetLines, [7, 2501, 9990])
    assert.ok(new Set(logs.map(log => log.level)).size === 3)
    const linkSample = 'MOCK_DEVOPS_LINK <a href="https://devops.example.com">https://devops.example.com</a>'
    const longMarker = ' SEARCH_TARGET_2026 '
    const longFillerLength = 10000 - longMarker.length
    const originalLongText = `${'x'.repeat(Math.floor(longFillerLength / 2))}${longMarker}${'y'.repeat(Math.ceil(longFillerLength / 2))}`
    assert.equal(logs[4999].text.slice(0, 10000), originalLongText)
    assert.equal(logs[4999].text.length, 10000 + 1 + linkSample.length)
    assert.ok(logs[4999].text.includes('SEARCH_TARGET_2026'))
    assert.equal(logs[9999].lineNo, 10000)
    assert.ok(logs[9999].text.includes('Pipeline log line 10000'))
    assert.equal(devMock.createLogPanelSearchStressLogs('production'), null)
    assert.equal(devMock.resolveLogPanelDevMock('search-stress', 'production'), null)

    const panel = readFileSync(new URL('../StepLogPanel.vue', import.meta.url), 'utf8')
    const reloadBody = panel.match(/reload \(\) \{([\s\S]*?)\n\s{12}},\n\s{12}applyResult/)[1]
    assert.match(reloadBody, /\['search-stress', 'format-stress-100k'\]\.includes\(this\.devMockState\) && process\.env\.NODE_ENV === 'development'[\s\S]*createLogPanelFormatStressLogs\('development'\)[\s\S]*createLogPanelSearchStressLogs\('development'\)[\s\S]*this\.applyResponse\([\s\S]*return[\s\S]*getLogPanelLatest/)
    assert.match(panel, /devMockState \(state, previousState\) \{[\s\S]*this\.clearSearchForContext\(\)[\s\S]*this\.reload\(\)/)
    assert.match(panel, /scheduleAfter \(\) \{\s*clearTimeout\(this\.requestTimer\)[\s\S]*if \(this\.devMockState \|\| this\.finished && !this\.hasAfter\) return/)
})

test('search-stress mock intersects level and subTag filters without rewriting line numbers', () => {
    const logs = devMock.createLogPanelSearchStressLogs('development')
    const all = devMock.filterLogPanelDevMockLogs(logs, ['INFO', 'WARN', 'ERROR', 'DEBUG'])
    assert.equal(all.length, logs.length)
    const onlyError = devMock.filterLogPanelDevMockLogs(logs, 'ERROR')
    assert.ok(onlyError.length)
    assert.ok(onlyError.every(log => log.level === 'ERROR'))
    const onlyTest = devMock.filterLogPanelDevMockLogs(logs, [], 'test')
    assert.ok(onlyTest.length)
    assert.ok(onlyTest.every(log => log.subTag === 'test'))
    const intersection = devMock.filterLogPanelDevMockLogs(logs, ['WARN'], 'deploy')
    assert.ok(intersection.length)
    assert.ok(intersection.every(log => log.level === 'WARN' && log.subTag === 'deploy'))
    assert.deepEqual(intersection.map(log => log.lineNo), logs.filter(log => log.level === 'WARN' && log.subTag === 'deploy').map(log => log.lineNo))
    assert.deepEqual(devMock.filterLogPanelDevMockLogs(logs, ['DEBUG'], 'missing'), [])

    const toolbar = loadComponentOptions('../LogPanelToolbar.vue', adapter)
    const emitted = []
    const controls = { levels: ['DEBUG'] }
    const toolbarVm = {
        controls,
        disabled: false,
        $set: (target, key, value) => {
            target[key] = value
        },
        $emit: (...args) => emitted.push(args)
    }
    Object.keys(toolbar.methods).forEach((name) => {
        toolbarVm[name] = toolbar.methods[name].bind(toolbarVm)
    })
    toolbarVm.selectAllLevels()
    assert.deepEqual(controls.levels, ['INFO', 'WARN', 'ERROR', 'DEBUG'], 'ALL restores an empty-result DEBUG filter')
    assert.deepEqual(emitted, [['levels-change', ['INFO', 'WARN', 'ERROR', 'DEBUG']]])

    const step = readFileSync(new URL('../StepLogPanel.vue', import.meta.url), 'utf8')
    assert.match(step, /\['search-stress', 'format-stress-100k'\]\.includes\(this\.devMockState\) && process\.env\.NODE_ENV === 'development'[\s\S]*filterLogPanelDevMockLogs\([\s\S]*request\.levels[\s\S]*request\.subTag[\s\S]*matchedTotal: logs\.length[\s\S]*subTags: \['compile', 'test', 'deploy'\]/)
})

test('timeout mocks match Figma display codes, thresholds, and messages without losing job evidence', () => {
    const step = devMock.resolveLogPanelDevMock('step-timeout', 'development')
    const renderedStep = devMock.applyLogPanelDevMock({ status: 'FAILED', errorCode: '2199011', errorMsg: 'stale failure' }, step, 'step')
    assert.equal(renderedStep.status, 'EXEC_TIMEOUT')
    assert.equal(renderedStep.errorCode, '2123015')
    assert.equal(renderedStep.errorMsg, '')
    assert.equal(adapter.resolveExecTimeoutThreshold(renderedStep, 'step'), '30m')
    assert.equal(step.summaryError.errorCode, '80221232322')
    assert.match(step.summaryError.errorMsg, /Script command execution failed with exit code\(1\)/)
    assert.match(step.summaryError.errorMsg, /BK_CI_HOOK_BRANCH/)
    assert.equal(step.messageKey, 'logPanel.mock.stepTimeoutMessage')

    const job = devMock.resolveLogPanelDevMock('job-timeout', 'development')
    const renderedJob = devMock.applyLogPanelDevMock({ status: 'FAILED', errorCode: '2199011', errorMsg: 'stale failure', jobControlOption: { timeoutVar: '${{ timeout }}', timeout: 900 } }, job, 'step')
    assert.equal(renderedJob.status, 'TERMINATE')
    assert.equal(renderedJob.errorCode, '2123015')
    assert.equal(renderedJob.errorMsg, '')
    assert.equal(job.timeoutEvidenceCode, 2199005)
    assert.equal(adapter.isJobTimeoutTerminatedStep(renderedJob.status, job.timeoutEvidenceCode, [], {}), true)
    assert.equal(adapter.resolveExecTimeoutThreshold(renderedJob, 'job'), '15m')
    assert.equal(job.summaryError.errorCode, '2123015')
    assert.equal(job.messageKey, 'logPanel.mock.jobTimeoutMessage')
    for (const mock of [step, job]) {
        assert.equal(mock.elapsed, 75000)
        assert.equal(mock.nodeName, 'dev-01')
        assert.equal(mock.hideNode, undefined)
    }
})

test('status mock overlays clear stale mutually exclusive fields from real history', () => {
    const contaminated = {
        status: 'FAILED', errorType: 'BUILD_MACHINE', errorCode: '2199011', errorMsg: 'real failure text',
        summaryError: { errorCode: '2199011' }, canRetry: true, canSkip: true, cancelUserId: 'real-user',
        progress: 88, subtasks: [{ name: 'real subtask' }], nodeName: 'real-node',
        startTime: 1_700_000_000_000, startEpoch: 1_700_000_000_000, endTime: 1_700_000_060_000,
        elapsed: 60_000, timeCost: { totalCost: 60_000 }
    }
    const states = ['running', 'canceled', 'success', 'failed', 'pause', 'review', 'unexecuted', 'step-timeout', 'job-timeout', 'system-canceled', 'parent-canceled', 'running-subtasks']
    for (const state of states) {
        const mock = devMock.resolveLogPanelDevMock(state, 'development')
        const projected = devMock.applyLogPanelDevMock(contaminated, mock, mock.scope === 'both' ? 'step' : mock.scope)
        assert.equal(projected.errorCode, mock.errorCode || '', `${state} error code`)
        assert.equal(projected.errorMsg, mock.errorMsg || '', `${state} error message`)
        assert.equal(projected.errorType, mock.errorType ?? null, `${state} error type`)
        assert.equal(projected.summaryError, mock.summaryError ?? null, `${state} summary`)
        assert.equal(projected.canRetry, Boolean(mock.canRetry), `${state} retry flag`)
        assert.equal(projected.canSkip, Boolean(mock.canSkip), `${state} skip flag`)
        assert.equal(projected.cancelUserId, mock.cancelUserId || '', `${state} cancellation user`)
        assert.equal(projected.progress, mock.progress ?? null, `${state} progress`)
        assert.deepEqual(projected.subtasks, mock.subtasks ?? null, `${state} subtasks`)
        assert.equal(projected.nodeName, mock.nodeName ?? null, `${state} node name`)
    }
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    assert.match(plugin, /if \(!this\.isHistoricalExecuteCount && this\.devLogMock\) return this\.devLogMock\.summaryError[\s\S]*?: null/)
    assert.match(plugin, /if \(this\.isHistoricalExecuteCount\) return \[\][\s\S]*return this\.devLogMock[\s\S]*this\.progressSubtasksSnapshot/)
})

test('all fourteen Figma frames have selectable fixtures and renderable conclusion models', () => {
    const states = [
        ['1296:32686', 'running', 'RUNNING', 'running', 'step'],
        ['1296:32788', 'canceled', 'CANCELED', 'canceled', 'step'],
        ['1296:33518', 'success', 'SUCCEED', 'success', 'step'],
        ['1296:33121', 'failed', 'FAILED', 'danger', 'step'],
        ['1296:33661', 'pause', 'PAUSE', 'pause', 'step'],
        ['1296:33835', 'review', 'REVIEWING', 'waiting', 'step'],
        ['1325:14270', 'queue', 'QUEUE', 'queue', 'job'],
        ['1325:14360', 'queue-expanded', 'LOOP_WAITING', 'idle', 'job'],
        ['1296:33976', 'unexecuted', 'UNEXEC', 'idle', 'step'],
        ['1296:33302', 'step-timeout', 'EXEC_TIMEOUT', 'danger', 'step'],
        ['1296:33410', 'job-timeout', 'TERMINATE', 'danger', 'job'],
        ['1296:32962', 'system-canceled', 'TERMINATE', 'canceled', 'step'],
        ['1296:33046', 'parent-canceled', 'CANCELED', 'canceled', 'step'],
        ['1296:32687', 'running-subtasks', 'RUNNING', 'running', 'step']
    ]
    assert.equal(states.length, 14)
    states.forEach(([nodeId, query, status, tone, scope]) => {
        const mock = devMock.resolveLogPanelDevMock(query, 'development')
        const source = { status: 'SUCCEED', name: 'Original' }
        const rendered = devMock.applyLogPanelDevMock(source, mock, scope)
        assert.equal(mock.state, query, `${nodeId} selectable via logMockState=${query}`)
        assert.equal(rendered.status, status, `${nodeId} status`)
        const modelStatus = query === 'job-timeout' && Number(mock.timeoutEvidenceCode) === 2199005 ? 'EXEC_TIMEOUT' : rendered.status
        assert.equal(adapter.buildConclusion({ ...rendered, status: modelStatus }, {}).tone, tone, `${nodeId} status tone`)
    })
    assert.equal(devMock.resolveLogPanelDevMock('queue', 'development').queueDetail.ahead.length, 5)
    assert.equal(devMock.resolveLogPanelDevMock('queue-expanded', 'development').queueExpanded, true)
    for (const state of ['queue', 'queue-expanded']) {
        const queue = devMock.applyLogPanelDevMock({
            status: 'SUCCEED', startTime: 1_700_000_000_000, endTime: null,
            timeCost: { totalCost: 113_140_000 }
        }, devMock.resolveLogPanelDevMock(state, 'development'), 'job')
        assert.equal(queue.startTime, null)
        assert.equal(queue.timeCost.totalCost, state === 'queue-expanded' ? 300000 : null)
        assert.equal(adapter.resolveConclusionElapsed({
            status: queue.status,
            startTime: queue.startTime,
            endTime: queue.endTime,
            timeCost: queue.timeCost
        }), state === 'queue-expanded' ? '5m 0s' : '')
    }
    const failedMock = devMock.resolveLogPanelDevMock('failed', 'development')
    assert.equal(failedMock.errorCode, '2123015')
    assert.equal(failedMock.summaryError.errorCode, '80221232322')
    const stepTimeout = devMock.applyLogPanelDevMock({ additionalOptions: { timeoutVar: 'REAL_STEP_TIMEOUT' } }, devMock.resolveLogPanelDevMock('step-timeout', 'development'), 'step')
    const jobTimeout = devMock.applyLogPanelDevMock({ jobControlOption: { timeoutVar: 'REAL_JOB_TIMEOUT' } }, devMock.resolveLogPanelDevMock('job-timeout', 'development'), 'job')
    assert.equal(stepTimeout.additionalOptions.timeoutVar, null)
    assert.equal(adapter.resolveExecTimeoutThreshold(stepTimeout, 'step'), '30m')
    assert.equal(jobTimeout.jobControlOption.timeoutVar, null)
    assert.equal(adapter.resolveExecTimeoutThreshold(jobTimeout, 'job'), '15m')
    assert.equal(devMock.resolveLogPanelDevMock('parent-canceled', 'development').parentCanceled.buildNum, 130)
    assert.equal(devMock.resolveLogPanelDevMock('system-canceled', 'development').labelKey, 'logPanel.status.systemCanceled')
    assert.equal(adapter.buildConclusion({ status: 'TERMINATE' }, {}).label, 'TERMINATE')
    assert.equal(devMock.resolveLogPanelDevMock('running-subtasks', 'development').subtasks.length, 3)
    assert.equal(devMock.resolveLogPanelDevMock('running-subtasks', 'development').progress, 20)
    assert.equal(devMock.resolveLogPanelDevMock('running-subtasks', 'development').subtasks[1].progress, '处理中…')
    const pause = devMock.resolveLogPanelDevMock('pause', 'development')
    const review = devMock.resolveLogPanelDevMock('review', 'development')
    assert.equal(pause.labelKey, 'logPanel.mock.pauseLabel')
    assert.equal(pause.messageKey, 'logPanel.mock.pauseMessage')
    assert.equal(pause.elapsed, undefined)
    assert.equal(pause.nodeName, undefined)
    assert.equal(review.labelKey, 'logPanel.mock.reviewLabel')
    assert.equal(review.messageKey, 'logPanel.mock.reviewMessage')
    assert.equal(review.elapsed, 75000)
    assert.equal(review.nodeName, 'dev-01')
    for (const state of ['canceled', 'success', 'system-canceled', 'parent-canceled']) {
        const mock = devMock.resolveLogPanelDevMock(state, 'development')
        assert.equal(mock.scope, 'step')
        assert.equal(mock.elapsed, 75000)
        assert.equal(mock.nodeName, 'dev-01')
    }
    const queueMock = devMock.resolveLogPanelDevMock('queue-expanded', 'development')
    assert.equal(queueMock.name, '前端构建')
    assert.equal(queueMock.status, 'LOOP_WAITING')
    assert.equal(queueMock.elapsed, 300000)
    assert.equal(queueMock.nodeName, 'bkci-docker-c-1001')
    assert.equal(queueMock.queueDetail.aheadCount, 5)
    assert.equal(queueMock.queueDetail.running.length, 1)
    assert.equal(queueMock.queueDetail.ahead.length, 5)
    assert.equal(queueMock.queueDetail.running[0].buildNum, 41)
    assert.equal(queueMock.queueDetail.running[0].userId, 'fayewang')
    assert.equal(queueMock.queueDetail.ahead[0].buildNum, 54)
    const queueSource = readFileSync(new URL('../JobQueueDetail.vue', import.meta.url), 'utf8')
    assert.match(queueSource, /v-for="section in queueSections"/)
    assert.match(queueSource, /section\.titleKey/)
    assert.match(queueSource, /section\.durationKey/)
    assert.match(queueSource, /class="queue-build-tag">#\{\{ item\.buildNum \}\}/)
    assert.match(queueSource, /<strong v-if="item\.detailVisible">\{\{ item\.title \}\}<\/strong>/)
    assert.match(queueSource, /v-if="item\.showStatus"\s+class="queue-item-status"/)
    assert.match(queueSource, /statusLabel \(status\)/)
    assert.match(queueSource, /v-for="section in queueSections"/)
    assert.match(queueSource, /<bk-popover[\s\S]*class="queue-more"/)
    assert.match(queueSource, /\.queue-lanes \{ display: grid; grid-template-columns: minmax\(0, 1fr\);/)
    assert.match(queueSource, /\.queue-lanes\.has-running \{ grid-template-columns: minmax\(0, 1fr\) minmax\(0, 3fr\); \}/)
    assert.match(queueSource, /\.queue-section \{ display: flex; flex-direction: column; gap: 8px;/)
    assert.match(queueSource, /\.queue-items \{ display: flex; gap: 8px;/)
    assert.match(queueSource, /\.queue-item \{ display: flex; flex: 1 1 0;[^}]*min-width: 0;/)
    assert.doesNotMatch(queueSource, /overflow-x: auto/)
    assert.doesNotMatch(queueSource, /min-width: 280px/)
    assert.match(queueSource, /\.queue-section-title[^}]*gap: 6px;[^}]*font: 12px\/normal "Microsoft YaHei", sans-serif/)
    assert.match(queueSource, /\.queue-section-title i[^}]*width: 6px; height: 6px/)
    assert.match(queueSource, /\.queue-item[^}]*height: 58px;[^}]*padding: 10px 12px/)
    assert.match(queueSource, /\.running-section \.queue-item \{ background: #2f3a2f; \}/)
    assert.match(queueSource, /\.queued-section \.queue-item \{ background: #36343a; \}/)
    assert.match(queueSource, /\.queue-build-tag[^}]*height: 18px; padding: 0; color: #83828c; font: 12px\/18px/)
    assert.match(queueSource, /logPanel\.queue\.waitedMinutes/)
    assert.match(queueSource, /logPanel\.queue\.aheadTruncated/)
    assert.match(queueSource, /\.queue-items\s*\{\s*display:\s*flex/)
    assert.doesNotMatch(queueSource, /queue\.position', \[index \+ 1\]/)
    for (const state of ['running', 'running-subtasks']) {
        const mock = devMock.resolveLogPanelDevMock(state, 'development')
        const rendered = devMock.applyLogPanelDevMock({}, mock, 'step')
        assert.equal(rendered.nodeName, 'dev-01')
        assert.equal(adapter.buildConclusion({}, rendered).node, 'dev-01')
    }
})

test('Job queue detail follows visible and server timing, and respects detail visibility', () => {
    const now = 2_000_000
    const data = {
        status: 0,
        data: {
            visible: true,
            serverTime: now,
            concurrencyGroup: 'release-lock',
            queueTime: now - 300_000,
            aheadCount: 25,
            total: 27,
            running: [{ buildId: '1', pipelineId: 'p1', pipelineName: 'Pipeline', buildNum: 41, status: 'RUNNING', userId: 'alice', buildMsg: 'release', startTime: now - 1_860_000, detailVisible: true }],
            ahead: [
                { buildId: '2', pipelineId: 'p2', pipelineName: 'PRIVATE', buildNum: 54, status: 'QUEUE_CACHE', userId: 'secret-user', buildMsg: 'PRIVATE MESSAGE', queueTime: now - 120_000, detailVisible: false }
            ]
        }
    }
    const resolved = adapter.resolveJobQueueDetail(data.data, { projectId: 'proj', webUrlPrefix: 'https://example.test', formatDuration: value => `${value}ms` })
    assert.equal(resolved.group, 'release-lock')
    assert.equal(resolved.position, 26)
    assert.equal(resolved.total, 27)
    assert.equal(resolved.aheadCount, 25)
    assert.equal(resolved.waitedMinutes, 5)
    assert.equal(resolved.running[0].elapsedMinutes, 31)
    assert.equal(resolved.running[0].status, 'RUNNING')
    assert.equal(resolved.running[0].showStatus, false)
    assert.match(resolved.running[0].linkUrl, /\/pipeline\/proj\/p1\/detail\/1$/)
    assert.equal(resolved.ahead[0].title, '')
    assert.equal(resolved.ahead[0].owner, '')
    assert.equal(resolved.ahead[0].linkUrl, '')
    assert.equal(resolved.ahead[0].elapsedMinutes, 2)
    assert.equal(resolved.ahead[0].status, 'QUEUE_CACHE')
    assert.equal(resolved.ahead[0].showStatus, false)
    assert.doesNotMatch(JSON.stringify(resolved.ahead[0]), /PRIVATE|secret-user/)
    const noRunning = adapter.resolveJobQueueDetail({ status: 0, data: { visible: true, serverTime: now, queueTime: now - 1000, aheadCount: 0, running: [], ahead: [] } }, { formatDuration: value => `${value}ms` })
    assert.equal(noRunning.running.length, 0)
    assert.equal(noRunning.aheadCount, 0)
    assert.equal(noRunning.total, null)
    const explicitTotal = adapter.resolveJobQueueDetail({ visible: true, serverTime: now, queueTime: now, aheadCount: 2, total: 8, running: [], ahead: [] })
    assert.equal(explicitTotal.total, 8)
    const invalidTotal = adapter.resolveJobQueueDetail({ visible: true, serverTime: now, queueTime: now, aheadCount: 2, total: 2, running: [], ahead: [] })
    assert.equal(invalidTotal.total, null)
    for (const runningCount of [1, 10]) {
        const items = Array.from({ length: runningCount }, (_, index) => ({
            buildId: String(index), pipelineId: 'pipe', pipelineName: 'Build', buildNum: index,
            status: 'RUNNING', userId: 'runner', startTime: now - 1000, detailVisible: true
        }))
        const many = adapter.resolveJobQueueDetail({ status: 0, data: { visible: true, serverTime: now, queueTime: now - 1000, aheadCount: 20, running: [...items, ...items], ahead: Array.from({ length: 25 }, () => ({ detailVisible: false, buildNum: 1, status: 'QUEUE', queueTime: now - 1000 })) } }, { formatDuration: value => `${value}ms` })
        assert.equal(many.running.length, runningCount * 2)
        assert.equal(many.ahead.length, 25)
        assert.equal(many.aheadCount, 20)
    }
    assert.equal(adapter.resolveJobQueueDetail({ status: 0, data: { visible: false } }), null)
    const unreliable = adapter.resolveJobQueueDetail({ status: 0, data: { visible: true, serverTime: 1000, queueTime: 2000, aheadCount: -1, running: [], ahead: [] } }, { formatDuration: value => `${value}ms` })
    assert.equal(unreliable.position, null)
    assert.equal(unreliable.total, null)
    const missingRunning = adapter.resolveJobQueueDetail({ visible: true, serverTime: now, queueTime: now, aheadCount: 2, ahead: [] })
    assert.equal(missingRunning.position, 3)
    assert.equal(missingRunning.total, null)
    const invalidTimes = adapter.resolveJobQueueDetail({ status: 0, data: { visible: true, serverTime: 'bad-time', queueTime: 'bad-time', aheadCount: 0, running: [{ detailVisible: true, buildNum: 1, status: 'RUNNING', startTime: 'bad-time' }], ahead: [{ detailVisible: true, buildNum: 2, status: 'QUEUE', queueTime: 'bad-time' }] } }, { formatDuration: value => `${value}ms` })
    assert.equal(invalidTimes.waitedMinutes, '')
    assert.equal(invalidTimes.running[0].elapsedMinutes, '')
    assert.equal(invalidTimes.ahead[0].elapsedMinutes, '')
    const underMinute = adapter.resolveJobQueueDetail({ visible: true, serverTime: now, queueTime: now - 1000, aheadCount: 0, running: [{ detailVisible: true, buildNum: 1, startTime: now - 1000 }], ahead: [] })
    assert.equal(underMinute.waitedMinutes, 1)
    assert.equal(underMinute.running[0].elapsedMinutes, 1)
    const otherStatuses = adapter.resolveJobQueueDetail({ visible: true, serverTime: now, aheadCount: 1, running: [{ buildNum: 8, status: 'FAILED', detailVisible: false }], ahead: [{ buildNum: 9, status: 'LOOP_WAITING', detailVisible: false }] })
    assert.equal(otherStatuses.running[0].status, 'FAILED')
    assert.equal(otherStatuses.running[0].showStatus, true)
    assert.equal(otherStatuses.ahead[0].status, 'LOOP_WAITING')
    assert.equal(otherStatuses.ahead[0].showStatus, true)
    assert.equal(adapter.resolveQueueCardStatusLabel('FAILED', key => key === 'details.statusMap.FAILED' ? '失败' : key), '失败')
    assert.equal(adapter.resolveQueueCardStatusLabel('CUSTOM_STATE', key => key), 'CUSTOM_STATE')
})

test('queue mock states are development-only and cover empty, hidden and overflow details', () => {
    const empty = devMock.resolveLogPanelDevMock('queue-no-running', 'development')
    const hidden = devMock.resolveLogPanelDevMock('queue-redacted', 'development')
    const overflow = devMock.resolveLogPanelDevMock('queue-long-ahead', 'development')
    assert.equal(empty.queueDetail.visible, true)
    assert.deepEqual(empty.queueDetail.running, [])
    assert.equal(hidden.queueDetail.running[0].detailVisible, false)
    assert.equal(hidden.queueDetail.running[0].pipelineName, 'SENSITIVE PIPELINE')
    assert.equal(hidden.queueDetail.running[0].userId, 'private-user')
    const hiddenModel = adapter.resolveJobQueueDetail(hidden.queueDetail, { projectId: 'proj', webUrlPrefix: 'https://example.test' })
    assert.equal(hiddenModel.running[0].title, '')
    assert.equal(hiddenModel.running[0].owner, '')
    assert.equal(hiddenModel.running[0].linkUrl, '')
    assert.equal(overflow.queueDetail.aheadCount, 20)
    assert.equal(overflow.queueDetail.ahead.length, 20)
    assert.equal(overflow.queueDetail.ahead[0].pipelineName.length > 40, true)
    assert.equal(overflow.queueDetail.ahead[1].buildMsg, '')
    assert.equal(devMock.resolveLogPanelDevMock('queue-expanded', 'development').queueDetail.total, 7)
    for (const state of ['queue-no-running', 'queue-redacted', 'queue-long-ahead']) {
        assert.equal(devMock.resolveLogPanelDevMock(state, 'production'), null)
    }
})

test('Job queue source owns serialized polling and invalidates stale contexts', () => {
    const source = readFileSync(new URL('../../job.vue', import.meta.url), 'utf8')
    const store = readFileSync(new URL('../../../../store/modules/pipelines/build.js', import.meta.url), 'utf8')
    const conclusion = readFileSync(new URL('../LogConclusionBar.vue', import.meta.url), 'utf8')
    assert.match(store, /requestConcurrencyQueue[\s\S]*concurrencyQueue[\s\S]*return response\.data/)
    assert.equal(adapter.isVisibleConcurrencyQueueResponse({ visible: true, concurrencyGroup: 'lock' }), true)
    assert.equal(adapter.isVisibleConcurrencyQueueResponse({ status: 0, data: { visible: true } }), false)
    for (const status of ['QUEUE', 'QUEUE_CACHE', 'LOOP_WAITING']) assert.equal(adapter.isConcurrencyQueueCandidate(status), true)
    assert.equal(adapter.isConcurrencyQueueCandidate('RUNNING'), false)
    assert.match(source, /isConcurrencyQueueCandidate\(this\.execDetail\.status\)/)
    assert.match(source, /'execDetail\.status'[\s\S]*this\.refreshQueueForStatus\(previousStatus\)/)
    assert.doesNotMatch(source, /isConcurrencyQueueCandidate\(this\.currentJob\.status\)/)
    assert.match(source, /}, 3000\)/)
    assert.match(source, /requestId === this\.queueRequestId && contextKey === this\.queueContextKey/)
    assert.match(source, /isVisibleConcurrencyQueueResponse\(response\)/)
    assert.match(source, /queueResponse\?\.visible === true/)
    assert.match(source, /if \(status === 403\)[\s\S]*this\.queueResponse = null[\s\S]*this\.handleLogPermission\(\)/)
    assert.equal(adapter.shouldClearQueueAfterError(true), true)
    assert.equal(adapter.shouldClearQueueAfterError(false), false)
    assert.match(source, /shouldClearQueueAfterError\(finalRequest\)[\s\S]*this\.queueResponse = null[\s\S]*this\.queueReceivedAt = 0/)
    assert.match(source, /beforeDestroy \(\)[\s\S]*this\.invalidateQueueRequests\(\)/)
    assert.match(conclusion, /logPanel\.queue\.positionLabel/)
    assert.match(conclusion, /logPanel\.queue\.positionValue/)
    assert.match(conclusion, /logPanel\.queue\.positionTotalValue/)
    assert.match(conclusion, /is-queue-detail/)
    assert.match(conclusion, /\.lp-status\.is-queue-detail\s*\{\s*background:\s*#242a36;\s*color:\s*#83828c;/)
    assert.match(conclusion, /\.lp-status\.is-queue-detail \.lp-status-label\s*\{\s*color:\s*#83828c;/)
    assert.match(conclusion, /padding:\s*8px 16px;/)
    assert.match(conclusion, /\$t\(conclusion\.queueDetail \? 'logPanel\.runningNode' : 'logPanel\.statusNode'\)/)
    assert.match(conclusion, /conclusion\.queueDetail \? ' ' : ': '/)
    assert.match(conclusion, /displayElapsed \(\) \{\s*const elapsed = String\(this\.conclusion\.elapsed \|\| ''\)\s*return this\.conclusion\.queueDetail \? elapsed\.replace/)
    assert.match(conclusion, /elapsed\.replace\(\/ 0s\$\//)
    assert.match(source, /jobTimeoutEvidence[\s\S]*systemCanceledEvidence\s*\? this\.\$t\('logPanel\.status\.systemCanceledDetail'\)\s*: cancelDisplay/)
    assert.match(source, /statusMessageKey:\s*queueDetail\s*\|\|\s*conditionSkipped\s*\|\|\s*conditionSkipCount\s*\? ''\s*:\s*model\.statusMessageKey/)
    assert.match(source, /queueDetail\s*\? job\.status === 'LOOP_WAITING' \? job\.status : this\.\$t\('logPanel\.queue\.mutexQueue'\)/)
    assert.match(conclusion, /\.lp-queue-position-label \{ color: #83828c; \}/)
    assert.match(conclusion, /\.lp-queue-position-value \{ color: #f0aa50; \}/)
    assert.match(conclusion, /class="devops-icon icon-angle-right lp-expand-icon"[\s\S]*:class="\{ 'is-open': queueExpanded \}"/)
    assert.match(conclusion, /\.lp-queue-meta \.is-open \{ transform: rotate\(90deg\); \}/)
})

test('directory elapsed refresh is scoped to live build statuses', () => {
    for (const status of ['RUNNING', 'PREPARE_ENV', 'QUEUE', 'DEPENDENT_WAITING', 'LOOP_WAITING', 'CALL_WAITING']) {
        assert.equal(adapter.isLiveElapsedStatus(status), true)
    }
    assert.equal(adapter.isLiveElapsedStatus('SUCCEED'), false)
    assert.equal(adapter.isLiveElapsedStatus('FAILED'), false)
})

test('dev-only fixtures include loading, long-title, and delayed-tab scenarios', () => {
    const loading = devMock.resolveLogPanelDevMock('loading', 'development')
    const longTitle = devMock.resolveLogPanelDevMock('long-title', 'development')
    const tabsDelay = devMock.resolveLogPanelDevMock('tabs-delay', 'development')
    assert.equal(loading.forceLoading, true)
    assert.equal(devMock.isLogPanelDevMockLoading(loading, 'development'), true)
    assert.ok(devMock.applyLogPanelDevMock({ name: 'Original' }, longTitle, 'step').name.length > 40)
    assert.equal(tabsDelay.delayTabsMs, 1000)
})

test('production never resolves, applies, or displays log mock data', () => {
    const source = { status: 'RUNNING', name: 'Real' }
    assert.equal(devMock.resolveLogPanelDevMock('queue', 'production'), null)
    assert.equal(devMock.resolveLogPanelDevMock('toString', 'development'), null)
    assert.equal(devMock.applyLogPanelDevMock(source, devMock.resolveLogPanelDevMock('queue', 'production'), 'job'), source)
    assert.equal(devMock.isLogPanelDevMockLoading({ forceLoading: true }, 'production'), false)
})

test('conclusion subtask expansion is limited to the running development fixture', () => {
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    const fixture = devMock.resolveLogPanelDevMock('running-subtasks', 'development')
    assert.equal(fixture.state, 'running-subtasks')
    assert.equal(devMock.resolveLogPanelDevMock('running-subtasks', 'production'), null)
    assert.match(plugin, /:progress="displayProgressPercent"/)
    assert.match(plugin, /:has-subtasks="canExpandProgressSubtasks && !!displayProgressSubtasks\.length"/)
    assert.match(plugin, /:progress-expanded="canExpandProgressSubtasks && progressExpanded"/)
    assert.match(plugin, /:subtasks="canExpandProgressSubtasks \? displayProgressSubtasks : \[\]"/)
    assert.match(plugin, /canExpandProgressSubtasks \(\) \{[\s\S]*?this\.devLogMock[\s\S]*?running-subtasks[\s\S]*?this\.displayProgressSubtasks\.length/)
    assert.match(plugin, /'currentElement\.status' \(status\)[\s\S]*?if \(!shouldFetchTaskProgress\(status\)\) \{\s*this\.resetProgressState\(\)/)
    assert.match(plugin, /'currentElement\.id': function \(\)[\s\S]*?this\.resetProgressState\(\)/)
})

test('leaving running-subtasks mock reloads progress for the same real RUNNING task', () => {
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    const previousMock = devMock.resolveLogPanelDevMock('running-subtasks', 'development')
    const nextMock = devMock.resolveLogPanelDevMock(undefined, 'development')
    const events = []
    const state = { requestId: 41, progressPercent: 20, progressDetailData: { fixture: true }, progressSubtasks: [{ name: 'fixture' }] }
    const fetched = adapter.applyProgressMockTransition(previousMock.state, nextMock?.state || null, 'RUNNING', {
        stopPoll: () => events.push('stop-poll'),
        invalidate: () => {
            state.requestId += 1
            events.push('invalidate')
        },
        reset: () => {
            state.progressPercent = null
            state.progressDetailData = null
            state.progressSubtasks = []
            events.push('reset')
        },
        fetch: () => events.push('fetch')
    })

    assert.equal(fetched, true)
    assert.deepEqual(events, ['stop-poll', 'invalidate', 'reset', 'fetch'])
    assert.equal(state.requestId, 42)
    assert.equal(state.progressPercent, null)
    assert.equal(state.progressDetailData, null)
    assert.deepEqual(state.progressSubtasks, [])
    assert.equal(adapter.shouldApplyProgressResponse({
        requestId: 41,
        currentRequestId: state.requestId,
        contextKey: 'same-task|1|RUNNING',
        currentContextKey: 'same-task|1|RUNNING'
    }), false)
    assert.match(plugin, /'\$route\.query\.logMockState' \(value, previousValue\)[\s\S]*?applyProgressMockTransition\(previousMock\?\.state \|\| null, nextMock\?\.state \|\| null, this\.currentElement\.status/)
})

test('header and queue components preserve centered slots and dev-only mock wiring', () => {
    const container = readFileSync(new URL('../../detailContainer.vue', import.meta.url), 'utf8')
    const job = readFileSync(new URL('../../job.vue', import.meta.url), 'utf8')
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    const toolbar = readFileSync(new URL('../LogPanelToolbar.vue', import.meta.url), 'utf8')
    const groupBar = readFileSync(new URL('../LogGroupBar.vue', import.meta.url), 'utf8')
    const paramsView = readFileSync(new URL('../LogParamsView.vue', import.meta.url), 'utf8')
    const queue = readFileSync(new URL('../JobQueueDetail.vue', import.meta.url), 'utf8')
    const conclusion = readFileSync(new URL('../LogConclusionBar.vue', import.meta.url), 'utf8')
    const errorSummary = readFileSync(new URL('../../ErrorSummary.vue', import.meta.url), 'utf8')
    const status = readFileSync(new URL('../../status.vue', import.meta.url), 'utf8')
    const panel = readFileSync(new URL('../StepLogPanel.vue', import.meta.url), 'utf8')
    const logLines = readFileSync(new URL('../LogLinesView.vue', import.meta.url), 'utf8')
    const jobDirectory = readFileSync(new URL('../JobLogPanel.vue', import.meta.url), 'utf8')
    const queueModel = readFileSync(new URL('../logPanelAdapter.js', import.meta.url), 'utf8')
    assert.match(container, /grid-template-columns:\s*minmax\(0, 1fr\) max-content minmax\(0, 1fr\)/)
    assert.match(container, /<div class="log-head-tab"><slot name="tab"><\/slot><\/div>/)
    assert.match(container, /scrollWidth > tools\.clientWidth/)
    assert.match(container, /syncToolScrollability \(\)/)
    assert.match(container, /new window\.ResizeObserver/)
    assert.match(container, /resizeObserver\.disconnect\(\)/)
    assert.match(container, /window\.addEventListener\('resize'/)
    assert.match(container, /window\.removeEventListener\('resize'/)
    assert.match(container, /\.log-head-tools\s*\{[\s\S]*grid-column:\s*3;[\s\S]*grid-row:\s*1;[\s\S]*justify-self:\s*stretch;[\s\S]*width:\s*100%;[\s\S]*max-width:\s*100%;[\s\S]*display:\s*flex;[\s\S]*justify-content:\s*flex-start;[\s\S]*overflow-x:\s*auto;[\s\S]*overflow-y:\s*hidden;[\s\S]*scrollbar-width:\s*thin;[\s\S]*::v-deep \.log-panel-toolbar\s*\{\s*flex:\s*0 0 auto;\s*margin-left:\s*auto/)
    assert.doesNotMatch(container, /clientWidth\s*<=\s*(?:1000|1100)|is-compact|isCompactHeader|grid-template-rows:\s*26px 26px|grid-column:\s*1 \/ -1/)
    assert.match(container, /v-if="hasToolSlot"[\s\S]*:tabindex="isScrollableTools \? 0 : undefined"/)
    assert.match(container, /:aria-label="isScrollableTools \? \$t\('logPanel\.scrollToolbar'\) : undefined"/)
    assert.doesNotMatch(container, /class="log-head-tools"\s+tabindex="0"/)
    assert.match(container, /\.log-head\s*\{[\s\S]*font:\s*12px\/20px "Microsoft YaHei", sans-serif/)
    assert.match(container, /\.log-title-text\s*\{[\s\S]*color:\s*#fff;[\s\S]*font-size:\s*12px;[\s\S]*line-height:\s*20px/)
    assert.match(container, /\.position-code\s*\{[\s\S]*font-size:\s*12px;[\s\S]*line-height:\s*20px/)
    assert.match(container, /\.head-tab\s*\{[\s\S]*span\s*\{[\s\S]*font-size:\s*12px;[\s\S]*line-height:\s*20px/)
    assert.match(container, /\.head-tab\s*\{[\s\S]*height:\s*26px;[\s\S]*line-height:\s*20px/)
    assert.match(job, /if \(process\.env\.NODE_ENV !== 'development'\) return null[\s\S]*resolveLogPanelDevMock\(this\.\$route\.query\.logMockState\)/)
    assert.match(job, /:dev-mock-loading="!!devLogMock\?\.forceLoading"/)
    assert.match(job, /:dev-mock-state="devLogMock\?\.state \|\| ''"/)
    assert.match(job, /queueExpanded:\s*Boolean\(this\.devLogMock\?\.queueExpanded\)/)
    assert.match(job, /queueDetailModel \(\) \{\s*return resolveJobQueueDetail\(this\.devLogMock\?\.queueDetail \|\| this\.queueResponse/)
    assert.match(job, /:title="headerTitle"/)
    assert.match(job, /headerTitle \(\) \{\s*const title = \(this\.displayJob\.name[\s\S]*?return !this\.isHistoricalExecuteCount && this\.queueDetailModel \? `\$\{title\} · \$\{this\.\$t\('logPanel\.queue\.mutexQueue'\)\}` : title/)
    assert.match(job, /devLogMock\?\.labelKey[\s\S]*this\.\$t\(this\.devLogMock\.labelKey\)/)
    assert.match(plugin, /if \(process\.env\.NODE_ENV !== 'development'\) return null[\s\S]*resolveLogPanelDevMock\(this\.\$route\.query\.logMockState\)/)
    assert.match(plugin, /:dev-mock-loading="!!devLogMock\?\.forceLoading"/)
    assert.match(plugin, /:dev-mock-state="devLogMock\?\.state \|\| ''"/)
    assert.match(panel, /devMockState:\s*\{\s*type:\s*String,\s*default:\s*''\s*\}/)
    assert.match(panel, /devMockState \(state, previousState\) \{\s*if \(state === previousState\) return\s*this\.clearSearchForContext\(\)[\s\S]*this\.reload\(\)/)
    assert.match(panel, /mounted \(\) \{\s*if \(!this\.devMockLoading\) this\.reload\(\)/)
    assert.match(panel, /loadAfter \(\) \{\s*this\.requestTimer = null\s*if \(this\.devMockState \|\| this\.loadingAfter/)
    assert.match(panel, /scheduleAfter \(\) \{\s*clearTimeout\(this\.requestTimer\)[\s\S]*if \(this\.devMockState \|\| this\.finished && !this\.hasAfter\) return/)
    assert.match(panel, /invalidate \(\) \{[\s\S]*clearTimeout\(this\.requestTimer\)[\s\S]*this\.requestTimer = null/)
    assert.match(plugin, /showHeaderTabs \(\)[\s\S]*devMockTabsVisible/)
    assert.match(plugin, /this\.devMockTabsTimer = setTimeout/)
    assert.match(plugin, /displayProgressSubtasks \(\)[\s\S]*this\.devLogMock[\s\S]*this\.progressSubtasksSnapshot/)
    assert.match(plugin, /devLogMock\?\.labelKey[\s\S]*this\.\$t\(this\.devLogMock\.labelKey\)/)
    assert.match(toolbar, /width:\s*148px/)
    assert.match(toolbar, /\.toolbar-action \{ display: inline-flex; width: 24px; height: 24px; align-items: center; justify-content: center;/)
    assert.match(toolbar, /\.level-summary \{ display: block; width: 44px; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; \}/)
    assert.match(toolbar, /\.toolbar-level-icon \{ position: absolute; top: 50%; right: 8px; width: 12px; height: 12px; transform: translateY\(-50%\); pointer-events: none;/)
    assert.match(toolbar, /\.log-panel-toolbar\s*\{[\s\S]*font:\s*12px\/20px "Microsoft YaHei", sans-serif/)
    assert.match(toolbar, /\.search-box input\s*\{[\s\S]*height:\s*26px;[\s\S]*font:\s*12px\/20px "Microsoft YaHei", sans-serif/)
    assert.match(toolbar, /button\s*\{[\s\S]*font:\s*inherit/)
    assert.match(toolbar, /import angleDownIcon from .*svg-sprites\/devops\/angle-down\.svg/)
    assert.match(toolbar, /:style="levelArrowStyle"/)
    assert.match(toolbar, /class="devops-icon icon-search toolbar-search-icon"/)
    assert.equal((toolbar.match(/<svg\s+class="match-nav-icon"\s+viewBox="0 0 32 32"/g) || []).length, 2)
    assert.doesNotMatch(toolbar, /icon-angle-right match-nav-icon|rotate\(180deg\)|bk-log-icon/)
    assert.match(toolbar, /\.match-nav \.match-nav-icon \{ flex: 0 0 12px; width: 12px; height: 12px; \}/)
    assert.doesNotMatch(toolbar, /bk-log-icon|log-icon-angle-(left|right)/)
    assert.match(toolbar, /<svg\s+class="toolbar-action-icon time-icon"\s+viewBox="0 0 16 16"/)
    assert.match(toolbar, /<svg\s+class="wrap-icon"\s+viewBox="0 0 16 16"/)
    assert.match(toolbar, /@click="toggleWrap"/)
    assert.match(toolbar, /class="toolbar-action is-wrap-toggle"[\s\S]*'is-active': controls\.wrap[\s\S]*class="wrap-icon"/)
    assert.match(toolbar, /\.toolbar-action\.is-active,[^{]*\{[^}]*color:\s*#3a84ff;[^}]*background:\s*rgba\(58, 132, 255, \.16\)/)
    assert.match(toolbar, /\.toolbar-action:not\(:disabled\):hover\s*\{\s*color:\s*#fff/)
    assert.match(toolbar, /translate\(1 1\) scale\(0\.013671875\)/)
    assert.match(toolbar, /M102\.4 0H0v1024h102\.4z/)
    assert.match(toolbar, /M204\.8 716\.8L409\.6 512v409\.6z/)
    assert.match(toolbar, /class="toolbar-action-icon download-icon"[\s\S]*d="M3 10V13H13V10H14V13\.5C14 13\.775 13\.775 14 13\.5 14H2\.5C2\.225 14 2 13\.775 2 13\.5V10H3ZM8\.5 2V9\.075L10\.125 7\.475L10\.825 8\.175L8\.7 10\.3L8 11L7\.3 10\.3L5\.175 8\.175L5\.875 7\.475L7\.5 9\.075V2H8\.5Z"/)
    assert.equal((toolbar.match(/@click="\$emit\('download'\)"/g) || []).length, 1)
    const downloadButton = toolbar.match(/<button(?=[^>]*class="toolbar-action")(?=[^>]*aria-label="\$t\('logPanel\.downloadLog'\)")[^>]*>[\s\S]*?<\/button>/)?.[0] || ''
    assert.ok(downloadButton)
    assert.doesNotMatch(downloadButton, /aria-pressed/)
    assert.match(toolbar, /:aria-pressed="String\(controls\.showTime\)"[\s\S]*@click="controls\.showTime = !controls\.showTime"/)
    assert.match(toolbar, /\.toolbar-action svg \{ flex: 0 0 16px; \}/)
    assert.match(jobDirectory, /\.job-log-empty\s*\{[^}]*color:\s*#83828c;[^}]*background:\s*#2c2d34;/)
    assert.match(logLines, /@supports not selector\(::-webkit-scrollbar\)[\s\S]*scrollbar-width:\s*thin;[\s\S]*scrollbar-color:\s*rgba\(240, 241, 245, \.22\) transparent/)
    assert.match(logLines, /@supports selector\(::-webkit-scrollbar\)[\s\S]*scrollbar-width:\s*auto;[\s\S]*scrollbar-color:\s*auto;[\s\S]*::-webkit-scrollbar-track,[\s\S]*::-webkit-scrollbar-corner[\s\S]*background:\s*transparent;[\s\S]*::-webkit-scrollbar-thumb:hover\s*\{\s*background:\s*rgba\(240, 241, 245, \.36\)/)
    assert.match(groupBar, /class="devops-icon icon-angle-left"/)
    assert.match(groupBar, /class="devops-icon icon-angle-right"/)
    assert.doesNotMatch(groupBar, /import Logo|components:\s*\{\s*Logo\s*\}/)
    assert.match(paramsView, /class="devops-icon icon-clock pm-state-icon"/)
    assert.match(paramsView, /class="devops-icon icon-angle-down pm-caret"/)
    assert.match(paramsView, /<logo\s+name="copy"\s+:size="16"/)
    assert.match(paramsView, /right-icon="bk-icon icon-search"/)
    assert.match(paramsView, /class="pm-search"[\s\S]*clearable[\s\S]*right-icon="bk-icon icon-search"/)
    assert.doesNotMatch(paramsView, /pm-search-icon|name="search"|input-right-icon/)
    assert.match(panel, /class="log-panel-loading"[\s\S]*role="status"[\s\S]*:aria-label="\$t\('loading'\)"[\s\S]*<span[\s\S]*class="loading-spinner"\s+aria-hidden="true"/)
    assert.match(panel, /@media \(prefers-reduced-motion: reduce\)\s*\{\s*\.loading-spinner\s*\{\s*animation:\s*none/)
    assert.match(status, /name="circle-2-1"[\s\S]*class="executing running"/)
    assert.match(status, /name: 'hourglass',[\s\S]*class: 'queue'/)
    assert.match(status, /name: 'abort',[\s\S]*class: 'canceled'/)
    assert.match(status, /name: this\.isHook \? 'build-hooks' : 'close-circle-shape'/)
    assert.match(status, /name: this\.isHook \? 'build-hooks' : 'check-circle-shape'/)
    assert.match(status, /svg \{ width: 16px; height: 16px;/)
    assert.match(status, /@media \(prefers-reduced-motion: reduce\)[\s\S]*\.executing \{ animation: none/)
    assert.equal((conclusion.match(/class="devops-icon icon-angle-right lp-expand-icon"/g) || []).length, 2)
    assert.match(conclusion, /row\.icon === 'clock'[\s\S]*class="devops-icon icon-clock subtask-status-icon"/)
    assert.match(conclusion, /v-else-if="row\.status"[\s\S]*:name="row\.icon"[\s\S]*:size="16"/)
    assert.match(conclusion, /@media \(prefers-reduced-motion: reduce\)[\s\S]*\.subtask-status-icon\.is-running \{ animation: none/)
    assert.match(jobDirectory, /\.job-log-directory button\s*\{[\s\S]*font:\s*12px\/20px "Microsoft YaHei", sans-serif/)
    assert.match(jobDirectory, /\.job-log-title\s*\{[\s\S]*font:\s*12px\/20px "Microsoft YaHei", sans-serif/)
    assert.match(jobDirectory, /\.job-log-directory small\s*\{[\s\S]*font:\s*12px\/20px "Microsoft YaHei", sans-serif/)
    assert.match(jobDirectory, /\.job-log-empty\s*\{[^}]*font:\s*12px\/20px "Microsoft YaHei", sans-serif/)
    assert.match(queue, /background:\s*#2b2722/)
    assert.match(queue, /\.job-queue-detail\s*\{[^}]*font:\s*12px\/20px "Microsoft YaHei", sans-serif/)
    assert.match(conclusion, /\.lp-status\s*\{[^}]*font:\s*12px\/20px "Microsoft YaHei", sans-serif/)
    assert.match(errorSummary, /<logo\s+name="angle-down"\s+:size="12"/)
    assert.match(errorSummary, /import Logo from '\@\/components\/Logo'/)
    assert.match(errorSummary, /overflow:\s*auto;[\s\S]*max-height:\s*360px/)
    assert.match(errorSummary, /v-bk-xss-html="errorMsg"/)
    assert.doesNotMatch(errorSummary, /-webkit-line-clamp:\s*2|max-height:\s*40px/)
    for (const sprite of ['file', 'copy', 'check-circle', 'close-circle', 'circle-2-1']) {
        assert.equal(existsSync(new URL(`../../../../../../svg-sprites/pipeline/${sprite}.svg`, import.meta.url)), true, `${sprite} pipeline sprite exists`)
    }
    assert.match(status, /name="circle-2-1"/)
    assert.match(errorSummary, /\.error-log-summary\s*\{[\s\S]*font:\s*12px\/20px "Microsoft YaHei", sans-serif/)
    assert.match(panel, /\.log-panel-empty, \.log-panel-loading\s*\{[^}]*background:\s*#2c2d34/)
    assert.match(panel, /\.log-panel-empty, \.log-panel-loading\s*\{[^}]*font:\s*12px\/20px "Microsoft YaHei", sans-serif/)
    assert.match(panel, /\.log-panel-error\s*\{[^}]*font:\s*12px\/20px "Microsoft YaHei", sans-serif/)
    assert.match(logLines, /\.lp-lines\s*\{[\s\S]*font-family:\s*Menlo, monospace;[\s\S]*font-size:\s*12px;[\s\S]*line-height:\s*20px;[\s\S]*color:\s*#c4c6cc;/)
    assert.match(logLines, /fontFamily: 'Menlo, monospace', fontSize: '12px', lineHeight: '20px'/)
    assert.match(logLines, /\.lp-time\s*\{[^}]*color:\s*#959da5;/)
    assert.match(logLines, /\.lp-line\.is-warn \.lp-text \{ color: #ff9c01; \}/)
    assert.match(logLines, /\.lp-line\.is-error \.lp-text \{ color: #ff6666; \}/)
    assert.match(panel, /\.loading-spinner\s*\{\s*width:\s*32px;\s*height:\s*32px/)
    assert.match(jobDirectory, /elapsedNow: Date\.now\(\),[\s\S]*elapsedTimer: null/)
    assert.match(jobDirectory, /setInterval\(\(\) => \{[\s\S]*this\.elapsedNow = Date\.now\(\)/)
    assert.match(jobDirectory, /beforeDestroy \(\)[\s\S]*clearInterval\(this\.elapsedTimer\)/)
    assert.match(queueModel, /data\?\.visible !== true/)
})

test('background after polling does not render a visible loading indicator', () => {
    const stepPanel = readFileSync(new URL('../StepLogPanel.vue', import.meta.url), 'utf8')
    const linesView = readFileSync(new URL('../LogLinesView.vue', import.meta.url), 'utf8')
    assert.match(stepPanel, /this\.loadingAfter\s*=\s*true/)
    assert.match(stepPanel, /if \(this\.devMockState \|\| this\.loadingAfter[\s\S]*this\.finished && !this\.hasAfter/)
    assert.doesNotMatch(stepPanel, /:loading-after="loadingAfter"/)
    assert.match(stepPanel, /:loading-before="loadingBefore"/)
    assert.match(linesView, /v-if="loadingBefore"[\s\S]*\{\{ \$t\('loading'\) \}\}/)
    assert.doesNotMatch(linesView, /loadingAfter|loading-after/)
})

test('historical conclusion bars remain visible while actions stay gated', () => {
    const job = readFileSync(new URL('../../job.vue', import.meta.url), 'utf8')
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    for (const [source] of [[job], [plugin]]) {
        const condition = source.match(/<log-conclusion-bar\s+v-if="([^"]+)"/)
        assert.ok(condition, 'LogConclusionBar has a visibility condition')
        assert.equal(condition[1], "currentTab === 'log' && showConclusion")
        assert.doesNotMatch(condition[1], /currentJob\.status|currentElement\.status/)
        assert.doesNotMatch(condition[1], /isLatestBuild/)
    }
    assert.match(job, /const gated = this\.actionsEnabled && !this\.\$route\.query\?\.archiveFlag && this\.conclusionVisible && !this\.isHistoricalExecuteCount/)
    assert.match(plugin, /const gated = this\.actionsEnabled && !this\.\$route\.query\?\.archiveFlag && this\.conclusionVisible/)
    assert.match(job, /conclusionVisible \(\) \{\s*return this\.selectedExecuteCount === null \|\| Number\(this\.selectedExecuteCount\) === Number\(this\.latestExecuteCount\)/)
    assert.match(plugin, /conclusionVisible \(\) \{\s*return this\.selectedExecuteCount === null \|\| Number\(this\.selectedExecuteCount\) === Number\(this\.currentElement\.executeCount\)/)
    assert.match(plugin, /currentNodeError \(\) \{[\s\S]*?return resolveNodeLogError/)
    assert.match(plugin, /activeErorr \(\) \{\s*return this\.currentNodeError/)
    assert.match(plugin, /const gated = this\.actionsEnabled && !this\.\$route\.query\?\.archiveFlag && this\.conclusionVisible/)
    assert.match(plugin, /handleStepAction \(skip\) \{\s*if \(this\.devLogMock\) \{\s*this\.showMockActionNotice\(\)\s*return/)
    assert.match(plugin, /handleMockablePanelAction \(action\) \{\s*if \(this\.devLogMock\) \{\s*this\.showMockActionNotice\(\)\s*return/)
    assert.match(plugin, /showMockActionNotice \(\) \{\s*if \(this\.\$bkMessage\) this\.\$bkMessage\(/)
    assert.match(plugin, /const mockActionsVisible = Boolean\(this\.devLogMock\) && this\.conclusionVisible/)
    assert.match(plugin, /canRetry: !this\.isHistoricalExecuteCount && \(this\.devLogMock \? mockActionsVisible && !!this\.devLogMock\.canRetry : gated && !!currentElement\.canRetry\)/)
    assert.match(plugin, /canSkip: !this\.isHistoricalExecuteCount && \(this\.devLogMock \? mockActionsVisible && !!this\.devLogMock\.canSkip : gated && !!currentElement\.canSkip\)/)
    assert.match(job, /retryableStep \(\)[\s\S]*find\(step => step\?\.canRetry \|\| step\?\.canSkip\)/)
    assert.match(job, /:pending-action="pendingAction"/)
    assert.match(job, /@retry="handleStepAction\(false\)"/)
    assert.match(job, /@skip="handleStepAction\(true\)"/)
    assert.match(job, /canRetry: gated && !!this\.retryableStep\?\.canRetry/)
    assert.match(job, /canSkip: gated && !!this\.retryableStep\?\.canSkip/)
    assert.match(plugin, /continueConfig: !this\.isHistoricalExecuteCount && \(this\.devLogMock \? mockActionsVisible : gated\) && status === 'PAUSE'/)
    assert.match(plugin, /review: !this\.isHistoricalExecuteCount && \(this\.devLogMock \? mockActionsVisible : gated\) && status === 'REVIEWING'/)
    assert.match(plugin, /const cancelDisplay = [\s\S]*resolveCancelDisplay\(/)
    assert.match(plugin, /parentBuildLabel: this\.devLogMock\?\.parentCanceled \|\| cancelConclusion\?\.type === 'parent' \? 'logPanel\.status\.parentBuild' : ''/)
    assert.match(plugin, /paramsElement \(\) \{\s*if \(this\.devLogMock\) \{\s*return \{ \.\.\.this\.currentElement, status: this\.devLogMock\.status \|\| this\.currentElement\.status \}/)
})

test('execute-count selector is not duplicated in the log toolbar', () => {
    const job = readFileSync(new URL('../../job.vue', import.meta.url), 'utf8')
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    const toolbar = readFileSync(new URL('../LogPanelToolbar.vue', import.meta.url), 'utf8')
    const jobToolbar = job.match(/<log-panel-toolbar[\s\S]*?\/>/)?.[0]
    const pluginToolbar = plugin.match(/<log-panel-toolbar[\s\S]*?\/>/)?.[0]
    assert.ok(jobToolbar)
    assert.ok(pluginToolbar)
    assert.doesNotMatch(jobToolbar, /show-execute-count|latest-count|active-count|context-key=.*execDetail\.id/)
    assert.doesNotMatch(pluginToolbar, /show-execute-count|latest-count|active-count|context-key=.*execDetail\.id/)
    assert.doesNotMatch(toolbar, /LogExecuteCountSelect|showExecuteCount|latestCount|activeCount|contextKey|executeCountDisabled/)
    assert.doesNotMatch(job, /showToolbarExecuteCount/)
})

test('retry and skip actions expose pending feedback and suppress duplicate emits', () => {
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    const bar = readFileSync(new URL('../LogConclusionBar.vue', import.meta.url), 'utf8')
    const parent = readFileSync(new URL('../../../../views/subpages/ExecDetail.vue', import.meta.url), 'utf8')
    const actionHandler = plugin.match(/handleStepAction \(skip\) \{([\s\S]*?)\n            \},\n            handleMockablePanelAction/)
    assert.ok(actionHandler)
    assert.match(plugin, /pendingAction: ''/)
    assert.match(plugin, /:pending-action="pendingAction"/)
    assert.ok(actionHandler[1].indexOf('if (this.pendingAction) return') < actionHandler[1].indexOf("this.$emit('continue'"))
    assert.match(actionHandler[1], /const action = skip \? 'skip' : 'retry'/)
    assert.match(actionHandler[1], /this\.pendingAction = action/)
    assert.match(actionHandler[1], /\$emit\('continue',[\s\S]*\(\) => \{\s*this\.pendingAction = ''/)
    assert.match(bar, /pendingAction: \{ type: String, default: '' \}/)
    assert.match(bar, /v-if="conclusion\.canRetry"[\s\S]*:loading="pendingAction === 'retry'"[\s\S]*:disabled="Boolean\(pendingAction\)"/)
    assert.match(bar, /v-if="conclusion\.canSkip"[\s\S]*:loading="pendingAction === 'skip'"[\s\S]*:disabled="Boolean\(pendingAction\)"/)
    assert.match(parent, /finally \{\s*done\?\.\(\)\s*\}/)
    assert.match(parent, /<job[\s\S]*@continue="handlePluginContinue"[\s\S]*:actions-enabled="isExecuteDetailPanel"/)
})

test('retry and skip refresh the execution detail when no new execute count is returned', () => {
    const pipeline = readFileSync(new URL('../../../ExecPipeline.vue', import.meta.url), 'utf8')
    const retryHandler = pipeline.match(/async retryPipeline \(isStageRetry\) \{([\s\S]*?)\n            \},\n            locate \(/)
    assert.ok(retryHandler)
    assert.match(retryHandler[1], /if \(res\.id\) \{[\s\S]*await this\.requestPipelineExecDetail\(this\.routerParams\)/)
})

test('controlled progress detail delegates loading, errors, and retry to the parent owner', () => {
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    const panel = readFileSync(new URL('../../../ProgressDetailPanel.vue', import.meta.url), 'utf8')
    assert.match(plugin, /progressDetailData: null,[\s\S]*progressLoading: false,[\s\S]*progressError: false/)
    assert.match(plugin, /controlled: true,[\s\S]*externalLoading: this\.progressLoading,[\s\S]*externalError: this\.progressError/)
    assert.match(plugin, /@retry="key === 'progress' && fetchProgress\(\)"/)
    assert.match(plugin, /if \(!isPoll\) \{\s*this\.progressLoading = true\s*this\.progressError = false/)
    assert.match(plugin, /this\.progressError = true/)
    assert.match(plugin, /if \(!this\.isProgressContextCurrent\(currentRequestId, currentContextKey\)\) return\s*this\.progressError = false\s*this\.progressDetailData = data \|\| null/)
    assert.match(plugin, /if \(this\.devLogMock\) \{\s*this\.stopProgressPoll\(\)\s*return/)
    assert.match(panel, /controlled \? externalLoading : loading/)
    assert.match(panel, /controlled && externalError[\s\S]*progressDetail\.loadFailed[\s\S]*\$emit\('retry'\)/)
    assert.match(panel, /if \(props\.controlled\) \{\s*progressData\.value = props\.detailData[\s\S]*return/)
    assert.match(panel, /!props\.controlled && shouldRefresh\.value/)
    assert.match(panel, /controlled: \{\s*type: Boolean/)
})

test('dev mock parameter states use the projected status instead of real failed params', () => {
    for (const state of ['PAUSE', 'QUEUE', 'WAITING', 'PREPARE_ENV', 'DEPENDENT_WAITING', 'UNEXEC', 'SKIP', 'DISABLED']) {
        const mock = { scope: 'step', status: state }
        const projected = devMock.applyLogPanelDevMock({ status: 'FAILED', data: { input: { stale: true } } }, mock, 'step')
        assert.equal(adapter.buildParamsModel(projected).notExecuted, true, state)
    }
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    assert.match(plugin, /paramsElement \(\) \{\s*if \(this\.devLogMock\) \{\s*return \{ \.\.\.this\.currentElement, status: this\.devLogMock\.status \|\| this\.currentElement\.status \}/)
})

test('log status fallback uses the project circle icon without changing known mappings', () => {
    const status = readFileSync(new URL('../../status.vue', import.meta.url), 'utf8')
    assert.match(status, /<logo\s+v-else\s+name="circle"\s+:size="16"\s+class="empty-status-icon pending"\s+aria-hidden="true"/)
    for (const knownStatus of ['WAITING', 'REVIEW_ABORT', 'REVIEWING', 'FAILED', 'SUCCEED', 'PAUSE', 'SKIP', 'CANCELED', 'TERMINATE']) {
        assert.match(status, new RegExp(`case '${knownStatus}'`))
    }
    assert.match(status, /case 'QUEUE':[\s\S]*name: 'hourglass'/)
    assert.match(status, /case 'FAILED':[\s\S]*name: this\.isHook \? 'build-hooks' : 'close-circle-shape'/)
    assert.match(status, /case 'SUCCEED':[\s\S]*name: this\.isHook \? 'build-hooks' : 'check-circle-shape'/)
    assert.match(status, /case 'SKIP':[\s\S]*name: 'build-canceled',[\s\S]*class: 'pending'/)
    assert.match(status, /case 'CANCELED':[\s\S]*name: 'abort'/)
})

test('scrollable-toolbar accessible name is present in all pipeline locales', () => {
    for (const locale of ['zh-CN', 'en-US', 'ja-JP']) {
        const source = readFileSync(new URL(`../../../../../../locale/pipeline/${locale}.json`, import.meta.url), 'utf8')
        const messages = JSON.parse(source)
        assert.ok(messages.logPanel.scrollToolbar)
        assert.ok(messages.loading)
    }
})

test('resolveConclusionDisplayMessage prefers message then statusMessageKey', () => {
    const translate = key => ({
        'logPanel.status.notExecuted': 'Not executed yet; no logs.',
        'logPanel.status.pausedBeforeRun': 'Paused before execution; no logs.'
    }[key] || key)
    assert.equal(adapter.resolveConclusionDisplayMessage({
        message: 'server error',
        statusMessageKey: 'notExecuted'
    }, translate), 'server error')
    assert.equal(adapter.resolveConclusionDisplayMessage({
        statusMessageKey: 'notExecuted'
    }, translate), 'Not executed yet; no logs.')
    assert.equal(adapter.resolveConclusionDisplayMessage({
        statusMessageKey: 'pausedBeforeRun'
    }, translate), 'Paused before execution; no logs.')
    assert.equal(adapter.resolveConclusionDisplayMessage({}, translate), '')
})

test('continues auto backfill only after before succeeds with new logs', () => {
    assert.equal(adapter.shouldContinueAutoBackfillAfterBefore({ succeeded: true, addedCount: 100 }), true)
    assert.equal(adapter.shouldContinueAutoBackfillAfterBefore({ succeeded: true, addedCount: 0 }), false)
    assert.equal(adapter.shouldContinueAutoBackfillAfterBefore({ succeeded: false, addedCount: 100 }), false)
})

test('builds a failed conclusion from reliable model fields', () => {
    const model = adapter.buildConclusion({
        status: 'FAILED',
        elapsed: 65000,
        errorCode: '2123015',
        errorMsg: 'quality gate failed'
    }, {
        name: 'Linux Job',
        thirdPartyAgentId: 'dev-01'
    })
    assert.equal(model.tone, 'danger')
    assert.equal(model.elapsed, '1m 5s')
    assert.equal(model.node, 'dev-01')
    assert.equal(model.errorCode, '2123015')
})

test('uses timestamp elapsed only with endTime or live status', () => {
    assert.equal(adapter.buildConclusion({
        status: 'SUCCEED',
        startEpoch: 1_700_000_000_000,
        endTime: 1_700_000_065_000
    }, {}).elapsed, '1m 5s')
    assert.equal(adapter.buildConclusion({
        status: 'SUCCEED',
        startEpoch: 1_700_000_000_000
    }, {}).elapsed, '')
})

test('masks secret parameter values', () => {
    const sections = adapter.buildParamsModel({
        data: {
            input: {
                username: 'alice',
                password: 'secret'
            }
        }
    })
    assert.equal(sections[0].rows.find(row => row.key === 'password').value, '******')
})

test('buildParamsModel marks running output pending without exposing type names', () => {
    const sections = adapter.buildParamsModel({
        status: 'RUNNING',
        data: { output: { artifactPath: 'artifact' } }
    })
    const output = sections.find(section => section.id === 'output')
    assert.equal(output.pending, true)
    assert.deepEqual(output.rows, [])
})

test('buildParamsModel keeps type-only output keys with empty values after completion', () => {
    const sections = adapter.buildParamsModel({
        status: 'SUCCEED',
        data: {
            output: {
                BK_CI_GIT_REPO_URL: 'string',
                artifactPath: 'artifact',
                message: 'string',
                reportPath: 'report'
            }
        }
    })
    const output = sections.find(section => section.id === 'output')
    assert.deepEqual(output.rows.map(row => [row.key, row.value]), [
        ['BK_CI_GIT_REPO_URL', ''],
        ['artifactPath', ''],
        ['message', ''],
        ['reportPath', '']
    ])
    assert.equal(output.pending, false)
})

test('buildParamsModel keeps only real values from mixed output rows', () => {
    const sections = adapter.buildParamsModel({
        status: 'RUNNING',
        data: {
            output: {
                artifactPath: 'artifact',
                result: 'done',
                reportPath: 'report',
                count: 42
            }
        }
    })
    const output = sections.find(section => section.id === 'output')
    assert.deepEqual(output.rows.map(row => [row.key, row.value]), [
        ['result', 'done'],
        ['count', '42']
    ])
    assert.equal(output.pending, false)
})

test('buildParamsModel keeps completed mixed output keys and blanks type names', () => {
    const sections = adapter.buildParamsModel({
        status: 'SUCCEED',
        data: {
            output: {
                artifactPath: 'artifact',
                result: 'done',
                count: 42
            }
        }
    })
    const output = sections.find(section => section.id === 'output')
    assert.deepEqual(output.rows.map(row => [row.key, row.value]), [
        ['artifactPath', ''],
        ['result', 'done'],
        ['count', '42']
    ])
})

test('buildParamsModel filters type-only output before secret masking', () => {
    const sections = adapter.buildParamsModel({
        status: 'SUCCEED',
        data: {
            output: {
                apiToken: 'string',
                _artifactCredential: {
                    value: 'artifact',
                    expr: '${{ secrets.ARTIFACT }}'
                },
                resultToken: 'real-result'
            }
        }
    })
    const output = sections.find(section => section.id === 'output')
    assert.deepEqual(output.rows.map(row => ({
        key: row.key,
        value: row.value,
        expr: row.expr
    })), [{
        key: 'apiToken',
        value: '',
        expr: undefined
    }, {
        key: '_artifactCredential',
        value: '',
        expr: undefined
    }, {
        key: 'resultToken',
        value: '******',
        expr: undefined
    }])
    assert.equal(output.pending, false)
})

test('buildParamsModel keeps completed empty and not-executed states distinct', () => {
    const finished = adapter.buildParamsModel({ status: 'SUCCEED', data: {} })
    const notExecuted = adapter.buildParamsModel({ status: 'UNEXEC', data: {} })
    assert.equal(finished.notExecuted, false)
    assert.equal(finished.find(section => section.id === 'output').pending, false)
    assert.equal(notExecuted.notExecuted, true)
})

test('buildParamsModel keeps pause and queue on the three-section model', () => {
    const paused = adapter.buildParamsModel({
        status: 'PAUSE',
        data: { input: { repo: { value: 'git@example.com/app.git' } } }
    })
    const queued = adapter.buildParamsModel({ status: 'QUEUE', data: { input: {} } })
    assert.equal(paused.notExecuted, true)
    assert.equal(queued.notExecuted, true)
    assert.equal(paused.find(section => section.id === 'input').rows[0].value, 'git@example.com/app.git')
})

test('buildParamsModel marks all pre-execution statuses as not executed', () => {
    for (const status of ['UNEXEC', 'SKIP', 'DISABLED', 'PAUSE', 'QUEUE', 'WAITING', 'PREPARE_ENV', 'DEPENDENT_WAITING']) {
        assert.equal(adapter.buildParamsModel({ status }).notExecuted, true, status)
    }
})

test('parseTaskProgressPercent reads progressDetail ratio and clamps to 0..100', () => {
    assert.equal(adapter.parseTaskProgressPercent({
        progressDetail: { progress: { value: 0.456 } }
    }), 45.6)
    assert.equal(adapter.parseTaskProgressPercent({
        progressDetail: { progress: { value: 0 } }
    }), 0)
    assert.equal(adapter.parseTaskProgressPercent({
        taskProgressRete: 1.2
    }), 100)
    assert.equal(adapter.parseTaskProgressPercent({
        taskProgressRete: 0
    }), null)
    assert.equal(adapter.parseTaskProgressPercent({
        progressDetail: { progress: { value: -0.1 } },
        taskProgressRete: 0.25
    }), 0)
    assert.equal(adapter.parseTaskProgressPercent(null), null)
})

test('resolveDisplayProgressPercent only exposes running progress', () => {
    assert.equal(adapter.resolveDisplayProgressPercent('RUNNING', 12.5), 12.5)
    assert.equal(adapter.resolveDisplayProgressPercent('RUNNING', 0), 0)
    assert.equal(adapter.resolveDisplayProgressPercent('RUNNING', null), null)
    assert.equal(adapter.resolveDisplayProgressPercent('SUCCEED', 50), null)
    assert.equal(adapter.resolveDisplayProgressPercent('PAUSE', 10), null)
})

test('shouldUseDirectJobDownload for setup job without tag', () => {
    assert.equal(adapter.shouldUseDirectJobDownload({ jobId: 'job-1' }), true)
    assert.equal(adapter.shouldUseDirectJobDownload({ tag: 'step-1', jobId: 'job-1' }), false)
    assert.equal(adapter.shouldUseDirectJobDownload({ downloadJobId: 'job-1' }), true)
})

test('resolveDirectJobDownloadUrl appends executeCount safely', () => {
    assert.equal(adapter.resolveDirectJobDownloadUrl({
        downloadLink: '/download?jobId=1',
        executeCount: 2
    }), '/download?jobId=1&executeCount=2')
    assert.equal(adapter.resolveDirectJobDownloadUrl({
        downloadLink: '',
        executeCount: 2
    }), '')
    assert.equal(adapter.resolveDirectJobDownloadUrl({
        downloadLink: '/download?executeCount=3',
        executeCount: 2
    }), '/download?executeCount=3')
})

test('shouldRetryBeforeLoad and schedule threshold guard before retries', () => {
    assert.equal(adapter.shouldRetryBeforeLoad({ status: 999, beforeRetries: 0 }), true)
    assert.equal(adapter.shouldRetryBeforeLoad({ status: 999, beforeRetries: 3 }), false)
    assert.equal(adapter.shouldRetryBeforeLoad({ status: 0, beforeRetries: 0 }), false)
    assert.equal(adapter.shouldScheduleBeforeRetry({ scrollTop: 40 }), true)
    assert.equal(adapter.shouldScheduleBeforeRetry({ scrollTop: 41 }), false)
})

test('parseTaskProgressSubtasks normalizes status and subtask source path', () => {
    const subtasks = adapter.parseTaskProgressSubtasks({
        progressDetail: {
            subtasks: {
                items: [
                    { name: 'A', status: 'SUCCESS', progress: 1 },
                    { name: 'B', status: 'FAILED' },
                    { name: 'C', status: 'running', progress: 0.5 }
                ]
            }
        }
    })
    assert.equal(subtasks.length, 3)
    assert.equal(subtasks[0].status, 'succeeded')
    assert.equal(subtasks[0].progress, '100%')
    assert.equal(subtasks[1].status, 'failed')
    assert.equal(subtasks[2].status, 'running')
    assert.equal(subtasks[2].progress, '50%')
    assert.equal(adapter.subtaskStatusIcon('SUCCESS'), 'check-circle')
    assert.equal(adapter.subtaskStatusIcon('FAILED'), 'close-circle')
    assert.equal(adapter.subtaskStatusIcon('running'), 'circle-2-1')
    assert.equal(adapter.subtaskStatusIcon('pending'), 'clock')
})

test('parseTaskProgressSubtasks formats completed same-day timeline in milliseconds', () => {
    const start = new Date(2026, 8, 20, 10, 0, 0)
    const subtasks = adapter.parseTaskProgressSubtasks({
        progressDetail: {
            subtasks: {
                items: [{ name: 'build', status: 'succeeded', progress: 1 }]
            },
            timeline: {
                items: [{ name: 'build', startTime: start.toISOString(), duration: 65_000 }]
            }
        }
    })

    assert.equal(subtasks[0].elapsed, '1m 5s')
    assert.equal(subtasks[0].timeRange, '10:00:00 ~ 10:01:05')
    assert.equal(subtasks[0].live, false)
    assert.equal(subtasks[0].idle, false)
})

test('parseTaskProgressSubtasks includes dates for cross-day timeline', () => {
    const start = new Date(2026, 8, 20, 23, 59, 30)
    const subtasks = adapter.parseTaskProgressSubtasks({
        progressDetail: {
            subtasks: {
                items: [{ name: 'publish', status: 'succeeded', progress: 1 }]
            },
            timeline: {
                items: [{ name: 'publish', startTime: start.toISOString(), duration: 90_000 }]
            }
        }
    })

    assert.equal(subtasks[0].elapsed, '1m 30s')
    assert.equal(subtasks[0].timeRange, '09-20 23:59:30 ~ 09-21 00:01:00')
})

test('parseTaskProgressSubtasks formats running timeline without a dangling separator', () => {
    const start = new Date(2026, 8, 20, 10, 0, 0)
    const now = start.getTime() + 65_000
    const subtasks = adapter.parseTaskProgressSubtasks({
        progressDetail: {
            subtasks: {
                items: [{ name: 'test', status: 'running', progress: 0.5 }]
            },
            timeline: {
                items: [{ name: 'test', startTime: start.toISOString(), duration: null }]
            }
        }
    }, { now })

    assert.equal(subtasks[0].elapsed, '1m 5s')
    assert.equal(subtasks[0].timeRange, '10:00:00')
    assert.equal(subtasks[0].timeRange.includes('~'), false)
    assert.equal(subtasks[0].live, true)
    assert.equal(subtasks[0].idle, false)
})

test('parseTaskProgressSubtasks does not invent live timing for completed rows with null duration', () => {
    const start = new Date(2026, 8, 20, 10, 0, 0)
    const subtasks = adapter.parseTaskProgressSubtasks({
        progressDetail: {
            subtasks: {
                items: [{ name: 'finished', status: 'succeeded', progress: 1 }]
            },
            timeline: {
                items: [{ name: 'finished', startTime: start.toISOString(), duration: null }]
            }
        }
    }, { now: start.getTime() + 65_000 })

    assert.equal(subtasks[0].elapsed, '')
    assert.equal(subtasks[0].timeRange, '')
    assert.equal(subtasks[0].live, false)
})

test('parseTaskProgressSubtasks renders pending tasks as idle without time range', () => {
    const subtasks = adapter.parseTaskProgressSubtasks({
        progressDetail: {
            subtasks: {
                items: [{ name: 'deploy', status: 'pending', progress: 0 }]
            },
            timeline: { items: [] }
        }
    })

    assert.equal(subtasks[0].elapsed, '')
    assert.equal(subtasks[0].timeRange, '')
    assert.equal(subtasks[0].live, false)
    assert.equal(subtasks[0].idle, true)
})

test('parseTaskProgressSubtasks matches duplicate names by occurrence and preserves unmatched rows', () => {
    const firstStart = new Date(2026, 8, 20, 10, 0, 0)
    const secondStart = new Date(2026, 8, 20, 11, 0, 0)
    const subtasks = adapter.parseTaskProgressSubtasks({
        progressDetail: {
            subtasks: {
                items: [
                    { name: 'shard', status: 'succeeded', progress: 1 },
                    { name: 'shard', status: 'failed', progress: 1 },
                    { name: 'unmatched', status: 'succeeded', progress: 1 }
                ]
            },
            timeline: {
                items: [
                    { name: 'shard', startTime: firstStart.toISOString(), duration: 1_000 },
                    { name: 'shard', startTime: secondStart.toISOString(), duration: 2_000 }
                ]
            }
        }
    })

    assert.equal(subtasks[0].elapsed, '1s')
    assert.equal(subtasks[0].timeRange, '10:00:00 ~ 10:00:01')
    assert.equal(subtasks[1].elapsed, '2s')
    assert.equal(subtasks[1].timeRange, '11:00:00 ~ 11:00:02')
    assert.equal(subtasks[2].name, 'unmatched')
    assert.equal(subtasks[2].elapsed, '')
    assert.equal(subtasks[2].timeRange, '')
})

test('shouldPollTaskProgress only for RUNNING', () => {
    assert.equal(adapter.shouldPollTaskProgress('RUNNING'), true)
    assert.equal(adapter.shouldPollTaskProgress('SUCCEED'), false)
    assert.equal(adapter.shouldPollTaskProgress('FAILED'), false)
})

test('nodeLink uses nodeHashId and pipeline node route only', () => {
    const link = adapter.nodeLink({ nodeHashId: 'hash-1', dispatchType: { buildType: 'THIRD_PARTY_AGENT_NAME', value: 'not-a-hash' } }, 'proj')
    assert.equal(link, '/console/environment/proj/pipeline/node/allNode?nodeHashId=hash-1')
    assert.equal(adapter.nodeLink({ dispatchType: { value: 'agent-id' } }, 'proj'), '')
})

test('conclusion tone treats cancel and heartbeat as warning not danger', () => {
    assert.equal(adapter.buildConclusion({ status: 'HEARTBEAT_TIMEOUT' }, {}).tone, 'canceled')
    assert.equal(adapter.buildConclusion({ status: 'TERMINATE' }, {}).tone, 'canceled')
    assert.equal(adapter.buildConclusion({ status: 'CANCELED' }, {}).tone, 'canceled')
    assert.equal(adapter.buildConclusion({ status: 'FAILED' }, {}).tone, 'danger')
    assert.equal(adapter.buildConclusion({ status: 'EXEC_TIMEOUT' }, {}).tone, 'danger')
})

test('buildConclusion actions and locate are context-gated', () => {
    const paused = adapter.buildConclusion({ status: 'PAUSE' }, {}, {})
    assert.equal(paused.continueConfig, false)
    assert.equal(adapter.buildConclusion({ status: 'PAUSE' }, {}, { continueConfig: true }).continueConfig, true)
    const reviewing = adapter.buildConclusion({ status: 'REVIEWING' }, {}, {})
    assert.equal(reviewing.review, false)
    assert.equal(adapter.buildConclusion({ status: 'REVIEWING' }, {}, { review: true }).review, true)
    assert.equal(adapter.buildConclusion({ status: 'FAILED' }, {}, {}).locateLog, false)
    assert.equal(adapter.buildConclusion({ status: 'FAILED' }, {}, { locateLog: true }).locateLog, true)
    assert.equal(adapter.buildConclusion({ status: 'HEARTBEAT_TIMEOUT' }, {}, { locateLog: true }).locateLog, true)
})

test('buildConclusion preserves retry and skip capabilities supplied by the execution model', () => {
    assert.equal(adapter.buildConclusion({ status: 'PAUSE' }, {}, { canRetry: true }).canRetry, true)
    assert.equal(adapter.buildConclusion({ status: 'PAUSE' }, {}, { canSkip: true }).canSkip, true)
    assert.equal(adapter.buildConclusion({ status: 'CANCELED' }, {}, { canRetry: true }).canRetry, true)
})

test('historical log mounting follows the loaded node status without disabling execute-count selection', () => {
    assert.equal(adapter.shouldMountLogPanel({ isHistoricalExecuteCount: true, historyRecordState: 'ready', status: 'UNEXEC' }), false)
    assert.equal(adapter.shouldMountLogPanel({ isHistoricalExecuteCount: true, historyRecordState: 'ready', status: 'WAITING' }), false)
    assert.equal(adapter.shouldMountLogPanel({ isHistoricalExecuteCount: true, historyRecordState: 'ready', status: 'DISABLED' }), false)
    assert.equal(adapter.shouldMountLogPanel({ isHistoricalExecuteCount: true, historyRecordState: 'ready', status: 'SUCCEED' }), true)
    assert.equal(adapter.shouldMountLogPanel({ isHistoricalExecuteCount: true, historyRecordState: 'loading', status: 'SUCCEED' }), false)
    assert.equal(adapter.shouldMountLogPanel({ isHistoricalExecuteCount: true, historyRecordState: 'unavailable', status: 'SUCCEED' }), true)
    assert.equal(adapter.shouldMountLogPanel({ isHistoricalExecuteCount: false, status: 'UNEXEC' }), false)
    assert.equal(adapter.shouldMountLogPanel({ isHistoricalExecuteCount: false, status: 'SUCCEED' }), true)

    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    const job = readFileSync(new URL('../../job.vue', import.meta.url), 'utf8')
    assert.match(plugin, /shouldMountLog \(\) \{[\s\S]*shouldMountLogPanel/)
    assert.match(job, /shouldMountLog \(\) \{[\s\S]*shouldMountLogPanel/)
    assert.match(plugin, /v-if="currentTab === 'log' && shouldMountLog"/)
    assert.match(job, /v-if="currentTab === 'log' && shouldMountLog"/)
    assert.doesNotMatch(plugin, /:disabled="!canRenderLog"/)
    assert.doesNotMatch(job, /:disabled="!canRenderLog"/)
})

test('plugin locate entry requires a failed terminal status and mounts the log panel', () => {
    assert.equal(adapter.shouldShowLocateLog('FAILED', true), true)
    assert.equal(adapter.shouldShowLocateLog('QUALITY_CHECK_FAIL', true), true)
    assert.equal(adapter.shouldShowLocateLog('SUCCEED', true), false)
    assert.equal(adapter.shouldShowLocateLog('RUNNING', true), false)
    assert.equal(adapter.shouldShowLocateLog('QUEUE', true), false)
    assert.equal(adapter.shouldShowLocateLog('UNEXEC', true), false)
    assert.equal(adapter.shouldShowLocateLog('FAILED', false), true)
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    assert.match(plugin, /locateLog:\s*shouldShowLocateLog\(effectiveStatus\)[\s\S]*&& this\.shouldMountLog/)
})

test('plugin locate entry also supports an informative summary matched on a non-error log row', () => {
    const summary = 'Script command execution failed with exit code 1 while publishing the report'
    const logs = [{ lineNo: 17, level: 'INFO', text: summary }]
    assert.deepEqual(adapter.resolveLocateLogTarget(logs, [summary]), {
        hasTarget: true,
        lineNo: 17,
        miss: false
    })
    assert.equal(adapter.shouldShowLocateLog('FAILED', false, true), true)
    assert.equal(adapter.shouldShowLocateLog('SUCCEED', false, true), false)
})

test('locate entry remains available before a summary or issue has an actual log row', () => {
    const summary = 'Script command execution failed with exit code 1 while publishing the report'
    assert.deepEqual(adapter.resolveLocateLogTarget([], [summary]), {
        hasTarget: true,
        lineNo: null,
        miss: true
    })
    assert.equal(adapter.shouldShowLocateLog('FAILED', false, false), true)
})

test('resolveConclusionElapsed terminal prefers elapsed then timeCost then timestamp', () => {
    assert.equal(adapter.resolveConclusionElapsed({ status: 'SUCCEED', elapsed: 65000 }), '1m 5s')
    assert.equal(adapter.resolveConclusionElapsed({
        status: 'SUCCEED',
        timeCost: { totalCost: 120000 }
    }), '2m 0s')
    assert.equal(adapter.resolveConclusionElapsed({
        status: 'SUCCEED',
        startEpoch: 1_000,
        endTime: 66_000
    }, { now: 66_000 }), '1m 5s')
    assert.equal(adapter.resolveConclusionElapsed({
        status: 'SUCCEED',
        startEpoch: 1_000
    }, { now: 66_000 }), '')
})

test('resolveConclusionElapsed supports review waiting and ISO timestamp sources', () => {
    assert.equal(adapter.isLiveElapsedStatus('REVIEWING'), true)
    assert.equal(adapter.isTaskElapsedStatus('RUNNING'), true)
    assert.equal(adapter.isTaskElapsedStatus('REVIEWING'), true)
    assert.equal(adapter.isTaskElapsedStatus('PREPARE_ENV'), true)
    assert.equal(adapter.isTaskElapsedStatus('QUEUE'), false)
    assert.equal(adapter.resolveElapsedTimestamp(false), null)
    assert.equal(adapter.resolveElapsedTimestamp([]), null)
    assert.equal(adapter.resolveElapsedTimestamp([1000]), null)
    assert.equal(adapter.resolveElapsedTimestamp(new Date(1000)), null)
    assert.equal(adapter.resolveElapsedTimestamp(-1), null)
    assert.equal(adapter.resolveElapsedTimestamp(''), null)
    assert.equal(adapter.resolveElapsedTimestamp('   '), null)
    assert.equal(adapter.resolveElapsedTimestamp('2026-02-30T01:00:00.000Z'), null)
    assert.equal(adapter.resolveElapsedTimestamp('2026-11-05 01:00:00'), null)
    assert.equal(adapter.resolveElapsedTimestamp('2026-11-05T01:00:00.000Z'), Date.parse('2026-11-05T01:00:00.000Z'))
    assert.equal(adapter.resolveConclusionElapsed({
        status: 'REVIEWING',
        startEpoch: 1_000,
        startTime: '2026-02-30T01:00:00.000Z'
    }, { now: 66_000 }), '1m 5s')
    assert.equal(adapter.resolveConclusionElapsed({
        status: 'REVIEWING',
        startTime: '2026-11-05T01:00:00.000Z'
    }, { now: Date.parse('2026-11-05T01:01:05.000Z') }), '1m 5s')
    assert.equal(adapter.resolveConclusionElapsed({
        status: 'SUCCEED',
        startTime: '2026-11-05T01:00:00.000Z',
        endTime: '2026-11-05T01:01:05.000Z'
    }), '1m 5s')
})

test('review completion signals that a stopped log tail should resume', () => {
    assert.equal(adapter.shouldResumeLogPolling('REVIEWING', 'RUNNING'), true)
    assert.equal(adapter.shouldResumeLogPolling('REVIEWING', 'SUCCEED'), true)
    assert.equal(adapter.shouldResumeLogPolling('FAILED', 'RUNNING'), true)
    assert.equal(adapter.shouldResumeLogPolling('RUNNING', 'SUCCEED'), false)
    assert.equal(adapter.shouldResumeLogPolling('REVIEWING', 'REVIEWING'), false)
})

test('resolveConclusionElapsed prioritizes validated totalCost on every ended state including review abort', () => {
    const endedStatuses = [
        'SUCCEED', 'FAILED', 'CANCELED', 'TERMINATE', 'REVIEW_ABORT', 'REVIEW_PROCESSED',
        'QUALITY_CHECK_PASS', 'QUALITY_CHECK_FAIL', 'QUOTA_FAILED', 'QUEUE_TIMEOUT', 'EXEC_TIMEOUT',
        'HEARTBEAT_TIMEOUT', 'SKIP'
    ]
    for (const status of endedStatuses) {
        assert.equal(adapter.resolveConclusionElapsed({ status, timeCost: { totalCost: 59_000 }, elapsed: 0 }), '59s', `${status} prefers totalCost`)
        assert.equal(adapter.resolveConclusionElapsed({ status, timeCost: { totalCost: '59000' }, elapsed: 0 }), '59s', `${status} accepts decimal string totalCost`)
        assert.equal(adapter.resolveConclusionElapsed({ status, timeCost: { totalCost: 0 }, elapsed: 59_000 }), '0s', `${status} accepts numeric zero`)
        assert.equal(adapter.resolveConclusionElapsed({ status, timeCost: { totalCost: '0' }, elapsed: 59_000 }), '0s', `${status} accepts string zero`)
    }
})

test('resolveConclusionElapsed rejects invalid local duration candidates and preserves timestamp and middle-state behavior', () => {
    for (const totalCost of ['', '  ', false, null, undefined, -1, '-1', NaN, Infinity, 'NaN', 'Infinity']) {
        assert.equal(adapter.resolveConclusionElapsed({ status: 'REVIEW_ABORT', timeCost: { totalCost }, elapsed: '59000' }), '59s')
        assert.equal(adapter.resolveConclusionElapsed({ status: 'REVIEW_ABORT', timeCost: { totalCost }, elapsed: totalCost, startTime: 1_000, endTime: 60_000 }), '59s')
    }
    assert.equal(adapter.resolveConclusionElapsed({ status: 'REVIEW_ABORT', timeCost: { totalCost: '' }, elapsed: '', startTime: 1_000, endTime: 60_000 }), '59s')
    assert.equal(adapter.resolveConclusionElapsed({ status: 'REVIEW_ABORT', timeCost: { totalCost: -1 }, elapsed: false, startTime: 1_000 }), '')
    assert.equal(adapter.resolveConclusionElapsed({ status: 'REVIEW_ABORT', startTime: '', startEpoch: '1000', endTime: '60000' }), '59s')
    assert.equal(adapter.resolveConclusionElapsed({ status: 'REVIEW_ABORT', startTime: false, startEpoch: 1_000, endTime: 60_000 }), '59s')
    assert.equal(adapter.resolveConclusionElapsed({ status: 'REVIEW_ABORT', startTime: -1, startEpoch: -2, endTime: 60_000 }), '')
    assert.equal(adapter.resolveConclusionElapsed({ status: 'REVIEW_ABORT', startTime: false, endTime: 60_000 }), '')
    assert.equal(adapter.resolveConclusionElapsed({ status: 'REVIEW_ABORT', startTime: 1_000, endTime: false }), '')
    assert.equal(adapter.resolveConclusionElapsed({ status: 'REVIEW_ABORT', startTime: 60_000, endTime: 1_000 }), '')
    assert.equal(adapter.resolveConclusionElapsed({ status: 'REVIEW_ABORT', startTime: 1_000 }), '')
    assert.equal(adapter.resolveConclusionElapsed({ status: 'REVIEW_ABORT', timeCost: { totalCost: 59_000 }, elapsed: 0 }), '59s')
    for (const status of ['REVIEWING', 'PAUSE']) {
        assert.equal(adapter.resolveConclusionElapsed({ status, timeCost: { totalCost: 59_000 }, elapsed: 0 }), '0s')
        assert.equal(adapter.resolveConclusionElapsed({ status, timeCost: { totalCost: 59_000 } }), '59s')
    }
})

test('resolveConclusionElapsed RUNNING prefers start+now over elapsed or zero timeCost', () => {
    assert.equal(adapter.resolveConclusionElapsed({
        status: 'RUNNING',
        startEpoch: 1_000,
        elapsed: 5_000,
        timeCost: { totalCost: 0 }
    }, { now: 66_000 }), '1m 5s')
    assert.equal(adapter.resolveConclusionElapsed({
        status: 'RUNNING',
        elapsed: 5_000,
        timeCost: { totalCost: 0 }
    }, { now: 66_000 }), '5s')
})

test('Job duration accepts serialized totalCost and uses the shared long-duration format', () => {
    const jobOptions = loadComponentOptions('../../job.vue', {
        mapGetters: () => ({}),
        pluginLog: {},
        detailContainer: {},
        ContainerContent: {},
        LogConclusionBar: {},
        JobLogPanel: {},
        LogPanelToolbar: {},
        MacDebugDialog: {},
        LogExecuteCountSelect: {},
        convertMStoString: () => 'legacy duration',
        isTaskElapsedStatus: adapter.isTaskElapsedStatus,
        resolveConclusionElapsed: adapter.resolveConclusionElapsed
    })
    const duration = jobOptions.computed.durationText.call({
        displayJob: {
            status: 'SUCCEED',
            timeCost: { totalCost: String(26 * 60 * 60 * 1000) }
        },
        elapsedNow: 0
    })
    assert.equal(duration, '1d 2h 0m 0s')
})

test('Job live duration keeps counting from startTime when startEpoch appears after PREPARE_ENV', () => {
    const preparing = { status: 'PREPARE_ENV', startTime: 1_000 }
    const started = { status: 'RUNNING', startTime: 1_000, startEpoch: 31_000 }
    assert.equal(adapter.resolveConclusionElapsed(preparing, { now: 31_000, preferStartTime: true }), '30s')
    assert.equal(adapter.resolveConclusionElapsed(started, { now: 32_000, preferStartTime: true }), '31s')
    assert.equal(adapter.resolveConclusionElapsed(started, { now: 32_000 }), '1s')
    assert.equal(adapter.resolveConclusionElapsed({ status: 'RUNNING', startEpoch: 31_000 }, { now: 32_000, preferStartTime: true }), '1s')
    assert.equal(adapter.resolveConclusionElapsed({ status: 'FAILED', startTime: 1_000, startEpoch: 31_000, endTime: 61_000 }, { preferStartTime: true }), '1m 0s')
    assert.equal(adapter.buildConclusion(started, {}, { now: 32_000, preferStartTime: true }).elapsed, '31s')

    const jobOptions = loadComponentOptions('../../job.vue', {
        mapGetters: () => ({}),
        pluginLog: {},
        detailContainer: {},
        ContainerContent: {},
        LogConclusionBar: {},
        JobLogPanel: {},
        LogPanelToolbar: {},
        MacDebugDialog: {},
        LogExecuteCountSelect: {},
        isTaskElapsedStatus: adapter.isTaskElapsedStatus,
        resolveConclusionElapsed: adapter.resolveConclusionElapsed
    })
    assert.equal(jobOptions.computed.durationText.call({ displayJob: preparing, elapsedNow: 31_000 }), '30s')
    assert.equal(jobOptions.computed.durationText.call({ displayJob: started, elapsedNow: 32_000 }), '31s')
})

test('shouldApplyProgressResponse and shouldScheduleProgressPoll guard stale polling', () => {
    assert.equal(adapter.shouldApplyProgressResponse({
        requestId: 1,
        currentRequestId: 1,
        contextKey: 'a|1|RUNNING',
        currentContextKey: 'a|1|RUNNING'
    }), true)
    assert.equal(adapter.shouldApplyProgressResponse({
        requestId: 1,
        currentRequestId: 2,
        contextKey: 'a|1|RUNNING',
        currentContextKey: 'a|1|RUNNING'
    }), false)
    assert.equal(adapter.shouldScheduleProgressPoll('RUNNING', true), true)
    assert.equal(adapter.shouldScheduleProgressPoll('RUNNING', false), false)
    assert.equal(adapter.shouldScheduleProgressPoll('SUCCEED', true), false)
})

test('resolveExecTimeoutThreshold reads step and job sources with raw or minute suffix', () => {
    assert.equal(adapter.resolveExecTimeoutThreshold({
        additionalOptions: { timeoutVar: 'BK_CI_STEP_TIMEOUT' }
    }, 'step'), 'BK_CI_STEP_TIMEOUT')
    assert.equal(adapter.resolveExecTimeoutThreshold({
        additionalOptions: { timeout: '30' }
    }, 'step'), '30m')
    assert.equal(adapter.resolveExecTimeoutThreshold({
        jobControlOption: { timeoutVar: '${{ variables.timeout }}' }
    }, 'job'), '${{ variables.timeout }}')
    assert.equal(adapter.resolveExecTimeoutThreshold({
        jobControlOption: { timeout: '15' }
    }, 'job'), '15m')
})

test('resolveExecTimeoutThreshold falls back when timeoutVar is empty string', () => {
    assert.equal(adapter.resolveExecTimeoutThreshold({
        additionalOptions: { timeoutVar: '', timeout: '45' }
    }, 'step'), '45m')
    assert.equal(adapter.resolveExecTimeoutThreshold({
        jobControlOption: { timeoutVar: '', timeout: '20' }
    }, 'job'), '20m')
})

test('builds mutually exclusive Step and Set up job query targets', () => {
    assert.deepEqual(adapter.buildLogTarget({ id: 'e-1' }, 'hash-1'), { tag: 'e-1' })
    assert.deepEqual(adapter.buildLogTarget({ id: 'startVM-job-1', isSetup: true }, 'hash-1'), { jobId: 'hash-1' })
    assert.deepEqual(adapter.buildLogTarget({
        id: 'hook-1',
        additionalOptions: { elementPostInfo: true }
    }, 'hash-1'), { tag: 'hook-1' })
    assert.deepEqual(adapter.buildLogTarget({ id: 'e-1', jobId: 'other' }, 'hash-1'), { tag: 'e-1' })
})

test('hasLogTarget is true only when tag or jobId is present', () => {
    assert.equal(adapter.hasLogTarget({ tag: 'e-1' }), true)
    assert.equal(adapter.hasLogTarget({ jobId: 'hash-1' }), true)
    assert.equal(adapter.hasLogTarget({}), false)
    assert.equal(adapter.hasLogTarget(adapter.buildLogTarget({ isSetup: true }, '')), false)
    assert.equal(adapter.hasLogTarget(adapter.buildLogTarget({}, 'hash-1')), false)
})

test('buildConclusion consumes cancelUserId and timeout from context only', () => {
    const canceled = adapter.buildConclusion({ status: 'CANCELED' }, {}, {
        cancelUserId: 'alice'
    })
    assert.equal(canceled.cancelUserId, 'alice')
    assert.equal(adapter.buildConclusion({ status: 'CANCELED' }, { cancelUserId: 'bob' }, {}).cancelUserId, '')

    const stepTimeout = adapter.buildConclusion(
        { status: 'EXEC_TIMEOUT', additionalOptions: { timeoutVar: 'STEP_TO', timeout: 15 } },
        {},
        { effectiveStatus: 'EXEC_TIMEOUT', stepStatus: 'EXEC_TIMEOUT', element: { additionalOptions: { timeoutVar: 'STEP_TO', timeout: 15 } } }
    )
    assert.equal(stepTimeout.timeoutThreshold, '15m')
    assert.equal(adapter.resolveExecTimeoutThreshold({ additionalOptions: { timeoutVar: '${{ variables.timeout }}', timeout: 15 } }, 'step'), '15m')

    const jobTimeout = adapter.buildConclusion(
        { status: 'EXEC_TIMEOUT' },
        { jobControlOption: { timeout: '20' } },
        {
            effectiveStatus: 'EXEC_TIMEOUT',
            jobStatus: 'EXEC_TIMEOUT',
            container: { jobControlOption: { timeout: '20' } }
        }
    )
    assert.equal(jobTimeout.timeoutThreshold, '20m')
    assert.equal(adapter.resolveExecTimeoutThreshold({ jobControlOption: { timeoutVar: '${{ variables.jobTimeout }}', timeout: 5 } }, 'job'), '5m')
})

test('hasLogIssues detects ERROR and WARN only', () => {
    assert.equal(adapter.hasLogIssues([{ level: 'INFO' }]), false)
    assert.equal(adapter.hasLogIssues([{ level: 'WARN' }, { level: 'INFO' }]), true)
    assert.equal(adapter.hasLogIssues([{ level: 'error' }]), true)
    assert.equal(adapter.hasLogIssues([{ text: '##[error] step failed' }]), true)
    assert.equal(adapter.hasLogIssues([{ content: '\u001b[31m##[warning] warning text\u001b[0m' }]), true)
    assert.equal(adapter.hasLogIssues([{ text: 'ordinary error while opening file' }]), false)
    assert.equal(adapter.hasLogIssues([{ text: 'ordinary warning in documentation' }]), false)
    assert.deepEqual(adapter.sampleLogIssueMarks([
        { text: '##[error] step failed' },
        { content: '\u001b[33m##[warn] warning text\u001b[0m' },
        { text: 'ordinary error word' }
    ]).map(mark => mark.cls), ['is-error', 'is-warn'])
    const stepPanel = readFileSync(new URL('../StepLogPanel.vue', import.meta.url), 'utf8')
    assert.match(stepPanel, /getLogIssueLevel,/)
    assert.match(stepPanel, /this\.logs\.some\(log => Boolean\(getLogIssueLevel\(log\)\)\)/)
    assert.match(stepPanel, /resolveLocateLogTarget\(this\.logs, targetMessages\)/)
    assert.match(stepPanel, /locateRequest: \{ type: Number, default: 0 \}/)
    assert.match(stepPanel, /locateRequestContext: \{ type: String, default: '' \}/)
    assert.match(stepPanel, /logContext: \{ type: String, default: '' \}/)
    assert.match(stepPanel, /taskStatus: \{ type: String, default: '' \}/)
    assert.match(stepPanel, /taskStatus \(status, previousStatus\) \{[\s\S]*shouldResumeLogPolling\(previousStatus, status\)[\s\S]*resumeLogPolling\(\)/)
    assert.match(stepPanel, /pendingStatusResume:\s*false/)
    assert.match(stepPanel, /resumeLogPolling \(\) \{[\s\S]*if \(this\.loadingInitial \|\| this\.loadingAfter\) \{[\s\S]*this\.pendingStatusResume = true/)
    assert.match(stepPanel, /this\.pendingStatusResume[\s\S]*this\.resumeLogPolling\(\)/)
    assert.match(stepPanel, /locateTarget:\s*\{[\s\S]*handler \(\) \{[\s\S]*this\.emitLocateAvailability\(\)/)
    assert.match(stepPanel, /resumeLogPolling \(\) \{[\s\S]*this\.finished = false[\s\S]*this\.loadAfter\(\)/)
    assert.match(stepPanel, /locateRequest \(token, previousToken\) \{[\s\S]*token === previousToken \|\| this\.locateRequestContext !== this\.logContext[\s\S]*this\.\$nextTick\(\(\) => \{[\s\S]*this\.locateTarget\.context === this\.logContext[\s\S]*this\.locateFirstIssue\(\)/)
    assert.match(stepPanel, /locate \(log\) \{\s*this\.highlightedLogId = log\.lineNo[\s\S]*scrollToLine\(log\.lineNo\)/)
    assert.match(stepPanel, /:highlighted-line-no="highlightedLogId"/)
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    const linesView = readFileSync(new URL('../LogLinesView.vue', import.meta.url), 'utf8')
    const job = readFileSync(new URL('../../job.vue', import.meta.url), 'utf8')
    const jobLogPanel = readFileSync(new URL('../JobLogPanel.vue', import.meta.url), 'utf8')
    assert.match(plugin, /:task-status="displayElement\.status"/)
    assert.match(plugin, /@locatable-change="handleLocateAvailability"/)
    assert.match(plugin, /locateLog:\s*shouldShowLocateLog\(effectiveStatus\)[\s\S]*&& this\.shouldMountLog/)
    assert.doesNotMatch(plugin, /hasLocatableSummary \(\)/)
    assert.match(plugin, /const start = resolveElapsedTimestamp\(this\.currentElement\.startEpoch\)\s*\?\?\s*resolveElapsedTimestamp\(this\.currentElement\.startTime\)/)
    assert.match(job, /const start = resolveElapsedTimestamp\(this\.currentJob\.startEpoch\)\s*\?\?\s*resolveElapsedTimestamp\(this\.currentJob\.startTime\)/)
    assert.match(jobLogPanel, /:task-status="selectedItem\.status"/)
    assert.match(job, /'currentJob\.status' \(\) \{[\s\S]*this\.startElapsedTimer\(\)/)
    assert.match(plugin, /:locate-request="locateRequest"/)
    assert.match(plugin, /:locate-request-context="locateRequestContext"/)
    assert.match(plugin, /:log-context="logContext"/)
    assert.match(plugin, /logContext \(\) \{\s*return `\$\{this\.currentElement\.id\}-\$\{this\.activeExecuteCount\}`/)
    assert.match(plugin, /handleLocateLog \(\) \{\s*this\.locateRequest \+= 1/)
    assert.match(plugin, /this\.locateRequestContext = this\.logContext/)
    assert.doesNotMatch(plugin, /handleLocateLog \(\) \{\s*this\.\$refs\.realLog/)
    assert.match(plugin, /:key="logContext"[\s\S]*:locate-request="locateRequest"[\s\S]*:log-context="logContext"/)
    assert.match(linesView, /'is-highlighted': String\(row\.line\.lineNo \?\? ''\) === String\(currentLocateLineNo \?\? ''\)/)
    assert.match(linesView, /scrollToLine \(lineNo\)/)
})

test('error-summary locator selects the most informative matching ERROR line', () => {
    const stderr = 'undefined symbol: foo in libalpha.so at /opt/build/module.cpp:42'
    const message = `Script command: ./run.sh\r\nError message tracking\r\n\u001b[1;31m##[error] ${stderr}\u001b[m\r\nFAQ: see docs`
    const unrelated = [
        { lineNo: 101, level: 'WARN', text: '##[warning] unrelated resource warning with enough detail' },
        { lineNo: 102, level: 'ERROR', text: '##[error] unrelated failure while preparing the workspace' },
        { lineNo: 'stderr-raw-103', level: 'ERROR', text: `\u001b]0;log title\u0007\u001b[1;31m##[error] ${stderr}\u001b[m` }
    ]
    assert.deepEqual(adapter.resolveLocateLogTarget(unrelated, [message]), { hasTarget: true, lineNo: 'stderr-raw-103', miss: false })

    const repeated = [
        { lineNo: 401, level: 'ERROR', text: `##[error] ${stderr}` },
        { lineNo: 402, level: 'ERROR', text: `##[error] ${stderr}` }
    ]
    assert.equal(adapter.resolveLocateLogTarget(repeated, [stderr]).lineNo, 402)
    assert.deepEqual(adapter.resolveLocateLogTarget(unrelated.slice(0, 2), [stderr]), { hasTarget: true, lineNo: null, miss: true })
    const primary = 'primary failure in module alpha at /build/src/a.c:11'
    const supplementary = 'supplementary detailed error in library beta at /build/lib/b.c:209 with a longer explanatory message'
    assert.equal(adapter.resolveLocateLogTarget([
        { lineNo: 501, level: 'ERROR', text: `##[error] ${primary}` },
        { lineNo: 502, level: 'ERROR', text: `##[error] ${supplementary}` }
    ], [primary, supplementary]).lineNo, 501)
})

test('error-summary locator uses ERROR then WARN fallback only for empty or non-informative summaries', () => {
    const logs = [
        { lineNo: 7, level: 'WARN', text: 'warning fallback has enough details' },
        { lineNo: 8, level: 'ERROR', text: 'error fallback has enough details' }
    ]
    assert.equal(adapter.resolveLocateLogTarget(logs, ['', '404']).lineNo, 8)
    assert.equal(adapter.resolveLocateLogTarget(logs.slice(0, 1), []).lineNo, 7)
    assert.equal(adapter.resolveLocateLogTarget(logs, ['error']).hasTarget, false)
})

test('error-summary locator matches informative Chinese and Japanese summaries without selecting earlier unrelated errors', () => {
    const chinese = '配置文件读取失败：目录 /etc/app.yaml 权限被拒绝，无法启动服务'
    const chineseLogs = [
        { lineNo: 601, level: 'ERROR', text: '##[error] 数据库连接超时：主节点不可用，服务无法继续启动' },
        { lineNo: 602, level: 'ERROR', text: `##[error] ${chinese}` }
    ]
    assert.deepEqual(adapter.resolveLocateLogTarget(chineseLogs, [chinese]), { hasTarget: true, lineNo: 602, miss: false })

    const japanese = '設定ファイル /etc/app.yaml を読み込めません：アクセス権がありません'
    const japaneseLogs = [
        { lineNo: 611, level: 'ERROR', text: '##[error] ネットワーク接続がタイムアウトしました。' },
        { lineNo: 612, level: 'ERROR', text: `##[error] ${japanese}` }
    ]
    assert.deepEqual(adapter.resolveLocateLogTarget(japaneseLogs, [japanese]), { hasTarget: true, lineNo: 612, miss: false })

    assert.equal(adapter.resolveLocateLogTarget(chineseLogs, ['发生错误']).hasTarget, false)
})

test('error-summary locator normalizes safe HTML formatting and entities consistently', () => {
    const htmlSummary = 'Script command: ./run.sh<br><a href="https://example.test">BASH_FAQ</a><br>Error: permission denied for /opt/build/app.conf &amp; service startup'
    const plainSummary = 'BASH_FAQ Error: permission denied for /opt/build/app.conf & service startup'
    const logs = [
        { lineNo: 'stderr-raw-103', level: 'ERROR', text: '##[error] <span>BASH_FAQ</span> Error: permission denied for /opt/build/app.conf &amp; service startup' }
    ]
    assert.deepEqual(adapter.resolveLocateLogTarget(logs, [htmlSummary]), { hasTarget: true, lineNo: 'stderr-raw-103', miss: false })
    assert.deepEqual(adapter.resolveLocateLogTarget(logs, [plainSummary]), { hasTarget: true, lineNo: 'stderr-raw-103', miss: false })
    assert.equal(adapter.normalizeLocateText('Compare < value > 3 in /opt/a-b.conf:42'), 'Compare < value > 3 in /opt/a-b.conf:42')
})

test('line-key lookup and locator source support numeric, numeric-string, and arbitrary string keys', () => {
    const logs = [{ lineNo: 103 }, { lineNo: '104' }, { lineNo: 'stderr-raw-103' }]
    assert.equal(virtualWindow.resolveLineIndexByKey(logs, 103), 0)
    assert.equal(virtualWindow.resolveLineIndexByKey(logs, '104'), 1)
    assert.equal(virtualWindow.resolveLineIndexByKey(logs, 'stderr-raw-103'), 2)
    const index = virtualWindow.createHeightIndex().rebuild(logs)
    const anchor = virtualWindow.captureAnchorFromDom({ rowTop: 20, boxTop: 0, lineKey: 'stderr-raw-103', lineNo: 'stderr-raw-103' })
    assert.equal(virtualWindow.resolveAnchorLineIndex(index, anchor, logs), 2)
    assert.deepEqual(adapter.resolveLogHighlightState({
        logs,
        highlightedLineNo: 'stderr-raw-103'
    }), { current: 0, highlightedLineNo: 'stderr-raw-103' })

    const lines = readFileSync(new URL('../LogLinesView.vue', import.meta.url), 'utf8')
    assert.match(lines, /resolveLineIndexByKey\(this\.displayLines, target\)/)
    assert.match(lines, /findLineRow \(lineKey\)[\s\S]*row\.dataset\.lineKey \|\| row\.dataset\.lineNo/)
    assert.match(lines, /const row = this\.findLineRow\(lineNo\)/)
    assert.doesNotMatch(lines, /Number\(lineNo\)|querySelector\(`\.lp-line\[data-line-no=/)
})

test('error-summary locator wiring is context-bound and virtual scrolling calibrates once per locate', () => {
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    const step = readFileSync(new URL('../StepLogPanel.vue', import.meta.url), 'utf8')
    const lines = readFileSync(new URL('../LogLinesView.vue', import.meta.url), 'utf8')
    assert.match(plugin, /:locate-target="locateTarget"/)
    assert.match(plugin, /messages:\s*\[[\s\S]*conclusion\.errorCode[\s\S]*this\.activeErorr\?\.errorMsg/)
    assert.match(plugin, /handleLocateMiss \(\) \{[\s\S]*\$t\('logPanel\.locateSummaryMiss'\)/)
    assert.match(step, /locateTarget: \{ type: Object, default: null \}/)
    assert.match(step, /this\.locateTarget\.context !== this\.logContext/)
    assert.match(step, /resolveLocateLogTarget\(this\.logs, targetMessages\)/)
    assert.match(step, /const hasTarget = resolveLocateLogTarget\(\[\], targetMessages\)\.hasTarget/)
    assert.match(step, /locate \(log\) \{\s*this\.highlightedLogId = log\.lineNo\s*this\.revealLogGroupAncestors\(log\.lineNo\)\s*this\.\$nextTick\(\(\) => this\.\$refs\.linesView\?\.scrollToLine\(log\.lineNo\)\)/)
    assert.doesNotMatch(lines, /highlightedLineNo \(val\)/)
    assert.match(lines, /calibrateLocateLine \(lineNo, generation, attempt\)[\s\S]*attempt >= 2[\s\S]*resolveLineCenterAdjustment/)
    assert.match(lines, /@wheel="onWheel"[\s\S]*@touchstart="onNativeInteraction"[\s\S]*@pointerdown="onNativeInteraction"[\s\S]*@keydown="onNativeInteraction"/)
    assert.equal(virtualWindow.resolveLineCenterAdjustment({ rowTop: 80, rowHeight: 20, boxTop: 0, boxHeight: 200 }), -10)
})

test('Step sends the complete source logs to structure projection and displays its visible rows', () => {
    const step = readFileSync(new URL('../StepLogPanel.vue', import.meta.url), 'utf8')
    const lines = readFileSync(new URL('../LogLinesView.vue', import.meta.url), 'utf8')
    assert.match(step, /:logs="visibleLogs"/)
    assert.match(step, /buildLogStructure\(this\.numberedLogs[\s\S]*context: this\.logContext \|\| this\.contextKey/)
    assert.match(step, /numberedLogs \(\) \{\s*return withDisplayLineNumbers\(this\.logs, this\.displayBase \+ 1\)/)
    assert.match(step, /findLogMatchIds\(this\.logs, this\.controls\.search\)/)
    assert.match(step, /resolveLogGroupAncestors\(this\.logStructure, lineNo\)/)
    assert.match(step, /@toggle-group="toggleLogGroup"/)
    assert.match(lines, /row\.line\._logGroup[\s\S]*@click\.stop="\$emit\('toggle-group', row\.line\._logGroup\.id\)"/)
})

test('visible build-level queue detail allows an UNEXEC Job conclusion bar only when queue data is visible', () => {
    const visibleQueue = { visible: true, position: 1, running: [], ahead: [] }
    assert.equal(adapter.canShowJobConclusion('UNEXEC', visibleQueue), true)
    assert.equal(adapter.canShowJobConclusion('WAITING', visibleQueue), true)
    assert.equal(adapter.canShowJobConclusion('', visibleQueue), true)
    assert.equal(adapter.canShowJobConclusion('UNEXEC', null), false)
    assert.equal(adapter.canShowJobConclusion('WAITING', { visible: false }), false)
    assert.equal(adapter.canShowJobConclusion('', null), false)
    assert.equal(adapter.canShowJobConclusion('', { visible: false }), false)
    assert.equal(adapter.canShowJobConclusion('DISABLED', visibleQueue), false)

    const job = readFileSync(new URL('../../job.vue', import.meta.url), 'utf8')
    const conclusionCondition = job.match(/<log-conclusion-bar\s+v-if="([^"]+)"/)
    assert.ok(conclusionCondition)
    assert.doesNotMatch(conclusionCondition[1], /currentJob\.status/)
    assert.match(job, /queueDetailModel \(\) \{\s*return resolveJobQueueDetail\(/)
    assert.match(job, /logConclusionModel \(\) \{[\s\S]*const queueDetail = this\.isHistoricalExecuteCount \? null : this\.queueDetailModel/)
    assert.match(job, /canShowConclusion \(\) \{\s*return canShowJobConclusion\(this\.currentJob\.status, this\.queueDetailModel\)/)
})

test('Job log directory does not mount log requests for statuses without logs', () => {
    const jobLogPanel = readFileSync(new URL('../JobLogPanel.vue', import.meta.url), 'utf8')
    for (const status of ['UNEXEC', 'WAITING', 'DISABLED']) {
        assert.equal(adapter.shouldLoadStepLog({ status }), false)
    }
    assert.equal(adapter.shouldLoadStepLog({ status: 'FAILED' }), true)
    assert.match(jobLogPanel, /import[\s\S]*shouldLoadStepLog/)
    assert.match(jobLogPanel, /<step-log-panel\s+v-if="canLoadSelectedLog"/)
    assert.match(jobLogPanel, /canLoadStepLogForSelection \(\) \{\s*return !this\.selectedItem\.roundSkipped && shouldLoadStepLog\(\{ status: this\.selectedItem\.status \}\)/)
    assert.match(jobLogPanel, /canLoadSelectedLog \(\) \{\s*return this\.canLoadStepLogForSelection && this\.hasLogTarget/)
    assert.match(jobLogPanel, /\$t\(selectedItem\.roundSkipped \? 'logPanel\.status\.skippedInRound' : canLoadStepLogForSelection \? 'logPanel\.emptyLog' : 'logPanel\.status\.notExecuted'\)/)
})

test('located issue highlight survives log refreshes and clears on an explicit search reset', () => {
    const logs = [
        { lineNo: 162, text: 'ordinary output' },
        { lineNo: 163, text: '##[error] check failed' },
        { lineNo: 164, text: 'after failure' }
    ]
    const firstIssue = logs.find(line => adapter.getLogIssueLevel(line))
    assert.equal(firstIssue.lineNo, 163)
    assert.deepEqual(adapter.resolveLogHighlightState({
        logs,
        highlightedLineNo: firstIssue.lineNo,
        currentMatch: 0
    }), { current: 0, highlightedLineNo: 163 })
    assert.deepEqual(adapter.resolveLogHighlightState({
        logs,
        highlightedLineNo: firstIssue.lineNo,
        reset: true,
        searching: false
    }), { current: 0, highlightedLineNo: '' })
    assert.deepEqual(adapter.resolveLogHighlightState({
        logs,
        highlightedLineNo: firstIssue.lineNo,
        searching: true
    }), { current: 0, highlightedLineNo: '' })
    const stepPanel = readFileSync(new URL('../StepLogPanel.vue', import.meta.url), 'utf8')
    assert.match(stepPanel, /resolveLogHighlightState\(\{[\s\S]*highlightedLineNo: this\.highlightedLogId[\s\S]*this\.highlightedLogId = state\.highlightedLineNo/)
})

test('sampleLogIssueMarks stays within cap when forcing head/tail ERROR', () => {
    const logs = Array.from({ length: 600 }, (_, i) => ({
        level: i === 1 || i === 598 ? 'ERROR' : 'WARN',
        lineNo: i + 1
    }))
    const marks = adapter.sampleLogIssueMarks(logs, 400)
    assert.ok(marks.length <= 400)
    assert.ok(marks.some(mark => mark.key === 'error-1'))
    assert.ok(marks.some(mark => mark.key === 'error-598'))
})

test('formatLevelSummary fits the 80px trigger', () => {
    assert.equal(adapter.formatLevelSummary(['INFO', 'WARN', 'ERROR']), 'INFO,WARN,ERROR')
    assert.equal(adapter.formatLevelSummary(['ERROR']), 'ERROR')
    assert.equal(adapter.formatLevelSummary(['INFO', 'WARN', 'ERROR', 'DEBUG']), 'ALL')
})

test('toolbar delegates its compact summary to formatLevelSummary', () => {
    const source = readFileSync(new URL('../LogPanelToolbar.vue', import.meta.url), 'utf8')
    assert.match(source, /import \{ formatLevelSummary, normalizeLogLevels \} from '.\/logPanelAdapter'/)
    assert.match(source, /levelSummary \(\) \{\s*return formatLevelSummary\(this\.selectedLevels, Boolean\(this\.controls\.levelAll\)\)\s*\}/)
    assert.match(source, /<bk-popover[\s\S]*trigger="click"[\s\S]*theme="log-panel-level-menu"[\s\S]*placement="bottom-start"[\s\S]*:arrow="false"[\s\S]*:disabled="disabled"[\s\S]*@show="onLevelPopoverShow"[\s\S]*@hide="onLevelPopoverHide"/)
    assert.match(source, /slot="content"[\s\S]*class="log-panel-level-popup"/)
    assert.match(source, /class="log-panel-level-all"[\s\S]*ALL[\s\S]*class="log-panel-level-divider"[\s\S]*role="separator"[\s\S]*v-for="level in levelOptions"/)
    assert.match(source, /class="log-panel-level-popup"\s+@mousedown\.stop\s+@mouseup\.stop/)
    assert.match(source, /levelPopoverOptions: \{ interactive: true, offset: '0, 4', distance: 0 \}/)
    assert.match(source, /onLevelPopoverShow \(\) \{\s*this\.levelPopupOpen = true\s*\}/)
    assert.match(source, /onLevelPopoverHide \(\) \{\s*this\.levelPopupOpen = false/)
    assert.match(source, /openLevelPopup \(\) \{[\s\S]*showHandler\(\)[\s\S]*firstCheckbox\?\.focus\(\)/)
    assert.match(source, /onDocumentKeyDown \(event\) \{[\s\S]*event\.key !== 'Escape'[\s\S]*closeLevelPopup\(\{ restoreFocus: true \}\)/)
    assert.match(source, /@change="toggleLevel\(level, \$event\)"/)
    assert.match(source, /toggleLevel \(level, event\) \{[\s\S]*new Set\(this\.controls\.levelAll \? \[\] : this\.selectedLevels\)/)
    assert.doesNotMatch(source, /@click="toggleLevelPopup"|onDocumentMouseDown|addEventListener\('mousedown'/)
    assert.match(source, /<style lang="scss">[\s\S]*\.log-panel-level-popup[\s\S]*\.log-panel-level-option/)
    assert.match(source, /\.log-panel-level-popup \.log-panel-level-all\s*\{[^}]*width:\s*100%;[^}]*height:\s*28px;[^}]*white-space:\s*nowrap/)
    assert.match(source, /\.log-panel-level-popup\s*\{[^}]*width:\s*96px;[^}]*min-width:\s*96px/)
    assert.match(source, /input:checked \+ \.level-check::before,[\s\S]*level-all\.is-selected \.level-check::before\s*\{\s*content:\s*'✓';\s*color:\s*#3a84ff/)
    assert.match(source, /input:focus-visible \+ \.level-check/)
    assert.doesNotMatch(source, /\.log-panel-level-option input\s*\{\s*width:\s*auto/)
    assert.match(source, /\.tippy-tooltip\.log-panel-level-menu-theme\s*\{[^}]*padding:\s*0;[^}]*background:\s*transparent;[^}]*border:\s*0;[^}]*box-shadow:\s*none/)
    assert.match(source, /\.log-panel-toolbar \.level-trigger:focus-visible\s*\{[^}]*outline:\s*1px solid #979ba5;[^}]*outline-offset:\s*-1px;[^}]*box-shadow:\s*none/)
    assert.match(source, /\.log-panel-toolbar \.level-trigger:focus:not\(:focus-visible\) \{ outline: none; box-shadow: none; \}/)
    assert.match(source, /\.log-panel-toolbar \.search-box input:focus-visible\s*\{[^}]*outline:\s*none;[^}]*box-shadow:\s*inset 0 0 0 2px #3c96ff/)
    assert.match(source, /width:\s*148px/)
    assert.match(source, /min-width:\s*max-content/)
    assert.doesNotMatch(source, /@media \(max-width:\s*900px\)/)
})

test('log header keeps tabs centered on the panel while reserving both side regions', () => {
    const source = readFileSync(new URL('../../detailContainer.vue', import.meta.url), 'utf8')
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    const job = readFileSync(new URL('../../job.vue', import.meta.url), 'utf8')
    assert.match(source, /grid-template-columns:\s*minmax\(0,\s*1fr\)\s+max-content\s+minmax\(0,\s*1fr\)/)
    assert.match(source, /\.log-head-tools\s*\{[\s\S]*grid-column:\s*3;[\s\S]*justify-self:\s*stretch;[\s\S]*min-width:\s*0;[\s\S]*width:\s*100%;[\s\S]*max-width:\s*100%;[\s\S]*display:\s*flex;[\s\S]*justify-content:\s*flex-start;[\s\S]*overflow-x:\s*auto[\s\S]*::v-deep \.log-panel-toolbar\s*\{\s*flex:\s*0 0 auto;\s*margin-left:\s*auto/)
    assert.doesNotMatch(source, /clientWidth\s*<=\s*(?:1000|1100)|is-compact|grid-template-rows:\s*26px 26px/)
    assert.match(source, /<div class="log-head-tab"><slot name="tab"><\/slot><\/div>/)
    assert.match(source, /v-if="hasToolSlot"[\s\S]*class="log-head-tools"[\s\S]*:tabindex="isScrollableTools \? 0 : undefined"/)
    assert.match(source, /isScrollableTools \? \$t\('logPanel\.scrollToolbar'\) : undefined/)
    assert.match(source, /column-gap:\s*16px/)
    assert.match(source, /\.log-head\s*\{[\s\S]*overflow:\s*hidden/)
    assert.match(source, /::v-deep \.log-status\s*\{[\s\S]*width:\s*16px[\s\S]*height:\s*16px[\s\S]*padding:\s*0/)
    assert.match(source, /\.log-status \+ \.log-title-text \{ margin-left:\s*8px/)
    assert.match(source, /\.black-theme\s*\{[\s\S]*box-shadow:\s*0 0 10px 0 rgba\(0, 0, 0, \.15\)/)
    assert.match(plugin, /:position-code="positionCode"/)
    assert.match(source, /allowToolOverflow:\s*\{\s*type:\s*Boolean,\s*default:\s*false\s*\}/)
    assert.match(source, /'allow-tool-overflow':\s*allowToolOverflow/)
    assert.match(source, /&\.allow-tool-overflow\s*\{[\s\S]*\.log-head,[\s\S]*\.log-head \.log-head-tools\s*\{\s*overflow:\s*visible/)
    assert.doesNotMatch(plugin, /allow-tool-overflow/)
    assert.match(source, /\.log-head-tools\s*\{[\s\S]*overflow-x:\s*auto;[\s\S]*overflow-y:\s*hidden/)
    assert.match(job, /:position-code="positionCode"/)
})

test('log panel close button binds the prototype SVG with a 16px current-color mask', () => {
    const source = readFileSync(new URL('../../detailContainer.vue', import.meta.url), 'utf8')
    const closeButton = source.match(/<button\s+type="button"[\s\S]*?class="log-home-close-bar"[\s\S]*?>[\s\S]*?<\/button>/)
    assert.ok(closeButton, 'the close button template must be present')
    assert.match(closeButton[0], /@click="closeLog"/)
    assert.match(closeButton[0], /:aria-label="\$t\('close'\)"/)
    assert.match(closeButton[0], /:title="\$t\('close'\)"/)
    assert.match(closeButton[0], /<span\s+class="log-home-close-icon"\s+:style="closeIconStyle"\s+aria-hidden="true"\s*\/>/)
    assert.doesNotMatch(source, /import Logo from ['"]@\/components\/Logo['"]|name="(?:icon-)?angle-right"/)

    const iconImport = source.match(/import\s+(\w+)\s+from\s+['"]([^'"]+\/svg-sprites\/devops\/angle-right\.svg)['"]/)
    assert.ok(iconImport, 'the prototype arrow must be imported directly from the devops SVG source')
    assert.equal(iconImport[2], '../../../../svg-sprites/devops/angle-right.svg')
    const componentUrl = new URL('../../detailContainer.vue', import.meta.url)
    const iconUrl = new URL(iconImport[2], componentUrl)
    assert.equal(existsSync(iconUrl), true, 'the imported SVG source must exist')
    const svg = readFileSync(iconUrl, 'utf8')
    assert.equal(svg.match(/<svg\b[^>]*\bviewBox="([^"]+)"/)?.[1], '0 0 32 32')
    const path = svg.match(/<path\b[^>]*\bd="([^"]+)"/)
    assert.ok(path?.[1].trim(), 'the imported SVG must contain its real non-empty path')
    assert.equal(path[1], 'M10 30c-0.24 0-0.48-0.080-0.68-0.26-0.4-0.38-0.44-1-0.060-1.42l11.38-12.32-11.38-12.32c-0.38-0.4-0.34-1.040 0.060-1.42s1.040-0.34 1.42 0.060l12 13c0.36 0.38 0.36 0.98 0 1.36l-12 13c-0.2 0.22-0.48 0.32-0.74 0.32z')

    const styleObject = source.match(/closeIconStyle:\s*\{([\s\S]*?)\n\s*\}/)
    assert.ok(styleObject, 'the close icon must bind a mask style')
    assert.match(styleObject[1], /backgroundColor:\s*'currentColor'/)
    const assertMasksUseImportedAsset = (componentSource, importedVariable) => {
        const componentStyle = componentSource.match(/closeIconStyle:\s*\{([\s\S]*?)\n\s*\}/)
        assert.ok(componentStyle, 'the close icon must bind a mask style')
        const maskBindings = [...componentStyle[1].matchAll(/(?:^|\n)\s*(mask|WebkitMask):\s*`url\(\$\{(\w+)\}\) no-repeat center \/ contain`/g)]
            .map(([, property, variable]) => ({ property, variable }))
        assert.equal(maskBindings.filter(({ property }) => property === 'mask').length, 1, 'mask must appear exactly once')
        assert.equal(maskBindings.filter(({ property }) => property === 'WebkitMask').length, 1, 'WebkitMask must appear exactly once')
        assert.deepEqual(maskBindings, [
            { property: 'mask', variable: importedVariable },
            { property: 'WebkitMask', variable: importedVariable }
        ], 'both exact mask properties must use the imported prototype SVG')
    }
    assertMasksUseImportedAsset(source, iconImport[1])

    const misboundSource = source.replace(
        "import angleRightIcon from '../../../../svg-sprites/devops/angle-right.svg'",
        "import angleRightIcon from '../../../../svg-sprites/devops/angle-down.svg'\n    import prototypeArrowIcon from '../../../../svg-sprites/devops/angle-right.svg'"
    )
    assert.notEqual(misboundSource, source, 'mutation points the mask variable at angle-down while retaining a separate prototype SVG import')
    const prototypeImportInMutation = misboundSource.match(/import\s+(\w+)\s+from\s+['"]([^'"]+\/svg-sprites\/devops\/angle-right\.svg)['"]/)
    assert.equal(prototypeImportInMutation?.[1], 'prototypeArrowIcon')
    assert.throws(() => assertMasksUseImportedAsset(misboundSource, prototypeImportInMutation[1]), /mask properties must use the imported prototype SVG|mask must appear exactly once/)
    for (const [invalidProperty, invalidSource] of [
        ['Mask', source.replace('mask: `url(', 'Mask: `url(')],
        ['Webkitmask', source.replace('WebkitMask:', 'Webkitmask:')]
    ]) {
        assert.notEqual(invalidSource, source, `mutation changes the ${invalidProperty} property casing`)
        assert.throws(() => assertMasksUseImportedAsset(invalidSource, iconImport[1]), /mask must appear exactly once|WebkitMask must appear exactly once/)
    }
    assert.match(source, /\.log-home-close-icon\s*\{[^}]*width:\s*16px;[^}]*height:\s*16px;[^}]*flex-shrink:\s*0;/)
    assert.match(source, /\.log-home-close-bar\s*\{[\s\S]*height:\s*60px;[\s\S]*width:\s*24px;[\s\S]*background:\s*#464953;[\s\S]*color:\s*#c4c6cc;[\s\S]*border-radius:\s*2px 0 0 2px;[\s\S]*&:focus-visible/)
})

test('plugin title includes the three-level position code and panel geometry contracts', () => {
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    const container = readFileSync(new URL('../../detailContainer.vue', import.meta.url), 'utf8')
    assert.equal(adapter.positionCode({ stageIndex: 0, containerIndex: 0, elementIndex: 0 }), '1-1-1')
    assert.match(plugin, /:position-code="positionCode"/)
    assert.match(container, /--log-panel-width:\s*min\(1200px, calc\(100vw - 40px\)\)/)
    assert.match(container, /width:\s*var\(--log-panel-width\)/)
    assert.match(container, /\.log-home-close-bar\s*\{[\s\S]*right:\s*calc\(var\(--log-panel-width\) \+ 8px\);[\s\S]*top:\s*8px/)
    assert.match(container, /margin:\s*8px/)
    const directory = readFileSync(new URL('../JobLogPanel.vue', import.meta.url), 'utf8')
    assert.match(directory, /flex:\s*0 0 280px;[^}]*padding:\s*0/)
})

test('minimap remains available for non-empty short and unwrapped logs but not empty results', () => {
    assert.equal(adapter.shouldShowLogMinimap({ lineCount: 0 }), false)
    assert.equal(adapter.shouldShowLogMinimap({ lineCount: 1 }), true)
    assert.equal(adapter.shouldShowLogMinimap({ lineCount: 20 }), true)
    const lines = readFileSync(new URL('../LogLinesView.vue', import.meta.url), 'utf8')
    const minimap = readFileSync(new URL('../LogErrorMinimap.vue', import.meta.url), 'utf8')
    assert.match(lines, /lineCount:\s*this\.renderLines\.length/)
    assert.match(lines, /this\.contentHeight = totalHeight/)
    assert.match(minimap, /v-if="coordinates\.interactive"/)
})

test('progress tab requires real progress data', () => {
    assert.equal(adapter.shouldShowProgressTab({ data: null, percent: null, subtasks: [] }), false)
    assert.equal(adapter.shouldShowProgressTab({ data: {}, percent: null, subtasks: [] }), false)
    assert.equal(adapter.shouldShowProgressTab({ data: { progress: { value: 'unknown' } }, percent: null, subtasks: [] }), false)
    assert.equal(adapter.shouldShowProgressTab({ data: { progress: { value: 50 } }, percent: 50, subtasks: [], error: true }), false)
    assert.equal(adapter.shouldShowProgressTab({ data: null, percent: 0, subtasks: [] }), true)
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    assert.match(plugin, /tab\.name !== 'progress' \|\| canShowProgressTab/)
})

test('dev UNEXEC mock suppresses logs even when the real step completed', () => {
    const unexecutedMock = devMock.resolveLogPanelDevMock('unexecuted', 'development')
    const loadingMock = devMock.resolveLogPanelDevMock('loading', 'development')
    const effectiveUnexecuted = devMock.applyLogPanelDevMock({ status: 'SUCCEED' }, unexecutedMock, 'step')
    assert.equal(effectiveUnexecuted.status, 'UNEXEC')
    assert.equal(adapter.shouldLoadStepLog({ status: 'SUCCEED', devMock: unexecutedMock }), false)
    assert.equal(adapter.shouldLoadStepLog({ status: 'SUCCEED' }), true)
    assert.equal(adapter.shouldLoadStepLog({ status: 'UNEXEC' }), false)
    assert.equal(adapter.shouldLoadStepLog({ status: 'SUCCEED', devMock: loadingMock }), true)
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    assert.match(plugin, /canRenderLog \(\) \{\s*if \(this\.isHistoricalExecuteCount\) return true\s*return shouldLoadStepLog\(\{ status: this\.currentElement\.status, devMock: this\.devLogMock \}\)/)
    assert.match(plugin, /shouldMountLog \(\) \{[\s\S]*shouldMountLogPanel/)
    assert.match(plugin, /canShowConclusion \(\) \{\s*if \(this\.isHistoricalExecuteCount\)[\s\S]*?return this\.canRenderLog \|\| this\.currentElement\.status === 'UNEXEC'/)
    assert.match(plugin, /<log-conclusion-bar[\s\S]*v-if="currentTab === 'log' && showConclusion"/)
    assert.match(plugin, /<step-log-panel[\s\S]*v-if="currentTab === 'log' && shouldMountLog"/)
    assert.match(plugin, /effectiveStatus === 'UNEXEC'[\s\S]*\$t\('logPanel\.status\.notExecutedLabel'\)[\s\S]*statusMessageKey: effectiveStatus === 'UNEXEC' \|\| conditionSkipped \? '' : model\.statusMessageKey/)
    assert.equal(adapter.buildConclusion({ status: effectiveUnexecuted.status }, {}).statusMessageKey, 'notExecuted')
    assert.match(plugin, /v-if="currentTab === 'log' && shouldMountLog"/)
    assert.doesNotMatch(plugin, /realLogFallback|<plugin-log/)
    const emptyState = plugin.match(/v-if="currentTab === 'log'[\s\S]*!shouldMountLog[\s\S]*displayElement\.status === 'UNEXEC'"([\s\S]*?)<\/div>/)
    assert.ok(emptyState)
    assert.match(emptyState[1], /role="status"[\s\S]*logPanel\.status\.notExecuted/)
    assert.doesNotMatch(emptyState[1], /log-line-number|stepNotStarted/)
})

test('visibleParamRows limits previews but not searches', () => {
    const rows = Array.from({ length: 25 }, (_, index) => ({ key: `key-${index}`, value: `value-${index}` }))
    assert.equal(adapter.visibleParamRows(rows, { keyword: '', expanded: false, limit: 20 }).length, 20)
    assert.equal(adapter.visibleParamRows(rows, { keyword: '', expanded: true, limit: 20 }).length, 25)

    const searchableRows = Array.from({ length: 25 }, (_, index) => ({
        key: `match-${index}`,
        value: `other-${index}`,
        expr: ''
    }))
    assert.equal(
        adapter.visibleParamRows(searchableRows, { keyword: 'match', expanded: false, limit: 20 }).length,
        25
    )

    const valueRows = [
        { key: 'alpha', value: 'needle-here', expr: '' },
        { key: 'beta', value: 'plain', expr: '' }
    ]
    assert.deepEqual(
        adapter.visibleParamRows(valueRows, { keyword: 'needle', expanded: false, limit: 20 }).map(row => row.key),
        ['alpha']
    )

    const exprRows = [
        { key: 'alpha', value: '', expr: '${{ variables.target }}' },
        { key: 'beta', value: '', expr: '${{ variables.other }}' }
    ]
    assert.deepEqual(
        adapter.visibleParamRows(exprRows, { keyword: 'target', expanded: false, limit: 20 }).map(row => row.key),
        ['alpha']
    )
})

test('secret expressions are removed during normalization and never searchable', () => {
    const sections = adapter.buildParamsModel({
        data: {
            input: {
                apiToken: {
                    value: 'resolved-secret',
                    expr: '${{ secrets.API_TOKEN }}'
                },
                _credential: '${{ secrets.CREDENTIAL }}',
                branch: {
                    value: 'main',
                    expr: '${{ variables.BRANCH }}'
                }
            }
        }
    })
    const input = sections.find(section => section.id === 'input')
    const apiToken = input.rows.find(row => row.key === 'apiToken')
    const credential = input.rows.find(row => row.key === '_credential')
    const branch = input.rows.find(row => row.key === 'branch')

    assert.equal(apiToken.value, '******')
    assert.equal(apiToken.expr, undefined)
    assert.equal(credential.value, '******')
    assert.equal(credential.expr, undefined)
    assert.equal(branch.expr, '${{ variables.BRANCH }}')
    assert.deepEqual(adapter.visibleParamRows(input.rows, {
        keyword: 'secrets.API_TOKEN',
        expanded: false
    }), [])
    assert.deepEqual(adapter.visibleParamRows(input.rows, {
        keyword: 'secrets.CREDENTIAL',
        expanded: false
    }), [])
})

test('visibleParamRows ignores expr on defensive secret rows', () => {
    const rows = [{
        key: 'apiToken',
        value: '******',
        expr: '${{ secrets.LEAK }}',
        secret: true
    }]
    assert.deepEqual(adapter.visibleParamRows(rows, {
        keyword: 'secrets.LEAK',
        expanded: false
    }), [])
})

test('paramCopyText never exposes secret values', () => {
    assert.equal(adapter.paramCopyText({ value: 'secret', secret: true }), '******')
    assert.equal(adapter.paramCopyText({ value: 'plain', secret: false }), 'plain')
})

test('resolveRecordElement supports regular and matrix positions', () => {
    const regular = { id: 'regular' }
    const matrix = { id: 'matrix' }
    const detail = {
        model: {
            stages: [{
                containers: [{
                    elements: [regular],
                    groupContainers: [{ elements: [matrix] }]
                }]
            }]
        }
    }
    assert.equal(adapter.resolveRecordElement(detail, {
        stageIndex: 0,
        containerIndex: 0,
        elementIndex: 0
    }), regular)
    assert.equal(adapter.resolveRecordElement(detail, {
        stageIndex: 0,
        containerIndex: 0,
        containerGroupIndex: 0,
        elementIndex: 0
    }), matrix)
})

test('resolveParamsElementSource never falls back to latest while historical data is unavailable', () => {
    const latestElement = { id: 'step', data: { input: { source: 'latest' } } }
    const historicalElement = { id: 'step', data: { input: { source: 'history' } } }
    const position = { stageIndex: 0, containerIndex: 0, elementIndex: 0 }
    const detail = element => ({
        model: {
            stages: [{ containers: [{ elements: [element] }] }]
        }
    })

    assert.equal(adapter.resolveParamsElementSource({
        execDetail: detail(latestElement),
        position,
        selectedExecuteCount: null,
        latestExecuteCount: 3
    }), latestElement)
    assert.equal(adapter.resolveParamsElementSource({
        execDetail: detail(latestElement),
        position,
        selectedExecuteCount: 3,
        latestExecuteCount: 3
    }), latestElement)
    assert.deepEqual(adapter.resolveParamsElementSource({
        execDetail: detail(latestElement),
        paramsExecDetail: null,
        position,
        selectedExecuteCount: 2,
        latestExecuteCount: 3
    }), {})
    assert.equal(adapter.resolveParamsElementSource({
        execDetail: detail(latestElement),
        paramsExecDetail: detail(historicalElement),
        position,
        selectedExecuteCount: 2,
        latestExecuteCount: 3
    }), historicalElement)
})

test('buildParamsRecordRequest accepts only booleans or exact true and false strings', () => {
    const base = {
        projectId: 'project',
        pipelineId: 'pipeline',
        buildNo: 'build',
        executeCount: 2
    }
    const expectedBase = {
        ...base,
        skipSetPipelineDetail: true
    }

    for (const archiveFlag of [
        undefined,
        null,
        '',
        '   ',
        'garbage',
        ' false ',
        'TRUE',
        1,
        {},
        []
    ]) {
        assert.deepEqual(adapter.buildParamsRecordRequest({ ...base, archiveFlag }), expectedBase)
    }
    for (const archiveFlag of [true, false, 'true', 'false']) {
        assert.deepEqual(adapter.buildParamsRecordRequest({ ...base, archiveFlag }), {
            ...expectedBase,
            archiveFlag
        })
    }
})

test('historical record validation requires the requested identity and positive snapshot evidence', () => {
    const detail = {
        id: 'build-1',
        pipelineId: 'pipe-1',
        executeCount: 1,
        model: { stages: [{ id: 'stage-1', containers: [] }] }
    }
    const context = { buildNo: 'build-1', pipelineId: 'pipe-1', executeCount: 1 }
    assert.equal(adapter.validateHistoricalLogRecord(detail, context), false)
    assert.equal(adapter.validateHistoricalLogRecord({ ...detail, executeCount: 2, startTime: 100 }, context), false)
    assert.equal(adapter.validateHistoricalLogRecord({ ...detail, executeCount: true, startTime: 100 }, context), false)
    assert.equal(adapter.validateHistoricalLogRecord({ ...detail, id: 'other', startTime: 100 }, context), false)
    assert.equal(adapter.validateHistoricalLogRecord({ ...detail, id: {}, startTime: 100 }, context), false)
    assert.equal(adapter.validateHistoricalLogRecord({ ...detail, startTime: '1700000000' }, context), true)
    assert.equal(adapter.validateHistoricalLogRecord({
        ...detail,
        errorInfoList: [{ containerId: 'job-1', taskId: 'task-1', errorCode: 'E1', errorMsg: 'failed' }]
    }, context), true)
    assert.equal(adapter.validateHistoricalLogRecord({
        ...detail,
        errorInfoList: [{ containerId: 'job-1', taskId: '', errorCode: 'E1' }]
    }, context), true)
    assert.equal(adapter.validateHistoricalLogRecord({
        ...detail,
        errorInfoList: [{ containerId: 'job-1', taskId: 'task-1' }]
    }, context), false)
    assert.equal(adapter.validateHistoricalLogRecord({ ...detail, startTime: 0 }, { ...context, isCurrent: false }), false)
    assert.equal(adapter.validateHistoricalLogRecord({
        ...detail,
        queueTime: 1700000000000,
        currentTimestamp: 1700000001000,
        model: {
            stages: [{
                id: 'stage-1',
                containers: [{
                    id: 'job-1',
                    executeCount: 2,
                    elements: [{ id: 'task-1', executeCount: 2, status: 'FAILED' }]
                }]
            }]
        }
    }, context), false)
})

test('historical snapshots match stable identities and preserve partial-retry counts without fabrication', () => {
    const position = { stageIndex: 0, containerIndex: 0, elementIndex: 0 }
    const source = {
        model: { stages: [{
            id: 'stage-1',
            containers: [{ id: 'job-1', containerId: 'container-1', containerHashId: 'hash-1', elements: [{ id: 'task-1' }] }]
        }] }
    }
    const first = {
        model: { stages: [{
            id: 'stage-1',
            containers: [
                { id: 'job-other', containerId: 'container-other', containerHashId: 'hash-other', elements: [] },
                { id: 'job-1-old', containerId: 'container-1', containerHashId: 'hash-1', executeCount: 1, elements: [{ id: 'task-1', executeCount: 1, status: 'FAILED' }] }
            ]
        }] }
    }
    const snapshot = adapter.resolveHistoricalPluginSnapshot(source, first, position, 2)
    assert.equal(snapshot.container.id, 'job-1-old')
    assert.equal(snapshot.element.status, 'FAILED')
    assert.equal(snapshot.element.executeCount, 1)

    const missingCount = structuredClone(first)
    delete missingCount.model.stages[0].containers[1].executeCount
    delete missingCount.model.stages[0].containers[1].elements[0].executeCount
    const missingSnapshot = adapter.resolveHistoricalPluginSnapshot(source, missingCount, position, 2)
    assert.equal(Object.hasOwn(missingSnapshot.container, 'executeCount'), false)
    assert.equal(Object.hasOwn(missingSnapshot.element, 'executeCount'), false)

    const tooNew = structuredClone(first)
    tooNew.model.stages[0].containers[1].elements[0].executeCount = 3
    assert.equal(adapter.resolveHistoricalPluginSnapshot(source, tooNew, position, 2), null)

    const childFromFuture = structuredClone(first)
    childFromFuture.model.stages[0].containers[1].executeCount = 1
    childFromFuture.model.stages[0].containers[1].elements[0].executeCount = 3
    assert.equal(adapter.resolveHistoricalJobSnapshot(source, childFromFuture, { stageIndex: 0, containerIndex: 0 }, 2), null)

    const duplicate = structuredClone(first)
    duplicate.model.stages[0].containers.push({ ...duplicate.model.stages[0].containers[1] })
    assert.equal(adapter.resolveHistoricalPluginSnapshot(source, duplicate, position, 2), null)

    const hashConflict = structuredClone(first)
    hashConflict.model.stages[0].containers[1].containerHashId = 'different-hash'
    assert.equal(adapter.resolveHistoricalPluginSnapshot(source, hashConflict, position, 2), null)

    const changedElement = structuredClone(first)
    changedElement.model.stages[0].containers[1].elements[0].id = 'new-task-id'
    assert.equal(adapter.resolveHistoricalPluginSnapshot(source, changedElement, position, 2), null)
})

test('historical matrix child matching stays under the uniquely matched parent', () => {
    const source = {
        model: { stages: [{ id: 'stage-1', containers: [{
            id: 'matrix-parent', containerHashId: 'parent-hash', matrixGroupFlag: true,
            groupContainers: [{ id: 'child-1', containerId: 'child-container-1', containerHashId: 'child-hash-1', elements: [] }]
        }] }] }
    }
    const history = {
        model: { stages: [{ id: 'stage-1', containers: [{
            id: 'matrix-parent-history', containerHashId: 'parent-hash', matrixGroupFlag: true,
            groupContainers: [
                { id: 'child-other', containerId: 'other', containerHashId: 'child-hash-2', elements: [] },
                { id: 'child-1-history', containerId: 'child-container-1', containerHashId: 'child-hash-1', elements: [] }
            ]
        }] }] }
    }
    const position = { stageIndex: 0, containerIndex: 0, containerGroupIndex: 0 }
    assert.equal(adapter.resolveHistoricalJobSnapshot(source, history, position, 1).container.id, 'child-1-history')
    const duplicate = structuredClone(history)
    duplicate.model.stages[0].containers[0].groupContainers.push({ ...duplicate.model.stages[0].containers[0].groupContainers[1] })
    assert.equal(adapter.resolveHistoricalJobSnapshot(source, duplicate, position, 1), null)
})

test('Job local snapshots stay off matrix-parent and legacy fallback routes but remain eligible for matrix children', async () => {
    const options = loadComponentOptions('../../job.vue', {
        mapGetters: () => ({}), pluginLog: {}, detailContainer: {}, ContainerContent: {},
        LogConclusionBar: {}, JobLogPanel: {}, LogPanelToolbar: {}, MacDebugDialog: {}, LogExecuteCountSelect: {}
    })
    assert.equal(options.computed.supportsLocalHistory.call({ currentJob: { matrixGroupFlag: true } }), false)
    assert.equal(options.computed.supportsLocalHistory.call({ currentJob: { matrixGroupFlag: false } }), true)
    assert.equal(options.computed.isHistoricalExecuteCount.call({
        selectedExecuteCount: 1, supportsLocalHistory: false, latestExecuteCount: 2
    }), false)
    let dispatches = 0
    const vm = {
        isHistoricalExecuteCount: false,
        historyRequestId: 0,
        historyRecordState: 'idle',
        $store: {
            dispatch: () => {
                dispatches += 1
            }
        }
    }
    await options.methods.fetchHistoricalDetail.call(vm)
    assert.equal(dispatches, 0)
})

test('historical record requests are read-only and do not request a global detail update', () => {
    assert.deepEqual(adapter.buildHistoricalRecordRequest({
        projectId: 'project', pipelineId: 'pipeline', buildNo: 'record-id', executeCount: 1, archiveFlag: true
    }), {
        projectId: 'project', pipelineId: 'pipeline', buildNo: 'record-id', executeCount: 1, archiveFlag: true
    })
    assert.equal(Object.hasOwn(adapter.buildHistoricalRecordRequest({}), 'skipSetPipelineDetail'), false)
})

test('historical conclusions project selected failure data without exposing execution actions', () => {
    const jobDependencies = {
        mapGetters: () => ({}), jobLog: {}, pluginLog: {}, detailContainer: {}, ContainerContent: {},
        LogConclusionBar: {}, JobLogPanel: {}, LogPanelToolbar: {}, MacDebugDialog: {}, LogExecuteCountSelect: {},
        buildConclusion: adapter.buildConclusion,
        resolveJobQueueDetail: () => null,
        isSystemAgentTermination: adapter.isSystemAgentTermination,
        shouldShowLocateLog: adapter.shouldShowLocateLog
    }
    const jobOptions = loadComponentOptions('../../job.vue', jobDependencies)
    const jobConclusion = jobOptions.computed.logConclusionModel.call({
        displayJob: { id: 'job-1', status: 'FAILED', canRetry: true, canSkip: true, timeCost: { totalCost: 31000 }, elements: [] },
        displayExecDetail: { errorInfoList: [], cancelUserId: '' },
        jobErrorSummary: { errorCode: 'E1', errorMsg: 'history failed' },
        jobFailureStep: null,
        isHistoricalExecuteCount: true,
        displayConclusionAvailable: true,
        isLatestBuild: true,
        conclusionVisible: false,
        queueDetailModel: null,
        devLogMock: null,
        elapsedNow: 0,
        $route: { params: { projectId: 'project-1' } },
        $t: key => key
    })
    assert.equal(jobConclusion.status, 'FAILED')
    assert.equal(jobConclusion.errorCode, 'E1')
    assert.equal(jobConclusion.canRetry, false)
    assert.equal(jobConclusion.canSkip, false)
    assert.equal(jobConclusion.locateLog, true)

    const pluginOptions = loadComponentOptions('../../plugin.vue', {
        mapActions: () => ({}), mapState: () => ({}), detailContainer: {}, ReferenceVariable: {}, pluginLog: {},
        AtomContent: {}, ProgressDetailPanel: {}, StepLogPanel: {}, LogPanelToolbar: {}, LogConclusionBar: {},
        LogExecuteCountSelect: {}, LogParamsView: {}, Artifactory: {}, Report: {},
        buildConclusion: adapter.buildConclusion,
        isJobTimeoutTerminatedStep: adapter.isJobTimeoutTerminatedStep,
        isSystemAgentTermination: adapter.isSystemAgentTermination,
        hasCustomRunCondition: () => true,
        shouldShowLocateLog: () => true
    })
    const pluginConclusion = pluginOptions.computed.logConclusionModel.call({
        displayElement: { id: 'task-1', status: 'FAILED', canRetry: true, canSkip: true, errorCode: 'E2', errorMsg: 'step failed' },
        displayContainer: { id: 'job-1', status: 'FAILED' },
        displayExecDetail: { errorInfoList: [], cancelUserId: '' },
        currentNodeError: null,
        isHistoricalExecuteCount: true,
        displayConclusionAvailable: true,
        isLatestBuild: true,
        conclusionVisible: false,
        actionsEnabled: true,
        canReview: true,
        devLogMock: null,
        realLogFallback: false,
        logIssuesAvailable: true,
        shouldMountLog: true,
        elapsedNow: 0,
        $route: { params: { projectId: 'project-1' } },
        $t: key => key
    })
    assert.equal(pluginConclusion.status, 'FAILED')
    assert.equal(pluginConclusion.canRetry, false)
    assert.equal(pluginConclusion.canSkip, false)
    assert.equal(pluginConclusion.review, false)
    assert.equal(pluginConclusion.continueConfig, false)
    assert.equal(pluginConclusion.conditionLink, false)
    assert.equal(pluginConclusion.locateLog, true)
})

test('job timeout conclusion takes priority over a matching system cancellation conclusion', () => {
    const pluginOptions = loadComponentOptions('../../plugin.vue', {
        mapActions: () => ({}), mapState: () => ({}), detailContainer: {}, ReferenceVariable: {}, pluginLog: {},
        AtomContent: {}, ProgressDetailPanel: {}, StepLogPanel: {}, LogPanelToolbar: {}, LogConclusionBar: {},
        LogExecuteCountSelect: {}, LogParamsView: {}, Artifactory: {}, Report: {},
        buildConclusion: adapter.buildConclusion,
        isJobTimeoutTerminatedStep: adapter.isJobTimeoutTerminatedStep,
        isSystemAgentTermination: adapter.isSystemAgentTermination,
        hasCustomRunCondition: adapter.hasCustomRunCondition,
        shouldShowLocateLog: adapter.shouldShowLocateLog
    })
    const conclusion = pluginOptions.computed.logConclusionModel.call({
        displayElement: { id: 'task-1', status: 'TERMINATE', errorCode: 2199005 },
        displayContainer: { id: 'job-1', name: 'job-1', status: 'TERMINATE' },
        displayExecDetail: {
            errorInfoList: [],
            cancelUserId: '',
            buildEndInfo: {
                endCategory: 'CANCEL',
                endType: 'CANCEL_SYSTEM',
                reason: 'system cancellation',
                positions: [{ taskId: 'task-1', containerId: 'job-1' }]
            }
        },
        currentNodeError: null,
        isHistoricalExecuteCount: false,
        displayConclusionAvailable: true,
        isLatestBuild: true,
        conclusionVisible: true,
        actionsEnabled: true,
        canReview: false,
        devLogMock: null,
        logIssuesAvailable: false,
        logSummaryAvailable: false,
        elapsedNow: 0,
        $route: { params: { projectId: 'project-1' } },
        $t: (key, values) => key === 'details.statusMap.EXEC_TIMEOUT'
            ? 'timeout label'
            : `${key}:${values?.join('|') || ''}`
    })
    assert.equal(conclusion.label, 'timeout label')
    assert.equal(conclusion.message, 'logPanel.status.jobTimeout:job-1|')

    const jobOptions = loadComponentOptions('../../job.vue', {
        mapGetters: () => ({}), jobLog: {}, pluginLog: {}, detailContainer: {}, ContainerContent: {},
        LogConclusionBar: {}, JobLogPanel: {}, LogPanelToolbar: {}, MacDebugDialog: {}, LogExecuteCountSelect: {},
        convertMStoString: () => '', buildConclusion: adapter.buildConclusion,
        resolveJobQueueDetail: () => null,
        isSystemAgentTermination: adapter.isSystemAgentTermination,
        shouldShowLocateLog: adapter.shouldShowLocateLog,
        canShowJobConclusion: () => true
    })
    const jobConclusion = jobOptions.computed.logConclusionModel.call({
        currentJob: { id: 'job-1', containerId: 'job-1', status: 'CANCELED', elements: [], timeCost: {} },
        displayJob: { id: 'job-1', containerId: 'job-1', status: 'CANCELED', elements: [], timeCost: {} },
        displayExecDetail: {
            errorInfoList: [],
            cancelUserId: '',
            buildEndInfo: {
                endCategory: 'CANCEL',
                endType: 'CANCEL_SYSTEM',
                reason: '所属 Job 超过执行时限 30m，步骤被终止',
                reasonCode: 'bkBuildCancelSystemJobExecTimeout',
                positions: [{ containerId: 'job-1' }]
            }
        },
        jobErrorSummary: null,
        jobFailureStep: null,
        retryableStep: null,
        isHistoricalExecuteCount: false,
        displayConclusionAvailable: true,
        isLatestBuild: true,
        conclusionVisible: true,
        actionsEnabled: true,
        queueDetailModel: null,
        devLogMock: null,
        elapsedNow: 0,
        $route: { params: { projectId: 'project-1' }, query: {} },
        $t: (key, values) => key === 'details.statusMap.EXEC_TIMEOUT'
            ? 'timeout label'
            : `${key}:${values?.join('|') || ''}`
    })
    assert.equal(jobConclusion.status, 'EXEC_TIMEOUT')
    assert.equal(jobConclusion.label, 'timeout label')
    assert.equal(jobConclusion.message, '所属 Job 超过执行时限 30m，步骤被终止')
})

test('progress status changes invalidate in-flight requests before terminal state cleanup', () => {
    const pluginOptions = loadComponentOptions('../../plugin.vue', {
        mapActions: () => ({}), mapState: () => ({}), detailContainer: {}, ReferenceVariable: {}, pluginLog: {},
        AtomContent: {}, ProgressDetailPanel: {}, StepLogPanel: {}, LogPanelToolbar: {}, LogConclusionBar: {},
        LogExecuteCountSelect: {}, LogParamsView: {}, Artifactory: {}, Report: {},
        shouldApplyProgressResponse: adapter.shouldApplyProgressResponse,
        shouldFetchTaskProgress: adapter.shouldFetchTaskProgress
    })
    let resetCount = 0
    const vm = {
        progressRequestId: 7,
        startElapsedTimer () {},
        stopProgressPoll () {},
        resetProgressState () {
            resetCount += 1
        },
        progressExpanded: true,
        progressContextKey: 'task-1|build-1|1'
    }
    pluginOptions.watch['currentElement.status'].call(vm, 'SUCCEED')
    assert.equal(vm.progressRequestId, 8)
    assert.equal(resetCount, 1)
    assert.equal(vm.progressExpanded, false)
    assert.equal(pluginOptions.methods.isProgressContextCurrent.call(vm, 7, 'task-1|build-1|1'), false)
})

test('execution-count changes clear the previous progress snapshot before refetching', () => {
    const pluginOptions = loadComponentOptions('../../plugin.vue', {
        mapActions: () => ({}), mapState: () => ({}), detailContainer: {}, ReferenceVariable: {}, pluginLog: {},
        AtomContent: {}, ProgressDetailPanel: {}, StepLogPanel: {}, LogPanelToolbar: {}, LogConclusionBar: {},
        LogExecuteCountSelect: {}, LogParamsView: {}, Artifactory: {}, Report: {},
        shouldFetchTaskProgress: status => status === 'RUNNING'
    })
    const events = []
    const vm = {
        currentTab: 'log',
        settingView: 'config',
        currentElement: { status: 'RUNNING' },
        invalidateParamsRecord () {},
        stopProgressPoll () {
            events.push('stop')
        },
        resetProgressState (options) {
            events.push(['reset', options])
        },
        fetchProgress () {
            events.push('fetch')
        }
    }

    pluginOptions.watch.activeExecuteCount.call(vm)

    assert.deepEqual(events, ['stop', ['reset', { preserveSnapshot: false }], 'fetch'])

    events.length = 0
    vm.currentElement.status = 'SUCCEED'
    pluginOptions.watch.activeExecuteCount.call(vm)

    assert.deepEqual(events, ['stop', ['reset', { preserveSnapshot: false }]])
})

test('stale progress responses do not write state after a real status transition', async () => {
    let resolveRequest
    const request = new Promise(resolve => {
        resolveRequest = resolve
    })
    const pluginOptions = loadComponentOptions('../../plugin.vue', {
        mapActions: () => ({}), mapState: () => ({}), detailContainer: {}, ReferenceVariable: {}, pluginLog: {},
        AtomContent: {}, ProgressDetailPanel: {}, StepLogPanel: {}, LogPanelToolbar: {}, LogConclusionBar: {},
        LogExecuteCountSelect: {}, LogParamsView: {}, Artifactory: {}, Report: {},
        parseTaskProgressPercent: adapter.parseTaskProgressPercent,
        parseTaskProgressSubtasks: adapter.parseTaskProgressSubtasks,
        shouldApplyProgressResponse: adapter.shouldApplyProgressResponse,
        shouldFetchTaskProgress: adapter.shouldFetchTaskProgress,
        shouldScheduleProgressPoll: adapter.shouldScheduleProgressPoll
    })
    const vm = {
        devLogMock: null,
        progressTimer: null,
        progressRequestId: 7,
        progressContextKey: 'task-1|build-1|1',
        currentElement: { id: 'task-1', status: 'RUNNING' },
        activeExecuteCount: 1,
        execDetail: { id: 'build-1' },
        $route: { params: { projectId: 'project-1', pipelineId: 'pipeline-1' } },
        $ajax: { get: () => request },
        progressLoading: false,
        progressError: false,
        progressDetailData: null,
        progressPercent: null,
        progressSubtasks: [],
        progressSubtasksSnapshot: [],
        progressExpandInitKey: '',
        progressExpanded: false,
        startElapsedTimer () {},
        stopProgressPoll () {},
        scheduleProgressPoll () {},
        resetProgressState: pluginOptions.methods.resetProgressState,
        isProgressContextCurrent: pluginOptions.methods.isProgressContextCurrent
    }
    const pendingFetch = pluginOptions.methods.fetchProgress.call(vm)
    vm.currentElement.status = 'SUCCEED'
    pluginOptions.watch['currentElement.status'].call(vm, 'SUCCEED')
    resolveRequest({ data: { progressDetail: { progress: { value: 0.75 } } } })
    await pendingFetch
    assert.equal(vm.progressDetailData, null)
    assert.equal(vm.progressPercent, null)
    assert.deepEqual(vm.progressSubtasks, [])
    assert.equal(vm.progressLoading, false)
})

test('parent cancellation conclusions use the nested parent pipeline operator', () => {
    const dependencies = {
        mapGetters: () => ({}), jobLog: {}, pluginLog: {}, detailContainer: {}, ContainerContent: {},
        LogConclusionBar: {}, JobLogPanel: {}, LogPanelToolbar: {}, MacDebugDialog: {}, LogExecuteCountSelect: {},
        convertMStoString: () => '',
        buildConclusion: adapter.buildConclusion,
        resolveJobQueueDetail: () => null,
        isSystemAgentTermination: adapter.isSystemAgentTermination,
        resolveNodeLogError: adapter.resolveNodeLogError,
        resolveJobFailureStep: adapter.resolveJobFailureStep,
        isConcurrencyQueueCandidate: () => false,
        isVisibleConcurrencyQueueResponse: () => false,
        canShowJobConclusion: () => true,
        shouldClearQueueAfterError: () => false,
        applyLogPanelDevMock: value => value,
        resolveLogPanelDevMock: () => null,
        shouldShowLocateLog: adapter.shouldShowLocateLog,
        RESOURCE_ACTION: {},
        handlePipelineNoPermission: () => {}
    }
    const jobOptions = loadComponentOptions('../../job.vue', dependencies)
    const buildEndInfo = {
        endCategory: 'CANCEL',
        endType: 'CANCEL_PARENT_PIPELINE',
        operator: 'top-level-operator',
        positions: [{ containerId: 'job-1', taskId: 'task-1' }],
        parentPipelineInfo: {
            pipelineName: 'Parent pipeline',
            buildNum: 12,
            operator: 'parent-operator'
        }
    }
    const job = {
        id: 'job-1',
        status: 'CANCELED',
        elements: [],
        timeCost: {}
    }
    const getJobConclusion = buildEndInfo => jobOptions.computed.logConclusionModel.call({
        currentJob: job,
        displayJob: job,
        displayExecDetail: { errorInfoList: [], buildEndInfo },
        jobErrorSummary: null,
        jobFailureStep: null,
        isHistoricalExecuteCount: false,
        displayConclusionAvailable: true,
        isLatestBuild: true,
        conclusionVisible: true,
        queueDetailModel: null,
        devLogMock: null,
        elapsedNow: 0,
        $route: { params: { projectId: 'project-1' } },
        $t: (key, values) => `${key}:${values?.join('|') || ''}`
    })
    const jobConclusion = getJobConclusion(buildEndInfo)
    assert.match(jobConclusion.message, /parent-operator/)
    assert.doesNotMatch(jobConclusion.message, /top-level-operator/)
    const fallbackBuildEndInfo = {
        ...buildEndInfo,
        parentPipelineInfo: { ...buildEndInfo.parentPipelineInfo, operator: '' }
    }
    const fallbackJobConclusion = getJobConclusion(fallbackBuildEndInfo)
    assert.equal(fallbackJobConclusion.message, 'logPanel.queue.parentCanceled:')

    const pluginOptions = loadComponentOptions('../../plugin.vue', {
        mapActions: () => ({}), mapState: () => ({}), detailContainer: {}, ReferenceVariable: {}, pluginLog: {},
        AtomContent: {}, ProgressDetailPanel: {}, StepLogPanel: {}, LogPanelToolbar: {}, LogConclusionBar: {},
        LogExecuteCountSelect: {}, LogParamsView: {}, Artifactory: {}, Report: {},
        buildConclusion: adapter.buildConclusion,
        isJobTimeoutTerminatedStep: adapter.isJobTimeoutTerminatedStep,
        isSystemAgentTermination: adapter.isSystemAgentTermination,
        hasCustomRunCondition: adapter.hasCustomRunCondition,
        shouldShowLocateLog: adapter.shouldShowLocateLog
    })
    const getPluginConclusion = buildEndInfo => pluginOptions.computed.logConclusionModel.call({
        displayElement: { id: 'task-1', status: 'CANCELED' },
        displayContainer: { id: 'job-1', status: 'CANCELED' },
        displayExecDetail: { errorInfoList: [], buildEndInfo },
        currentNodeError: null,
        isHistoricalExecuteCount: false,
        displayConclusionAvailable: true,
        isLatestBuild: true,
        conclusionVisible: true,
        actionsEnabled: true,
        canReview: false,
        devLogMock: null,
        logIssuesAvailable: false,
        logSummaryAvailable: false,
        shouldMountLog: true,
        elapsedNow: 0,
        $route: { params: { projectId: 'project-1' } },
        $t: (key, values) => `${key}:${values?.join('|') || ''}`
    })
    const pluginConclusion = getPluginConclusion(buildEndInfo)
    assert.match(pluginConclusion.message, /parent-operator/)
    assert.doesNotMatch(pluginConclusion.message, /top-level-operator/)
    const fallbackPluginConclusion = getPluginConclusion(fallbackBuildEndInfo)
    assert.equal(fallbackPluginConclusion.message, 'logPanel.queue.parentCanceled:')
})

test('Job and plugin historical detail methods use a validated local record and ignore stale responses', async () => {
    const source = {
        id: 'build-1', pipelineId: 'pipe-1', executeCount: 3,
        model: { stages: [{ id: 'stage-1', containers: [{
            id: 'job-current', containerId: 'container-1', containerHashId: 'job-hash', executeCount: 3,
            status: 'SUCCEED', name: 'Current title',
            elements: [{ id: 'task-1', executeCount: 3, status: 'SUCCEED', name: 'Current step' }]
        }] }] }
    }
    const historical = count => ({
        id: 'build-1', pipelineId: 'pipe-1', executeCount: count, queueTime: 1700000000000, currentTimestamp: 1700000001000,
        model: { stages: [{ id: 'stage-1', containers: [{
            id: `job-history-${count}`, containerId: 'container-1', containerHashId: 'job-hash', executeCount: count,
            status: 'FAILED', name: `Historical title ${count}`, timeCost: { totalCost: 31000 },
            elements: [{ id: 'task-1', executeCount: Math.max(1, count - 1), status: 'FAILED', name: 'Historical step', errorCode: 'E1', errorMsg: 'step failed' }]
        }] }] }
    })
    const position = { stageIndex: 0, containerIndex: 0, elementIndex: 0 }
    const requestContext = count => ({
        projectId: 'project-1', pipelineId: 'pipe-1', buildNo: 'build-1', executeCount: count,
        stageId: 'stage-1', containerHashId: 'job-hash', containerId: 'container-1', containerLegacyId: 'job-current',
        targetHashId: 'job-hash', targetId: 'job-current', targetContainerId: 'container-1', elementId: 'task-1'
    })
    const jobDependencies = {
        mapGetters: () => ({}), jobLog: {}, pluginLog: {}, detailContainer: {}, ContainerContent: {},
        LogConclusionBar: {}, JobLogPanel: {}, LogPanelToolbar: {}, MacDebugDialog: {}, LogExecuteCountSelect: {},
        convertMStoString: () => '', buildConclusion: adapter.buildConclusion,
        resolveHistoricalJobSnapshot: adapter.resolveHistoricalJobSnapshot,
        validateHistoricalLogRecord: adapter.validateHistoricalLogRecord,
        buildHistoricalRecordRequest: adapter.buildHistoricalRecordRequest
    }
    const jobOptions = loadComponentOptions('../../job.vue', jobDependencies)
    const jobVm = {
        ...jobOptions.methods,
        selectedExecuteCount: 2,
        isHistoricalExecuteCount: true,
        historyRequestId: 0,
        historyContextKey: 'job-context-2',
        historyRequestContext: requestContext(2),
        historyExecDetail: null,
        historyJobSnapshot: null,
        historyRecordState: 'idle',
        execDetail: source,
        editingElementPos: position,
        _isDestroyed: false,
        $route: { params: { projectId: 'project-1', pipelineId: 'pipe-1' }, query: {} },
        $store: { dispatch: async (action, payload) => {
            assert.equal(action, 'atom/fetchPipelineExecDetail')
            assert.deepEqual(payload, {
                projectId: 'project-1', pipelineId: 'pipe-1', buildNo: 'build-1', executeCount: 2
            })
            return historical(2)
        } }
    }
    await jobOptions.methods.fetchHistoricalDetail.call(jobVm)
    assert.equal(jobVm.historyRecordState, 'ready')
    assert.equal(jobVm.historyJobSnapshot.container.name, 'Historical title 2')
    assert.equal(jobVm.historyJobSnapshot.container.elements[0].executeCount, 1)
    assert.deepEqual(jobOptions.computed.displayJob.call({ isHistoricalExecuteCount: true, historyRecordState: 'loading', historyJobSnapshot: null }), {})
    assert.equal(jobOptions.computed.showConclusion.call({ isHistoricalExecuteCount: true, historyRecordState: 'loading', displayConclusionAvailable: false, displayCanShowConclusion: false }), false)
    assert.equal(jobOptions.computed.displayJob.call({ isHistoricalExecuteCount: true, historyRecordState: 'ready', historyJobSnapshot: { container: { status: 'FAILED' } } }).status, 'FAILED')

    const pluginDependencies = {
        mapActions: () => ({}), mapState: () => ({}),
        detailContainer: {}, ReferenceVariable: {}, pluginLog: {}, AtomContent: {}, ProgressDetailPanel: {}, StepLogPanel: {},
        LogPanelToolbar: {}, LogConclusionBar: {}, LogExecuteCountSelect: {}, LogParamsView: {}, Artifactory: {}, Report: {},
        convertMStoString: () => '', resolveReviewUsers,
        buildConclusion: adapter.buildConclusion,
        buildHistoricalRecordRequest: adapter.buildHistoricalRecordRequest,
        validateHistoricalLogRecord: adapter.validateHistoricalLogRecord,
        resolveHistoricalPluginSnapshot: adapter.resolveHistoricalPluginSnapshot
    }
    const pluginOptions = loadComponentOptions('../../plugin.vue', pluginDependencies)
    const pluginVm = {
        ...pluginOptions.methods,
        selectedExecuteCount: 2,
        isHistoricalExecuteCount: true,
        historyRequestId: 0,
        historyContextKey: 'plugin-context-2',
        historyRequestContext: { ...requestContext(2), elementId: 'task-1' },
        historyExecDetail: null,
        historyPluginSnapshot: null,
        historyRecordState: 'idle',
        execDetail: source,
        editingElementPos: position,
        _isDestroyed: false,
        $route: { params: { projectId: 'project-1', pipelineId: 'pipe-1' }, query: {} },
        $store: { dispatch: async () => historical(2) }
    }
    await pluginOptions.methods.fetchHistoricalDetail.call(pluginVm)
    assert.equal(pluginVm.historyRecordState, 'ready')
    assert.equal(pluginVm.historyPluginSnapshot.element.name, 'Historical step')
    assert.equal(pluginVm.historyPluginSnapshot.element.executeCount, 1)
    assert.deepEqual(pluginOptions.computed.displayElement.call({ isHistoricalExecuteCount: true, historyRecordState: 'loading', historyPluginSnapshot: null }), {})
    assert.equal(pluginOptions.computed.showConclusion.call({ isHistoricalExecuteCount: true, historyRecordState: 'loading', canShowConclusion: false }), false)
    assert.equal(pluginOptions.computed.displayElement.call({ isHistoricalExecuteCount: true, historyRecordState: 'ready', historyPluginSnapshot: { element: { status: 'FAILED' } } }).status, 'FAILED')

    const pendingResolvers = []
    const staleVm = {
        ...jobOptions.methods,
        selectedExecuteCount: 1,
        isHistoricalExecuteCount: true,
        historyRequestId: 0,
        historyContextKey: 'ctx-1',
        historyRequestContext: requestContext(1),
        historyExecDetail: null,
        historyJobSnapshot: null,
        historyRecordState: 'idle',
        execDetail: source,
        editingElementPos: position,
        _isDestroyed: false,
        $route: { params: { projectId: 'project-1', pipelineId: 'pipe-1' }, query: {} },
        $store: { dispatch: () => new Promise(resolve => pendingResolvers.push(resolve)) }
    }
    const firstRequest = jobOptions.methods.fetchHistoricalDetail.call(staleVm)
    staleVm.historyContextKey = 'ctx-2'
    staleVm.historyRequestContext = requestContext(2)
    const secondRequest = jobOptions.methods.fetchHistoricalDetail.call(staleVm)
    staleVm.historyContextKey = 'ctx-1'
    staleVm.historyRequestContext = requestContext(1)
    const thirdRequest = jobOptions.methods.fetchHistoricalDetail.call(staleVm)
    pendingResolvers[2](historical(1))
    await thirdRequest
    pendingResolvers[1](historical(2))
    await secondRequest
    pendingResolvers[0](historical(1))
    await firstRequest
    assert.equal(staleVm.historyRecordState, 'ready')
    assert.equal(staleVm.historyExecDetail.executeCount, 1)
    assert.equal(staleVm.historyJobSnapshot.container.name, 'Historical title 1')

    const closedRequest = jobOptions.methods.fetchHistoricalDetail.call(staleVm)
    staleVm.invalidateHistoricalDetail()
    pendingResolvers[3](historical(1))
    await closedRequest
    assert.equal(staleVm.historyRecordState, 'idle')
    assert.equal(staleVm.historyExecDetail, null)

    staleVm.historyRequestContext = requestContext(1)
    staleVm.historyContextKey = 'supported-context'
    staleVm.isHistoricalExecuteCount = true
    const fallbackRequest = jobOptions.methods.fetchHistoricalDetail.call(staleVm)
    staleVm.isHistoricalExecuteCount = false
    staleVm.historyContextKey = 'legacy-fallback-context'
    jobOptions.watch.historyContextKey.call(staleVm)
    pendingResolvers[4](historical(1))
    await fallbackRequest
    assert.equal(staleVm.historyRecordState, 'idle')
    assert.equal(staleVm.historyExecDetail, null)
})

test('shouldFetchParamsRecord accepts only positive integer execute counts', () => {
    assert.equal(adapter.shouldFetchParamsRecord(1, 2), true)
    assert.equal(adapter.shouldFetchParamsRecord('3', 5), true)

    assert.equal(adapter.shouldFetchParamsRecord(null, 2), false)
    assert.equal(adapter.shouldFetchParamsRecord(undefined, 2), false)
    assert.equal(adapter.shouldFetchParamsRecord('', 2), false)
    assert.equal(adapter.shouldFetchParamsRecord('   ', 2), false)
    assert.equal(adapter.shouldFetchParamsRecord(false, 2), false)
    assert.equal(adapter.shouldFetchParamsRecord(true, 2), false)
    assert.equal(adapter.shouldFetchParamsRecord('abc', 2), false)
    assert.equal(adapter.shouldFetchParamsRecord(0, 2), false)
    assert.equal(adapter.shouldFetchParamsRecord(-1, 2), false)
    assert.equal(adapter.shouldFetchParamsRecord(1.5, 2), false)
    assert.equal(adapter.shouldFetchParamsRecord('1.5', 2), false)
    assert.equal(adapter.shouldFetchParamsRecord(2, 2), false)
    assert.equal(adapter.shouldFetchParamsRecord('2', 2), false)
})

test('node errors prefer the complete matching record message and keep node-only fields as fallback', () => {
    const summary = adapter.resolveNodeLogError([
        { taskId: 7, containerId: 2, errorCode: 'fallback-code', errorMsg: '\u001b[31mfallback message\u001b[0m' },
        { taskId: 8, containerId: 2, errorCode: 'other-node', errorMsg: 'must not leak' }
    ], { id: '7', containerId: '2' }, { errorCode: 'node-code' })
    assert.deepEqual(summary, { errorType: undefined, errorCode: 'node-code', errorMsg: 'fallback message' })
    assert.equal(adapter.resolveNodeLogError([
        { taskId: 8, containerId: 2, errorCode: 'other-node', errorMsg: 'must not leak' }
    ], { id: 7, containerId: 2 }, {}), null)
})

test('Job summary uses same-task complete visible error text for direct log location', () => {
    const task = { id: 'task-7', containerId: 'job-2' }
    const complete = 'BASH_FAQ: command failed because deploy target is unavailable and requires a retry.'
    const errors = [
        { taskId: 'task-8', containerId: 'job-2', errorMsg: 'neighbor task must not leak' },
        { taskId: task.id, containerId: task.containerId, errorMsg: `<a target="_blank" href="https://example.test">${complete}</a>` }
    ]
    const summary = adapter.resolveNodeLogError(errors, task, {
        errorMsg: `${complete.slice(0, 22)} <a target="_blank" href="https://example.test`
    })
    assert.equal(summary.errorMsg, complete)
    const logs = [{ lineNo: 177, level: 'ERROR', text: `##[error] <a target="_blank" href="https://example.test">${complete}</a>` }]
    assert.deepEqual(adapter.resolveLocateLogTarget(logs, [summary.errorMsg]), { hasTarget: true, lineNo: 177, miss: false })
    assert.equal(adapter.normalizeJobSummaryMessage('before <a target="_blank" href="https://example.test'), 'before')
    assert.equal(adapter.normalizeJobSummaryMessage('x <array> y &lt; z'), 'x <array> y < z')
})

test('Job failure attribution excludes only explicit automatic continue and preserves stable failed order', () => {
    const job = { id: 42, status: 'FAILED' }
    const steps = [
        { id: 1, name: 'auto continue', status: 'FAILED', canSkip: true, additionalOptions: { manualSkip: false, continueWhenFailed: true, failControl: ['continueWhenFailed'] } },
        { id: 2, name: 'manual skip', status: 'FAILED', canSkip: true, additionalOptions: { manualSkip: true, continueWhenFailed: true, failControl: ['continueWhenFailed'] } },
        { id: 3, name: 'later failure', status: 'FAILED' }
    ]
    assert.equal(adapter.resolveJobFailureStep(steps, [], job), steps[1])
    assert.equal(adapter.resolveJobFailureStep([
        { id: 1, name: 'unspecified', status: 'FAILED', additionalOptions: { failControl: ['continueWhenFailed'] } }
    ], [], job).name, 'unspecified')
    assert.equal(adapter.resolveJobFailureStep([
        { id: 1, name: 'conflicting settings', status: 'FAILED', additionalOptions: { manualSkip: false, continueWhenFailed: true, failControl: [] } }
    ], [], job).name, 'conflicting settings')
    assert.equal(adapter.resolveJobFailureStep([
        { id: 1, name: 'array representation', status: 'FAILED', additionalOptions: { manualSkip: false, failControl: ['continueWhenFailed'] } }
    ], [], job), null)
    assert.equal(adapter.resolveJobFailureStep([
        { id: 1, name: 'not failed job', status: 'FAILED' }
    ], [], { id: 42, status: 'EXEC_TIMEOUT' }), null)
})

test('Job failure attribution requires evidence for non-FAILED step states and ignores timeout or canceled Job cascades', () => {
    const terminated = { id: 5, name: 'terminated', status: 'TERMINATE', errorCode: 'manual-termination' }
    assert.equal(adapter.resolveJobFailureStep([terminated], [], { id: 42, status: 'FAILED' }), terminated)
    assert.equal(adapter.resolveJobFailureStep([{ id: 5, name: 'no evidence', status: 'TERMINATE' }], [], { id: 42, status: 'FAILED' }), null)
    for (const status of ['EXEC_TIMEOUT', 'HEARTBEAT_TIMEOUT', 'QUALITY_CHECK_FAIL', 'REVIEW_ABORT']) {
        assert.equal(adapter.resolveJobFailureStep([{ id: 5, name: status, status, errorMsg: 'step error' }], [], { id: 42, status: 'FAILED' }).status, status)
        assert.equal(adapter.resolveJobFailureStep([{ id: 5, name: status, status }], [], { id: 42, status: 'FAILED' }), null)
    }
    assert.equal(adapter.resolveJobFailureStep([terminated], [], { id: 42, status: 'EXEC_TIMEOUT' }), null)
})

test('public nodes prefer the actual container name and only third-party nodes expose links and IPs', () => {
    const publicNode = { dockerContainerName: 'docker-host', containerName: 'container-host', dispatchType: { displayName: 'resource spec' }, nodeHashId: 'node-1', hostIp: '10.0.0.1' }
    assert.equal(adapter.nodeDisplay(publicNode), 'docker-host')
    assert.equal(adapter.nodeIp(publicNode), '')
    assert.equal(adapter.nodeLink(publicNode, 'proj'), '')
    const thirdParty = { dispatchType: { buildType: 'THIRD_PARTY_AGENT_NAME' }, nodeName: 'agent', nodeHashId: 'node-2', hostIp: '10.0.0.2' }
    assert.equal(adapter.nodeDisplay(thirdParty), 'agent')
    assert.equal(adapter.nodeIp(thirdParty), '10.0.0.2')
    assert.match(adapter.nodeLink(thirdParty, 'proj'), /nodeHashId=node-2/)
})

test('Job setup failure can suppress stale runtime node display', () => {
    const model = adapter.buildConclusion({ status: 'FAILED' }, { dockerContainerName: 'stale-node' }, { hideNode: true })
    assert.equal(model.node, '')
    assert.equal(model.nodeIp, '')
    assert.equal(model.nodeLink, '')
})

test('Job and Step error summary computed properties do not consume errorInfoList for history or non-failure states', () => {
    const jobStubs = {
        mapGetters: () => ({}),
        jobLog: {}, pluginLog: {}, detailContainer: {}, ContainerContent: {}, LogConclusionBar: {}, JobLogPanel: {}, LogPanelToolbar: {}, MacDebugDialog: {},
        convertMStoString: () => '',
        buildConclusion: adapter.buildConclusion,
        resolveJobQueueDetail: () => null,
        resolveNodeLogError: adapter.resolveNodeLogError,
        resolveJobFailureStep: adapter.resolveJobFailureStep,
        shouldShowLocateLog: adapter.shouldShowLocateLog,
        isSystemAgentTermination: adapter.isSystemAgentTermination,
        isConcurrencyQueueCandidate: () => false,
        isVisibleConcurrencyQueueResponse: () => false,
        canShowJobConclusion: () => false,
        shouldClearQueueAfterError: () => false,
        applyLogPanelDevMock: value => value,
        resolveLogPanelDevMock: () => null,
        RESOURCE_ACTION: {},
        handlePipelineNoPermission: () => {}
    }
    const jobOptions = loadComponentOptions('../../job.vue', jobStubs)
    const errorInfoList = [{ taskId: '', containerId: 42, errorCode: 'new-build', errorMsg: 'current build error' }]
    const getJobSummary = (status, conclusionVisible, currentJob = { id: 42, status }) => jobOptions.computed.jobErrorSummary.call({
        currentJob,
        displayJob: currentJob,
        conclusionVisible,
        displayConclusionAvailable: conclusionVisible,
        isHistoricalExecuteCount: false,
        devLogMock: null,
        execDetail: { errorInfoList },
        displayExecDetail: { errorInfoList }
    })
    assert.equal(getJobSummary('FAILED', false), null)
    assert.equal(getJobSummary('SUCCEED', true), null)
    assert.deepEqual(getJobSummary('FAILED', true), {
        errorType: undefined,
        errorCode: 'new-build',
        errorMsg: 'current build error'
    })
    assert.deepEqual(getJobSummary('FAILED', true, { id: 42, status: 'FAILED', errorCode: 'own-code', errorMsg: 'own message' }), {
        errorType: undefined,
        errorCode: 'own-code',
        errorMsg: 'own message'
    })
    const currentJobWithOwnError = { id: 42, status: 'SUCCEED', errorCode: 'job-own-code', errorMsg: 'own success record' }
    const conclusion = jobOptions.computed.logConclusionModel.call({
        currentJob: currentJobWithOwnError,
        displayJob: currentJobWithOwnError,
        jobErrorSummary: getJobSummary('SUCCEED', true, currentJobWithOwnError),
        isLatestBuild: true,
        conclusionVisible: true,
        displayConclusionAvailable: true,
        isHistoricalExecuteCount: false,
        execDetail: { id: 'build', status: 'SUCCEED', cancelUserId: '', errorInfoList },
        displayExecDetail: { id: 'build', status: 'SUCCEED', cancelUserId: '', errorInfoList },
        $route: { params: { projectId: 'project' } },
        elapsedNow: 0,
        queueDetailModel: null,
        devLogMock: null,
        realLogFallback: false,
        logIssuesAvailable: false,
        $t: key => key
    })
    assert.equal(conclusion.errorCode, 'job-own-code')
    assert.equal(conclusion.message, 'own success record')

    const pluginStubs = {
        mapActions: () => ({}), mapState: () => ({}),
        detailContainer: {}, ReferenceVariable: {}, pluginLog: {}, AtomContent: {}, ProgressDetailPanel: {}, StepLogPanel: {}, LogPanelToolbar: {}, LogConclusionBar: {}, LogParamsView: {},
        convertMStoString: () => '', resolveReviewUsers,
        positionCode: adapter.positionCode, shouldShowProgressTab: () => false,
        buildConclusion: adapter.buildConclusion, shouldLoadStepLog: () => false,
        applyProgressMockTransition: () => ({}), buildParamsRecordRequest: () => ({}), buildParamsModel: () => ({}),
        hasCustomRunCondition: () => false, parseTaskProgressPercent: () => null, parseTaskProgressSubtasks: () => [],
        resolveDisplayProgressPercent: () => null, resolveParamsElementSource: () => ({}), shouldApplyProgressResponse: () => false,
        shouldFetchParamsRecord: () => false, shouldFetchTaskProgress: () => false, shouldScheduleProgressPoll: () => false,
        resolveNodeLogError: adapter.resolveNodeLogError, isJobTimeoutTerminatedStep: adapter.isJobTimeoutTerminatedStep,
        isSystemAgentTermination: adapter.isSystemAgentTermination, shouldShowLocateLog: () => false,
        resolveConclusionDisplayMessage: () => '', applyLogPanelDevMock: value => value,
        resolveLogPanelDevMock: () => null, PROCESS_API_URL_PREFIX: '', RESOURCE_ACTION: {}, handlePipelineNoPermission: () => {}
    }
    const pluginOptions = loadComponentOptions('../../plugin.vue', pluginStubs)
    const getPluginSummary = (status, conclusionVisible) => pluginOptions.computed.currentNodeError.call({
        currentElement: { id: 7, status },
        displayElement: { id: 7, status },
        container: { id: 42 },
        displayContainer: { id: 42 },
        conclusionVisible,
        displayConclusionAvailable: conclusionVisible,
        isHistoricalExecuteCount: false,
        devLogMock: null,
        execDetail: { errorInfoList: [{ taskId: 7, containerId: 42, errorCode: 'step-code', errorMsg: 'step error' }] },
        displayExecDetail: { errorInfoList: [{ taskId: 7, containerId: 42, errorCode: 'step-code', errorMsg: 'step error' }] }
    })
    assert.equal(getPluginSummary('FAILED', false), null)
    assert.equal(getPluginSummary('SUCCEED', true), null)
    assert.deepEqual(getPluginSummary('FAILED', true), {
        errorType: undefined,
        errorCode: 'step-code',
        errorMsg: 'step error'
    })
})

test('Job locate target is built from its current own cause or its failed step, never an arbitrary error', () => {
    const source = readFileSync(new URL('../../job.vue', import.meta.url), 'utf8')
    assert.match(source, /locateItem\?\.\(itemId, messages\)/)
    assert.match(source, /@locate-miss="handleLocateMiss"/)
    const dependencies = {
        mapGetters: () => ({}), jobLog: {}, pluginLog: {}, detailContainer: {}, ContainerContent: {}, LogConclusionBar: {}, JobLogPanel: {}, LogPanelToolbar: {}, MacDebugDialog: {},
        convertMStoString: () => '', buildConclusion: adapter.buildConclusion, resolveJobQueueDetail: () => null,
        resolveNodeLogError: adapter.resolveNodeLogError, resolveJobFailureStep: adapter.resolveJobFailureStep,
        shouldShowLocateLog: adapter.shouldShowLocateLog,
        isSystemAgentTermination: adapter.isSystemAgentTermination, isConcurrencyQueueCandidate: () => false,
        isVisibleConcurrencyQueueResponse: () => false, canShowJobConclusion: () => false, shouldClearQueueAfterError: () => false,
        applyLogPanelDevMock: value => value, resolveLogPanelDevMock: () => null, RESOURCE_ACTION: {}, handlePipelineNoPermission: () => {}
    }
    const options = loadComponentOptions('../../job.vue', dependencies)
    const selectedStep = { id: 9, name: '上报为空', status: 'FAILED' }
    const common = {
        conclusionVisible: true,
        displayConclusionAvailable: true,
        isLatestBuild: true,
        currentJob: { id: 42, status: 'FAILED', elements: [selectedStep] },
        displayJob: { id: 42, status: 'FAILED', elements: [selectedStep] },
        isHistoricalExecuteCount: false,
        jobErrorSummary: null,
        jobFailureStep: selectedStep,
        execDetail: { errorInfoList: [
            { taskId: 8, containerId: 42, errorCode: 'other', errorMsg: 'different step' },
            { taskId: 9, containerId: 42, errorCode: '2199011', errorMsg: 'Script command execution failed: report empty' },
            { taskId: 9, containerId: 99, errorCode: 'historic', errorMsg: 'another job' }
        ] },
        displayExecDetail: { errorInfoList: [
            { taskId: 8, containerId: 42, errorCode: 'other', errorMsg: 'different step' },
            { taskId: 9, containerId: 42, errorCode: '2199011', errorMsg: 'Script command execution failed: report empty' },
            { taskId: 9, containerId: 99, errorCode: 'historic', errorMsg: 'another job' }
        ] }
    }
    assert.deepEqual(options.computed.locateMessages.call(common), ['2199011\nScript command execution failed: report empty'])
    assert.deepEqual(options.computed.locateMessages.call({
        ...common, jobErrorSummary: { errorCode: 'JOB-OWN', errorMsg: 'Job setup failed' }
    }), ['JOB-OWN\nJob setup failed'])
    assert.deepEqual(options.computed.locateMessages.call({ ...common, conclusionVisible: false }), [])
    assert.deepEqual(options.computed.locateMessages.call({ ...common, isLatestBuild: false }), ['2199011\nScript command execution failed: report empty'])
    assert.deepEqual(options.computed.locateMessages.call({ ...common, jobFailureStep: null }), [])
})

test('Job conclusion exposes locate when a danger status has a failure summary', () => {
    const dependencies = {
        mapGetters: () => ({}), jobLog: {}, pluginLog: {}, detailContainer: {}, ContainerContent: {}, LogConclusionBar: {}, JobLogPanel: {}, LogPanelToolbar: {}, MacDebugDialog: {},
        convertMStoString: () => '', buildConclusion: adapter.buildConclusion, resolveJobQueueDetail: () => null,
        resolveNodeLogError: adapter.resolveNodeLogError, resolveJobFailureStep: adapter.resolveJobFailureStep,
        shouldShowLocateLog: adapter.shouldShowLocateLog,
        isSystemAgentTermination: adapter.isSystemAgentTermination, isConcurrencyQueueCandidate: () => false,
        isVisibleConcurrencyQueueResponse: () => false, canShowJobConclusion: () => false, shouldClearQueueAfterError: () => false,
        applyLogPanelDevMock: value => value, resolveLogPanelDevMock: () => null, RESOURCE_ACTION: {}, handlePipelineNoPermission: () => {}
    }
    const options = loadComponentOptions('../../job.vue', dependencies)
    const model = ({ status = 'FAILED', issues = true, latest = true, errorCode = 'job-error', errorMsg = 'trusted Job failure', history = true, mockLocate = false, failureStep = null } = {}) => {
        const currentJob = { id: 42, status, errorCode, errorMsg, nodeName: 'runner' }
        const execDetail = { id: 'build', status, cancelUserId: '', errorInfoList: [] }
        const summary = options.computed.jobErrorSummary.call({ currentJob, displayJob: currentJob, conclusionVisible: history, displayConclusionAvailable: history, isHistoricalExecuteCount: false, devLogMock: null, execDetail, displayExecDetail: execDetail })
        const vm = {
            currentJob, displayJob: currentJob, execDetail, displayExecDetail: execDetail, displayConclusionAvailable: history, isHistoricalExecuteCount: false, jobErrorSummary: summary, jobFailureStep: failureStep,
            locateMessages: options.computed.locateMessages.call({ currentJob, displayJob: currentJob, displayExecDetail: execDetail, displayConclusionAvailable: history, isHistoricalExecuteCount: false, jobErrorSummary: summary, jobFailureStep: failureStep, conclusionVisible: history, isLatestBuild: latest, execDetail, devLogMock: null }),
            isLatestBuild: latest, conclusionVisible: history, elapsedNow: 0, queueDetailModel: null,
            devLogMock: mockLocate ? { locateLog: true } : null, realLogFallback: false, logIssuesAvailable: issues,
            $route: { params: { projectId: 'project' } }, $t: key => key
        }
        return options.computed.logConclusionModel.call(vm)
    }
    assert.equal(model().locateLog, true)
    assert.equal(model({ status: 'RUNNING' }).locateLog, false)
    assert.equal(model({ status: 'SUCCEED' }).locateLog, false)
    assert.equal(model({ status: 'FAILED', errorCode: '', errorMsg: '' }).locateLog, false)
    assert.equal(model({ latest: false }).locateLog, true)
    assert.equal(model({ history: false }).locateLog, false)
    assert.equal(model({ issues: false }).locateLog, true)
    assert.equal(model().locateLog, true, 'Job conclusion exposes locate with a matching Job error summary')
    assert.equal(model({ mockLocate: true }).locateLog, true, 'development mock does not hide the Job locate action')
    assert.equal(model({ errorCode: '', errorMsg: '', failureStep: { name: 'failed step' } }).showNodeMessageDivider, true)
    assert.equal(adapter.shouldShowLocateLog('FAILED', true), true, 'plugin locate behavior remains available')
})

test('Job keeps the debug action out of the config ref path and gates it to the latest eligible execution', () => {
    const source = readFileSync(new URL('../../job.vue', import.meta.url), 'utf8')
    const dependencies = {
        mapGetters: () => ({}), jobLog: {}, pluginLog: {}, detailContainer: {}, ContainerContent: {},
        LogConclusionBar: {}, JobLogPanel: {}, LogPanelToolbar: {}, MacDebugDialog: {},
        convertMStoString: () => '', buildConclusion: adapter.buildConclusion, resolveJobQueueDetail: () => null,
        resolveNodeLogError: adapter.resolveNodeLogError, resolveJobFailureStep: adapter.resolveJobFailureStep,
        isSystemAgentTermination: adapter.isSystemAgentTermination, isConcurrencyQueueCandidate: () => false,
        isVisibleConcurrencyQueueResponse: () => false, canShowJobConclusion: () => false, shouldClearQueueAfterError: () => false,
        applyLogPanelDevMock: value => value, resolveLogPanelDevMock: () => null, RESOURCE_ACTION: {}, handlePipelineNoPermission: () => {},
        checkShowDebugDockerBtn: () => true
    }
    const options = loadComponentOptions('../../job.vue', dependencies, { WEB_URL_PREFIX: '/console' })
    const eligibility = latest => options.computed.showDebugDockerBtn.call({
        checkShowDebugDockerBtn: () => true,
        currentJob: { id: 12 }, $route: { name: 'pipelinesDetail' }, execDetail: {},
        isLatestBuild: latest, activeExecuteCount: 3, latestExecuteCount: 3
    })
    assert.equal(eligibility(true), true)
    assert.equal(eligibility(false), false)
    assert.equal(options.computed.showDebugDockerBtn.call({
        checkShowDebugDockerBtn: () => true, currentJob: {}, $route: { name: 'pipelinesDetail' }, execDetail: {},
        isLatestBuild: true, activeExecuteCount: 2, latestExecuteCount: 3
    }), false)
    assert.match(source, /<template v-slot:right-actions>[\s\S]*v-if="showDebugDockerBtn"[\s\S]*@click\.stop="handleDebug"/)
    assert.match(source, /handleDebug\s*\(\)\s*\{[\s\S]*?\$refs\.container\?\.startDebug\?\.\(\)/)
    assert.match(source, /locateLog:\s*shouldShowLocateLog\(effectiveStatus\)[\s\S]*Boolean\(this\.jobFailureStep \|\| this\.jobErrorSummary \|\| this\.setupFailed\)/)
    const bar = readFileSync(new URL('../LogConclusionBar.vue', import.meta.url), 'utf8')
    assert.match(bar, /<div class="lp-status-actions">\s*<slot name="right-actions"><\/slot>/)
    assert.match(bar, /\.lp-status-actions ::v-deep \.lp-debug-action\.is-outline\s*\{[^}]*height:\s*24px;[^}]*padding:\s*0 12px;[^}]*background:\s*rgba\(107, 71, 71, 0\.4\);[^}]*border:\s*1px solid #979ba5;[^}]*color:\s*#c4c6cc/)
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    assert.doesNotMatch(plugin, /v-slot:right-actions/)
})

test('LogPanelToolbar wrap toggle preserves disabled behavior and is reflected by prototype-state classes', () => {
    const dependencies = { formatLevelSummary: adapter.formatLevelSummary, normalizeLogLevels: adapter.normalizeLogLevels }
    const options = loadComponentOptions('../LogPanelToolbar.vue', dependencies)
    const controls = { wrap: true }
    options.methods.toggleWrap.call({ disabled: false, controls })
    assert.equal(controls.wrap, false)
    options.methods.toggleWrap.call({ disabled: true, controls })
    assert.equal(controls.wrap, false)
    const source = readFileSync(new URL('../LogPanelToolbar.vue', import.meta.url), 'utf8')
    assert.match(source, /class="toolbar-action is-wrap-toggle"\s+:class="\{ 'is-active': controls\.wrap \}"[\s\S]*:aria-pressed="String\(controls\.wrap\)"\s+@click="toggleWrap"/)
})

test('Step location prefers summary matches, then the last ERROR, then the first WARN', () => {
    const source = readFileSync(new URL('../StepLogPanel.vue', import.meta.url), 'utf8')
    const dependencies = Object.fromEntries([
        'mapActions', 'markRaw', 'shallowRef', 'LogGroupBar', 'LogLinesView', 'findLogMatchIds', 'formatLogTime', 'getLogIssueLevel',
        'normalizeLogLevels', 'isPermissionError', 'latestDisplayBase', 'normalizeLogPanelResponse', 'serializeLogLevels', 'shouldBackfillSmallLog',
        'shouldApplyLogSearchScroll', 'shouldContinueSearchBackfill', 'resolveCleanResponseMessage', 'resolveCleanStatusFallbackKey', 'resolveCleanStatusText',
        'resolveDirectJobDownloadUrl', 'resolveLocateLogTarget', 'resolveLogHighlightState', 'resolveNextLogMatchIndex',
        'shouldContinueAutoBackfillAfterBefore', 'resolveLogLoadErrorKey', 'shouldReloadOnLevelsChange', 'shouldRetryBeforeLoad', 'shouldScheduleBeforeRetry',
        'shouldShowStatusEmptyLine', 'shouldUseDirectJobDownload', 'withDisplayLineNumbers', 'countNewLogPanelLines', 'mergeLogPanelLines', 'nextAfterCursor',
        'createLogPanelFormatStressLogs', 'createLogPanelSearchStressLogs', 'filterLogPanelDevMockLogs', 'buildLogStructure', 'isLogStructureFilterEligible',
        'projectLogStructure', 'resolveLogGroupAncestors'
    ].map(name => [
        name,
        name === 'resolveLocateLogTarget'
            ? adapter.resolveLocateLogTarget
            : name === 'getLogIssueLevel' ? adapter.getLogIssueLevel : (() => {})
    ]))
    const options = loadComponentOptions('../StepLogPanel.vue', dependencies)
    const blank = { lineNo: 'real-log-175', level: 'ERROR', text: '' }
    const target = { lineNo: 'real-log-177', level: 'ERROR', text: '2199011 Script command execution failed: report empty' }
    const events = []
    const vm = {
        locateTarget: null, logContext: '', logs: [blank, target], $emit: event => events.push(event),
        locate: line => events.push(line.lineNo)
    }
    assert.equal(options.methods.locateFirstIssue.call(vm, ['2199011\nScript command execution failed: report empty']), true)
    assert.deepEqual(events, ['real-log-177'])
    events.length = 0
    assert.equal(options.methods.locateFirstIssue.call(vm, ['missing distinctive message']), true)
    assert.deepEqual(events, ['real-log-177'])
    events.length = 0
    assert.equal(options.methods.locateFirstIssue.call(vm), true)
    assert.deepEqual(events, ['real-log-177'])
    assert.match(source, /const hasTarget = resolveLocateLogTarget\(\[\], targetMessages\)\.hasTarget/)
})

test('JobLogPanel forwards the current Job target to whichever Step log is selected', () => {
    const dependencies = {
        StepLogPanel: {}, LogStatus: {}, buildLogTarget: adapter.buildLogTarget, hasLogTarget: adapter.hasLogTarget,
        isLiveElapsedStatus: adapter.isLiveElapsedStatus, resolveConclusionElapsed: adapter.resolveConclusionElapsed,
        resolveDirectJobDownloadUrl: adapter.resolveDirectJobDownloadUrl, shouldLoadStepLog: adapter.shouldLoadStepLog
    }
    const options = loadComponentOptions('../JobLogPanel.vue', dependencies)
    const selectedLogs = ['prepare-job', 'failed-step']
    const calls = []
    const vm = { $refs: { panel: { locateFirstIssue: messages => calls.push({ selected: selectedLogs.at(-1), messages }) } } }
    const target = ['2199011\nScript command execution failed: report empty']
    options.methods.locateFirstIssue.call(vm, target)
    selectedLogs.push('selected-plugin-log')
    options.methods.locateFirstIssue.call(vm, target)
    assert.deepEqual(calls, [
        { selected: 'failed-step', messages: target },
        { selected: 'selected-plugin-log', messages: target }
    ])
    const source = readFileSync(new URL('../JobLogPanel.vue', import.meta.url), 'utf8')
    assert.match(source, /locateItem \(itemId, messages\) \{[\s\S]*this\.pendingLocate/)
    assert.match(source, /handleLogsLoaded \(\) \{[\s\S]*locateFirstIssue/)
})

test('JobLogPanel keeps a locate request until the Step log is ready', async () => {
    const dependencies = {
        StepLogPanel: {}, LogStatus: {}, buildLogTarget: adapter.buildLogTarget, hasLogTarget: adapter.hasLogTarget,
        isLiveElapsedStatus: adapter.isLiveElapsedStatus, resolveConclusionElapsed: adapter.resolveConclusionElapsed,
        resolveDirectJobDownloadUrl: adapter.resolveDirectJobDownloadUrl, shouldLoadStepLog: adapter.shouldLoadStepLog
    }
    const options = loadComponentOptions('../JobLogPanel.vue', dependencies)
    const panel = {
        loadingInitial: true,
        locateCalls: 0,
        locateFirstIssue () {
            if (this.loadingInitial) return false
            this.locateCalls += 1
            return true
        }
    }
    const vm = new Vue({
        data: () => ({
            selectedId: 'failed-step',
            contextKey: 'failed-step|job-1',
            loadedContextKey: '',
            pendingLocate: { itemId: 'failed-step', messages: ['step failed'] }
        }),
        methods: {
            ...options.methods,
            locateFirstIssue (...args) {
                return panel.locateFirstIssue(...args)
            }
        }
    }).$mount()
    vm.$refs.panel = panel

    vm.handleLogsLoaded()
    assert.ok(vm.pendingLocate, 'the request remains pending while the log view is loading')
    vm.$nextTick(() => {
        panel.loadingInitial = false
    })
    await vm.$nextTick()
    assert.equal(panel.locateCalls, 1, 'the request is retried after the log view is ready')
    assert.equal(vm.pendingLocate, null, 'the request is cleared after locating')
})

test('selected Errors row keeps its action group visible while hover and keyboard focus remain supported', () => {
    const source = readFileSync(new URL('../../../ExecPipeline.vue', import.meta.url), 'utf8')
    assert.match(source, /class="build-error-cell"\s+:class="\{ 'is-active': isActiveErrorAtom\(props\.row\) \}"/)
    assert.match(source, /&\.is-active \.build-error-actions\s*\{\s*opacity:\s*1/)
    assert.match(source, /&:hover \.build-error-actions,[\s\S]*&:active \.build-error-actions,[\s\S]*&:focus-within \.build-error-actions/)
})

test('condition-skipped steps are counted when a run condition prevented execution', () => {
    const skipped = {
        status: 'SKIP',
        additionalOptions: { runCondition: 'CUSTOM' }
    }
    assert.equal(adapter.isConditionSkipped(skipped), true)
    assert.equal(adapter.isConditionSkipped({ ...skipped, startTime: '2026-11-05T04:00:00.000Z' }), false)
    assert.equal(adapter.isConditionSkipped({ status: 'SKIP' }), false)
    assert.equal(adapter.isConditionSkipped({ status: 'UNEXEC', additionalOptions: { runCondition: 'CUSTOM' } }), false)
    assert.equal(adapter.countConditionSkippedSteps([
        skipped,
        { status: 'SKIP', additionalOptions: { runCondition: 'PRE_TASK_SUCCESS' } },
        { status: 'SUCCEED', additionalOptions: { runCondition: 'CUSTOM' } }
    ]), 2)
})

test('condition-skipped steps with a default run condition are included in the job summary', () => {
    const skipped = {
        status: 'SKIP',
        additionalOptions: { runCondition: 'PRE_TASK_SUCCESS' },
        startEpoch: 0,
        startTime: 0
    }
    assert.equal(adapter.isConditionSkipped(skipped), true)
    assert.equal(adapter.countConditionSkippedSteps([skipped]), 1)
})

test('condition skip summary preserves the concrete backend reason from the warning log', () => {
    const reason = '检查插件运行条件:\n[只有前面有任务失败时才运行] skip=true'
    assert.equal(adapter.resolveConditionSkipReason([
        { level: 'INFO', text: 'plugin output' },
        { level: 'WARN', text: reason }
    ]), reason)
    assert.equal(adapter.resolveConditionSkipReason([
        { level: 'WARN', text: '检查插件运行条件:\n[只有前面有任务失败时才运行] skip=false' }
    ]), '')
    const finalStageReason = '检查Job运行条件:\nFinal stage Job skip when previous stage status is FAILED'
    assert.equal(adapter.resolveConditionSkipReason([{ level: 'WARN', text: finalStageReason }]), finalStageReason)
})

test('Job skipped by a run condition exposes a conclusion summary', () => {
    assert.equal(adapter.isJobConditionSkipped({
        status: 'SKIP',
        jobControlOption: { runCondition: 'CUSTOM_CONDITION_MATCH' },
        elements: [{ status: 'SKIP' }]
    }), true)
    assert.equal(adapter.isJobConditionSkipped({
        status: 'SKIP',
        jobControlOption: { runCondition: 'STAGE_RUNNING' },
        elements: [{ status: 'SKIP' }]
    }), true)
    assert.equal(adapter.isJobConditionSkipped({
        status: 'SKIP'
    }), true)
    assert.equal(adapter.isJobConditionSkipped({
        status: 'SKIP',
        jobControlOption: { enable: false }
    }), false)
    assert.equal(adapter.isJobConditionSkipped({
        status: 'SUCCEED',
        jobControlOption: { runCondition: 'CUSTOM_CONDITION_MATCH' }
    }), false)

    const jobOptions = loadComponentOptions('../../job.vue', {
        mapGetters: () => ({}),
        pluginLog: {},
        detailContainer: {},
        ContainerContent: {},
        LogConclusionBar: {},
        JobLogPanel: {},
        LogPanelToolbar: {},
        MacDebugDialog: {},
        LogExecuteCountSelect: {},
        buildConclusion: adapter.buildConclusion,
        isJobExecTimeoutCancel: adapter.isJobExecTimeoutCancel,
        isSystemAgentTermination: adapter.isSystemAgentTermination,
        resolveJobQueueDetail: () => null,
        shouldShowLocateLog: adapter.shouldShowLocateLog
    })
    const conclusion = jobOptions.computed.logConclusionModel.call({
        displayJob: {
            id: 'job-1',
            status: 'SKIP',
            elements: [],
            timeCost: {}
        },
        displayExecDetail: { errorInfoList: [], cancelUserId: '' },
        jobErrorSummary: null,
        jobFailureStep: null,
        isHistoricalExecuteCount: false,
        conclusionVisible: true,
        queueDetailModel: null,
        devLogMock: null,
        elapsedNow: 0,
        actionsEnabled: true,
        retryableStep: null,
        conditionSkipReason: '检查 Job 运行条件：前置阶段未成功，skip=true',
        $route: { params: { projectId: 'project-1' }, query: {} },
        $t: key => key
    })
    assert.equal(conclusion.conditionSkipSummary, '检查 Job 运行条件：前置阶段未成功，skip=true')
    assert.equal(conclusion.statusMessageKey, '')

    const job = readFileSync(new URL('../../job.vue', import.meta.url), 'utf8')
    assert.match(job, /const conditionSkipped = isJobConditionSkipped\(job\)/)
    assert.match(job, /conditionSkipped\s*\?\s*this\.conditionSkipReason \|\| this\.\$t\('logPanel\.status\.conditionSkipped'\)/)
})

test('cancel conclusions apply only to matching build-end positions and build parent links', () => {
    const base = {
        endCategory: 'CANCEL',
        endType: 'CANCEL_USER',
        operator: 'alice',
        positions: [{ containerId: 'job-1', taskId: 'task-1' }]
    }
    assert.deepEqual(adapter.resolveCancelConclusion(base, { containerId: 'job-1', taskId: 'task-1', webUrlPrefix: '/console' }), {
        type: 'user',
        operator: 'alice',
        reason: '',
        reasonCode: '',
        parentPipelineInfo: {},
        parentBuildUrl: ''
    })
    assert.equal(adapter.resolveCancelConclusion(base, { containerId: 'job-2', taskId: 'task-1' }), null)
    assert.equal(adapter.resolveCancelConclusion(base, { containerId: '', taskId: 'task-1' }), null)
    assert.equal(adapter.resolveCancelConclusion({
        ...base,
        endType: 'CANCEL_SYSTEM',
        reason: 'maintenance'
    }, { containerId: 'job-1', taskId: 'task-1' }).reason, 'maintenance')
    assert.equal(adapter.resolveCancelConclusion({
        ...base,
        endType: 'CANCEL_PARENT_PIPELINE',
        parentPipelineInfo: { projectId: 'p', pipelineId: 'pipe', buildId: 'build' }
    }, { containerId: 'job-1', taskId: 'task-1', webUrlPrefix: '/console' }).parentBuildUrl, '/console/pipeline/p/pipe/detail/build')
})

test('build cancellations also cover canceled or never-started components without their own position', () => {
    const system = { endCategory: 'CANCEL', endType: 'CANCEL_SYSTEM', reason: '排队已满，本次构建被自动取消' }
    const withPositions = { ...system, positions: [{ containerId: 'job-1', taskId: 'task-1' }] }

    assert.equal(adapter.resolveCancelConclusion(withPositions, { containerId: 'job-2', taskId: 'task-9', status: 'CANCELED' }).type, 'system')
    assert.equal(adapter.resolveCancelConclusion(withPositions, { containerId: 'job-2', status: 'CANCELED' }).reason, system.reason)
    assert.equal(adapter.resolveCancelConclusion(withPositions, { containerId: 'job-2', taskId: 'task-9', status: 'UNEXEC' }), null)
    assert.equal(adapter.resolveCancelConclusion(withPositions, { containerId: 'job-2', taskId: 'task-9', status: 'SUCCEED' }), null)
    assert.equal(adapter.resolveCancelConclusion(withPositions, { containerId: 'job-2', taskId: 'task-9' }), null)

    const queued = { ...system, positions: null }
    assert.equal(adapter.resolveCancelConclusion(queued, { containerId: 'job-1', taskId: 'task-1', status: '' }).type, 'system')
    assert.equal(adapter.resolveCancelConclusion(queued, { containerId: 'job-1', taskId: 'task-1', status: 'UNEXEC' }).type, 'system')
    assert.equal(adapter.resolveCancelConclusion(queued, { containerId: 'job-1', status: 'QUEUE' }).type, 'system')
    assert.equal(adapter.resolveCancelConclusion(queued, { containerId: 'job-1', taskId: 'task-1', status: 'SUCCEED' }), null)
    assert.equal(adapter.resolveCancelConclusion(queued, { containerId: 'job-1', taskId: 'task-1' }), null)

    assert.equal(adapter.resolveCancelConclusion({
        ...withPositions,
        reasonCode: 'bkBuildCancelSystemJobExecTimeout'
    }, { containerId: 'job-2', taskId: 'task-9', status: 'CANCELED' }), null)
    assert.equal(adapter.resolveCancelConclusion({ ...queued, endCategory: 'FAIL' }, { containerId: 'job-1', status: '' }), null)
})

test('cancel display differentiates user, system, parent, and Job timeout cancellations', () => {
    const translate = (key, values) => `${key}:${values?.join('|') || ''}`
    const user = adapter.resolveCancelDisplay({
        type: 'user',
        operator: 'alice',
        reason: '用户手动取消构建',
        parentBuildUrl: ''
    }, {}, translate)
    assert.deepEqual(user, {
        label: 'logPanel.status.userCanceled:',
        message: '用户手动取消构建',
        cancelUserId: 'alice',
        parentBuildUrl: ''
    })

    const userWithoutReason = adapter.resolveCancelDisplay({
        type: 'user',
        operator: 'alice',
        reason: '',
        parentBuildUrl: ''
    }, {}, translate)
    assert.equal(userWithoutReason.message, '')

    assert.equal(adapter.resolveCancelDisplay({
        type: 'system',
        operator: '',
        reason: '系统因排队上限取消构建',
        parentBuildUrl: ''
    }, {}, translate).message, '系统因排队上限取消构建')
    assert.equal(adapter.resolveCancelDisplay({
        type: 'system',
        operator: '',
        reason: '',
        parentBuildUrl: ''
    }, {}, translate).message, 'logPanel.status.systemCanceledFallback:')

    const parent = adapter.resolveCancelDisplay({
        type: 'parent',
        operator: '',
        reason: '',
        parentPipelineInfo: { pipelineName: 'Parent', buildNum: 12, operator: 'bob' },
        parentBuildUrl: '/parent'
    }, {}, translate)
    assert.equal(parent.label, 'logPanel.status.parentCanceled:')
    assert.match(parent.message, /Parent\|12\|bob/)
    assert.equal(parent.parentBuildUrl, '/parent')
    assert.equal(adapter.resolveCancelDisplay({
        type: 'parent',
        operator: '',
        reason: '',
        parentPipelineInfo: {},
        parentBuildUrl: ''
    }, {}, translate).message, 'logPanel.queue.parentCanceled:')

    assert.equal(adapter.isJobExecTimeoutCancel({
        endCategory: 'CANCEL',
        endType: 'CANCEL_SYSTEM',
        reasonCode: 'bkBuildCancelSystemJobExecTimeout'
    }), true)
    assert.equal(adapter.resolveCancelDisplay({
        type: 'system',
        operator: '',
        reason: '所属 Job 超过执行时限 30m，步骤被终止',
        parentBuildUrl: ''
    }, {}, translate).label, 'logPanel.status.systemCanceled:')
})

test('agent-lost Job ends FAILED but is detected as system cancellation from its terminated step', () => {
    const agentLostStep = { id: 'task-2', status: 'TERMINATE', errorType: 'BUILD_MACHINE', errorCode: 2199502 }
    assert.equal(adapter.isJobSystemAgentTermination({
        id: 'job-1',
        status: 'FAILED',
        elements: [{ id: 'task-1', status: 'SUCCEED' }, agentLostStep]
    }), true)
    assert.equal(adapter.isJobSystemAgentTermination({
        id: 'job-1',
        status: 'FAILED',
        elements: [{ id: 'task-2', status: 'TERMINATE' }]
    }, [{ taskId: 'task-2', containerId: 'job-1', errorType: 4, errorCode: 2199502 }]), true)
    assert.equal(adapter.isJobSystemAgentTermination({ id: 'job-1', status: 'TERMINATE', errorType: 4, errorCode: 2199502, elements: [] }), true)
    assert.equal(adapter.isJobSystemAgentTermination({ id: 'job-1', status: 'RUNNING', elements: [agentLostStep] }), false)
    assert.equal(adapter.isJobSystemAgentTermination({
        id: 'job-1',
        status: 'FAILED',
        elements: [{ id: 'task-2', status: 'FAILED', errorType: 'BUILD_MACHINE', errorCode: 2199502 }]
    }), false)
    assert.equal(adapter.isJobSystemAgentTermination({
        id: 'job-1',
        status: 'FAILED',
        elements: [{ id: 'task-2', status: 'TERMINATE', errorType: 'USER', errorCode: 2199005 }]
    }), false)

    const jobOptions = loadComponentOptions('../../job.vue', {
        mapGetters: () => ({}), jobLog: {}, pluginLog: {}, detailContainer: {}, ContainerContent: {},
        LogConclusionBar: {}, JobLogPanel: {}, LogPanelToolbar: {}, MacDebugDialog: {}, LogExecuteCountSelect: {},
        convertMStoString: () => '', buildConclusion: adapter.buildConclusion,
        resolveJobQueueDetail: () => null,
        shouldShowLocateLog: adapter.shouldShowLocateLog,
        canShowJobConclusion: () => true
    })
    const job = { id: 'job-1', containerId: 'job-1', status: 'FAILED', elements: [agentLostStep], timeCost: {} }
    const context = {
        currentJob: job,
        displayJob: job,
        displayExecDetail: {
            errorInfoList: [],
            cancelUserId: '',
            buildEndInfo: { endCategory: 'FAIL', endType: 'FAIL_EXEC', reasonCode: 'bkBuildCancelSystemHeartbeatTimeout', positions: [] }
        },
        jobErrorSummary: null,
        jobFailureStep: null,
        retryableStep: null,
        isHistoricalExecuteCount: false,
        conclusionVisible: true,
        actionsEnabled: true,
        queueDetailModel: null,
        devLogMock: null,
        elapsedNow: 0,
        $route: { params: { projectId: 'project-1' }, query: {} },
        $t: key => key
    }
    const conclusion = jobOptions.computed.logConclusionModel.call(context)
    assert.equal(conclusion.label, 'logPanel.status.systemCanceled')
    assert.equal(conclusion.message, 'logPanel.status.systemCanceledDetail')
    assert.equal(conclusion.tone, 'canceled')

    const failedStep = { id: 'task-1', name: 'Build', status: 'FAILED' }
    const withRealFailure = jobOptions.computed.logConclusionModel.call({
        ...context,
        displayJob: { ...job, elements: [failedStep, agentLostStep] },
        jobFailureStep: failedStep
    })
    assert.notEqual(withRealFailure.label, 'logPanel.status.systemCanceled')
    assert.equal(withRealFailure.tone, 'danger')
})

test('system and parent cancellations never show the recorded canceller', () => {
    assert.equal(adapter.resolveCancelUserId(null, 'alice'), 'alice')
    assert.equal(adapter.resolveCancelUserId({ type: 'user' }, 'alice'), 'alice')
    assert.equal(adapter.resolveCancelUserId({ type: 'system' }, 'System'), '')
    assert.equal(adapter.resolveCancelUserId({ type: 'parent' }, 'bob'), '')

    const buildEndInfo = {
        endCategory: 'CANCEL',
        endType: 'CANCEL_SYSTEM',
        reason: '并发组 g1 有新构建，本次构建被取消',
        positions: [{ containerId: 'job-1', taskId: 'task-1' }]
    }
    const pluginOptions = loadComponentOptions('../../plugin.vue', {
        mapActions: () => ({}), mapState: () => ({}), detailContainer: {}, ReferenceVariable: {}, pluginLog: {},
        AtomContent: {}, ProgressDetailPanel: {}, StepLogPanel: {}, LogPanelToolbar: {}, LogConclusionBar: {},
        LogExecuteCountSelect: {}, LogParamsView: {}, Artifactory: {}, Report: {},
        buildConclusion: adapter.buildConclusion,
        isJobTimeoutTerminatedStep: adapter.isJobTimeoutTerminatedStep,
        isSystemAgentTermination: adapter.isSystemAgentTermination,
        hasCustomRunCondition: adapter.hasCustomRunCondition,
        shouldShowLocateLog: adapter.shouldShowLocateLog
    })
    const pluginConclusion = pluginOptions.computed.logConclusionModel.call({
        displayElement: { id: 'task-1', status: 'CANCELED' },
        displayContainer: { id: 'job-1', name: 'job-1', status: 'CANCELED' },
        displayExecDetail: { errorInfoList: [], cancelUserId: 'System', buildEndInfo },
        currentNodeError: null,
        isHistoricalExecuteCount: false,
        conclusionVisible: true,
        actionsEnabled: true,
        canReview: false,
        devLogMock: null,
        elapsedNow: 0,
        $route: { params: { projectId: 'project-1' }, query: {} },
        $t: key => `t:${key}`
    })
    assert.equal(pluginConclusion.label, 't:logPanel.status.systemCanceled')
    assert.equal(pluginConclusion.message, buildEndInfo.reason)
    assert.equal(pluginConclusion.cancelUserId, '')

    const jobOptions = loadComponentOptions('../../job.vue', {
        mapGetters: () => ({}), jobLog: {}, pluginLog: {}, detailContainer: {}, ContainerContent: {},
        LogConclusionBar: {}, JobLogPanel: {}, LogPanelToolbar: {}, MacDebugDialog: {}, LogExecuteCountSelect: {},
        convertMStoString: () => '', buildConclusion: adapter.buildConclusion,
        resolveJobQueueDetail: () => null,
        shouldShowLocateLog: adapter.shouldShowLocateLog,
        canShowJobConclusion: () => true
    })
    const job = { id: 'job-1', containerId: 'job-1', status: 'CANCELED', elements: [{ id: 'task-1', status: 'CANCELED' }], timeCost: {} }
    const jobConclusion = jobOptions.computed.logConclusionModel.call({
        currentJob: job,
        displayJob: job,
        displayExecDetail: { errorInfoList: [], cancelUserId: 'alice', buildEndInfo },
        jobErrorSummary: null,
        jobFailureStep: null,
        retryableStep: null,
        isHistoricalExecuteCount: false,
        conclusionVisible: true,
        actionsEnabled: true,
        queueDetailModel: null,
        devLogMock: null,
        elapsedNow: 0,
        $route: { params: { projectId: 'project-1' }, query: {} },
        $t: key => `t:${key}`
    })
    assert.equal(jobConclusion.label, 't:logPanel.status.systemCanceled')
    assert.equal(jobConclusion.cancelUserId, '')
})

test('a build system-canceled while queued explains the cancellation on never-started steps and Jobs', () => {
    const buildEndInfo = {
        endCategory: 'CANCEL',
        endType: 'CANCEL_SYSTEM',
        reason: '排队已满，新构建入队导致排队中的构建被自动取消'
    }
    const translate = key => `t:${key}`
    const pluginOptions = loadComponentOptions('../../plugin.vue', {
        mapActions: () => ({}), mapState: () => ({}), detailContainer: {}, ReferenceVariable: {}, pluginLog: {},
        AtomContent: {}, ProgressDetailPanel: {}, StepLogPanel: {}, LogPanelToolbar: {}, LogConclusionBar: {},
        LogExecuteCountSelect: {}, LogParamsView: {}, Artifactory: {}, Report: {},
        buildConclusion: adapter.buildConclusion,
        isJobTimeoutTerminatedStep: adapter.isJobTimeoutTerminatedStep,
        isSystemAgentTermination: adapter.isSystemAgentTermination,
        hasCustomRunCondition: adapter.hasCustomRunCondition,
        shouldShowLocateLog: adapter.shouldShowLocateLog
    })
    const pluginContext = {
        displayElement: { id: 'task-1', status: 'UNEXEC' },
        currentElement: { id: 'task-1', status: 'UNEXEC' },
        displayContainer: { id: 'job-1', name: 'job-1' },
        displayExecDetail: { errorInfoList: [], cancelUserId: 'System', buildEndInfo },
        currentNodeError: null,
        isHistoricalExecuteCount: false,
        conclusionVisible: true,
        actionsEnabled: true,
        canReview: false,
        devLogMock: null,
        elapsedNow: 0,
        $route: { params: { projectId: 'project-1' }, query: {} },
        $t: translate
    }
    const pluginConclusion = pluginOptions.computed.logConclusionModel.call(pluginContext)
    assert.equal(pluginConclusion.label, 't:logPanel.status.systemCanceled')
    assert.equal(pluginConclusion.message, buildEndInfo.reason)
    assert.equal(pluginConclusion.tone, 'canceled')
    assert.equal(pluginConclusion.cancelUserId, '')
    assert.equal(pluginOptions.computed.hasBuildCancelConclusion.call({ ...pluginContext, displayElement: { id: 'task-1' } }), true)

    const pluginWithoutCancel = pluginOptions.computed.logConclusionModel.call({
        ...pluginContext,
        displayExecDetail: { errorInfoList: [], cancelUserId: '', buildEndInfo: null }
    })
    assert.equal(pluginWithoutCancel.label, 't:logPanel.status.notExecutedLabel')

    const jobOptions = loadComponentOptions('../../job.vue', {
        mapGetters: () => ({}), jobLog: {}, pluginLog: {}, detailContainer: {}, ContainerContent: {},
        LogConclusionBar: {}, JobLogPanel: {}, LogPanelToolbar: {}, MacDebugDialog: {}, LogExecuteCountSelect: {},
        convertMStoString: () => '', buildConclusion: adapter.buildConclusion,
        resolveJobQueueDetail: () => null,
        shouldShowLocateLog: adapter.shouldShowLocateLog,
        canShowJobConclusion: adapter.canShowJobConclusion
    })
    const job = { id: 'job-1', containerId: 'job-1', elements: [{ id: 'task-1' }], timeCost: {} }
    assert.equal(jobOptions.computed.canShowConclusion.call({ currentJob: job, queueDetailModel: null, execDetail: { buildEndInfo } }), true)
    assert.equal(jobOptions.computed.canShowConclusion.call({ currentJob: job, queueDetailModel: null, execDetail: { buildEndInfo: null } }), false)
    const jobConclusion = jobOptions.computed.logConclusionModel.call({
        currentJob: job,
        displayJob: job,
        displayExecDetail: { errorInfoList: [], cancelUserId: 'System', buildEndInfo },
        jobErrorSummary: null,
        jobFailureStep: null,
        retryableStep: null,
        isHistoricalExecuteCount: false,
        conclusionVisible: true,
        actionsEnabled: true,
        queueDetailModel: null,
        devLogMock: null,
        elapsedNow: 0,
        $route: { params: { projectId: 'project-1' }, query: {} },
        $t: translate
    })
    assert.equal(jobConclusion.label, 't:logPanel.status.systemCanceled')
    assert.equal(jobConclusion.message, buildEndInfo.reason)
    assert.equal(jobConclusion.tone, 'canceled')
    assert.equal(jobConclusion.cancelUserId, '')
})

test('locate matching prefers error rows and rejects info-only summary fragments', () => {
    const message = 'report publishing failed with exit code 1'
    assert.equal(adapter.resolveLocateLogTarget([
        { lineNo: 1, level: 'INFO', text: message },
        { lineNo: 2, level: 'ERROR', text: message }
    ], [message]).lineNo, 2)
    assert.deepEqual(adapter.resolveLocateLogTarget([
        { lineNo: 3, level: 'INFO', text: 'report publishing failed with exit code' }
    ], [message]), { hasTarget: true, lineNo: null, miss: true })
})

test('toolbar disables log controls', () => {
    const source = readFileSync(new URL('../LogPanelToolbar.vue', import.meta.url), 'utf8')
    assert.match(source, /class="level-picker"[\s\S]*:class="\{ 'is-disabled': disabled \}"/)
    assert.match(source, /class="search-box"[\s\S]*:class="\{ 'is-disabled': disabled \}"/)
})

test('progress subtasks retain a terminal snapshot and expose the expand action without a percentage', () => {
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    const conclusion = readFileSync(new URL('../LogConclusionBar.vue', import.meta.url), 'utf8')
    assert.match(plugin, /progressSubtasksSnapshot/)
    assert.match(plugin, /progressExpandInitKey/)
    assert.match(plugin, /return this\.devLogMock[\s\S]*this\.progressSubtasks\.length[\s\S]*this\.progressSubtasksSnapshot/)
    assert.match(conclusion, /v-if="progress !== null && progress !== undefined \|\| hasSubtasks"/)
    assert.match(conclusion, /v-if="hasSubtasks"/)
})

function loadPluginConclusionOptions () {
    return loadComponentOptions('../../plugin.vue', {
        mapActions: () => ({}), mapState: () => ({}), detailContainer: {}, ReferenceVariable: {}, pluginLog: {},
        AtomContent: {}, ProgressDetailPanel: {}, StepLogPanel: {}, LogPanelToolbar: {}, LogConclusionBar: {},
        LogExecuteCountSelect: {}, LogParamsView: {}, Artifactory: {}, Report: {},
        buildConclusion: adapter.buildConclusion,
        isJobTimeoutTerminatedStep: adapter.isJobTimeoutTerminatedStep,
        isSystemAgentTermination: adapter.isSystemAgentTermination,
        hasCustomRunCondition: adapter.hasCustomRunCondition,
        shouldShowLocateLog: adapter.shouldShowLocateLog
    })
}

function loadJobConclusionOptions () {
    return loadComponentOptions('../../job.vue', {
        mapGetters: () => ({}), jobLog: {}, pluginLog: {}, detailContainer: {}, ContainerContent: {},
        LogConclusionBar: {}, JobLogPanel: {}, LogPanelToolbar: {}, MacDebugDialog: {}, LogExecuteCountSelect: {},
        convertMStoString: () => '', buildConclusion: adapter.buildConclusion,
        resolveJobQueueDetail: () => null,
        isSystemAgentTermination: adapter.isSystemAgentTermination,
        shouldShowLocateLog: adapter.shouldShowLocateLog,
        canShowJobConclusion: () => true
    })
}

function pluginConclusionContext (overrides = {}) {
    return {
        displayElement: { id: 'task-1', status: 'SUCCEED' },
        currentElement: { id: 'task-1', status: 'SUCCEED' },
        displayContainer: { id: 'job-1', name: 'job-1', status: 'SUCCEED' },
        displayExecDetail: { errorInfoList: [], cancelUserId: '', buildEndInfo: null },
        currentNodeError: null,
        isHistoricalExecuteCount: false,
        displayConclusionAvailable: true,
        conclusionVisible: true,
        actionsEnabled: true,
        devLogMock: null,
        shouldMountLog: true,
        elapsedNow: 0,
        $route: { params: { projectId: 'project-1' }, query: {} },
        $t: (key, values) => values?.length ? `${key}:${values.join('|')}` : key,
        ...overrides
    }
}

function jobConclusionContext (job, overrides = {}) {
    return {
        currentJob: job,
        displayJob: job,
        displayExecDetail: { errorInfoList: [], cancelUserId: '', buildEndInfo: null },
        jobErrorSummary: null,
        jobFailureStep: null,
        retryableStep: null,
        isHistoricalExecuteCount: false,
        displayConclusionAvailable: true,
        conclusionVisible: true,
        actionsEnabled: true,
        queueDetailModel: null,
        devLogMock: null,
        elapsedNow: 0,
        $route: { params: { projectId: 'project-1' }, query: {} },
        $t: (key, values) => values?.length ? `${key}:${values.join('|')}` : key,
        ...overrides
    }
}

test('steps of a cleaned build record keep the succeeded conclusion when the build succeeded', () => {
    assert.equal(adapter.resolveRecordElementStatus('', 'SUCCEED'), 'SUCCEED')
    assert.equal(adapter.resolveRecordElementStatus(undefined, 'succeed'), 'SUCCEED')
    assert.equal(adapter.resolveRecordElementStatus('', 'FAILED'), '')
    assert.equal(adapter.resolveRecordElementStatus(undefined, 'RUNNING'), undefined)
    assert.equal(adapter.resolveRecordElementStatus('SKIP', 'SUCCEED'), 'SKIP')

    const options = loadPluginConclusionOptions()
    const cleaned = options.computed.displayElement.call({
        isHistoricalExecuteCount: false,
        currentElement: { id: 'task-1', name: 'Bash' },
        execDetail: { status: 'SUCCEED' }
    })
    assert.equal(cleaned.status, 'SUCCEED')
    assert.equal(cleaned.name, 'Bash')
    const running = { id: 'task-1', name: 'Bash' }
    assert.equal(options.computed.displayElement.call({ isHistoricalExecuteCount: false, currentElement: running, execDetail: { status: 'RUNNING' } }), running)

    const conclusion = options.computed.logConclusionModel.call(pluginConclusionContext({
        displayElement: cleaned,
        $t: key => key === 'details.statusMap.SUCCEED' ? '执行成功' : key
    }))
    assert.equal(conclusion.tone, 'success')
    assert.equal(conclusion.label, '执行成功')
})

test('not-executed steps show a short status label beside the empty-log copy', () => {
    const options = loadPluginConclusionOptions()
    const conclusion = options.computed.logConclusionModel.call(pluginConclusionContext({
        displayElement: { id: 'task-1', status: 'UNEXEC' },
        currentElement: { id: 'task-1', status: 'UNEXEC' }
    }))
    assert.equal(conclusion.label, 'logPanel.status.notExecutedLabel')
    assert.equal(conclusion.statusMessageKey, '')
    for (const [locale, expected] of [['zh-CN', '未执行'], ['en-US', 'Not executed'], ['ja-JP', '未実行']]) {
        const messages = JSON.parse(readFileSync(new URL(`../../../../../../locale/pipeline/${locale}.json`, import.meta.url), 'utf8'))
        assert.equal(messages.logPanel.status.notExecutedLabel, expected)
        assert.ok(messages.logPanel.status.jobSelfTimeout.includes('{0}'), locale)
    }
})

test('canceled conclusions name the user cancellation when the build has no end detail', () => {
    assert.equal(adapter.resolveFallbackCancelLabelKey({ status: 'CANCELED', cancelUserId: 'fayewang' }), 'userCanceled')
    assert.equal(adapter.resolveFallbackCancelLabelKey({ status: 'CANCELED', cancelUserId: 'System' }), '')
    assert.equal(adapter.resolveFallbackCancelLabelKey({ status: 'CANCELED', cancelUserId: '' }), '')
    assert.equal(adapter.resolveFallbackCancelLabelKey({ status: 'TERMINATE', cancelUserId: 'fayewang' }), '')
    assert.equal(adapter.resolveFallbackCancelLabelKey({ status: 'CANCELED', cancelUserId: 'fayewang', buildEndInfo: { endCategory: 'CANCEL' } }), '')

    const pluginConclusion = loadPluginConclusionOptions().computed.logConclusionModel.call(pluginConclusionContext({
        displayElement: { id: 'task-1', status: 'CANCELED' },
        currentElement: { id: 'task-1', status: 'CANCELED' },
        displayExecDetail: { errorInfoList: [], cancelUserId: 'fayewang', buildEndInfo: null }
    }))
    assert.equal(pluginConclusion.label, 'logPanel.status.userCanceled')
    assert.equal(pluginConclusion.cancelUserId, 'fayewang')
    assert.equal(pluginConclusion.tone, 'canceled')

    const job = { id: 'job-1', containerId: 'job-1', status: 'CANCELED', elements: [], timeCost: {} }
    const jobConclusion = loadJobConclusionOptions().computed.logConclusionModel.call(jobConclusionContext(job, {
        displayExecDetail: { errorInfoList: [], cancelUserId: 'fayewang', buildEndInfo: null }
    }))
    assert.equal(jobConclusion.label, 'logPanel.status.userCanceled')
    assert.equal(jobConclusion.cancelUserId, 'fayewang')
})

test('Job terminated by its execution time limit reads as a timeout with the limit', () => {
    const steps = [
        { id: 'setup-ok', status: 'SUCCEED' },
        { id: 'bash', status: 'TERMINATE', errorCode: 2199005 }
    ]
    assert.equal(adapter.resolveJobExecTimeoutCode({ id: 'job-1', status: 'TERMINATE', elements: steps }), 2199005)
    assert.equal(adapter.resolveJobExecTimeoutCode({ id: 'job-1', status: 'FAILED', elements: [] }, [{ containerId: 'job-1', errorCode: '2199005' }]), '2199005')
    assert.equal(adapter.resolveJobExecTimeoutCode({ id: 'job-1', status: 'FAILED', elements: [] }, [{ containerId: 'job-2', errorCode: 2199005 }]), '')
    assert.equal(adapter.resolveJobExecTimeoutCode({ id: 'job-1', status: 'SUCCEED', errorCode: 2199005 }), '')
    assert.equal(adapter.resolveJobExecTimeoutCode({ id: 'job-1', status: 'TERMINATE', elements: [{ status: 'TERMINATE', errorCode: 2199502 }] }), '')

    const job = {
        id: 'job-1',
        containerId: 'job-1',
        name: '构建环境-LINUX',
        status: 'TERMINATE',
        elements: steps,
        jobControlOption: { timeoutVar: '${{ variables.timeout }}', timeout: 15 },
        timeCost: { totalCost: 65000 }
    }
    const conclusion = loadJobConclusionOptions().computed.logConclusionModel.call(jobConclusionContext(job))
    assert.equal(conclusion.status, 'EXEC_TIMEOUT')
    assert.equal(conclusion.tone, 'danger')
    assert.equal(conclusion.label, 'details.statusMap.EXEC_TIMEOUT')
    assert.equal(conclusion.errorCode, 2199005)
    assert.equal(conclusion.message, 'logPanel.status.jobSelfTimeout:15m')
    assert.equal(conclusion.cancelUserId, '')
})

test('review conclusion always offers the solid handle action and leaves permission to the review entry', () => {
    const conclusion = loadPluginConclusionOptions().computed.logConclusionModel.call(pluginConclusionContext({
        displayElement: { id: 'task-1', status: 'REVIEWING' },
        currentElement: { id: 'task-1', status: 'REVIEWING' }
    }))
    assert.equal(conclusion.review, true)
    assert.equal(conclusion.tone, 'waiting')
    const historical = loadPluginConclusionOptions().computed.logConclusionModel.call(pluginConclusionContext({
        displayElement: { id: 'task-1', status: 'REVIEWING' },
        isHistoricalExecuteCount: true,
        conclusionVisible: false
    }))
    assert.equal(historical.review, false)

    const bar = readFileSync(new URL('../LogConclusionBar.vue', import.meta.url), 'utf8')
    assert.match(bar, /\.is-pause \.lp-handle, \.is-waiting \.lp-handle \{ height: 24px;[^}]*padding: 0 12px; border-radius: 4px; border-color: #e18732; background: #e18732; color: #fff; font-size: 12px;/)
})

test('conclusion bar follows the walkthrough palette for success and failure meta copy', () => {
    const bar = readFileSync(new URL('../LogConclusionBar.vue', import.meta.url), 'utf8')
    assert.match(bar, /\.lp-status\.is-success \{ background: #2b3329;/)
    assert.match(bar, /\.is-success \.lp-status-label \{ color: #45e35f; \}/)
    assert.match(bar, /\.is-failed \.lp-meta, \.is-danger \.lp-meta,[^{]*\{ color: #979ba5; \}/)
    assert.ok(bar.indexOf('.is-danger .lp-node-link') < bar.indexOf('.lp-node-link { color: #83828c;'))
    assert.match(bar, /\.is-failed \.lp-status-label, \.is-danger \.lp-status-label, \.is-failed \.lp-msg, \.is-danger \.lp-msg \{ color: #f06e6e; \}/)
    assert.match(bar, /\.lp-progress-track \{ display: block; width: 120px; height: 4px; background: #4d4f56; border-radius: 2px;/)
    assert.match(bar, /\.lp-progress-fill \{ display: block; height: 4px; background: #3a84ff; border-radius: 2px; \}/)
})

test('standalone execute count selector only renders when the conclusion bar is hidden and there are multiple runs', () => {
    const show = adapter.shouldShowStandaloneExecuteCount
    assert.equal(show({ tab: 'log', showConclusion: false, latestCount: 2 }), true)
    assert.equal(show({ tab: 'log', showConclusion: false, latestCount: '3' }), true)
    assert.equal(show({ tab: 'log', showConclusion: true, latestCount: 2 }), false, 'mutually exclusive with the selector inside the conclusion bar')
    assert.equal(show({ tab: 'log', showConclusion: false, latestCount: 1 }), false)
    assert.equal(show({ tab: 'log', showConclusion: false, latestCount: undefined }), false)
    assert.equal(show({ tab: 'setting', showConclusion: false, latestCount: 2 }), false)
    assert.equal(show({ tab: 'log', showConclusion: false, latestCount: 2, matrix: true }), false)
    assert.equal(show(), false)
})

test('Step keeps the execute count selector reachable while a historical run is loading, unavailable, waiting or disabled', () => {
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    assert.equal((plugin.match(/ref="conclusionExecuteCountSelect"/g) || []).length, 2)
    assert.match(plugin, /<log-conclusion-bar\s+v-if="currentTab === 'log' && showConclusion"[\s\S]*?ref="conclusionExecuteCountSelect"[\s\S]*?<\/log-conclusion-bar>/)
    assert.match(plugin, /v-if="showStandaloneExecuteCount \|\| showHistoryRecordState"[\s\S]*?<log-execute-count-select\s+v-if="showStandaloneExecuteCount"\s+ref="conclusionExecuteCountSelect"[\s\S]*?@execute-change="handleExecuteChange"[\s\S]*?<template v-if="showHistoryRecordState">[\s\S]*?@click="fetchHistoricalDetail"/)

    const options = loadComponentOptions('../../plugin.vue', {
        mapActions: () => ({}), mapState: () => ({}), detailContainer: {}, ReferenceVariable: {}, pluginLog: {},
        AtomContent: {}, ProgressDetailPanel: {}, StepLogPanel: {}, LogPanelToolbar: {}, LogConclusionBar: {},
        LogParamsView: {}, Artifactory: {}, Report: {},
        shouldShowStandaloneExecuteCount: adapter.shouldShowStandaloneExecuteCount
    })
    const { showConclusion, showStandaloneExecuteCount, showHistoryRecordState } = options.computed
    const resolve = state => {
        const vm = { currentTab: 'log', currentElement: { executeCount: 2 }, canShowConclusion: true, conclusionVisible: true, ...state }
        vm.showConclusion = showConclusion.call(vm)
        return { conclusion: vm.showConclusion, standalone: showStandaloneExecuteCount.call(vm), history: showHistoryRecordState.call(vm) }
    }
    assert.deepEqual(resolve({ isHistoricalExecuteCount: true, historyRecordState: 'loading' }), { conclusion: false, standalone: true, history: true })
    assert.deepEqual(resolve({ isHistoricalExecuteCount: true, historyRecordState: 'unavailable' }), { conclusion: false, standalone: true, history: true })
    assert.deepEqual(resolve({ isHistoricalExecuteCount: true, historyRecordState: 'ready' }), { conclusion: true, standalone: false, history: false })
    assert.deepEqual(resolve({ isHistoricalExecuteCount: false, historyRecordState: 'idle', canShowConclusion: false }), { conclusion: false, standalone: true, history: false }, 'WAITING / DISABLED steps hide the conclusion bar')
    assert.deepEqual(resolve({ isHistoricalExecuteCount: false, historyRecordState: 'idle', canShowConclusion: false, currentElement: { executeCount: 1 } }), { conclusion: false, standalone: false, history: false })

    const vm = { realLogControls: { search: 'x', current: 1, total: 2 }, currentElement: { executeCount: 2 }, selectedExecuteCount: 1 }
    options.methods.handleExecuteChange.call(vm, 2)
    assert.equal(vm.selectedExecuteCount, null, 'a failed historical load can switch back to the latest run')
})

test('Job keeps the execute count selector reachable while a historical run is loading or unavailable and when it has not started', () => {
    const job = readFileSync(new URL('../../job.vue', import.meta.url), 'utf8')
    assert.equal((job.match(/ref="conclusionExecuteCountSelect"/g) || []).length, 2)
    assert.match(job, /<log-conclusion-bar\s+v-if="currentTab === 'log' && showConclusion"[\s\S]*?ref="conclusionExecuteCountSelect"[\s\S]*?<\/log-conclusion-bar>/)
    assert.match(job, /v-if="showStandaloneExecuteCount \|\| showHistoryRecordState"[\s\S]*?<log-execute-count-select\s+v-if="showStandaloneExecuteCount"\s+ref="conclusionExecuteCountSelect"[\s\S]*?:latest-count="latestExecuteCount"[\s\S]*?@execute-change="handleExecuteChange"[\s\S]*?<template v-if="showHistoryRecordState">[\s\S]*?@click="fetchHistoricalDetail"/)

    const options = loadComponentOptions('../../job.vue', {
        mapGetters: () => ({}), jobLog: {}, pluginLog: {}, detailContainer: {}, ContainerContent: {},
        LogConclusionBar: {}, JobLogPanel: {}, LogPanelToolbar: {}, MacDebugDialog: {},
        canShowJobConclusion: adapter.canShowJobConclusion,
        shouldShowStandaloneExecuteCount: adapter.shouldShowStandaloneExecuteCount
    })
    const { showConclusion, displayConclusionAvailable, displayCanShowConclusion, showStandaloneExecuteCount, showHistoryRecordState } = options.computed
    const resolve = ({ status = 'SUCCEED', historyStatus = 'SUCCEED', matrix = false, latest = 2, ...state }) => {
        const vm = {
            currentTab: 'log',
            currentJob: { status, matrixGroupFlag: matrix },
            displayJob: { status: historyStatus },
            latestExecuteCount: latest,
            conclusionVisible: !state.isHistoricalExecuteCount,
            ...state
        }
        vm.canShowConclusion = adapter.canShowJobConclusion(vm.currentJob.status, null)
        vm.displayConclusionAvailable = displayConclusionAvailable.call(vm)
        vm.displayCanShowConclusion = displayCanShowConclusion.call(vm)
        vm.showConclusion = showConclusion.call(vm)
        return { conclusion: vm.showConclusion, standalone: showStandaloneExecuteCount.call(vm), history: showHistoryRecordState.call(vm) }
    }
    assert.deepEqual(resolve({ status: 'UNEXEC', isHistoricalExecuteCount: true, historyRecordState: 'loading' }), { conclusion: false, standalone: true, history: true })
    assert.deepEqual(resolve({ status: 'UNEXEC', isHistoricalExecuteCount: true, historyRecordState: 'unavailable' }), { conclusion: false, standalone: true, history: true })
    assert.deepEqual(resolve({ status: 'UNEXEC', isHistoricalExecuteCount: true, historyRecordState: 'ready' }), { conclusion: true, standalone: false, history: false })
    assert.deepEqual(resolve({ status: 'UNEXEC', isHistoricalExecuteCount: false, historyRecordState: 'idle' }), { conclusion: false, standalone: true, history: false })
    assert.deepEqual(resolve({ status: 'WAITING', isHistoricalExecuteCount: false, historyRecordState: 'idle' }), { conclusion: false, standalone: true, history: false })
    assert.deepEqual(resolve({ status: 'UNEXEC', isHistoricalExecuteCount: false, historyRecordState: 'idle', latest: 1 }), { conclusion: false, standalone: false, history: false })
    assert.deepEqual(resolve({ status: 'UNEXEC', isHistoricalExecuteCount: false, historyRecordState: 'idle', matrix: true }), { conclusion: false, standalone: false, history: false })

    const vm = { realLogControls: { search: 'x', current: 1, total: 2 }, latestExecuteCount: 2, selectedExecuteCount: 1 }
    options.methods.handleExecuteChange.call(vm, 2)
    assert.equal(vm.selectedExecuteCount, null, 'a failed historical load can switch back to the latest run')
})

test('Job setup failure exposes locate before an error summary exists and locates the setup item', () => {
    const options = loadJobConclusionOptions()
    const setupFailedJob = { id: 'job-1', containerId: 'job-1', status: 'FAILED', startVMStatus: 'FAILED', elements: [{ id: 'task-1', name: 'Bash', status: 'UNEXEC' }] }
    const setupFailed = options.computed.setupFailed.call({ displayJob: setupFailedJob })
    assert.equal(setupFailed, true)
    assert.equal(options.computed.setupFailed.call({ displayJob: { ...setupFailedJob, startVMStatus: 'SUCCEED' } }), false)
    assert.equal(options.computed.setupFailed.call({ displayJob: { ...setupFailedJob, startVMStatus: 'heartbeat_timeout' } }), true)

    const jobFailureStep = options.computed.jobFailureStep.call(jobConclusionContext(setupFailedJob, { setupFailed }))
    assert.equal(jobFailureStep, null)
    const model = options.computed.logConclusionModel.call(jobConclusionContext(setupFailedJob, { setupFailed, jobFailureStep, jobErrorSummary: null }))
    assert.equal(model.locateLog, true, 'locate is available while the build-level error summary has not been generated yet')
    const succeeded = options.computed.logConclusionModel.call(jobConclusionContext({ ...setupFailedJob, status: 'SUCCEED', startVMStatus: 'SUCCEED' }, { setupFailed: false }))
    assert.equal(succeeded.locateLog, false)
    const running = options.computed.logConclusionModel.call(jobConclusionContext({ ...setupFailedJob, status: 'RUNNING' }, { setupFailed }))
    assert.equal(running.locateLog, false, 'setup failure alone does not expose locate on a non-danger status')

    const located = []
    options.methods.handleLocateLog.call({
        setupFailed,
        jobFailureStep: null,
        locateMessages: [],
        displayPluginList: [{ id: 'startVM-job-1', isSetup: true }, { id: 'task-1' }],
        $refs: { realLog: { selectedId: 'task-1', locateItem: (...args) => located.push(args) } }
    })
    assert.deepEqual(located, [['startVM-job-1']])
})
