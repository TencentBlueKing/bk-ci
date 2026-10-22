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
                    v-if="!obj.isHidden"
                    :key="key"
                    :desc="obj.desc"
                    :required="isMatrixRuleField(key) ? false : obj.required"
                    :label="obj.label"
                    :is-error="isFieldError(key)"
                    :error-msg="getFieldErrorMsg(key)"
                >
                    <template v-if="isMaxConcurrencyField(key)">
                        <!-- 双字段校验挂载点：数字 / 变量互斥，规则拆分 -->
                        <input
                            type="hidden"
                            name="maxConcurrency"
                            v-validate.initial="getValidateRule('maxConcurrency', optionModel.maxConcurrency)"
                            :value="normalizedMatrixControlOption.maxConcurrency"
                        />
                        <input
                            type="hidden"
                            name="maxConcurrencyVar"
                            v-validate.initial="getValidateRule('maxConcurrencyVar', optionModel.maxConcurrencyVar)"
                            :value="normalizedMatrixControlOption.maxConcurrencyVar"
                        />
                        <matrix-max-concurrency-input
                            :max-concurrency="normalizedMatrixControlOption.maxConcurrency"
                            :max-concurrency-var="normalizedMatrixControlOption.maxConcurrencyVar"
                            :handle-change="handleUpdateJobMatrix"
                            :disabled="disabled"
                            :pipeline-dialect="pipelineDialect"
                            :placeholder="obj.placeholder"
                        />
                    </template>
                    <component
                        v-else
                        :is="obj.component"
                        :name="key"
                        v-validate.initial="getValidateRule(key, obj)"
                        :handle-change="handleUpdateJobMatrix"
                        :value="normalizedMatrixControlOption[key]"
                        :disabled="disabled"
                        :pipeline-dialect="pipelineDialect"
                        v-bind="getFieldBindProps(obj)"
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
    import MatrixMaxConcurrencyInput from './MatrixMaxConcurrencyInput'

    const MATRIX_RULE_FIELDS = ['strategyStr', 'includeCaseStr', 'excludeCaseStr']
    const MATRIX_REQUIRED_ERROR_FIELD = 'matrixRuleRequired'
    const MAX_CONCURRENCY_FIELDS = ['maxConcurrency', 'maxConcurrencyVar']

    export default {
        name: 'job-mutual',
        components: {
            MatrixMaxConcurrencyInput
        },
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
                    excludeCaseStr: matrixControlOption.excludeCaseStr ?? matrixControlOption.exclude ?? '',
                    // Int?：保留 null，勿把 null 归一成 ''（会触发后端反序列化格式错误）
                    maxConcurrency: matrixControlOption.maxConcurrency ?? null,
                    maxConcurrencyVar: matrixControlOption.maxConcurrencyVar ?? ''
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
            this._pendingMatrixUpdate = null
            this._pendingMatrixPatchedKeys = null
            this._pendingMatrixFlush = null
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
            isMaxConcurrencyField (name) {
                return name === 'maxConcurrency'
            },
            isFieldError (key) {
                if (this.isMaxConcurrencyField(key)) {
                    return this.errors.has('maxConcurrency') || this.errors.has('maxConcurrencyVar')
                }
                return this.errors.has(key)
            },
            getFieldErrorMsg (key) {
                if (this.isMaxConcurrencyField(key)) {
                    return this.errors.first('maxConcurrency') || this.errors.first('maxConcurrencyVar')
                }
                return this.errors.first(key)
            },
            // 不把 form-field 元数据（label/desc/rule/component/default 等）铺到具体输入组件上，避免占位符等属性冲突
            getFieldBindProps (obj) {
                const omitKeys = ['component', 'rule', 'label', 'desc', 'required', 'default', 'isHidden']
                return Object.keys(obj).reduce((acc, key) => {
                    if (!omitKeys.includes(key)) {
                        acc[key] = obj[key]
                    }
                    return acc
                }, {})
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
                    this.revalidateConcurrencyFields()
                    this.syncMatrixRequiredStatus()
                })
            },
            clearMatrixFieldErrors () {
                MATRIX_RULE_FIELDS.forEach((field) => {
                    this.errors.remove(field)
                })
                MAX_CONCURRENCY_FIELDS.forEach((field) => {
                    this.errors.remove(field)
                })
                this.errors.remove(MATRIX_REQUIRED_ERROR_FIELD)
            },
            revalidateConcurrencyFields (nextOption) {
                const option = nextOption || this.normalizedMatrixControlOption
                const maxConcurrency = option.maxConcurrency ?? null
                const maxConcurrencyVar = option.maxConcurrencyVar ?? ''
                this.$nextTick(() => {
                    // 优先校验活跃字段；空闲字段清空后应通过，顺带清掉陈旧错误
                    this.$validator.validate('maxConcurrency', maxConcurrency)
                    this.$validator.validate('maxConcurrencyVar', maxConcurrencyVar)
                })
            },
            applyConcurrencyExclusivity (option) {
                const next = { ...option }
                const hasVar = next.maxConcurrencyVar !== '' && next.maxConcurrencyVar !== null && next.maxConcurrencyVar !== undefined
                const hasNum = next.maxConcurrency !== '' && next.maxConcurrency !== null && next.maxConcurrency !== undefined
                // 互斥：变量优先（与回显规则一致）；两侧皆有时清数字侧
                // maxConcurrency 为后端 Int?：未用时用 null，禁止 ''
                if (hasVar) {
                    next.maxConcurrency = null
                } else if (hasNum) {
                    next.maxConcurrencyVar = ''
                    if (next.maxConcurrency === '') {
                        next.maxConcurrency = null
                    }
                } else if (next.maxConcurrency === '') {
                    next.maxConcurrency = null
                }
                return next
            },
            flushPendingMatrixUpdate () {
                if (!this._pendingMatrixUpdate) {
                    return
                }
                const patchedKeys = Object.keys(this._pendingMatrixPatchedKeys || {})
                const next = this.applyConcurrencyExclusivity(this._pendingMatrixUpdate)
                this._pendingMatrixUpdate = null
                this._pendingMatrixPatchedKeys = null
                this._pendingMatrixFlush = null
                this.updateContainerParams('matrixControlOption', next)
                if (patchedKeys.some(key => this.isMatrixRuleField(key))) {
                    this.validateMatrixFields()
                }
                if (patchedKeys.some(key => MAX_CONCURRENCY_FIELDS.includes(key))) {
                    this.revalidateConcurrencyFields(next)
                }
            },
            handleUpdateJobMatrix (name, value) {
                this.setPipelineEditing(true)
                // 同一 tick 内可能连续改 maxConcurrency / maxConcurrencyVar，合并后再写回，避免第二次覆盖第一次
                if (!this._pendingMatrixUpdate) {
                    this._pendingMatrixUpdate = Object.assign({}, this.matrixControlOption || {})
                    this._pendingMatrixPatchedKeys = {}
                    this._pendingMatrixFlush = Promise.resolve().then(() => {
                        this.flushPendingMatrixUpdate()
                    })
                }
                this._pendingMatrixUpdate[name] = value
                this._pendingMatrixPatchedKeys[name] = true
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
                    excludeCaseStr: matrixControlOption.excludeCaseStr ?? matrixControlOption.exclude ?? '',
                    maxConcurrencyVar: matrixControlOption.maxConcurrencyVar ?? ''
                }
                const shouldSync = ['strategyStr', 'includeCaseStr', 'excludeCaseStr', 'fastKill', 'maxConcurrency', 'maxConcurrencyVar']
                    .some(key => matrixControlOption[key] !== compatibleMatrixControlOption[key])

                if (shouldSync) {
                    this.updateContainerParams('matrixControlOption', compatibleMatrixControlOption)
                }
            }
        }
    }
</script>
