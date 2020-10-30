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
                        <span class="item-label">{{ $t('details.executor') }}：</span>
                        <span class="trigger-mode">{{ execDetail.userId || '--' }}</span>
                    </div>
                    <div class="info-item">
                        <span class="item-label">{{ $t('details.executionTime') }}：</span>
                        <span>{{ execDetail.endTime ? convertMStoStringByRule(execDetail.endTime - execDetail.startTime) : '--' }}</span>
                    </div>
                </div>
                <bk-tab-panel
                    v-for="panel in panels"
                    v-bind="{ name: panel.name, label: panel.label }"
                    render-directive="if"
                    :key="panel.name"
                >
                    <div :class="panel.className" style="height: 100%">
                        <component :is="panel.component" v-bind="panel.bindData"></component>
                    </div>
                </bk-tab-panel>
            </bk-tab>
        </template>
        <template v-if="editingElementPos && execDetail">
            <template v-if="showLog">
                <plugin @close="showLog = false" />
            </template>
            <template v-else-if="showContainerPanel">
                <job @close="showLog = false" />
            </template>
            <template v-else-if="showStagePanel">
                <stage @close="showLog = false" />
            </template>
            <template v-else-if="showStageReviewPanel">
                <stage-review @close="showLog = false" />
            </template>
        </template>
        <template v-if="execDetail && showCompleteLog">
            <complete-log @close="showLog = false"></complete-log>
        </template>
        <mini-map :stages="execDetail.model.stages" scroll-class=".exec-pipeline" v-if="!isLoading && !fetchingAtomList && curItemTab === 'executeDetail' && !hasNoPermission"></mini-map>
    </section>
</template>

<script>
    import { mapState, mapActions } from 'vuex'
    import webSocketMessage from '@/utils/webSocketMessage'
    import stages from '@/components/Stages'
    import viewPart from '@/components/viewPart'
    import codeRecord from '@/components/codeRecord'
    import outputOption from '@/components/outputOption'
    import StagePropertyPanel from '@/components/StagePropertyPanel'
    import emptyTips from '@/components/devops/emptyTips'
    import completeLog from '@/components/ExecDetail/completeLog.vue'
    import plugin from '@/components/ExecDetail/plugin'
    import job from '@/components/ExecDetail/job'
    import stage from '@/components/ExecDetail/stage'
    import stageReview from '@/components/ExecDetail/stageReview'
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'
    import pipelineConstMixin from '@/mixins/pipelineConstMixin'
    import { convertMStoStringByRule } from '@/utils/util'
    import Logo from '@/components/Logo'
    import MiniMap from '@/components/MiniMap'

    export default {
        components: {
            stages,
            StagePropertyPanel,
            viewPart,
            codeRecord,
            outputOption,
            emptyTips,
            plugin,
            completeLog,
            job,
            stage,
            stageReview,
            Logo,
            MiniMap
        },
        mixins: [pipelineOperateMixin, pipelineConstMixin],

        data () {
            return {
                isLoading: true,
                hasNoPermission: false,
                linkUrl: WEB_URL_PIRFIX + location.pathname,
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
                                    name: this.routerParams.pipelineId
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
                'execDetail',
                'editingElementPos',
                'isPropertyPanelVisible',
                'isShowCompleteLog',
                'fetchingAtomList',
                'showStageReviewPanel'
            ]),
            ...mapState([
                'fetchError'
            ]),

            panels () {
                return [{
                    name: 'executeDetail',
                    label: this.$t('details.executeDetail'),
                    component: 'stages',
                    className: 'exec-pipeline',
                    bindData: {
                        editable: false,
                        isExecDetail: true,
                        stages: this.execDetail && this.execDetail.model && this.execDetail.model.stages
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
            currentJob () {
                const { editingElementPos, execDetail } = this
                const model = execDetail.model || {}
                const stages = model.stages || []
                const currentStage = stages[editingElementPos.stageIndex] || []
                return currentStage.containers[editingElementPos.containerIndex]
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
            currentElement () {
                const {
                    editingElementPos: { stageIndex, containerIndex, elementIndex },
                    execDetail: { model: { stages } }
                } = this
                return stages[stageIndex].containers[containerIndex].elements[elementIndex]
            },
            getElementId () {
                const {
                    editingElementPos: { stageIndex, containerIndex, elementIndex },
                    execDetail: { model: { stages } }
                } = this
                return stages[stageIndex].containers[containerIndex].elements[elementIndex].id || ''
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
            getExecuteCount () {
                const {
                    editingElementPos: { stageIndex, containerIndex, elementIndex },
                    execDetail: { model: { stages } }
                } = this
                const element = stages[stageIndex].containers[containerIndex].elements[elementIndex]
                if (element !== undefined) {
                    return element.executeCount || 1
                }
                return 1
            },
            sidePanelConfig () {
                return this.showLog ? {
                    title: `${this.getElementViewName || this.$t('history.viewLog')}`,
                    width: 820
                } : {
                    title: this.$t('propertyBar'),
                    class: 'bkci-property-panel',
                    width: 640
                }
            },
            buildNum () {
                const { execDetail } = this
                return execDetail && execDetail.buildNum ? execDetail.buildNum : ''
            },
            routerParams () {
                return this.$route.params
            },
            curItemTab () {
                return this.routerParams.type || 'executeDetail'
            },
            showRetryIcon () {
                return this.execDetail && ['RUNNING', 'QUEUE', 'STAGE_SUCCESS'].indexOf(this.execDetail.status) < 0
            }
        },

        watch: {
            execDetail (val) {
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
            this.$store.dispatch('soda/requestInterceptAtom', {
                projectId: this.routerParams.projectId,
                pipelineId: this.routerParams.pipelineId
            })
            webSocketMessage.installWsMessage(this.setPipelineDetail)
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
                'updateAtom',
                'togglePropertyPanel',
                'requestPipelineExecDetail',
                'setPipelineDetail',
                'getInitLog',
                'getAfterLog'
            ]),
            ...mapActions('soda', [
                'requestInterceptAtom'
            ]),
            convertMStoStringByRule,
            switchTab (tabType = 'executeDetail') {
                this.$router.push({
                    name: 'pipelinesDetail',
                    params: {
                        ...this.$route.params,
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
            .bk-tab-section {
                height: calc(100% - 52px);
            }
        }
        .exec-pipeline {
            position: relative;
            overflow: auto;
            height: 100%;
            /deep/ .devops-stage-list {
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
            color: $fontLigtherColor;
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
