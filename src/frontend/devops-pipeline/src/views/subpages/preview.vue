<template>
    <div class="pipeline-execute-preview" v-bkloading="{ isLoading }">
        <div class="scroll-container">
            <div class="execute-preview-content">
                <div class="global-params" v-if="buildList.length">
                    <p class="item-title">{{ $t('preview.build') }}<i :class="['devops-icon icon-angle-down', { 'icon-flip': isDropdownShowBuild }]" @click="toggleIcon('build')"></i></p>
                    <pipeline-params-form ref="buildForm" v-if="isDropdownShowBuild" :param-values="buildValues" :handle-param-change="handleBuildChange" :params="buildList"></pipeline-params-form>
                </div>
                <div class="version-option" v-if="isVisibleVersion">
                    <p class="item-title">{{ $t('preview.introVersion') }}：<i :class="['devops-icon icon-angle-down', { 'icon-flip': isDropdownShowVersion }]" @click="toggleIcon('version')"></i></p>
                    <pipeline-versions-form ref="versionForm"
                        v-if="isDropdownShowVersion"
                        :build-no="buildNo"
                        :is-preview="true"
                        :version-param-values="versionParamValues"
                        :handle-version-change="handleVersionChange"
                        :handle-build-no-change="handleBuildNoChange"
                    ></pipeline-versions-form>
                </div>
                <div class="global-params" v-if="paramList.length">
                    <p class="item-title">{{ $t('template.pipelineVar') }}：<i :class="['devops-icon icon-angle-down', { 'icon-flip': isDropdownShowParam }]" @click="toggleIcon('params')"></i></p>
                    <pipeline-params-form ref="paramsForm" v-if="isDropdownShowParam" :param-values="paramValues" :handle-param-change="handleParamChange" :params="paramList"></pipeline-params-form>
                </div>
                <div class="execute-detail-option" v-if="pipeline">
                    <p class="item-title">
                        <section v-if="curPipelineInfo.canElementSkip">{{ $t('preview.atomToExec') }}：
                            <span class="item-title-tips">({{ $t('preview.skipTipsPrefix') }}<span @click.stop="editTrigger" class="text-link item-title-tips-link">{{ $t('preview.manualTrigger') }}</span>{{ $t('preview.skipTipsSuffix') }})</span>
                            <bk-checkbox v-if="curPipelineInfo.canElementSkip" style="margin-left: 15px" v-model="checkTotal" @click.stop>{{ $t('preview.selectAll') }}/{{ $t('preview.selectNone') }}</bk-checkbox>
                        </section>
                    </p>
                    <div class="pipeline-detail">
                        <pipeline
                            is-preview
                            :show-header="false"
                            :pipeline="previewPipeline"
                            :editable="false"
                            :can-skip-element="curPipelineInfo.canElementSkip"
                        >
                        </pipeline>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import { mapGetters, mapActions } from 'vuex'
    import Pipeline from '@/components/Pipeline'
    import { bus } from '@/utils/bus'
    import { getParamsValuesMap } from '@/utils/util'
    import PipelineParamsForm from '@/components/pipelineParamsForm.vue'
    import PipelineVersionsForm from '@/components/PipelineVersionsForm.vue'
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'
    import { allVersionKeyList } from '@/utils/pipelineConst'

    export default {
        components: {
            Pipeline,
            PipelineParamsForm,
            PipelineVersionsForm
        },
        mixins: [pipelineOperateMixin],
        data () {
            return {
                previewPipeline: null,
                isLoading: false,
                isVisibleVersion: false,
                isDropdownShowParam: true,
                isDropdownShowVersion: true,
                isDropdownShowBuild: true,
                paramList: [],
                versionParamList: [],
                paramValues: {},
                versionParamValues: {},
                curPipelineInfo: {},
                buildNo: {},
                checkTotal: true,
                buildValues: {},
                buildList: []
            }
        },
        computed: {
            ...mapGetters({
                curParamList: 'pipelines/getCurAtomPrams'
            }),
            ...mapGetters('atom', [
                'getAllElements'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            }
        },
        watch: {
            pipelineId (pipelineId) {
                this.$router.push({
                    name: 'pipelinesEdit',
                    params: {
                        projectId: this.projectId,
                        pipelineId
                    }
                })
            },
            pipeline: {
                immediate: true,
                handler (newVal, oldVal) {
                    if (oldVal === null && newVal) {
                        this.setPipelineSkipProp(newVal.stages, this.checkTotal)
                        this.previewPipeline = {
                            ...newVal,
                            stages: newVal.stages.slice(1)
                        }
                    }
                }
            },
            checkTotal (checkedTotal) {
                this.setPipelineSkipProp(this.previewPipeline.stages, checkedTotal)
            }
        },
        async created () {
            await this.init()
            bus.$off('start-execute')
            bus.$on('start-execute', this.getExecuteParams)
        },
        beforeDestroy () {
            bus.$off('start-execute')
            this.togglePropertyPanel({
                isShow: false
            })
            this.$store.commit('pipelines/updateCurAtomPrams', null)
            this.setPipelineEditing(false)
            this.setPipeline(null)
        },
        destroyed () {
            bus.$off('start-execute')
        },
        methods: {
            ...mapActions('atom', [
                'togglePropertyPanel',
                'requestPipeline',
                'setPipeline',
                'setPipelineEditing'
            ]),
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
            async init () {
                this.isLoading = true
                try {
                    this.requestPipeline(this.$route.params)
                    if (!this.curParamList) {
                        const res = await this.$store.dispatch('pipelines/requestStartupInfo', {
                            projectId: this.projectId,
                            pipelineId: this.pipelineId
                        })

                        this.curPipelineInfo = res
                    } else {
                        this.curPipelineInfo = this.curParamList
                    }

                    if (this.curPipelineInfo.canManualStartup) {
                        if (this.curPipelineInfo.buildNo) {
                            this.buildNo = this.curPipelineInfo.buildNo
                            this.isVisibleVersion = this.curPipelineInfo.buildNo.required
                        }
                        this.paramList = this.curPipelineInfo.properties.filter(p => p.required && !allVersionKeyList.includes(p.id) && p.propertyType !== 'BUILD')
                        this.versionParamList = this.curPipelineInfo.properties.filter(p => allVersionKeyList.includes(p.id))
                        this.buildList = this.curPipelineInfo.properties.filter(p => p.propertyType === 'BUILD')
                        this.paramValues = getParamsValuesMap(this.paramList)
                        this.versionParamValues = getParamsValuesMap(this.versionParamList)
                        this.buildValues = getParamsValuesMap(this.buildList)
                    } else {
                        throw new Error(this.$t('newlist.withoutManualAtom'))
                    }
                } catch (err) {
                    this.handleError(err, [{
                        actionId: this.$permissionActionMap.execute,
                        resourceId: this.$permissionResourceMap.pipeline,
                        instanceId: [{
                            id: this.pipelineId,
                            name: this.pipelineId
                        }],
                        projectId: this.projectId
                    }])
                } finally {
                    this.isLoading = false
                }
            },
            async getExecuteParams () {
                if (this.executeStatus) return
                const allElements = this.getAllElements(this.previewPipeline.stages)
                const skipAtoms = allElements.filter(element => !element.canElementSkip).map(element => `devops_container_condition_skip_atoms_${element.id}`)
                const versionValid = this.$refs.versionForm ? await this.$refs.versionForm.$validator.validateAll() : true

                let paramsFormValid = true
                let buildFormValid = true
                if (this.$refs.paramsForm) {
                    paramsFormValid = await this.$refs.paramsForm.$validator.validateAll()
                    this.$refs.paramsForm.submitForm()
                }
                if (this.$refs.buildForm) {
                    buildFormValid = await this.$refs.buildForm.$validator.validateAll()
                    this.$refs.buildForm.submitForm()
                }
                if (buildFormValid && paramsFormValid && versionValid) {
                    const { paramValues, versionParamValues, buildNo, buildValues } = this
                    const newParams = Object.assign({}, paramValues, versionParamValues, buildValues)
                    if (this.isVisibleVersion) Object.assign(newParams, { buildNo })
                    this.executePipeline(skipAtoms.reduce((res, skip) => {
                        res[skip] = true
                        return res
                    }, newParams), true)
                } else {
                    // 参数非法
                    this.$showTips({
                        message: this.$t('preview.paramsInvalidMsg'),
                        theme: 'error'
                    })
                }
            },
            toggleIcon (type) {
                if (type === 'version') this.isDropdownShowVersion = !this.isDropdownShowVersion
                else if (type === 'params') this.isDropdownShowParam = !this.isDropdownShowParam
                else this.isDropdownShowBuild = !this.isDropdownShowBuild
            },
            handleBuildChange (name, value) {
                this.buildValues[name] = value
            },
            handleParamChange (name, value) {
                this.paramValues[name] = value
            },
            handleVersionChange (name, value) {
                this.versionParamValues[name] = value
            },
            handleBuildNoChange (name, value) {
                this.buildNo.buildNo = value
            },
            editTrigger () {
                const url = `${WEB_URL_PREFIX}/pipeline/${this.projectId}/${this.pipelineId}/edit#manualTrigger`
                window.open(url, '_blank')
            }
        }
    }
</script>

<style lang="scss">
    @import '../../scss/conf';
    @import '../../scss/mixins/ellipsis';

    .pipeline-execute-preview {
        height: 100%;
        .scroll-container {
            height: 100%;
            overflow: initial;
            &:before {
                display: none;
            }
        }
        .execute-preview-content {
            padding: 20px 44px 30px;
            height: 100%;
            overflow: auto;
            .item-title {
                line-height: 36px;
                border-bottom: 1px solid $borderWeightColor;
                .devops-icon {
                    display: inline-block;
                    margin-left: 6px;
                    transition: all ease 0.2s;
                    font-size: 12px;
                    color: $primaryColor;
                    cursor: pointer;
                    &.icon-flip {
                        transform: rotate(180deg);
                    }
                }
                &-tips {
                    font-size: 14px;
                    &-link {
                        font-size: 14px;
                        cursor: pointer;
                    }
                }
            }
            .global-params {
                margin-bottom: 20px;
                .bk-form {
                    display: flex;
                    flex-wrap: wrap;
                    justify-content: space-between;
                    padding-top: 10px;
                }
                .bk-form-content {
                    position: relative;
                }
                .bk-form-help {
                    margin: 0;
                    width: 100%;
                    @include ellipsis();
                    display: inline-block;
                }
                .bk-form-item {
                    margin-top: 0px;
                    width: 48%;
                }
                .bk-form .bk-form-item:before, .bk-form:after {
                    display: none;
                }
            }

            .pipeline-detail {
                padding: 20px 10px;
            }

            .version-option {
                margin-bottom: 30px;
                 .build-no-group .bk-form-content {
                    margin-top: 32px;
                    width: 400px;
                }
                .bk-form-item {
                    width: 200px;
                    margin: 10px 20px 0 0;
                }
            }
            .global-params,
            .version-option {
                .bk-label {
                    width: 100%;
                    text-align: left;
                    @include ellipsis();
                }

                .bk-form-content {
                    float: left;
                    margin-left: 0;
                    width: 100%;
                }
            }

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
