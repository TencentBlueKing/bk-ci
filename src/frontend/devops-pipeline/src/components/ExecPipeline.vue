<template>
    <div class="exec-pipeline-wrapper">
        <div class="pipeline-exec-summary">
            <div class="pipeline-exec-count">
                <span>{{ $t("details.num") }}</span>
                <bk-select
                    ext-cls="pipeline-exec-count-select"
                    :value="executeCount"
                    :popover-width="200"
                    :clearable="false"
                    @selected="handleExecuteCountChange"
                >
                    <bk-option
                        v-for="item in executeCounts"
                        :key="item.id"
                        :id="item.id"
                        :name="item.name"
                    >
                        <p class="exec-count-select-option">
                            <span>{{ item.name }}</span>
                            <span class="exec-count-select-option-user">{{ item.user }}</span>
                        </p>
                    </bk-option>
                </bk-select>
                <span class="exec-status-label">
                    {{ $t("details.times", [statusLabel]) }}
                    <span
                        v-if="execDetail.status === 'CANCELED'"
                        v-bk-tooltips="`${$t('details.canceller')}：${execDetail.cancelUserId}`"
                        class="devops-icon icon-info-circle"
                    >
                    </span>
                </span>
            </div>
            <ul class="pipeline-exec-timeline">
                <li
                    class="pipeline-exec-timeline-item"
                    v-for="step in timeSteps"
                    :key="step.title"
                >
                    <span>
                        {{ step.title }}
                    </span>
                    <p v-bk-tooltips="step.popup" class="time-step-divider"></p>
                    <p class="constant-width-num">
                        {{ step.description }}
                    </p>
                </li>
            </ul>
        </div>
        <section class="pipeline-exec-content">
            <header class="pipeline-style-setting-header">
                <!-- <div class="bk-button-group">
                    <bk-button v-for="item in pipelineModes" :key="item.id" :class="item.cls">
                        {{ item.label }}
                    </bk-button>
                </div> -->
                <bk-checkbox
                    :true-value="true"
                    :false-value="false"
                    :value="hideSkipExecTask"
                    @change="setHideSkipExecTask"
                    ext-cls="hide-skip-pipeline-task"
                >
                    {{ $t("details.hideSkipStep") }}
                </bk-checkbox>
                <bk-button text theme="primary" @click="showCompleteLog">
                    <i class="devops-icon icon-txt"></i>
                    {{ $t("history.viewLog") }}
                </bk-button>
            </header>
            <simplebar
                class="exec-pipeline-scroll-box"
                :class-names="{
                    track: 'pipeline-scrollbar-track'
                }"
                data-simplebar-auto-hide="false"
            >
                <div class="exec-pipeline-ui-wrapper">
                    <bk-pipeline
                        :editable="false"
                        ref="bkPipeline"
                        is-exec-detail
                        :current-exec-count="executeCount"
                        :cancel-user-id="cancelUserId"
                        :user-name="userName"
                        :pipeline="curPipeline"
                        v-bind="$attrs"
                        @click="handlePiplineClick"
                        @stage-check="handleStageCheck"
                        @stage-retry="handleRetry"
                        @atom-quality-check="qualityCheck"
                        @atom-review="reviewAtom"
                        @atom-continue="handleContinue"
                        @atom-exec="handleExec"
                        @debug-container="debugDocker"
                    />
                </div>
            </simplebar>
            <footer
                v-if="showErrorPopup"
                ref="errorPopup"
                :class="{
                    'exec-errors-popup': true,
                    visible: showErrors
                }"
            >
                <bk-button
                    text
                    class="drag-dot"
                    theme="normal"
                    @click="toggleErrorPopup"
                >
                    <i class="bk-icon icon-angle-up toggle-error-popup-icon" />
                </bk-button
                >
                <bk-tab
                    class="pipeline-exec-error-tab"
                    :active="activeTab"
                    :label-height="42"
                    type="unborder-card"
                >
                    <template slot="setting">
                        <bk-link
                            class="pipeline-error-guide-link"
                            theme="primary"
                            target="_blank"
                            :href="pipelineErrorGuideLink"
                        >
                            <span class="fix-error-jump">
                                <logo class="fix-error-jump-icon" size="20" name="tiaozhuan" />
                                {{ $t("details.pipelineErrorGuide") }}
                            </span>
                        </bk-link>
                    </template>
                    <bk-tab-panel v-for="(panel, index) in panels" v-bind="panel" :key="index" :render-label="renderLabel">
                        <bk-table
                            ext-cls="error-popup-table"
                            :data="errorList"
                            :border="false"
                            @row-click="(row) => setAtomLocate(row)"
                            highlight-current-row
                        >
                            <bk-table-column width="80">
                                <div slot-scope="props" class="exec-error-type-cell">
                                    <span class="exec-error-locate-icon">
                                        <Logo
                                            v-if="isActiveErrorAtom(props.row)"
                                            name="location-right"
                                            size="18"
                                        />
                                    </span>
                                    <logo
                                        v-if="props.row.errorTypeConf"
                                        :name="props.row.errorTypeConf.icon"
                                        size="18"
                                    />
                                </div>
                            </bk-table-column>
                            <bk-table-column
                                v-for="(column, i) in errorsTableColumns"
                                v-bind="column"
                                :key="i"
                            />
                            <bk-table-column
                                :label="$t('details.pipelineErrorInfo')"
                            >
                                <template v-slot="props">
                                    <div class="build-error-cell">
                                        <span
                                            class="build-error-info">
                                            {{ props.row.errorMsg }}
                                        </span>
                                        <bk-button
                                            v-if="!props.row.cleaned"
                                            class="build-error-see-more"
                                            theme="primary"
                                            @click.stop="setAtomLocate(props.row, true)"
                                            text
                                        >
                                            {{$t('history.viewLog')}}
                                        </bk-button>
                                    </div>
                                </template>
                            </bk-table-column>
                        </bk-table>

                    </bk-tab-panel>
                </bk-tab>
            </footer>
        </section>
        <bk-dialog
            v-model="showRetryStageDialog"
            render-directive="if"
            ext-cls="stage-retry-dialog"
            :width="400"
            :auto-close="false"
            @confirm="retryPipeline(true)"
        >
            <bk-radio-group v-model="failedContainer">
                <bk-radio :value="false">{{ $t("editPage.retryAllJobs") }}</bk-radio>
                <bk-radio :value="true">{{ $t("editPage.retryFailJobs") }}</bk-radio>
            </bk-radio-group>
        </bk-dialog>
        <check-atom-dialog
            :is-show-check-dialog="isShowCheckDialog"
            :toggle-check="toggleCheckDialog"
            :element="currentAtom"
        />

        <div class="queue-time-detail-popup">
            <div class="pipeline-time-detail-sum">
                <span>{{ $t("details.queueCost") }}</span>
                <span class="constant-width-num">{{ queueCost }}</span>
            </div>
        </div>
        <div class="time-detail-popup">
            <div class="pipeline-time-detail-sum">
                <span>{{ $t("details.totalCost") }}</span>
                <span class="constant-width-num">{{ isRunning ? `${$t("details.running")}...` : totalCost }}</span>
            </div>
            <ul class="pipeline-time-detail-sum-list">
                <li v-for="cost in timeDetailRows" :key="cost.field">
                    <span>{{ cost.label }}</span>
                    <span class="constant-width-num">{{ cost.value }}</span>
                </li>
            </ul>
        </div>
        <template v-if="execDetail && showLog">
            <complete-log
                @close="hideCompleteLog"
                :execute-count="executeCount"
            ></complete-log>
        </template>
    </div>
</template>

<script>
    import CheckAtomDialog from '@/components/CheckAtomDialog'
    import CompleteLog from '@/components/ExecDetail/completeLog'
    import Logo from '@/components/Logo'
    import { errorTypeMap } from '@/utils/pipelineConst'
    import { convertMillSec, convertTime } from '@/utils/util'
    import simplebar from 'simplebar-vue'
    import 'simplebar-vue/dist/simplebar.min.css'
    import { mapActions, mapState } from 'vuex'
    export default {
        components: {
            simplebar,
            CheckAtomDialog,
            CompleteLog,
            Logo
        },
        props: {
            execDetail: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                showRetryStageDialog: false,
                showLog: false,
                retryTaskId: '',
                skipTask: false,
                failedContainer: false,
                activeTab: 'errors',
                currentAtom: {},
                pipelineMode: 'uiMode',
                showErrors: false,
                activeErrorAtom: null,
                afterAsideVisibleDone: null,
                errorRow: null,
                isErrorOverflow: [],
                curPipeline: this.execDetail?.model,
                pipelineErrorGuideLink: this.$pipelineDocs.PIPELINE_ERROR_GUIDE_DOC
            }
        },
        computed: {
            ...mapState('common', ['ruleList', 'templateRuleList']),
            ...mapState('atom', [
                'hideSkipExecTask',
                'showPanelType',
                'isPropertyPanelVisible'
            ]),

            panels () {
                return [
                    {
                        name: 'errors',
                        label: this.$t('Errors')
                    }
                ]
            },
            isRunning () {
                return this.execDetail?.status === 'RUNNING'
            },
            timeDetailConf () {
                return {
                    allowHtml: true,
                    theme: 'light',
                    placement: 'bottom'
                }
            },
            errorList () {
                return this.execDetail?.errorInfoList?.map((error, index) => ({
                    ...error,
                    errorTypeAlias: this.$t(errorTypeMap[error.errorType].title),
                    errorTypeConf: errorTypeMap[error.errorType]
                }))
            },
            showErrorPopup () {
                return Array.isArray(this.errorList) && this.errorList.length > 0
            },
            timeDetailRows () {
                return ['executeCost', 'systemCost', 'waitCost'].map((key) => ({
                    field: key,
                    label: this.$t(`details.${key}`),
                    value: this.execDetail?.model?.timeCost?.[key]
                        ? convertMillSec(this.execDetail.model.timeCost[key])
                        : '--'
                }))
            },
            queueCost () {
                return this.execDetail?.queueTimeCost
                ? convertMillSec(this.execDetail?.queueTimeCost)
                : '--'
            },
            totalCost () {
                return this.execDetail?.model?.timeCost?.totalCost
                ? convertMillSec(this.execDetail.model.timeCost.totalCost)
                : '--'
            },
            errorsTableColumns () {
                return [
                    // {
                    //     label: this.$t('details.pipelineErrorType'),
                    //     prop: 'errorTypeAlias',
                    //     width: 150
                    // },
                    {
                        label: this.$t('details.pipelineErrorPos'),
                        prop: 'taskName',
                        width: 200
                    },
                    {
                        label: this.$t('details.pipelineErrorCode'),
                        prop: 'errorCode',
                        width: 150
                    }

                ]
            },
            userName () {
                return this.$userInfo && this.$userInfo.username ? this.$userInfo.username : ''
            },
            isInstanceEditable () {
                return this.execDetail?.model?.instanceFromTemplate
            },
            curMatchRules () {
                return this.$route.path.indexOf('template') > 0
                    ? this.templateRuleList
                    : this.isInstanceEditable
                        ? this.templateRuleList.concat(this.ruleList)
                        : this.ruleList
            },
            statusLabel () {
                return this.execDetail?.status
                ? this.$t(`details.statusMap.${this.execDetail?.status}`)
                : ''
            },
            cancelUserId () {
                return this.execDetail?.cancelUserId ?? '--'
            },
            executeCount () {
                return this.execDetail?.executeCount ?? 1
            },
            pipelineModes () {
                return [
                    {
                        label: this.$t('details.codeMode'),
                        disabled: true,
                        id: 'codeMode',
                        cls: this.pipelineMode === 'codeMode' ? 'is-selected' : ''
                    },
                    {
                        label: this.$t('details.uiMode'),
                        id: 'uiMode',
                        cls: this.pipelineMode === 'uiMode' ? 'is-selected' : ''
                    }
                ]
            },
            timeSteps () {
                return [
                    {
                        title: this.$t('details.triggerTime'),
                        description: convertTime(this.execDetail?.queueTime),
                        popup: {
                            ...this.timeDetailConf,
                            content: '.queue-time-detail-popup'
                        }
                    },
                    {
                        title: this.$t('details.startTime'),
                        description: convertTime(this.execDetail?.startTime),
                        popup: {
                            ...this.timeDetailConf,
                            content: '.time-detail-popup'
                        }
                    },
                    {
                        title: this.$t('details.endTime'),
                        description: convertTime(this.execDetail?.endTime),
                        popup: {
                            disabled: true
                        }
                    }
                ]
            },
            executeCounts () {
                const len = this.execDetail?.startUserList?.length ?? 0
                return (
                    this.execDetail?.startUserList?.map((user, index) => ({
                    id: len - index,
                    name: `${len - index} / ${len}`,
                    user
                    })) ?? []
                )
            },
            routerParams () {
                return this.$route.params
            },
            errorPopupHeight () {
                return getComputedStyle(this.$refs.errorPopup)?.height ?? '42px'
            }
        },
        watch: {
            isPropertyPanelVisible (val) {
                if (!val && this.showPanelType !== '') {
                    this.afterAsideVisibleDone?.()
                    this.afterAsideVisibleDone = null
                }
            },
            'execDetail.model': function (val) {
                if (val) {
                    this.curPipeline = val
                }
            },
            executeCount () {
                if (this.activeErrorAtom) {
                    this.locateError(this.activeErrorAtom, false)
                    this.activeErrorAtom = null
                }
                this.$nextTick(() => {
                    if (this.errorList?.length > 0) {
                        this.setAtomLocate(this.errorList[0])
                        this.setShowErrorPopup()
                    }
                })
            }

        },
        updated () {
            if (this.showErrorPopup) {
                this.setScrollBarPostion()
            }
        },
        mounted () {
            this.requestInterceptAtom(this.routerParams)
            if (this.errorList?.length > 0) {
                this.setScrollBarPostion()
                setTimeout(() => {
                    this.setAtomLocate(this.errorList[0])
                }, 600)
            }
        },
        beforeDestroy () {
            this.togglePropertyPanel({
                isShow: false
            })
            if (this.activeErrorAtom?.taskId) {
                this.locateError(this.activeErrorAtom, false)
            }
            const rootCssVar = document.querySelector(':root')
            rootCssVar.style.setProperty('--track-bottom', 0)
        },
        methods: {
            ...mapActions('atom', [
                'setHideSkipExecTask',
                'reviewExcuteAtom',
                'togglePropertyPanel',
                'toggleStageReviewPanel',
                'requestPipelineExecDetail',
                'pausePlugin'
            ]),
            ...mapActions('common', ['requestInterceptAtom']),
            ...mapActions('pipelines', ['requestRetryPipeline']),
            renderLabel (h, name) {
                const panel = this.panels.find(panel => panel.name === name)
                return h(
                    'p',
                    {},
                    [
                        h(
                            'span',
                            {
                                class: 'panel-name pointer',
                                on: {
                                    click: this.setShowErrorPopup
                                }
                            },
                            panel?.label ?? name
                        ),
                        h(
                            'bk-tag',
                            {
                                props: {
                                    theme: 'info',
                                    radius: '4px'
                                }
                            },
                            this.errorList.length
                        )
                    ]

                )
            },
            setScrollBarPostion () {
                const rootCssVar = document.querySelector(':root')
                rootCssVar.style.setProperty('--track-bottom', this.showErrors ? this.errorPopupHeight : '42px')
            },
            isActiveErrorAtom (atom) {
                return this.activeErrorAtom?.taskId === atom.taskId && this.activeErrorAtom?.containerId === atom.containerId
            },
            isSkip (status) {
                return ['SKIP'].includes(status)
            },
            showErrorMsgDetail (row) {
                this.errorRow = row
            },
            toggleAsidePropertyPanel (...args) {
                this.hideCompleteLog()
                this.togglePropertyPanel(...args)
            },
            hideCompleteLog () {
                this.showLog = false
            },
            showCompleteLog () {
                if (this.isPropertyPanelVisible) {
                    this.togglePropertyPanel({
                        isShow: false
                    })
                }
                this.showLog = true
            },
            toggleErrorPopup () {
                this.showErrors = !this.showErrors
            },
            setShowErrorPopup () {
                this.showErrors = true
            },

            handlePiplineClick (args) {
                this.toggleAsidePropertyPanel({
                    isShow: true,
                    editingElementPos: args
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
            async qualityCheck ({ elementId, action }) {
                try {
                    const data = {
                        ...this.routerParams,
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
                }
            },
            handleRetry ({ taskId, skip = false }) {
                this.showRetryStageDialog = true
                this.retryTaskId = taskId
                this.skipTask = skip
            },
            async reviewAtom (atom) {
                // 人工审核
                if (atom?.computedReviewers?.includes?.(this.userName)) {
                    this.currentAtom = atom
                    this.toggleCheckDialog(true)
                }
            },
            toggleCheckDialog (isShow = false) {
                this.isShowCheckDialog = isShow
                if (!isShow) {
                    this.currentAtom = {}
                }
            },
            async handleContinue ({ taskId, skip = false }) {
                this.retryTaskId = taskId
                this.skipTask = skip
                await this.retryPipeline()
            },
            async handleExec (
                {
                    stageIndex,
                    containerIndex,
                    containerGroupIndex,
                    isContinue,
                    elementIndex,
                    showPanelType,
                    stageId,
                    containerId,
                    taskId,
                    atom
                },
                done
            ) {
                if (!isContinue) {
                    const postData = {
                        projectId: this.routerParams.projectId,
                        pipelineId: this.routerParams.pipelineId,
                        buildId: this.routerParams.buildNo,
                        stageId,
                        containerId,
                        taskId,
                        isContinue,
                        element: atom
                    }

                    try {
                        await this.pausePlugin(postData)
                        await this.requestPipelineExecDetail(this.routerParams)
                    } catch (err) {
                        this.$showTips({
                            message: err.message || err,
                            theme: 'error'
                        })
                        done()
                    }
                } else {
                    this.toggleAsidePropertyPanel({
                        isShow: true,
                        showPanelType,
                        editingElementPos: {
                            stageIndex,
                            containerIndex,
                            containerGroupIndex,
                            elementIndex
                        }
                    })
                    this.afterAsideVisibleDone = done
                }
            },
            async retryPipeline (isStageRetry) {
                let message, theme
                this.showRetryStageDialog = false
                try {
                    // 请求执行构建
                    const res = await this.requestRetryPipeline({
                        projectId: this.routerParams.projectId,
                        pipelineId: this.routerParams.pipelineId,
                        buildId: this.routerParams.buildNo,
                        taskId: this.retryTaskId,
                        skip: this.skipTask,
                        ...(isStageRetry ? { failedContainer: this.failedContainer } : {})
                    })
                    if (res.id) {
                        message = this.$t(this.skipTask ? 'skipSuc' : 'subpage.retrySuc')
                        theme = 'success'
                        res?.executeCount && this.handleExecuteCountChange(res.executeCount)
                    } else {
                        message = res?.message ?? this.$t(this.skipTask ? 'skipFail' : 'subpage.retryFail')
                        theme = 'error'
                    }
                } catch (err) {
                    this.handleError(err, [
                        {
                            actionId: this.$permissionActionMap.execute,
                            resourceId: this.$permissionResourceMap.pipeline,
                            instanceId: [
                                {
                                    id: this.routerParams.pipelineId,
                                    name: this.routerParams.pipelineId
                                }
                            ],
                            projectId: this.routerParams.projectId
                        }
                    ])
                } finally {
                    message
                        && this.$showTips({
                            message,
                            theme
                        })
                    this.retryTaskId = ''
                    this.skipTask = false
                }
            },
            locate (row, isLocate = true) {
                try {
                    const { stageId, containerId, taskId, matrixFlag } = row
                    let containerGroupIndex, containerIndex, matrixId
                    const stageIndex = this.curPipeline.stages.findIndex(stage => stage.id === stageId)
                    const stage = this.curPipeline.stages[stageIndex]
                    let container
                    if (matrixFlag) {
                        const numContainerId = parseInt(containerId, 10)
                        matrixId = Math.floor(numContainerId / 1000).toString()
                        containerIndex = stage.containers.findIndex(item => item.id === matrixId)
                        containerGroupIndex = stage.containers[containerIndex]?.groupContainers?.findIndex?.(item => item.id === containerId)
                        container = stage.containers[containerIndex].groupContainers[containerGroupIndex]
                    } else {
                        container = stage.containers.find((item, index) => {
                            if (item.id === containerId) {
                                containerIndex = index
                                return true
                            }
                            return false
                        })
                    }

                    const elementIndex = container.elements.findIndex(element => element.id === taskId)
                    return {
                        matrixId,
                        stageIndex: stageIndex > -1 ? stageIndex : undefined,
                        containerIndex: containerIndex > -1 ? containerIndex : undefined,
                        containerGroupIndex: containerGroupIndex > -1 ? containerGroupIndex : undefined,
                        elementIndex: elementIndex > -1 ? elementIndex : undefined
                    }
                } catch (e) {
                    console.log(e)
                    return {}
                }
            },
            async locateError (row, isLocate = true, showLog = false) {
                try {
                    const {
                        stageIndex,
                        containerIndex,
                        containerGroupIndex,
                        matrixId,
                        elementIndex
                    } = this.locate(row)
                    const { stageId, containerId, matrixFlag } = row
                    let container = this.curPipeline.stages[stageIndex].containers[containerIndex]
                    if (matrixFlag) {
                        container = container.groupContainers[containerGroupIndex]
                        if (isLocate) {
                            await this.$refs.bkPipeline.expandMatrix(stageId, matrixId, containerId)
                        }
                    }
                    const element = container.elements[elementIndex]
                    if (element) {
                        if (element.additionalOptions?.elementPostInfo) { // isPostActionAtom
                            await this.$refs.bkPipeline.expandPostAction(stageId, matrixId, containerId)
                        }
                        this.$set(element, 'locateActive', isLocate)
                    } else {
                        this.$set(container, 'locateActive', isLocate)
                    }
                    console.log(element, elementIndex, container)
                    if (this.isPropertyPanelVisible || (showLog && isLocate)) {
                        this.toggleAsidePropertyPanel({
                            isShow: true,
                            editingElementPos: {
                                stageIndex,
                                containerIndex,
                                containerGroupIndex,
                                elementIndex
                            }
                        })
                    }
                } catch (error) {
                    console.error(error)
                }
            },
            setAtomLocate (row, showLog = false) {
                if (this.isActiveErrorAtom(row) && !showLog) return
                if (this.activeErrorAtom) {
                    this.locateError(this.activeErrorAtom, false, showLog)
                }

                this.locateError(row, true, showLog)
                this.activeErrorAtom = row
            },
            handleExecuteCountChange (executeCount) {
                this.$router.push({
                    ...this.$route,
                    params: {
                        ...this.$route.params,
                        type: this.$route.params.type ?? 'executeDetail',
                        executeCount
                    }
                })
                this.$nextTick(() => {
                    this.requestPipelineExecDetail({
                        ...this.routerParams,
                        executeCount
                    })
                })
            },
            debugDocker ({ container }) {
                const vmSeqId = container.id
                const { projectId, pipelineId, buildNo: buildId } = this.$route.params
                const buildResourceType = container.dispatchType?.buildType
                const buildIdStr = buildId ? `&buildId=${buildId}` : ''

                const tab = window.open('about:blank')
                const url = `${WEB_URL_PREFIX}/pipeline/${projectId}/dockerConsole/?pipelineId=${pipelineId}&dispatchType=${buildResourceType}&vmSeqId=${vmSeqId}${buildIdStr}`
                tab.location = url
            }
        }
    }
</script>

<style lang="scss">
@import "@/scss/conf";
@import "@/scss/mixins/ellipsis";
:root {
  --track-bottom: 0;
}
.exec-pipeline-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.constant-width-num {
    font-family: "Microsoft Yahei";
}
.pipeline-exec-summary {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  .pipeline-exec-count {
    display: flex;
    align-items: center;
    flex-shrink: 0;
    font-size: 12px;
    font-weight: bold;
    .pipeline-exec-count-select {
      width: 88px;
      flex-shrink: 0;
      margin: 0 8px;
    }
    .exec-status-label {
      display: grid;
      align-items: center;
      grid-auto-flow: column;
      grid-gap: 6px;
    }
  }
  .pipeline-exec-timeline {
    width: 666px;
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    top: 11px;
    margin-left: 108px;
    &-item {
      position: relative;
      padding-left: 20px;
      font-size: 12px;

      > span {
        position: relative;
        background: white;
        padding: 0 4px 0 0;
        z-index: 2;
        color: #979ba5;
        display: inline-block;
        margin-bottom: 8px;
      }
      &:before {
        position: absolute;
        content: "";
        left: 0;
        top: 2px;
        width: 9px;
        height: 9px;
        border-radius: 50%;
        background: white;
        border: 2px solid #d8d8d8;
      }

      &:not(:last-child) .time-step-divider {
        display: block;
        position: absolute;
        width: calc(100% - 24px);
        height: 24px;
        top: 0;
        &:hover {
          &:before {
            background: $primaryColor;
          }
        }
        &:before {
          content: "";
          position: absolute;
          height: 1px;
          width: 100%;
          top: 8px;
          background: #d8d8d8;
        }
      }
    }
  }
}
.pipeline-exec-content {
  flex: 1;
  position: relative;
  background: #fafbfd;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  .pipeline-style-setting-header {
    display: flex;
    align-items: center;
    font-size: 12px;
    padding: 16px 24px;
    flex-shrink: 0;
    .hide-skip-pipeline-task {
      padding: 0 16px 0 24px;
      position: relative;
      &:after {
        content: "";
        position: absolute;
        width: 1px;
        height: 16px;
        right: 7px;
        top: 1px;
        background: #dcdee5;
      }
    }
  }
    .exec-pipeline-scroll-box {
        flex: 1;
        .simplebar-wrapper,
        .simplebar-content-wrapper {
            height: 100%;
        }
        .exec-pipeline-ui-wrapper {
            padding: 0 24px 42px 24px;
            height: 100%;
        }
    }
  .exec-errors-popup {
    position: fixed;
    bottom: 0;
    left: 24px;
    right: 34px;
    overflow: hidden;
    will-change: auto;
    max-height: 30vh;
    transition: all 0.5s ease;
    transform: translateY(calc(100% - 42px));
    box-shadow: 0 -2px 20px 0 rgba(0, 0, 0, 0.15);
    background: white;
    z-index: 6;
    .pipeline-exec-error-tab {
      display: flex;
      flex-direction: column;
      height: 100%;
      .bk-tab-section {
        padding: 0;
        position: static;
      }
    }
    .error-popup-table {
        max-height: calc(30vh - 42px);
        display: flex;
        flex-direction: column;
        .bk-table-header-wrapper {
            flex-shrink: 0;
        }
        .bk-table-body-wrapper {
            overflow-y: auto;
        }
    }
    .toggle-error-popup-icon {
        display: flex;
        align-items: center;
        transform-origin: center;
        font-size: 30px;
        width: 30px;
    }

    &.visible {
        transform: translateY(0);
        .toggle-error-popup-icon {
            transition: transform 0.6s ease;
            transform: rotate(180deg);
        }
    }
    .drag-dot {
      position: absolute;
      left: 50%;
      top: 0;
      z-index: 2;
    }
    .pipeline-error-guide-link {
        margin-right: 24px;
        .fix-error-jump {
        display: flex;
        align-items: center;
        color: $primaryColor;
        font-size: 12px;

        .fix-error-jump-icon {
            padding: 0 4px;
        }
        }
    }

    .exec-error-type-cell {
      color: $primaryColor;
      display: grid;
      align-items: center;
      grid-auto-flow: column;
      grid-template-columns: repeat(2, 1fr);
      .exec-error-locate-icon {
        display: flex;
      }
    }
    .build-error-cell {
        display: flex;
        align-items: center;
        width: 100%;
        .build-error-see-more {
            flex-shrink: 0;
            margin-left: 10px;
        }
        .build-error-info {
            @include ellipsis();
            flex: 1;
        }
    }
  }
}
.time-detail-popup,
.queue-time-detail-popup {
  font-size: 12px;
  width: 160px;
  .pipeline-time-detail-sum {
    display: flex;
    > span {
      color: #63656e;
      text-align: left;
      font-weight: 600;
    }

    > span:first-child {
      color: #979ba5;
      font-weight: normal;
      width: 60px;
      flex-shrink: 0;
    }
  }
  &.time-detail-popup .pipeline-time-detail-sum {
    border-bottom: 1px solid #dcdee5;
    padding: 0 0 6px 0;
    margin-bottom: 6px;
  }
  .pipeline-time-detail-sum-list {
    > li {
      display: flex;
      margin-bottom: 8px;
      > span {
        color: #63656e;
        text-align: left;
      }
      > span:first-child {
        color: #979ba5;
        width: 60px;
        flex-shrink: 0;
      }
      &:last-child {
        margin-bottom: 0;
      }
    }
  }
}
.exec-count-select-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  .exec-count-select-option-user {
    color: #979ba5;
  }
}
.pipeline-scrollbar-track {
    left: 24px;
    right: 34px;
    position: fixed;
    bottom: var(--track-bottom);
    height: 10px;
    transition: all 0.3s;
    .simplebar-scrollbar {
        height: 12px;
        &:before {
            background: #a5a5a5;
        }
    }
}
</style>
