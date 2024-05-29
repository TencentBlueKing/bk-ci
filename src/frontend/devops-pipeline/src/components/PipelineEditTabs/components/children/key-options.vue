<template>
    <section>
        <form-field :hide-colon="true" :desc="$t('editPage.batchAddTips')" :label="$t('editPage.batchAdd')">
            <bk-input class="key-val" :type="'textarea'" v-model="batchInput" :row="3" :placeholder="$t('editPage.optionTips')" />
        </form-field>
        <div class="batch-confirm-div">
            <span @click="handleBatchInput">{{$t('editPage.batchAddBtn')}}</span>
        </div>
        <div class="Key-value-nomal" style="margin-top: 16px;">
            <form-field :hide-colon="true" :label="$t('editPage.optionSetting')">
                <template v-if="list.length">
                    <li class="param-item" v-for="(param, index) in list" :key="index" :isError="errors.any(`option-${index}`)">
                        <form-field :is-error="errors.has(`option-${index}.key`)" :error-msg="errors.first(`option-${index}.key`)">
                            <vuex-input
                                :data-vv-scope="`option-${index}`"
                                :disabled="disabled"
                                :handle-change="(name, value) => handleEdit(name, value, index)"
                                v-validate="keyRule"
                                name="key"
                                :placeholder="$t('editPage.optionValTips')"
                                :value="param.key" />
                        </form-field>
                        <form-field :is-error="errors.has(`option-${index}.value`)" :error-msg="errors.first(`option-${index}.value`)">
                            <vuex-input
                                :data-vv-scope="`option-${index}`"
                                :disabled="disabled"
                                :handle-change="(name, value) => handleEdit(name, value, index)"
                                v-validate="valueRule"
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
    export default {
        components: {
            VuexInput,
            FormField
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
                batchInput: '',
                list: []
            }
        },
        computed: {
            keyRule () {
                return `required|unique:${this.list.map(p => p.key).join(',')}`
            },
            valueRule () {
                return `unique:${this.list.map(p => p.value).join(',')}|max: 100`
            }
        },
        created () {
            this.list = this.options || []
        },
        methods: {
            handleBatchInput () {
                if (!this.batchInput) return
                let opts = []
                if (this.batchInput && typeof this.batchInput === 'string') {
                    opts = this.batchInput.split('\n').map(opt => {
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
                this.batchInput = ''
                this.validateAllOptions()
            },
            validateAllOptions () {
                this.$nextTick(() => {
                    this.list.forEach((option, index) => {
                        this.$validator.validate(`option-${index}.*`)
                    })
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
            }
        }
    }
</script>

<style lang="scss" scoped>
    .batch-confirm-div {
        width: 100%;
        padding: 8px 0 16px 0;
        border-bottom: 1px solid #DCDEE5;
        color: #3A84FF;
        cursor: pointer;
        font-size: 12px;
    }
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
            /* border: 1px solid #C4C6CC;
            border-radius: 2px; */
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
