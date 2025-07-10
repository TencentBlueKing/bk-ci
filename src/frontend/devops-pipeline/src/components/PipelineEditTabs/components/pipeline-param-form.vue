<template>
    <section>
        <bk-form
            form-type="vertical"
            class="new-ui-form"
            :key="param"
        >
            <form-field
                :hide-colon="true"
                :required="true"
                :label="idLabel"
                :is-error="errors.has(`pipelineParam.id`)"
                :error-msg="errors.first(`pipelineParam.id`)"
            >
                <vuex-input
                    :disabled="disabled"
                    :handle-change="(name, value) => handleUpdateParam(name, value)"
                    :data-vv-scope="'pipelineParam'"
                    v-validate="idValidRule"
                    name="id"
                    :placeholder="$t('nameInputTips')"
                    :value="param.id"
                />
            </form-field>

            <form-field
                :hide-colon="true"
                :label="nameLabel"
                :is-error="errors.has('pipelineParam.name')"
                :error-msg="errors.first('pipelineParam.name')"
            >
                <vuex-input
                    :disabled="disabled"
                    :handle-change="(name, value) => handleUpdateParam(name, value)"
                    :data-vv-scope="'pipelineParam'"
                    v-validate="`notInList:${getUniqueArgs('name')}`"
                    name="name"
                    :placeholder="$t('newui.pipelineParam.nameInputTips')"
                    :value="param.name"
                />
            </form-field>

            <form-field
                :hide-colon="true"
                :required="true"
                :label="typeLabel"
            >
                <selector
                    :popover-min-width="246"
                    :disabled="disabled"
                    name="type"
                    :clearable="false"
                    :list="paramsList"
                    :handle-change="(name, value) => handleParamTypeChange(name, value)"
                    :value="param.type"
                />
            </form-field>

            <param-value-option
                :param="param"
                :disabled="disabled"
                :value-required="paramType === 'constant'"
                :handle-change="handleUpdateParam"
            >
            </param-value-option>

            <form-field
                :hide-colon="true"
                :label="$t('desc')"
            >
                <vuex-textarea
                    :disabled="disabled"
                    :handle-change="(name, value) => handleUpdateParam(name, value)"
                    name="desc"
                    :placeholder="$t('editPage.descTips')"
                    :value="param.desc"
                />
            </form-field>

            <form-field
                :hide-colon="true"
                :label="$t('groupLabel')"
                :desc="$t('groupLabelTips')"
            >
                <select-input
                    :value="param.category"
                    name="category"
                    :disabled="disabled"
                    type="text"
                    :options="groupLabelList"
                    :handle-change="(name, value) => handleUpdateParam(name, value)"
                />
            </form-field>

            <template v-if="paramType !== 'constant'">
                <div class="param-checkbox-row">
                    <atom-checkbox
                        name="required"
                        :text="$t('editPage.showOnExec')"
                        :desc="$t('newui.pipelineParam.buildParamTips')"
                        :disabled="disabled"
                        :value="param.required"
                        :handle-change="(name, value) => handleUpdateParam(name, value)"
                    />
                    <atom-checkbox
                        name="valueNotEmpty"
                        class="neccessary-checkbox"
                        v-show="param.required"
                        :disabled="disabled"
                        :text="$t('editPage.required')"
                        :value="param.valueNotEmpty"
                        :handle-change="(name, value) => handleUpdateParam(name, value)"
                    />
                </div>
                <div class="param-checkbox-row">
                    <atom-checkbox
                        name="readOnly"
                        :disabled="disabled"
                        :text="$t('editPage.readOnlyOnRun')"
                        :desc="$t('newui.pipelineParam.readOnlyTips')"
                        :value="param.readOnly"
                        :handle-change="(name, value) => handleUpdateParam(name, value)"
                    />
                </div>
            </template>
            <Accordion
                show-content
                show-checkbox
            >
                <header slot="header">
                    {{ $t('editPage.controlOption') }}
                </header>
                <article slot="content">
                    <SubParameter
                        :title="$t('editPage.displayCondition')"
                        name="displayCondition"
                        :param="displayConditionList"
                        v-bind="displayConditionSetting"
                        :handle-change="handleUpdateDisplayCondition"
                    />
                </article>
            </Accordion>
        </bk-form>
    </section>
</template>

<script>
    import SelectInput from '@/components/AtomFormComponent/SelectInput/'
    import SubParameter from '@/components/AtomFormComponent/SubParameter'
    import Accordion from '@/components/atomFormField/Accordion'
    import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'
    import Selector from '@/components/atomFormField/Selector'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import validMixins from '@/components/validMixins'
    import { deepCopy } from '@/utils/util'
    import ParamValueOption from './children/param-value-option'

    import {
        CONST_TYPE_LIST,
        DEFAULT_PARAM,
        PARAM_LIST,
        STRING
    } from '@/store/modules/atom/paramsConfig'

    export default {
        components: {
            ParamValueOption,
            FormField,
            VuexInput,
            AtomCheckbox,
            Selector,
            VuexTextarea,
            SelectInput,
            Accordion,
            SubParameter
        },
        mixins: [validMixins],
        props: {
            editIndex: {
                type: Number,
                default: -1
            },
            paramType: {
                type: String,
                default: 'var'
            },
            editItem: {
                type: Object,
                default: () => ({})
            },
            disabled: {
                type: Boolean,
                default: false
            },
            globalParams: {
                type: Array,
                default: () => ([])
            },
            updateParam: {
                type: Function,
                required: true
            },
            resetEditItem: {
                type: Function,
                required: true
            }
        },
        data () {
            return {
                // 点开编辑框时的初始值
                initParamItem: {},
                param: {}
            }
        },
        computed: {
            idValidRule () {
                return this.paramType === 'constant'
                    ? `required|paramsIdRule|notInList:${this.getUniqueArgs('id')}|constVarRule|max:64`
                    : `required|paramsIdRule|notInList:${this.getUniqueArgs('id')}`
            },
            idLabel () {
                return this.paramType === 'constant' ? this.$t('newui.pipelineParam.constName') : this.$t('newui.pipelineParam.varName')
            },
            nameLabel () {
                return this.paramType === 'constant' ? this.$t('newui.pipelineParam.constAlias') : this.$t('newui.pipelineParam.varAlias')
            },
            typeLabel () {
                return this.paramType === 'constant' ? this.$t('newui.pipelineParam.constType') : this.$t('editPage.paramsType')
            },
            paramsList () {
                const list = PARAM_LIST.map(item => {
                    return {
                        id: item.id,
                        name: this.$t(`storeMap.${item.name}`)
                    }
                })
                const variableList = list.filter(item => item.id !== 'CHECKBOX')
                return this.paramType === 'constant' ? list.filter(item => CONST_TYPE_LIST.includes(item.id)) : variableList
            },
            groupLabelList () {
                if (!Array.isArray(this.globalParams)) {
                    return []
                }
                const ids = new Set()
                return this.globalParams.reduce((uniqueItems, item) => {
                    if (!ids.has(item.category) && item.category) {
                        ids.add(item.category)
                        uniqueItems.push({
                            id: item.category,
                            name: item.category
                        })
                    }
                    return uniqueItems
                }, [])
            },
            displayConditionList () {
                return {
                    paramType: 'list',
                    list: this.globalParams.filter(item => item.id !== this.param.id).map(item => ({
                        ...item,
                        key: item.id
                    }))
                    
                }
            },
            displayConditionSetting () {
                return {
                    atomValue: {
                        displayCondition: Object.keys(this.param.displayCondition ?? {}).map(key => ({
                            key,
                            value: this.param.displayCondition[key]
                        }))
                    }
                }
            }
        },
        created () {
            if (this.editIndex === -1) {
                this.param = deepCopy(DEFAULT_PARAM[STRING])
                if (this.paramType === 'constant') {
                    Object.assign(this.param, { constant: true, required: false })
                }
                this.resetEditItem(this.param)
            } else {
                this.param = deepCopy(this.editItem)
            }
            this.initParamItem = deepCopy(this.param)
        },
        methods: {
            handleParamTypeChange (key, value) {
                this.param = {
                    ...deepCopy(DEFAULT_PARAM[value]),
                    id: this.param.id,
                    name: this.param.name,
                    constant: this.paramType === 'constant'
                }
                this.resetEditItem(this.param)
            },
            handleUpdateParam (key, value) {
                Object.assign(this.param, { [key]: value })
                this.updateParam(key, value)
            },
            getUniqueArgs (field) {
                // 新增跟编辑时，list不一样
                return this.globalParams.map(p => p[field]).filter(item => item !== this.initParamItem[field]).join(',')
            },
            isParamChanged () {
                return JSON.stringify(this.initParamItem) !== JSON.stringify(this.param)
            },
            handleUpdateDisplayCondition (key, value) {
                const displayCondition = JSON.parse(value).reduce((acc, cur) => {
                    acc[cur.key] = cur.value
                    return acc
                }, {})
                this.handleUpdateParam(key, displayCondition)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .param-checkbox-row {
        margin-top: 24px;
        line-height: 20px;
    }
    .neccessary-checkbox {
        margin-left: 24px;
        border-left: 1px solid #D8D8D8;
        padding-left: 24px;
    }
</style>
