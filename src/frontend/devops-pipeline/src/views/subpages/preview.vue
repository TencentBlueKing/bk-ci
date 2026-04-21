<template>
    <div
        class="pipeline-execute-preview"
        v-bkloading="{ isLoading }"
    >
        <div
            v-if="pacError.show"
            class="pac-error-container"
        >
            <bk-exception
                class="exception-wrap-item"
                :type="pacError.type"
            >
                <div class="pac-error-content">
                    <p
                        class="pac-error-title"
                        v-bk-xss-html="pacError.message"
                    />
                    <p
                        v-if="pacError.branch"
                        class="pac-error-detail"
                    >
                        {{ $t('preview.errorBranch') }}{{ pacError.branch }}
                    </p>
                    <p
                        v-if="pacError.pipelinePath"
                        class="pac-error-detail"
                    >
                        {{ $t('preview.errorPipelinePath') }}{{ pacError.pipelinePath }}
                        <a
                            v-if="pacError.href"
                            :href="pacError.href"
                            target="_blank"
                            class="pac-error-link"
                        >{{ pacError.hrefTitle }}</a>
                    </p>
                </div>
            </bk-exception>
        </div>
        <template v-else>
            <div
                v-show="!isDebugPipeline && !pacEnabled"
                class="pipeline-execute-version-select params-content-item"
            >
                <span>
                    {{ $t('history.tableMap.pipelineVersion') }}
                </span>
                <VersionSelector
                    :editable="pacEnabled"
                    :class="{
                        'exec-version-is-disabled': !pacEnabled
                    }"
                    :value="executeVersion"
                    @change="handleExecuteVersionChange"
                    :include-draft="false"
                    :show-extension="false"
                    refresh-list-on-expand
                    :unique-id="pipelineId"
                />
                <i
                    class="bk-icon icon-info-circle"
                    v-bk-tooltips="execVersionSelectorDisableTips"
                />
            </div>
            <bk-alert
                v-if="expireReleasedVersion"
                type="warning"
                class="expire-released-version-alert"
            >
                <template #title>
                    <div class="expire-released-version-alert-content">
                        <i18n
                            path="preview.expireReleasedVersionTips"
                            tag="span"
                        >
                            <span
                                place="branch"
                                class="expire-released-version-branch"
                            >{{ selectedBranch }}</span>
                            <span
                                place="expired"
                                class="expire-released-version-expired"
                            >{{ $t('preview.expired') }}</span>
                        </i18n>
                        <span class="expire-released-version-actions">
                            <version-diff-entry
                                v-if="branchVersion && pipelineInfo?.releaseVersion"
                                :text="false"
                                type="icon"
                                size="small"
                                :can-switch-version="false"
                                :version="branchVersion"
                                :latest-version="pipelineInfo?.releaseVersion"
                            />
                            <span
                                v-if="codeRepoUrl"
                                class="text-link expire-released-version-link"
                                @click="goToCodeRepo"
                            >
                                <i class="devops-icon icon-jump-link" />
                                {{ $t('preview.goToCodeRepo') }}
                            </span>
                        </span>
                    </div>
                </template>
            </bk-alert>
            <bk-alert
                v-if="isDebugPipeline"
                :title="$t('debugHint')"
            ></bk-alert>
            <div class="pipeline-execute-preview-content">
                <template v-if="!isDebugPipeline && buildList.length">
                    <section class="params-content-item">
                        <header
                            :class="['params-collapse-trigger', {
                                'params-collapse-expand': activeName.has(1)
                            }]"
                            @click="toggleCollapse(1)"
                        >
                            <bk-icon
                                type="right-shape"
                                class="icon-angle-right"
                            />
    
                            {{ $t('buildMsg') }}
                        </header>
                        <div
                            v-if="activeName.has(1)"
                            class="params-collapse-content"
                        >
                            <pipeline-params-form
                                ref="buildForm"
                                :param-values="buildValues"
                                :handle-param-change="handleBuildChange"
                                :params="buildList"
                            />
                        </div>
                    </section>
                </template>
                <section class="params-content-item">
                    <header
                        :class="['params-collapse-trigger', {
                            'params-collapse-expand': activeName.has(2)
                        }]"
                        @click="toggleCollapse(2)"
                    >
                        <bk-icon
                            type="right-shape"
                            class="icon-angle-right"
                        />
    
                        {{ $t('buildParams') }}
                        
                        <span
                            @click.stop=""
                        >
                            <param-set
                                ref="paramSetSelector"
                                :all-params="pipelineParams"
                                :use-last-params="useLastParams"
                                :is-visible-version="isVisibleVersion"
                                @change="updateParamsValues"
                            />
                        </span>
                        <i
                            class="devops-icon icon-question-circle"
                            v-bk-tooltips="$t('paramSetTips')"
                        />
                        <span
                            :class="['text-link', {
                                'disabled': !showChangedParamsAlert
                            }]"
                            @click.stop="resetDefaultParams"
                        >
                            {{ $t('resetDefault') }}
                        </span>
                        <span class="collapse-trigger-divider">|</span>
                        <span
                            class="text-link"
                            @click.stop="saveAsParamSet"
                        >
                            {{ $t('saveAsParamSet') }}
                        </span>
                    </header>
                    <div
                        v-show="activeName.has(2)"
                        class="params-collapse-content"
                    >
                        <bk-alert
                            v-if="showChangedParamsAlert"
                            class="changed-tips-alert"
                            type="warning"
                        >
                            <template #title>
                                <div>
                                    {{ $t('paramSetApplyTips', [applySetDiff.setName]) }}
                                    <ul
                                        class="param-set-diff-tips"
                                        v-if="paramSetDiffTips.length"
                                    >
                                        <li
                                            v-for="(tip, index) in paramSetDiffTips"
                                            :key="index"
                                        >
                                            {{ tip }}
                                        </li>
                                    </ul>
                                </div>
                            </template>
                        </bk-alert>
                        <pipeline-params-form
                            v-if="hasPipelineParams"
                            ref="paramsForm"
                            :param-values="paramsValues"
                            :all-pipeline-param-values="allPipelineParamValues"
                            :highlight-changed-param="showChangedParamsAlert"
                            :handle-param-change="handleParamChange"
                            :params="paramList"
                            sort-category
                        >
                            <template
                                slot="versionParams"
                                v-if="isVisibleVersion"
                            >
                                <renderSortCategoryParams :name="$t('preview.introVersion')">
                                    <template slot="content">
                                        <pipeline-versions-form
                                            class="mb20"
                                            ref="versionParamForm"
                                            :build-no="buildNo"
                                            :is-preview="true"
                                            :version-param-values="versionParamValues"
                                            :handle-version-change="handleVersionChange"
                                            :handle-build-no-change="handleBuildNoChange"
                                            :highlight-changed-param="showChangedParamsAlert"
                                            :version-param-list="versionParamList"
                                        />
                                    </template>
                                </renderSortCategoryParams>
                            </template>
                        </pipeline-params-form>
                        <bk-exception
                            v-else
                            type="empty"
                            scene="part"
                        >
                            {{ $t('noParams') }}
                        </bk-exception>
                    </div>
                </section>
                <template v-if="constantParams.length > 0">
                    <section class="params-content-item">
                        <header
                            :class="['params-collapse-trigger', {
                                'params-collapse-expand': activeName.has(3)
                            }]"
                            @click="toggleCollapse(3)"
                        >
                            <bk-icon
                                type="right-shape"
                                class="icon-angle-right"
                            />
                            {{ $t('newui.const') }}
                        </header>
                        <div
                            v-if="activeName.has(3)"
                            class="params-collapse-content"
                        >
                            <pipeline-params-form
                                ref="constParamsForm"
                                disabled
                                :param-values="constantValues"
                                :all-pipeline-param-values="allPipelineParamValues"
                                :params="constantParams"
                                sort-category
                            />
                        </div>
                    </section>
                </template>
                <template v-if="hasOtherParams">
                    <section class="params-content-item">
                        <header
                            :class="['params-collapse-trigger', {
                                'params-collapse-expand': activeName.has(4)
                            }]"
                            @click="toggleCollapse(4)"
                        >
                            <bk-icon
                                type="right-shape"
                                class="icon-angle-right"
                            />

                            {{ $t('newui.pipelineParam.otherVar') }}
                        </header>
                        <div
                            v-if="activeName.has(4)"
                            class="params-collapse-content"
                        >
                            <pipeline-params-form
                                ref="otherParamsForm"
                                disabled
                                :param-values="otherValues"
                                :all-pipeline-param-values="allPipelineParamValues"
                                :params="otherParams"
                                sort-category
                            >
                                <template
                                    slot="versionParams"
                                    v-if="!isVisibleVersion && versionParamValues.length"
                                >
                                    <pipeline-versions-form
                                        class="mb20"
                                        ref="versionParamForm"
                                        :build-no="buildNo"
                                        is-preview
                                        disabled
                                        :version-param-values="versionParamValues"
                                        :handle-version-change="handleVersionChange"
                                        :handle-build-no-change="handleBuildNoChange"
                                        :version-param-list="versionParamList"
                                    />
                                </template>
                            </pipeline-params-form>
                        </div>
                    </section>
                </template>
        
                <section class="params-content-item">
                    <header
                        :class="['params-collapse-trigger', {
                            'params-collapse-expand': activeName.has(5)
                        }]"
                        @click="toggleCollapse(5)"
                    >
                        <bk-icon
                            type="right-shape"
                            class="icon-angle-right"
                        />

                        {{ $t(canElementSkip ? 'preview.atomToExec' : 'executeStepPreview') }}
                        <template v-if="canElementSkip">
                            <span
                                v-if="!isDebugPipeline"
                                class="no-bold-font"
                            >
                                ({{ $t('preview.skipTipsPrefix') }}
                                <span
                                    @click.stop="editTrigger"
                                    class="text-link item-title-tips-link"
                                >
                                    {{ $t('preview.manualTrigger') }}
                                </span>
                                {{ $t('preview.skipTipsSuffix') }})
                            </span>
                            <span
                                @click.stop
                                class="no-bold-font"
                            >
                                <bk-checkbox
                                    @change="handleCheckTotalChange"
                                    v-model="checkTotal"
                                >
                                    {{ $t('preview.selectAll') }}/{{ $t('preview.selectNone') }}
                                </bk-checkbox>
                            </span>
                        </template>
                    </header>
                    <div
                        v-if="activeName.has(5)"
                        class="params-collapse-content pipeline-optional-model"
                    >
                        <pipeline
                            is-preview
                            :show-header="false"
                            :pipeline="pipelineModel"
                            :editable="false"
                            :can-skip-element="canElementSkip"
                        />
                    </div>
                </section>
            </div>
        </template>
    </div>
</template>

<script>
    import ParamSet from '@/components/ParamSet.vue'
    import Pipeline from '@/components/Pipeline'
    import VersionSelector from '@/components/PipelineDetailTabs/VersionSelector.vue'
    import PipelineVersionsForm from '@/components/PipelineVersionsForm.vue'
    import VersionDiffEntry from '@/components/PipelineDetailTabs/VersionDiffEntry.vue'
    import PipelineParamsForm from '@/components/pipelineParamsForm.vue'
    import renderSortCategoryParams from '@/components/renderSortCategoryParams'
    import { UPDATE_PREVIEW_PIPELINE_NAME, PAC_BRANCH_CHANGE, UPDATE_PAC_ERROR_STATUS, PAC_BRANCH_LOADING, PAC_BRANCH_INIT_DONE, bus } from '@/utils/bus'
    import { allVersionKeyList } from '@/utils/pipelineConst'
    import { getParamsValuesMap, isObject, isShallowEqual } from '@/utils/util'
    import { mapActions, mapGetters, mapState } from 'vuex'

    export default {
        components: {
            VersionSelector,
            PipelineVersionsForm,
            PipelineParamsForm,
            Pipeline,
            renderSortCategoryParams,
            ParamSet,
            VersionDiffEntry
        },
        data () {
            return {
                isVisibleVersion: false,
                isLoading: false,
                startupInfo: null,
                pipelineModel: null,
                activeName: new Set([1, 2, 3, 4, 5, 6]),
                paramList: [],
                versionParamList: [],
                paramsValues: {},
                versionParamValues: {},
                buildNo: {},
                buildValues: {},
                buildList: [],
                constantParams: [],
                constantValues: {},
                otherParams: [],
                otherValues: {},
                showChangedParamsAlert: false,
                checkTotal: true,
                isApplySet: false,
                pendingParamSetChange: null,
                applySetDiff: {
                    setName: '',
                    diffMap: {
                        changed: [],
                        deleted: [],
                        noRequired: []
                    }
                },
                selectedBranch: '', // PAC 分支选择
                branchVersion: null, // PAC 分支版本号，用于启动构建时指定 version
                expireReleasedVersion: false, // 是否过期的已发布版本
                // PAC 分支版本错误状态
                pacError: {
                    show: false,
                    type: '', // '404' | 'empty'
                    message: '',
                    branch: '', // 分支名称
                    pipelinePath: '' // 流水线路径
                }
            }
        },
        computed: {
            ...mapGetters('pipelines', [
                'getExecuteParams'
            ]),
            ...mapGetters('atom', [
                'getAllElements',
                'pacEnabled'
            ]),
            ...mapState('atom', [
                'pipelineInfo',
                'tempParamSet'
            ]),
            execVersionSelectorDisableTips () {
                return {
                    theme: 'light',
                    content: this.$t('preview.versionSelectorDisableTips')
                }
            },
            executeVersion () {
                return this.$route.params.version ? parseInt(this.$route.params.version) : this.pipelineInfo?.releaseVersion
            },
            isDebugPipeline () {
                return Object.prototype.hasOwnProperty.call(this.$route.query, 'debug')
            },
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            useLastParams () {
                return this.isDebugPipeline || this.startupInfo?.useLatestParameters
            },
            hasOtherParams () {
                if (this.isVisibleVersion) {
                    return [...this.otherParams, ...this.versionParamList].length
                }
                return this.otherParams.length
            },
            pipelineParams () {
                if (this.isVisibleVersion) {
                    return [...this.paramList, ...this.versionParamList]
                }
                return this.paramList
            },
            hasPipelineParams () {
                return this.pipelineParams.length
            },
            canElementSkip () {
                return this.isDebugPipeline || (this.startupInfo?.canElementSkip ?? false)
            },
            paramSetDiffTips () {
                if (!this.hasPipelineParams) {
                    return [
                        this.$t('currentPipelineHasNoParams')
                    ]
                }
                const diffs = Object.keys(this.applySetDiff.diffMap).reduce((acc, key) => {
                    const item = this.applySetDiff.diffMap[key]
                    if (item.length > 0) {
                        acc[key] = item
                    }
                    return acc
                }, {})
                if (diffs.length === 0) {
                    return []
                }
                return Object.keys(diffs).map(key => {
                    const item = diffs[key]
                    return this.$t(`inSet${`${key.slice(0, 1).toUpperCase()}${key.slice(1)}`}ParamTips`, [item.length, item.join(', ')])
                })
            },
            allPipelineParamValues () {
                return {
                    ...this.paramsValues,
                    ...this.versionParamValues,
                    ...this.buildValues,
                    ...this.constantValues,
                    ...this.otherValues
                }
            },
            codeRepoUrl () {
                const yamlInfo = this.pipelineInfo?.yamlInfo
                if (!yamlInfo?.webUrl) return ''
                const branch = this.selectedBranch
                const filePath = yamlInfo.filePath
                if (branch && filePath) {
                    return `${yamlInfo.webUrl}/blob/${encodeURIComponent(branch)}/${filePath}`
                }
                return yamlInfo.webUrl
            }
        },
        watch: {
            executeVersion: {
                handler () {
                    // 非 PAC 模式或调试模式直接初始化
                    if (!this.pacEnabled || this.isDebugPipeline) {
                        this.$nextTick(() => this.init())
                    }
                },
                immediate: true
            }
        },

        mounted () {
            bus.$off('start-execute')
            bus.$on('start-execute', this.executePipeline)
            bus.$on(PAC_BRANCH_CHANGE, this.handleBranchChange)
            bus.$on(PAC_BRANCH_INIT_DONE, this.handleBranchInitDone)

            if (this.pacEnabled && !this.isDebugPipeline) {
                this.isLoading = true
            }
        },
        beforeDestroy () {
            bus.$off('start-execute', this.executePipeline)
            bus.$off(PAC_BRANCH_CHANGE, this.handleBranchChange)
            bus.$off(PAC_BRANCH_INIT_DONE, this.handleBranchInitDone)
            this.togglePropertyPanel({
                isShow: false
            })
            setTimeout(() => {
                this.resetExecuteConfig(this.pipelineId)
            }, 0)
            // Clear temp paramSet when leaving preview page
            if (this.tempParamSet) {
                this.setTempParamSet(null)
            }
        },
        methods: {
            ...mapActions('atom', [
                'togglePropertyPanel',
                'fetchPipelineByVersion',
                'selectPipelineVersion',
                'fetchPacBranchPipeline',
                'setTempParamSet'
            ]),
            ...mapActions('pipelines', [
                'requestStartupInfo',
                'requestExecPipeline',
                'setExecuteStatus',
                'resetExecuteConfig',
                'setExecuteParams'
            ]),
            toggleCollapse (id) {
                if (this.activeName.has(id)) {
                    this.activeName.delete(id)
                } else {
                    this.activeName.add(id)
                }
                this.activeName = new Set(this.activeName)
            },
            handleExecuteVersionChange (version, versionInfo) {
                // TODO:
                this.selectPipelineVersion(versionInfo)
                this.$router.push({
                    name: this.$route.name,
                    params: {
                        ...this.$route.params,
                        version
                    }
                })
            },
            initParams (startupInfo) {
                if (startupInfo.canManualStartup) {
                    const values = this.getExecuteParams(this.pipelineId)
                    if (startupInfo.buildNo) {
                        this.buildNo = startupInfo.buildNo
                        this.isVisibleVersion = startupInfo.buildNo.required
                    }

                    this.paramList = startupInfo.properties.filter(p => !p.constant && p.required && !allVersionKeyList.includes(p.id) && p.propertyType !== 'BUILD').map(p => ({
                        ...p,
                        isChanged: isObject(p.defaultValue)
                            ? !isShallowEqual(p.defaultValue, p.value)
                            : p.defaultValue !== p.value,
                        readOnly: false,
                        label: `${p.id}${p.name ? `(${p.name})` : ''}`
                    }))
                    this.versionParamList = startupInfo.properties.filter(p => allVersionKeyList.includes(p.id)).map(p => ({
                        ...p,
                        isChanged: p.defaultValue !== p.value
                    }))
                    this.buildList = startupInfo.properties.filter(p => p.propertyType === 'BUILD')
                    this.constantParams = startupInfo.properties.filter(p => p.constant).map(p => ({
                        ...p,
                        label: `${p.id}${p.name ? `(${p.name})` : ''}`
                    }))
                    this.otherParams = startupInfo.properties.filter(p => !p.constant && !p.required && !allVersionKeyList.includes(p.id) && p.propertyType !== 'BUILD').map(p => ({
                        ...p,
                        label: `${p.id}${p.name ? `(${p.name})` : ''}`
                    }))
                    this.getParamsValue(values)
                    this.setExecuteParams({
                        pipelineId: this.pipelineId,
                        params: {
                            ...this.paramsValues,
                            ...this.versionParamValues,
                            ...this.buildValues,
                            ...this.constantValues,
                            ...this.otherValues
                        }
                    })
                } else {
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('newlist.withoutManualAtom')
                    })
                }
            },
            resetDefaultParams () {
                if (!this.showChangedParamsAlert) return
                this.$refs.paramSetSelector?.clear()
                this.updateParams()
            },
            handleCheckTotalChange (checkedTotal) {
                this.setPipelineSkipProp(this.pipelineModel.stages, checkedTotal)
            },
            getParamsValue (values) {
                const key = this.useLastParams ? 'value' : 'defaultValue'
                this.paramsValues = getParamsValuesMap(this.paramList, key, values)
                this.versionParamValues = getParamsValuesMap(this.versionParamList, key, values)
                this.buildValues = getParamsValuesMap(this.buildList, key, values)
                this.constantValues = getParamsValuesMap(this.constantParams, key, values)
                this.otherValues = getParamsValuesMap(this.otherParams, key, values)
            },
            updateParams (valueKey = 'defaultValue', values, versionValues) {
                this.showChangedParamsAlert = valueKey === 'value'
                this.paramsValues = values ?? getParamsValuesMap(this.paramList, valueKey)
                this.setExecuteParams({
                    pipelineId: this.pipelineId,
                    params: {
                        ...this.paramsValues
                    }
                })
                if (this.isVisibleVersion) {
                    this.versionParamValues = versionValues ?? getParamsValuesMap(this.versionParamList, valueKey)
                    this.setExecuteParams({
                        pipelineId: this.pipelineId,
                        params: {
                            ...this.versionParamValues
                        }
                    })
                }
            },
            async handleValidate () {
                const result = await this.validateForm()
                if (!result) {
                    this.$showTips({
                        message: this.$t('preview.paramsInvalidMsg'),
                        theme: 'error'
                    })
                }
                return result
            },
            async validateForm (type) {
                switch (type) {
                    case 'versionParamForm':
                        return await this.$refs?.versionParamForm?.$validator?.validateAll?.() ?? true
                    case 'paramsForm':
                        return await this.$refs?.paramsForm?.validateAll?.() ?? true
                    case 'buildForm':
                        return await this.$refs?.buildForm?.$validator?.validateAll?.() ?? true
                    default: {
                        const versionValid = await this.$refs?.versionParamForm?.$validator?.validateAll?.() ?? true
                        const paramsFormValid = await this.$refs?.paramsForm?.validateAll() ?? true
                        const buildFormValid = await this.$refs?.buildForm?.$validator?.validateAll?.() ?? true
                        return versionValid && paramsFormValid && buildFormValid
                    }
                }
            },
            handleChange (type, name, value) {
                this[`${type}Values`] = {
                    ...this[`${type}Values`],
                    [name]: value
                }
                this.setExecuteParams({
                    pipelineId: this.pipelineId,
                    params: {
                        ...this[`${type}Values`]
                    }
                })
            },
            handleBuildChange (...args) {
                this.handleChange('build', ...args)
            },
            handleParamChange (...args) {
                this.handleChange('params', ...args)
            },
            handleVersionChange (...args) {
                this.handleChange('versionParam', ...args)
            },
            handleBuildNoChange (name, value) {
                this.buildNo[name] = value

                this.setExecuteParams({
                    pipelineId: this.pipelineId,
                    params: {
                        buildNo: this.buildNo
                    }
                })
            },
            async init (branch, branchInfo) {
                try {
                    this.isLoading = true
                    const params = {
                        projectId: this.projectId,
                        pipelineId: this.pipelineId
                    }
                    
                    // 判断是否选择了正式版本（RELEASED）
                    const isReleasedVersion = branchInfo?.versionStatus === 'RELEASED'
                    
                    // 如果有选择分支/版本，根据类型添加不同参数
                    if (branch && this.pacEnabled) {
                        if (isReleasedVersion) {
                            // 正式版本使用 version 参数
                            params.version = branchInfo.version
                        } else {
                            // 分支使用 branch 参数
                            params.branch = branch
                        }
                    } else {
                        params.version = this.$route.params.version ?? this.pipelineInfo?.[this.isDebugPipeline ? 'version' : 'releaseVersion']
                    }
                    
                    let pipelineRes
                    
                    // 如果选择了分支/版本，根据类型使用不同接口
                    if (branch && this.pacEnabled) {
                        if (isReleasedVersion) {
                            // 正式版本使用普通版本接口
                            const [res, versionPipelineRes] = await Promise.all([
                                this.requestStartupInfo(params),
                                this.fetchPipelineByVersion(params)
                            ])
                            this.startupInfo = res
                            pipelineRes = versionPipelineRes
                            // 保存版本号，用于启动构建时指定 version
                            this.branchVersion = branchInfo.version
                        } else {
                            // 分支使用 PAC 分支编排接口
                            const [res, branchPipelineRes] = await Promise.all([
                                this.requestStartupInfo(params),
                                this.fetchPacBranchPipeline({
                                    projectId: this.projectId,
                                    pipelineId: this.pipelineId,
                                    branch
                                })
                            ])
                            this.startupInfo = res
                            pipelineRes = branchPipelineRes
                            // 保存分支版本号，用于启动构建时指定 version
                            this.branchVersion = branchPipelineRes?.version ?? null
                            this.expireReleasedVersion = branchPipelineRes?.expireReleasedVersion ?? false
                        }
                    } else {
                        const [res, normalPipelineRes] = await Promise.all([
                            this.requestStartupInfo(params),
                            this.fetchPipelineByVersion(params)
                        ])
                        this.startupInfo = res
                        pipelineRes = normalPipelineRes
                        // 非 PAC 分支模式下清空 branchVersion
                        this.branchVersion = null
                    }
                    
                    this.pipelineModel = {
                        ...pipelineRes?.modelAndSetting?.model,
                        stages: pipelineRes?.modelAndSetting?.model.stages.slice(1)
                    }
                    this.setPipelineSkipProp(this.pipelineModel.stages, this.checkTotal)
                    bus.$emit(UPDATE_PREVIEW_PIPELINE_NAME, this.pipelineModel?.name)
                    this.initParams(this.startupInfo)
                    this.showChangedParamsAlert = this.startupInfo?.useLatestParameters
                    this.pacError = { show: false, type: '', message: '' }
                    bus.$emit(UPDATE_PAC_ERROR_STATUS, false)
                    if (this.pendingParamSetChange) {
                        const { setName, paramsValues, versionValues } = this.pendingParamSetChange
                        this.pendingParamSetChange = null
                        this.updateParamsValues(setName, paramsValues, versionValues)
                    }
                } catch (err) {
                    const errorCode = err?.code || err?.status
                    // PAC 模式下特定错误码不返回，而是展示错误页面
                    const hrefMatch = err?.message?.match(/href="([^"]+)"/)
                    const href = hrefMatch ? hrefMatch[1] : ''
                    if (this.pacEnabled && errorCode === 2101378) {
                        // 分支版本不存在
                        this.pacError = {
                            show: true,
                            type: '404',
                            message: this.$t('preview.branchVersionNotFound'),
                            branch: branch || this.selectedBranch,
                            pipelinePath: this.pipelineInfo?.yamlInfo?.filePath || '',
                            href,
                            hrefTitle: this.$t('preview.goToCodeRepo')
                        }
                        bus.$emit(UPDATE_PAC_ERROR_STATUS, true)
                    } else if (this.pacEnabled && errorCode === 2101379) {
                        // 分支版本创建失败
                        const message = href
                            ? `${this.$t('preview.branchVersionCreateFailed')} <a href="${href}" class="pac-error-link" target="_blank">${this.$t('preview.viewDetail')}</a>`
                            : this.$t('preview.branchVersionCreateFailed')
                        this.pacError = {
                            show: true,
                            type: 'empty',
                            message
                        }
                        bus.$emit(UPDATE_PAC_ERROR_STATUS, true)
                    } else {
                        this.handleError(
                            err,
                            {
                                projectId: this.projectId,
                                resourceCode: this.pipelineId,
                                action: this.$permissionResourceAction.EXECUTE
                            }
                        )
                        this.$router.back()
                    }
                } finally {
                    this.isLoading = false
                }
            },
            /**
             * 处理分支变更
             * @param {String} branchName 分支名
             * @param {Object} branchInfo 分支信息对象
             */
            async handleBranchChange (branchName, branchInfo) {
                this.selectedBranch = branchName
                bus.$emit(PAC_BRANCH_LOADING, true)
                // 切换分支前重置执行参数，确保使用新接口返回的数据
                this.resetExecuteConfig(this.pipelineId)
                // 重新获取编排和参数数据
                try {
                    await this.init(branchName, branchInfo)
                } finally {
                    bus.$emit(PAC_BRANCH_LOADING, false)
                }
            },
            /**
             * 处理分支列表初始化完成（无分支可选或加载失败）
             * @param {Object} payload - { hasBranch: boolean, error?: Error }
             */
            handleBranchInitDone (payload) {
                // 分支列表为空或加载失败时，结束 loading 状态并显示错误
                if (!payload.hasBranch) {
                    this.isLoading = false
                    this.pacError = {
                        show: true,
                        type: 'empty',
                        message: payload.error
                            ? this.$t('preview.branchListLoadFailed')
                            : this.$t('preview.noBranchAvailable')
                    }
                    bus.$emit(UPDATE_PAC_ERROR_STATUS, true)
                }
            },
            async executePipeline () {
                let message, theme
                const paramsValid = await this.handleValidate()
                if (!paramsValid) return
                const params = this.getExecuteParams(this.pipelineId) ?? {}
                Object.keys(params).forEach(key => {
                    if (key !== 'buildNo' && isObject(params[key])) {
                        params[key] = JSON.stringify(params[key])
                    }
                })
                const skipAtoms = this.getSkipedAtoms()
                console.log(params, skipAtoms)
                try {
                    this.setExecuteStatus(true)
                    // 确定启动版本号：PAC 分支模式使用 branchVersion，调试模式使用 pipelineInfo.version，其他使用 executeVersion
                    let execVersion
                    if (this.isDebugPipeline) {
                        execVersion = this.pipelineInfo?.version
                    } else if (this.pacEnabled && this.branchVersion !== null) {
                        // PAC 模式下选择了分支，使用分支版本号
                        execVersion = this.branchVersion
                    } else {
                        execVersion = this.executeVersion
                    }
                    // 请求执行构建
                    const res = await this.requestExecPipeline({
                        projectId: this.projectId,
                        pipelineId: this.pipelineId,
                        version: execVersion,
                        params: {
                            ...skipAtoms,
                            ...params
                        }
                    })

                    if (res && res.id) {
                        message = this.$t('newlist.sucToStartBuild')
                        theme = 'success'

                        this.$router.push({
                            name: 'pipelinesDetail',
                            params: {
                                projectId: this.projectId,
                                pipelineId: this.pipelineId,
                                buildNo: res.id
                            }
                        })
                    } else {
                        message = this.$t('newlist.failToStartBuild')
                        theme = 'error'
                    }
                } catch (err) {
                    this.handleError(err, {
                        projectId: this.$route.params.projectId,
                        resourceCode: this.$route.params.pipelineId,
                        action: this.$permissionResourceAction.EXECUTE
                    })
                } finally {
                    this.setExecuteStatus(false)

                    message && this.$showTips({
                        message,
                        theme
                    })
                }
            },

            getSkipedAtoms () {
                const allElements = this.getAllElements(this.pipelineModel.stages)
                return allElements
                    .filter(element => !element.canElementSkip)
                    .reduce((acc, element) => {
                        acc[`devops_container_condition_skip_atoms_${element.id}`] = true
                        return acc
                    }, {})
            },
            setPipelineSkipProp (stages, checkedTotal) {
                stages.forEach(stage => {
                    const stageDisabled = stage.stageControlOption?.enable === false
                    this.$set(stage, 'runStage', !stageDisabled && checkedTotal)
                    stage.containers.forEach(container => {
                        const containerDisabled = container.jobControlOption?.enable === false
                        this.$set(container, 'runContainer', !containerDisabled && checkedTotal)
                        container.elements.forEach(element => {
                            const isSkipEle = element.additionalOptions?.enable === false || containerDisabled
                            this.$set(element, 'canElementSkip', !isSkipEle && checkedTotal)
                        })
                    })
                })
            },

            editTrigger () {
                const url = `${WEB_URL_PREFIX}/pipeline/${this.projectId}/${this.pipelineId}/edit/?tab=trigger`
                window.open(url, '_blank')
            },

            goToCodeRepo () {
                if (this.codeRepoUrl) {
                    window.open(this.codeRepoUrl, '_blank')
                }
            },

            saveAsParamSet () {
                this.$refs.paramSetSelector.saveAsParamSet(this.pipelineParams, {
                    ...this.paramsValues,
                    ...this.versionParamValues
                })
            },
            particalyUpdateParams (origin, partical, diffMap) {
                const allParamMap = this.startupInfo?.properties?.reduce((acc, param) => {
                    acc.set(param.id, param)
                    return acc
                }, new Map()) ?? new Map()
                Object.keys(partical).forEach(key => {
                    const param = allParamMap.get(key)
                    if (Object.prototype.hasOwnProperty.call(origin, key)) {
                        origin[key] = partical[key]
                    }

                    if (!param) {
                        diffMap.deleted.push(key)
                    } else if (!(param.required === true && param.constant === false) && !allVersionKeyList.includes(key)) {
                        diffMap.noRequired.push(key)
                    } else if (!isShallowEqual(param.defaultValue, partical[key])) {
                        diffMap.changed.push(key)
                    }
                })
                return {
                    origin,
                    diffMap
                }
            },
            updateParamsValues (setName, paramsValues, versionValues) {
                if (!this.startupInfo) {
                    this.pendingParamSetChange = { setName, paramsValues, versionValues }
                    return
                }
                const applySetDiff = {
                    setName,
                    diffMap: {
                        changed: [],
                        deleted: [],
                        noRequired: []
                    }
                }
                this.particalyUpdateParams(this.paramsValues, paramsValues, applySetDiff.diffMap)
                if (versionValues) {
                    this.particalyUpdateParams(this.versionParamValues, versionValues, applySetDiff.diffMap)
                }
                const changedMap = applySetDiff.diffMap.changed.reduce((acc, key) => {
                    acc[key] = true
                    return acc
                }, {})
                
                this.paramList.forEach(param => {
                    param.isChanged = changedMap[param.id] ?? false
                })
                this.versionParamList.forEach(param => {
                    param.isChanged = changedMap[param.id] ?? false
                })
                this.applySetDiff = applySetDiff
                this.isApplySet = true
                this.updateParams('value', this.paramsValues, this.versionParamValues)
            }
            
        }
    }
</script>

<style lang="scss">
@import '../../scss/conf';
@import '../../scss/mixins/ellipsis';
$header-height: 36px;

.pipeline-execute-preview {
    height: 100%;
    display: flex;
    flex-direction: column;
    margin: 24px 24px 12px 24px;
    overflow: hidden;
    &-content {
        overflow: auto !important;
    }
    .params-content-item {
        background: #FFFFFF;
        box-shadow: 0 2px 4px 0 #1919290d;
        border-radius: 2px;
        margin-bottom: 20px;
    }

    .pipeline-execute-version-select {
        display: flex;
        flex-shrink: 0;
        align-items: center;
        padding: 0 24px;
        height: 80px;
        margin: 0 0 24px 0;
        background-color: white;
        border-bottom: 1px solid #DCDEE5;
        font-size: 14px;
        > span {
            border: 1px solid #DCDEE5;
            line-height: 32px;
            background: #FAFBFD;
            border: 1px solid #C4C6CC;
            padding: 0 8px;
            color: #4D4F56;
            border-radius: 2px 0 0 2px;
            margin-right: -1px;
        }
        .pipeline-version-dropmenu-trigger {
            line-height: 32px;
            height: 32px;
            width: 520px;
            box-sizing: content-box;
            background-color: white;
            border: 1px solid #C4C6CC;
            border-radius: 0 2px 2px 0;
        }

        .exec-version-is-disabled .pipeline-version-dropmenu-trigger {
            background-color: #fafbfd;
            cursor: not-allowed;
            border-color: #dcdee5;
        }
        > .icon-info-circle {
            display: block;
            margin-left: 8px;
            cursor: pointer;
        }

    }

    @for $i from 1 through 6 {
        :nth-child(#{$i} of .params-collapse-trigger) {
            top: $header-height * ($i - 1);
        }
    }

    .params-collapse-trigger {
        display: flex;
        flex-shrink: 0;
        align-items: center;
        font-size: 14px;
        font-weight: 700;
        height: $header-height;
        cursor: pointer;
        top: 0;
        margin: 0 24px;
        position: sticky;
        grid-gap: 8px;
        color: #313238;
        background-color: white;
        z-index: 6;

        .no-bold-font {
            font-weight: normal;
        }

        &.params-collapse-expand {
            .icon-angle-right {
                transform: rotate(90deg);
            }
        }

        .icon-angle-right {
            transition: all 0.3 ease;
            color: #4D4F56;
        }

        .collapse-trigger-divider {
            display: inline-block;
            color: #DCDEE5;
        }

        .text-link {
            font-size: 14px;
            ;
            font-weight: normal;

        }
        .icon-question-circle {
            display: inline-block;
            color: #979BA5;
            margin-left: 4px;
        }
    }

    .params-collapse-content {
        padding: 16px 24px;
    }

    .empty-execute-params-exception {
        display: flex;
        align-items: center;
        justify-content: center;
        height: 100%;
    }

    .bkci-property-panel {
        .bk-sideslider-wrapper {
            top: 0;

            .bk-sideslider-title {
                word-wrap: break-word;
                word-break: break-all;
                overflow: hidden;
                padding: 0 0 0 20px !important;
            }
        }
    }
    .pipeline-optional-model {
        height: calc(100vh - 160px) !important;
    }

    .changed-tips-alert {
        margin-bottom: 12px;
    }

    .expire-released-version-alert {
        margin-bottom: 12px;
        .bk-alert-wraper {
            align-items: center;
        }

        .expire-released-version-alert-content {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 8px;
        }

        .expire-released-version-branch {
            color: #3A84FF;
            margin: 0 4px;
        }

        .expire-released-version-expired {
            color: #EA3636;
            margin: 0 2px;
        }

        .expire-released-version-actions {
            display: inline-flex;
            align-items: center;
            gap: 16px;
            flex-shrink: 0;
            margin-left: 12px;

            .devops-icon {
                margin-right: 4px;
            }
        }

        .expire-released-version-link {
            display: inline-flex;
            align-items: center;
            cursor: pointer;
            .icon-jump-link {
                margin-right: 2px;
            }
        }
    }
    .param-set-diff-tips {
        padding: 12px;
        list-style: disc;
        > li {
            list-style: disc;
        }
    }

    .pac-error-container {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #FFFFFF;
        box-shadow: 0 2px 4px 0 #1919290d;
        border-radius: 2px;
        
        .exception-wrap-item {
            padding: 40px 0;
        }
        
        .pac-error-content {
            text-align: center;
            
            .pac-error-title {
                font-size: 14px;
                color: #63656E;
                margin-bottom: 16px;
                a {
                    color: #3A84FF;
                }
            }
            
            .pac-error-detail {
                font-size: 12px;
                color: #979BA5;
                margin-bottom: 8px;
                
                &:last-child {
                    margin-bottom: 0;
                }
            }
            .pac-error-link {
                color: #3A84FF;
                font-size: 12px;
                margin-left: 5px;
            }
        }
    }
}
</style>
