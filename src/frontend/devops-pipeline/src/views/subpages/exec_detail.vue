<template>
    <section class="pipeline-detail-wrapper"
        v-bkloading="{ isLoading: isLoading || fetchingAtomList }">
        <empty-tips
            v-if="hasNoPermission"
            :title="noPermissionTipsConfig.title"
            :desc="noPermissionTipsConfig.desc"
            :btns="noPermissionTipsConfig.btns">
        </empty-tips>

        <template v-else-if="execDetail">
            <bk-tab :active="curItemTab" @tab-change="switchTab" class="bkdevops-pipeline-tab-card pipeline-detail-tab-card" type="unborder-card">
                <div slot="setting" class="pipeline-info">
                    <div class="info-item">
                        <span class="item-label">状态：</span>
                        <template v-if="execDetail.status === 'CANCELED'">
                            <span v-bk-tooltips.light="`取消人：${execDetail.cancelUserId}`" :class="{ [execDetail.status]: execDetail.status }">{{ statusMap[execDetail.status] }}</span>
                        </template>
                        <span v-else :class="{ [execDetail.status]: execDetail.status }">{{ statusMap[execDetail.status] }}</span>
                        <i v-if="showRetryIcon" title="重试" class="bk-icon icon-retry" @click.stop="retry(execDetail.id, true)"></i>
                    </div>
                    <div class="info-item">
                        <span class="item-label">执行人：</span>
                        <span class="trigger-mode">{{ execDetail.userId || '--' }}</span>
                    </div>
                    <div class="info-item">
                        <span class="item-label">任务耗时：</span>
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

        <bk-sideslider v-if="editingElementPos && execDetail" v-bind="sidePanelConfig" :is-show.sync="isPropertyPanelShow" :quick-close="true">
            <template slot="content">
                <pipeline-log
                    class="log-panel"
                    v-if="showLog"
                    :build-no="$route.params.buildNo"
                    :build-num="execDetail.buildNum"
                    :show-export="true"
                    :build-tag="getElementId"
                    :execute-count="getExecuteCount"
                />
                <container-property-panel
                    v-else-if="showContainerPanel"
                    :container-index="editingElementPos.containerIndex"
                    :stage-index="editingElementPos.stageIndex"
                    :stages="execDetail.model.stages"
                    :editable="false"
                />
            </template>
        </bk-sideslider>
    </section>
</template>

<script>
    import { mapState, mapActions } from 'vuex'
    import pipelineWebsocket from '@/utils/pipelineWebSocket'
    import stages from '@/components/Stages'
    import viewPart from '@/components/viewPart'
    import codeRecord from '@/components/codeRecord'
    import outputOption from '@/components/outputOption'
    import PipelineLog from '@/components/Log'
    import ContainerPropertyPanel from '@/components/ContainerPropertyPanel/'
    import emptyTips from '@/components/devops/emptyTips'
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'
    import { statusMap } from '@/utils/pipelineConst'
    import { convertMStoStringByRule } from '@/utils/util'

    export default {
        components: {
            stages,
            PipelineLog,
            ContainerPropertyPanel,
            viewPart,
            codeRecord,
            outputOption,
            emptyTips
        },
        mixins: [pipelineOperateMixin],

        data () {
            return {
                isLoading: true,
                hasNoPermission: false,
                noPermissionTipsConfig: {
                    title: '没有权限',
                    desc: '你没有查看该流水线的权限，请切换项目或申请相应权限',
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: this.changeProject,
                            text: '切换项目'
                        },
                        {
                            theme: 'success',
                            size: 'normal',
                            handler: () => {
                                this.goToApplyPerm('role_manager')
                            },
                            text: '申请权限'
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
                'fetchingAtomList'
            ]),
            ...mapState([
                'fetchError'
            ]),
            statusMap () {
                return statusMap
            },
            panels () {
                return [{
                    name: 'executeDetail',
                    label: '执行详情',
                    component: 'stages',
                    className: 'exec-pipeline',
                    bindData: {
                        editable: false,
                        stages: this.execDetail && this.execDetail.model && this.execDetail.model.stages
                    }
                }, {
                    name: 'partView',
                    label: '查看构件',
                    className: '',
                    component: 'view-part',
                    bindData: {}
                }, {
                    name: 'codeRecords',
                    label: '代码变更记录',
                    className: '',
                    component: 'code-record',
                    bindData: {}
                }, {
                    name: 'output',
                    label: '产出物报告',
                    className: '',
                    component: 'output-option',
                    bindData: {
                        curPipeline: this.execDetail && this.execDetail.model
                    }
                }]
            },
            showLog () {
                const { editingElementPos, $route: { params } } = this
                return typeof editingElementPos.elementIndex !== 'undefined' && params.buildNo
            },
            showContainerPanel () {
                const { editingElementPos } = this
                return typeof editingElementPos.containerIndex !== 'undefined'
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
                    title: `${this.getElementViewName || '查看日志'}`,
                    width: 820
                } : {
                    title: '属性栏',
                    class: 'sodaci-property-panel',
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
            isPropertyPanelShow: {
                get () {
                    return this.isPropertyPanelVisible
                },
                set (value) {
                    this.togglePropertyPanel({
                        isShow: value
                    })
                }
            },
            showRetryIcon () {
                return this.execDetail && (this.execDetail.latestVersion === this.execDetail.curVersion) && ['RUNNING', 'QUEUE', 'SUCCEED'].indexOf(this.execDetail.status) < 0
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
                        this.initWebSocket(val)
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

        created () {
            this.requestPipelineExecDetail(this.routerParams)
        },

        beforeDestroy () {
            this.setPipelineDetail(null)
            this.togglePropertyPanel({
                isShow: false
            })
            pipelineWebsocket.disconnect()
        },

        methods: {
            ...mapActions('atom', [
                'updateAtom',
                'togglePropertyPanel',
                'requestPipelineExecDetail',
                'setPipelineDetail'
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
            },

            initWebSocket () {
                const projectId = this.routerParams.projectId
                const subscribe = `/topic/pipelineDetail/${this.routerParams.buildNo}`

                pipelineWebsocket.connect(projectId, subscribe, {
                    success: (res) => {
                        const data = JSON.parse(res.body)
                        this.setPipelineDetail(data)
                    },
                    error: (message) => this.$showTips({ message, theme: 'error' })
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
                padding-bottom: 10px;
            }
        }
        .exec-pipeline {
            position: relative;
            overflow: auto;
            height: 100%;
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
            width: 360px;
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
            }
        }
    }
</style>
