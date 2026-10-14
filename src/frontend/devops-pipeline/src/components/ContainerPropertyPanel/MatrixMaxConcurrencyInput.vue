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
            :name="name"
            :value="value"
            :disabled="disabled"
            :placeholder="placeholder"
            @input="handleNumericInput"
            @blur="handleNumericBlur"
        />
        <input
            v-else
            class="bk-form-input pointer-events-auto matrix-max-concurrency-field"
            type="text"
            :name="name"
            :value="value"
            :disabled="disabled"
            :placeholder="varPlaceholder"
            @input="handleVarInput"
            @blur="handleVarBlur"
        />
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import atomFieldMixin from '@/components/atomFormField/atomFieldMixin'

    export default {
        name: 'matrix-max-concurrency-input',
        components: {
            Logo
        },
        mixins: [atomFieldMixin],
        props: {
            value: {
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
            value: {
                immediate: true,
                handler (newValue) {
                    if (this.isValidVar(newValue)) {
                        this.isVarInputMode = true
                    }
                }
            }
        },
        methods: {
            isValidVar (value) {
                if (value === null || value === undefined || value === '') {
                    return false
                }
                const str = String(value)
                if (this.pipelineDialect === 'CLASSIC') {
                    return str.isBkVar()
                }
                return str.isBKConstraintVar()
            },
            emitChange (nextValue) {
                if (String(nextValue) === String(this.value ?? '')) {
                    return
                }
                this.handleChange(this.name, nextValue)
            },
            handleChangeType () {
                if (this.disabled) {
                    return
                }
                this.isVarInputMode = !this.isVarInputMode
                this.emitChange('')
            },
            handleNumericInput (e) {
                const raw = e.target.value || ''
                const digitsOnly = raw.replace(/\D/g, '')
                if (digitsOnly !== raw) {
                    e.target.value = digitsOnly
                }
                this.emitChange(digitsOnly)
            },
            handleNumericBlur (e) {
                const trimVal = (e.target.value || '').trim()
                e.target.value = trimVal
                this.emitChange(trimVal)
            },
            handleVarInput (e) {
                this.emitChange(e.target.value)
            },
            handleVarBlur (e) {
                const trimVal = (e.target.value || '').trim()
                e.target.value = trimVal
                this.emitChange(trimVal)
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
