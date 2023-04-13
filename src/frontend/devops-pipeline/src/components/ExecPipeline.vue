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
                    {{ $t("details.times") }}，{{ statusLabel }}
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
                    <p>
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
                <bk-button text theme="primary" @click="toggleCompleteLog">
                    <i class="devops-icon icon-txt"></i>
                    {{ $t("history.viewLog") }}
                </bk-button>
            </header>
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
                />
            </div>
            <footer
                v-if="showErrorPopup"
                :class="{
                    'exec-errors-popup': true,
                    visible: showErrors
                }"
                v-bk-clickoutside="hideErrorPopup"
            >
                <bk-button theme="normal" text class="drag-dot" @click="toggleErrorPopup"
                >.....</bk-button
                >
                <bk-tab
                    class="pipeline-exec-error-tab"
                    :active="activeTab"
                    :label-height="42"
                    type="unborder-card"
                >
                    <template slot="setting">
                        <bk-link theme="primary" :href="pipelineErrorGuideLink" target="_blank">
                            <span class="fix-error-jump">
                                <logo class="fix-error-jump-icon" size="20" name="tiaozhuan" />
                                {{ $t("details.pipelineErrorGuide") }}
                            </span>
                        </bk-link>
                        <bk-button theme="normal" text @click="toggleErrorPopup">
                            <i
                                class="bk-icon hide-error-popup-icon"
                                :class="{
                                    'icon-angle-down': showErrors,
                                    'icon-angle-up': !showErrors
                                }"
                            />
                        </bk-button>
                    </template>
                    <bk-tab-panel v-for="(panel, index) in panels" v-bind="panel" :key="index">
                        <template slot="label">
                            <span @click="setShowErrorPopup" class="panel-name pointer">{{panel.label}}</span>
                        </template>
                        <bk-table
                            :data="errorList"
                            :border="false"
                            @row-click="setAtomLocate"
                            highlight-current-row
                        >
                            <bk-table-column width="80">
                                <div slot-scope="props" class="exec-error-type-cell">
                                    <span class="exec-error-locate-icon">
                                        <Logo
                                            v-if="
                                                activeErrorAtom && activeErrorAtom.taskId === props.row.taskId
                                            "
                                            name="location-right"
                                            size="18"
                                        />
                                    </span>
                                    <logo
                                        v-if="props.row.errorTypeConf"
                                        :name="props.row.errorTypeConf.icon"
                                        size="12"
                                    />
                                </div>
                            </bk-table-column>
                            <bk-table-column
                                v-for="(column, i) in errorsTableColumns"
                                v-bind="column"
                                :key="i"
                            />
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
                <span>{{ isRunning ? `${$t("details.statusMap.RUNNING")}...` : queueCost }}</span>
            </div>
        </div>
        <div class="time-detail-popup">
            <div class="pipeline-time-detail-sum">
                <span>{{ $t("details.totalCost") }}</span>
                <span>{{ isRunning ? `${$t("details.statusMap.RUNNING")}...` : totalCost }}</span>
            </div>
            <ul class="pipeline-time-detail-sum-list">
                <li v-for="cost in timeDetailRows" :key="cost.field">
                    <span>{{ cost.label }}</span>
                    <span>{{ cost.value }}</span>
                </li>
            </ul>
        </div>
        <template v-if="execDetail && showLog">
            <complete-log
                @close="toggleCompleteLog"
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
    import { mapActions, mapState } from 'vuex'
    export default {
        components: {
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
                activeTab: '-1',
                currentAtom: {},
                pipelineMode: 'uiMode',
                showErrors: false,
                activeErrorAtom: null,
                pipelineErrorGuideLink:
                    '//bk.tencent.com/docs/markdown/持续集成平台/产品白皮书/FAQS/FAQ.md'
            }
        },
        computed: {
            ...mapState('common', ['ruleList', 'templateRuleList']),
            ...mapState('atom', ['hideSkipExecTask']),
            curPipeline () {
                return this.execDetail?.model
            },
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
                return this.execDetail?.errorInfoList?.map((error) => ({
                    ...error,
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
                return this.execDetail?.model?.timeCost?.queueCost
                ? convertMillSec(this.execDetail.model.timeCost.queueCost)
                : '--'
            },
            totalCost () {
                return this.execDetail?.model?.timeCost?.totalCost
                ? convertMillSec(this.execDetail.model.timeCost.totalCost)
                : '--'
            },
            errorsTableColumns () {
                return [
                    {
                        label: this.$t('details.pipelineErrorType'),
                        prop: 'errorType',
                        width: 150
                    },
                    {
                        label: this.$t('details.pipelineErrorCode'),
                        prop: 'errorCode',
                        width: 150
                    },
                    {
                        label: this.$t('details.pipelineErrorPos'),
                        prop: 'taskName',
                        width: 200
                    },
                    {
                        label: this.$t('details.pipelineErrorInfo'),
                        prop: 'errorMsg'
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
            }
        },
        mounted () {
            this.requestInterceptAtom(this.routerParams)
            if (this.errorList?.length > 0) {
                this.setAtomLocate(this.errorList[0])
                this.setShowErrorPopup()
            }
        },
        beforeDestroy () {
            this.togglePropertyPanel({
                isShow: false
            })
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
            isSkip (status) {
                return ['SKIP'].includes(status)
            },
            toggleCompleteLog () {
                this.showLog = !this.showLog
            },
            toggleErrorPopup () {
                this.showErrors = !this.showErrors
            },
            setShowErrorPopup () {
                this.showErrors = true
            },
            hideErrorPopup () {
                this.showErrors = false
            },
            handlePiplineClick (args) {
                this.togglePropertyPanel({
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
            async qualityCheck ({ elementId, action }, done) {
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
                } finally {
                    done()
                }
            },
            handleRetry ({ taskId, skip = false }) {
                this.showRetryStageDialog = true
                this.retryTaskId = taskId
                this.skipTask = skip
            },
            async reviewAtom (atom) {
                // 人工审核
                this.currentAtom = atom
                this.toggleCheckDialog(true)
            },
            toggleCheckDialog (isShow = false) {
                this.isShowCheckDialog = isShow
                if (!isShow) {
                    this.currentAtom = {}
                }
            },
            async handleContinue ({ taskId, skip = false }, done) {
                this.retryTaskId = taskId
                this.skipTask = skip
                await this.retryPipeline()
                done()
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
                        this.requestPipelineExecDetail(this.routerParams)
                    } catch (err) {
                        this.$showTips({
                            message: err.message || err,
                            theme: 'error'
                        })
                    } finally {
                        done()
                    }
                } else {
                    this.togglePropertyPanel({
                        isShow: true,
                        showPanelType,
                        editingElementPos: {
                            stageIndex,
                            containerIndex,
                            containerGroupIndex,
                            elementIndex
                        }
                    })
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
                        message = this.$t('subpage.retrySuc')
                        theme = 'success'
                        res?.executeCount && this.handleExecuteCountChange(res.executeCount)
                    } else {
                        message = this.$t('subpage.retryFail')
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
            async locateAtom (row, isLocate = true) {
                try {
                    const { stageId, containerId, taskId, matrixFlag } = row
                    const stage = this.curPipeline.stages.find((stage) => stage.id === stageId)
                    const numContainerId = parseInt(containerId, 10)
                    const matrixId = Math.floor(numContainerId / 1000)
                    let container
                    if (matrixFlag) {
                        container = stage.containers
                            .filter((item) => Array.isArray(item.groupContainers))
                            .map((item) => item.groupContainers)
                            .flat()
                            .find((item) => item.id === containerId)
                        console.log(container)
                    } else {
                        container = stage.containers.find((item) => item.id === containerId)
                    }
                    await this.$refs.bkPipeline.expandMatrix(stageId, matrixId, containerId)
                    const element = container.elements.find((element) => element.id === taskId)
                    this.$set(element, 'locateActive', isLocate)

                    console.log(element, taskId, this.$store.state.atom.execDetail.model.stages[1])
                } catch (e) {
                    console.log(e)
                }
            },
            setAtomLocate (row) {
                if (this.activeErrorAtom?.taskId === row.taskId) return
                if (this.activeErrorAtom?.taskId) {
                    this.locateAtom(this.activeErrorAtom, false)
                }
                this.hideErrorPopup()
                this.locateAtom(row)
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
            }
        }
    }
</script>

<style lang="scss">
@import "@/scss/conf";
.exec-pipeline-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
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
  .exec-pipeline-ui-wrapper {
    flex: 1;
    overflow: auto;
    padding: 0 24px 42px 24px;
  }
  .exec-errors-popup {
    width: 100%;
    position: fixed;
    bottom: 0;
    left: 0;
    overflow: hidden;
    will-change: auto;
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
    &.visible {
      transform: translateY(0);
    }
    .drag-dot {
      position: absolute;
      left: 50%;
      top: 0;
      z-index: 2;
    }
    .fix-error-jump {
      display: flex;
      align-items: center;
      color: $primaryColor;
      font-size: 12px;
      .fix-error-jump-icon {
        padding: 0 4px;
      }
    }
    .hide-error-popup-icon {
      display: inline-block;
      font-size: 20px;
      margin-right: 24px;
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
      flex-shrik: 0;
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
        flex-shrik: 0;
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
</style>
