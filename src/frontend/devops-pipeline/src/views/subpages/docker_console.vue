<template>
    <section>
        <section v-if="!connectError" style="height: 100%" v-bkloading="{
            isLoading,
            title: $t('editPage.docker.loadingTitle')
        }">
            <div class="console-header">
                <bk-button class="debug-btn" theme="danger" @click="stopDebugDocker">{{ $t('editPage.docker.exitDebug') }}</bk-button>
                <p class="debug-tips" v-show="isRunning">{{ $t('editPage.docker.fromRunningTips') }}</p>
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
                config: {
                    title: this.$t('editPage.docker.failTitle'),
                    desc: this.$t('editPage.docker.failDesc')
                }
            }
        },
        computed: {
            isLoading () {
                return !this.url
            },
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.query.pipelineId
            },
            targetIp () {
                return this.$route.query.targetIp
            },
            vmSeqId () {
                return this.$route.query.vmSeqId
            },
            containerId () {
                return this.$route.query.containerId
            }
        },
        async created () {
            if (this.targetIp && this.pipelineId && this.containerId) {
                this.isRunning = true
                this.getLinkDetail(this.containerId, this.targetIp)
                // this.url = `ws://${PROXY_URL_PREFIX}/docker-console?pipelineId=${this.pipelineId}&containerId=${this.containerId}&projectId=${this.projectId}&targetIP=${this.targetIp}`
            } else {
                await this.getContainerInfo()
            }
        },
        mounted () {
            this.addLeaveListenr()
        },
        beforeDestroy () {
            this.removeLeaveListenr()
        },
        methods: {
            async getContainerInfo () {
                clearTimeout(this.timer)
                try {
                    const res = await this.$store.dispatch('soda/getContainerInfo', {
                        projectId: this.projectId,
                        pipelineId: this.pipelineId,
                        vmSeqId: this.vmSeqId
                    })
                    if (res && res.status === 2 && res.containerId && res.address) {
                        this.getLinkDetail(res.containerId, res.address)
                        // this.url = `ws://${PROXY_URL_PREFIX}/docker-console?pipelineId=${this.pipelineId}&containerId=${res.containerId}&projectId=${this.projectId}&targetIP=${res.address}`
                    } else {
                        this.timer = setTimeout(async () => {
                            await this.getContainerInfo()
                        }, 5000)
                    }
                } catch (err) {
                    console.log(err)
                    if (err && err.code === 1) {
                        this.connectError = true
                        this.config.desc = err.message || this.$t('editPage.docker.failDesc')
                    } else {
                        this.$showTips({
                            theme: 'error',
                            message: err.message || err
                        })
                    }
                }
            },
            async stopDebugDocker () {
                const content = this.$t('editPage.docker.stopTips')

                navConfirm({ title: this.$t('editPage.docker.confirmStop'), content })
                    .then(async () => {
                        try {
                            await this.$store.dispatch('soda/stopDebugDocker', {
                                projectId: this.projectId,
                                pipelineId: this.pipelineId,
                                vmSeqId: this.vmSeqId
                            })
                            this.$router.push({
                                name: 'pipelinesEdit',
                                params: {
                                    pipelineId: this.pipelineId
                                }
                            })
                        } catch (err) {
                            this.$showTips({
                                theme: 'error',
                                message: err.message || err
                            })
                        }
                    }).catch(() => {})
            },
            async getLinkDetail (containerId, targetIp) {
                if (!containerId || !targetIp) {
                    this.showTips({
                        theme: 'error',
                        message: this.$t('editPage.docker.abnormalParams')
                    })
                }
                try {
                    const execId = await this.$store.dispatch('soda/getDockerExecId', {
                        targetIp,
                        containerId,
                        projectId: this.projectId,
                        pipelineId: this.pipelineId,
                        cmd: ['/bin/bash']
                    })
                    this.execId = execId
                    this.resizeUrl = `docker-console-resize?pipelineId=${this.pipelineId}&projectId=${this.projectId}&targetIp=${targetIp}`
                    this.url = `ws://${PROXY_URL_PREFIX}/docker-console-new?eventId=${execId}&pipelineId=${this.pipelineId}&projectId=${this.projectId}&targetIP=${targetIp}&containerId=${containerId}`
                } catch (err) {
                    this.$showTips(err.message || err)
                }
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
