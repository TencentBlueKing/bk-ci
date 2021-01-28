<template>
    <div class="define-param">
        <draggable v-model="globalParams" :options="paramsDragOptions">
            <accordion
                v-for="(param, index) in globalParams"
                :key="param.paramIdKey"
                :show-content="true"
                :is-error="errors.any(`param-${param.key}`)">
                <header class="param-header" slot="header">
                    <span>
                        <bk-popover
                            style="vertical-align: middle"
                            v-if="errors.all(`param-${param.key}`).length"
                            placement="top">
                            <i class="bk-icon icon-info-circle-shape" />
                            <div slot="content">
                                <p v-for="error in errors.all(`param-${param.key}`)" :key="error">{{ error }}</p>
                            </div>
                        </bk-popover>
                        {{ param.key }}
                    </span>
                    <!-- <i
                        v-if="!disabled && settingKey !== 'templateParams'"
                        @click.stop.prevent="editParamShow(index)"
                        class="devops-icon"
                        :class="[`${param.required ? 'icon-eye' : 'icon-eye-slash'}`]" /> -->
                    <!-- <i
                        v-if="!disabled"
                        class="devops-icon icon-move" /> -->
                    <i
                        v-if="!disabled"
                        @click.stop.prevent="editParam({ index: index, isAdd: false })"
                        class="devops-icon icon-minus" />
                </header>
                <bk-form slot="content">
                    <div class="params-flex-col">
                        <bk-form-item label-width="auto" :label="$t('editPage.paramsType')" class="flex-col-span-1">
                            <selector
                                :popover-min-width="246"
                                :data-vv-scope="`param-${param.key}`"
                                :disabled="disabled"
                                name="type"
                                :list="paramsList"
                                :handle-change="(name, value) => handleParamTypeChange(name, value, index)"
                                :value="param.valueType" />
                        </bk-form-item>
                        <bk-form-item label-width="auto" class="flex-col-span-1" v-if="settingKey !== 'templateParams'">
                            <atom-checkbox
                                :disabled="disabled"
                                :text="$t('editPage.required')"
                                :value="param.required"
                                name="required"
                                :handle-change="(name, value) => handleUpdateParam(name, value, index)" />
                        </bk-form-item>
                    </div>
                    <div class="params-flex-col pt10">
                        <bk-form-item
                            label-width="auto"
                            class="flex-col-span-1"
                            :label="$t('name')"
                            :is-error="errors.has(`param-${param.key}.id`)"
                            :error-msg="errors.first(`param-${param.key}.id`)">
                            <vuex-input
                                :ref="`paramId${index}Input`"
                                :data-vv-scope="`param-${param.key}`"
                                :disabled="disabled"
                                :handle-change="(name, value) => handleUpdateParamId(name, value, index)"
                                v-validate.initial="`required|unique:${globalParams.map(p => p.key).join(',')}`"
                                name="key"
                                :placeholder="$t('nameInputTips')"
                                :value="param.key" />
                        </bk-form-item>
                        <bk-form-item
                            label-width="auto"
                            class="flex-col-span-1"
                            :label="$t(`editPage.${getParamsDefaultValueLabel(param.valueType)}`)"
                            :required="param.required"
                            :is-error="errors.has(`param-${param.key}.defaultValue`)"
                            :error-msg="errors.first(`param-${param.key}.defaultValue`)"
                            :desc="$t(`editPage.${getParamsDefaultValueLabelTips(param.valueType)}`)">
                            <selector
                                :popover-min-width="250"
                                v-if="isSelectorParam(param.valueType)"
                                :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                                :list="transformOpt(param.options)"
                                :multi-select="isMultipleParam(param.valueType)"
                                name="value"
                                :data-vv-scope="`param-${param.key}`"
                                :placeholder="$t('editPage.defaultValueTips')"
                                :disabled="disabled"
                                :key="param.valueType"
                                :value="getSelectorDefaultVal(param)">
                            </selector>
                            <enum-input
                                v-if="isBooleanParam(param.valueType)"
                                name="value"
                                :list="boolList"
                                :disabled="disabled"
                                :data-vv-scope="`param-${param.key}`"
                                :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                                :value="param.value">
                            </enum-input>
                            <vuex-input
                                v-if="isStringParam(param.valueType)"
                                :disabled="disabled"
                                :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                                name="value"
                                :click-unfold="true"
                                :data-vv-scope="`param-${param.key}`"
                                :placeholder="$t('editPage.defaultValueTips')" :value="param.value" />
                            <vuex-textarea
                                v-if="isTextareaParam(param.valueType)"
                                :click-unfold="true"
                                :disabled="disabled"
                                :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                                name="value"
                                :data-vv-scope="`param-${param.key}`"
                                :placeholder="$t('editPage.defaultValueTips')"
                                :value="param.value" />
                        </bk-form-item>
                    </div>
                    <bk-form-item
                        label-width="auto"
                        v-if="isSelectorParam(param.valueType)"
                        :label="$t('editPage.selectOptions')"
                        :desc="$t('editPage.optionsDesc')"
                        :is-error="errors.has(`param-${param.key}.options`)"
                        :error-msg="errors.first(`param-${param.key}.options`)">
                        <vuex-textarea
                            v-validate.initial="'excludeComma'"
                            :disabled="disabled"
                            :handle-change="(name, value) => editOption(name, value, index)" name="options"
                            :data-vv-scope="`param-${param.key}`"
                            :placeholder="$t('editPage.optionTips')"
                            :value="getOptions(param)" />
                    </bk-form-item>
                    <bk-form-item label-width="auto" :label="$t('desc')">
                        <vuex-input
                            :disabled="disabled"
                            :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                            name="desc"
                            :placeholder="$t('editPage.descTips')"
                            :value="param.desc" />
                    </bk-form-item>
                </bk-form>
            </accordion>
        </draggable>
        <a class="text-link" v-if="!disabled" @click.stop.prevent="editParam({ index: value.length, isAdd: true })">
            <i class="devops-icon icon-plus-circle" />
            <span>{{ $t('editPage.addParams') }}</span>
        </a>
    </div>
</template>

<script>
    import { mapGetters } from 'vuex'
    import draggable from 'vuedraggable'
    import { deepCopy } from '@/utils/util'
    import Selector from '@/components/atomFormField/Selector'
    import Accordion from '@/components/atomFormField/Accordion'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'

    import { STORE_API_URL_PREFIX, REPOSITORY_API_URL_PREFIX } from '@/store/constants'

    import {
        isTextareaParam,
        isStringParam,
        isBooleanParam,
        isEnumParam,
        isMultipleParam,
        getParamsDefaultValueLabel,
        getParamsDefaultValueLabelTips,
        CHECK_DEFAULT_PARAM,
        STRING
    } from '@/store/modules/atom/paramsConfig'

    const BOOLEAN = [
        {
            value: true,
            label: true
        },
        {
            value: false,
            label: false
        }
    ]

    export default {
        name: 'define-param',
        components: {
            Selector,
            draggable,
            Accordion,
            VuexInput,
            EnumInput,
            VuexTextarea,
            AtomCheckbox
        },
        props: {
            settingKey: {
                type: String,
                default: 'params'
            },
            value: {
                type: Array,
                default: () => []
            },
            paramsList: {
                type: Array,
                default: () => []
            },
            disabled: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                globalParams: [],
                paramIdCount: 0
            }
        },
        
        computed: {
            ...mapGetters('atom', [
                'osList',
                'getBuildResourceTypeList'
            ]),

            paramsDragOptions () {
                return {
                    ghostClass: 'sortable-ghost-atom',
                    chosenClass: 'sortable-chosen-atom',
                    handle: '.icon-move',
                    animation: 200,
                    disabled: this.disabled
                }
            },

            boolList () {
                return BOOLEAN
            }
        },
        watch: {
            value: {
                handler (val) {
                    this.globalParams = val
                },
                immediate: true
            }
        },
        created () {
            this.globalParams = this.value
        },
        methods: {
            isTextareaParam,
            isStringParam,
            isBooleanParam,
            isMultipleParam,
            getParamsDefaultValueLabel,
            getParamsDefaultValueLabelTips,

            handleUpdateParamId (name, value, index) {
                this.$emit('handle-update-param-id', {
                    key: name,
                    value: value,
                    paramIndex: index
                })
            },

            handleUpdateParam (key, value, paramIndex) {
                this.$emit('handle-update-param', {
                    key: key,
                    value: value,
                    paramIndex: paramIndex
                })
            },
        
            editOption (name, value, index) {
                try {
                    let opts = []
                    if (value && typeof value === 'string') {
                        opts = value.split('\n').map(opt => {
                            const v = opt.trim()
                            const res = v.match(/^([\w\.\\\/]+)=(\S+)$/) || [v, v, v]
                            const [, key, value] = res
                            return {
                                key,
                                value
                            }
                        })
                    }

                    this.$emit('handle-update-param', {
                        key: name,
                        value: opts,
                        paramIndex: index
                    })
                    const param = this.value[index]
                    if (typeof param.value === 'string' && (isMultipleParam(param.type) || isEnumParam(param.type))) { // 选项清除时，修改对应的默认值
                        const dv = param.defaultValue.split(',').filter(v => param.options.map(k => k.key).includes(v))
                        if (isMultipleParam(param.type)) {
                            this.$emit('handle-update-param', {
                                key: 'defaultValue',
                                value: dv,
                                paramIndex: index
                            })
                        } else {
                            this.$emit('handle-update-param', {
                                key: 'defaultValue',
                                value: dv.join(','),
                                paramIndex: index
                            })
                        }
                    }
                } catch (e) {
                    this.$showTips({
                        message: e.message,
                        theme: 'error'
                    })
                    return []
                }
            },

            editParam (payload) {
                const { value } = this
                const { index, isAdd } = payload
                if (isAdd) {
                    const param = {
                        ...deepCopy(CHECK_DEFAULT_PARAM[STRING]),
                        key: `param${Math.floor(Math.random() * 100)}`,
                        paramIdKey: `paramIdKey-${this.paramIdCount++}`
                    }
                    if (this.settingKey === 'templateParams') {
                        Object.assign(param, { 'required': false })
                    }
                    value.splice(index + 1, 0, param)
                } else {
                    value.splice(index, 1)
                }

                this.$emit('input', value)
            },

            editParamShow (paramIndex) {
                let isShow = false
                const param = this.value[paramIndex]
                if (param) {
                    isShow = param.required
                }
                this.$emit('handle-update-param', {
                    key: 'required',
                    value: !isShow,
                    paramIndex: paramIndex
                })
            },

            isSelectorParam (type) {
                return isMultipleParam(type) || isEnumParam(type)
            },

            getCodeUrl (type) {
                type = type || 'CODE_GIT'
                return `/${REPOSITORY_API_URL_PREFIX}/user/repositories/{projectId}/hasPermissionList?permission=USE&repositoryType=${type}&page=1&pageSize=1000`
            },
            
            getBuildResourceUrl ({ os, buildType }) {
                return `/${STORE_API_URL_PREFIX}/user/pipeline/container/projects/${this.$route.params.projectId}/oss/${os}?buildType=${buildType}`
            },

            transformOpt (opts) {
                const uniqueMap = {}
                opts = opts.filter(opt => opt.key.length)
                return Array.isArray(opts) ? opts.filter(opt => {
                    if (!uniqueMap[opt.key]) {
                        uniqueMap[opt.key] = 1
                        return true
                    }
                    return false
                }).map(opt => ({ id: opt.key, name: opt.value })) : []
            },

            getSelectorDefaultVal ({ type, value = '' }) {
                if (isMultipleParam(type)) {
                    return value && typeof value === 'string' ? value.split(',') : []
                }

                return value
            },

            getOptions (param) {
                try {
                    return param.options.map(opt => opt.key === opt.value ? opt.key : `${opt.key}=${opt.value}`).join('\n')
                } catch (e) {
                    return ''
                }
            },
            
            handleParamTypeChange (key, value, paramIndex) {
                this.$emit('handle-param-type-change', {
                    key: key,
                    value: value,
                    paramIndex: paramIndex
                })
            }
        }
    }
</script>

<style lang="scss">
    @import '../../../scss/conf';

    .define-param {
        .params-flex-col {
            display: flex;
            .bk-form-item {
                flex: 1;
                padding-right: 8px;
                margin-top: 0;
                line-height: 30px;
                &:last-child {
                    padding-right: 0;
                }
                &+.bk-form-item {
                    margin-top: 0 !important;
                }
                span.bk-form-help {
                    display: block;
                }
            }
            .flex-col-span-1 {
                flex: 1;
            }
            .content .text-link {
                font-size: 14px;
                cursor: pointer;
            }
        }
        .param-header {
            display: flex;
            flex: 1;
            align-items: center;
            word-wrap: break-word;
            word-break: break-all;
            > span {
                flex: 1;
            }
            >.devops-icon {
                width: 24px;
                text-align: center;
                &.icon-plus {
                    &:hover {
                        color: $primaryColor;
                    }
                }
                &.icon-delete {
                    &:hover {
                        color: $dangerColor;
                    }
                }
            }
        }
        .sortable-ghost-atom {
        opacity: 0.5;
        }
        .sortable-chosen-atom {
            transform: scale(1.0);
        }
    }
</style>
