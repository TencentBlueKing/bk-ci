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
                <log :title="currentElement.name"
                    :status="currentElement.status"
                    :execute-count="currentElement.executeCount"
                    @changeExecute="changeExecute"
                    :down-load-link="downLoadPluginLink"
                    :id="currentElement.id"
                    @closeLog="closeLog"
                    ref="log"
                />
            </template>
            <template v-else-if="showContainerPanel">
                <job v-if="currentJob['@type'] !== 'trigger'"
                    :title="currentJob.name"
                    :status="currentJob.status"
                    :plugin-list="pluginList"
                    :down-load-link="downLoadJobLink"
                    @closeLog="closeLog"
                    @closePlugin="closePlugin"
                    @openPlugin="initLog"
                    ref="log"
                />
                <container-property-panel v-else
                    :title="sidePanelConfig.title"
                    :container-index="editingElementPos.containerIndex"
                    :stage-index="editingElementPos.stageIndex"
                    :stages="execDetail.model.stages"
                    :editable="false"
                />
            </template>
            <template v-else-if="typeof editingElementPos.stageIndex !== 'undefined'">
                <stage-property-panel
                    :stage="stage"
                    :stage-index="editingElementPos.stageIndex"
                    :editable="false"
                />
            </template>
        </template>
        <template v-if="execDetail">
            <log v-if="showCompleteLog"
                :title="execDetail.pipelineName"
                :status="execDetail.status"
                :id="execDetail.id"
                :down-load-name="execDetail.pipelineName"
                :down-load-link="downLoadAllLink"
                @closeLog="closeLog"
                ref="log"
            >
            </log>
        </template>
        <review-dialog :is-show="showReviewDialog"></review-dialog>
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
    import StagePropertyPanel from '@/components/StagePropertyPanel'
    import emptyTips from '@/components/devops/emptyTips'
    import ReviewDialog from '@/components/ReviewDialog'
    import log from '../../../../devops-log'
    import job from '../../../../devops-log/src/job'
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'
    import pipelineConstMixin from '@/mixins/pipelineConstMixin'
    import { convertMStoStringByRule } from '@/utils/util'
    import Logo from '@/components/Logo'

    export default {
        components: {
            stages,
            ContainerPropertyPanel,
            StagePropertyPanel,
            viewPart,
            codeRecord,
            outputOption,
            emptyTips,
            log,
            job,
            ReviewDialog,
            Logo
        },
        mixins: [pipelineOperateMixin, pipelineConstMixin],

        data () {
            return {
                isLoading: true,
                hasNoPermission: false,
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
                'isShowCompleteLog',
                'fetchingAtomList',
                'showReviewDialog'
            ]),
            ...mapState([
                'fetchError'
            ]),

            downLoadAllLink () {
                const fileName = encodeURI(encodeURI(this.execDetail.pipelineName))
                return `${AJAX_URL_PIRFIX}/log/api/user/logs/${this.$route.params.projectId}/${this.$route.params.pipelineId}/${this.execDetail.id}/download?executeCount=1&fileName=${fileName}`
            },

            downLoadJobLink () {
                const editingElementPos = this.editingElementPos
                const fileName = encodeURI(encodeURI(`${editingElementPos.stageIndex + 1}-${editingElementPos.containerIndex + 1}-${this.currentJob.name}`))
                const jobId = this.currentJob.containerId
                const tag = `startVM-${this.currentJob.id}`
                const curLogPostData = this.logPostData[tag] || {}
                const currentExe = curLogPostData.currentExe || 1
                return `${AJAX_URL_PIRFIX}/log/api/user/logs/${this.$route.params.projectId}/${this.$route.params.pipelineId}/${this.execDetail.id}/download?jobId=${jobId}&executeCount=${currentExe}&fileName=${fileName}`
            },
            downLoadPluginLink () {
                const editingElementPos = this.editingElementPos
                const fileName = encodeURI(encodeURI(`${editingElementPos.stageIndex + 1}-${editingElementPos.containerIndex + 1}-${editingElementPos.elementIndex + 1}-${this.currentElement.name}`))
                const tag = this.currentElement.id
                const curLogPostData = this.logPostData[tag] || {}
                const currentExe = curLogPostData.currentExe || 1
                return `${AJAX_URL_PIRFIX}/log/api/user/logs/${this.$route.params.projectId}/${this.$route.params.pipelineId}/${this.execDetail.id}/download?tag=${tag}&executeCount=${currentExe}&fileName=${fileName}`
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
                    if (res) this.$nextTick(this.initLog)
                    return res
                },
                set (value) {
                    this.togglePropertyPanel({
                        isShow: value
                    })
                }
            },
            showCompleteLog () {
                const { isShowCompleteLog, $route: { params } } = this
                const res = isShowCompleteLog && params.buildNo
                if (res) this.$nextTick(this.initAllLog)
                return res
            },
            showContainerPanel () {
                const { editingElementPos } = this
                const res = typeof editingElementPos.containerIndex !== 'undefined'
                return res
            },
            pluginList () {
                const startUp = { name: 'Set up job', status: this.currentJob.startVMStatus, id: `startVM-${this.currentJob.id}` }
                return [startUp, ...this.currentJob.elements]
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
            },

            changeExecute (tag, currentExe) {
                const curLogPostData = this.logPostData[tag]
                curLogPostData.currentExe = currentExe
                curLogPostData.lineNo = 0
                clearTimeout(curLogPostData.id)
                curLogPostData.clearIds.push(curLogPostData.id)
                curLogPostData.id = undefined
                this.openLogApi(curLogPostData)
            },

            closePlugin (tag) {
                const curLogPostData = this.logPostData[tag]
                clearTimeout(curLogPostData.id)
                curLogPostData.clearIds.push(curLogPostData.id)
                curLogPostData.id = undefined
            },

            initAllLog () {
                const route = this.$route.params || {}
                const tag = this.execDetail.id
                this.logPostData[tag] = {
                    projectId: route.projectId,
                    pipelineId: route.pipelineId,
                    buildId: this.execDetail.id,
                    lineNo: 0,
                    id: undefined,
                    clearIds: [],
                    ref: this.$refs.log,
                    currentExe: 1
                }
                this.openLogApi(this.logPostData[tag])
            },

            initLog (tag = this.currentElement.id, ref = this.$refs.log) {
                const route = this.$route.params || {}
                let currentLogPost = this.logPostData[tag]
                if (!currentLogPost) {
                    const curEle = this.pluginList.find(x => x.id === tag) || {}
                    currentLogPost = {
                        projectId: route.projectId,
                        pipelineId: route.pipelineId,
                        buildId: this.execDetail.id,
                        tag,
                        currentExe: curEle.executeCount || 1,
                        ref,
                        lineNo: 0,
                        id: undefined,
                        clearIds: []
                    }
                    this.logPostData[tag] = currentLogPost
                    this.openLogApi(currentLogPost)
                } else {
                    this.getAfterLogApi(100, currentLogPost)
                }
            },

            openLogApi (currentLogPost) {
                this.getInitLog(currentLogPost).then((res) => {
                    this.handleLogRes(res, currentLogPost)
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                    if (currentLogPost.ref) currentLogPost.ref.handleApiErr(err.message)
                })
            },

            handleLogRes (res, currentLogPost, curId = currentLogPost.id) {
                if (currentLogPost.clearIds.includes(curId) || currentLogPost.ref === undefined) return
                res = res.data || {}
                if (res.status !== 0) {
                    let errMessage
                    switch (res.status) {
                        case 1:
                            errMessage = this.$t('history.logEmpty')
                            break
                        case 2:
                            errMessage = this.$t('history.logClear')
                            break
                        case 3:
                            errMessage = this.$t('history.logClose')
                            break
                        default:
                            errMessage = this.$t('history.logErr')
                            break
                    }
                    currentLogPost.ref.handleApiErr(errMessage)
                    return
                }

                const logs = res.logs || []
                const lastLog = logs[logs.length - 1] || {}
                const lastLogNo = lastLog.lineNo || currentLogPost.lineNo - 1 || -1
                currentLogPost.lineNo = +lastLogNo + 1
                if (res.finished) {
                    if (res.hasMore) {
                        currentLogPost.ref.addLogData(logs)
                        this.getAfterLogApi(100, currentLogPost)
                    } else {
                        currentLogPost.ref.addLogData(logs)
                    }
                } else {
                    currentLogPost.ref.addLogData(logs)
                    this.getAfterLogApi(1000, currentLogPost)
                }
            },

            getAfterLogApi (mis, currentLogPost) {
                const curId = currentLogPost.id = setTimeout(() => {
                    if (currentLogPost.clearIds.includes(curId) || currentLogPost.ref === undefined) return
                    this.getAfterLog(currentLogPost).then((res) => {
                        this.handleLogRes(res, currentLogPost, curId)
                    }).catch((err) => {
                        this.$bkMessage({ theme: 'error', message: err.message || err })
                        currentLogPost.ref.handleApiErr(err.message)
                    })
                }, mis)
            },

            closeLog () {
                this.showLog = false
                Object.keys(this.logPostData).forEach((key) => {
                    const currentPostData = this.logPostData[key] || {}
                    const currentId = currentPostData.id || ''
                    clearTimeout(currentId)
                    currentPostData.clearIds.push(currentId)
                    currentPostData.id = undefined
                })
                this.logPostData = {}
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
