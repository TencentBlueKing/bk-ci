<template>
    <div class="pipeline-detail-header">
        <pipeline-bread-crumb>
            <span class="build-num-switcher-wrapper">
                {{ $t("pipelinesDetail") }}
                <build-num-switcher v-bind="buildNumConf" />
            </span>
        </pipeline-bread-crumb>
        <aside class="pipeline-detail-right-aside">
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
            <bk-button
                v-else
                :disabled="loading"
                :icon="loading ? 'loading' : ''"
                outline
                theme="default"
                key="reBuild"
                v-perm="{
                    permissionData: {
                        projectId: projectId,
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
            <bk-button
                v-perm="{
                    permissionData: {
                        projectId: $route.params.projectId,
                        resourceType: 'pipeline',
                        resourceCode: $route.params.pipelineId,
                        action: RESOURCE_ACTION.EDIT
                    }
                }"
                @click="goEdit"
            >
                {{ $t("edit") }}
            </bk-button>
            <bk-button
                theme="primary"
                v-perm="{
                    permissionData: {
                        projectId: $route.params.projectId,
                        resourceType: 'pipeline',
                        resourceCode: $route.params.pipelineId,
                        action: RESOURCE_ACTION.EXECUTE
                    }
                }"
                @click="goExecPreview"
            >
                {{ $t("exec") }}
            </bk-button>
            <more-actions />
        </aside>
    </div>
</template>

<script>
    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import BuildNumSwitcher from './BuildNumSwitcher'
    import MoreActions from './MoreActions.vue'
    import PipelineBreadCrumb from './PipelineBreadCrumb'

    export default {
        components: {
            PipelineBreadCrumb,
            BuildNumSwitcher,
            MoreActions
        },
        data () {
            return {
                loading: false
            }
        },
        computed: {
            ...mapState('atom', ['executeStatus', 'execDetail']),
            ...mapGetters({
                curPipeline: 'pipelines/getCurPipeline'
            }),
            RESOURCE_ACTION () {
                return RESOURCE_ACTION
            },
            isRunning () {
                return ['RUNNING', 'QUEUE'].indexOf(this.execDetail?.status) > -1
            },
            canManualStartup () {
                return this.curPipeline ? this.curPipeline.canManualStartup : false
            },
            pipelineStatus () {
                return this.canManualStartup ? 'ready' : 'disable'
            },
            buildNumConf () {
                return {
                    latestBuildNum: this.execDetail?.latestBuildNum ?? 1,
                    currentBuildNum: this.execDetail?.buildNum ?? 1
                }
            }
        },
        watch: {
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
                    this.handleError(err, [
                        {
                            actionId: this.$permissionActionMap.execute,
                            resourceId: this.$permissionResourceMap.pipeline,
                            instanceId: [
                                {
                                    id: this.$route.params.pipelineId,
                                    name: this.curPipeline.pipelineName
                                }
                            ],
                            projectId: this.$route.params.projectId
                        }
                    ])
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
                    this.$emit('update-table')
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
                this.$router.push({
                    name: 'pipelinesPreview'
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
  padding: 0 24px 0 14px;
  .exec-deatils-operate-divider {
    display: block;
    margin: 0 6px;
    height: 32px;
    width: 1px;
    background: #d8d8d8;
  }
  .build-num-switcher-wrapper {
    display: grid;
    grid-auto-flow: column;
    grid-gap: 6px;
  }
  .pipeline-detail-right-aside {
    display: grid;
    grid-gap: 10px;
    grid-auto-flow: column;
  }
}
</style>
