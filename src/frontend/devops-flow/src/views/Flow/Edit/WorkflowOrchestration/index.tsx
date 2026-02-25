import { SvgIcon } from '@/components/SvgIcon'
import AtomPropertyPanel from '@/components/WorkflowOrchestration/AtomPropertyPanel'
import AtomSelector from '@/components/WorkflowOrchestration/AtomSelector'
import JobPropertyPanel from '@/components/WorkflowOrchestration/JobPropertyPanel'
import StagePropertyPanel from '@/components/WorkflowOrchestration/StagePropertyPanel'
import { useFlowModel } from '@/hooks/useFlowModel'
import { useUIStore } from '@/stores/ui'
import 'bkui-pipeline/dist/bk-pipeline.css'
import BkPipeline from 'bkui-pipeline/vue3'
import { Exception, Loading } from 'bkui-vue'
import { defineComponent, ref, watch, onBeforeUnmount } from 'vue'
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
    } = useFlowModel()
    const uiStore = useUIStore()

    // ========== Refs ==========
    const isStagePanelVisible = ref(false)
    const isJobPanelVisible = ref(false)
    const isAtomPanelVisible = ref(false)
    const isAtomSelectorVisible = ref(false)

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
        // 如果任何一个侧边栏打开，则自动收起变量面板
        if (isStageOpen || isJobOpen || isAtomOpen || isAtomSelectorOpen) {
          if (uiStore.isVariablePanelOpen) {
            uiStore.setVariablePanelOpen(false)
          }
        }
      },
    )

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
      </Loading>
    )
  },
})
