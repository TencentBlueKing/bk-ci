<template>
    <section class="param-input-home">
        <span
            v-if="hyphen && hyphen.trim()"
            class="param-hyphen"
        >{{ hyphen }}</span>
        <section class="parameter-input">
            <p
                v-if="label && label.trim()"
                class="input-label"
                :title="label"
            >
                <label>
                    {{ label }}
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
                </label>
                <i
                    v-if="desc"
                    class="bk-icon icon-info-circle label-desc"
                    v-bk-tooltips.top="{ content: desc, allowHTML: false }"
                />
            </p>
            <bk-input
                class="input-main"
                :clearable="!disabled"
                :value="value"
                :placeholder="$t('settings.itemPlaceholder')"
                @change="(newValue) => $emit('update-value', newValue)"
                v-if="type === 'input'"
                :disabled="disabled"
            ></bk-input>
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
                class="input-main"
                :disabled="disabled"
                v-model="selectValue"
                :multiple="isMultiple"
                :placeholder="$t('selectTips')"
                @clear="handleSelectClear"
                @change="handleSelectChange"
                searchable
            >
                <bk-option
                    v-for="option in paramList"
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
            listType: {
                type: String
            },
            list: {
                type: Array,
                default: () => ([])
            },
            url: {
                type: String
            },
            isMultiple: {
                type: Boolean,
                default: false
            },
            paramValues: {
                type: Object,
                default: () => ({})
            },
            paramId: {
                type: String,
                default: 'id'
            },
            paramName: {
                type: String,
                default: 'name'
            },
            dataPath: {
                type: String,
                default: 'data.records'
            },
            hyphen: {
                type: String
            }
        },

        data () {
            return {
                showList: false,
                paramList: [],
                loading: false,
                queryKey: [],
                isVarInputMode: false,
                enumValue: '',
                selectValue: null,
                displayValue: ''
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
            },

            paramValues: {
                handler (value, oldValue) {
                    const index = this.queryKey.findIndex((key) => value[key] !== oldValue[key])
                    if (index > -1) {
                        this.$emit('update-value', '')
                        this.initList()
                    }
                },
                deep: true
            }
        },

        created () {
            this.initList()
        },

        methods: {
            handleSelectClear () {
                this.selectValue = this.isMultiple ? [] : ''
                this.$emit('update-value', '')
            },

            handleSelectChange (value) {
                const params = this.isMultiple ? value.join(',') : value
                this.$emit('update-value', params)
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

            handleEnumChange (name, value){
                this.enumValue = value
                this.$emit('update-value', value)
            },

            initList () {
                if (this.listType === 'list') {
                    this.paramList = (JSON.parse(JSON.stringify(this.list)) || []).map((item) => ({ id: item[this.paramId], name: item[this.paramName] }))
                    return
                }

                if (typeof this.url === 'string' && this.url !== '') { // 只有存在url字段时才去请求
                    const [url, queryKey] = this.generateReqUrl(this.url, this.paramValues)
                    this.queryKey = queryKey

                    if (!url) return
                    this.loading = true
                    this.$ajax.get(url).then((res) => {
                        const list = this.getResponseData(res, this.dataPath)
                        const data = (list || []).map((item) => ({ id: item[this.paramId], name: item[this.paramName] }))
                        this.paramList.splice(0, this.paramList.length, ...data)
                    }).catch(e => this.$showTips({ message: e.message, theme: 'error' })).finally(() => (this.loading = false))
                }
            }
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
                max-width: 100%;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
            > i {
                flex-shrink: 0;
                margin-left: 8px;
            }
        }
        .input-main {
            flex: 1;
        }
        .change-type {
            display: inline-flex;
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
</style>