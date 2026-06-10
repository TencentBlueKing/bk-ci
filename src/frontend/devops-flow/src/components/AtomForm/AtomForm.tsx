import type { Element } from '@/api/flowModel'
import { rely } from '@/utils/atom'
import { Collapse, Form } from 'bkui-vue'
import type { InjectionKey } from 'vue'
import { defineComponent, computed, defineAsyncComponent, provide, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import styles from './AtomForm.module.css'

export type ReportFieldErrorFn = (name: string, hasError: boolean) => void
export const REPORT_FIELD_ERROR_KEY: InjectionKey<ReportFieldErrorFn> = Symbol('reportFieldError')

const { FormItem } = Form

// 动态导入组件
const VuexInput = defineAsyncComponent(() => import('./VuexInput'))
const VuexTextarea = defineAsyncComponent(() => import('./VuexTextarea'))
const Selector = defineAsyncComponent(() => import('./Selector'))
const AtomCheckbox = defineAsyncComponent(() => import('./AtomCheckbox'))
const EnumInput = defineAsyncComponent(() => import('./EnumInput'))
const KeyValueMap = defineAsyncComponent(() => import('./KeyValueMap'))
const AtomAceEditor = defineAsyncComponent(() => import('./AtomAceEditor'))
const AtomCheckboxList = defineAsyncComponent(() => import('./AtomCheckboxList'))
const StaffInput = defineAsyncComponent(() => import('./StaffInput'))
const AtomDatePicker = defineAsyncComponent(() => import('./AtomDatePicker'))
const CronTab = defineAsyncComponent(() => import('./CronTab'))
const SubParameter = defineAsyncComponent(() => import('./SubParameter'))
const SelectInput = defineAsyncComponent(() => import('./SelectInput'))
const FormFieldGroup = defineAsyncComponent(() => import('./FormFieldGroup'))
const EnumButton = defineAsyncComponent(() => import('./EnumButton'))
const CompositeInput = defineAsyncComponent(() => import('./CompositeInput'))
const TipsSimple = defineAsyncComponent(() => import('./TipsSimple'))
const ConditionalInputSelector = defineAsyncComponent(() => import('./ConditionalInputSelector'))
const DevopsSelect = defineAsyncComponent(() => import('./DevopsSelect'))
const DynamicParameterSimple = defineAsyncComponent(() => import('./DynamicParameterSimple'))

const SELF_ERROR_COMPONENTS = new Set(['timer-cron-tab'])

// 组件映射表
export const COMPONENT_MAP: Record<string, any> = {
  'vuex-input': VuexInput,
  'vuex-textarea': VuexTextarea,
  selector: Selector,
  'atom-checkbox': AtomCheckbox,
  'enum-input': EnumInput,
  'key-value': KeyValueMap,
  'key-value-normal': KeyValueMap,
  'atom-ace-editor': AtomAceEditor,
  'atom-checkbox-list': AtomCheckboxList,
  'staff-input': StaffInput,
  'company-staff-input': StaffInput,
  'atom-date-picker': AtomDatePicker,
  // 兼容旧配置的名称
  input: VuexInput,
  textarea: VuexTextarea,
  select: Selector,
  checkbox: AtomCheckbox,
  radio: EnumInput,
  'checkbox-list': AtomCheckboxList,
  'user-input': StaffInput,
  'date-picker': AtomDatePicker,
  'time-picker': AtomDatePicker,
  'code-editor': AtomAceEditor,
  'timer-cron-tab': CronTab,
  'sub-parameter': SubParameter,
  'request-selector': Selector,
  'select-input': SelectInput,
  'enum-button': EnumButton,
  'composite-input': CompositeInput,
  'tips-simple': TipsSimple,
  'conditional-input-selector': ConditionalInputSelector,
  'devops-select': DevopsSelect,
  'dynamic-parameter-simple': DynamicParameterSimple,
}

// Display mode types
export const DISPLAY_MODE = {
  ACCORDION: 'accordion', // 手风琴折叠模式（用于插件）
  TRIGGER: 'trigger', // 触发器模式（扁平化分组）
} as const

export type DisplayModeType = (typeof DISPLAY_MODE)[keyof typeof DISPLAY_MODE]

interface InputGroup {
  name: string
  label: string
  isExpanded: boolean
  props: Record<string, any>
}
interface GroupEntry {
  key: string
  isInputGroup: boolean
  name?: string
  label?: string
  isExpanded?: boolean
  propKey?: string
  prop?: Record<string, any>
  props: Record<string, any> | Array<Record<string, any>>
  [key: string]: any
}

export interface AtomPropsModel {
  input: Record<string, any>
  inputGroups?: InputGroup[]
  output: Record<string, any>
  [key: string]: any
}

export default defineComponent({
  name: 'AtomForm',
  props: {
    atomPropsModel: {
      type: Object as PropType<AtomPropsModel>,
      default: () => ({}),
    },
    atomValue: {
      type: Object,
      default: () => ({}),
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    element: {
      type: Object as PropType<Element>,
      required: true,
    },
    // 展示模式：accordion（手风琴）或 trigger（触发器扁平化）
    displayMode: {
      type: String as PropType<DisplayModeType>,
      default: DISPLAY_MODE.ACCORDION,
    },
    errorFields: {
      type: Array as PropType<string[]>,
      default: () => [],
    },
  },
  emits: ['change', 'fieldError'],
  setup(props, { emit }) {
    const { t } = useI18n()
    // 折叠面板的展开状态（受控）
    const expandedGroups = ref<string[]>([])

    // 初始化分组展开状态：依据配置 isExpanded 决定默认展开的分组。
    // 仅在分组集合发生变化时执行（如切换插件/版本），不会在编辑字段触发的重渲染时重置，
    // 因此用户在分组内的交互不会导致分组意外收起。
    watch(
      () => props.atomPropsModel?.inputGroups,
      (groups) => {
        if (!Array.isArray(groups) || groups.length === 0) {
          expandedGroups.value = []
          return
        }
        const known = new Set(groups.map((g) => g.name))
        const defaultExpanded = groups.filter((g) => g && g.isExpanded).map((g) => g.name)
        // 保留用户已展开/收起的状态，仅对新出现的分组应用默认值
        const preserved = expandedGroups.value.filter((name) => known.has(name))
        expandedGroups.value = Array.from(new Set([...preserved, ...defaultExpanded]))
      },
      { immediate: true, deep: true },
    )

    // 受控更新展开状态，保证 modelValue 始终反映真实状态
    const handleGroupToggle = (val: unknown) => {
      expandedGroups.value = Array.isArray(val) ? val.map(String) : []
    }

    const fieldErrors = ref<Set<string>>(new Set())
    const reportFieldError: ReportFieldErrorFn = (name, hasError) => {
      const next = new Set(fieldErrors.value)
      if (hasError) {
        next.add(name)
      } else {
        next.delete(name)
      }
      fieldErrors.value = next
    }
    provide(REPORT_FIELD_ERROR_KEY, reportFieldError)

    watch(fieldErrors, (errors) => {
      emit('fieldError', Array.from(errors))
    })

    const handleChange = (name: string, value: any) => {
      emit('change', name, value)
    }

    // 判断字段是否隐藏
    const isHidden = (obj: any, element: any) => {
      try {
        // 1. rely 检查 (依赖于当前表单值)
        if (!rely(obj, props.atomValue)) {
          return true
        }

        if (typeof obj.isHidden === 'function') {
          return obj.isHidden(element)
        }

        if (typeof obj.isHidden === 'string') {
          return false
        }

        if (typeof obj.hidden === 'boolean') {
          return obj.hidden
        }

        return false
      } catch (error) {
        console.error('Error in isHidden:', error)
        return false
      }
    }

    // 获取默认值
    const getPlaceholder = (obj: any) => {
      return obj.placeholder || obj.desc || ''
    }
    const hasGroups = computed(() => {
      if (Array.isArray(props.atomPropsModel?.inputGroups)) {
        return props.atomPropsModel?.inputGroups?.length > 0
      }
      return false
    })

    const paramsGroupSort = computed<string[]>(() => props.atomPropsModel?.sort || [])

    const paramsGroupEntries = computed<GroupEntry[]>(() => {
      const { inputGroups = [] } = props.atomPropsModel
      let { input } = props.atomPropsModel

      if (!input) {
        input = props.atomPropsModel
      }

      if (paramsGroupSort.value.length) {
        const inputGroupMap = inputGroups.reduce<Record<string, GroupEntry>>((acc, group) => {
          acc[group.name] = {
            ...group,
            key: group.name,
            isInputGroup: true,
            props: [],
          }
          return acc
        }, {})

        const groupedMap: Record<string, GroupEntry> = {}
        Object.keys(input).forEach((key) => {
          const prop = input[key]
          const inputGroup = prop.groupName ? inputGroupMap[prop.groupName] : undefined
          if (inputGroup) {
            ;(inputGroup.props as Array<Record<string, any>>).push({
              key,
              ...prop,
            })
            groupedMap[prop.groupName] = inputGroup
          } else {
            groupedMap[key] = {
              key,
              propKey: key,
              prop,
              isInputGroup: false,
              props: [],
            }
          }
        })

        return paramsGroupSort.value
          .map((key) => groupedMap[key])
          .filter((entry): entry is GroupEntry => !!entry)
      }

      const groupMap = inputGroups.reduce<Record<string, GroupEntry>>((acc, group) => {
        acc[group.name] = {
          ...group,
          key: group.name,
          isInputGroup: true,
          props: {},
        }
        return acc
      }, {})

      const rootProps: Record<string, any> = {}
      Object.keys(input).forEach((key) => {
        const prop = input[key]
        const targetGroup = prop.groupName && groupMap[prop.groupName]
          ? groupMap[prop.groupName]
          : null

        if (targetGroup) {
          ;(targetGroup.props as Record<string, any>)[key] = prop
        } else {
          rootProps[key] = prop
        }
      })

      const entries: GroupEntry[] = []
      if (Object.keys(rootProps).length > 0) {
        entries.push({
          key: 'rootProps',
          isInputGroup: false,
          props: rootProps,
        })
      }

      Object.values(groupMap).forEach((group) => {
        entries.push(group)
      })

      return entries
    })

    // 渲染单个表单字段（默认模式）
    const renderFormField = (key: string, obj: any) => {
      if (isHidden(obj, props.element)) return null

      // group 类型：使用 FormFieldGroup 包裹子字段
      if (obj.type === 'group') {
        return (
          <FormFieldGroup
            key={key}
            label={obj.label}
            desc={obj.desc}
            name={key}
            value={props.atomValue[key] ?? obj.default ?? false}
            showSwitch={obj.showSwitch ?? false}
            topDivider={obj.topDivider ?? false}
            docs={obj.docs}
            docsLink={obj.docsLink}
            handleChange={handleChange}
          >
            {obj.children?.map((child: any) => {
              if (isHidden(child, props.element)) return null

              const childKey = child.key
              const componentType = child.component || child.type
              const ChildComponent = COMPONENT_MAP[componentType] || VuexInput
              const childValue = props.atomValue[childKey] ?? child.default ?? ''
              const hasError = props.errorFields.includes(childKey)
              const showDefaultError = hasError && !SELF_ERROR_COMPONENTS.has(componentType)
              const { '@type': _type, ...childRest } = child

              return (
                <FormItem
                  key={childKey}
                  label={child.label}
                  required={child.required}
                  property={childKey}
                  description={child.desc}
                  class={hasError ? styles.fieldError : ''}
                >
                  <ChildComponent
                    name={childKey}
                    value={childValue}
                    disabled={props.disabled}
                    readOnly={props.disabled}
                    placeholder={getPlaceholder(child)}
                    handleChange={handleChange}
                    atomValue={props.atomValue}
                    {...childRest}
                  />
                  {showDefaultError && <p class={styles.fieldErrorMessage}>{t('flow.orchestration.fieldRequired')}</p>}
                </FormItem>
              )
            })}
          </FormFieldGroup>
        )
      }

      const componentType = obj.component || obj.type
      const Component = COMPONENT_MAP[componentType] || VuexInput
      const value = props.atomValue[key] ?? obj.default ?? ''
      const hasError = props.errorFields.includes(key)
      const showDefaultError = hasError && !SELF_ERROR_COMPONENTS.has(componentType)
      // remove '@type' from obj
      const { '@type': _type, ...rest } = obj
      return (
        <FormItem
          key={key}
          label={obj.label}
          required={obj.required}
          property={key}
          description={obj.desc}
          class={hasError ? styles.fieldError : ''}
        >
          <Component
            name={key}
            value={value}
            disabled={props.disabled}
            readOnly={props.disabled}
            placeholder={getPlaceholder(obj)}
            handleChange={handleChange}
            atomValue={props.atomValue}
            {...rest}
          />
          {showDefaultError && <p class={styles.fieldErrorMessage}>{t('flow.orchestration.fieldRequired')}</p>}
        </FormItem>
      )
    }

    // 渲染 Trigger 模式下的表单字段（左侧标签卡片，右侧输入框）
    const renderTriggerFormField = (key: string, obj: any) => {
      if (isHidden(obj, props.element)) return null

      const Component = COMPONENT_MAP[obj.component] || COMPONENT_MAP[obj.type] || VuexInput
      const value = props.atomValue[key] ?? obj.default ?? ''
      const hasError = props.errorFields.includes(key)
      // remove '@type' from obj
      const { '@type': _type, ...rest } = obj

      return (
        <div key={key} class={[styles.triggerFieldRow, hasError && styles.triggerFieldError]}>
          <div class={styles.triggerFieldLabel}>
            <span class={styles.triggerFieldLabelText}>
              {obj.label}
              {obj.required && <span class={styles.requiredMark}>*</span>}
            </span>
          </div>
          <div class={styles.triggerFieldInput}>
            <Component
              name={key}
              value={value}
              disabled={props.disabled}
              placeholder={getPlaceholder(obj)}
              handleChange={handleChange}
              atomValue={props.atomValue}
              {...rest}
            />
          </div>
        </div>
      )
    }

    // 渲染分组内容（默认模式）
    const renderGroupContent = (groupProps: Record<string, any>) => {
      return Object.entries(groupProps).map(([key, obj]) => renderFormField(key, obj))
    }

    // 渲染 Trigger 模式下的分组内容
    const renderTriggerGroupContent = (groupProps: Record<string, any>) => {
      return Object.entries(groupProps).map(([key, obj]) => renderTriggerFormField(key, obj))
    }

    // 渲染 Trigger 模式下的分组
    const renderTriggerGroups = () => {
      return paramsGroupEntries.value
        .filter((group) => group.isInputGroup && rely(group, props.atomValue))
        .map((group) => {
          const groupProps = Array.isArray(group.props)
            ? Object.fromEntries(group.props.map((item) => [item.key, item]))
            : group.props

          if (Object.keys(groupProps).length === 0) return null

          return (
            <div key={group.key} class={styles.triggerGroup}>
              <div class={styles.triggerGroupTitle}>{group.label || group.key}:</div>
              <div class={styles.triggerGroupContent}>{renderTriggerGroupContent(groupProps)}</div>
            </div>
          )
        })
    }

    const renderAccordionEntries = () => {
      return paramsGroupEntries.value.map((entry) => {
        if (!entry.isInputGroup) {
          if (entry.propKey && entry.prop) {
            return renderFormField(entry.propKey, entry.prop)
          }

          if (!Array.isArray(entry.props)) {
            return renderGroupContent(entry.props)
          }

          return null
        }

        if (!rely(entry, props.atomValue)) {
          return null
        }

        const groupProps = Array.isArray(entry.props)
          ? Object.fromEntries(entry.props.map((item) => [item.key, item]))
          : entry.props

        if (Object.keys(groupProps).length === 0) {
          return null
        }

        return (
          <Collapse.CollapsePanel
            key={entry.key}
            name={entry.key}
            title={entry.label || entry.key}
            class={styles.groupPanel}
          >
            {{
              content: () => <div class={styles.groupContent}>{renderGroupContent(groupProps)}</div>,
            }}
          </Collapse.CollapsePanel>
        )
      })
    }

    const renderRootOrSortedFields = () => {
      return paramsGroupEntries.value
        .filter((entry) => !entry.isInputGroup)
        .map((entry) => {
          if (entry.propKey && entry.prop) {
            return renderFormField(entry.propKey, entry.prop)
          }

          if (!Array.isArray(entry.props)) {
            return renderGroupContent(entry.props)
          }

          return null
        })
    }

    const getRootPropsForTrigger = () => {
      return paramsGroupEntries.value.reduce<Record<string, any>>((acc, entry) => {
        if (entry.isInputGroup) return acc
        if (entry.propKey && entry.prop) {
          acc[entry.propKey] = entry.prop
          return acc
        }
        if (!Array.isArray(entry.props)) {
          Object.assign(acc, entry.props)
        }
        return acc
      }, {})
    }

    const rootPropsForTrigger = computed(() => getRootPropsForTrigger())

    return () => {
      // Trigger 模式：只有当有分组时才使用特殊样式，无分组时使用正常表单
      if (props.displayMode === DISPLAY_MODE.TRIGGER) {
        // 无分组时，使用正常表单显示
        if (!hasGroups.value) {
          return (
            <Form formType="vertical" class={styles.atomForm}>
              {renderRootOrSortedFields()}
            </Form>
          )
        }

        // 有分组时，使用 Trigger 特殊样式
        return (
          <div class={styles.triggerForm}>
            {/* 渲染根级别字段（未分组的字段） */}
            {Object.keys(rootPropsForTrigger.value).length > 0 && (
              <Form formType="vertical" class={styles.atomForm}>
                {renderGroupContent(rootPropsForTrigger.value)}
              </Form>
            )}

            {/* 渲染分组 */}
            {renderTriggerGroups()}
          </div>
        )
      }

      // 默认手风琴模式
      return (
        <Form formType="vertical" class={styles.atomForm}>
          {!hasGroups.value && renderRootOrSortedFields()}
          {hasGroups.value && (
            <Collapse
              modelValue={expandedGroups.value}
              onUpdate:modelValue={handleGroupToggle}
              class={styles.groupCollapse}
              useBlockTheme
              headerIconAlign="left"
            >
              {renderAccordionEntries()}
            </Collapse>
          )}
        </Form>
      )
    }
  },
})
