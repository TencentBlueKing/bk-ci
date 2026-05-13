import type { AtomModal } from '@/api/atom'
import type { Container, Element } from '@/api/flowModel'
import {
  apiTransfer,
  taskInsertYaml,
  taskToYaml,
  yamlNavToFlowModel,
} from '@/api/flowContentList'
import CodeEditor from '@/components/CodeEditor'
import AtomPropertyPanel from '@/components/WorkflowOrchestration/AtomPropertyPanel'
import AtomSelector from '@/components/WorkflowOrchestration/AtomSelector'
import { DEFAULT_VERSION } from '@/hooks/useAtomVersion'
import { useAtomStore } from '@/stores/atom'
import { useFlowModelStore } from '@/stores/flowModel'
import {
  diffAtomVersions,
  getAtomDefaultValue,
  getAtomOutputObj,
  isNewAtomTemplate,
} from '@/utils/atom'
import { createDefaultContainer, createDefaultElement } from '@/utils/flowDefaults'
import { randomLenString } from '@/utils/util'
import { Button, Loading, Message } from 'bkui-vue'
import { computed, defineComponent, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import styles from './CodeModeEditor.module.css'

interface CodeEditorPos {
  stageIndex: number
  containerIndex: number
  elementIndex: number
}

interface EditingState {
  pos: CodeEditorPos
  isInsert: boolean
  cursor?: { line: number; column: number } | null
}

export default defineComponent({
  name: 'CodeModeEditor',
  setup() {
    const { t } = useI18n()
    const route = useRoute()
    const projectId = computed(() => route.params.projectId as string)
    const pipelineId = computed(() => route.params.flowId as string)

    const flowModelStore = useFlowModelStore()
    const atomStore = useAtomStore()

    const editorRef = ref<InstanceType<typeof CodeEditor> | null>(null)

    const editingElement = ref<Element | null>(null)
    const editingContainer = ref<Container | null>(null)
    const editingState = ref<EditingState | null>(null)

    const isAtomPanelVisible = ref(false)
    const isAtomSelectorVisible = ref(false)

    const isAdding = ref(false)
    const isApplying = ref(false)
    const isPreviewing = ref(false)
    const isOpeningPanel = ref(false)
    const previewYamlContent = ref('')
    const isPreviewVisible = ref(false)

    // 任意需要在 editor 区域阻塞用户交互的耗时请求
    const isEditorBusy = computed(
      () => isOpeningPanel.value || isApplying.value || isAdding.value,
    )

    const yamlContent = computed(() => flowModelStore.yamlContent)

    function handleYamlChange(yaml: string) {
      flowModelStore.updateYamlContent(yaml)
    }

    function resetEditing() {
      isAtomPanelVisible.value = false
      isAtomSelectorVisible.value = false
      isPreviewVisible.value = false
      editingElement.value = null
      editingContainer.value = null
      editingState.value = null
      previewYamlContent.value = ''
    }

    watch(isAtomPanelVisible, (visible) => {
      if (!visible) {
        isAtomSelectorVisible.value = false
        isPreviewVisible.value = false
        editingElement.value = null
        editingContainer.value = null
        editingState.value = null
        previewYamlContent.value = ''
      }
    })

    async function loadFlowModelFromYaml() {
      try {
        const res = await apiTransfer({
          projectId: projectId.value,
          pipelineId: pipelineId.value,
          actionType: 'FULL_YAML2MODEL',
          oldYaml: yamlContent.value,
        })
        return res.modelAndSetting?.model ?? null
      } catch (error) {
        console.error('Failed to transfer yaml to model:', error)
        return null
      }
    }

    async function handleAddPlugin() {
      if (isOpeningPanel.value) return
      const editor = editorRef.value
      if (!editor) return

      const pos = (editor as any).getPosition?.()
      if (!pos) return

      let stageIndex = 0
      let containerIndex = 0
      let stepIndex = -1

      try {
        isOpeningPanel.value = true
        try {
          const navRes = await yamlNavToFlowModel({
            projectId: projectId.value,
            yaml: yamlContent.value,
            line: pos.lineNumber,
            column: pos.column,
          })
          stageIndex = navRes.stageIndex ?? 0
          containerIndex = navRes.containerIndex ?? 0
          stepIndex = navRes.stepIndex ?? -1
        } catch (error) {
          console.error('Failed to nav yaml position:', error)
          Message({ theme: 'warning', message: t('flow.content.yamlPositionFailed') })
        }

        const model = await loadFlowModelFromYaml()
        const stage = model?.stages?.[stageIndex] ?? null
        const container =
          stage?.containers?.[containerIndex] ?? createDefaultContainer(containerIndex)

        const newElementIndex = (stepIndex ?? -1) + 1
        const placeholder = createDefaultElement(newElementIndex)

        editingContainer.value = container
        editingElement.value = placeholder
        editingState.value = {
          pos: {
            stageIndex,
            containerIndex,
            elementIndex: newElementIndex,
          },
          isInsert: true,
          cursor: { line: pos.lineNumber, column: pos.column },
        }

        isAtomPanelVisible.value = true
        isAtomSelectorVisible.value = true
      } finally {
        isOpeningPanel.value = false
      }
    }

    async function handleStepClick(
      pos: CodeEditorPos,
      atomFromYaml: Record<string, any> | null,
    ) {
      if (isOpeningPanel.value) return
      try {
        isOpeningPanel.value = true

        let elementToEdit: Element | null = null
        let containerToEdit: Container | null = null

        const model = await loadFlowModelFromYaml()
        if (model) {
          const stage = model.stages?.[pos.stageIndex + 1]
          const container = stage?.containers?.[pos.containerIndex] ?? null
          const element = container?.elements?.[pos.elementIndex] ?? null
          elementToEdit = element
          containerToEdit = container
        }

        if (!elementToEdit && atomFromYaml) {
          elementToEdit = createDefaultElement(pos.elementIndex, atomFromYaml as Partial<Element>)
        }

        if (!elementToEdit) {
          Message({ theme: 'warning', message: t('flow.content.yamlPositionFailed') })
          return
        }

        editingContainer.value = containerToEdit
        editingElement.value = elementToEdit
        editingState.value = {
          pos: {
            stageIndex: pos.stageIndex + 1,
            containerIndex: pos.containerIndex,
            elementIndex: pos.elementIndex,
          },
          isInsert: false,
          cursor: null,
        }

        isAtomPanelVisible.value = true
      } finally {
        isOpeningPanel.value = false
      }
    }

    function buildElementFromAtomModal(
      atomCode: string,
      version: string,
      atomModal: AtomModal,
      previousElement: Element | null,
    ): Element {
      const finalVersion = version || DEFAULT_VERSION
      const isChangeAtom = !previousElement || previousElement.atomCode !== atomCode
      const htmlTemplateVersion = atomModal.htmlTemplateVersion
      const isNewTemplate = isNewAtomTemplate(htmlTemplateVersion)
      const atomProps = atomModal.props || {}

      let element: Element

      if (isNewTemplate) {
        const preVerData = (previousElement?.data as any) || {}
        const atomInputProps = (atomProps.input as Record<string, any>) || {}

        const diffRes = diffAtomVersions(
          (preVerData.input as Record<string, any>) || {},
          {},
          atomInputProps,
          isChangeAtom,
        )

        const mergedInput = {
          ...getAtomDefaultValue(atomInputProps),
          ...diffRes.atomValue,
        }

        const outputObj = getAtomOutputObj(atomProps.output || {})
        const elementIndex = editingState.value?.pos.elementIndex ?? 0

        element = createDefaultElement(elementIndex, {
          id: previousElement?.id || `element-${randomLenString(4)}`,
          '@type':
            atomModal.classType && atomModal.classType !== atomCode
              ? atomModal.classType
              : atomCode,
          atomCode,
          name: isChangeAtom ? atomModal.name : previousElement!.name,
          version: finalVersion,
          classType: atomModal.classType || atomCode,
          data: {
            input: mergedInput,
            output: outputObj,
            namespace: isChangeAtom ? '' : preVerData.namespace || '',
            config: atomProps.config || {},
          } as any,
        })
      } else {
        const diffRes = diffAtomVersions(
          (previousElement as Record<string, any>) || {},
          {},
          atomProps,
          isChangeAtom,
        )
        const mergedProps = {
          ...getAtomDefaultValue(atomProps),
          ...diffRes.atomValue,
        }
        const elementIndex = editingState.value?.pos.elementIndex ?? 0

        element = createDefaultElement(elementIndex, {
          id: previousElement?.id || `element-${randomLenString(4)}`,
          '@type':
            atomModal.classType && atomModal.classType !== atomCode
              ? atomModal.classType
              : atomCode,
          atomCode,
          version: finalVersion,
          name: isChangeAtom ? atomModal.name : previousElement!.name,
          ...mergedProps,
        })
      }

      const runtime = element as any
      if (atomModal.logoUrl) runtime.logoUrl = atomModal.logoUrl
      if (atomModal.os) runtime.os = atomModal.os
      if (atomModal.buildLessRunFlag !== undefined) {
        runtime.buildLessRunFlag = atomModal.buildLessRunFlag
      }

      atomStore.setAtomModal(atomCode, finalVersion, atomModal)
      return element
    }

    function handleAtomSelect({
      atomCode,
      version,
      atomModal,
    }: {
      atomCode: string
      version?: string
      atomModal?: AtomModal
    }) {
      if (!atomModal) return
      const next = buildElementFromAtomModal(
        atomCode,
        version || DEFAULT_VERSION,
        atomModal,
        editingElement.value,
      )
      editingElement.value = next
    }

    function handleUpdateAtom(element: Element) {
      editingElement.value = element
    }

    function handleChooseAtom() {
      isAtomSelectorVisible.value = true
    }

    async function handleAppendToYaml() {
      const state = editingState.value
      if (!state || !editingElement.value || !state.cursor) return
      try {
        isAdding.value = true
        const res = await taskInsertYaml({
          projectId: projectId.value,
          pipelineId: pipelineId.value,
          line: state.cursor.line,
          column: state.cursor.column,
          yaml: yamlContent.value,
          data: editingElement.value,
          type: 'INSERT',
        })
        if (res?.yaml) {
          flowModelStore.updateYamlContent(res.yaml)
        }
        resetEditing()
      } catch (error: any) {
        Message({
          theme: 'error',
          message: error?.message || t('flow.content.yamlInsertFailed'),
        })
      } finally {
        isAdding.value = false
      }
    }

    async function handleApplyToYaml() {
      const state = editingState.value
      const editor = editorRef.value
      if (!state || !editingElement.value || !editor) return
      try {
        isApplying.value = true
        const yaml = await taskToYaml({
          projectId: projectId.value,
          pipelineId: pipelineId.value,
          data: editingElement.value,
        })
        if (yaml) {
          ;(editor as any).insertFragmentAtPos?.(yaml, {
            stageIndex: state.pos.stageIndex - 1,
            containerIndex: state.pos.containerIndex,
            elementIndex: state.pos.elementIndex,
          })
        }
        resetEditing()
      } catch (error: any) {
        Message({
          theme: 'error',
          message: error?.message || t('flow.content.yamlInsertFailed'),
        })
      } finally {
        isApplying.value = false
      }
    }

    async function handlePreviewYaml() {
      if (!editingElement.value) return
      try {
        isPreviewing.value = true
        const yaml = await taskToYaml({
          projectId: projectId.value,
          pipelineId: pipelineId.value,
          data: editingElement.value,
        })
        previewYamlContent.value = yaml || ''
        isPreviewVisible.value = true
      } catch (error: any) {
        Message({
          theme: 'error',
          message: error?.message || t('flow.content.yamlPreviewFailed'),
        })
      } finally {
        isPreviewing.value = false
      }
    }

    function handleClosePreview() {
      isPreviewVisible.value = false
      previewYamlContent.value = ''
    }

    function handleCancel() {
      resetEditing()
    }

    const canPreview = computed(() => !!editingElement.value?.atomCode)

    return () => (
      <div class={styles.codeModeEditor}>
        <header class={styles.toolbar}>
          <Button
            loading={isOpeningPanel.value}
            disabled={isEditorBusy.value}
            onClick={handleAddPlugin}
          >
            {t('flow.content.addPlugin')}
          </Button>
        </header>

        <Loading loading={isEditorBusy.value} class={styles.editorWrapper}>
          <CodeEditor
            ref={editorRef as any}
            modelValue={yamlContent.value}
            height="100%"
            codeLensTitle={t('flow.content.editStep')}
            onUpdate:modelValue={handleYamlChange}
            onStep-click={handleStepClick}
          />

          {isPreviewVisible.value && (
            <div class={styles.previewPopup}>
              <header class={styles.previewHeader}>
                <span>{t('flow.content.previewYaml')}</span>
                <span class={styles.previewClose} onClick={handleClosePreview}>
                  ×
                </span>
              </header>
              <div class={styles.previewContent}>
                <CodeEditor
                  modelValue={previewYamlContent.value}
                  readOnly={true}
                  height="100%"
                  fileUri="task-preview.yml"
                />
              </div>
            </div>
          )}
        </Loading>

        <AtomSelector
          v-model:visible={isAtomSelectorVisible.value}
          container={editingContainer.value ?? undefined}
          atom={editingElement.value ?? undefined}
          onSelect={handleAtomSelect}
        />

        <AtomPropertyPanel
          v-model:visible={isAtomPanelVisible.value}
          currentElement={editingElement.value}
          editable={true}
          onChooseAtom={handleChooseAtom}
          onUpdateAtom={handleUpdateAtom}
        >
          {{
            footer: () => (
              <div class={styles.panelFooter}>
                {editingState.value?.isInsert ? (
                  <Button
                    theme="primary"
                    loading={isAdding.value}
                    disabled={isAdding.value || !editingElement.value?.atomCode}
                    onClick={handleAppendToYaml}
                  >
                    {t('flow.content.append')}
                  </Button>
                ) : (
                  <Button
                    loading={isApplying.value}
                    disabled={isApplying.value || !editingElement.value?.atomCode}
                    onClick={handleApplyToYaml}
                  >
                    {t('flow.content.applyToYaml')}
                  </Button>
                )}
                <Button
                  loading={isPreviewing.value}
                  disabled={!canPreview.value || isPreviewing.value || isAdding.value}
                  onClick={handlePreviewYaml}
                >
                  {t('flow.content.previewYaml')}
                </Button>
                <Button
                  disabled={isAdding.value || isApplying.value}
                  onClick={handleCancel}
                >
                  {t('flow.common.cancel')}
                </Button>
              </div>
            ),
          }}
        </AtomPropertyPanel>
      </div>
    )
  },
})
