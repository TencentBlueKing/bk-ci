<template>
    <section class="pipeline-detail-wrapper"
        v-bkloading="{ isLoading: isLoading || fetchingAtomList }">
        <empty-tips
            v-if="hasNoPermission"
            :show-lock="true"
            :title="noPermissionTipsConfig.title"
            :desc="noPermissionTipsConfig.desc"
            :btns="noPermissionTipsConfig.btns">
        </empty-tips>

        <template v-else-if="execDetail">
            <bk-tab :active="curItemTab" @tab-change="switchTab" class="bkdevops-pipeline-tab-card pipeline-detail-tab-card" type="unborder-card">
                <div slot="setting" class="pipeline-info">
                    <div class="info-item">
                        <span class="item-label">{{ $t('status') }}：</span>
                        <template v-if="execDetail.status === 'CANCELED'">
                            <span v-bk-tooltips.light="`${$t('details.canceller')}：${execDetail.cancelUserId}`" :class="{ [execDetail.status]: execDetail.status }">{{ getStatusLabel(execDetail.status) }}</span>
                        </template>

                        <span v-else :class="{ [execDetail.status]: execDetail.status }">{{ getStatusLabel(execDetail.status) }}</span>
                        <i v-if="showRetryIcon" title="rebuild" class="devops-icon icon-retry" @click.stop="retry(execDetail.id, true)"></i>
                        <logo v-else-if="execDetail.status === 'STAGE_SUCCESS'" :title="$t('details.statusMap.STAGE_SUCCESS')" name="flag" fill="#34d97b" size="16"></logo>
                        <i v-else :title="$t('history.stopBuild')" class="devops-icon icon-stop-shape" @click.stop="stopExecute(execDetail.id)"></i>
                    </div>
                    <div class="info-item">
                        <span class="item-label">{{ $t('details.trigger') }}：</span>
                        <span class="trigger-mode">{{ execDetail.triggerUser || '--' }}</span>
                    </div>
                    <div class="info-item">
                        <span class="item-label">{{ $t('details.executionTime') }}：</span>
                        <span>{{ execDetail.executeTime ? convertMStoStringByRule(execDetail.executeTime) : '--' }}</span>
                    </div>
                </div>
                <bk-tab-panel
                    v-for="panel in panels"
                    v-bind="{ name: panel.name, label: panel.label }"
                    render-directive="if"
                    :key="panel.name"
                >
                    <div :class="panel.className" style="height: 100%">
                        <component :is="panel.component" v-bind="panel.bindData" v-on="panel.listeners"></component>
                    </div>
                </bk-tab-panel>
            </bk-tab>
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
                <plugin :exec-detail="execDetail" :editing-element-pos="editingElementPos" @close="showLog = false" />
            </template>
            <template v-else-if="showContainerPanel">
                <job :exec-detail="execDetail" :editing-element-pos="editingElementPos" @close="showLog = false" />
            </template>
            <template v-else-if="showStagePanel">
                <stage :exec-detail="execDetail" :editing-element-pos="editingElementPos" @close="showLog = false" />
            </template>
            <template v-else-if="showStageReviewPanel.isShow">
                <stage-review-panel :stage="stage" @approve="requestPipelineExecDetail(routerParams)" />
            </template>
        </template>
        <template v-if="execDetail && showCompleteLog">
            <complete-log @close="showLog = false"></complete-log>
        </template>
        <mini-map :stages="execDetail.model.stages" scroll-class=".exec-pipeline" v-if="!isLoading && !fetchingAtomList && curItemTab === 'executeDetail' && !hasNoPermission"></mini-map>
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
        ></check-atom-dialog>
    </section>
</template>

<script>
    import { mapState, mapActions } from 'vuex'
    import webSocketMessage from '@/utils/webSocketMessage'
    import viewPart from '@/components/viewPart'
    import codeRecord from '@/components/codeRecord'
    import outputOption from '@/components/outputOption'
    import StagePropertyPanel from '@/components/StagePropertyPanel'
    import emptyTips from '@/components/devops/emptyTips'
    import completeLog from '@/components/ExecDetail/completeLog.vue'
    import plugin from '@/components/ExecDetail/plugin'
    import job from '@/components/ExecDetail/job'
    import stage from '@/components/ExecDetail/stage'
    import stageReviewPanel from '@/components/StageReviewPanel'
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'
    import pipelineConstMixin from '@/mixins/pipelineConstMixin'
    import { convertMStoStringByRule } from '@/utils/util'
    import Logo from '@/components/Logo'
    import MiniMap from '@/components/MiniMap'
    import AtomPropertyPanel from '@/components/AtomPropertyPanel'
    import CheckAtomDialog from '@/components/CheckAtomDialog'

    export default {
        components: {
            StagePropertyPanel,
            viewPart,
            codeRecord,
            outputOption,
            emptyTips,
            plugin,
            completeLog,
            job,
            stage,
            stageReviewPanel,
            Logo,
            MiniMap,
            AtomPropertyPanel,
            CheckAtomDialog
        },
        mixins: [pipelineOperateMixin, pipelineConstMixin],

        data () {
            return {
                isLoading: true,
                hasNoPermission: false,
                linkUrl: WEB_URL_PREFIX + location.pathname,
                showRetryStageDialog: false,
                retryTaskId: '',
                skipTask: false,
                failedContainer: false,
                isShowCheckDialog: false,
                currentAtom: {},
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
                                this.toApplyPermission(this.$permissionActionMap.execute, {
                                    id: this.routerParams.pipelineId,
                                    type: this.$permissionResourceTypeMap.PIPELINE_DEFAULT
                                })
                            },
                            text: this.$t('applyPermission')
                        }
                    ]
                }
            }
        },

        computed: {
            ...mapState('common', [
                'ruleList',
                'templateRuleList'
            ]),
            ...mapState('atom', [
                'execDetail',
                'editingElementPos',
                'isPropertyPanelVisible',
                'isShowCompleteLog',
                'showPanelType',
                'fetchingAtomList',
                'pipeline',
                'showStageReviewPanel'
            ]),
            ...mapState([
                'fetchError'
            ]),
            userName () {
                return this.$userInfo && this.$userInfo.username ? this.$userInfo.username : ''
            },
            panels () {
                return [{
                    name: 'executeDetail',
                    label: this.$t('details.executeDetail'),
                    component: 'bk-pipeline',
                    className: 'exec-pipeline',
                    bindData: {
                        editable: false,
                        isExecDetail: true,
                        userName: this.userName,
                        cancelUserId: this.execDetail && this.execDetail.cancelUserId,
                        pipeline: this.execDetail && this.execDetail.model,
                        matchRules: this.curMatchRules
                    },
                    listeners: {
                        click: this.handlePiplineClick,
                        'stage-check': this.handleStageCheck,
                        'stage-retry': this.handleRetry,
                        'atom-quality-check': this.qualityCheck,
                        'atom-review': this.reviewAtom,
                        'atom-continue': this.handleContinue,
                        'atom-exec': this.handleExec
                    }
                }, {
                    name: 'partView',
                    label: this.$t('details.partView'),
                    className: '',
                    component: 'view-part',
                    bindData: {}
                }, {
                    name: 'codeRecords',
                    label: this.$t('details.codeRecords'),
                    className: '',
                    component: 'code-record',
                    bindData: {}
                }, {
                    name: 'output',
                    label: this.$t('details.outputReport'),
                    className: '',
                    component: 'output-option',
                    bindData: {
                        curPipeline: this.execDetail && this.execDetail.model
                    }
                }]
            },
            showLog: {
                get () {
                    const { editingElementPos, isPropertyPanelVisible, $route: { params } } = this
                    return typeof editingElementPos.elementIndex !== 'undefined' && params.buildNo && isPropertyPanelVisible
                },
                set (value) {
                    this.togglePropertyPanel({
                        isShow: value
                    })
                }
            },
            showCompleteLog () {
                const { isShowCompleteLog, $route: { params } } = this
                return isShowCompleteLog && params.buildNo
            },
            showStagePanel () {
                return typeof this.editingElementPos.stageIndex !== 'undefined' && this.isPropertyPanelVisible
            },
            showContainerPanel () {
                const { editingElementPos, isPropertyPanelVisible } = this
                return typeof editingElementPos.containerIndex !== 'undefined' && isPropertyPanelVisible
            },
            stage () {
                const { editingElementPos, execDetail } = this
                if (editingElementPos) {
                    const model = execDetail.model || {}
                    const stages = model.stages || []
                    const stage = stages[editingElementPos.stageIndex]
                    return stage
                }
                return null
            },
            getElementViewName () {
                try {
                    const {
                        editingElementPos: { stageIndex, containerIndex, elementIndex },
                        execDetail: { model: { stages } }
                    } = this
                    const element = stages[stageIndex].containers[containerIndex].elements[elementIndex]

                    return `${element.name} T${stageIndex + 1}-${containerIndex + 1}-${elementIndex + 1}`
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
                return this.$route.params
            },
            curItemTab () {
                return this.routerParams.type || 'executeDetail'
            },
            showRetryIcon () {
                return this.execDetail && ['RUNNING', 'QUEUE', 'STAGE_SUCCESS'].indexOf(this.execDetail.status) < 0
            },
            isInstanceEditable () {
                return this.execDetail?.model?.instanceFromTemplate
            },
            curMatchRules () {
                return this.$route.path.indexOf('template') > 0 ? this.templateRuleList : this.isInstanceEditable ? this.templateRuleList.concat(this.ruleList) : this.ruleList
            }
        },

        watch: {
            execDetail (val) {
                console.log(val, 'execDetailexecDetail')
                this.isLoading = val === null
            },
            'routerParams.buildNo': {
                handler (val, oldVal) {
                    if (val !== oldVal) {
                        this.requestPipelineExecDetail(this.routerParams)
                    }
                }
            },
            fetchError (error) {
                if (error.code === 403) {
                    this.isLoading = false
                    this.hasNoPermission = true
                }
            }
        },

        mounted () {
            this.requestPipelineExecDetail(this.routerParams)
            this.$store.dispatch('common/requestInterceptAtom', {
                projectId: this.routerParams.projectId,
                pipelineId: this.routerParams.pipelineId
            })
            webSocketMessage.installWsMessage(this.setPipelineDetail)

            // 第三方系统、通知等，点击链接进入流水线执行详情页面时，定位到具体的 task/ job (自动打开对应的侧滑框)
            const { stageIndex, elementIndex, containerGroupIndex, containerIndex } = this.$route.query
            if (stageIndex) {
                this.togglePropertyPanel({
                    isShow: true,
                    editingElementPos: {
                        containerGroupIndex,
                        containerIndex,
                        elementIndex,
                        stageIndex
                    }
                })
            }
        },

        beforeDestroy () {
            this.setPipelineDetail(null)
            this.togglePropertyPanel({
                isShow: false
            })
            webSocketMessage.unInstallWsMessage()
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
            convertMStoStringByRule,
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
            toggleCheckDialog (isShow = false) {
                this.isShowCheckDialog = isShow
                if (!isShow) {
                    this.currentAtom = {}
                }
            },
            async reviewAtom (atom) {
                // 人工审核
                this.currentAtom = atom
                this.toggleCheckDialog(true)
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
            handleRetry ({ taskId, skip = false }) {
                this.showRetryStageDialog = true
                this.retryTaskId = taskId
                this.skipTask = skip
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
            switchTab (tabType = 'executeDetail') {
                this.$router.push({
                    name: 'pipelinesDetail',
                    params: {
                        ...this.routerParams,
                        type: tabType
                    }
                })
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';
    @import './../../scss/pipelineStatus';
    .pipeline-detail-wrapper {
        height: 100%;
        padding: 7px 25px 0 25px;

        .pipeline-detail-tab-card {
            height: 100%;
            display: flex;
            flex-direction: column;
            .bk-tab-content {
                height: calc(100% - 25px);
                overflow: auto;
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
                .icon-retry {
                    font-size: 20px;
                    color: $primaryColor;
                    cursor: pointer;
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
