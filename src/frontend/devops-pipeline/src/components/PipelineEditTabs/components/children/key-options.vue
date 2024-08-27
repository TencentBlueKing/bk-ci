<template>
    <section>
        <batch-add-options :submit-batch-add="handleBatchInput" />
        <div class="Key-value-nomal" style="margin-top: 16px;">
            <form-field :hide-colon="true" :label="$t('editPage.optionSetting')">
                <template v-if="list.length">
                    <li class="param-item" v-for="(param, index) in list" :key="index">
                        <form-field :is-error="keyErrs[index]" :error-msg="keyErrs[index]">
                            <vuex-input
                                :disabled="disabled"
                                :handle-change="(name, value) => handleEdit(name, value, index)"
                                name="key"
                                :placeholder="$t('editPage.optionValTips')"
                                :value="param.key" />
                        </form-field>
                        <form-field :is-error="valueErrs[index]" :error-msg="valueErrs[index]">
                            <vuex-input
                                :disabled="disabled"
                                :handle-change="(name, value) => handleEdit(name, value, index)"
                                name="value"
                                :placeholder="$t('editPage.optionNameTips')"
                                :value="param.value" />
                        </form-field>
                        <div class="operate-icon-div" v-if="!disabled">
                            <i @click.stop.prevent="handleAdd(index)" class="bk-icon icon-plus-circle-shape" />
                            <i @click.stop.prevent="handleDelete(index)" class="bk-icon icon-minus-circle-shape" />
                        </div>
                    </li>
                </template>
                <a :class="['text-link', 'hover-click']" v-if="!disabled && list.length === 0" @click.stop.prevent="handleAdd">
                    <i class="devops-icon icon-plus-circle" />
                    <span>{{$t('newui.pipelineParam.addItem')}}</span>
                </a>
            </form-field>
        </div>
    </section>
</template>

<script>
    import atomFieldMixin from '@/components/atomFormField/atomFieldMixin'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import validMixins from '@/components/validMixins'
    import BatchAddOptions from './batch-add-options'
    export default {
        components: {
            VuexInput,
            FormField,
            BatchAddOptions
        },
        mixins: [atomFieldMixin, validMixins],
        props: {
            disabled: {
                type: Boolean,
                default: false
            },
            options: {
                type: Array,
                default: () => ([])
            },
            handleChangeOptions: {
                type: Function,
                required: true
            }
        },
        data () {
            return {
                list: [],
                keyErrs: {},
                valueErrs: {}
            }
        },
        created () {
            this.list = this.options || []
        },
        methods: {
            // 批量增加
            handleBatchInput (batchStr) {
                if (!batchStr) return
                let opts = []
                if (batchStr && typeof batchStr === 'string') {
                    opts = batchStr.split('\n').map(opt => {
                        const v = opt.trim()
                        const equalPos = v.indexOf('=')
                        const res = equalPos > -1
                            ? [v.slice(0, equalPos), v.slice(equalPos + 1)]
                            : [v, v]
                        const [key, value] = res
                        return {
                            key,
                            value
                        }
                    })
                }
                this.list.splice(this.list.length, 0, ...opts)
                this.handleChangeOptions('options', this.list)
                this.validateAllOptions()
            },
            // 触发验证
            validateAllOptions () {
                this.$nextTick(() => {
                    this.keyErrs = this.findInvalidItems('key', 'value')
                    this.valueErrs = this.findInvalidItems('value', 'name')
                })
            },
            handleEdit (name, val, index) {
                const item = this.list[index]
                Object.assign(item, { [name]: val })
                this.list.splice(index, 1, item)
                this.handleChangeOptions('options', this.list)
                this.validateAllOptions()
            },
            handleAdd (index) {
                const item = { key: '', value: '' }
                if (index === undefined) {
                    this.list.push(item)
                } else {
                    this.list.splice(index + 1, 0, item)
                }
                this.handleChangeOptions('options', this.list)
                // this.validateAllOptions()
            },
            handleDelete (index) {
                this.list.splice(index, 1)
                this.handleChangeOptions('options', this.list)
                this.validateAllOptions()
            },
            // 找出校验有错误的选项
            findInvalidItems (key, errPrefix) {
                const seen = new Map()
                const result = {}

                for (let i = 0; i < this.list.length; i++) {
                    const value = this.list[i][key]

                    if (!value) {
                        if (key === 'key') {
                            result[i] = this.$t('editPage.requiredTips', [errPrefix])
                        }
                    } else {
                        if (seen.has(value)) {
                            result[i] = result[i] = this.$t('editPage.noRepeatTips', [errPrefix])
                            if (!result[seen.get(value)]) {
                                result[seen.get(value)] = result[i] = this.$t('editPage.noRepeatTips', [errPrefix])
                            }
                        } else {
                            seen.set(value, i)
                        }
                    }
                }
                return result
            }
        }
    }
</script>

<style lang="scss" scoped>
    .key-item {
        display: flex;
        align-items: center;
        height: 46px;
        .key-index {
            font-size: 12px;
            color: #979BA5;
            width: 24px;
        }
        .key-val {
            flex: 1;
            background: #FFFFFF;
        }
        .key-del {
            width: 32px;
            cursor: pointer;
            text-align: right;
            i {
                font-size: 14px;
            }
        }
    }
    .operate-icon-div {
        display: flex;
        height: 32px;
        align-items: center;
        flex: none !important;
        cursor: pointer;
        margin-right: 0 !important;
        i {
            color: #979BA5;
            font-size: 14px;
            &:not(:last-child) {
                margin-right: 8px;
            }
        }
    }
    .key-add {
        margin-top: 8px;
        font-size: 12px;
        display: flex;
        align-items: center;
        cursor: pointer;
        color: #3A84FF;
        i {
            margin-right: 6px;
        }
    }
</style>
