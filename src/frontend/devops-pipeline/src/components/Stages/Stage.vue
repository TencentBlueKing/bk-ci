<template>
    <div class="pipeline-stage">
        <ul ref="stageRef" class="soda-process-stage">
            <stage-container v-for="(container, index) in containers"
                :key="index"
                :stage-index="stageIndex"
                :container-index="index"
                :stage-length="stageLength"
                :editable="editable"
                :is-preview="isPreview"
                :container-length="containers.length"
                :container="container">
            </stage-container>
        </ul>
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
            }
        },
        data () {
            return {
                isAddMenuShow: false
            }
        },
        computed: {
            ...mapState('atom', [
                'insertStageIndex'
            ]),
            isFirstStage () {
                return this.stageIndex === 0
            },
            isLastStage () {
                return this.stageIndex === this.stageLength - 1
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
                'toggleStageSelectPopup'
            ]),
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
                const parentEle = this.$refs.stageRef.parentElement
                const cruveEle = parentEle.querySelector('.cruve-line')
                const parallelAddTip = parentEle.querySelector('.parallel-add')
                const height = getOuterHeight(this.$refs.stageRef)
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
    .pipeline-stage {
        position: relative;
        margin: 0;
        .append-stage {
            position: absolute;
            top: $itemHeight / 2 - $addBtnSize / 2;
            right: -$StagePadding - $addBtnSize / 2;
            z-index: 3;
            .add-plus-icon {
                box-shadow: 0px 2px 4px 0px rgba(60, 150, 255, 0.2);
            }
        }
        .add-menu {
            position: absolute;
            top: $itemHeight / 2 - $addBtnSize / 2;
            left: $StagePadding - $addBtnSize / 2;
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
