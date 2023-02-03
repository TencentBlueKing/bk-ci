<template>
    <div style="text-align: left">
        <form class="bk-form" action="http://localhost" target="previewHiddenIframe" ref="previewParamsForm" onsubmit="return false;">
            <form-field v-for="param in paramList"
                :key="param.id" :required="param.required"
                :is-error="errors.has('devops' + param.name)"
                :error-msg="errors.first('devops' + param.name)"
                :label="param.label || param.id"
                :style="{ width: param.width }"
            >
                <section class="component-row">
                    <component :is="param.component" v-validate="{ required: param.required }" :click-unfold="true" :show-select-all="true" :handle-change="handleParamUpdate" v-bind="Object.assign({}, param, { id: undefined, name: 'devops' + param.name })" :disabled="disabled" style="width: 100%;" :placeholder="param.placeholder"></component>
                    <div class="file-upload" v-if="showFileUploader(param.type)">
                        <file-param-input :file-path="param.value"></file-param-input>
                    </div>
                </section>
                <span v-if="!errors.has('devops' + param.name)" :class="['preview-params-desc', param.type === 'TEXTAREA' ? 'params-desc-styles' : '']" :title="param.desc">{{ param.desc }}</span>
            </form-field>
        </form>
        <iframe v-show="false" name="previewHiddenIframe"></iframe>
    </div>
</template>

<script>
    import VuexInput from '@/components/atomFormField/VuexInput'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import Selector from '@/components/atomFormField/Selector'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import metadataList from '@/components/common/metadata-list'
    import FileParamInput from '@/components/FileParamInput'
    import {
        BOOLEAN_LIST,
        isMultipleParam,
        isEnumParam,
        isSvnParam,
        isGitParam,
        isCodelibParam,
        isFileParam,
        ParamComponentMap,
        STRING,
        BOOLEAN,
        MULTIPLE,
        ENUM,
        SVN_TAG,
        GIT_REF,
        CODE_LIB,
        CONTAINER_TYPE,
        SUB_PIPELINE,
        TEXTAREA
    } from '@/store/modules/atom/paramsConfig'

    export default {

        components: {
            Selector,
            EnumInput,
            VuexInput,
            VuexTextarea,
            FormField,
            metadataList,
            FileParamInput
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
                    if (param.type !== STRING || param.type !== TEXTAREA) {
                        restParam = {
                            ...restParam,
                            displayKey: 'value',
                            settingKey: 'key',
                            list: this.getParamOpt(param)
                        }

                        // codeLib 接口返回的数据没有匹配的默认值,导致回显失效，兼容加上默认值
                        if (param.type === CODE_LIB) {
                            const value = this.paramValues[param.id]
                            const listItemIndex = restParam.list && restParam.list.findIndex(i => i.value === value)
                            if (listItemIndex < 0 && value) {
                                restParam.list.push({
                                    key: value,
                                    value: value
                                })
                            }
                        }
                    }

                    if (!param.searchUrl) {
                        if (isMultipleParam(param.type)) { // 去除不在选项里面的值
                            const mdv = this.getMultiSelectorValue(this.paramValues[param.id], param.options.map(v => v.key))
                            const mdvStr = mdv.join(',')

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
                    }
                    return {
                        ...param,
                        component: ParamComponentMap[param.type],
                        name: param.id,
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
                return this.paramList.find(param => `devops${param.name}` === name)
            },

            handleParamUpdate (name, value) {
                const param = this.getParamByName(name)
                if (isMultipleParam(param.type)) { // 复选框，需要将数组转化为逗号隔开的字符串
                    value = Array.isArray(value) ? value.join(',') : ''
                }
                this.handleParamChange(param.name, value)
            },
            showFileUploader (type) {
                return isFileParam(type) && this.$route.path.indexOf('preview') > -1
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/scss/conf';
    @import '@/scss/mixins/ellipsis';

    .component-row {
        display: flex;
        .metadata-box {
            position: relative;
            display: none;
        }
        .meta-data {
            align-self: center;
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
        .file-upload {
            display: flex;
            margin-left: 10px;
            ::v-deep .bk-upload.button {
                position: static;
                display: flex;
                .file-wrapper {
                    margin-bottom: 0;
                    height: 32px;
                }
                p.tip {
                    white-space: nowrap;
                    position: static;
                    margin-left: 8px;
                }
                .all-file {
                    width: 100%;
                    position: absolute;
                    right: 0;
                    top: 0;
                    .file-item {
                        margin-bottom: 0;
                    }
                    .error-msg {
                        margin: 0
                    }
                }
            }
        }
    }
    .preview-params-desc {
        color: #999;
        width: 100%;
        font-size: 12px;
        @include ellipsis();
    }
    .params-desc-styles {
        margin-top: 32px;
    }
</style>
