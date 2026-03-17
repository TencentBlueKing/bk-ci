import type { Element } from '@/api/flowModel'
import { rely } from '@/utils/atom'
import { Collapse, Form } from 'bkui-vue'
import type { InjectionKey } from 'vue'
import { computed, defineAsyncComponent, defineComponent, provide, ref, watch, type PropType } from 'vue'
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

const SELF_ERROR_COMPONENTS = new Set(['timer-cron-tab'])

// 组件映射表
const COMPONENT_MAP: Record<string, any> = {
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
interface GroupMapItem {
  name?: string
  label?: string
  isExpanded?: boolean
  props: Record<string, any>
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
    // 折叠面板的展开状态
    const expandedGroups = ref<string[]>([])

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

    const paramsGroupMap = computed<Record<string, GroupMapItem>>(() => {
      const { inputGroups = [] } = props.atomPropsModel
      let { input } = props.atomPropsModel

      if (!input) {
        input = props.atomPropsModel
      }

      // 初始化分组映射，包含 rootProps 用于存放未分组的字段
      const groupMap = inputGroups.reduce<Record<string, GroupMapItem>>(
        (acc, group) => {
          acc[group.name] = {
            name: group.name,
            label: group.label,
            props: {},
          }
          return acc
        },
        {
          rootProps: {
            props: {},
          },
        },
      )

      // 将字段分配到对应的分组
      Object.keys(input).forEach((key) => {
        const prop = input[key]
        // 如果字段指定了 groupName 且该分组存在，则放入对应分组
        const targetGroup =
          prop.groupName && groupMap[prop.groupName] ? groupMap[prop.groupName] : groupMap.rootProps
        targetGroup!.props[key] = prop
      })

      return groupMap
    })

    // 渲染单个表单字段（默认模式）
    const renderFormField = (key: string, obj: any) => {
      if (isHidden(obj, props.element)) return null

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
      return Object.entries(paramsGroupMap.value)
        .filter(([key, group]) => key !== 'rootProps' && Object.keys(group.props).length > 0)
        .map(([key, group]) => (
          <div key={key} class={styles.triggerGroup}>
            <div class={styles.triggerGroupTitle}>{group.label || key}:</div>
            <div class={styles.triggerGroupContent}>{renderTriggerGroupContent(group.props)}</div>
          </div>
        ))
    }

    return () => {
      // Trigger 模式：只有当有分组时才使用特殊样式，无分组时使用正常表单
      if (props.displayMode === DISPLAY_MODE.TRIGGER) {
        // 无分组时，使用正常表单显示
        if (!hasGroups.value) {
          return (
            <Form formType="vertical" class={styles.atomForm}>
              {renderGroupContent(paramsGroupMap.value.rootProps!.props)}
            </Form>
          )
        }

        // 有分组时，使用 Trigger 特殊样式
        return (
          <div class={styles.triggerForm}>
            {/* 渲染根级别字段（未分组的字段） */}
            {Object.keys(paramsGroupMap.value.rootProps!.props).length > 0 && (
              <Form formType="vertical" class={styles.atomForm}>
                {renderGroupContent(paramsGroupMap.value.rootProps!.props)}
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
          {/* 渲染根级别字段（未分组的字段） */}
          {Object.keys(paramsGroupMap.value.rootProps!.props).length > 0 &&
            renderGroupContent(paramsGroupMap.value.rootProps!.props)}

          {/* 渲染分组 */}
          {hasGroups.value && (
            <Collapse modelValue={expandedGroups.value} class={styles.groupCollapse} useBlockTheme>
              {Object.entries(paramsGroupMap.value)
                .filter(([key, group]) => key !== 'rootProps')
                .map(([key, group]) => (
                  <Collapse.CollapsePanel key={key} name={key}>
                    {{
                      header: () => <span class={styles.groupHeader}>{group.label || key}</span>,
                      content: () => renderGroupContent(group.props),
                    }}
                  </Collapse.CollapsePanel>
                ))}
            </Collapse>
          )}
        </Form>
      )
    }
  },
})
