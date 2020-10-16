<template>
    <div class="pipeline-execute-preview" v-bkloading="{ isLoading }">
        <div class="scroll-container">
            <div class="execute-preview-content">
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
                        <pipeline :show-header="false" :pipeline="pipeline" :editable="false" :is-preview="true" :can-skip-element="curPipelineInfo.canElementSkip"></pipeline>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import { mapGetters, mapActions } from 'vuex'
    import Pipeline from '@/components/Pipeline'
    import Vue from 'vue'
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
                isLoading: false,
                isVisibleVersion: false,
                isDropdownShowParam: true,
                isDropdownShowVersion: true,
                paramList: [],
                versionParamList: [],
                paramValues: {},
                versionParamValues: {},
                curPipelineInfo: {},
                buildNo: {},
                checkTotal: true
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
            checkTotal (val) {
                this.pipeline.stages.forEach(stage => {
                    const stageDisabled = stage.stageControlOption && stage.stageControlOption.enable === false
                    if (!stageDisabled) {
                        stage.runStage = val
                    }

                    stage.containers.forEach(container => {
                        if (container['@type'] !== 'trigger') {
                            const containerDisabled = stageDisabled || (container.jobControlOption && container.jobControlOption.enable === false)
                            if (!containerDisabled) {
                                container.runContainer = val
                            }
                        }
                    })
                })
            },
            'pipeline.stages' (val, old) {
                if (val) {
                    val.forEach(stage => {
                        const stageDisabled = stage.stageControlOption && stage.stageControlOption.enable === false
                        if (!stage.hasOwnProperty('runStage')) {
                            Vue.set(stage, 'runStage', !stageDisabled)
                        }
                        stage.containers.forEach(container => {
                            if (container['@type'] !== 'trigger') {
                                const containerDisabled = container.jobControlOption && container.jobControlOption.enable === false
                                if (!container.hasOwnProperty('runContainer')) {
                                    Vue.set(container, 'runContainer', !containerDisabled)
                                }

                                container.elements.forEach(element => {
                                    const isSkipEle = (element.additionalOptions && element.additionalOptions.enable === false) || containerDisabled
                                    if (!element.hasOwnProperty('canElementSkip')) {
                                        Vue.set(element, 'canElementSkip', !isSkipEle)
                                    }
                                })
                            }
                        })
                    })
                }
            }
        },
        async created () {
            await this.init()
            bus.$on('start-execute', this.getExecuteParams)
        },
        beforeDestroy () {
            this.togglePropertyPanel({
                isShow: false
            })
            this.$store.commit('pipelines/updateCurAtomPrams', null)
            this.setPipeline()
            this.setPipelineEditing(false)

            bus.$off('start-execute', this.getExecuteParams)
        },
        methods: {
            ...mapActions('atom', [
                'togglePropertyPanel',
                'requestPipeline',
                'setPipeline',
                'setPipelineEditing'
            ]),
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
                        this.paramList = this.curPipelineInfo.properties.filter(p => p.required && !allVersionKeyList.includes(p.id))
                        this.versionParamList = this.curPipelineInfo.properties.filter(p => allVersionKeyList.includes(p.id))
                        this.paramValues = getParamsValuesMap(this.paramList)
                        this.versionParamValues = getParamsValuesMap(this.versionParamList)
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
                const allElements = this.getAllElements(this.pipeline.stages.slice(1))
                const skipAtoms = allElements.filter(element => !element.canElementSkip).map(element => `devops_container_condition_skip_atoms_${element.id}`)
                const versionValid = this.$refs.versionForm ? await this.$refs.versionForm.$validator.validateAll() : true

                let valid = true
                if (this.$refs.paramsForm) {
                    valid = await this.$refs.paramsForm.$validator.validateAll()
                    this.$refs.paramsForm.submitForm()
                }
                if (valid && versionValid) {
                    const { paramValues, versionParamValues, buildNo } = this
                    const newParams = Object.assign({}, paramValues, versionParamValues)
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
                else this.isDropdownShowParam = !this.isDropdownShowParam
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
                const url = `${WEB_URL_PIRFIX}/pipeline/${this.projectId}/${this.pipelineId}/edit#manualTrigger`
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
            overflow: auto;
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
                margin-bottom: 30px;
                .bk-form {
                    display: flex;
                    flex-wrap: wrap;
                    justify-content: space-between;
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
                    margin-top: 20px;
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
                    margin: 20px 20px 0 0;
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
