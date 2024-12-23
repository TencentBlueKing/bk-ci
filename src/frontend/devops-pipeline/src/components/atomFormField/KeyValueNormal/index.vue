<template>
    <div class="Key-value-nomal">
        <ul>
            <template v-if="paramList.length">
                <li
                    class="param-item"
                    v-for="(param, index) in paramList"
                    :key="index"
                    :isError="!isMetadataVar && errors.any(`param-${index}`)"
                >
                    <form-field
                        :is-error="!isMetadataVar && errors.has(`param-${index}.key`)"
                        :error-msg="errors.first(`param-${index}.key`)"
                    >
                        <vuex-input
                            :data-vv-scope="`param-${index}`"
                            :disabled="disabled || editValueOnly"
                            :handle-change="(name, value) => handleParamChange(name, value, index)"
                            v-validate.initial="keyRules"
                            name="key"
                            :placeholder="paramKeyPlaceholder"
                            :value="param.key"
                        />
                    </form-field>
                    <div class="bk-form-item">
                        <vuex-input
                            name="value"
                            :disabled="disabled"
                            :placeholder="paramValuePlaceholder"
                            :value="param.value"
                            :handle-change="(name, value) => handleParamChange(name, value, index)"
                        />
                    </div>
                    <i
                        @click.stop.prevent="editParam(index, false)"
                        class="devops-icon icon-minus hover-click"
                        v-if="!disabled && !editValueOnly"
                    />
                </li>
            </template>
            <a
                v-if="!editValueOnly"
                :class="['text-link', 'hover-click', { disabled: disabled }]"
                @click.stop.prevent="editParam(paramList.length, true)"
            >
                <i class="devops-icon icon-plus-circle" />
                <span>{{ addBtnText || defaultAddBtnText }}</span>
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
        name: 'key-value-normal',
        components: {
            VuexInput,
            FormField
        },
        mixins: [atomFieldMixin, validMixins],
        props: {
            name: {
                type: String,
                default: ''
            },
            addBtnText: {
                type: String,
                default: ''
            },
            value: {
                type: Object,
                default: () => []
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
            },
            isMetadataVar: {
                type: Boolean,
                default: false
            },
            // 只允许修改值，不允许增减项和修改key
            editValueOnly: {
                type: Boolean,
                default: false
            },
            upperCased: Boolean,
            keyPlaceholder: {
                type: String
            },
            valuePlaceholder: {
                type: String
            }
        },
        data () {
            return {
                paramList: [],
                defaultAddBtnText: this.$t('editPage.addParams')
            }
        },
        computed: {
            snonVarRule () {
                return !this.isSupportVar ? 'nonVarRule' : ''
            },
            keyRules () {
                return `required|unique:${this.paramList.map(p => p.key).join(',')}|max: 50|${this.snonVarRule}`
            },
            paramKeyPlaceholder () {
                if (this.keyPlaceholder) return this.keyPlaceholder
                return this.isMetadataVar ? this.$t('view.key') : 'Key'
            },
            paramValuePlaceholder () {
                if (this.valuePlaceholder) return this.valuePlaceholder
                return this.isMetadataVar ? this.$t('view.value') : 'Value'
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
        },
        methods: {
            editParam (index, isAdd) {
                if (this.disabled) return

                if (isAdd) {
                    const paramKey = `${this.isMetadataVar ? 'key' : 'param'}${this.paramList.length + 1}`
                    const param = {
                        key: this.upperCased ? paramKey.toUpperCase() : paramKey,
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
            color: $fontLighterColor;
        }
        .hover-click {
            cursor: pointer;
            line-height: 36px;
            &.disabled {
                cursor: not-allowed
            }
        }
    }
</style>
