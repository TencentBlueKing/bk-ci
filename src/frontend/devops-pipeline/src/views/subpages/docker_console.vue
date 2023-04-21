<template>
    <section>
        <section v-if="!connectError" style="height: 100%" v-bkloading="{
            isLoading,
            title: loadingTitle
        }">
            <div class="console-header">
                <bk-button class="debug-btn" theme="danger" @click="stopDebug">{{ $t('editPage.docker.exitDebug') }}</bk-button>
                <p class="debug-tips" v-show="isRunning">{{ $t('editPage.docker.fromRunningTips') }}</p>
            </div>
            <div class="container">
                <my-terminal v-if="!isLoading" :url="url" :resize-url="resizeUrl" :exec-id="execId" :console-type="realDispatchType"></my-terminal>
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
    import Console from '@/components/atomFormField/Xterm/Console'
    import emptyTips from '@/components/pipelineList/imgEmptyTips'
    import { navConfirm } from '@/utils/util'

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
                    title: this.$t('editPage.docker.failTitle'),
                    desc: this.$t('editPage.docker.failDesc')
                },
                containerName: '',
                realDispatchType: ''
            }
        },
        computed: {
            isLoading () {
                return !this.url || this.isExiting
            },
            loadingTitle () {
                return !this.isExiting ? this.$t('editPage.docker.loadingTitle') : this.$t('editPage.docker.exiting')
            },
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.query.pipelineId
            },
            buildId () {
                return this.$route.query.buildId
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
            async linkConsole () {
                try {
                    const { projectId, pipelineId, buildId, vmSeqId, dispatchType } = this
                    const res = await this.$store.dispatch('common/startDebugDocker', {
                        projectId,
                        pipelineId,
                        buildId,
                        vmSeqId,
                        dispatchType
                    })
                    let { websocketUrl } = res
                    this.realDispatchType = res.dispatchType || this.dispatchType
                    if (this.realDispatchType === 'PUBLIC_BCS') {
                        websocketUrl = websocketUrl + '?hide_banner=true'
                    }
                    this.url = websocketUrl
                } catch (err) {
                    console.log(err)
                    this.connectError = true
                    this.config.desc = err.message || this.$t('editPage.docker.failDesc')
                }
            },
            async stopDebug () {
                const content = this.$t('editPage.docker.stopTips')

                navConfirm({ title: this.$t('editPage.docker.confirmStop'), content })
                    .then(async () => {
                        try {
                            this.isExiting = true
                            const { projectId, pipelineId, vmSeqId, realDispatchType } = this
                            await this.$store.dispatch('common/stopDebugDocker', { projectId, pipelineId, vmSeqId, dispatchType: realDispatchType })
                            this.$router.push({
                                name: 'pipelinesEdit',
                                params: {
                                    pipelineId: this.pipelineId
                                }
                            })
                        } catch (err) {
                            this.isExiting = false
                            this.$showTips({
                                theme: 'error',
                                message: err.message || err
                            })
                        }
                    }).catch(() => {})
            },
            addLeaveListenr () {
                window.addEventListener('beforeunload', this.leaveSure)
            },
            removeLeaveListenr () {
                window.removeEventListener('beforeunload', this.leaveSure)
            },
            leaveSure (e) {
                const dialogText = this.$t('editPage.confirmMsg')
                e.returnValue = dialogText
                return dialogText
            }
        }
    }
</script>

<style lang='scss'>
    @import './../../scss/conf';

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
