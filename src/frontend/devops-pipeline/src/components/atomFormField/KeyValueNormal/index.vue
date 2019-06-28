<template>
    <div class="Key-value-nomal">
        <ul>
            <template v-if="paramList.length">
                <li class="param-item" v-for="(param, index) in paramList" :key="index" :isError="errors.any(`param-${index}`)">
                    <form-field :is-error="errors.has(`param-${index}.key`)" :error-msg="errors.first(`param-${index}.key`)">
                        <vuex-input
                            :data-vv-scope="`param-${index}`"
                            :disabled="disabled"
                            :handle-change="(name, value) => handleParamChange(name, value, index)"
                            v-validate.initial="`required|unique:${paramList.map(p => p.key).join(&quot;,&quot;)}|max: 30|${snonVarRule}`"
                            name="key"
                            placeholder="Key"
                            :value="param.key" />
                    </form-field>
                    <div class="bk-form-item">
                        <vuex-input name="value" :disabled="disabled" placeholder="Value" :value="param.value" :handle-change="(name, value) => handleParamChange(name, value, index)" />
                    </div>
                    <i @click.stop.prevent="editParam(index, false)" class="bk-icon icon-minus hover-click" v-if="!disabled" />
                </li>
            </template>
            <a class="text-link hover-click" v-if="!disabled" @click.stop.prevent="editParam(paramList.length, true)">
                <i class="bk-icon icon-plus-circle" />
                <span>新增变量</span>
            </a>
        </ul>
    </div>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
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
            value: {
                type: Object,
                default: []
            },
            setParentValidate: {
                type: Function,
                default: () => () => {}
            },
            disabled: {
                type: Boolean,
                default: false
            },
            // 为true允许数组为空，为false表示至少留一项
            allowNull: {
                type: Boolean,
                default: true
            },
            isSupportVar: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                paramList: []
            }
        },
        computed: {
            snonVarRule () {
                return !this.isSupportVar ? 'nonVarRule' : ''
            }
        },
        watch: {
            errors: {
                // deep: true,
                // handler: function (errors, old) {
                //     this.setParentValidate()
                // }
            },
            value (val) {
                this.paramList = val
            }
        },
        async created () {
            this.paramList = this.value
            // this.paramList.push({namespace: 111, key: 1, value: 222})
        },
        methods: {
            editParam (index, isAdd) {
                if (isAdd) {
                    const param = {
                        key: `param${this.paramList.length + 1}`,
                        value: ''
                    }
                    this.paramList.splice(index + 1, 0, param)
                } else {
                    // 如果不允许数组为空并且是剩余最后一项，则不允许删除
                    if (this.allowNull || this.paramList.length > 1) {
                        this.paramList.splice(index, 1)
                    }
                }
                this.handleChange(this.name, this.paramList)
            },
            handleParamChange (key, value, paramIndex) {
                const param = this.paramList[paramIndex]
                if (param) {
                    Object.assign(param, {
                        [key]: value
                    })
                    this.handleChange(this.name, this.paramList)
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '../../../scss/conf.scss';
    .Key-value-nomal {
        .param-item {
            display: flex;
            // justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 10px;
            > span {
                flex: 1;
                margin-right: 0 10px;
            }
            > div {
                flex: 1;
                margin-right: 10px;
            }
            > .bk-form-item {
                margin-top: 0px !important;
            }
        }
        .param-item-empty {
            text-align: center;
            color: $fontLigtherColor;
        }
        .hover-click {
            cursor: pointer;
            line-height: 36px;
        }
    }
</style>
