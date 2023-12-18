<template>
    <section
        class="pipeline-detail-wrapper tapd-content"
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
            <div class="exec-detail-main">
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
    import codeRecord from '@/components/codeRecord'
    import emptyTips from '@/components/devops/emptyTips'
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
    import pipelineConstMixin from '@/mixins/pipelineConstMixin'
    import {
        handlePipelineNoPermission,
        RESOURCE_ACTION
    } from '@/utils/permission'
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
            curPanel () {
                return {
                    name: 'executeDetail',
                    label: this.$t('details.executeDetail'),
                    component: 'exec-pipeline',
                    className: 'exec-pipeline',
                    bindData: {
                        execDetail: this.execDetail,
                        isLatestBuild: this.isLatestBuild,
                        matchRules: this.curMatchRules
                    }
                }
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
                this.isLoading = false
                if (error.code === 403) {
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
            ...mapActions('common', ['requestInterceptAtom']),

            hideSidePanel () {
                this.showLog = false
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
.pipeline-detail-wrapper.tapd-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: auto;
  scrollbar-gutter: stable;

  .exec-detail-main {
    background: white;
    flex: 1;
    box-shadow: 0 2px 2px 0 #00000026;
  }
  .exec-pipeline {
    position: relative;
    overflow: auto;
    height: 100%;
    ::v-deep .devops-stage-list {
      padding-bottom: 25px;
    }
  }

  .pp-min-map {
    display: none;
  }
}
</style>
