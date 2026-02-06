import type { Container, Element, Stage } from '@/api/flowModel'
import CodeEditor from '@/components/CodeEditor'
import EmptyPage from '@/components/EmptyPage/index'
import ModeSwitch from '@/components/ModeSwitch'
import AtomPropertyPanel from '@/components/WorkflowOrchestration/AtomPropertyPanel'
import JobPropertyPanel from '@/components/WorkflowOrchestration/JobPropertyPanel'
import StagePropertyPanel from '@/components/WorkflowOrchestration/StagePropertyPanel'
import { useFlowModel } from '@/hooks/useFlowModel'
import { useModeStore } from '@/stores/flowMode'
import { useUIStore } from '@/stores/ui'
import layoutStyles from '@/styles/layout.module.css'
import VariablePanel from '@/views/Flow/Edit/VariablePanel'
import type { ClickEventPayload } from 'bkui-pipeline'
import { Loading } from 'bkui-vue'
import { storeToRefs } from 'pinia'
import { defineComponent, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import styles from './FlowModel.module.css'

import 'bkui-pipeline/dist/bk-pipeline.css'
import BkPipeline from 'bkui-pipeline/vue3'

export default defineComponent({
  name: 'FlowModel',
  setup(props, { emit }) {
    const { t } = useI18n()
    const route = useRoute()
    const modeStore = useModeStore()
    const uiStore = useUIStore()
    const { isVariablePanelOpen } = storeToRefs(uiStore)

    // 使用 useFlowModel hook 管理数据
    const {
      flowModelWithoutTriggerStage,
      yamlContent,
      loading,
      isFlowEmpty,
      flowModel,
      flowSetting,
    } = useFlowModel()

    // ========== 侧边栏状态 ==========
    const isStagePanelVisible = ref(false)
    const isJobPanelVisible = ref(false)
    const isAtomPanelVisible = ref(false)

    const selectedStage = ref<Stage | null>(null)
    const selectedJob = ref<Container | null>(null)
    const selectedJobStage = ref<Stage | null>(null)
    const selectedJobIndex = ref(-1)
    const selectedAtom = ref<Element | null>(null)

    // 流水线 ID
    const flowId = route.params.flowId as string

    // ========== 点击处理 ==========
    const handleFlowClick = (payload: ClickEventPayload) => {
      const { stageIndex = -1, containerIndex, elementIndex, atomIndex } = payload
      const realElementIndex = elementIndex !== undefined ? elementIndex : atomIndex

      if (stageIndex === -1 || !flowModelWithoutTriggerStage.value) return

      // 关闭所有面板
      isStagePanelVisible.value = false
      isJobPanelVisible.value = false
      isAtomPanelVisible.value = false

      const stages = flowModelWithoutTriggerStage.value.stages || []
      const stage = stages[stageIndex]
      if (!stage) return

      // 点击插件
      if (realElementIndex !== undefined && realElementIndex !== -1 && containerIndex !== undefined) {
        const container = stage.containers?.[containerIndex]
        const element = container?.elements?.[realElementIndex]
        if (element) {
          selectedAtom.value = element
          isAtomPanelVisible.value = true
        }
        return
      }

      // 点击 Job
      if (containerIndex !== undefined && containerIndex !== -1) {
        const container = stage.containers?.[containerIndex]
        if (container) {
          selectedJob.value = container
          selectedJobStage.value = stage
          selectedJobIndex.value = containerIndex
          isJobPanelVisible.value = true
        }
        return
      }

      // 点击 Stage
      selectedStage.value = stage
      isStagePanelVisible.value = true
    }

    return () => (
      <div class={[styles.flowModel, isVariablePanelOpen.value && styles.withVariablePanel]}>
        {/* <ModeSwitch
          projectId={route.params.projectId as string}
          pipelineId={route.params.flowId as string}
          modelAndSetting={
            flowModel.value && flowSetting.value
              ? {
                  model: flowModel.value,
                  setting: flowSetting.value,
                }
              : undefined
          }
        /> */}

        <div class={layoutStyles.flexDetailContent}>
          {loading.value ? (
            <div class={layoutStyles.loadingWrapper}>
              <Loading loading size="small" mode="spin" theme="primary" />
            </div>
          ) : (
            <>
              {modeStore.isCodeMode ? (
                <div class={styles.codeEditorWrapper}>
                  <CodeEditor
                    modelValue={yamlContent.value}
                    readOnly={true}
                    height="100%"
                    codeLensTitle={t('flow.content.editStep')}
                  />
                </div>
              ) : (
                <div class={styles.uiModeWrapper}>
                  {!isFlowEmpty.value && flowModelWithoutTriggerStage.value ? (
                    <BkPipeline
                      editable={false}
                      isCreativeStream={true}
                      pipeline={flowModelWithoutTriggerStage.value}
                      onClick={handleFlowClick}
                    />
                  ) : (
                    <EmptyPage
                      title={t('flow.content.blankTemplateNoOrchestration')}
                      desc={t('flow.content.createToAddFirstStage')}
                    />
                  )}
                </div>
              )}
            </>
          )}
        </div>

        {/* Stage 属性面板 - 只读 */}
        <StagePropertyPanel
          v-model={isStagePanelVisible.value}
          stage={selectedStage.value}
          editable={false}
        />

        {/* Job 属性面板 - 只读 */}
        <JobPropertyPanel
          v-model={isJobPanelVisible.value}
          editingContainer={selectedJob.value}
          stage={selectedJobStage.value}
          containerIndex={selectedJobIndex.value}
          editable={false}
        />

        {/* Atom 属性面板 - 只读 */}
        <AtomPropertyPanel
          v-model:visible={isAtomPanelVisible.value}
          currentElement={selectedAtom.value}
          editable={false}
        />

        {/* 变量面板 - 只读 */}
        <VariablePanel
          v-model={isVariablePanelOpen.value}
          flowId={flowId}
          editable={false}
          onToggle={(isOpen: boolean) => uiStore.setVariablePanelOpen(isOpen)}
        />
      </div>
    )
  },
})
