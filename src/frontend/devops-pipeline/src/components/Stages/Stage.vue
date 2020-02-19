<template>
    <div :class="[{ 'pipeline-drag': editable && !isTriggerStage, 'show-stage-area': editable && !isTriggerStage }, 'pipeline-stage']" ref="stageRef">
        <bk-button v-if="editable && !isTriggerStage" class="pipeline-stage-entry" @click="showStagePanel">{{stage.name}}</bk-button>
        <draggable v-model="compitedContainer" v-bind="dragOptions" :move="checkMove" tag="ul" class="soda-process-stage">
            <stage-container v-for="(container, index) in compitedContainer"
                :key="`${container.id}-${index}`"
                :stage-index="stageIndex"
                :container-index="index"
                :stage-length="stageLength"
                :editable="editable"
                :is-preview="isPreview"
                :can-skip-element="canSkipElement"
                :container-length="compitedContainer.length"
                :container="container">
            </stage-container>
        </draggable>
        <template v-if="editable || isPreStageDone">
            <span v-bk-clickoutside="toggleAddMenu" v-if="!isFirstStage" class="add-menu" @click.stop="handleClick">
                <i :class="{ [iconCls]: true, 'active': isAddMenuShow }" />
                <template v-if="isAddMenuShow">
                    <span class="insert-tip direction line-add" @click.stop="showStageSelectPopup(false)">
                        <i class="tip-icon" />
                        <span>
                            {{ $t('editPage.insert') }}
                        </span>
                    </span>
                    <span class="cruve-line left"></span>
                    <div @click.stop="showStageSelectPopup(true)" class="insert-tip parallel-add">
                        <i class="tip-icon" />
                        <span>
                            {{ $t('editPage.append') }}
                        </span>
                    </div>
                </template>
            </span>
            <span v-if="isLastStage && editable" @click="appendStage" class="append-stage pointer">
                <i class="add-plus-icon" />
            </span>
        </template>
    </div>
</template>

<script>
    import { mapActions, mapState, mapGetters } from 'vuex'
    import StageContainer from './StageContainer'
    import { getOuterHeight } from '@/utils/util'
    export default {
        components: {
            StageContainer
        },
        props: {
            containers: {
                type: Array,
                default: []
            },
            stage: Object,
            stageIndex: Number,
            stageLength: Number,
            editable: {
                type: Boolean,
                default: true
            },
            isPreview: {
                type: Boolean,
                default: false
            },
            preStatus: {
                type: String
            },
            canSkipElement: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                isAddMenuShow: false
            }
        },
        computed: {
            ...mapState('atom', [
                'insertStageIndex',
                'pipeline'
            ]),
            ...mapGetters('atom', [
                'isTriggerContainer'
            ]),
            isFirstStage () {
                return this.stageIndex === 0
            },
            isLastStage () {
                return this.stageIndex === this.stageLength - 1
            },
            isTriggerStage () {
                return this.checkIsTriggerStage(this.stage)
            },
            compitedContainer: {
                get () {
                    return this.containers
                },
                set (containers) {
                    let data = []
                    containers.forEach((container) => {
                        if (container.containers) data = [...data, ...container.containers]
                        else data.push(container)
                    })
                    this.setPipelineContainer({ oldContainers: this.containers, containers: data })
                    this.setPipelineEditing(true)
                }
            },
            dragOptions () {
                return {
                    group: 'pipeline-job',
                    ghostClass: 'sortable-ghost-atom',
                    chosenClass: 'sortable-chosen-atom',
                    animation: 130,
                    disabled: !this.editable
                }
            },
            isPreStageDone () {
                try {
                    return this.preStatus === 'SUCCEED'
                } catch (error) {
                    return false
                }
            },
            iconCls () {
                switch (true) {
                    case this.isPreStageDone:
                        return 'play-icon'
                    case !this.isAddMenuShow:
                        return 'add-plus-icon'
                    case this.isAddMenuShow:
                        return 'minus-icon'
                    default:
                        return 'add-plus-icon'
                }
            }
        },
        mounted () {
            this.updateHeight()
        },
        updated () {
            this.updateHeight()
        },
        methods: {
            ...mapActions('atom', [
                'setInertStageIndex',
                'togglePropertyPanel',
                'toggleStageSelectPopup',
                'setPipelineContainer',
                'setPipelineEditing'
            ]),
            checkIsTriggerStage (stage) {
                try {
                    return this.isTriggerContainer(stage.containers[0])
                } catch (e) {
                    return false
                }
            },

            showStagePanel () {
                const { stageIndex } = this
                this.togglePropertyPanel({
                    isShow: true,
                    editingElementPos: {
                        stageIndex
                    }
                })
            },

            checkMove (event) {
                const dragContext = event.draggedContext || {}
                const element = dragContext.element || {}
                const isTrigger = element['@type'] === 'trigger'

                const relatedContext = event.relatedContext || {}
                const relatedelement = relatedContext.element || {}
                const isRelatedTrigger = relatedelement['@type'] === 'trigger'
                const isTriggerStage = this.checkIsTriggerStage(relatedelement)

                return !isTrigger && !isRelatedTrigger && !isTriggerStage
            },

            appendStage () {
                const { stageIndex, setInertStageIndex, showStageSelectPopup } = this
                setInertStageIndex({
                    insertStageIndex: stageIndex + 1
                })
                showStageSelectPopup(false)
            },
            showStageSelectPopup (isParallel) {
                this.toggleStageSelectPopup({
                    isStagePopupShow: true,
                    isAddParallelContainer: isParallel
                })
            },
            toggleAddMenu (isAddMenuShow) {
                if (!this.editable) return
                const { stageIndex, setInertStageIndex } = this
                this.isAddMenuShow = typeof isAddMenuShow === 'boolean' ? isAddMenuShow : false
                if (this.isAddMenuShow) {
                    setInertStageIndex({
                        insertStageIndex: stageIndex
                    })
                }
            },
            handleClick () {
                if (this.isPreStageDone) {
                    this.startNextStage()
                } else if (this.editable) {
                    this.toggleAddMenu(!this.isAddMenuShow)
                }
            },

            startNextStage () {
                console.log('go next stage')
            },
            updateHeight () {
                const parentEle = this.$refs.stageRef
                const cruveEle = parentEle.querySelector('.cruve-line')
                const parallelAddTip = parentEle.querySelector('.parallel-add')
                const height = getOuterHeight('.soda-process-stage', parentEle)
                if (cruveEle) {
                    cruveEle.style.height = `${height}px`
                }
                if (parallelAddTip) {
                    parallelAddTip.style.top = `${height + 10}px`
                }
            }
        }
    }
</script>

<style lang='scss'>
    @import 'Stage';
    $addIconTop: $itemHeight / 2 - $addBtnSize / 2 + $StagepaddingTop;
    .pipeline-drag {
        cursor: url('../../images/grab.cur'), default;
    }
    .pipeline-stage {
        position: relative;
        margin: 0;
        padding-top: $StagepaddingTop;
        &.show-stage-area:hover {
            background: $stageBGColor;
            .pipeline-stage-entry {
                display: block;
            }
        }
        .pipeline-stage-entry {
            position: absolute;
            display: none;
            width: 100%;
            left: 0;
            top: 0;
            height: 32px;
            line-height: 32px;
            background-color: $stageBGColor;
            border-color: #e4e4e4;
            &:hover {
                color: $primaryColor;
                border-color: #e4e4e4;
            }
        }
        .append-stage {
            position: absolute;
            top: $addIconTop;
            right: -$StagePadding - $addBtnSize / 2 + $StageMargin / 2;
            z-index: 3;
            .add-plus-icon {
                box-shadow: 0px 2px 4px 0px rgba(60, 150, 255, 0.2);
            }
        }
        .add-menu {
            position: absolute;
            top: $addIconTop;
            left: -9px;
            cursor: pointer;
            z-index: 3;
            .add-plus-icon {
                box-shadow: 0px 2px 4px 0px rgba(60, 150, 255, 0.2);
            }
            .line-add {
                top: -30px;
                left: -16px;
            }
            .cruve-line {
                @include cruve-connect ($lineRadius, dashed, $primaryColor, false);
                position: absolute;
                top: 10px + $lineRadius;
                z-index: -1;
                left: 21px;
            }
            .parallel-add {
                left: 50px;
                &:before {
                    content: '';
                    position: absolute;
                    left: -22px;
                    top: 10px;
                    border-top: 2px dashed $primaryColor;
                    width: 17px;
                    height: 0px;
                }
            }
        }
    }
</style>
