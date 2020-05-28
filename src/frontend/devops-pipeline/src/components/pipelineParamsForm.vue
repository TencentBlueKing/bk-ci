<template>
    <div style="text-align: left">
        <form class="bk-form" action="http://localhost" target="previewHiddenIframe" ref="previewParamsForm" onsubmit="return false;">
            <form-field v-for="(param, index) in paramList"
                :key="param.id" :required="param.required"
                :is-error="errors.has(param.id)"
                :error-msg="errors.first(param.id)"
                :label="param.id">
                <section class="component-row">
                    <component :is="param.component" v-validate="{ required: param.required }" :handle-change="handleParamUpdate" v-bind="param" :disabled="disabled" style="width: 100%;"></component>
                    <span class="meta-data" v-show="showMetadata(param.type, param.value)">{{ $t('metaData') }}
                        <aside class="metadata-box">
                            <metadata-list :is-left-render="(index % 2) === 1" :path="param.type === 'ARTIFACTORY' ? param.value : ''"></metadata-list>
                        </aside>
                    </span>
                </section>
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
    import metadataList from '@/components/common/metadata-list'
    import { BOOLEAN_LIST, isMultipleParam, isEnumParam, isSvnParam, isGitParam, isCodelibParam, ParamComponentMap, STRING, BOOLEAN, MULTIPLE, ENUM, SVN_TAG, GIT_REF, CODE_LIB, CONTAINER_TYPE, ARTIFACTORY, SUB_PIPELINE } from '@/store/modules/atom/paramsConfig'

    export default {

        components: {
            Selector,
            EnumInput,
            VuexInput,
            FormField,
            metadataList
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

                    if (isMultipleParam(param.type)) { // 去除不在选项里面的值
                        const mdv = this.getMultiSelectorValue(this.paramValues[param.id], param.options.map(v => v.key))
                        const mdvStr = mdv.join(',')
                        // debugger
                        Object.assign(restParam, {
                            multiSelect: true,
                            value: mdv
                        })

                        if (this.paramValues[param.id] !== mdvStr) {
                            this.handleParamChange(param.id, mdvStr)
                        }
                    } else if (isEnumParam(param.type) || isSvnParam(param.type) || isGitParam(param.type) || isCodelibParam(param.type)) { // 若默认值不在选项里，清除对应的默认值
                        if (this.paramValues[param.id] && !param.options.find(opt => opt.key === this.paramValues[param.id])) {
                            this.handleParamChange(param.id, '')
                            Object.assign(restParam, {
                                value: ''
                            })
                        }
                    }
                    return {
                        ...param,
                        component: ParamComponentMap[param.type],
                        name: 'devops' + param.id,
                        id: undefined,
                        required: param.type === SVN_TAG || param.type === GIT_REF,
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
                    case param.type === GIT_REF:
                    case param.type === CODE_LIB:
                    case param.type === CONTAINER_TYPE:
                    case param.type === ARTIFACTORY:
                    case param.type === SUB_PIPELINE:
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
            },
            showMetadata (type, value) {
                return type === 'ARTIFACTORY' && value && this.$route.path.indexOf('preview')
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/scss/conf';
    .component-row {
        display: flex;
        .metadata-box {
            position: relative;
            display: none;
        }
        .meta-data {
            margin-top: 8px;
            margin-left: 10px;
            font-size: 12px;
            color: $primaryColor;
            white-space: nowrap;
            cursor: pointer;
        }
        .meta-data:hover {
            .metadata-box {
                display: block;
            }
        }
    }
</style>
