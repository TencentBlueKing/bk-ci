<template>
    <div class="pipeline-execute-preview" v-bkloading="{ isLoading }">
        <div class="scroll-container">
            <div class="execute-previe-content">
                <div class="version-option" v-if="isVisibleVersion">
                    <p class="item-title">{{ $t('preview.introVersion') }}：<i :class="['bk-icon icon-angle-down', { 'icon-flip': isDropdownShowVersion }]" @click="toggleIcon('version')"></i></p>
                    <pipeline-versions-form ref="versionForm"
                        v-if="isDropdownShowVersion"
                        :build-no="buildNo"
                        :version-param-values="versionParamValues"
                        :handle-version-change="handleVersionChange"
                        :handle-build-no-change="handleBuildNoChange"
                    ></pipeline-versions-form>
                </div>
                <div class="global-params" v-if="paramList.length">
                    <p class="item-title">{{ $t('template.pipelineVar') }}：<i :class="['bk-icon icon-angle-down', { 'icon-flip': isDropdownShowParam }]" @click="toggleIcon('params')"></i></p>
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
                        <stages :stages="pipeline.stages" :editable="false" :is-preview="true" :can-skip-element="curPipelineInfo.canElementSkip"></stages>
                    </div>
                </div>
            </div>
        </div>
        <template v-if="editingElementPos">
            <template v-if="typeof editingElementPos.elementIndex !== &quot;undefined&quot;">
                <atom-property-panel
                    :element-index="editingElementPos.elementIndex"
                    :container-index="editingElementPos.containerIndex"
                    :stage-index="editingElementPos.stageIndex"
                    :editable="false"
                    :stages="pipeline.stages"
                />
            </template>
            <template v-else-if="typeof editingElementPos.containerIndex !== &quot;undefined&quot;">
                <container-property-panel
                    :title="panelTitle"
                    :container-index="editingElementPos.containerIndex"
                    :stage-index="editingElementPos.stageIndex"
                    :stages="pipeline.stages"
                    :editable="false"
                />
            </template>
        </template>
    </div>
</template>

<script>
    import { mapState, mapGetters, mapActions } from 'vuex'
    import Stages from '@/components/Stages'
    import AtomPropertyPanel from '@/components/AtomPropertyPanel'
    import ContainerPropertyPanel from '@/components/ContainerPropertyPanel'
    import Vue from 'vue'
    import { bus } from '@/utils/bus'
    import PipelineParamsForm from '@/components/pipelineParamsForm.vue'
    import PipelineVersionsForm from '@/components/PipelineVersionsForm.vue'
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'

    export default {
        components: {
            Stages,
            AtomPropertyPanel,
            ContainerPropertyPanel,
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
            ...mapGetters('soda', {
                curParamList: 'pipelines/getCurAtomPrams'
            }),
            ...mapGetters('atom', [
                'getContainers',
                'getStage',
                'getAllElements'
            ]),
            ...mapState('atom', [
                'editingElementPos',
                'isPropertyPanelVisible'
            ]),
            panelTitle () {
                const { stageIndex, containerIndex, elementIndex } = this.editingElementPos
                if (typeof elementIndex !== 'undefined') {
                    return ''
                }
                const stage = this.getStageByIndex(stageIndex)
                const containers = this.getContainers(stage)
                return typeof containerIndex !== 'undefined'
                    ? containers[containerIndex].name + '： ' + (stageIndex + 1) + '-' + (containerIndex + 1)
                    : this.$t('propertyBar')
            }
        },
        watch: {
            pipelineId () {
                this.$router.push({
                    name: 'pipelinesEdit',
                    params: {
                        projectId: this.projectId,
                        pipelineId: this.pipelineId
                    }
                })
            },
            checkTotal (val) {
                this.pipeline.stages.forEach(stage => {
                    stage.containers.forEach(container => {
                        if (container['@type'] !== 'trigger') {
                            const containerDisabled = container.jobControlOption && container.jobControlOption.enable === false
                            if (!containerDisabled) {
                                container.runContainer = val
                            }
                        }
                    })
                })
            },
            'pipeline.stages' (val) {
                if (val) {
                    val.forEach(stage => {
                        stage.containers.forEach(container => {
                            if (container['@type'] !== 'trigger') {
                                const containerDisabled = container.jobControlOption && container.jobControlOption.enable === false
                                if (!container.hasOwnProperty('runContainer')) {
                                    Vue.set(container, 'runContainer', true)
                                } else {
                                    container.runContainer = true
                                }
                                if (containerDisabled) {
                                    container.runContainer = false
                                }
                                container.elements.forEach(element => {
                                    if (!element.hasOwnProperty('canElementSkip')) {
                                        Vue.set(element, 'canElementSkip', true)
                                    } else {
                                        element.canElementSkip = true
                                    }
                                    if ((element.additionalOptions && element.additionalOptions.enable === false) || containerDisabled) {
                                        element.canElementSkip = false
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
            getStageByIndex (stageIndex) {
                const { getStage, pipeline } = this
                return getStage(pipeline.stages, stageIndex)
            },
            getParamsValue (params) {
                return params.reduce((values, param) => {
                    values[param.id] = param.defaultValue
                    return values
                }, {})
            },
            async init () {
                this.isLoading = true

                try {
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
                        this.paramList = this.curPipelineInfo.properties.filter(p => p.required)
                        this.versionParamList = this.curPipelineInfo.properties.filter(p => !p.required)
                        this.paramValues = this.getParamsValue(this.paramList)
                        this.versionParamValues = this.getParamsValue(this.versionParamList)
                        this.requestPipeline(this.$route.params)
                    } else {
                        throw new Error(this.$t('newlist.withoutManualAtom'))
                    }
                } catch (err) {
                    if (err.code === 403) { // 没有权限执行
                        this.$showAskPermissionDialog({
                            noPermissionList: [{
                                resource: this.$t('pipeline'),
                                option: this.$t('exec')
                            }],
                            applyPermissionUrl: `${PERM_URL_PIRFIX}/backend/api/perm/apply/subsystem/?client_id=pipeline&project_code=${this.projectId}&service_code=pipeline&role_executor=pipeline:${this.pipelineId}`
                        })
                    } else {
                        this.$showTips({
                            message: err.message || err,
                            theme: 'error'
                        })
                    }
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
    @import './../../scss/conf';

    .pipeline-execute-preview {
        height: 100%;
        .scroll-container {
            height: 100%;
            overflow: auto;
            &:before {
                display: none;
            }
        }
        .execute-previe-content {
            padding: 20px 44px 30px;
            height: 100%;
            overflow: auto;
            .item-title {
                line-height: 36px;
                border-bottom: 1px solid $borderWeightColor;
                .bk-icon {
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
                .bk-form-content {
                    position: relative;
                }
                .bk-form-help {
                    position: absolute;
                    top: 36px;
                    margin: 0;
                    display: inline-block;
                    width: 100%;
                    white-space: nowrap;
                    text-overflow: ellipsis;
                    overflow: hidden;
                }
            }
            .bk-form-item {
                float: left;
                margin-top: 20px;
                width: 46%;
                height: 70px;
                &:nth-child(2n) {
                    margin-left: 30px;
                }
            }
            .bk-label {
                width: 100%;
                text-align: left;
                white-space: nowrap;
                text-overflow: ellipsis;
                overflow: hidden;
            }
            .bk-form-content {
                float: left;
                margin-left: 0;
                width: 100%;
            }
            .pipeline-detail {
                padding: 20px 10px;
            }
            .version-option {
                margin-bottom: 30px;
                .bk-form-item {
                    width: 200px;
                    margin-left: 0;
                    margin-right: 20px;
                }
                .flex-colspan-2 {
                    width: 400px;
                }
            }
            .flex-colspan-2 {
                .bk-form-content {
                    margin-top: 42px;
                }
            }
        }
        .sodaci-property-panel {
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
