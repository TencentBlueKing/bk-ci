<template>
    <selector
        :name="fieldName"
        :disabled="disabled"
        :is-loading="isLoading"
        :handle-change="handleSelect"
        :list="optionList"
        :value="selectValue"
    >
    </selector>
</template>

<script>
    import { mapActions } from 'vuex'
    import Selector from '@/components/atomFormField/Selector'

    export default {
        name: 'devcloud-option',
        components: {
            Selector
        },
        props: {
            buildType: {
                type: String,
                default: 'DEVCLOUD'
            },
            value: {
                type: String,
                default: ''
            },
            disabled: {
                type: Boolean,
                default: false
            },
            handleChange: {
                type: Function,
                required: false
            },
            templateId: {
                type: String,
                default: ''
            },
            changeShowPerformance: {
                type: Function,
                required: false
            }
        },
        data () {
            return {
                selectValue: '',
                isLoading: false,
                optionList: []
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            fieldName () {
                return this.buildType === 'PUBLIC_DEVCLOUD' ? 'performanceUid' : 'performanceConfigId'
            },
            // 是否是流水线编辑页
            isEditPage () {
                return this.$route.meta.edit
            },
        },
        watch: {
            buildType (v) {
                this.selectValue = ''
                this.$nextTick(() => {
                    this.getData()
                })
            }
        },
        async created () {
            this.selectValue = this.value
            this.getData()
        },
        methods: {
            ...mapActions('atom', [
                'fetchDevcloudSettings',
                'getHistoryDevcloudSettings',
                'fetchDockerSettings'
            ]),
            handleSelect (name, value) {
                this.handleChange(name, value)
            },
            getShowOption (obj) {
                return `${obj.description} (${obj.cpu}${this.$t('editPage.cpuUnit')}/${obj.memory}/${obj.disk})`
            },
            // 流水线编辑页面PUBLIC_DEVCLOUD类型下的机型列表
            async getEditPageDeviceList () {
                const res = await this.fetchDevcloudSettings({
                    projectId: this.projectId,
                    pipelineId: this.pipelineId,
                    templateId: this.templateId
                })
                this.changeShowPerformance(true)
                this.selectValue = this.value || res.data.defaultUid

                this.optionList = res.data.performanceList.map(i => ({
                    ...i,
                    id: i.uid
                })) || []
                const hasPermission = this.optionList.find(i => i.id === this.selectValue)
                if (!hasPermission) {
                    this.optionList.splice(0, 0, {
                        id: this.value,
                        name: this.$t('editPage.withoutOption')
                    })
                }
            },
            // 流水线查看页面PUBLIC_DEVCLOUD类型下的机型列表
            async getViewPageDeviceList () {
                const res = await this.getHistoryDevcloudSettings({
                    projectId: this.projectId,
                    uid: this.value
                })
                this.changeShowPerformance(true)
                this.selectValue = this.value || res.data.uid
                this.optionList = [res.data].map(i => ({
                    ...i,
                    id: i.uid
                })) || []
            },
            // 其它构件资源类型下的机型列表
            async getOtherDockerSettings () {
                const res = await this.fetchDockerSettings({ projectId: this.projectId, buildType: this.buildType })
                const needShow = res.data.needShow || false
                this.changeShowPerformance(needShow)
                if (needShow) {
                    this.selectValue = this.value || res.data.default
                }
                this.optionList = res.data.dockerResourceOptionsMaps || []
                this.optionList = this.optionList.map(item => {
                    return {
                        ...item,
                        name: this.getShowOption(item.dockerResourceOptionsShow)
                    }
                })
            },
            async getData () {
                try {
                    this.isLoading = true
                    if (this.buildType === 'PUBLIC_DEVCLOUD') {
                        if (this.isEditPage) {
                            await this.getEditPageDeviceList()
                        } else {
                            await this.getViewPageDeviceList()
                        }
                    } else {
                        await this.getOtherDockerSettings()
                    }
                } catch (err) {
                    this.$showTips({
                        theme: 'error',
                        message: err.message || err
                    })
                }
                this.isLoading = false
            }
        }
    }
</script>
