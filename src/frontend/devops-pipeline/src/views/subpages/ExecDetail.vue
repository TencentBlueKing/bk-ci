<template>
    <section
        class="pipeline-detail-wrapper"
        v-bkloading="{ isLoading: isLoading || fetchingAtomList }"
    >
        <empty-tips
            v-if="hasNoPermission"
            :show-lock="true"
            :title="noPermissionTipsConfig.title"
            :desc="noPermissionTipsConfig.desc"
            :btns="noPermissionTipsConfig.btns"
        >
        </empty-tips>

        <template v-else-if="execDetail">
            <Summary :exec-detail="execDetail"></Summary>
            <main class="exec-detail-main">
                <bk-tab
                    :active="curItemTab"
                    @tab-change="switchTab"
                    :label-height="42"
                    class="pipeline-detail-tab-card"
                    type="unborder-card"
                >
                    <bk-tab-panel
                        v-for="panel in panels"
                        v-bind="{ name: panel.name, label: panel.label }"
                        render-directive="if"
                        :key="panel.name"
                    >
                        <div :class="panel.className" style="height: 100%">
                            <component
                                :is="panel.component"
                                v-bind="panel.bindData"
                                v-on="panel.listeners"
                            ></component>
                        </div>
                    </bk-tab-panel>
                </bk-tab>
            </main>
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
                    @close="hideSidePanel"
                />
            </template>
            <template v-else-if="showContainerPanel">
                <job
                    :exec-detail="execDetail"
                    :editing-element-pos="editingElementPos"
                    @close="hideSidePanel"
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
                    @approve="requestPipelineExecDetail(routerParams)"
                />
            </template>
        </template>
    </section>
</template>

<script>
    import { mapState, mapActions } from 'vuex'
    import webSocketMessage from '@/utils/webSocketMessage'
    import codeRecord from '@/components/codeRecord'
    import StartParams from '@/components/StartParams'
    import ExecPipeline from '@/components/ExecPipeline'
    import Outputs from '@/components/Outputs'
    import StagePropertyPanel from '@/components/StagePropertyPanel'
    import emptyTips from '@/components/devops/emptyTips'
    import plugin from '@/components/ExecDetail/plugin'
    import job from '@/components/ExecDetail/job'
    import Summary from '@/components/ExecDetail/Summary'
    import stage from '@/components/ExecDetail/stage'
    import stageReviewPanel from '@/components/StageReviewPanel'
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'
    import pipelineConstMixin from '@/mixins/pipelineConstMixin'
    import Logo from '@/components/Logo'
    import AtomPropertyPanel from '@/components/AtomPropertyPanel'

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
            Summary
        },
        mixins: [pipelineOperateMixin, pipelineConstMixin],

        data () {
            return {
                isLoading: true,
                hasNoPermission: false,
                linkUrl: WEB_URL_PREFIX + location.pathname,
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
            ...mapState(['fetchError']),
            panels () {
                return [
                    {
                        name: 'executeDetail',
                        label: this.$t('details.executeDetail'),
                        component: 'exec-pipeline',
                        className: 'exec-pipeline',
                        bindData: {
                            execDetail: this.execDetail,
                            matchRules: this.curMatchRules
                        }
                    },
                    {
                        name: 'outputs',
                        label: this.$t('details.outputs'),
                        className: '',
                        component: 'outputs',
                        bindData: {}
                    },
                    {
                        name: 'codeRecords',
                        label: this.$t('details.codeRecords'),
                        className: '',
                        component: 'code-record',
                        bindData: {}
                    },
                    {
                        name: 'startupParams',
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
                    const model = execDetail?.model ?? {}
                    const stages = model.stages ?? []
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
                return this.$route.params
            },
            curItemTab () {
                return this.routerParams.type || 'executeDetail'
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
            webSocketMessage.installWsMessage(this.setPipelineDetail)

            // 第三方系统、通知等，点击链接进入流水线执行详情页面时，定位到具体的 task/ job (自动打开对应的侧滑框)
            const {
                stageIndex,
                elementIndex,
                containerGroupIndex,
                containerIndex
            } = this.$route.query
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
            ...mapActions('common', ['requestInterceptAtom']),
            hideSidePanel () {
                this.showLog = false
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
@import "@/scss/conf";
@import "@/scss/pipelineStatus";
@import "@/scss/detail-tab.scss";
.pipeline-detail-wrapper.biz-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-top: 1px solid #dde4eb;
  overflow: auto;

  .exec-detail-main {
    margin: 16px 24px 0 24px;
    background: #f5f7fa;
    flex: 1;
  }
  .pipeline-detail-tab-card {
    height: 100%;
    display: flex;
    flex-direction: column;
    background: white;
    .bk-tab-section {
      position: relative;
      flex: 1;
      padding: 0;
      overflow: auto;
    }
    .bk-tab-content {
      height: 100%;
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
