<template>
    <section>
        <section v-if="!connectError" style="height: 100%" v-bkloading="{
            isLoading,
            title: loadingTitle
        }">
            <div class="console-header">
                <bk-button class="debug-btn" theme="danger" @click="stopDebug">Stop Debug</bk-button>
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
                    title: 'Failed in login to debug',
                    desc: 'Illegal name for mirroring or other errors'
                },
                containerName: '',
                fromRunningTips: 'Go to the current debugging through the portal of pipeline at runtime and the container for debugging is the one used for the current build'
            }
        },
        computed: {
            ...mapState(['projectId']),
            isLoading () {
                return !this.url
            },
            loadingTitle () {
                return !this.isExiting ? `If the container can't be activated after one minute you did so, please contact DevOps (customer service from Blue Shield)` : 'exiting'
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
            cmd () {
                return this.$route.query.cmd
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
                const { projectId, pipelineId, buildId, vmSeqId, cmd } = this
                pipelines.startDebugDocker(
                {
                    projectId,
                    pipelineId,
                    buildId,
                    vmSeqId,
                    cmd  
                }).then((res) => {
                    this.url = res
                })
                .catch((err) => {
                    console.log(err)
                        this.connectError = true
                        this.config.desc = err.message || this.$t('editPage.docker.failDesc')
                })
            },
            async stopDebug () {
                const content = 'You will leave this page after stop debugging sucessfully'

                this.$bkInfo({
                    title: 'Confirm stopping debugging',
                    subTitle: content,
                    confirmLoading: true,
                    confirmFn: async () => {
                        try {
                            await pipelines.stopDebugDocker(this.projectId, this.pipelineId, this.vmSeqId)
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
                const dialogText = 'If leave, the data of new edition will be lost'
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
