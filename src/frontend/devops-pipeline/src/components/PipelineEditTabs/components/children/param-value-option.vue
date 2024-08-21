<template>
    <section class="bk-form-item">
        <select-type-param
            v-if="isSelectorParam(param.type)"
            :param="param"
            :handle-update-options="handleUpdateOptions"
            :handle-update-payload="handleUpdatePayload"
            :reset-default-val="handleResetDefaultVal"
        />

        <form-field
            :hide-colon="true"
            :label="valueRequired ? $t('newui.pipelineParam.constValue') : $t(`editPage.${getParamsDefaultValueLabel(param.type)}`)"
            :required="valueRequired"
            :is-error="errors.has(`pipelineParam.defaultValue`)"
            :error-msg="errors.first(`pipelineParam.defaultValue`)"
            :desc="valueRequired ? undefined : $t(`editPage.${getParamsDefaultValueLabelTips(param.type)}`)"
        >
            <template v-if="isSelectorParam(param.type)">
                <request-selector
                    v-if="param.payload && param.payload.type === 'remote'"
                    v-bind="remoteParamOption"
                    v-validate.initial="{ required: valueRequired }"
                    :popover-min-width="250"
                    :disabled="disabled"
                    name="defaultValue"
                    :multi-select="isMultipleParam(param.type)"
                    :data-vv-scope="'pipelineParam'"
                    :value="selectDefautVal"
                    :handle-change="(name, value) => handleUpdateSelectorVal(name, value)"
                >
                </request-selector>
                <selector
                    v-else
                    :popover-min-width="250"
                    :handle-change="(name, value) => handleUpdateSelectorVal(name, value)"
                    :list="optionList"
                    :multi-select="isMultipleParam(param.type)"
                    name="defaultValue"
                    v-validate="{ required: valueRequired }"
                    :data-vv-scope="'pipelineParam'"
                    :placeholder="$t('editPage.defaultValueTips')"
                    :disabled="disabled"
                    show-select-all
                    :key="param.type"
                    :value="selectDefautVal"
                >
                </selector>
            </template>
            <enum-input
                v-if="isBooleanParam(param.type)"
                name="defaultValue"
                :list="boolList"
                :disabled="disabled"
                :handle-change="handleChange"
                :value="param.defaultValue"
            >
            </enum-input>
            <vuex-input
                v-if="isStringParam(param.type) || isSvnParam(param.type) || isGitParam(param.type) || isArtifactoryParam(param.type)"
                :disabled="disabled"
                :handle-change="handleChange"
                name="defaultValue"
                v-validate="{ required: valueRequired }"
                :data-vv-scope="'pipelineParam'"
                :click-unfold="true"
                :placeholder="$t('editPage.defaultValueTips')"
                :value="param.defaultValue"
            />
            <file-param-input
                v-if="isFileParam(param.type)"
                name="defaultValue"
                :required="valueRequired"
                :disabled="disabled"
                :value="param.defaultValue"
                :handle-change="handleChange"
            />
            <vuex-textarea
                v-if="isTextareaParam(param.type)"
                :disabled="disabled"
                :handle-change="handleChange"
                name="defaultValue"
                v-validate="{ required: valueRequired }"
                :data-vv-scope="'pipelineParam'"
                :placeholder="$t('editPage.defaultValueTips')"
                :value="param.defaultValue"
            />
            <request-selector
                v-if="isCodelibParam(param.type)"
                :popover-min-width="250"
                :url="getCodeUrl(param.scmType)"
                v-bind="codelibOption"
                :disabled="disabled"
                name="defaultValue"
                v-validate="{ required: valueRequired }"
                :data-vv-scope="'pipelineParam'"
                :value="param.defaultValue"
                :handle-change="handleChange"
            >
            </request-selector>
            <request-selector
                v-if="isBuildResourceParam(param.type)"
                :popover-min-width="250"
                :url="getBuildResourceUrl(param.containerType)"
                param-id="name"
                :disabled="disabled"
                name="defaultValue"
                v-validate="{ required: valueRequired }"
                :data-vv-scope="'pipelineParam'"
                :value="param.defaultValue"
                :handle-change="handleChange"
                :replace-key="param.replaceKey"
                :search-url="param.searchUrl"
            >
            </request-selector>
            <request-selector
                v-if="isSubPipelineParam(param.type)"
                :popover-min-width="250"
                v-bind="subPipelineOption"
                :disabled="disabled"
                name="defaultValue"
                v-validate="{ required: valueRequired }"
                :data-vv-scope="'pipelineParam'"
                :value="param.defaultValue"
                :handle-change="handleChange"
                :replace-key="param.replaceKey"
                :search-url="param.searchUrl"
            >
            </request-selector>
        </form-field>

        <form-field
            :hide-colon="true"
            v-if="isSvnParam(param.type)"
            :label="$t('editPage.svnParams')"
            :is-error="errors.has(`repoHashId`)"
            :error-msg="errors.first(`pipelineParam.repoHashId`)"
        >
            <request-selector
                v-bind="getRepoOption('CODE_SVN')"
                :disabled="disabled"
                name="repoHashId"
                :value="param.repoHashId"
                :handle-change="handleChange"
                v-validate="'required'"
                :data-vv-scope="'pipelineParam'"
                :replace-key="param.replaceKey"
                :search-url="param.searchUrl"
            >
            </request-selector>
        </form-field>

        <form-field
            :hide-colon="true"
            v-if="isSvnParam(param.type)"
            :label="$t('editPage.relativePath')"
            :is-error="errors.has(`relativePath`)"
            :error-msg="errors.first(`pipelineParam.relativePath`)"
        >
            <vuex-input
                :disabled="disabled"
                :handle-change="handleChange"
                name="relativePath"
                :placeholder="$t('editPage.relativePathTips')"
                :value="param.relativePath"
            ></vuex-input>
        </form-field>

        <form-field
            :hide-colon="true"
            v-if="isGitParam(param.type)"
            :label="$t('editPage.gitRepo')"
            :is-error="errors.has(`repoHashId`)"
            :error-msg="errors.first(`pipelineParam.repoHashId`)"
        >
            <request-selector
                v-bind="getRepoOption('CODE_GIT,CODE_GITLAB,GITHUB,CODE_TGIT')"
                :disabled="disabled"
                name="repoHashId"
                :value="param.repoHashId"
                :handle-change="handleChange"
                v-validate="'required'"
                :data-vv-scope="'pipelineParam'"
                replace-key="{keyword}"
                :search-url="getSearchUrl()"
            >
            </request-selector>
        </form-field>

        <form-field
            :hide-colon="true"
            v-if="isCodelibParam(param.type)"
            :label="$t('editPage.codelibParams')"
            :is-error="errors.has(`scmType`)"
            :error-msg="errors.first(`pipelineParam.scmType`)"
        >
            <selector
                :disabled="disabled"
                :list="codeTypeList"
                :handle-change="(name, value) => handleCodeTypeChange(name, value)"
                name="scmType"
                placeholder=""
                :value="param.scmType"
            ></selector>
        </form-field>

        <template v-if="isBuildResourceParam(param.type)">
            <form-field
                :hide-colon="true"
                :label="$t('editPage.buildEnv')"
                :is-error="errors.has(`os`)"
                :error-msg="errors.first(`pipelineParam.os`)"
            >
                <selector
                    :popover-min-width="510"
                    :disabled="disabled"
                    :list="baseOSList"
                    :handle-change="(name, value) => handleBuildResourceChange(name, value, param)"
                    name="os"
                    placeholder=""
                    :value="param.containerType.os"
                ></selector>
            </form-field>

            <form-field
                :hide-colon="true"
                :label="$t('editPage.addMetaData')"
                :is-error="errors.has(`buildType`)"
                :error-msg="errors.first(`pipelineParam.buildType`)"
            >
                <selector
                    :popover-min-width="510"
                    :disabled="disabled"
                    :list="getBuildTypeList(param.containerType.os)"
                    setting-key="type"
                    :handle-change="(name, value) => handleBuildResourceChange(name, value, param)"
                    name="buildType"
                    placeholder=""
                    :value="param.containerType.buildType"
                ></selector>
            </form-field>
        </template>
    </section>
</template>

<script>
    import { mapGetters } from 'vuex'
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
        isArtifactoryParam,
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
            // 默认值是否必填，常量时必填
            valueRequired: {
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
        data () {
            return {
                optionList: [],
                selectDefautVal: '',
                remoteParamOption: {}
            }
        },
        computed: {
            ...mapGetters('atom', [
                'osList',
                'getBuildResourceTypeList'
            ]),
            baseOSList () {
                return this.osList.filter(os => os.value !== 'NONE').map(os => ({
                    id: os.value,
                    name: os.label
                }))
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
            isRemoteSelect () {
                return this.param?.payload?.type === 'remote'
            }
        },
        created () {
            this.setRemoteParamOption(this.param.payload)
            if (this.isSelectorParam(this.param.type)) {
                if (!this.isRemoteSelect) {
                    this.transformOpt(this.param.options || [])
                    this.setSelectorDefaultVal(this.param)
                } else {
                    this.selectDefautVal = isMultipleParam(this.param.type) ? this.param?.defaultValue?.split(',') : this.param?.defaultValue
                }
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
            isArtifactoryParam,
            isSubPipelineParam,
            isFileParam,
            getParamsDefaultValueLabel,
            getParamsDefaultValueLabelTips,
            handleUpdateParam (name, value) {
                this.handleChange(name, value)
            },
            isSelectorParam (type) {
                return isMultipleParam(type) || isEnumParam(type)
            },
            setRemoteParamOption (payload) {
                payload = payload || {}
                const remoteOpion = {
                    url: payload.url || '',
                    dataPath: payload.dataPath || 'data',
                    paramId: payload.paramId || 'id',
                    paramName: payload.paramName || 'name'
                }
                this.remoteParamOption = remoteOpion
            },
            getRepoOption (type) {
                return getRepoOption(type)
            },
            getBuildTypeList (os) {
                return this.getBuildResourceTypeList(os)
            },
            handleBuildResourceChange (name, value, param) {
                const resetBuildType = name === 'os' ? { buildType: this.getBuildTypeList(value)[0].type } : {}

                this.handleUpdateParam('containerType', Object.assign({
                    ...param.containerType,
                    [name]: value
                }, resetBuildType))
                this.handleUpdateParam('defaultValue', '')
            },
            setSelectorDefaultVal ({ type, defaultValue = '' }) {
                if (typeof this.param.defaultValue === 'string' && (isMultipleParam(this.param.type) || isEnumParam(this.param.type))) { // 选项清除时，修改对应的默认值
                    const dv = this.param.defaultValue.split(',').filter(v => this.param.options.map(k => k.key).includes(v))
                    if (isMultipleParam(this.param.type)) {
                        this.selectDefautVal = dv
                    } else {
                        this.selectDefautVal = dv.join(',')
                    }
                    this.handleUpdateParam('defaultValue', dv.join(','))
                }
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
                this.optionList = final
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
            handleUpdatePayload (key, val) {
                this.handleChange(key, val)
                if (val.type === 'remote') {
                    this.setRemoteParamOption(val)
                }
            },
            handleResetDefaultVal () {
                if (isMultipleParam(this.param.type)) {
                    this.selectDefautVal = []
                } else {
                    this.selectDefautVal = ''
                }
                this.handleUpdateParam('defaultValue', '')
            },
            handleUpdateOptions (key, val) {
                this.handleChange(key, val)
                this.transformOpt(val)

                // 选项变更后，重制默认值
                const { param } = this
                if (typeof param.defaultValue === 'string' && (isMultipleParam(param.type) || isEnumParam(param.type))) { // 选项清除时，修改对应的默认值
                    const dv = param.defaultValue.split(',').filter(v => param.options.map(k => k.key).includes(v))
                    this.handleUpdateParam('defaultValue', dv.join(','))
                }
                this.setSelectorDefaultVal(this.param)
            },
            handleUpdateSelectorVal (key, value) {
                this.selectDefautVal = value
                if (isMultipleParam(this.param.type)) {
                    value = value.join(',')
                }
                this.handleUpdateParam(key, value)
            }
        }
    }
</script>
