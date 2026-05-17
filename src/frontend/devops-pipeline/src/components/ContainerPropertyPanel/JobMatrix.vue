<template>
    <accordion
        show-checkbox
        :show-content="enableMatrix"
        key="otherChoice"
        is-version="true"
    >
        <header
            class="var-header"
            slot="header"
        >
            <span>{{ $t('editPage.enableMatrix') }}</span>
            <input
                class="accordion-checkbox"
                :disabled="disabled"
                :checked="enableMatrix"
                type="checkbox"
                @click.stop
                @change="toggleMatrix"
            />
        </header>
        <div
            slot="content"
            class="bk-form bk-form-vertical"
            v-if="enableMatrix"
        >
            <template v-for="(obj, key) in optionModel">
                <form-field
                    :key="key"
                    :desc="obj.desc"
                    :required="isMatrixRuleField(key) ? false : obj.required"
                    :label="obj.label"
                    :is-error="errors.has(key)"
                    :error-msg="errors.first(key)"
                >
                    <component
                        :is="obj.component"
                        :name="key"
                        v-validate.initial="getValidateRule(key, obj)"
                        :handle-change="handleUpdateJobMatrix"
                        :value="normalizedMatrixControlOption[key]"
                        :disabled="disabled"
                        v-bind="obj"
                    />
                </form-field>
            </template>
        </div>
    </accordion>
</template>

<script>
    import atomMixin from '@/components/AtomPropertyPanel/atomMixin'
    import validMixins from '@/components/validMixins'
    import jobOptionConfigMixin from '@/store/modules/common/jobOptionConfigMixin'
    import { hasAnyMatrixRuleValue } from '@/utils/matrixRule'
    import { mapActions } from 'vuex'

    const MATRIX_RULE_FIELDS = ['strategyStr', 'includeCaseStr', 'excludeCaseStr']
    const MATRIX_REQUIRED_ERROR_FIELD = 'matrixRuleRequired'

    export default {
        name: 'job-mutual',
        mixins: [atomMixin, validMixins, jobOptionConfigMixin],
        props: {
            enableMatrix: {
                type: Boolean,
                default: false
            },
            matrixControlOption: {
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
            }
        },
        computed: {
            optionModel () {
                return this.JOB_MATRIX || {}
            },
            normalizedMatrixControlOption () {
                const matrixControlOption = this.matrixControlOption || {}

                return {
                    ...matrixControlOption,
                    strategyStr: matrixControlOption.strategyStr ?? matrixControlOption.strategy ?? '',
                    includeCaseStr: matrixControlOption.includeCaseStr ?? matrixControlOption.include ?? '',
                    excludeCaseStr: matrixControlOption.excludeCaseStr ?? matrixControlOption.exclude ?? ''
                }
            },
            matrixRuleValues () {
                const {
                    strategyStr = '',
                    includeCaseStr = '',
                    excludeCaseStr = ''
                } = this.normalizedMatrixControlOption

                return {
                    strategyStr,
                    includeCaseStr,
                    excludeCaseStr
                }
            }
        },
        watch: {
            enableMatrix (enable) {
                if (enable) {
                    this.validateMatrixFields()
                    return
                }
                this.clearMatrixFieldErrors()
            },
            matrixControlOption: {
                deep: true,
                handler () {
                    if (!this.disabled) {
                        this.initOptionConfig()
                    }
                    this.syncMatrixRequiredStatus()
                }
            }
        },
        created () {
            if (!this.disabled) {
                this.initOptionConfig()
            }
            this.syncMatrixRequiredStatus()
        },
        methods: {
            ...mapActions('atom', [
                'setPipelineEditing'
            ]),
            isMatrixRuleField (name) {
                return MATRIX_RULE_FIELDS.includes(name)
            },
            getValidateRule (name, option) {
                return Object.assign(
                    {},
                    option.rule,
                    this.isMatrixRuleField(name)
                        ? { atLeastNotEmpty: this.matrixRuleValues }
                        : {},
                    { required: this.isMatrixRuleField(name) ? false : !!option.required }
                )
            },
            syncMatrixRequiredStatus () {
                if (!this.enableMatrix) {
                    this.errors.remove(MATRIX_REQUIRED_ERROR_FIELD)
                    return
                }

                if (hasAnyMatrixRuleValue(this.matrixRuleValues)) {
                    this.errors.remove(MATRIX_REQUIRED_ERROR_FIELD)
                    return
                }

                if (this.errors.items.every(err => err.field !== MATRIX_REQUIRED_ERROR_FIELD)) {
                    this.errors.add({
                        field: MATRIX_REQUIRED_ERROR_FIELD,
                        msg: this.$t('editPage.matrixAnyRequiredTips')
                    })
                }
            },
            validateMatrixFields () {
                this.$nextTick(() => {
                    MATRIX_RULE_FIELDS.forEach((field) => {
                        this.$validator.validate(field, this.normalizedMatrixControlOption[field] || '')
                    })
                    this.syncMatrixRequiredStatus()
                })
            },
            clearMatrixFieldErrors () {
                MATRIX_RULE_FIELDS.forEach((field) => {
                    this.errors.remove(field)
                })
                this.errors.remove(MATRIX_REQUIRED_ERROR_FIELD)
            },
            handleUpdateJobMatrix (name, value) {
                this.setPipelineEditing(true)
                this.updateContainerParams(
                    'matrixControlOption',
                    Object.assign({}, this.matrixControlOption || {}, { [name]: value })
                )
                if (this.isMatrixRuleField(name)) {
                    this.validateMatrixFields()
                }
            },
            toggleMatrix (e) {
                const enable = e.target.checked
                this.setPipelineEditing(true)
                this.updateContainerParams('matrixGroupFlag', enable)
            },
            initOptionConfig () {
                const matrixControlOption = this.getJobOptionDefault(this.JOB_MATRIX, this.matrixControlOption || {})
                const compatibleMatrixControlOption = {
                    ...matrixControlOption,
                    strategyStr: matrixControlOption.strategyStr ?? matrixControlOption.strategy ?? '',
                    includeCaseStr: matrixControlOption.includeCaseStr ?? matrixControlOption.include ?? '',
                    excludeCaseStr: matrixControlOption.excludeCaseStr ?? matrixControlOption.exclude ?? ''
                }
                const shouldSync = ['strategyStr', 'includeCaseStr', 'excludeCaseStr', 'fastKill', 'maxConcurrency']
                    .some(key => matrixControlOption[key] !== compatibleMatrixControlOption[key])

                if (shouldSync) {
                    this.updateContainerParams('matrixControlOption', compatibleMatrixControlOption)
                }
            }
        }
    }
</script>
