<template>
    <div class="build-params-comp">
        <template>
            <accordion show-checkbox :show-content="hasGlobalParams">
                <template slot="header">
                    <span>
                        {{ title }}
                        <bk-popover placement="right">
                            <i style="display:block;" class="bk-icon icon-info-circle"></i>
                            <div slot="content" style="white-space: pre-wrap;">
                                <div> {{ $t('editPage.paramsTips') }} </div>
                            </div>
                        </bk-popover>
                    </span>
                </template>
                <template slot="content">
                    <div class="no-prop" v-if="!hasGlobalParams">
                        <bk-button theme="primary" :disabled="disabled" @click="editParam(null, true)">{{ $t('editPage.addParams') }}</bk-button>
                    </div>
                    <template v-else>
                        <draggable v-model="globalParams" :options="paramsDragOptions">
                            <accordion v-for="(param, index) in globalParams" :key="param.paramIdKey" :is-error="errors.any(`param-${param.id}`)">
                                <header class="param-header" slot="header">
                                    <span>
                                        <bk-popover style="vertical-align: middle" v-if="errors.all(`param-${param.id}`).length" placement="top">
                                            <i class="bk-icon icon-info-circle-shape"></i>
                                            <div slot="content">
                                                <p v-for="error in errors.all(`param-${param.id}`)" :key="error">{{ error }}</p>
                                            </div>
                                        </bk-popover>
                                        {{ param.id }}
                                    </span>
                                    <i v-if="!disabled && !isTemplateParams" @click.stop.prevent="editParamShow(index)" class="devops-icon" :class="[`${param.required ? 'icon-eye' : 'icon-eye-slash'}`]" />
                                    <i v-if="!disabled" class="devops-icon icon-move" />
                                    <i v-if="!disabled" @click.stop.prevent="editParam(index, false)" class="devops-icon icon-minus" />
                                </header>
                                <bk-form slot="content">
                                    <div class="params-flex-col">
                                        <bk-form-item label-width="auto" :label="$t('editPage.paramsType')" class="flex-col-span-1">
                                            <selector
                                                :popover-min-width="246"
                                                :data-vv-scope="`param-${param.id}`"
                                                :disabled="disabled"
                                                name="type"
                                                :list="paramsList"
                                                :handle-change="(name, value) => handleParamTypeChange(name, value, index)"
                                                :value="param.type"
                                            />
                                        </bk-form-item>
                                        <bk-form-item label-width="auto" class="flex-col-span-1" v-if="!isTemplateParams">
                                            <atom-checkbox :disabled="disabled" :text="$t('editPage.showOnStarting')" :value="param.required" name="required" :handle-change="(name, value) => handleUpdateParam(name, value, index)" />
                                        </bk-form-item>
                                    </div>
                                    <div class="params-flex-col pt10">
                                        <bk-form-item label-width="auto" class="flex-col-span-1" :label="$t('name')" :is-error="errors.has(`param-${param.id}.id`)" :error-msg="errors.first(`param-${param.id}.id`)">
                                            <vuex-input :ref="`paramId${index}Input`" :data-vv-scope="`param-${param.id}`" :disabled="disabled" :handle-change="(name, value) => handleUpdateParamId(name, value, index)" v-validate.initial="`required|unique:${validateParams.map(p => p.id).join(',')}`" name="id" :placeholder="$t('nameInputTips')" :value="param.id" />
                                        </bk-form-item>
                                        <bk-form-item label-width="auto" class="flex-col-span-1" :label="$t(`editPage.${getParamsDefaultValueLabel(param.type)}`)" :required="isBooleanParam(param.type)" :is-error="errors.has(`param-${param.id}.defaultValue`)" :error-msg="errors.first(`param-${param.id}.defaultValue`)" :desc="$t(`editPage.${getParamsDefaultValueLabelTips(param.type)}`)">
                                            <selector
                                                style="max-width: 250px"
                                                :popover-min-width="250"
                                                v-if="isSelectorParam(param.type)"
                                                :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                                                :list="transformOpt(param.options)"
                                                :multi-select="isMultipleParam(param.type)"
                                                name="defaultValue"
                                                :data-vv-scope="`param-${param.id}`"
                                                :placeholder="$t('editPage.defaultValueTips')"
                                                :disabled="disabled"
                                                show-select-all
                                                :key="param.type"
                                                :value="getSelectorDefaultVal(param)"
                                            >
                                            </selector>
                                            <enum-input
                                                v-if="isBooleanParam(param.type)"
                                                name="defaultValue"
                                                :list="boolList"
                                                :disabled="disabled"
                                                :data-vv-scope="`param-${param.id}`"
                                                :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                                                :value="param.defaultValue">
                                            </enum-input>
                                            <vuex-input v-if="isStringParam(param.type) || isSvnParam(param.type) || isGitParam(param.type) || isFileParam(param.type)" :disabled="disabled" :handle-change="(name, value) => handleUpdateParam(name, value, index)" name="defaultValue" :click-unfold="true" :data-vv-scope="`param-${param.id}`" :placeholder="$t('editPage.defaultValueTips')" :value="param.defaultValue" />
                                            <vuex-textarea v-if="isTextareaParam(param.type)" :click-unfold="true" :hover-unfold="true" :disabled="disabled" :handle-change="(name, value) => handleUpdateParam(name, value, index)" name="defaultValue" :data-vv-scope="`param-${param.id}`" :placeholder="$t('editPage.defaultValueTips')" :value="param.defaultValue" />
                                            <request-selector v-if="isCodelibParam(param.type)" :popover-min-width="250" :url="getCodeUrl(param.scmType)" v-bind="codelibOption" :disabled="disabled" name="defaultValue" :value="param.defaultValue" :handle-change="(name, value) => handleUpdateParam(name, value, index)" :data-vv-scope="`param-${param.id}`"></request-selector>
                                            <request-selector v-if="isBuildResourceParam(param.type)" :popover-min-width="250" :url="getBuildResourceUrl(param.containerType)" param-id="name" :disabled="disabled" name="defaultValue" :value="param.defaultValue" :handle-change="(name, value) => handleUpdateParam(name, value, index)" :data-vv-scope="`param-${param.id}`" :replace-key="param.replaceKey" :search-url="param.searchUrl"></request-selector>
                                            <request-selector v-if="isSubPipelineParam(param.type)" :popover-min-width="250" v-bind="subPipelineOption" :disabled="disabled" name="defaultValue" :value="param.defaultValue" :handle-change="(name, value) => handleUpdateParam(name, value, index)" :data-vv-scope="`param-${param.id}`" :replace-key="param.replaceKey" :search-url="param.searchUrl"></request-selector>
                                        </bk-form-item>
                                    </div>

                                    <bk-form-item label-width="auto" v-if="isSelectorParam(param.type)" :label="$t('editPage.selectOptions')" :desc="$t('editPage.optionsDesc')" :is-error="errors.has(`param-${param.id}.options`)" :error-msg="errors.first(`param-${param.id}.options`)">
                                        <vuex-textarea v-validate.initial="'excludeComma'" :disabled="disabled" :handle-change="(name, value) => editOption(name, value, index)" name="options" :data-vv-scope="`param-${param.id}`" :placeholder="$t('editPage.optionTips')" :value="getOptions(param)"></vuex-textarea>
                                    </bk-form-item>

                                    <bk-form-item label-width="auto" v-if="isSvnParam(param.type)" :label="$t('editPage.svnParams')" :is-error="errors.has(`param-${param.id}.repoHashId`)" :error-msg="errors.first(`param-${param.id}.repoHashId`)">
                                        <request-selector v-bind="getRepoOption('CODE_SVN')" :disabled="disabled" name="repoHashId" :value="param.repoHashId" :handle-change="(name, value) => handleUpdateParam(name, value, index)" :data-vv-scope="`param-${param.id}`" v-validate.initial="'required'" :replace-key="param.replaceKey" :search-url="param.searchUrl"></request-selector>
                                    </bk-form-item>

                                    <bk-form-item label-width="auto" v-if="isSvnParam(param.type)" :label="$t('editPage.relativePath')" :is-error="errors.has(`param-${param.id}.relativePath`)" :error-msg="errors.first(`param-${param.id}.relativePath`)">
                                        <vuex-input :disabled="disabled" :handle-change="(name, value) => handleUpdateParam(name, value, index)" name="relativePath" :data-vv-scope="`param-${param.id}`" :placeholder="$t('editPage.relativePathTips')" :value="param.relativePath"></vuex-input>
                                    </bk-form-item>

                                    <bk-form-item label-width="auto" v-if="isGitParam(param.type)" :label="$t('editPage.gitRepo')" :is-error="errors.has(`param-${param.id}.repoHashId`)" :error-msg="errors.first(`param-${param.id}.repoHashId`)">
                                        <request-selector v-bind="getRepoOption('CODE_GIT,CODE_GITLAB,GITHUB,CODE_TGIT')" :disabled="disabled" name="repoHashId" :value="param.repoHashId" :handle-change="(name, value) => handleUpdateParam(name, value, index)" :data-vv-scope="`param-${param.id}`" v-validate.initial="'required'" replace-key="{keyword}" :search-url="getSearchUrl()"></request-selector>
                                    </bk-form-item>

                                    <bk-form-item label-width="auto" v-if="isCodelibParam(param.type)" :label="$t('editPage.codelibParams')" :is-error="errors.has(`param-${param.id}.scmType`)" :error-msg="errors.first(`param-${param.id}.scmType`)">
                                        <selector :disabled="disabled" :list="codeTypeList" :handle-change="(name, value) => handleCodeTypeChange(name, value, index)" name="scmType" :data-vv-scope="`param-${param.id}`" placeholder="" :value="param.scmType"></selector>
                                    </bk-form-item>

                                    <template v-if="isBuildResourceParam(param.type)">
                                        <bk-form-item label-width="auto" :label="$t('editPage.buildEnv')" :is-error="errors.has(`param-${param.id}.os`)" :error-msg="errors.first(`param-${param.id}.os`)">
                                            <selector :popover-min-width="510" :disabled="disabled" :list="baseOSList" :handle-change="(name, value) => handleBuildResourceChange(name, value, index, param)" name="os" :data-vv-scope="`param-${param.id}`" placeholder="" :value="param.containerType.os"></selector>
                                        </bk-form-item>

                                        <bk-form-item label-width="auto" :label="$t('editPage.addMetaData')" :is-error="errors.has(`param-${param.id}.buildType`)" :error-msg="errors.first(`param-${param.id}.buildType`)">
                                            <selector :popover-min-width="510" :disabled="disabled" :list="getBuildTypeList(param.containerType.os)" setting-key="type" :handle-change="(name, value) => handleBuildResourceChange(name, value, index, param)" name="buildType" :data-vv-scope="`param-${param.id}`" placeholder="" :value="param.containerType.buildType"></selector>
                                        </bk-form-item>
                                    </template>

                                    <bk-form-item label-width="auto" v-if="isFileParam(param.type)">
                                        <file-param-input
                                            :file-path="param.defaultValue"
                                        ></file-param-input>
                                    </bk-form-item>

                                    <bk-form-item label-width="auto" :label="$t('desc')">
                                        <vuex-input :disabled="disabled" :handle-change="(name, value) => handleUpdateParam(name, value, index)" name="desc" :placeholder="$t('editPage.descTips')" :value="param.desc" />
                                    </bk-form-item>
                                </bk-form>
                            </accordion>
                        </draggable>
                        <a class="text-link" v-if="!disabled" @click.stop.prevent="editParam(globalParams.length, true)">
                            <i class="devops-icon icon-plus-circle" />
                            <span>{{ $t('editPage.addParams') }}</span>
                        </a>
                    </template>
                </template>
            </accordion>
        </template>
    </div>
</template>

<script>
    import { mapGetters } from 'vuex'
    import { deepCopy } from '@/utils/util'
    import Accordion from '@/components/atomFormField/Accordion'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import RequestSelector from '@/components/atomFormField/RequestSelector'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import Selector from '@/components/atomFormField/Selector'
    import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'
    import FileParamInput from '@/components/FileParamInput'
    import validMixins from '../validMixins'
    import draggable from 'vuedraggable'
    import { allVersionKeyList } from '@/utils/pipelineConst'
    import { STORE_API_URL_PREFIX, REPOSITORY_API_URL_PREFIX, PROCESS_API_URL_PREFIX } from '@/store/constants'
    import {
        isTextareaParam,
        isStringParam,
        isBooleanParam,
        isBuildResourceParam,
        isEnumParam,
        isMultipleParam,
        isCodelibParam,
        isSvnParam,
        isGitParam,
        isSubPipelineParam,
        isFileParam,
        getRepoOption,
        getParamsDefaultValueLabel,
        getParamsDefaultValueLabelTips,
        DEFAULT_PARAM,
        PARAM_LIST,
        STRING,
        CODE_LIB_OPTION,
        CODE_LIB_TYPE,
        SUB_PIPELINE_OPTION
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
        name: 'build-params',
        components: {
            Accordion,
            VuexInput,
            EnumInput,
            AtomCheckbox,
            Selector,
            draggable,
            VuexTextarea,
            RequestSelector,
            FileParamInput
        },
        mixins: [validMixins],
        props: {
            settingKey: {
                type: String,
                default: 'params'
            },
            params: {
                type: Array,
                default: () => []
            },
            // 用于校验模板常量和流水线变量不能重名
            additionParams: {
                type: Array,
                default: () => []
            },
            disabled: {
                type: Boolean,
                default: false
            },
            updateContainerParams: {
                type: Function,
                required: true
            },
            title: {
                type: String,
                default: '--'
            }
        },
        data () {
            return {
                paramIdCount: 0,
                renderParams: []
            }
        },

        computed: {
            ...mapGetters('atom', [
                'osList',
                'getBuildResourceTypeList'
            ]),
            isTemplateParams () {
                return this.settingKey === 'templateParams'
            },
            baseOSList () {
                return this.osList.filter(os => os.value !== 'NONE').map(os => ({
                    id: os.value,
                    name: os.label
                }))
            },
            validateParams () {
                return this.params.concat(this.additionParams)
            },
            paramsList () {
                return PARAM_LIST.map(item => {
                    return {
                        id: item.id,
                        name: this.$t(`storeMap.${item.name}`)
                    }
                })
            },
            boolList () {
                return BOOLEAN
            },
            codelibOption () {
                return CODE_LIB_OPTION
            },
            codeTypeList () {
                return CODE_LIB_TYPE
            },
            subPipelineOption () {
                return SUB_PIPELINE_OPTION
            },
            globalParams: {
                get () {
                    return this.renderParams.filter(p => !allVersionKeyList.includes(p.id) && p.id !== 'BK_CI_BUILD_MSG')
                },
                set (params) {
                    this.updateContainerParams(this.settingKey, [...params, ...this.versions])
                }
            },
            versions () {
                return this.params.filter(p => allVersionKeyList.includes(p.id))
            },
            hasGlobalParams () {
                return this.globalParams.length !== 0
            },
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
            params (newVal) {
                this.renderParams = this.getParams(newVal)
            }
        },

        mounted () {
            this.renderParams = this.getParams(this.params)
        },

        methods: {
            isTextareaParam,
            isStringParam,
            isBooleanParam,
            isMultipleParam,
            isSvnParam,
            isGitParam,
            isCodelibParam,
            isBuildResourceParam,
            isSubPipelineParam,
            isFileParam,
            getParamsDefaultValueLabel,
            getParamsDefaultValueLabelTips,
            isSelectorParam (type) {
                return isMultipleParam(type) || isEnumParam(type)
            },
            getRepoOption (type) {
                return getRepoOption(type)
            },
            getBuildTypeList (os) {
                return this.getBuildResourceTypeList(os)
            },
            getSelectorDefaultVal ({ type, defaultValue = '' }) {
                if (isMultipleParam(type)) {
                    return defaultValue && typeof defaultValue === 'string' ? defaultValue.split(',') : []
                }

                return defaultValue
            },
            handleParamTypeChange (key, value, paramIndex) {
                const newGlobalParams = [
                    ...this.globalParams.slice(0, paramIndex),
                    {
                        ...deepCopy(DEFAULT_PARAM[value]),
                        id: this.globalParams[paramIndex].id,
                        paramIdKey: this.globalParams[paramIndex].paramIdKey,
                        required: !this.isTemplateParams
                    },
                    ...this.globalParams.slice(paramIndex + 1)
                ]

                this.handleChange([
                    ...newGlobalParams,
                    ...this.versions
                ])
            },

            handleUpdateParamId (key, value, paramIndex) {
                try {
                    const param = this.globalParams[paramIndex]
                    const preValue = param[key]

                    if (preValue !== value) {
                        Object.assign(param, {
                            [key]: value
                        })

                        this.handleChange([
                            ...this.renderParams
                        ])
                        this.$nextTick(() => {
                            if (this.$refs[`paramId${paramIndex}Input`] && this.$refs[`paramId${paramIndex}Input`][0]) {
                                this.$refs[`paramId${paramIndex}Input`][0].$el.focus()
                            }
                            setTimeout(() => { // hack remove error
                                this.errors.clear(`param-${preValue}`)
                            }, 0)
                        })
                    }
                } catch (e) {
                    console.log('update error', e)
                }
            },

            handleUpdateParam (key, value, paramIndex) {
                try {
                    const param = this.globalParams[paramIndex]
                    if (isMultipleParam(param.type) && key === 'defaultValue') {
                        Object.assign(param, {
                            [key]: value.join(',')
                        })
                    } else if (param) {
                        Object.assign(param, {
                            [key]: value
                        })
                    }
                    this.handleChange([
                        ...this.renderParams
                    ])
                } catch (e) {
                    console.log('update error', e)
                }
            },

            editParam (index, isAdd) {
                const { globalParams, versions } = this
                if (isAdd) {
                    const param = {
                        ...deepCopy(DEFAULT_PARAM[STRING]),
                        id: `param${Math.floor(Math.random() * 100)}`,
                        paramIdKey: `paramIdKey-${this.paramIdCount++}`,
                        required: !this.isTemplateParams
                    }
                    globalParams.splice(index + 1, 0, param)
                } else {
                    globalParams.splice(index, 1)
                }
                this.handleChange([
                    ...globalParams,
                    ...versions
                ])
            },

            editParamShow (paramIndex) {
                let isShow = false
                const param = this.globalParams[paramIndex]
                if (param) {
                    isShow = param.required
                }
                this.handleUpdateParam('required', !isShow, paramIndex)
            },

            editOption (name, value, index) {
                try {
                    let opts = []
                    if (value && typeof value === 'string') {
                        opts = value.split('\n').map(opt => {
                            const v = opt.trim()
                            const res = v.match(/^([\w\.\-\\\/]+)=([\S\s]+)$/) || [v, v, v]
                            const [, key, value] = res
                            console.log(key, value)
                            return {
                                key,
                                value
                            }
                        })
                    }

                    this.handleUpdateParam(name, opts, index)
                    const param = this.renderParams[index]
                    if (typeof param.defaultValue === 'string' && (isMultipleParam(param.type) || isEnumParam(param.type))) { // 选项清除时，修改对应的默认值
                        const dv = param.defaultValue.split(',').filter(v => param.options.map(k => k.key).includes(v))
                        if (isMultipleParam(param.type)) {
                            this.handleUpdateParam('defaultValue', dv, index)
                        } else {
                            this.handleUpdateParam('defaultValue', dv.join(','), index)
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

            handleBuildResourceChange (name, value, index, param) {
                const resetBuildType = name === 'os' ? { buildType: this.getBuildTypeList(value)[0].type } : {}

                this.handleUpdateParam('containerType', Object.assign({
                    ...param.containerType,
                    [name]: value
                }, resetBuildType), index)
                this.handleUpdateParam('defaultValue', '', index)
            },

            handleProperties (key, value, index) {
                const properties = {}
                value.forEach(val => {
                    properties[val.key] = val.value
                })
                this.handleUpdateParam(key, properties, index)
            },

            getBuildResourceUrl ({ os, buildType }) {
                return `/${STORE_API_URL_PREFIX}/user/pipeline/container/projects/${this.$route.params.projectId}/oss/${os}?buildType=${buildType}`
            },

            handleCodeTypeChange (name, value, index) {
                this.handleUpdateParam(name, value, index)
                this.handleUpdateParam('defaultValue', '', index)
            },

            getCodeUrl (type) {
                type = type || 'CODE_GIT'
                return `/${REPOSITORY_API_URL_PREFIX}/user/repositories/{projectId}/hasPermissionList?permission=USE&repositoryType=${type}&page=1&pageSize=1000`
            },

            getSearchUrl () {
                return `/${PROCESS_API_URL_PREFIX}/user/buildParam/repository/${this.$route.params.projectId}/hashId?repositoryType=CODE_GIT,CODE_GITLAB,GITHUB,CODE_TGIT&permission=LIST&aliasName={keyword}&page=1&pageSize=200`
            },

            handleChange (params) {
                this.updateContainerParams(this.settingKey, params)
            },

            getOptions (param) {
                try {
                    return param.options.map(opt => opt.key === opt.value ? opt.key : `${opt.key}=${opt.value}`).join('\n')
                } catch (e) {
                    return ''
                }
            },

            getProperties (param) {
                try {
                    return Object.keys(param.properties).map(item => {
                        return {
                            key: item,
                            value: param.properties[item]
                        }
                    })
                } catch (e) {
                    return []
                }
            },
            // 全局参数添加遍历的key值
            getParams (params) {
                const result = params.map(item => {
                    const temp = { ...item }
                    if (!allVersionKeyList.includes(item.id)) {
                        temp.paramIdKey = typeof item.paramIdKey !== 'undefined' ? item.paramIdKey : `paramIdKey-${this.paramIdCount++}`
                    }
                    return temp
                })
                return result
            },

            transformOpt (opts) {
                const uniqueMap = {}
                opts = opts.filter(opt => opt.key.length)
                return Array.isArray(opts)
                    ? opts.filter(opt => {
                        if (!uniqueMap[opt.key]) {
                            uniqueMap[opt.key] = 1
                            return true
                        }
                        return false
                    }).map(opt => ({ id: opt.key, name: opt.value }))
                    : []
            }
        }
    }
</script>

<style lang="scss">
    @import '../../scss/conf';
    .build-params-comp {
        margin: 20px 0;
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
        }
        .content .text-link {
            font-size: 14px;
            cursor: pointer;
        }
    }
    .no-prop {
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .param-option {
        display: flex;
        align-items: flex-start;
        margin: 0 0 10px 0;
        .option-input {
            flex: 1;
            &:first-child {
                margin-right: 10px;
            }
        }
        .devops-icon {
            font-size: 14px;
            padding: 10px  0 0 10px;
            cursor: pointer;
            &:disabled {
                cursor: auto;
                opacity: .5;
            }
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

    .params-help {
        color: $fontColor;
        font-size: 12px;
    }
    .sortable-ghost-atom {
        opacity: 0.5;
    }
    .sortable-chosen-atom {
        transform: scale(1.0);
    }

    .param-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 10px;
        > span {
            margin: 0 10px;
        }
    }
</style>
