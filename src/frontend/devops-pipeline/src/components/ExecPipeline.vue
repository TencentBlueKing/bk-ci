<template>
    <div class="exec-pipeline-wrapper">
        <div class="pipeline-exec-summary">
            <div class="pipeline-exec-count">
                <span>{{$t('details.num')}}</span>
                <bk-select ext-cls="pipeline-exec-count-select" :list="executeCounts"></bk-select>
                <span>{{$t('details.times')}}，{{statusLabel}}</span>
            </div>
            <ul class="pipeline-exec-timeline">
                <li class="pipeline-exec-timeline-item" v-for="step in timeSteps" :key="step.title">
                    <span>
                        {{step.title}}
                    </span>
                    <p v-bk-tooltips="timeDetailConf" class="time-step-divider"></p>
                    <p>
                        {{step.description}}
                    </p>
                </li>
            </ul>
        </div>
        <section class="pipeline-exec-content">
            <header class="pipeline-style-setting-header">
                <div class="bk-button-group">
                    <bk-button
                        v-for="item in pipelineModes"
                        :key="item.id"
                        :class="item.cls"
                    >
                        {{item.label}}
                    </bk-button>
                </div>
                <bk-checkbox
                    :true-value="true"
                    :false-value="false"
                    v-model="hideSkipTask"
                    ext-cls="hide-skip-pipeline-task"
                >
                    {{$t('details.hideSkipStep')}}
                </bk-checkbox>
                <bk-button text theme="primary" @click="toggleCompleteLog">
                    <i class="devops-icon icon-txt"></i>
                    {{$t('history.viewLog')}}
                </bk-button>
            </header>
            <bk-pipeline
                :editable="false"
                is-exec-detail
                :hide-skip-task="hideSkipTask"
                :cancel-user-id="cancelUserId"
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
                <bk-radio :value="false">{{ $t('editPage.retryAllJobs') }}</bk-radio>
                <bk-radio :value="true">{{ $t('editPage.retryFailJobs') }}</bk-radio>
            </bk-radio-group>
        </bk-dialog>
        <check-atom-dialog
            :is-show-check-dialog="isShowCheckDialog"
            :toggle-check="toggleCheckDialog"
            :element="currentAtom"
        />
        <footer
            :class="{
                'exec-errors-popup': true,
                'visible': showErrors
            }"
            v-bk-clickoutside="hideErrorPopup"
        >
            <bk-button theme="normal" text class="drag-dot" @click="toggleErrorPopup">.....</bk-button>
            <bk-tab class="pipeline-exec-error-tab" :active.sync="active" type="unborder-card">
                <template slot="setting">
                    <bk-link theme="primary" href="javascript:;">
                        <span class="fix-error-jump">
                            <logo class="fix-error-jump-icon" size="20" name="tiaozhuan" />
                            {{$t('流水线故障排查指南')}}
                        </span>
                    </bk-link>
                    <bk-button theme="normal" text @click="toggleErrorPopup">
                        <i class="bk-icon  hide-error-popup-icon" :class="{
                            'icon-angle-down': showErrors,
                            'icon-angle-up': !showErrors
                        }"
                        />
                    </bk-button>
                </template>
                <bk-tab-panel
                    v-for="(panel, index) in panels"
                    v-bind="panel"
                    :key="index"
                >
                    <bk-table
                        :data="errorList"
                        :border="false"
                        @row-click="setAtomLocate"
                    >
                        <bk-table-column width="80">
                            <div slot-scope="props" class="exec-error-type-cell">
                                <span class="exec-error-locate-icon">
                                    <Logo
                                        v-if="activeErrorAtom && activeErrorAtom.taskId === props.row.taskId"
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
        <template v-if="execDetail && showLog">
            <complete-log @close="toggleCompleteLog"></complete-log>
        </template>
    </div>
</template>

<script>
    import { mapState, mapActions } from 'vuex'
    import { convertTime, convertMStoString } from '@/utils/util'
    import CheckAtomDialog from '@/components/CheckAtomDialog'
    import CompleteLog from '@/components/ExecDetail/completeLog'
    import { errorTypeMap } from '@/utils/pipelineConst'
    import Logo from '@/components/Logo'
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
            console.log(this)
            return {
                showRetryStageDialog: false,
                showLog: false,
                retryTaskId: '',
                skipTask: false,
                failedContainer: false,
                currentAtom: {},
                pipelineMode: 'uiMode',
                hideSkipTask: false,
                showErrors: false,
                activeErrorAtom: null
            }
        },
        computed: {
            ...mapState('common', [
                'ruleList',
                'templateRuleList'
            ]),
            panels () {
                return [
                    {
                        name: 'errors',
                        label: this.$t('Errors')
                    }
                ]
            },
            timeDetailConf () {
                return {
                    allowHtml: true,
                    theme: 'light',
                    content: '.time-detail-popup',
                    placement: 'bottom'
                }
            },
            errorList () {
                return this.execDetail?.errorInfoList?.map((error) => ({
                    ...error,
                    errorTypeConf: errorTypeMap[error.errorType]
                }))
            },
            timeDetailRows () {
                console.log(Object.keys(this.execDetail?.timeCost ?? {}).map(key => ({
                    field: key,
                    label: this.$t(`details.${key}`),
                    value: convertMStoString(this.execDetail?.timeCost[key])
                })))
                return Object.keys(this.execDetail?.timeCost ?? {}).map(key => ({
                    field: key,
                    label: this.$t(`details.${key}`),
                    value: convertMStoString(this.execDetail?.timeCost[key])
                }))
            },
            totalCost () {
                return convertMStoString(this.execDetail.endTime - this.execDetail.startTime)
            },
            errorsTableColumns () {
                return [
                    {
                        label: this.$t('错误类型'),
                        prop: 'errorType'
                    },
                    {
                        label: this.$t('错误码'),
                        prop: 'errorCode'
                    },
                    {
                        label: this.$t('错误位置'),
                        prop: 'taskId'
                    },
                    {
                        label: this.$t('错误信息'),
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
                return this.$route.path.indexOf('template') > 0 ? this.templateRuleList : this.isInstanceEditable ? this.templateRuleList.concat(this.ruleList) : this.ruleList
            },
            statusLabel () {
                return this.execDetail?.status ? this.$t(`details.statusMap.${this.execDetail?.status}`) : ''
            },
            cancelUserId () {
                return this.execDetail?.cancelUserId ?? '--'
            },
            curPipeline () {
                console.log(this.execDetail)
                return this.execDetail?.model ?? null
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
                        description: convertTime(this.execDetail?.startTime)
                    },
                    {
                        title: this.$t('details.startTime'),
                        description: convertTime(this.execDetail?.startTime)
                    },
                    {
                        title: this.$t('details.endTime'),
                        description: convertTime(this.execDetail?.endTime)
                    }
                ]
            },
            routerParams () {
                return this.$route.params
            }
        },
        mounted () {
            this.requestInterceptAtom(this.routerParams)
        },
        beforeDestroy () {
            this.togglePropertyPanel({
                isShow: false
            })
        },
        methods: {
            ...mapActions('atom', [
                'reviewExcuteAtom',
                'togglePropertyPanel',
                'toggleStageReviewPanel',
                'requestPipelineExecDetail',
                'setPipelineDetail',
                'getInitLog',
                'getAfterLog',
                'pausePlugin'
            ]),
            ...mapActions('common', [
                'requestInterceptAtom'
            ]),
            ...mapActions('pipelines', [
                'requestRetryPipeline'
            ]),
            toggleCompleteLog () {
                this.showLog = !this.showLog
            },
            toggleErrorPopup () {
                this.showErrors = !this.showErrors
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
            async handleExec ({
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
            }, done) {
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
                    } else {
                        message = this.$t('subpage.retryFail')
                        theme = 'error'
                    }
                } catch (err) {
                    this.handleError(err, [{
                        actionId: this.$permissionActionMap.execute,
                        resourceId: this.$permissionResourceMap.pipeline,
                        instanceId: [{
                            id: this.routerParams.pipelineId,
                            name: this.routerParams.pipelineId
                        }],
                        projectId: this.routerParams.projectId
                    }])
                } finally {
                    message && this.$showTips({
                        message,
                        theme
                    })
                    this.retryTaskId = ''
                    this.skipTask = false
                }
            },
            locateAtom (row, isLocate = true) {
                try {
                    const { stageId, jobId, taskId } = row
                    const stage = this.curPipeline.stages.find(stage => stage.id === stageId)
                    const container = stage.containers.find(container => container.id === jobId)
                    const element = container.elements.find(element => element.id === taskId)
                    this.$set(element, 'locateActive', isLocate)
                } catch (e) {
                    console.log(e)
                }
            },
            setAtomLocate (row) {
                if (this.activeErrorAtom?.taskId) {
                    this.locateAtom(this.activeErrorAtom, false)
                }
                this.locateAtom(row, true)
                this.activeErrorAtom = row
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf';
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
                width: 120px;
                flex-shrink: 0;
                margin: 0 8px;
            }
        }
        .pipeline-exec-timeline {
            flex: 1;
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            top: 11px;
            margin-left: 108px;
            &-item {
                position:relative;
                padding-left: 20px;
                font-size: 12px;

                > span {
                    position: relative;
                    background: white;
                    padding: 0 4px;
                    z-index: 2;
                    color: #979BA5;
                    display: inline-block;
                    margin-bottom: 8px;
                }
                &:before {
                    position: absolute;
                    content: '';
                    left: 0;
                    top: 2px;
                    width: 9px;
                    height: 9px;
                    border-radius: 50%;
                    background: white;
                    border: 2px solid #D8D8D8;
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
                        content: '';
                        position: absolute;
                        height: 1px;
                        width: 100%;
                        top: 8px;
                        background: #D8D8D8;
                    }
                }
            }
        }
    }
    .pipeline-exec-content {
        padding: 16px 24px;
        flex: 1;
        background: #FAFBFD;
        .pipeline-style-setting-header {
            display: flex;
            align-items: center;
            font-size: 12px;
            margin-bottom: 16px;
            .hide-skip-pipeline-task {
                padding: 0 16px 0 24px;
                position: relative;
                &:after {
                    content: '';
                    position: absolute;
                    width: 1px;
                    height: 16px;
                    right: 7px;
                    top: 1px;
                    background: #DCDEE5;
                }
            }
        }
    }
    .exec-errors-popup {
        width: 100%;
        height: 42px;
        position: absolute;
        bottom: 0;
        left: 0;
        overflow: hidden;
        transition: height .5s ease;
        box-shadow: 0 -2px 20px 0 rgba(0,0,0,0.15);
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
            height: 200px;
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
    .time-detail-popup {
        font-size: 12px;
        width: 160px;
        .pipeline-time-detail-sum {
            display: flex;
            justify-content: space-between;
            font-weight: bold;
            >span:first-child {
                color: #979BA5;
                font-weight: normal;
            }
        }
        .pipeline-time-detail-sum-list {
            > li {
                display: flex;
                justify-content: space-between;
                margin-bottom: 8px;
                >span:first-child {
                    color: #979BA5;
                }
                &:last-child {
                    margin-bottom: 0;
                }
            }
        }
    }
</style>
