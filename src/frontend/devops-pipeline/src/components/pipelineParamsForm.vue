<template>
    <section>
        <slot name="versionParams"></slot>
        <bk-form
            form-type="vertical"
            :class="{
                'pipeline-execute-params-form': true,
                'is-category': sortCategory
            }"
        >
            <template v-if="sortCategory">
                <renderSortCategoryParams
                    v-for="(list, key) in renderParamList"
                    :key="key"
                    :name="key"
                >
                    <template slot="content">
                        <form-field
                            v-for="param in list"
                            :key="param.id"
                            :required="param.required"
                            :is-error="errors.has('devops' + param.name)"
                            :error-msg="errors.first('devops' + param.name)"
                            :label="param.label || param.id"
                        >
                            <section class="component-row">
                                <component
                                    :is="param.component"
                                    v-validate="{ required: param.required, objectRequired: isObject(param.value) }"
                                    :click-unfold="true"
                                    :show-select-all="true"
                                    :handle-change="handleParamUpdate"
                                    flex
                                    v-bind="Object.assign({}, param, { id: undefined, name: 'devops' + param.name })"
                                    :class="{
                                        'is-diff-param': highlightChangedParam && param.isChanged
                                    }"
                                    :disabled="disabled"
                                    :placeholder="param.placeholder"
                                    :is-diff-param="highlightChangedParam && param.isChanged"
                                    :enable-version-control="param.enableVersionControl"
                                    :random-sub-path="param.latestRandomStringInPath"
                                />
                            </section>
                            <span
                                v-if="!errors.has('devops' + param.name)"
                                :class="['preview-params-desc', param.type === 'TEXTAREA' ? 'params-desc-styles' : '']"
                                :title="param.desc"
                            >
                                {{ param.desc }}
                            </span>
                        </form-field>
                    </template>
                </renderSortCategoryParams>
            </template>
            <template v-else>
                <form-field
                    v-for="param in paramList"
                    :key="param.id"
                    :required="param.required"
                    :is-error="errors.has('devops' + param.name)"
                    :error-msg="errors.first('devops' + param.name)"
                    :label="param.label || param.id"
                >
                    <section class="component-row">
                        <component
                            :is="param.component"
                            v-validate="{ required: param.required, objectRequired: isObject(param.value) }"
                            :click-unfold="true"
                            :show-select-all="true"
                            :handle-change="handleParamUpdate"
                            flex
                            v-bind="Object.assign({}, param, { id: undefined, name: 'devops' + param.name })"
                            :class="{
                                'is-diff-param': highlightChangedParam && param.isChanged
                            }"
                            :disabled="disabled"
                            :placeholder="param.placeholder"
                            :is-diff-param="highlightChangedParam && param.isChanged"
                            :enable-version-control="param.enableVersionControl"
                            :random-sub-path="param.latestRandomStringInPath"
                        />
                    </section>
                    <span
                        v-if="!errors.has('devops' + param.name)"
                        :class="['preview-params-desc', param.type === 'TEXTAREA' ? 'params-desc-styles' : '']"
                        :title="param.desc"
                    >
                        {{ param.desc }}
                    </span>
                </form-field>
            </template>
        </bk-form>
    </section>
</template>

<script>
    import EnumInput from '@/components/atomFormField/EnumInput'
    import RequestSelector from '@/components/atomFormField/RequestSelector'
    import Selector from '@/components/atomFormField/Selector'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import metadataList from '@/components/common/metadata-list'
    import FileParamInput from '@/components/atomFormField/FileParamInput'
    import CascadeRequestSelector from '@/components/atomFormField/CascadeRequestSelector'
    import renderSortCategoryParams from '@/components/renderSortCategoryParams'
    import { isObject } from '@/utils/util'
    import {
        BOOLEAN,
        BOOLEAN_LIST,
        CODE_LIB,
        CONTAINER_TYPE,
        ENUM,
        GIT_REF,
        isCodelibParam,
        isEnumParam,
        isFileParam,
        isGitParam,
        isMultipleParam,
        isRemoteType,
        isSvnParam,
        MULTIPLE,
        ParamComponentMap,
        STRING,
        SUB_PIPELINE,
        SVN_TAG,
        TEXTAREA,
        REPO_REF,
        getBranchOption,
        isRepoParam
    } from '@/store/modules/atom/paramsConfig'

    export default {

        components: {
            Selector,
            RequestSelector,
            EnumInput,
            VuexInput,
            VuexTextarea,
            FormField,
            metadataList,
            FileParamInput,
            CascadeRequestSelector,
            renderSortCategoryParams
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
            },
            highlightChangedParam: Boolean,
            sortCategory: {
                type: Boolean,
                default: false
            }
        },
        computed: {
            paramList () {
                return this.params.map(param => {
                    let restParam = {}
                    if (param.type !== STRING || param.type !== TEXTAREA) {
                        if (isRemoteType(param)) {
                            restParam = {
                                ...restParam,
                                ...param.payload,
                                multiSelect: param.type === 'MULTIPLE',
                                value: param.type === 'MULTIPLE' ? this.paramValues?.[param.id]?.split(',') : this.paramValues[param.id]
                            }
                        } else {
                            restParam = {
                                ...restParam,
                                displayKey: 'value',
                                settingKey: 'key',
                                list: this.getParamOpt(param)
                            }
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

                    if (!param.searchUrl && !isRemoteType(param)) {
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

                    if (isFileParam(param.type)) {
                        // 预览时，重新上传文件，会把文件类型的value变成对象而非字符串，这时要更新随机串回显到页面上
                        const paramValue = this.paramValues[param.id]
                        const newRandomString = paramValue?.latestRandomStringInPath
                        const defaultRandomString = param.latestRandomStringInPath ?? param.randomStringInPath
                        restParam.latestRandomStringInPath = newRandomString ?? defaultRandomString
                        restParam.value = typeof paramValue === 'object' ? paramValue?.directory : paramValue
                    }
                    return {
                        ...param,
                        component: this.getParamComponentType(param),
                        name: param.id,
                        required: param.valueNotEmpty,
                        value: this.paramValues[param.id],
                        ...restParam,
                        ...(
                            isRepoParam(param.type)
                                ? {
                                    childrenOptions: this.getBranchOption(this.paramValues?.[param.id]?.['repo-name'])
                                }
                                : {}
                        )
                    }
                })
            },
            renderParamList () {
                // 将参数列表按照分组进行分组,未分组的参数放到一个分组里
                const key = this.$t('notGrouped')
                const listMap = this.paramList.reduce((acc, item) => {
                    const categoryKey = item.category || key
                    if (!acc[categoryKey]) {
                        acc[categoryKey] = []
                    }
                    acc[categoryKey].push(item)
                    return acc
                }, {})
                
                if (!(key in listMap)) {
                    return listMap
                }
                const { [key]: value, ...rest } = listMap
                return { [key]: value, ...rest }
            }
        },
        methods: {
            isObject,
            getBranchOption,
            getParamComponentType (param) {
                if (isRemoteType(param)) {
                    return 'request-selector'
                } else {
                    return ParamComponentMap[param.type]
                }
            },
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
                    case param.type === REPO_REF:
                        return param.options
                    default:
                        return []
                }
            },
            getCodeRepoOpt (param) {
                switch (true) {
                    case param.type === REPO_REF:
                        return param.options
                    default:
                        return []
                }
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
    .pipeline-execute-params-form {
        display: grid;
        grid-template-columns: repeat(2, minmax(200px, 1fr));
        grid-gap: 0 24px;
        &.is-category {
            grid-template-columns: repeat(1, minmax(200px, 1fr));
        }
        &.bk-form.bk-form-vertical .bk-form-item+.bk-form-item {
            margin-top: 0 !important;
        }

        .component-row {
            display: flex;
            position: relative;
            .metadata-box {
                position: relative;
                display: none;
            }

            .bk-select {
                &:not(.is-disabled) {
                    background: white;
                }
                width: 100%;
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
    .is-diff-param {
        border-color: #FF9C01 !important;
    }
</style>
