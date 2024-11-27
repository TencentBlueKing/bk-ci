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
                @click="handleClick"
            >
                {{ $t("cancel") }}
            </bk-button>
            <template v-else-if="!isDebugExec">
                <bk-button
                    :disabled="loading || isCurPipelineLocked"
                    :icon="loading ? 'loading' : ''"
                    outline
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
                    @click="handleClick"
                >
                    {{ $t("history.reBuild") }}
                </bk-button>
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
                    :disabled="!canManualStartup"
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

    export default {
        components: {
            PipelineBreadCrumb,
            ReleaseButton
        },
        data () {
            return {
                loading: false
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
                return this.pipelineInfo?.canManualStartup ?? false
            },
            isDebugExec () {
                return this.execDetail?.debug ?? false
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
        methods: {
            ...mapActions('pipelines', ['requestRetryPipeline', 'requestTerminatePipeline']),
            async handleClick () {
                try {
                    this.loading = true
                    if (this.isRunning) {
                        await this.stopExecute(this.execDetail?.id)
                    } else {
                        await this.retry(this.execDetail?.id)
                    }
                } catch (err) {
                    this.handleError(err, {
                        projectId: this.$route.params.projectId,
                        resourceCode: this.$route.params.pipelineId,
                        action: this.$permissionResourceAction.EXECUTE
                    })
                    this.loading = false
                }
            },
            async retry (buildId, goDetail = false) {
                const { projectId, pipelineId } = this.$route.params

                // 请求执行构建
                const res = await this.requestRetryPipeline({
                    ...this.$route.params,
                    buildId
                })

                if (res && res.id) {
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
}
</style>
