<template>
    <section
        class="pipeline-detail-wrapper"
        @scroll="handlerScroll"
        v-bkloading="{ isLoading: isLoading }"
    >
        <empty-tips
            v-if="hasNoPermission"
            :show-lock="true"
            :title="noPermissionTipsConfig.title"
            :desc="noPermissionTipsConfig.desc"
            :btns="noPermissionTipsConfig.btns"
        >
        </empty-tips>

        <empty-tips
            v-else-if="detailLoadFailed"
            :title="$t('details.loadDetailFailed')"
            :btns="[{ theme: 'primary', text: $t('retry'), handler: loadPipelineExecDetail }]"
        />

        <template v-else-if="execDetail">
            <div class="exec-detail-summary-header">
                <span
                    :class="{
                        'exec-detail-build-summary-anchor': true,
                        [execDetail.status]: execDetail.status
                    }"
                ></span>
                <aside class="exec-detail-summary-header-title">
                    <build-end-info-popover
                        v-if="showBuildEndInfoPopover"
                        :build-end-info="execDetail.buildEndInfo"
                        :start-time="buildEndStartTime"
                        :trigger-user="startUser"
                        :trigger-type-desc="triggerType"
                        @highlight="handleBuildEndPositionHighlight"
                        @locateLog="handleBuildEndPositionLocate"
                    >
                        <bk-tag
                            class="exec-status-tag"
                            type="stroke"
                            :theme="statusTagTheme"
                        >
                            <span class="exec-status-label">
                                <logo
                                    v-if="execDetail.status === 'STAGE_SUCCESS'"
                                    name="flag"
                                    size="12"
                                    fill="#34d97b"
                                />
                                {{ statusLabel }}
                                <span
                                    v-if="!execDetail.buildEndInfo"
                                    v-bk-tooltips="`${$t('details.canceller')}：${execDetail.cancelUserId || '--'}`"
                                    class="devops-icon icon-info-circle"
                                >
                                </span>
                                <span
                                    v-else
                                    class="devops-icon icon-info-circle"
                                >
                                </span>
                            </span>
                        </bk-tag>
                    </build-end-info-popover>
                    <build-running-info-popover
                        v-else-if="showBuildRunningInfoPopover"
                        :build-running-info="execDetail.buildRunningInfo"
                        :current-timestamp="execDetail.currentTimestamp"
                        @highlight="handleBuildEndPositionHighlight"
                    >
                        <bk-tag
                            class="exec-status-tag"
                            type="stroke"
                            :theme="statusTagTheme"
                        >
                            <span class="exec-status-label">
                                <i
                                    :class="['devops-icon', {
                                        'icon-hourglass hourglass-queue': execDetail.status === 'QUEUE',
                                        'icon-circle-2-1 spin-icon': execDetail.status === 'RUNNING'
                                    }]"
                                />
                                {{ statusLabel }}
                                <span class="devops-icon icon-info-circle"></span>
                            </span>
                        </bk-tag>
                    </build-running-info-popover>
                    <bk-tag
                        v-else
                        class="exec-status-tag"
                        type="stroke"
                        :theme="statusTagTheme"
                    >
                        <span class="exec-status-label">
                            <i
                                v-if="isRunning"
                                :class="['devops-icon', {
                                    'icon-hourglass hourglass-queue': execDetail.status === 'QUEUE',
                                    'icon-circle-2-1 spin-icon': execDetail.status === 'RUNNING'
                                }]"
                            />
                            {{ statusLabel }}
                        </span>
                    </bk-tag>
                    <span
                        v-bk-overflow-tips
                        class="exec-detail-summary-header-build-msg"
                    >
                        {{ execDetail.buildMsg }}
                    </span>
                </aside>
                <aside class="exec-detail-summary-header-trigger">
                    <img
                        v-if="execDetail.triggerUserProfile"
                        class="exec-trigger-profile"
                    />
                    <logo
                        class="exec-trigger-profile"
                        name="default-user"
                        size="24"
                    />
                    <span v-if="startUser">
                        {{
                            $t("details.executorInfo", [
                                startUser,
                                execDetail.trigger,
                                execFormatStartTime
                            ])
                        }}
                    </span>
                </aside>
            </div>
            <pending-manual-items-alert
                v-if="showPendingManualAlert"
                :pending-items="execDetail.buildRunningInfo.pendingItems"
                :pending-item-count="execDetail.buildRunningInfo.pendingItemCount"
                @highlight="handleBuildEndPositionHighlight"
                @process="handlePendingManualProcess"
            />
            <p
                class="summary-header-shadow"
                v-show="show"
            ></p>
            <Summary
                ref="detailSummary"
                :visible="summaryVisible"
                :exec-detail="execDetail"
                :version-change="pipelineInfo?.versionChange"
            ></Summary>

            <p class="pipeline-exec-gap">
                <span
                    @click="collapseSummary"
                    :class="['summary-collapsed-handler', {
                        'is-collapsed': !summaryVisible
                    }]"
                >
                    <i class="devops-icon icon-angle-double-up"></i>
                </span>
            </p>
            <header class="exec-detail-switcher">
                <span
                    v-for="panel in panels"
                    :key="panel.name"
                    :class="['exec-detail-switcher-tab', {
                        active: curItemTab === panel.name
                    }]"
                    @click="switchTab(panel)"
                >
                    {{ panel.label }}
                </span>
            </header>
            <div
                :class="['exec-detail-main', {
                    'is-outputs-panel': curItemTab === 'outputs'
                }]"
            >
                <component
                    ref="execDetailPanel"
                    :is="curPanel.component"
                    v-bind="curPanel.bindData"
                    v-on="curPanel.listeners"
                ></component>
            </div>
        </template>
        <template v-if="editingElementPos && execDetail">
            <template v-if="showPanelType === 'PAUSE'">
                <atom-property-panel
                    :element-index="editingElementPos.elementIndex"
                    :container-group-index="editingElementPos.containerGroupIndex"
                    :container-index="editingElementPos.containerIndex"
                    :stage-index="editingElementPos.stageIndex"
                    :stages="execDetail.model.stages"
                    :editable="true"
                    :is-instance-template="false"
                />
            </template>
            <template v-else-if="showLog">
                <plugin
                    :exec-detail="execDetail"
                    :editing-element-pos="editingElementPos"
                    :properties="curProject.properties?.pluginDetailsDisplayOrder"
                    @close="hideSidePanel"
                    @continue="handlePluginContinue"
                    @review="handlePluginReview"
                    :actions-enabled="isExecuteDetailPanel"
                    :is-latest-build="effectiveIsLatestBuild"
                />
            </template>
            <template v-else-if="showContainerPanel">
                <job
                    :exec-detail="execDetail"
                    :pipeline="pipelineModel"
                    :editing-element-pos="editingElementPos"
                    @close="hideSidePanel"
                    @continue="handlePluginContinue"
                    :actions-enabled="isExecuteDetailPanel"
                    :is-latest-build="effectiveIsLatestBuild"
                />
            </template>
            <template v-else-if="showStagePanel">
                <stage
                    :exec-detail="execDetail"
                    :editing-element-pos="editingElementPos"
                    @close="hideSidePanel"
                />
            </template>
            <template v-else-if="showStageReviewPanel.isShow">
                <stage-review-panel
                    :stage="stage"
                    @approve="loadPipelineExecDetail"
                />
            </template>
        </template>
    </section>
</template>

<script>
    import AtomPropertyPanel from '@/components/AtomPropertyPanel'
    import codeRecord from '@/components/codeRecord'
    import emptyTips from '@/components/devops/emptyTips'
    import BuildEndInfoPopover from '@/components/ExecDetail/BuildEndInfoPopover'
    import BuildRunningInfoPopover from '@/components/ExecDetail/BuildRunningInfoPopover'
    import PendingManualItemsAlert from '@/components/ExecDetail/PendingManualItemsAlert'
    import { getBuildEndInfoConfig } from '@/components/ExecDetail/buildEndInfoConfig'
    import { getBuildRunningInfoConfig, PENDING_ITEM_TYPE } from '@/components/ExecDetail/buildRunningInfoConfig'
    import job from '@/components/ExecDetail/job'
    import plugin from '@/components/ExecDetail/plugin'
    import stage from '@/components/ExecDetail/stage'
    import Summary from '@/components/ExecDetail/Summary'
    import ExecPipeline from '@/components/ExecPipeline'
    import Logo from '@/components/Logo'
    import Outputs from '@/components/Outputs'
    import StagePropertyPanel from '@/components/StagePropertyPanel'
    import stageReviewPanel from '@/components/StageReviewPanel'
    import StartParams from '@/components/StartParams'
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'
    import {
        handlePipelineNoPermission,
        RESOURCE_ACTION
    } from '@/utils/permission'
    import { mapThemeOfStatus } from '@/utils/pipelineStatus'
    import { FETCH_ERROR } from '@/store/constants'
    import { convertTime } from '@/utils/util'
    import webSocketMessage from '@/utils/webSocketMessage'
    import { mapActions, mapGetters, mapState } from 'vuex'

    const PANELS = {
        executeDetail: 'executeDetail',
        outputs: 'outputs',
        reports: 'reports',
        codeRecords: 'codeRecords',
        startupParams: 'startupParams'
    }

    export default {
        components: {
            StagePropertyPanel,
            Outputs,
            codeRecord,
            StartParams,
            ExecPipeline,
            emptyTips,
            plugin,
            job,
            stage,
            stageReviewPanel,
            Logo,
            AtomPropertyPanel,
            Summary,
            BuildEndInfoPopover,
            BuildRunningInfoPopover,
            PendingManualItemsAlert
        },
        mixins: [pipelineOperateMixin],
        provide () {
            return { captureExecutionDetailContext: this.captureExecutionDetailContext }
        },

        data () {
            return {
                isLoading: true,
                hasNoPermission: false,
                detailLoadFailed: false,
                detailRequestId: 0,
                pendingDetailKey: '',
                pendingRealtimeDetail: null,
                loadedDetailKey: '',
                linkUrl: WEB_URL_PREFIX + location.pathname,
                show: false,
                summaryVisible: true,
                noPermissionTipsConfig: {
                    title: this.$t('noPermission'),
                    desc: this.$t('history.noPermissionTips'),
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: this.changeProject,
                            text: this.$t('changeProject')
                        },
                        {
                            theme: 'success',
                            size: 'normal',
                            handler: () => {
                                handlePipelineNoPermission({
                                    projectId: this.routerParams.projectId,
                                    resourceCode: this.routerParams.pipelineId,
                                    action: RESOURCE_ACTION.EXECUTE
                                })
                            },
                            text: this.$t('applyPermission')
                        }
                    ]
                }
            }
        },

        computed: {
            ...mapState('atom', [
                'pipelineInfo',
                'editingElementPos',
                'isPropertyPanelVisible',
                'isShowCompleteLog',
                'showPanelType',
                'fetchingAtomList',
                'showStageReviewPanel'
            ]),
            ...mapGetters('atom', {
                execDetail: 'getExecDetail'
            }),
            execFormatStartTime () {
                return convertTime(this.execDetail?.queueTime)
            },
            isRunning () {
                return ['RUNNING', 'QUEUE'].includes(this.execDetail?.status)
            },
            archiveFlag () {
                return this.$route.query.archiveFlag
            },
            panels () {
                return [
                    {
                        name: PANELS.executeDetail,
                        label: this.$t('details.executeDetail'),
                        component: 'exec-pipeline',
                        className: 'exec-pipeline',
                        bindData: {
                            execDetail: this.execDetail,
                            isLatestBuild: this.effectiveIsLatestBuild,
                            matchRules: this.curMatchRules,
                            isRunning: this.isRunning
                        },
                        listeners: {
                            'refresh-detail': this.loadPipelineExecDetail
                        }
                    },
                    {
                        name: PANELS.outputs,
                        label: this.$t('details.artifact'),
                        className: '',
                        component: 'outputs',
                        bindData: {
                            currentTab: 'artifacts'
                        }
                    },
                    {
                        name: PANELS.reports,
                        label: this.$t('details.report'),
                        className: '',
                        component: 'outputs',
                        bindData: {
                            currentTab: 'reports'
                        }
                    },
                    {
                        name: PANELS.codeRecords,
                        label: this.$t('details.codeRecords'),
                        className: '',
                        component: 'code-record',
                        bindData: {}
                    },
                    {
                        name: PANELS.startupParams,
                        label: this.$t('details.startupParams'),
                        className: '',
                        component: 'start-params',
                        bindData: {}
                    }
                ]
            },
            showLog: {
                get () {
                    const {
                        editingElementPos,
                        isPropertyPanelVisible,
                        $route: { params }
                    } = this
                    return (
                        typeof editingElementPos.elementIndex !== 'undefined'
                        && params.buildNo
                        && isPropertyPanelVisible
                    )
                },
                set (value) {
                    this.togglePropertyPanel({
                        isShow: value
                    })
                }
            },
            showStagePanel () {
                return (
                    typeof this.editingElementPos.stageIndex !== 'undefined'
                    && this.isPropertyPanelVisible
                )
            },
            showContainerPanel () {
                const { editingElementPos, isPropertyPanelVisible } = this
                return (
                    typeof editingElementPos.containerIndex !== 'undefined' && isPropertyPanelVisible
                )
            },
            stage () {
                const { editingElementPos, execDetail } = this
                if (editingElementPos) {
                    const stages = execDetail?.model?.stages ?? []
                    const stage = stages[editingElementPos.stageIndex]
                    return stage
                }
                return null
            },
            getElementViewName () {
                try {
                    const {
                        editingElementPos: { stageIndex, containerIndex, elementIndex },
                        execDetail: {
                            model: { stages }
                        }
                    } = this
                    const element
                        = stages[stageIndex].containers[containerIndex].elements[elementIndex]

                    return `${element.name} T${stageIndex + 1}-${containerIndex + 1}-${
                        elementIndex + 1
                    }`
                } catch (error) {
                    return ''
                }
            },
            sidePanelConfig () {
                return this.showLog
                    ? {
                        title: `${this.getElementViewName || this.$t('history.viewLog')}`,
                        width: 820
                    }
                    : {
                        title: this.$t('propertyBar'),
                        class: 'bkci-property-panel',
                        width: 640
                    }
            },
            routerParams () {
                return {
                    ...this.$route.params,
                    ...this.$route.query
                }
            },
            curItemTab () {
                if (PANELS[this.routerParams.type]) {
                    return this.routerParams.type
                }
                return PANELS.executeDetail
            },
            isExecuteDetailPanel () {
                return this.curItemTab === PANELS.executeDetail
            },
            curPanel () {
                return this.panels.find(panel => panel.name === this.curItemTab)
            },
            detailRequestKey () {
                return this.getDetailRequestKey(this.routerParams)
            },
            statusTagTheme () {
                return mapThemeOfStatus(this.execDetail?.status)
            },
            statusLabel () {
                return this.execDetail?.status ? this.$t(`details.statusMap.${this.execDetail?.status}`) : ''
            },
            isLatestBuild () {
                return this.execDetail?.buildNum === this.execDetail?.latestBuildNum && this.execDetail?.curVersion === this.execDetail?.latestVersion
            },
            effectiveIsLatestBuild () {
                return this.archiveFlag ? !this.archiveFlag : this.isLatestBuild
            },
            pipelineModel () {
                return this.execDetail?.model || {}
            },
            executeCount () {
                return this.execDetail?.executeCount ?? 1
            },
            recordList () {
                const list = [...this.execDetail?.recordList]
                return (
                    list.reverse().map((record, index) => ({
                        id: index + 1,
                        user: record.startUser
                    })) ?? []
                )
            },
            startUser () {
                return this.recordList.find(i => i.id === this.executeCount)?.user || ''
            },
            triggerType () {
                return this.execDetail?.trigger
                    ? this.$t('details.triggerTypeDesc', [this.execDetail.trigger])
                    : ''
            },
            buildEndStartTime () {
                return this.execDetail?.startTime || this.execDetail?.queueTime
            },
            showBuildEndInfoPopover () {
                return !!getBuildEndInfoConfig(this.execDetail?.buildEndInfo?.endCategory)
                    && !!this.execDetail?.buildEndInfo
            },
            showBuildRunningInfoPopover () {
                return this.isRunning
                    && !!getBuildRunningInfoConfig(this.execDetail?.buildRunningInfo?.runningCategory)
                    && !!this.execDetail?.buildRunningInfo
            },
            showPendingManualAlert () {
                return Number(this.execDetail?.buildRunningInfo?.pendingItemCount) > 0
            }
        },

        watch: {
            execDetail (val) {
                if (val) {
                    this.$updateTabTitle?.(`#${val.buildNum}  ${val.buildMsg} | ${val.pipelineName}`)
                }
            },
            detailRequestKey () {
                this.loadPipelineExecDetail()
            },
            '$route.params.type': {
                handler (newVal) {
                    if (newVal !== 'outputs') {
                        const query = { ...this.$route.query }
                        delete query.metadataKey
                        delete query.metadataValues
                        this.$router.replace({ query })
                    }
                },
                immediate: true
            }
        },
        beforeRouteEnter (to, from, next) {
            
            if (!PANELS[to.params.type]) {
                next({
                    name: 'pipelinesDetail',
                    params: {
                        ...to.params,
                        type: PANELS.executeDetail
                    },
                    query: to.query
                })
            } else {
                next()
            }
        },
        mounted () {
            this.loadPipelineExecDetail()
            webSocketMessage.installWsMessage((data) => this.handleRealtimeDetail(data))
            webSocketMessage.registeOnReconnect(() => {
                this.loadPipelineExecDetail()
            })
            // 第三方系统、通知等，点击链接进入流水线执行详情页面时，定位到具体的 task/ job (自动打开对应的侧滑框)
            const {
                stageIndex,
                elementIndex,
                containerGroupIndex,
                containerIndex
            } = this.$route.query
            const normalizedStageIndex = this.normalizeIndex(stageIndex)
            const normalizedContainerIndex = this.normalizeIndex(containerIndex)
            const normalizedElementIndex = this.normalizeIndex(elementIndex)
            const normalizedGroupIndex = this.normalizeIndex(containerGroupIndex)
            if (normalizedStageIndex !== null) {
                const editingElementPos = { stageIndex: normalizedStageIndex }
                if (normalizedContainerIndex !== null) {
                    editingElementPos.containerIndex = normalizedContainerIndex
                    if (normalizedGroupIndex !== null) editingElementPos.containerGroupIndex = normalizedGroupIndex
                    if (normalizedElementIndex !== null) editingElementPos.elementIndex = normalizedElementIndex
                }
                this.togglePropertyPanel({
                    isShow: true,
                    editingElementPos
                })
            }
        },

        beforeDestroy () {
            this.detailRequestId += 1
            this.setPipelineDetail(null)
            this.resetAtomModalMap()
            this.isLoading = false
            webSocketMessage.unInstallWsMessage()
        },

        methods: {
            captureExecutionDetailContext () {
                const key = this.getDetailRequestKey(this.routerParams)
                const round = this.execDetail?.executeCount
                const tab = this.curItemTab
                const isCurrent = () => !this._isBeingDestroyed && !this._isDestroyed
                    && key === this.getDetailRequestKey(this.routerParams)
                    && round === this.execDetail?.executeCount
                    && tab === this.curItemTab
                return {
                    isCurrent,
                    refresh: () => isCurrent() ? this.loadPipelineExecDetail() : null
                }
            },
            isNewerDetailSnapshot (candidate, baseline) {
                if (!candidate) return false
                if (!baseline) return true
                const candidateRound = Number(candidate.executeCount)
                const baselineRound = Number(baseline.executeCount)
                if (candidateRound > 0 && baselineRound > 0 && candidateRound !== baselineRound) {
                    return candidateRound > baselineRound
                }
                const candidateTime = Number(candidate.currentTimestamp)
                const baselineTime = Number(baseline.currentTimestamp)
                if (candidateTime === baselineTime) return Boolean(candidate.endTime && !baseline.endTime)
                return Number.isFinite(candidateTime) && (!Number.isFinite(baselineTime) || candidateTime > baselineTime)
            },
            getDetailRequestKey (params) {
                return [
                    params.projectId,
                    params.pipelineId,
                    params.buildNo,
                    params.executeCount,
                    params.version,
                    params.archiveFlag
                ].map(value => value ?? '').join('|')
            },
            loadPipelineExecDetail () {
                const params = { ...this.routerParams }
                const requestId = ++this.detailRequestId
                const requestKey = this.getDetailRequestKey(params)
                const isCurrentRequest = () => (
                    requestId === this.detailRequestId
                    && !this._isBeingDestroyed
                    && !this._isDestroyed
                    && requestKey === this.getDetailRequestKey(this.routerParams)
                )

                const preserveDetail = Boolean(this.execDetail && this.loadedDetailKey === requestKey)
                if (this.pendingDetailKey !== requestKey) this.pendingRealtimeDetail = null
                this.pendingDetailKey = requestKey
                this.hasNoPermission = false
                this.detailLoadFailed = false
                if (!preserveDetail) {
                    this.loadedDetailKey = ''
                    if (this.execDetail) this.setPipelineDetail(null)
                    this.isLoading = true
                }

                return this.fetchPipelineExecDetail(params)
                    .then(data => {
                        if (!isCurrentRequest()) return null
                        if (!data) throw new Error('Missing execution detail')
                        const realtimeDetail = this.pendingRealtimeDetail
                        const currentDetail = this.loadedDetailKey === requestKey ? this.execDetail : null
                        const incoming = this.isNewerDetailSnapshot(realtimeDetail, data) ? realtimeDetail : data
                        const newest = this.isNewerDetailSnapshot(currentDetail, incoming) ? currentDetail : incoming
                        this.loadedDetailKey = requestKey
                        if (newest !== currentDetail) this.setPipelineDetail(newest)
                        this.isLoading = false
                        return newest
                    })
                    .catch(error => {
                        if (!isCurrentRequest()) return null
                        this.isLoading = false
                        const statuses = [error?.code, error?.status, error?.httpStatus, error?.response?.status].map(Number)
                        if (statuses.includes(403)) {
                            this.hasNoPermission = true
                            this.loadedDetailKey = ''
                            this.setPipelineDetail(null)
                        } else if (statuses.includes(404)) {
                            this.loadedDetailKey = ''
                            if (this.execDetail) this.setPipelineDetail(null)
                            this.$store.commit(FETCH_ERROR, { message: error?.message, code: 404 })
                        } else if (!preserveDetail) {
                            this.detailLoadFailed = true
                        }
                        return null
                    })
                    .finally(() => {
                        if (!isCurrentRequest()) return
                        this.pendingDetailKey = ''
                        this.pendingRealtimeDetail = null
                    })
            },
            handleRealtimeDetail (data) {
                const params = this.routerParams
                const { pipelineId, buildNo, executeCount, archiveFlag } = params
                const round = Number(data?.executeCount)
                if (this._isBeingDestroyed || this._isDestroyed || this.hasNoPermission
                    || String(archiveFlag) === 'true'
                    || data?.id !== buildNo || data?.pipelineId !== pipelineId
                    || !Number.isSafeInteger(round) || round < 1
                    || (executeCount && round !== Number(executeCount))) return

                const key = this.getDetailRequestKey(params)
                if (this.loadedDetailKey !== key || !this.execDetail) {
                    // HTTP must authorize this build and round before any push is displayed.
                    if (this.pendingDetailKey === key && this.isNewerDetailSnapshot(data, this.pendingRealtimeDetail)) {
                        this.pendingRealtimeDetail = data
                    }
                    return
                }
                if (this.isNewerDetailSnapshot(data, this.execDetail)) this.setPipelineDetail(data)
            },
            ...mapActions('atom', [
                'reviewExcuteAtom',
                'togglePropertyPanel',
                'toggleStageReviewPanel',
                'fetchPipelineExecDetail',
                'setPipelineDetail',
                'getInitLog',
                'getAfterLog',
                'pausePlugin',
                'resetAtomModalMap'
            ]),
            handlerScroll (e) {
                this.show = e.target.scrollTop > 88
            },
            ...mapActions('common', ['requestInterceptAtom']),

            hideSidePanel () {
                this.showLog = false
            },
            async handlePluginContinue ({ taskId, skip }, done) {
                try {
                    if (!this.isExecuteDetailPanel || !this.$refs.execDetailPanel || typeof this.$refs.execDetailPanel.handleContinue !== 'function') {
                        throw new Error(this.$t('subpage.retryFail'))
                    }
                    await this.$refs.execDetailPanel.handleContinue({ taskId, skip })
                } catch (err) {
                    this.$showTips({
                        theme: 'error',
                        message: err.message || this.$t('subpage.retryFail')
                    })
                } finally {
                    done?.()
                }
            },
            async handlePluginReview (atom) {
                if (!this.isExecuteDetailPanel || !this.$refs.execDetailPanel || typeof this.$refs.execDetailPanel.reviewAtom !== 'function') {
                    this.$showTips({ theme: 'error', message: this.$t('subpage.retryFail') })
                    return
                }
                await this.$refs.execDetailPanel.reviewAtom(atom)
            },
            normalizeIndex (value) {
                const index = Number(value)
                return Number.isInteger(index) && index >= 0 ? index : null
            },
            handlePipelineClick (args) {
                this.togglePropertyPanel({
                    isShow: true,
                    editingElementPos: args
                })
            },
            locateBuildEndPosition (position = {}) {
                try {
                    const { stageId, containerId, taskId } = position
                    const stages = this.execDetail?.model?.stages || []
                    const stageIndex = stages.findIndex(stage => stage.id === stageId)
                    if (stageIndex < 0) return null

                    const stage = stages[stageIndex]
                    let containerIndex
                    let containerGroupIndex
                    let container

                    for (let i = 0; i < stage.containers.length; i++) {
                        const item = stage.containers[i]
                        if (item.matrixGroupFlag) {
                            const groupIndex = item.groupContainers?.findIndex?.(group => group.id === containerId)
                            if (groupIndex > -1) {
                                containerIndex = i
                                containerGroupIndex = groupIndex
                                container = item.groupContainers[groupIndex]
                                break
                            }
                        } else if (item.id === containerId) {
                            containerIndex = i
                            container = item
                            break
                        }
                    }

                    if (!container) return null

                    const elementIndex = taskId
                        ? container.elements?.findIndex?.(element => element.id === taskId)
                        : undefined

                    return {
                        stageIndex,
                        containerIndex: containerIndex > -1 ? containerIndex : undefined,
                        containerGroupIndex: containerGroupIndex > -1 ? containerGroupIndex : undefined,
                        elementIndex: elementIndex > -1 ? elementIndex : undefined
                    }
                } catch (e) {
                    console.error(e)
                    return null
                }
            },
            getBuildEndLocateFailedKey () {
                const endKey = getBuildEndInfoConfig(this.execDetail?.buildEndInfo?.endCategory)?.locateFailedKey
                if (endKey) return endKey
                return getBuildRunningInfoConfig(this.execDetail?.buildRunningInfo?.runningCategory)?.locateFailedKey
            },
            runBuildEndPositionAction (position, { openLog = false } = {}) {
                console.log(position,'???????')
                
                const editingElementPos = this.locateBuildEndPosition(position)
                if (!editingElementPos) {
                    const locateFailedKey = this.getBuildEndLocateFailedKey()
                    this.$showTips({
                        message: this.$t(locateFailedKey),
                        theme: 'warning'
                    })
                    return
                }
                const needSwitchTab = this.curItemTab !== PANELS.executeDetail
                if (needSwitchTab) {
                    this.switchTab({ name: PANELS.executeDetail })
                }
                const runAction = () => {
                    this.$refs.execDetailPanel?.setBuildEndHighlight?.({
                        editingElementPos,
                        position
                    })
                    if (openLog) {
                        this.togglePropertyPanel({
                            isShow: true,
                            editingElementPos
                        })
                    }
                }
                this.$nextTick(() => {
                    needSwitchTab ? this.$nextTick(runAction) : runAction()
                })
            },
            handleBuildEndPositionHighlight (position) {
                this.runBuildEndPositionAction(position)
            },
            handleBuildEndPositionLocate (position) {
                this.runBuildEndPositionAction(position, { openLog: true })
            },
            showLocateFailedTips () {
                const locateFailedKey = this.getBuildEndLocateFailedKey()
                this.$showTips({
                    message: this.$t(locateFailedKey),
                    theme: 'warning'
                })
            },
            runWithExecuteDetailTab (runAction) {
                const needSwitchTab = this.curItemTab !== PANELS.executeDetail
                if (needSwitchTab) {
                    this.switchTab({ name: PANELS.executeDetail })
                }
                this.$nextTick(() => {
                    needSwitchTab ? this.$nextTick(runAction) : runAction()
                })
            },
            closeAsidePanels () {
                this.togglePropertyPanel({
                    isShow: false,
                    showPanelType: ''
                })
                this.toggleStageReviewPanel({
                    showStageReviewPanel: {
                        isShow: false
                    }
                })
                this.$refs.execDetailPanel?.toggleCheckDialog?.(false)
            },
            handlePendingManualProcess (item = {}) {
                switch (item.itemType) {
                    case PENDING_ITEM_TYPE.TASK_PAUSE:
                        this.openPendingPausePanel(item)
                        break
                    case PENDING_ITEM_TYPE.TASK_REVIEW:
                        this.openPendingReviewDialog(item)
                        break
                    case PENDING_ITEM_TYPE.STAGE_REVIEW:
                        this.openPendingStageReview(item)
                        break
                    default:
                        this.handleBuildEndPositionHighlight(item)
                }
            },
            openPendingPausePanel (item) {
                const editingElementPos = this.locateBuildEndPosition(item)
                if (!editingElementPos || typeof editingElementPos.elementIndex !== 'number') {
                    this.showLocateFailedTips()
                    return
                }
                this.runWithExecuteDetailTab(() => {
                    this.toggleStageReviewPanel({
                        showStageReviewPanel: {
                            isShow: false
                        }
                    })
                    this.$refs.execDetailPanel?.toggleCheckDialog?.(false)
                    this.togglePropertyPanel({
                        isShow: true,
                        showPanelType: 'PAUSE',
                        editingElementPos
                    })
                    this.$refs.execDetailPanel?.setBuildEndHighlight?.({
                        editingElementPos,
                        position: item
                    })
                })
            },
            openPendingReviewDialog (item) {
                if (!item.taskId) {
                    this.showLocateFailedTips()
                    return
                }
                const editingElementPos = this.locateBuildEndPosition(item)
                this.runWithExecuteDetailTab(() => {
                    this.closeAsidePanels()
                    this.$refs.execDetailPanel?.reviewAtom?.({ id: item.taskId })
                    if (editingElementPos) {
                        this.$refs.execDetailPanel?.setBuildEndHighlight?.({
                            editingElementPos,
                            position: item
                        })
                    }
                })
            },
            openPendingStageReview (item) {
                const stages = this.execDetail?.model?.stages || []
                const stageIndex = stages.findIndex(stage => stage.id === item.stageId)
                if (stageIndex < 0) {
                    this.showLocateFailedTips()
                    return
                }
                const stage = stages[stageIndex]
                const type = stage?.checkOut?.status === 'REVIEWING' ? 'checkOut' : 'checkIn'
                this.runWithExecuteDetailTab(() => {
                    this.togglePropertyPanel({
                        isShow: false,
                        showPanelType: ''
                    })
                    this.$refs.execDetailPanel?.toggleCheckDialog?.(false)
                    this.toggleStageReviewPanel({
                        showStageReviewPanel: {
                            isShow: true,
                            type
                        },
                        editingElementPos: {
                            stageIndex
                        }
                    })
                })
            },
            handleStageCheck ({ type, stageIndex }) {
                this.toggleStageReviewPanel({
                    showStageReviewPanel: {
                        isShow: true,
                        type
                    },
                    editingElementPos: {
                        stageIndex
                    }
                })
            },
            async qualityCheck ({ elementId, action }, done) {
                try {
                    const data = {
                        projectId: this.routerParams.projectId,
                        pipelineId: this.routerParams.pipelineId,
                        buildId: this.routerParams.buildNo,
                        elementId,
                        action
                    }
                    const res = await this.reviewExcuteAtom(data)
                    if (res) {
                        this.$showTips({
                            message: this.$t('editPage.operateSuc'),
                            theme: 'success'
                        })
                    }
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                } finally {
                    done()
                }
            },

            switchTab (panel) {
                this.$router.push({
                    name: 'pipelinesDetail',
                    params: {
                        ...this.routerParams,
                        type: panel.name
                    },
                    query: this.$route.query
                })
            },
            collapseSummary () {
                this.summaryVisible = !this.summaryVisible
            }
        }
    }
</script>

<style lang="scss">
@import "@/scss/conf";
@import "@/scss/pipelineStatus";
@import "@/scss/mixins/ellipsis";
@import "@/scss/buildStatus";
@import "@/scss/detail-tab.scss";
.pipeline-detail-wrapper.biz-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-top: 1px solid #dde4eb;
  overflow: auto;
  scrollbar-gutter: stable;
  background: #F5F7FA;

  .exec-detail-summary-header {
    padding: 8px 24px;
    background: white;
    display: flex;
    justify-content: space-between;
    position: sticky;
    top: 0;
    z-index: 22;
    .exec-detail-build-summary-anchor {
      @include build-status();
      position: absolute;
      content: "";
      width: 6px;
      height: 24px;
      left: 0;
    }
    &-title {
      height: 24px;
      display: flex;
      align-items: center;
      flex: 1;
      margin: 0;
      overflow: hidden;

      .exec-status-tag {
        margin: 0;
      }

      .build-end-info-popover-trigger,
      .build-running-info-popover-trigger {
        display: inline-flex;
        align-items: center;
      }

      .exec-status-label {
        display: grid;
        align-items: center;
        grid-auto-flow: column;
        grid-gap: 6px;
      }

      .exec-detail-summary-header-build-msg {
        flex: 1;
        margin: 0 24px 0 8px;
        font-size: 14px;
        @include ellipsis();
        color: #313238;
        min-width: auto;
      }
    }
    &-trigger {
      display: flex;
      align-items: center;
      flex-shrink: 0;
      font-size: 12px;
      .exec-trigger-profile {
        width: 24px;
        height: 24px;
        border-radius: 12px;
        margin-right: 6px;
        color: #c4c6cc;
      }
    }
  }

  .summary-header-shadow {
    width: 100%;
    min-height: 2px;
    position: sticky;
    top: 38px;
    z-index: 8;
    box-shadow: 0 2px 2px 0 rgba(0, 0, 0, 0.15);
  }

  .pipeline-exec-gap {
    background: #f5f7fa;
    height: 16px;
    width: 100%;
    flex-shrink: 0;
    position: sticky;
    top: 40px;
    z-index: 8;
    .summary-collapsed-handler {
        position: absolute;
        top: 0;
        left: 50%;
        transform: translateX(-60px);
        width: 120px;
        height: 12px;
        background: #EAEBF0;

        border-radius:  0 0 2px 2px;
        text-align: center;
        line-height: 12px;
        font-size: 12px;
        transition: all 0.3s;
        z-index: 10;
        cursor: pointer;
        &:hover {
            color: $primaryColor;
        }
        &.is-collapsed {
            > i {
                display: block;
                transform: rotate(180deg);
            }
        }
      }
  }
  .exec-detail-switcher {
    background: #f0f1f5;
    height: 42px;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    margin: 0 24px;
    position: sticky;
    top: 56px;
    z-index: 8;

    .exec-detail-switcher-tab {
        padding: 0 18px;
        height: 42px;
        font-size: 14px;
        display: flex;
        align-items: center;
        position: relative;
        cursor: pointer;
        &:hover {
            color: $primaryColor;
        }
        &:not(:first-child)::before {
            position: absolute;
            content: '';
            width: 1px;
            height: 16px;
            background-color: #C4C6CC;
            top: 13px;
            left: 0;
        }

        &:last-child::after {
            position: absolute;
            content: '';
            width: 1px;
            height: 16px;
            background-color: #C4C6CC;
            top: 13px;
            right: 0;
        }

        &.active {
            background-color: white;
            border-radius: 4px 4px 0 0;

            &::before,
            &::after {
                background-color: white;
            }
        }
        &.active + ::before {
            display: none;
        }
    }
  }

  .exec-detail-main {
    background: white;
    margin: 0 24px;
    flex: 1;
    box-shadow: 0 2px 2px 0 #00000026;
    &.is-outputs-panel {
        overflow: hidden;
    }
  }
  .exec-pipeline {
    position: relative;
    overflow: auto;
    height: 100%;
    ::v-deep .devops-stage-list {
      padding-bottom: 25px;
    }
  }

  .bk-sideslider-wrapper {
    top: 0;
    padding-bottom: 0;
    height: 100vh;
    .bk-sideslider-content {
      height: calc(100% - 60px);
    }
  }
  .inner-header-title > i {
    font-size: 12px;
    color: $fontLighterColor;
    font-style: normal;
  }
  .pipeline-detail-wrapper .inner-header {
    cursor: default;
    .bk-tooltip-popper[x-placement^="bottom"] {
      top: 37px !important;
    }
  }

  .pipeline-info {
    width: 480px;
    display: flex;
    height: 100%;
    align-items: center;
    justify-content: space-between;
    .info-item {
      line-height: normal;
      font-size: 0;
      display: flex;
      color: $fontWeightColor;
      & > span {
        display: inline-block;
        font-size: 14px;
      }
      .trigger-mode {
        display: inline-block;
        max-width: 120px;
      }
      .item-label {
        color: #c4cdd6;
      }
      .icon-stop-shape {
        font-size: 15px;
        color: $primaryColor;
        cursor: pointer;
        border: 1px solid $primaryColor;
        padding: 1px;
        border-radius: 50%;
        margin-left: 3px;
      }
    }
  }
}
</style>
