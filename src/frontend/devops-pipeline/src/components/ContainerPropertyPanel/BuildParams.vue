<template>
    <div class="build-params-comp">
        <template>
            <accordion
                show-checkbox
                :show-content="hasGlobalParams"
            >
                <template slot="header">
                    <span>
                        {{ title }}
                        <bk-popover placement="right">
                            <i
                                style="display:block;"
                                class="bk-icon icon-info-circle"
                            ></i>
                            <div
                                slot="content"
                                style="white-space: pre-wrap;"
                            >
                                <div> {{ $t('editPage.paramsTips') }} </div>
                            </div>
                        </bk-popover>
                    </span>
                </template>
                <template slot="content">
                    <div
                        class="no-prop"
                        v-if="!hasGlobalParams"
                    >
                        <bk-button
                            theme="primary"
                            :disabled="disabled"
                            @click="editParam(null, true)"
                        >
                            {{ $t('editPage.addParams') }}
                        </bk-button>
                    </div>
                    <template v-else>
                        <draggable
                            v-model="globalParams"
                            :options="paramsDragOptions"
                        >
                            <accordion
                                v-for="(param, index) in globalParams"
                                :key="param.paramIdKey"
                                :is-error="errors.any(`param-${param.id}`)
                                    || errors.any(`param-${param.id}-repo-name`)
                                    || errors.any(`param-${param.id}-branch`)"
                            >
                                <header
                                    class="param-header"
                                    slot="header"
                                >
                                    <span>
                                        <bk-popover
                                            style="vertical-align: middle"
                                            v-if="(errors.all(`param-${param.id}`).length || errors.all(`param-${param.id}-repo-name`).length || errors.all(`param-${param.id}-branch`).length)"
                                            placement="top"
                                        >
                                            <i class="bk-icon icon-info-circle-shape"></i>
                                            <div slot="content">
                                                <p
                                                    v-for="error in (errors.all(`param-${param.id}`))"
                                                    :key="error"
                                                >
                                                    {{ error }}</p>
                                                <p
                                                    v-for="error in (errors.all(`param-${param.id}-repo-name`))"
                                                    :key="error"
                                                >
                                                    {{ error }}</p>
                                                    
                                                <template v-if="!errors.all(`param-${param.id}-repo-name`).length">
                                                    <p
                                                        v-for="error in (errors.all(`param-${param.id}-branch`))"
                                                        :key="error"
                                                    >
                                                        {{ error }}</p>
                                                </template>
                                            </div>
                                        </bk-popover>
                                        {{ param.id }}
                                    </span>
                                    <i
                                        v-if="!disabled && !isTemplateParams"
                                        @click.stop.prevent="editParamShow(index)"
                                        class="devops-icon"
                                        :class="[`${param.required ? 'icon-eye' : 'icon-eye-slash'}`]"
                                    />
                                    <i
                                        v-if="!disabled"
                                        class="devops-icon icon-move"
                                    />
                                    <i
                                        v-if="!disabled"
                                        @click.stop.prevent="editParam(index, false)"
                                        class="devops-icon icon-minus"
                                    />
                                </header>
                                <bk-form slot="content">
                                    <div class="params-flex-col">
                                        <bk-form-item
                                            label-width="auto"
                                            :label="$t('editPage.paramsType')"
                                            class="flex-col-span-1"
                                        >
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
                                        <bk-form-item
                                            label-width="auto"
                                            class="flex-col-span-1"
                                            v-if="!isTemplateParams"
                                        >
                                            <atom-checkbox
                                                :disabled="disabled"
                                                :text="$t('editPage.showOnStarting')"
                                                :value="param.required"
                                                name="required"
                                                :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                                            />
                                        </bk-form-item>
                                    </div>
                                    <div
                                        class="params-flex-col pt10"
                                        :style="{ 'flex-direction': !isRepoParam(param.type) && !isFileParam(param.type) ? '' : 'column' }"
                                    >
                                        <bk-form-item
                                            label-width="auto"
                                            class="flex-col-span-1"
                                            :label="$t('name')"
                                            :is-error="errors.has(`param-${param.id}.id`)"
                                            :error-msg="errors.first(`param-${param.id}.id`)"
                                        >
                                            <vuex-input
                                                :ref="`paramId${index}Input`"
                                                :data-vv-scope="`param-${param.id}`"
                                                :disabled="disabled"
                                                :handle-change="(name, value) => handleUpdateParamId(name, value, index)"
                                                v-validate.initial="`required|paramsIdRule|unique:${validateParams.map(p => p.id).join(',')}`"
                                                name="id"
                                                :placeholder="$t('nameInputTips')"
                                                :value="param.id"
                                            />
                                        </bk-form-item>
                                        <template v-if="!isRepoParam(param.type)">
                                            <bk-form-item
                                                v-if="!isFileParam(param.type)"
                                                label-width="auto"
                                                class="flex-col-span-1"
                                                :label="$t(`editPage.${getParamsDefaultValueLabel(param.type)}`)"
                                                :required="isBooleanParam(param.type)"
                                                :is-error="errors.has(`param-${param.id}.defaultValue`)"
                                                :error-msg="errors.first(`param-${param.id}.defaultValue`)"
                                                :desc="$t(`editPage.${getParamsDefaultValueLabelTips(param.type)}`)"
                                            >
                                                <template
                                                    v-if="isSelectorParam(param.type)"
                                                >
                                                    <request-selector
                                                        v-if="param.payload && param.payload.type === 'remote'"
                                                        v-bind="getRemoteParamOption(param.payload)"
                                                        v-validate.initial="{ required: valueRequired }"
                                                        :popover-min-width="450"
                                                        :disabled="disabled"
                                                        name="defaultValue"
                                                        :multi-select="isMultipleParam(param.type)"
                                                        :data-vv-scope="'pipelineParam'"
                                                        :value="param.defaultValue"
                                                        :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                                                        :key="param.type"
                                                    >
                                                    </request-selector>
                                                    <selector
                                                        v-else
                                                        style="max-width: 250px"
                                                        :popover-min-width="250"
                                                        :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                                                        :list="transformOpt(param.options)"
                                                        :multi-select="isMultipleParam(param.type)"
                                                        name="defaultValue"
                                                        :data-vv-scope="`param-${param.id}`"
                                                        :disabled="disabled"
                                                        show-select-all
                                                        :key="param.type"
                                                        :value="param.defaultValue"
                                                    >
                                                    </selector>
                                                </template>
                                                <enum-input
                                                    v-if="isBooleanParam(param.type)"
                                                    name="defaultValue"
                                                    :list="boolList"
                                                    :disabled="disabled"
                                                    :data-vv-scope="`param-${param.id}`"
                                                    :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                                                    :value="param.defaultValue"
                                                >
                                                </enum-input>
                                                <vuex-input
                                                    v-if="isStringParam(param.type) || isSvnParam(param.type) || isGitParam(param.type)"
                                                    :disabled="disabled"
                                                    :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                                                    name="defaultValue"
                                                    :click-unfold="true"
                                                    :data-vv-scope="`param-${param.id}`"
                                                    :placeholder="$t('editPage.defaultValueTips')"
                                                    :value="param.defaultValue"
                                                />
                                                <vuex-textarea
                                                    v-if="isTextareaParam(param.type)"
                                                    :click-unfold="true"
                                                    :hover-unfold="true"
                                                    :disabled="disabled"
                                                    :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                                                    name="defaultValue"
                                                    :data-vv-scope="`param-${param.id}`"
                                                    :placeholder="$t('editPage.defaultValueTips')"
                                                    :value="param.defaultValue"
                                                />
                                                <request-selector
                                                    v-if="isCodelibParam(param.type)"
                                                    style="max-width: 250px"
                                                    :popover-min-width="250"
                                                    :url="getCodeUrl(param.scmType)"
                                                    v-bind="codelibOption"
                                                    :disabled="disabled"
                                                    name="defaultValue"
                                                    :value="param.defaultValue"
                                                    :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                                                    :data-vv-scope="`param-${param.id}`"
                                                ></request-selector>
                                                <request-selector
                                                    v-if="isBuildResourceParam(param.type)"
                                                    style="max-width: 250px"
                                                    :popover-min-width="250"
                                                    :url="getBuildResourceUrl(param.containerType)"
                                                    param-id="name"
                                                    :disabled="disabled"
                                                    name="defaultValue"
                                                    :value="param.defaultValue"
                                                    :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                                                    :data-vv-scope="`param-${param.id}`"
                                                    :replace-key="param.replaceKey"
                                                    :search-url="param.searchUrl"
                                                ></request-selector>
                                                <request-selector
                                                    v-if="isSubPipelineParam(param.type)"
                                                    style="max-width: 250px"
                                                    :popover-min-width="250"
                                                    v-bind="subPipelineOption"
                                                    :disabled="disabled"
                                                    name="defaultValue"
                                                    :value="param.defaultValue"
                                                    :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                                                    :data-vv-scope="`param-${param.id}`"
                                                    :replace-key="param.replaceKey"
                                                    :search-url="param.searchUrl"
                                                ></request-selector>
                                            </bk-form-item>
    
                                            <bk-form-item
                                                v-else
                                                style="max-width: 100%;"
                                                label-width="auto"
                                                :label="$t(`editPage.${getParamsDefaultValueLabel(param.type)}`)"
                                                :required="isBooleanParam(param.type)"
                                                :is-error="errors.has(`param-${param.id}.defaultValue`)"
                                                :error-msg="errors.first(`param-${param.id}.defaultValue`)"
                                                :desc="$t(`editPage.${getParamsDefaultValueLabelTips(param.type)}`)"
                                            >
                                                <file-param-input
                                                    name="defaultValue"
                                                    v-bind="param"
                                                    :required="valueRequired"
                                                    :disabled="disabled"
                                                    :value="param.defaultValue"
                                                    :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                                                    :enable-version-control="param.enableVersionControl"
                                                    :random-sub-path="param.randomStringInPath"
                                                />
                                            </bk-form-item>
                                        </template>
                                    </div>

                                    <select-type-param
                                        v-if="isSelectorParam(param.type)"
                                        :key="param.type"
                                        class="mt20"
                                        :param="param"
                                        :handle-update-options="(name, value) => handleUpdateOptions(name, value, index)"
                                        :handle-update-payload="(name, value) => handleUpdateParam(name, value, index)"
                                        :reset-default-val="() => handleResetDefaultVal(param, index)"
                                    />

                                    <bk-form-item
                                        label-width="auto"
                                        v-if="isSvnParam(param.type)"
                                        :label="$t('editPage.svnParams')"
                                        :is-error="errors.has(`param-${param.id}.repoHashId`)"
                                        :error-msg="errors.first(`param-${param.id}.repoHashId`)"
                                    >
                                        <request-selector
                                            v-bind="getRepoOption('CODE_SVN')"
                                            :disabled="disabled"
                                            name="repoHashId"
                                            :value="param.repoHashId"
                                            :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                                            :data-vv-scope="`param-${param.id}`"
                                            v-validate.initial="'required'"
                                            :replace-key="param.replaceKey"
                                            :search-url="param.searchUrl"
                                        ></request-selector>
                                    </bk-form-item>

                                    <bk-form-item
                                        label-width="auto"
                                        v-if="isSvnParam(param.type)"
                                        :label="$t('editPage.relativePath')"
                                        :is-error="errors.has(`param-${param.id}.relativePath`)"
                                        :error-msg="errors.first(`param-${param.id}.relativePath`)"
                                    >
                                        <vuex-input
                                            :disabled="disabled"
                                            :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                                            name="relativePath"
                                            :data-vv-scope="`param-${param.id}`"
                                            :placeholder="$t('editPage.relativePathTips')"
                                            :value="param.relativePath"
                                        ></vuex-input>
                                    </bk-form-item>

                                    <bk-form-item
                                        label-width="auto"
                                        v-if="isGitParam(param.type)"
                                        :label="$t('editPage.gitRepo')"
                                        :is-error="errors.has(`param-${param.id}.repoHashId`)"
                                        :error-msg="errors.first(`param-${param.id}.repoHashId`)"
                                    >
                                        <request-selector
                                            v-bind="getRepoOption('CODE_GIT,CODE_GITLAB,GITHUB,CODE_TGIT')"
                                            :disabled="disabled"
                                            name="repoHashId"
                                            :value="param.repoHashId"
                                            :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                                            :data-vv-scope="`param-${param.id}`"
                                            v-validate.initial="'required'"
                                            replace-key="{keyword}"
                                            :search-url="getSearchUrl('CODE_GIT,CODE_GITLAB,GITHUB,CODE_TGIT')"
                                        ></request-selector>
                                    </bk-form-item>
                                    
                                    <template v-if="isRepoParam(param.type)">
                                        <bk-form-item
                                            label-width="auto"
                                            v-if="isRepoParam(param.type)"
                                            :label="$t('editPage.repoName')"
                                            required
                                            :is-error="!param.defaultValue['repo-name']"
                                            :error-msg="errors.first(`param-${param.id}.defaultValue`)"
                                        >
                                            <request-selector
                                                v-bind="getRepoOption('CODE_GIT,CODE_GITLAB,GITHUB,CODE_TGIT,CODE_SVN', 'aliasName')"
                                                :disabled="disabled"
                                                name="defaultValue"
                                                :value="param.defaultValue['repo-name']"
                                                :handle-change="(name, value) => handleChangeCodeRepo(name, value, index)"
                                                :data-vv-scope="`param-${param.id}-repo-name`"
                                                v-validate.initial="'required'"
                                                replace-key="{keyword}"
                                                :search-url="getSearchUrl('CODE_GIT,CODE_GITLAB,GITHUB,CODE_TGIT,CODE_SVN')"
                                            ></request-selector>
                                        </bk-form-item>
                                        <bk-form-item
                                            label-width="auto"
                                            v-if="isRepoParam(param.type)"
                                            :label="$t('editPage.branchName')"
                                            required
                                            :is-error="!param.defaultValue.branch"
                                            :error-msg="errors.first(`param-${param.id}.defaultValue`)"
                                            :key="param.defaultValue['repo-name']"
                                        >
                                            <request-selector
                                                v-bind="getBranchOption(param.defaultValue['repo-name'])"
                                                :disabled="disabled || !param.defaultValue['repo-name']"
                                                name="defaultValue"
                                                :value="param.defaultValue.branch"
                                                :handle-change="(name, value) => handleChangeBranch(name, value, index)"
                                                :data-vv-scope="`param-${param.id}-branch`"
                                                v-validate.initial="'required'"
                                                replace-key="{keyword}"
                                                :search-url="getSearchBranchUrl(param)"
                                            ></request-selector>
                                        </bk-form-item>
                                    </template>

                                    <bk-form-item
                                        label-width="auto"
                                        v-if="isCodelibParam(param.type)"
                                        :label="$t('editPage.codelibParams')"
                                        :is-error="errors.has(`param-${param.id}.scmType`)"
                                        :error-msg="errors.first(`param-${param.id}.scmType`)"
                                    >
                                        <selector
                                            :disabled="disabled"
                                            :list="codeTypeList"
                                            :handle-change="(name, value) => handleCodeTypeChange(name, value, index)"
                                            name="scmType"
                                            :data-vv-scope="`param-${param.id}`"
                                            placeholder=""
                                            :value="param.scmType"
                                        ></selector>
                                    </bk-form-item>

                                    <template v-if="isBuildResourceParam(param.type)">
                                        <bk-form-item
                                            label-width="auto"
                                            :label="$t('editPage.buildEnv')"
                                            :is-error="errors.has(`param-${param.id}.os`)"
                                            :error-msg="errors.first(`param-${param.id}.os`)"
                                        >
                                            <selector
                                                :popover-min-width="510"
                                                :disabled="disabled"
                                                :list="baseOSList"
                                                :handle-change="(name, value) => handleBuildResourceChange(name, value, index, param)"
                                                name="os"
                                                :data-vv-scope="`param-${param.id}`"
                                                placeholder=""
                                                :value="param.containerType.os"
                                            ></selector>
                                        </bk-form-item>

                                        <bk-form-item
                                            label-width="auto"
                                            :label="$t('editPage.addMetaData')"
                                            :is-error="errors.has(`param-${param.id}.buildType`)"
                                            :error-msg="errors.first(`param-${param.id}.buildType`)"
                                        >
                                            <selector
                                                :popover-min-width="510"
                                                :disabled="disabled"
                                                :list="getBuildTypeList(param.containerType.os)"
                                                setting-key="type"
                                                :handle-change="(name, value) => handleBuildResourceChange(name, value, index, param)"
                                                name="buildType"
                                                :data-vv-scope="`param-${param.id}`"
                                                placeholder=""
                                                :value="param.containerType.buildType"
                                            ></selector>
                                        </bk-form-item>
                                    </template>

                                    <bk-form-item
                                        label-width="auto"
                                        :label="$t('desc')"
                                    >
                                        <vuex-input
                                            :disabled="disabled"
                                            :handle-change="(name, value) => handleUpdateParam(name, value, index)"
                                            name="desc"
                                            :placeholder="$t('editPage.descTips')"
                                            :value="param.desc"
                                        />
                                    </bk-form-item>
                                    <Accordion
                                        show-content
                                        show-checkbox
                                    >
                                        <header slot="header">
                                            {{ $t('editPage.controlOption') }}
                                        </header>
                                        <article slot="content">
                                            <SubParameter
                                                :title="$t('editPage.displayCondition')"
                                                name="displayCondition"
                                                :param="displayConditionList"
                                                :atom-value="{
                                                    displayCondition: Object.keys(param.displayCondition ?? {}).map(key => ({
                                                        key,
                                                        value: param.displayCondition[key]
                                                    }))
                                                }"
                                                :handle-change="(name, value) => handleUpdateDisplayCondition(name, value, index)"
                                            />
                                        </article>
                                    </Accordion>
                                </bk-form>
                            </accordion>
                        </draggable>
                        <a
                            class="text-link"
                            v-if="!disabled"
                            @click.stop.prevent="editParam(globalParams.length, true)"
                        >
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
    import SubParameter from '@/components/AtomFormComponent/SubParameter'
    import Accordion from '@/components/atomFormField/Accordion'
    import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import FileParamInput from '@/components/atomFormField/FileParamInput'
    import RequestSelector from '@/components/atomFormField/RequestSelector'
    import Selector from '@/components/atomFormField/Selector'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import SelectTypeParam from '@/components/PipelineEditTabs/components/children/select-type-param'
    import { PROCESS_API_URL_PREFIX, REPOSITORY_API_URL_PREFIX, STORE_API_URL_PREFIX } from '@/store/constants'
    import {
        CODE_LIB_OPTION,
        CODE_LIB_TYPE,
        DEFAULT_PARAM,
        PARAM_LIST,
        STRING,
        SUB_PIPELINE_OPTION,
        getBranchOption,
        getParamsDefaultValueLabel,
        getParamsDefaultValueLabelTips,
        getRepoOption,
        isBooleanParam,
        isBuildResourceParam,
        isCodelibParam,
        isEnumParam,
        isFileParam,
        isGitParam,
        isMultipleParam,
        isRepoParam,
        isStringParam,
        isSubPipelineParam,
        isSvnParam,
        isTextareaParam
    } from '@/store/modules/atom/paramsConfig'
    import { allVersionKeyList } from '@/utils/pipelineConst'
    import { deepCopy } from '@/utils/util'
    import draggable from 'vuedraggable'
    import { mapGetters } from 'vuex'
    import validMixins from '../validMixins'

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
            FileParamInput,
            SelectTypeParam,
            SubParameter
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
                }).filter(item => item.id !== 'CHECKBOX')
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
            },
            displayConditionList () {
                return {
                    paramType: 'list',
                    list: this.globalParams.map(item => ({
                        ...item,
                        key: item.id
                    }))
                    
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
            isRepoParam,
            getParamsDefaultValueLabel,
            getParamsDefaultValueLabelTips,
            isSelectorParam (type) {
                return isMultipleParam(type) || isEnumParam(type)
            },
            getRepoOption (type, paramId) {
                return getRepoOption(type, paramId)
            },
            getBranchOption (repositoryId) {
                return getBranchOption(repositoryId)
            },
            getBuildTypeList (os) {
                return this.getBuildResourceTypeList(os)
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
            handleChangeCodeRepo (key, value, paramIndex) {
                const param = this.globalParams[paramIndex]
                Object.assign(param, {
                    [key]: {
                        'repo-name': value,
                        branch: ''
                    }
                })
                this.handleChange([
                    ...this.renderParams
                ])
            },
            handleChangeBranch (key, value, paramIndex) {
                const param = this.globalParams[paramIndex]
               
                Object.assign(param, {
                    [key]: {
                        ...param.defaultValue,
                        branch: value
                    }
                })
                this.handleChange([
                    ...this.renderParams
                ])
            },
            handleUpdateParam (key, value, paramIndex) {
                try {
                    const param = this.globalParams[paramIndex]
                    if (param) {
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

            getRemoteParamOption (payload) {
                payload = payload || {}
                return {
                    url: payload?.url || '',
                    dataPath: payload?.dataPath || 'data',
                    paramId: payload?.paramId || 'id',
                    paramName: payload?.paramName || 'name'
                }
            },
            handleResetDefaultVal (param, index) {
                this.handleUpdateParam('defaultValue', isMultipleParam(param.type) ? [] : '', index)
            },
            handleUpdateOptions (name, value, index) {
                try {
                    this.transformOpt(value)
                    this.handleUpdateParam(name, value, index)
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
                return `/${REPOSITORY_API_URL_PREFIX}/user/repositories/${this.$route.params.projectId}/hasPermissionList?permission=USE&repositoryType=${type}&page=1&pageSize=1000`
            },
            getSearchUrl (type) {
                return `${this.getCodeUrl(type)}&aliasName={keyword}`
            },
            
            getSearchBranchUrl (param) {
                return `/${PROCESS_API_URL_PREFIX}/user/buildParam/${this.$route.params.projectId}/repository/refs?search={keyword}&repositoryType=NAME&repositoryId=${param.defaultValue}`
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
            },
            handleUpdateDisplayCondition (key, value, paramIndex) {
                const displayCondition = JSON.parse(value).reduce((acc, cur) => {
                    acc[cur.key] = cur.value
                    return acc
                }, {})
                this.handleUpdateParam(key, displayCondition, paramIndex)
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
            max-width: 50%;
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
        padding: 10px 0 0 10px;
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

    >span {
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

    >span {
        margin: 0 10px;
    }
}
</style>
