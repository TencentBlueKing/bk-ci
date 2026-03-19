import { AtomRunCondition, JobRunCondition, StageRunCondition } from '@/utils/flowDefaults'

// 插件失败控制选项
export const getAtomFailControlList = (t: (key: string) => string) => [
  {
    id: 'continueWhenFailed',
    name: t('flow.orchestration.continueWhenFailed'),
  },
  {
    id: 'retryWhenFailed',
    name: t('flow.orchestration.automaticRetry'),
  },
  {
    id: 'MANUAL_RETRY',
    name: t('flow.orchestration.manualRetry'),
  },
]

// 插件运行条件选项
export const getAtomRunConditionList = (t: (key: string) => string) => [
  {
    value: AtomRunCondition.PRE_TASK_SUCCESS,
    label: t('flow.orchestration.atomPreSuc'),
  },
  {
    value: AtomRunCondition.PRE_TASK_FAILED_BUT_CANCEL,
    label: t('flow.orchestration.atomEvenFail'),
  },
  {
    value: AtomRunCondition.PRE_TASK_FAILED_EVEN_CANCEL,
    label: t('flow.orchestration.atomEvenCancel'),
  },
  {
    value: AtomRunCondition.PRE_TASK_FAILED_ONLY,
    label: t('flow.orchestration.atomOnlyFail'),
  },
  {
    value: AtomRunCondition.CUSTOM_VARIABLE_MATCH,
    label: t('flow.orchestration.varMatch'),
  },
  {
    value: AtomRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
    label: t('flow.orchestration.varNotMatch'),
  },
  {
    value: AtomRunCondition.CUSTOM_CONDITION_MATCH,
    label: t('flow.orchestration.customCondition'),
  },
]

// Stage 运行条件选项
export const getStageRunConditionList = (t: (key: string) => string) => [
  {
    id: StageRunCondition.AFTER_LAST_FINISHED,
    name: t('flow.orchestration.afterPrevStageSuccess'),
  },
  {
    id: StageRunCondition.CUSTOM_VARIABLE_MATCH,
    name: t('flow.orchestration.customVariableMatch'),
  },
  {
    id: StageRunCondition.CUSTOM_CONDITION_MATCH,
    name: t('flow.orchestration.customConditionMatch'),
  },
  {
    id: StageRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
    name: t('flow.orchestration.customVariableNotMatch'),
  },
]

// Job 运行条件选项
export const getJobRunConditionList = (t: (key: string) => string, isFinally?: boolean) => {
  if (isFinally) {
    // Finally阶段的Job运行条件选项
    return [
      {
        id: JobRunCondition.STAGE_RUNNING,
        name: t('flow.orchestration.stageRunning'),
      },
      {
        id: JobRunCondition.PRE_STAGE_SUCCESS,
        name: t('flow.orchestration.preStageSuccess'),
      },
      {
        id: JobRunCondition.PRE_STAGE_FAILED,
        name: t('flow.orchestration.preStageFail'),
      },
      {
        id: JobRunCondition.PRE_STAGE_CANCEL,
        name: t('flow.orchestration.preStageCancel'),
      },
    ]
  }
  // 普通阶段的Job运行条件选项
  return [
    {
      id: JobRunCondition.STAGE_RUNNING,
      name: t('flow.orchestration.stageRunning'),
    },
    {
      id: JobRunCondition.CUSTOM_VARIABLE_MATCH,
      name: t('flow.orchestration.customVariableMatch'),
    },
    {
      id: JobRunCondition.CUSTOM_VARIABLE_MATCH_NOT_RUN,
      name: t('flow.orchestration.customVariableNotMatch'),
    },
    {
      id: JobRunCondition.CUSTOM_CONDITION_MATCH,
      name: t('flow.orchestration.customConditionMatch'),
    },
  ]
}

// Job 依赖选项
export const getJobDependOnOptions = (t: (key: string) => string) => [
  { id: 'ID', name: t('flow.orchestration.selectDependentJob') },
  { id: 'NAME', name: t('flow.orchestration.inputDependentJobId') },
]
