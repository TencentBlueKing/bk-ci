<template>
    <div
        v-if="execDetail"
        class="pipeline-detail-header"
    >
        <pipeline-bread-crumb
            :show-record-entry="isDebugExec"
            show-build-num-switch
            :pipeline-name="pipelineInfo?.pipelineName"
        />
        <aside
            v-if="!archiveFlag"
            :class="['pipeline-detail-right-aside', {
                'is-debug-exec-detail': isDebugExec
            }]"
        >
            <bk-button
                v-if="isRunning"
                :disabled="loading"
                :icon="loading ? 'loading' : ''"
                outline
                theme="warning"
                @click="handleCancel"
            >
                {{ $t("cancel") }}
            </bk-button>
            <template v-else-if="!isDebugExec">
                <bk-dropdown-menu
                    trigger="click"
                    :disabled="loading || isCurPipelineLocked"
                >
                    <div
                        class="rebuild-dropdown-trigger"
                        slot="dropdown-trigger"
                    >
                        <i
                            v-if="loading"
                            class="devops-icon icon-circle-2-1 spin-icon"
                        />
                        <span>{{ $t("history.reBuild") }}</span>
                        <i class="bk-icon icon-angle-down"></i>
                    </div>
                    <ul
                        class="rebuild-dropdown-content"
                        slot="dropdown-content"
                    >
                        <li
                            :class="['dropdown-item', {
                                'disabled': loading || isCurPipelineLocked
                            }]"
                            v-perm="{
                                hasPermission: canExecute,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId,
                                    resourceType: 'pipeline',
                                    resourceCode: pipelineId,
                                    action: RESOURCE_ACTION.EXECUTE
                                }
                            }"
                            @click="handleClick('reBuild')"
                        >
                            {{ $t("history.reBuild") }}
                            <bk-popover
                                :z-index="3000"
                            >
                                <i class="bk-icon icon-info-circle" />
                                <template slot="content">
                                    <p>{{ $t('history.reBuildTips1') }}</p>
                                    <p>{{ $t('history.reBuildTips2') }}</p>
                                    <p>{{ $t('history.reBuildTips3') }}</p>
                                </template>
                            </bk-popover>
                        </li>
                        <li
                            :class="['dropdown-item', {
                                'disabled': loading || isCurPipelineLocked || !canReplay
                            }]"
                            v-bk-tooltips="{
                                content: this.$t('history.canNotReplayTips'),
                                disabled: canReplay
                            }"
                            v-perm="{
                                hasPermission: canExecute,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId,
                                    resourceType: 'pipeline',
                                    resourceCode: pipelineId,
                                    action: RESOURCE_ACTION.EXECUTE
                                }
                            }"
                            @click="handleClick('rePlay')"
                        >
                            {{ $t("history.rePlay") }}
                            <bk-popover
                                :z-index="3000"
                                :disabled="!canReplay"
                            >
                                <i class="bk-icon icon-info-circle" />
                                <template slot="content">
                                    <p>{{ $t('history.rePlayTips1') }}</p>
                                    <p>{{ $t('history.rePlayTips2') }}</p>
                                    <p>{{ $t('history.rePlayTips3') }}</p>
                                </template>
                            </bk-popover>
                        </li>
                    </ul>
                </bk-dropdown-menu>
                <span class="exec-deatils-operate-divider"></span>
            </template>
            <bk-button
                v-perm="{
                    hasPermission: canEdit,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId,
                        resourceType: 'pipeline',
                        resourceCode: pipelineId,
                        action: RESOURCE_ACTION.EDIT
                    }
                }"
                :disabled="loading"
                key="edit"
                @click="goEdit"
            >
                {{ $t("edit") }}
            </bk-button>
            <span
                v-bk-tooltips="{
                    disabled: canManualStartup,
                    content: $t('pipelineManualDisable')
                }"
            >
                <bk-button
                    :loading="executeStatus"
                    :disabled="!canManualStartup || loading"
                    v-perm="{
                        hasPermission: canExecute,
                        disablePermissionApi: true,
                        permissionData: {
                            projectId,
                            resourceType: 'pipeline',
                            resourceCode: pipelineId,
                            action: RESOURCE_ACTION.EXECUTE
                        }
                    }"
                    @click="goExecPreview"
                >
                    {{ $t(isDebugExec ? "debug" : "exec") }}
                </bk-button>
            </span>
            <release-button
                v-if="isDebugExec"
                :can-release="canRelease"
                :project-id="projectId"
                :pipeline-id="pipelineId"
            />
        </aside>
    </div>
    <i
        v-else
        class="devops-icon icon-circle-2-1 spin-icon"
        style="margin-left: 20px;"
    ></i>
</template>

<script>
    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import PipelineBreadCrumb from './PipelineBreadCrumb'
    import ReleaseButton from './ReleaseButton'
    const PIPELINE_REPLAY_STATUS = {
        REPLAYING: 'REPLAYING',
        REPLAY_SUCCESS: 'REPLAY_SUCCESS',
        CANNOT_REPLAY: 'CANNOT_REPLAY',
        CAN_REPLAY: 'CAN_REPLAY'
    }
    export default {
        components: {
            PipelineBreadCrumb,
            ReleaseButton
        },
        data () {
            return {
                loading: false,
                timesNum: 1,
                canReplay: true
            }
        },
        computed: {
            ...mapState('atom', ['execDetail', 'pipelineInfo', 'saveStatus']),
            ...mapGetters({
                isCurPipelineLocked: 'atom/isCurPipelineLocked'
            }),
            ...mapState('pipelines', ['executeStatus']),
            RESOURCE_ACTION () {
                return RESOURCE_ACTION
            },
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            canEdit () {
                return this.pipelineInfo?.permissions?.canEdit ?? true
            },
            canExecute () {
                return this.pipelineInfo?.permissions?.canExecute ?? true
            },
            isRunning () {
                return ['RUNNING', 'QUEUE'].indexOf(this.execDetail?.status) > -1
            },
            canRelease () {
                return (this.pipelineInfo?.canRelease ?? false) && !this.saveStatus && !this.isRunning
            },
            canManualStartup () {
                return this.pipelineInfo?.canManualStartup ?? true
            },
            isDebugExec () {
                return this.execDetail?.debug ?? false
            },
            archiveFlag () {
                return this.$route.query.archiveFlag
            }
        },
        watch: {
            '$route.params.buildNo': function (newBuildNum, oldBuildNum) {
                if (newBuildNum !== oldBuildNum) {
                    this.loading = false
                }
            },
            'execDetail.status': function (newStatus, oldStatus) {
                if (newStatus !== oldStatus) {
                    this.loading = false
                }
            }
        },
        mounted () {
            this.fetchPipelineRePlayStatus()
        },
        methods: {
            ...mapActions(
                'pipelines',
                [
                    'requestRetryPipeline',
                    'requestTerminatePipeline',
                    'requestRePlayPipeline',
                    'requestPipelineRePlayStatus',
                    'requestRePlayEventDetail'
                ]
            ),
            async handleCancel () {
                try {
                    this.loading = true
                    await this.stopExecute(this.execDetail?.id)
                } catch (err) {
                    this.handleError(err, {
                        projectId: this.$route.params.projectId,
                        resourceCode: this.$route.params.pipelineId,
                        action: this.$permissionResourceAction.EXECUTE
                    })
                    this.loading = false
                }
            },
            async handleClick (type = 'reBuild') {
                const h = this.$createElement
                const title = type === 'reBuild' ? this.$t('history.reBuildConfirmTips') : this.$t('history.rePlayConfirmTips')
                this.$bkInfo({
                    title,
                    width: 500,
                    confirmLoading: true,
                    subHeader: h('div', {
                        style: {
                            background: '#f5f6fa',
                            padding: '10px',
                            fontSize: '12px',
                            lineHeight: '20px'
                        }
                    }, type === 'reBuild'
                        ? [
                            h('p', this.$t('history.reBuildInfo1')),
                            h('p', this.$t('history.reBuildInfo2'))
                        ]
                        : [
                            h('p', this.$t('history.rePlayInfo1')),
                            h('p', this.$t('history.rePlayInfo2'))
                        ]),
                    confirmFn: async () => {
                        try {
                            this.loading = true
                            await this.retry(type, this.execDetail?.id)
                        } catch (err) {
                            this.handleError(err, {
                                projectId: this.$route.params.projectId,
                                resourceCode: this.$route.params.pipelineId,
                                action: this.$permissionResourceAction.EXECUTE
                            })
                            this.loading = false
                        }
                    }
                })
            },
            async retry (type = 'reBuild', buildId, forceTrigger = false) {
                const { projectId, pipelineId } = this.$route.params
                const retryFn = type === 'reBuild' ? this.requestRetryPipeline : this.requestRePlayPipeline
                // 请求执行构建
                const res = await retryFn({
                    ...this.$route.params,
                    buildId,
                    forceTrigger
                })
                if (res?.id) {
                    this.$router.replace({
                        name: 'pipelinesDetail',
                        params: {
                            ...this.$route.params,
                            projectId,
                            pipelineId,
                            buildNo: res.id,
                            type: 'executeDetail',
                            executeCount: res.executeCount
                        }
                    })

                    this.$showTips({
                        message: this.$t('subpage.rebuildSuc'),
                        theme: 'success'
                    })
                } else if (res?.eventId && res.status === PIPELINE_REPLAY_STATUS.REPLAYING) {
                    // 等待轮询完成
                    const pollingResult = await this.fetchRePlayEventDetail(res.eventId)
                    return pollingResult
                } else if (res?.code === 2101272) {
                    this.loading = false
                    this.$bkInfo({
                        title: this.$t('history.rePlay'),
                        subTitle: res.message,
                        width: 500,
                        confirmLoading: true,
                        confirmFn: async () => {
                            try {
                                this.loading = true
                                const result = await this.retry('rePlay', buildId, true)
                                return result
                            } catch (err) {
                                this.handleError(err, {
                                    projectId: this.$route.params.projectId,
                                    resourceCode: this.$route.params.pipelineId,
                                    action: this.$permissionResourceAction.EXECUTE
                                })
                                this.loading = false
                            }
                        }
                    })
                } else {
                    throw Error(this.$t('subpage.rebuildFail'))
                }
            },
            /**
             *  终止流水线
             */
            async stopExecute (buildId) {
                const res = await this.requestTerminatePipeline({
                    ...this.$route.params,
                    buildId
                })

                if (res) {
                    this.$showTips({
                        message: this.$t('subpage.stopSuc'),
                        theme: 'success'
                    })
                } else {
                    throw Error(this.$t('subpage.stopFail'))
                }
            },
            goExecPreview () {
                const version = this.pipelineInfo?.[this.isDebugExec ? 'version' : 'releaseVersion']
                this.$router.push({
                    name: 'executePreview',
                    query: {
                        ...(this.isDebugExec ? { debug: '' } : {})
                    },
                    params: {
                        ...this.$route.params,
                        version
                    }
                })
            },
            goEdit () {
                this.$router.push({
                    name: 'pipelinesEdit'
                })
            },

            async fetchPipelineRePlayStatus () {
                try {
                    const res = await this.requestPipelineRePlayStatus({
                        projectId: this.projectId,
                        pipelineId: this.pipelineId,
                        buildId: this.$route.params.buildNo
                    })
                    this.canReplay = res.status === PIPELINE_REPLAY_STATUS.CAN_REPLAY
                } catch (err) {
                    console.error(err)
                }
            },
            
            async fetchRePlayEventDetail (eventId) {
                try {
                    this.loading = true
                    const res = await this.requestRePlayEventDetail({
                        projectId: this.projectId,
                        eventId
                    })
                    if (!res.records.length) {
                        // 用于webhook触发的构建任务轮询获取构建状态（超3次返回为空时，则直接报错重放失败提示）
                        if (this.timesNum > 3) {
                            this.timesNum = 1
                            this.loading = false
                            this.$showTips({
                                message: this.$t('history.rePlayFailed'),
                                theme: 'error'
                            })
                            return true
                        }
                        await new Promise(resolve => setTimeout(resolve, 5000))
                        this.timesNum++
                        return await this.fetchRePlayEventDetail(eventId)
                    } else {
                        const successStatus = res.records[0].status === 'SUCCEED'
                        if (successStatus) {
                            this.$router.replace({
                                name: 'pipelinesDetail',
                                params: {
                                    ...this.$route.params,
                                    projectId: this.projectId,
                                    pipelineId: this.pipelineId,
                                    buildNo: res?.records[0]?.buildId,
                                    type: 'executeDetail'
                                }
                            })
                        }
                        this.loading = false
                        this.$showTips({
                            message: res.records[0].reason,
                            theme: successStatus ? 'success': 'error'
                        })
                    }
                } catch (err) {
                    console.error(err)
                }
            }
        }
    }
</script>

<style lang="scss">
.pipeline-detail-header {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  padding: 0 0 0 14px;
  height: 100%;
  .exec-deatils-operate-divider {
    display: block;
    margin: 0 6px;
    height: 32px;
    width: 1px;
    background: #d8d8d8;
  }
  .pipeline-detail-right-aside {
    display: grid;
    grid-gap: 10px;
    grid-auto-flow: column;
    height: 100%;
    align-items:center;
    &:not(.is-debug-exec-detail) {
        padding-right: 24px;
    }
  }
  .rebuild-dropdown-trigger {
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid #c4c6cc;
    height: 32px;
    font-size: 14px;
    border-radius: 2px;
    padding: 0 15px;
    color: #63656E;
    &:hover {
        cursor: pointer;
        border-color: #979ba5;
    }
    .icon-angle-down {
        margin-left: 5px;
        font-size: 16px;
    }
    .spin-icon {
        margin-right: 5px;
        color: #458bff;
        z-index: 2000;
    }
  }
  .rebuild-dropdown-content {
    .dropdown-item {
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        height: 32px;
        line-height: 32px;
        font-size: 14px;

        &:hover {
            background-color: #f0f1f5;
            color: #3a84ff;
        }
        &.disabled {
            cursor: not-allowed;
            color: #dcdee5;
        }
        .icon-info-circle {
            margin-left: 5px;
            font-size: 12px;
        }
    }
  }
}
</style>
