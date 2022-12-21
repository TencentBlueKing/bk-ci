<template>
    <section>
        <section v-if="!connectError" style="height: 100%" v-bkloading="{
            isLoading,
            title: loadingTitle
        }">
            <div class="console-header">
                <bk-button class="debug-btn" theme="danger" @click="stopDebug">{{$t('pipeline.stopDebug')}}</bk-button>
                <p class="debug-tips" v-show="isRunning">{{ fromRunningTips }}</p>
            </div>
            <div class="container">
                <my-terminal v-if="!isLoading" :url="url" :resize-url="resizeUrl" :exec-id="execId"></my-terminal>
            </div>
            <div class="footer"></div>
        </section>
        <empty-tips
            v-else
            :title="config.title"
            :desc="config.desc"
            :btns="config.btns">
        </empty-tips>
    </section>
</template>

<script>
    import { mapState } from 'vuex'
    import { pipelines } from '@/http'
    import Console from '@/components/Xterm/Console'
    import emptyTips from '@/components/empty-tips'

    export default {
        name: 'WebSSH',
        components: {
            'my-terminal': Console,
            emptyTips
        },
        data () {
            return {
                connectError: false,
                timer: null,
                url: '',
                resizeUrl: '',
                execId: '',
                isRunning: false,
                isExiting: false,
                config: {
                    title: this.$t('pipeline.debugFailTitle'),
                    desc: this.$t('pipeline.debugFailDesc')
                },
                containerName: '',
                fromRunningTips: this.$t('pipeline.debugRunningTips')
            }
        },
        computed: {
            ...mapState(['projectId']),
            isLoading () {
                return !this.url
            },
            loadingTitle () {
                return !this.isExiting ? this.$t('pipeline.debugLoadingTitle') : 'exiting'
            },
            pipelineId () {
                return this.$route.query.pipelineId
            },
            buildId () {
                return this.$route.query.buildId || null
            },
            vmSeqId () {
                return this.$route.query.vmSeqId
            },
            dispatchType () {
                return this.$route.query.dispatchType
            }
        },
        async created () {
            this.linkConsole()
        },
        mounted () {
            this.addLeaveListenr()
        },
        beforeDestroy () {
            this.removeLeaveListenr()
        },
        methods: {
            linkConsole () {
                const { projectId, pipelineId, buildId, vmSeqId, dispatchType } = this
                pipelines.startDebugDocker(
                    {
                        projectId,
                        pipelineId,
                        buildId,
                        vmSeqId,
                        dispatchType
                    }
                ).then((res) => {
                    this.url = res.websocketUrl || ''
                }).catch((err) => {
                    console.log(err)
                    this.connectError = true
                    this.config.desc = err.message || this.$t('editPage.docker.failDesc')
                })
            },
            async stopDebug () {
                const content = this.$t('pipeline.stopDebugTips')

                this.$bkInfo({
                    title: this.$t('pipeline.stopDebugTitle'),
                    subTitle: content,
                    confirmLoading: true,
                    confirmFn: async () => {
                        try {
                            await pipelines.stopDebugDocker(this.projectId, this.pipelineId, this.vmSeqId, this.dispatchType)
                            this.$router.push({
                                name: 'pipelineDetail',
                                params: {
                                    pipelineId: this.pipelineId,
                                    buildId: this.buildId
                                }
                            })
                            return true
                        } catch (err) {
                            this.isExiting = false
                            this.$bkMessage({
                                theme: 'error',
                                message: err.message || err
                            })
                            return false
                        }
                    }
                })
            },
            addLeaveListenr () {
                window.addEventListener('beforeunload', this.leaveSure)
            },
            removeLeaveListenr () {
                window.removeEventListener('beforeunload', this.leaveSure)
            },
            leaveSure (e) {
                const dialogText = this.$t('pipeline.debugLeaveTips')
                e.returnValue = dialogText
                return dialogText
            }
        }
    }
</script>

<style lang='postcss'>
    @import '@/css/conf';

    .console-header {
        height: 60px;
        line-height: 60px;
        background-color: #000;
        .debug-btn {
            float: right;
            margin: 12px 20px;
        }
        .debug-tips {
            float: right;
            font-size: 14px;
        }
    }
    .container {
        height: calc(100% - 160px);
    }
    .footer {
        height: 100px;
        background-color: #000;
    }
</style>
