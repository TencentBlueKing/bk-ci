<template>
    <div class="define-param">
        <draggable
            v-model="globalParams"
            :options="paramsDragOptions"
        >
            <accordion
                show-content
                v-for="(param, index) in globalParams"
                :key="param.paramIdKey"
                :is-error="errors.any(`param-${param.key}`)"
            >
                <header
                    class="param-header"
                    slot="header"
                >
                    <span>
                        <bk-popover
                            style="vertical-align: middle"
                            v-if="errors.all(`param-${param.key}`).length"
                            placement="top"
                        >
                            <i class="bk-icon icon-info-circle-shape" />
                            <div slot="content">
                                <p
                                    v-for="error in errors.all(`param-${param.key}`)"
                                    :key="error"
                                >{{ error }}</p>
                            </div>
                        </bk-popover>
                        {{ param.key }}
                    </span>
                    <i
                        v-if="!disabled"
                        @click.stop.prevent="editParam({ index: index, isAdd: false })"
                        class="devops-icon icon-minus"
                    />
                </header>
                <bk-form slot="content">
                    <div class="params-flex-col">
                        <bk-form-item
                            label-width="auto"
                            :label="$t('editPage.paramsType')"
                            class="flex-col-span-1"
                        >
                            <selector
                                :popover-min-width="246"
                                :data-vv-scope="`param-${param.key}`"
                                :disabled="disabled"
                                name="type"
                                :list="paramsList"
                                :handle-change="(name, value) => handleParamTypeChange(name, value, index)"
                                :value="param.valueType"
                            />
                        </bk-form-item>
                        <bk-form-item
                            label-width="auto"
                            class="flex-col-span-1"
                            v-if="settingKey !== 'templateParams'"
                        >
                            <atom-checkbox
                                :disabled="disabled"
                                :text="$t('editPage.required')"
                                :value="param.required"
                                name="required"
                                :handle-change="(name, value) => handleParamChange(name, value, index)"
                            />
                        </bk-form-item>
                    </div>
                    <div class="params-flex-col pt10">
                        <bk-form-item
                            label-width="auto"
                            class="flex-col-span-1"
                            :label="$t('name')"
                            :is-error="errors.has(`param-${param.key}.id`)"
                            :error-msg="errors.first(`param-${param.key}.id`)"
                        >
                            <vuex-input
                                :ref="`paramId${index}Input`"
                                :data-vv-scope="`param-${param.key}`"
                                :disabled="disabled"
                                :handle-change="(name, value) => handleParamChange(name, value, index)"
                                v-validate.initial="`required|unique:${globalParams.map(p => p.key).join(',')}`"
                                name="key"
                                :placeholder="$t('nameInputTips')"
                                :value="param.key"
                            />
                        </bk-form-item>
                        <bk-form-item
                            label-width="auto"
                            class="flex-col-span-1"
                            :label="$t(`editPage.${getParamsDefaultValueLabel(param.valueType)}`)"
                            :is-error="errors.has(`param-${param.key}.defaultValue`)"
                            :error-msg="errors.first(`param-${param.key}.defaultValue`)"
                            :desc="$t(`editPage.${getParamsDefaultValueLabelTips(param.valueType)}`)"
                        >
                            <!-- 自定义变量展示 -->
                            <define-param-show
                                :param="param"
                                :global-params="globalParams"
                                :param-index="index"
                                @handleParamChange="handleParamChange"
                            />
                        </bk-form-item>
                    </div>
                    <bk-form-item
                        label-width="auto"
                        v-if="isSelectorParam(param.valueType)"
                        :label="$t('editPage.selectOptions')"
                        :desc="$t('editPage.optionsDesc')"
                        :is-error="errors.has(`param-${param.key}.options`)"
                        :error-msg="errors.first(`param-${param.key}.options`)"
                    >
                        <vuex-textarea
                            v-validate.initial="'excludeComma'"
                            :disabled="disabled"
                            :handle-change="(name, value) => editOption(name, value, index)"
                            name="options"
                            :data-vv-scope="`param-${param.key}`"
                            :placeholder="$t('editPage.optionTips')"
                            :value="getOptions(param)"
                        />
                    </bk-form-item>
                    <bk-form-item
                        label-width="auto"
                        :label="$t('editPage.chineseName')"
                    >
                        <vuex-input
                            :disabled="disabled"
                            :handle-change="(name, value) => handleParamChange(name, value, index)"
                            name="chineseName"
                            max-length="20"
                            :placeholder="$t('editPage.chineseNameTips')"
                            :value="param.chineseName"
                        />
                    </bk-form-item>
                    <bk-form-item
                        label-width="auto"
                        :label="$t('desc')"
                    >
                        <vuex-input
                            :disabled="disabled"
                            :handle-change="(name, value) => handleParamChange(name, value, index)"
                            name="desc"
                            :placeholder="$t('editPage.descTips')"
                            :value="param.desc"
                        />
                    </bk-form-item>
                </bk-form>
            </accordion>
        </draggable>
        <a
            class="text-link"
            v-if="!disabled"
            @click.stop.prevent="editParam({ index: value.length, isAdd: true })"
        >
            <i class="devops-icon icon-plus-circle" />
            <span>{{ $t('editPage.addParams') }}</span>
        </a>
    </div>
</template>

<script>
    import Accordion from '@/components/atomFormField/Accordion'
    import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'
    import Selector from '@/components/atomFormField/Selector'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import { deepCopy } from '@/utils/util'
    import draggable from 'vuedraggable'
    import atomFieldMixin from '../../atomFormField/atomFieldMixin'
    import DefineParamShow from './show.vue'

    import {
        CHECK_DEFAULT_PARAM,
        CHECK_PARAM_LIST,
        getParamsDefaultValueLabel,
        getParamsDefaultValueLabelTips,
        isEnumParam,
        isMultipleParam,
        STRING
    } from '@/store/modules/atom/paramsConfig'
    
    const getDefineParamList = () => {
        return CHECK_PARAM_LIST.map(item => {
            return {
                id: item.id,
                name: global.pipelineVue.$t(`storeMap.${item.name}`)
            }
        })
    }

    export default {
        name: 'define-param',
        components: {
            Selector,
            draggable,
            Accordion,
            VuexInput,
            VuexTextarea,
            AtomCheckbox,
            DefineParamShow
        },
        mixins: [atomFieldMixin],
        props: {
            name: {
                type: String,
                default: ''
            },
            settingKey: {
                type: String,
                default: 'params'
            },
            value: {
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
                paramIdCount: 0,
                paramsList: getDefineParamList()
            }
        },
        
        computed: {
            paramsDragOptions () {
                return {
                    ghostClass: 'sortable-ghost-atom',
                    chosenClass: 'sortable-chosen-atom',
                    handle: '.icon-move',
                    animation: 200,
                    disabled: this.disabled
                }
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
            isMultipleParam,
            getParamsDefaultValueLabel,
            getParamsDefaultValueLabelTips,
            getSelectorDefaultVal ({ valueType, value = '' }) {
                if (isMultipleParam(valueType)) {
                    return value && typeof value === 'string' ? value.split(',') : []
                }
                return value
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

                    this.handleParamChange(name, opts, index)

                    const param = this.value[index]
                    if (typeof param.value === 'string' && (isMultipleParam(param.valueType) || isEnumParam(param.valueType))) { // 选项清除时，修改对应的默认值
                        const dv = param.value.split(',').filter(v => param.options.map(k => k.key).includes(v))
                        if (isMultipleParam(param.valueType)) {
                            this.handleParamChange('value', dv, index)
                        } else {
                            this.handleParamChange('value', dv.join(','), index)
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
                        Object.assign(param, { required: false })
                    }
                    value.splice(index + 1, 0, param)
                } else {
                    value.splice(index, 1)
                }

                this.$emit('input', value)
            },

            isSelectorParam (type) {
                return isMultipleParam(type) || isEnumParam(type)
            },

            getOptions (param) {
                try {
                    return param.options.map(opt => opt.key === opt.value ? opt.key : `${opt.key}=${opt.value}`).join('\n')
                } catch (e) {
                    return ''
                }
            },
            
            handleParamTypeChange (key, value, paramIndex) {
                const params = this.globalParams
                const newParams = [
                    ...params.slice(0, paramIndex),
                    {
                        ...deepCopy(CHECK_DEFAULT_PARAM[value]),
                        key: params[paramIndex].key,
                        paramIdKey: params[paramIndex].paramIdKey
                    },
                    ...params.slice(paramIndex + 1)
                ]
              
                this.handleChange(this.name, newParams)
            },

            handleParamChange (key, value, paramIndex) {
                const param = this.globalParams[paramIndex]

                if (isMultipleParam(param.valueType) && key === 'value') {
                    Object.assign(param, {
                        [key]: value.join(',')
                    })
                } else if (param) {
                    Object.assign(param, {
                        [key]: value
                    })
                }
                this.handleChange(this.name, this.globalParams)
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
