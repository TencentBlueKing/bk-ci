<template>
    <div class="create-pipeline-wrapper" v-bkloading="{ isLoading: isSaving, title: $t('editPage.saving') }">
        <header v-if="showHeader" class="create-pipeline-header">
            <div>
                <slot name="pipeline-name"><span style="cursor: default" :title="pipeline.name">{{ pipeline.name }}</span></slot>
            </div>
            <div class="pipeline-bar">
                <slot name="pipeline-bar"></slot>
            </div>
        </header>
        <div v-if="pipeline" class="scroll-container">
            <div class="scroll-wraper">
                <bk-pipeline
                    v-if="pipeline"

                    :pipeline="pipeline"
                    :user-name="userName"
                    :editable="pipelineEditable"
                    :can-skip-element="canSkipElement"
                    :is-preview="isPreview"
                    :match-rules="curMatchRules"
                    @change="handlePipelineChange"
                    @add-atom="addAtom"
                    @click="handlePipelineClick"
                    @add-stage="handleAddStage"
                    @stage-check="handleStageCheck"
                >
                </bk-pipeline>
            </div>
        </div>

        <bk-dialog v-model="isStageShow"
            :width="stageTypeDialogWidth"
            :title="$t('editPage.selectJob')"
            :show-footer="false"
            :esc-close="true"
            :mask-close="true"
        >
            <section class="bk-form bk-form-vertical bk-form-wrapper">
                <ul class="stage-type-list">
                    <li v-for="os in osList" :key="os.value" @click="insert(os.value)" :class="os.className">
                        <i :class="`devops-icon icon-${os.value.toLowerCase()} stage-type-icon`" />
                        <span class="stage-label">{{ os.label }}</span>
                    </li>
                </ul>
            </section>
        </bk-dialog>
        <template v-if="container">
            <atom-selector :container="container" :element="element" v-bind="editingElementPos" />
        </template>
        <template v-if="editingElementPos">
            <template v-if="typeof editingElementPos.elementIndex !== 'undefined'">
                <atom-property-panel
                    :element-index="editingElementPos.elementIndex"
                    :container-index="editingElementPos.containerIndex"
                    :stage-index="editingElementPos.stageIndex"
                    :editable="pipelineEditable"
                    :stages="pipeline.stages"
                    :is-instance-template="pipeline.instanceFromTemplate"
                />
            </template>
            <template v-else-if="typeof editingElementPos.containerIndex !== 'undefined'">
                <container-property-panel
                    :title="panelTitle"
                    :container-index="editingElementPos.containerIndex"
                    :stage-index="editingElementPos.stageIndex"
                    :stages="pipeline.stages"
                    :editable="pipelineEditable"
                />
            </template>
            <template v-else-if="typeof editingElementPos.stageIndex !== 'undefined' && showStageReviewPanel.isShow">
                <stage-review-panel
                    :stage="stage"
                    :stage-index="editingElementPos.stageIndex"
                    :editable="pipelineEditable"
                />
            </template>
            <template v-else-if="typeof editingElementPos.stageIndex !== 'undefined'">
                <stage-property-panel
                    :stage="stage"
                    :stage-index="editingElementPos.stageIndex"
                    :editable="pipelineEditable"
                />
            </template>
        </template>
    </div>
</template>

<script>
    import { mapState, mapActions, mapGetters } from 'vuex'
    import AtomPropertyPanel from './AtomPropertyPanel'
    import ContainerPropertyPanel from './ContainerPropertyPanel'
    import StagePropertyPanel from './StagePropertyPanel'
    import StageReviewPanel from './StageReviewPanel'
    import AtomSelector from './AtomSelector'
    import { isObject } from '../utils/util'

    export default {
        components: {
            StagePropertyPanel,
            AtomPropertyPanel,
            ContainerPropertyPanel,
            StageReviewPanel,
            AtomSelector
        },
        props: {
            isSaving: {
                type: Boolean,
                default: false
            },
            pipeline: {
                type: Object,
                required: true
            },
            templateType: {
                type: String,
                default: ''
            },
            showHeader: {
                type: Boolean,
                default: true
            },
            editable: {
                type: Boolean,
                default: true
            },
            canSkipElement: {
                type: Boolean,
                default: false
            },
            isPreview: {
                type: Boolean,
                default: false
            }
        },
        computed: {
            ...mapState('common', [
                'ruleList',
                'templateRuleList'
            ]),
            ...mapGetters('atom', [
                'osList',
                'getElement',
                'getContainers',
                'getStage'
            ]),
            ...mapState('atom', [
                'isPropertyPanelVisible',
                'editingElementPos',
                'isStagePopupShow',
                'insertStageIndex',
                'insertStageIsFinally',
                'isAddParallelStage',
                'showStageReviewPanel'
            ]),
            userName () {
                return this.$userInfo && this.$userInfo.username ? this.$userInfo.username : ''
            },
            routeParams () {
                return this.$route.params
            },
            isStageShow: {
                get () {
                    return this.isStagePopupShow
                },
                set (value) {
                    this.setInsertStageState({
                        isStagePopupShow: value
                    })
                }
            },
            stageTypeDialogWidth () {
                return Array.isArray(this.osList) ? this.osList.length * 130 + 208 : 480
            },
            stage () {
                if (isObject(this.editingElementPos)) {
                    const { stageIndex } = this.editingElementPos
                    const stage = this.getStageByIndex(stageIndex)
                    return stage
                }
                return null
            },
            container () {
                if (isObject(this.editingElementPos)) {
                    const { stageIndex, containerIndex } = this.editingElementPos
                    const stage = this.getStageByIndex(stageIndex)
                    const containers = this.getContainers(stage)
                    return containers[containerIndex]
                }
                return null
            },
            element () {
                if (this.editingElementPos && typeof this.editingElementPos.elementIndex !== 'undefined') {
                    return this.container.elements[this.editingElementPos.elementIndex]
                }
                return null
            },
            panelTitle () {
                const { stageIndex, containerIndex, elementIndex } = this.editingElementPos
                if (typeof elementIndex !== 'undefined') {
                    return ''
                }
                return typeof containerIndex !== 'undefined'
                    ? this.container.name + '： ' + (stageIndex + 1) + '-' + (containerIndex + 1)
                    : this.$t('propertyBar')
            },
            containerType () {
                const { stageIndex, containerIndex } = this.editingElementPos
                const stage = this.getStageByIndex(stageIndex)
                const containers = this.getContainers(stage)
                return containers[containerIndex]['@type']
            },
            pipelineEditable () {
                return this.editable
                    && !this.pipeline.instanceFromTemplate
                    && this.templateType !== 'CONSTRAINT'
                    && !this.isPreview
            },
            isInstanceEditable () {
                return !this.pipelineEditable && this.pipeline.instanceFromTemplate
            },
            curMatchRules () {
                return this.$route.path.indexOf('template') > 0
                    ? [
                        ...this.templateRuleList,
                        ...(this.isInstanceEditable ? this.ruleList : [])
                    ]
                    : [...this.ruleList]
            }
        },
        beforeDestroy () {
            this.toggleAtomSelectorPopup(false)
            this.togglePropertyPanel({
                isShow: false
            })
        },
        methods: {
            ...mapActions('atom', [
                'toggleAtomSelectorPopup',
                'togglePropertyPanel',
                'setInsertStageState',
                'addAtom',
                'addContainer',
                'addStage',
                'setPipeline',
                'setPipelineEditing',
                'toggleStageReviewPanel'
            ]),
            handleAddStage ({ stageIndex, isParallel, isFinally }) {
                this.setInsertStageState({
                    isStagePopupShow: true,
                    isAddParallelStage: isParallel,
                    insertStageIsFinally: isFinally,
                    insertStageIndex: stageIndex
                })
            },
            handleStageCheck ({ type, stageIndex }) {
                this.toggleStageReviewPanel({
                    showStageReviewPanel: {
                        isShow: true,
                        type
                    },
                    editingElementPos: {
                        stageIndex
                    }
                })
            },
            handlePipelineClick (args) {
                this.togglePropertyPanel({
                    isShow: true,
                    editingElementPos: args
                })
            },

            getStageByIndex (stageIndex) {
                const { getStage, pipeline } = this
                return getStage(pipeline.stages, stageIndex)
            },
            handlePipelineChange (pipeline) {
                this.setPipeline(pipeline)
                this.setPipelineEditing(true)
            },
            resetInsertStageState () {
                this.setInsertStageState({
                    isStagePopupShow: false,
                    isAddParallelStage: false,
                    insertStageIndex: null,
                    insertStageIsFinally: false
                })
            },
            insertContainer (type, insertStageIndex) {
                const stage = this.getStageByIndex(insertStageIndex)
                const containers = this.getContainers(stage)
                this.addContainer({
                    containers,
                    type
                })
                this.resetInsertStageState()
            },
            insert (type) {
                if (!this.isStagePopupShow) return
                const {
                    pipeline,
                    insertStageIndex,
                    isAddParallelStage,
                    insertStageIsFinally
                } = this

                if (!isAddParallelStage) {
                    this.addStage({
                        stages: pipeline.stages,
                        insertStageIndex,
                        insertStageIsFinally
                    })
                    if (insertStageIsFinally) {
                        const element = document.getElementsByClassName('bk-tab-section')[0]
                        element && (element.scrollLeft = element.scrollWidth + 300)
                    }
                }
                this.insertContainer(type, insertStageIndex)
            }
        }
    }
</script>

<style lang='scss'>
    @import '../scss/conf.scss';
    .create-pipeline-header {
        display: flex;
        align-items: center;
        height: 60px;
        border-bottom: 1px solid $borderWeightColor;
        box-shadow: 0px 2px 5px 0px rgba(0, 0, 0, 0.03);
        padding: 0 20px 0 30px;
        > p {
            flex: 1;
            > span {
                display: inline-block;
            }
        }
    }
    .create-pipeline-wrapper {
        display: flex;
        height: 100%;
        min-width: 100%;
        flex-direction: column;
        .edit-process-title {
            .bk-dialog-body {
                padding-right: 50px !important;
            }
            .bk-label {
                width: 100px;
            }
            .bk-form-content {
                margin-left: 100px;
            }
        }
    }
    .pipeline-bar {
        display: flex;
        flex: 1;
        align-items: center;
        height: 100%;
        justify-content: flex-end;
    }

    .scroll-container {
        position: relative;
        flex: 1;
        overflow: auto;
        .scroll-wraper {
            padding: 20px 0 40px 30px;
            min-height: 100%;
        }
        &:before {
            position: absolute;
            top: 44px;
            content: '';
            height: 0;
            left: 30px;
            min-width: calc( 100% - 30px);
            border-top: 2px dashed #c3cdd7;
       }
    }

    .stage-type-list {
        display: flex;
        margin: 0 80px;
        > li {
            text-align: center;
            cursor: pointer;
            margin: 0 15px 50px 15px;
            font-size: 16px;
            .stage-type-icon {
                display: block;
                width: 100px;
                height: 100px;
                color: #c3cdd7;
                line-height: 100px;
                font-size: 66px;
                border: 1px solid $borderWeightColor;
                border-radius: 8px;
                margin: 0 0 20px 0;
                font-style: normal;
            }
            .stage-label {
                display: inline-block;
                vertical-align: text-bottom;
                height: 24px;
                color: #333c48;
            }
            &.normal-stage {
                font-size: 16px;
                font-style: normal;
                .stage-type-icon {
                    font-size: 28px;
                }
                 .stage-label {
                     margin-bottom: -8px;
                 }
            }
            &:hover {
                .stage-type-icon {
                    color: white;
                    background-image: linear-gradient( 0, rgb(195,205,215) 0%, rgb(115,121,135) 100%);
                }
            }
        }
        .disabled {
            pointer-events: none;
            opacity: 0.5
        }
    }
    .bk-tooltip-inner {
        max-width: 450px;
    }

</style>
