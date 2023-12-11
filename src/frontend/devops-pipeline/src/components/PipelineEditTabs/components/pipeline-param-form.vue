<template>
    <section>
        <bk-form form-type="vertical" class="new-ui-form" :key="param">
            <form-field :required="true" :label="idLabel" :is-error="errors.has(`pipelineParam.id`)" :error-msg="errors.first(`pipelineParam.id`)">
                <vuex-input :disabled="disabled" :handle-change="(name, value) => handleUpdateParam(name, value)" :data-vv-scope="'pipelineParam'" v-validate="`required|unique:${globalParams.map(p => p.id).join(',')}`" name="id" :placeholder="$t('nameInputTips')" :value="param.id" />
            </form-field>

            <form-field :label="nameLabel" :is-error="errors.has('pipelineParam.name')" :error-msg="errors.first('pipelineParam.name')">
                <vuex-input :disabled="disabled" :handle-change="(name, value) => handleUpdateParam(name, value)" :data-vv-scope="'pipelineParam'" v-validate.initial="`unique:${globalParams.map(p => p.name).join(',')}`" name="name" :placeholder="$t('请输入别名')" :value="param.name" />
            </form-field>

            <form-field :required="true" :label="typeLabel">
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

            <form-field :label="$t('desc')">
                <vuex-textarea :disabled="disabled" :handle-change="(name, value) => handleUpdateParam(name, value)" name="desc" :placeholder="$t('editPage.descTips')" :value="param.desc" />
            </form-field>

            <template v-if="paramType !== 'constant'">
                <div class="param-checkbox-row">
                    <atom-checkbox
                        name="required"
                        :text="$t('editPage.showOnExec')"
                        :disabled="disabled"
                        :value="param.required"
                        :handle-change="(name, value) => handleUpdateParam(name, value)" />
                    <i class="bk-icon icon-question-circle-shape" v-bk-tooltips="$t('editPage.入参tips')" />
                    <atom-checkbox
                        name="valueNotEmpty"
                        class="neccessary-checkbox"
                        v-if="param.required"
                        :disabled="disabled"
                        :text="$t('editPage.required')"
                        :value="param.valueNotEmpty"
                        :handle-change="(name, value) => handleUpdateParam(name, value)" />
                </div>
                <div class="param-checkbox-row">
                    <atom-checkbox
                        name="readOnly"
                        :disabled="disabled"
                        :text="$t('editPage.readOnlyOnRun')"
                        :value="param.readOnly"
                        :handle-change="(name, value) => handleUpdateParam(name, value)" />
                    <i class="bk-icon icon-question-circle-shape" v-bk-tooltips="$t('editPage.只读tips')" />
                </div>
            </template>
        </bk-form>
    </section>
</template>

<script>
    import Vue from 'vue'
    import { deepCopy } from '@/utils/util'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import Selector from '@/components/atomFormField/Selector'
    import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'
    import validMixins from '@/components/validMixins'
    import ParamValueOption from './children/param-value-option'

    import {
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
            VuexTextarea
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
                param: {}
            }
        },
        computed: {
            idLabel () {
                return this.paramType === 'constant' ? this.$t('常量名') : this.$t('变量名')
            },
            nameLabel () {
                return this.paramType === 'constant' ? this.$t('常量别名') : this.$t('变量别名')
            },
            typeLabel () {
                return this.paramType === 'constant' ? this.$t('常量类型') : this.$t('editPage.paramsType')
            },
            paramsList () {
                return PARAM_LIST.map(item => {
                    return {
                        id: item.id,
                        name: this.$t(`storeMap.${item.name}`)
                    }
                })
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
                Object.assign(this.param, this.editItem)
            }
            Vue.set(this.param, 'required', this.param.required || true)
        },
        methods: {
            handleParamTypeChange (key, value) {
                this.param = {
                    ...deepCopy(DEFAULT_PARAM[value]),
                    id: this.param.id,
                    name: this.param.name
                }
                this.resetEditItem(this.param)
            },
            handleUpdateParam (key, value) {
                Object.assign(this.param, { [key]: value })
                this.updateParam(key, value)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .icon-question-circle-shape {
        margin-left: 16px;
    }
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
