<template>
    <article class="detail-home" v-bkloading="{ isLoading }">
        <section class="detail-header">
            <i :class="[getIconClass(buildDetail.status), 'header-icon']"></i>
            <p class="detail-info">
                <span class="info-title">
                    <span class="build-title text-ellipsis" v-bk-overflow-tips>{{ buildDetail.buildTitle }}</span>
                    <span class="title-item">
                        <icon
                            :name="buildTypeIcon"
                            size="14"
                            v-bk-tooltips="{
                                content: buildDetail.operationKind === 'delete' ? 'delete' : buildDetail.objectKind,
                                placements: ['top']
                            }"
                        ></icon>
                    </span>
                    <span class="title-item">
                        <span v-if="buildDetail.objectKind === 'schedule'">System</span>
                        <template v-else>
                            <i class="stream-icon stream-user"></i>
                            <template v-if="buildDetail.objectKind === 'openApi'">
                                OPENAPI（{{ buildDetail.userId }}）
                            </template>
                            <template v-else>
                                {{ buildDetail.userId }}
                            </template>
                        </template>
                    </span>
                </span>
                <span class="info-data">
                    <span class="info-item text-ellipsis">
                        <template v-if="buildDetail.operationKind === 'delete' && buildDetail.deleteTag">
                            <icon name="tag" size="14"></icon>
                            {{ buildDetail.commitId }}
                        </template>
                        <template v-else>
                            <icon name="source-branch" size="14"></icon>
                            {{ buildDetail.branch }}
                        </template>
                    </span>
                    <span class="info-item text-ellipsis"><icon name="clock" size="14"></icon>{{ buildDetail.executeTime | spendTimeFilter }}</span>
                    <span class="info-item text-ellipsis">
                        <icon name="message" size="14"></icon>
                        {{ buildDetail.buildHistoryRemark || '--' }}
                        <bk-popconfirm trigger="click" @confirm="confirmUpdateRemark" placement="bottom" confirm-text="confirm" cancel-text="cancel">
                            <div slot="content">
                                <h3 class="mb10">Edit notes</h3>
                                <bk-input type="textarea" v-model="remark" placeholder="Please enter a note" class="mb10 w200"></bk-input>
                            </div>
                            <bk-icon type="edit2" style="font-size: 18px;cursor:pointer" />
                        </bk-popconfirm>
                    </span>
                </span>
                <span class="info-data">
                    <span :class="['info-item', 'text-ellipsis', { 'text-link': buildDetail.jumpUrl }]" @click="goToLink(buildDetail.jumpUrl)">
                        <icon name="commit" size="14" v-if="buildDetail.objectKind === 'schedule'"></icon>
                        <icon :name="buildTypeIcon" size="14" v-else></icon>
                        {{ buildDetail.buildSource || '--' }}
                    </span>
                    <span class="info-item text-ellipsis"><icon name="date" size="14"></icon>{{ buildDetail.startTime | timeFilter }}</span>
                </span>
            </p>

            <div v-bk-tooltips="computedOptToolTip" class="nav-button" v-if="buildDetail.status === 'RUNNING'">
                <bk-button class="detail-button" @click="cancleBuild" :loading="isOperating" :disabled="!curPipeline.enabled || !permission">Cancel Build</bk-button>
            </div>
            <div v-bk-tooltips="computedOptToolTip" class="nav-button" v-else>
                <bk-button class="detail-button" @click="rebuild" :loading="isOperating" :disabled="!curPipeline.enabled || !permission">Re-build</bk-button>
            </div>
        </section>
        <pipeline
            class="detail-stages"
            :editable="false"
            :is-exec-detail="true"
            :pipeline="{ stages: stageList }"
        ></pipeline>
    </article>
</template>

<script>
    import { mapState, mapActions } from 'vuex'
    import { pipelines } from '@/http'
    import {
        preciseDiff,
        timeFormatter,
        getbuildTypeIcon
    } from '@/utils'
    import { getPipelineStatusClass, getPipelineStatusCircleIconCls } from '@/components/status'
    import register from '@/utils/websocket-register'
    import Pipeline from '@/components/Pipeline'

    export default {
        components: {
            Pipeline
        },
        filters: {
            spendTimeFilter (val) {
                return preciseDiff(val)
            },

            timeFilter (val) {
                return timeFormatter(val)
            }
        },

        data () {
            return {
                stageList: [],
                buildDetail: {},
                isLoading: false,
                isOperating: false,
                remark: ''
            }
        },

        computed: {
            ...mapState(['projectId', 'projectInfo', 'permission', 'curPipeline']),

            computedOptToolTip () {
                return {
                    content: !this.curPipeline.enabled ? 'Pipeline disabled' : 'Permission denied',
                    disabled: this.curPipeline.enabled && this.permission
                }
            },

            buildTypeIcon () {
                return getbuildTypeIcon(this.buildDetail.objectKind, this.buildDetail.operationKind)
            }
        },

        watch: {
            '$route.params.buildId' () {
                this.initData()
            }
        },

        created () {
            this.initData()
        },

        beforeDestroy () {
            register.unInstallWsMessage('detail')
        },

        methods: {
            ...mapActions(['setModelDetail']),

            initData () {
                this.isLoading = true
                this.loopGetPipelineDetail().then(() => {
                    this.remark = this.buildDetail.buildHistoryRemark
                }).finally(() => {
                    this.isLoading = false
                })
            },

            getPipelineBuildDetail () {
                const params = {
                    pipelineId: this.$route.params.pipelineId,
                    buildId: this.$route.params.buildId
                }
                return pipelines.getPipelineBuildDetail(this.projectId, params).then((res) => {
                    const modelDetail = res.modelDetail || {}
                    const model = modelDetail.model || {}
                    this.setModelDetail(modelDetail)
                    this.stageList = (model.stages || []).slice(1)
                    this.buildDetail = {
                        ...res.gitProjectPipeline,
                        ...res.gitRequestEvent,
                        buildHistoryRemark: res.buildHistoryRemark,
                        executeTime: modelDetail.executeTime,
                        startTime: modelDetail.startTime,
                        status: modelDetail.status
                    }
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                })
            },

            loopGetPipelineDetail () {
                register.installWsMessage((res) => {
                    const model = res.model || {}
                    this.stageList = (model.stages || []).slice(1)
                    this.buildDetail.status = res.status
                    this.buildDetail.startTime = res.startTime
                    this.buildDetail.executeTime = res.executeTime
                }, 'IFRAMEprocess', 'detail')
                return this.getPipelineBuildDetail()
            },

            // loopGetPipelineDetail () {
            //     clearTimeout(this.loopGetPipelineDetail.loopId)
            //     this.loopGetPipelineDetail.loopId = setTimeout(this.loopGetPipelineDetail, 5000)
            //     return this.getPipelineBuildDetail()
            // },

            rebuild () {
                this.isOperating = true
                pipelines.rebuildPipeline(this.projectId, this.$route.params.pipelineId, this.$route.params.buildId).then(() => {
                    this.getPipelineBuildDetail()
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isOperating = false
                })
            },

            cancleBuild () {
                this.isOperating = true
                pipelines.cancelBuildPipeline(this.projectId, this.$route.params.pipelineId, this.$route.params.buildId).then(() => {
                    this.getPipelineBuildDetail()
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isOperating = false
                })
            },

            confirmUpdateRemark () {
                pipelines.updateRemark(this.projectId, this.$route.params.pipelineId, this.$route.params.buildId, this.remark).then(() => {
                    this.getPipelineBuildDetail()
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                })
            },

            getIconClass (status) {
                return [getPipelineStatusClass(status), ...getPipelineStatusCircleIconCls(status)]
            },

            goToLink (url) {
                if (url) {
                    window.open(url, '_blank')
                }
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .detail-home {
        background: #fff;
    }
    .detail-header {
        padding: 10px 24px;
        height: 96px;
        box-shadow: 0 1px 0 0 #E3E5EA;
        display: flex;
        align-items: flex-start;
        .header-icon {
            width: 32px;
            height: 32px;
            font-size: 32px;
            margin-right: 8px;
            line-height: 32px;
            &.executing {
                font-size: 14px;
            }
            &.icon-exclamation, &.icon-exclamation-triangle, &.icon-clock {
                font-size: 24px;
            }
            &.running {
                color: #459fff;
            }
            &.canceled {
                color: #f6b026;
            }
            &.danger {
                color: #ff5656;
            }
            &.success {
                color: #34d97b;
            }
            &.pause {
                color: #ff9801;
            }
        }
        .detail-info {
            flex: 1;
            margin: 0 7px;
            .info-title {
                color: #313328;
                line-height: 24px;
                height: 24px;
                display: flex;
                align-items: center;
                margin: 4px 0 4px;
                .build-title {
                    max-width: 600px;
                    display: inline-block;
                }
                .title-item {
                    position: relative;
                    display: flex;
                    align-items: center;
                    padding-left: 15px;
                    margin-left: 35px;
                    height: 20px;
                    color: #81838a;
                    font-size: 12px;
                    &:last-child {
                        margin-left: 15px;
                    }
                    .stream-user {
                        width: 20px;
                        height: 20px;
                        line-height: 20px;
                        font-size: 14px;
                        border-radius: 100%;
                        margin-right: 8px;
                    }
                    &:before {
                        position: absolute;
                        content: '';
                        left: 0;
                        height: 12px;
                        width: 1px;
                        background: #c8ccd8;
                    }
                }
            }
            .info-data {
                color: #81838a;
                line-height: 20px;
                font-size: 12px;
                display: flex;
                align-items: center;
            }
            .info-item {
                width: 200px;
                display: flex;
                align-items: center;
                margin-bottom: 2px;
                >svg {
                    margin-right: 8px;
                }
                &:not(:first-child) {
                    margin-left: 40px;
                }
            }
            .history-remark {
                margin-bottom: 15px;
            }
        }
        .nav-button {
            margin-top: 22px;
        }
    }
    .detail-stages {
        height: calc(100% - 96px);
        padding: 30px;
        overflow: auto;
    }
</style>
