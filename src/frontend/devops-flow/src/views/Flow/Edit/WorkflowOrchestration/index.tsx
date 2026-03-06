import { SvgIcon } from '@/components/SvgIcon'
import StageReviewEditPanel from '@/components/StageReviewEditPanel'
import AtomPropertyPanel from '@/components/WorkflowOrchestration/AtomPropertyPanel'
import AtomSelector from '@/components/WorkflowOrchestration/AtomSelector'
import JobPropertyPanel from '@/components/WorkflowOrchestration/JobPropertyPanel'
import StagePropertyPanel from '@/components/WorkflowOrchestration/StagePropertyPanel'
import { useFlowModel } from '@/hooks/useFlowModel'
import type { CheckConfig, Stage } from '@/types/flow'
import { useUIStore } from '@/stores/ui'
import 'bkui-pipeline/dist/bk-pipeline.css'
import BkPipeline from 'bkui-pipeline/vue3'
import { Exception, Loading } from 'bkui-vue'
import { defineComponent, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import sharedStyles from '../shared.module.css'
import styles from './index.module.css'

export default defineComponent({
  name: 'EditWorkflowOrchestration',
  setup() {
    // ========== Hooks ==========
    const { t } = useI18n()
    const {
      loading,
      flowModel,
      isEditingStage,
      isEditingJob,
      isEditingPlugin,
      editingElement,
      editingStage,
      editingContainer,
      isNewStage,
      isNewJob,
      editingContainerStage,
      editingElementContainer,
      editingContainerIndex,
      isEditingFinallyStage,
      hasFlowStages,
      flowModelWithoutTriggerStage,
      updateFlowModel,
      handleAddJob,
      handleAddAtom,
      handleFlowClick,
      handleAddStage,
      handlePipelineChange,
      updateAtom,
      handleAtomSelect,
      handleAddFirstStage,
      handleClosePanel,
      handleStageConfirm,
      handleStageChange,
      handleJobConfirm,
      updateJob,
    } = useFlowModel()
    const uiStore = useUIStore()

    // ========== Refs ==========
    const isStagePanelVisible = ref(false)
    const isJobPanelVisible = ref(false)
    const isAtomPanelVisible = ref(false)
    const isAtomSelectorVisible = ref(false)

    // Stage review edit state
    const isStageReviewEditVisible = ref(false)
    const stageReviewEditStage = ref<Stage | null>(null)
    const stageReviewEditCheckType = ref<'checkIn' | 'checkOut'>('checkIn')

    // ========== Computed ==========
    // (暂无 computed)

    // ========== Lifecycle Hooks ==========
    watch(
      () => isEditingStage.value,
      (val) => {
        if (val) isStagePanelVisible.value = true
      },
    )

    watch(
      () => isEditingJob.value,
      (val) => {
        if (val) isJobPanelVisible.value = true
      },
    )

    watch(
      () => isEditingPlugin.value,
      (val) => {
        if (val) {
          isAtomPanelVisible.value = true
          if (!editingElement.value?.atomCode) {
            handleChooseAtom()
          }
        }
      },
    )

    watch(isStagePanelVisible, (val) => {
      if (!val && isEditingStage.value) {
        handleClosePanel()
      }
    })

    watch(isJobPanelVisible, (val) => {
      if (!val && isEditingJob.value) {
        handleClosePanel()
      }
    })

    watch(isAtomPanelVisible, (val) => {
      if (!val && isEditingPlugin.value) {
        handleClosePanel()
      }
    })

    watch(
      () => [
        isStagePanelVisible.value,
        isJobPanelVisible.value,
        isAtomPanelVisible.value,
        isAtomSelectorVisible.value,
      ],
      ([isStageOpen, isJobOpen, isAtomOpen, isAtomSelectorOpen]) => {
        if (isStageOpen || isJobOpen || isAtomOpen || isAtomSelectorOpen) {
          if (uiStore.isVariablePanelOpen) {
            uiStore.setVariablePanelOpen(false)
          }
        }
      },
    )

    // ========== Stage Review Edit ==========
    function handleStageCheck(payload: any) {
      const { type = 'checkIn', stageIndex } = payload || {}
      if (!flowModel.value?.stages) return
      const stage = flowModel.value.stages[stageIndex + 1]
      if (stage) {
        stageReviewEditStage.value = stage
        stageReviewEditCheckType.value = type
        isStageReviewEditVisible.value = true
      }
    }

    function handleStageReviewChange(stage: Stage, checkType: 'checkIn' | 'checkOut', config: CheckConfig) {

      const stageIndex = flowModel.value?.stages?.findIndex((s) => s.id === stage.id)
      if (stageIndex !== undefined && stageIndex >= 0 && flowModel.value?.stages[stageIndex]) {
        flowModel.value.stages[stageIndex] = {
          ...flowModel.value.stages[stageIndex],
          [checkType]: config,
        }
        updateFlowModel(flowModel.value)
      }
    }

    function closeStageReviewEdit() {
      isStageReviewEditVisible.value = false
      stageReviewEditStage.value = null
    }

    // ========== Functions ==========
    function renderEmptyState() {
      return (
        <div class={styles.emptyFlowStage} onClick={handleAddFirstStage}>
          <SvgIcon name="add-small" />
          <span>{t('flow.orchestration.clickToAddStage')}</span>
        </div>
      )
    }

    function handleChooseAtom() {
      isAtomSelectorVisible.value = true
    }

    return () => (
      <Loading
        zIndex={1000}
        loading={loading.value}
        class={[sharedStyles.tabContainer, styles.workflowOrchestration]}
      >
        {hasFlowStages.value ? (
          <BkPipeline
            pipeline={flowModelWithoutTriggerStage.value!}
            isCreativeStream={true}
            onAppendJob={handleAddJob}
            onAddAtom={handleAddAtom}
            onClick={handleFlowClick}
            onAddStage={handleAddStage}
            onChange={handlePipelineChange}
            onStageCheck={handleStageCheck}
          />
        ) : flowModelWithoutTriggerStage.value ? (
          renderEmptyState()
        ) : (
          <Exception type="empty" scene="part">
            {t('flow.orchestration.noFlowStageTips')}
          </Exception>
        )}

        {/* Stage property panel */}
        <StagePropertyPanel
          v-model={isStagePanelVisible.value}
          stage={editingStage.value}
          editable={true}
          isNew={isNewStage.value}
          onChange={handleStageChange}
          onConfirm={handleStageConfirm}
        />

        {/* Job property panel */}
        <JobPropertyPanel
          v-model={isJobPanelVisible.value}
          editable={true}
          editingContainer={editingContainer.value}
          stage={editingContainerStage.value}
          containerIndex={editingContainerIndex.value}
          isNew={isNewJob.value}
          isFinally={isEditingFinallyStage.value}
          onConfirm={handleJobConfirm}
          onChange={updateJob}
        />

        {/* Atom property panel */}
        <AtomPropertyPanel
          v-model:visible={isAtomPanelVisible.value}
          currentElement={editingElement.value!}
          onChooseAtom={handleChooseAtom}
          onUpdateAtom={updateAtom}
        />

        {/* Atom selector */}
        <AtomSelector
          v-model:visible={isAtomSelectorVisible.value}
          container={editingElementContainer.value ?? undefined}
          atom={editingElement.value ?? undefined}
          onSelect={handleAtomSelect}
        />

        {/* Stage review edit panel */}
        <StageReviewEditPanel
          isShow={isStageReviewEditVisible.value}
          stage={stageReviewEditStage.value}
          checkType={stageReviewEditCheckType.value}
          onClose={closeStageReviewEdit}
          onChange={handleStageReviewChange}
        />
      </Loading>
    )
  },
})
