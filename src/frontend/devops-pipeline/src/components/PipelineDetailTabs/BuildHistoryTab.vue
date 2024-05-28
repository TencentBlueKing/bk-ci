<template>
    <div class="build-history-tab-content">
        <empty-tips v-if="hasNoPermission" :show-lock="true" v-bind="emptyTipsConfig"></empty-tips>
        <build-history-table
            v-else
            :show-log="showLog"
            :is-debug="isDebug"
        />
        <complete-log
            v-if="completeLogVisible"
            @close="hideCompleteLog"
            :execute-count="1"
            :exec-detail="execDetail"
        />
    </div>
</template>

<script>
    import BuildHistoryTable from '@/components/BuildHistoryTable/'
    import completeLog from '@/components/ExecDetail/completeLog.vue'
    import emptyTips from '@/components/devops/emptyTips'
    import {
        RESOURCE_ACTION,
        handlePipelineNoPermission
    } from '@/utils/permission'
    import { mapActions, mapGetters, mapState } from 'vuex'

    export default {
        name: 'build-history-tab',
        components: {
            BuildHistoryTable,
            completeLog,
            emptyTips
        },
        props: {
            isDebug: Boolean,
            pipelineName: {
                type: String,
                default: '--'
            }
        },
        data () {
            return {
                hasNoPermission: false,
                execDetail: {},
                completeLogVisible: false
            }
        },

        computed: {
            ...mapGetters({
                historyPageStatus: 'pipelines/getHistoryPageStatus',
                isReleasePipeline: 'atom/isReleasePipeline',
                isCurPipelineLocked: 'atom/isCurPipelineLocked'
            }),
            ...mapState('pipelines', [
                'executeStatus'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            emptyTipsConfig () {
                const { hasNoPermission } = this
                const title = hasNoPermission ? this.$t('noPermission') : this.$t('history.noBuildRecords')
                const desc = hasNoPermission ? this.$t('history.noPermissionTips') : this.$t('history.buildEmptyDesc')

                const btns = hasNoPermission
                    ? [{
                        theme: 'primary',
                        size: 'normal',
                        handler: this.changeProject,
                        text: this.$t('changeProject')
                    }, {
                        theme: 'success',
                        size: 'normal',
                        handler: this.toApplyPermission,
                        text: this.$t('applyPermission')
                    }]
                    : [{
                        theme: 'primary',
                        size: 'normal',
                        disabled: this.executeStatus,
                        loading: this.executeStatus,
                        handler: () => {
                            const params = this.$route.params
                            if (this.isReleasePipeline) {
                                return this.$router.push({
                                    name: 'pipelinesEdit',
                                    params
                                })
                            }
                            !this.executeStatus && !this.isCurPipelineLocked && this.$router.push({
                                name: 'executePreview',
                                query: {
                                    ...(this.isDebug ? { debug: '' } : {})
                                },
                                params

                            })
                        },
                        text: this.$t(this.isReleasePipeline ? 'goEdit' : 'history.startBuildTips')
                    }]
                return {
                    title,
                    desc,
                    btns
                }
            }
        },
        async mounted () {
            if (this.$route.hash) { // 带上buildId时，弹出日志弹窗
                const isBuildId = /^#b-+/.test(this.$route.hash) // 检查是否是合法的buildId
                isBuildId && this.showLog({
                    id: this.$route.hash.slice(1),
                    status: true
                })
            }
        },

        methods: {
            ...mapActions('pipelines', [
                'requestExecPipeline'
            ]),
            ...mapActions('atom', [
                'togglePropertyPanel'
            ]),

            changeProject () {
                this.$toggleProjectMenu(true)
            },
            async toApplyPermission () {
                try {
                    handlePipelineNoPermission({
                        projectId: this.projectId,
                        resourceCode: this.pipelineId,
                        action: RESOURCE_ACTION.VIEW
                    })
                } catch (e) {
                    console.error(e)
                }
            },

            showLog (buildRecord) {
                this.completeLogVisible = true
                this.execDetail = {
                    id: buildRecord.id,
                    status: buildRecord.status,
                    pipelineName: this.pipelineName
                }
            },
            hideCompleteLog () {
                this.completeLogVisible = false
                this.execDetail = {}
            }
        }
    }
</script>

<style lang="scss">
    .build-history-tab-content {
        height: 100%;
        overflow: hidden;
        padding: 24px;
        display: flex;
        flex-direction: column;
        align-items: stretch;
        .bk-sideslider-wrapper {
            top: 0;
            padding-bottom: 0;
             .bk-sideslider-content {
                height: calc(100% - 60px);
            }
        }
    }
</style>
