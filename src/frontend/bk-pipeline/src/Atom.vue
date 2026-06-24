<template>
  <li :key="atom.id" :class="atomCls" @click.stop="handleAtomClick" :id="atom.id">
    <template v-if="isQualityGateAtom">
      <span class="atom-title">
        <i></i>
        <span>{{ t("quality") }}</span>
        <i></i>
      </span>
      <template v-if="isReviewing">
        <Logo v-if="isBusy" name="circle-2-1" size="14" class="spin-icon" />
        <span v-else :class="{
          'handler-list': true,
          'disabled-review': isReviewing && !hasReviewPerm,
        }">
          <span class="revire-btn" @click.stop="qualityApprove('PROCESS')">{{
            t("resume")
            }}</span>
          <span class="review-btn" @click.stop="qualityApprove('ABORT')">{{
            t("terminate")
            }}</span>
        </span>
      </template>
    </template>
    <template v-else>
      <Logo v-if="atom.locateActive" name="location-right" size="18" class="active-atom-location-icon" />
      <p class="atom-retry-indicate-icon-group">
        <span v-for="item in retryIndicateList" :key="item.retryType" v-bk-tooltips="item.tips"
          class="atom-retry-indicate-icon">
          <Logo :name="item.retryType" size="18" />
        </span>
      </p>
      <bk-round-progress v-if="showProgress" ext-cls="atom-progress" v-bind="progressConf"
        :percent="atom.progressRate" />
      <Logo v-else-if="atom.asyncStatus && atom.asyncStatus !== 'SUCCEED'" class="atom-progress"
        :name="`sub_pipeline_${atom.asyncStatus.toLowerCase()}`" />
      <status-icon v-else-if="!isSkip && !!atomStatus" type="element" :status="atomStatus" :is-hook="isHookAtom" />

      <img v-else-if="atom.logoUrl" :src="atom.logoUrl" :class="logoCls" />
      <Logo v-else :class="logoCls" :name="svgAtomIcon" size="18" />
      <p class="atom-name">
        <span :title="atom.name" :class="skipSpanCls">
          {{ atom.atomCode ? atom.name : t("pendingAtom") }}
        </span>
      </p>
      <template v-if="isExecuting">
        <span class="atom-execounter">{{ execTime }}</span>
      </template>
      <Logo v-if="isBusy" name="circle-2-1" size="14" class="spin-icon" />
      <bk-popover :delay="[300, 0]" v-else-if="isReviewing" placement="top">
        <span @click.stop="reviewAtom" class="atom-reviewing-tips atom-operate-area" :disabled="!hasReviewPerm">
          {{ t("manualCheck") }}
        </span>
        <template slot="content">
          <p>{{ t("checkUser") }}{{ reviewUsers.join(";") }}</p>
        </template>
      </bk-popover>
      <bk-popover :delay="[300, 0]" v-else-if="isReviewAbort" placement="top">
        <span class="atom-review-diasbled-tips">{{ t("aborted") }}</span>
        <template slot="content">
          <p>
            {{ t("abortTips") }}{{ t("checkUser")
            }}{{ reactiveData.cancelUserId }}
          </p>
        </template>
      </bk-popover>
      <template v-else-if="atom.status === 'PAUSE'">
        <bk-popover :delay="[300, 0]" placement="top" :disabled="!Array.isArray(atom.pauseReviewers)">
          <span :class="resumeSpanCls" @click.stop="atomExecute(true)">
            {{ t("resume") }}
          </span>
          <template slot="content">
            <p>{{ t("checkUser") }}{{ pauseReviewerStr }}</p>
          </template>
        </bk-popover>
        <span @click.stop="atomExecute(false)" class="pause-button">
          <span>{{ t("stop") }}</span>
        </span>
      </template>
      <span class="atom-operate-area">
        <span v-if="atom.canRetry && !isBusy" @click.stop="skipOrRetry(false)">
          {{ t("retry") }}
        </span>
        <span v-if="atom.canSkip && !isBusy" @click.stop="skipOrRetry(true)">
          {{ t("SKIP") }}
        </span>
        <bk-popover v-if="
          !isSkip &&
          !isWaiting &&
          atom.timeCost &&
          !atom.canSkip &&
          !atom.canRetry &&
          !isExecuting &&
          !reactiveData.editable
        " :delay="[300, 0]" placement="top" :disabled="!atom.timeCost.executeCost">
          <span class="atom-execute-time">
            {{ formatTime }}
          </span>
          <template slot="content">
            <p>{{ formatTime }}</p>
          </template>
        </bk-popover>
      </span>

      <Logo v-if="reactiveData.editable && !atom.isError" name="clipboard" class="copy" size="14" :title="t('copyAtom')"
        @click.stop="copyAtom" />

      <template v-if="reactiveData.editable">
        <i @click.stop="deleteAtom(false)" class="add-plus-icon close" />
        <Logo v-if="atom.isError" class="atom-invalid-icon" name="exclamation-triangle-shape" />
      </template>
      <span v-if="reactiveData.canSkipElement" @click.stop="">
      <bk-checkbox class="atom-canskip-checkbox" :value="atom.canElementSkip" :model-value="atom.canElementSkip" @change="handleAtomSkipChange"
          :disabled="isSkip" />
      </span>

      <i
        v-if="reactiveData.editable && !isLastAtom"
        class="add-plus-icon insert-after"
        @click.stop="handleInsertAfter"
        v-bk-tooltips="t('insertAfterAtom')"
      />
    </template>
  </li>
</template>

<script setup>
import { ref, computed, inject, watch, onMounted, onBeforeUnmount } from "vue";
import Logo from "./Logo";
import StatusIcon from "./StatusIcon";
import {
  ATOM_CONTINUE_EVENT_NAME,
  ATOM_EXEC_EVENT_NAME,
  ATOM_QUALITY_CHECK_EVENT_NAME,
  ATOM_REVIEW_EVENT_NAME,
  CLICK_EVENT_NAME,
  COPY_EVENT_NAME,
  DELETE_EVENT_NAME,
  QUALITY_IN_ATOM_CODE,
  QUALITY_OUT_ATOM_CODE,
  STATUS_MAP,
} from "./constants";
import { t } from "./locale";
import {
  convertMStoString,
  eventBus,
  hashID,
  isTriggerContainer,
  randomString,
} from "./util";

const props = defineProps({
  stage: {
    type: Object,
    required: true,
  },
  container: {
    type: Object,
    required: true,
  },
  atom: {
    type: Object,
    required: true,
  },
  stageIndex: {
    type: Number,
    required: true,
  },
  containerIndex: {
    type: Number,
    required: true,
  },
  containerGroupIndex: Number,
  atomIndex: {
    type: Number,
    required: true,
  },
  isWaiting: Boolean,
  containerDisabled: Boolean,
  isLastAtom: Boolean,
  prevAtom: {
    type: Object,
  },
});

const emit = defineEmits([
  COPY_EVENT_NAME,
  DELETE_EVENT_NAME,
  "atom-skip-change",
  "insert-after",
]);

const reactiveData = inject("reactiveData");

const isBusy = ref(false);
const timer = ref(null);
const execTime = ref(
  props.atom.startEpoch
    ? convertMStoString(Date.now() - props.atom.startEpoch)
    : "--"
);
const isQualityGate = (atom) => {
  try {
    return [QUALITY_IN_ATOM_CODE, QUALITY_OUT_ATOM_CODE].includes(
      atom.atomCode
    );
  } catch (error) {
    return false;
  }
};

const isSkip = computed(() => {
  try {
    return (
      props.atom.status === "SKIP" ||
      props.atom.additionalOptions?.enable === false ||
      props.containerDisabled
    );
  } catch (error) {
    return false;
  }
});

const atomStatus = computed(() => {
  try {
    if (props.atom.status) {
      return props.atom.status;
    }
    return props.isWaiting ? STATUS_MAP.WAITING : "";
  } catch (error) {
    return "";
  }
});

const isExecuting = computed(() => {
  return (
    [STATUS_MAP.RUNNING].includes(atomStatus.value) && props.atom.startEpoch
  );
});

const isReviewing = computed(() => {
  return props.atom?.status === STATUS_MAP.REVIEWING;
});

const reviewUsers = computed(() => {
  try {
    const list =
      props.atom?.reviewUsers ?? props.atom?.data?.input?.reviewers ?? [];
    const reviewUsersList = list
      .map((user) => user.split(";").map((val) => val.trim()))
      .reduce((prev, curr) => {
        return prev.concat(curr);
      }, []);
    return reviewUsersList;
  } catch (error) {
    console.error(error);
    return [];
  }
});

const hasReviewPerm = computed(() => {
  return reviewUsers.value.includes(reactiveData.userName);
});

const hasExecPerm = computed(() => {
  const hasPauseReviewer = Array.isArray(props.atom.pauseReviewers);
  if (
    !hasPauseReviewer ||
    (hasPauseReviewer && props.atom.pauseReviewers.length === 0)
  ) {
    return true;
  }
  return props.atom.pauseReviewers.includes(reactiveData.userName);
});

const isUnExecThisTime = computed(() => {
  return props.atom?.executeCount < reactiveData.currentExecCount;
});

const skipSpanCls = computed(() => {
  return { "skip-name": isSkip.value };
});

const resumeSpanCls = computed(() => {
  return {
    disabled: isBusy.value || !hasExecPerm.value,
    "pause-button": true,
  };
});

const isReviewAbort = computed(() => {
  return props.atom.status === STATUS_MAP.REVIEW_ABORT;
});

const isHookAtom = computed(() => {
  try {
    return !!props.atom.additionalOptions.elementPostInfo;
  } catch (error) {
    return false;
  }
});

const qualityStatus = computed(() => {
  switch (true) {
    case [STATUS_MAP.SUCCEED, STATUS_MAP.REVIEW_PROCESSED].includes(
      props.atom.status
    ):
      return STATUS_MAP.SUCCEED;
    case [STATUS_MAP.QUALITY_CHECK_FAIL, STATUS_MAP.REVIEW_ABORT].includes(
      props.atom.status
    ):
      return STATUS_MAP.FAILED;
  }
  return "";
});

const isQualityGateAtom = computed(() => {
  return isQualityGate(props.atom);
});

const isLastQualityAtom = computed(() => {
  return props.atom.atomCode === QUALITY_OUT_ATOM_CODE && props.isLastAtom;
});

const isPrevAtomQuality = computed(() => {
  return props.prevAtom !== null && isQualityGate(props.prevAtom);
});

const atomStatusCls = computed(() => {
  try {
    if (props.atom.additionalOptions?.enable === false) {
      return STATUS_MAP.DISABLED;
    }
    return atomStatus.value;
  } catch (error) {
    console.error("get atom cls error", error);
    return "";
  }
});

const logoCls = computed(() => {
  return {
    "atom-icon": true,
    "skip-icon": isSkip.value,
  };
});

const atomCls = computed(() => {
  return {
    readonly: !reactiveData.editable,
    "bk-pipeline-atom": true,
    "trigger-atom": isTriggerContainer(props.container),
    [STATUS_MAP.REVIEWING]: isReviewing.value,
    [qualityStatus.value]: isQualityGateAtom.value && !!qualityStatus.value,
    [atomStatusCls.value]: !!atomStatusCls.value,
    "quality-atom": isQualityGateAtom.value,
    "is-sub-pipeline-atom": props.atom.atomCode === "SubPipelineExec",
    "is-error": props.atom.isError,
    "is-intercept": isQualityCheckAtom.value,
    "template-compare-atom": props.atom.templateModify,
    "last-quality-atom": isLastQualityAtom.value,
    "quality-prev-atom": isPrevAtomQuality.value,
    "un-exec-this-time": reactiveData.isExecDetail && isUnExecThisTime.value,
  };
});

const svgAtomIcon = computed(() => {
  if (isHookAtom.value) {
    return "build-hooks";
  }
  const { atomCode } = props.atom;
  if (!atomCode) {
    return "placeholder";
  }
  return atomCode;
});

const pauseReviewerStr = computed(() => {
  return (
    Array.isArray(props.atom.pauseReviewers) &&
    props.atom.pauseReviewers.join(";")
  );
});

const formatTime = computed(() => {
  try {
    const totalCost = Math.max(0, props.atom?.timeCost?.totalCost ?? 0);
    return convertMStoString(totalCost);
  } catch (error) {
    return "--";
  }
});

const isQualityCheckAtom = computed(() => {
  return (
    Array.isArray(reactiveData.matchRules) &&
    reactiveData.matchRules.some(
      (rule) =>
        rule.taskId === props.atom.atomCode &&
        (rule.ruleList.some(
          (val) => props.atom.name.indexOf(val.gatewayId) > -1
        ) ||
          rule.ruleList.every((val) => !val.gatewayId))
    )
  );
});

const showProgress = computed(() => {
  return (
    isExecuting.value &&
    typeof props.atom.progressRate === "number" &&
    props.atom.progressRate < 1
  );
});

const progressConf = computed(() => {
  return {
    width: 28,
    numUnit: "",
    numStyle: {
      fontSize: "10px",
      color: "#333",
      transform: "translate(-50%, -50%)",
    },
    config: {
      strokeWidth: 12,
      bgColor: "#f0f1f5",
      activeColor: "#459fff",
    },
  };
});

const retryIndicateList = computed(() => {
  return ["retryCountAuto", "retryCountManual"].reduce((acc, cur) => {
    const count = props.atom?.[cur] ?? 0;
    if (count > 0) {
      acc.push({
        retryType: cur,
        tips: t(`${cur}Tips`, [count]),
      });
    }
    return acc;
  }, []);
});
const executeCounter = () => {
  clearInterval(timer.value);
  timer.value = setInterval(() => {
    execTime.value = convertMStoString(Date.now() - props.atom.startEpoch);
  }, 1000);
};

const reviewAtom = () => {
  if (hasReviewPerm.value) {
    eventBus.$emit(ATOM_REVIEW_EVENT_NAME, props.atom, reviewUsers.value);
  }
};

const handleAtomClick = () => {
  eventBus.$emit(CLICK_EVENT_NAME, {
    stageIndex: props.stageIndex,
    containerIndex: props.containerIndex,
    containerGroupIndex: props.containerGroupIndex,
    elementIndex: props.atomIndex,
  });
};

const copyAtom = () => {
  const { id, stepId, ...restAttr } = props.atom;
  emit(COPY_EVENT_NAME, {
    elementIndex: props.atomIndex,
    element: JSON.parse(
      JSON.stringify({
        ...restAttr,
        stepId: randomString(3),
        id: `e-${hashID()}`,
      })
    ),
  });
};

const deleteAtom = () => {
  emit(DELETE_EVENT_NAME, {
    elementIndex: props.atomIndex,
  });
};

const handleInsertAfter = () => {
  emit("insert-after", {
    elementIndex: props.atomIndex,
  });
};

const handleAtomSkipChange = (value) => {
  emit("atom-skip-change", {
    elementIndex: props.atomIndex,
    canElementSkip: value,
  });
};

const asyncEvent = (...args) => {
  return new Promise((resolve, reject) => {
    eventBus.$emit(
      ...args,
      () => {
        isBusy.value = false;
        resolve();
      },
      reject
    );
  });
};

const atomExecute = async (isContinue = false) => {
  if (isBusy.value || !hasExecPerm.value) return;

  isBusy.value = true;
  const { stageIndex, containerIndex, containerGroupIndex, atomIndex } = props;

  await asyncEvent(ATOM_EXEC_EVENT_NAME, {
    stageIndex,
    containerIndex,
    containerGroupIndex,
    isContinue,
    showPanelType: "PAUSE",
    elementIndex: atomIndex,
    stageId: props.stage.id,
    containerId: props.container.id,
    taskId: props.atom.id,
    atom: props.atom,
  });
};

const qualityApprove = async (action) => {
  if (hasReviewPerm.value) {
    try {
      isBusy.value = true;
      const { stageIndex, containerIndex, containerGroupIndex, atomIndex } =
        props;
      const data = {
        elementId: props.atom.id,
        stageIndex,
        containerIndex,
        containerGroupIndex,
        atomIndex,
        action,
      };
      await asyncEvent(ATOM_QUALITY_CHECK_EVENT_NAME, data);
    } catch (error) {
      console.error(error);
    }
  }
};

const skipOrRetry = async (skip = false) => {
  if (isBusy.value) return;
  try {
    isBusy.value = true;

    await asyncEvent(ATOM_CONTINUE_EVENT_NAME, {
      taskId: props.atom.id,
      skip,
    });
  } catch (error) {
    console.error(error);
  }
};

watch(isExecuting, (v) => {
  if (v) {
    executeCounter();
  } else {
    clearInterval(timer.value);
  }
});

watch(
  () => props.atom.locateActive,
  (val) => {
    if (val) {
      const ele = document.getElementById(props.atom.id);
      ele?.scrollIntoView?.({
        block: "center",
        inline: "center",
        behavior: "smooth",
      });
    }
  }
);

watch(atomStatus, () => {
  isBusy.value = false;
});

onMounted(() => {
  if (isExecuting.value) {
    executeCounter();
  }
});

onBeforeUnmount(() => {
  clearInterval(timer.value);
});
</script>

<style lang="scss">
@import "./conf";

.bk-pipeline .bk-pipeline-atom {
  cursor: pointer;
  position: relative;
  display: flex;
  flex-direction: row;
  align-items: center;
  height: $itemHeight;
  margin: 0 0 11px 0;
  background-color: white;
  border-radius: 2px;
  font-size: 14px;
  transition: all 0.4s ease-in-out;
  z-index: 2;
  border: 1px solid $fontLighterColor;

  .atom-progress {
    display: inline-flex;
    width: 42px;
    height: 42px;
    align-items: center;
    justify-content: center;
  }

  .active-atom-location-icon {
    position: absolute;
    color: $primaryColor;
    left: -30px;
  }

  &.trigger-atom {

    &:before,
    &:after {
      display: none;
    }
  }

  &:first-child {
    &:before {
      top: -16px;
    }
  }

  &:before {
    content: "";
    position: absolute;
    height: 14px;
    width: 2px;
    background: $fontLighterColor;
    top: -12px;
    left: 22px;
    z-index: 1;
  }

  &:after {
    content: "";
    position: absolute;
    height: 4px;
    width: 4px;
    border: 2px solid $fontLighterColor;
    border-radius: 50%;
    background: white !important;
    top: -5px;
    left: 19px;
    z-index: 2;
  }

  &.is-intercept {
    border-color: $warningColor;

    &:hover {
      border-color: $warningColor;
    }
  }

  &.is-error {
    border-color: $dangerColor;
    color: $dangerColor;

    &:hover {
      .atom-invalid-icon {
        display: none;
      }
    }

    .atom-invalid-icon {
      margin: 0 12px;
    }
  }

  &:not(.readonly):hover {
    border-color: $primaryColor;

    .atom-icon.skip-icon {
      color: $fontLighterColor;
    }

    .atom-icon,
    .atom-name {
      color: $primaryColor;
    }

    .add-plus-icon.close,
    .copy {
      cursor: pointer;
      display: block;
    }
  }

  .atom-icon {
    text-align: center;
    margin: 0 14.5px;
    font-size: 18px;
    width: 18px;
    color: $fontWeightColor;
    fill: currentColor;
  }

  .atom-icon.skip-icon {
    color: $fontLighterColor;
  }

  .atom-name span.skip-name {
    text-decoration: line-through;
    color: $fontLighterColor;

    &:hover {
      color: $fontLighterColor;
    }
  }

  .pause-button {
    margin-right: 8px;
    color: $primaryColor;
  }

  .atom-retry-indicate-icon-group {
    display: flex;
    grid-gap: 6px;
    position: absolute;
    top: -9px;
    right: 10px;
    color: $primaryColor;

    .atom-retry-indicate-icon {
      width: 18px;
      height: 18px;
      background-color: white;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  .add-plus-icon.close {
    @include add-plus-icon(#fff, #fff, #c4c6cd, 16px, true);
    @include add-plus-icon-hover($dangerColor, $dangerColor, white);
    display: none;
    margin-right: 10px;
    border: none;
    transform: rotate(45deg);

    &:before,
    &:after {
      left: 7px;
      top: 4px;
    }
  }

  .copy {
    display: none;
    margin-right: 10px;
    color: $fontLighterColor;

    &:hover {
      color: $primaryColor;
    }
  }

  >.atom-name {
    flex: 1;
    color: $fontWeightColor;
    @include ellipsis();
    max-width: 188px;
    margin-right: 2px;

    span:hover {
      color: $primaryColor;
    }
  }

  .disabled {
    cursor: not-allowed;
    color: $fontLighterColor;
  }

  .atom-execounter {
    color: $primaryColor;
    font-size: 12px;
  }

  .atom-operate-area {
    margin: 0 8px 0 0;
    color: $primaryColor;
    font-size: 12px;
  }

  .atom-reviewing-tips {
    &[disabled] {
      cursor: not-allowed;
      color: #c3cdd7;
    }
  }

  .atom-review-diasbled-tips {
    color: #c3cdd7;
    margin: 0 8px 0 2px;
  }

  .atom-canskip-checkbox {
    margin-right: 6px;
  }

  &.quality-atom {
    display: flex;
    justify-content: center;
    border-color: transparent;
    height: 24px;
    background: transparent;
    border-color: transparent !important;
    font-size: 12px;

    &:before {
      height: 40px;
      z-index: 8;
    }

    &:after {
      display: none;
    }

    &.last-quality-atom {
      &:before {
        height: 22px;
      }
    }

    .atom-title {
      display: flex;
      width: 100%;
      align-items: center;
      justify-content: center;
      margin-left: 22px;

      >span {
        border-radius: 12px;
        font-weight: bold;
        border: 1px solid $fontLighterColor;
        padding: 0 12px;
        margin: 0 4px;
      }

      >i {
        height: 0;
        flex: 1;
        border-top: 2px dashed $fontLighterColor;
      }
    }

    .handler-list {
      position: absolute;
      right: 0;

      span {
        color: $primaryColor;
        font-size: 12px;

        &:first-child {
          margin-right: 5px;
        }
      }
    }

    .executing-job {
      position: absolute;
      top: 6px;
      right: 42px;

      &:before {
        display: inline-block;
        animation: rotating infinite 0.6s ease-in-out;
      }
    }

    .disabled-review span {
      color: $fontLighterColor;
      cursor: default;
    }
  }

  .add-plus-icon.insert-after {
    @include add-plus-icon($primaryColor, $primaryColor, white, 18px, true);
    @include add-plus-icon-hover($primaryColor, $primaryColor, white);
    display: none;
    position: absolute;
    bottom: -10px;
    left: 50%;
    transform: translateX(-50%);
    cursor: pointer;
    z-index: 10;

    &:hover {
      transform: translateX(-50%) scale(1.1);
    }
  }

  &:hover {
    .add-plus-icon.insert-after {
      display: block;
    }
  }

  &.readonly {
    background-color: white;

    .atom-name:hover {
      span {
        color: $fontWeightColor;
      }

      .skip-name {
        text-decoration: line-through;
        color: $fontLighterColor;
      }
    }

    &.quality-prev-atom {
      &:before {
        height: 24px;
        top: -23px;
      }
    }
  }
}
</style>
