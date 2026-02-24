import type { Element } from '@/api/flowModel'
import { SvgIcon } from '@/components/SvgIcon'
import TriggerEventSelector from '@/components/TriggerEventSelector'
import TriggerPropertyPanel from '@/components/TriggerPropertyPanel'
import { useFlowModel } from '@/hooks/useFlowModel'
import { createDefaultElement } from '@/utils/flowDefaults'
import { Button, Message, Popover, Switcher, Table } from 'bkui-vue'
import { computed, defineComponent, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import type { TriggerBaseItem } from '../../../../api/trigger'
import sharedStyles from '../shared.module.css'
import styles from './TriggerEvents.module.css'

export default defineComponent({
  name: 'EditTriggerEvents',
  setup() {
    const { t } = useI18n()
    const route = useRoute()
    const projectCode = computed(() => route.params.projectId as string)
    const flowModel = useFlowModel()
    const isTriggerPanelVisible = ref(false)
    const editingTriggerIndex = ref<number | null>(null)
    const panelElement = ref<Element | null>(null)
    const isCreatingTrigger = ref(false)
    const triggerEventSelectorPopoverRef = ref<InstanceType<typeof Popover> | null>(null)

    function cloneElement(element: Element): Element {
      return JSON.parse(JSON.stringify(element))
    }

    const triggerContainer = computed(() => {
      return flowModel.flowModel.value?.stages?.[0]?.containers?.[0] || null
    })

    const triggerElements = computed(() => triggerContainer.value?.elements || [])

    watch(isTriggerPanelVisible, (visible) => {
      if (!visible) {
        editingTriggerIndex.value = null
        panelElement.value = null
        isCreatingTrigger.value = false
      }
    })

    const openTriggerPanel = (element: Element, index: number | null, isNew = false) => {
      panelElement.value = cloneElement(element)
      editingTriggerIndex.value = index
      isCreatingTrigger.value = isNew
      isTriggerPanelVisible.value = true
    }

    const handleTriggerNameClick = (index: number) => {
      const element = triggerElements.value[index]
      if (!element) return
      openTriggerPanel(element, index, false)
    }

    // 处理选择触发事件
    const handleSelectEvent = (trigger: TriggerBaseItem) => {
      const triggerStage = flowModel.flowModel.value?.stages?.[0]
      if (!triggerStage) return

      const container = triggerStage.containers?.[0]
      if (!container) return

      const nextIndex = container.elements?.length || 0

      // 创建新的触发事件元素
      const newElement = createDefaultElement(nextIndex, { 
        ...trigger, 
        '@type': 'marketEvent'
      })

      openTriggerPanel(newElement, nextIndex, true)
      triggerEventSelectorPopoverRef.value?.hide()
    }

    // 切换启用状态
    const handleToggleEnable = (index: number, enabled: boolean) => {
      const event = flowModel.triggerEvents.value[index]
      if (!event) return

      // 更新flowModel中的enable状态
      const triggerStage = flowModel.flowModel.value?.stages?.[0]
      if (!triggerStage) return

      const container = triggerStage.containers?.[0]
      if (!container) return

      const element = container.elements?.[index]
      if (!element) return

      // 直接更新元素
      container.elements[index] = {
        ...element,
        additionalOptions: {
          ...(element.additionalOptions || {}),
          enable: enabled,
        } as any, // 使用类型断言避免类型检查问题
      }

      // 触发更新
      if (flowModel.flowModel.value) {
        flowModel.updateFlowModel(flowModel.flowModel.value)
      }
    }

    // 删除触发事件
    const handleDelete = (index: number) => {
      if (flowModel.triggerEvents.value.length <= 1) {
        Message({
          theme: 'error',
          message: t('flow.content.triggerEventAtLeastOne'),
        })
        return
      }

      const triggerStage = flowModel.flowModel.value?.stages?.[0]
      if (!triggerStage) return

      const container = triggerStage.containers?.[0]
      if (!container || !container.elements) return

      // 直接删除元素
      container.elements.splice(index, 1)

      // 触发更新
      if (flowModel.flowModel.value) {
        flowModel.updateFlowModel(flowModel.flowModel.value)
      }
    }

    const handleTriggerSave = (element: Element) => {
      const container = triggerContainer.value
      const index = editingTriggerIndex.value
      if (!container || index === null) return
      if (!container.elements) {
        container.elements = []
      }

      if (isCreatingTrigger.value && index >= container.elements.length) {
        container.elements.push(element)
      } else {
        container.elements[index] = element
      }

      if (flowModel.flowModel.value) {
        flowModel.updateFlowModel(flowModel.flowModel.value)
      }

      isTriggerPanelVisible.value = false
    }

    const getTriggerDisplayName = (element: Element) => {
      if (element.name) return element.name
      return element.atomCode || t('flow.content.selectTriggerEvent')
    }

    // 表格列配置
    const columns = [
      {
        label: t('flow.content.event'),
        prop: 'name',
        minWidth: 200,
        render: ({ row, index }: { row: Element; index: number }) => (
          <div
            class={[styles.eventCell, styles.eventCellClickable]}
            onClick={() => handleTriggerNameClick(index)}
          >
            <span class={[styles.eventName, !row.atomCode && styles.deletedName]}>
              {getTriggerDisplayName(row)}
            </span>
          </div>
        ),
      },
      {
        label: t('flow.content.enableStatus'),
        render: ({ row, index }: { row: Element; index: number }) => (
          <Switcher
            modelValue={row.additionalOptions?.enable ?? true}
            size="small"
            theme="primary"
            disabled={!row.atomCode}
            onChange={(val: boolean) => handleToggleEnable(index, val)}
          />
        ),
      },
      {
        label: t('flow.content.actions'),
        render: ({ index }: { index: number }) => (
          <Button
            text
            theme="primary"
            onClick={() => handleDelete(index)}
            title={t('flow.common.delete')}
          >
            <SvgIcon name="minus-circle" size={16} />
          </Button>
        ),
      },
    ]

    return () => (
      <div class={sharedStyles.tabContainer}>
        <div class={styles.triggerEvents}>
          <div class={styles.header}>
            <Popover
              ref={triggerEventSelectorPopoverRef}
              trigger="click"
              placement="bottom-start"
              theme="light"
              width={600}
              arrow={false}
              extCls={styles.triggerEventSelectorPopover}
            >
              {{
                default: () => (
                  <Button text theme="primary">
                    <SvgIcon name="add-small" size={20} class={styles.addTriggerBtnIcon} />
                    {t('flow.content.addTriggerEvent')}
                  </Button>
                ),
                content: () => (
                  <TriggerEventSelector projectCode={projectCode.value} onSelect={handleSelectEvent} />
                ),
              }}
            </Popover>
          </div>

          <Table
            data={flowModel.triggerEvents.value}
            columns={columns as any}
            class={styles.triggerTable}
          >
            {{
              empty: () => <div class={styles.emptyState}>{t('flow.content.noTriggerEvents')}</div>,
            }}
          </Table>
        </div>

        {/* 触发事件选择器 */}
        {/* Popover renders selector; no extra content here */}

        <TriggerPropertyPanel
          v-model:visible={isTriggerPanelVisible.value}
          element={panelElement.value}
          onSave={handleTriggerSave}
        />
      </div>
    )
  },
})
