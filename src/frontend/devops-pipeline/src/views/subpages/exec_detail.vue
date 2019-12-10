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
                        <span class="item-label">{{ $t('status') }}：</span>
                        <template v-if="execDetail.status === 'CANCELED'">
                            <span v-bk-tooltips.light="`${$t('details.canceller')}：${execDetail.cancelUserId}`" :class="{ [execDetail.status]: execDetail.status }">{{ statusMap[execDetail.status] }}</span>
                        </template>
                        <span v-else :class="{ [execDetail.status]: execDetail.status }">{{ statusMap[execDetail.status] }}</span>
                        <i v-if="showRetryIcon" :title="$t('retry')" class="bk-icon icon-retry" @click.stop="retry(execDetail.id, true)"></i>
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
                <log :show="showLog"
                    :is-init="isInitLog"
                    :title="currentElement.name"
                    :status="currentElement.status"
                    :id="currentElement.id"
                    :link-url="linkUrl"
                    log-type="plugin"
                    :down-load-link="downLoadPluginLink"
                    @closeLog="closeLog"
                    ref="log"
                />
            </template>
            <template v-else-if="showContainerPanel">
                <log :show="showContainerPanel"
                    :is-init="isInitLog"
                    :title="currentJob.name"
                    :status="currentJob.status"
                    :id="currentJob.containerId"
                    :link-url="linkUrl"
                    log-type="job"
                    :down-load-link="downLoadJobLink"
                    @closeLog="closeLog"
                    ref="log"
                >
                </log>
            </template>
        </template>
    </section>
</template>

<script>
    import { mapState, mapActions } from 'vuex'
    import webSocketMessage from '@/utils/webSocketMessage'
    import stages from '@/components/Stages'
    import viewPart from '@/components/viewPart'
    import codeRecord from '@/components/codeRecord'
    import outputOption from '@/components/outputOption'
    import ContainerPropertyPanel from '@/components/ContainerPropertyPanel/'
    import emptyTips from '@/components/devops/emptyTips'
    import log from '@/components/Log'
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'
    import pipelineConstMixin from '@/mixins/pipelineConstMixin'
    import { convertMStoStringByRule } from '@/utils/util'

    export default {
        components: {
            stages,
            ContainerPropertyPanel,
            viewPart,
            codeRecord,
            outputOption,
            emptyTips,
            log
        },
        mixins: [pipelineOperateMixin, pipelineConstMixin],

        data () {
            return {
                isLoading: true,
                hasNoPermission: false,
                isInitLog: false,
                logPostData: {},
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
                                this.goToApplyPerm('role_manager')
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
                'fetchingAtomList'
            ]),
            ...mapState([
                'fetchError'
            ]),
            downLoadJobLink () {
                return `${AJAX_URL_PIRFIX}/log/api/user/logs/pipelinedebug/${this.$route.params.pipelineId}/${this.execDetail.id}/download?jobId=${this.currentJob.containerId}`
            },
            downLoadPluginLink () {
                return `${AJAX_URL_PIRFIX}/log/api/user/logs/pipelinedebug/${this.$route.params.pipelineId}/${this.execDetail.id}/download?tag=${this.currentElement.id}`
            },
            panels () {
                return [{
                    name: 'executeDetail',
                    label: this.$t('details.executeDetail'),
                    component: 'stages',
                    className: 'exec-pipeline',
                    bindData: {
                        editable: false,
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
                    const { editingElementPos, $route: { params } } = this
                    const res = typeof editingElementPos.elementIndex !== 'undefined' && params.buildNo
                    if (res) this.initLog()
                    return res
                },
                set (value) {
                    this.togglePropertyPanel({
                        isShow: value
                    })
                }
            },
            showContainerPanel () {
                const { editingElementPos } = this
                const res = typeof editingElementPos.containerIndex !== 'undefined'
                if (res) this.initJobLog()
                return res
            },
            currentJob () {
                const { editingElementPos, execDetail } = this
                const model = execDetail.model || {}
                const stages = model.stages || []
                const currentStage = stages[editingElementPos.stageIndex] || []
                return currentStage.containers[editingElementPos.containerIndex]
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
            showRetryIcon () {
                return this.execDetail && (this.execDetail.latestVersion === this.execDetail.curVersion) && ['RUNNING', 'QUEUE', 'SUCCEED'].indexOf(this.execDetail.status) < 0
            }
        },

        watch: {
            execDetail (val) {
                this.isLoading = val === null
                const query = this.$route.query || {}
                const logType = query.logType
                const id = query.id
                if (logType === 'plugin') this.showElementLog(id)
                if (logType === 'job') this.showJobLog(id)
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
                'buildLogWs',
                'stopLogWs'
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
            },

            showElementLog (elementId) {
                let eleIndex, conIndex
                const staIndex = this.execDetail.model.stages.findIndex(stage => {
                    conIndex = stage.containers.findIndex(container => {
                        eleIndex = container.elements.findIndex(element => element.id === elementId)
                        return eleIndex > -1
                    })
                    return conIndex > -1
                })
                if (staIndex > -1) {
                    this.togglePropertyPanel({
                        isShow: true,
                        editingElementPos: {
                            stageIndex: staIndex,
                            containerIndex: conIndex,
                            elementIndex: eleIndex
                        }
                    })
                }
            },

            showJobLog (id) {
                let conIndex
                const staIndex = this.execDetail.model.stages.findIndex(stage => {
                    conIndex = stage.containers.findIndex(container => {
                        return container.containerId === id
                    })
                    return conIndex > -1
                })
                this.togglePropertyPanel({
                    isShow: true,
                    editingElementPos: {
                        stageIndex: staIndex,
                        containerIndex: conIndex
                    }
                })
            },

            initJobLog () {
                const route = this.$route.params || {}
                this.logPostData = {
                    projectId: route.projectId,
                    pipelineId: route.pipelineId,
                    buildId: this.execDetail.id,
                    jobId: this.currentJob.containerId,
                    payLoad: {
                        page: `/log/build/${this.execDetail.id}/job/${this.currentJob.containerId}`,
                        sessionId: this.uuid()
                    }
                }
                this.openLogApi()
            },

            initLog () {
                const route = this.$route.params || {}
                this.logPostData = {
                    projectId: route.projectId,
                    pipelineId: route.pipelineId,
                    buildId: this.execDetail.id,
                    tag: this.currentElement.id,
                    payLoad: {
                        page: `/log/build/${this.execDetail.id}/tag/${this.currentElement.id}`,
                        sessionId: this.uuid()
                    }
                }
                this.openLogApi()
            },

            openLogApi () {
                if (this.openLogApi.hasOpen) return
                this.openLogApi.hasOpen = true
                this.isInitLog = true
                this.getInitLog(this.logPostData).then((res) => {
                    res = res.data || {}
                    this.$refs.log.addLogData(res.logs, true)
                    if (res.hasMore) {
                        const lastLog = res.logs[res.logs.length - 1] || { lineNo: 0 }
                        this.logPostData.lineNo = lastLog.lineNo
                        webSocketMessage.openDialogWebSocket((res) => {
                            this.$refs.log.addLogData(res.logs || [])
                            if (res.finished) {
                                this.closeLog()
                            }
                        }, this.logPostData.payLoad)
                        this.buildLogWs(this.logPostData).catch((err) => this.$bkMessage({ theme: 'error', message: err.message || err }))
                    }
                }).catch((err) => this.$bkMessage({ theme: 'error', message: err.message || err })).finally(() => (this.isInitLog = false))
            },

            closeLog () {
                this.openLogApi.hasOpen = false
                this.showLog = false
                this.stopLogWs(this.logPostData).catch((err) => this.$bkMessage({ theme: 'error', message: err.message || err }))
                webSocketMessage.closeDialogWebSocket(this.logPostData.payLoad)
            },

            uuid () {
                let id = ''
                for (let i = 0; i < 7; i++) {
                    const randomNum = Math.floor((1 + Math.random()) * 0x10000).toString(16).substring(1)
                    id += randomNum
                }
                return id
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
                height: calc(100% - 60px);
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
            }
        }
    }
</style>
