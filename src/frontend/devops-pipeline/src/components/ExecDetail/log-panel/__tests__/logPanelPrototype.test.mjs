import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import test from 'node:test'
import { readFileSync } from 'node:fs'
import * as adapter from '../logPanelAdapter.js'

const VueCompiler = createRequire(import.meta.url)('vue/dist/vue.common.js')

function loadOptions (relativePath, dependencies = {}) {
    const source = readFileSync(new URL(relativePath, import.meta.url), 'utf8')
    const script = source.match(/<script>([\s\S]*?)<\/script>/)[1]
        .replace(/^\s*import[\s\S]*?\sfrom\s+['"][^'"]+['"];?\s*$/gm, '')
        .replace(/export default\s*\{/, 'return {')
    const values = {
        angleDownIcon: '/mock-angle-down.svg',
        LogExecuteCountSelect: {},
        isConditionSkipped: adapter.isConditionSkipped,
        isJobConditionSkipped: adapter.isJobConditionSkipped,
        countConditionSkippedSteps: adapter.countConditionSkippedSteps,
        resolveCancelConclusion: adapter.resolveCancelConclusion,
        resolveCancelDisplay: adapter.resolveCancelDisplay,
        resolveCancelUserId: adapter.resolveCancelUserId,
        isJobExecTimeoutCancel: adapter.isJobExecTimeoutCancel,
        isJobSystemAgentTermination: adapter.isJobSystemAgentTermination,
        resolveJobExecTimeoutCode: adapter.resolveJobExecTimeoutCode,
        resolveFallbackCancelLabelKey: adapter.resolveFallbackCancelLabelKey,
        resolveRecordElementStatus: adapter.resolveRecordElementStatus,
        ...dependencies
    }
    const names = Object.keys(values)
    return new Function(...names, script)(...names.map(name => values[name]))
}

function renderTemplate (template, data, components) {
    const previousDocument = globalThis.document
    if (!previousDocument) {
        globalThis.document = {
            createElement () {
                return {
                    textContent: '',
                    set innerHTML (value) {
                        this.textContent = value
                    }
                }
            }
        }
    }
    try {
        const compiled = VueCompiler.compile(template)
        assert.deepEqual(compiled.errors || [], [])
        const vm = new VueCompiler({
            data: () => data,
            components,
            methods: { focusExecuteCountAfterKeyboardSelection () {} },
            render: compiled.render,
            staticRenderFns: compiled.staticRenderFns
        })
        return vm._render()
    } finally {
        if (!previousDocument) delete globalThis.document
    }
}

function renderConclusionPrefixSelector (componentSource, data) {
    const match = componentSource.match(/<log-conclusion-bar[\s\S]*?<\/log-conclusion-bar>/)
    assert.ok(match, 'source contains the actual conclusion-bar template')
    const vnode = renderTemplate(`<div>${match[0]}</div>`, data, {
        LogConclusionBar: { render: h => h('section') },
        LogExecuteCountSelect: { name: 'LogExecuteCountSelect', render: h => h('i') }
    }).children[0]
    if (vnode.componentOptions?.tag !== 'log-conclusion-bar') return []
    return vnode.data.scopedSlots['prefix-actions']({})
}

function renderLegacyLogSearch (componentSource, data) {
    const match = componentSource.match(/<bk-log-search[\s\S]*?<\/bk-log-search>/)
    assert.ok(match, 'legacy log template contains its original bk-log-search selector')
    return renderTemplate(`<div>${match[0]}</div>`, data, {
        BkLogSearch: { name: 'BkLogSearch', props: ['executeCount'], render: h => h('div') }
    }).children[0]
}

function renderJobHeaderTools (componentSource, data) {
    const start = componentSource.indexOf('<detail-container')
    const contentStart = componentSource.indexOf('<template v-slot:content>', start)
    assert.ok(start >= 0 && contentStart > start, 'Job template exposes a header before the content slot')
    const header = componentSource.slice(start, contentStart)
        .replace(/\s+@(?:close|execute-change|focus-return|download|move)="[^"]*"/g, '')
        .replace(/\{\{\s*\$t\([^}]+\)\s*\}\}/g, 'tab')
    const root = renderTemplate(`<div>${header}</detail-container></div>`, data, {
        DetailContainer: { render: h => h('div') },
        LogPanelToolbar: { name: 'LogPanelToolbar', render: h => h('div') },
        BkButton: { name: 'BkButton', render: h => h('button') }
    })
    return root.children[0].componentOptions.children || []
}

test('toolbar ALL interaction keeps the effective API levels non-empty and returns through the prototype sequence', () => {
    const toolbar = loadOptions('../LogPanelToolbar.vue', adapter)
    const controls = { levels: ['INFO', 'WARN', 'ERROR', 'DEBUG'], levelAll: false }
    const events = []
    const vm = {
        controls,
        disabled: false,
        $set: (target, key, value) => {
            target[key] = value
        },
        $emit: (...args) => events.push(args)
    }
    Object.defineProperty(vm, 'selectedLevels', { get: () => controls.levels })
    Object.assign(vm, toolbar.methods)

    vm.selectAllLevels()
    assert.equal(controls.levelAll, true)
    assert.deepEqual(controls.levels, ['INFO', 'WARN', 'ERROR', 'DEBUG'])
    assert.equal(events.length, 0, 'changing only ALL presentation does not repeat an equivalent request')
    vm.toggleLevel('ERROR', { target: { checked: true } })
    vm.toggleLevel('WARN', { target: { checked: true } })
    assert.deepEqual(controls.levels, ['WARN', 'ERROR'])
    vm.toggleLevel('ERROR', { target: { checked: false } })
    vm.toggleLevel('WARN', { target: { checked: false } })
    assert.equal(controls.levelAll, true, 'removing the final concrete level returns to ALL')
    assert.equal(events.length, 4)
})

test('execution-count selector offers ascending numeric options, closes on selection, and restores focus', () => {
    const selector = loadOptions('../LogExecuteCountSelect.vue', { angleDownIcon: '/mock-angle-down.svg' })
    const emitted = []
    let triggerFocused = 0
    const vm = {
        latestCount: 4,
        activeCount: 3,
        contextKey: 'step-a|build-1',
        disabled: false,
        open: false,
        restoreFocus: false,
        $emit: (...args) => emitted.push(args),
        $nextTick: callback => callback(),
        $refs: {
            trigger: { focus: () => triggerFocused++ },
            'option-3': { focus: () => {} },
            'option-4': { focus: () => {} },
            popover: {
                showHandler: () => vm.onShow(),
                hideHandler: () => vm.onHide()
            }
        }
    }
    Object.defineProperty(vm, 'countOptions', { get: () => selector.computed.countOptions.call(vm) })
    Object.assign(vm, selector.methods)
    assert.deepEqual(selector.computed.countOptions.call(vm), [1, 2, 3, 4])
    vm.openMenu()
    assert.equal(vm.open, true)
    vm.selectCount('2')
    assert.deepEqual(emitted, [['execute-change', 2]], 'selector emits a Number, not a string')
    assert.equal(vm.open, false)
    assert.equal(triggerFocused, 1)

    vm.activeCount = 2
    vm.open = true
    vm.selectCount(2)
    assert.deepEqual(emitted, [['execute-change', 2]], 'selecting the active count does not request it again')
    assert.equal(vm.open, false)

    vm.activeCount = 2
    vm.open = true
    vm.onOptionKeyDown({ key: 'Enter', preventDefault () {} }, 1)
    assert.deepEqual(emitted.slice(-2), [['execute-change', 1], ['focus-return', 'step-a|build-1']], 'keyboard selection carries its context for focus restoration after relocation')
    assert.equal(triggerFocused, 2, 'a changed keyboard selection does not focus the selector that is about to unmount')

    vm.open = true
    selector.watch.contextKey.call(vm)
    assert.equal(vm.open, false, 'context changes close an open menu without stealing focus')
    assert.equal(triggerFocused, 2, 'a context change does not restore focus')
    vm.open = true
    selector.watch.latestCount.call(vm)
    assert.equal(vm.open, false, 'a new execution count closes the stale menu')
    vm.open = true
    selector.watch.disabled.call(vm, true)
    assert.equal(vm.open, false, 'disabling the selector closes the menu')
    vm.disabled = false
    vm.open = true
    vm.onDocumentKeyDown({ key: 'Escape', preventDefault () {} })
    assert.equal(vm.open, false)
    assert.equal(triggerFocused, 3, 'Escape returns focus to the trigger')
    assert.deepEqual(selector.computed.countOptions.call({ latestCount: 1 }), [1])
})

test('long execution-count menus are viewport-bounded and focus keeps the active and keyboard-target options visible', () => {
    const selector = loadOptions('../LogExecuteCountSelect.vue', { angleDownIcon: '/mock-angle-down.svg' })
    const source = readFileSync(new URL('../LogExecuteCountSelect.vue', import.meta.url), 'utf8')
    const focused = []
    const vm = {
        latestCount: 40,
        activeCount: 37,
        open: false,
        $nextTick: callback => callback(),
        $refs: {
            'option-37': { focus: () => focused.push(37) },
            'option-40': { focus: () => focused.push(40) }
        }
    }
    Object.defineProperty(vm, 'countOptions', { get: () => selector.computed.countOptions.call(vm) })
    Object.assign(vm, selector.methods)

    assert.equal(vm.countOptions.length, 40)
    vm.onShow()
    assert.deepEqual(focused, [37], 'opening the menu focuses the currently selected item')
    vm.onOptionKeyDown({ key: 'End', preventDefault () {} }, 37)
    assert.deepEqual(focused, [37, 40], 'keyboard navigation focuses the target item for native scroll-into-view behavior')
    assert.match(source, /\.execute-count-menu\s*\{[\s\S]*max-height:\s*80vh;[\s\S]*max-height:\s*min\(80vh, 640px\);[\s\S]*overflow-y:\s*auto;/)
    assert.match(source, /\.execute-count-menu\s*\{[\s\S]*overscroll-behavior:\s*contain;/)
})

test('keyboard count selection returns focus to the conclusion selector', async () => {
    const parentCases = [
        {
            path: '../../job.vue',
            dependencies: {
                mapGetters: () => ({}),
                detailContainer: {}, jobLog: {}, pluginLog: {}, ContainerContent: {}, JobLogPanel: {},
                LogPanelToolbar: {}, LogExecuteCountSelect: {}, LogConclusionBar: {}, MacDebugDialog: {}
            },
            vm: {
                currentJob: { id: 'job-1', matrixGroupFlag: false },
                execDetail: { id: 'build-1' },
                currentTab: 'log',
                realLogFallback: false
            }
        },
        {
            path: '../../plugin.vue',
            dependencies: {
                mapActions: () => ({}), mapState: () => ({}), detailContainer: {}, ReferenceVariable: {},
                pluginLog: {}, AtomContent: {}, ProgressDetailPanel: {}, StepLogPanel: {}, LogPanelToolbar: {},
                LogConclusionBar: {}, LogExecuteCountSelect: {}, LogParamsView: {}
            },
            vm: {
                currentElement: { id: 'element-1' },
                execDetail: { id: 'build-1' },
                currentTab: 'log',
                realLogFallback: false
            }
        }
    ]

    for (const { path, dependencies, vm: state } of parentCases) {
        const parent = loadOptions(path, dependencies)
        const focused = []
        const contextKey = `${state.currentJob?.id || state.currentElement.id}|${state.execDetail.id}`
        const vm = {
            ...state,
            $refs: {
                conclusionExecuteCountSelect: {
                    focusTrigger: () => focused.push('conclusion')
                }
            },
            $nextTick: callback => VueCompiler.nextTick(callback)
        }

        parent.methods.focusExecuteCountAfterKeyboardSelection.call(vm, contextKey)
        await VueCompiler.nextTick()
        assert.deepEqual(focused, ['conclusion'], `${path} focuses the conclusion selector`)

        parent.methods.focusExecuteCountAfterKeyboardSelection.call(vm, contextKey)
        if (state.currentJob) vm.currentJob = { ...vm.currentJob, id: 'job-2' }
        else vm.currentElement = { ...vm.currentElement, id: 'element-2' }
        await VueCompiler.nextTick()
        assert.deepEqual(focused, ['conclusion'], `${path} drops focus restoration after the context changes`)
    }
})

test('Job header has no config debug action while the log conclusion keeps its debug action', () => {
    const job = readFileSync(new URL('../../job.vue', import.meta.url), 'utf8')
    const headerState = tab => ({
        currentTab: tab,
        headerTitle: 'Job',
        displayJob: { status: 'FAILED', id: 'job-1', matrixGroupFlag: false },
        positionCode: 'job',
        showDebugDockerBtn: true,
        realLogFallback: false,
        currentJob: { id: 'job-1', matrixGroupFlag: false },
        execDetail: { id: 'build-1' },
        realLogControls: {},
        latestExecuteCount: 1,
        activeExecuteCount: 1,
        showConclusion: true,
        canRenderLog: true
    })
    const configTools = renderJobHeaderTools(job, headerState('setting'))
        .filter(vnode => vnode.data?.slot === 'tool')
    const logTools = renderJobHeaderTools(job, headerState('log'))
        .filter(vnode => vnode.data?.slot === 'tool')
    assert.deepEqual(configTools, [], 'config tab does not render a login-debug header tool')
    assert.deepEqual(logTools.map(vnode => vnode.componentOptions?.tag), ['log-panel-toolbar'])

    const conclusion = job.match(/<log-conclusion-bar[\s\S]*?<\/log-conclusion-bar>/)?.[0] || ''
    assert.match(conclusion, /<template v-slot:right-actions>[\s\S]*v-if="showDebugDockerBtn"[\s\S]*@click\.stop="handleDebug"/)
})

test('execution-count popover uses a masked shared SVG and the prototype menu geometry', () => {
    const selector = loadOptions('../LogExecuteCountSelect.vue', { angleDownIcon: '/mock-angle-down.svg' })
    const data = selector.data.call({})
    const source = readFileSync(new URL('../LogExecuteCountSelect.vue', import.meta.url), 'utf8')
    assert.match(data.arrowStyle.mask, /mock-angle-down\.svg/)
    assert.equal(data.arrowStyle.WebkitMask, data.arrowStyle.mask)
    assert.match(source, /import angleDownIcon from .*svg-sprites\/devops\/angle-down\.svg/)
    assert.match(source, /width:\s*112px;[\s\S]*height:\s*24px;[\s\S]*padding:\s*0 24px 0 8px/)
    assert.match(source, /min-width:\s*112px;[\s\S]*padding:\s*4px 0;[\s\S]*background:\s*#2c2d34;[\s\S]*border:\s*1px solid #4d4f56/)
    assert.match(source, /height:\s*28px;[\s\S]*padding:\s*0 12px;[\s\S]*font:\s*12px\/28px/)
    assert.match(source, /\.execute-count-option:hover \{ color:\s*#fff; background:\s*#3a3f4b; \}/)
    assert.match(source, /rgba\(0, 0, 0, \.35\)/)
})

test('Job failed-step conclusion keeps the node divider while exposing its locator', () => {
    const job = loadOptions('../../job.vue', {
        mapGetters: () => ({}),
        jobLog: {}, pluginLog: {}, detailContainer: {}, ContainerContent: {}, LogConclusionBar: {}, JobLogPanel: {}, LogPanelToolbar: {}, LogExecuteCountSelect: {}, MacDebugDialog: {},
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
    })
    const conclusion = job.computed.logConclusionModel.call({
        currentJob: { id: 'job-1', name: 'Job', status: 'FAILED', dockerContainerName: 'node-1', timeCost: {} },
        displayJob: { id: 'job-1', name: 'Job', status: 'FAILED', dockerContainerName: 'node-1', timeCost: {} },
        jobErrorSummary: null,
        jobFailureStep: { name: 'compile', taskName: 'compile' },
        isLatestBuild: true,
        conclusionVisible: true,
        displayConclusionAvailable: true,
        isHistoricalExecuteCount: false,
        execDetail: { cancelUserId: '', errorInfoList: [] },
        displayExecDetail: { cancelUserId: '', errorInfoList: [] },
        $route: { params: { projectId: 'project' } },
        elapsedNow: 0,
        queueDetailModel: null,
        devLogMock: null,
        $t: (key, values) => key === 'logPanel.status.failedByStep' ? `failed: ${values[0]}` : key
    })
    assert.equal(conclusion.locateLog, true)
    assert.equal(conclusion.node, 'node-1')
    assert.equal(conclusion.showNodeMessageDivider, true)
    const bar = readFileSync(new URL('../LogConclusionBar.vue', import.meta.url), 'utf8')
    assert.match(bar, /conclusion\.node && \(conclusion\.errorCode \|\| conclusion\.locateLog \|\| conclusion\.showNodeMessageDivider\)/)
    assert.match(bar, /<div class="lp-status-body">\s*<slot name="prefix-actions"><\/slot>\s*<strong class="lp-status-label">/)
})

test('execute-count selector is only exposed in the conclusion bar', () => {
    const job = readFileSync(new URL('../../job.vue', import.meta.url), 'utf8')
    const plugin = readFileSync(new URL('../../plugin.vue', import.meta.url), 'utf8')
    const legacyJobLog = readFileSync(new URL('../../log/jobLog.vue', import.meta.url), 'utf8')
    const legacyPluginLog = readFileSync(new URL('../../log/pluginLog.vue', import.meta.url), 'utf8')
    const toolbarSource = readFileSync(new URL('../LogPanelToolbar.vue', import.meta.url), 'utf8')
    const toolbar = loadOptions('../LogPanelToolbar.vue', adapter)
    assert.equal(toolbar.props.showExecuteCount, undefined)
    const jobToolbar = job.match(/<log-panel-toolbar[\s\S]*?\/>/)?.[0]
    const pluginToolbar = plugin.match(/<log-panel-toolbar[\s\S]*?\/>/)?.[0]
    assert.ok(jobToolbar)
    assert.ok(pluginToolbar)
    assert.doesNotMatch(jobToolbar, /show-execute-count|latest-count|active-count|context-key=.*execDetail\.id/)
    assert.doesNotMatch(pluginToolbar, /show-execute-count|latest-count|active-count|context-key=.*execDetail\.id/)
    assert.doesNotMatch(toolbarSource, /log-execute-count-select|showExecuteCount/)
    assert.match(job, /<log-conclusion-bar[\s\S]*v-if="currentTab === 'log' && showConclusion"[\s\S]*<template v-slot:prefix-actions>[\s\S]*<log-execute-count-select[\s\S]*v-if="!currentJob\.matrixGroupFlag"[\s\S]*@execute-change="handleExecuteChange"/)
    assert.match(plugin, /<log-conclusion-bar[\s\S]*v-if="currentTab === 'log' && showConclusion"[\s\S]*<template v-slot:prefix-actions>[\s\S]*<log-execute-count-select[\s\S]*@execute-change="handleExecuteChange"/)
    assert.match(job, /<plugin-log[\s\S]*v-if="currentJob\.matrixGroupFlag"/)
    assert.doesNotMatch(job, /<job-log(?:\s|>)|realLogFallback/)
    assert.match(legacyJobLog, /:execute-count="executeCount"[\s\S]*@change-execute="changeExecute"/)
    assert.match(legacyPluginLog, /:execute-count="executeCount"[\s\S]*@change-execute="changeExecute"/)

    const jobData = {
        currentTab: 'log', conclusionVisible: true, canShowConclusion: true, showConclusion: true, realLogFallback: false,
        currentJob: { id: 'job', matrixGroupFlag: false }, execDetail: { id: 'build' },
        latestExecuteCount: 3, activeExecuteCount: 3, logConclusionModel: {}, canRenderLog: true,
        handleLocateLog () {}, handleExecuteChange () {}
    }
    const jobPrefix = () => renderConclusionPrefixSelector(job, jobData)
    assert.equal(jobPrefix().filter(vnode => vnode.componentOptions?.tag === 'log-execute-count-select').length, 1)
    assert.equal(jobPrefix().filter(vnode => vnode.componentOptions?.tag === 'log-execute-count-select')[0].componentOptions.propsData?.disabled, undefined)
    jobData.realLogFallback = true
    assert.equal(jobPrefix().filter(vnode => vnode.componentOptions?.tag === 'log-execute-count-select').length, 1)
    assert.equal(renderLegacyLogSearch(legacyJobLog, { executeCount: 3, changeExecute () {} }).componentOptions.propsData.executeCount, 3)
    jobData.realLogFallback = false
    jobData.currentJob.matrixGroupFlag = true
    assert.equal(jobPrefix().filter(vnode => vnode.componentOptions?.tag === 'log-execute-count-select').length, 0)
    assert.equal(renderLegacyLogSearch(legacyPluginLog, { executeCount: 3, changeExecute () {} }).componentOptions.propsData.executeCount, 3)

    const pluginData = {
        currentTab: 'log', currentElement: { id: 'step', status: 'FAILED' },
        conclusionVisible: true, canShowConclusion: true, showConclusion: true, realLogFallback: false,
        execDetail: { id: 'build' }, latestExecuteCount: 3, activeExecuteCount: 3,
        logConclusionModel: {}, canRenderLog: true, displayProgressPercent: null,
        canExpandProgressSubtasks: false, displayProgressSubtasks: [], progressExpanded: false,
        pendingAction: '', handleLocateLog () {}, handleExecuteChange () {},
        handleMockablePanelAction () {}, handleStepAction () {}
    }
    const pluginPrefix = () => renderConclusionPrefixSelector(plugin, pluginData)
    assert.equal(pluginPrefix().filter(vnode => vnode.componentOptions?.tag === 'log-execute-count-select').length, 1)
    assert.equal(pluginPrefix().filter(vnode => vnode.componentOptions?.tag === 'log-execute-count-select')[0].componentOptions.propsData?.disabled, undefined)
    pluginData.realLogFallback = true
    assert.equal(pluginPrefix().filter(vnode => vnode.componentOptions?.tag === 'log-execute-count-select').length, 1)
    assert.equal(renderLegacyLogSearch(legacyPluginLog, { executeCount: 3, changeExecute () {} }).componentOptions.propsData.executeCount, 3)

    jobData.currentJob.status = 'UNEXEC'
    jobData.currentJob.matrixGroupFlag = false
    jobData.latestExecuteCount = 2
    jobData.activeExecuteCount = 2
    assert.equal(jobPrefix().filter(vnode => vnode.componentOptions?.tag === 'log-execute-count-select')[0].componentOptions.propsData?.disabled, undefined)
    pluginData.currentElement.status = 'UNEXEC'
    pluginData.realLogFallback = false
    pluginData.latestExecuteCount = 2
    pluginData.activeExecuteCount = 2
    assert.equal(pluginPrefix().filter(vnode => vnode.componentOptions?.tag === 'log-execute-count-select')[0].componentOptions.propsData?.disabled, undefined)
})

test('download filename encoder is defined and its consumers stay consistent across merges', () => {
    const job = readFileSync(new URL('../../job.vue', import.meta.url), 'utf8')
    const legacyPluginLog = readFileSync(new URL('../../log/pluginLog.vue', import.meta.url), 'utf8')
    const adapterSource = readFileSync(new URL('../logPanelAdapter.js', import.meta.url), 'utf8')
    const exportsEncoder = /export function encodeDownloadFileName\b/.test(adapterSource)
    const rawFileName = '1-2-构建 & #'

    assert.equal(typeof adapter.encodeDownloadFileName, 'function', 'logPanelAdapter.js must export the filename encoder')
    assert.equal(exportsEncoder, true, 'the filename encoder export must remain explicit')
    assert.equal(adapter.encodeDownloadFileName(rawFileName), encodeURI(encodeURI(rawFileName)))

    for (const [name, source] of [['job.vue', job], ['log/pluginLog.vue', legacyPluginLog]]) {
        const importsEncoder = /import\s*\{[^}]*\bencodeDownloadFileName\b[^}]*\}\s*from\s*['"][^'"]*logPanelAdapter['"]/.test(source)
        const callsEncoder = /\bencodeDownloadFileName\s*\(/.test(source)
        if (!callsEncoder) continue
        assert.equal(importsEncoder, true, `${name} must import the filename encoder before calling it`)
        assert.equal(exportsEncoder, true, `${name} and logPanelAdapter.js must agree on the filename encoder export`)
    }
})

test('plugin parameter subview returns to the same selected configuration tab', () => {
    const empty = () => ({})
    const plugin = loadOptions('../../plugin.vue', {
        AtomContent: {}, ReferenceVariable: {}, mapActions: empty, mapState: empty,
        Artifactory: {}, Report: {}, detailContainer: {}, pluginLog: {}, ProgressDetailPanel: {},
        convertMStoString: () => '', resolveReviewUsers: () => [], StepLogPanel: {}, LogPanelToolbar: {},
        LogConclusionBar: {}, LogParamsView: {}, ...Object.fromEntries([
            'positionCode', 'shouldShowProgressTab', 'buildConclusion', 'shouldLoadStepLog',
            'applyProgressMockTransition', 'buildParamsRecordRequest', 'buildParamsModel',
            'hasCustomRunCondition', 'parseTaskProgressPercent', 'parseTaskProgressSubtasks',
            'resolveDisplayProgressPercent', 'resolveParamsElementSource', 'shouldApplyProgressResponse',
            'shouldFetchParamsRecord', 'shouldFetchTaskProgress', 'shouldScheduleProgressPoll',
            'resolveNodeLogError', 'isJobTimeoutTerminatedStep', 'isSystemAgentTermination',
            'shouldShowLocateLog', 'resolveConclusionDisplayMessage', 'applyLogPanelDevMock',
            'resolveLogPanelDevMock', 'PROCESS_API_URL_PREFIX', 'RESOURCE_ACTION',
            'handlePipelineNoPermission'
        ].map(name => [name, () => {}]))
    })
    const vm = { currentTab: 'setting', settingView: 'config', paramsViewEnabled: false }
    vm.showParamsView = plugin.methods.showParamsView
    vm.showConfigView = plugin.methods.showConfigView

    vm.showParamsView()
    assert.deepEqual([vm.currentTab, vm.settingView], ['setting', 'config'], 'disabled parameter entry cannot navigate into the view')
    vm.paramsViewEnabled = true
    vm.showParamsView()
    assert.deepEqual([vm.currentTab, vm.settingView], ['setting', 'params'])
    vm.showConfigView()
    assert.deepEqual([vm.currentTab, vm.settingView], ['setting', 'config'])
})

test('minimap keeps static counts and renders bounded color bars instead of preview text', () => {
    const minimap = loadOptions('../LogErrorMinimap.vue', {
        ...adapter,
        ...Object.fromEntries(['resolveMinimapTextLayout', 'projectMinimapRows', 'resolveMinimapVisualMetrics'].map(name => [name, () => ({})]))
    })
    const source = readFileSync(new URL('../LogErrorMinimap.vue', import.meta.url), 'utf8')
    const vm = { coordinates: { scale: 0.18 } }
    vm.levelOf = minimap.methods.levelOf
    const bar = minimap.methods.previewBarStyle.call(vm, { line: { level: 'INFO' }, clipOffset: 0 })
    assert.equal(bar['--lp-bar-color'], 'rgba(240, 241, 245, 0.45)')
    assert.equal(bar['--lp-bar-thickness'], '1.2px')
    assert.match(source, /class="lp-minimap-bar"/)
    assert.match(source, /width:\s*calc\(\(100% - 30px\) \* 0\.8\)/)
    assert.doesNotMatch(source, /filter-level|@click="filter|class="lp-minimap-text"/)
    assert.match(source, /class="lp-badge-count"/)
})
