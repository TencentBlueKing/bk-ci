<template>
    <section
        class="step-log-panel"
    >
        <div
            v-if="loadingInitial"
            class="log-panel-loading"
            role="status"
            :aria-label="$t('loading')"
        >
            <span
                class="loading-spinner"
                aria-hidden="true"
            ></span>
        </div>
        <div
            v-else-if="permissionDenied"
            class="log-panel-empty"
        >
            {{ $t('noPermission') }}
        </div>
        <div
            v-else-if="errorMessage && !logs.length"
            class="log-panel-load-error"
            role="alert"
        >
            <span>{{ errorMessageText }}</span>
            <button
                type="button"
                :disabled="loadingInitial"
                @click="reload"
            >
                {{ $t('logPanel.reloadLogs') }}
            </button>
        </div>
        <div
            v-else-if="showStatusEmptyLine"
            class="log-panel-status-empty"
        >
            <span class="log-line-number">1</span>{{ emptyLineText }}
        </div>
        <div
            v-else-if="cleanStatus"
            class="log-panel-empty is-clean"
        >
            {{ cleanStatusText }}
        </div>
        <div
            v-else
            class="step-log-body"
        >
            <log-group-bar
                v-if="subTags.length"
                :value="selectedSubTag"
                :sub-tags="subTags"
                @input="selectedSubTag = $event"
            />
            <p
                v-if="errorMessage"
                class="log-panel-error"
            >
                {{ errorMessageText }}
            </p>
            <log-lines-view
                ref="linesView"
                :logs="visibleLogs"
                :issue-logs="numberedLogs"
                :display-base="displayBase + 1"
                :keyword="controls.search"
                :show-time="controls.showTime"
                :wrap="controls.wrap"
                :follow-bottom="bottomFollow"
                :highlighted-line-no="highlightedLogId"
                :loading-before="loadingBefore"
                @reach-top="loadBefore"
                @stick-bottom-change="onStickBottomChange"
                @locate="locate"
                @toggle-group="toggleLogGroup"
                @view-large-variable="openLargeVariable"
            />
        </div>
        <large-variable-dialog
            :value="largeVariable.visible"
            :variable-key="largeVariable.key"
            :length="largeVariable.length"
            :state="largeVariable.state"
            :content="largeVariable.value"
            :error-message="largeVariable.errorMessage"
            @input="closeLargeVariable"
            @retry="fetchLargeVariable"
        />
    </section>
</template>

<script>
    import { mapActions } from 'vuex'
    import { markRaw, shallowRef } from 'vue'
    import LogGroupBar from './LogGroupBar'
    import LogLinesView from './LogLinesView'
    import LargeVariableDialog from './LargeVariableDialog'
    import { resolveLargeVariableResult } from './logLargeVariable'
    import {
        findLogMatchIds,
        formatLogTime,
        getLogIssueLevel,
        isPermissionError,
        latestDisplayBase,
        normalizeLogPanelResponse,
        serializeLogLevels,
        shouldBackfillSmallLog,
        shouldApplyLogSearchScroll,
        shouldContinueSearchBackfill,
        resolveCleanResponseMessage,
        resolveCleanStatusFallbackKey,
        resolveCleanStatusText,
        resolveDirectJobDownloadUrl,
        resolveLocateLogTarget,
        resolveLogHighlightState,
        resolveNextLogMatchIndex,
        shouldContinueAutoBackfillAfterBefore,
        resolveLogLoadErrorKey,
        shouldReloadOnLevelsChange,
        shouldRetryBeforeLoad,
        shouldScheduleBeforeRetry,
        shouldResumeLogPolling,
        shouldShowStatusEmptyLine,
        shouldUseDirectJobDownload,
        resolveConditionSkipReason,
        withDisplayLineNumbers
    } from './logPanelAdapter'
    import { countNewLogPanelLines, mergeLogPanelLines, nextAfterCursor } from './logPanelTail'
    import { buildLogStructure, isLogStructureFilterEligible, projectLogStructure, resolveLogGroupAncestors } from './logLineStructure.js'

    export default {
        name: 'StepLogPanel',
        components: { LogGroupBar, LogLinesView, LargeVariableDialog },
        props: {
            projectId: { type: String, required: true },
            pipelineId: { type: String, required: true },
            buildId: { type: String, required: true },
            tag: { type: String, default: undefined },
            jobId: { type: String, default: undefined },
            contextKey: { type: String, default: '' },
            executeCount: { type: Number, required: true },
            archiveFlag: { type: [String, Boolean], default: undefined },
            downloadLink: { type: String, default: '' },
            downloadTag: { type: String, default: undefined },
            downloadJobId: { type: String, default: undefined },
            locateRequest: { type: Number, default: 0 },
            locateRequestContext: { type: String, default: '' },
            logContext: { type: String, default: '' },
            locateTarget: { type: Object, default: null },
            emptyLineText: { type: String, default: '' },
            controls: { type: Object, required: true },
            taskStatus: { type: String, default: '' }
        },
        data () {
            return {
                logs: shallowRef(markRaw([])),
                displayBase: 0,
                matchedTotal: null,
                hasBefore: false,
                hasAfter: false,
                finished: true,
                endLineNo: 0,
                startLineNo: 0,
                loadingInitial: true,
                loadingBefore: false,
                loadingAfter: false,
                permissionDenied: false,
                cleanStatus: '',
                serverMessage: '',
                errorMessage: '',
                retryContext: '',
                suppressLevelsReload: false,
                beforeRetries: 0,
                afterRetries: 0,
                generation: 0,
                bottomFollow: true,
                requestTimer: null,
                beforeTimer: null,
                tailIdentity: {},
                maxLineNo: 0,
                maxTimestamp: null,
                beforeCursor: null,
                suppressSubTagReload: false,
                subTags: [],
                selectedSubTag: '',
                highlightedLogId: '',
                searchBackfillStopped: false,
                collapsedGroupIds: [],
                pendingStatusResume: false,
                largeVariable: { visible: false, key: '', length: 0, tag: '', state: 'loading', value: '', errorMessage: '' },
                largeVariableSeq: 0
            }
        },
        computed: {
            matchIds () {
                return findLogMatchIds(this.logs, this.controls.search)
            },
            numberedLogs () {
                return withDisplayLineNumbers(this.logs, this.displayBase + 1)
            },
            logStructureComplete () {
                return this.finished && !this.hasBefore && !this.hasAfter
                    && Number.isFinite(Number(this.matchedTotal)) && Number(this.matchedTotal) === this.logs.length
            },
            logStructure () {
                return buildLogStructure(this.numberedLogs, { context: this.logContext || this.contextKey, complete: this.logStructureComplete })
            },
            groupFoldingEnabled () {
                return isLogStructureFilterEligible(this.controls.levels)
                    && this.logStructure.groups.some(group => group.complete)
            },
            visibleLogs () {
                if (!isLogStructureFilterEligible(this.controls.levels)) return this.numberedLogs
                return projectLogStructure(this.logStructure, { enabled: this.groupFoldingEnabled, collapsedIds: this.collapsedGroupIds })
            },
            cleanStatusText () {
                const fallbackKey = resolveCleanStatusFallbackKey(this.cleanStatus)
                const fallback = fallbackKey ? this.$t(`logPanel.${fallbackKey}`) : ''
                return resolveCleanStatusText(this.serverMessage, this.cleanStatus, fallback)
            },
            errorMessageText () {
                return String(this.errorMessage || '').startsWith('logPanel.') ? this.$t(this.errorMessage) : this.errorMessage
            },
            showStatusEmptyLine () {
                return shouldShowStatusEmptyLine(this.cleanStatus, this.emptyLineText)
            },
        },
        watch: {
            executeCount () {
                this.collapsedGroupIds = []
                if (!this.contextKey) {
                    this.clearSearchForContext()
                    this.reload()
                }
            },
            tag () {
                this.collapsedGroupIds = []
                if (!this.contextKey) {
                    this.clearSearchForContext()
                    this.reload()
                }
            },
            jobId () {
                this.collapsedGroupIds = []
                if (!this.contextKey) {
                    this.clearSearchForContext()
                    this.reload()
                }
            },
            locateRequest (token, previousToken) {
                if (token === previousToken || this.locateRequestContext !== this.logContext) return
                this.$nextTick(() => {
                    if (this.locateRequestContext === this.logContext && (!this.locateTarget || this.locateTarget.context === this.logContext)) this.locateFirstIssue()
                })
            },
            contextKey () {
                this.collapsedGroupIds = []
                this.suppressSubTagReload = true
                this.selectedSubTag = ''
                this.clearSearchForContext()
                this.reload()
                this.$nextTick(() => {
                    this.suppressSubTagReload = false
                })
            },
            archiveFlag () {
                this.collapsedGroupIds = []
                if (!this.contextKey) this.reload()
            },
            'controls.levels': {
                deep: true,
                handler () {
                    this.collapsedGroupIds = []
                    if (!shouldReloadOnLevelsChange({ suppressLevelsReload: this.suppressLevelsReload })) return
                    this.$emit('controls-change')
                    this.reload()
                }
            },
            'controls.search' (value, previousValue) {
                this.searchBackfillStopped = false
                if (!value && previousValue) {
                    clearTimeout(this.beforeTimer)
                    this.beforeTimer = null
                }
                this.updateMatches(true)
                if (value) this.$nextTick(this.continueSearchBackfill)
            },
            locateTarget: {
                deep: true,
                handler () {
                    this.emitLocateAvailability()
                }
            },
            selectedSubTag () {
                this.collapsedGroupIds = []
                if (!this.suppressSubTagReload) this.reload()
            },
            taskStatus (status, previousStatus) {
                if (shouldResumeLogPolling(previousStatus, status)) this.resumeLogPolling()
            }
        },
        created () {
            this.suppressLevelsReload = true
            this.ensureLevelsControl()
        },
        mounted () {
            this.reload()
            this.$nextTick(() => {
                this.suppressLevelsReload = false
            })
        },
        beforeDestroy () {
            this.invalidate()
        },
        methods: {
            ...mapActions('atom', ['getLogPanelLatest', 'getLogPanelBefore', 'getLogPanelAfter', 'getLogStatus', 'getDownloadLogFromArtifactory', 'getBuildVariableValue']),
            openLargeVariable (payload = {}) {
                this.largeVariable = {
                    visible: true,
                    key: payload.key || '',
                    length: Number(payload.length) || 0,
                    tag: payload.tag || this.tag || '',
                    state: 'loading',
                    value: '',
                    errorMessage: ''
                }
                this.fetchLargeVariable()
            },
            closeLargeVariable () {
                this.largeVariableSeq += 1
                this.largeVariable = { ...this.largeVariable, visible: false }
            },
            async fetchLargeVariable () {
                const seq = ++this.largeVariableSeq
                const { key, tag } = this.largeVariable
                this.largeVariable = { ...this.largeVariable, state: 'loading', value: '', errorMessage: '' }
                const params = {
                    projectId: this.projectId,
                    pipelineId: this.pipelineId,
                    buildId: this.buildId,
                    key,
                    executeCount: this.executeCount,
                    taskId: tag,
                    archiveFlag: this.archiveFlag
                }
                Object.keys(params).forEach(name => {
                    if (params[name] === undefined || params[name] === null || params[name] === '') delete params[name]
                })
                try {
                    const res = await this.getBuildVariableValue(params)
                    if (seq !== this.largeVariableSeq) return
                    const result = resolveLargeVariableResult(res?.data)
                    this.largeVariable = { ...this.largeVariable, state: result.state, value: result.value }
                } catch (error) {
                    if (seq !== this.largeVariableSeq) return
                    this.largeVariable = isPermissionError(error)
                        ? { ...this.largeVariable, state: 'noPermission' }
                        : { ...this.largeVariable, state: 'error', errorMessage: error?.message || '' }
                }
            },
            invalidate () {
                this.generation += 1
                this.pendingStatusResume = false
                this.collapsedGroupIds = []
                clearTimeout(this.requestTimer)
                clearTimeout(this.beforeTimer)
                this.requestTimer = null
                this.beforeTimer = null
                this.tailIdentity = {}
                this.maxLineNo = 0
                this.maxTimestamp = null
                this.beforeCursor = null
                this.loadingBefore = false
                this.loadingAfter = false
            },
            clearSearchForContext () {
                this.searchBackfillStopped = true
                clearTimeout(this.beforeTimer)
                this.beforeTimer = null
                if (this.controls.search) this.$set(this.controls, 'search', '')
                this.controls.current = 0
                this.controls.total = 0
                this.highlightedLogId = ''
            },
            ensureLevelsControl () {
                if (typeof this.controls.levelAll !== 'boolean') {
                    this.$set(this.controls, 'levelAll', String(this.controls.level || '').trim().toUpperCase() === 'ALL')
                }
                if (Array.isArray(this.controls.levels) && this.controls.levels.length) return
                const raw = String(this.controls.level || 'INFO,WARN,ERROR').trim()
                const levels = raw === 'ALL'
                    ? ['INFO', 'WARN', 'ERROR', 'DEBUG']
                    : raw.split(',').map(level => String(level || '').trim().toUpperCase()).filter(Boolean)
                this.$set(this.controls, 'levels', levels.length ? levels : ['INFO', 'WARN', 'ERROR'])
            },
            continueSearchBackfill () {
                if (!this.controls.search || this.searchBackfillStopped || this.loadingInitial || this.loadingBefore || !this.hasBefore) return
                this.loadBefore()
            },
            requestParams () {
                this.ensureLevelsControl()
                return {
                    projectId: this.projectId,
                    pipelineId: this.pipelineId,
                    buildId: this.buildId,
                    tag: this.tag,
                    jobId: this.jobId,
                    executeCount: this.executeCount,
                    levels: serializeLogLevels(this.controls.levels),
                    pageSize: 1000,
                    subTag: this.selectedSubTag,
                    archiveFlag: this.archiveFlag
                }
            },
            reload () {
                const context = [this.tag, this.jobId, this.executeCount, this.archiveFlag, serializeLogLevels(this.controls.levels || []), this.selectedSubTag].join('|')
                if (context !== this.retryContext) {
                    this.retryContext = context
                    this.beforeRetries = 0
                    this.afterRetries = 0
                }
                this.invalidate()
                const generation = this.generation
                this.searchBackfillStopped = false
                this.hasBefore = false
                this.hasAfter = false
                this.finished = true
                this.startLineNo = 0
                this.endLineNo = 0
                this.logs = []
                this.controls.current = 0
                this.controls.total = 0
                this.highlightedLogId = ''
                this.$emit('issues-change', false)
                this.$emit('locatable-change', false)
                this.displayBase = 0
                this.matchedTotal = null
                this.bottomFollow = true
                this.loadingInitial = true
                this.permissionDenied = false
                this.cleanStatus = ''
                this.serverMessage = ''
                this.errorMessage = ''
                this.tailIdentity = {}
                this.maxLineNo = 0
                this.maxTimestamp = null
                this.beforeCursor = null
                let latestSucceeded = false
                this.getLogPanelLatest(this.requestParams()).then(response => {
                    if (generation !== this.generation) return
                    const result = normalizeLogPanelResponse(response, { strictLatest: true })
                    this.serverMessage = result.message
                    this.applyResponse(result, 'latest', response, generation)
                    latestSucceeded = result.status === 0 && !result.clean
                }).catch(error => {
                    if (generation !== this.generation) return
                    if (this.controls.search) this.searchBackfillStopped = true
                    if (isPermissionError(error)) {
                        this.permissionDenied = true
                        this.$emit('permission', error)
                    } else {
                        this.errorMessage = resolveLogLoadErrorKey(error)
                    }
                }).finally(() => {
                    if (generation === this.generation) {
                        this.loadingInitial = false
                        if (this.pendingStatusResume) {
                            this.pendingStatusResume = false
                            this.resumeLogPolling()
                        }
                        const search = String(this.controls.search || '')
                        this.$nextTick(() => {
                            if (!latestSucceeded || generation !== this.generation || search !== String(this.controls.search || '')) {
                                if (generation === this.generation && search === String(this.controls.search || '')) this.$emit('logs-loaded')
                                return
                            }
                            if (this.controls.search) this.continueSearchBackfill()
                            else {
                                this.scrollToBottom()
                                this.maybeAutoBackfill()
                            }
                            this.$emit('logs-loaded')
                        })
                    }
                })
            },
            applyResult (result, direction) {
                const incoming = result.logs.map((log, index) => ({ ...log, lineNo: log.lineNo ?? log.line ?? index + 1, time: formatLogTime(log) }))
                const incomingCursor = nextAfterCursor(incoming)
                if (incomingCursor.startLineNo !== undefined) this.maxLineNo = Math.max(this.maxLineNo, incomingCursor.startLineNo)
                if (incomingCursor.sinceTimestamp !== undefined) this.maxTimestamp = this.maxTimestamp === null ? incomingCursor.sinceTimestamp : Math.max(this.maxTimestamp, incomingCursor.sinceTimestamp)
                this.expireTailIdentity()
                const oldLogs = this.logs
                const currentLineNos = new Set(oldLogs.map(log => Number(log.lineNo)))
                const accepted = incoming.filter(log => direction !== 'after' || currentLineNos.has(Number(log.lineNo)) || this.tailIdentity[Number(log.lineNo)] === undefined)
                const addedCount = direction === 'latest' ? accepted.length : countNewLogPanelLines(oldLogs, accepted)
                const merged = direction === 'latest' ? mergeLogPanelLines([], accepted) : mergeLogPanelLines(oldLogs, accepted)
                if (direction === 'latest') {
                    const total = Number(result.matchedTotal)
                    if (Number.isFinite(total)) this.matchedTotal = total
                    this.displayBase = latestDisplayBase(this.matchedTotal ?? merged.length, merged.length) - 1
                } else if (direction === 'before') {
                    this.displayBase = Math.max(0, this.displayBase - addedCount)
                } else if (direction === 'after' && addedCount > 0 && Number.isFinite(this.matchedTotal)) {
                    this.matchedTotal += addedCount
                }
                this.logs = merged
                if (String(this.taskStatus || '').toUpperCase() === 'SKIP') {
                    const reason = resolveConditionSkipReason(this.logs)
                    if (reason) this.$emit('condition-skip-reason', reason)
                }
                const validCollapsedIds = new Set(this.logStructure.groups.filter(group => group.complete).map(group => group.id))
                this.collapsedGroupIds = this.collapsedGroupIds.filter(id => validCollapsedIds.has(id))
                this.hasBefore = result.hasBefore
                this.hasAfter = result.hasAfter
                this.finished = result.finished
                if (direction === 'before') {
                    this.beforeRetries = 0
                    clearTimeout(this.beforeTimer)
                    this.beforeTimer = null
                } else if (direction === 'after') {
                    this.afterRetries = 0
                }
                this.errorMessage = ''
                const rawFirstValue = result.logs.length ? (result.logs[0].lineNo ?? result.logs[0].line) : null
                const rawFirstLineNo = rawFirstValue === null || rawFirstValue === undefined || rawFirstValue === '' ? NaN : Number(rawFirstValue)
                if (direction === 'latest' || direction === 'before') {
                    const responseCursor = result.startLineNo === null || result.startLineNo === undefined || result.startLineNo === '' ? NaN : Number(result.startLineNo)
                    if (Number.isSafeInteger(responseCursor)) this.beforeCursor = responseCursor
                    else if (Number.isSafeInteger(rawFirstLineNo)) this.beforeCursor = rawFirstLineNo
                }
                const cursor = nextAfterCursor(this.logs)
                this.endLineNo = this.maxLineNo || cursor.startLineNo || result.endLineNo || this.endLineNo
                this.startLineNo = this.beforeCursor ?? this.startLineNo
                this.subTags = result.subTags
                this.$emit('sub-tags', this.subTags)
                this.updateMatches()
                this.emitIssuesChange()
                this.emitLocateAvailability()
                if (!result.finished || result.hasAfter) this.scheduleAfter()
            },
            emitIssuesChange () {
                this.$emit('issues-change', this.logs.some(log => Boolean(getLogIssueLevel(log))))
            },
            emitLocateAvailability () {
                if (this.locateTarget && this.locateTarget.context !== this.logContext) {
                    this.$emit('locatable-change', false)
                    return
                }
                const result = resolveLocateLogTarget(this.logs, this.locateTarget?.messages || [])
                this.$emit('locatable-change', result.lineNo !== null && result.lineNo !== undefined)
            },
            updateMatches (reset = false) {
                const ids = this.matchIds
                const previousHighlightedLineNo = this.highlightedLogId
                this.controls.total = ids.length
                const state = resolveLogHighlightState({
                    logs: this.logs,
                    matchIds: ids,
                    highlightedLineNo: this.highlightedLogId,
                    currentMatch: this.controls.current,
                    reset,
                    searching: Boolean(this.controls.search)
                })
                this.controls.current = state.current
                this.highlightedLogId = state.highlightedLineNo
                const shouldLocateSearchHit = Boolean(this.controls.search && state.highlightedLineNo)
                    && (reset || !previousHighlightedLineNo)
                if (shouldLocateSearchHit) {
                    this.revealLogGroupAncestors(state.highlightedLineNo)
                    const generation = this.generation
                    const search = String(this.controls.search || '')
                    const target = state.highlightedLineNo
                    this.$nextTick(() => {
                        if (shouldApplyLogSearchScroll({
                            generation,
                            currentGeneration: this.generation,
                            search,
                            currentSearch: String(this.controls.search || ''),
                            target,
                            currentTarget: this.highlightedLogId,
                            matchIds: this.matchIds
                        })) this.$refs.linesView?.scrollToLine(target)
                    })
                }
            },
            moveMatch (step) {
                const ids = this.matchIds
                if (!ids.length) return
                this.controls.current = resolveNextLogMatchIndex(ids.length, this.controls.current, step)
                const target = ids[this.controls.current - 1]
                this.highlightedLogId = target
                this.revealLogGroupAncestors(target)
                const generation = this.generation
                const search = String(this.controls.search || '')
                this.$nextTick(() => {
                    if (shouldApplyLogSearchScroll({
                        generation,
                        currentGeneration: this.generation,
                        search,
                        currentSearch: String(this.controls.search || ''),
                        target,
                        currentTarget: this.highlightedLogId,
                        matchIds: this.matchIds
                    })) this.$refs.linesView?.scrollToLine(target)
                })
            },
            applyResponse (result, direction, response, generation = this.generation) {
                if (result.status === 2 || result.status === 3 || result.clean) {
                    this.serverMessage = resolveCleanResponseMessage(result.message)
                    this.cleanStatus = result.status === 3 ? 'closed' : 'clean'
                    clearTimeout(this.requestTimer)
                    clearTimeout(this.beforeTimer)
                    this.requestTimer = null
                    this.beforeTimer = null
                    this.loadingBefore = false
                    this.loadingAfter = false
                    this.$emit('locatable-change', false)
                    return
                }
                this.serverMessage = result.message || this.serverMessage
                if (result.status === 999) {
                    this.errorMessage = resolveLogLoadErrorKey({ status: result.status })
                    if (direction === 'latest') {
                        clearTimeout(this.requestTimer)
                        clearTimeout(this.beforeTimer)
                        this.requestTimer = null
                        this.beforeTimer = null
                        return
                    }
                    if (direction === 'before') {
                        if (shouldRetryBeforeLoad({ status: result.status, beforeRetries: this.beforeRetries })) {
                            this.beforeRetries += 1
                            this.scheduleBefore(generation)
                        }
                        return
                    }
                    if (direction === 'after') {
                        this.afterRetries += 1
                        this.scheduleAfter()
                        return
                    }
                    return
                }
                this.cleanStatus = result.status === 1 && direction === 'latest' ? 'empty' : ''
                this.applyResult(result, direction)
            },
            loadBefore () {
                if (this.loadingBefore || !this.hasBefore || (!this.logs.length && !this.startLineNo)) return
                const searchBackfillAtStart = Boolean(this.controls.search)
                const searchAtStart = String(this.controls.search || '')
                this.loadingBefore = true
                const generation = this.generation
                const view = this.$refs.linesView
                let anchor = null
                let oldHeight = 0
                let interactionToken = view?.layoutInteractionToken
                const captureScrollState = () => {
                    anchor = view?.captureAnchor() || null
                    oldHeight = view?.getScrollHeight() || 0
                    interactionToken = view?.layoutInteractionToken
                }
                const oldLogCount = this.logs.length
                const oldHighlightedLineNo = this.highlightedLogId
                const previousCursor = this.beforeCursor ?? this.startLineNo
                let beforeSucceeded = false
                const firstLine = this.beforeCursor ?? this.startLineNo
                this.getLogPanelBefore({ ...this.requestParams(), endLineNo: firstLine }).then(response => {
                    if (generation !== this.generation) return
                    captureScrollState()
                    const result = normalizeLogPanelResponse(response)
                    if (result.status !== 999 && result.status !== 2 && result.status !== 3 && !result.clean) {
                        beforeSucceeded = true
                    }
                    this.applyResponse(result, 'before', response, generation)
                }).catch(error => {
                    if (generation !== this.generation) return
                    captureScrollState()
                    if (isPermissionError(error)) this.$emit('permission', error)
                    else {
                        this.errorMessage = error.message || String(error)
                        if (!searchBackfillAtStart && !this.controls.search) {
                            this.beforeRetries = Math.min(this.beforeRetries + 1, 3)
                            this.scheduleBefore(generation)
                        }
                    }
                }).finally(() => {
                    if (generation !== this.generation) return
                    this.loadingBefore = false
                    const searchActive = Boolean(this.controls.search)
                    if (!beforeSucceeded && (searchBackfillAtStart || searchActive)) {
                        this.searchBackfillStopped = searchActive
                        clearTimeout(this.beforeTimer)
                        this.beforeTimer = null
                    }
                    this.$nextTick(() => {
                        if (generation !== this.generation) return
                        const searchHitNeedsLocate = searchActive && Boolean(this.highlightedLogId)
                            && (!oldHighlightedLineNo || searchAtStart !== String(this.controls.search || ''))
                        if (!searchHitNeedsLocate) {
                            if (this.bottomFollow && !this.controls.search) this.scrollToBottom()
                            else if (interactionToken === view?.layoutInteractionToken) {
                                if (anchor) view?.restoreAnchor(anchor)
                                else view?.preserveScrollOnPrepend(oldHeight)
                            }
                        }
                        const addedCount = this.logs.length - oldLogCount
                        const continueSearch = shouldContinueSearchBackfill({
                            searching: searchActive,
                            stopped: this.searchBackfillStopped,
                            hasBefore: this.hasBefore,
                            succeeded: beforeSucceeded,
                            addedCount,
                            previousCursor,
                            nextCursor: this.beforeCursor ?? this.startLineNo
                        })
                        if (continueSearch) this.continueSearchBackfill()
                        else if (!searchActive && !searchBackfillAtStart && shouldContinueAutoBackfillAfterBefore({ succeeded: beforeSucceeded, addedCount })) {
                            const atTop = !this.bottomFollow && shouldScheduleBeforeRetry({ scrollTop: view?.getScrollTop() ?? 0 })
                            if (atTop) this.loadBefore()
                            else this.maybeAutoBackfill()
                        }
                    })
                })
            },
            loadAfter () {
                this.requestTimer = null
                if (this.loadingAfter || (!this.logs.length && this.finished) || (this.finished && !this.hasAfter)) return
                this.loadingAfter = true
                const generation = this.generation
                const view = this.$refs.linesView
                const cursor = nextAfterCursor(this.logs)
                if (this.maxLineNo) cursor.startLineNo = this.maxLineNo
                if (this.maxTimestamp !== null) cursor.sinceTimestamp = this.maxTimestamp
                this.getLogPanelAfter({
                    ...this.requestParams(),
                    ...cursor,
                    startLineNo: cursor.startLineNo ?? this.endLineNo
                }).then(response => {
                    if (generation !== this.generation) return
                    const wasFollowingAtResponse = this.bottomFollow
                    const anchor = !wasFollowingAtResponse ? view?.captureAnchor() : null
                    const interactionToken = view?.layoutInteractionToken
                    const result = normalizeLogPanelResponse(response)
                    this.applyResponse(result, 'after', response, generation)
                    if (anchor) {
                        this.$nextTick(() => {
                            if (generation === this.generation && !this.bottomFollow
                                && interactionToken === view?.layoutInteractionToken) view?.restoreAnchor(anchor)
                        })
                    } else if (result.status === 0 && result.logs.length && this.bottomFollow && !this.controls.search) {
                        this.$nextTick(() => {
                            if (generation === this.generation && this.bottomFollow) this.scrollToBottom()
                        })
                    }
                }).catch(error => {
                    if (generation !== this.generation) return
                    if (isPermissionError(error)) this.$emit('permission', error)
                    else {
                        this.errorMessage = error.message || String(error)
                        this.afterRetries += 1
                        this.scheduleAfter()
                    }
                }).finally(() => {
                    if (generation === this.generation) {
                        this.loadingAfter = false
                        if (this.pendingStatusResume) {
                            this.pendingStatusResume = false
                            this.resumeLogPolling()
                        }
                    }
                })
            },
            scheduleAfter () {
                clearTimeout(this.requestTimer)
                this.requestTimer = null
                if (this.finished && !this.hasAfter) return
                const delay = Math.min(10000, 1000 * (2 ** Math.min(this.afterRetries, 3)))
                const generation = this.generation
                this.requestTimer = setTimeout(() => {
                    this.requestTimer = null
                    if (generation === this.generation) this.loadAfter()
                }, delay)
            },
            resumeLogPolling () {
                if (this.loadingInitial || this.loadingAfter) {
                    this.pendingStatusResume = true
                    return
                }
                clearTimeout(this.requestTimer)
                this.requestTimer = null
                this.finished = false
                this.loadAfter()
            },
            maybeAutoBackfill () {
                if (this.loadingBefore || !this.hasBefore) return
                if (!shouldBackfillSmallLog({
                    finished: this.finished,
                    hasAfter: this.hasAfter,
                    hasBefore: this.hasBefore,
                    matchedTotal: this.matchedTotal,
                    loadedCount: this.logs.length
                })) return
                this.loadBefore()
            },
            scheduleBefore (generation) {
                clearTimeout(this.beforeTimer)
                if (!this.hasBefore || this.beforeRetries >= 3) return
                this.beforeTimer = setTimeout(() => {
                    this.beforeTimer = null
                    const scrollTop = this.$refs.linesView?.getScrollTop() ?? 0
                    if (generation === this.generation && shouldScheduleBeforeRetry({ scrollTop })) this.loadBefore()
                }, Math.min(8000, 500 * (2 ** Math.min(this.beforeRetries - 1, 3))))
            },
            expireTailIdentity () {
                if (this.maxTimestamp === null) return
                Object.keys(this.tailIdentity).forEach(lineNo => {
                    if (this.maxTimestamp - this.tailIdentity[lineNo] > 15000) this.$delete(this.tailIdentity, lineNo)
                })
            },
            onStickBottomChange (atBottom) {
                const previous = this.bottomFollow
                if (previous === Boolean(atBottom)) return
                this.bottomFollow = atBottom
                if (!previous && this.bottomFollow) {
                    clearTimeout(this.requestTimer)
                    this.requestTimer = null
                    if (this.hasAfter || !this.finished) this.loadAfter()
                }
            },
            locate (log) {
                this.highlightedLogId = log.lineNo
                this.revealLogGroupAncestors(log.lineNo)
                this.$nextTick(() => this.$refs.linesView?.scrollToLine(log.lineNo))
            },
            revealLogGroupAncestors (lineNo) {
                if (!this.groupFoldingEnabled) return
                const ancestors = resolveLogGroupAncestors(this.logStructure, lineNo)
                if (!ancestors.length) return
                this.collapsedGroupIds = this.collapsedGroupIds.filter(id => !ancestors.includes(id))
            },
            toggleLogGroup (groupId) {
                if (!this.groupFoldingEnabled) return
                this.collapsedGroupIds = this.collapsedGroupIds.includes(groupId)
                    ? this.collapsedGroupIds.filter(id => id !== groupId)
                    : [...this.collapsedGroupIds, groupId]
            },
            locateFirstIssue (messages) {
                if (this.locateTarget && this.locateTarget.context !== this.logContext) return false
                const hasExplicitMessages = arguments.length > 0
                const targetMessages = hasExplicitMessages ? messages : (this.locateTarget?.messages || [])
                const hasTarget = resolveLocateLogTarget([], targetMessages).hasTarget
                const result = hasTarget ? resolveLocateLogTarget(this.logs, targetMessages) : null
                const hasText = log => String(log?.text ?? log?.content ?? log?.message ?? '').trim()
                const lastError = [...this.logs].reverse().find(log => getLogIssueLevel(log) === 'error' && hasText(log))
                const firstWarn = this.logs.find(log => getLogIssueLevel(log) === 'warn' && hasText(log))
                const targetLine = result?.lineNo !== null && result?.lineNo !== undefined
                    ? result.lineNo
                    : lastError?.lineNo ?? lastError?.line ?? firstWarn?.lineNo ?? firstWarn?.line
                const line = this.logs.find(log => (log.lineNo ?? log.line) === targetLine)
                if (line) this.locate(line)
                if (line) return true
                this.$emit('locate-miss')
                return false
            },
            scrollToBottom () {
                if (this.bottomFollow && !this.controls.search) this.$refs.linesView?.scrollToBottom()
            },
            download () {
                const context = {
                    projectId: this.projectId,
                    pipelineId: this.pipelineId,
                    buildId: this.buildId,
                    tag: this.downloadTag || this.tag,
                    jobId: this.downloadJobId || this.jobId,
                    executeCount: this.executeCount
                }
                if (shouldUseDirectJobDownload(context)) {
                    const url = resolveDirectJobDownloadUrl({
                        downloadLink: this.downloadLink,
                        executeCount: this.executeCount
                    })
                    if (url) location.href = url
                    return
                }
                this.getLogStatus(context).then(response => {
                    const mode = response?.data?.logMode || response?.data?.mode
                    if (mode === 'LOCAL') {
                        this.$bkMessage({ theme: 'primary', message: this.$t('history.uploadLog'), limit: 1 })
                        return
                    }
                    const artifact = mode === 'ARCHIVED' && context.tag
                        ? this.getDownloadLogFromArtifactory(context)
                        : Promise.resolve(this.downloadLink)
                    return artifact.then(url => {
                        if (!url) return
                        if (mode === 'ARCHIVED' && context.tag) {
                            location.href = url
                            return
                        }
                        if (/[?&]executeCount=/.test(url)) {
                            location.href = url
                            return
                        }
                        const separator = url.includes('?') ? '&' : '?'
                        location.href = `${url}${separator}executeCount=${this.executeCount}`
                    })
                }).catch(error => this.$bkMessage({ theme: 'error', message: error.message || error }))
            }
        }
    }
</script>

<style lang="scss" scoped>
    .step-log-panel { position: relative; display: flex; min-height: 0; min-width: 0; flex: 1; background: #2c2d34; color: #c4c6cc; }
    .step-log-body { display: flex; flex: 1; flex-direction: column; min-height: 0; min-width: 0; }
    .log-panel-empty, .log-panel-loading { display: flex; align-items: center; justify-content: center; flex: 1; color: #979ba5; background: #2c2d34; font: 12px/20px "Microsoft YaHei", sans-serif; }
    .log-panel-empty.is-clean { color: #83828c; }
    .log-panel-load-error { display: flex; flex: 1; flex-direction: column; align-items: center; justify-content: center; gap: 12px; color: #83828c; background: #2c2d34; font: 12px/20px "Microsoft YaHei", sans-serif; }
    .log-panel-load-error button { padding: 4px 12px; color: #3a84ff; background: transparent; border: 1px solid #3a84ff; border-radius: 2px; cursor: pointer; }
    .log-panel-load-error button:disabled { opacity: .5; cursor: not-allowed; }
    .log-panel-status-empty { flex: 1; padding: 8px 24px; font: 12px/20px "Microsoft YaHei", sans-serif; color: #979ba5; background: #2c2d34; }
    .log-panel-status-empty .log-line-number { display: inline-block; min-width: 4ch; margin-right: 17px; text-align: right; color: #979ba5; font-variant-numeric: tabular-nums; user-select: none; }
    .log-panel-error { margin: 8px 20px 0; color: #f06e6e; font: 12px/20px "Microsoft YaHei", sans-serif; }
    .loading-spinner { width: 32px; height: 32px; border: 2px solid #4d4f56; border-top-color: #3a84ff; border-radius: 50%; animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    @media (prefers-reduced-motion: reduce) { .loading-spinner { animation: none; } }
</style>
