<template>
    <selector
        name="performanceConfigId"
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
            }
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
                'fetchDevcloudSettings'
            ]),
            handleSelect (name, value) {
                this.handleChange(name, value)
            },
            getShowOption (obj) {
                return `${obj.description} (${obj.cpu}${this.$t('editPage.cpuUnit')}/${obj.memory}/${obj.disk})`
            },
            async getData () {
                try {
                    this.isLoading = true
                    const res = await this.fetchDevcloudSettings({ projectId: this.projectId, buildType: this.buildType })
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
