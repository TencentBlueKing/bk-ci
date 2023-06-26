<template>
    <accordion show-checkbox show-content key="otherChoice">
        <header class="var-header" slot="header">
            <span>{{ $t('editPage.jobOption') }}</span>
            <i class="devops-icon icon-angle-down" style="display:block"></i>
        </header>
        <div slot="content" class="bk-form bk-form-vertical">
            <template v-for="(obj, key) in optionModel">
                <form-field :key="key" v-if="!isHidden(obj, container)" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                    <component :is="obj.component" :set-parent-validate="setKeyValueValidate" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdateJobOption" :value="jobOption[key]" :disabled="disabled" v-bind="obj"></component>
                </form-field>
            </template>
        </div>
    </accordion>
</template>

<script>
    import atomMixin from '@/components/AtomPropertyPanel/atomMixin'
    import validMixins from '@/components/validMixins'
    import jobOptionConfigMixin from '@/store/modules/common/jobOptionConfigMixin'
    import Vue from 'vue'
    import { mapActions } from 'vuex'
    export default {
        name: 'job-config',
        mixins: [atomMixin, validMixins, jobOptionConfigMixin],
        props: {
            jobOption: {
                type: Object,
                default: () => ({})
            },
            disabled: {
                type: Boolean,
                default: false
            },
            updateContainerParams: {
                type: Function,
                required: true
            },
            stage: {
                type: Array,
                default: () => ({})
            },
            stageIndex: Number,
            containerIndex: Number
        },
        computed: {
            optionModel () {
                return this.JOB_OPTION || {}
            },
            container () {
                return this.stage.containers[this.containerIndex] || {}
            }
        },
        created () {
            console.log(this.optionModel, 'optionModel')
            if (!this.disabled) {
                this.initOptionConfig()
            }
        },
        methods: {
            ...mapActions('atom', [
                'setPipelineEditing'
            ]),
            handleUpdateJobOption (name, value) {
                this.setPipelineEditing(true)
                this.updateContainerParams('jobControlOption',
                                           Object.assign(this.jobOption || {}, { [name]: value })
                )
            },
            initOptionConfig () {
                if (this.jobOption === undefined || JSON.stringify(this.jobOption) === '{}') {
                    this.updateContainerParams('jobControlOption', this.getJobOptionDefault())
                } else {
                    if (this.jobOption && this.jobOption.dependOnType === undefined) {
                        Vue.set(this.jobOption, 'dependOnType', 'ID')
                        this.handleUpdateJobOption('dependOnId', [])
                    }
                    if (this.jobOption && this.jobOption.prepareTimeout === undefined) {
                        Vue.set(this.jobOption, 'prepareTimeout', '10')
                    }
                }
            },
            setKeyValueValidate (addErrors, removeErrors) {
                this.$emit('setKeyValueValidate', addErrors, removeErrors)
            }
        }
    }
</script>
