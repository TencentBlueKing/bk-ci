<template>
  <VueDraggable
    v-model="computedStages"
    v-bind="dragOptions"
    :move="checkMove"
    class="bk-pipeline"
  >
    <Stage
      class="list-item"
      v-for="(stage, index) in computedStages"
      :ref="(el) => setStageRef(el, stage.id)"
      :key="stage.id"
      :editable="editable"
      :stage="stage"
      :is-preview="isPreview"
      :is-exec-detail="isExecDetail"
      :has-finally-stage="hasFinallyStage"
      :stage-index="index"
      :cancel-user-id="cancelUserId"
      :handle-change="updatePipeline"
      :is-latest-build="isLatestBuild"
      :stage-length="computedStages.length"
      :containers="stage.containers"
      :match-rules="matchRules"
      @[COPY_EVENT_NAME_VALUE]="handleCopyStage"
      @[DELETE_EVENT_NAME_VALUE]="handleDeleteStage"
    >
    </Stage>
  </VueDraggable>
</template>

<script setup>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  provide,
  ref,
} from "vue";
import { VueDraggable } from "vue-draggable-plus";
import {
  ADD_STAGE,
  APPEND_JOB,
  ATOM_ADD_EVENT_NAME,
  ATOM_CONTINUE_EVENT_NAME,
  ATOM_EXEC_EVENT_NAME,
  ATOM_QUALITY_CHECK_EVENT_NAME,
  ATOM_REVIEW_EVENT_NAME,
  CLICK_EVENT_NAME,
  COPY_EVENT_NAME,
  DEBUG_CONTAINER,
  DELETE_EVENT_NAME,
  STAGE_CHECK,
  STAGE_RETRY,
} from "./constants";
import Stage from "./Stage";
import { eventBus, hashID, isTriggerContainer } from "./util";

// 定义 emits - 必须在 defineProps 之前，且不能引用局部变量
const emit = defineEmits([
  "input",
  "change",
  CLICK_EVENT_NAME,
  DELETE_EVENT_NAME,
  ATOM_REVIEW_EVENT_NAME,
  ATOM_CONTINUE_EVENT_NAME,
  ATOM_EXEC_EVENT_NAME,
  ATOM_QUALITY_CHECK_EVENT_NAME,
  ATOM_ADD_EVENT_NAME,
  ADD_STAGE,
  STAGE_CHECK,
  STAGE_RETRY,
  DEBUG_CONTAINER,
  APPEND_JOB,
]);

const customEvents = [
  CLICK_EVENT_NAME,
  DELETE_EVENT_NAME,
  ATOM_REVIEW_EVENT_NAME,
  ATOM_CONTINUE_EVENT_NAME,
  ATOM_EXEC_EVENT_NAME,
  ATOM_QUALITY_CHECK_EVENT_NAME,
  ATOM_ADD_EVENT_NAME,
  ADD_STAGE,
  STAGE_CHECK,
  STAGE_RETRY,
  DEBUG_CONTAINER,
  APPEND_JOB,
];

const props = defineProps({
  editable: {
    type: Boolean,
    default: true,
  },
  isPreview: {
    type: Boolean,
    default: false,
  },
  currentExecCount: {
    type: Number,
    default: 1,
  },
  isExecDetail: {
    type: Boolean,
    default: false,
  },
  isLatestBuild: {
    type: Boolean,
    default: false,
  },
  canSkipElement: {
    type: Boolean,
    default: false,
  },
  pipeline: {
    type: Object,
    required: true,
  },
  cancelUserId: {
    type: String,
    default: "unknow",
  },
  userName: {
    type: String,
    default: "unknow",
  },
  matchRules: {
    type: Array,
    default: () => [],
  },
  isExpandAllMatrix: {
    type: Boolean,
    default: true,
  },
  isCreativeStream: {
    type: Boolean,
    default: false,
  },
});

// 使用 ref 存储 stage refs
const stageRefs = ref({});

// provide 响应式数据 - Vue 2.7 和 Vue 3 兼容
// 使用 Object.defineProperty 创建响应式代理，在 Vue 2.7 和 Vue 3 中都能正常工作
// Vue 2.7: Object.defineProperty 配合 Vue 的响应式系统
// Vue 3: 虽然推荐使用 reactive，但 Object.defineProperty 仍然可以工作
const reactiveData = {};
const keys = [
  "currentExecCount",
  "isPreview",
  "userName",
  "matchRules",
  "editable",
  "isExecDetail",
  "isLatestBuild",
  "canSkipElement",
  "cancelUserId",
  "isExpandAllMatrix",
  "isCreativeStream"
];

keys.forEach((key) => {
  Object.defineProperty(reactiveData, key, {
    enumerable: true,
    get: () => props[key],
    configurable: true,
  });
});

provide("reactiveData", reactiveData);
provide("emitPipelineChange", () => {
  emitPipelineChange(props.pipeline);
});

const DELETE_EVENT_NAME_VALUE = DELETE_EVENT_NAME;
const COPY_EVENT_NAME_VALUE = COPY_EVENT_NAME;

const computedStages = computed({
  get() {
    return props.pipeline?.stages ?? [];
  },
  set(stages) {
    const data = stages.map((stage, index) => {
      const name = `stage-${index + 1}`;
      const id = `s-${hashID()}`;
      if (!stage.containers) {
        return {
          id,
          name,
          containers: [stage],
        };
      }
      return stage;
    });
    updatePipeline(props.pipeline, {
      stages: data.filter((stage) => stage.containers.length),
    });
  },
});

const dragOptions = computed(() => {
  return {
    group: "pipeline-stage",
    handle: ".pipeline-stage-entry",
    ghostClass: "sortable-ghost-atom",
    chosenClass: "sortable-chosen-atom",
    animation: 130,
    disabled: !props.editable,
  };
});

const hasFinallyStage = computed(() => {
  try {
    const stageLength = computedStages.value.length;
    const last = computedStages.value[stageLength - 1];
    return last.finally;
  } catch (error) {
    return false;
  }
});

const emitPipelineChange = (newVal) => {
  emit("input", newVal);
  emit("change", newVal);
};

const registeCustomEvent = (destory = false) => {
  customEvents.forEach((eventName) => {
    const fn = (destory ? eventBus.$off : eventBus.$on).bind(eventBus);
    fn(eventName, (...args) => {
      emit(eventName, ...args);
    });
  });
};

const checkIsTriggerStage = (stage) => {
  try {
    return isTriggerContainer(stage.containers[0]);
  } catch (e) {
    return false;
  }
};

const updatePipeline = (model, params) => {
  // Apply updates to the model object
  Object.assign(model, params);
  
  // Determine if model is a stage or container by checking for containers array
  const isStage = Array.isArray(model.containers);
  
  // Get unique identifier for matching
  const modelId = model.id || model.containerId;
  
  let newStages;
  if (isStage) {
    // model is a stage - find by id and replace with updated version
    newStages = props.pipeline.stages.map(stage => 
      (stage.id === modelId) ? model : stage
    );
  } else {
    // model is a container - find by containerId and update it in the correct stage
    newStages = props.pipeline.stages.map(stage => {
      const containerIndex = stage.containers?.findIndex(
        c => (c.containerId === modelId || c.id === modelId)
      );
      if (containerIndex !== -1 && containerIndex !== undefined) {
        const newContainers = stage.containers.map((c, idx) => 
          idx === containerIndex ? model : c
        );
        return { ...stage, containers: newContainers };
      }
      return stage;
    });
  }
  
  // Emit a new pipeline object with the updated stages
  emitPipelineChange({
    ...props.pipeline,
    stages: newStages,
  });
};

const checkMove = (event) => {
  const dragContext = event.draggedContext || {};
  const element = dragContext.element || {};
  const isTrigger = element.containers[0]?.["@type"] === "trigger";
  const isFinally = element.finally === true;

  const relatedContext = event.relatedContext || {};
  const relatedelement = relatedContext.element || {};
  const isRelatedTrigger = relatedelement["@type"] === "trigger";

  const isTriggerStage = checkIsTriggerStage(relatedelement);
  const isRelatedFinally = relatedelement.finally === true;

  return (
    !isTrigger &&
    !isRelatedTrigger &&
    !isTriggerStage &&
    !isFinally &&
    !isRelatedFinally
  );
};

const handleCopyStage = ({ stageIndex, stage }) => {
  const newStages = [...props.pipeline.stages];
  newStages.splice(stageIndex + 1, 0, stage);
  emitPipelineChange({
    ...props.pipeline,
    stages: newStages,
  });
};

const handleDeleteStage = (stageId) => {
  const newStages = props.pipeline.stages.filter(
    (stage) => stage.id !== stageId
  );
  emitPipelineChange({
    ...props.pipeline,
    stages: newStages,
  });
};

/**
 * 获取 ref 实例（处理 Vue 2.7 中 ref 可能是数组的情况）
 */
const getRefInstance = (ref, key) => {
  if (!ref || !key) return null;
  const refValue = ref[key];
  return Array.isArray(refValue) ? refValue[0] : refValue;
};

/**
 * 获取 Stage 组件实例
 */
const getStageInstance = (stageId) => {
  const stageRef = stageRefs.value[stageId];
  if (!stageRef || !stageRef[0]) {
    console.warn("Stage instance not found:", stageId);
    return null;
  }
  return stageRef[0];
};

/**
 * 获取 StageContainer 组件实例
 */
const getContainerInstance = (stageInstance, containerId) => {
  if (!stageInstance || !containerId) return null;
  const containerInstance = getRefInstance(stageInstance.$refs, containerId);
  if (!containerInstance) {
    console.warn("Container instance not found:", containerId);
    return null;
  }
  return containerInstance;
};

/**
 * 获取 Job 或 MatrixGroup 实例（从 StageContainer 的 jobBox ref）
 */
const getJobOrMatrixInstance = (containerInstance) => {
  if (!containerInstance) return null;
  const instance = containerInstance.jobBox;
  if (!instance) {
    console.warn("Job or MatrixGroup instance not found in container");
    return null;
  }
  return instance;
};

/**
 * 从 MatrixGroup 中获取 Job 实例
 */
const getJobFromMatrixGroup = (matrixGroupInstance, containerId) => {
  if (!matrixGroupInstance || !containerId) return null;
  const jobInstance = getRefInstance(matrixGroupInstance.$refs, containerId);
  if (!jobInstance) {
    console.warn("Job instance not found in matrix group:", containerId);
    return null;
  }
  return jobInstance;
};

const expandPostAction = (stageId, matrixId, containerId) => {
  return new Promise((resolve) => {
    try {
      const stageInstance = getStageInstance(stageId);
      if (!stageInstance) {
        resolve(false);
        return;
      }
      
      let jobInstance = null;
      
      if (matrixId) {
        // 如果有 matrixId，说明是矩阵组中的作业
        const containerInstance = getContainerInstance(stageInstance, matrixId);
        if (containerInstance) {
          const matrixGroupInstance = getJobOrMatrixInstance(containerInstance);
          if (matrixGroupInstance) {
            jobInstance = getJobFromMatrixGroup(matrixGroupInstance, containerId);
          }
        }
      } else {
        // 普通作业
        const containerInstance = getContainerInstance(stageInstance, containerId);
        if (containerInstance) {
          jobInstance = getJobOrMatrixInstance(containerInstance);
        }
      }
      
      if (!jobInstance) {
        console.warn("Job instance not found:", { stageId, matrixId, containerId });
        resolve(false);
        return;
      }
      
      // 访问 Job 组件内部的 atomList ref
      const atomListInstance = getRefInstance(jobInstance.$refs, 'atomList');
      
      if (atomListInstance && typeof atomListInstance.expandPostAction === 'function') {
        atomListInstance.expandPostAction();
        nextTick(() => {
          resolve(true);
        });
      } else {
        console.warn("atomList or expandPostAction not found");
        resolve(false);
      }
    } catch (error) {
      console.error("expandPostAction error:", error);
      resolve(false);
    }
  });
};

const expandMatrix = (stageId, matrixId, containerId, expand = true) => {
  console.log("expandMatrix", stageId, matrixId, containerId, expand);
  return new Promise((resolve) => {
    try {
      const stageInstance = getStageInstance(stageId);
      if (!stageInstance) {
        resolve(false);
        return;
      }
      
      const containerInstance = getContainerInstance(stageInstance, matrixId);
      if (!containerInstance) {
        resolve(false);
        return;
      }
      
      const matrixGroupInstance = getJobOrMatrixInstance(containerInstance);
      if (!matrixGroupInstance) {
        console.warn("MatrixGroup instance not found in container:", matrixId);
        resolve(false);
        return;
      }
      
      // 调用 toggleMatrixOpen 方法展开/收起矩阵
      if (typeof matrixGroupInstance.toggleMatrixOpen === 'function') {
        matrixGroupInstance.toggleMatrixOpen(expand);
      } else {
        console.warn("toggleMatrixOpen method not found on matrix group instance");
        resolve(false);
        return;
      }
      
      // 如果展开矩阵，还需要展开内部的 Job
      if (expand && containerId) {
        nextTick(() => {
          try {
            const jobInstance = getJobFromMatrixGroup(matrixGroupInstance, containerId);
            if (jobInstance && typeof jobInstance.toggleShowAtom === 'function') {
              jobInstance.toggleShowAtom(expand);
            }
          } catch (err) {
            console.warn("Failed to expand job in matrix:", err);
          }
          resolve(true);
        });
      } else {
        resolve(true);
      }
    } catch (error) {
      console.error("expandMatrix error:", error);
      resolve(false);
    }
  });
};

const expandJob = (stageId, containerId, expand = true) => {
  console.log("expandJob", stageId, containerId, expand);
  return new Promise((resolve) => {
    try {
      const stageInstance = getStageInstance(stageId);
      if (!stageInstance) {
        resolve(false);
        return;
      }
      
      const containerInstance = getContainerInstance(stageInstance, containerId);
      if (!containerInstance) {
        resolve(false);
        return;
      }
      
      const jobInstance = getJobOrMatrixInstance(containerInstance);
      if (!jobInstance) {
        console.warn("Job instance not found in container:", containerId);
        resolve(false);
        return;
      }
      
      // 调用 toggleShowAtom 方法
      if (typeof jobInstance.toggleShowAtom === 'function') {
        jobInstance.toggleShowAtom(expand);
        resolve(true);
      } else {
        console.warn("toggleShowAtom method not found on job instance");
        resolve(false);
      }
    } catch (error) {
      console.error("expandJob error:", error);
      resolve(false);
    }
  });
};

// 设置 ref 的回调函数
const setStageRef = (el, stageId) => {
  if (el) {
    if (!stageRefs.value[stageId]) {
      stageRefs.value[stageId] = [];
    }
    stageRefs.value[stageId].push(el);
  }
};

onMounted(() => {
  registeCustomEvent();
});

onBeforeUnmount(() => {
  window.showLinuxTipYet = false;
  registeCustomEvent(true);
});

// 暴露方法供外部调用
defineExpose({
  expandPostAction,
  expandMatrix,
  expandJob,
});
</script>

<style lang="scss">
.bk-pipeline {
  display: flex;
  padding-right: 120px;
  width: fit-content;
  position: relative;
  align-items: flex-start;
  ul,
  li {
    margin: 0;
    padding: 0;
  }
}

.list-item {
  transition: transform 0.2s ease-out;
}

.list-enter, .list-leave-to
        /* .list-complete-leave-active for below version 2.1.8 */ {
  opacity: 0;
  transform: translateY(36px) scale(0, 1);
}

.list-leave-active {
  position: absolute !important;
}
</style>
