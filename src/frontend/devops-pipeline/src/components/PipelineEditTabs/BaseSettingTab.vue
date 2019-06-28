
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
                    name: '基本信息',
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
                    name: '运行锁定',
                    component: 'RunningLock',
                    componentProps: {
                        pipelineSetting: this.pipelineSetting,
                        handleRunningLockChange: (name, value) => {
                            this.updatePipelineSetting({
                                container: this.pipelineSetting,
                                param: {
                                    [name]: value
                                }
                            })
                        }
                    }
                }
                // {
                //     id: 'cleanPolicy',
                //     name: '清理策略',
                //     component: CleanPolicy,
                //     componentProps: {
                //         pipelineSetting: this.pipelineSetting,
                //         updatePipelineSetting: this.updatePipelineSetting
                //     }
                // }
                ]
            }
        }
    }
</script>
