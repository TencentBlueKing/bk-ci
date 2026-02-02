import type { Param } from '@/api/flowModel'
import { SvgIcon } from '@/components/SvgIcon'
import { useFlowVariables } from '@/hooks/useFlowVariables'
import { VariableCategory, VariablePanelTab, type ReadOnlyVariableGroup } from '@/types/variable'
import { Alert, Button, Collapse, Input, Message, Tab } from 'bkui-vue'
import { computed, defineComponent, ref, watch } from 'vue'
import { VueDraggable, type SortableEvent } from 'vue-draggable-plus'
import { useI18n } from 'vue-i18n'
import ReadOnlyVariableItem from './ReadOnlyVariableItem'
import VariableForm from './VariableForm'
import VariableItem from './VariableItem'
import styles from './VariablePanel.module.css'

interface CategoryItem {
  category: VariableCategory
  name: string
  variables: Record<string, Param[]>
  emptyText: string
  totalCount?: number
}

export default defineComponent({
  name: 'VariablePanel',
  props: {
    flowId: {
      type: String,
      required: true,
    },
    editable: {
      type: Boolean,
      default: true,
    },
    modelValue: {
      type: Boolean,
      default: true,
    },
  },
  emits: ['toggle', 'update:modelValue'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const DRAG_HANDLE_CLASS = 'drag-handle'
    const isOpen = ref(props.modelValue)
    const activePanelTab = ref(VariablePanelTab.VARIABLES) // 顶级Tab
    const searchKeyword = ref('')
    // Default expanded panels - expand INPUT category by default
    const expandedPanels = ref<number[]>([0]) // 0 is the index of INPUT category

    // Use flow variables hook - unified hook for all variables
    const {
      // Flow variables
      variables,
      existingIds,
      addVariable,
      updateVariable,
      removeVariable,
      updateParams,
      // Plugin output variables
      pluginOutputVariables,
      fetchPluginOutputVariables,
      // System variables
      systemVariables,
      fetchSystemVariables,
    } = useFlowVariables(props.flowId)

    // Edit mode state
    const isEditMode = ref(false)
    const editingVariable = ref<Param | null>(null)
    const currentAddingCategory = ref<VariableCategory>(VariableCategory.INPUT)

    // Filter variables by category and group them
    const getVariablesByCategory = (category: VariableCategory) => {
      const filtered = variables.value.filter((v) => {
        // Determine category from constant flag

        const variableCategory = v.constant
          ? VariableCategory.CONSTANT
          : !v.required
            ? VariableCategory.OTHER
            : VariableCategory.INPUT
        const matchCategory = variableCategory === category
        const matchSearch =
          !searchKeyword.value ||
          v.id.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
          v.name.toLowerCase().includes(searchKeyword.value.toLowerCase())
        return matchCategory && matchSearch
      })

      // Group by category (user-defined group name)
      const grouped = filtered.reduce(
        (acc, variable) => {
          const groupKey = variable.category || t('flow.variable.ungrouped')
          if (!acc[groupKey]) {
            acc[groupKey] = []
          }
          acc[groupKey].push(variable)
          return acc
        },
        {} as Record<string, Param[]>,
      )

      return grouped
    }

    // Filter system variable groups
    const getFilteredReadonlyVariableGroups = (list: ReadOnlyVariableGroup[]) => {
      if (!searchKeyword.value) {
        return list
      }

      return list
        .map((group) => ({
          ...group,
          params: group.params.filter(
            (v) =>
              v.id.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
              v.name.toLowerCase().includes(searchKeyword.value.toLowerCase()),
          ),
        }))
        .filter((group) => group.params.length > 0)
    }

    // Computed variables for each tab (grouped)
    const inputVariables = computed(() => getVariablesByCategory(VariableCategory.INPUT))
    const constantVariables = computed(() => getVariablesByCategory(VariableCategory.CONSTANT))
    const otherVariables = computed(() => getVariablesByCategory(VariableCategory.OTHER))
    const filteredPluginOutputVariables = computed(() =>
      getFilteredReadonlyVariableGroups(pluginOutputVariables.value),
    )
    const filteredSystemVariableGroups = computed(() =>
      getFilteredReadonlyVariableGroups(systemVariables.value),
    )

    // Get current category for adding variables
    const currentCategory = computed(() => currentAddingCategory.value)

    // Get existing categories (group names) from variables
    const getExistingCategories = () => {
      const categories = new Set<string>()
      variables.value.forEach((v) => {
        if (v.category) {
          categories.add(v.category)
        }
      })
      return Array.from(categories)
    }

    const variableCategories = computed<CategoryItem[]>(() => {
      return [
        {
          category: VariableCategory.INPUT,
          name: t('flow.variable.inputParams'),
          variables: inputVariables.value,
          emptyText: t('flow.variable.noInputParams'),
        },
        {
          category: VariableCategory.CONSTANT,
          name: t('flow.variable.constants'),
          variables: constantVariables.value,
          emptyText: t('flow.variable.noConstants'),
        },
        {
          category: VariableCategory.OTHER,
          name: t('flow.variable.otherVariables'),
          variables: otherVariables.value,
          emptyText: t('flow.variable.noOtherVariables'),
        },
      ].map((category) => {
        const totalCount = Object.values(category.variables).reduce(
          (sum, group) => sum + group.length,
          0,
        )
        return {
          ...category,
          totalCount,
        }
      })
    })

    // Drag sort configuration - 限制拖拽区域
    const getDragOptions = (category: VariableCategory, group: string) => {
      return {
        group: `variables-${category}-${group}`,
        animation: 200,
        ghostClass: styles.dragGhost,
        chosenClass: styles.dragChosen,
        dragClass: styles.dragging,
        disabled: !props.editable,
        handle: `.${DRAG_HANDLE_CLASS}`, // 只有drag-handle类名支持拖拽
      }
    }

    // Toggle panel
    const togglePanel = () => {
      isOpen.value = !isOpen.value
      emit('toggle', isOpen.value)
      emit('update:modelValue', isOpen.value)
    }

    // Watch modelValue changes from parent
    watch(
      () => props.modelValue,
      (newValue) => {
        if (newValue !== isOpen.value) {
          isOpen.value = newValue
        }
      },
    )

    // Handle panel tab change
    const handlePanelTabChange = (name: string) => {
      activePanelTab.value = name as VariablePanelTab
      searchKeyword.value = ''
      // Load data when switching to different tabs
      if (name === VariablePanelTab.PLUGIN_OUTPUT) {
        fetchPluginOutputVariables()
      } else if (name === VariablePanelTab.SYSTEM) {
        fetchSystemVariables()
      }
    }

    // Handle add variable
    const handleAddVariable = (category?: VariableCategory) => {
      editingVariable.value = null
      // Set the category for new variable
      if (category) {
        currentAddingCategory.value = category
      }
      isEditMode.value = true
    }

    // Handle edit variable
    const handleEditVariable = (variable: Param) => {
      editingVariable.value = variable
      isEditMode.value = true
    }

    // Handle save variable
    const handleSaveVariable = async (variable: Param) => {
      try {
        if (editingVariable.value) {
          // Update existing variable
          await updateVariable(variable)
        } else {
          // Add new variable
          await addVariable(variable)
        }

        isEditMode.value = false
        editingVariable.value = null
      } catch (error) {
        // Error handling is done in the hook
      }
    }

    // Handle delete variable
    const handleDeleteVariable = async (variableId: string) => {
      try {
        await removeVariable(variableId)
      } catch (error) {
        // Error handling is done in the hook
      }
    }

    // Handle copy variable reference
    const handleCopyReference = (reference: string) => {
      Message({ theme: 'success', message: t('flow.variable.copySuccess') })
    }

    // Handle cancel
    const handleCancel = () => {
      isEditMode.value = false
      editingVariable.value = null
    }

    // Watch flow model changes to reload plugin output variables
    watch(
      () => props.flowId,
      () => {
        // Reload plugin output variables when model changes
        if (isOpen.value && activePanelTab.value === VariablePanelTab.PLUGIN_OUTPUT) {
          fetchPluginOutputVariables()
        }
      },
    )

    const renderSearchInput = () => (
      <Input
        v-model={searchKeyword.value}
        placeholder={t('flow.variable.searchPlaceholder')}
        clearable
        class={styles.searchInput}
      >
        {{
          suffix: () => <SvgIcon name="search" class={styles.searchInputIcon} />,
        }}
      </Input>
    )

    // Render grouped variable list - 平铺展示，支持拖拽排序
    const renderGroupedVariableList = (
      category: VariableCategory,
      groupedVariables: Record<string, Param[]>,
      emptyText: string,
    ) => {
      const groups = Object.keys(groupedVariables)
      if (groups.length === 0) {
        return <div class={styles.emptyTip}>{emptyText}</div>
      }

      return groups.map((groupName) => {
        const groupVariables = groupedVariables[groupName] ?? []

        function handleDragEnd(e: SortableEvent) {
          if (e.oldIndex === undefined || e.newIndex === undefined || e.oldIndex === e.newIndex) {
            return
          }

          // Get the two variables that need to swap order
          const oldVariable = groupVariables[e.oldIndex]
          const newVariable = groupVariables[e.newIndex]

          if (!oldVariable || !newVariable) return

          // Swap order values
          const tempOrder = oldVariable.order ?? e.oldIndex
          oldVariable.order = newVariable.order ?? e.newIndex
          newVariable.order = tempOrder

          // Get all variables, sort by order, and update
          const allVariables = [...variables.value].sort((a, b) => {
            const orderA = a.order ?? Infinity
            const orderB = b.order ?? Infinity
            return orderA - orderB
          })

          // Update all variables with sorted order
          updateParams(allVariables)
        }

        return (
          <div key={groupName} class={styles.variableGroup}>
            <div class={styles.groupLabel}>
              <span class={styles.groupName}>{groupName}</span>
              <span class={styles.groupCount}>{groupVariables?.length ?? 0}</span>
            </div>
            <div class={styles.groupContent}>
              <VueDraggable
                {...getDragOptions(category, groupName)}
                modelValue={groupVariables as Param[]}
                class={styles.draggableList}
                onEnd={handleDragEnd}
              >
                {groupVariables?.map((variable) => (
                  <VariableItem
                    key={variable.id}
                    dragHandleCls={DRAG_HANDLE_CLASS}
                    variable={variable}
                    editable={props.editable}
                    draggable={props.editable}
                    onEdit={handleEditVariable}
                    onDelete={handleDeleteVariable}
                    onCopy={handleCopyReference}
                  />
                ))}
              </VueDraggable>
            </div>
          </div>
        )
      })
    }

    const renderReadonlyVariables = (
      variableGroups: ReadOnlyVariableGroup[],
      emptyText: string,
      readonlyVarType: VariablePanelTab,
    ) => {
      if (variableGroups.length === 0) {
        return <div class={styles.emptyTip}>{emptyText}</div>
      }

      return (
        <div class={styles.variableList}>
          <div class={styles.listHeader}>{renderSearchInput()}</div>
          <Alert theme="info">
            {t(
              readonlyVarType === VariablePanelTab.PLUGIN_OUTPUT
                ? 'flow.variable.pluginOutputVariableTips'
                : 'flow.variable.systemVariableTips',
            )}
          </Alert>
          <div class={styles.listContent}>
            <Collapse useBlockTheme list={variableGroups}>
              {{
                title: (group: ReadOnlyVariableGroup) => (
                  <div class={styles.collapseHeader}>
                    <span class={styles.categoryTitle}>{group.name}</span>
                    <span class={styles.categorySum}>{group.params.length}</span>
                  </div>
                ),
                content: (group: ReadOnlyVariableGroup) => (
                  <div class={styles.flatVariableList}>
                    {group.params.map((variable) => (
                      <ReadOnlyVariableItem
                        key={variable.id}
                        variable={variable}
                        type={readonlyVarType}
                        onCopy={handleCopyReference}
                      />
                    ))}
                  </div>
                ),
              }}
            </Collapse>
          </div>
        </div>
      )
    }

    return () => (
      <div class={[styles.variablePanel, isOpen.value && styles.panelOpen]}>
        {/* Toggle button */}
        <div
          class={[styles.toggleButton, !isOpen.value && styles.buttonClosed]}
          onClick={togglePanel}
        >
          <SvgIcon
            name="arrows-up"
            class={isOpen.value ? styles.rotate90deg : styles.rotate270deg}
            size={18}
          />
          {t('flow.variable.title')}
        </div>

        {/* Panel content */}
        {isOpen.value && (
          <div class={styles.panelContent}>
            {!isEditMode.value ? (
              // Variable list view with top-level tabs
              <Tab
                active={activePanelTab.value}
                type="unborder-card"
                onChange={handlePanelTabChange}
                class={styles.panelTab}
              >
                {/* Variables Tab */}
                <Tab.TabPanel name={VariablePanelTab.VARIABLES} label={t('flow.variable.title')}>
                  <div class={styles.variableList}>
                    {/* Tips */}
                    <Alert theme="info">{t('flow.variable.variableTips')}</Alert>

                    {/* Action buttons */}
                    <div class={styles.actionButtons}>
                      {props.editable && (
                        <>
                          <Button
                            theme="primary"
                            onClick={() => handleAddVariable(VariableCategory.INPUT)}
                            class={styles.addButton}
                          >
                            {t('flow.variable.addVariable')}
                          </Button>
                          <Button
                            onClick={() => handleAddVariable(VariableCategory.CONSTANT)}
                            class={styles.addButton}
                          >
                            {t('flow.variable.addConstant')}
                          </Button>
                          {renderSearchInput()}
                        </>
                      )}
                    </div>

                    <div class={styles.listContent}>
                      <Collapse v-model={expandedPanels.value} accordion={false} list={variableCategories.value} useBlockTheme>
                        {{
                          title: (item: CategoryItem) => (
                            <div class={styles.collapseHeader}>
                              <span class={styles.categoryTitle}>{item.name}</span>
                              <span class={styles.categorySum}>{item.totalCount}</span>
                            </div>
                          ),
                          content: (item: CategoryItem) =>
                            renderGroupedVariableList(
                              item.category,
                              item.variables,
                              item.emptyText,
                            ),
                        }}
                      </Collapse>
                    </div>
                  </div>
                </Tab.TabPanel>

                {/* Plugin Output Variables Tab */}
                <Tab.TabPanel
                  name={VariablePanelTab.PLUGIN_OUTPUT}
                  label={t('flow.variable.pluginOutputVariables')}
                >
                  {renderReadonlyVariables(
                    filteredPluginOutputVariables.value,
                    t('flow.variable.noPluginOutputVariables'),
                    VariablePanelTab.PLUGIN_OUTPUT,
                  )}
                </Tab.TabPanel>

                {/* System Variables Tab */}
                <Tab.TabPanel
                  name={VariablePanelTab.SYSTEM}
                  label={t('flow.variable.systemVariables')}
                >
                  {renderReadonlyVariables(
                    filteredSystemVariableGroups.value,
                    t('flow.variable.noSystemVariables'),
                    VariablePanelTab.SYSTEM,
                  )}
                </Tab.TabPanel>
              </Tab>
            ) : (
              // Variable edit form view
              <div class={styles.editFormContainer}>
                <div class={styles.editFormHeader}>
                  <span class={styles.editFormBackIcon} onClick={handleCancel}>
                    <SvgIcon name="arrows-left-shape" />
                  </span>
                  <span class={styles.editFormTitle}>
                    {editingVariable.value
                      ? t('flow.variable.editVariable')
                      : t('flow.variable.addVariable')}
                  </span>
                </div>
                <VariableForm
                  variable={editingVariable.value}
                  category={currentCategory.value}
                  existingIds={existingIds.value}
                  existingCategories={getExistingCategories()}
                  editable={props.editable}
                  allVariables={variables.value}
                  onSave={handleSaveVariable}
                  onCancel={handleCancel}
                />
              </div>
            )}
          </div>
        )}
      </div>
    )
  },
})
