<template>
    <section>
        <slot name="versionParams"></slot>
        <bk-form form-type="vertical">
            <template v-if="sortCategory">
                <renderSortCategoryParams
                    v-for="key in sortedCategories"
                    :key="key"
                    :name="key"
                    :vertical="sortCategoryVertical"
                >
                    <template slot="content">
                        <div
                            v-for="param in paramsListMap[key]"
                            :key="param.id"
                        >
                            <render-param
                                v-bind="param"
                                :param="param"
                                ref="categoryRenderParam"
                                :is-in-param-set="isInParamSet"
                                :is-exec-preview="isExecPreview"
                                :disabled="disabled || param.isFollowTemplate"
                                :show-operate-btn="showOperateBtn"
                                :handle-set-parma-required="handleSetParmaRequired"
                                :handle-use-default-value="handleUseDefaultValue"
                                :highlight-changed-param="highlightChangedParam"
                                :handle-param-update="handleParamUpdate"
                                :handle-follow-template="(id) => handleFollowTemplate(followTemplateKey, id)"
                                @remove-param="handleRemoveParamItem"
                            />
                        </div>
                    </template>
                </renderSortCategoryParams>
            </template>
            <template v-else>
                <div
                    v-for="param in paramList"
                    :key="param.id"
                >
                    <render-param
                        v-bind="param"
                        :param="param"
                        ref="renderParam"
                        :is-exec-preview="isExecPreview"
                        :disabled="disabled || param.isFollowTemplate"
                        :show-operate-btn="showOperateBtn"
                        :handle-set-parma-required="handleSetParmaRequired"
                        :handle-use-default-value="handleUseDefaultValue"
                        :highlight-changed-param="highlightChangedParam"
                        :handle-param-update="handleParamUpdate"
                        :handle-follow-template="handleFollowTemplate"
                    />
                </div>
            </template>
        </bk-form>
    </section>
</template>

<script>
    import renderParam from '@/components/renderParam'
    import renderSortCategoryParams from '@/components/renderSortCategoryParams'
    import {
        BOOLEAN,
        BOOLEAN_LIST,
        CODE_LIB,
        CONTAINER_TYPE,
        ENUM,
        getBranchOption,
        getParamsGroupByLabel,
        GIT_REF,
        isCodelibParam,
        isEnumParam,
        isFileParam,
        isGitParam,
        isMultipleParam,
        isRemoteType,
        isRepoParam,
        isSvnParam,
        MULTIPLE,
        ParamComponentMap,
        REPO_REF,
        STRING,
        SUB_PIPELINE,
        SVN_TAG,
        TEXTAREA
    } from '@/store/modules/atom/paramsConfig'
    import { COMMON_PARAM_PREFIX, isObject, isShallowEqual } from '@/utils/util'

    export default {
        components: {
            renderSortCategoryParams,
            renderParam
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
            },
            sortCategoryVertical: {
                type: Boolean,
                default: false
            },
            isInParamSet: {
                type: Boolean,
                default: false
            },
            showOperateBtn: {
                type: Boolean,
                default: false
            },
            hideDeleted: {
                type: Boolean,
                default: false
            },
            handleUseDefaultValue: {
                type: Function,
                default: () => () => {}
            },
            handleSetParmaRequired: {
                type: Function,
                default: () => () => {}
            },
            followTemplateKey: {
                type: String,
                default: ''
            },
            handleFollowTemplate: {
                type: Function,
                default: () => () => {}
            },
            isExecPreview: {
                // 是否为执行预览页面
                type: Boolean,
                default: true
            }
        },
        data () {
            return {
                prevAffectedValues: {}
            }
        },
        computed: {
            paramPrefix () {
                return COMMON_PARAM_PREFIX
            },
            paramList () {
                return this.params.map(param => {
                    let restParam = {}
                    if (param.type !== STRING || param.type !== TEXTAREA) {
                        if (isRemoteType(param)) {
                            const isMultiple = param.type === 'MULTIPLE'
                            const val = (isMultiple && typeof this.paramValues?.[param.id] === 'string') ? this.paramValues[param.id].split(',').filter(i => i !== '') : this.paramValues?.[param.id]
                            const affected = this.getAffectedBy(param.payload.url)
                            const affectedChanged = this.detectChanged(this.prevAffectedValues?.[param.id], affected)
                            this.prevAffectedValues[param.id] = affected

                            restParam = {
                                ...restParam,
                                ...param.payload,
                                multiSelect: isMultiple,
                                value: isMultiple && !Array.isArray(val) ? [] : val,
                                allIdString: true,
                                paramValues: this.paramValues,
                                affected,
                                affectedChanged,
                                affectTips: affectedChanged && Object.keys(affected).length > 0 ? this.$t('relyChanged', [Object.keys(affected).join('/')]) : ''
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
                        fieldName: this.paramPrefix + param.id,
                        required: param.valueNotEmpty,
                        value: this.paramValues[param.id],
                        ...restParam,
                        ...(
                            isRepoParam(param.type)
                                ? {
                                    childrenOptions: this.getBranchOption(this.paramValues?.[param.id]?.['repo-name'])
                                }
                                : {}
                        ),
                        // eslint-disable-next-line
                        show: Object.keys(param.displayCondition ?? {}).every((key) => this.isEqual(this.paramValues[key], param.displayCondition[key])),
                        
                    }
                })
            },
            paramsListMap () {
                const list = this.hideDeleted ? this.paramList.filter(i => !i.isDelete) : this.paramList
                return getParamsGroupByLabel(list)?.listMap ?? {}
            },
            sortedCategories () {
                return getParamsGroupByLabel(this.paramList)?.sortedCategories ?? []
            }
            
        },
        methods: {
            isObject,
            getBranchOption,
            isEqual (a, b) {
                try {
                    // hack: 处理 undefined 和 '' 的情况
                    if (typeof a === 'undefined' && b === '') {
                        return true
                    }
                    return String(a) === String(b)
                } catch (error) {
                    return false
                }
            },
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
                return this.paramList.find(param => param.fieldName === name)
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
            },
            handleRemoveParamItem (id)  {
                this.$emit('remove-param', id)
            },
            getAffectedBy (originUrl) {
                try {
                    const PLUGIN_URL_PARAM_REG = /\{(.*?)(\?){0,1}\}/g
                    return originUrl.match(PLUGIN_URL_PARAM_REG).map(item => item.replace(/\{(\S+)\}/, '$1')).reduce((acc, key) => {
                        if (Object.hasOwnProperty.call(this.paramValues, key)) {
                            acc[key] = this.paramValues[key]
                        }
                        return acc
                    }, {})
                } catch (error) {
                    return {}
                }
            },
            detectChanged (prev, current) {
                if (prev && current) {
                    return !isShallowEqual(prev, current)
                }
                return false
            },
            async validateAll () {
                const refsList = this.sortCategory ? this.$refs.categoryRenderParam : this.$refs.renderParam
                for (let i = 0; i < refsList.length; i++) {
                    const ref = refsList[i]
                    const res = await ref.$validator?.validateAll?.()
                    console.log(res, 'validate res')
                    if (!res) {
                        return false
                    }
                    
                }
                return true
            }
        }
    }
</script>
