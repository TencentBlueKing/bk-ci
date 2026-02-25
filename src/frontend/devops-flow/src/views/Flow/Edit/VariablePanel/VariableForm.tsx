import type { Param } from '@/api/flowModel'
import { SvgIcon } from '@/components/SvgIcon'
import type { OptionsApiConfig, ParamOption } from '@/types/variable'
import {
  DEFAULT_VARIABLE_VALUES,
  OptionsSourceType,
  ParamType,
  VARIABLE_TYPE_LIST,
  VariableCategory,
  validateVariableId,
} from '@/types/variable'
import { Button, Checkbox, Form, Input, Popover, Radio, Select } from 'bkui-vue'
import { computed, defineComponent, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import OptionsEditor from './OptionsEditor'
import styles from './VariableForm.module.css'

const FormItem = Form.FormItem

export default defineComponent({
  name: 'VariableForm',
  props: {
    variable: {
      type: Object as PropType<Param | null>,
      default: null,
    },
    category: {
      type: String as PropType<VariableCategory>,
      required: true,
    },
    existingIds: {
      type: Array as PropType<string[]>,
      default: () => [],
    },
    editable: {
      type: Boolean,
      default: true,
    },
    existingCategories: {
      type: Array as PropType<string[]>,
      default: () => [],
    },
    // 当前流程的所有变量，用于 API 选项的变量引用
    allVariables: {
      type: Array as PropType<Param[]>,
      default: () => [],
    },
  },
  emits: ['save', 'cancel'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const formRef = ref()
    const formData = ref<Param>(getInitialFormData())

    // Is editing mode
    const isEditMode = computed(() => !!props.variable)

    // Is constant
    const isConstant = computed(() => props.category === VariableCategory.CONSTANT)

    // Available variable types
    const availableTypes = computed(() => {
      return VARIABLE_TYPE_LIST
    })

    // Form rules
    const rules = {
      id: [
        {
          required: true,
          message: t('flow.variable.idRequired'),
          trigger: 'blur',
        },
        {
          validator: (value: string) => {
            if (!validateVariableId(value, isConstant.value)) {
              return false
            }
            return true
          },
          message: isConstant.value ? t('flow.variable.constantIdRule') : t('flow.variable.idRule'),
          trigger: 'blur',
        },
        {
          validator: (value: string) => {
            if (isEditMode.value && value === props.variable?.id) {
              return true
            }
            return !props.existingIds.includes(value)
          },
          message: t('flow.variable.idExists'),
          trigger: 'blur',
        },
      ],
      name: [],
      type: [
        {
          required: true,
          message: t('flow.variable.typeRequired'),
          trigger: 'change',
        },
      ],
      defaultValue: [
        {
          validator: (value: unknown) => {
            // Only validate for constants
            if (!isConstant.value) {
              return true
            }
            // Boolean type allows false value
            if (formData.value.type === ParamType.BOOLEAN) {
              return value !== undefined && value !== null
            }
            // Array type (MULTIPLE) - check if not empty
            if (Array.isArray(value)) {
              return value.length > 0
            }
            // String/other types - check if not empty string
            return value !== undefined && value !== null && value !== ''
          },
          message: t('flow.variable.defaultValueRequired'),
          trigger: 'blur',
        },
      ],
    }

    // Initialize form data
    function getInitialFormData(): Param & { payload?: OptionsApiConfig } {
      if (props.variable) {
        return {
          ...props.variable,
          payload: (props.variable as Param & { payload?: OptionsApiConfig }).payload || {
            type: OptionsSourceType.LIST,
          },
        }
      }
      const defaultData = DEFAULT_VARIABLE_VALUES[ParamType.STRING]
      return {
        id: '',
        name: '',
        type: ParamType.STRING,
        constant: props.category === VariableCategory.CONSTANT,
        defaultValue: defaultData.defaultValue,
        required: true,
        desc: '',
        options: defaultData.options || [],
        valueNotEmpty: true,
        readOnly: false,
        payload: { type: OptionsSourceType.LIST },
      }
    }

    // Watch variable prop changes
    watch(
      () => props.variable,
      (newVal) => {
        formData.value = getInitialFormData()
      },
      { immediate: true },
    )

    // Handle type change
    const handleTypeChange = (value: ParamType | string) => {
      const typeValue = typeof value === 'string' ? value : value
      const defaultData = DEFAULT_VARIABLE_VALUES[typeValue as ParamType]
      formData.value = {
        ...formData.value,
        type: typeValue,
        defaultValue: defaultData.defaultValue,
        options: defaultData.options || [],
      }
    }

    // Handle save
    const handleSave = async () => {
      try {
        await formRef.value.validate()
        emit('save', formData.value)
      } catch (error) {
        console.error('Form validation failed:', error)
      }
    }

    // Handle cancel
    const handleCancel = () => {
      emit('cancel')
    }

    // Show options editor
    const showOptionsEditor = computed(() => {
      return formData.value.type === ParamType.ENUM || formData.value.type === ParamType.MULTIPLE
    })

    // Handle options update from OptionsEditor
    const handleOptionsUpdate = (options: ParamOption[]) => {
      formData.value.options = options
    }

    // Handle payload update from OptionsEditor
    const handlePayloadUpdate = (payload: OptionsApiConfig) => {
      ;(formData.value as Param & { payload?: OptionsApiConfig }).payload = payload
    }

    // Helper to check if type matches (handles both string and enum)
    const isType = (type: ParamType | string): boolean => {
      return formData.value.type === type || formData.value.type === String(type)
    }

    return () => (
      <div class={styles.variableForm}>
        <Form ref={formRef} model={formData.value} rules={rules} form-type="vertical">
          <FormItem label={t('flow.variable.id')} property="id" required>
            <Input
              v-model={formData.value.id}
              placeholder={
                isConstant.value
                  ? t('flow.variable.constantIdPlaceholder')
                  : t('flow.variable.idPlaceholder')
              }
              disabled={isEditMode.value || !props.editable}
            />
          </FormItem>

          <FormItem label={t('flow.variable.name')} property="name">
            <Input
              v-model={formData.value.name}
              placeholder={t('flow.variable.namePlaceholder')}
              disabled={!props.editable}
            />
          </FormItem>

          <FormItem label={t('flow.variable.type')} property="type" required>
            <Select
              v-model={formData.value.type}
              onChange={handleTypeChange}
              disabled={!props.editable}
            >
              {availableTypes.value.map((type) => (
                <Select.Option key={type.id} value={type.id} label={t(type.nameKey)}>
                  {t(type.nameKey)}
                </Select.Option>
              ))}
            </Select>
          </FormItem>

          <FormItem
            label={t('flow.variable.defaultValue')}
            property="defaultValue"
            required={isConstant.value}
          >
            {isType(ParamType.BOOLEAN) ? (
              <Radio.Group v-model={formData.value.defaultValue} disabled={!props.editable}>
                <Radio label={true}>true</Radio>
                <Radio label={false}>false</Radio>
              </Radio.Group>
            ) : isType(ParamType.TEXTAREA) ? (
              <Input
                v-model={formData.value.defaultValue}
                type="textarea"
                rows={3}
                placeholder={t('flow.variable.defaultValuePlaceholder')}
                disabled={!props.editable}
              />
            ) : isType(ParamType.ENUM) || isType(ParamType.MULTIPLE) ? (
              <Select
                v-model={formData.value.defaultValue}
                multiple={isType(ParamType.MULTIPLE)}
                disabled={!props.editable}
              >
                {((formData.value.options || []) as ParamOption[]).map((option) => (
                  <Select.Option key={option.key} value={option.key} label={option.value}>
                    {option.value}
                  </Select.Option>
                ))}
              </Select>
            ) : (
              <Input
                v-model={formData.value.defaultValue}
                placeholder={t('flow.variable.defaultValuePlaceholder')}
                disabled={!props.editable}
              />
            )}
          </FormItem>

          {showOptionsEditor.value && (
            <FormItem label={t('flow.variable.optionSource')}>
              <OptionsEditor
                options={(formData.value.options || []) as ParamOption[]}
                payload={
                  (formData.value as Param & { payload?: OptionsApiConfig }).payload || {
                    type: OptionsSourceType.LIST,
                  }
                }
                disabled={!props.editable}
                variables={props.allVariables}
                currentVariableId={formData.value.id}
                onUpdate:options={handleOptionsUpdate}
                onUpdate:payload={handlePayloadUpdate}
              />
            </FormItem>
          )}

          <FormItem label={t('flow.variable.group')}>
            <Input
              v-model={formData.value.category}
              placeholder={t('flow.variable.groupPlaceholder')}
              disabled={!props.editable}
              list="category-list"
            />
            <datalist id="category-list">
              {props.existingCategories.map((cat) => (
                <option key={cat} value={cat} />
              ))}
            </datalist>
          </FormItem>

          <FormItem label={t('flow.content.description')}>
            <Input
              v-model={formData.value.desc}
              type="textarea"
              rows={2}
              placeholder={t('flow.content.descriptionPlaceholder')}
              disabled={!props.editable}
            />
          </FormItem>

          {/* 只在非常量类型时显示这些选项 */}
          {!isConstant.value && (
            <>
              {/* 第一行：两个复选框并排，中间有分隔线 */}
              <FormItem>
                <div class={styles.checkboxRow}>
                  <div class={styles.checkboxItem}>
                    <Checkbox v-model={formData.value.required} disabled={!props.editable}>
                      {t('flow.variable.showOnExec')}
                    </Checkbox>
                    <Popover content={t('flow.variable.showOnExecDesc')} placement="top">
                      <span class={styles.infoIcon}>
                        <SvgIcon name="info-circle" />
                      </span>
                    </Popover>
                  </div>
                  <div class={styles.separator}></div>
                  <div class={styles.checkboxItem}>
                    <Checkbox
                      v-model={formData.value.valueNotEmpty}
                      disabled={!props.editable || !formData.value.required}
                    >
                      {t('flow.variable.required')}
                    </Checkbox>
                  </div>
                </div>
              </FormItem>
              {/* 第二行：运行时只读 */}
              <FormItem>
                <div class={styles.checkboxItem}>
                  <Checkbox v-model={formData.value.readOnly} disabled={!props.editable}>
                    {t('flow.variable.readOnlyOnRun')}
                  </Checkbox>
                  <Popover content={t('flow.variable.readOnlyOnRunDesc')} placement="top">
                    <span class={styles.infoIcon}>
                      <SvgIcon name="info-circle" />
                    </span>
                  </Popover>
                </div>
              </FormItem>
            </>
          )}

          {props.editable && (
            <FormItem>
              <div class={styles.formActions}>
                <Button theme="primary" onClick={handleSave}>
                  {t('flow.common.save')}
                </Button>
                <Button onClick={handleCancel}>{t('flow.common.cancel')}</Button>
              </div>
            </FormItem>
          )}
        </Form>
      </div>
    )
  },
})
