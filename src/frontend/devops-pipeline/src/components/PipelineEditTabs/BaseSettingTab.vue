
<template>
    <vertical-tab :tabs="tabs"></vertical-tab>
</template>

<script>
    import VerticalTab from './VerticalTab'

    export default {
        name: 'base-setting-tab',
        components: {
            VerticalTab
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
        }
    }
</script>
