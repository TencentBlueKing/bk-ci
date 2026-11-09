<template>
    <div class="matrix-max-concurrency-input">
        <span
            v-if="!disabled || isVarInputMode"
            :class="[
                'change-type',
                isVarInputMode ? 'close-var' : 'open-var',
                { 'is-disabled': disabled }
            ]"
            v-bk-tooltips="disabled
                ? { disabled: true }
                : { content: !isVarInputMode ? $t('switchToVarMode') : $t('closeVarMode') }"
            @click="handleChangeType"
        >
            <Logo
                size="14"
                name="isSetAsVariable"
            />
        </span>
        <input
            v-if="!isVarInputMode"
            class="bk-form-input pointer-events-auto matrix-max-concurrency-field"
            type="text"
            inputmode="numeric"
            name="maxConcurrency"
            :value="maxConcurrency == null ? '' : maxConcurrency"
            :disabled="disabled"
            :placeholder="placeholder"
            @input="handleNumericInput"
            @blur="handleNumericBlur"
        />
        <input
            v-else
            class="bk-form-input pointer-events-auto matrix-max-concurrency-field"
            type="text"
            name="maxConcurrencyVar"
            :value="maxConcurrencyVar"
            :disabled="disabled"
            :placeholder="varPlaceholder"
            @input="handleVarInput"
            @blur="handleVarBlur"
        />
    </div>
</template>

<script>
    import Logo from '@/components/Logo'

    export default {
        name: 'matrix-max-concurrency-input',
        components: {
            Logo
        },
        inheritAttrs: false,
        props: {
            maxConcurrency: {
                type: [String, Number],
                default: ''
            },
            maxConcurrencyVar: {
                type: [String, Number],
                default: ''
            },
            placeholder: {
                type: String,
                default: '1–50'
            },
            pipelineDialect: {
                type: String,
                default: 'CLASSIC'
            },
            disabled: {
                type: Boolean,
                default: false
            },
            handleChange: {
                type: Function,
                default: () => () => {}
            }
        },
        data () {
            return {
                isVarInputMode: false
            }
        },
        computed: {
            varPlaceholder () {
                return this.pipelineDialect === 'CLASSIC'
                    ? this.$t('placeholderVar')
                    : this.$t('placeholderConstraintVar')
            }
        },
        watch: {
            maxConcurrencyVar: {
                immediate: true,
                handler (newValue) {
                    // 回显：maxConcurrencyVar 非空（合法或任意非空串）则进入 VAR 模式
                    if (newValue !== null && newValue !== undefined && String(newValue) !== '') {
                        this.isVarInputMode = true
                    }
                }
            }
        },
        methods: {
            revalidateFields (maxConcurrency, maxConcurrencyVar) {
                this.$nextTick(() => {
                    if (!this.$validator) {
                        return
                    }
                    this.$validator.validate('maxConcurrency', maxConcurrency)
                    this.$validator.validate('maxConcurrencyVar', maxConcurrencyVar)
                })
            },
            // maxConcurrency 对应后端 Int?：空/未用必须用 null，禁止写 ''；有值写 number
            clearMaxConcurrency () {
                this.handleChange('maxConcurrency', null)
            },
            normalizeNumericValue (value) {
                if (value === '' || value === null || value === undefined) {
                    return null
                }
                const num = Number(value)
                return Number.isFinite(num) ? num : null
            },
            handleChangeType () {
                if (this.disabled) {
                    return
                }
                // 切换模式：清空双字段，再翻转模式并重校验，避免残留错误提示
                this.clearMaxConcurrency()
                this.handleChange('maxConcurrencyVar', '')
                this.isVarInputMode = !this.isVarInputMode
                this.revalidateFields(null, '')
            },
            handleNumericInput (e) {
                const raw = e.target.value || ''
                const digitsOnly = raw.replace(/\D/g, '')
                if (digitsOnly !== raw) {
                    e.target.value = digitsOnly
                }
                const next = this.normalizeNumericValue(digitsOnly)
                this.handleChange('maxConcurrency', next)
                this.handleChange('maxConcurrencyVar', '')
                this.revalidateFields(next, '')
            },
            handleNumericBlur (e) {
                const trimVal = (e.target.value || '').trim()
                e.target.value = trimVal
                const next = this.normalizeNumericValue(trimVal)
                this.handleChange('maxConcurrency', next)
                this.handleChange('maxConcurrencyVar', '')
                this.revalidateFields(next, '')
            },
            handleVarInput (e) {
                const next = e.target.value
                this.handleChange('maxConcurrencyVar', next)
                this.clearMaxConcurrency()
                this.revalidateFields(null, next)
            },
            handleVarBlur (e) {
                const trimVal = (e.target.value || '').trim()
                e.target.value = trimVal
                this.handleChange('maxConcurrencyVar', trimVal)
                this.clearMaxConcurrency()
                this.revalidateFields(null, trimVal)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .matrix-max-concurrency-input {
        display: flex;
        align-items: center;
        width: 100%;

        .change-type {
            display: inline-flex;
            flex-shrink: 0;
            align-items: center;
            justify-content: center;
            margin-right: 8px;
            width: 18px;
            height: 18px;
            border-radius: 2px;
            cursor: pointer;

            &.is-disabled {
                cursor: default;
                pointer-events: none;
            }
        }

        .open-var {
            background: #EAEBF0;
            svg {
                color: #979BA5;
            }
            &:hover {
                background-color: #DCDEE5;
            }
            &:hover svg {
                color: #4D4F56;
            }
        }

        .close-var {
            background: #E1ECFF;
            svg {
                color: #3A84FF;
            }
            &:hover {
                background-color: #CDDFFE;
            }
            &:hover svg {
                color: #1768EF;
            }
        }

        .matrix-max-concurrency-field {
            flex: 1;
            min-width: 0;
        }
    }
</style>
