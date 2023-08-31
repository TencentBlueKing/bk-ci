
<template>
    <section>
        <base-info
            :pipeline-setting="pipelineSetting"
            :handle-base-info-change="handleBaseInfoChange">
        </base-info>
        <running-lock
            :pipeline-setting="pipelineSetting"
            :handle-running-lock-change="handleRunningLockChange"
            style="margin-top: 24px;">
        </running-lock>
    </section>
</template>

<script>
    import BaseInfo from '@/components/pipelineSetting/BaseInfo'
    import RunningLock from '@/components/pipelineSetting/RunningLock'

    export default {
        name: 'base-setting-tab',
        components: {
            BaseInfo,
            RunningLock
        },
        props: {
            pipelineSetting: Object,
            updatePipelineSetting: Function
        },
        computed: {
            tabs () {
                return [{
                    id: 'baseInfo',
                    name: this.$t('settings.baseInfo'),
                    component: 'BaseInfo',
                    componentProps: {
                        pipelineSetting: this.pipelineSetting,
                        handleBaseInfoChange: (name, value) => {
                            this.updatePipelineSetting({
                                container: this.pipelineSetting,
                                param: {
                                    [name]: value
                                }
                            })
                        }
                    }
                }, {
                    id: 'runningLock',
                    name: this.$t('settings.runLock'),
                    component: 'RunningLock',
                    componentProps: {
                        pipelineSetting: this.pipelineSetting,
                        handleRunningLockChange: (param) => {
                            this.updatePipelineSetting({
                                container: this.pipelineSetting,
                                param
                            })
                        }
                    }
                }
                ]
            }
        },
        methods: {
            handleBaseInfoChange (name, value) {
                this.updatePipelineSetting({
                    container: this.pipelineSetting,
                    param: {
                        [name]: value
                    }
                })
            },
            handleRunningLockChange (param) {
                this.updatePipelineSetting({
                    container: this.pipelineSetting,
                    param
                })
            }
        }
    }
</script>

<style lang="scss">
    .pipeline-setting-title {
        font-size: 14px;
        font-weight: bold;
        border-bottom: 1px solid #DCDEE5;
        padding-bottom: 4px;
        margin-bottom: 16px;
    }
</style>
