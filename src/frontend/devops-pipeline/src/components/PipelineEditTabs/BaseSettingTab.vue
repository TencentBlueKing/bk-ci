
<template>
    <section>
        <base-info
            :pipeline-setting="pipelineSetting"
            :editable="editable"
            :handle-base-info-change="handleBaseInfoChange">
        </base-info>
        <running-lock
            :pipeline-setting="pipelineSetting"
            :editable="editable"
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
            editable: {
                type: Boolean,
                default: true
            },
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
                        pipelineSetting: this.pipelineSetting
                    }
                }, {
                    id: 'runningLock',
                    name: this.$t('settings.runLock'),
                    component: 'RunningLock',
                    componentProps: {
                        pipelineSetting: this.pipelineSetting
                    }
                }
                ]
            }
        },
        methods: {
            handleBaseInfoChange (name, value) {
                this.updatePipelineSetting({
                    setting: this.pipelineSetting,
                    param: {
                        [name]: value
                    }
                })
            },
            handleRunningLockChange (param) {
                this.updatePipelineSetting({
                    setting: this.pipelineSetting,
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
