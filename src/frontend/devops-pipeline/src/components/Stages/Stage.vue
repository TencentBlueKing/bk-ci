<template>
    <div :class="[{ 'pipeline-drag': editable && !isTriggerStage, 'show-stage-area': !isTriggerStage }, 'pipeline-stage']" ref="stageRef">
        <bk-button v-if="!isTriggerStage" :class="['pipeline-stage-entry', [stageStatusCls], { 'editable-stage-entry': editable, 'stage-disabled': stageDisabled }]" @click="showStagePanel">
            <span v-if="stage.status === 'PAUSE'" class="bk-icon icon-play-circle-shape" v-bk-tooltips.top="canTriggerStage ? $t('editPage.toCheck') : $t('editPage.noAuthToCheck')" @click.stop="startNextStage"></span>
            <logo v-else-if="stage.status === 'SKIP'" v-bk-tooltips="$t('skipStageDesc')" class="skip-icon redo-arrow" name="redo-arrow" size="16"></logo>
            <i v-else-if="stageStatusIcon" :class="`stage-status-icon bk-icon icon-${stageStatusIcon}`"></i>
            <span class="stage-entry-name">{{ stageTitle }}</span>
            <i v-if="stage.isError" class="bk-icon icon-exclamation-triangle-shape stage-entry-error-icon" />
            <span @click.stop v-if="showCheckedToatal && canSkipElement" class="check-total-stage">
                <bk-checkbox class="atom-canskip-checkbox" v-model="stage.runStage" :disabled="stageDisabled"></bk-checkbox>
            </span>
            <span class="stage-entry-btns">
                <span :title="$t('editPage.copyStage')" v-if="showCopyStage && !stage.isError" class="bk-icon copy-stage" @click.stop="copyStage">
                    <Logo name="copy" size="16"></Logo>
                </span>
                <i v-if="showCopyStage" @click.stop="deleteStageHandler" class="add-plus-icon close" />
            </span>
        </bk-button>
        <draggable v-model="computedContainer" v-bind="dragOptions" :move="checkMove" tag="ul" class="soda-process-stage">
            <stage-container v-for="(container, index) in computedContainer"
                :key="`${container.id}-${index}`"
                :stage-index="stageIndex"
                :container-index="index"
                :stage-length="stageLength"
                :editable="editable"
                :is-preview="isPreview"
                :can-skip-element="canSkipElement"
                :stage-disabled="stageDisabled"
                :container-length="computedContainer.length"
                :container="container">
            </stage-container>
        </draggable>
        <template v-if="editable">
            <span v-bk-clickoutside="toggleAddMenu" v-if="!isFirstStage" class="add-menu" @click.stop="toggleAddMenu(!isAddMenuShow)">
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

    import Vue from 'vue'
    import { mapActions, mapState, mapGetters } from 'vuex'
    import StageContainer from './StageContainer'
    import { getOuterHeight } from '@/utils/util'
    import Logo from '@/components/Logo'

    export default {
        components: {
            StageContainer,
            Logo
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
            showCopyStage () {
                return !this.isTriggerStage && this.editable
            },
            isFirstStage () {
                return this.stageIndex === 0
            },
            isLastStage () {
                return this.stageIndex === this.stageLength - 1
            },
            isTriggerStage () {
                return this.checkIsTriggerStage(this.stage)
            },
            stageTitle () {
                return this.stage ? this.stage.name : 'stage'
            },
            showCheckedToatal () {
                const { isTriggerStage, $route } = this
                return $route.path.indexOf('preview') > 0 && !isTriggerStage && this.canSkipElement
            },
            stageDisabled () {
                return !!(this.stage.stageControlOption && this.stage.stageControlOption.enable === false)
            },
            canTriggerStage () {
                try {
                    return this.stage.stageControlOption.triggerUsers.includes(this.$userInfo.username)
                } catch (e) {
                    return false
                }
            },
            computedContainer: {
                get () {
                    return this.containers
                },
                set (containers) {
                    let data = []
                    containers.forEach((container) => {
                        if (container.containers) data = [...data, ...container.containers] // 拖动的是stage
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
            isStagePause () {
                try {
                    return this.stage.status === 'PAUSE'
                } catch (error) {
                    return false
                }
            },
            iconCls () {
                switch (true) {
                    case !this.isAddMenuShow:
                        return 'add-plus-icon'
                    case this.isAddMenuShow:
                        return 'minus-icon'
                    default:
                        return 'add-plus-icon'
                }
            },
            stageStatusIcon () {
                switch (this.stage.status) {
                    case 'SUCCEED':
                        return 'check-circle'
                    case 'FAILED':
                        return 'close-circle'
                    case 'SKIP':
                        return 'redo-arrow'
                    case 'RUNNING':
                        return 'circle-2-1 spin-icon'
                }
                return ''
            },
            stageStatusCls () {
                return this.stage && this.stage.status ? this.stage.status : ''
            }
        },
        watch: {
            'stage.runStage' (newVal) {
                const { stage, updateStage } = this
                const { containers } = stage
                if (this.stageDisabled || !this.showCheckedToatal) return
                containers.filter(container => (container.jobControlOption === undefined || container.jobControlOption.enable)).map(container => {
                    container.runContainer = newVal
                    return false
                })
                updateStage({
                    stage,
                    newParam: {
                        containers
                    }
                })
            }
        },
        mounted () {
            this.updateHeight()
            if (this.showCheckedToatal) {
                Vue.set(this.stage, 'runStage', !this.stageDisabled)
            }
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
                'setPipelineEditing',
                'updateStage',
                'triggerStage',
                'deleteStage',
                'toggleReviewDialog'
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

            startNextStage () {
                if (this.canTriggerStage) {
                    this.toggleReviewDialog({
                        isShow: true,
                        reviewInfo: this.stage
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
            },
            deleteStageHandler () {
                const { stageIndex } = this

                this.deleteStage({
                    stageIndex
                })
            },
            copyStage () {
                try {
                    const copyStage = JSON.parse(JSON.stringify(this.stage))
                    const { id, ...stage } = copyStage
                    stage.containers = stage.containers.map(container => {
                        const { id, ...job } = container

                        job.elements = job.elements.map(element => {
                            const { id, ...ele } = element
                            return ele
                        })
                        return job
                    })

                    this.pipeline.stages.splice(this.stageIndex + 1, 0, JSON.parse(JSON.stringify(stage)))
                    this.setPipelineEditing(true)
                } catch (e) {
                    console.error(e)
                    this.$showTips({
                        theme: 'error',
                        message: this.$t('editPage.copyStageFail')
                    })
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
        .pipeline-stage-entry {
            position: absolute;
            display: block;
            width: 88%;
            left: 6%;
            top: 0;
            height: 32px;
            line-height: 32px;
            background-color: #EFF5FF;
            border-color: #D4E8FF;
            color: $primaryColor;
            z-index: 1;

            &:not(.editable-stage-entry),
            &.stage-disabled {
                background-color: #F3F3F3;
                border-color: #D0D8EA;
                color: black;

                .skip-icon,
                .icon-play-circle-shape {
                    vertical-align: middle;
                }

                &.SKIP {
                    color: $borderLightColor;
                    fill: $borderLightColor;
                }

                &.RUNNING {
                    background-color: #EFF5FF;
                    border-color: #D4E8FF;
                    color: $primaryColor;
                }
                &.PAUSE {
                    background-color: #F3F3F3;
                    border-color: #D0D8EA;
                    color: black;

                    .icon-play-circle-shape {
                        color: $primaryColor;
                        font-size: 20px;
                        width: 20px;
                        height: 20px;
                    }
                }

                &.FAILED {
                    border-color: #FFD4D4;
                    background-color: #FFF9F9;
                    color: black;
                    .stage-status-icon {
                        color: #FF5656;
                    }
                }
                &.SUCCEED {
                    background-color: #F3FFF6;
                    border-color: #BBEFC9;
                    color: black;
                    .stage-status-icon {
                        color: #34DA7B;
                    }

                }
            }

            &.editable-stage-entry:hover {
                color: black;
                border-color: #1A6DF3;
                background-color: #D1E2FD;
                .stage-entry-btns {
                    display: flex;
                }
                .stage-entry-error-icon {
                    display: none;
                }
            }

            .stage-entry-error-icon,
            .check-total-stage {
                position: absolute;
                right: 27px;
                &.stage-entry-error-icon {
                    top: 7px;
                    right: 8px;
                    color: $dangerColor;
                }
            }

            .stage-entry-btns {
                position: absolute;
                right: 0;
                top: 7px;
                display: none;
                .copy-stage {
                    margin-right: 8px;
                    fill: white;
                }
                .close {
                    @include add-plus-icon(#2E2E3A, #2E2E3A, white, 16px, true);
                    @include add-plus-icon-hover($dangerColor, $dangerColor, white);
                    border: none;
                    margin-right: 10px;
                    transform: rotate(45deg);
                    cursor: pointer;
                    &:before, &:after {
                        left: 7px;
                        top: 4px;
                    }
                }
            }
        }

        &.show-stage-area {
            .soda-process-stage:before {
                position: absolute;
                content: '';
                width: 88%;
                top: 0;
                left: 6%;
                height: 100%;
                background: $stageBGColor;
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
