<template>
    <section
        class="pipeline-detail-wrapper"
        @scroll="handlerScroll"
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
            <div class="exec-detail-summary-header">
                <span
                    :class="{
                        'exec-detail-build-summary-anchor': true,
                        [execDetail.status]: execDetail.status
                    }"
                ></span>
                <aside class="exec-detail-summary-header-title">
                    <bk-tag class="exec-status-tag" type="stroke" :theme="statusTagTheme">
                        <span class="exec-status-label">
                            {{ statusLabel }}
                            <span
                                v-if="execDetail.status === 'CANCELED'"
                                v-bk-tooltips="`${$t('details.canceller')}：${execDetail.cancelUserId || '--'}`"
                                class="devops-icon icon-info-circle"
                            >
                            </span>
                        </span>
                    </bk-tag>
                    <span class="exec-detail-summary-header-build-msg">
                        {{ execDetail.buildMsg }}
                    </span>
                </aside>
                <aside class="exec-detail-summary-header-trigger">
                    <img v-if="execDetail.triggerUserProfile" class="exec-trigger-profile" />
                    <logo class="exec-trigger-profile" name="default-user" size="24" />
                    <span v-if="execDetail.triggerUser">
                        {{
                            $t("details.executorInfo", [
                                execDetail.triggerUser,
                                execDetail.trigger,
                                execFormatStartTime
                            ])
                        }}
                    </span>
                </aside>
            </div>
            <p class="summary-header-shadow" v-show="show"></p>
            <Summary
                ref="detailSummary"
                :visible="summaryVisible"
                :exec-detail="execDetail"
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
            <div :class="['exec-detail-main', {
                'is-outputs-panel': curItemTab === 'outputs'
            }]">
                <component
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
    import AtomPropertyPanel from '@/components/AtomPropertyPanel'
    import Summary from '@/components/ExecDetail/Summary'
    import job from '@/components/ExecDetail/job'
    import plugin from '@/components/ExecDetail/plugin'
    import stage from '@/components/ExecDetail/stage'
    import ExecPipeline from '@/components/ExecPipeline'
    import Logo from '@/components/Logo'
    import Outputs from '@/components/Outputs'
    import StagePropertyPanel from '@/components/StagePropertyPanel'
    import stageReviewPanel from '@/components/StageReviewPanel'
    import StartParams from '@/components/StartParams'
    import codeRecord from '@/components/codeRecord'
    import emptyTips from '@/components/devops/emptyTips'
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'
    import pipelineConstMixin from '@/mixins/pipelineConstMixin'
    import { mapThemeOfStatus } from '@/utils/pipelineStatus'
    import { convertTime } from '@/utils/util'
    import webSocketMessage from '@/utils/webSocketMessage'
    import { mapActions, mapGetters, mapState } from 'vuex'

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
                'editingElementPos',
                'isPropertyPanelVisible',
                'isShowCompleteLog',
                'showPanelType',
                'fetchingAtomList',
                'pipeline',
                'showStageReviewPanel'
            ]),
            ...mapGetters('atom', {
                execDetail: 'getExecDetail'
            }),
            ...mapState(['fetchError']),
            execFormatStartTime () {
                return convertTime(this.execDetail?.queueTime)
            },
            panels () {
                return [
                    {
                        name: 'executeDetail',
                        label: this.$t('details.executeDetail'),
                        component: 'exec-pipeline',
                        className: 'exec-pipeline',
                        bindData: {
                            execDetail: this.execDetail,
                            isLatestBuild: this.isLatestBuild,
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
                return this.$route.params
            },
            curItemTab () {
                return this.routerParams.type || 'executeDetail'
            },
            curPanel () {
                return this.panels.find(panel => panel.name === this.curItemTab)
            },
            statusTagTheme () {
                return mapThemeOfStatus(this.execDetail?.status)
            },
            statusLabel () {
                return this.execDetail?.status ? this.$t(`details.statusMap.${this.execDetail?.status}`) : ''
            },
            isLatestBuild () {
                return this.execDetail?.buildNum === this.execDetail?.latestBuildNum && this.execDetail?.curVersion === this.execDetail?.latestVersion
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
        beforeRouteEnter (to, from, next) {
            if (!to.params.type) {
                next({
                    name: 'pipelinesDetail',
                    params: {
                        ...to.params,
                        type: 'executeDetail'
                    }
                })
            } else {
                next()
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
            handlerScroll (e) {
                this.show = e.target.scrollTop > 88
            },
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

            switchTab (panel) {
                this.$router.push({
                    name: 'pipelinesDetail',
                    params: {
                        ...this.routerParams,
                        type: panel.name
                    }
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

      .exec-status-label {
        display: grid;
        align-items: center;
        grid-auto-flow: column;
        grid-gap: 6px;
      }

      .exec-detail-summary-header-build-msg {
        flex: 1;
        margin: 0 24px 0 8px;
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

        border-radius: 2px 2px 0 0;
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
