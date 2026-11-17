<template>
    <div>
        <detail-container
            @close="$emit('close')"
            :title="displayElement.name || (isHistoricalExecuteCount ? currentElement.name : '')"
            :status="displayElement.status || ''"
            :current-tab="currentTab"
            :position-code="positionCode"
            :is-hook="((displayElement.additionalOptions || {}).elementPostInfo || false)"
        >
            <span
                class="head-tab"
                slot="tab"
                v-if="showHeaderTabs"
            >
                <template v-for="tab in sortedTabList">
                    <span
                        v-if="tab.show && (tab.name !== 'progress' || canShowProgressTab)"
                        :key="tab.name"
                        :class="{ active: currentTab === tab.name }"
                        role="tab"
                        tabindex="0"
                        :aria-selected="String(currentTab === tab.name)"
                        @keydown.enter="selectTab(tab.name)"
                        @keydown.space.prevent="selectTab(tab.name)"
                        @click="selectTab(tab.name)"
                    >{{ tabLabel(tab.name) }}</span>
                </template>
            </span>
            <div
                v-if="paramsViewEnabled && currentTab === 'setting' && settingView === 'config'"
                slot="tool"
                class="plugin-setting-tools"
            >
                <bk-button
                    text
                    theme="primary"
                    class="head-tool"
                    @click="showParamsView"
                >
                    {{ $t('logPanel.params.viewParams') }}
                </bk-button>
            </div>
            <log-panel-toolbar
                v-if="currentTab === 'log'"
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
                    :progress="displayProgressPercent"
                    :has-subtasks="canExpandProgressSubtasks && !!displayProgressSubtasks.length"
                    :progress-expanded="canExpandProgressSubtasks && progressExpanded"
                    :subtasks="canExpandProgressSubtasks ? displayProgressSubtasks : []"
                    :pending-action="pendingAction"
                    @toggle-progress="toggleProgressExpanded"
                    @locate="handleLocateLog"
                    @continue-config="handleMockablePanelAction('continue-config')"
                    @review="handleMockablePanelAction('review')"
                    @retry="handleStepAction(false)"
                    @skip="handleStepAction(true)"
                    @go-condition="handleMockablePanelAction('condition')"
                >
                    <template v-slot:prefix-actions>
                        <log-execute-count-select
                            ref="conclusionExecuteCountSelect"
                            :latest-count="currentElement.executeCount"
                            :active-count="activeExecuteCount"
                            :context-key="`${currentElement.id}|${execDetail.id}`"
                            @execute-change="handleExecuteChange"
                            @focus-return="focusExecuteCountAfterKeyboardSelection"
                        />
                    </template>
                </log-conclusion-bar>
                <div
                    v-if="showStandaloneExecuteCount || showHistoryRecordState"
                    class="log-history-detail-state"
                >
                    <log-execute-count-select
                        v-if="showStandaloneExecuteCount"
                        ref="conclusionExecuteCountSelect"
                        :latest-count="currentElement.executeCount"
                        :active-count="activeExecuteCount"
                        :context-key="`${currentElement.id}|${execDetail.id}`"
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
                        && (!isHistoricalExecuteCount || historyRecordState === 'ready')
                        && displayElement.status === 'UNEXEC'"
                    class="log-not-started"
                    role="status"
                >
                    {{ $t('logPanel.status.notExecuted') }}
                </div>
                <div
                    v-else-if="currentTab === 'log'
                        && !shouldMountLog
                        && (!isHistoricalExecuteCount || historyRecordState === 'ready')"
                    class="log-not-started"
                >
                    <span class="log-line-number">1</span>{{ $t('logPanel.stepNotStarted') }}
                </div>
                <step-log-panel
                    v-if="currentTab === 'log' && shouldMountLog"
                    :key="logContext"
                    :locate-request="locateRequest"
                    :locate-request-context="locateRequestContext"
                    :log-context="logContext"
                    :locate-target="locateTarget"
                    :project-id="$route.params.projectId"
                    :pipeline-id="$route.params.pipelineId"
                    :build-id="execDetail.id"
                    :tag="currentElement.id"
                    :execute-count="activeExecuteCount"
                    :task-status="displayElement.status"
                    :download-link="realDownloadLink"
                    :download-tag="currentElement.id"
                    :archive-flag="$route.query.archiveFlag"
                    :empty-line-text="stepEmptyLineText"
                    :controls="realLogControls"
                    ref="realLog"
                    @error="handleLogError"
                    @permission="handleLogPermission"
                    @issues-change="handleIssuesChange"
                    @condition-skip-reason="handleConditionSkipReason"
                    @locatable-change="handleLocateAvailability"
                    @locate-miss="handleLocateMiss"
                />
                <log-params-view
                    v-if="currentTab === 'setting' && settingView === 'params'"
                    :sections="paramsSections"
                    :not-executed="paramsNotExecuted"
                    :loading="paramsLoading"
                    :load-error="paramsLoadError"
                    show-config-shortcut
                    :shortcut-label="$t('logPanel.params.backConfig')"
                    @retry="fetchParamsRecord"
                    @view-config="showConfigView"
                />
                <component
                    v-show="currentTab === key && (key !== 'setting' || settingView === 'config')"
                    :is="value.component"
                    v-bind="value.bindData"
                    v-for="(value, key) in componentList"
                    :key="key"
                    :ref="key"
                    @toggle="(show) => toggleTab(key, show)"
                    @complete="completeLoading(key)"
                    @retry="key === 'progress' && fetchProgress()"
                ></component>
            </template>
        </detail-container>
    </div>
</template>

<script>
    import AtomContent from '@/components/AtomPropertyPanel/AtomContent.vue'
    import { mapActions } from 'vuex'
    import Artifactory from './Artifactory'
    import Report from './Report'
    import detailContainer from './detailContainer'
    import ProgressDetailPanel from '@/components/ProgressDetailPanel'
    import { convertMStoString } from '@/utils/util'
    import StepLogPanel from './log-panel/StepLogPanel'
    import LogPanelToolbar from './log-panel/LogPanelToolbar'
    import LogConclusionBar from './log-panel/LogConclusionBar'
    import LogExecuteCountSelect from './log-panel/LogExecuteCountSelect'
    import LogParamsView from './log-panel/LogParamsView'
    import {
        positionCode,
        shouldShowProgressTab,
        buildConclusion,
        shouldLoadStepLog,
        buildParamsRecordRequest,
        buildParamsModel,
        hasCustomRunCondition,
        parseTaskProgressPercent,
        parseTaskProgressSubtasks,
        resolveDisplayProgressPercent,
        resolveParamsElementSource,
        shouldApplyProgressResponse,
        shouldFetchParamsRecord,
        shouldFetchTaskProgress,
        shouldScheduleProgressPoll,
        resolveNodeLogError,
        isJobTimeoutTerminatedStep,
        isSystemAgentTermination,
        isConditionSkipped,
        resolveCancelConclusion,
        resolveCancelDisplay,
        resolveCancelUserId,
        resolveFallbackCancelLabelKey,
        resolveRecordElementStatus,
        isJobExecTimeoutCancel,
        isTaskElapsedStatus,
        resolveElapsedTimestamp,
        shouldMountLogPanel,
        shouldShowLocateLog,
        shouldShowStandaloneExecuteCount,
        resolveConclusionDisplayMessage,
        buildHistoricalRecordRequest,
        validateHistoricalLogRecord,
        resolveHistoricalPluginSnapshot
    } from './log-panel/logPanelAdapter'
    import { PROCESS_API_URL_PREFIX } from '@/store/constants'
    import { RESOURCE_ACTION, handlePipelineNoPermission } from '@/utils/permission'

    export default {
        components: {
            detailContainer,
            AtomContent,
            ProgressDetailPanel,
            StepLogPanel,
            LogPanelToolbar,
            LogConclusionBar,
            LogExecuteCountSelect,
            LogParamsView
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
            properties: {
                type: Array,
                default: () => ['LOG', 'ARTIFACT', 'CONFIG']
            },
            actionsEnabled: {
                type: Boolean,
                default: true
            }
        },
        data () {
            return {
                currentTab: 'log',
                settingView: 'config',
                paramsViewEnabled: false,
                selectedExecuteCount: null,
                historyExecDetail: null,
                historyPluginSnapshot: null,
                historyRecordState: 'idle',
                historyRequestId: 0,
                pendingAction: '',
                elapsedNow: Date.now(),
                elapsedTimer: null,
                realLogControls: { levels: ['INFO', 'WARN', 'ERROR'], levelAll: false, search: '', current: 0, total: 0, showTime: false, wrap: true },
                realLogError: '',
                paramsExecDetail: null,
                paramsRequestId: 0,
                paramsLoading: false,
                paramsLoadError: '',
                progressExpanded: false,
                progressExpandInitKey: '',
                progressPercent: null,
                progressSubtasks: [],
                progressSubtasksSnapshot: [],
                progressDetailData: null,
                progressLoading: false,
                progressError: false,
                locateRequest: 0,
                locateRequestContext: '',
                progressRequestId: 0,
                progressTimer: null,
                logIssuesAvailable: false,
                conditionSkipReason: '',
                logSummaryAvailable: false,
                tabList: [
                    { name: 'progress', show: true },
                    { name: 'log', show: true },
                    { name: 'artifactory', show: false, completeLoading: false },
                    { name: 'setting', show: true },
                    { name: 'report', show: false, completeLoading: false }
                ]
            }
        },
        computed: {
            realDownloadLink () {
                const pos = this.editingElementPos
                const name = encodeURI(encodeURI(`${pos.stageIndex + 1}-${pos.containerIndex + 1}-${pos.elementIndex + 1}-${this.currentElement.name}`))
                return `${API_URL_PREFIX}/log/api/user/logs/${this.$route.params.projectId}/${this.$route.params.pipelineId}/${this.execDetail.id}/download?tag=${this.currentElement.id}&fileName=${name}`
            },
            stages () {
                return this.execDetail.model.stages
            },

            container () {
                const {
                    editingElementPos: { stageIndex, containerIndex, containerGroupIndex },
                    execDetail: { model: { stages } }
                } = this
                try {
                    const container = containerGroupIndex !== undefined
                        ? stages[stageIndex].containers[containerIndex].groupContainers[containerGroupIndex]
                        : stages[stageIndex].containers[containerIndex]
                    return container || {}
                } catch (_) {
                    return {}
                }
            },

            currentElement () {
                const {
                    editingElementPos: { elementIndex }
                } = this
                return this.container.elements?.[elementIndex] ?? {}
            },

            supportsLocalHistory () {
                return true
            },

            isHistoricalExecuteCount () {
                return this.selectedExecuteCount !== null
                    && this.supportsLocalHistory
                    && Number.isSafeInteger(Number(this.selectedExecuteCount))
                    && Number(this.selectedExecuteCount) >= 1
                    && Number(this.selectedExecuteCount) < Number(this.currentElement.executeCount)
            },

            historyRequestContext () {
                const { stageIndex, containerIndex, containerGroupIndex, elementIndex } = this.editingElementPos
                const stage = this.execDetail?.model?.stages?.[stageIndex]
                const parent = stage?.containers?.[containerIndex]
                const container = containerGroupIndex === undefined ? parent : parent?.groupContainers?.[containerGroupIndex]
                const element = container?.elements?.[elementIndex]
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
                    targetHashId: container?.containerHashId,
                    targetId: container?.id,
                    targetContainerId: container?.containerId,
                    elementId: element?.id
                }
            },

            historyContextKey () {
                return JSON.stringify({ request: this.historyRequestContext, supported: this.supportsLocalHistory })
            },

            displayContainer () {
                if (!this.isHistoricalExecuteCount) return this.container
                return this.historyRecordState === 'ready' ? (this.historyPluginSnapshot?.container || {}) : {}
            },

            displayElement () {
                if (!this.isHistoricalExecuteCount) {
                    const status = resolveRecordElementStatus(this.currentElement.status, this.execDetail.status)
                    return status === this.currentElement.status ? this.currentElement : { ...this.currentElement, status }
                }
                return this.historyRecordState === 'ready' ? (this.historyPluginSnapshot?.element || {}) : {}
            },

            displayExecDetail () {
                if (!this.isHistoricalExecuteCount) return this.execDetail
                return this.historyRecordState === 'ready' ? (this.historyExecDetail || {}) : {}
            },

            displayConclusionAvailable () {
                return !this.isHistoricalExecuteCount || this.historyRecordState === 'ready'
            },

            showConclusion () {
                if (this.isHistoricalExecuteCount) return this.historyRecordState === 'ready' && this.canShowConclusion
                return this.conclusionVisible && this.canShowConclusion
            },

            showStandaloneExecuteCount () {
                return shouldShowStandaloneExecuteCount({
                    tab: this.currentTab,
                    showConclusion: this.showConclusion,
                    latestCount: this.currentElement.executeCount
                })
            },

            showHistoryRecordState () {
                return this.currentTab === 'log' && this.isHistoricalExecuteCount && this.historyRecordState !== 'ready'
            },

            showHeaderTabs () {
                return true
            },
            positionCode () {
                return positionCode(this.editingElementPos)
            },
            canShowProgressTab () {
                if (this.isHistoricalExecuteCount) return false
                return shouldShowProgressTab({
                    data: this.progressDetailData,
                    percent: this.progressPercent,
                    subtasks: this.progressSubtasks,
                    error: this.progressError
                })
            },

            durationText () {
                if (isTaskElapsedStatus(this.displayElement.status)) {
                    const start = resolveElapsedTimestamp(this.displayElement.startEpoch)
                        ?? resolveElapsedTimestamp(this.displayElement.startTime)
                    const elapsed = Number.isFinite(start) ? Math.max(0, this.elapsedNow - start) : 0
                    return elapsed >= 1000 ? convertMStoString(elapsed) : ''
                }
                const totalCost = this.displayElement.timeCost?.totalCost
                return Number.isFinite(totalCost) && totalCost >= 1000 ? convertMStoString(totalCost) : ''
            },
            progressContextKey () {
                return `${this.currentElement.id}|${this.execDetail.id}|${this.activeExecuteCount}`
            },
            logContext () {
                return `${this.currentElement.id}-${this.activeExecuteCount}`
            },
            logConclusionModel () {
                const currentElement = this.displayElement
                const container = this.displayContainer
                const displayDetail = this.displayExecDetail
                const status = currentElement.status
                const jobStatus = container.status
                const jobTimeoutCode = currentElement.errorCode
                const cancelConclusion = resolveCancelConclusion(displayDetail.buildEndInfo, {
                    taskId: currentElement.id,
                    containerId: container.containerId || container.id,
                    status: status || ''
                })
                const jobTimeoutEvidence = isJobTimeoutTerminatedStep(status, jobTimeoutCode, displayDetail.errorInfoList, { id: currentElement.id, containerId: container.id })
                    || isJobExecTimeoutCancel(displayDetail.buildEndInfo) && Boolean(cancelConclusion)
                const systemCanceledEvidence = isSystemAgentTermination(status, currentElement.errorType, currentElement.errorCode, displayDetail.errorInfoList, { id: currentElement.id, containerId: container.id })
                const cancelDisplay = jobTimeoutEvidence ? null : cancelConclusion
                    ? resolveCancelDisplay(cancelConclusion, displayDetail.buildEndInfo, (key, values) => this.$t(key, values))
                    : null
                const effectiveStatus = jobTimeoutEvidence ? 'EXEC_TIMEOUT' : status
                const nodeError = this.currentNodeError
                const element = {
                    ...(jobTimeoutEvidence ? { ...currentElement, status: effectiveStatus } : currentElement),
                    errorCode: nodeError?.errorCode || currentElement.errorCode,
                    errorMsg: nodeError?.errorMsg || currentElement.errorMsg
                }
                const gated = this.actionsEnabled && !this.$route.query?.archiveFlag && this.conclusionVisible && !this.isHistoricalExecuteCount
                const model = buildConclusion(element, container, {
                    projectId: this.$route.params.projectId,
                    now: this.elapsedNow,
                    cancelUserId: displayDetail.cancelUserId || '',
                    effectiveStatus,
                    jobStatus: jobTimeoutEvidence ? 'EXEC_TIMEOUT' : jobStatus,
                    stepStatus: currentElement.status,
                    element: currentElement,
                    container,
                    hideNode: ['FAILED', 'EXEC_TIMEOUT', 'HEARTBEAT_TIMEOUT'].includes(String(container.startVMStatus || '').toUpperCase()),
                    canRetry: !this.isHistoricalExecuteCount && gated && !!currentElement.canRetry,
                    canSkip: !this.isHistoricalExecuteCount && gated && !!currentElement.canSkip,
                    continueConfig: !this.isHistoricalExecuteCount && gated && status === 'PAUSE',
                    review: !this.isHistoricalExecuteCount && gated && status === 'REVIEWING',
                    locateLog: shouldShowLocateLog(effectiveStatus) && this.shouldMountLog,
                    conditionLink: !this.isHistoricalExecuteCount && hasCustomRunCondition(currentElement)
                })
                const conditionSkipped = !this.isHistoricalExecuteCount && isConditionSkipped(currentElement)
                const fallbackCancelKey = cancelConclusion ? '' : resolveFallbackCancelLabelKey({ status: effectiveStatus, cancelUserId: model.cancelUserId, buildEndInfo: displayDetail.buildEndInfo })
                const key = `details.statusMap.${effectiveStatus}`
                const translated = this.$t(key)
                return {
                    ...model,
                    tone: cancelDisplay ? 'canceled' : model.tone,
                    message: systemCanceledEvidence
                        ? this.$t('logPanel.status.systemCanceledDetail')
                        : cancelDisplay
                        ? cancelDisplay.message
                        : effectiveStatus === 'EXEC_TIMEOUT'
                        ? this.$t(jobTimeoutEvidence ? 'logPanel.status.jobTimeout' : 'logPanel.status.stepTimeout', jobTimeoutEvidence ? [container.name, model.timeoutThreshold] : [model.timeoutThreshold])
                        : model.message,
                    label: cancelDisplay
                        ? cancelDisplay.label
                        : fallbackCancelKey
                        ? this.$t(`logPanel.status.${fallbackCancelKey}`)
                        : effectiveStatus === 'UNEXEC'
                        ? this.$t('logPanel.status.notExecutedLabel')
                        : systemCanceledEvidence
                        ? this.$t('logPanel.status.systemCanceled')
                        : translated === key ? effectiveStatus : translated,
                    parentBuildLabel: cancelConclusion?.type === 'parent' ? 'logPanel.status.parentBuild' : '',
                    parentBuildUrl: cancelDisplay?.parentBuildUrl || cancelConclusion?.parentBuildUrl || '',
                    cancelUserId: cancelDisplay?.cancelUserId || resolveCancelUserId(cancelConclusion, model.cancelUserId),
                    conditionSkipSummary: conditionSkipped
                        ? this.conditionSkipReason || this.$t('logPanel.status.conditionSkipped')
                        : '',
                    statusMessageKey: effectiveStatus === 'UNEXEC' || conditionSkipped ? '' : model.statusMessageKey,
                }
            },
            paramsModel () {
                return buildParamsModel(this.paramsElement)
            },
            paramsSections () {
                return this.paramsModel
            },
            paramsNotExecuted () {
                return !!this.paramsModel.notExecuted
            },
            paramsElement () {
                return resolveParamsElementSource({
                    execDetail: this.execDetail,
                    paramsExecDetail: this.paramsExecDetail,
                    position: this.editingElementPos,
                    selectedExecuteCount: this.selectedExecuteCount,
                    latestExecuteCount: this.currentElement.executeCount
                })
            },
            paramsContextKey () {
                return `${this.currentElement.id}|${this.activeExecuteCount}`
            },
            conclusionVisible () {
                return this.selectedExecuteCount === null || Number(this.selectedExecuteCount) === Number(this.currentElement.executeCount)
            },
            activeExecuteCount () {
                return this.selectedExecuteCount ?? this.currentElement.executeCount
            },
            canRenderLog () {
                if (this.isHistoricalExecuteCount) return true
                return shouldLoadStepLog({ status: this.currentElement.status })
            },
            shouldMountLog () {
                return shouldMountLogPanel({
                    isHistoricalExecuteCount: this.isHistoricalExecuteCount,
                    historyRecordState: this.historyRecordState,
                    status: this.isHistoricalExecuteCount ? this.displayElement.status : this.currentElement.status
                })
            },
            canShowConclusion () {
                if (this.isHistoricalExecuteCount) {
                    return this.historyRecordState === 'ready'
                        && (shouldLoadStepLog({ status: this.displayElement.status }) || this.displayElement.status === 'UNEXEC')
                }
                return this.canRenderLog || this.currentElement.status === 'UNEXEC' || this.hasBuildCancelConclusion
            },
            hasBuildCancelConclusion () {
                const container = this.displayContainer
                return Boolean(resolveCancelConclusion(this.displayExecDetail.buildEndInfo, {
                    taskId: this.displayElement.id,
                    containerId: container.containerId || container.id,
                    status: this.displayElement.status || ''
                }))
            },
            stepEmptyLineText () {
                return this.currentElement.status === 'PAUSE' ? this.$t('logPanel.stepNotStarted') : ''
            },
            displayProgressPercent () {
                if (this.isHistoricalExecuteCount) return null
                return resolveDisplayProgressPercent(this.currentElement.status, this.progressPercent)
            },
            displayProgressSubtasks () {
                if (this.isHistoricalExecuteCount) return []
                return this.progressSubtasks.length ? this.progressSubtasks : this.progressSubtasksSnapshot
            },
            canExpandProgressSubtasks () {
                if (this.isHistoricalExecuteCount) return false
                return this.displayProgressSubtasks.length > 0
            },
            progressPanelData () {
                if (this.isHistoricalExecuteCount) return null
                return this.progressDetailData
            },
            componentList () {
                return {
                    progress: {
                        component: ProgressDetailPanel,
                        bindData: {
                            buildId: this.execDetail.id,
                            taskId: this.currentElement.id,
                            executeCount: this.currentElement.executeCount,
                            taskStatus: this.currentElement.status,
                            showEmpty: true,
                            showHeader: true,
                            headerMeta: this.progressHeaderMeta,
                            detailData: this.progressPanelData,
                            controlled: true,
                            externalLoading: this.progressLoading,
                            externalError: this.progressError
                        }
                    },
                    artifactory: {
                        component: Artifactory,
                        bindData: {
                            taskId: this.currentElement.id
                        }
                    },
                    report: {
                        component: Report,
                        bindData: {
                            taskId: this.currentElement.id
                        }
                    },
                    setting: {
                        component: AtomContent,
                        bindData: {
                            elementIndex: this.editingElementPos.elementIndex,
                            containerIndex: this.editingElementPos.containerIndex,
                            containerGroupIndex: this.editingElementPos.containerGroupIndex,
                            stageIndex: this.editingElementPos.stageIndex,
                            stages: this.stages,
                            editable: false,
                            isInstanceTemplate: false
                        }
                    }
                }
            },

            currentNodeError () {
                const element = this.displayElement
                const container = this.displayContainer
                const errorInfoList = this.displayExecDetail.errorInfoList
                const status = String(element.status || '').toUpperCase()
                const timeoutTerminated = isJobTimeoutTerminatedStep(status, element.errorCode, errorInfoList, { id: element.id, containerId: container.id })
                const canDisplayConclusion = this.isHistoricalExecuteCount ? this.displayConclusionAvailable : this.conclusionVisible
                if (!canDisplayConclusion || !['FAILED', 'HEARTBEAT_TIMEOUT', 'EXEC_TIMEOUT'].includes(status) && !timeoutTerminated) return null
                return resolveNodeLogError(
                    errorInfoList,
                    { id: element.id, containerId: container.id },
                    element
                )
            },
            activeErorr () {
                return this.currentNodeError
            },
            locateTarget () {
                const conclusion = this.logConclusionModel
                return {
                    context: this.logContext,
                    messages: [
                        [conclusion.errorCode, resolveConclusionDisplayMessage(conclusion, key => this.$t(key))].filter(Boolean).join('\n'),
                        this.activeErorr?.errorMsg || ''
                    ]
                }
            },
            progressHeaderMeta () {
                const buildNum = this.execDetail.buildNum ? `#${this.execDetail.buildNum}` : ''
                const stageName = this.$t('logPanel.stage', [this.editingElementPos.stageIndex + 1])
                return [buildNum, stageName].filter(Boolean).join(' - ')
            },
            defaultTab () {
                return 'log'
            },
            sortedTabList () {
                const mapping = {
                    PROGRESS: 'progress',
                    LOG: 'log',
                    ARTIFACT: 'artifactory',
                    CONFIG: 'setting'
                }

                const orderedTabs = [
                    this.tabList.find(tab => tab.name === 'progress'),
                    ...this.properties.map(prop => {
                        const tabName = mapping[prop]
                        return this.tabList.find(tab => tab.name === tabName)
                    })
                ].filter(Boolean)

                const reportTab = this.tabList.find(tab => tab.name === 'report')
                if (reportTab) {
                    orderedTabs.push(reportTab)
                }
                return orderedTabs.filter(Boolean)
            },
            visibleTabList () {
                return this.sortedTabList.filter(tab => tab.show && (tab.name !== 'progress' || this.canShowProgressTab))
            },
            visibleTabKey () {
                return this.visibleTabList.map(tab => tab.name).join('|')
            }
        },

        watch: {
            'currentElement.status' (status) {
                this.startElapsedTimer()
                this.stopProgressPoll()
                this.progressRequestId += 1
                if (!shouldFetchTaskProgress(status)) {
                    this.resetProgressState()
                    this.progressExpanded = false
                    return
                }
                this.fetchProgress()
            },
            activeExecuteCount () {
                this.conditionSkipReason = ''
                this.invalidateParamsRecord()
                if (this.currentTab === 'setting' && this.settingView === 'params') this.fetchParamsRecord()
                this.stopProgressPoll()
                this.resetProgressState({ preserveSnapshot: false })
                if (!shouldFetchTaskProgress(this.currentElement.status)) {
                    return
                }
                this.fetchProgress()
            },
            currentTab (tab) {
                if (tab === 'setting' && this.settingView === 'params') {
                    this.fetchParamsRecord()
                } else {
                    this.invalidateParamsRecord()
                }
            },
            settingView (view) {
                if (this.currentTab === 'setting' && view === 'params') this.fetchParamsRecord()
                else this.invalidateParamsRecord()
            },
            visibleTabKey: {
                immediate: true,
                handler () {
                    this.ensureCurrentTab()
                }
            },
            'currentElement.id': function () {
                this.stopElapsedTimer()
                this.stopProgressPoll()
                this.progressRequestId += 1
                this.invalidateParamsRecord()
                this.resetProgressState({ preserveSnapshot: false })
                this.logIssuesAvailable = false
                this.conditionSkipReason = ''
                this.logSummaryAvailable = false
                this.realLogControls = { levels: ['INFO', 'WARN', 'ERROR'], levelAll: false, search: '', current: 0, total: 0, showTime: false, wrap: true }
                this.selectedExecuteCount = null
                this.settingView = 'config'
                this.tabList = [
                    { name: 'progress', show: true },
                    { name: 'log', show: true },
                    { name: 'artifactory', show: false, completeLoading: false },
                    { name: 'setting', show: true },
                    { name: 'report', show: false, completeLoading: false }
                ]
                this.currentTab = 'log'
                this.startElapsedTimer()
                this.fetchProgress()
            },
            historyContextKey () {
                this.invalidateHistoricalDetail()
                if (this.isHistoricalExecuteCount) this.fetchHistoricalDetail()
            }
        },

        mounted () {
            this.startElapsedTimer()
            this.fetchProgress()
        },

        beforeDestroy () {
            this.invalidateHistoricalDetail()
            this.stopElapsedTimer()
            this.stopProgressPoll()
            this.progressRequestId += 1
            this.invalidateParamsRecord()
        },

        methods: {
            ...mapActions('atom', ['requestPipelineExecDetail']),
            invalidateHistoricalDetail () {
                this.historyRequestId += 1
                this.historyExecDetail = null
                this.historyPluginSnapshot = null
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
                this.historyPluginSnapshot = null
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
                        ? resolveHistoricalPluginSnapshot(this.execDetail, detail, this.editingElementPos, context.executeCount)
                        : null
                    if (!snapshot) {
                        this.historyRecordState = 'unavailable'
                        return
                    }
                    this.historyExecDetail = detail
                    this.historyPluginSnapshot = snapshot
                    this.historyRecordState = 'ready'
                } catch (_) {
                    if (this.isHistoricalDetailRequestCurrent(requestId, contextKey)) this.historyRecordState = 'unavailable'
                }
            },
            focusExecuteCountAfterKeyboardSelection (contextKey) {
                const currentContextKey = `${this.currentElement.id}|${this.execDetail.id}`
                if (contextKey !== currentContextKey || this.currentTab !== 'log') return
                this.$nextTick(() => {
                    if (contextKey !== `${this.currentElement.id}|${this.execDetail.id}` || this.currentTab !== 'log') return
                    this.$refs.conclusionExecuteCountSelect?.focusTrigger?.()
                })
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
            stopProgressPoll () {
                if (this.progressTimer) clearTimeout(this.progressTimer)
                this.progressTimer = null
            },
            resetProgressState ({ preserveSnapshot = true } = {}) {
                if (preserveSnapshot && this.progressSubtasks.length) {
                    this.progressSubtasksSnapshot = [...this.progressSubtasks]
                }
                this.progressPercent = null
                this.progressSubtasks = []
                this.progressDetailData = null
                this.progressLoading = false
                this.progressError = false
                if (!preserveSnapshot) {
                    this.progressExpanded = false
                    this.progressSubtasksSnapshot = []
                    this.progressExpandInitKey = ''
                }
            },
            toggleProgressExpanded () {
                this.progressExpanded = !this.progressExpanded
            },
            scheduleProgressPoll (requestId, contextKey) {
                this.stopProgressPoll()
                this.progressTimer = setTimeout(() => {
                    this.fetchProgress({ isPoll: true, requestId, contextKey })
                }, 3000)
            },
            isProgressContextCurrent (requestId, contextKey) {
                return shouldApplyProgressResponse({
                    requestId,
                    currentRequestId: this.progressRequestId,
                    contextKey,
                    currentContextKey: this.progressContextKey
                })
            },
            async fetchProgress ({ isPoll = false, requestId, contextKey } = {}) {
                if (!isPoll) {
                    this.stopProgressPoll()
                    requestId = ++this.progressRequestId
                    contextKey = this.progressContextKey
                }
                const currentRequestId = requestId
                const currentContextKey = contextKey
                if (!this.currentElement.id || !shouldFetchTaskProgress(this.currentElement.status)) {
                    this.resetProgressState()
                    return
                }
                if (!isPoll) {
                    this.progressLoading = true
                    this.progressError = false
                }
                try {
                    const { projectId, pipelineId } = this.$route.params
                    const query = new URLSearchParams({
                        buildId: this.execDetail.id,
                        taskId: this.currentElement.id
                    })
                    if (this.activeExecuteCount) query.append('executeCount', this.activeExecuteCount)
                    const { data } = await this.$ajax.get(
                        `${PROCESS_API_URL_PREFIX}/user/builds/${projectId}/${pipelineId}/getTaskProgressDetail?${query}`
                    )
                    if (!this.isProgressContextCurrent(currentRequestId, currentContextKey)) return
                    this.progressError = false
                    this.progressDetailData = data || null
                    this.progressPercent = parseTaskProgressPercent(data)
                    this.progressSubtasks = parseTaskProgressSubtasks(data, { now: Date.now() })
                    if (this.progressSubtasks.length) {
                        this.progressSubtasksSnapshot = [...this.progressSubtasks]
                        if (this.progressExpandInitKey !== this.progressContextKey) {
                            this.progressExpandInitKey = this.progressContextKey
                            this.progressExpanded = true
                        }
                    }
                } catch (_) {
                    if (!this.isProgressContextCurrent(currentRequestId, currentContextKey)) return
                    this.progressDetailData = null
                    this.progressPercent = null
                    this.progressSubtasks = []
                    this.progressError = true
                } finally {
                    if (this.isProgressContextCurrent(currentRequestId, currentContextKey)) this.progressLoading = false
                    if (shouldScheduleProgressPoll(
                        this.currentElement.status,
                        this.isProgressContextCurrent(currentRequestId, currentContextKey)
                    )) {
                        this.scheduleProgressPoll(currentRequestId, currentContextKey)
                    }
                }
            },
            handleIssuesChange (hasIssues) {
                this.logIssuesAvailable = hasIssues
            },
            handleConditionSkipReason (reason) {
                if (reason) this.conditionSkipReason = reason
            },
            handleLocateAvailability (available) {
                this.logSummaryAvailable = Boolean(available)
            },
            handleLocateLog () {
                this.locateRequest += 1
                this.locateRequestContext = this.logContext
            },
            handleLocateMiss () {
                if (this.$bkMessage) this.$bkMessage({ theme: 'warning', message: this.$t('logPanel.locateSummaryMiss'), limit: 1 })
            },
            handleStepAction (skip) {
                if (this.pendingAction) return
                const action = skip ? 'skip' : 'retry'
                this.pendingAction = action
                this.$emit('continue', {
                    taskId: this.currentElement.id,
                    skip
                }, () => {
                    this.pendingAction = ''
                })
            },
            handleMockablePanelAction (action) {
                if (action === 'review') this.$emit('review', this.currentElement)
                else this.selectTab('setting')
            },

            handleExecuteChange (executeCount) {
                const count = Number(executeCount)
                this.realLogControls.search = ''
                this.realLogControls.current = 0
                this.realLogControls.total = 0
                this.selectedExecuteCount = count === Number(this.currentElement.executeCount) ? null : count
            },
            invalidateParamsRecord () {
                this.paramsRequestId += 1
                this.paramsExecDetail = null
                this.paramsLoading = false
                this.paramsLoadError = ''
            },
            isParamsContextCurrent (requestId, contextKey) {
                return requestId === this.paramsRequestId && contextKey === this.paramsContextKey
            },
            async fetchParamsRecord () {
                this.paramsExecDetail = null
                this.paramsLoadError = ''
                if (
                    this.currentTab !== 'setting'
                    || this.settingView !== 'params'
                    || !this.currentElement.id
                    || !shouldFetchParamsRecord(this.selectedExecuteCount, this.currentElement.executeCount)
                ) {
                    this.paramsLoading = false
                    return
                }

                const requestId = ++this.paramsRequestId
                const contextKey = this.paramsContextKey
                this.paramsLoading = true
                try {
                    const detail = await this.requestPipelineExecDetail(buildParamsRecordRequest({
                        projectId: this.$route.params.projectId,
                        pipelineId: this.$route.params.pipelineId,
                        buildNo: this.execDetail.id,
                        executeCount: this.activeExecuteCount,
                        archiveFlag: this.$route.query.archiveFlag
                    }))
                    if (this.isParamsContextCurrent(requestId, contextKey)) {
                        this.paramsExecDetail = detail
                        this.paramsLoadError = ''
                    }
                } catch (_) {
                    if (this.isParamsContextCurrent(requestId, contextKey)) {
                        this.paramsExecDetail = null
                        this.paramsLoadError = this.$t('logPanel.params.loadFailed')
                    }
                } finally {
                    if (this.isParamsContextCurrent(requestId, contextKey)) {
                        this.paramsLoading = false
                    }
                }
            },
            startElapsedTimer () {
                this.stopElapsedTimer()
                const start = resolveElapsedTimestamp(this.currentElement.startEpoch)
                    ?? resolveElapsedTimestamp(this.currentElement.startTime)
                const hasStart = start !== null
                if (isTaskElapsedStatus(this.currentElement.status) && hasStart) {
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

            selectTab (name) {
                if (name === 'setting') this.settingView = 'config'
                this.currentTab = name
            },
            tabLabel (name) {
                if (name === 'setting') return this.$t('execDetail.setting')
                return this.$t(`execDetail.${name}`)
            },
            showParamsView () {
                if (!this.paramsViewEnabled) return
                this.currentTab = 'setting'
                this.settingView = 'params'
            },
            showConfigView () {
                this.settingView = 'config'
            },

            ensureCurrentTab () {
                if (this.currentTab === 'setting') return
                if (!this.currentTab || !this.visibleTabList.some(tab => tab.name === this.currentTab)) {
                    this.currentTab = this.visibleTabList.find(tab => tab.name === 'log')?.name
                        ?? this.visibleTabList.find(tab => tab.name === this.defaultTab)?.name
                        ?? this.visibleTabList[0]?.name
                        ?? 'log'
                }
            },

            toggleTab (key, show = false) {
                if (key === 'progress') return
                const tab = this.tabList.find(tab => tab.name === key)
                if (!tab) return
                tab.show = show
            },

            completeLoading (key) {
                const tab = this.sortedTabList.find(tab => tab.name === key)
                tab.completeLoading = true
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

    .plugin-setting-tools {
        display: flex;
        align-items: center;
        flex: 0 0 auto;
        width: max-content;
        min-width: max-content;
        margin-left: auto;
        white-space: nowrap;
    }
    ::v-deep .plugin-setting-tools > .head-tool:last-child {
        margin-right: 0;
    }
    ::v-deep .plugin-setting-tools .reference-var {
        padding: 0 0 21px;
        margin-bottom: -21px;
    }
    ::v-deep .plugin-setting-tools .env-layout {
        top: 60px;
    }
    ::v-deep .plugin-setting-tools .env-layout-container::before {
        content: none;
    }
    ::v-deep .plugin-setting-tools .reference-var > span {
        position: relative;
    }
    ::v-deep .plugin-setting-tools .reference-var > span::after {
        content: '';
        position: absolute;
        top: calc(100% + 17px);
        left: 50%;
        width: 12px;
        height: 7px;
        background: #fff;
        clip-path: polygon(50% 0, 100% 100%, 0 100%);
        transform: translateX(-50%);
        visibility: hidden;
        pointer-events: none;
        z-index: 1113;
    }
    ::v-deep .plugin-setting-tools .reference-var:hover > span::after {
        visibility: visible;
    }
    ::v-deep .plugin-setting-tools .env-layout .bk-table {
        border-top: 0;
    }
    ::v-deep .atom-property-panel {
        padding: 10px 50px;
        .bk-form-item.is-required .bk-label, .bk-form-inline-item.is-required .bk-label {
            margin-right: 10px;
        }
    }
</style>
