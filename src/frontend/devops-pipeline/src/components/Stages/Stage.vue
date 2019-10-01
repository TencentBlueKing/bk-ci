<template>
    <div :class="[{ 'pipeline-drag': editable }, 'pipeline-stage']" ref="stageRef">
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
        <template v-if="editable">
            <span v-bk-clickoutside="toggleAddMenu" v-if="!isFirstStage" class="add-menu" @click.stop="toggleAddMenu(!isAddMenuShow)">
                <i :class="{ 'add-plus-icon': !isAddMenuShow, 'minus-icon': isAddMenuShow, 'active': isAddMenuShow }" />
                <template v-if="isAddMenuShow">
                    <span class="insert-tip direction line-add" @click.stop="showStageSelectPopup(false)">
                        <i class="tip-icon" />
                        插入
                    </span>
                    <span class="cruve-line left"></span>
                    <div @click.stop="showStageSelectPopup(true)" class="insert-tip parallel-add">
                        <i class="tip-icon" />
                        插入
                    </div>
                </template>
            </span>
            <span v-if="isLastStage" @click="appendStage" class="append-stage pointer">
                <i class="add-plus-icon" />
            </span>
        </template>
    </div>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
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
            isFirstStage () {
                return this.stageIndex === 0
            },
            isLastStage () {
                return this.stageIndex === this.stageLength - 1
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
                'toggleStageSelectPopup',
                'setPipelineContainer',
                'setPipelineEditing'
            ]),

            checkMove (event) {
                const dragContext = event.draggedContext || {}
                const element = dragContext.element || {}
                const isTrigger = element['@type'] === 'trigger'

                const relatedContext = event.relatedContext || {}
                const relatedelement = relatedContext.element || {}
                const isRelatedTrigger = relatedelement['@type'] === 'trigger'
                const isTriggerStage = relatedelement.containers && relatedelement.containers[0]['@type'] === 'trigger'

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
                const { stageIndex, setInertStageIndex } = this
                this.isAddMenuShow = typeof isAddMenuShow === 'boolean' ? isAddMenuShow : false
                if (this.isAddMenuShow) {
                    setInertStageIndex({
                        insertStageIndex: stageIndex
                    })
                }
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
    .pipeline-drag {
        cursor: url('../../images/grab.cur'), default;
    }
    .pipeline-stage {
        position: relative;
        margin: 0;
        .append-stage {
            position: absolute;
            top: $itemHeight / 2 - $addBtnSize / 2;
            right: -$StagePadding - $addBtnSize / 2 + $StageMargin / 2;
            z-index: 3;
            .add-plus-icon {
                box-shadow: 0px 2px 4px 0px rgba(60, 150, 255, 0.2);
            }
        }
        .add-menu {
            position: absolute;
            top: $itemHeight / 2 - $addBtnSize / 2;
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
