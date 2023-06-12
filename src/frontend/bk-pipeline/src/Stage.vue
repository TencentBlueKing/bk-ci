<template>
    <div ref="stageRef" :class="pipelineStageCls">
        <div @click.stop="stageEntryClick" class="pipeline-stage-entry">
            <stage-check-icon
                v-if="isMiddleStage"
                class="check-in-icon"
                check-type="checkIn"
                :stage-index="stageIndex"
                :stage-check="stage.checkIn"
                :is-exec-detail="reactiveData.isExecDetail"
                :user-name="reactiveData.userName"
                :stage-status="stageStatusCls"
            />
            <span :title="stageTitle" :class="stageTitleCls">
                <Logo
                    v-if="!!stageStatusIcon"
                    v-bk-tooltips="isStageSkip ? t('skipStageDesc') : { disabled: true }"
                    :name="stageStatusIcon"
                    :class="stageNameStatusCls"
                    size="20"
                />
                <span class="stage-title-name">{{ stageTitle }}</span>
            </span>
            <Logo
                v-if="isStageError"
                name="exclamation-triangle-shape"
                size="14"
                class="stage-entry-error-icon"
            />
            <span @click.stop v-if="reactiveData.canSkipElement" class="check-total-stage">
                <bk-checkbox
                    class="atom-canskip-checkbox"
                    v-model="stage.runStage"
                    @change="handleStageRun"
                    :disabled="stageDisabled"
                ></bk-checkbox>
            </span>
            <span
                v-if="canStageRetry"
                @click.stop="triggerStageRetry"
                class="stage-single-retry"
            >
                {{ t("retry") }}
            </span>
            <span v-if="!stage.isError" class="stage-entry-btns">
                <Logo
                    class="copy-stage"
                    v-if="showCopyStage"
                    name="clipboard"
                    size="14"
                    :title="t('copyStage')"
                    @click.stop="copyStage"
                />
                <i
                    v-if="showDeleteStage"
                    @click.stop="deleteStageHandler"
                    class="add-plus-icon close"
                />
            </span>
            <stage-check-icon
                v-if="showStageCheck(stage.checkOut)"
                class="check-out-icon"
                check-type="checkOut"
                :stage-index="stageIndex"
                :stage-check="stage.checkOut"
                :is-exec-detail="reactiveData.isExecDetail"
                :user-name="reactiveData.userName"
                :stage-status="stageStatusCls"
            />
        </div>
        <span class="stage-connector">
            <Logo size="14" name="right-shape" class="connector-angle" />
        </span>
        <draggable
            v-model="computedContainer"
            v-bind="dragOptions"
            :move="checkMove"
            tag="ul"
        >
            <stage-container
                v-for="(container, index) in computedContainer"
                :ref="container.containerId"
                :key="container.containerId"
                :stage-index="stageIndex"
                :container-index="index"
                :stage-length="stageLength"
                :editable="reactiveData.editable"
                :can-skip-element="isShowCheckbox"
                :handle-change="handleChange"
                :stage-disabled="stageDisabled"
                :container-length="computedContainer.length"
                :container="container"
                :is-finally-stage="isFinallyStage"
                :stage="stage"
                @[COPY_EVENT_NAME]="handleCopyContainer"
                @[DELETE_EVENT_NAME]="handleDeleteContainer"
            >
            </stage-container>
        </draggable>

        <template v-if="reactiveData.editable">
            <span
                v-if="!isFirstStage"
                class="add-menu"
                @click.stop="toggleAddMenu(!isAddMenuShow)"
            >
                <i :class="{ [iconCls]: true, active: isAddMenuShow }" />
                <template v-if="isAddMenuShow">
                    <cruve-line
                        class="add-connector connect-line left"
                        :width="60"
                        :height="cruveHeight"
                    ></cruve-line>
                    <insert-stage-menu
                        :disable-finally="disableFinally"
                        :edit-stage="editStage"
                    ></insert-stage-menu>
                    <div
                        @click.stop="editStage(true)"
                        class="insert-tip parallel-add"
                        :style="`top: ${cruveHeight}px`"
                    >
                        <i class="tip-icon" />
                        <span>
                            {{ t("append") }}
                        </span>
                    </div>
                </template>
            </span>
            <span
                v-if="isLastStage && !isFinallyStage && reactiveData.editable"
                @click.stop="toggleAddMenu(!lastAddMenuShow, true)"
                class="append-stage pointer"
            >
                <span class="add-plus-connector"></span>
                <i class="add-plus-icon" />
                <insert-stage-menu
                    v-if="lastAddMenuShow"
                    :disable-finally="disableFinally"
                    :is-last="true"
                    :edit-stage="editStage"
                ></insert-stage-menu>
            </span>
        </template>
    </div>
</template>

<script>
    import draggable from 'vuedraggable'
    import StageContainer from './StageContainer'
    import Logo from './Logo'
    import CruveLine from './CruveLine'
    import InsertStageMenu from './InsertStageMenu'
    import StageCheckIcon from './StageCheckIcon'
    import { localeMixins } from './locale'

    import { getOuterHeight, hashID, randomString, eventBus } from './util'
    import {
        CLICK_EVENT_NAME,
        ADD_STAGE,
        DELETE_EVENT_NAME,
        COPY_EVENT_NAME,
        STAGE_RETRY,
        STATUS_MAP
    } from './constants'

    export default {
        components: {
            draggable,
            StageContainer,
            CruveLine,
            Logo,
            InsertStageMenu,
            StageCheckIcon
        },
        mixins: [localeMixins],
        props: {
            containers: {
                type: Array,
                default: []
            },
            stage: {
                type: Object,
                required: true
            },
            stageIndex: Number,
            stageLength: Number,
            hasFinallyStage: Boolean,
            handleChange: {
                type: Function,
                required: true
            }
        },
        inject: ['reactiveData'],
        emits: [CLICK_EVENT_NAME, ADD_STAGE, DELETE_EVENT_NAME, COPY_EVENT_NAME],
        data () {
            return {
                isAddMenuShow: false,
                lastAddMenuShow: false,
                cruveHeight: 0,
                failedContainer: false,
                DELETE_EVENT_NAME,
                COPY_EVENT_NAME
            }
        },
        computed: {
            isStageError () {
                try {
                    return this.stage.isError
                } catch (e) {
                    console.warn(e)
                    return false
                }
            },
            canStageRetry () {
                return this.stage.canRetry === true
            },
            showCopyStage () {
                return this.isMiddleStage && this.reactiveData.editable && !this.isFirstStage
            },
            showDeleteStage () {
                return this.reactiveData.editable && !this.stage.isTrigger
            },
            isFirstStage () {
                return this.stageIndex === 0
            },
            isLastStage () {
                return this.stageIndex === this.stageLength - 1
            },
            isFinallyStage () {
                return this.stage.finally === true
            },
            isMiddleStage () {
                return !(this.stage.isTrigger || this.isFinallyStage)
            },
            stageTitle () {
                return this.stage ? this.stage.name : 'stage'
            },
            stageTitleCls () {
                return {
                    'stage-entry-name': true,
                    'skip-name': this.stageDisabled || this.stage.status === STATUS_MAP.SKIP
                }
            },
            stageNameStatusCls () {
                return {
                    'stage-name-status-icon': true,
                    [this.stageStatusCls]: true,
                    'spin-icon': this.stageStatusCls === STATUS_MAP.RUNNING
                }
            },
            pipelineStageCls () {
                return [
                    this.stageStatusCls,
                    'pipeline-stage',
                    {
                        'is-final-stage': this.isFinallyStage,
                        'pipeline-drag': this.reactiveData.editable && !this.stage.isTrigger,
                        readonly: !this.reactiveData.editable || this.stageDisabled,
                        editable: this.reactiveData.editable,
                        'un-exec-this-time': this.reactiveData.isExecDetail && this.isUnExecThisTime
                    }
                ]
            },
            stageDisabled () {
                return !!(
                    this.stage.stageControlOption && this.stage.stageControlOption.enable === false
                )
            },
            computedContainer: {
                get () {
                    return this.containers
                },
                set (containers) {
                    let data = []
                    containers.forEach((container) => {
                        if (Array.isArray(container.containers)) {
                            // 拖动的是stage
                            data = [...data, ...container.containers]
                        } else {
                            data.push(container)
                        }
                    })
                    if (data.length === 0) {
                        this.$nextTick(() => {
                            this.deleteStageHandler()
                        })
                    } else {
                        this.handleChange(this.stage, {
                            containers: data
                        })
                    }
                }
            },
            dragOptions () {
                return {
                    group: this.stage.finally ? 'finally-stage-job' : 'pipeline-job',
                    ghostClass: 'sortable-ghost-atom',
                    chosenClass: 'sortable-chosen-atom',
                    animation: 130,
                    disabled: !this.reactiveData.editable
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
            stageStatusCls () {
                return this.stage && this.stage.status ? this.stage.status : ''
            },
            disableFinally () {
                return this.hasFinallyStage || this.stageLength === 1
            },
            isStageSkip () {
                return this.stage.status === STATUS_MAP.SKIP
            },
            stageStatusIcon () {
                if (this.isStageSkip) return 'redo-arrow'
                switch (this.stageStatusCls) {
                    case STATUS_MAP.SUCCEED:
                        return 'check-circle'
                    case STATUS_MAP.FAILED:
                        return 'close-circle'
                    case STATUS_MAP.SKIP:
                        return 'redo-arrow'
                    case STATUS_MAP.RUNNING:
                        return 'circle-2-1'
                    default:
                        return ''
                }
            },
            isUnExecThisTime () {
                return this.stage?.executeCount < this.reactiveData.currentExecCount
            }
        },
        mounted () {
            this.updateHeight()
            document.addEventListener('click', this.hideAddStage)
        },
        beforeDestroy () {
            window.removeEventListener('click', this.hideAddStage)
        },
        updated () {
            this.updateHeight()
        },
        methods: {
            handleStageRun (checked) {
                const { containers } = this.stage
                if (this.stageDisabled || !this.reactiveData.canSkipElement) return
                containers
                    .filter(
                        (container) =>
                            container.jobControlOption === undefined || container.jobControlOption.enable
                    )
                    .forEach((container) => {
                        container.runContainer = checked
                    })
                this.handleChange(this.stage, {
                    containers
                })
            },
            triggerStageRetry () {
                eventBus.$emit(STAGE_RETRY, {
                    taskId: this.stage.id
                })
            },

            stageEntryClick () {
                eventBus.$emit(CLICK_EVENT_NAME, {
                    stageIndex: this.stageIndex
                })
            },

            showStageCheck (stageCheck = {}) {
                const hasReviewFlow = stageCheck.manualTrigger
                const hasReviewQuality
                    = Array.isArray(stageCheck.ruleIds) && stageCheck.ruleIds.length > 0
                return this.isMiddleStage && (hasReviewFlow || hasReviewQuality)
            },

            checkMove (event) {
                const dragContext = event.draggedContext || {}
                const element = dragContext.element || {}
                const isTrigger = element['@type'] === 'trigger'
                const relatedContext = event.relatedContext || {}
                const relatedelement = relatedContext.element || {}
                const isRelatedTrigger = relatedelement['@type'] === 'trigger'
                const isTriggerStage = relatedelement.isTrigger
                const isFinallyStage = relatedelement.finally === true

                return !isTrigger && !isRelatedTrigger && !isTriggerStage && !isFinallyStage
            },
            editStage (isParallel, isFinally, isLast) {
                eventBus.$emit(ADD_STAGE, {
                    stageIndex: isFinally || isLast ? this.stageLength : this.stageIndex,
                    isParallel,
                    isFinally
                })
                this.hideAddStage()
            },

            toggleAddMenu (isShow, isLast = false) {
                if (!this.reactiveData.editable) return
                const show = typeof isShow === 'boolean' ? isShow : false
                if (isLast) {
                    this.lastAddMenuShow = show
                } else {
                    this.isAddMenuShow = show
                }
            },

            hideAddStage () {
                this.isAddMenuShow = false
                this.lastAddMenuShow = false
            },
            updateHeight () {
                const parentEle = this.$refs.stageRef
                const height = getOuterHeight(parentEle)
                this.cruveHeight = height
            },
            deleteStageHandler () {
                this.$emit(DELETE_EVENT_NAME, this.stage.id)
            },
            handleCopyContainer ({ containerIndex, container }) {
                this.stage.containers.splice(containerIndex + 1, 0, container)
            },
            handleDeleteContainer ({ containerIndex }) {
                if (Number.isInteger(containerIndex)) {
                    this.stage.containers.splice(containerIndex, 1)
                } else {
                    this.deleteStageHandler()
                }
            },
            copyStage () {
                try {
                    const copyStage = JSON.parse(JSON.stringify(this.stage))
                    const stage = {
                        ...copyStage,
                        id: `s-${hashID()}`,
                        containers: copyStage.containers.map((container) => ({
                            ...container,
                            jobId: `job_${randomString(3)}`,
                            containerId: `c-${hashID()}`,
                            containerHashId: undefined,
                            elements: container.elements.map((element) => ({
                                ...element,
                                id: `e-${hashID()}`
                            })),
                            jobControlOption: container.jobControlOption
                                ? {
                                    ...container.jobControlOption,
                                    dependOnType: 'ID',
                                    dependOnId: []
                                }
                                : undefined
                        }))
                    }
                    this.$emit(COPY_EVENT_NAME, {
                        stageIndex: this.stageIndex,
                        stage
                    })
                } catch (e) {
                    console.error(e)
                    this.$showTips({
                        theme: 'error',
                        message: this.t('copyStageFail')
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
@use "sass:math";
@import "./conf";
$addIconTop: math.div($stageEntryHeight, 2) - math.div($addBtnSize, 2);
$entryBtnWidth: 80px;

.pipeline-drag {
  cursor: grab, default;
}

.pipeline-stage {
  position: relative;
  width: 280px;
  border-radius: 2px;
  padding: 0;
  background: $stageBGColor;
  margin: 0 $StageMargin 0 0;

  .pipeline-stage-entry {
    position: relative;
    cursor: pointer;
    display: flex;
    width: 100%;
    height: 50px;
    align-items: center;
    min-width: 0;
    font-size: 14px;
    background-color: #eff5ff;
    border: 1px solid #d4e8ff;
    color: $primaryColor;

    &:hover {
      border-color: #1a6df3;
      background-color: #d1e2fd;
    }
    .check-in-icon,
    .check-out-icon {
      position: absolute;
      left: math.div(-$reviewIconSize, 2);
      top: math.div(($stageEntryHeight - $reviewIconSize), 2);

      &.check-out-icon {
        left: auto;
        right: math.div(-$reviewIconSize, 2);
      }
    }

    .stage-entry-name {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 $entryBtnWidth;
      overflow: hidden;
      .stage-title-name {
        @include ellipsis();
        margin-left: 6px;
      }
    }

    .stage-single-retry {
      cursor: pointer;
      position: absolute;
      right: 6%;
      color: $primaryColor;
    }

    .stage-entry-error-icon,
    .check-total-stage {
      position: absolute;
      right: 27px;
      &.stage-entry-error-icon {
        top: 16px;
        right: 8px;
        color: $dangerColor;
      }
    }

    .stage-entry-btns {
      position: absolute;
      right: 0;
      top: 16px;
      display: none;
      width: $entryBtnWidth;
      align-items: center;
      justify-content: flex-end;
      color: white;
      fill: white;
      z-index: 2;

      .copy-stage:hover {
        color: $primaryColor;
      }

      .close {
        @include add-plus-icon(#2e2e3a, #2e2e3a, white, 16px, true);
        @include add-plus-icon-hover($dangerColor, $dangerColor, white);
        border: none;
        margin: 0 10px 0 8px;
        transform: rotate(45deg);
        cursor: pointer;
        &:before,
        &:after {
          left: 7px;
          top: 4px;
        }
      }
    }
  }

  &.editable {
    &:not(.readonly) {
      .pipeline-stage-entry:hover {
        color: black;
        border-color: #1a6df3;
        background-color: #d1e2fd;
      }
    }
    .pipeline-stage-entry:hover {
      .stage-entry-btns {
        display: flex;
      }
      .stage-entry-error-icon {
        display: none;
      }
    }
  }

  &.readonly {
    &.SKIP .pipeline-stage-entry {
      color: $borderLightColor;
      fill: $borderLightColor;
    }

    &.RUNNING .pipeline-stage-entry {
      background-color: #eff5ff;
      border-color: #d4e8ff;
      color: $primaryColor;
    }
    &.REVIEWING .pipeline-stage-entry {
      background-color: #f3f3f3;
      border-color: #d0d8ea;
      color: black;
    }

    &.FAILED .pipeline-stage-entry {
      border-color: #ffd4d4;
      background-color: #fff9f9;
      color: black;
    }
    &.SUCCEED .pipeline-stage-entry {
      background-color: #f3fff6;
      border-color: #bbefc9;
      color: black;
    }
    .pipeline-stage-entry {
      background-color: #f3f3f3;
      border-color: #d0d8ea;
      color: black;

      .skip-icon {
        vertical-align: middle;
      }
    }
  }

  $addConnectLeft: math.div($addBtnSize, 2) + 1;
  .add-connector {
    stroke-dasharray: 4, 4;
    top: 7px;
    left: $addConnectLeft;
  }

  .append-stage {
    position: absolute;
    top: $addIconTop;
    right: $appendIconRight;
    z-index: 3;
    .add-plus-icon {
      box-shadow: 0px 2px 4px 0px rgba(60, 150, 255, 0.2);
    }
    .line-add {
      top: -46px;
      left: -16px;
    }
    .add-plus-connector {
      position: absolute;
      width: 40px;
      height: 2px;
      left: -26px;
      top: 8px;
      background-color: $primaryColor;
    }
  }

  .add-menu {
    position: absolute;
    top: $addIconTop;
    left: $addIconLeft + 1;
    cursor: pointer;
    z-index: 3;
    .add-plus-icon {
      box-shadow: 0px 2px 4px 0px rgba(60, 150, 255, 0.2);
    }
    .minus-icon {
      z-index: 4;
    }
    .line-add {
      top: -46px;
      left: -16px;
    }
    .parallel-add {
      left: 50px;
    }
  }

  &:first-child {
    .stage-connector {
      display: none;
    }
  }

  &.is-final-stage {
    .stage-connector {
      width: $StageMargin;
    }
  }
  .stage-connector {
    position: absolute;
    width: $StageMargin - math.div($reviewIconSize, 2);
    height: $stageConnectorSize;
    left: -$StageMargin;
    top: math.div($stageEntryHeight, 2) - 1;
    color: $primaryColor;
    background-color: $primaryColor;
    // 实心圆点
    &:before {
      content: "";
      width: $dotR;
      height: $dotR;
      position: absolute;
      left: math.div(-$dotR, 2);
      top: -(math.div($dotR, 2) - 1);
      background-color: $primaryColor;
      border-radius: 50%;
    }
    .connector-angle {
      position: absolute;
      right: -$angleSize + 3px;
      top: -$angleSize;
    }
  }

  .insert-stage {
    position: absolute;
    display: block;
    width: 160px;
    background-color: #ffffff;
    border: 1px solid #dcdee5;
    .click-item {
      padding: 0 15px;
      font-size: 12px;
      line-height: 32px;

      &:hover,
      :hover {
        color: #3c96ff;
        background-color: #eaf3ff;
      }
    }
    .disabled-item {
      cursor: not-allowed;
      color: #c4cdd6;
      &:hover,
      :hover {
        color: #c4cdd6;
        background-color: #ffffff;
      }
    }
  }
}
.stage-retry-dialog {
  .bk-form-radio {
    display: block;
    margin-top: 15px;
    .bk-radio-text {
      font-size: 14px;
    }
  }
}
</style>
