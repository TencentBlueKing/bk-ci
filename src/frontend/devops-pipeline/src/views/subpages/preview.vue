<template>
    <div
        class="pipeline-execute-preview"
        v-bkloading="{ isLoading }"
    >
        <div
            v-if="!isDebugPipeline"
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
                :build-only="false"
            />
            <i
                class="bk-icon icon-info-circle"
                v-bk-tooltips="execVersionSelectorDisableTips"
            />
        </div>
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
                    <template v-if="hasPipelineParams">
                        <span class="collapse-trigger-divider">|</span>
                        <span
                            v-if="useLastParams"
                            class="text-link"
                            @click.stop="updateParams()"
                        >
                            {{ $t('resetDefault') }}
                            <i
                                class="devops-icon icon-question-circle"
                                v-bk-tooltips="resetDefaultParamsTips"
                            />
                        </span>
                        <span
                            v-else
                            class="text-link"
                            @click.stop="updateParams('value')"
                        >
                            {{ $t('useLastParams') }}
                        </span>
                    </template>
                </header>
                <div
                    v-show="activeName.has(2)"
                    class="params-collapse-content"
                >
                    <bk-alert
                        v-if="showChangedParamsAlert && changedParamsLength"
                        type="warning"
                        :title="$t('paramChangeTips', [changedParamsLength])"
                    >
                    </bk-alert>
                    <pipeline-params-form
                        v-if="hasPipelineParams"
                        ref="paramsForm"
                        :param-values="paramsValues"
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
    </div>
</template>

<script>
    import Pipeline from '@/components/Pipeline'
    import PipelineVersionsForm from '@/components/PipelineVersionsForm.vue'
    import PipelineParamsForm from '@/components/pipelineParamsForm.vue'
    import { UPDATE_PREVIEW_PIPELINE_NAME, bus } from '@/utils/bus'
    import { allVersionKeyList } from '@/utils/pipelineConst'
    import { getParamsValuesMap, isObject, isShallowEqual } from '@/utils/util'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import VersionSelector from '../../components/PipelineDetailTabs/VersionSelector.vue'
    import renderSortCategoryParams from '@/components/renderSortCategoryParams'

    export default {
        components: {
            VersionSelector,
            PipelineVersionsForm,
            PipelineParamsForm,
            Pipeline,
            renderSortCategoryParams
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
                checkTotal: true
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
                'pipelineInfo'
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
            changedParamsLength () {
                const length = [...this.paramList, ...this.versionParamList].filter(p => p.isChanged).length
                if (this.buildNo.isChanged) {
                    return length + 1
                }
                return length
            },
            hasOtherParams () {
                if (!this.isVisibleVersion) {
                    return [...this.otherParams, ...this.versionParamList].length
                }
                return this.otherParams.length
            },
            hasPipelineParams () {
                if (this.isVisibleVersion) {
                    return [...this.paramList, ...this.versionParamList].length
                }
                return this.paramList.length
            },
            resetDefaultParamsTips () {
                return this.$t(this.isDebugPipeline ? 'debugParamsTips' : 'restoreDetaulParamsTips')
            },
            canElementSkip () {
                return this.isDebugPipeline || (this.startupInfo?.canElementSkip ?? false)
            }
        },
        watch: {
            executeVersion: {
                handler () {
                    this.$nextTick(this.init)
                },
                immediate: true
            }
        },

        mounted () {
            bus.$off('start-execute')
            bus.$on('start-execute', this.executePipeline)
        },
        beforeDestroy () {
            bus.$off('start-execute', this.executePipeline)
            this.togglePropertyPanel({
                isShow: false
            })
            setTimeout(() => {
                this.resetExecuteConfig(this.pipelineId)
            }, 0)
        },
        methods: {
            ...mapActions('atom', [
                'togglePropertyPanel',
                'fetchPipelineByVersion',
                'selectPipelineVersion'
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
            updateParams (valueKey = 'defaultValue') {
                this.showChangedParamsAlert = valueKey === 'value'
                this.paramsValues = getParamsValuesMap(this.paramList, valueKey)
                this.setExecuteParams({
                    pipelineId: this.pipelineId,
                    params: {
                        ...this.paramsValues
                    }
                })
                if (this.isVisibleVersion) {
                    this.versionParamValues = getParamsValuesMap(this.versionParamList, valueKey)
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
                        return await this.$refs?.paramsForm?.$validator?.validateAll?.() ?? true
                    case 'buildForm':
                        return await this.$refs?.buildForm?.$validator?.validateAll?.() ?? true
                    default: {
                        const versionValid = await this.$refs?.versionParamForm?.$validator?.validateAll?.() ?? true
                        const paramsFormValid = await this.$refs?.paramsForm?.$validator?.validateAll?.() ?? true
                        const buildFormValid = await this.$refs?.buildForm?.$validator?.validateAll?.() ?? true
                        return versionValid && paramsFormValid && buildFormValid
                    }
                }
            },
            handleChange (type, name, value) {
                this[`${type}Values`][name] = value
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
            async init () {
                try {
                    this.isLoading = true
                    const params = {
                        projectId: this.projectId,
                        pipelineId: this.pipelineId,
                        version: this.$route.params.version ?? this.pipelineInfo?.[this.isDebugPipeline ? 'version' : 'releaseVersion']
                    }
                    const [res, pipelineRes] = await Promise.all([
                        this.requestStartupInfo(params),
                        this.fetchPipelineByVersion(params)
                    ])
                    this.pipelineModel = {
                        ...pipelineRes?.modelAndSetting?.model,
                        stages: pipelineRes?.modelAndSetting?.model.stages.slice(1)
                    }
                    this.setPipelineSkipProp(this.pipelineModel.stages, this.checkTotal)
                    bus.$emit(UPDATE_PREVIEW_PIPELINE_NAME, this.pipelineModel?.name)
                    this.startupInfo = res
                    this.initParams(this.startupInfo)
                    this.showChangedParamsAlert = this.startupInfo?.useLatestParameters
                } catch (err) {
                    this.handleError(
                        err,
                        {
                            projectId: this.projectId,
                            resourceCode: this.pipelineId,
                            action: this.$permissionResourceAction.EXECUTE
                        }
                    )
                    this.$router.back()
                } finally {
                    this.isLoading = false
                }
            },
            async executePipeline () {
                let message, theme
                const paramsValid = await this.handleValidate()
                if (!paramsValid) return
                const params = this.getExecuteParams(this.pipelineId)
                Object.keys(params).forEach(key => {
                    if (key !== 'buildNo' && isObject(params[key])) {
                        params[key] = JSON.stringify(params[key])
                    }
                })
                const skipAtoms = this.getSkipedAtoms()
                console.log(params, skipAtoms)
                try {
                    this.setExecuteStatus(true)
                    // 请求执行构建
                    const res = await this.requestExecPipeline({
                        projectId: this.projectId,
                        pipelineId: this.pipelineId,
                        version: this.isDebugPipeline ? this.pipelineInfo?.version : this.executeVersion,
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
        grid-gap: 10px;
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
            margin: 0 10px;
            color: #DCDEE5;
        }

        .text-link {
            font-size: 14px;
            ;
            font-weight: normal;

            .icon-question-circle {
                display: inline-block;
                color: #979BA5;
                margin-left: 4px;
            }
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
}
</style>
