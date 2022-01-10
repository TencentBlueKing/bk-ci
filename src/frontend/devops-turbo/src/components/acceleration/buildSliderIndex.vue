<template>
    <div>
        <build-slider-public v-if="machineType === '1'" :task-info="taskInfo" :env-list="envList" @buildSetting="buildSetting" @configCancel="configCancel"></build-slider-public>
        <build-slider-third v-if="machineType === '2'" :task-info="taskInfo" :env-list="envList" @buildSetting="buildSetting" @configCancel="configCancel"></build-slider-third>
        <build-slider-install v-if="machineType === '3'" :task-info="taskInfo" :env-list="envList" @buildSetting="buildSetting" @configCancel="configCancel"></build-slider-install>
    </div>
</template>
<script>
    import buildSliderPublic from '@/components/acceleration/buildSliderPublic'
    import buildSliderThird from '@/components/acceleration/buildSliderThird'
    import buildSliderInstall from '@/components/acceleration/buildSliderInstall'

    export default {
        name: 'build-slider-index',
        components: {
            buildSliderPublic,
            buildSliderThird,
            buildSliderInstall
        },
        props: {
            task: {
                type: Object,
                default () {
                    return {
                        taskId: '',
                        taskName: ''
                    }
                }
            }
        },
        data () {
            return {
                machineType: '', // 1 2
                taskInfo: {},
                envList: []
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        created () {
            this.requestTaskInfo()
            this.getCompilerConfig()
        },
        methods: {
            async requestTaskInfo () {
                try {
                    const res = await this.$store.dispatch('turbo/requestTaskInfo', {
                        taskId: this.task.taskId
                    })
                    if (res) {
                        this.taskInfo = Object.assign({}, res)
                        this.machineType = res.machineType
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                }
            },
            buildSetting (taskParams) {
                this.$emit('buildSetting', taskParams)
            },
            toggleSlider () {
                this.$parent.$parent.toggleSlider()
            },
            configCancel () {
                this.$emit('toggleSlider')
            },
            // 获取编译器信息
            async getCompilerConfig () {
                const { projectId, $store } = this
                try {
                    const res = await $store.dispatch('turbo/getCompilerConfig', {
                        projectId: projectId
                    })
                    if (res) {
                        const envList = []
                        for (let i = 0; i < res.length; i++) {
                            const envListItem = {}
                            const gccVersion = []
                            const name = res[i]['paramCode']
                            const gccVersionList = res[i]['paramExtend1'].split(';')
                            for (let j = 0; j < gccVersionList.length; j++) {
                                const gccVersionItem = {}
                                gccVersionItem.name = gccVersionList[j]
                                gccVersionItem.version = name + gccVersionList[j]
                                gccVersion.push(gccVersionItem)
                            }
                            envListItem.name = name
                            envListItem.gccVersion = gccVersion
                            envList.push(envListItem)
                        }
                        console.log(this, this.envList)
                        this.envList = envList
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                }
            }
        }
    }
</script>
<style lang="scss">
    .bk-form-content.form-average-two-small .bk-form-inline {
        width: 184px;
        .text {
            width: 100%;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        &+.bk-form-inline {
            margin-left: 8px;
        }
    }
    .form-average-two-small .bk-form-inline {
        width: 184px;
        .text {
            width: 100%;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        &+.bk-form-inline {
            margin-left: 8px;
        }
    }
</style>
