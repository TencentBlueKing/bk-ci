<template>
    <detail-container
        @close="handleClose"
        :title="headerTitle"
        :status="displayJob.status || ''"
        :current-tab="currentTab"
        :position-code="positionCode"
    >
        <span
            class="head-tab"
            slot="tab"
        >
            <span
                @click="currentTab = 'log'"
                :class="{ active: currentTab === 'log' }"
            >{{ $t('execDetail.log') }}</span>
            <span
                @click="currentTab = 'setting'"
                :class="{ active: currentTab === 'setting' }"
            >{{ $t('execDetail.setting') }}</span>
        </span>
        <log-panel-toolbar
            v-if="!currentJob.matrixGroupFlag && currentTab === 'log'"
            slot="tool"
            :controls="realLogControls"
            :disabled="!shouldMountLog"
            @download="$refs.realLog && $refs.realLog.download()"
            @move="$refs.realLog && $refs.realLog.moveMatch($event)"
        />
        <template v-slot:content>
            <log-conclusion-bar
                v-if="currentTab === 'log' && showConclusion"
                :conclusion="logConclusionModel"
                :pending-action="pendingAction"
                @locate="handleLocateLog"
                @retry="handleStepAction(false)"
                @skip="handleStepAction(true)"
            >
                <template v-slot:prefix-actions>
                    <log-execute-count-select
                        ref="conclusionExecuteCountSelect"
                        v-if="!currentJob.matrixGroupFlag"
                        :latest-count="latestExecuteCount"
                        :active-count="activeExecuteCount"
                        :context-key="`${currentJob.id}|${execDetail.id}`"
                        @execute-change="handleExecuteChange"
                        @focus-return="focusExecuteCountAfterKeyboardSelection"
                    />
                </template>
                <template v-slot:right-actions>
                    <bk-button
                        v-if="showDebugDockerBtn"
                        size="small"
                        outline
                        class="lp-debug-action"
                        @click.stop="handleDebug"
                    >
                        {{ $t('editPage.docker.debugConsole') }}
                    </bk-button>
                </template>
            </log-conclusion-bar>
            <div
                v-if="showStandaloneExecuteCount || showHistoryRecordState"
                class="log-history-detail-state"
            >
                <log-execute-count-select
                    v-if="showStandaloneExecuteCount"
                    ref="conclusionExecuteCountSelect"
                    :latest-count="latestExecuteCount"
                    :active-count="activeExecuteCount"
                    :context-key="`${currentJob.id}|${execDetail.id}`"
                    @execute-change="handleExecuteChange"
                    @focus-return="focusExecuteCountAfterKeyboardSelection"
                />
                <template v-if="showHistoryRecordState">
                    <span role="status">{{ $t(historyRecordState === 'loading' ? 'logPanel.historyDetail.loading' : 'logPanel.historyDetail.unavailable') }}</span>
                    <button
                        v-if="historyRecordState === 'unavailable'"
                        type="button"
                        @click="fetchHistoricalDetail"
                    >
                        {{ $t('logPanel.historyDetail.retry') }}
                    </button>
                </template>
            </div>
            <div
                v-if="currentTab === 'log'
                    && !shouldMountLog
                    && (!isHistoricalExecuteCount || historyRecordState === 'ready')"
                class="log-not-started"
            >
                <span class="log-line-number">1</span>{{ $t('logPanel.jobNotStarted') }}
            </div>
            <template v-if="currentTab === 'log' && shouldMountLog">
                <job-log-panel
                    v-if="!currentJob.matrixGroupFlag"
                    :key="`${currentJob.id}-${activeExecuteCount}`"
                    :project-id="$route.params.projectId"
                    :pipeline-id="$route.params.pipelineId"
                    :build-id="execDetail.id"
                    :plugin-list="displayPluginList"
                    :root-job-id="currentJob.containerHashId"
                    :job-id="currentJob.containerHashId"
                    :download-link="downLoadJobLink"
                    :download-job-id="currentJob.containerHashId"
                    :execute-count="activeExecuteCount"
                    :archive-flag="$route.query.archiveFlag"
                    :controls="realLogControls"
                    ref="realLog"
                    @error="handleLogError"
                    @permission="handleLogPermission"
                    @locate-miss="handleLocateMiss"
                    @issues-change="handleIssuesChange"
                    @condition-skip-reason="handleConditionSkipReason"
                />
                <plugin-log
                    :id="currentJob.containerHashId"
                    :key="currentJob.containerHashId"
                    :build-id="execDetail.id"
                    :exec-detail="execDetail"
                    :current-tab="currentTab"
                    :execute-count="latestExecuteCount"
                    :active-execute-count="activeExecuteCount"
                    type="containerLog"
                    ref="jobLog"
                    @execute-change="handleExecuteChange"
                    v-if="currentJob.matrixGroupFlag"
                />
            </template>
            <container-content
                v-if="currentTab === 'setting'"
                :container-index="editingElementPos.containerIndex"
                :container-group-index="editingElementPos.containerGroupIndex"
                :stage-index="editingElementPos.stageIndex"
                :stages="execDetail.model.stages"
                :editable="false"
                :pipeline="pipeline"
                ref="container"
            />
        </template>
    </detail-container>
</template>

<script>
    import { mapGetters } from 'vuex'
    import pluginLog from './log/pluginLog'
    import detailContainer from './detailContainer'
    import ContainerContent from '@/components/ContainerPropertyPanel/ContainerContent'
    import { convertMStoString } from '@/utils/util'
    import LogConclusionBar from './log-panel/LogConclusionBar'
    import { buildConclusion, countConditionSkippedSteps, isJobConditionSkipped, resolveCancelConclusion, resolveCancelDisplay, resolveConclusionElapsed, resolveElapsedTimestamp, resolveJobQueueDetail, resolveNodeLogError, resolveJobFailureStep, isJobExecTimeoutCancel, isJobSystemAgentTermination, resolveJobExecTimeoutCode, resolveFallbackCancelLabelKey, resolveCancelUserId, isTaskElapsedStatus, isConcurrencyQueueCandidate, isVisibleConcurrencyQueueResponse, canShowJobConclusion, shouldClearQueueAfterError, buildHistoricalRecordRequest, validateHistoricalLogRecord, resolveHistoricalJobSnapshot, shouldMountLogPanel, shouldShowLocateLog, shouldShowStandaloneExecuteCount, resolveJobDirectoryItems, encodeDownloadFileName } from './log-panel/logPanelAdapter'
    import JobLogPanel from './log-panel/JobLogPanel'
    import LogPanelToolbar from './log-panel/LogPanelToolbar'
    import LogExecuteCountSelect from './log-panel/LogExecuteCountSelect'
    import { RESOURCE_ACTION, handlePipelineNoPermission } from '@/utils/permission'

    export default {
        components: {
            detailContainer,
            pluginLog,
            ContainerContent,
            JobLogPanel,
            LogPanelToolbar,
            LogExecuteCountSelect,
            LogConclusionBar
        },
        props: {
            execDetail: {
                type: Object,
                required: true
            },
            editingElementPos: {
                type: Object,
                required: true
            },
            pipeline: {
                type: Object
            },
            actionsEnabled: {
                type: Boolean,
                default: true
            },
            isLatestBuild: {
                type: Boolean,
                default: true
            }
        },
        data () {
            return {
                showTime: false,
                searchStr: '',
                currentTab: 'log',
                selectedExecuteCount: null,
                historyExecDetail: null,
                historyJobSnapshot: null,
                historyRecordState: 'idle',
                historyRequestId: 0,
                realLogControls: { levels: ['INFO', 'WARN', 'ERROR'], levelAll: false, search: '', current: 0, total: 0, showTime: false, wrap: true },
                realLogError: '',
                pendingAction: '',
                logIssuesAvailable: false,
                conditionSkipReason: '',
                elapsedNow: Date.now(),
                elapsedTimer: null,
                queueResponse: null,
                queueReceivedAt: 0,
                queueTimer: null,
                queueRequestId: 0,
                queueClosed: false,
                queueForbidden: false
            }
        },

        computed: {

            ...mapGetters('atom', [
                'checkShowDebugDockerBtn'
            ]),

            downLoadJobLink () {
                const editingElementPos = this.editingElementPos
                const fileName = encodeDownloadFileName(`${editingElementPos.stageIndex + 1}-${editingElementPos.containerIndex + 1}-${this.currentJob.name}`)
                const jobId = this.currentJob.containerHashId
                return `${API_URL_PREFIX}/log/api/user/logs/${this.$route.params.projectId}/${this.$route.params.pipelineId}/${this.execDetail.id}/download?jobId=${jobId}&fileName=${fileName}`
            },

            currentJob () {
                const { editingElementPos, execDetail } = this
                const model = execDetail.model || {}
                const stages = model.stages || []
                const currentStage = stages[editingElementPos.stageIndex] || []

                try {
                    const job = editingElementPos.containerGroupIndex === undefined
                        ? currentStage.containers[editingElementPos.containerIndex]
                        : currentStage.containers[editingElementPos.containerIndex].groupContainers[editingElementPos.containerGroupIndex]
                    return job || {}
                } catch (_) {
                    return {}
                }
            },

            supportsLocalHistory () {
                return !this.currentJob.matrixGroupFlag
            },

            isHistoricalExecuteCount () {
                return this.selectedExecuteCount !== null
                    && this.supportsLocalHistory
                    && Number.isSafeInteger(Number(this.selectedExecuteCount))
                    && Number(this.selectedExecuteCount) >= 1
                    && Number(this.selectedExecuteCount) < Number(this.latestExecuteCount)
            },

            historyRequestContext () {
                const { stageIndex, containerIndex, containerGroupIndex } = this.editingElementPos
                const stage = this.execDetail?.model?.stages?.[stageIndex]
                const parent = stage?.containers?.[containerIndex]
                const target = containerGroupIndex === undefined ? parent : parent?.groupContainers?.[containerGroupIndex]
                return {
                    projectId: this.$route.params.projectId,
                    pipelineId: this.$route.params.pipelineId,
                    buildNo: this.execDetail.id,
                    executeCount: Number(this.activeExecuteCount),
                    archiveFlag: this.$route.query.archiveFlag,
                    stageId: stage?.id,
                    containerHashId: parent?.containerHashId,
                    containerId: parent?.containerId,
                    containerLegacyId: parent?.id,
                    targetHashId: target?.containerHashId,
                    targetId: target?.id,
                    targetContainerId: target?.containerId
                }
            },

            historyContextKey () {
                return JSON.stringify({ request: this.historyRequestContext, supported: this.supportsLocalHistory })
            },

            displayJob () {
                if (!this.isHistoricalExecuteCount) return this.currentJob
                return this.historyRecordState === 'ready' ? (this.historyJobSnapshot?.container || {}) : {}
            },

            displayExecDetail () {
                if (!this.isHistoricalExecuteCount) return this.execDetail
                return this.historyRecordState === 'ready' ? (this.historyExecDetail || {}) : {}
            },

            displayPluginList () {
                if (!this.isHistoricalExecuteCount) return resolveJobDirectoryItems(this.pluginList, this.activeExecuteCount)
                if (this.historyRecordState === 'ready' && this.historyJobSnapshot?.container) {
                    const job = this.historyJobSnapshot.container
                    return resolveJobDirectoryItems([
                        { ...this.pluginList[0], status: job.startVMStatus, executeCount: job.executeCount },
                        ...(Array.isArray(job.elements) ? job.elements : [])
                    ], this.activeExecuteCount)
                }
                return this.pluginList.map(item => ({ ...item, status: '', timeCost: null, elapsed: null, startTime: null, startEpoch: null, endTime: null }))
            },

            displayConclusionAvailable () {
                return !this.isHistoricalExecuteCount || this.historyRecordState === 'ready'
            },

            showConclusion () {
                return this.displayConclusionAvailable
                    && (this.isHistoricalExecuteCount ? this.displayCanShowConclusion : this.conclusionVisible && this.canShowConclusion)
            },

            showStandaloneExecuteCount () {
                return shouldShowStandaloneExecuteCount({
                    tab: this.currentTab,
                    showConclusion: this.showConclusion,
                    latestCount: this.latestExecuteCount,
                    matrix: this.currentJob.matrixGroupFlag
                })
            },

            showHistoryRecordState () {
                return this.currentTab === 'log' && this.isHistoricalExecuteCount && this.historyRecordState !== 'ready'
            },

            setupFailed () {
                return ['FAILED', 'EXEC_TIMEOUT', 'HEARTBEAT_TIMEOUT'].includes(String(this.displayJob.startVMStatus || '').toUpperCase())
            },

            headerTitle () {
                const title = (this.displayJob.name || (this.isHistoricalExecuteCount ? this.currentJob.name : '')) || ''
                return !this.isHistoricalExecuteCount && this.queueDetailModel ? `${title} · ${this.$t('logPanel.queue.mutexQueue')}` : title
            },

            positionCode () {
                const { stageIndex, containerIndex } = this.editingElementPos
                return `${stageIndex + 1}-${containerIndex + 1}`
            },

            latestExecuteCount () {
                return this.currentJob.matrixGroupFlag ? this.currentJob.executeCount : this.executeCount
            },

            durationText () {
                const duration = resolveConclusionElapsed(this.displayJob, { now: this.elapsedNow, preferStartTime: true })
                return duration && duration !== '0s' ? duration : ''
            },
            jobErrorSummary () {
                const status = String(this.displayJob.status || '').toUpperCase()
                const canDisplayConclusion = this.isHistoricalExecuteCount ? this.displayConclusionAvailable : this.conclusionVisible
                if (!canDisplayConclusion || !['FAILED', 'HEARTBEAT_TIMEOUT', 'EXEC_TIMEOUT'].includes(status)) return null
                return resolveNodeLogError(this.displayExecDetail.errorInfoList, { id: '', containerId: this.displayJob.id }, this.displayJob)
            },
            jobFailureStep () {
                const job = this.displayJob
                const error = this.jobErrorSummary
                const canDisplayConclusion = this.isHistoricalExecuteCount ? this.displayConclusionAvailable : this.conclusionVisible
                if (!canDisplayConclusion || String(job.status || '').toUpperCase() !== 'FAILED' || this.setupFailed || error?.errorCode || error?.errorMsg) return null
                const step = resolveJobFailureStep(job.elements, this.displayExecDetail.errorInfoList, job)
                return step && (step.name || step.taskName) ? step : null
            },
            retryableStep () {
                return (Array.isArray(this.displayJob.elements) ? this.displayJob.elements : [])
                    .find(step => step?.canRetry || step?.canSkip) || null
            },
            locateMessages () {
                const canDisplayConclusion = this.isHistoricalExecuteCount ? this.displayConclusionAvailable : this.conclusionVisible
                if (!canDisplayConclusion || !['FAILED', 'HEARTBEAT_TIMEOUT', 'EXEC_TIMEOUT'].includes(String(this.displayJob.status || '').toUpperCase())) return []
                const ownError = this.jobErrorSummary
                if (ownError?.errorCode || ownError?.errorMsg) {
                    return [[ownError.errorCode, ownError.errorMsg].filter(Boolean).join('\n')]
                }
                const step = this.jobFailureStep
                if (!step) return []
                const stepError = resolveNodeLogError(this.displayExecDetail.errorInfoList, { id: step.id, containerId: this.displayJob.id }, step)
                return stepError?.errorCode || stepError?.errorMsg
                    ? [[stepError.errorCode, stepError.errorMsg].filter(Boolean).join('\n')]
                    : []
            },
            queueDetailModel () {
                return resolveJobQueueDetail(this.queueResponse, {
                    projectId: this.$route.params.projectId,
                    formatDuration: convertMStoString,
                    receivedAt: this.queueReceivedAt,
                    now: this.elapsedNow
                })
            },
            logConclusionModel () {
                const job = this.displayJob
                const displayDetail = this.displayExecDetail
                const cancelConclusion = resolveCancelConclusion(displayDetail.buildEndInfo, { containerId: job.containerId || job.id, status: job.status || '' })
                const jobTimeoutCancel = isJobExecTimeoutCancel(displayDetail.buildEndInfo) && Boolean(cancelConclusion)
                const jobTimeoutCode = jobTimeoutCancel ? '' : resolveJobExecTimeoutCode(job, displayDetail.errorInfoList)
                const jobTimeoutEvidence = jobTimeoutCancel || Boolean(jobTimeoutCode)
                const element = {
                    status: job.status,
                    elapsed: job.timeCost?.totalCost,
                    startTime: job.startTime,
                    startEpoch: job.startEpoch,
                    endTime: job.endTime,
                    errorCode: this.jobErrorSummary?.errorCode || job.errorCode || jobTimeoutCode,
                    errorMsg: this.jobErrorSummary?.errorMsg || job.errorMsg,
                    timeCost: job.timeCost
                }
                const gated = this.actionsEnabled && !this.$route.query?.archiveFlag && this.conclusionVisible && !this.isHistoricalExecuteCount
                const systemCanceledEvidence = !this.jobFailureStep && isJobSystemAgentTermination(job, displayDetail.errorInfoList)
                const cancelDisplay = jobTimeoutEvidence ? null : cancelConclusion
                    ? resolveCancelDisplay(cancelConclusion, displayDetail.buildEndInfo, (key, values) => this.$t(key, values))
                    : null
                const effectiveStatus = jobTimeoutEvidence ? 'EXEC_TIMEOUT' : job.status
                const conditionSkipped = isJobConditionSkipped(job)
                const conditionSkipCount = countConditionSkippedSteps(job.elements)
                const model = buildConclusion({ ...element, status: effectiveStatus }, job, {
                    projectId: this.$route.params.projectId,
                    now: this.elapsedNow,
                    preferStartTime: true,
                    cancelUserId: displayDetail.cancelUserId || '',
                    effectiveStatus,
                    jobStatus: effectiveStatus,
                    container: job,
                    hideNode: this.setupFailed,
                    canRetry: gated && !!this.retryableStep?.canRetry,
                    canSkip: gated && !!this.retryableStep?.canSkip,
                    locateLog: shouldShowLocateLog(effectiveStatus) && Boolean(this.jobFailureStep || this.jobErrorSummary || this.setupFailed)
                })
                const fallbackCancelKey = cancelConclusion ? '' : resolveFallbackCancelLabelKey({ status: effectiveStatus, cancelUserId: model.cancelUserId, buildEndInfo: displayDetail.buildEndInfo })
                const key = `details.statusMap.${effectiveStatus}`
                const translated = this.$t(key)
                const queueDetail = this.isHistoricalExecuteCount ? null : this.queueDetailModel
                return {
                    ...model,
                    tone: cancelDisplay || systemCanceledEvidence && !jobTimeoutEvidence ? 'canceled' : model.tone,
                    label: jobTimeoutEvidence
                        ? translated
                        : cancelDisplay
                            ? cancelDisplay.label
                            : fallbackCancelKey
                                ? this.$t(`logPanel.status.${fallbackCancelKey}`)
                                : systemCanceledEvidence
                                    ? this.$t('logPanel.status.systemCanceled')
                                    : queueDetail
                                        ? job.status === 'LOOP_WAITING' ? job.status : this.$t('logPanel.queue.mutexQueue')
                                        : translated === key ? job.status : translated,
                    message: jobTimeoutCancel
                        ? displayDetail.buildEndInfo?.reason || model.message
                        : jobTimeoutCode
                            ? this.$t('logPanel.status.jobSelfTimeout', [model.timeoutThreshold])
                            : systemCanceledEvidence
                                ? this.$t('logPanel.status.systemCanceledDetail')
                                : cancelDisplay
                                    ? cancelDisplay.message
                                    : queueDetail
                                        ? (queueDetail.running.length ? this.$t('logPanel.queue.waitingReason') : '')
                                        : this.jobFailureStep ? this.$t('logPanel.status.failedByStep', [this.jobFailureStep.name || this.jobFailureStep.taskName || ''])
                                            : model.message,
                    statusMessageKey: queueDetail || conditionSkipped || conditionSkipCount ? '' : model.statusMessageKey,
                    cancelUserId: cancelDisplay?.cancelUserId || resolveCancelUserId(cancelConclusion, model.cancelUserId),
                    parentBuildLabel: cancelConclusion?.type === 'parent' ? 'logPanel.status.parentBuild' : '',
                    parentBuildUrl: cancelDisplay?.parentBuildUrl || cancelConclusion?.parentBuildUrl || '',
                    queueDetail,
                    queueExpanded: false,
                    conditionSkipSummary: conditionSkipped
                        ? this.conditionSkipReason || this.$t('logPanel.status.conditionSkipped')
                        : conditionSkipCount ? this.$t('logPanel.status.conditionSkipSummary', [conditionSkipCount]) : '',
                    showNodeMessageDivider: Boolean(model.node && this.jobFailureStep && (this.jobFailureStep.name || this.jobFailureStep.taskName))
                }
            },
            conclusionVisible () {
                return this.selectedExecuteCount === null || Number(this.selectedExecuteCount) === Number(this.latestExecuteCount)
            },
            activeExecuteCount () {
                return this.selectedExecuteCount ?? this.latestExecuteCount
            },
            queueContextKey () {
                return `${this.$route.params.projectId}|${this.$route.params.pipelineId}|${this.execDetail.id}|${this.currentJob.id}`
            },
            isQueueCandidate () {
                return isConcurrencyQueueCandidate(this.execDetail.status)
            },
            canRenderLog () {
                if (this.isHistoricalExecuteCount) return true
                return !['UNEXEC', 'WAITING', 'DISABLED'].includes(this.currentJob.status)
            },
            shouldMountLog () {
                return shouldMountLogPanel({
                    isHistoricalExecuteCount: this.isHistoricalExecuteCount,
                    historyRecordState: this.historyRecordState,
                    status: this.isHistoricalExecuteCount ? this.displayJob.status : this.currentJob.status
                })
            },
            canShowConclusion () {
                return canShowJobConclusion(this.currentJob.status, this.queueDetailModel)
                    || Boolean(resolveCancelConclusion(this.execDetail.buildEndInfo, {
                        containerId: this.currentJob.containerId || this.currentJob.id,
                        status: this.currentJob.status || ''
                    }))
            },

            displayCanShowConclusion () {
                return this.isHistoricalExecuteCount
                    ? this.historyRecordState === 'ready' && canShowJobConclusion(this.displayJob.status)
                    : this.canShowConclusion
            },

            pluginList () {
                const startUp = {
                    name: this.$t('logPanel.setupJob'),
                    status: this.currentJob.startVMStatus,
                    id: `startVM-${this.currentJob.id}`,
                    isSetup: true,
                    executeCount: this.currentJob.executeCount || 1
                }
                return [startUp, ...this.currentJob.elements]
            },
            showDebugDockerBtn () {
                return this.checkShowDebugDockerBtn(this.currentJob, this.$route.name, this.execDetail)
                    && this.isLatestBuild
                    && Number(this.activeExecuteCount) === Number(this.latestExecuteCount)
            },

            executeCount () {
                const executeCountList = this.pluginList.map((plugin) => plugin.executeCount || 1)
                return Math.max(...executeCountList)
            }
        },
        watch: {
            'currentJob.id' () {
                this.currentTab = 'log'
                this.logIssuesAvailable = false
                this.conditionSkipReason = ''
                this.realLogControls = { levels: ['INFO', 'WARN', 'ERROR'], levelAll: false, search: '', current: 0, total: 0, showTime: false, wrap: true }
                this.selectedExecuteCount = null
            },
            historyContextKey () {
                this.invalidateHistoricalDetail()
                if (this.isHistoricalExecuteCount) this.fetchHistoricalDetail()
            },
            'execDetail.status' (status, previousStatus) {
                this.startElapsedTimer()
                this.refreshQueueForStatus(previousStatus)
            },
            'currentJob.status' () {
                this.startElapsedTimer()
            },
            activeExecuteCount () {
                this.conditionSkipReason = ''
            },
            queueContextKey () {
                this.resetQueueContext()
            }
        },
        mounted () {
            this.startElapsedTimer()
            this.resetQueueContext()
        },
        beforeDestroy () {
            this.invalidateHistoricalDetail()
            this.stopElapsedTimer()
            this.invalidateQueueRequests()
        },
        methods: {
            invalidateHistoricalDetail () {
                this.historyRequestId += 1
                this.historyExecDetail = null
                this.historyJobSnapshot = null
                this.historyRecordState = 'idle'
            },
            isHistoricalDetailRequestCurrent (requestId, contextKey) {
                return requestId === this.historyRequestId
                    && contextKey === this.historyContextKey
                    && this.isHistoricalExecuteCount
                    && !this._isDestroyed
            },
            async fetchHistoricalDetail () {
                if (!this.isHistoricalExecuteCount) return
                const requestId = ++this.historyRequestId
                const contextKey = this.historyContextKey
                const context = this.historyRequestContext
                this.historyExecDetail = null
                this.historyJobSnapshot = null
                this.historyRecordState = 'loading'
                try {
                    const detail = await this.$store.dispatch('atom/fetchPipelineExecDetail', buildHistoricalRecordRequest(context))
                    if (!this.isHistoricalDetailRequestCurrent(requestId, contextKey)) return
                    const valid = validateHistoricalLogRecord(detail, {
                        ...context,
                        generation: requestId,
                        currentGeneration: this.historyRequestId,
                        contextKey,
                        currentContextKey: this.historyContextKey
                    })
                    const snapshot = valid
                        ? resolveHistoricalJobSnapshot(this.execDetail, detail, this.editingElementPos, context.executeCount)
                        : null
                    if (!snapshot) {
                        this.historyRecordState = 'unavailable'
                        return
                    }
                    this.historyExecDetail = detail
                    this.historyJobSnapshot = snapshot
                    this.historyRecordState = 'ready'
                } catch (_) {
                    if (this.isHistoricalDetailRequestCurrent(requestId, contextKey)) this.historyRecordState = 'unavailable'
                }
            },
            focusExecuteCountAfterKeyboardSelection (contextKey) {
                const currentContextKey = `${this.currentJob.id}|${this.execDetail.id}`
                if (contextKey !== currentContextKey || this.currentTab !== 'log' || this.currentJob.matrixGroupFlag) return
                this.$nextTick(() => {
                    if (contextKey !== `${this.currentJob.id}|${this.execDetail.id}` || this.currentTab !== 'log' || this.currentJob.matrixGroupFlag) return
                    this.$refs.conclusionExecuteCountSelect?.focusTrigger?.()
                })
            },
            handleIssuesChange (hasIssues) {
                this.logIssuesAvailable = hasIssues
            },
            handleConditionSkipReason (reason) {
                if (reason) this.conditionSkipReason = reason
            },
            handleLocateLog () {
                const itemId = this.jobFailureStep?.id
                    || (this.setupFailed ? this.displayPluginList[0]?.id : this.$refs.realLog?.selectedId)
                if (!itemId) return
                const messages = this.locateMessages
                if (messages.length) this.$refs.realLog?.locateItem?.(itemId, messages)
                else this.$refs.realLog?.locateItem?.(itemId)
            },
            handleLocateMiss () {
                if (this.$bkMessage) this.$bkMessage({ theme: 'warning', message: this.$t('logPanel.locateSummaryMiss'), limit: 1 })
            },
            handleStepAction (skip) {
                if (this.pendingAction || !this.retryableStep) return
                const action = skip ? 'skip' : 'retry'
                this.pendingAction = action
                this.$emit('continue', {
                    taskId: this.retryableStep.id,
                    skip
                }, () => {
                    this.pendingAction = ''
                })
            },
            startElapsedTimer () {
                this.stopElapsedTimer()
                const start = resolveElapsedTimestamp(this.currentJob.startEpoch)
                    ?? resolveElapsedTimestamp(this.currentJob.startTime)
                const hasStart = start !== null
                if ((isTaskElapsedStatus(this.currentJob.status) && hasStart) || this.queueResponse?.visible === true) {
                    this.elapsedNow = Date.now()
                    this.elapsedTimer = setInterval(() => {
                        this.elapsedNow = Date.now()
                    }, 1000)
                }
            },
            stopElapsedTimer () {
                if (this.elapsedTimer) clearInterval(this.elapsedTimer)
                this.elapsedTimer = null
            },
            clearQueueTimer () {
                if (this.queueTimer) clearTimeout(this.queueTimer)
                this.queueTimer = null
            },
            invalidateQueueRequests () {
                this.clearQueueTimer()
                this.queueRequestId += 1
            },
            resetQueueContext () {
                this.invalidateQueueRequests()
                this.queueResponse = null
                this.queueClosed = false
                this.queueForbidden = false
                if (this.isQueueCandidate) this.fetchConcurrencyQueue()
            },
            refreshQueueForStatus (previousStatus) {
                this.clearQueueTimer()
                if (this.isQueueCandidate) this.fetchConcurrencyQueue()
                else if (isConcurrencyQueueCandidate(previousStatus)) this.fetchConcurrencyQueue(true)
                else this.invalidateQueueRequests()
            },
            isQueueRequestCurrent (requestId, contextKey) {
                return !this._isDestroyed && !this.queueClosed && requestId === this.queueRequestId && contextKey === this.queueContextKey
            },
            scheduleQueuePoll (contextKey) {
                this.clearQueueTimer()
                if (!this.isQueueCandidate || this.queueClosed || this.queueForbidden) return
                this.queueTimer = setTimeout(() => {
                    if (contextKey === this.queueContextKey) this.fetchConcurrencyQueue()
                }, 3000)
            },
            async fetchConcurrencyQueue (finalRequest = false) {
                if (this.queueClosed) return
                const requestId = ++this.queueRequestId
                const contextKey = this.queueContextKey
                const { projectId, pipelineId } = this.$route.params
                try {
                    const response = await this.$store.dispatch('pipelines/requestConcurrencyQueue', {
                        projectId,
                        pipelineId,
                        buildId: this.execDetail.id
                    })
                    if (!this.isQueueRequestCurrent(requestId, contextKey)) return
                    this.queueResponse = isVisibleConcurrencyQueueResponse(response) ? response : null
                    this.queueReceivedAt = this.queueResponse ? Date.now() : 0
                    this.startElapsedTimer()
                } catch (error) {
                    if (!this.isQueueRequestCurrent(requestId, contextKey)) return
                    if (shouldClearQueueAfterError(finalRequest)) {
                        this.queueResponse = null
                        this.queueReceivedAt = 0
                        this.startElapsedTimer()
                    }
                    const status = Number(error?.response?.status || error?.httpStatus || error?.status || error?.code)
                    if (status === 403) {
                        this.queueResponse = null
                        this.queueForbidden = true
                        this.clearQueueTimer()
                        this.handleLogPermission()
                    }
                } finally {
                    if (this.isQueueRequestCurrent(requestId, contextKey) && !finalRequest) this.scheduleQueuePoll(contextKey)
                }
            },
            handleClose () {
                this.queueClosed = true
                this.queueResponse = null
                this.invalidateQueueRequests()
                this.$emit('close')
            },
            handleLogError (error) {
                this.realLogError = error?.message || String(error || '')
                if (this.$bkMessage) this.$bkMessage({ theme: 'error', message: this.realLogError, limit: 1 })
            },
            handleLogPermission () {
                handlePipelineNoPermission({
                    projectId: this.$route.params.projectId,
                    resourceCode: this.$route.params.pipelineId,
                    action: RESOURCE_ACTION.VIEW
                })
            },
            handleExecuteChange (executeCount) {
                const count = Number(executeCount)
                this.realLogControls.search = ''
                this.realLogControls.current = 0
                this.realLogControls.total = 0
                this.selectedExecuteCount = count === Number(this.latestExecuteCount) ? null : count
            },
            handleDebug () {
                if (this.$refs.container) {
                    this.$refs.container.startDebug()
                    return
                }
                this.currentTab = 'setting'
                this.$nextTick(() => {
                    this.$refs.container?.startDebug?.()
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .log-history-detail-state {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 16px;
        color: #979ba5;
        font-size: 12px;
    }

    .log-history-detail-state button {
        padding: 0;
        color: #3a84ff;
        background: transparent;
        border: 0;
        cursor: pointer;
    }

    ::v-deep .container-property-panel {
        padding: 10px 50px;
        overflow: auto;
        .bk-form-item.is-required .bk-label, .bk-form-inline-item.is-required .bk-label {
            margin-right: 10px;
        }
    }
    .head-tool {
        line-height: 1.4;
    }
</style>
