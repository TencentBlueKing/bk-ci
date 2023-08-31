<template>
    <section class="bk-form-item">
        <form-field :label="$t(`editPage.${getParamsDefaultValueLabel(param.type)}`)" :required="isBooleanParam(param.type)" :is-error="errors.has(`defaultValue`)" :error-msg="errors.first(`defaultValue`)" :desc="$t(`editPage.${getParamsDefaultValueLabelTips(param.type)}`)">
            <selector
                :popover-min-width="250"
                v-if="isSelectorParam(param.type)"
                :handle-change="(name, value) => handleUpdateParam(name, value)"
                :list="transformOpt(param.options)"
                :multi-select="isMultipleParam(param.type)"
                name="defaultValue"
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
                    
                :handle-change="(name, value) => handleUpdateParam(name, value)"
                :value="param.defaultValue">
            </enum-input>
            <vuex-input v-if="isStringParam(param.type) || isSvnParam(param.type) || isGitParam(param.type) || isFileParam(param.type)" :disabled="disabled" :handle-change="(name, value) => handleUpdateParam(name, value)" name="defaultValue" :click-unfold="true" :placeholder="$t('editPage.defaultValueTips')" :value="param.defaultValue" />
            <vuex-textarea v-if="isTextareaParam(param.type)" :disabled="disabled" :handle-change="(name, value) => handleUpdateParam(name, value)" name="defaultValue" :placeholder="$t('editPage.defaultValueTips')" :value="param.defaultValue" />
            <request-selector v-if="isCodelibParam(param.type)" :popover-min-width="250" :url="getCodeUrl(param.scmType)" v-bind="codelibOption" :disabled="disabled" name="defaultValue" :value="param.defaultValue" :handle-change="(name, value) => handleUpdateParam(name, value)"></request-selector>
            <request-selector v-if="isBuildResourceParam(param.type)" :popover-min-width="250" :url="getBuildResourceUrl(param.containerType)" param-id="name" :disabled="disabled" name="defaultValue" :value="param.defaultValue" :handle-change="(name, value) => handleUpdateParam(name, value)" :replace-key="param.replaceKey" :search-url="param.searchUrl"></request-selector>
            <request-selector v-if="isSubPipelineParam(param.type)" :popover-min-width="250" v-bind="subPipelineOption" :disabled="disabled" name="defaultValue" :value="param.defaultValue" :handle-change="(name, value) => handleUpdateParam(name, value)" :replace-key="param.replaceKey" :search-url="param.searchUrl"></request-selector>
        </form-field>

        <select-type-param v-if="isSelectorParam(param.type)" :param="param" :handle-update-options="handleUpdateOptions" />

        <form-field v-if="isSvnParam(param.type)" :label="$t('editPage.svnParams')" :is-error="errors.has(`repoHashId`)" :error-msg="errors.first(`repoHashId`)">
            <request-selector v-bind="getRepoOption('CODE_SVN')" :disabled="disabled" name="repoHashId" :value="param.repoHashId" :handle-change="(name, value) => handleUpdateParam(name, value)" v-validate.initial="'required'" :replace-key="param.replaceKey" :search-url="param.searchUrl"></request-selector>
        </form-field>

        <form-field v-if="isSvnParam(param.type)" :label="$t('editPage.relativePath')" :is-error="errors.has(`relativePath`)" :error-msg="errors.first(`relativePath`)">
            <vuex-input :disabled="disabled" :handle-change="(name, value) => handleUpdateParam(name, value)" name="relativePath" :placeholder="$t('editPage.relativePathTips')" :value="param.relativePath"></vuex-input>
        </form-field>

        <form-field v-if="isGitParam(param.type)" :label="$t('editPage.gitRepo')" :is-error="errors.has(`repoHashId`)" :error-msg="errors.first(`repoHashId`)">
            <request-selector v-bind="getRepoOption('CODE_GIT,CODE_GITLAB,GITHUB,CODE_TGIT')" :disabled="disabled" name="repoHashId" :value="param.repoHashId" :handle-change="(name, value) => handleUpdateParam(name, value)" v-validate.initial="'required'" replace-key="{keyword}" :search-url="getSearchUrl()"></request-selector>
        </form-field>

        <form-field v-if="isCodelibParam(param.type)" :label="$t('editPage.codelibParams')" :is-error="errors.has(`scmType`)" :error-msg="errors.first(`scmType`)">
            <selector :disabled="disabled" :list="codeTypeList" :handle-change="(name, value) => handleCodeTypeChange(name, value)" name="scmType" placeholder="" :value="param.scmType"></selector>
        </form-field>

        <template v-if="isBuildResourceParam(param.type)">
            <form-field :label="$t('editPage.buildEnv')" :is-error="errors.has(`os`)" :error-msg="errors.first(`os`)">
                <selector :popover-min-width="510" :disabled="disabled" :list="baseOSList" :handle-change="(name, value) => handleBuildResourceChange(name, value, param)" name="os" placeholder="" :value="param.containerType.os"></selector>
            </form-field>

            <form-field :label="$t('editPage.addMetaData')" :is-error="errors.has(`buildType`)" :error-msg="errors.first(`buildType`)">
                <selector :popover-min-width="510" :disabled="disabled" :list="getBuildTypeList(param.containerType.os)" setting-key="type" :handle-change="(name, value) => handleBuildResourceChange(name, value, param)" name="buildType" placeholder="" :value="param.containerType.buildType"></selector>
            </form-field>
        </template>

        <form-field v-if="isFileParam(param.type)">
            <file-param-input
                :file-path="param.defaultValue"
            ></file-param-input>
        </form-field>
    </section>
</template>

<script>
    import SelectTypeParam from './select-type-param'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import RequestSelector from '@/components/atomFormField/RequestSelector'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import Selector from '@/components/atomFormField/Selector'
    import FileParamInput from '@/components/FileParamInput'
    import validMixins from '@/components/validMixins'
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
        CODE_LIB_OPTION,
        CODE_LIB_TYPE,
        SUB_PIPELINE_OPTION
    } from '@/store/modules/atom/paramsConfig'
    import { STORE_API_URL_PREFIX, REPOSITORY_API_URL_PREFIX, PROCESS_API_URL_PREFIX } from '@/store/constants'

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
        components: {
            SelectTypeParam,
            FormField,
            VuexInput,
            EnumInput,
            Selector,
            VuexTextarea,
            RequestSelector,
            FileParamInput
        },
        mixins: [validMixins],
        props: {
            disabled: {
                type: Boolean,
                default: false
            },
            param: {
                type: Object,
                default: () => ({})
            },
            handleChange: {
                type: Function,
                default: () => {}
            }

        },
        computed: {
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
            }
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
            transformOpt (opts) {
                const uniqueMap = {}
                opts = opts.filter(opt => opt.key.length)
                const final = Array.isArray(opts)
                    ? opts.filter(opt => {
                        if (!uniqueMap[opt.key]) {
                            uniqueMap[opt.key] = 1
                            return true
                        }
                        return false
                    }).map(opt => ({ id: opt.key, name: opt.value }))
                    : []
                console.log('final options', final)
                return final
            },
            editOption (name, value, index) {
                try {
                    let opts = []
                    if (value && typeof value === 'string') {
                        opts = value.split('\n').map(opt => {
                            const v = opt.trim()
                            const res = v.match(/^([\w\.\-\\\/]+)=([\S\s]+)$/) || [v, v, v]
                            const [, key, value] = res
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

            getBuildResourceUrl ({ os, buildType }) {
                return `/${STORE_API_URL_PREFIX}/user/pipeline/container/projects/${this.$route.params.projectId}/oss/${os}?buildType=${buildType}`
            },

            handleCodeTypeChange (name, value) {
                this.handleUpdateParam(name, value)
                this.handleUpdateParam('defaultValue', '')
            },

            getCodeUrl (type) {
                type = type || 'CODE_GIT'
                return `/${REPOSITORY_API_URL_PREFIX}/user/repositories/{projectId}/hasPermissionList?permission=USE&repositoryType=${type}&page=1&pageSize=1000`
            },

            getSearchUrl () {
                return `/${PROCESS_API_URL_PREFIX}/user/buildParam/repository/${this.$route.params.projectId}/hashId?repositoryType=CODE_GIT,CODE_GITLAB,GITHUB,CODE_TGIT&permission=LIST&aliasName={keyword}&page=1&pageSize=200`
            },

            // handleChange (params) {
            //     this.updateContainerParams(this.settingKey, params)
            // },
            handleUpdateOptions (key, val) {
                this.handleChange(key, val)
                this.transformOpt(val)
                
                // 选项变更后，重制默认值
                const { param } = this
                if (typeof param.defaultValue === 'string' && (isMultipleParam(param.type) || isEnumParam(param.type))) { // 选项清除时，修改对应的默认值
                    const dv = param.defaultValue.split(',').filter(v => param.options.map(k => k.key).includes(v))
                    if (isMultipleParam(param.type)) {
                        this.handleUpdateParam('defaultValue', dv)
                    } else {
                        this.handleUpdateParam('defaultValue', dv.join(','))
                    }
                }
                this.getSelectorDefaultVal(this.param)
            },
            handleUpdateParam (key, value) {
                console.log(key, value, 'inner')
                this.handleChange(key, value)
            }
        }
    }
</script>
