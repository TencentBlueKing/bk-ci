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
        :editable="reactiveData.editable"
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
      <span
        @click.stop
        v-else-if="reactiveData.canSkipElement"
        class="check-total-stage"
      >
        <bk-checkbox
          class="atom-canskip-checkbox"
          :value="stage.runStage"
          :model-value="stage.runStage"
          @change="handleStageRun"
          :disabled="stageDisabled"
        ></bk-checkbox>
      </span>
      <span
        v-else-if="canStageRetry"
        @click.stop="triggerStageRetry"
        class="stage-single-retry"
      >
        {{ t("retry") }}
      </span>
      <stage-check-icon
        v-else-if="showStageCheck(stage.checkOut)"
        class="check-out-icon"
        check-type="checkOut"
        :stage-index="stageIndex"
        :stage-check="stage.checkOut"
        :is-exec-detail="reactiveData.isExecDetail"
        :user-name="reactiveData.userName"
        :stage-status="stageStatusCls"
      />
      <span v-else class="stage-entry-btns">
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
    </div>
    <span class="stage-connector">
      <Logo size="14" name="right-shape" class="connector-angle" />
    </span>
    <VueDraggable
      v-model="computedContainer"
      v-bind="dragOptions"
      :move="checkMove" 
    >
      <stage-container
        v-for="(container, index) in computedContainer"
        :key="container.containerId"
        :ref="container.containerId"
        :stage-index="stageIndex"
        :container-index="index"
        :stage-length="stageLength"
        :editable="reactiveData.editable"
        :can-skip-element="reactiveData.canSkipElement"
        :handle-change="handleChange"
        :stage-disabled="stageDisabled"
        :is-trigger-stage="isTriggerStage"
        :container-length="computedContainer.length"
        :container="container"
        :is-finally-stage="isFinallyStage"
        :stage="stage"
        @[COPY_EVENT_NAME_VALUE]="handleCopyContainer"
        @[DELETE_EVENT_NAME_VALUE]="handleDeleteContainer"
      >
      </stage-container>
    </VueDraggable>
    <div v-if="reactiveData.editable && stageIndex === 0" class="append-stage-wrapper">
      <append-menu
        :stage-index="stageIndex"
        @append-job="handleAppendJob"
      ></append-menu>
    </div>

    <template v-if="reactiveData.editable">
      <span
        class="add-menu"
        v-if="stageIndex > 0"
        @click.stop="toggleAddMenu(!isAddMenuShow)"
      >
        <span class="add-plus-connector"></span>
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
          <append-menu
            v-if="reactiveData.isCreativeStream"
            :stage-index="stageIndex"
            @append-job="handleAppendJob"
            class="parallel-add"
            :style="`position: absolute; top: ${cruveHeight}px`"
          />
          <div
            v-else
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

<script setup>
import {
    computed,
    getCurrentInstance,
    inject,
    nextTick,
    onBeforeUnmount,
    onMounted,
    onUpdated,
    ref,
} from "vue";
import { VueDraggable } from "vue-draggable-plus";
import AppendMenu from "./AppendMenu.vue";
import CruveLine from "./CruveLine";
import InsertStageMenu from "./InsertStageMenu";
import Logo from "./Logo";
import StageCheckIcon from "./StageCheckIcon";
import StageContainer from "./StageContainer";
import {
    ADD_STAGE,
    APPEND_JOB,
    CLICK_EVENT_NAME,
    COPY_EVENT_NAME,
    DELETE_EVENT_NAME,
    STAGE_RETRY,
    STATUS_MAP,
} from "./constants";
import { t } from "./locale";
import {
    eventBus,
    getOuterHeight,
    hashID,
    isTriggerContainer,
    randomString,
} from "./util";

const props = defineProps({
  containers: {
    type: Array,
    default: [],
  },
  stage: {
    type: Object,
    required: true,
  },
  stageIndex: Number,
  stageLength: Number,
  hasFinallyStage: Boolean,
  handleChange: {
    type: Function,
    required: true,
  },
});

const emit = defineEmits([
  CLICK_EVENT_NAME,
  ADD_STAGE,
  DELETE_EVENT_NAME,
  COPY_EVENT_NAME,
  APPEND_JOB,
]);

const reactiveData = inject("reactiveData");
const emitPipelineChange = inject("emitPipelineChange");
const instance = getCurrentInstance();

// 获取 $showTips 方法（如果存在）
const showTips = (options) => {
  if (instance?.proxy?.$showTips) {
    instance.proxy.$showTips(options);
  } else {
    console.warn("$showTips is not available");
  }
};

const DELETE_EVENT_NAME_VALUE = DELETE_EVENT_NAME;
const COPY_EVENT_NAME_VALUE = COPY_EVENT_NAME;

const stageRef = ref(null);
const isAddMenuShow = ref(false);
const lastAddMenuShow = ref(false);
const cruveHeight = ref(0);
const isStageError = computed(() => {
  try {
    return props.stage.isError;
  } catch (e) {
    console.warn(e);
    return false;
  }
});

const isTriggerStage = computed(() => {
  try {
    return isTriggerContainer(props.stage?.containers?.[0]);
  } catch (e) {
    console.warn(e);
    return false;
  }
});

const canStageRetry = computed(() => {
  return props.stage.canRetry === true;
});

const isLastStage = computed(() => {
  return props.stageIndex === props.stageLength - 1;
});

const isFinallyStage = computed(() => {
  return props.stage.finally === true;
});

const isMiddleStage = computed(() => {
  return !(isTriggerStage.value || isFinallyStage.value);
});

const showCopyStage = computed(() => {
  return isMiddleStage.value && reactiveData.editable && !isTriggerStage.value;
});

const showDeleteStage = computed(() => {
  return reactiveData.editable && !isTriggerStage.value;
});

const stageTitle = computed(() => {
  return props.stage ? props.stage.name : "stage";
});

const stageDisabled = computed(() => {
  return !!(
    props.stage.stageControlOption &&
    props.stage.stageControlOption.enable === false
  );
});

const stageStatusCls = computed(() => {
  return props.stage && props.stage.status ? props.stage.status : "";
});

const isStageSkip = computed(() => {
  return props.stage.status === STATUS_MAP.SKIP;
});

const stageStatusIcon = computed(() => {
  if (isStageSkip.value) return "redo-arrow";
  switch (stageStatusCls.value) {
    case STATUS_MAP.SUCCEED:
      return "check-circle";
    case STATUS_MAP.FAILED:
      return "close-circle";
    case STATUS_MAP.SKIP:
      return "redo-arrow";
    case STATUS_MAP.RUNNING:
      return "circle-2-1";
    default:
      return "";
  }
});

const isUnExecThisTime = computed(() => {
  return props.stage?.executeCount < reactiveData.currentExecCount;
});

const stageTitleCls = computed(() => {
  return {
    "stage-entry-name": true,
    "skip-name": stageDisabled.value || props.stage.status === STATUS_MAP.SKIP,
  };
});

const stageNameStatusCls = computed(() => {
  return {
    "stage-name-status-icon": true,
    [stageStatusCls.value]: true,
    "spin-icon": stageStatusCls.value === STATUS_MAP.RUNNING,
  };
});

const pipelineStageCls = computed(() => {
  return [
    stageStatusCls.value,
    "pipeline-stage",
    {
      "is-final-stage": isFinallyStage.value,
      "pipeline-drag": reactiveData.editable && !isTriggerStage.value,
      readonly: !reactiveData.editable || stageDisabled.value,
      editable: reactiveData.editable,
      "un-exec-this-time": reactiveData.isExecDetail && isUnExecThisTime.value,
    },
  ];
});

const computedContainer = computed({
  get() {
    return props.containers;
  },
  set(containers) {
    let data = [];
    containers.forEach((container) => {
      if (Array.isArray(container.containers)) {
        // 拖动的是stage
        data = [...data, ...container.containers];
      } else {
        data.push(container);
      }
    });
    if (data.length === 0) {
      nextTick(() => {
        deleteStageHandler();
      });
    } else {
      props.handleChange(props.stage, {
        containers: data,
      });
    }
  },
});

const dragOptions = computed(() => {
  return {
    group: props.stage.finally ? "finally-stage-job" : "pipeline-job",
    ghostClass: "sortable-ghost-atom",
    chosenClass: "sortable-chosen-atom",
    animation: 130,
    disabled: !reactiveData.editable,
  };
});

const iconCls = computed(() => {
  switch (true) {
    case !isAddMenuShow.value:
      return "add-plus-icon";
    case isAddMenuShow.value:
      return "minus-icon";
    default:
      return "add-plus-icon";
  }
});

const disableFinally = computed(() => {
  return props.hasFinallyStage;
});
const handleStageRun = (checked) => {
  const { containers } = props.stage;
  if (stageDisabled.value || !reactiveData.canSkipElement) return;
  containers
    .filter(
      (container) =>
        container.jobControlOption === undefined ||
        container.jobControlOption.enable
    )
    .forEach((container) => {
      container.runContainer = checked;
    });
  props.handleChange(props.stage, {
    containers,
  });
};

const triggerStageRetry = () => {
  eventBus.$emit(STAGE_RETRY, {
    taskId: props.stage.id,
  });
};

const stageEntryClick = () => {
  eventBus.$emit(CLICK_EVENT_NAME, {
    stageIndex: props.stageIndex,
  });
};

const showStageCheck = (stageCheck = {}) => {
  const hasReviewFlow = stageCheck.manualTrigger;
  const hasReviewQuality =
    Array.isArray(stageCheck.ruleIds) && stageCheck.ruleIds.length > 0;
  return isMiddleStage.value && (hasReviewFlow || hasReviewQuality);
};

const checkMove = (event) => {
  const dragContext = event.draggedContext || {};
  const element = dragContext.element || {};
  const isTrigger = element["@type"] === "trigger";
  const relatedContext = event.relatedContext || {};
  const relatedelement = relatedContext.element || {};
  const isRelatedTrigger = relatedelement["@type"] === "trigger";
  const isTriggerStageValue = isTriggerContainer(
    relatedelement?.containers?.[0]
  );
  const isFinallyStageValue = relatedelement.finally === true;

  return (
    !isTrigger &&
    !isRelatedTrigger &&
    !isTriggerStageValue &&
    !isFinallyStageValue
  );
};

const editStage = (isParallel, isFinally, isLast) => {
  eventBus.$emit(ADD_STAGE, {
    stageIndex: isFinally || isLast ? props.stageLength : props.stageIndex,
    isParallel,
    isFinally,
  });
  hideAddStage();
};

const toggleAddMenu = (isShow, isLast = false) => {
  if (!reactiveData.editable) return;
  const show = typeof isShow === "boolean" ? isShow : false;
  if (isLast) {
    lastAddMenuShow.value = show;
  } else {
    isAddMenuShow.value = show;
  }
};

const hideAddStage = () => {
  isAddMenuShow.value = false;
  lastAddMenuShow.value = false;
};

const updateHeight = () => {
  if (stageRef.value) {
    const height = getOuterHeight(stageRef.value);
    cruveHeight.value = height;
  }
};

const deleteStageHandler = () => {
  emit(DELETE_EVENT_NAME, props.stage.id);
};

const handleCopyContainer = ({ containerIndex, container }) => {
  const newContainers = [...props.stage.containers];
  newContainers.splice(containerIndex + 1, 0, container);
  props.handleChange({
    ...props.stage,
    containers: newContainers,
  });
};

const handleDeleteContainer = ({ containerIndex }) => {
  if (Number.isInteger(containerIndex)) {
    const newContainers = [...props.stage.containers];
    newContainers.splice(containerIndex, 1);
    props.handleChange({
      ...props.stage,
      containers: newContainers,
    });
  } else {
    deleteStageHandler();
  }
};

const copyStage = () => {
  try {
    const copyStage = JSON.parse(JSON.stringify(props.stage));
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
          id: `e-${hashID()}`,
        })),
        jobControlOption: container.jobControlOption
          ? {
              ...container.jobControlOption,
              dependOnType: "ID",
              dependOnId: [],
            }
          : undefined,
      })),
    };
    emit(COPY_EVENT_NAME, {
      stageIndex: props.stageIndex,
      stage,
    });
  } catch (e) {
    console.error(e);
    showTips({
      theme: "error",
      message: t("copyStageFail"),
    });
  }
};

const handleAppendJob = (payload) => {
  eventBus.$emit(APPEND_JOB, payload);
};

onMounted(() => {
  updateHeight();
  document.addEventListener("click", hideAddStage);
});

onBeforeUnmount(() => {
  document.removeEventListener("click", hideAddStage);
});

onUpdated(() => {
  updateHeight();
});
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
  flex-shrink: 0;

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

    .add-plus-connector {
      position: absolute;
      width: 24px;
      height: 2px;
      left: 17px;
      top: 8px;
      background-color: $primaryColor;
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
