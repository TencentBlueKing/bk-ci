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
                    class="change-type"
                    @click="handleChangeType"
                >
                    <bk-icon
                        type="sort"
                        class="icon-sort"
                    />
                    {{ !isVarInputMode ? $t('fillVariable') : $t('selectPredefinedOption') }}
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
                class="input-main"
                :clearable="!disabled"
                v-model="displayValue"
                @blur="handleVarBlur"
                :disabled="disabled"
                :placeholder="$t('placeholderVar')"
            ></bk-input>
        </section>
    </section>
</template>

<script>
    import mixins from '../mixins'
    import EnumInput from '@/components/atomFormField/EnumInput'

    export default {
        components: {
            EnumInput,
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
                enumValue: ''
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
                    const isVar = newValue?.isBkVar()
                    const inList = this.list?.some(i => i.value === newValue)

                    if (this.type === 'enum-input') {
                        if (newValue ==='' ||  (isVar && !inList)) {
                            this.isVarInputMode = true
                            this.displayValue = newValue
                        } else {
                            const defaultVal = this.list[0]?.value
                            this.enumValue = newValue || defaultVal || ''
                        }
                    } else {
                        if (isVar && !inList) {
                            this.isVarInputMode = true
                            this.displayValue = newValue
                        } else {
                            this.selectValue = this.value
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
                if (this.type === 'enum-input' && !this.isVarInputMode) {
                    const defaultVal = this.list[0]?.value
                    this.$emit('update-value', defaultVal)
                } else {
                    this.displayValue = ''
                    this.selectValue = this.isMultiple ? [] : ''
                    this.$emit('update-value', '')
                }
            },
            
            handleVarBlur (newValue) {
                if (newValue !== '' && newValue.isBkVar()) {
                    this.displayValue = newValue
                } else {
                    this.displayValue = ''
                }
                this.$emit('update-value', this.displayValue)
            },
        }
    }
</script>

<style lang="scss" scoped>
    .param-input-home {
        display: flex;
        align-items: flex-end;
        flex: 1;
        .param-hyphen {
            margin-right: 11px;
        }
    }
    .parameter-input {
        flex: 1;
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
                display: flex;
                align-items: center;
                color: #3a84ff;
                cursor: pointer;
                .icon-sort::before {
                    display: inline-block;
                    transform: rotate(90deg);
                }
                .icon-sort {
                    margin: 0 5px;
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
