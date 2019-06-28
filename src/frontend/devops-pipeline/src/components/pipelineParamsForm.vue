<template>
    <div style="text-align: left">
        <form class="bk-form" target="previewHiddenIframe" ref="previewParamsForm" onsubmit="return false;">
            <form-field v-for="param in paramList"
                :key="param.id" :required="param.required"
                :is-error="errors.has(param.id)"
                :error-msg="errors.first(param.id)"
                :label="param.id">
                <component :is="param.component" v-validate="{ required: param.required }" :handle-change="handleParamUpdate" v-bind="param" :disabled="disabled"></component>
                <span v-if="!errors.has(param.id)" style="color: #63656E; position:static" :title="param.desc" class="bk-form-help">{{ param.desc }}</span>
            </form-field>
        </form>
        <iframe v-show="false" name="previewHiddenIframe"></iframe>
    </div>
</template>

<script>
    import VuexInput from '@/components/atomFormField/VuexInput'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import Selector from '@/components/atomFormField/Selector'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import { BOOLEAN_LIST, isMultipleParam, isEnumParam, isSvnParam, isCodelibParam, ParamComponentMap, STRING, BOOLEAN, MULTIPLE, ENUM, SVN_TAG, CODE_LIB, CONTAINER_TYPE } from '@/store/modules/atom/paramsConfig'

    export default {

        components: {
            Selector,
            EnumInput,
            VuexInput,
            FormField
        },
        props: {
            disabled: {
                type: Boolean,
                default: false
            },
            paramValues: {
                type: Object,
                default: () => ({})
            },
            params: {
                type: Array,
                default: []
            },
            handleParamChange: {
                type: Function,
                default: () => () => {}
            }
        },

        computed: {
            paramList () {
                return this.params.map(param => {
                    let restParam = {}
                    if (param.type !== STRING) {
                        restParam = {
                            ...restParam,
                            displayKey: 'value',
                            settingKey: 'key',
                            list: this.getParamOpt(param)
                        }
                    }

                    if (isMultipleParam(param.type) || isEnumParam(param.type) || isSvnParam(param.type) || isCodelibParam(param.type)) {
                        const mdv = this.getMultiSelectorValue(this.paramValues[param.id], param.options.map(v => v.key))
                        const mdvStr = mdv.join(',')
                        Object.assign(restParam, {
                            multiSelect: isMultipleParam(param.type),
                            value: mdv
                        })

                        if (this.paramValues[param.id] !== mdvStr) {
                            this.handleParamChange(param.id, mdvStr)
                        }
                    }
                    return {
                        ...param,
                        component: ParamComponentMap[param.type],
                        name: param.id,
                        required: param.type === SVN_TAG,
                        value: this.paramValues[param.id],
                        ...restParam
                    }
                })
            }
        },

        methods: {
            getParamOpt (param) {
                switch (true) {
                    case param.type === BOOLEAN:
                        return BOOLEAN_LIST
                    case param.type === ENUM:
                    case param.type === MULTIPLE:
                    case param.type === SVN_TAG:
                    case param.type === CODE_LIB:
                    case param.type === CONTAINER_TYPE:
                        return param.options
                    default:
                        return []
                }
            },
            submitForm () {
                // 触发表单默认提交事件，保存用户输入
                this.$refs.previewParamsForm && this.$refs.previewParamsForm.submit()
            },
            getMultiSelectorValue (value = '', options) {
                if (typeof value === 'string' && value) { // remove invalid option
                    return value.split(',').filter(v => options.includes(v))
                }
                return []
            },

            getParamByName (name) {
                return this.paramList.find(param => param.name === name)
            },

            handleParamUpdate (name, value) {
                const param = this.getParamByName(name)
                if (isMultipleParam(param.type)) { // 复选框，需要将数组转化为逗号隔开的字符串
                    value = Array.isArray(value) ? value.join(',') : ''
                }
                this.handleParamChange(name, value)
            }
        }
    }
</script>
