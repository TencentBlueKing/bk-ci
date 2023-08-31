<template>
    <div class="pipeline-execute-preview" v-bkloading="{ isLoading }">
        <component :is="stepComponent.is" v-bind="stepComponent.props" :is-debug="isDebugPipeline" />
    </div>
</template>

<script>
    import { mapActions, mapGetters, mapState } from 'vuex'
    import ExecuteParams from '@/components/Preview/ExecuteParams'
    import OptionalExecute from '@/components/Preview/OptionalExecute'
    import { bus } from '@/utils/bus'
    export default {
        components: {
            ExecuteParams,
            OptionalExecute
        },
        data () {
            return {
                isLoading: false,
                startupInfo: null,
                pipelineModel: null
            }
        },
        computed: {
            ...mapGetters('pipelines', [
                'getExecuteParams',
                'getSkipedAtomIds'
            ]),
            ...mapState('pipelines', ['executeStep']),
            isDebugPipeline () {
                return Object.prototype.hasOwnProperty.call(this.$route.query, 'debug')
            },
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            stepComponent () {
                switch (this.executeStep) {
                    case 2:
                        return {
                            is: OptionalExecute,
                            props: {
                                canElementSkip: this.startupInfo?.canElementSkip ?? false,
                                pipeline: this.pipelineModel
                            }
                        }
                    case 1:
                    default:
                        return {
                            is: ExecuteParams,
                            props: {
                                startupInfo: this.startupInfo
                            }
                        }
                }
            }
        },
        mounted () {
            this.init()
            bus.$off('start-execute')
            bus.$on('start-execute', this.executePipeline)
        },
        beforeDestroy () {
            bus.$off('start-execute', this.executePipeline)
            this.togglePropertyPanel({
                isShow: false
            })
            setTimeout(() => {
                this.resetExecuteConfig(this.pipelineId)
            }, 0)
        },
        methods: {
            ...mapActions('atom', [
                'togglePropertyPanel',
                'fetchPipelineByVersion',
                'setPipeline',
                'setPipelineEditing'
            ]),
            ...mapActions('pipelines', [
                'requestStartupInfo',
                'requestExecPipeline',
                'setExecuteStatus',
                'resetExecuteConfig'
            ]),
            async init () {
                try {
                    this.isLoading = true
                    const [res, pipelineModel] = await Promise.all([
                        this.requestStartupInfo({
                            projectId: this.projectId,
                            pipelineId: this.pipelineId
                        }),
                        this.fetchPipelineByVersion(this.$route.params)
                    ])
                    this.pipelineModel = pipelineModel
                    this.startupInfo = res
                } catch (err) {
                    this.handleError(err, [{
                        actionId: this.$permissionActionMap.execute,
                        resourceId: this.$permissionResourceMap.pipeline,
                        instanceId: [{
                            id: this.pipelineId,
                            name: this.pipelineId
                        }],
                        projectId: this.projectId
                    }])
                } finally {
                    this.isLoading = false
                }
            },
            async executePipeline () {
                let message, theme
                const params = this.getExecuteParams(this.pipelineId)
                const skipAtoms = this.getSkipedAtomIds(this.pipelineId)
                console.log(params, skipAtoms)
                try {
                    this.setExecuteStatus(true)
                    // 请求执行构建
                    const res = await this.requestExecPipeline({
                        projectId: this.projectId,
                        pipelineId: this.pipelineId,
                        params: {
                            ...skipAtoms,
                            ...params
                        }
                    })

                    if (res && res.id) {
                        message = this.$t('newlist.sucToStartBuild')
                        theme = 'success'

                        this.$router.push({
                            name: 'pipelinesDetail',
                            params: {
                                projectId: this.projectId,
                                pipelineId: this.pipelineId,
                                buildNo: res.id
                            }
                        })
                    } else {
                        message = this.$t('newlist.failToStartBuild')
                        theme = 'error'
                    }
                } catch (err) {
                    this.handleError(err, [{
                        actionId: this.$permissionActionMap.execute,
                        resourceId: this.$permissionResourceMap.pipeline,
                        instanceId: [{
                            id: this.pipelineId,
                            name: this.pipelineInfo?.pipelineName ?? '--'
                        }],
                        projectId: this.projectId
                    }])
                } finally {
                    this.setExecuteStatus(false)

                    message && this.$showTips({
                        message,
                        theme
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
    .pipeline-execute-preview {
        height: 100%;
        width: 100%;
        display: flex;
        flex-direction: column;
    }
</style>
