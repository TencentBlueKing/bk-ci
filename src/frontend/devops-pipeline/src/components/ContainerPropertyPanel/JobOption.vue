<template>
    <accordion show-checkbox show-content key="otherChoice">
        <header class="var-header" slot="header">
            <span>流程控制选项</span>
            <i class="bk-icon icon-angle-down" style="display:block"></i>
        </header>
        <div slot="content" class="bk-form bk-form-vertical">
            <template v-for="(obj, key) in optionModel">
                <form-field :key="key" v-if="!isHidden(obj, jobOption)" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                    <component :is="obj.component" :set-parent-validate="setKeyValueValidate" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdateJobOption" :value="jobOption[key]" :disabled="disabled" v-bind="obj"></component>
                </form-field>
            </template>
        </div>
    </accordion>
</template>

<script>
    import { mapActions } from 'vuex'
    import atomMixin from '@/components/AtomPropertyPanel/atomMixin'
    import validMixins from '@/components/validMixins'
    import {
        getJobOptionDefault,
        JOB_OPTION
    } from '@/store/modules/soda/jobOptionConfig'
    export default {
        name: 'job-config',
        mixins: [atomMixin, validMixins],
        props: {
            jobOption: {
                type: Object,
                default: {}
            },
            disabled: {
                type: Boolean,
                default: false
            },
            updateContainerParams: {
                type: Function,
                required: true
            }
        },
        computed: {
            optionModel () {
                return JOB_OPTION || {}
            }
        },
        created () {
            if (!this.disabled) {
                this.initOptionConfig()
            }
        },
        methods: {
            ...mapActions('atom', [
                'setPipelineEditing'
            ]),
            getJobOptionDefault,
            handleUpdateJobOption (name, value) {
                this.setPipelineEditing(true)
                this.updateContainerParams('jobControlOption',
                                           Object.assign(this.jobOption || {}, { [name]: value })
                )
            },
            initOptionConfig () {
                if (this.jobOption === undefined || JSON.stringify(this.jobOption) === '{}') {
                    this.updateContainerParams('jobControlOption', this.getJobOptionDefault())
                }
            },
            setKeyValueValidate (addErrors, removeErrors) {
                this.$emit('setKeyValueValidate', addErrors, removeErrors)
            }
        }
    }
</script>
