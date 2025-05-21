<template>
    <accordion
        show-checkbox
        show-content
        key="otherChoice"
    >
        <header
            class="var-header"
            slot="header"
        >
            <span>{{ $t('editPage.jobOption') }}</span>
            <i
                class="devops-icon icon-angle-down"
                style="display:block"
            ></i>
        </header>
        <div
            slot="content"
            class="bk-form bk-form-vertical"
        >
            <template v-for="(obj, key) in optionModel">
                <template v-if="obj.type === 'group'">
                    <form-field-group
                        v-if="!isHidden(obj, container)"
                        :key="key"
                        v-bind="obj"
                    >
                        <template v-for="i in obj.children">
                            <form-field
                                :key="i.key"
                                v-if="!isHidden(i, container)"
                                v-bind="i"
                                :is-error="errors.has(i.key)"
                                :error-msg="errors.first(i.key)"
                            >
                                <component
                                    :is="i.component"
                                    :set-parent-validate="setKeyValueValidate"
                                    :name="i.key"
                                    v-validate.initial="Object.assign({}, i.rule, { required: !!i.required })"
                                    :handle-change="handleUpdateJobOption"
                                    :value="jobOption[i.key]"
                                    :disabled="disabled"
                                    v-bind="i"
                                ></component>
                            </form-field>
                        </template>
                    </form-field-group>
                </template>
                <template v-else>
                    <form-field
                        :key="key"
                        v-if="!isHidden(obj, container)"
                        :desc="obj.desc"
                        :required="obj.required"
                        :label="obj.label"
                        :docs-link="obj.docsLink"
                        :is-error="errors.has(key)"
                        :error-msg="errors.first(key)"
                    >
                        <component
                            :is="obj.component"
                            :set-parent-validate="setKeyValueValidate"
                            :name="key"
                            v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })"
                            :handle-change="handleUpdateJobOption"
                            :value="jobOption[key]"
                            :disabled="disabled"
                            v-bind="obj"
                        ></component>
                    </form-field>
                </template>
            </template>
        </div>
    </accordion>
</template>

<script>
    import atomMixin from '@/components/AtomPropertyPanel/atomMixin'
    import validMixins from '@/components/validMixins'
    import jobOptionConfigMixin from '@/store/modules/common/jobOptionConfigMixin'
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
                let clearFields = {}
                
                if (
                    value === this.JOB_OPTION[name]?.clearValue
                    && Array.isArray(this.JOB_OPTION[name]?.clearFields)
                ) {
                    // 重置关联的值，可配置相关的联动值
                    clearFields = this.JOB_OPTION[name].clearFields.reduce((acc, key) => {
                        acc[key] = this.getFieldDefault(key, this.JOB_OPTION)
                        return acc
                    }, {})
                }
                
                this.updateContainerParams(
                    'jobControlOption',
                    Object.assign(
                        (this.jobOption || {}),
                        {
                            [name]: value,
                            ...clearFields
                        }
                    )
                )
            },
            initOptionConfig () {
                this.updateContainerParams('jobControlOption', this.getJobOptionDefault(this.JOB_OPTION, this.jobOption))
            },
            setKeyValueValidate (addErrors, removeErrors) {
                this.$emit('setKeyValueValidate', addErrors, removeErrors)
            }
        }
    }
</script>
