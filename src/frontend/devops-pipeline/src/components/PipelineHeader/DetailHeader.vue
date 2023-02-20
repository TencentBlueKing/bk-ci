<template>
    <div class="pipeline-detail-header">
        <pipeline-bread-crumb>
            <span class="build-num-switcher-wrapper">
                {{ $t("执行详情：") }}
                <build-num-switcher v-bind="buildNumConf" />
            </span>
        </pipeline-bread-crumb>
        <aside class="pipeline-detail-right-aside">
            <bk-button
                :disabled="loading || !canManualStartup"
                :loading="loading"
                :theme="isRunning ? 'warning' : ''"
                @click="handleClick"
            >
                {{ isRunning ? $t("终止构建") : $t("重新构建") }}
            </bk-button>
        </aside>
    </div>
</template>

<script>
    import { mapState, mapGetters, mapActions } from 'vuex'
    import PipelineBreadCrumb from './PipelineBreadCrumb'
    import BuildNumSwitcher from './BuildNumSwitcher'
    export default {
        components: {
            PipelineBreadCrumb,
            BuildNumSwitcher
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
                this.loading = true
                if (this.isRunning) {
                    await this.stopExecute(this.execDetail?.id)
                } else {
                    await this.retry(this.execDetail?.id)
                }
            },
            async retry (buildId, goDetail = false) {
                let message, theme
                const { projectId, pipelineId } = this.$route.params
                try {
                    // 请求执行构建
                    const res = await this.requestRetryPipeline({
                        ...this.$route.params,
                        buildId
                    })

                    if (res && res.id) {
                        message = this.$t('subpage.rebuildSuc')
                        theme = 'success'
                        if (goDetail) {
                            this.$router.replace({
                                name: 'pipelinesDetail',
                                params: {
                                    projectId,
                                    pipelineId,
                                    buildNo: res.id
                                }
                            })
                        }
                        this.$emit('update-table')
                    } else {
                        message = this.$t('subpage.rebuildFail')
                        theme = 'error'
                    }
                } catch (err) {
                    this.handleError(err, [
                        {
                            actionId: this.$permissionActionMap.execute,
                            resourceId: this.$permissionResourceMap.pipeline,
                            instanceId: [
                                {
                                    id: pipelineId,
                                    name: this.curPipeline.pipelineName
                                }
                            ],
                            projectId
                        }
                    ])
                } finally {
                    message
                        && this.$showTips({
                            message,
                            theme
                        })
                }
            },
            /**
             *  终止流水线
             */
            async stopExecute (buildId) {
                let message, theme

                try {
                    const res = await this.requestTerminatePipeline({
                        ...this.$route.params,
                        buildId
                    })

                    if (res) {
                        message = this.$t('subpage.stopSuc')
                        theme = 'success'
                    } else {
                        message = this.$t('subpage.stopFail')
                        theme = 'error'
                    }
                } catch (err) {
                    this.handleError(err, [
                        {
                            actionId: this.$permissionActionMap.execute,
                            resourceId: this.$permissionResourceMap.pipeline,
                            instanceId: [
                                {
                                    id: this.curPipeline.pipelineId,
                                    name: this.curPipeline.pipelineName
                                }
                            ],
                            projectId: this.$route.params.projectId
                        }
                    ])
                } finally {
                    message
                        && this.$showTips({
                            message,
                            theme
                        })
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
  padding: 0 24px 0 14px;
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
