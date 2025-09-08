<template>
    <section class="bk-form-item">
        <form-field
            :hide-colon="true"
            v-if="isCodelibParam(param.type)"
            :label="$t('editPage.codelibParams')"
            :is-error="errors.has(`pipelineParam.scmType`)"
            :error-msg="errors.first(`pipelineParam.scmType`)"
        >
            <selector
                :disabled="disabled"
                :list="codeTypeList"
                :handle-change="(name, value) => handleCodeTypeChange(name, value)"
                name="scmType"
                placeholder=""
                :value="param.scmType"
                :clearable="false"
            ></selector>
        </form-field>
        <form-field
            :hide-colon="true"
            v-if="isGitParam(param.type)"
            :label="$t('editPage.gitRepo')"
            :required="true"
            :is-error="errors.has(`pipelineParam.repoHashId`)"
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
                :search-url="getSearchUrl('CODE_GIT,CODE_GITLAB,GITHUB,CODE_TGIT')"
            >
            </request-selector>
        </form-field>
        <form-field
            :hide-colon="true"
            v-if="isRepoParam(param.type)"
            :label="$t('editPage.repoName')"
            :desc="$t('editPage.referencedTips', ['${{ variables.' + `${param.id}` + '.repo-name }}'])"
            :required="true"
            :is-error="!param.defaultValue['repo-name']"
            :error-msg="errors.first(`pipelineParam.defaultValue`)"
        >
            <request-selector
                v-bind="getRepoOption('CODE_GIT,CODE_GITLAB,GITHUB,CODE_TGIT,CODE_SVN', 'aliasName')"
                :disabled="disabled"
                name="defaultValue"
                :value="param.defaultValue['repo-name']"
                :handle-change="(name, value) => handleChangeCodeRepo(name, value)"
                v-validate="'required'"
                :data-vv-scope="'pipelineParam'"
                replace-key="{keyword}"
                :search-url="getSearchUrl('CODE_GIT,CODE_GITLAB,GITHUB,CODE_TGIT,CODE_SVN')"
            >
            </request-selector>
        </form-field>
        <form-field
            :hide-colon="true"
            v-if="isRepoParam(param.type)"
            :label="$t('editPage.branchName')"
            :desc="$t('editPage.referencedTips', ['${{ variables.' + `${param.id}` + '.branch }}'])"
            :required="true"
            :is-error="!param.defaultValue.branch"
            :error-msg="errors.first(`pipelineParam.defaultValue`)"
            :key="param.defaultValue['repo-name']"
        >
            <request-selector
                v-bind="getBranchOption(param.defaultValue['repo-name'])"
                :disabled="disabled || !param.defaultValue"
                name="defaultValue"
                :value="param.defaultValue.branch"
                :handle-change="handleChangeBranch"
                v-validate="'required'"
                :data-vv-scope="'pipelineParam'"
                replace-key="{keyword}"
                :search-url="getSearchBranchUrl()"
            >
            </request-selector>
        </form-field>
        <form-field
            :hide-colon="true"
            v-if="isSvnParam(param.type)"
            :label="$t('editPage.svnParams')"
            :required="true"
            :is-error="errors.has(`pipelineParam.repoHashId`)"
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
        <select-type-param
            v-if="isSelectorParam(param.type)"
            :param="param"
            :handle-update-options="handleUpdateOptions"
            :handle-update-payload="handleUpdatePayload"
            :reset-default-val="handleResetDefaultVal"
        />
        <form-field
            v-if="!isRepoParam(param.type)"
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
                v-if="isStringParam(param.type) || isSvnParam(param.type) || isGitParam(param.type) || isArtifactoryParam(param.type) || isRepoParam(param.type)"
                :disabled="disabled"
                :handle-change="handleChange"
                name="defaultValue"
                v-validate.initial="varLengthRule"
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
                :enable-version-control="param.enableVersionControl"
                :random-sub-path="param.randomStringInPath"
                :handle-change="handleChange"
            />
            <vuex-textarea
                v-if="isTextareaParam(param.type)"
                :disabled="disabled"
                :handle-change="handleChange"
                name="defaultValue"
                v-validate.initial="varLengthRule"
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

        <template v-if="isArtifactoryParam(param.type)">
            <form-field
                :hide-colon="true"
                :label="$t('editPage.filterRule')"
                :is-error="errors.has(`pipelineParam.glob`)"
                :error-msg="errors.first(`pipelineParam.glob`)"
            >
                <vuex-input
                    :disabled="disabled"
                    :handle-change="handleChange"
                    name="glob"
                    :data-vv-scope="'pipelineParam'"
                    :placeholder="$t('editPage.filterRuleTips')"
                    :value="param.glob"
                ></vuex-input>
            </form-field>

            <form-field
                :hide-colon="true"
                :label="$t('metaData')"
                :is-error="errors.has(`pipelineParam.properties`)"
                :error-msg="errors.first(`pipelineParam.properties`)"
            >
                <key-value-normal
                    :disabled="disabled"
                    name="properties"
                    :data-vv-scope="'pipelineParam'"
                    :is-metadata-var="true"
                    :add-btn-text="$t('editPage.addMetaData')"
                    :value="getProperties(param)"
                    :handle-change="
                        (name, value) => handleProperties(name, value)
                    "
                ></key-value-normal>
            </form-field>
        </template>
    </section>
</template>

<script>
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import FileParamInput from '@/components/atomFormField/FileParamInput'
    import KeyValueNormal from '@/components/atomFormField/KeyValueNormal'
    import RequestSelector from '@/components/atomFormField/RequestSelector'
    import Selector from '@/components/atomFormField/Selector'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import validMixins from '@/components/validMixins'
    import { PROCESS_API_URL_PREFIX, REPOSITORY_API_URL_PREFIX, STORE_API_URL_PREFIX, VAR_MAX_LENGTH } from '@/store/constants'
    import {
        CODE_LIB_OPTION,
        CODE_LIB_TYPE,
        getBranchOption,
        getParamsDefaultValueLabel,
        getParamsDefaultValueLabelTips,
        getRepoOption,
        isArtifactoryParam,
        isBooleanParam,
        isCodelibParam,
        isEnumParam,
        isFileParam,
        isGitParam,
        isMultipleParam,
        isRepoParam,
        isStringParam,
        isSubPipelineParam,
        isSvnParam,
        isTextareaParam,
        SUB_PIPELINE_OPTION
    } from '@/store/modules/atom/paramsConfig'
    import { mapGetters } from 'vuex'
    import SelectTypeParam from './select-type-param'
    
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
            FileParamInput,
            KeyValueNormal
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
                'getBuildResourceTypeList',
                'failIfVariableInvalid'
            ]),
            varLengthRule () {

                return Object.assign({
                    required: this.valueRequired
                }, this.failIfVariableInvalid ? {
                    max: VAR_MAX_LENGTH
                } : {})
            },
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
            isArtifactoryParam,
            isSubPipelineParam,
            isFileParam,
            isRepoParam,
            getParamsDefaultValueLabel,
            getParamsDefaultValueLabelTips,
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
            getRepoOption (type, paramId) {
                return getRepoOption(type, paramId)
            },
            getBranchOption (name) {
                return getBranchOption(name)
            },
            getBuildTypeList (os) {
                return this.getBuildResourceTypeList(os)
            },
            setSelectorDefaultVal ({ type, defaultValue = '' }) {
                if (typeof this.param.defaultValue === 'string' && (isMultipleParam(this.param.type) || isEnumParam(this.param.type))) { // 选项清除时，修改对应的默认值
                    const dv = this.param.defaultValue.split(',').filter(v => this.param.options.map(k => k.key).includes(v))
                    if (isMultipleParam(this.param.type)) {
                        this.selectDefautVal = dv
                    } else {
                        this.selectDefautVal = dv.join(',')
                    }
                    this.handleChange('defaultValue', dv.join(','))
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
                this.handleChange(name, value)
                this.handleChange('defaultValue', '')
            },

            getCodeUrl (type) {
                type = type || 'CODE_GIT'
                return `/${REPOSITORY_API_URL_PREFIX}/user/repositories/${this.$route.params.projectId}/hasPermissionList?permission=USE&repositoryType=${type}&page=1&pageSize=1000`
            },
            getSearchUrl (type) {
                return `${this.getCodeUrl(type)}&aliasName={keyword}`
            },
            getSearchBranchUrl () {
                return `/${PROCESS_API_URL_PREFIX}/user/buildParam/${this.$route.params.projectId}/repository/refs?search={keyword}&repositoryType=NAME&repositoryId=${this.param.defaultValue['repo-name']}`
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
                this.handleChange('defaultValue', '')
            },
            handleUpdateOptions (key, val) {
                this.handleChange(key, val)
                this.transformOpt(val)

                // 选项变更后，重制默认值
                const { param } = this
                if (typeof param.defaultValue === 'string' && (isMultipleParam(param.type) || isEnumParam(param.type))) { // 选项清除时，修改对应的默认值
                    const dv = param.defaultValue.split(',').filter(v => param.options.map(k => k.key).includes(v))
                    this.handleChange('defaultValue', dv.join(','))
                }
                this.setSelectorDefaultVal(this.param)
            },
            handleUpdateSelectorVal (key, value) {
                this.selectDefautVal = value
                if (isMultipleParam(this.param.type)) {
                    value = value.join(',')
                }
                this.handleChange(key, value)
            },
            getProperties (param) {
                try {
                    return Object.keys(param.properties).map((item) => {
                        return {
                            key: item,
                            value: param.properties[item]
                        }
                    })
                } catch (e) {
                    return []
                }
            },
            handleChangeCodeRepo (key, value) {
                this.handleChange(key, {
                    'repo-name': value,
                    branch: ''
                })
            },
            handleChangeBranch (key, value) {
                this.handleChange(key, {
                    ...this.param.defaultValue,
                    branch: value
                })
            }
        }
    }
</script>
