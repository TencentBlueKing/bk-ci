<template>
    <div class="create-pipeline-wrapper" v-bkloading="{ isLoading: isSaving, title: '正在保存' }">
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
                <stages :stages="pipeline.stages" :editable="!pipeline.instanceFromTemplate && templateType !== &quot;CONSTRAINT&quot;"></stages>
            </div>
        </div>

        <bk-dialog v-model="isStageShow"
            width="620"
            ext-cls="pipeline-type-container"
            title="请选择Job类型"
            :show-footer="false"
            :esc-close="true"
            :mask-close="true"
        >
            <section class="bk-form bk-form-vertical bk-form-wrapper">
                <ul class="stage-type-list">
                    <li v-for="os in osList" :key="os.value" @click="insert(os.value)" :class="os.className">
                        <i :class="`bk-icon icon-${os.value.toLowerCase()} stage-type-icon`" />
                        <span class="stage-label">{{ os.label }}</span>
                    </li>
                </ul>
            </section>
        </bk-dialog>

        <atom-selector v-if="container" :container="container" :element="element" v-bind="editingElementPos" :fresh-atom-list="freshAtomList" />

        <bk-sideslider v-if="editingElementPos" :title="panelTitle" :class="{ 'sodaci-property-panel': true, 'hide-title': !panelTitle }" width="640" :is-show.sync="isPropertyPanelShow" :quick-close="true">
            <template slot="content">
                <atom-property-panel
                    v-if="typeof editingElementPos.elementIndex !== &quot;undefined&quot;"
                    :element-index="editingElementPos.elementIndex"
                    :container-index="editingElementPos.containerIndex"
                    :stage-index="editingElementPos.stageIndex"
                    :editable="!pipeline.instanceFromTemplate && templateType !== &quot;CONSTRAINT&quot;"
                    :stages="pipeline.stages"
                />
                <container-property-panel
                    v-else-if="typeof editingElementPos.containerIndex !== &quot;undefined&quot;"
                    :container-index="editingElementPos.containerIndex"
                    :stage-index="editingElementPos.stageIndex"
                    :stages="pipeline.stages"
                    :editable="!pipeline.instanceFromTemplate && templateType !== &quot;CONSTRAINT&quot;"
                />
            </template>
        </bk-sideslider>
    </div>
</template>

<script>
    import { mapState, mapActions, mapGetters } from 'vuex'
    import Stages from './Stages'
    import AtomPropertyPanel from './AtomPropertyPanel'
    import ContainerPropertyPanel from './ContainerPropertyPanel'
    import AtomSelector from './AtomSelector'
    import { isObject } from '../utils/util'

    export default {
        components: {
            Stages,
            AtomPropertyPanel,
            ContainerPropertyPanel,
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
            }
        },
        computed: {
            ...mapGetters('atom', [
                'osList',
                'getElement',
                'getContainers',
                'getStage'
            ]),
            ...mapState('atom', [
                'fetchingAtomList',
                'isPropertyPanelVisible',
                'editingElementPos',
                'isStagePopupShow',
                'insertStageIndex',
                'isAddParallelContainer'
            ]),
            routeParams () {
                return this.$route.params
            },
            isStageShow: {
                get () {
                    return this.isStagePopupShow
                },
                set (value) {
                    this.toggleStageSelectPopup({
                        isStagePopupShow: value
                    })
                }
            },
            isPropertyPanelShow: {
                get () {
                    return this.isPropertyPanelVisible
                },
                set (value) {
                    this.toggleAtomSelectorPopup(value)
                    this.togglePropertyPanel({
                        isShow: value
                    })
                }
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
                    : '属性栏'
            },
            containerType () {
                const { stageIndex, containerIndex } = this.editingElementPos
                const stage = this.getStageByIndex(stageIndex)
                const containers = this.getContainers(stage)
                return containers[containerIndex]['@type']
            }
        },
        beforeDestroy () {
            this.isPropertyPanelShow = false
        },
        methods: {
            ...mapActions('atom', [
                'toggleAtomSelectorPopup',
                'toggleStageSelectPopup',
                'togglePropertyPanel',
                'addStage',
                'addContainer',
                'fetchAtoms',
                'clearStoreAtom',
                'setStoreSearch',
                'addStoreAtom'
            ]),
            freshAtomList (searchKey) {
                if (this.fetchingAtomList) return
                const projectCode = this.$route.params.projectId
                this.fetchAtoms({
                    projectCode
                })
                this.clearStoreAtom()
                this.setStoreSearch(searchKey)
                this.addStoreAtom()
            },
            getStageByIndex (stageIndex) {
                const { getStage, pipeline } = this
                return getStage(pipeline.stages, stageIndex)
            },
            insertContainer (type, insertStageIndex) {
                const { getContainers, addContainer, toggleStageSelectPopup, getStageByIndex } = this
                const stage = getStageByIndex(insertStageIndex)
                const containers = getContainers(stage)
                addContainer({
                    containers,
                    type
                })
                toggleStageSelectPopup({
                    isStagePopupShow: false
                })
            },
            insert (type) {
                const { pipeline, insertStageIndex, isAddParallelContainer } = this
                if (!isAddParallelContainer) {
                    this.addStage({
                        stages: pipeline.stages,
                        insertStageIndex
                    })
                }
                this.insertContainer(type, insertStageIndex)
            }
        }
    }
</script>

<style lang='scss'>
    @import '../scss/conf.scss';
    .pipeline-type-container {
        .bk-dialog-header {
            margin-top: 30px;
        }
        .bk-dialog-tool {
            display: none;
        }
    }
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
        overflow: auto;
        flex: 1;
        .scroll-wraper {
            padding: 40px 0 40px 30px;
            min-height: 100%;
            overflow: auto;
        }
        &:before {
            position: absolute;
            top: 61px;
            content: '';
            height: 0;
            left: 30px;
            min-width: calc( 100% - 30px);
            border-top: 2px dashed #c3cdd7;
       }
    }
    .sodaci-property-panel {
        &.hide-title {
            .bk-sideslider-title {
                display: none;
            }
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
