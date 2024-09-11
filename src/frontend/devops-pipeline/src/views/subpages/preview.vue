<template>
    <div
        class="pipeline-execute-preview"
        v-bkloading="{ isLoading }"
    >
        <bk-alert
            v-if="isDebugPipeline"
            :title="$t('debugHint')"
        ></bk-alert>
        <template v-if="!isDebugPipeline && buildList.length">
            <header
                :class="['params-collapse-trigger', {
                    'params-collapse-expand': activeName.has(1)
                }]"
                @click="toggleCollapse(1)"
            >
                <i class="devops-icon icon-angle-right" />
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
        </template>
        <template v-if="isVisibleVersion">
            <header
                :class="['params-collapse-trigger', {
                    'params-collapse-expand': activeName.has(2)
                }]"
                @click="toggleCollapse(2)"
            >
                <i class="devops-icon icon-angle-right" />
                {{ $t('preview.introVersion') }}
            </header>
            <div
                v-if="activeName.has(2)"
                class="params-collapse-content"
            >
                <pipeline-versions-form
                    ref="versionParamForm"
                    :build-no="buildNo"
                    :is-preview="true"
                    :version-param-values="versionParamValues"
                    :handle-version-change="handleVersionChange"
                    :handle-build-no-change="handleBuildNoChange"
                />
            </div>
        </template>
        <header
            :class="['params-collapse-trigger', {
                'params-collapse-expand': activeName.has(3)
            }]"
            @click="toggleCollapse(3)"
        >
            <i class="devops-icon icon-angle-right" />
            {{ $t('buildParams') }}
            <template v-if="paramList.length > 0">
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
            v-if="activeName.has(3)"
            class="params-collapse-content"
        >
            <bk-alert
                v-if="showChangedParamsAlert && changedParams.length"
                type="warning"
                :title="$t('paramChangeTips', [changedParams.length])"
            >
            </bk-alert>
            <pipeline-params-form
                v-if="paramList.length > 0"
                ref="paramsForm"
                :param-values="paramsValues"
                :highlight-changed-param="showChangedParamsAlert"
                :handle-param-change="handleParamChange"
                :params="paramList"
            />
            <bk-exception
                v-else
                type="empty"
                scene="part"
            >
                {{ $t('noParams') }}
            </bk-exception>
        </div>

        <template v-if="constantParams.length > 0">
            <header
                :class="['params-collapse-trigger', {
                    'params-collapse-expand': activeName.has(4)
                }]"
                @click="toggleCollapse(4)"
            >
                <i class="devops-icon icon-angle-right" />
                {{ $t('newui.const') }}
            </header>
            <div
                v-if="activeName.has(4)"
                class="params-collapse-content"
            >
                <pipeline-params-form
                    ref="constParamsForm"
                    disabled
                    :param-values="constantValues"
                    :params="constantParams"
                />
            </div>
        </template>
        <template v-if="otherParams.length > 0">
            <header
                :class="['params-collapse-trigger', {
                    'params-collapse-expand': activeName.has(5)
                }]"
                @click="toggleCollapse(5)"
            >
                <i class="devops-icon icon-angle-right" />
                {{ $t('newui.pipelineParam.otherVar') }}
            </header>
            <div
                v-if="activeName.has(5)"
                class="params-collapse-content"
            >
                <pipeline-params-form
                    ref="otherParamsForm"
                    disabled
                    :param-values="otherValues"
                    :params="otherParams"
                />
            </div>
        </template>

        <header
            :class="['params-collapse-trigger', {
                'params-collapse-expand': activeName.has(6)
            }]"
            @click="toggleCollapse(6)"
        >
            <i class="devops-icon icon-angle-right" />
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
            v-if="activeName.has(6)"
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
    </div>
</template>

<script>
    import Pipeline from '@/components/Pipeline'
    import PipelineVersionsForm from '@/components/PipelineVersionsForm.vue'
    import PipelineParamsForm from '@/components/pipelineParamsForm.vue'
    import { UPDATE_PREVIEW_PIPELINE_NAME, bus } from '@/utils/bus'
    import { allVersionKeyList } from '@/utils/pipelineConst'
    import { getParamsValuesMap } from '@/utils/util'
    import { mapActions, mapGetters, mapState } from 'vuex'
    export default {
        components: {
            PipelineVersionsForm,
            PipelineParamsForm,
            Pipeline
        },
        data () {
            return {
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
                'getAllElements'
            ]),
            ...mapState('atom', [
                'pipelineInfo'
            ]),
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
            changedParams () {
                return this.paramList.filter(p => p.isChanged)
            },
            resetDefaultParamsTips () {
                return this.$t(this.isDebugPipeline ? 'debugParamsTips' : 'restoreDetaulParamsTips')
            },
            canElementSkip () {
                return this.isDebugPipeline || (this.startupInfo?.canElementSkip ?? false)
            }
        },
        watch: {
            'pipelineInfo.releaseVersion' (val) {
                if (!this.$route.params.version && val) {
                    this.init()
                }
            }
        },

        mounted () {
            if (this.$route.params.version || this.pipelineInfo?.releaseVersion) {
                this.init()
            }
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
                'fetchPipelineByVersion'
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
            initParams (startupInfo) {
                if (startupInfo.canManualStartup) {
                    const values = this.getExecuteParams(this.pipelineId)
                    console.log(values)
                    if (startupInfo.buildNo) {
                        this.buildNo = startupInfo.buildNo
                        this.isVisibleVersion = startupInfo.buildNo.required
                    }
                    this.paramList = startupInfo.properties.filter(p => !p.constant && p.required && !allVersionKeyList.includes(p.id) && p.propertyType !== 'BUILD').map(p => ({
                        ...p,
                        isChanged: p.defaultValue !== p.value,
                        readOnly: false,
                        label: `${p.id}${p.name ? `(${p.name})` : ''}`
                    }))
                    this.versionParamList = startupInfo.properties.filter(p => allVersionKeyList.includes(p.id))
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
                this.buildNo.buildNo = value

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
                const skipAtoms = this.getSkipedAtoms()
                console.log(params, skipAtoms)
                try {
                    this.setExecuteStatus(true)
                    // 请求执行构建
                    const res = await this.requestExecPipeline({
                        projectId: this.projectId,
                        pipelineId: this.pipelineId,
                        ...(this.isDebugPipeline ? { version: this.pipelineInfo?.version } : {}),
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
    box-shadow: 0 2px 2px 0 #00000026;
    overflow: auto !important;
    background-color: white;

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
        border-bottom: 1px solid #DCDEE5;
        height: $header-height;
        top: 0;
        margin: 0 24px;
        position: sticky;
        grid-gap: 10px;
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
            font-size: 12px;
            width: 12px;
            height: 12px;
            line-height: 1;
            text-align: center;
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
}
</style>
