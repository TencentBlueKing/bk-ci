<template>
    <section class="param-input-home">
        <section class="parameter-input">
            <p
                v-if="label && label.trim()"
                class="input-label"
                :title="label"
            >
                <label>
                    {{ label }}
                </label>
                <i
                    v-if="desc"
                    class="bk-icon icon-info-circle label-desc"
                    v-bk-tooltips.top="{ content: desc, allowHTML: false }"
                />
                <span
                    v-if="showTypeSwitcher"
                    @click="handleChangeType"
                    :class="['change-type', !isVarInputMode ? 'open-var' : 'close-var']"
                    v-bk-tooltips="{ content: !isVarInputMode ? $t('switchToVarMode') : $t('closeVarMode') }"
                >
                    <Logo
                        size="14"
                        name="isSetAsVariable"
                    />
                </span>
            </p>
            <bk-input
                v-if="type === 'input'"
                class="input-main"
                :clearable="!disabled"
                :disabled="disabled"
                :value="value"
                :placeholder="placeholder || $t('settings.itemPlaceholder')"
                @change="(newValue) => $emit('update-value', newValue)"
            />
            <enum-input
                v-else-if="type === 'enum-input' && !isVarInputMode"
                class="input-main"
                name="value"
                :list="list"
                :disabled="disabled"
                :value="enumValue"
                :handle-change="handleEnumChange"
            />
            <bk-select
                v-else-if="type === 'select' && !isVarInputMode"
                :disabled="disabled"
                v-model="selectValue"
                :placeholder="placeholder || $t('selectTips')"
                @change="(newValue) => $emit('update-value', newValue)"
                ext-cls="select-custom input-main"
                ext-popover-cls="select-popover-custom"
                searchable
            >
                <bk-option
                    v-for="option in options"
                    :key="option.id"
                    :id="option.id"
                    :name="option.name"
                >
                </bk-option>
            </bk-select>
            <bk-input
                v-else-if="isVarInputMode"
                :class="['input-main', isError ? 'error-input' : '']"
                :clearable="!disabled"
                v-model="displayValue"
                @blur="handleVarBlur"
                @clear="handleVarClear"
                :disabled="disabled"
                :placeholder="pipelineDialect === 'CLASSIC' ? $t('placeholderVar') : $t('placeholderConstraintVar')"
            ></bk-input>

            <span class="error-text">
                {{ isError ? $t('editPage.paramValueTips') : '' }}
            </span>
        </section>
    </section>
</template>

<script>
    import mixins from '../mixins'
    import Logo from '@/components/Logo'
    import EnumInput from '@/components/atomFormField/EnumInput'

    export default {
        components: {
            EnumInput,
            Logo,
        },

        mixins: [mixins],

        props: {
            label: {
                type: String
            },
            desc: {
                type: String,
                default: ''
            },
            id: {
                type: String
            },
            type: {
                type: String
            },
            value: {
                type: [String, Array]
            },
            disabled: {
                type: Boolean,
                default: false
            },
            options: {
                type: Array
            },
            list: {
                type: Array,
                default: () => ([])
            },
            placeholder: {
                type: String,
                default: ''
            }
        },

        data () {
            return {
                isVarInputMode: false,
                displayValue: '',
                selectValue: this.value,
                enumValue: '',
                isError: false
            }
        },

        computed: {
            showTypeSwitcher () {
                return ['select', 'enum-input'].includes(this.type)
            }
        },

        watch: {
            value: {
                handler (newValue) {
                    const isVar = this.getValidaVar(newValue)
                    const inList = this.list?.some(i => i.value === newValue)
                    const defaultVal = this.list[0]?.value

                    const handleVarMode = () => {
                        this.isVarInputMode = true
                        this.displayValue = newValue
                    }

                    if (this.type === 'enum-input') {
                        if (newValue ==='' ||  (isVar && !inList)) {
                            handleVarMode()
                        } else {
                            this.enumValue = newValue

                            if (newValue !== defaultVal) {
                                handleVarMode()
                                this.handleVarBlur(newValue)
                            }
                        }
                    } else {
                        if (isVar && !inList) {
                            handleVarMode()
                        } else {
                            if (this.isMultiple) {
                                const valArr = this.value ? this.value.split(',') : []
                                this.selectValue = valArr
                            } else {
                                this.selectValue = this.value
                            }
                        }
                    }
                },
                immediate:true
            }
        },

        methods: {
            handleEnumChange (name, value){
                this.enumValue = value
                this.$emit('update-value', value)
            },

            handleChangeType () {
                this.isVarInputMode = !this.isVarInputMode
                this.isError = false
                if (this.type === 'enum-input' && !this.isVarInputMode) {
                    const defaultVal = this.list[0]?.value
                    this.$emit('update-value', defaultVal)
                } else {
                    this.displayValue = ''
                    this.selectValue = this.isMultiple ? [] : ''
                    this.$emit('update-value', '')
                }
            },

            handleVarClear () {
                this.isError = false
                this.$emit('update-value', '')
            },

            handleVarBlur (newValue) {
                let displayValue = ''
                let isError = false
                
                if (newValue !== '') {
                    if (this.getValidaVar(newValue)) {
                        displayValue = newValue
                    } else {
                        isError = true
                    }
                }
                
                this.displayValue = displayValue
                this.isError = isError
                this.$emit('update-value', this.displayValue)
            },
        }
    }
</script>

<style lang="scss" scoped>
    .param-input-home {
        display: flex;
        align-items: center;
        flex: 1;
        .param-hyphen {
            margin-right: 11px;
            margin-bottom: 16px;
        }
    }
    .parameter-input {
        flex: 1;
        & > *:not(.error-text) {
           line-height: 30px;
        }
        .input-label {
            display: flex;
            align-items: center;
            overflow: hidden;
            font-size: 12px;
            > label {
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;

            }
            > i {
                margin-left: 8px;
                flex-shrink: 0;
            }
            .change-type {
                display: inline-block;
                margin-left: 8px;
                width: 18px;
                height: 18px;
                line-height: 20px;
                text-align: center;
                margin-top: 2px;
                border-radius: 2px;
            }
            .open-var {
                background: #EAEBF0;
                svg {
                    color: #979BA5;
                }
                &:hover {
                    background-color: #DCDEE5;
                }
                &:hover svg{
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
                &:hover svg{
                    color: #1768EF;
                }
            }
        }
        .input-main {
            flex: 1;
        }
    }
    .parameter-select {
        position: relative;
        .parameter-list {
            position: absolute;
            top: 32px;
            left: 0;
            right: 0;
            padding: 6px 0;
            list-style: none;
            border: 1px solid #dcdee5;
            border-radius: 2px;
            line-height: 32px;
            background: #fff;
            color: #63656e;
            overflow: auto;
            max-height: 216px;
            min-width: 100px;
            z-index: 2;
            li {
                padding: 0 16px;
                position: relative;
                &:hover {
                    color: #3a84ff;
                    background-color: #eaf3ff;
                }
            }
            .is-active {
                color: #3a84ff;
                background-color: #f4f6fa;
                &:after {
                    content: '';
                    position: absolute;
                    right: 16px;
                    top: 11px;
                    height: 7px;
                    width: 3px;
                    transform: rotate(45deg);
                    border-bottom: 1px solid #3a84ff;
                    border-right: 1px solid #3a84ff;
                }
            }
        }
    }
</style>
<style lang="scss">
.param-input-home {
    .error-text {
        color: #ff5656;
        display: inline-block;
        height: 16px;
        line-height: 16px;
    }
    .error-input {
        .bk-form-input {
            border-color: #ff5656 !important;
        }
    }
}
</style>