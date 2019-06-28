<template>
    <div class="pipeline-execute-preview" v-bkloading="{ isLoading }">
        <div class="scroll-container">
            <div class="execute-previe-content" v-if="curPipelineInfo.canElementSkip">
                <div class="global-params" v-if="paramList.length">
                    <p class="item-title">全局参数：<i :class="['bk-icon icon-angle-down', { 'icon-flip': isDropdownShow }]" @click="toggleIcon"></i></p>
                    <pipeline-params-form ref="paramsForm" v-if="isDropdownShow" :param-values="paramValues" :handle-param-change="handleParamChange" :params="paramList"></pipeline-params-form>
                </div>
                <div class="execute-detail-option" v-if="pipeline">
                    <p class="item-title">选择可执行的插件：
                        <span class="item-title-tips">(若你的流水线已经调试成功，可在<span @click.stop="editTrigger" class="text-link item-title-tips-link">手动触发</span>插件中关闭该选项)</span>
                        <bk-checkbox style="margin-left: 15px" v-model="checkTotal" @click.stop>全选/全不选</bk-checkbox>
                    </p>
                    <div class="pipeline-detail">
                        <stages :stages="pipeline.stages" :editable="false" :is-preview="true"></stages>
                    </div>
                </div>
            </div>
        </div>

        <side-slider v-if="editingElementPos" :title="panelTitle" class="sodaci-property-panel" width="640" :is-show.sync="isPropertyPanelShow" :quick-close="true">
            <template slot="content">
                <atom-property-panel
                    v-if="typeof editingElementPos.elementIndex !== &quot;undefined&quot;"
                    :element-index="editingElementPos.elementIndex"
                    :container-index="editingElementPos.containerIndex"
                    :stage-index="editingElementPos.stageIndex"
                    :editable="false"
                    :stages="pipeline.stages"
                />
                <container-property-panel
                    v-else-if="typeof editingElementPos.containerIndex !== &quot;undefined&quot;"
                    :container-index="editingElementPos.containerIndex"
                    :stage-index="editingElementPos.stageIndex"
                    :stages="pipeline.stages"
                    :editable="false"
                />
            </template>
        </side-slider>
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
    import pipelineOperateMixin from '@/mixins/pipeline-operate-mixin'

    export default {
        components: {
            Stages,
            AtomPropertyPanel,
            ContainerPropertyPanel,
            PipelineParamsForm
        },
        mixins: [pipelineOperateMixin],
        data () {
            return {
                isLoading: false,
                isDropdownShow: true,
                paramList: [],
                paramValues: {},
                curPipelineInfo: {},
                execDisabled: false,
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
            isPropertyPanelShow: {
                get () {
                    return this.isPropertyPanelVisible
                },
                set (value) {
                    this.togglePropertyPanel({
                        isShow: value
                    })
                }
            },
            panelTitle () {
                const { stageIndex, containerIndex, elementIndex } = this.editingElementPos
                if (typeof elementIndex !== 'undefined') {
                    return ''
                }
                const stage = this.getStageByIndex(stageIndex)
                const containers = this.getContainers(stage)
                return typeof containerIndex !== 'undefined'
                    ? containers[containerIndex].name + '： ' + (stageIndex + 1) + '-' + (containerIndex + 1)
                    : '属性栏'
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
            this.isPropertyPanelShow = false
            bus.$off('start-execute', this.getExecuteParams)
        },
        methods: {
            ...mapActions('atom', [
                'togglePropertyPanel',
                'requestPipeline'
            ]),
            getStageByIndex (stageIndex) {
                const { getStage, pipeline } = this
                return getStage(pipeline.stages, stageIndex)
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
                        this.paramList = this.curPipelineInfo.properties.filter(p => p.required)
                        if (this.curPipelineInfo.canElementSkip) {
                            this.paramValues = this.paramList.reduce((values, param) => {
                                values[param.id] = param.defaultValue
                                return values
                            }, {})

                            this.requestPipeline(this.$route.params)
                        } else {
                            throw new Error('当前流水线手动触发执行不可跳过插件')
                        }
                    } else {
                        throw new Error('当前流水线不包含手动触发插件')
                    }
                } catch (err) {
                    if (err.code === 403) { // 没有权限执行
                        this.$showAskPermissionDialog({
                            noPermissionList: [{
                                resource: '流水线',
                                option: '执行'
                            }],
                            applyPermissionUrl: `${PERM_URL_PIRFIX}/backend/api/perm/apply/subsystem/?client_id=pipeline&project_code=${this.projectId}&service_code=pipeline&role_executor=pipeline:${this.pipelineId}`
                        })
                    } else {
                        this.$showTips({
                            message: err.message || err,
                            theme: 'error'
                        })
                    }

                    if (err.message === '当前流水线手动触发执行不可跳过插件' || err.message === '当前流水线不包含手动触发插件') {
                        this.$router.push({
                            name: 'pipelinesEdit',
                            params: {
                                projectId: this.projectId,
                                pipelineId: this.pipelineId
                            }
                        })
                    }
                } finally {
                    this.isLoading = false
                }
            },
            async getExecuteParams () {
                if (this.execDisabled) return
                const allElements = this.getAllElements(this.pipeline.stages.slice(1))
                const skipAtoms = allElements.filter(element => !element.canElementSkip).map(element => `devops_container_condition_skip_atoms_${element.id}`)

                let valid = true
                if (this.$refs.paramsForm) {
                    valid = await this.$refs.paramsForm.$validator.validateAll()
                    this.$refs.paramsForm.submitForm()
                }

                if (valid) {
                    this.execDisabled = true
                    const { paramValues } = this

                    this.executePipeline(skipAtoms.reduce((res, skip) => {
                        res[skip] = true
                        return res
                    }, paramValues), true)
                }
            },
            /**
             * 设置权限弹窗的参数
             */
            setPermissionConfig (resource, option) {
                this.$showAskPermissionDialog({
                    noPermissionList: [{
                        resource,
                        option
                    }],
                    applyPermissionUrl: `${PERM_URL_PIRFIX}/backend/api/perm/apply/subsystem/?client_id=pipeline&project_code=${this.projectId}&service_code=pipeline&${option === '执行' ? 'role_executor' : 'role_manager'}=pipeline:${this.pipelineId}`
                })
            },
            toggleIcon () {
                this.isDropdownShow = !this.isDropdownShow
            },
            handleParamChange (name, value) {
                this.paramValues[name] = value
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
